package com.rabona.arena.game;

import com.rabona.arena.entity.BallEntity;
import com.rabona.arena.entity.FootballerEntity;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Bot yapay zekasi. Kaleci: sut tahmini + plonjon. Savunma: pres, top calma, kayarak mudahale.
 * Hucum: top surme, calim, pas ve sut secimi. Pozisyon: dinamik dizilis.
 */
public class BotBrain {
    private final FootballerEntity bot;
    private int decide, holdTimer, skillCd, jumpCd, idleLook;
    private Move plannedShot;

    private static final Move[] SKILLS = {Move.RAINBOW, Move.ELASTICO, Move.ROULETTE, Move.STEPOVER, Move.CROQUETA,
            Move.NUTMEG, Move.CHOP, Move.SOMBRERO, Move.HEEL_FLICK, Move.CRUYFF, Move.DRAGBACK, Move.FAKE_SHOT,
            Move.SPEED_BURST, Move.SEAL, Move.ELASTICO, Move.ROULETTE, Move.STEPOVER};

    public BotBrain(FootballerEntity bot) { this.bot = bot; }

    private RandomSource rnd() { return bot.getRandom(); }

    public void tick() {
        if (bot.getServer() == null) return;
        Match m = Match.get(bot.getServer());
        Athlete a = Athlete.of(bot);
        if (skillCd > 0) skillCd--;
        if (jumpCd > 0) jumpCd--;
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
                Vec3 spot = m.formationSpot(bot.getSquad(), bot.getSlot(), null, true);
                goTo(spot, 0.8);
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

        if (mine && ball.isHeld()) {
            keeperDistribute(m, a, ball);
            return;
        }
        if (mine) {
            attack(m, a, ball);
            return;
        }
        // duran top kisitlamasi
        boolean restricted = ball.restrictTeam >= 0 && ball.tickCount < ball.restrictUntil;
        if (restricted && ball.restrictTeam != team.ordinal()) {
            Vec3 spot = m.formationSpot(team, bot.getSlot(), ball.position(), false);
            Vec3 away = spot;
            if (spot.distanceTo(ball.position()) < 5) away = ball.position().add(MoveLogic.flat(spot.subtract(ball.position())).scale(5.5));
            goTo(away, 1.0);
            return;
        }
        if (bot.isKeeper()) {
            keeper(m, a, ball);
            return;
        }
        if (aerial(m, a, ball)) return;

        if (carrier != null && ball.isHeld() && Match.teamOf(carrier) != team) {
            // rakip kaleci topu tutuyor: dizilise don
            goTo(clampToPitch(m, m.formationSpot(team, bot.getSlot(), ball.position(), false)), 0.9);
        } else if (carrier != null && Match.teamOf(carrier) == team) {
            support(m, ball, carrier);
        } else if (carrier != null) {
            defend(m, a, ball, carrier);
        } else {
            looseBall(m, a, ball, restricted);
        }
    }

