package com.krolasyon.futbol.client;

import com.krolasyon.futbol.entity.FootballEntity;
import com.krolasyon.futbol.entity.FootballerEntity;
import com.krolasyon.futbol.game.CardData;
import com.krolasyon.futbol.game.MatchManager;
import com.krolasyon.futbol.game.Move;
import com.krolasyon.futbol.game.MoveExecutor;
import com.krolasyon.futbol.game.Team;
import com.krolasyon.futbol.registry.ModEntities;
import com.mojang.logging.LogUtils;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** CI-only visual self test (inactive unless -Dkrolasyon.autotest=true). */
public final class AutoTest {
    private static final Logger LOG = LogUtils.getLogger();
    private int tick;
    private boolean worldRequested;
    private int inWorld = -1;
    private final List<Step> steps = new ArrayList<>();
    private int stepIndex;
    private int stepStart;
    private boolean fly = true;
    private boolean gui;
    private CameraType camera = CameraType.FIRST_PERSON;

    record Step(int delay, String shot, Consumer<MinecraftServer> action, Runnable client) {}

    public static void init() {
        LOG.info("[AUTOTEST] enabled");
        MinecraftForge.EVENT_BUS.register(new AutoTest());
    }

    private AutoTest() {
        cmd(0, null, "gamerule doDaylightCycle false", "gamerule doWeatherCycle false", "gamerule doMobSpawning false",
                "time set 6000", "weather clear", "gamemode creative @a", "tp @a 0 -60 0 0 0");
        cmd(20, null, "execute as @p at @p run futbol saha");
        wait(80, null);
        cmd(40, "stadium_side", "tp @a 0 -44 -40 0 28");
        cmd(30, "crowd_close", "tp @a -10 -55 -14 200 8");
        // animation showcase with posed bots
        showcase(new Move[]{Move.BODY_FEINT, Move.SHIRT_OFF, Move.TRIVELA, Move.SLIDE}, "a");
        showcase(new Move[]{Move.SHOT, Move.RAINBOW, Move.BICYCLE, Move.DIVE}, "b");
        // the local player wearing a kit and animating (third person front)
        cmd(5, null, "kill @e[type=krolasyonfutbol:footballer]", "kill @e[type=krolasyonfutbol:football]", "execute as @p run futbol takim kirmizi",
                "gamemode survival @a", "tp @a 0 -60 -6 180 0");
        client(10, null, () -> {
            fly = false;
            camera = CameraType.THIRD_PERSON_FRONT;
        });
        cmd(30, "player_shirt_off", "execute as @p run futbol hareket shirt_off");
        wait(30, null);
        // club: coins, pack reveal
        server(2, null, s -> {
            var sp = s.getPlayerList().getPlayers().get(0);
            CardData.addCoins(sp, 2000, "test");
            CardData.openPack(sp, 2);
        });
        client(5, null, () -> mc().setScreen(new ClubScreen()));
        wait(25, "club_reveal");
        server(2, null, s -> {
            var sp = s.getPlayerList().getPlayers().get(0);
            for (int i = 0; i < 4; i++) CardData.openPack(sp, i % 3);
        });
        client(5, null, () -> ClientState.revealed = -1);
        wait(25, "club_cards");
        client(1, null, () -> mc().setScreen(null));
        // full match (player as midfielder) with HUD and football cameras
        client(1, null, () -> {
            camera = CameraType.THIRD_PERSON_BACK;
            gui = true;
            fly = true;
            ClientState.cameraMode = 1;
        });
        cmd(5, null, "gamemode creative @a", "kill @e[type=krolasyonfutbol:football]", "futbol baslat 5 5");
        server(1, null, s -> MatchManager.setRole(s.getPlayerList().getPlayers().get(0), com.krolasyon.futbol.game.Role.MID));
        wait(40, "tv_kickoff");
        wait(80, "tv_a");
        wait(100, "tv_b");
        client(1, null, () -> ClientState.cameraMode = 2);
        wait(60, "topdown_a");
        wait(80, "topdown_b");
        client(1, null, () -> ClientState.cameraMode = 0);
        cmd(2, null, "tp @a 0 -46 -34 0 30");
        wait(80, "match_a");
        wait(120, "match_b");
        server(2, null, s -> LOG.info("[AUTOTEST] score {}-{} state {}", MatchManager.scoreRed, MatchManager.scoreBlue, MatchManager.state));
        // force a goal to test the replay
        server(1, null, s -> {
            FootballEntity b = MatchManager.ball();
            var p = MatchManager.pitch();
            if (b != null && p != null) {
                Vec3 g = p.goalCenter(1);
                b.placeAt(new Vec3(g.x - 9, g.y + 0.2, g.z + 1));
                b.kick(null, new Vec3(1.3, 0.12, 0), Vec3.ZERO, FootballEntity.TR_FIRE, 0);
            }
        });
        wait(15, "goal_banner");
        wait(40, "replay_a");
        wait(40, "replay_b");
        wait(40, "replay_c");
        wait(80, null);
        cmd(2, null, "tp @a -20 -50 -28 20 28");
        wait(120, "match_c");
        wait(160, "match_d");
        server(2, null, s -> LOG.info("[AUTOTEST] score {}-{} state {}", MatchManager.scoreRed, MatchManager.scoreBlue, MatchManager.state));
        client(1, null, () -> mc().setScreen(new MatchScreen()));
        wait(10, "menu_match");
        client(1, null, () -> mc().setScreen(new MoveScreen()));
        wait(10, "menu_moves");
        client(1, null, () -> mc().setScreen(null));
        wait(20, "END");
    }

