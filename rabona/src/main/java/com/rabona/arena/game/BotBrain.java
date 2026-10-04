package com.rabona.arena.game;

import com.rabona.arena.entity.BallEntity;
import com.rabona.arena.entity.FootballerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Bot yapay zekasi (mevki tabanli).
 * <ul>
 *   <li>Topla: pas / sut / top surme secenekleri puanlanir; gercek takimlar gibi paslasarak oyun kurar.</li>
 *   <li>Topsuz hucum: destek ucgenleri, derin kosular, bek bindirmeleri.</li>
 *   <li>Savunma: tek pres + bir kapatma, digerleri bolge savunmasi ve markaj.</li>
 *   <li>Kaleci: sut tahmini, plonjon, oyunu geriden kurma.</li>
 * </ul>
 */
public class BotBrain {
    private final FootballerEntity bot;
    private int decideCd, holdTimer, skillCd, jumpCd, idleLook, slideCd, receivedAt, runUntil;
    private boolean hadBall;
    private Vec3 runTarget;

    private static final Move[] SKILLS = {Move.BODY_FEINT, Move.STEPOVER, Move.ELASTICO, Move.ROULETTE, Move.CROQUETA,
            Move.CHOP, Move.DRAGBACK, Move.CRUYFF, Move.BODY_FEINT, Move.STEPOVER, Move.NUTMEG, Move.RAINBOW,
            Move.SOMBRERO, Move.FAKE_SHOT, Move.HEEL_FLICK, Move.SPEED_BURST};

    public BotBrain(FootballerEntity bot) { this.bot = bot; }

    private RandomSource rnd() { return bot.getRandom(); }

    public void tick() {
        if (bot.getServer() == null) return;
        Match m = Match.get(bot.getServer());
        Athlete a = Athlete.of(bot);
        if (skillCd > 0) skillCd--;
        if (jumpCd > 0) jumpCd--;
        if (slideCd > 0) slideCd--;
        if (a.stun > 0 || a.busy()) {
            if (a.busy() && a.current.cat == Move.Cat.CELEBRATION) return;
            stop();
            return;
        }
        if (m.pitch == null || !Match.isRunning() || bot.level() != m.level()) {
            idle();
            return;
        }
        BallEntity ball = m.ball();
        switch (m.phase) {
            case KICKOFF, HALFTIME -> {
                stop();
                if (ball != null) lookAt(ball.center());
                return;
            }
            case GOAL, ENDED -> {
                goTo(m.formationSpot(bot.getSquad(), bot.getFieldPos(), null, false, true), 0.8);
                return;
            }
            default -> {}
        }
        if (ball == null) {
            idle();
            return;
        }
        play(m, a, ball);
    }

    // ================================================================ ana karar
    private void play(Match m, Athlete a, BallEntity ball) {
        Team team = bot.getSquad();
        LivingEntity carrier = ball.getController();
        boolean mine = carrier == bot;
        lookAt(ball.center());
        if (!mine) hadBall = false;

        if (mine && ball.isHeld()) {
            keeperDistribute(m, a, ball);
            return;
        }
        if (mine) {
            onBall(m, a, ball);
            return;
        }
        boolean restricted = ball.restrictTeam >= 0 && ball.tickCount < ball.restrictUntil;
        if (restricted && ball.restrictTeam != team.ordinal()) {
            Vec3 spot = m.formationSpot(team, bot.getFieldPos(), ball.position(), false, false);
            if (spot.distanceTo(ball.position()) < 6) spot = ball.position().add(MoveLogic.flat(spot.subtract(ball.position())).scale(6.5));
            goTo(clamp(m, spot), 1.0);
            return;
        }
        if (bot.isKeeper()) {
            keeper(m, a, ball);
            return;
        }
        if (aerial(m, ball)) return;

        if (carrier != null && ball.isHeld() && Match.teamOf(carrier) != team) {
            goTo(clamp(m, m.formationSpot(team, bot.getFieldPos(), ball.position(), false, false)), 0.9);
        } else if (carrier != null && Match.teamOf(carrier) == team) {
            offBall(m, a, ball, carrier);
        } else if (carrier != null) {
            defend(m, a, ball, carrier);
        } else {
            looseBall(m, a, ball, restricted);
        }
    }

