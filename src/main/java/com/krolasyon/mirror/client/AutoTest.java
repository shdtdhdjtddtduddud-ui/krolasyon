package com.krolasyon.mirror.client;

import com.krolasyon.mirror.MirrorWorld;
import com.krolasyon.mirror.block.MirrorBlock;
import com.krolasyon.mirror.block.MirrorBlockEntity;
import com.krolasyon.mirror.entity.ReflectionEntity;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.Difficulty;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * CI-only visual self test (inactive unless -Dmirrorworld.autotest=true): places a mirror, walks it through every
 * phase, visits the mirror dimension, breaks the mirror and saves screenshots, then quits.
 */
public final class AutoTest {
    private static final Logger LOG = LogUtils.getLogger();
    private static final BlockPos MIRROR = new BlockPos(0, -60, 0);
    private int tick;
    private boolean worldRequested;
    private int inWorld = -1;
    private final List<Step> steps = new ArrayList<>();
    private int stepIndex;
    private int stepStart;
    private volatile boolean pending;

    record Step(int delay, String shot, Consumer<MinecraftServer> action) {}

    public static void init() {
        LOG.info("[AUTOTEST] enabled");
        MinecraftForge.EVENT_BUS.register(new AutoTest());
    }

    private AutoTest() {
        cmd(0, null, "gamerule doDaylightCycle false", "gamerule doWeatherCycle false", "gamerule doMobSpawning false",
                "time set 6000", "weather clear", "gamemode creative @a", "tp @a 0.5 -60 3.5 180 0", "kill @e[type=!player]");
        server(20, null, s -> {
            ServerLevel l = s.overworld();
            BlockState st = MirrorWorld.MIRROR.get().defaultBlockState().setValue(MirrorBlock.FACING, Direction.SOUTH);
            l.setBlock(MIRROR, st.setValue(MirrorBlock.HALF, DoubleBlockHalf.LOWER), 3);
            l.setBlock(MIRROR.above(), st.setValue(MirrorBlock.HALF, DoubleBlockHalf.UPPER), 3);
            ServerPlayer p = s.getPlayerList().getPlayers().get(0);
            be(s).setOwner(p.getUUID(), p.getGameProfile().getName());
            // the mirror hangs on a small wall: what it shows must not need any room behind it
            for (int x = -1; x <= 1; x++) for (int y = 0; y < 3; y++) {
                l.setBlockAndUpdate(new BlockPos(x, -60 + y, -1), net.minecraft.world.level.block.Blocks.STONE_BRICKS.defaultBlockState());
            }
        });
        wait(40, "mimic_front");
        cmd(20, "mimic_angle", "tp @a 0.8 -60 2.4 155 -5");
        server(3, "mimic_swing", s -> s.getPlayerList().getPlayers().get(0).swing(InteractionHand.MAIN_HAND, true));
        cmd(20, "mimic_close", "tp @a 0.5 -60 1.6 180 10");
        cmd(2, null, "tp @a 0.5 -60 2.6 180 0");
        server(1, null, s -> be(s).debugAdvance());
        for (int i = 0; i < 6; i++) wait(45, "awaken_" + i);
        cmd(2, null, "tp @a 0.5 -60 3.2 180 22");
        server(25, "request_cobble", s -> be(s).debugAdvance());
        server(20, "clearing_land", s -> be(s).debugAdvance());
        for (String name : new String[]{"logs", "glass", "torch"}) {
            server(30, "request_" + name, s -> be(s).debugBuildUntilPause());
            server(20, "thanks_" + name, s -> be(s).debugAdvance());
            wait(60, "building_after_" + name);
        }
        server(30, "done", s -> be(s).debugBuildUntilPause());
        cmd(20, "done_side", "tp @a 2.2 -60 2.2 140 -5");
        cmd(60, "mirror_world_front", "effect give @a night_vision 999 0 true",
                "execute in mirrorworld:mirror run tp @a 0.5 -60 -1.6 180 -12");
        cmd(40, "mirror_world_inside", "execute in mirrorworld:mirror run tp @a 0.5 -60 -4.5 180 5");
        cmd(60, null, "execute in minecraft:overworld run tp @a 0.5 -60 3.5 180 0");
        server(3, "breaking", s -> s.overworld().destroyBlock(MIRROR, false));
        wait(80, "merged_front");
        cmd(30, "merged_far", "tp @a 9.5 -54 9.5 140 22");
        cmd(30, "merged_inside", "tp @a 0.5 -60 -4.5 180 5");
        server(20, null, s -> {
            ServerLevel l = s.overworld();
            List<ReflectionEntity> r = l.getEntitiesOfClass(ReflectionEntity.class, new AABB(-30, -70, -30, 30, 0, 30));
            LOG.info("[AUTOTEST] reflections after merge: {} at {}", r.size(), r.isEmpty() ? null : r.get(0).position());
            int found = 0;
            for (BlockPos p : BlockPos.betweenClosed(-4, -61, -11, 4, -52, -1)) {
                if (l.getBlockEntity(p) instanceof ChestBlockEntity c) {
                    found++;
                    LOG.info("[AUTOTEST] merged chest at {} first item {}", p, c.getItem(0));
                }
            }
            LOG.info("[AUTOTEST] merged chests: {}, block at door {}", found, l.getBlockState(new BlockPos(0, -60, -3)));
        });
        cmd(40, "reflection_walks", "tp @a 0.5 -60 4.5 180 5");
        wait(20, "END");
    }

