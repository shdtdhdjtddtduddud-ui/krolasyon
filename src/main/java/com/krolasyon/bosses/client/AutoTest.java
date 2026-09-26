package com.krolasyon.bosses.client;

import com.krolasyon.bosses.entity.BossEntity;
import com.krolasyon.bosses.entity.CrimsonHoundEntity;
import com.krolasyon.bosses.entity.SealWardenEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
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
import com.mojang.logging.LogUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * CI-only visual self test (inactive unless -Dkrolasyon.autotest=true): creates a flat world, spawns each boss,
 * forces every ability and saves screenshots, then quits.
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

    private static final String TARGET = "summon minecraft:husk 4 -60 -13 {NoAI:1b,PersistenceRequired:1b,Silent:1b,Health:1000f,Attributes:[{Name:\"generic.max_health\",Base:1000d}]}";

    private AutoTest() {
        cmd(0, null, "gamerule doDaylightCycle false", "gamerule doWeatherCycle false", "gamerule doMobSpawning false",
                "time set 6000", "weather clear", "difficulty normal", "gamemode creative @a", "tp @a 1 -60 2 180 -6",
                "kill @e[type=!player]");
        // ---------------- Seal Warden ----------------
        cmd(40, null, TARGET, "summon krolasyonbosses:seal_warden 0 -60 -30 {Rotation:[0f,0f]}");
        wait(18, "warden_run");
        wait(70, "warden_idle_far");
        cmd(20, "warden_front", "tp @a 1 -60 -5 180 -8");
        cmd(20, "warden_side", "tp @a -9 -60 -16 -90 -6");
        cmd(20, "warden_back", "tp @a 2 -60 -27 0 -6");
        cmd(2, null, "tp @a 1 -60 -5 180 -8");
        for (int[] a : new int[][]{{0, 8}, {1, 22}, {2, 18}, {3, 34}, {4, 13}, {5, 32}}) {
            force(SealWardenEntity.class, a[0]);
            wait(a[1], "warden_ability" + a[0]);
            if (a[0] == 1) wait(6, "warden_ability1b");
            if (a[0] == 2) wait(6, "warden_ability2b");
            wait(40, null);
        }
        wait(40, "warden_watchers");
        server(20, null, s -> each(s, SealWardenEntity.class, e -> e.hurt(e.damageSources().generic(), e.getMaxHealth() * 0.55F)));
        wait(30, "warden_phase2");
        server(20, null, s -> each(s, SealWardenEntity.class, LivingEntity::kill));
        wait(28, "warden_death");
        wait(80, null);
        // ---------------- Crimson Hound ----------------
        cmd(10, null, "kill @e[type=!player]", "tp @a 1 -60 2 180 -6", TARGET,
                "summon krolasyonbosses:crimson_hound 0 -60 -32 {Rotation:[0f,0f]}");
        wait(14, "hound_run");
        wait(70, "hound_idle_far");
        cmd(20, "hound_front", "tp @a 1 -60 -5 180 -8");
        cmd(20, "hound_side", "tp @a -10 -60 -17 -90 -6");
        cmd(20, "hound_back", "tp @a 2 -60 -28 0 -8");
        cmd(2, null, "tp @a 1 -60 -5 180 -8");
        for (int[] a : new int[][]{{0, 6}, {1, 12}, {2, 26}, {3, 20}, {4, 16}, {5, 9}}) {
            force(CrimsonHoundEntity.class, a[0]);
            wait(a[1], "hound_ability" + a[0]);
            if (a[0] == 3) wait(8, "hound_ability3b");
            wait(45, null);
        }
        server(10, null, s -> each(s, CrimsonHoundEntity.class, e -> e.hurt(e.damageSources().generic(), e.getMaxHealth() * 0.55F)));
        wait(30, "hound_phase2");
        server(20, null, s -> each(s, CrimsonHoundEntity.class, LivingEntity::kill));
        wait(26, "hound_death");
        // both bosses together with monsters around
        cmd(80, null, "kill @e[type=!player]", "tp @a 0 -60 0 180 -10",
                "summon krolasyonbosses:seal_warden -6 -60 -22 {Rotation:[0f,0f]}",
                "summon krolasyonbosses:crimson_hound 6 -60 -22 {Rotation:[0f,0f]}",
                "summon minecraft:husk 0 -60 -12", "summon minecraft:skeleton -3 -60 -14 {ArmorItems:[{},{},{},{id:\"minecraft:iron_helmet\",Count:1b}]}",
                "summon minecraft:spider 3 -60 -13", "summon minecraft:husk 0 -60 -30", "summon minecraft:husk -8 -60 -12");
        wait(60, "fight_a");
        wait(60, "fight_b");
        wait(60, "fight_c");
        wait(100, "fight_d");
        wait(20, "END");
    }

    private void cmd(int wait, String shot, String... commands) {
        steps.add(new Step(wait, shot, s -> {
            for (String c : commands) s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withSuppressedOutput(), c);
        }));
    }

    private void wait(int ticks, String shot) { steps.add(new Step(ticks, shot, null)); }

    private void server(int wait, String shot, Consumer<MinecraftServer> action) { steps.add(new Step(wait, shot, action)); }

    private <T extends BossEntity> void force(Class<T> cls, int ability) {
        steps.add(new Step(1, null, s -> each(s, cls, b -> {
            LivingEntity target = b.getTarget();
            if (target == null) {
                List<Zombie> z = b.level().getEntitiesOfClass(Zombie.class, b.getBoundingBox().inflate(40));
                if (!z.isEmpty()) target = z.get(0);
            }
            if (target == null) {
                List<net.minecraft.world.entity.monster.Husk> z = b.level().getEntitiesOfClass(net.minecraft.world.entity.monster.Husk.class, b.getBoundingBox().inflate(40));
                if (!z.isEmpty()) target = z.get(0);
            }
            LOG.info("[AUTOTEST] force {} ability {} target {}", b.getName().getString(), ability, target);
            if (target != null) b.debugForceAbility(ability, target);
        })));
    }

    private static <T extends BossEntity> void each(MinecraftServer s, Class<T> cls, Consumer<T> c) {
        ServerLevel l = s.overworld();
        for (T e : l.getEntitiesOfClass(cls, new AABB(-80, -80, -80, 80, 40, 80))) c.accept(e);
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
            LevelSettings settings = new LevelSettings("ktest", GameType.CREATIVE, false, Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("ktest", settings, new WorldOptions(42L, false, false),
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
                srv.execute(() -> each(srv, BossEntity.class, b -> LOG.info("[AUTOTEST] state {} pos={} hp={} target={} ability={} t={} anim={}",
                        b.getName().getString(), b.position(), b.getHealth(), b.getTarget() == null ? null : b.getTarget().getName().getString(),
                        b.debugAbility(), b.debugAbilityTick(), st.shot())));
                if (st.shot().equals("END")) {
                    LOG.info("[AUTOTEST] finished");
                    mc.stop();
                    return;
                }
                Screenshot.grab(mc.gameDirectory, "ktest_" + String.format("%02d_", stepIndex) + st.shot() + ".png", mc.getMainRenderTarget(),
                        msg -> LOG.info("[AUTOTEST] screenshot {}", st.shot()));
            }
            stepIndex++;
            stepStart = tick + 1;
        }
    }
}