    // ================================================================ topla
    private void onBall(Match m, Athlete a, BallEntity ball) {
        if (!hadBall) {
            hadBall = true;
            receivedAt = bot.tickCount;
            decideCd = 6 + rnd().nextInt(8);
        }
        Team team = bot.getSquad();
        Pitch p = m.pitch;
        int s = m.attackSign(team);
        Vec3 me = bot.position();
        Vec3 goal = p.goalCenter(s);
        int held = bot.tickCount - receivedAt;

        // insan takim arkadasi pas istedi
        ServerPlayer req = m.passRequester(team);
        if (req != null && held > 3 && req.distanceTo(bot) > 3 && req.distanceTo(bot) < 42) {
            Vec3 target = req.position().add(Athlete.of(req).vel.scale(10));
            boolean blocked = laneBlocked(m, me, target);
            Move pass = blocked || req.distanceTo(bot) > 24 ? Move.PASS_LOB : aTeam(m, req.position()) > aTeam(m, me) + 5 ? Move.PASS_THROUGH : Move.PASS_SHORT;
            faceTo(target);
            if (MoveLogic.tryPerform(bot, pass, 1, 0, target)) {
                m.clearPassRequest(req);
                return;
            }
        }

        if (--decideCd > 0) {
            carry(m, a, ball);
            return;
        }
        decideCd = 5 + rnd().nextInt(6);

        LivingEntity front = nearestOpponent(m, 3.4, 0.25);
        LivingEntity close = nearestOpponent(m, 2.6, -2);
        double shoot = shootScore(m, ball);
        PassOption pass = bestPass(m, held, close != null);
        double dribble = dribbleScore(m, held);

        if (shoot > 0.5 && rnd().nextFloat() < 0.45 + shoot * 0.5) {
            shoot(m, a, ball, MoveLogic.horiz(me, goal));
            return;
        }
        if (pass != null && pass.score > dribble) {
            doPass(m, pass);
            return;
        }
        // 1e1: calim
        if (front != null && front.distanceTo(bot) < 2.8 && skillCd <= 0 && rnd().nextFloat() < bot.dribbling / 160f + m.difficulty * 0.05) {
            Move sk = SKILLS[rnd().nextInt(SKILLS.length)];
            int side = MoveLogic.right(MoveLogic.face(bot)).dot(front.position().subtract(me)) > 0 ? -1 : 1;
            faceTo(goal);
            if (MoveLogic.tryPerform(bot, sk, 1, side, null)) {
                skillCd = 50 + rnd().nextInt(50);
                return;
            }
        }
        // ozel yetenek (zor seviyede daha sik)
        if (a.energy >= 60 && rnd().nextFloat() < 0.015 * (m.difficulty + 1)) {
            if (MoveLogic.tryPerform(bot, rnd().nextBoolean() ? Move.AB_THUNDER : Move.AB_GHOST, 1, 0, null)) return;
        }
        // baski altinda ve iyi secenek yok: guvenli pas
        if (close != null && pass != null && pass.score > -0.6) {
            doPass(m, pass);
            return;
        }
        carry(m, a, ball);
    }

    private record PassOption(LivingEntity mate, Vec3 target, Move move, double score) {}

