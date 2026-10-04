package com.krolasyon.futbol.game;

import com.krolasyon.futbol.entity.FootballEntity;
import com.krolasyon.futbol.entity.FootballerEntity;
import com.krolasyon.futbol.registry.ModSounds;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Role based football AI. Off the ball every player keeps a role specific position (defenders hold the line,
 * midfielders offer passing angles, wingers stay wide, forwards time runs behind the last defender). On the ball the
 * carrier weighs shooting, passing (progress, openness and a clear passing lane) and dribbling into space, so teams
 * build up play instead of all running at goal. Out of possession one player presses, one covers and the rest stay compact.
 */
public final class BotBrain {
    private final FootballerEntity bot;
    private int actCooldown;
    private int holdTicks;
    private int keeperHold;
    private int casualTimer;
    private int runPhase;
    private Vec3 wander;
    private LivingEntity requester;
    private long requestUntil;

    public BotBrain(FootballerEntity bot) { this.bot = bot; }

    private RandomSource rnd() { return bot.getRandom(); }

    public void passRequested(LivingEntity who) {
        requester = who;
        requestUntil = bot.level().getGameTime() + 40;
    }

    public void tick() {
        long now = bot.level().getGameTime();
        if (actCooldown > 0) actCooldown--;
        runPhase++;
        FootData fd = FootData.of(bot);
        FootballEntity ball = MatchManager.ballFor(bot);
        if (ball != null) bot.getLookControl().setLookAt(ball, 40F, 40F);
        if (now < fd.busyUntil || now < fd.stunnedUntil) return;
        boolean inMatch = MatchManager.isActive() && bot.getFootTeam().playing() && MatchManager.pitch() != null
                && MatchManager.pitch().near(bot.position(), 30);
        if (!inMatch) {
            casual(ball);
            return;
        }
        switch (MatchManager.state) {
            case KICKOFF -> goTo(MatchManager.kickoffPos(bot), 1.0);
            case GOAL -> {
                if (MatchManager.lastGoalTeam == bot.getFootTeam()) {
                    Pitch p = MatchManager.pitch();
                    Vec3 corner = new Vec3(p.lineX(bot.getFootTeam().attackDir()) - bot.getFootTeam().attackDir() * 2, p.floor(), bot.getZ() < p.midZ() ? p.zMin() + 2 : p.zMax() - 2);
                    goTo(corner, 1.1);
                    if (rnd().nextInt(90) == 0 && actCooldown == 0) {
                        Move[] c = {Move.SIUU, Move.KNEE_SLIDE, Move.AIRPLANE, Move.BACKFLIP, Move.DANCE, Move.SHIRT_OFF};
                        MoveExecutor.perform(bot, c[rnd().nextInt(c.length)], 0);
                        actCooldown = 60;
                    }
                } else {
                    goTo(MatchManager.kickoffPos(bot), 0.7);
                }
            }
            case ENDED -> {
                if (MatchManager.winner() == bot.getFootTeam() && rnd().nextInt(90) == 0 && actCooldown == 0) {
                    Move[] c = {Move.SIUU, Move.DANCE, Move.BACKFLIP, Move.AIRPLANE, Move.SHIRT_OFF};
                    MoveExecutor.perform(bot, c[rnd().nextInt(c.length)], 0);
                    actCooldown = 60;
                }
                stop();
            }
            case PLAYING -> {
                if (ball != null) play(ball, now);
            }
            default -> stop();
        }
    }

    // ------------------------------------------------------------------ movement

    private void goTo(Vec3 t, double speed) {
        if (t == null) return;
        double dx = t.x - bot.getX(), dz = t.z - bot.getZ();
        double d2 = dx * dx + dz * dz;
        if (d2 < 0.3) {
            stop();
            return;
        }
        if (d2 < 9 && speed > 0.9) speed = 0.9; // settle instead of sprinting the last metres
        bot.getMoveControl().setWantedPosition(t.x, bot.getY(), t.z, speed);
        bot.setSprinting(speed > 1.05);
    }

    private void stop() {
        bot.getMoveControl().setWantedPosition(bot.getX(), bot.getY(), bot.getZ(), 0.0);
        bot.setSprinting(false);
    }

    // ------------------------------------------------------------------ match play

