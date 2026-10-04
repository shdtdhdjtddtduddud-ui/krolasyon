package com.rabona.arena.client;

import com.mojang.logging.LogUtils;
import com.rabona.arena.entity.BallEntity;
import com.rabona.arena.entity.FootballerEntity;
import com.rabona.arena.game.*;
import com.rabona.arena.registry.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Yalnizca CI gorsel testi (-Drabona.autotest=true): duz dunya, stadyum, botlu mac ve hareket vitrini,
 * ekran goruntuleri alip cikar.
 */
public final class AutoTest {
    private static final Logger LOG = LogUtils.getLogger();
    private static final boolean ON = Boolean.getBoolean("rabona.autotest");
    private static AutoTest INSTANCE;

    record Step(int delay, String shot, boolean hud, Consumer<MinecraftServer> action, Runnable clientAction) {
        Step(int delay, String shot, boolean hud, Consumer<MinecraftServer> action) { this(delay, shot, hud, action, null); }
    }

    private final List<Step> steps = new ArrayList<>();
    private int tick, stepIndex, stepStart, inWorld = -1;
    private boolean worldRequested;
    private static final Vec3 SHOW = new Vec3(0.5, -60, -62.5);

    public static void tick() {
        if (!ON) return;
        if (INSTANCE == null) {
            LOG.info("[RTEST] enabled");
            INSTANCE = new AutoTest();
        }
        INSTANCE.onTick();
    }

