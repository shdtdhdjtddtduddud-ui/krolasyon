package com.krolasyon.cille.client;

import com.krolasyon.cille.entity.CilleEntity;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
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

/**
 * CI-only visual self test (inactive unless -Dcille.autotest=true): flat world, every Cille is summoned, shown
 * from several sides, forced through each technique and screenshotted, then the game quits.
 */
public final class AutoTest {
    private static final Logger LOG = LogUtils.getLogger();
    private int tick;
    private boolean worldRequested;
    private int inWorld = -1;
    private final List<Step> steps = new ArrayList<>();
    private int stepIndex;
    private int stepStart;

    record Step(int delay, String shot, Consumer<MinecraftServer> action) {}

    public static void init() {
        LOG.info("[AUTOTEST] enabled");
        MinecraftForge.EVENT_BUS.register(new AutoTest());
    }

    private AutoTest() {
        cmd(0, null, "gamerule doDaylightCycle false", "gamerule doWeatherCycle false", "gamerule doMobSpawning false",
                "time set 5000", "weather clear", "difficulty normal", "gamemode creative @a", "kill @e[type=!player]");
        cille("kabkaba", 0, new int[][]{{1, 40}, {2, 18}, {3, 26}, {4, 20}, {5, 28}, {6, 16}, {7, 28}}, 0);
        cille("behmud", 70, new int[][]{{1, 26}, {2, 40}, {3, 14}, {4, 22}, {5, 14}, {6, 8}, {7, 42}, {8, 16}, {9, 6}, {10, 22}, {11, 45}, {12, 32}}, 4);
        cille("sarkan", 140, new int[][]{{1, 40}, {2, 24}, {3, 20}, {4, 32}, {5, 26}, {6, 16}, {7, 22}, {8, 26}}, 0);
        cille("nirumend", 210, new int[][]{{1, 30}, {2, 26}, {3, 16}, {4, 26}, {5, 16}, {6, 32}, {7, 26}}, 0);

        // two marbles thrown at each other: they burst and the Cilles come out
        int x = 280;
        cmd(30, null, "kill @e[type=!player]", "tp @a " + x + " -60 -6 180 10");
        cmd(40, null, "summon cille:marble " + (x - 4) + " -59 -16 {Motion:[0.32d,0.15d,0.0d]}", "summon cille:marble " + (x + 4) + " -59 -16 {Motion:[-0.32d,0.15d,0.0d]}");
        wait(10, "marble_flight");
        wait(8, "marble_burst");
        wait(18, "marble_release");
        wait(40, "marble_after");

        // group shot of all four
        cmd(30, null, "kill @e[type=!player]", "tp @a " + x + " -60 6 180 -4",
                "summon cille:kabkaba " + (x - 9) + " -60 -18 {Rotation:[20f,0f],NoAI:1b}",
                "summon cille:sarkan " + (x - 3) + " -60 -14 {Rotation:[5f,0f],NoAI:1b}",
                "summon cille:nirumend " + (x + 6) + " -60 -20 {Rotation:[-15f,0f],NoAI:1b}",
                "summon cille:behmud " + (x + 1) + " -54 -26 {Rotation:[0f,0f],NoAI:1b}");
        wait(60, "group");
        cmd(20, "group_low", "tp @a " + x + " -61 2 180 -14");
        wait(20, "END");
    }

    private void cille(String id, int x, int[][] abilities, int fly) {
        String target = "summon minecraft:husk " + (x + 5) + " -60 -14 {NoAI:1b,PersistenceRequired:1b,Silent:1b,Health:1000f,Attributes:[{Name:\"generic.max_health\",Base:1000d}]}";
        cmd(30, null, "kill @e[type=!player]", "tp @a " + x + " -60 4 180 " + (fly > 0 ? -12 : -6));
        cmd(40, null, target, "summon cille:" + id + " " + x + " " + (-60 + fly) + " -30 {Rotation:[0f,0f]}");
        wait(14, id + "_release");
        wait(60, id + "_idle");
        cmd(20, id + "_front", "tp @a " + (x + 1) + " -60 -10 180 " + (fly > 0 ? -18 : -8));
        cmd(20, id + "_side", "tp @a " + (x - 12) + " -60 -24 -90 " + (fly > 0 ? -14 : -6));
        cmd(20, id + "_back", "tp @a " + (x + 2) + " -60 -42 0 " + (fly > 0 ? -14 : -6));
        cmd(4, null, "tp @a " + (x + 13) + " -59 -6 150 -8");
        for (int[] a : abilities) {
            force(a[0]);
            wait(a[1], id + "_ability" + a[0]);
            wait(45, null);
        }
        server(10, null, s -> each(s, e -> e.hurt(e.damageSources().generic(), e.getMaxHealth() * 0.55F)));
        wait(30, id + "_phase2");
        server(20, null, s -> each(s, LivingEntity::kill));
        wait(30, id + "_death");
        wait(40, null);
    }

    private void cmd(int wait, String shot, String... commands) {
        steps.add(new Step(wait, shot, s -> {
            for (String c : commands) s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withSuppressedOutput(), c);
        }));
    }

    private void wait(int ticks, String shot) { steps.add(new Step(ticks, shot, null)); }

    private void server(int wait, String shot, Consumer<MinecraftServer> action) { steps.add(new Step(wait, shot, action)); }

    private void force(int ability) {
        steps.add(new Step(1, null, s -> each(s, b -> {
            LivingEntity target = null;
            List<Husk> z = b.level().getEntitiesOfClass(Husk.class, b.getBoundingBox().inflate(40));
            if (!z.isEmpty()) target = z.get(0);
            LOG.info("[AUTOTEST] force {} ability {} target {}", b.getName().getString(), ability, target);
            b.debugForceAbility(ability, target);
        })));
    }

    private static void each(MinecraftServer s, Consumer<CilleEntity> c) {
        ServerLevel l = s.overworld();
        for (CilleEntity e : l.getEntitiesOfClass(CilleEntity.class, new AABB(-200, -80, -200, 400, 60, 200))) c.accept(e);
    }

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
            LevelSettings settings = new LevelSettings("ctest", GameType.CREATIVE, false, Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("ctest", settings, new WorldOptions(42L, false, false),
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
                srv.execute(() -> each(srv, b -> LOG.info("[AUTOTEST] state {} pos={} hp={} target={} ability={} shot={}",
                        b.getName().getString(), b.position(), b.getHealth(), b.getTarget() == null ? null : b.getTarget().getName().getString(),
                        b.currentAbility(), st.shot())));
                if (st.shot().equals("END")) {
                    LOG.info("[AUTOTEST] finished");
                    mc.stop();
                    return;
                }
                Screenshot.grab(mc.gameDirectory, "ctest_" + String.format("%03d_", stepIndex) + st.shot() + ".png", mc.getMainRenderTarget(),
                        msg -> LOG.info("[AUTOTEST] screenshot {}", st.shot()));
            }
            stepIndex++;
            stepStart = tick + 1;
        }
    }
}