    private void play(FootballEntity ball, long now) {
        Pitch p = MatchManager.pitch();
        Team my = bot.getFootTeam();
        Team opp = my.opponent();
        LivingEntity ctrl = ball.getController();
        LivingEntity holder = ball.getHolder();
        double dBall = bot.distanceTo(ball);

        if (bot.isKeeper()) {
            keeper(ball, p, ctrl, holder, now);
            return;
        }
        if (ctrl == bot) {
            carry(ball, p, now);
            return;
        }
        holdTicks = 0;
        Team ctrlTeam = ctrl != null ? MatchManager.teamOf(ctrl) : holder != null ? MatchManager.teamOf(holder) : Team.NONE;

        if (ball.restrictTeam == opp && now < ball.restrictUntil) {
            if (dBall < 6) {
                Vec3 away = MoveExecutor.flat(bot.position().subtract(ball.position())).scale(6);
                goTo(bot.position().add(away), 1.0);
            } else goTo(shapePos(p, ball, false), 1.0);
            return;
        }

        boolean loose = ctrl == null && holder == null;
        double dy = ball.getY() - bot.getY();
        if (loose && actCooldown == 0 && dBall < 2.8 && !ball.onGround()) {
            Vec3 goal = p.attackGoal(my);
            double dGoal = horiz(bot.position(), goal);
            if (dy > 1.1 && dy < 3.3) {
                Vec3 aim = dGoal < 20 ? goalAim(p, my, 0.4) : bestMateSpot(p);
                MoveExecutor.perform(bot, Move.HEADER, 0.7F, aim);
                actCooldown = 20;
                return;
            } else if (dy > 0.35 && dy <= 1.1) {
                if (dGoal < 22 && rnd().nextFloat() < 0.5F) {
                    boolean backToGoal = MoveExecutor.facing(bot).dot(MoveExecutor.flat(goal.subtract(bot.position()))) < -0.3;
                    Move mv = backToGoal && rnd().nextFloat() < 0.6F ? Move.BICYCLE : rnd().nextFloat() < 0.1F ? Move.SCORPION : Move.VOLLEY;
                    MoveExecutor.perform(bot, mv, 0.8F, goalAim(p, my, 0.5));
                } else {
                    MoveExecutor.perform(bot, Move.FIRST_TOUCH, 0, null);
                }
                actCooldown = 20;
                return;
            }
        }

        if (ctrlTeam == my && my.playing()) {
            goTo(supportPos(p, ball, ctrl != null ? ctrl : holder), 1.0 + (bot.getRole() == Role.FWD || bot.getRole() == Role.WING ? 0.1 : 0));
            return;
        }

        // interception of a loose moving ball (passes, clearances)
        if (loose && ball.getDeltaMovement().horizontalDistance() > 0.45 && ball.lastTouchTeam != my) {
            Vec3 ip = interceptPoint(ball, 3.0);
            if (ip != null) {
                goTo(ip, 1.15);
                return;
            }
        }

        LivingEntity chaser = MatchManager.chaser(my);
        LivingEntity second = MatchManager.secondChaser(my);
        if (bot == chaser) {
            if (ctrl != null && ctrlTeam != my) press(ctrl, now);
            else if (holder != null) goTo(shapePos(p, ball, false), 1.0);
            else {
                Vec3 v = ball.getDeltaMovement();
                double lead = Math.min(10, dBall * 1.8);
                goTo(ball.position().add(v.x * lead, 0, v.z * lead), 1.15);
                if (v.length() > 1.0 && dBall < 3.5 && actCooldown == 0 && ball.lastTouchTeam == opp && rnd().nextFloat() < 0.25F) {
                    MoveExecutor.perform(bot, Move.BLOCK, 0);
                    actCooldown = 30;
                }
            }
            return;
        }
        if (bot == second && ctrl != null && ctrlTeam != my) {
            // cover: sit between the ball carrier and our goal
            Vec3 own = p.ownGoal(my);
            Vec3 cover = ctrl.position().add(MoveExecutor.flat(own.subtract(ctrl.position())).scale(5.5));
            goTo(cover, 1.05);
            return;
        }
        goTo(shapePos(p, ball, true), 1.0);
    }

    /** chaser closing down the ball carrier and timing a tackle */
    private void press(LivingEntity carrier, long now) {
        double d = bot.distanceTo(carrier);
        Vec3 own = MatchManager.pitch().ownGoal(bot.getFootTeam());
        // approach goal-side of the carrier, not from behind
        Vec3 front = carrier.position().add(MoveExecutor.flat(own.subtract(carrier.position())).scale(Math.min(1.2, d * 0.4)));
        goTo(front, d > 6 ? 1.15 : 1.0);
        if (actCooldown > 0 || d > 2.4) return;
        FootData fd = FootData.of(bot);
        float r = rnd().nextFloat();
        Long slideReady = fd.readyAt.get(Move.SLIDE);
        boolean slideOk = (slideReady == null || now >= slideReady) && d > 1.4 && r < 0.06F + bot.statDefend * 0.04F;
        if (slideOk) {
            MoveExecutor.perform(bot, Move.SLIDE, 0, carrier.position());
            actCooldown = 40;
        } else if (r < 0.10F + bot.statDefend * 0.10F) {
            MoveExecutor.perform(bot, d < 1.4 && r < 0.03F ? Move.SHOULDER : Move.TACKLE, 0, carrier.position());
            actCooldown = 18 + rnd().nextInt(18);
        }
    }