    private AutoTest() {
        cmd(0, null, false, "gamerule doDaylightCycle false", "gamerule doWeatherCycle false", "gamerule doMobSpawning false",
                "time set 6000", "weather clear", "gamemode creative @a", "kill @e[type=!player]");
        server(5, null, false, s -> StadiumBuilder.build(s.overworld(), new BlockPos(0, -61, 0), true, player(s)));
        wait(80, null, false);
        server(20, "stadium_overview", false, s -> tp(s, new Vec3(-30, -38, -40), new Vec3(0, -60, 0), 0));
        server(20, "stadium_goal", false, s -> tp(s, new Vec3(26, -55, -8), new Vec3(36, -59, 0), 0));
        server(20, "stands_corner", false, s -> tp(s, new Vec3(-20, -50, 8), new Vec3(-48, -54, 30), 0));
        // botlu mac (mevkiler, paslasma)
        server(10, null, false, s -> {
            Match m = Match.get(s);
            m.teamSize = 6;
            m.durationMin = 5;
            m.fillBots();
            m.start();
        });
        server(60, "kickoff", false, s -> tp(s, new Vec3(0, -52, -24), new Vec3(0, -60, 0), 0));
        wait(80, "play_1", false);
        for (int k = 2; k <= 7; k++) server(60, "play_" + k, k == 5, s -> follow(s));
        server(5, null, false, s -> Match.get(s).join(player(s), Team.RED));
        wait(40, "player_in_team_hud", true);
        client(30, "cam_tv", true, () -> ClientState.cameraMode = 1);
        client(30, "cam_behind", true, () -> ClientState.cameraMode = 2);
        client(30, "cam_top", true, () -> ClientState.cameraMode = 3);
        client(5, null, false, () -> ClientState.cameraMode = 0);
        server(5, null, false, s -> Match.get(s).join(player(s), Team.NONE));
        // taraftarlar
        server(30, "crowd", false, s -> tp(s, new Vec3(-10, -56, -18), new Vec3(-4, -56, -32), 0));
        // zorla gol -> tekrar
        server(5, null, false, s -> {
            Match m = Match.get(s);
            BallEntity b = m.ball();
            if (b == null || m.pitch == null) return;
            b.setController(null, false);
            Vec3 p = m.pitch.world(m.attackSign(Team.RED) * 28, 1, m.pitch.surfaceY() + 0.4);
            b.setPos(p.x, p.y, p.z);
            b.setDeltaMovement(m.pitch.axisA().scale(m.attackSign(Team.RED) * 1.3).add(0, 0.12, 0));
            tp(s, m.pitch.world(m.attackSign(Team.RED) * 20, -12, m.pitch.surfaceY() + 6), p, 0);
        });
        wait(40, "goal_banner", true);
        wait(40, "replay_1", true);
        wait(45, "replay_2", true);
        wait(40, "replay_3", true);
        wait(60, null, false);
        // kartlar
        server(5, null, false, s -> {
            Cards.reward(player(s), 5000, "win");
            Cards.openPack(player(s), Cards.Pack.LEGEND);
        });
        client(130, "cards_reveal", true, () -> Minecraft.getInstance().setScreen(new CardScreen()));
        client(20, "cards_shop", true, () -> ClientState.packReveal = java.util.List.of());
        server(10, null, false, s -> Cards.openPack(player(s), Cards.Pack.GOLD));
        client(20, "cards_collection", true, () -> {
            ClientState.packReveal = java.util.List.of();
            CardScreen.tab = 1;
        });
        client(20, "match_menu", true, () -> Minecraft.getInstance().setScreen(new MatchScreen()));
        client(20, "moves_menu", true, () -> Minecraft.getInstance().setScreen(new MoveScreen()));
        client(5, null, false, () -> Minecraft.getInstance().setScreen(null));
        server(5, null, false, s -> {
            Match m = Match.get(s);
            m.stop();
            m.clearBots();
            s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withSuppressedOutput(), "kill @e[type=rabonaarena:ball]");
        });
        // hareket vitrini
        server(10, null, false, s -> spawnShowBot(s));
        show(Move.BODY_FEINT, 4, true);
        show(Move.CELEB_SHIRT_OFF, 30, false);
        show(Move.SHOT_POWER, 8, true);
        show(Move.SHOT_RABONA, 10, true);
        show(Move.RAINBOW, 6, true);
        show(Move.RAINBOW, 10, true);
        show(Move.ELASTICO, 4, true);
        show(Move.ROULETTE, 6, true);
        show(Move.SEAL, 10, true);
        show(Move.AROUND_WORLD, 12, true);
        showAir(Move.BICYCLE, 8);
        showAir(Move.SCORPION, 7);
        showAir(Move.HEADER, 4);
        show(Move.SLIDE, 7, false);
        show(Move.GK_DIVE_RIGHT, 10, false);
        show(Move.TACKLE, 4, false);
        show(Move.BLOCK, 6, false);
        show(Move.CELEB_SIU, 10, false);
        show(Move.CELEB_SIU, 22, false);
        show(Move.CELEB_KNEESLIDE, 12, false);
        show(Move.CELEB_BACKFLIP, 10, false);
        show(Move.CELEB_PLANE, 15, false);
        show(Move.AB_FIRE, 15, true);
        show(Move.AB_TORNADO, 8, true);
        show(Move.AB_THUNDER, 8, false);
        show(Move.STUMBLE, 4, false);
        server(5, "kit_back", false, s -> {
            FootballerEntity b = showBot(s);
            if (b != null) tp(s, b.position().add(0, 0.2, -2.6), b.position().add(0, 1.1, 0), 0);
        });
        wait(10, "END", false);
    }

    // ---------------------------------------------------------------- adimlar
    private void show(Move m, int at, boolean withBall) {
        server(4, null, false, s -> {
            FootballerEntity b = showBot(s);
            if (b == null) return;
            b.teleportTo(SHOW.x, SHOW.y, SHOW.z);
            b.setYRot(0);
            b.setYHeadRot(0);
            b.yBodyRot = 0;
            b.setDeltaMovement(Vec3.ZERO);
            Athlete a = Athlete.of(b);
            a.current = null;
            a.stun = 0;
            a.stamina = 100;
            a.energy = 100;
            java.util.Arrays.fill(a.cooldowns, 0);
            BallEntity ball = ball(s);
            if (withBall) {
                ball.setController(null, false);
                ball.setPos(SHOW.x, SHOW.y, SHOW.z + 0.75);
                ball.setDeltaMovement(Vec3.ZERO);
                ball.setController(b, false);
            } else {
                ball.setController(null, false);
                ball.setPos(SHOW.x + 6, SHOW.y, SHOW.z - 6);
            }
            tp(s, SHOW.add(3.2, 0.4, 3.0), SHOW.add(0, 1.0, 0), 0);
        });
        server(at, null, false, s -> {
            FootballerEntity b = showBot(s);
            if (b == null) return;
            boolean ok = MoveLogic.tryPerform(b, m, 1, 1, SHOW.add(0, 1.5, 30));
            LOG.info("[RTEST] perform {} -> {}", m, ok);
        });
        wait(0, "move_" + m.key() + "_" + at, false);
        wait(Math.max(10, m.duration - at + 12), null, false);
    }

    private void showAir(Move m, int at) {
        server(4, null, false, s -> {
            FootballerEntity b = showBot(s);
            if (b == null) return;
            b.teleportTo(SHOW.x, SHOW.y, SHOW.z);
            b.setYRot(0);
            b.setYHeadRot(0);
            b.yBodyRot = 0;
            Athlete a = Athlete.of(b);
            a.current = null;
            a.stamina = 100;
            java.util.Arrays.fill(a.cooldowns, 0);
            BallEntity ball = ball(s);
            ball.setController(null, false);
            double h = m == Move.HEADER ? 1.8 : 1.0;
            ball.setPos(SHOW.x, SHOW.y + h, SHOW.z + (m == Move.SCORPION ? -0.6 : 0.9));
            ball.setDeltaMovement(new Vec3(0, 0.15, 0));
            tp(s, SHOW.add(3.4, 0.6, 2.4), SHOW.add(0, 1.2, 0), 0);
            boolean ok = MoveLogic.tryPerform(b, m, 1, 0, SHOW.add(0, 1.5, 30));
            LOG.info("[RTEST] perform air {} -> {}", m, ok);
        });
        wait(at, "move_" + m.key() + "_" + at, false);
        wait(Math.max(12, m.duration - at + 12), null, false);
    }

    private void cmd(int wait, String shot, boolean hud, String... commands) {
        steps.add(new Step(wait, shot, hud, s -> {
            for (String c : commands) s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withSuppressedOutput(), c);
        }));
    }

    private void client(int wait, String shot, boolean hud, Runnable r) {
        steps.add(new Step(wait, shot, hud, null, r));
    }

    private void wait(int ticks, String shot, boolean hud) { steps.add(new Step(ticks, shot, hud, null)); }

    private void server(int wait, String shot, boolean hud, Consumer<MinecraftServer> action) { steps.add(new Step(wait, shot, hud, action)); }

    // ---------------------------------------------------------------- yardimcilar
    private static ServerPlayer player(MinecraftServer s) { return s.getPlayerList().getPlayers().get(0); }

    private static void tp(MinecraftServer s, Vec3 from, Vec3 look, int unused) {
        ServerPlayer p = player(s);
        Vec3 d = look.subtract(from);
        float yaw = Match.yawToward(from, look);
        float pitch = (float) -(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * 180 / Math.PI);
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
        p.teleportTo(s.overworld(), from.x, from.y, from.z, yaw, pitch);
    }

    private static void follow(MinecraftServer s) {
        Match m = Match.get(s);
        BallEntity b = m.ball();
        if (b == null) return;
        Vec3 c = b.position();
        Vec3 cam = c.add(-9, 7, -11);
        tp(s, cam, c, 0);
        ServerLevel l = s.overworld();
        for (FootballerEntity f : l.getEntitiesOfClass(FootballerEntity.class, new AABB(c, c).inflate(60))) {
            Athlete a = Athlete.of(f);
            LOG.info("[RTEST] bot {} team={} slot={} pos={} move={} stamina={}", f.getBaseName(), f.getSquad(), f.getFieldPos(),
                    f.position(), a.current, (int) a.stamina);
        }
        LOG.info("[RTEST] ball pos={} ctrl={} phase={} score={}-{}", b.position(), b.getControllerId(), m.phase, m.scoreRed, m.scoreBlue);
    }

    private static FootballerEntity showBot(MinecraftServer s) {
        List<FootballerEntity> l = s.overworld().getEntitiesOfClass(FootballerEntity.class, new AABB(SHOW, SHOW).inflate(30));
        return l.isEmpty() ? null : l.get(0);
    }

    private static void spawnShowBot(MinecraftServer s) {
        FootballerEntity f = new FootballerEntity(ModEntities.FOOTBALLER.get(), s.overworld());
        f.setup(Team.BLUE, 10, 2, "Vitrin", 90);
        f.setFieldPos(Pos.CM);
        f.moveTo(SHOW.x, SHOW.y, SHOW.z, 0, 0);
        s.overworld().addFreshEntity(f);
    }

    private static BallEntity ball(MinecraftServer s) {
        List<BallEntity> l = s.overworld().getEntitiesOfClass(BallEntity.class, new AABB(SHOW, SHOW).inflate(40));
        if (!l.isEmpty()) return l.get(0);
        BallEntity b = new BallEntity(ModEntities.BALL.get(), s.overworld());
        b.setSkin(0);
        b.moveTo(SHOW.x, SHOW.y, SHOW.z + 1, 0, 0);
        s.overworld().addFreshEntity(b);
        return b;
    }

    // ---------------------------------------------------------------- dongu
    private void onTick() {
        Minecraft mc = Minecraft.getInstance();
        tick++;
        mc.options.pauseOnLostFocus = false;
        if (mc.getOverlay() != null) return;
        if (!worldRequested && tick > 40 && mc.level == null) {
            worldRequested = true;
            LOG.info("[RTEST] creating world");
            LevelSettings settings = new LevelSettings("rtest", GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("rtest", settings, new WorldOptions(42L, false, false),
                    reg -> reg.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
            return;
        }
        if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) return;
        if (mc.screen != null && !(mc.screen instanceof CardScreen || mc.screen instanceof MatchScreen || mc.screen instanceof MoveScreen)) mc.setScreen(null);
        if (inWorld < 0) {
            inWorld = tick;
            stepStart = tick + 100;
            LOG.info("[RTEST] in world");
        }
        if (tick < stepStart || stepIndex >= steps.size()) return;
        Step st = steps.get(stepIndex);
        mc.options.hideGui = !st.hud();
        if (tick == stepStart && st.clientAction() != null) st.clientAction().run();
        if (tick == stepStart && st.action() != null) {
            MinecraftServer s = mc.getSingleplayerServer();
            s.execute(() -> {
                try {
                    st.action().accept(s);
                } catch (Throwable t) {
                    LOG.error("[RTEST] step failed", t);
                }
            });
        }
        if (tick - stepStart >= st.delay()) {
            if (st.shot() != null) {
                if (st.shot().equals("END")) {
                    LOG.info("[RTEST] finished");
                    mc.stop();
                    return;
                }
                String name = "rtest_" + String.format("%02d_", stepIndex) + st.shot() + ".png";
                Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), msg -> LOG.info("[RTEST] screenshot {}", name));
            }
            stepIndex++;
            stepStart = tick + 1;
        }
    }
}
