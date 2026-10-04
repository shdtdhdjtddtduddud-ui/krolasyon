package com.rabona.arena.game;

import com.rabona.arena.entity.BallEntity;
import com.rabona.arena.entity.FootballerEntity;
import com.rabona.arena.net.Net;
import com.rabona.arena.net.S2C;
import com.rabona.arena.registry.ModEntities;
import com.rabona.arena.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.joml.Vector3f;

import java.util.*;

/** Mac yoneticisi: takimlar, skor, sure, duran toplar ve botlar. */
public class Match {
    public enum Phase { IDLE, KICKOFF, PLAYING, GOAL, HALFTIME, ENDED }

    private static Match INSTANCE;
    private static MinecraftServer SERVER;

    public final Map<UUID, Team> roster = new LinkedHashMap<>();
    public final Map<UUID, Integer> numbers = new HashMap<>();
    public Pitch pitch;
    public Phase phase = Phase.IDLE;
    public int scoreRed, scoreBlue, timeLeft, totalTime, half = 1, phaseTimer;
    public boolean swapped;
    public Team kickoffTeam = Team.RED;
    public int durationMin = 6, teamSize = 5, difficulty = 1;
    public UUID ballId;
    private int syncTimer;
    private String lastScorer = "";

    // ================================================================ erisim
    public static Match get(MinecraftServer server) {
        if (INSTANCE == null || SERVER != server) {
            SERVER = server;
            INSTANCE = new Match();
            INSTANCE.loadData(server);
        }
        return INSTANCE;
    }

    public static void shutdown() {
        INSTANCE = null;
        SERVER = null;
    }

    public static boolean isRunning() {
        return INSTANCE != null && INSTANCE.phase != Phase.IDLE && INSTANCE.phase != Phase.ENDED;
    }

    public static Team teamOf(Entity e) {
        if (e instanceof FootballerEntity f) return f.getSquad();
        if (e instanceof Player p && INSTANCE != null) return INSTANCE.roster.getOrDefault(p.getUUID(), Team.NONE);
        return Team.NONE;
    }

    public ServerLevel level() {
        return SERVER.overworld();
    }

    // ================================================================ yon bilgisi
    /** takim icin hucum yonu isareti (+1/-1 yerel a ekseni). */
    public int attackSign(Team t) {
        int s = t == Team.RED ? 1 : -1;
        return swapped ? -s : s;
    }

    public Vec3 attackDir(LivingEntity e) {
        Team t = teamOf(e);
        if (pitch == null || !t.playing()) return null;
        return pitch.axisA().scale(attackSign(t));
    }

    public Vec3 targetGoal(LivingEntity e) {
        Team t = teamOf(e);
        if (pitch == null) return null;
        if (!t.playing()) {
            // serbest oyun: bakilan yondeki kale
            double la = pitch.a(e.position());
            Vec3 f = MoveLogic.face(e);
            int s = f.dot(pitch.axisA()) >= 0 ? 1 : -1;
            if (Math.abs(la) > Pitch.HALF_LEN + 3) return null;
            return pitch.goalCenter(s).add(0, 1.2, 0);
        }
        return pitch.goalCenter(attackSign(t)).add(0, 1.2, 0);
    }

    public Vec3 ownGoal(Team t) {
        return pitch == null ? null : pitch.goalCenter(-attackSign(t));
    }

    // ================================================================ takimlar
    public void join(ServerPlayer p, Team t) {
        Team old = roster.getOrDefault(p.getUUID(), Team.NONE);
        Scoreboard sb = SERVER.getScoreboard();
        if (t == Team.NONE) {
            roster.remove(p.getUUID());
            PlayerTeam cur = sb.getPlayersTeam(p.getScoreboardName());
            if (cur != null && cur.getName().startsWith("ra_")) sb.removePlayerFromTeam(p.getScoreboardName(), cur);
        } else {
            roster.put(p.getUUID(), t);
            sb.addPlayerToTeam(p.getScoreboardName(), vanillaTeam(sb, t));
            if (!numbers.containsKey(p.getUUID())) numbers.put(p.getUUID(), freeNumber(t));
        }
        if (old != t) {
            broadcast(Component.translatable("msg.rabonaarena.joined", p.getDisplayName(), t.displayName()));
            if (isRunning() && t.playing() && pitch != null) {
                // sahaya yerlestir
                Vec3 pos = pitch.world(-attackSign(t) * 8, (p.getRandom().nextDouble() - 0.5) * 10, pitch.surfaceY());
                p.teleportTo(level(), pos.x, pos.y, pos.z, yawToward(pos, pitch.goalCenter(attackSign(t))), 0);
            }
        }
        sync();
    }