    @Nullable
    private Vec3 interceptPoint(FootballEntity ball, double maxDist) {
        Vec3 pos = ball.position();
        Vec3 v = ball.getDeltaMovement().multiply(1, 0, 1);
        double speedMe = 0.27;
        for (int t = 2; t < 30; t += 2) {
            pos = pos.add(v.scale(2));
            v = v.scale(Math.pow(FootballEntity.ROLL, 2));
            double d = horiz(bot.position(), pos);
            if (d < maxDist + t * 0.05 && d / speedMe <= t + 4) return pos;
        }
        return null;
    }

    /** off-ball position when our team has the ball */
    private Vec3 supportPos(Pitch p, FootballEntity ball, LivingEntity carrier) {
        Team my = bot.getFootTeam();
        int n = Mth.clamp(MatchManager.playersPerTeam, 1, 11);
        double[] a = MatchManager.slotUV(n, bot.slot);
        double bu = p.teamU(my, ball.getX()), bv = p.teamV(my, ball.getZ());
        double u, v;
        switch (bot.getRole()) {
            case DEF -> {
                u = Mth.clamp(a[0] + (bu - 0.5) * 0.4 + 0.04, 0.12, 0.52);
                v = a[1] * 1.15;
            }
            case MID -> {
                double side = a[1] != 0 ? Math.signum(a[1]) : (bot.slot % 2 == 0 ? 1 : -1);
                u = Mth.clamp(bu + (carrier == null ? 0 : 0.03), 0.2, 0.8);
                v = Mth.clamp(bv + side * 0.38, -0.85, 0.85);
                if (Math.abs(u - bu) < 0.05 && Math.abs(v - bv) < 0.2) v += side * 0.25;
            }
            case WING -> {
                u = Mth.clamp(Math.max(a[0], bu + 0.1), 0.3, 0.88);
                v = Math.signum(a[1] == 0 ? 1 : a[1]) * 0.82;
            }
            case FWD -> {
                double line = lastLineU(p);
                boolean run = bu > 0.38 && (runPhase / 50) % 2 == 1;
                u = Mth.clamp(run ? line + 0.06 : line - 0.02, 0.45, 0.93);
                v = Mth.clamp(a[1] * 0.5 + bv * 0.2 + (run ? Math.sin(runPhase * 0.03) * 0.2 : 0), -0.5, 0.5);
            }
            default -> {
                u = a[0];
                v = a[1];
            }
        }
        return p.point(my, u, Mth.clamp(v, -0.92, 0.92));
    }

    /** compact defensive shape when the opponent has (or is about to have) the ball */
    private Vec3 shapePos(Pitch p, FootballEntity ball, boolean mark) {
        Team my = bot.getFootTeam();
        int n = Mth.clamp(MatchManager.playersPerTeam, 1, 11);
        double[] a = MatchManager.slotUV(n, bot.slot);
        double bu = p.teamU(my, ball.getX()), bv = p.teamV(my, ball.getZ());
        double u = Mth.clamp(a[0] * 0.9 + (bu - 0.5) * 0.42 - 0.04, 0.06, 0.8);
        double v = Mth.clamp(a[1] * 0.72 + bv * 0.35, -0.9, 0.9);
        if (bot.getRole() == Role.FWD) u = Math.max(u, 0.45);
        Vec3 pos = p.point(my, u, v);
        if (mark && bot.getRole() == Role.DEF) {
            // pick up the most dangerous attacker near our zone and stand goal-side of him
            LivingEntity threat = null;
            double best = 1e9;
            for (LivingEntity e : MatchManager.members(my.opponent())) {
                if (e instanceof FootballerEntity k && k.isKeeper()) continue;
                double d = horiz(e.position(), pos);
                if (d < 9 && d < best && p.teamU(my, e.getX()) < 0.55) {
                    best = d;
                    threat = e;
                }
            }
            if (threat != null) {
                Vec3 own = p.ownGoal(my);
                pos = threat.position().add(MoveExecutor.flat(own.subtract(threat.position())).scale(2.0));
            }
        }
        return pos;
    }

