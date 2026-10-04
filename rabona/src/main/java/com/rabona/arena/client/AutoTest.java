package com.rabona.arena.client;

import com.rabona.arena.game.Move;

import net.minecraft.world.entity.LivingEntity;

import com.rabona.arena.game.MoveLogic;

import com.rabona.arena.game.Pitch;

import com.rabona.arena.net.Net;

import com.rabona.arena.net.C2S;

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
    private static final java.util.Map<Integer, Vec3> LAST = new java.util.HashMap<>();

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
        server(5, null, false, s -> Match.get(s).manualSwitch(player(s)));
        wait(20, "fifa_switch", true);
        for (int k = 0; k < 4; k++) server(40, null, false, s -> {
            Match m = Match.get(s);
            BallEntity b = m.ball();
            ServerPlayer p = player(s);
            LOG.info("[RTEST] player pos={} ballCtrl={} me={} myPos={}", p.position(), b == null ? -2 : b.getControllerId(), p.getId(), m.posOf(p));
        });
        // faul -> serbest vurus (insan atici) -> olu yaprak
        server(5, null, false, s -> {
            Match m = Match.get(s);
            ServerPlayer p = player(s);
            LOG.info("[RTEST] tactics red={} ({}) blue={} ({})", m.tactic(Team.RED), m.manager(Team.RED).name(), m.tactic(Team.BLUE), m.manager(Team.BLUE).name());
            if (m.phase != Match.Phase.PLAYING) m.phase = Match.Phase.PLAYING;
            int sg = m.attackSign(Team.RED);
            Vec3 at = m.pitch.world(sg * (Pitch.HALF_LEN - 22), 4, m.pitch.surfaceY());
            p.teleportTo(at.x, at.y, at.z);
            LivingEntity off = null;
            for (LivingEntity e : m.members(Team.BLUE)) if (!(e instanceof FootballerEntity f && f.isKeeper())) off = e;
            m.foul(off, p, true, 0.9);
            LOG.info("[RTEST] foul -> phase={} setPiece={} taker={} me={}", m.phase, m.setPiece, m.spTaker, p.getId());
        });
        wait(25, "free_kick_banner", true);
        server(5, null, false, s -> {
            Match m = Match.get(s);
            ServerPlayer p = player(s);
            Vec3 goal = m.pitch.goalCenter(m.attackSign(Team.RED)).add(0, 1.5, 0);
            float yaw = Match.yawToward(p.position(), goal);
            p.connection.teleport(p.getX(), p.getY(), p.getZ(), yaw, -6);
        });
        wait(30, "free_kick_aim", true);
        server(5, null, false, s -> {
            ServerPlayer p = player(s);
            boolean ok = MoveLogic.tryPerform(p, Move.SHOT_DEADLEAF, 0.95f, 1, null);
            LOG.info("[RTEST] free kick deadleaf -> {} phase={}", ok, Match.get(s).phase);
        });
        wait(18, "free_kick_flight", true);
        wait(40, null, false);
        // penalti
        server(5, null, false, s -> {
            Match m = Match.get(s);
            ServerPlayer p = player(s);
            m.phase = Match.Phase.PLAYING;
            m.setPiece = Match.SetPiece.NONE;
            BallEntity b = m.ball();
            if (b != null) b.locked = false;
            int sg = m.attackSign(Team.RED);
            Vec3 at = m.pitch.world(sg * (Pitch.HALF_LEN - 6), 1, m.pitch.surfaceY());
            p.teleportTo(at.x, at.y, at.z);
            LivingEntity off = null;
            for (LivingEntity e : m.members(Team.BLUE)) if (!(e instanceof FootballerEntity f && f.isKeeper())) off = e;
            m.foul(off, p, false, 0.5);
            LOG.info("[RTEST] penalty -> phase={} setPiece={} taker={}", m.phase, m.setPiece, m.spTaker);
        });
        wait(30, "penalty", true);
        server(5, null, false, s -> {
            ServerPlayer p = player(s);
            Match m = Match.get(s);
            Vec3 goal = m.pitch.goalCenter(m.attackSign(Team.RED)).add(0, 1.0, 0);
            p.connection.teleport(p.getX(), p.getY(), p.getZ(), Match.yawToward(p.position(), goal) + 8, 2);
        });
        wait(15, "penalty_aim", true);
        server(5, null, false, s -> LOG.info("[RTEST] penalty shot -> {}", MoveLogic.tryPerform(player(s), Move.SHOT_FINESSE, 0.8f, 1, null)));
        wait(30, "penalty_shot", true);
        wait(30, null, false);
        // oyuncu degisikligi
        server(5, null, false, s -> {
            Match m = Match.get(s);
            if (m.phase == Match.Phase.SET_PIECE) {
                m.phase = Match.Phase.PLAYING;
                m.setPiece = Match.SetPiece.NONE;
            }
            FootballerEntity out = null;
            for (FootballerEntity f : m.bots()) if (f.getSquad() == Team.RED && !f.isKeeper()) out = f;
            String before = out == null ? "-" : out.getBaseName();
            boolean ok = m.substitute(Team.RED, out, 0, player(s));
            LOG.info("[RTEST] substitution {} -> {} ok={} left={}", before, out == null ? "-" : out.getBaseName(), ok, m.subsLeft(Team.RED));
        });
        wait(15, "sub_banner", true);
        client(25, "sub_screen", true, () -> Net.toServer(new C2S.Menu(C2S.Menu.BENCH, 0)));
        client(5, null, false, () -> Minecraft.getInstance().setScreen(null));
        client(30, "cam_tv", true, () -> ClientState.cameraMode = 1);
        client(30, "cam_behind", true, () -> ClientState.cameraMode = 2);
        client(30, "cam_top", true, () -> ClientState.cameraMode = 3);
        client(5, null, false, () -> ClientState.cameraMode = 0);
        // yerel 2. oyuncu: kumanda girdisi sunucuya gonderilmis gibi
        client(5, null, false, () -> ClientState.p2Mode = 1);
        server(5, null, false, s -> Match.get(s).setP2(player(s), 1));
        for (int k = 0; k < 6; k++) server(10, k == 4 ? "p2_markers" : null, true, s -> {
            Match m = Match.get(s);
            FootballerEntity f = m.p2Entity();
            BallEntity b = m.ball();
            if (f == null || b == null) return;
            Vec3 d = b.position().subtract(f.position());
            Vec3 n = new Vec3(d.x, 0, d.z).normalize();
            m.p2Input(player(s), (float) n.x, (float) n.z, true);
            LOG.info("[RTEST] p2 bot {} pos={} dist={}", f.getNumber(), f.position(), f.distanceTo(b));
        });
        server(5, null, false, s -> Match.get(s).setP2(player(s), 0));
        client(5, null, false, () -> ClientState.p2Mode = 0);
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
        server(10, null, false, s -> {
            Cards.buyManager(player(s));
            Cards.buyManager(player(s));
            Cards.autoLineup(player(s));
        });
        client(25, "cards_lineup", true, () -> CardScreen.tab = 2);
        client(25, "cards_managers", true, () -> CardScreen.tab = 3);
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
        LOG.info("[RTEST] ball pos={} ctrl={} phase={} score={}-{} moves={}", b.position(), b.getControllerId(), m.phase, m.scoreRed, m.scoreBlue, MoveLogic.COUNTS);
        double tot = 0;
        int n = 0;
        for (FootballerEntity f : l.getEntitiesOfClass(FootballerEntity.class, new AABB(c, c).inflate(80))) {
            Vec3 prev = LAST.put(f.getId(), f.position());
            if (prev != null) { tot += prev.distanceTo(f.position()); n++; }
        }
        if (n > 0) LOG.info("[RTEST] avg bot distance per 60 ticks = {}", tot / n);
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
        if (mc.screen != null && !(mc.screen instanceof CardScreen || mc.screen instanceof MatchScreen || mc.screen instanceof MoveScreen || mc.screen instanceof SubScreen)) mc.setScreen(null);
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
