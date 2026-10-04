package com.krolasyon.futbol.game;

import com.krolasyon.futbol.entity.FootballEntity;
import com.krolasyon.futbol.entity.FootballerEntity;
import com.krolasyon.futbol.network.GoalS2C;
import com.krolasyon.futbol.network.MatchS2C;
import com.krolasyon.futbol.network.Net;
import com.krolasyon.futbol.registry.ModEntities;
import com.krolasyon.futbol.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import javax.annotation.Nullable;
import java.util.*;

/** Server side match state: teams, score, clock, kick-offs, goals, restarts, bots and client sync. */
public final class MatchManager {
    private MatchManager() {}

    public enum State { IDLE, KICKOFF, PLAYING, GOAL, ENDED }

    public static State state = State.IDLE;
    public static int scoreRed, scoreBlue, timeLeft, stateTimer;
    public static int playersPerTeam = 5;
    public static int minutes = 5;
    public static Team kickoffTeam = Team.RED;
    public static Team lastGoalTeam = Team.NONE;
    public static String lastScorer = "";
    public static final Map<UUID, Team> TEAMS = new LinkedHashMap<>();
    public static final Map<UUID, Integer> NUMBERS = new HashMap<>();

    private static MinecraftServer server;
    private static UUID ballId;
    private static FootballEntity ballCache;
    private static int outTimer;
    private static int ambientTimer;
    private static long lastOoh;
    private static boolean dirty = true;
    private static final Map<Team, List<LivingEntity>> MEMBERS = new EnumMap<>(Team.class);
    private static final Map<Team, LivingEntity[]> CHASERS = new EnumMap<>(Team.class);

    private static final double[][][] FORMATIONS = {
            {},
            {{0.55, 0}},
            {{0.03, 0}, {0.5, 0}},
            {{0.03, 0}, {0.32, 0}, {0.62, 0}},
            {{0.03, 0}, {0.3, -0.35}, {0.3, 0.35}, {0.62, 0}},
            {{0.03, 0}, {0.28, -0.4}, {0.28, 0.4}, {0.5, 0}, {0.7, 0}},
            {{0.03, 0}, {0.27, -0.45}, {0.27, 0.45}, {0.48, -0.35}, {0.48, 0.35}, {0.7, 0}},
            {{0.03, 0}, {0.26, -0.5}, {0.24, 0}, {0.26, 0.5}, {0.48, -0.3}, {0.48, 0.3}, {0.7, 0}},
            {{0.03, 0}, {0.26, -0.55}, {0.24, -0.18}, {0.24, 0.18}, {0.26, 0.55}, {0.5, -0.3}, {0.5, 0.3}, {0.72, 0}},
            {{0.03, 0}, {0.26, -0.55}, {0.24, -0.18}, {0.24, 0.18}, {0.26, 0.55}, {0.48, -0.5}, {0.45, 0}, {0.48, 0.5}, {0.72, 0}},
            {{0.03, 0}, {0.26, -0.55}, {0.24, -0.18}, {0.24, 0.18}, {0.26, 0.55}, {0.47, -0.55}, {0.45, -0.18}, {0.45, 0.18}, {0.47, 0.55}, {0.72, 0}},
            {{0.03, 0}, {0.26, -0.55}, {0.24, -0.18}, {0.24, 0.18}, {0.26, 0.55}, {0.47, -0.6}, {0.45, -0.2}, {0.45, 0.2}, {0.47, 0.6}, {0.72, -0.15}, {0.72, 0.15}},
    };
    private static final int[] NUMBER_PREF = {10, 9, 7, 11, 8, 17, 19, 21, 23, 4, 5, 6, 3, 2, 14, 16, 18, 20, 22, 24};

    // ------------------------------------------------------------------ lifecycle

    public static void init(MinecraftServer s) {
        server = s;
        state = State.IDLE;
        scoreRed = scoreBlue = 0;
        TEAMS.clear();
        NUMBERS.clear();
        ballId = null;
        ballCache = null;
        Scheduler.clear();
        dirty = true;
    }