    /** deepest opposing outfield player (team relative u), i.e. the offside line */
    private double lastLineU(Pitch p) {
        Team my = bot.getFootTeam();
        double line = 0.6;
        for (LivingEntity e : MatchManager.members(my.opponent())) {
            if (e instanceof FootballerEntity k && k.isKeeper()) continue;
            line = Math.max(line, p.teamU(my, e.getX()));
        }
        return Math.min(line, 0.9);
    }

    // ------------------------------------------------------------------ on the ball

    private void carry(FootballEntity ball, Pitch p, long now) {
        Team my = bot.getFootTeam();
        holdTicks++;
        Vec3 goal = p.attackGoal(my);
        double dGoal = horiz(bot.position(), goal);
        LivingEntity opp = nearestOpponent(9);
        double dOpp = opp == null ? 99 : bot.distanceTo(opp);
        FootData fd = FootData.of(bot);
        double myU = p.teamU(my, bot.getX());
        boolean pressured = dOpp < 2.8;

        // a human teammate asked for the ball
        if (requester != null && now < requestUntil && requester.isAlive() && actCooldown == 0 && holdTicks > 3) {
            Vec3 t = requester.position();
            double d = horiz(bot.position(), t);
            Move mv = d > 24 ? Move.LOB_PASS : Move.SHORT_PASS;
            MoveExecutor.perform(bot, mv, 0.5F, t.add(MoveExecutor.facing(requester).scale(Math.min(3, d * 0.1))));
            requester = null;
            actCooldown = 18;
            holdTicks = 0;
            return;
        }

        if (actCooldown == 0 && (holdTicks > 8 || pressured) && holdTicks % 5 == 0) {
            double shoot = shootScore(p, dGoal);
            if (shoot > 0.5 + rnd().nextDouble() * 0.25) {
                shoot(p, dGoal, now, fd);
                return;
            }
            PassOption best = bestPass(p, pressured);
            double dribble = dribbleScore(p, myU);
            if (pressured) {
                if (rnd().nextFloat() < bot.statDribble * 0.45F) {
                    skill(fd, now, opp);
                    return;
                }
                if (best != null && best.score > -2) {
                    executePass(p, best, fd, now);
                    return;
                }
            } else if (best != null && best.score > dribble) {
                executePass(p, best, fd, now);
                return;
            }
            if (bot.getRole() == Role.WING && myU > 0.76 && Math.abs(p.teamV(my, bot.getZ())) > 0.5 && rnd().nextFloat() < 0.5F) {
                MoveExecutor.perform(bot, Move.CROSS, 0.6F);
                actCooldown = 25;
                return;
            }
            if (dOpp > 8 && rnd().nextFloat() < 0.04F) {
                MoveExecutor.perform(bot, Move.SPEED_BURST, 0);
                actCooldown = 20;
                return;
            }
        }
        // carry the ball: wingers down the line, others towards goal, steering around defenders
        Vec3 aimPoint;
        if (bot.getRole() == Role.WING && myU < 0.78) aimPoint = p.point(my, Math.min(0.95, myU + 0.2), Math.signum(p.teamV(my, bot.getZ()) + 1e-3) * 0.8);
        else if (bot.getRole() == Role.DEF && myU > 0.45) aimPoint = p.point(my, myU, p.teamV(my, bot.getZ()) * 0.5);
        else aimPoint = new Vec3(goal.x, goal.y, p.midZ() + (bot.getZ() - p.midZ()) * 0.4);
        Vec3 dir = MoveExecutor.flat(aimPoint.subtract(bot.position()));
        if (opp != null && dOpp < 6) {
            Vec3 away = MoveExecutor.flat(bot.position().subtract(opp.position()));
            dir = MoveExecutor.flat(dir.add(away.scale(0.8 * (6 - dOpp) / 6)));
        }
        Vec3 t = p.clampInside(bot.position().add(dir.scale(4)), 1.5);
        goTo(t, 0.95 + bot.statSpeed * 0.12);
    }

    private double shootScore(Pitch p, double dGoal) {
        Team my = bot.getFootTeam();
        if (dGoal > 24) return 0;
        double base = (24 - dGoal) / 24.0;
        double role = switch (bot.getRole()) {
            case FWD -> 1.35;
            case WING -> 1.05;
            case MID -> 0.85;
            default -> 0.45;
        };
        double angle = Math.abs(p.teamV(my, bot.getZ()));
        double s = base * role * (angle > 0.7 ? 0.45 : 1.0) * (0.75 + bot.statShot * 0.4);
        Vec3 goal = p.attackGoal(my).add(0, 0, 0);
        int blockers = 0;
        for (LivingEntity e : MatchManager.members(my.opponent())) {
            if (e instanceof FootballerEntity k && k.isKeeper()) continue;
            if (segDist(bot.position(), goal, e.position()) < 1.6) blockers++;
        }
        s *= Math.pow(0.6, blockers);
        if (dGoal < 11) s += 0.35;
        return s;
    }