    // ================================================================ hucum
    private void attack(Match m, Athlete a, BallEntity ball) {
        Team team = bot.getSquad();
        Pitch p = m.pitch;
        int s = m.attackSign(team);
        Vec3 goal = p.goalCenter(s);
        Vec3 me = bot.position();
        double distGoal = MoveLogic.horiz(me, goal);
        double lb = p.b(me);
        LivingEntity threat = nearestOpponent(m, 3.6, 0.15);
        int diff = m.difficulty;

        if (--decide <= 0) {
            decide = 4 + rnd().nextInt(4);
            // SUT
            boolean inRange = distGoal < 24 && Math.abs(lb) < 17;
            if (inRange && (distGoal < 16 || rnd().nextFloat() < 0.25)) {
                shoot(m, a, ball, distGoal);
                return;
            }
            // ozel yetenekler
            if (a.energy >= 60 && rnd().nextFloat() < 0.04 * (diff + 1)) {
                Move ab = rnd().nextBoolean() ? Move.AB_THUNDER : Move.AB_GHOST;
                if (MoveLogic.tryPerform(bot, ab, 1, 0, null)) return;
            }
            // baski altinda: calim ya da pas
            if (threat != null) {
                if (skillCd <= 0 && rnd().nextFloat() < 0.35 + diff * 0.12) {
                    Move sk = SKILLS[rnd().nextInt(SKILLS.length)];
                    int side = rnd().nextBoolean() ? 1 : -1;
                    faceTo(goal);
                    if (MoveLogic.tryPerform(bot, sk, 1, side, null)) {
                        skillCd = 30 + rnd().nextInt(40);
                        return;
                    }
                }
                if (passBest(m, a, ball, true)) return;
            } else if (rnd().nextFloat() < 0.07 && passBest(m, a, ball, false)) {
                return;
            }
            // kanattan orta
            if (Math.abs(lb) > Pitch.HALF_WID * 0.55 && Math.abs(p.a(me)) > Pitch.HALF_LEN * 0.6 && p.a(me) * s > 0) {
                LivingEntity target = mateInBox(m, s);
                if (target != null) {
                    faceTo(target.position());
                    Move cross = rnd().nextFloat() < 0.15 ? Move.PASS_RABONA : Move.PASS_CROSS;
                    if (MoveLogic.tryPerform(bot, cross, 1, lb > 0 ? 1 : -1, target.position().add(0, 0.5, 0))) return;
                }
            }
        }
        // top surme: kaleye dogru, ondeki rakipten kac
        Vec3 dir = MoveLogic.flat(goal.subtract(me));
        if (threat != null) {
            Vec3 away = MoveLogic.flat(me.subtract(threat.position()));
            Vec3 side = MoveLogic.right(dir);
            double sgn = Math.signum(side.dot(away));
            if (sgn == 0) sgn = 1;
            dir = dir.add(side.scale(0.8 * sgn)).normalize();
        }
        // saha icinde kal
        if (Math.abs(lb) > Pitch.HALF_WID - 3) dir = dir.add(p.axisB().scale(-Math.signum(lb) * 0.8)).normalize();
        Vec3 to = me.add(dir.scale(4));
        boolean sprint = a.stamina > 25 && threat == null;
        goTo(to, sprint ? 1.2 : 0.95);
    }

    private void shoot(Match m, Athlete a, BallEntity ball, double dist) {
        Pitch p = m.pitch;
        int s = m.attackSign(bot.getSquad());
        // kalecinin olmadigi kose
        LivingEntity gk = null;
        for (LivingEntity e : m.members(bot.getSquad().opponent())) {
            if (e instanceof FootballerEntity f && f.isKeeper()) gk = e;
        }
        double gkB = gk == null ? 0 : p.b(gk.position());
        double b = (gkB > 0 ? -1 : 1) * (Pitch.GOAL_HALF_W - 0.7 - rnd().nextDouble() * 1.2);
        double h = 0.3 + rnd().nextDouble() * (Pitch.GOAL_H - 0.9);
        Vec3 target = p.world(s * (p.goalLineA() + 0.4), b, p.surfaceY() + h);
        int diff = m.difficulty;
        Move shot;
        float r = rnd().nextFloat();
        boolean gkOut = gk != null && Math.abs(p.a(gk.position())) < p.goalLineA() - 4;
        if (a.energy >= 100 && rnd().nextFloat() < 0.25 + diff * 0.2) shot = rnd().nextBoolean() ? Move.AB_FIRE : Move.AB_TORNADO;
        else if (gkOut && dist < 18 && r < 0.6) shot = Move.SHOT_CHIP;
        else if (dist < 9 && r < 0.25) shot = Move.SHOT_TOEPOKE;
        else if (r < 0.42) shot = Move.SHOT_POWER;
        else if (r < 0.7) shot = Move.SHOT_FINESSE;
        else if (r < 0.8) shot = Move.SHOT_TRIVELA;
        else if (r < 0.88 && dist > 16) shot = Move.SHOT_KNUCKLE;
        else if (r < 0.94) shot = Move.SHOT_RABONA;
        else shot = Move.SHOT_POWER;
        faceTo(target);
        int side = b > 0 ? -1 : 1;
        float power = 0.75f + rnd().nextFloat() * 0.25f;
        if (!MoveLogic.tryPerform(bot, shot, power, side, target)) {
            MoveLogic.tryPerform(bot, Move.SHOT_POWER, power, side, target);
        }
    }