    private PassOption bestPass(Match m, int held, boolean pressured) {
        Team team = bot.getSquad();
        Vec3 me = bot.position();
        double myA = aTeam(m, me);
        PassOption best = null;
        for (LivingEntity mate : m.members(team)) {
            if (mate == bot || !mate.isAlive()) continue;
            boolean gk = mate instanceof FootballerEntity f && f.isKeeper();
            double d = mate.distanceTo(bot);
            if (d < 4 || d > 42) continue;
            double mateA = aTeam(m, mate.position());
            if (gk && !(pressured && myA < -Pitch.HALF_LEN * 0.4)) continue;
            double prog = (mateA - myA) / Pitch.HALF_LEN;
            double open = Math.min(8, minOppDist(m, mate.position())) / 8;
            Vec3 tgt = mate.position().add(Athlete.of(mate).vel.scale(8));
            boolean blocked = laneBlocked(m, me, tgt);
            Move mv;
            Vec3 target = tgt;
            if (d > 26 || (blocked && d > 10)) {
                mv = Move.PASS_LOB;
            } else if (prog > 0.12 && open > 0.5 && rnd().nextFloat() < 0.6) {
                mv = Move.PASS_THROUGH;
                target = tgt.add(m.attackDir(bot).scale(4));
            } else {
                mv = Move.PASS_SHORT;
            }
            // kanattan ortaya orta
            Pos myPos = bot.getFieldPos();
            if (myPos.wide() && myA > Pitch.HALF_LEN * 0.55 && Math.abs(m.pitch.b(me)) > Pitch.HALF_WID * 0.45
                    && mateA > Pitch.HALF_LEN - Pitch.BOX_DEPTH && Math.abs(m.pitch.b(mate.position())) < Pitch.BOX_HALF) {
                mv = rnd().nextFloat() < 0.12 ? Move.PASS_RABONA : Move.PASS_CROSS;
                target = mate.position().add(0, 0.6, 0);
                blocked = false;
            }
            double score = prog * 1.3 + open * 0.9 - (blocked && mv != Move.PASS_LOB && mv != Move.PASS_CROSS ? 1.4 : 0);
            if (mv == Move.PASS_LOB) score -= 0.25;
            if (d < 7) score -= 0.25;
            if (d > 30) score -= (d - 30) * 0.04;
            if (mate instanceof Player) score += 0.3;
            if (pressured) score += 0.35;
            if (held > 40) score += Math.min(0.6, (held - 40) * 0.012);
            score += (bot.passing - 70) * 0.008;
            Pos mp = m.posOf(mate);
            if (mp.role == Pos.Role.FWD && mateA > 0) score += 0.15;
            score += (rnd().nextDouble() - 0.5) * 0.25;
            if (best == null || score > best.score) best = new PassOption(mate, target, mv, score);
        }
        return best;
    }

    private void doPass(Match m, PassOption o) {
        Move mv = o.move;
        if (mv == Move.PASS_SHORT && rnd().nextFloat() < 0.05) {
            double behind = MoveLogic.face(bot).dot(MoveLogic.flat(o.target.subtract(bot.position())));
            mv = behind < -0.4 ? Move.PASS_BACKHEEL : Move.PASS_NOLOOK;
        }
        if (mv != Move.PASS_BACKHEEL && mv != Move.PASS_NOLOOK) faceTo(o.target);
        int side = MoveLogic.right(MoveLogic.face(bot)).dot(o.target.subtract(bot.position())) > 0 ? 1 : -1;
        if (!MoveLogic.tryPerform(bot, mv, 1, side, o.target)) MoveLogic.tryPerform(bot, Move.PASS_SHORT, 1, side, o.target);
    }

    private double shootScore(Match m, BallEntity ball) {
        Pitch p = m.pitch;
        int s = m.attackSign(bot.getSquad());
        Vec3 me = bot.position();
        Vec3 goal = p.goalCenter(s);
        double d = MoveLogic.horiz(me, goal);
        if (d > 27) return 0;
        double b = Math.abs(p.b(me));
        double angle = Math.max(0, 1 - b / (Pitch.HALF_WID * 0.75));
        int blockers = 0;
        for (LivingEntity o : m.members(bot.getSquad().opponent())) {
            if (o instanceof FootballerEntity f && f.isKeeper()) continue;
            if (segDist(me, goal, o.position()) < 1.3 && o.distanceTo(bot) < d) blockers++;
        }
        double sc = (1 - d / 28) * angle * Math.pow(0.55, blockers) + (bot.shooting - 70) * 0.006;
        if (d < 11) sc += 0.3;
        if (bot.getFieldPos().role == Pos.Role.DEF) sc -= 0.25;
        return sc;
    }

    private double dribbleScore(Match m, int held) {
        LivingEntity front = nearestOpponent(m, 10, 0.35);
        double space = front == null ? 1 : Math.min(10, front.distanceTo(bot)) / 10;
        double sc = space * 0.9 - held / 90.0 + (bot.dribbling - 70) * 0.01;
        Pos.Role r = bot.getFieldPos().role;
        if (r == Pos.Role.FWD) sc += 0.2;
        if (r == Pos.Role.DEF && aTeam(m, bot.position()) < 0) sc -= 0.35;
        return sc;
    }