    private double dribbleScore(Pitch p, double myU) {
        Team my = bot.getFootTeam();
        Vec3 fwd = new Vec3(my.attackDir(), 0, 0);
        double space = 12;
        for (LivingEntity e : MatchManager.members(my.opponent())) {
            Vec3 to = e.position().subtract(bot.position());
            if (MoveExecutor.flat(to).dot(fwd) > 0.3) space = Math.min(space, to.horizontalDistance());
        }
        double roleF = switch (bot.getRole()) {
            case WING -> 1.4;
            case FWD -> 1.15;
            case MID -> 0.9;
            default -> myU < 0.45 ? 0.55 : 0.2;
        };
        return (space - 4) * 0.35 * roleF + bot.statDribble - 0.2 - holdTicks * 0.02;
    }

    private record PassOption(LivingEntity mate, Vec3 target, boolean through, boolean lob, double score) {}

    @Nullable
    private PassOption bestPass(Pitch p, boolean pressured) {
        Team my = bot.getFootTeam();
        double myU = p.teamU(my, bot.getX());
        PassOption best = null;
        for (LivingEntity m : MatchManager.members(my)) {
            if (m == bot || !m.isAlive()) continue;
            boolean keeper = m instanceof FootballerEntity k && k.isKeeper();
            if (keeper && (myU > 0.3 || !pressured)) continue;
            double d = bot.distanceTo(m);
            if (d < 4 || d > 42) continue;
            double mu = p.teamU(my, m.getX());
            double prog = mu - myU;
            LivingEntity mo = nearestOpponentTo(m, 10);
            double open = mo == null ? 10 : mo.distanceTo(m);
            double lane = laneClearance(bot.position(), m.position());
            boolean lob = lane < 1.4 && d > 12;
            double s = prog * (bot.getRole() == Role.DEF ? 4 : 7) + Math.min(open, 7) * 0.35 + Math.min(lane, 4) * 0.45 - Math.abs(d - 14) * 0.04;
            if (lane < 1.2 && !lob) s -= 4;
            if (lob) s -= 0.8;
            if (m instanceof Player) s += 0.8;
            if (pressured) s += 1.0;
            if (prog < -0.1 && !pressured) s -= 1.5;
            if (best == null || s > best.score) best = new PassOption(m, m.position(), false, lob, s);
            // through ball into the run of forwards and wingers
            Role mr = m instanceof FootballerEntity fb ? fb.getRole() : Role.FWD;
            if ((mr == Role.FWD || mr == Role.WING) && mu > myU + 0.08) {
                Vec3 lead = m.position().add(my.attackDir() * 5.0, 0, 0);
                lead = p.clampInside(lead, 2);
                LivingEntity lo = nearestOpponentTo(lead, 6);
                double space = lo == null ? 6 : horiz(lo.position(), lead);
                double tl = laneClearance(bot.position(), lead);
                double ts = (p.teamU(my, lead.x) - myU) * 8 + space * 0.4 + Math.min(tl, 4) * 0.4 + 0.5 - (tl < 1.2 ? 4 : 0);
                if (ts > best.score) best = new PassOption(m, lead, true, false, ts);
            }
        }
        return best;
    }

    private void executePass(Pitch p, PassOption o, FootData fd, long now) {
        Team my = bot.getFootTeam();
        double d = horiz(bot.position(), o.target());
        Vec3 toMate = MoveExecutor.flat(o.target().subtract(bot.position()));
        double facingDot = MoveExecutor.facing(bot).dot(toMate);
        float r = rnd().nextFloat();
        Move mv;
        if (now >= fd.superReadyAt && r < 0.03F) mv = Move.ICE_PASS;
        else if (o.through()) mv = Move.THROUGH_PASS;
        else if (o.lob() || d > 26) mv = Move.LOB_PASS;
        else if (facingDot < -0.5 && r < 0.5F) mv = Move.BACKHEEL_PASS;
        else if (r < 0.05F) mv = Move.RABONA_PASS;
        else if (r < 0.10F && Math.abs(facingDot) < 0.6) mv = Move.NO_LOOK_PASS;
        else mv = Move.SHORT_PASS;
        double err = (1.0 - bot.statPass) * 1.4;
        Vec3 target = o.target().add(rnd().nextGaussian() * err, 0, rnd().nextGaussian() * err);
        MoveExecutor.perform(bot, mv, 0.5F, target);
        actCooldown = 18;
        holdTicks = 0;
    }