    private boolean passBest(Match m, Athlete a, BallEntity ball, boolean pressured) {
        Team team = bot.getSquad();
        int s = m.attackSign(team);
        Pitch p = m.pitch;
        LivingEntity best = null;
        double bestScore = -1e9;
        for (LivingEntity mate : m.members(team)) {
            if (mate == bot) continue;
            if (mate instanceof FootballerEntity f && f.isKeeper()) continue;
            double d = mate.distanceTo(bot);
            if (d < 4 || d > 34) continue;
            double adv = (p.a(mate.position()) - p.a(bot.position())) * s;
            double space = 99;
            for (LivingEntity o : m.members(team.opponent())) space = Math.min(space, o.distanceTo(mate));
            // pas yolunda rakip var mi
            double lane = laneBlocked(m, bot.position(), mate.position()) ? -12 : 0;
            double score = adv * 0.6 + Math.min(space, 8) * 1.5 + lane - d * 0.12 + (mate instanceof Player ? 4 : 0);
            if (score > bestScore) { bestScore = score; best = mate; }
        }
        if (best == null || (!pressured && bestScore < 6)) return false;
        double d = best.distanceTo(bot);
        Move pass;
        Vec3 target = best.position();
        double adv = (p.a(best.position()) - p.a(bot.position())) * s;
        if (laneBlocked(m, bot.position(), best.position()) || d > 24) {
            pass = Move.PASS_LOB;
        } else if (adv > 6 && rnd().nextFloat() < 0.6) {
            pass = Move.PASS_THROUGH;
            target = target.add(m.pitch.axisA().scale(s * 4)).add(Athlete.of(best).vel.scale(12));
        } else {
            pass = adv < -3 && rnd().nextFloat() < 0.3 ? Move.PASS_BACKHEEL : rnd().nextFloat() < 0.08 ? Move.PASS_NOLOOK : Move.PASS_SHORT;
        }
        if (pass != Move.PASS_BACKHEEL && pass != Move.PASS_NOLOOK) faceTo(target);
        int side = MoveLogic.right(MoveLogic.face(bot)).dot(target.subtract(bot.position())) > 0 ? 1 : -1;
        return MoveLogic.tryPerform(bot, pass, 1, side, target);
    }

    private boolean laneBlocked(Match m, Vec3 from, Vec3 to) {
        Vec3 d = to.subtract(from);
        double len = d.length();
        if (len < 1) return false;
        Vec3 n = d.scale(1 / len);
        for (LivingEntity o : m.members(bot.getSquad().opponent())) {
            Vec3 r = o.position().subtract(from);
            double t = r.dot(n);
            if (t < 1 || t > len - 1) continue;
            Vec3 c = from.add(n.scale(t));
            if (MoveLogic.horiz(c, o.position()) < 1.3) return true;
        }
        return false;
    }

    private LivingEntity mateInBox(Match m, int s) {
        Pitch p = m.pitch;
        for (LivingEntity mate : m.members(bot.getSquad())) {
            if (mate == bot) continue;
            double a = p.a(mate.position()) * s;
            if (a > Pitch.HALF_LEN - Pitch.BOX_DEPTH && Math.abs(p.b(mate.position())) < Pitch.BOX_HALF) return mate;
        }
        return null;
    }

