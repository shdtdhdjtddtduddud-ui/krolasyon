package com.krolasyon.bosses.client;

import com.krolasyon.bosses.entity.BossEntity;
import com.krolasyon.bosses.entity.CrimsonHoundEntity;
import com.krolasyon.bosses.entity.SealWardenEntity;
import com.krolasyon.bosses.morph.MorphServer;
import com.krolasyon.bosses.morph.Forms;
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

    private static final String TARGET = "summon minecraft:husk %s -60 %s {NoAI:1b,PersistenceRequired:1b,Silent:1b,Health:1000f,Attributes:[{Name:\"generic.max_health\",Base:1000d}]}";
    private boolean showGui;

    private static final int[][][] SHOTS = {
            {{5, 11}, {20, 50}, {16, 24}, {30}, {16, 24}},
            {{8, 16}, {12, 24}, {14, 31, 36}, {8, 14}, {14, 40, 55}},
            {{14, 30}, {13, 22}, {8, 20}, {10, 22}, {20, 35}},
            {{4, 10}, {14, 40}, {10, 16, 24}, {6, 15}, {20, 50, 74}},
            {{4, 9}, {9, 19}, {10, 18}, {14, 22}, {12, 20}},
            {{10, 16}, {10, 17}, {10, 18}, {14, 26}, {20, 40}}};
    private static final String[] ITEMS = {"tide_blade", "solar_greatsword", "dragon_glaive", "ember_blade", "shade_blade", "hell_trident"};

    private AutoTest() {
        cmd(0, null, "gamerule doDaylightCycle false", "gamerule doWeatherCycle false", "gamerule doMobSpawning false",
                "time set 6000", "weather clear", "difficulty normal", "gamemode survival @a", "tp @a 0 -60 0 180 5",
                "kill @e[type=!player]", "effect give @a minecraft:resistance 99999 4 true", "effect give @a minecraft:saturation 99999 1 true");
        for (int f = 1; f < Forms.COUNT; f++) formRun(f);
        formRun(0);
        wait(20, "END");
    }

    private void formRun(int f) {
        String k = Forms.KEY[f];
        cmd(2, null, "clear @a", "give @a krolasyonbosses:" + ITEMS[f], "tp @a 0 -60 0 180 5", "kill @e[type=!player]");
        client(10, k + "_item", mc -> { mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT); showGui = true; });
        client(1, null, mc -> showGui = false);
        targets();
        player(5, null, p -> MorphServer.transform(p, f));
        wait(8, k + "_cocoon");
        wait(14, k + "_burst");
        wait(20, k + "_roar");
        wait(30, k + "_idle_front");
        cmd(15, k + "_idle_side", "tp @a 0 -60 0 120 5");
        client(15, k + "_idle_back", mc -> mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK));
        client(2, null, mc -> mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT));
        cmd(5, null, "tp @a 0 -60 0 180 5", "kill @e[type=minecraft:husk]");
        client(1, null, mc -> { mc.options.keyUp.setDown(true); mc.options.keySprint.setDown(true); });
        wait(12, k + "_run");
        client(2, null, mc -> { mc.options.keyUp.setDown(false); mc.options.keySprint.setDown(false); });
        cmd(10, null, "tp @a 0 -60 0 180 5");
        client(3, null, mc -> mc.options.keyJump.setDown(true));
        client(4, k + "_jump", mc -> mc.options.keyJump.setDown(false));
        wait(20, null);
        for (int id = 0; id < 5; id++) ability(f, id, SHOTS[f][id], "tp @a 0 -60 0 165 8");
        client(10, null, mc -> { mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON); showGui = true; });
        cmd(12, k + "_first_person", "tp @a 0 -60 0 180 0");
        client(5, null, mc -> { showGui = false; mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT); });
        player(15, k + "_revert", MorphServer::revert);
        wait(20, null);
    }

    private void targets() {
        cmd(2, null, String.format(TARGET, "0", "-9"), String.format(TARGET, "-3", "-12"), String.format(TARGET, "3", "-13"),
                String.format(TARGET, "0", "-18"), String.format(TARGET, "-5", "-20"), String.format(TARGET, "5", "-19"));
    }

    private void ability(int f, int id, int[] shots, String tp) {
        cmd(5, null, "kill @e[type=minecraft:husk]", tp);
        targets();
        client(5, null, mc -> mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK));
        player(1, null, p -> MorphServer.debugForce(p, id));
        int last = 0;
        for (int s : shots) {
            wait(s - last, Forms.KEY[f] + "_a" + id + "_t" + s);
            last = s;
        }
        wait(Math.max(20, 60 - last), null);
        cmd(1, null, "kill @e[type=krolasyonbosses:zone]", "kill @e[type=krolasyonbosses:form_projectile]");
        client(2, null, mc -> mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT));
    }

    private void player(int wait, String shot, Consumer<net.minecraft.server.level.ServerPlayer> c) {
        steps.add(new Step(wait, shot, s -> { for (net.minecraft.server.level.ServerPlayer p : s.getPlayerList().getPlayers()) c.accept(p); }));
    }

    private void client(int wait, String shot, Consumer<Minecraft> c) {
        steps.add(new Step(wait, shot, s -> Minecraft.getInstance().execute(() -> c.accept(Minecraft.getInstance()))));
    }

    private <T extends BossEntity> void bossRun(String id, Class<T> cls, int[][] abilities) {
        cmd(10, null, "kill @e[type=!player]", "tp @a 1 -60 2 180 -6", TARGET,
                "summon krolasyonbosses:" + id + " 0 -60 -30 {Rotation:[0f,0f]}");
        wait(18, id + "_run");
        wait(70, id + "_idle_far");
        cmd(20, id + "_front", "tp @a 1 -60 -5 180 -8");
        cmd(20, id + "_side", "tp @a -9 -60 -16 -90 -6");
        cmd(20, id + "_back", "tp @a 2 -60 -27 0 -6");
        cmd(2, null, "tp @a 1 -60 -5 180 -8");
        int last = -1;
        for (int[] a : abilities) {
            if (a[0] != last) force(cls, a[0]);
            wait(a[1], id + "_ability" + a[0] + (a[0] == last ? "b" : ""));
            last = a[0];
            wait(40, null);
        }
        server(10, null, s -> each(s, cls, e -> e.hurt(e.damageSources().generic(), e.getMaxHealth() * 0.55F)));
        wait(30, id + "_phase2");
        server(20, null, s -> each(s, cls, LivingEntity::kill));
        wait(28, id + "_death");
        wait(70, null);
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
        mc.options.hideGui = !showGui;
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