    /** Topu surer: kaleye, kanat oyuncusu once cizgiye. */
    private void carry(Match m, Athlete a, BallEntity ball) {
        Pitch p = m.pitch;
        int s = m.attackSign(bot.getSquad());
        Vec3 me = bot.position();
        Vec3 att = m.attackDir(bot);
        Vec3 goal = p.goalCenter(s);
        Vec3 dir;
        Pos pos = bot.getFieldPos();
        double myA = aTeam(m, me);
        if (pos.wide() && myA < Pitch.HALF_LEN * 0.6) {
            dir = att; // kanatta cizgiye kos
        } else {
            dir = MoveLogic.flat(goal.subtract(me));
        }
        LivingEntity threat = nearestOpponent(m, 4.5, 0.1);
        if (threat != null) {
            Vec3 away = MoveLogic.flat(me.subtract(threat.position()));
            Vec3 side = MoveLogic.right(dir);
            double sgn = Math.signum(side.dot(away));
            if (sgn == 0) sgn = 1;
            dir = dir.add(side.scale(0.75 * sgn)).normalize();
        }
        double lb = p.b(me);
        if (Math.abs(lb) > Pitch.HALF_WID - 3) dir = dir.add(p.axisB().scale(-Math.signum(lb) * 0.8)).normalize();
        boolean space = threat == null;
        goTo(me.add(dir.scale(4)), space && a.stamina > 30 ? 1.25 : 0.95);
    }

    private void shoot(Match m, Athlete a, BallEntity ball, double dist) {
        Pitch p = m.pitch;
        int s = m.attackSign(bot.getSquad());
        LivingEntity gk = null;
        for (LivingEntity e : m.members(bot.getSquad().opponent())) if (e instanceof FootballerEntity f && f.isKeeper()) gk = e;
        double gkB = gk == null ? 0 : p.b(gk.position());
        double b = (gkB > 0 ? -1 : 1) * (Pitch.GOAL_HALF_W - 0.7 - rnd().nextDouble() * 1.2);
        double h = 0.3 + rnd().nextDouble() * (Pitch.GOAL_H - 0.9);
        Vec3 target = p.world(s * (p.goalLineA() + 0.4), b, p.surfaceY() + h);
        boolean gkOut = gk != null && Math.abs(p.a(gk.position())) < p.goalLineA() - 4;
        float r = rnd().nextFloat();
        Move shot;
        if (a.energy >= 100 && rnd().nextFloat() < 0.15 + m.difficulty * 0.15) shot = rnd().nextBoolean() ? Move.AB_FIRE : Move.AB_TORNADO;
        else if (gkOut && dist < 18 && r < 0.6) shot = Move.SHOT_CHIP;
        else if (dist < 9 && r < 0.2) shot = Move.SHOT_TOEPOKE;
        else if (r < 0.4) shot = Move.SHOT_POWER;
        else if (r < 0.72) shot = Move.SHOT_FINESSE;
        else if (r < 0.82) shot = Move.SHOT_TRIVELA;
        else if (r < 0.9 && dist > 16) shot = Move.SHOT_KNUCKLE;
        else if (r < 0.94) shot = Move.SHOT_RABONA;
        else shot = Move.SHOT_POWER;
        faceTo(target);
        int side = b > 0 ? -1 : 1;
        float power = 0.7f + rnd().nextFloat() * 0.3f;
        if (!MoveLogic.tryPerform(bot, shot, power, side, target)) MoveLogic.tryPerform(bot, Move.SHOT_POWER, power, side, target);
    }

    // ================================================================ topsuz hucum
    private void offBall(Match m, Athlete a, BallEntity ball, LivingEntity carrier) {
        Team team = bot.getSquad();
        Pos pos = bot.getFieldPos();
        Vec3 att = m.attackDir(bot);
        Vec3 spot = m.formationSpot(team, pos, ball.position(), true, false);
        double carrierA = aTeam(m, carrier.position());
        // derin kosu (forvet / kanat), arada bir
        if (runUntil > bot.tickCount && runTarget != null) {
            goTo(clamp(m, runTarget), 1.3);
            return;
        }
        if ((pos.role == Pos.Role.FWD || pos == Pos.AM) && carrierA < Pitch.HALF_LEN * 0.6 && rnd().nextFloat() < 0.02) {
            double line = deepestDefenderA(m);
            Vec3 t = m.pitch.world(m.attackSign(team) * Math.min(Pitch.HALF_LEN - 4, line + 4), m.pitch.b(spot), m.pitch.surfaceY());
            runTarget = t;
            runUntil = bot.tickCount + 45;
            return;
        }
        // bek bindirmesi
        if ((pos == Pos.LB || pos == Pos.RB) && Math.signum(m.pitch.b(ball.position()) * m.attackSign(team)) == Math.signum(pos.b)
                && carrierA > -Pitch.HALF_LEN * 0.2) {
            spot = spot.add(att.scale(8));
        }
        // destek ucgeni: en yakin iki takim arkadasi kisa pas secenegi olur
        if (supportRank(m, carrier) < 2 && pos.role != Pos.Role.DEF) {
            double sideSign = Math.signum(m.pitch.b(bot.position()) - m.pitch.b(carrier.position()));
            if (sideSign == 0) sideSign = 1;
            Vec3 off = att.scale(5).add(m.pitch.axisB().scale(sideSign * 7));
            spot = carrier.position().add(off);
        }
        if (spot.distanceTo(carrier.position()) < 5) spot = spot.add(MoveLogic.flat(spot.subtract(carrier.position())).scale(4));
        spot = clamp(m, spot);
        double d = bot.position().distanceTo(spot);
        goTo(spot, d > 7 ? 1.25 : d > 2.5 ? 1.0 : 0.6);
    }