    // ================================================================ destek
    private void support(Match m, BallEntity ball, LivingEntity carrier) {
        Team team = bot.getSquad();
        int s = m.attackSign(team);
        Vec3 spot = m.formationSpot(team, bot.getSlot(), ball.position(), false);
        String role = bot.roleKey();
        if (role.equals("fwd") || role.equals("mid")) {
            spot = spot.add(m.pitch.axisA().scale(s * (role.equals("fwd") ? 7 : 4)));
            // kosu: arada bir derin kosu
            if ((bot.tickCount / 60) % 3 == 0 && role.equals("fwd")) spot = spot.add(m.pitch.axisA().scale(s * 6));
        }
        if (spot.distanceTo(carrier.position()) < 6) {
            spot = spot.add(MoveLogic.flat(spot.subtract(carrier.position())).scale(4));
        }
        spot = clampToPitch(m, spot);
        goTo(spot, bot.distanceToSqr(spot) > 64 ? 1.15 : 0.9);
    }

    // ================================================================ savunma
    private void defend(Match m, Athlete a, BallEntity ball, LivingEntity carrier) {
        Team team = bot.getSquad();
        LivingEntity closest = closestOf(m.members(team), carrier.position(), true);
        double d = bot.distanceTo(carrier);
        int diff = m.difficulty;
        boolean presser = closest == bot || (d < 7 && secondClosest(m, carrier));
        if (presser) {
            Vec3 own = m.ownGoal(team);
            Vec3 block = carrier.position().add(MoveLogic.flat(own.subtract(carrier.position())).scale(1.1));
            Vec3 aim = d < 2.5 ? carrier.position() : block;
            goTo(aim, a.stamina > 15 ? 1.25 : 0.9);
            faceTo(carrier.position());
            if (d < 2.0 && rnd().nextFloat() < 0.07 + diff * 0.04) {
                Move t = rnd().nextFloat() < 0.18 ? Move.SHOULDER : Move.TACKLE;
                MoveLogic.tryPerform(bot, t, 1, 0, null);
            } else if (d > 2.4 && d < 4.2 && a.stamina > 30 && rnd().nextFloat() < 0.012 + diff * 0.01) {
                MoveLogic.tryPerform(bot, Move.SLIDE, 1, 0, null);
            } else if (d > 6 && d < 14 && a.stamina > 50 && rnd().nextFloat() < 0.01) {
                MoveLogic.tryPerform(bot, Move.PRESS, 1, 0, null);
            }
            if (a.energy >= 70 && d < 6 && rnd().nextFloat() < 0.004 * (diff + 1)) {
                MoveLogic.tryPerform(bot, rnd().nextBoolean() ? Move.AB_ICE : Move.AB_MAGNET, 1, 0, null);
            }
            return;
        }
        // sut blogu
        Vec3 bv = ball.getDeltaMovement();
        if (bv.length() > 0.8 && bot.distanceTo(ball) < 3.2 && rnd().nextFloat() < 0.3) {
            MoveLogic.tryPerform(bot, Move.BLOCK, 1, 0, null);
            return;
        }
        Vec3 spot = m.formationSpot(team, bot.getSlot(), ball.position(), false);
        // en yakin rakibi markaj
        LivingEntity mark = null;
        double md = 9;
        for (LivingEntity o : m.members(team.opponent())) {
            double dd = o.position().distanceTo(spot);
            if (o != carrier && dd < md) { md = dd; mark = o; }
        }
        if (mark != null) {
            Vec3 own = m.ownGoal(team);
            spot = mark.position().add(MoveLogic.flat(own.subtract(mark.position())).scale(1.6));
        }
        goTo(clampToPitch(m, spot), 1.05);
    }

    private boolean secondClosest(Match m, LivingEntity carrier) {
        LivingEntity first = closestOf(m.members(bot.getSquad()), carrier.position(), true);
        double mine = bot.distanceTo(carrier);
        int closer = 0;
        for (LivingEntity e : m.members(bot.getSquad())) {
            if (e == bot || e == first || (e instanceof FootballerEntity f && f.isKeeper())) continue;
            if (e.distanceTo(carrier) < mine) closer++;
        }
        return closer == 0;
    }