    public static void shutdown() {
        server = null;
        ballCache = null;
        MEMBERS.clear();
        CHASERS.clear();
    }

    @Nullable
    public static Pitch pitch() { return server == null ? null : PitchData.get(server).pitch; }

    public static boolean isActive() { return state != State.IDLE; }

    // ------------------------------------------------------------------ teams

    public static Team teamOf(LivingEntity e) {
        if (e instanceof FootballerEntity b) return b.getFootTeam();
        if (e instanceof Player p) return TEAMS.getOrDefault(p.getUUID(), Team.NONE);
        return Team.NONE;
    }

    public static List<LivingEntity> members(Team t) { return MEMBERS.getOrDefault(t, Collections.emptyList()); }

    @Nullable
    public static LivingEntity chaser(Team t) {
        LivingEntity[] c = CHASERS.get(t);
        return c == null ? null : c[0];
    }

    @Nullable
    public static LivingEntity secondChaser(Team t) {
        LivingEntity[] c = CHASERS.get(t);
        return c == null ? null : c[1];
    }

    public static void setTeam(ServerPlayer p, Team t) {
        Team old = TEAMS.getOrDefault(p.getUUID(), Team.NONE);
        if (t == Team.NONE) {
            TEAMS.remove(p.getUUID());
            NUMBERS.remove(p.getUUID());
        } else {
            TEAMS.put(p.getUUID(), t);
            if (old != t || !NUMBERS.containsKey(p.getUUID())) NUMBERS.put(p.getUUID(), freeNumber(t, p.getUUID()));
        }
        Scoreboard sb = server.getScoreboard();
        String name = p.getScoreboardName();
        if (sb.getPlayersTeam(name) != null) sb.removePlayerFromTeam(name);
        if (t != Team.NONE) {
            String tn = t == Team.RED ? "futbol_kirmizi" : "futbol_mavi";
            PlayerTeam pt = sb.getPlayerTeam(tn);
            if (pt == null) {
                pt = sb.addPlayerTeam(tn);
                pt.setColor(t.chat);
                pt.setDisplayName(Component.literal(t.title));
                pt.setAllowFriendlyFire(false);
            }
            sb.addPlayerToTeam(name, pt);
        }
        broadcast(Component.literal("⚽ " + p.getName().getString() + " → ").withStyle(ChatFormatting.WHITE)
                .append(Component.literal(t == Team.NONE ? "İzleyici" : t.title + " #" + NUMBERS.get(p.getUUID())).withStyle(t.chat)));
        if (isActive()) rebalance();
        dirty = true;
    }

    private static int freeNumber(Team t, UUID self) {
        Set<Integer> used = new HashSet<>();
        for (Map.Entry<UUID, Team> e : TEAMS.entrySet())
            if (e.getValue() == t && !e.getKey().equals(self) && NUMBERS.containsKey(e.getKey())) used.add(NUMBERS.get(e.getKey()));
        for (LivingEntity e : members(t)) if (e instanceof FootballerEntity b) used.add(b.getNumber());
        for (int n : NUMBER_PREF) if (!used.contains(n)) return n;
        return 99;
    }