    private int supportRank(Match m, LivingEntity carrier) {
        double mine = bot.distanceTo(carrier);
        int closer = 0;
        for (LivingEntity e : m.members(bot.getSquad())) {
            if (e == bot || e == carrier || (e instanceof FootballerEntity f && f.isKeeper())) continue;
            if (e.distanceTo(carrier) < mine) closer++;
        }
        return closer;
    }

    private double deepestDefenderA(Match m) {
        double line = -99;
        for (LivingEntity o : m.members(bot.getSquad().opponent())) {
            if (o instanceof FootballerEntity f && f.isKeeper()) continue;
            line = Math.max(line, aTeam(m, o.position()));
        }
        return line == -99 ? Pitch.HALF_LEN * 0.5 : line;
    }

    // ================================================================ savunma
    private void defend(Match m, Athlete a, BallEntity ball, LivingEntity carrier) {
        Team team = bot.getSquad();
        int rank = rankTo(m, carrier.position());
        double d = bot.distanceTo(carrier);
        int diff = m.difficulty;
        Vec3 own = m.ownGoal(team);
        if (rank == 0) {
            // pres: kaleye giden yolu kapat, dogru anda mudahale
            Vec3 contain = carrier.position().add(MoveLogic.flat(own.subtract(carrier.position())).scale(1.3));
            goTo(d < 2.4 ? carrier.position() : contain, a.stamina > 15 ? 1.25 : 0.95);
            faceTo(carrier.position());
            double loose = MoveLogic.horiz(ball.center(), carrier.position());
            double chance = 0.025 + bot.defending / 2400.0 + diff * 0.012 + (loose > 1.1 ? 0.12 : 0);
            if (d < 2.1 && rnd().nextFloat() < chance) {
                MoveLogic.tryPerform(bot, rnd().nextFloat() < 0.15 ? Move.SHOULDER : Move.TACKLE, 1, 0, null);
            } else if (slideCd <= 0 && d > 2.2 && d < 3.5 && a.stamina > 40 && rnd().nextFloat() < 0.003 * (diff + 1)) {
                if (MoveLogic.tryPerform(bot, Move.SLIDE, 1, 0, null)) slideCd = 260;
            }
            if (a.energy >= 70 && d < 6 && rnd().nextFloat() < 0.002 * (diff + 1)) {
                MoveLogic.tryPerform(bot, rnd().nextBoolean() ? Move.AB_ICE : Move.AB_MAGNET, 1, 0, null);
            }
            return;
        }
        if (rank == 1) {
            // kapatma: presin arkasinda, kale ile top arasinda
            Vec3 cover = carrier.position().add(MoveLogic.flat(own.subtract(carrier.position())).scale(5));
            goTo(clamp(m, cover), 1.1);
            return;
        }
        Vec3 bv = ball.getDeltaMovement();
        if (bv.length() > 0.8 && bot.distanceTo(ball) < 3.2 && rnd().nextFloat() < 0.25) {
            MoveLogic.tryPerform(bot, Move.BLOCK, 1, 0, null);
            return;
        }
        // bolge + markaj
        Vec3 spot = m.formationSpot(team, bot.getFieldPos(), ball.position(), false, false);
        LivingEntity mark = null;
        double md = 9;
        for (LivingEntity o : m.members(team.opponent())) {
            if (o == carrier || (o instanceof FootballerEntity f && f.isKeeper())) continue;
            double dd = o.position().distanceTo(spot);
            if (dd < md) { md = dd; mark = o; }
        }
        if (mark != null) spot = mark.position().add(MoveLogic.flat(own.subtract(mark.position())).scale(1.8));
        goTo(clamp(m, spot), bot.position().distanceTo(spot) > 6 ? 1.2 : 0.95);
    }