    // ================================================================ sahipsiz top
    private void looseBall(Match m, Athlete a, BallEntity ball, boolean restricted) {
        Team team = bot.getSquad();
        Vec3 bp = ball.position();
        LivingEntity closest = closestOf(m.members(team), bp, true);
        boolean chase = closest == bot || (closest instanceof Player && bot.distanceTo(ball) < 5 && !restricted);
        if (!chase) {
            // ikinci en yakin da baski yapar
            if (!restricted && secondClosestToBall(m, bp) && bot.distanceTo(ball) < 12) chase = true;
        }
        if (chase) {
            Vec3 v = ball.getDeltaMovement();
            double d = bot.position().distanceTo(bp);
            Vec3 pred = bp.add(v.scale(Math.min(14, d / 0.3)));
            pred = clampToPitch(m, pred);
            goTo(pred, a.stamina > 10 ? 1.25 : 0.95);
        } else {
            goTo(clampToPitch(m, m.formationSpot(team, bot.getSlot(), bp, false)), 1.0);
        }
    }

    private boolean secondClosestToBall(Match m, Vec3 bp) {
        double mine = bot.position().distanceTo(bp);
        int closer = 0;
        for (LivingEntity e : m.members(bot.getSquad())) {
            if (e == bot || (e instanceof FootballerEntity f && f.isKeeper())) continue;
            if (e.position().distanceTo(bp) < mine) closer++;
        }
        return closer == 1;
    }

