package com.krolasyon.alien.client;

import com.krolasyon.alien.ai.HidingManager;
import com.krolasyon.alien.block.LockerBlock;
import com.krolasyon.alien.entity.XenomorphEntity;
import com.krolasyon.alien.registry.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** CI-only visual self test (inactive unless -Dalien.autotest=true). */
public final class AutoTest {
    private static final Logger LOG = LogUtils.getLogger();
    private int tick;
    private boolean worldRequested;
    private int inWorld = -1;
    private final List<Step> steps = new ArrayList<>();
    private int stepIndex;
    private int stepStart;

    record Step(int delay, String shot, boolean gui, Consumer<MinecraftServer> action) {}

    public static void init() {
        LOG.info("[AUTOTEST] enabled");
        MinecraftForge.EVENT_BUS.register(new AutoTest());
    }

    private static ServerPlayer player(MinecraftServer s) { return s.getPlayerList().getPlayers().get(0); }

    private static void run(MinecraftServer s, String c) {
        s.getCommands().performPrefixedCommand(player(s).createCommandSourceStack().withSuppressedOutput().withPermission(4), c);
    }

    private AutoTest() {
        cmd(0, null, false, "gamerule doDaylightCycle false", "gamerule doMobSpawning false", "time set 18000", "difficulty normal",
                "gamemode creative @a", "tp @a 0 -60 0 0 0");
        cmd(40, null, false, "sevastopol olustur");
        cmd(60, "room_a", false, "kill @e[type=alienisolation:xenomorph]", "tp @a 0 -60 0 225 0");
        cmd(20, "room_b", false, "tp @a 0 -60 0 45 10");
        // close look at the creature
        cmd(30, "xeno_front", false, "tp @a 0 -60 2 180 -8", "summon alienisolation:xenomorph 0 -60 -2 {NoAI:1b,Rotation:[0f,0f]}");
        cmd(20, "xeno_side", false, "tp @a 3.5 -60 -2 90 -5");
        cmd(20, "xeno_back", false, "tp @a 0 -60 -6 0 -5");
        cmd(20, "xeno_q", false, "tp @a 2.5 -60 0.5 135 -10");
        anim(XenomorphEntity.ANIM_HISS, 13, "anim_hiss");
        anim(XenomorphEntity.ANIM_SEARCH, 22, "anim_search");
        anim(XenomorphEntity.ANIM_ATTACK, 7, "anim_attack");
        anim(XenomorphEntity.ANIM_KILL, 33, "anim_kill");
        anim(XenomorphEntity.ANIM_VENT_EXIT, 8, "anim_vent_exit");
        anim(XenomorphEntity.ANIM_FLINCH, 10, "anim_flinch");
        anim(XenomorphEntity.ANIM_LOCKER, 25, "anim_locker");
        // tracker HUD
        cmd(10, null, false, "kill @e[type=alienisolation:xenomorph]", "tp @a 0 -60 0 0 0");
        server(30, "tracker", true, s -> {
            ServerPlayer p = player(s);
            p.getInventory().setItem(0, new ItemStack(ModItems.MOTION_TRACKER.get()));
            p.getInventory().selected = 0;
            run(s, "summon minecraft:pig ~3 ~ ~8");
            run(s, "summon alienisolation:xenomorph ~-4 ~ ~14");
        });
        // locker view
        server(30, "locker", true, s -> {
            ServerPlayer p = player(s);
            BlockPos me = p.blockPosition();
            for (BlockPos b : BlockPos.betweenClosed(me.offset(-7, -1, -7), me.offset(7, 1, 7))) {
                BlockState st = p.level().getBlockState(b);
                if (st.getBlock() instanceof LockerBlock && st.getValue(LockerBlock.HALF) == DoubleBlockHalf.LOWER) {
                    HidingManager.enter(p, b.immutable());
                    break;
                }
            }
        });
        server(20, null, false, s -> HidingManager.exit(player(s)));
        // a real hunt in survival
        cmd(10, null, false, "kill @e[type=alienisolation:xenomorph]", "kill @e[type=minecraft:pig]", "gamemode survival @a",
                "tp @a 0 -60 0 0 0", "effect give @a minecraft:resistance 99999 4 true");
        cmd(5, null, false, "summon alienisolation:xenomorph 14 -60 0");
        for (int i = 0; i < 14; i++) wait(60, "hunt_" + i, true);
        wait(20, "END", false);
    }

    private void anim(int id, int at, String shot) {
        cmd(2, null, false, "tp @a 2.5 -60 0.5 135 -10");
        server(at, shot, false, s -> s.overworld().getEntitiesOfClass(XenomorphEntity.class, new AABB(-20, -70, -20, 20, -40, 20))
                .forEach(x -> x.debugAnim(id)));
        wait(40, null, false);
    }

    private void cmd(int wait, String shot, boolean gui, String... commands) {
        steps.add(new Step(wait, shot, gui, s -> { for (String c : commands) run(s, c); }));
    }

    private void wait(int ticks, String shot, boolean gui) { steps.add(new Step(ticks, shot, gui, null)); }

    private void server(int wait, String shot, boolean gui, Consumer<MinecraftServer> action) { steps.add(new Step(wait, shot, gui, action)); }

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
            LevelSettings settings = new LevelSettings("atest", GameType.CREATIVE, false, Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("atest", settings, new WorldOptions(42L, false, false),
                    reg -> reg.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
            return;
        }
        if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) return;
        if (mc.screen != null) mc.setScreen(null);
        if (inWorld < 0) {
            inWorld = tick;
            stepStart = tick + 100;
            LOG.info("[AUTOTEST] in world");
        }
        if (tick < stepStart || stepIndex >= steps.size()) return;
        Step st = steps.get(stepIndex);
        mc.options.hideGui = !st.gui();
        if (tick == stepStart && st.action() != null) {
            MinecraftServer s = mc.getSingleplayerServer();
            s.execute(() -> {
                try {
                    st.action().accept(s);
                } catch (Throwable t) {
                    LOG.error("[AUTOTEST] step failed", t);
                }
            });
        }
        if (tick - stepStart >= st.delay()) {
            if (st.shot() != null) {
                MinecraftServer srv = mc.getSingleplayerServer();
                srv.execute(() -> {
                    for (XenomorphEntity x : srv.overworld().getEntitiesOfClass(XenomorphEntity.class, new AABB(-120, -80, -120, 120, 0, 120)))
                        LOG.info("[AUTOTEST] {} alien {}", st.shot(), x.debugState());
                    ServerPlayer p = player(srv);
                    LOG.info("[AUTOTEST] {} player pos={} hp={} alive={}", st.shot(), p.blockPosition().toShortString(), p.getHealth(), p.isAlive());
                });
                if (st.shot().equals("END")) {
                    LOG.info("[AUTOTEST] finished");
                    mc.stop();
                    return;
                }
                Screenshot.grab(mc.gameDirectory, "atest_" + String.format("%02d_", stepIndex) + st.shot() + ".png", mc.getMainRenderTarget(),
                        msg -> LOG.info("[AUTOTEST] screenshot {}", st.shot()));
            }
            stepIndex++;
            stepStart = tick + 1;
        }
    }
}