    /** Takimimda bu noktaya en yakin kacinci oyuncuyum (kaleci haric). */
    private int rankTo(Match m, Vec3 p) {
        double mine = bot.position().distanceTo(p);
        int closer = 0;
        for (LivingEntity e : m.members(bot.getSquad())) {
            if (e == bot || (e instanceof FootballerEntity f && f.isKeeper())) continue;
            if (e.position().distanceTo(p) < mine) closer++;
        }
        return closer;
    }

    // ================================================================ sahipsiz top
    private void looseBall(Match m, Athlete a, BallEntity ball, boolean restricted) {
        Team team = bot.getSquad();
        Vec3 bp = ball.position();
        int rank = rankTo(m, bp);
        if (rank == 0 || (rank == 1 && !restricted && bot.distanceTo(ball) < 10)) {
            Vec3 v = ball.getDeltaMovement();
            double d = bot.position().distanceTo(bp);
            Vec3 pred = bp.add(v.scale(Math.min(16, d / 0.25)));
            goTo(clamp(m, pred), a.stamina > 10 ? 1.3 : 1.0);
        } else {
            goTo(clamp(m, m.formationSpot(team, bot.getFieldPos(), bp, m.hasPossession(team), false)), 1.0);
        }
    }

    /** Havadaki top: kafa, vole, rovasata, gogusle kontrol. */
    private boolean aerial(Match m, BallEntity ball) {
        if (!ball.isFree()) return false;
        Vec3 bc = ball.center();
        double h = MoveLogic.horiz(bc, bot.position());
        double rel = bc.y - bot.getY();
        if (h > 2.6 || rel < 0.5) return false;
        Pitch p = m.pitch;
        int s = m.attackSign(bot.getSquad());
        Vec3 goal = p.goalCenter(s);
        double dGoal = MoveLogic.horiz(bot.position(), goal);
        Vec3 target;
        if (dGoal < 22) {
            target = p.world(s * (p.goalLineA() + 0.3), (rnd().nextDouble() - 0.5) * 5, p.surfaceY() + 0.8 + rnd().nextDouble());
        } else {
            LivingEntity mate = null;
            double best = 99;
            for (LivingEntity e : m.members(bot.getSquad())) {
                if (e == bot) continue;
                double dd = Math.abs(e.distanceTo(bot) - 12);
                if (dd < best && aTeam(m, e.position()) > aTeam(m, bot.position()) - 4) { best = dd; mate = e; }
            }
            target = mate != null ? mate.position().add(0, 1, 0) : bot.position().add(p.axisA().scale(s * 18)).add(0, 1, 0);
        }
        if (rel > 2.1 && rel < 3.0 && jumpCd <= 0 && bot.onGround()) {
            bot.getJumpControl().jump();
            jumpCd = 15;
        }
        Move mv;
        if (rel > 1.25) mv = (dGoal < 14 && rnd().nextFloat() < 0.25) ? Move.DIVING_HEADER : Move.HEADER;
        else if (dGoal < 20) {
            float r = rnd().nextFloat();
            mv = r < 0.08 ? Move.BICYCLE : r < 0.12 ? Move.SCORPION : r < 0.7 ? Move.VOLLEY : Move.CHEST_CONTROL;
        } else mv = rnd().nextFloat() < 0.7 ? Move.CHEST_CONTROL : Move.HEADER;
        if (rnd().nextFloat() > 0.35) return false;
        faceTo(target);
        return MoveLogic.tryPerform(bot, mv, 0.9f, 0, target);
    }