    private PlayerTeam vanillaTeam(Scoreboard sb, Team t) {
        String n = "ra_" + t.key;
        PlayerTeam pt = sb.getPlayerTeam(n);
        if (pt == null) {
            pt = sb.addPlayerTeam(n);
            pt.setColor(t.chat);
            pt.setDisplayName(t.displayName());
            pt.setAllowFriendlyFire(false);
        }
        return pt;
    }

    private int freeNumber(Team t) {
        int[] pref = {10, 7, 9, 11, 8, 17, 23, 19, 14, 21, 99};
        Set<Integer> used = new HashSet<>();
        roster.forEach((u, tt) -> { if (tt == t && numbers.containsKey(u)) used.add(numbers.get(u)); });
        for (FootballerEntity f : bots()) if (f.getSquad() == t) used.add(f.getNumber());
        for (int n : pref) if (!used.contains(n)) return n;
        for (int n = 2; n < 100; n++) if (!used.contains(n)) return n;
        return 99;
    }

    public List<ServerPlayer> playersOf(Team t) {
        List<ServerPlayer> l = new ArrayList<>();
        for (ServerPlayer p : SERVER.getPlayerList().getPlayers()) if (roster.get(p.getUUID()) == t) l.add(p);
        return l;
    }

    public List<FootballerEntity> bots() {
        List<FootballerEntity> l = new ArrayList<>();
        for (Entity e : level().getAllEntities()) if (e instanceof FootballerEntity f && f.isAlive()) l.add(f);
        return l;
    }

    public List<LivingEntity> members(Team t) {
        List<LivingEntity> l = new ArrayList<>(playersOf(t));
        for (FootballerEntity f : bots()) if (f.getSquad() == t) l.add(f);
        return l;
    }

    // ================================================================ botlar
    public FootballerEntity spawnBot(ServerLevel sl, Team t, Vec3 at) {
        FootballerEntity f = new FootballerEntity(ModEntities.FOOTBALLER.get(), sl);
        RandomSource rnd = new RandomSource(sl.getRandom().nextLong());
        f.setup(t, freeNumber(t), rnd.skin(), BotNames.random(sl.getRandom()), 62 + difficulty * 10 + sl.getRandom().nextInt(12));
        f.moveTo(at.x, at.y, at.z, sl.getRandom().nextFloat() * 360, 0);
        sl.addFreshEntity(f);
        assignRoles();
        sync();
        return f;
    }

    private record RandomSource(long seed) {
        int skin() { return (int) Math.floorMod(seed, FootballerEntity.SKINS); }
    }

    public void fillBots() {
        if (pitch == null) return;
        for (Team t : new Team[]{Team.RED, Team.BLUE}) {
            int have = members(t).size();
            for (int i = have; i < teamSize; i++) {
                Vec3 p = pitch.world(-attackSign(t) * (6 + i * 3), (i % 2 == 0 ? 1 : -1) * (2 + i), pitch.surfaceY());
                spawnBot(level(), t, p);
            }
        }
        assignRoles();
        broadcast(Component.translatable("msg.rabonaarena.bots_filled", teamSize).withStyle(ChatFormatting.GREEN));
    }

    public void clearBots() {
        for (FootballerEntity f : bots()) {
            Athlete.remove(f.getUUID());
            f.discard();
        }
        sync();
    }

