package com.krolasyon.labirent.maze;

import com.krolasyon.labirent.LabConfig;
import com.krolasyon.labirent.Labirent;
import com.krolasyon.labirent.entity.BoxElevatorEntity;
import com.krolasyon.labirent.net.Net;
import com.krolasyon.labirent.story.StoryDirector;
import com.krolasyon.labirent.team.Role;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Server-side heart of the mod: builds the maze, runs day/night, doors, shifting, arrivals, monsters and the story. */
@Mod.EventBusSubscriber(modid = Labirent.MODID)
public final class MazeManager {
    @Nullable private static MazeManager instance;

    public final MinecraftServer server;
    public final MazeState state;
    public final MazeBuilder builder = new MazeBuilder();
    public final DoorAnimator doors = new DoorAnimator();
    public final ShiftAnimator shift = new ShiftAnimator();
    public final ExploreTracker explore = new ExploreTracker();
    public final BoxDirector box = new BoxDirector(this);
    public final GrieverDirector grievers = new GrieverDirector(this);
    public final StoryDirector story = new StoryDirector(this);
    private boolean buildQueued;
    private boolean gladeReady;
    private int ticks;
    @Nullable private ServerBossEvent buildBar;

    private MazeManager(MinecraftServer server) {
        this.server = server;
        this.state = MazeState.get(server);
    }

    @Nullable
    public static MazeManager get() { return instance; }

    public boolean gladeReady() { return gladeReady; }

    @Nullable
    public ServerLevel level() { return Dims.maze(server); }