    private Vec3 bestMateSpot(Pitch p) {
        Team my = bot.getFootTeam();
        LivingEntity best = null;
        double bs = -1e9;
        for (LivingEntity m : MatchManager.members(my)) {
            if (m == bot) continue;
            double s = p.teamU(my, m.getX()) * 5 - bot.distanceTo(m) * 0.05;
            if (s > bs) {
                bs = s;
                best = m;
            }
        }
        return best != null ? best.position() : bot.position().add(my.attackDir() * 15, 0, 0);
    }

    private double laneClearance(Vec3 from, Vec3 to) {
        double min = 99;
        for (LivingEntity e : MatchManager.members(bot.getFootTeam().opponent())) min = Math.min(min, segDist(from, to, e.position()));
        return min;
    }

    /** horizontal distance from point c to segment a-b (99 if c projects outside the segment) */
    private static double segDist(Vec3 a, Vec3 b, Vec3 c) {
        double abx = b.x - a.x, abz = b.z - a.z;
        double len2 = abx * abx + abz * abz;
        if (len2 < 1e-6) return 99;
        double t = ((c.x - a.x) * abx + (c.z - a.z) * abz) / len2;
        if (t < 0.05 || t > 1.0) return 99;
        double px = a.x + abx * t - c.x, pz = a.z + abz * t - c.z;
        return Math.sqrt(px * px + pz * pz);
    }

    private Vec3 goalAim(Pitch p, Team my, double err) {
        Vec3 g = p.attackGoal(my);
        double side = rnd().nextBoolean() ? 1 : -1;
        double z = g.z + side * (1.4 + rnd().nextDouble() * 1.8);
        double y = g.y + 0.4 + rnd().nextDouble() * 2.0;
        double e = (1.2 - bot.statShot) * err * 4.0;
        return new Vec3(g.x + my.attackDir() * 0.5, y + rnd().nextGaussian() * e * 0.4, z + rnd().nextGaussian() * e);
    }

    private void shoot(Pitch p, double dGoal, long now, FootData fd) {
        Team my = bot.getFootTeam();
        Vec3 aim = goalAim(p, my, 1.0);
        float r = rnd().nextFloat();
        Move mv;
        if (now >= fd.superReadyAt && dGoal > 8 && dGoal < 26 && rnd().nextFloat() < 0.15F) {
            Move[] s = {Move.FIRE_SHOT, Move.LIGHTNING_SHOT, Move.TORNADO_SHOT, Move.EAGLE_SHOT};
            mv = s[rnd().nextInt(s.length)];
        } else if (keeperOffLine(p, my) && dGoal < 22 && r < 0.3F) {
            mv = Move.CHIP;
            aim = p.attackGoal(my);
        } else if (dGoal < 9 && r < 0.05F) {
            mv = Move.PANENKA;
            aim = p.attackGoal(my).add(0, 1.2, 0);
        } else if (r < 0.28F) mv = Move.POWER_SHOT;
        else if (r < 0.55F) mv = Move.SHOT;
        else if (r < 0.75F) mv = Move.FINESSE;
        else if (r < 0.85F) mv = Move.TRIVELA;
        else if (r < 0.93F) mv = Move.KNUCKLE;
        else mv = Move.RABONA_SHOT;
        MoveExecutor.perform(bot, mv, 0.55F + rnd().nextFloat() * 0.45F, aim);
        actCooldown = 30;
    }

    private boolean keeperOffLine(Pitch p, Team my) {
        for (LivingEntity e : MatchManager.members(my.opponent())) {
            if (e instanceof FootballerEntity k && k.isKeeper()) return Math.abs(k.getX() - p.lineX(my.attackDir())) > 4.5;
        }
        return true;
    }

    private void skill(FootData fd, long now, LivingEntity opp) {
        Move mv;
        if (now >= fd.superReadyAt && rnd().nextFloat() < 0.05F) mv = Move.GHOST_DRIBBLE;
        else {
            Move[] s = {Move.STEPOVER, Move.ELASTICO, Move.ROULETTE, Move.CRUYFF, Move.BODY_FEINT, Move.BODY_FEINT, Move.DRAGBACK,
                    Move.RAINBOW, Move.SOMBRERO, Move.NUTMEG, Move.STEPOVER};
            mv = s[rnd().nextInt(s.length)];
        }
        if (opp != null) {
            // feint away from the defender
            MoveExecutor.perform(bot, mv, 0);
        } else MoveExecutor.perform(bot, mv, 0);
        actCooldown = 22;
    }