    public void assignRoles() {
        for (Team t : new Team[]{Team.RED, Team.BLUE}) {
            List<FootballerEntity> list = new ArrayList<>();
            for (FootballerEntity f : bots()) if (f.getSquad() == t) list.add(f);
            list.sort(Comparator.comparingInt(FootballerEntity::getNumber));
            // kaleci: 1 numara tercih
            list.sort(Comparator.comparingInt(f -> f.getNumber() == 1 ? 0 : 1));
            for (int i = 0; i < list.size(); i++) list.get(i).setSlot(i);
            if (!list.isEmpty() && list.get(0).getNumber() != 1) {
                boolean taken = false;
                for (FootballerEntity f : list) if (f.getNumber() == 1) taken = true;
                if (!taken) list.get(0).setNumber(1);
            }
        }
    }

    /** dizilis: slot -> (a, b) normalize; a: -1 kendi kale .. +1 rakip kale. */
    public static final double[][] FORMATION = {
            {-0.95, 0}, {-0.58, 0}, {0.38, 0}, {-0.05, -0.55}, {-0.05, 0.55}, {-0.52, -0.62},
            {-0.52, 0.62}, {-0.18, 0}, {0.32, -0.6}, {0.32, 0.6}, {-0.66, 0.28}};

    public Vec3 formationSpot(Team t, int slot, Vec3 ballPos, boolean kickoff) {
        double[] f = FORMATION[Math.min(slot, FORMATION.length - 1)];
        int s = attackSign(t);
        double ba = ballPos == null ? 0 : pitch.a(ballPos) * s / Pitch.HALF_LEN;
        double bb = ballPos == null ? 0 : pitch.b(ballPos) / Pitch.HALF_WID;
        double a, b;
        if (kickoff) {
            a = Math.min(f[0], -0.06) * 0.9;
            b = f[1];
        } else if (slot == 0) {
            a = -0.95;
            b = Mth.clamp(bb * 0.12, -0.08, 0.08);
        } else {
            a = Mth.clamp(f[0] * 0.65 + ba * 0.55 + 0.05, -0.85, 0.85);
            b = Mth.clamp(f[1] * 0.85 + bb * 0.3, -0.92, 0.92);
        }
        return pitch.world(a * Pitch.HALF_LEN * s, b * Pitch.HALF_WID, pitch.surfaceY());
    }

