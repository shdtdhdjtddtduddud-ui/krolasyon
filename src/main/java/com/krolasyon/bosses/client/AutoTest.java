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
        playerRun();
        if (!Boolean.getBoolean("krolasyon.autotest.bosses")) {
            wait(20, "END");
            return;
        }
        bossRun("revenge", com.krolasyon.bosses.entity.RevengeEntity.class, new int[][]{{0, 10}, {1, 18}, {2, 18}, {3, 14}, {4, 26}, {5, 30}, {5, 46}});
        bossRun("heart_demon", com.krolasyon.bosses.entity.HeartDemonEntity.class, new int[][]{{0, 8}, {1, 11}, {2, 22}, {3, 9}, {4, 16}, {5, 14}});
        // all four bosses brawling with monsters around
        cmd(80, null, "kill @e[type=!player]", "tp @a 0 -60 2 180 -10",
                "summon krolasyonbosses:revenge -7 -60 -22 {Rotation:[0f,0f]}",
                "summon krolasyonbosses:heart_demon 7 -60 -22 {Rotation:[0f,0f]}",
                "summon krolasyonbosses:seal_warden -3 -60 -34 {Rotation:[0f,0f]}",
                "summon krolasyonbosses:crimson_hound 4 -60 -34 {Rotation:[0f,0f]}",
                "summon minecraft:husk 0 -60 -12", "summon minecraft:skeleton -3 -60 -14 {ArmorItems:[{},{},{},{id:\"minecraft:iron_helmet\",Count:1b}]}",
                "summon minecraft:spider 3 -60 -13", "summon minecraft:husk -8 -60 -12");
        wait(60, "fight_a");
        wait(60, "fight_b");
        wait(80, "fight_c");
        wait(100, "fight_d");
        wait(20, "END");
    }

    private static final String DUMMY = " {NoAI:1b,PersistenceRequired:1b,Silent:1b,Health:1000f,Attributes:[{Name:\"generic.max_health\",Base:1000d}]}";

    /** the player boss form: transformation, every ability, claws in first person and the HUD */
    private void playerRun() {
        cmd(10, null, "kill @e[type=!player]", "tp @a 0 -60 0 180 8", "clear @a",
                "summon minecraft:husk 0 -60 -7" + DUMMY, "summon minecraft:husk -3 -60 -9" + DUMMY, "summon minecraft:husk 3 -60 -10" + DUMMY,
                "give @a krolasyonbosses:heartbreaker_blade");
        client(2, null, mc -> mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT));
        wait(20, "player_human");
        player(1, null, com.krolasyon.bosses.form.DemonForm::transform);
        wait(4, "player_transform_a");
        wait(8, "player_transform_b");
        wait(8, "player_transform_c");
        wait(40, "player_demon_front");
        client(2, null, mc -> mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK));
        cmd(2, null, "tp @a 0 -60 4 200 15");
        wait(10, "player_demon_back");
        int[][] shots = {{0, 9}, {1, 20}, {2, 8}, {3, 14}, {4, 12}};
        for (int[] a : shots) {
            final int slot = a[0];
            cmd(2, null, "tp @a 0 -60 3 180 10");
            player(1, null, p -> com.krolasyon.bosses.form.DemonForm.tryAbility(p, slot));
            wait(a[1], "player_ability" + slot);
            if (slot == 4) wait(14, "player_ability4_slam");
            wait(70, null);
        }
        client(2, null, mc -> { showGui = true; mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON); });
        cmd(2, null, "tp @a 0 -60 0 180 10");
        wait(20, "player_first_person_hud");
        client(1, null, mc -> { if (mc.player != null) mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND); });
        wait(2, "player_first_person_swing");
        client(2, null, mc -> { showGui = false; mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT); });
        client(1, null, mc -> { if (mc.player != null) mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND); });
        wait(3, "player_slash");
        client(1, null, mc -> { if (mc.player != null) mc.player.jumpFromGround(); });
        wait(5, "player_jump");
        client(1, null, mc -> { if (mc.player != null) mc.player.setDeltaMovement(0, 0.9, 0); });
        player(1, com.krolasyon.bosses.form.DemonForm::airJump);
        wait(4, "player_flip");
        wait(40, null);
        player(1, p -> com.krolasyon.bosses.form.DemonForm.revert(p, true));
        wait(8, "player_revert");
        wait(30, null);
    }

    private boolean showGui;

    private void player(int wait, Consumer<net.minecraft.server.level.ServerPlayer> c) { player(wait, null, c); }

    private void player(int wait, String shot, Consumer<net.minecraft.server.level.ServerPlayer> c) {
        steps.add(new Step(wait, shot, s -> {
            for (net.minecraft.server.level.ServerPlayer p : s.getPlayerList().getPlayers()) c.accept(p);
        }));
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
                srv.execute(() -> srv.getPlayerList().getPlayers().forEach(p -> LOG.info("[AUTOTEST] player {} demon={} hp={}/{} pos={} shot={}",
                        p.getName().getString(), com.krolasyon.bosses.form.DemonForm.isDemon(p), p.getHealth(), p.getMaxHealth(), p.position(), st.shot())));
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