    private boolean tryPass(Pitch p, boolean pressured) {
        PassOption o = bestPass(p, pressured);
        if (o == null) return false;
        executePass(p, o, FootData.of(bot), bot.level().getGameTime());
        return true;
    }

    // ------------------------------------------------------------------ keeper

    private void keeper(FootballEntity ball, Pitch p, LivingEntity ctrl, LivingEntity holder, long now) {
        Team my = bot.getFootTeam();
        Vec3 own = p.ownGoal(my);
        int s = -my.attackDir();
        if (holder == bot) {
            keeperHold++;
            goTo(own.add(-s * 2.5, 0, 0), 0.8);
            if (keeperHold > 35) {
                LivingEntity best = null;
                double bd = -1e9;
                for (LivingEntity m : MatchManager.members(my)) {
                    if (m == bot) continue;
                    LivingEntity mo = nearestOpponentTo(m, 8);
                    double open = mo == null ? 8 : mo.distanceTo(m);
                    double sc = open - bot.distanceTo(m) * 0.08;
                    if (sc > bd) {
                        bd = sc;
                        best = m;
                    }
                }
                Vec3 t = best != null ? best.position() : own.add(-s * 30, 0, 0);
                MoveExecutor.perform(bot, Move.KEEPER_THROW, 0.6F, t);
                keeperHold = 0;
                actCooldown = 20;
            }
            return;
        }
        keeperHold = 0;
        if (ctrl == bot) {
            if (actCooldown == 0) {
                Vec3 t = own.add(-s * (28 + rnd().nextInt(10)), 0, (rnd().nextDouble() - 0.5) * 24);
                MoveExecutor.perform(bot, Move.LOB_PASS, 0.8F, t);
                actCooldown = 20;
            }
            return;
        }
        Vec3 bp = ball.position();
        Vec3 v = ball.getDeltaMovement();
        // shot reaction
        if (v.x * s > 0.25 && v.horizontalDistance() > 0.6 && ctrl == null && holder == null) {
            double t = (bot.getX() - ball.getX()) / v.x;
            if (t > 0 && t < 22) {
                double zc = ball.getZ() + v.z * t;
                double lateral = zc - bot.getZ();
                if (Math.abs(lateral) > 1.1 && Math.abs(lateral) < 5.0 && t < 10 && actCooldown == 0) {
                    MoveExecutor.perform(bot, Move.DIVE, 0, new Vec3(bot.getX(), bot.getY(), zc));
                    actCooldown = 30;
                    return;
                }
                if (ball.getY() > bot.getY() + 1.9 && t < 5 && actCooldown == 0 && ball.getY() < bot.getY() + 3.4) {
                    MoveExecutor.perform(bot, Move.PUNCH, 0);
                    actCooldown = 25;
                    return;
                }
                goTo(new Vec3(bot.getX(), bot.getY(), Mth.clamp(zc, p.midZ() - 3.4, p.midZ() + 3.4)), 1.4);
                return;
            }
        }
        // rush out for loose balls in the box
        boolean loose = ctrl == null && holder == null;
        if (loose && p.inPenaltyArea(my, bp) && bot.distanceTo(ball) < 14 && closerThanOpponents(ball)) {
            goTo(bp, 1.35);
            if (bot.distanceTo(ball) < 1.8 && v.length() < 1.2) tryCatch(bot, ball);
            return;
        }
        if (ctrl != null && MatchManager.teamOf(ctrl) != my && p.inPenaltyArea(my, ctrl.position()) && bot.distanceTo(ctrl) < 4.5 && actCooldown == 0) {
            goTo(ctrl.position(), 1.3);
            if (bot.distanceTo(ctrl) < 2.2) {
                MoveExecutor.perform(bot, Move.TACKLE, 0, ctrl.position());
                actCooldown = 30;
            }
            return;
        }
        Vec3 toBall = bp.subtract(own);
        double bd = toBall.horizontalDistance();
        Vec3 dir = MoveExecutor.flat(toBall);
        double out = Mth.clamp(bd * 0.12, 1.0, 3.5);
        Vec3 pos = own.add(dir.scale(out));
        pos = new Vec3(pos.x, own.y, Mth.clamp(pos.z, p.midZ() - 3.2, p.midZ() + 3.2));
        goTo(pos, 1.2);
    }

    private boolean closerThanOpponents(FootballEntity ball) {
        double my = bot.distanceTo(ball);
        for (LivingEntity e : MatchManager.members(bot.getFootTeam().opponent())) if (e.distanceTo(ball) < my) return false;
        return true;
    }

