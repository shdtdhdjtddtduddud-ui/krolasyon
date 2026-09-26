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

    record Step(int wait, String shot, Consumer<MinecraftServer> action) {}

    public static void init() {
        LOG.info("[AUTOTEST] enabled");
        MinecraftForge.EVENT_BUS.register(new AutoTest());
    }

    private AutoTest() {
        cmd(0, null, "gamerule doDaylightCycle false", "gamerule doWeatherCycle false", "gamerule doMobSpawning false",
                "time set 6000", "weather clear", "difficulty normal", "gamemode creative @a", "tp @a 0 -60 0 180 -8",
                "kill @e[type=!player]");
        // ---------------- Seal Warden ----------------
        cmd(40, null, "summon minecraft:zombie 3 -60 -7 {NoAI:1b,Invulnerable:1b,PersistenceRequired:1b,Silent:1b}",
                "summon krolasyonbosses:seal_warden 0 -60 -24 {Rotation:[0f,0f]}");
        wait(16, "warden_run");
        wait(70, "warden_idle");
        for (int[] a : new int[][]{{0, 8}, {1, 24}, {2, 18}, {3, 34}, {4, 13}, {5, 32}}) {
            force(SealWardenEntity.class, a[0], 60);
            wait(a[1], "warden_ability" + a[0]);
            if (a[0] == 1) wait(6, "warden_ability1b");
        }
        wait(60, "warden_watchers");
        server(20, null, s -> each(s, SealWardenEntity.class, e -> e.hurt(e.damageSources().generic(), e.getMaxHealth() * 0.55F)));
        wait(30, "warden_phase2");
        server(20, null, s -> each(s, SealWardenEntity.class, LivingEntity::kill));
        wait(28, "warden_death");
        wait(80, null);
        // ---------------- Crimson Hound ----------------
        cmd(10, null, "kill @e[type=!player]",
                "summon minecraft:zombie 3 -60 -7 {NoAI:1b,Invulnerable:1b,PersistenceRequired:1b,Silent:1b}",
                "summon krolasyonbosses:crimson_hound 0 -60 -26 {Rotation:[0f,0f]}");
        wait(14, "hound_run");
        wait(70, "hound_idle");
        cmd(2, null, "tp @a 8 -60 -3 140 -6");
        wait(20, "hound_side");
        cmd(2, null, "tp @a 0 -60 0 180 -8");
        for (int[] a : new int[][]{{0, 6}, {1, 12}, {2, 26}, {3, 20}, {4, 16}, {5, 9}}) {
            force(CrimsonHoundEntity.class, a[0], 70);
            wait(a[1], "hound_ability" + a[0]);
        }
        wait(40, null);
        server(10, null, s -> each(s, CrimsonHoundEntity.class, e -> e.hurt(e.damageSources().generic(), e.getMaxHealth() * 0.55F)));
        wait(30, "hound_phase2");
        server(20, null, s -> each(s, CrimsonHoundEntity.class, LivingEntity::kill));
        wait(26, "hound_death");
        // both bosses together with monsters around
        cmd(80, null, "kill @e[type=!player]",
                "summon krolasyonbosses:seal_warden -6 -60 -22 {Rotation:[0f,0f]}",
                "summon krolasyonbosses:crimson_hound 6 -60 -22 {Rotation:[0f,0f]}",
                "summon minecraft:zombie 0 -60 -12", "summon minecraft:skeleton -3 -60 -14", "summon minecraft:spider 3 -60 -13",
                "summon minecraft:zombie 0 -60 -30", "summon minecraft:husk -8 -60 -12");
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

    private <T extends BossEntity> void force(Class<T> cls, int ability, int lockTicks) {
        steps.add(new Step(1, null, s -> each(s, cls, b -> {
            LivingEntity target = b.getTarget();
            if (target == null) {
                List<Zombie> z = b.level().getEntitiesOfClass(Zombie.class, b.getBoundingBox().inflate(40));
                if (!z.isEmpty()) target = z.get(0);
            }
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
        if (tick - stepStart >= st.wait()) {
            if (st.shot() != null) {
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
