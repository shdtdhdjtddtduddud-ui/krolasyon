package com.krolasyon.futbol.client;

import com.krolasyon.futbol.entity.FootballEntity;
import com.krolasyon.futbol.entity.FootballerEntity;
import com.krolasyon.futbol.game.Move;
import com.krolasyon.futbol.game.Team;
import com.krolasyon.futbol.network.MatchS2C;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Client mirror of the match plus local preferences. */
public final class ClientState {
    private ClientState() {}

    public static int state, red, blue, timeLeft, stateTimer, kickoffTeam, playersPerTeam = 5, minutes = 5, bots;
    public static boolean hasPitch;
    public static int px, py, pz;
    public static String lastScorer = "";
    public static int lastGoalTeam;
    public static List<MatchS2C.Entry> entries = new ArrayList<>();
    public static final Map<UUID, Integer> TEAMS = new HashMap<>();
    public static final Map<UUID, Integer> NUMBERS = new HashMap<>();

    // goal banner
    public static int goalTicks;
    public static int goalTeam;
    public static String goalScorer = "";
    public static boolean goalOwn;

    // local input state
    public static boolean charging;
    public static int chargeTicks;
    public static long superCooldownEnd;
    public static int superCooldownTotal = 1;
    public static int shake;
    public static long clientTicks;
    public static boolean pendingMenu;
    public static int pendingMenuDelay;

    public static Move favSkill = Move.RAINBOW;
    public static Move favSuper = Move.FIRE_SHOT;
    public static Move favCeleb = Move.SIUU;

    // ball relations refreshed each tick
    public static final Set<Integer> CONTROLLERS = new HashSet<>();
    public static final Set<Integer> HOLDERS = new HashSet<>();
    public static FootballEntity nearestBall;
    public static double nearestBallDist = 999;

    public static void update(MatchS2C m) {
        state = m.state;
        red = m.red;
        blue = m.blue;
        timeLeft = m.timeLeft;
        stateTimer = m.stateTimer;
        kickoffTeam = m.kickoffTeam;
        playersPerTeam = m.playersPerTeam;
        minutes = m.minutes;
        hasPitch = m.hasPitch;
        px = m.px;
        py = m.py;
        pz = m.pz;
        lastScorer = m.lastScorer;
        lastGoalTeam = m.lastGoalTeam;
        bots = m.bots;
        entries = m.entries;
        TEAMS.clear();
        NUMBERS.clear();
        for (MatchS2C.Entry e : m.entries) {
            TEAMS.put(e.id(), e.team());
            NUMBERS.put(e.id(), e.number());
        }
    }

    public static Team teamOf(Entity e) {
        if (e instanceof FootballerEntity b) return b.getFootTeam();
        if (e instanceof Player p) return Team.byId(TEAMS.getOrDefault(p.getUUID(), 0));
        return Team.NONE;
    }

    public static int numberOf(Entity e) {
        if (e instanceof FootballerEntity b) return b.getNumber();
        return NUMBERS.getOrDefault(e.getUUID(), 0);
    }

    public static boolean keeper(Entity e) { return e instanceof FootballerEntity b && b.isKeeper(); }

    public static Team myTeam() {
        Player p = Minecraft.getInstance().player;
        return p == null ? Team.NONE : teamOf(p);
    }

    public static void refreshBalls() {
        CONTROLLERS.clear();
        HOLDERS.clear();
        nearestBall = null;
        nearestBallDist = 999;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof FootballEntity b) {
                int c = b.getControllerId();
                if (c >= 0) CONTROLLERS.add(c);
                LivingEntity h = b.getHolder();
                if (h != null) HOLDERS.add(h.getId());
                double d = b.distanceTo(mc.player);
                if (d < nearestBallDist) {
                    nearestBallDist = d;
                    nearestBall = b;
                }
            }
        }
    }

    // ---------------------------------------------------------- preferences

    private static Path prefsFile() { return Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("krolasyonfutbol-client.properties"); }

    public static void loadPrefs() {
        try {
            Path p = prefsFile();
            if (!Files.exists(p)) return;
            Properties pr = new Properties();
            try (var in = Files.newBufferedReader(p)) {
                pr.load(in);
            }
            Move a = Move.parse(pr.getProperty("skill", ""));
            Move b = Move.parse(pr.getProperty("super", ""));
            Move c = Move.parse(pr.getProperty("celebration", ""));
            if (a != null) favSkill = a;
            if (b != null && b.isSuper()) favSuper = b;
            if (c != null) favCeleb = c;
        } catch (Exception ignored) {
        }
    }

    public static void savePrefs() {
        try {
            Path p = prefsFile();
            Files.createDirectories(p.getParent());
            Properties pr = new Properties();
            pr.setProperty("skill", favSkill.name());
            pr.setProperty("super", favSuper.name());
            pr.setProperty("celebration", favCeleb.name());
            try (var out = Files.newBufferedWriter(p)) {
                pr.store(out, "Krolasyon Futbol");
            }
        } catch (IOException ignored) {
        }
    }
}