    // ------------------------------------------------------------------ lifecycle events
    @SubscribeEvent
    public static void onStarted(ServerStartedEvent e) { instance = new MazeManager(e.getServer()); }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent e) { instance = null; }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent e) {
        if (e.phase == TickEvent.Phase.END && instance != null) instance.tick();
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (instance != null && e.getEntity() instanceof ServerPlayer sp) instance.login(sp);
    }

    @SubscribeEvent
    public static void onDimChange(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (instance != null && e.getEntity() instanceof ServerPlayer sp && e.getTo() == Dims.MAZE) {
            Net.sendFullMap(sp, instance.state);
            Net.sendState(sp, instance);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (instance == null || !(e.getEntity() instanceof ServerPlayer sp)) return;
        if (LabConfig.AUTO_JOIN.get() && instance.state.arrived.contains(sp.getUUID()) && sp.level().dimension() != Dims.MAZE) {
            instance.sendToGlade(sp);
        }
        if (sp.level().dimension() == Dims.MAZE) {
            Net.sendFullMap(sp, instance.state);
            Net.sendState(sp, instance);
        }
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent e) {
        if (!(e.getEntity() instanceof Player p) || p.isCreative() || !(e.getLevel() instanceof ServerLevel l) || !Dims.isMaze(l)) return;
        if (!Glade.inside(e.getPos())) {
            e.setCanceled(true);
            p.displayClientMessage(Component.translatable("msg.labirent.no_build"), true);
        }
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent e) {
        Player p = e.getPlayer();
        if (p == null || p.isCreative() || !(e.getLevel() instanceof ServerLevel l) || !Dims.isMaze(l)) return;
        if (!Glade.inside(e.getPos()) || e.getPos().getY() < MazeLayout.GROUND - 1) {
            e.setCanceled(true);
            p.displayClientMessage(Component.translatable("msg.labirent.no_break"), true);
        }
    }

    @SubscribeEvent
    public static void onMount(EntityMountEvent e) {
        if (e.getLevel().isClientSide()) return;
        if (e.isDismounting() && e.getEntityBeingMounted() instanceof BoxElevatorEntity box && box.isAlive() && !box.released()) e.setCanceled(true);
    }

    // ------------------------------------------------------------------ build
    private void queueBuild(ServerLevel level) {
        buildQueued = true;
        if (state.seed == 0) state.seed = server.overworld().getSeed() ^ 0x4C61626972656E74L;
        if (state.layout == null) {
            state.layout = MazeLayout.generate(state.seed);
            state.setDirty();
        }
        if (state.built) {
            gladeReady = true;
            if (state.shiftPendingMask != 0) ShiftAnimator.rebuildSectors(builder, state, state.shiftPendingMask);
            return;
        }
        Glade.enqueue(builder);
        builder.then(l -> {
            gladeReady = true;
            explore.revealGlade(state);
        });
        List<int[]> units = new ArrayList<>();
        for (int i = 0; i < MazeLayout.G; i++)
            for (int j = 0; j < MazeLayout.G; j++)
                if (!MazeLayout.inGladeInterior(i, j)) units.add(new int[]{i, j});
        units.sort(Comparator.comparingInt(u -> (u[0] - 40) * (u[0] - 40) + (u[1] - 40) * (u[1] - 40)));
        builder.add(new MazeBuilder.Job() {
            int k;
            final BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();

            @Override
            public boolean run(ServerLevel l, MazeBuilder.Budget b) {
                while (k < units.size() && b.ok()) {
                    int[] u = units.get(k++);
                    MazeBuilder.buildUnit(l, state.layout, u[0], u[1], p);
                }
                return k >= units.size();
            }
        });
        builder.then(Landmarks::cliff);
        builder.then(l -> Landmarks.codeStones(l, state));
        Landmarks.enqueueChamber(builder);
        builder.then(l -> {
            state.built = true;
            state.setDirty();
            if (buildBar != null) {
                buildBar.removeAllPlayers();
                buildBar = null;
            }
            for (ServerPlayer p : server.getPlayerList().getPlayers())
                p.displayClientMessage(Component.translatable("msg.labirent.built"), false);
        });
    }

    private void updateBuildBar(ServerLevel level) {
        if (buildBar == null) {
            buildBar = new ServerBossEvent(Component.translatable("msg.labirent.building"), BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.PROGRESS);
        }
        buildBar.setProgress(builder.progress());
        for (ServerPlayer p : server.getPlayerList().getPlayers()) buildBar.addPlayer(p);
    }

    // ------------------------------------------------------------------ main loop
    private void tick() {
        ServerLevel level = level();
        if (level == null) return;
        ticks++;
        if (!buildQueued) queueBuild(level);
        builder.tick(level, state.built ? 5_000_000L : 35_000_000L);
        if (!state.built && ticks % 10 == 0) updateBuildBar(level);
        if (!gladeReady) return;

        long time = level.getDayTime();
        int day = (int) (time / 24000L);
        int phase = (int) (time % 24000L);
        boolean night = phase >= 12500 && phase < 23300;
        boolean sunGone = state.has(MazeState.EV_SUN_GONE);

        if (state.built) {
            doors.tick(level, night && !sunGone);
            int nightNo = (int) ((time + 10000L) / 24000L);
            if (!shift.active() && builder.idle()) {
                if (state.lastShiftNight < 0) {
                    state.lastShiftNight = nightNo;
                    state.setDirty();
                } else if (nightNo > state.lastShiftNight) {
                    shift.start(level, state, nightNo, LabConfig.SHIFT_SECTORS.get());
                }
            }
            shift.tick(level, state);
            grievers.tick(level, day, phase, night);
        }
        box.tick(level, day, phase);
        story.tick(level, day, phase, night);

        if (ticks % 10 == 0) {
            for (ServerPlayer p : level.players()) {
                if (p.isSpectator()) continue;
                int range = state.role(p.getUUID()) == Role.KOSUCU ? 18 : 11;
                explore.reveal(state, p.getX(), p.getZ(), range);
            }
            if (!explore.dirty().isEmpty()) {
                Net.sendMapDelta(level, state, explore.dirty());
                explore.dirty().clear();
            }
        }
        if (ticks % 5 == 0) guardWalls(level);
        if (ticks % 20 == 0 && !level.players().isEmpty()) {
            Net.State snapshot = Net.buildState(this);
            for (ServerPlayer p : level.players()) Net.sendState(p, snapshot);
        }
    }

    /** nobody walks on top of the walls */
    private void guardWalls(ServerLevel level) {
        if (LabConfig.ALLOW_WALL_CLIMB.get()) return;
        for (ServerPlayer p : level.players()) {
            if (p.isCreative() || p.isSpectator()) continue;
            if (p.getY() > MazeLayout.GROUND + MazeLayout.H - 0.2) {
                int i = MazeLayout.unitOf(p.getBlockX()), j = MazeLayout.unitOf(p.getBlockZ());
                if (i < 0 || j < 0) continue;
                int[] open = nearestOpen(i, j);
                p.teleportTo(MazeLayout.mid(open[0]), MazeLayout.GROUND + 1, MazeLayout.mid(open[1]));
                p.fallDistance = 0;
                p.displayClientMessage(Component.translatable("msg.labirent.no_climb"), true);
            }
        }
    }

    public int[] nearestOpen(int i, int j) {
        for (int r = 0; r < 6; r++)
            for (int a = -r; a <= r; a++)
                for (int b = -r; b <= r; b++)
                    if (state.layout.isOpen(i + a, j + b) && MazeLayout.isCell(i + a, j + b)) return new int[]{i + a, j + b};
        return new int[]{MazeLayout.CENTER + 1, MazeLayout.CENTER + 1};
    }

    // ------------------------------------------------------------------ players
    private void login(ServerPlayer p) {
        if (state.captain == null) {
            state.captain = p.getUUID();
            state.captainName = p.getGameProfile().getName();
            state.setDirty();
        }
        state.names.put(p.getUUID(), p.getGameProfile().getName());
        if (!state.roles.containsKey(p.getUUID())) {
            state.roles.put(p.getUUID(), Role.SERBEST);
            state.setDirty();
        }
        if (!LabConfig.AUTO_JOIN.get()) return;
        if (!state.arrived.contains(p.getUUID())) {
            box.queuePlayer(p);
        } else if (p.level().dimension() != Dims.MAZE) {
            sendToGlade(p);
        } else {
            Net.sendFullMap(p, state);
            Net.sendState(p, this);
        }
    }

    public void sendToGlade(ServerPlayer p) {
        ServerLevel level = level();
        if (level == null) return;
        BlockPos s = Glade.SPAWN;
        p.teleportTo(level, s.getX() + 0.5, s.getY(), s.getZ() + 0.5, 180F, 0F);
        p.setRespawnPosition(Dims.MAZE, s, 180F, true, false);
        Net.sendFullMap(p, state);
        Net.sendState(p, this);
    }

    public boolean isCaptain(Player p) { return p.getUUID().equals(state.captain); }

    public void setRole(java.util.UUID id, Role r) {
        state.roles.put(id, r);
        state.setDirty();
        story.onRoleAssigned(r);
    }
}