    private static Minecraft mc() { return Minecraft.getInstance(); }

    private void showcase(Move[] moves, String id) {
        cmd(2, null, "kill @e[type=krolasyonfutbol:footballer]", "kill @e[type=krolasyonfutbol:football]", "futbol bitir");
        server(3, null, s -> spawnRow(s, moves, -14, 90F));
        cmd(10, null, "tp @a 0 -58.6 -22 0 6");
        server(1, null, s -> {
            for (FootballerEntity b : bots(s)) {
                Move m = moves[Math.min(moves.length - 1, b.slot)];
                boolean ok = MoveExecutor.perform(b, m, 0.8F, b.position().add(-14, 1, 0));
                LOG.info("[AUTOTEST] perform {} -> {}", m, ok);
            }
        });
        int first = 0;
        for (Move m : moves) first = Math.max(first, Math.min(m.impact, 9));
        wait(Math.max(4, first), "anim_" + id + "1");
        wait(7, "anim_" + id + "2");
        wait(10, "anim_" + id + "3");
    }

    private static List<FootballerEntity> bots(MinecraftServer s) {
        return s.overworld().getEntitiesOfClass(FootballerEntity.class, new AABB(-60, -80, -60, 60, 0, 60));
    }

    private static void spawnRow(MinecraftServer s, Move[] moves, double z, float yaw) {
        ServerLevel l = s.overworld();
        for (int i = 0; i < moves.length; i++) {
            FootballerEntity b = ModEntities.FOOTBALLER.get().create(l);
            if (b == null) continue;
            b.slot = i;
            b.setFootTeam(i % 2 == 0 ? Team.RED : Team.BLUE);
            b.setKeeper(moves[i] == Move.DIVE);
            b.setNumber(new int[]{10, 7, 9, 11}[i % 4]);
            double x = -6 + i * 4;
            b.moveTo(x + 0.5, -60, z + 0.5, yaw, 0);
            b.setYHeadRot(yaw);
            b.yBodyRot = yaw;
            b.setNoAi(true);
            l.addFreshEntity(b);
            if (moves[i] != null && moves[i].needsBall) {
                FootballEntity ball = ModEntities.BALL.get().create(l);
                Vec3 f = Vec3.directionFromRotation(0, yaw);
                ball.moveTo(x + 0.5 + f.x * 0.7, -60, z + 0.5 + f.z * 0.7);
                if (moves[i] == Move.HEADER) ball.moveTo(x + 0.5 + f.x * 0.5, -58.2, z + 0.5);
                l.addFreshEntity(ball);
            }
        }
    }

    private void cmd(int wait, String shot, String... commands) {
        steps.add(new Step(wait, shot, s -> {
            for (String c : commands) s.getCommands().performPrefixedCommand(s.createCommandSourceStack(), c);
        }, null));
    }

    private void wait(int ticks, String shot) { steps.add(new Step(ticks, shot, null, null)); }

    private void server(int wait, String shot, Consumer<MinecraftServer> action) { steps.add(new Step(wait, shot, action, null)); }

    private void client(int wait, String shot, Runnable r) { steps.add(new Step(wait, shot, null, r)); }

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
            LevelSettings settings = new LevelSettings("ftest", GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("ftest", settings, new WorldOptions(42L, false, false),
                    reg -> reg.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
            return;
        }
        if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) return;
        if (mc.screen != null && !(mc.screen instanceof MatchScreen) && !(mc.screen instanceof MoveScreen) && !(mc.screen instanceof ClubScreen)) mc.setScreen(null);
        mc.options.hideGui = !gui;
        mc.options.setCameraType(camera);
        if (fly && mc.player.getAbilities().mayfly) {
            mc.player.getAbilities().flying = true;
        } else if (!fly) {
            mc.player.getAbilities().flying = false;
        }
        if (inWorld < 0) {
            inWorld = tick;
            stepStart = tick + 100;
            LOG.info("[AUTOTEST] in world");
        }
        if (tick < stepStart || stepIndex >= steps.size()) return;
        Step st = steps.get(stepIndex);
        if (tick == stepStart) {
            if (st.client() != null) st.client().run();
            if (st.action() != null) {
                MinecraftServer s = mc.getSingleplayerServer();
                s.execute(() -> {
                    try {
                        st.action().accept(s);
                    } catch (Throwable t) {
                        LOG.error("[AUTOTEST] step failed", t);
                    }
                });
            }
        }
        if (tick - stepStart >= st.delay()) {
            if (st.shot() != null) {
                if (st.shot().equals("END")) {
                    LOG.info("[AUTOTEST] finished");
                    mc.stop();
                    return;
                }
                MinecraftServer srv = mc.getSingleplayerServer();
                srv.execute(() -> {
                    for (Entity e : srv.overworld().getEntitiesOfClass(FootballEntity.class, new AABB(-60, -80, -60, 60, 0, 60)))
                        LOG.info("[AUTOTEST] ball {} vel {} ctrl {}", e.position(), e.getDeltaMovement(), ((FootballEntity) e).getControllerId());
                });
                Screenshot.grab(mc.gameDirectory, "ftest_" + String.format("%02d_", stepIndex) + st.shot() + ".png", mc.getMainRenderTarget(),
                        msg -> LOG.info("[AUTOTEST] screenshot {}", st.shot()));
            }
            stepIndex++;
            stepStart = tick + 1;
        }
    }
}