    private static MirrorBlockEntity be(MinecraftServer s) {
        if (s.overworld().getBlockEntity(MIRROR) instanceof MirrorBlockEntity be) return be;
        throw new IllegalStateException("mirror missing");
    }

    private void cmd(int wait, String shot, String... commands) {
        steps.add(new Step(wait, shot, s -> {
            for (String c : commands) s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withSuppressedOutput(), c);
        }));
    }

    private void wait(int ticks, String shot) { steps.add(new Step(ticks, shot, null)); }

    private void server(int wait, String shot, Consumer<MinecraftServer> action) { steps.add(new Step(wait, shot, action)); }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        tick++;
        mc.options.pauseOnLostFocus = false;
        if (mc.getOverlay() != null) return;
        if (!worldRequested && tick > 40 && mc.level == null) {
            worldRequested = true;
            LOG.info("[AUTOTEST] creating world");
            LevelSettings settings = new LevelSettings("mtest", GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("mtest", settings, new WorldOptions(42L, false, false),
                    reg -> reg.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
            return;
        }
        if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) return;
        if (mc.screen != null) mc.setScreen(null);
        mc.options.hideGui = true;
        if (inWorld < 0) {
            inWorld = tick;
            stepStart = tick + 100;
            LOG.info("[AUTOTEST] in world");
        }
        if (tick < stepStart || stepIndex >= steps.size()) return;
        Step st = steps.get(stepIndex);
        if (tick == stepStart && st.action() != null) {
            MinecraftServer s = mc.getSingleplayerServer();
            pending = true;
            s.execute(() -> {
                try {
                    st.action().accept(s);
                } catch (Throwable t) {
                    LOG.error("[AUTOTEST] step failed", t);
                }
                pending = false;
            });
        }
        if (pending) {
            // the server may stall (e.g. generating mirror chunks): count the delay from when the action ran
            stepStart = tick;
            return;
        }
        if (tick - stepStart >= st.delay()) {
            if (st.shot() != null) {
                MinecraftServer srv = mc.getSingleplayerServer();
                srv.execute(() -> {
                    if (srv.overworld().getBlockEntity(MIRROR) instanceof MirrorBlockEntity b) {
                        LOG.info("[AUTOTEST] state shot={} phase={} stage={} need={} placed={} house={} changed={}", st.shot(), b.getPhase(),
                                b.getStage(), b.getNeed(), b.getPlaced(), b.houseShown(), b.changedPositions().length);
                    } else {
                        LOG.info("[AUTOTEST] state shot={} (no mirror)", st.shot());
                    }
                });
                if (st.shot().equals("END")) {
                    LOG.info("[AUTOTEST] finished");
                    mc.stop();
                    return;
                }
                Screenshot.grab(mc.gameDirectory, "mtest_" + String.format("%02d_", stepIndex) + st.shot() + ".png", mc.getMainRenderTarget(),
                        msg -> LOG.info("[AUTOTEST] screenshot {}", st.shot()));
            }
            stepIndex++;
            stepStart = tick + 1;
        }
    }
}