    private static int humansOnline(Team t) {
        int n = 0;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) if (TEAMS.get(p.getUUID()) == t) n++;
        return n;
    }

    // ------------------------------------------------------------------ ball

    @Nullable
    public static FootballEntity ball() {
        if (ballCache != null && ballCache.isAlive()) return ballCache;
        ballCache = null;
        Pitch p = pitch();
        if (p == null || ballId == null) return null;
        Entity e = p.level(server).getEntity(ballId);
        if (e instanceof FootballEntity b && b.isAlive()) ballCache = b;
        return ballCache;
    }

    public static boolean isMatchBall(FootballEntity b) { return isActive() && ballId != null && ballId.equals(b.getUUID()); }

    @Nullable
    public static FootballEntity ballFor(LivingEntity e) {
        if (isActive()) {
            FootballEntity b = ball();
            if (b != null && b.level() == e.level()) return b;
        }
        return MoveExecutor.nearestBall(e, 32);
    }

    public static FootballEntity ensureBall() {
        Pitch p = pitch();
        FootballEntity b = ball();
        if (b != null || p == null) return b;
        ServerLevel l = p.level(server);
        b = ModEntities.BALL.get().create(l);
        if (b == null) return null;
        Vec3 c = p.center();
        b.moveTo(c.x, c.y + 0.2, c.z);
        l.addFreshEntity(b);
        ballId = b.getUUID();
        ballCache = b;
        return b;
    }

    public static void onBallLost(FootballEntity b) {
        if (isMatchBall(b) && pitch() != null) {
            b.placeAt(pitch().center().add(0, 0.2, 0));
        }
    }

    public static void centerBall() {
        FootballEntity b = ensureBall();
        if (b != null && pitch() != null) b.placeAt(pitch().center().add(0, 0.2, 0));
    }

    // ------------------------------------------------------------------ match flow

    public static boolean start(@Nullable ServerPlayer by) {
        Pitch p = pitch();
        if (p == null) {
            if (by != null) by.displayClientMessage(Component.literal("Önce bir saha kur! (Saha Kurucu eşyası veya /futbol saha)").withStyle(ChatFormatting.RED), false);
            return false;
        }
        removeBots();
        scoreRed = scoreBlue = 0;
        timeLeft = minutes * 60 * 20;
        lastScorer = "";
        lastGoalTeam = Team.NONE;
        ensureBall();
        state = State.KICKOFF;
        refreshMembers();
        spawnBots();
        refreshMembers();
        kickoff(Team.RED);
        broadcast(Component.literal("⚽ MAÇ BAŞLIYOR! ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(Component.literal(Team.RED.title).withStyle(ChatFormatting.RED))
                .append(Component.literal(" vs ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(Team.BLUE.title).withStyle(ChatFormatting.BLUE))
                .append(Component.literal("  (" + minutes + " dk, " + playersPerTeam + "v" + playersPerTeam + ")").withStyle(ChatFormatting.GRAY)));
        dirty = true;
        return true;
    }

    public static void stop() {
        if (state == State.IDLE) return;
        state = State.IDLE;
        FootballEntity b = ball();
        if (b != null) b.frozen = false;
        removeBots();
        broadcast(Component.literal("⚽ Maç durduruldu.").withStyle(ChatFormatting.YELLOW));
        dirty = true;
    }

    private static void kickoff(Team t) {
        Pitch p = pitch();
        kickoffTeam = t;
        state = State.KICKOFF;
        stateTimer = 70;
        outTimer = 0;
        FootballEntity b = ensureBall();
        if (b != null) {
            b.placeAt(p.center().add(0, 0.05, 0));
            b.frozen = true;
            b.restrictTeam = Team.NONE;
        }
        ServerLevel l = p.level(server);
        for (Team team : new Team[]{Team.RED, Team.BLUE}) {
            int idx = 0;
            for (LivingEntity e : members(team)) {
                if (e instanceof FootballerEntity bot) {
                    Vec3 k = kickoffPos(bot);
                    bot.teleportTo(k.x, k.y, k.z);
                    bot.setYRot(team.attackDir() > 0 ? -90F : 90F);
                    bot.yBodyRot = bot.getYRot();
                } else if (e instanceof ServerPlayer sp && sp.level() == l) {
                    Vec3 k = p.point(team, t == team && idx == 0 ? 0.47 : 0.38, idx == 0 ? 0.0 : (idx % 2 == 0 ? 1 : -1) * 0.25 * ((idx + 1) / 2));
                    sp.teleportTo(l, k.x, k.y, k.z, team.attackDir() > 0 ? -90F : 90F, 0F);
                    idx++;
                }
            }
        }
        dirty = true;
    }

    private static void end() {
        state = State.ENDED;
        stateTimer = 220;
        FootballEntity b = ball();
        if (b != null) b.frozen = true;
        Team w = winner();
        playAll(ModSounds.WHISTLE_END.get(), 1.0F, 1.0F);
        playAll(ModSounds.CROWD_CHEER.get(), 0.9F, 0.95F);
        MutableComponent c = Component.literal("⚽ MAÇ BİTTİ!  ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(Component.literal(Team.RED.abbr + " " + scoreRed).withStyle(ChatFormatting.RED))
                .append(Component.literal(" - ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(scoreBlue + " " + Team.BLUE.abbr).withStyle(ChatFormatting.BLUE));
        c.append(Component.literal(w == Team.NONE ? "  Berabere!" : "  Kazanan: " + w.title).withStyle(w == Team.NONE ? ChatFormatting.YELLOW : w.chat));
        broadcast(c);
        if (w != Team.NONE) for (int i = 0; i < 8; i++) {
            final int k = i;
            Scheduler.later(i * 8, () -> fireworks(w, k));
        }
        dirty = true;
    }

    public static Team winner() { return scoreRed > scoreBlue ? Team.RED : scoreBlue > scoreRed ? Team.BLUE : Team.NONE; }

    // ------------------------------------------------------------------ tick

    public static void tick(MinecraftServer s) {
        server = s;
        Pitch p = pitch();
        if (p == null) {
            if (dirty || s.getTickCount() % 40 == 0) sync();
            return;
        }
        refreshMembers();
        switch (state) {
            case KICKOFF -> {
                if (--stateTimer <= 0) {
                    state = State.PLAYING;
                    FootballEntity b = ensureBall();
                    if (b != null) {
                        b.frozen = false;
                        b.restrictTeam = kickoffTeam;
                        b.restrictUntil = p.level(s).getGameTime() + 60;
                    }
                    playAll(ModSounds.WHISTLE.get(), 1.0F, 1.0F);
                    dirty = true;
                }
            }
            case PLAYING -> {
                if (--timeLeft <= 0) {
                    end();
                    break;
                }
                checkBall(p);
                if (++ambientTimer > 150) {
                    ambientTimer = 0;
                    playNear(p, ModSounds.CROWD_AMBIENT.get(), 0.45F, 0.9F + s.overworld().random.nextFloat() * 0.2F);
                }
            }
            case GOAL -> {
                if (stateTimer == 95) {
                    FootballEntity b = ball();
                    if (b != null) b.frozen = true;
                }
                if (--stateTimer <= 0) kickoff(kickoffTeam);
            }
            case ENDED -> {
                if (--stateTimer <= 0) {
                    state = State.IDLE;
                    FootballEntity b = ball();
                    if (b != null) b.frozen = false;
                    dirty = true;
                }
            }
            default -> {}
        }
        if (dirty || s.getTickCount() % 10 == 0) sync();
    }

    private static void refreshMembers() {
        MEMBERS.clear();
        CHASERS.clear();
        Pitch p = pitch();
        if (p == null || server == null) return;
        ServerLevel l = p.level(server);
        List<LivingEntity> red = new ArrayList<>(), blue = new ArrayList<>();
        AABB box = new AABB(p.xMin() - 30, p.floor() - 20, p.zMin() - 30, p.xMax() + 30, p.floor() + 40, p.zMax() + 30);
        for (FootballerEntity b : l.getEntitiesOfClass(FootballerEntity.class, box, Entity::isAlive)) {
            if (b.getFootTeam() == Team.RED) red.add(b);
            else if (b.getFootTeam() == Team.BLUE) blue.add(b);
        }
        for (ServerPlayer sp : l.players()) {
            Team t = TEAMS.get(sp.getUUID());
            if (t == Team.RED) red.add(sp);
            else if (t == Team.BLUE) blue.add(sp);
        }
        MEMBERS.put(Team.RED, red);
        MEMBERS.put(Team.BLUE, blue);
        FootballEntity ball = ball();
        if (ball == null) return;
        for (Team t : new Team[]{Team.RED, Team.BLUE}) {
            LivingEntity first = null, second = null;
            double d1 = 1e9, d2 = 1e9, human = 1e9;
            for (LivingEntity e : members(t)) {
                double d = e.distanceToSqr(ball);
                if (e instanceof Player) {
                    human = Math.min(human, d);
                    continue;
                }
                if (e instanceof FootballerEntity fb && fb.isKeeper()) continue;
                if (d < d1) {
                    second = first;
                    d2 = d1;
                    first = e;
                    d1 = d;
                } else if (d < d2) {
                    second = e;
                    d2 = d;
                }
            }
            if (first != null && Math.sqrt(human) < Math.sqrt(d1) - 2.0) {
                second = first;
                first = null;
            }
            CHASERS.put(t, new LivingEntity[]{first, second});
        }
    }

    private static void checkBall(Pitch p) {
        FootballEntity b = ensureBall();
        if (b == null) return;
        if (b.getHolder() != null) {
            outTimer = 0;
            return;
        }
        Vec3 c = b.center();
        int side = p.goalSide(c, FootballEntity.R);
        if (side != 0) {
            goal(p, b, side);
            return;
        }
        if (p.outOverGoalLine(c, FootballEntity.R) || p.outOverTouchLine(c, FootballEntity.R)) {
            if (++outTimer > 10) restart(p, b, c);
        } else outTimer = 0;
    }

    private static void goal(Pitch p, FootballEntity b, int side) {
        Team scoring = side > 0 ? Team.RED : Team.BLUE;
        Team conceding = scoring.opponent();
        if (scoring == Team.RED) scoreRed++;
        else scoreBlue++;
        boolean own = b.lastTouchTeam == conceding;
        lastScorer = b.lastToucherName == null || b.lastToucherName.isEmpty() ? "?" : b.lastToucherName;
        lastGoalTeam = scoring;
        state = State.GOAL;
        stateTimer = 130;
        kickoffTeam = conceding;
        b.restrictTeam = Team.NONE;
        playAll(ModSounds.GOAL_HORN.get(), 1.0F, 1.0F);
        playAll(ModSounds.CROWD_CHEER.get(), 1.0F, 1.0F);
        playAll(ModSounds.WHISTLE.get(), 0.8F, 1.0F);
        broadcast(Component.literal("⚽ GOOOL! ").withStyle(scoring.chat, ChatFormatting.BOLD)
                .append(Component.literal(lastScorer + (own ? " (kendi kalesine)" : "") + "  ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(Team.RED.abbr + " " + scoreRed + " - " + scoreBlue + " " + Team.BLUE.abbr).withStyle(ChatFormatting.GOLD)));
        GoalS2C pkt = new GoalS2C(scoring.ordinal(), lastScorer, own, scoreRed, scoreBlue);
        for (ServerPlayer sp : server.getPlayerList().getPlayers()) Net.sendTo(sp, pkt);
        for (int i = 0; i < 6; i++) {
            final int k = i;
            Scheduler.later(5 + i * 9, () -> fireworks(scoring, k));
        }
        if (b.lastToucher != null) {
            Entity scorer = p.level(server).getEntity(b.lastToucher);
            if (scorer instanceof FootballerEntity bot && !own) {
                Move[] c = {Move.SIUU, Move.KNEE_SLIDE, Move.AIRPLANE, Move.BACKFLIP, Move.DANCE};
                Scheduler.later(8, () -> MoveExecutor.perform(bot, c[bot.getRandom().nextInt(c.length)], 0));
            } else if (scorer instanceof ServerPlayer sp && !own) {
                sp.displayClientMessage(Component.literal("Gol sevinci için [H] tuşuna bas!").withStyle(ChatFormatting.GOLD), true);
            }
        }
        dirty = true;
    }

    private static void restart(Pitch p, FootballEntity b, Vec3 c) {
        outTimer = 0;
        long now = p.level(server).getGameTime();
        Team last = b.lastTouchTeam;
        RandomSource r = p.level(server).random;
        Team gets = last == Team.NONE ? (r.nextBoolean() ? Team.RED : Team.BLUE) : last.opponent();
        Vec3 pos;
        String what;
        if (p.outOverTouchLine(c, FootballEntity.R) && !p.outOverGoalLine(c, FootballEntity.R)) {
            double z = c.z < p.midZ() ? p.zMin() + 0.4 : p.zMax() - 0.4;
            pos = new Vec3(Mth.clamp(c.x, p.xMin() + 1, p.xMax() - 1), p.floor() + 0.05, z);
            what = "Taç atışı";
        } else {
            int side = c.x < p.midX() ? -1 : 1;
            Team defending = side < 0 ? Team.RED : Team.BLUE;
            if (gets == defending) {
                double x = side < 0 ? p.xMin() + 5 : p.xMax() - 5;
                pos = new Vec3(x, p.floor() + 0.05, p.midZ() + (c.z < p.midZ() ? -3 : 3));
                what = "Aut atışı";
            } else {
                double x = side < 0 ? p.xMin() + 0.6 : p.xMax() - 0.6;
                double z = c.z < p.midZ() ? p.zMin() + 0.6 : p.zMax() - 0.6;
                pos = new Vec3(x, p.floor() + 0.05, z);
                what = "Korner";
            }
        }
        b.placeAt(pos);
        b.restrictTeam = gets;
        b.restrictUntil = now + 100;
        playAll(ModSounds.WHISTLE.get(), 0.6F, 1.25F);
        for (ServerPlayer sp : server.getPlayerList().getPlayers())
            sp.displayClientMessage(Component.literal(what + " — " + gets.title).withStyle(gets.chat), true);
    }

    public static void onNearMiss(FootballEntity b) {
        if (state != State.PLAYING || pitch() == null) return;
        long now = b.level().getGameTime();
        if (now - lastOoh < 60) return;
        lastOoh = now;
        playNear(pitch(), ModSounds.CROWD_OOH.get(), 0.9F, 1.0F);
    }

    public static void onSave(LivingEntity keeper) {
        if (state != State.PLAYING || pitch() == null) return;
        long now = keeper.level().getGameTime();
        if (now - lastOoh < 60) return;
        lastOoh = now;
        playNear(pitch(), ModSounds.CROWD_OOH.get(), 0.8F, 1.1F);
    }

    // ------------------------------------------------------------------ bots & formation

    public static void removeBots() {
        Pitch p = pitch();
        if (p == null || server == null) return;
        ServerLevel l = p.level(server);
        AABB box = new AABB(p.xMin() - 40, p.floor() - 30, p.zMin() - 40, p.xMax() + 40, p.floor() + 60, p.zMax() + 40);
        for (FootballerEntity b : l.getEntitiesOfClass(FootballerEntity.class, box)) b.discard();
        MEMBERS.clear();
    }

    private static int[] slotOrder(int n) {
        // GK first, then attack, defence, midfield, remaining
        int[] order = new int[n];
        for (int i = 0; i < n; i++) order[i] = i;
        if (n >= 3) {
            List<Integer> l = new ArrayList<>();
            l.add(0);
            l.add(n - 1);
            for (int i = 1; i < n - 1; i++) l.add(i);
            for (int i = 0; i < n; i++) order[i] = l.get(i);
        }
        return order;
    }

    private static void spawnBots() {
        Pitch p = pitch();
        ServerLevel l = p.level(server);
        int n = Mth.clamp(playersPerTeam, 1, 11);
        for (Team t : new Team[]{Team.RED, Team.BLUE}) {
            int humans = humansOnline(t);
            int bots = Math.max(0, n - humans);
            int[] order = slotOrder(n);
            for (int i = 0; i < bots; i++) spawnBot(l, p, t, order[i], n);
        }
    }

    private static void spawnBot(ServerLevel l, Pitch p, Team t, int slot, int n) {
        FootballerEntity b = ModEntities.FOOTBALLER.get().create(l);
        if (b == null) return;
        b.slot = slot;
        b.setFootTeam(t);
        b.setKeeper(n >= 2 && slot == 0);
        int num = b.isKeeper() ? 1 : freeNumber(t, UUID.randomUUID());
        b.setNumber(num);
        Vec3 pos = slotPos(p, t, slot, n, 0.5, 0, true);
        b.moveTo(pos.x, pos.y, pos.z, t.attackDir() > 0 ? -90F : 90F, 0F);
        l.addFreshEntity(b);
        List<LivingEntity> list = MEMBERS.computeIfAbsent(t, k -> new ArrayList<>());
        list.add(b);
    }

    /** keeps the team at playersPerTeam when humans join or leave during a match */
    public static void rebalance() {
        Pitch p = pitch();
        if (p == null || !isActive()) return;
        refreshMembers();
        int n = Mth.clamp(playersPerTeam, 1, 11);
        for (Team t : new Team[]{Team.RED, Team.BLUE}) {
            int humans = humansOnline(t);
            List<FootballerEntity> bots = new ArrayList<>();
            for (LivingEntity e : members(t)) if (e instanceof FootballerEntity b) bots.add(b);
            int want = Math.max(0, n - humans);
            int[] order = slotOrder(n);
            if (bots.size() > want) {
                bots.sort(Comparator.comparingInt(b -> -indexOf(order, b.slot)));
                for (int i = 0; i < bots.size() - want; i++) bots.get(i).discard();
            } else if (bots.size() < want) {
                Set<Integer> used = new HashSet<>();
                for (FootballerEntity b : bots) used.add(b.slot);
                int add = want - bots.size();
                for (int s : order) {
                    if (add <= 0) break;
                    if (used.contains(s)) continue;
                    spawnBot(p.level(server), p, t, s, n);
                    add--;
                }
            }
        }
        refreshMembers();
    }

    private static int indexOf(int[] a, int v) {
        for (int i = 0; i < a.length; i++) if (a[i] == v) return i;
        return a.length;
    }

    public static Vec3 slotPos(Pitch p, Team t, int slot, int n, double ballU, double ballV, boolean kickoff) {
        double[][] f = FORMATIONS[Mth.clamp(n, 1, 11)];
        double[] s = f[Mth.clamp(slot, 0, f.length - 1)];
        double u = s[0], v = s[1];
        boolean gk = n >= 2 && slot == 0;
        if (kickoff) {
            if (!gk) u = Math.min(u * 0.85, 0.44);
        } else if (!gk) {
            u = Mth.clamp(u + (ballU - 0.5) * 0.45, 0.06, 0.93);
            v = Mth.clamp(v * 0.9 + ballV * 0.28, -0.92, 0.92);
        }
        return p.point(t, u, v);
    }

    public static Vec3 kickoffPos(FootballerEntity b) {
        Pitch p = pitch();
        Team t = b.getFootTeam();
        int n = Mth.clamp(playersPerTeam, 1, 11);
        Vec3 pos = slotPos(p, t, b.slot, n, 0.5, 0, true);
        if (t == kickoffTeam && b.slot == (n >= 3 ? n - 1 : n - 1) && !b.isKeeper()) pos = p.point(t, 0.485, 0.02);
        return pos;
    }

    public static Vec3 dynamicPos(FootballerEntity b) {
        Pitch p = pitch();
        Team t = b.getFootTeam();
        FootballEntity ball = ball();
        double bu = 0.5, bv = 0;
        if (ball != null) {
            bu = p.teamU(t, ball.getX());
            bv = p.teamV(t, ball.getZ());
        }
        return slotPos(p, t, b.slot, Mth.clamp(playersPerTeam, 1, 11), bu, bv, false);
    }

    // ------------------------------------------------------------------ effects

    private static void fireworks(Team t, int k) {
        Pitch p = pitch();
        if (p == null) return;
        ServerLevel l = p.level(server);
        RandomSource r = l.random;
        ItemStack fw = new ItemStack(Items.FIREWORK_ROCKET);
        CompoundTag tag = fw.getOrCreateTagElement("Fireworks");
        ListTag ex = new ListTag();
        CompoundTag e = new CompoundTag();
        e.putByte("Type", (byte) (k % 2 == 0 ? 1 : 4));
        e.putIntArray("Colors", new int[]{t.color, 0xFFFFFF});
        e.putIntArray("FadeColors", new int[]{0xFFD54F});
        e.putBoolean("Flicker", true);
        e.putBoolean("Trail", true);
        ex.add(e);
        tag.put("Explosions", ex);
        tag.putByte("Flight", (byte) 2);
        double x = p.midX() + (r.nextDouble() - 0.5) * Pitch.HL * 1.6;
        double z = (r.nextBoolean() ? p.zMin() - 6 : p.zMax() + 6);
        l.addFreshEntity(new FireworkRocketEntity(l, x, p.floor() + 3, z, fw));
    }

    private static void playAll(SoundEvent s, float vol, float pitch) {
        if (server == null) return;
        for (ServerPlayer sp : server.getPlayerList().getPlayers()) sp.playNotifySound(s, SoundSource.RECORDS, vol, pitch);
    }

    private static void playNear(Pitch p, SoundEvent s, float vol, float pitch) {
        for (ServerPlayer sp : server.getPlayerList().getPlayers())
            if (sp.level() == p.level(server) && p.near(sp.position(), 40)) sp.playNotifySound(s, SoundSource.RECORDS, vol, pitch);
    }

    public static void broadcast(Component c) {
        if (server != null) server.getPlayerList().broadcastSystemMessage(c, false);
    }

    public static void welcome(ServerPlayer sp) {
        if (pitch() == null) {
            sp.sendSystemMessage(Component.literal("⚽ Krolasyon Futbol: Saha kurmak için 'Saha Kurucu' eşyasını kullan. Menü: [J]").withStyle(ChatFormatting.GOLD));
            return;
        }
        MutableComponent c = Component.literal("⚽ Takımını seç: ").withStyle(ChatFormatting.GOLD);
        c.append(button("[KIRMIZI]", "/futbol takim kirmizi", ChatFormatting.RED));
        c.append(Component.literal(" "));
        c.append(button("[MAVİ]", "/futbol takim mavi", ChatFormatting.BLUE));
        c.append(Component.literal(" "));
        c.append(button("[İZLEYİCİ]", "/futbol takim izleyici", ChatFormatting.GRAY));
        c.append(Component.literal("  veya [J] menüsü").withStyle(ChatFormatting.DARK_GRAY));
        sp.sendSystemMessage(c);
        Net.sendTo(sp, new com.krolasyon.futbol.network.OpenMenuS2C());
    }

    private static Component button(String text, String cmd, ChatFormatting color) {
        return Component.literal(text).withStyle(Style.EMPTY.withColor(color).withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(cmd))));
    }

    // ------------------------------------------------------------------ sync

    public static void markDirty() { dirty = true; }

    private static void sync() {
        dirty = false;
        if (server == null) return;
        Pitch p = pitch();
        List<MatchS2C.Entry> entries = new ArrayList<>();
        for (Map.Entry<UUID, Team> e : TEAMS.entrySet()) {
            ServerPlayer sp = server.getPlayerList().getPlayer(e.getKey());
            String name = sp != null ? sp.getName().getString() : "?";
            entries.add(new MatchS2C.Entry(e.getKey(), name, e.getValue().ordinal(), NUMBERS.getOrDefault(e.getKey(), 0), sp != null));
        }
        int bots = 0;
        for (Team t : new Team[]{Team.RED, Team.BLUE}) for (LivingEntity e : members(t)) if (e instanceof FootballerEntity) bots++;
        MatchS2C pkt = new MatchS2C(state.ordinal(), scoreRed, scoreBlue, timeLeft, stateTimer, kickoffTeam.ordinal(), playersPerTeam, minutes,
                p != null, p != null ? p.cx : 0, p != null ? p.y : 0, p != null ? p.cz : 0, lastScorer, lastGoalTeam.ordinal(), bots, entries);
        for (ServerPlayer sp : server.getPlayerList().getPlayers()) Net.sendTo(sp, pkt);
    }
}