    // ================================================================ kaleci
    private void keeper(Match m, Athlete a, BallEntity ball) {
        Team team = bot.getSquad();
        Pitch p = m.pitch;
        int own = -m.attackSign(team);
        Vec3 goal = p.goalCenter(own);
        Vec3 bc = ball.center();
        double ba = p.a(bc), bb = p.b(bc);
        double goalA = own * p.goalLineA();
        double distBall = MoveLogic.horiz(bc, goal);
        Vec3 v = ball.getDeltaMovement();
        double va = v.dot(p.axisA()), vb = v.dot(p.axisB());
        double myA = p.a(bot.position()), myB = p.b(bot.position());
        if (ball.isFree() && va * own > 0.25) {
            double t = (myA - ba) / va;
            if (t > 0 && t < 22) {
                double predB = bb + vb * t;
                double predY = bc.y + v.y * t - 0.5 * BallEntity.GRAVITY * t * t - p.surfaceY();
                double off = predB - myB;
                int react = 6 + m.difficulty * 3;
                if (Math.abs(predB) < Pitch.GOAL_HALF_W + 1.2 && predY < Pitch.GOAL_H + 0.6) {
                    if (Math.abs(off) > 1.1 && t <= react && rnd().nextFloat() < 0.55 + m.difficulty * 0.15) {
                        Vec3 worldOff = p.axisB().scale(off);
                        boolean right = MoveLogic.right(MoveLogic.face(bot)).dot(worldOff) > 0;
                        faceTo(bot.position().add(p.axisA().scale(-own)));
                        MoveLogic.tryPerform(bot, right ? Move.GK_DIVE_RIGHT : Move.GK_DIVE_LEFT, 1, 0, null);
                        return;
                    }
                    goTo(p.world(myA, Mth.clamp(predB, -Pitch.GOAL_HALF_W + 0.4, Pitch.GOAL_HALF_W - 0.4), p.surfaceY()), 1.3);
                    return;
                }
            }
        }
        if (ball.isFree() && bc.distanceTo(bot.position().add(0, 0.9, 0)) < 1.45 && inOwnBox(m, bot.position())) {
            MoveLogic.catchOrParry(bot, a, ball);
            holdTimer = 30 + rnd().nextInt(30);
            return;
        }
        LivingEntity carrier = ball.getController();
        boolean danger = inOwnBox(m, bc) && (ball.isFree() && v.length() < 0.5 || carrier != null && Match.teamOf(carrier) != team && distBall < 9);
        if (danger) {
            goTo(ball.position(), 1.3);
            if (carrier != null && bot.distanceTo(carrier) < 2.0 && rnd().nextFloat() < 0.15) MoveLogic.tryPerform(bot, Move.TACKLE, 1, 0, null);
            return;
        }
        double depth = Mth.clamp(distBall * 0.07, 0.8, 3.2);
        Vec3 dir = MoveLogic.flat(bc.subtract(goal));
        Vec3 pos = goal.add(dir.scale(depth));
        double pb = Mth.clamp(p.b(pos), -Pitch.GOAL_HALF_W + 0.6, Pitch.GOAL_HALF_W - 0.6);
        double pa = goalA - own * Mth.clamp(Math.abs(p.a(pos) - goalA), 0.8, 3.2);
        goTo(p.world(pa, pb, p.surfaceY()), 1.0);
    }

    /** Kaleci oyunu geriden kurar: bos defans/orta saha oyuncusuna elle atar, yoksa degaj. */
    private void keeperDistribute(Match m, Athlete a, BallEntity ball) {
        stop();
        if (holdTimer <= 0) holdTimer = 35;
        if (--holdTimer > 0) return;
        Team team = bot.getSquad();
        LivingEntity mate = null;
        double best = -1e9;
        for (LivingEntity e : m.members(team)) {
            if (e == bot) continue;
            double space = minOppDist(m, e.position());
            double d = e.distanceTo(bot);
            double sc = Math.min(space, 10) - Math.abs(d - 13) * 0.25 + (m.posOf(e).role == Pos.Role.DEF ? 2 : 0) + (e instanceof Player ? 2 : 0);
            if (laneBlocked(m, bot.position(), e.position())) sc -= 6;
            if (sc > best) { best = sc; mate = e; }
        }
        if (mate != null && best > 3) {
            faceTo(mate.position());
            MoveLogic.tryPerform(bot, Move.GK_THROW, 1, 0, mate.position());
        } else {
            faceTo(bot.position().add(m.pitch.axisA().scale(m.attackSign(team) * 10)));
            MoveLogic.tryPerform(bot, Move.GK_PUNT, 0.9f, 0, null);
        }
    }