    /** Havadaki top: kafa, vole, rovasata, gogusle kontrol. */
    private boolean aerial(Match m, Athlete a, BallEntity ball) {
        if (!ball.isFree()) return false;
        Vec3 bc = ball.center();
        double h = MoveLogic.horiz(bc, bot.position());
        double rel = bc.y - bot.getY();
        if (h > 2.6 || rel < 0.5) return false;
        Pitch p = m.pitch;
        int s = m.attackSign(bot.getSquad());
        Vec3 goal = p.goalCenter(s);
        double dGoal = MoveLogic.horiz(bot.position(), goal);
        Vec3 target = dGoal < 22 ? p.world(s * (p.goalLineA() + 0.3), (rnd().nextDouble() - 0.5) * 5, p.surfaceY() + 0.8 + rnd().nextDouble())
                : bot.position().add(p.axisA().scale(s * 18)).add(0, 1, 0);
        if (rel > 2.1 && rel < 3.0 && jumpCd <= 0 && bot.onGround()) {
            bot.getJumpControl().jump();
            jumpCd = 15;
        }
        Move mv = null;
        if (rel > 1.25) mv = (dGoal < 14 && rnd().nextFloat() < 0.25) ? Move.DIVING_HEADER : Move.HEADER;
        else if (dGoal < 20) {
            float r = rnd().nextFloat();
            mv = r < 0.12 ? Move.BICYCLE : r < 0.18 ? Move.SCORPION : r < 0.75 ? Move.VOLLEY : Move.CHEST_CONTROL;
        } else mv = rnd().nextFloat() < 0.6 ? Move.CHEST_CONTROL : Move.VOLLEY;
        if (rnd().nextFloat() > 0.35) return false; // tepki suresi
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
        // kaleye dogru gelen sut?
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
                        // yerel b ekseni ile kalecinin sag yonunu karsilastir
                        Vec3 worldOff = p.axisB().scale(off);
                        boolean right = MoveLogic.right(MoveLogic.face(bot)).dot(worldOff) > 0;
                        faceTo(bot.position().add(p.axisA().scale(-own)));
                        MoveLogic.tryPerform(bot, right ? Move.GK_DIVE_RIGHT : Move.GK_DIVE_LEFT, 1, 0, null);
                        return;
                    }
                    goTo(p.world(myA, Mth.clamp(predB, -Pitch.GOAL_HALF_W + 0.4, Pitch.GOAL_HALF_W - 0.4), p.surfaceY()), 1.3);
                }
            }
        }
        // yakin top: tut
        if (ball.isFree() && bc.distanceTo(bot.position().add(0, 0.9, 0)) < 1.45 && inOwnBox(m, bot.position())) {
            MoveLogic.catchOrParry(bot, a, ball);
            holdTimer = 30 + rnd().nextInt(30);
            return;
        }
        // kutuya giren yavas top / rakip: cik
        LivingEntity carrier = ball.getController();
        boolean danger = inOwnBox(m, bc) && (ball.isFree() && v.length() < 0.5 || carrier != null && Match.teamOf(carrier) != team && distBall < 9);
        if (danger) {
            goTo(ball.position(), 1.3);
            if (carrier != null && bot.distanceTo(carrier) < 2.0 && rnd().nextFloat() < 0.15) MoveLogic.tryPerform(bot, Move.TACKLE, 1, 0, null);
            return;
        }
        // pozisyon: top ile kale ortasi arasinda
        double depth = Mth.clamp(distBall * 0.07, 0.8, 3.2);
        Vec3 dir = MoveLogic.flat(bc.subtract(goal));
        Vec3 pos = goal.add(dir.scale(depth));
        double pb = Mth.clamp(p.b(pos), -Pitch.GOAL_HALF_W + 0.6, Pitch.GOAL_HALF_W - 0.6);
        double pa = goalA - own * Mth.clamp(Math.abs(p.a(pos) - goalA), 0.8, 3.2);
        goTo(p.world(pa, pb, p.surfaceY()), 1.0);
    }

    private void keeperDistribute(Match m, Athlete a, BallEntity ball) {
        stop();
        if (holdTimer <= 0) holdTimer = 35;
        if (--holdTimer > 0) return;
        Team team = bot.getSquad();
        LivingEntity mate = null;
        double best = -1e9;
        for (LivingEntity e : m.members(team)) {
            if (e == bot) continue;
            double space = 99;
            for (LivingEntity o : m.members(team.opponent())) space = Math.min(space, o.distanceTo(e));
            double d = e.distanceTo(bot);
            double sc = Math.min(space, 10) - Math.abs(d - 14) * 0.2;
            if (sc > best) { best = sc; mate = e; }
        }
        if (mate != null && best > 4 && rnd().nextFloat() < 0.65) {
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
        for (LivingEntity o : m.members(bot.getSquad().opponent())) {
            double d = o.distanceTo(bot);
            if (d < bd && MoveLogic.flat(o.position().subtract(bot.position())).dot(f) > cone) { bd = d; best = o; }
        }
        return best;
    }

    private LivingEntity closestOf(java.util.List<LivingEntity> list, Vec3 to, boolean skipKeeper) {
        LivingEntity best = null;
        double bd = 1e9;
        for (LivingEntity e : list) {
            if (skipKeeper && e instanceof FootballerEntity f && f.isKeeper()) continue;
            double d = e.position().distanceToSqr(to);
            if (d < bd) { bd = d; best = e; }
        }
        return best;
    }

    private Vec3 clampToPitch(Match m, Vec3 v) {
        Pitch p = m.pitch;
        double a = Mth.clamp(p.a(v), -Pitch.HALF_LEN + 0.5, Pitch.HALF_LEN - 0.5);
        double b = Mth.clamp(p.b(v), -Pitch.HALF_WID + 0.5, Pitch.HALF_WID - 0.5);
        return p.world(a, b, p.surfaceY());
    }

    private void goTo(Vec3 p, double speed) {
        double pace = 0.85 + bot.getSkill() * 0.0025;
        if (bot.distanceToSqr(p) < 0.6) {
            stop();
            return;
        }
        bot.getMoveControl().setWantedPosition(p.x, p.y, p.z, speed * pace);
        bot.setSprinting(speed > 1.1);
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