    /** keeper tries to hold the ball; called from body collisions and dives */
    public static boolean tryCatch(FootballerEntity bot, FootballEntity ball) {
        if (!bot.isKeeper() || ball.getHolder() != null) return false;
        Pitch p = MatchManager.pitch();
        if (p == null || !MatchManager.isActive() || !p.inPenaltyArea(bot.getFootTeam(), bot.position())) return false;
        if (ball.restrictTeam != Team.NONE && ball.restrictTeam != bot.getFootTeam() && bot.level().getGameTime() < ball.restrictUntil) return false;
        double speed = ball.getDeltaMovement().length();
        float chance = bot.statKeeper * (speed > 2.0 ? 0.55F : speed > 1.4F ? 0.8F : 1.0F);
        if (ball.special != FootballEntity.SP_NONE) chance *= 0.45F;
        if (ball.lastTouchTeam == bot.getFootTeam() && speed < 1.0) chance = 0; // no back-pass catches
        if (bot.getRandom().nextFloat() < chance) {
            ball.setHolder(bot);
            ball.playBallSound(ModSounds.CATCH.get(), 1.0F, 1.0F);
            MatchManager.onSave(bot);
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ casual play (no match running)

    private void casual(@Nullable FootballEntity ball) {
        if (ball == null || bot.distanceTo(ball) > 28) {
            if (wander == null || casualTimer-- <= 0 || horiz(bot.position(), wander) < 1) {
                wander = bot.position().add((rnd().nextDouble() - 0.5) * 12, 0, (rnd().nextDouble() - 0.5) * 12);
                casualTimer = 80 + rnd().nextInt(80);
            }
            goTo(wander, 0.6);
            return;
        }
        LivingEntity ctrl = ball.getController();
        if (ctrl == bot) {
            holdTicks++;
            Player near = bot.level().getNearestPlayer(bot, 30);
            if (holdTicks > 30 && actCooldown == 0 && rnd().nextFloat() < 0.08F) {
                float r = rnd().nextFloat();
                if (near != null && r < 0.5F) {
                    Move[] passes = {Move.SHORT_PASS, Move.LOB_PASS, Move.RABONA_PASS, Move.NO_LOOK_PASS};
                    MoveExecutor.perform(bot, passes[rnd().nextInt(passes.length)], 0.5F, near.position());
                } else if (r < 0.8F) {
                    Move[] s = {Move.RAINBOW, Move.STEPOVER, Move.ELASTICO, Move.ROULETTE, Move.JUGGLE, Move.SEAL, Move.SOMBRERO, Move.CRUYFF};
                    MoveExecutor.perform(bot, s[rnd().nextInt(s.length)], 0);
                } else {
                    Vec3 t = bot.position().add(MoveExecutor.facing(bot).scale(15)).add(0, 1, 0);
                    MoveExecutor.perform(bot, Move.SHOT, 0.5F, t);
                }
                actCooldown = 30;
                holdTicks = 0;
                return;
            }
            if (wander == null || casualTimer-- <= 0 || horiz(bot.position(), wander) < 1.5) {
                wander = bot.position().add((rnd().nextDouble() - 0.5) * 16, 0, (rnd().nextDouble() - 0.5) * 16);
                casualTimer = 60;
            }
            goTo(wander, 1.0);
            return;
        }
        holdTicks = 0;
        if (ctrl instanceof Player pl && bot.distanceTo(pl) < 2.3 && actCooldown == 0 && rnd().nextFloat() < 0.04F) {
            MoveExecutor.perform(bot, Move.TACKLE, 0, pl.position());
            actCooldown = 60;
            return;
        }
        if (ball.getHolder() == null) goTo(ball.position(), ctrl == null ? 1.15 : 0.9);
    }

    // ------------------------------------------------------------------ utils

    @Nullable
    private LivingEntity nearestOpponent(double r) { return nearestOpponentTo(bot, r); }

    @Nullable
    private LivingEntity nearestOpponentTo(LivingEntity from, double r) { return nearestOpponentTo(from.position(), r); }

    @Nullable
    private LivingEntity nearestOpponentTo(Vec3 from, double r) {
        Team opp = bot.getFootTeam().opponent();
        LivingEntity best = null;
        double bd = r * r;
        List<LivingEntity> list = MatchManager.members(opp);
        for (LivingEntity e : list) {
            double d = e.distanceToSqr(from.x, from.y, from.z);
            if (d < bd) {
                bd = d;
                best = e;
            }
        }
        return best;
    }

    private static double horiz(Vec3 a, Vec3 b) {
        double dx = a.x - b.x, dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }
}