    // ================================================================ mac akisi
    public boolean start() {
        if (pitch == null) {
            broadcast(Component.translatable("msg.rabonaarena.no_pitch").withStyle(ChatFormatting.RED));
            return false;
        }
        scoreRed = scoreBlue = 0;
        half = 1;
        swapped = false;
        totalTime = durationMin * 60 * 20;
        timeLeft = totalTime;
        Athlete.resetStats();
        assignRoles();
        broadcast(Component.translatable("msg.rabonaarena.match_start", durationMin).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        kickoff(Team.RED);
        return true;
    }

    public void stop() {
        phase = Phase.IDLE;
        sound(ModSounds.WHISTLE_END.get(), 2f, 1f);
        broadcast(Component.translatable("msg.rabonaarena.match_stopped").withStyle(ChatFormatting.GRAY));
        sync();
    }

    private void kickoff(Team t) {
        kickoffTeam = t;
        phase = Phase.KICKOFF;
        phaseTimer = 70;
        BallEntity ball = ensureBall();
        Vec3 c = pitch.center();
        ball.setController(null, false);
        ball.setPos(c.x, c.y + 0.02, c.z);
        ball.setDeltaMovement(Vec3.ZERO);
        ball.setSpin(new Vector3f());
        ball.setEffect(0, 0);
        ball.restrict(t.ordinal(), 200);
        for (Team tt : new Team[]{Team.RED, Team.BLUE}) {
            int i = 0;
            for (ServerPlayer p : playersOf(tt)) {
                Vec3 pos = tt == t && i == 0
                        ? pitch.world(-attackSign(tt) * 1.2, 0, pitch.surfaceY())
                        : pitch.world(-attackSign(tt) * (6 + i * 2), (i % 2 == 0 ? -1 : 1) * (5 + i * 2), pitch.surfaceY());
                p.teleportTo(level(), pos.x, pos.y, pos.z, yawToward(pos, pitch.goalCenter(attackSign(tt))), 0);
                p.setDeltaMovement(Vec3.ZERO);
                i++;
            }
        }
        for (FootballerEntity f : bots()) {
            if (!f.getSquad().playing()) continue;
            Vec3 pos = formationSpot(f.getSquad(), f.getSlot(), null, true);
            if (f.getSquad() == t && f.getSlot() == firstOutfieldSlot(t) && playersOf(t).isEmpty()) {
                pos = pitch.world(-attackSign(t) * 1.2, 0, pitch.surfaceY());
            }
            f.teleportTo(pos.x, pos.y, pos.z);
            f.setYRot(yawToward(pos, pitch.goalCenter(attackSign(f.getSquad()))));
            f.setYHeadRot(f.getYRot());
            f.setDeltaMovement(Vec3.ZERO);
            f.getNavigation().stop();
        }
        sync();
    }

    private int firstOutfieldSlot(Team t) {
        int best = 99;
        for (FootballerEntity f : bots()) if (f.getSquad() == t && f.getSlot() > 0) best = Math.min(best, f.getSlot());
        return best == 99 ? 0 : best;
    }

    public static float yawToward(Vec3 from, Vec3 to) {
        return (float) (Mth.atan2(to.z - from.z, to.x - from.x) * Mth.RAD_TO_DEG) - 90f;
    }

    public BallEntity ball() {
        if (ballId == null) return null;
        Entity e = level().getEntity(ballId);
        return e instanceof BallEntity b && b.isAlive() ? b : null;
    }

    private BallEntity ensureBall() {
        BallEntity b = ball();
        if (b == null) {
            // sahada serbest top varsa onu kullan
            for (Entity e : level().getAllEntities()) {
                if (e instanceof BallEntity be && be.isAlive() && pitch.inside(be.position(), 6)) { b = be; break; }
            }
        }
        if (b == null) {
            b = new BallEntity(ModEntities.BALL.get(), level());
            Vec3 c = pitch.center();
            b.moveTo(c.x, c.y, c.z, 0, 0);
            level().addFreshEntity(b);
        }
        ballId = b.getUUID();
        return b;
    }

    // ================================================================ tick
    public void tick() {
        if (++syncTimer >= 10) {
            syncTimer = 0;
            sync();
        }
        if (phase == Phase.IDLE || pitch == null) return;
        BallEntity ball = ball();
        if (ball == null && phase != Phase.ENDED) ball = ensureBall();

        switch (phase) {
            case KICKOFF -> {
                if (ball != null) {
                    Vec3 c = pitch.center();
                    if (ball.getControllerId() >= 0) ball.setController(null, false);
                    ball.setPos(c.x, c.y + 0.02, c.z);
                    ball.setDeltaMovement(Vec3.ZERO);
                }
                if (--phaseTimer <= 0) {
                    phase = Phase.PLAYING;
                    sound(ModSounds.WHISTLE.get(), 2f, 1f);
                    if (ball != null) ball.restrict(kickoffTeam.ordinal(), 30);
                    sync();
                }
            }
            case PLAYING -> {
                timeLeft--;
                if (timeLeft % 400 == 0) sound(ModSounds.CROWD_AMBIENT.get(), 0.9f, 1f);
                if (half == 1 && timeLeft <= totalTime / 2) {
                    halftime();
                    return;
                }
                if (timeLeft <= 0) {
                    end();
                    return;
                }
                if (ball != null) checkBall(ball);
            }
            case GOAL -> {
                if (--phaseTimer <= 0) kickoff(kickoffTeam);
            }
            case HALFTIME -> {
                if (--phaseTimer <= 0) {
                    swapped = true;
                    half = 2;
                    broadcast(Component.translatable("msg.rabonaarena.second_half").withStyle(ChatFormatting.GOLD));
                    kickoff(Team.BLUE);
                }
            }
            case ENDED -> {
                if (phaseTimer % 15 == 0 && phaseTimer > 60) celebrateFireworks(scoreRed == scoreBlue ? null : scoreRed > scoreBlue ? Team.RED : Team.BLUE);
                if (--phaseTimer <= 0) {
                    phase = Phase.IDLE;
                    sync();
                }
            }
            default -> {}
        }
    }

    private void halftime() {
        phase = Phase.HALFTIME;
        phaseTimer = 100;
        sound(ModSounds.WHISTLE_END.get(), 2f, 1f);
        broadcast(Component.translatable("msg.rabonaarena.halftime", scoreRed, scoreBlue).withStyle(ChatFormatting.YELLOW));
        sync();
    }

    private void end() {
        phase = Phase.ENDED;
        phaseTimer = 240;
        sound(ModSounds.WHISTLE_END.get(), 2.5f, 1f);
        sound(ModSounds.CROWD_GOAL.get(), 2f, 0.9f);
        Component res = scoreRed == scoreBlue ? Component.translatable("msg.rabonaarena.draw")
                : Component.translatable("msg.rabonaarena.winner", (scoreRed > scoreBlue ? Team.RED : Team.BLUE).displayName());
        broadcast(Component.translatable("msg.rabonaarena.full_time", scoreRed, scoreBlue).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        broadcast(res);
        // mac yildizi
        LivingEntity mvp = null;
        int best = -1;
        for (Team t : new Team[]{Team.RED, Team.BLUE}) {
            for (LivingEntity e : members(t)) {
                Athlete a = Athlete.of(e);
                int score = a.goals * 10 + a.assists * 6 + a.tackles * 2 + a.skills + a.saves * 3;
                if (score > best) { best = score; mvp = e; }
            }
        }
        if (mvp != null) broadcast(Component.translatable("msg.rabonaarena.mvp", mvp.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
        Net.toAll(new S2C.Banner(2, scoreRed, scoreBlue, mvp == null ? "" : mvp.getName().getString(), ""));
        for (FootballerEntity f : bots()) {
            if (scoreRed != scoreBlue && f.getSquad() == (scoreRed > scoreBlue ? Team.RED : Team.BLUE)) {
                Move[] c = {Move.CELEB_DANCE, Move.CELEB_BACKFLIP, Move.CELEB_PLANE, Move.CELEB_SIU};
                Scheduler.later(10 + f.getRandom().nextInt(30), () -> MoveLogic.tryPerform(f, c[f.getRandom().nextInt(c.length)], 1, 0, null));
            }
        }
        sync();
    }

    // ================================================================ top kontrolu
    private void checkBall(BallEntity ball) {
        if (ball.isHeld()) {
            // kaleci topu fazla tutmasin
            return;
        }
        Vec3 c = ball.center();
        double a = pitch.a(c), b = pitch.b(c);
        double y = c.y - pitch.surfaceY();
        double line = pitch.goalLineA() + BallEntity.RADIUS;
        if (Math.abs(a) > line) {
            int side = a > 0 ? 1 : -1;
            if (Math.abs(b) < Pitch.GOAL_HALF_W && y < Pitch.GOAL_H + 0.1 && y > -0.5) {
                Team scorer = attackSign(Team.RED) == side ? Team.RED : Team.BLUE;
                goal(ball, scorer);
            } else {
                Entity last = level().getEntity(ball.lastToucherId);
                Team lastTeam = last == null ? Team.NONE : teamOf(last);
                Team defending = attackSign(Team.RED) == side ? Team.BLUE : Team.RED;
                if (lastTeam == defending) {
                    restart(ball, defending.opponent(), pitch.world(side * (Pitch.HALF_LEN - 0.2), Math.signum(b) * (Pitch.HALF_WID - 0.2), pitch.surfaceY()), "corner");
                } else {
                    restart(ball, defending, pitch.world(side * (Pitch.HALF_LEN - Pitch.SMALL_DEPTH), Math.signum(b) * 3, pitch.surfaceY()), "goal_kick");
                }
            }
            return;
        }
        if (Math.abs(b) > Pitch.HALF_WID + 0.5 + BallEntity.RADIUS) {
            Entity last = level().getEntity(ball.lastToucherId);
            Team lastTeam = last == null ? Team.NONE : teamOf(last);
            Team to = lastTeam == Team.NONE ? Team.RED : lastTeam.opponent();
            restart(ball, to, pitch.world(Mth.clamp(a, -Pitch.HALF_LEN + 1, Pitch.HALF_LEN - 1), Math.signum(b) * (Pitch.HALF_WID - 0.3), pitch.surfaceY()), "throw_in");
        }
    }

    private void restart(BallEntity ball, Team team, Vec3 at, String kind) {
        ball.setController(null, false);
        ball.setPos(at.x, at.y + 0.02, at.z);
        ball.setDeltaMovement(Vec3.ZERO);
        ball.setSpin(new Vector3f());
        ball.setEffect(0, 0);
        ball.restrict(team.ordinal(), 100);
        sound(ModSounds.WHISTLE.get(), 1.5f, 1.15f);
        Net.toAll(new S2C.Feed(Component.translatable("restart.rabonaarena." + kind, team.displayName())));
    }

    public void goal(BallEntity ball, Team scorerTeam) {
        if (scorerTeam == Team.RED) scoreRed++;
        else scoreBlue++;
        Entity last = level().getEntity(ball.lastToucherId);
        Entity prev = level().getEntity(ball.prevToucherId);
        String scorerName = last == null ? "?" : last.getName().getString();
        String assistName = "";
        boolean own = last != null && teamOf(last) != scorerTeam && teamOf(last).playing();
        if (last instanceof LivingEntity le && !own) {
            Athlete.of(le).goals++;
            Athlete.of(le).addEnergy(25);
            if (prev instanceof LivingEntity pe && pe != le && teamOf(pe) == scorerTeam) {
                Athlete.of(pe).assists++;
                Athlete.of(pe).addEnergy(15);
                assistName = pe.getName().getString();
            }
            if (le instanceof FootballerEntity fb) {
                Move[] c = {Move.CELEB_SIU, Move.CELEB_KNEESLIDE, Move.CELEB_BACKFLIP, Move.CELEB_PLANE, Move.CELEB_DANCE, Move.CELEB_SHUSH};
                Scheduler.later(12, () -> MoveLogic.tryPerform(fb, c[fb.getRandom().nextInt(c.length)], 1, 0, null));
            } else if (le instanceof ServerPlayer sp) {
                sp.displayClientMessage(Component.translatable("msg.rabonaarena.celebrate_hint").withStyle(ChatFormatting.GOLD), true);
            }
        }
        if (own) scorerName = scorerName + " (K.K.)";
        lastScorer = scorerName;
        String moveName = ball.lastMove >= 0 ? Move.byId(ball.lastMove).key() : "";
        phase = Phase.GOAL;
        phaseTimer = 110;
        kickoffTeam = scorerTeam.opponent();
        ball.setEffect(0, 0);
        sound(ModSounds.GOAL_HORN.get(), 3f, 1f);
        sound(ModSounds.CROWD_GOAL.get(), 3f, 1f);
        Net.toAll(new S2C.Banner(1, scoreRed, scoreBlue, scorerName, assistName + "|" + moveName + "|" + scorerTeam.ordinal()));
        broadcast(Component.translatable("msg.rabonaarena.goal", scorerName, scorerTeam.displayName(), scoreRed, scoreBlue)
                .withStyle(scorerTeam.chat, ChatFormatting.BOLD));
        celebrateFireworks(scorerTeam);
        level().sendParticles(ParticleTypes.TOTEM_OF_UNDYING, ball.getX(), ball.getY() + 1, ball.getZ(), 80, 1, 1, 1, 0.5);
        sync();
    }

    private void celebrateFireworks(Team t) {
        if (pitch == null) return;
        ServerLevel sl = level();
        int color = t == null ? 0xFFD54F : t.color;
        for (int i = 0; i < 6; i++) {
            double a = (sl.getRandom().nextDouble() - 0.5) * 2 * Pitch.HALF_LEN;
            double b = (sl.getRandom().nextBoolean() ? 1 : -1) * (Pitch.HALF_WID + 7);
            Vec3 p = pitch.world(a, b, pitch.surfaceY() + 4);
            ItemStack rocket = new ItemStack(Items.FIREWORK_ROCKET);
            CompoundTag fw = rocket.getOrCreateTagElement("Fireworks");
            fw.putByte("Flight", (byte) 1);
            ListTag ex = new ListTag();
            CompoundTag e = new CompoundTag();
            e.putByte("Type", (byte) sl.getRandom().nextInt(5));
            e.putIntArray("Colors", new int[]{color, 0xFFFFFF});
            e.putIntArray("FadeColors", new int[]{0xFFD54F});
            e.putBoolean("Flicker", true);
            e.putBoolean("Trail", true);
            ex.add(e);
            fw.put("Explosions", ex);
            sl.addFreshEntity(new FireworkRocketEntity(sl, p.x, p.y, p.z, rocket));
        }
    }

    // ================================================================ dokunus olaylari
    public static void onTouch(BallEntity ball, LivingEntity e) {}

    public static void onPostHit(BallEntity ball) {
        if (INSTANCE != null && INSTANCE.phase == Phase.PLAYING) {
            INSTANCE.sound(ModSounds.CROWD_OOH.get(), 2f, 1f);
            Net.toAll(new S2C.Feed(Component.translatable("msg.rabonaarena.post").withStyle(ChatFormatting.YELLOW)));
        }
    }

    // ================================================================ yardimcilar
    void sound(SoundEvent s, float vol, float pitchF) {
        if (pitch == null) return;
        Vec3 c = pitch.center();
        level().playSound(null, c.x, c.y + 6, c.z, s, SoundSource.RECORDS, vol * 3, pitchF);
        for (ServerPlayer p : SERVER.getPlayerList().getPlayers()) {
            if (p.level() == level() && pitch.inside(p.position(), 30) && p.position().distanceTo(c) > 40) {
                p.playNotifySound(s, SoundSource.RECORDS, vol * 0.6f, pitchF);
            }
        }
    }

    public void broadcast(Component c) {
        SERVER.getPlayerList().broadcastSystemMessage(Component.literal("[Rabona] ").withStyle(ChatFormatting.DARK_GREEN).append(c), false);
    }

    public void sync() {
        if (SERVER == null) return;
        Net.toAll(S2C.MatchState.of(this));
    }

    public String lastScorer() { return lastScorer; }

    // ================================================================ kayit
    public void setPitch(Pitch p) {
        pitch = p;
        saveData();
        sync();
    }

    private void loadData(MinecraftServer server) {
        Data d = server.overworld().getDataStorage().computeIfAbsent(Data::load, Data::new, "rabonaarena");
        pitch = d.pitch;
        durationMin = d.duration;
        teamSize = d.teamSize;
        difficulty = d.difficulty;
    }

    public void saveData() {
        Data d = SERVER.overworld().getDataStorage().computeIfAbsent(Data::load, Data::new, "rabonaarena");
        d.pitch = pitch;
        d.duration = durationMin;
        d.teamSize = teamSize;
        d.difficulty = difficulty;
        d.setDirty();
    }

    public static class Data extends SavedData {
        Pitch pitch;
        int duration = 6, teamSize = 5, difficulty = 1;

        static Data load(CompoundTag t) {
            Data d = new Data();
            if (t.contains("pitch")) d.pitch = Pitch.load(t.getCompound("pitch"));
            if (t.contains("duration")) d.duration = t.getInt("duration");
            if (t.contains("teamSize")) d.teamSize = t.getInt("teamSize");
            if (t.contains("difficulty")) d.difficulty = t.getInt("difficulty");
            return d;
        }

        @Override
        public CompoundTag save(CompoundTag t) {
            if (pitch != null) t.put("pitch", pitch.save());
            t.putInt("duration", duration);
            t.putInt("teamSize", teamSize);
            t.putInt("difficulty", difficulty);
            return t;
        }
    }
}
