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
import net.minecraft.util.RandomSource;
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
    public enum Phase { IDLE, KICKOFF, PLAYING, GOAL, HALFTIME, ENDED, SET_PIECE }

    public enum SetPiece { NONE, FREE_KICK, PENALTY }

    private static Match INSTANCE;
    private static MinecraftServer SERVER;

    public final Map<UUID, Team> roster = new LinkedHashMap<>();
    public final Map<UUID, Integer> numbers = new HashMap<>();
    public final Map<UUID, Pos> playerPos = new HashMap<>();
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
        invalidate();
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

    // tick basina onbellek (botlar her tick bu listeleri defalarca ister)
    private long cacheTick = -1;
    private List<FootballerEntity> botCache = List.of();
    private final Map<Team, List<LivingEntity>> memberCache = new EnumMap<>(Team.class);

    public void invalidate() { cacheTick = -1; }

    private void refreshCache() {
        long now = level().getGameTime();
        if (now == cacheTick) return;
        cacheTick = now;
        List<FootballerEntity> l = new ArrayList<>();
        for (Entity e : level().getAllEntities()) if (e instanceof FootballerEntity f && f.isAlive()) l.add(f);
        botCache = Collections.unmodifiableList(l);
        memberCache.clear();
        for (Team t : Team.values()) {
            List<LivingEntity> m = new ArrayList<>(playersOf(t));
            for (FootballerEntity f : l) if (f.getSquad() == t) m.add(f);
            memberCache.put(t, Collections.unmodifiableList(m));
        }
    }

    public List<FootballerEntity> bots() {
        refreshCache();
        return botCache;
    }

    public List<LivingEntity> members(Team t) {
        refreshCache();
        return memberCache.getOrDefault(t, List.of());
    }

    // ================================================================ botlar
    public FootballerEntity spawnBot(ServerLevel sl, Team t, Vec3 at) {
        FootballerEntity f = new FootballerEntity(ModEntities.FOOTBALLER.get(), sl);
        int ovr = 58 + difficulty * 9 + sl.getRandom().nextInt(10);
        f.setup(t, freeNumber(t), sl.getRandom().nextInt(FootballerEntity.SKINS), BotNames.random(sl.getRandom()), ovr);
        f.moveTo(at.x, at.y, at.z, sl.getRandom().nextFloat() * 360, 0);
        sl.addFreshEntity(f);
        invalidate();
        assignRoles();
        Cards.applySquad(this, t);
        sync();
        return f;
    }

    public void fillBots() {
        if (pitch == null) return;
        for (Team t : new Team[]{Team.RED, Team.BLUE}) {
            int have = members(t).size();
            for (int i = have; i < teamSize; i++) {
                Vec3 p = pitch.world(-attackSign(t) * (6 + i * 3), (i % 2 == 0 ? 1 : -1) * (2 + i), pitch.surfaceY());
                FootballerEntity f = new FootballerEntity(ModEntities.FOOTBALLER.get(), level());
                int ovr = 58 + difficulty * 9 + level().getRandom().nextInt(10);
                f.setup(t, freeNumber(t), level().getRandom().nextInt(FootballerEntity.SKINS), BotNames.random(level().getRandom()), ovr);
                f.moveTo(p.x, p.y, p.z, 0, 0);
                level().addFreshEntity(f);
            }
        }
        invalidate();
        assignRoles();
        for (Team t : new Team[]{Team.RED, Team.BLUE}) Cards.applySquad(this, t);
        sync();
        broadcast(Component.translatable("msg.rabonaarena.bots_filled", teamSize).withStyle(ChatFormatting.GREEN));
    }

    public void clearBots() {
        for (FootballerEntity f : bots()) {
            Athlete.remove(f.getUUID());
            f.discard();
        }
        invalidate();
        sync();
    }

    // ================================================================ menajer, taktik, oyuncu degisikligi
    public static final int MAX_SUBS = 5;
    /** Insan kaptani olmayan takimlarin (bot) menajeri. */
    private final Map<Team, Cards.Manager> botManagers = new EnumMap<>(Team.class);
    private final Map<Team, List<Cards.Card>> benches = new EnumMap<>(Team.class);
    private final Map<Team, Integer> subsUsed = new EnumMap<>(Team.class);
    private final Set<UUID> subbedIn = new HashSet<>();

    public Cards.Manager manager(Team t) {
        Cards.Profile pr = Cards.captain(this, t);
        Cards.Manager m = pr == null ? null : pr.activeManager();
        if (m != null) return m;
        return botManagers.computeIfAbsent(t, k -> Cards.Manager.random(level().getRandom(), 60 + difficulty * 8, 74 + difficulty * 8));
    }

    private final Map<Team, Tactic> tacticCache = new EnumMap<>(Team.class);
    private long tacticTick = -1;

    /** Takimin taktigi (tick basina onbellekli - botlar sik sorar). */
    public Tactic tactic(Team t) {
        if (!t.playing()) return Tactic.BALANCED;
        long now = level().getGameTime();
        if (now != tacticTick) {
            tacticTick = now;
            tacticCache.clear();
        }
        return tacticCache.computeIfAbsent(t, k -> {
            Cards.Manager m = manager(k);
            return m == null ? Tactic.BALANCED : m.tacticEnum();
        });
    }

    public boolean subbedIn(FootballerEntity f) { return subbedIn.contains(f.getUUID()); }

    public int subsLeft(Team t) { return MAX_SUBS - subsUsed.getOrDefault(t, 0); }

    public List<Cards.Card> bench(Team t) {
        return benches.computeIfAbsent(t, k -> Cards.bench(this, k));
    }

    /** Kadro / menajer degisti: diziliisi ve kartlari yeniden uygula. */
    public void refreshTeam(Team t) {
        benches.remove(t);
        tacticTick = -1;
        assignRoles();
        Cards.applySquad(this, t);
        sync();
    }

    /** Oyuncu degisikligi: sahadaki botun yerine yedekten oyuncu girer (taze kondisyon). */
    public boolean substitute(Team t, FootballerEntity out, int benchIdx, LivingEntity by) {
        if (!t.playing() || out == null || out.getSquad() != t || !out.isAlive()) return false;
        List<Cards.Card> b = bench(t);
        if (benchIdx < 0 || benchIdx >= b.size()) return false;
        if (subsLeft(t) <= 0) {
            if (by instanceof ServerPlayer sp) sp.displayClientMessage(Component.translatable("msg.rabonaarena.no_subs").withStyle(ChatFormatting.RED), true);
            return false;
        }
        Cards.Card in = b.remove(benchIdx);
        String outName = out.getBaseName();
        Cards.Manager mg = manager(t);
        Cards.apply(out, t, in, mg == null ? 0 : mg.bonus());
        Athlete a = Athlete.of(out);
        a.stamina = 100;
        a.energy = Math.max(a.energy, 30);
        a.yellows = 0;
        a.fouls = 0;
        subbedIn.add(out.getUUID());
        subsUsed.merge(t, 1, Integer::sum);
        level().sendParticles(ParticleTypes.HAPPY_VILLAGER, out.getX(), out.getY() + 1, out.getZ(), 14, 0.4, 0.6, 0.4, 0.05);
        sound(ModSounds.WHISTLE.get(), 1.2f, 1.3f);
        Net.toAll(new S2C.Feed(Component.translatable("msg.rabonaarena.sub", t.displayName(), Component.literal(in.name()).withStyle(ChatFormatting.GREEN),
                Component.literal(outName).withStyle(ChatFormatting.RED)).withStyle(ChatFormatting.WHITE)));
        Net.toAll(new S2C.Banner(5, scoreRed, scoreBlue, in.name(), outName + "|" + t.ordinal()));
        sync();
        return true;
    }

    /** Bot takimlari: devre arasinda yorgun oyuncularini degistirir. */
    private void autoSubs() {
        for (Team t : new Team[]{Team.RED, Team.BLUE}) {
            if (!playersOf(t).isEmpty()) continue;
            List<FootballerEntity> list = new ArrayList<>();
            for (FootballerEntity f : bots()) if (f.getSquad() == t && !f.isKeeper() && !subbedIn(f)) list.add(f);
            list.sort(Comparator.comparingDouble(f -> Athlete.of(f).stamina));
            for (int i = 0; i < Math.min(2, list.size()); i++) {
                List<Cards.Card> b = bench(t);
                if (b.isEmpty()) break;
                int best = 0;
                double bd = 99;
                for (int k = 0; k < b.size(); k++) {
                    double d = Pos.byId(b.get(k).pos()).dist(list.get(i).getFieldPos());
                    if (d < bd) { bd = d; best = k; }
                }
                substitute(t, list.get(i), best, null);
            }
        }
    }

    private void resetSquads() {
        botManagers.clear();
        benches.clear();
        subsUsed.clear();
        subbedIn.clear();
    }

    private void announceManagers() {
        for (Team t : new Team[]{Team.RED, Team.BLUE}) {
            Cards.Manager m = manager(t);
            if (m == null) continue;
            Tactic tc = m.tacticEnum();
            broadcast(Component.translatable("msg.rabonaarena.manager_plan", t.displayName(), m.name(), tc.title(), tc.shape)
                    .withStyle(ChatFormatting.AQUA));
        }
    }

    // ================================================================ faul, kart, serbest vurus, penalti
    public SetPiece setPiece = SetPiece.NONE;
    public Team spTeam = Team.NONE;
    public Vec3 spSpot;
    public int spTaker = -1, spTimer, spSetup;
    private final List<Integer> wall = new ArrayList<>();

    /** Faul: kart (sari/kirmizi) ve duran top (ceza sahasinda penalti). */
    public void foul(LivingEntity offender, LivingEntity victim, boolean fromBehind, double severity) {
        if (phase != Phase.PLAYING || pitch == null) return;
        Team vt = teamOf(victim), ot = teamOf(offender);
        if (!vt.playing() || ot == vt) return;
        Athlete oa = Athlete.of(offender);
        oa.fouls++;
        sound(ModSounds.WHISTLE.get(), 1.8f, 1.05f);
        String name = offender.getName().getString();
        double cardChance = severity * (fromBehind ? 0.75 : 0.35);
        if (level().getRandom().nextDouble() < cardChance) {
            oa.yellows++;
            boolean red = oa.yellows >= 2 || severity > 0.95;
            Net.toAll(new S2C.Banner(3, scoreRed, scoreBlue, name, red ? "red" : "yellow"));
            broadcast(Component.translatable(red ? "msg.rabonaarena.red_card" : "msg.rabonaarena.yellow_card", offender.getDisplayName())
                    .withStyle(red ? ChatFormatting.RED : ChatFormatting.YELLOW));
            if (red) Scheduler.later(30, () -> sendOff(offender));
        } else {
            Net.toAll(new S2C.Feed(Component.translatable("msg.rabonaarena.foul", offender.getDisplayName()).withStyle(ChatFormatting.GOLD)));
        }
        int s = attackSign(vt);
        Vec3 spot = victim.position();
        double a = pitch.a(spot) * s, b = pitch.b(spot);
        boolean inBox = a > Pitch.HALF_LEN - Pitch.BOX_DEPTH && a < Pitch.HALF_LEN + 0.5 && Math.abs(b) < Pitch.BOX_HALF + 0.5;
        if (inBox) {
            startSetPiece(SetPiece.PENALTY, vt, pitch.world(s * (Pitch.HALF_LEN - Pitch.PEN_SPOT + 0.5), 0, pitch.surfaceY()));
        } else {
            double ca = Mth.clamp(pitch.a(spot), -Pitch.HALF_LEN + 1, Pitch.HALF_LEN - 1);
            double cb = Mth.clamp(b, -Pitch.HALF_WID + 1, Pitch.HALF_WID - 1);
            startSetPiece(SetPiece.FREE_KICK, vt, pitch.world(ca, cb, pitch.surfaceY()));
        }
    }

    private void sendOff(LivingEntity e) {
        if (e instanceof FootballerEntity f) {
            if (f.getId() == p2Bot) p2Bot = -1;
            Athlete.remove(f.getUUID());
            f.discard();
            invalidate();
            assignRoles();
        } else if (e instanceof ServerPlayer p) {
            join(p, Team.NONE);
        }
        sync();
    }

    private void startSetPiece(SetPiece kind, Team team, Vec3 spot) {
        BallEntity ball = ensureBall();
        phase = Phase.SET_PIECE;
        setPiece = kind;
        spTeam = team;
        spSpot = spot;
        spTimer = 20 * 14;
        spSetup = 30;
        ball.setController(null, false);
        ball.setPos(spot.x, spot.y + 0.02, spot.z);
        ball.setDeltaMovement(Vec3.ZERO);
        ball.setSpin(new Vector3f());
        ball.setEffect(0, 0);
        ball.locked = true;
        int s = attackSign(team);
        Vec3 goal = pitch.goalCenter(s);
        Vec3 toGoal = MoveLogic.flat(goal.subtract(spot));
        Vec3 side = MoveLogic.right(toGoal);
        // kullanan: takimdaki insan (en yakin), yoksa en iyi sutor bot
        LivingEntity taker = null;
        double bd = 1e9;
        for (ServerPlayer h : playersOf(team)) {
            double d = h.distanceToSqr(spot);
            if (d < bd) { bd = d; taker = h; }
        }
        if (taker == null) {
            int best = -1;
            for (FootballerEntity f : bots()) {
                if (f.getSquad() != team || f.isKeeper()) continue;
                if (f.shooting > best) { best = f.shooting; taker = f; }
            }
        }
        spTaker = taker == null ? -1 : taker.getId();
        if (taker != null) place(taker, spot.subtract(toGoal.scale(1.4)), goal);
        wall.clear();
        Team def = team.opponent();
        if (kind == SetPiece.PENALTY) {
            double edge = Pitch.HALF_LEN - Pitch.BOX_DEPTH - 2;
            int i = 0;
            for (LivingEntity e : allMembers()) {
                if (e == taker) continue;
                if (e instanceof FootballerEntity f && f.isKeeper() && f.getSquad() == def) {
                    place(f, pitch.world(s * (pitch.goalLineA() - 0.6), 0, pitch.surfaceY()), spot);
                    continue;
                }
                double lb = (i % 2 == 0 ? 1 : -1) * (2 + (i / 2) * 2.5);
                place(e, pitch.world(s * edge, Mth.clamp(lb, -Pitch.HALF_WID + 1, Pitch.HALF_WID - 1), pitch.surfaceY()), goal);
                i++;
            }
        } else {
            double dist = MoveLogic.horiz(spot, goal);
            List<FootballerEntity> defs = new ArrayList<>();
            for (FootballerEntity f : bots()) if (f.getSquad() == def && !f.isKeeper()) defs.add(f);
            defs.sort(Comparator.comparingDouble(f -> f.distanceToSqr(goal)));
            int wallSize = dist < 24 ? Math.min(4, defs.size()) : dist < 32 ? Math.min(2, defs.size()) : 0;
            for (int i = 0; i < wallSize; i++) {
                FootballerEntity f = defs.get(i);
                Vec3 wp = spot.add(toGoal.scale(9)).add(side.scale((i - (wallSize - 1) / 2.0) * 0.7));
                place(f, wp, spot);
                wall.add(f.getId());
            }
            // rakipler 9 blok uzaga
            for (LivingEntity e : members(def)) {
                if (wall.contains(e.getId()) || (e instanceof FootballerEntity f && f.isKeeper())) continue;
                if (e.distanceTo(ball) < 9.5) {
                    Vec3 away = MoveLogic.flat(e.position().subtract(spot));
                    if (away.lengthSqr() < 1e-4) away = toGoal;
                    place(e, spot.add(away.scale(10)), spot);
                }
            }
        }
        sound(ModSounds.WHISTLE.get(), 2f, 1f);
        Net.toAll(new S2C.Banner(4, scoreRed, scoreBlue, taker == null ? "" : taker.getName().getString(), kind.name().toLowerCase()));
        sync();
    }

    private List<LivingEntity> allMembers() {
        List<LivingEntity> l = new ArrayList<>(members(Team.RED));
        l.addAll(members(Team.BLUE));
        return l;
    }

    private void place(LivingEntity e, Vec3 pos, Vec3 lookAt) {
        float yaw = yawToward(pos, lookAt);
        if (e instanceof ServerPlayer p) {
            p.teleportTo(level(), pos.x, pos.y, pos.z, yaw, 0);
        } else {
            e.teleportTo(pos.x, pos.y, pos.z);
            e.setYRot(yaw);
            e.setYHeadRot(yaw);
            e.yBodyRot = yaw;
            if (e instanceof FootballerEntity f) f.getNavigation().stop();
        }
        e.setDeltaMovement(Vec3.ZERO);
    }

    public boolean isTaker(LivingEntity e) {
        return phase == Phase.SET_PIECE && e.getId() == spTaker;
    }

    public boolean isWall(LivingEntity e) { return phase == Phase.SET_PIECE && wall.contains(e.getId()); }

    private void setPieceTick(BallEntity ball) {
        if (ball == null) return;
        if (spSetup > 0) spSetup--;
        if (--spTimer <= 0 || !ball.locked) {
            endSetPiece(ball);
            return;
        }
        Entity t = level().getEntity(spTaker);
        if (spSetup == 0 && t instanceof FootballerEntity bot && spTimer % 20 == 0 && spTimer < 20 * 14 - 50) botTakeSetPiece(bot, ball);
        if (!(t instanceof LivingEntity)) {
            endSetPiece(ball);
            return;
        }
        // insan atici topun basinda kalir (fare ile nisan alir)
        if (t instanceof net.minecraft.server.level.ServerPlayer sp && MoveLogic.horiz(sp.position(), ball.position()) > 2.0) {
            Vec3 back = sp.position().subtract(ball.position());
            back = new Vec3(back.x, 0, back.z).normalize().scale(1.2);
            sp.teleportTo(ball.getX() + back.x, ball.getY(), ball.getZ() + back.z);
        }
    }

    private void botTakeSetPiece(FootballerEntity bot, BallEntity ball) {
        int s = attackSign(spTeam);
        Vec3 goal = pitch.goalCenter(s);
        RandomSource r = level().getRandom();
        double dist = MoveLogic.horiz(ball.position(), goal);
        double b = (r.nextBoolean() ? 1 : -1) * (Pitch.GOAL_HALF_W - 0.8 - r.nextDouble());
        Vec3 target = pitch.world(s * (pitch.goalLineA() + 0.4), b, pitch.surfaceY() + 0.4 + r.nextDouble() * 1.8);
        Move m;
        if (setPiece == SetPiece.PENALTY) {
            float x = r.nextFloat();
            m = x < 0.1 ? Move.SHOT_PANENKA : x < 0.55 ? Move.SHOT_FINESSE : Move.SHOT_POWER;
            if (m == Move.SHOT_PANENKA) target = pitch.world(s * (pitch.goalLineA() + 0.4), 0, pitch.surfaceY() + 1.2);
        } else if (dist < 30) {
            float x = r.nextFloat();
            m = x < 0.4 ? Move.SHOT_FINESSE : x < 0.65 ? Move.SHOT_DEADLEAF : x < 0.85 ? Move.SHOT_KNUCKLE : Move.SHOT_TRIVELA;
        } else {
            LivingEntity mate = null;
            double best = 1e9;
            for (LivingEntity e : members(spTeam)) {
                if (e == bot) continue;
                double d = e.position().distanceTo(goal);
                if (d < best) { best = d; mate = e; }
            }
            m = Move.PASS_CROSS;
            target = mate == null ? goal : mate.position().add(0, 0.6, 0);
        }
        float yaw = yawToward(bot.position(), target);
        bot.setYRot(yaw);
        bot.setYHeadRot(yaw);
        bot.yBodyRot = yaw;
        MoveLogic.tryPerform(bot, m, 0.85f + r.nextFloat() * 0.15f, b > 0 ? -1 : 1, target);
    }

    private void endSetPiece(BallEntity ball) {
        ball.locked = false;
        phase = Phase.PLAYING;
        setPiece = SetPiece.NONE;
        spTaker = -1;
        wall.clear();
        sync();
    }

    public static void onSetPieceKick(BallEntity ball, LivingEntity by) {
        Match m = INSTANCE;
        if (m == null || m.phase != Phase.SET_PIECE) return;
        // baraj ziplar
        for (int id : m.wall) {
            Entity e = m.level().getEntity(id);
            if (e instanceof FootballerEntity f) MoveLogic.tryPerform(f, Move.BLOCK, 1, 0, null);
        }
        m.endSetPiece(ball);
    }

    // ================================================================ yerel 2. oyuncu (ayni bilgisayar, 2. kumanda)
    public UUID p2Owner;

    public boolean isP2Owner(ServerPlayer p) { return p2Owner != null && p2Owner.equals(p.getUUID()) && p2Team.playing(); }
    public int p2Bot = -1;
    public Team p2Team = Team.NONE;
    public float p2X, p2Z;
    public boolean p2Sprint;
    public long p2Last, p2SwitchCd;

    /** mode: 0 kapali, 1 rakip takim, 2 benim takimim. */
    public void setP2(ServerPlayer owner, int mode) {
        if (mode == 0) {
            p2Owner = null;
            p2Bot = -1;
            p2Team = Team.NONE;
            sync();
            return;
        }
        Team own = teamOf(owner);
        if (!own.playing()) {
            join(owner, Team.RED);
            own = Team.RED;
        }
        p2Owner = owner.getUUID();
        p2Team = mode == 1 ? own.opponent() : own;
        boolean any = false;
        for (FootballerEntity f : bots()) if (f.getSquad() == p2Team && !f.isKeeper()) any = true;
        if (!any) fillBots();
        BallEntity b = ball();
        FootballerEntity f = closestBot(p2Team, b != null ? b.position() : pitch != null ? pitch.center() : owner.position());
        p2Bot = f == null ? -1 : f.getId();
        broadcast(Component.translatable("msg.rabonaarena.p2_on", p2Team.displayName()).withStyle(ChatFormatting.AQUA));
        sync();
    }

    public FootballerEntity p2Entity() {
        if (p2Owner == null || p2Bot < 0) return null;
        Entity e = level().getEntity(p2Bot);
        return e instanceof FootballerEntity f && f.isAlive() && f.getSquad() == p2Team ? f : null;
    }

    public boolean isDriven(FootballerEntity f) {
        return p2Owner != null && f.getId() == p2Bot && SERVER.getPlayerList().getPlayer(p2Owner) != null;
    }

    public void p2Input(ServerPlayer from, float x, float z, boolean sprint) {
        if (!from.getUUID().equals(p2Owner)) return;
        p2X = x;
        p2Z = z;
        p2Sprint = sprint;
        p2Last = level().getGameTime();
    }

    public void p2Switch(boolean manual) {
        if (p2Owner == null) return;
        BallEntity b = ball();
        if (b == null) return;
        LivingEntity c = b.getController();
        FootballerEntity target = c instanceof FootballerEntity fb && fb.getSquad() == p2Team && !fb.isKeeper() ? fb : closestBot(p2Team, b.position());
        if (target != null && target.getId() != p2Bot) {
            p2Bot = target.getId();
            p2SwitchCd = level().getGameTime() + 20;
            level().sendParticles(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.2f, 1f, 1f), 1.4f),
                    target.getX(), target.getY() + 0.1, target.getZ(), 14, 0.4, 0.02, 0.4, 0);
            sync();
        }
    }

    private void autoSwitchP2(BallEntity ball) {
        FootballerEntity me = p2Entity();
        if (p2Owner == null) return;
        LivingEntity c = ball.getController();
        if (c instanceof FootballerEntity bot && bot.getSquad() == p2Team && !bot.isKeeper() && bot.getId() != p2Bot && ball.controlTicks <= 2) {
            p2Bot = bot.getId();
            sync();
            return;
        }
        if (me == null) {
            p2Switch(false);
            return;
        }
        if (level().getGameTime() % 10 != 0 || level().getGameTime() < p2SwitchCd) return;
        if (c != null && teamOf(c) == p2Team) return;
        double my = me.distanceTo(ball);
        FootballerEntity best = closestBot(p2Team, ball.position());
        if (my > 16 && best != null && best != me && best.distanceTo(ball) < my - 8) p2Switch(false);
    }

    // ================================================================ FIFA tarzi oyuncu degistirme
    /** FIFA kontrolunu kapatan oyuncular. */
    public final Set<UUID> fifaOff = new HashSet<>();
    private final Map<UUID, Long> switchCd = new HashMap<>();

    public boolean fifaOn(ServerPlayer p) {
        return !fifaOff.contains(p.getUUID()) && posOf(p) != Pos.GK;
    }

    private boolean canSwitch(ServerPlayer p) {
        return switchCd.getOrDefault(p.getUUID(), 0L) <= level().getGameTime();
    }

    /** Top takimdaki bir bota gecerse oyuncu o bota gecer; savunmada uzak kalinca topa yakin oyuncuya gecer. */
    private void autoSwitch(BallEntity ball) {
        LivingEntity c = ball.getController();
        if (c instanceof FootballerEntity bot && !bot.isKeeper() && !ball.isHeld() && ball.controlTicks <= 2 && bot.getId() != p2Bot) {
            ServerPlayer h = nearestHuman(bot.getSquad(), bot.position(), true);
            if (h != null) {
                swap(h, bot);
                return;
            }
        }
        if (level().getGameTime() % 10 != 0) return;
        for (Team t : new Team[]{Team.RED, Team.BLUE}) {
            if (c != null && teamOf(c) == t) continue; // top bizde: oyuncu kendisi oynar
            for (ServerPlayer h : playersOf(t)) {
                if (!fifaOn(h) || !canSwitch(h)) continue;
                double my = h.distanceTo(ball);
                if (my < 16) continue;
                FootballerEntity best = closestBot(t, ball.position());
                if (best != null && best.distanceTo(ball) < my - 8) swap(h, best);
            }
        }
    }

    /** Elle degistirme (Sol Alt): topa en yakin takim arkadasina gec. */
    public void manualSwitch(ServerPlayer p) {
        Team t = teamOf(p);
        BallEntity b = ball();
        if (!t.playing() || b == null || posOf(p) == Pos.GK) return;
        LivingEntity c = b.getController();
        FootballerEntity target = c instanceof FootballerEntity fb && fb.getSquad() == t && !fb.isKeeper() && fb.getId() != p2Bot ? fb : closestBot(t, b.position());
        if (target != null) swap(p, target);
    }

    private FootballerEntity closestBot(Team t, Vec3 to) {
        FootballerEntity best = null;
        double bd = 1e9;
        for (FootballerEntity f : bots()) {
            if (f.getSquad() != t || f.isKeeper()) continue;
            if (f.getId() == p2Bot && p2Owner != null) continue; // 2. oyuncunun futbolcusu
            double d = f.position().distanceToSqr(to);
            if (d < bd) { bd = d; best = f; }
        }
        return best;
    }

    private ServerPlayer nearestHuman(Team t, Vec3 to, boolean needSwitch) {
        ServerPlayer best = null;
        double bd = 1e9;
        for (ServerPlayer h : playersOf(t)) {
            if (!fifaOn(h) || (needSwitch && !canSwitch(h))) continue;
            double d = h.position().distanceToSqr(to);
            if (d < bd) { bd = d; best = h; }
        }
        return best;
    }

    /** Oyuncu ile botun yerini, mevkisini ve hareketini degistir; top bottaysa oyuncuya gecer. */
    public void swap(ServerPlayer p, FootballerEntity bot) {
        BallEntity b = ball();
        boolean hadBall = b != null && b.getController() == bot;
        Vec3 pp = p.position(), bp = bot.position();
        float py = p.getYRot(), by = bot.getYRot();
        Vec3 pv = Athlete.of(p).vel, bv = bot.getDeltaMovement();
        Pos ppos = posOf(p), bpos = bot.getFieldPos();
        p.teleportTo(level(), bp.x, bp.y, bp.z, by, p.getXRot());
        p.setDeltaMovement(bv.x, 0, bv.z);
        p.hurtMarked = true;
        bot.teleportTo(pp.x, pp.y, pp.z);
        bot.setYRot(py);
        bot.setYHeadRot(py);
        bot.yBodyRot = py;
        bot.setDeltaMovement(pv.x, 0, pv.z);
        playerPos.put(p.getUUID(), bpos);
        bot.setFieldPos(ppos);
        Athlete.of(p).lastPos = null;
        if (hadBall) {
            b.setController(p, false);
            b.makeImmune(bot, 10);
        }
        switchCd.put(p.getUUID(), level().getGameTime() + 25);
        com.rabona.arena.RabonaArena.LOG.info("[RTEST] swap {} -> bot {} ({}) ball={}", p.getName().getString(), bot.getNumber(), bpos, hadBall);
        level().sendParticles(new net.minecraft.core.particles.DustParticleOptions(
                new org.joml.Vector3f(teamOf(p) == Team.RED ? 1f : 0.2f, 0.3f, teamOf(p) == Team.RED ? 0.2f : 1f), 1.4f),
                bp.x, bp.y + 0.1, bp.z, 14, 0.4, 0.02, 0.4, 0);
        p.displayClientMessage(Component.translatable("msg.rabonaarena.switched", bot.getNumber(), bpos.title()).withStyle(teamOf(p).chat), true);
        sync();
    }

    // ================================================================ pas isteme
    private final Map<UUID, Long> passRequests = new HashMap<>();

    public void requestPass(ServerPlayer p) {
        Team t = teamOf(p);
        passRequests.put(p.getUUID(), level().getGameTime() + 50);
        level().sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY() + 2.3, p.getZ(), 6, 0.2, 0.15, 0.2, 0);
        level().playSound(null, p.getX(), p.getY(), p.getZ(), net.minecraft.sounds.SoundEvents.NOTE_BLOCK_PLING.get(), net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.6f);
        Net.toTrackingAndSelf(p, new S2C.Anim(p.getId(), Move.CALL_PASS.ordinal(), 0));
        BallEntity b = ball();
        LivingEntity c = b == null ? null : b.getController();
        if (c instanceof ServerPlayer cp && cp != p && teamOf(cp) == t) {
            cp.displayClientMessage(Component.translatable("msg.rabonaarena.pass_request", p.getDisplayName()).withStyle(ChatFormatting.GREEN), true);
        }
    }

    public ServerPlayer passRequester(Team t) {
        long now = level().getGameTime();
        passRequests.values().removeIf(v -> v < now);
        for (UUID u : passRequests.keySet()) {
            ServerPlayer p = SERVER.getPlayerList().getPlayer(u);
            if (p != null && teamOf(p) == t) return p;
        }
        return null;
    }

    public void clearPassRequest(ServerPlayer p) { passRequests.remove(p.getUUID()); }

    public Pos posOf(LivingEntity e) {
        if (e instanceof FootballerEntity f) return f.getFieldPos();
        Pos p = playerPos.get(e.getUUID());
        return p == null ? Pos.ST : p;
    }

    public void setPlayerPos(ServerPlayer p, Pos pos) {
        playerPos.put(p.getUUID(), pos);
        assignRoles();
        Cards.applySquad(this, teamOf(p));
        broadcast(Component.translatable("msg.rabonaarena.pos_set", p.getDisplayName(), pos.title()));
        sync();
    }

    /** Insanlar sectikleri mevkiyi alir, botlar dizilisteki kalan mevkileri doldurur. */
    public void assignRoles() {
        for (Team t : new Team[]{Team.RED, Team.BLUE}) {
            List<ServerPlayer> humans = playersOf(t);
            List<FootballerEntity> list = new ArrayList<>();
            for (FootballerEntity f : bots()) if (f.getSquad() == t) list.add(f);
            int n = humans.size() + list.size();
            if (n == 0) continue;
            List<Pos> free = new ArrayList<>(List.of(Pos.formation(n, tactic(t))));
            for (ServerPlayer h : humans) {
                Pos want = posOf(h);
                Pos best = null;
                double bd = 9;
                for (Pos fp : free) {
                    double d = fp.dist(want) + (fp == want ? -1 : 0);
                    if (d < bd) { bd = d; best = fp; }
                }
                if (best != null) free.remove(best);
            }
            list.sort(Comparator.comparingInt(FootballerEntity::getNumber));
            List<FootballerEntity> unplaced = new ArrayList<>();
            for (FootballerEntity f : list) {
                if (free.remove(f.getFieldPos())) continue;
                unplaced.add(f);
            }
            unplaced.sort(Comparator.comparingInt(f -> f.isKeeper() ? 0 : 1));
            for (FootballerEntity f : unplaced) {
                Pos p = Pos.CM;
                if (!free.isEmpty()) {
                    Pos cur = f.getFieldPos();
                    p = free.get(0);
                    for (Pos fp : free) {
                        boolean gkOk = (fp == Pos.GK) == (cur == Pos.GK);
                        boolean bestOk = (p == Pos.GK) == (cur == Pos.GK);
                        if ((gkOk && !bestOk) || (gkOk == bestOk && fp.dist(cur) < p.dist(cur))) p = fp;
                    }
                    free.remove(p);
                }
                f.setFieldPos(p);
                f.randomStats(f.getSkill());
            }
            boolean one = false;
            for (FootballerEntity f : list) if (f.getNumber() == 1) one = true;
            for (FootballerEntity f : list) if (f.isKeeper() && !one) { f.setNumber(1); one = true; }
        }
    }

    /**
     * Mevkiye gore hedef konum. Topa sahip takim one ve kanatlara acilir, savunan takim kompaktlasir.
     * b ekseni takim yonune gore (sol bek her zaman hucum yonune gore solda).
     */
    public Vec3 formationSpot(Team t, Pos p, Vec3 ballPos, boolean possession, boolean kickoff) {
        int s = attackSign(t);
        double ba = ballPos == null ? 0 : pitch.a(ballPos) * s / Pitch.HALF_LEN;
        double bb = ballPos == null ? 0 : pitch.b(ballPos) * s / Pitch.HALF_WID;
        double a, b;
        if (kickoff) {
            a = Math.min(p.a, -0.06) * 0.92;
            b = p.b;
        } else if (p == Pos.GK) {
            a = -0.95;
            b = Mth.clamp(bb * 0.1, -0.08, 0.08);
        } else {
            Tactic tc = tactic(t);
            double shift = switch (p.role) {
                case DEF -> possession ? 0.22 : -0.04;
                case MID -> possession ? 0.26 : -0.12;
                default -> possession ? 0.2 : -0.16 + tc.counter;
            };
            a = p.a + shift + ba * 0.38 + (p.role == Pos.Role.FWD ? tc.line * 0.4 : tc.line);
            if (p.role == Pos.Role.DEF) a = Math.min(a, possession && p.wide() ? 0.5 : 0.25);
            if (p.role == Pos.Role.FWD) a = Math.max(a, -0.25);
            a = Mth.clamp(a, -0.88, 0.84);
            b = p.b * (possession ? 1.12 * tc.width : 0.78 * Math.min(1, tc.width + 0.1)) + bb * (possession ? 0.14 : 0.32);
            b = Mth.clamp(b, -0.93, 0.93);
        }
        return pitch.world(a * Pitch.HALF_LEN * s, b * Pitch.HALF_WID * s, pitch.surfaceY());
    }

    /** Takim topa sahip mi? */
    public boolean hasPossession(Team t) {
        BallEntity b = ball();
        if (b == null) return false;
        LivingEntity c = b.getController();
        if (c != null) return teamOf(c) == t;
        Entity last = level().getEntity(b.lastToucherId);
        return last != null && teamOf(last) == t;
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
        resetSquads();
        assignRoles();
        for (Team t : new Team[]{Team.RED, Team.BLUE}) Cards.applySquad(this, t);
        broadcast(Component.translatable("msg.rabonaarena.match_start", durationMin).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        announceManagers();
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
        // santrayi en ondeki oyuncu kullanir
        LivingEntity taker = null;
        double best = -9;
        for (LivingEntity e : members(t)) {
            double a = posOf(e).a + (e instanceof Player ? 0.05 : 0);
            if (a > best) { best = a; taker = e; }
        }
        for (Team tt : new Team[]{Team.RED, Team.BLUE}) {
            for (LivingEntity e : members(tt)) {
                Vec3 pos = e == taker ? pitch.world(-attackSign(tt) * 1.2, 0, pitch.surfaceY())
                        : formationSpot(tt, posOf(e), null, false, true);
                float yaw = yawToward(pos, pitch.goalCenter(attackSign(tt)));
                if (e instanceof ServerPlayer p) {
                    p.teleportTo(level(), pos.x, pos.y, pos.z, yaw, 0);
                } else {
                    e.teleportTo(pos.x, pos.y, pos.z);
                    e.setYRot(yaw);
                    e.setYHeadRot(yaw);
                    if (e instanceof FootballerEntity f) f.getNavigation().stop();
                }
                e.setDeltaMovement(Vec3.ZERO);
            }
        }
        sync();
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
                if (ball != null) {
                    checkBall(ball);
                    autoSwitch(ball);
                    autoSwitchP2(ball);
                }
            }
            case GOAL -> {
                if (--phaseTimer <= 0) kickoff(kickoffTeam);
            }
            case SET_PIECE -> {
                timeLeft--;
                setPieceTick(ball);
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
        Scheduler.later(40, this::autoSubs);
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
        Team winner = scoreRed == scoreBlue ? Team.NONE : scoreRed > scoreBlue ? Team.RED : Team.BLUE;
        for (Team t : new Team[]{Team.RED, Team.BLUE}) {
            for (ServerPlayer p : playersOf(t)) {
                Cards.reward(p, winner == Team.NONE ? 70 : winner == t ? 150 : 40, winner == Team.NONE ? "draw" : winner == t ? "win" : "loss");
                if (p == mvp) Cards.reward(p, 60, "mvp");
            }
        }
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
            if (le instanceof ServerPlayer sp) Cards.reward(sp, 50, "goal");
            if (prev instanceof LivingEntity pe && pe != le && teamOf(pe) == scorerTeam) {
                if (pe instanceof ServerPlayer sp2) Cards.reward(sp2, 30, "assist");
                Athlete.of(pe).assists++;
                Athlete.of(pe).addEnergy(15);
                assistName = pe.getName().getString();
            }
            if (le instanceof FootballerEntity fb) {
                Move[] c = {Move.CELEB_SIU, Move.CELEB_KNEESLIDE, Move.CELEB_BACKFLIP, Move.CELEB_PLANE, Move.CELEB_DANCE, Move.CELEB_SHUSH, Move.CELEB_SHIRT_OFF};
                Scheduler.later(12, () -> MoveLogic.tryPerform(fb, c[fb.getRandom().nextInt(c.length)], 1, 0, null));
            } else if (le instanceof ServerPlayer sp) {
                sp.displayClientMessage(Component.translatable("msg.rabonaarena.celebrate_hint").withStyle(ChatFormatting.GOLD), true);
            }
        }
        if (own) scorerName = scorerName + " (K.K.)";
        lastScorer = scorerName;
        String moveName = ball.lastMove >= 0 ? Move.byId(ball.lastMove).key() : "";
        phase = Phase.GOAL;
        phaseTimer = 200; // istemci bu surede gol tekrarini oynatir
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