    private boolean inOwnBox(Match m, Vec3 pos) {
        Pitch p = m.pitch;
        int own = -m.attackSign(bot.getSquad());
        double a = p.a(pos) * own;
        return a > Pitch.HALF_LEN - Pitch.BOX_DEPTH - 0.5 && a < Pitch.HALF_LEN + 2 && Math.abs(p.b(pos)) < Pitch.BOX_HALF + 0.5;
    }

    // ================================================================ yardimcilar
    private double aTeam(Match m, Vec3 p) { return m.pitch.a(p) * m.attackSign(bot.getSquad()); }

    private double minOppDist(Match m, Vec3 p) {
        double d = 99;
        for (LivingEntity o : m.members(bot.getSquad().opponent())) d = Math.min(d, o.position().distanceTo(p));
        return d;
    }

    private boolean laneBlocked(Match m, Vec3 from, Vec3 to) {
        for (LivingEntity o : m.members(bot.getSquad().opponent())) {
            if (segDist(from, to, o.position()) < 1.4 && o.position().distanceTo(from) > 1.5) return true;
        }
        return false;
    }

    private static double segDist(Vec3 a, Vec3 b, Vec3 p) {
        Vec3 ab = new Vec3(b.x - a.x, 0, b.z - a.z);
        double len2 = ab.lengthSqr();
        if (len2 < 1e-6) return MoveLogic.horiz(a, p);
        double t = Mth.clamp(((p.x - a.x) * ab.x + (p.z - a.z) * ab.z) / len2, 0, 1);
        return MoveLogic.horiz(new Vec3(a.x + ab.x * t, 0, a.z + ab.z * t), p);
    }

    private void idle() {
        stop();
        if (++idleLook % 40 == 0) {
            Player p = bot.level().getNearestPlayer(bot, 12);
            if (p != null) lookAt(p.getEyePosition());
        }
    }

    private LivingEntity nearestOpponent(Match m, double r, double cone) {
        LivingEntity best = null;
        double bd = r;
        Vec3 f = MoveLogic.face(bot);
        List<LivingEntity> opp = m.members(bot.getSquad().opponent());
        for (LivingEntity o : opp) {
            double d = o.distanceTo(bot);
            if (d < bd && (cone < -1 || MoveLogic.flat(o.position().subtract(bot.position())).dot(f) > cone)) { bd = d; best = o; }
        }
        return best;
    }

    private Vec3 clamp(Match m, Vec3 v) {
        Pitch p = m.pitch;
        double a = Mth.clamp(p.a(v), -Pitch.HALF_LEN + 0.5, Pitch.HALF_LEN - 0.5);
        double b = Mth.clamp(p.b(v), -Pitch.HALF_WID + 0.5, Pitch.HALF_WID - 0.5);
        return p.world(a, b, p.surfaceY());
    }

    /**
     * Hareket. speed: 1.0 = oyuncunun yurume hizi, 1.3 = oyuncunun kosu (sprint) hizi.
     * Botlar oyuncudan hizli olamaz; hiz karti (pace) +-%7 etkiler.
     */
    private void goTo(Vec3 p, double speed) {
        double pace = 0.93 + bot.pace * 0.0011;
        Athlete a = Athlete.of(bot);
        if (a.stamina < 15) speed = Math.min(speed, 1.0);
        if (bot.distanceToSqr(p) < 0.6) {
            stop();
            return;
        }
        speed = Math.min(1.3, speed);
        boolean sprint = speed > 1.1;
        // setSprinting zaten +%30 hiz ekler; ikinci kez eklememek icin bol
        bot.getMoveControl().setWantedPosition(p.x, p.y, p.z, (sprint ? speed / 1.3 : speed) * pace);
        bot.setSprinting(sprint);
    }

    private void stop() {
        bot.getMoveControl().setWantedPosition(bot.getX(), bot.getY(), bot.getZ(), 0);
        bot.setSprinting(false);
        bot.setZza(0);
    }

    private void lookAt(Vec3 p) {
        bot.getLookControl().setLookAt(p.x, p.y, p.z, 40, 40);
    }

    private void faceTo(Vec3 t) {
        float yaw = Match.yawToward(bot.position(), t);
        bot.setYRot(yaw);
        bot.setYHeadRot(yaw);
        bot.yBodyRot = yaw;
        bot.yHeadRotO = yaw;
    }
}
