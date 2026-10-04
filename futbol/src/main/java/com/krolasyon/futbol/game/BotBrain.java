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

/** Positional football AI: formation play, pressing, dribbling, passing, shooting, skills and goalkeeping. */
public final class BotBrain {
    private final FootballerEntity bot;
    private int actCooldown;
    private int holdTicks;
    private int keeperHold;
    private int casualTimer;
    private Vec3 wander;

    public BotBrain(FootballerEntity bot) { this.bot = bot; }

    private RandomSource rnd() { return bot.getRandom(); }

    public void tick() {
        long now = bot.level().getGameTime();
        if (actCooldown > 0) actCooldown--;
        FootData fd = FootData.of(bot);
        FootballEntity ball = MatchManager.ballFor(bot);
        if (ball != null) bot.getLookControl().setLookAt(ball, 40F, 40F);
        if (now < fd.busyUntil || now < fd.stunnedUntil) return;
        boolean inMatch = MatchManager.isActive() && bot.getTeam().playing() && MatchManager.pitch() != null
                && MatchManager.pitch().near(bot.position(), 30);
        if (!inMatch) {
            casual(ball);
            return;
        }
        switch (MatchManager.state) {
            case KICKOFF -> goTo(MatchManager.kickoffPos(bot), 1.1);
            case GOAL -> {
                if (MatchManager.lastGoalTeam == bot.getTeam()) {
                    Pitch p = MatchManager.pitch();
                    Vec3 corner = new Vec3(p.lineX(bot.getTeam().attackDir()) - bot.getTeam().attackDir() * 2, p.floor(), bot.getZ() < p.midZ() ? p.zMin() + 2 : p.zMax() - 2);
                    goTo(corner, 1.25);
                    if (rnd().nextInt(70) == 0 && actCooldown == 0) {
                        Move[] c = {Move.SIUU, Move.KNEE_SLIDE, Move.AIRPLANE, Move.BACKFLIP, Move.DANCE};
                        MoveExecutor.perform(bot, c[rnd().nextInt(c.length)], 0);
                        actCooldown = 40;
                    }
                } else {
                    goTo(MatchManager.kickoffPos(bot), 0.8);
                }
            }
            case ENDED -> {
                if (MatchManager.winner() == bot.getTeam() && rnd().nextInt(90) == 0 && actCooldown == 0) {
                    Move[] c = {Move.SIUU, Move.DANCE, Move.BACKFLIP, Move.AIRPLANE};
                    MoveExecutor.perform(bot, c[rnd().nextInt(c.length)], 0);
                    actCooldown = 50;
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
        if (dx * dx + dz * dz < 0.25) {
            stop();
            return;
        }
        bot.getMoveControl().setWantedPosition(t.x, bot.getY(), t.z, speed);
        bot.setSprinting(speed > 1.15);
    }

    private void stop() {
        bot.getMoveControl().setWantedPosition(bot.getX(), bot.getY(), bot.getZ(), 0.0);
        bot.setSprinting(false);
    }

    // ------------------------------------------------------------------ match play

    private void play(FootballEntity ball, long now) {
        Pitch p = MatchManager.pitch();
        Team my = bot.getTeam();
        Team opp = my.opponent();
        LivingEntity ctrl = ball.getController();
        LivingEntity holder = ball.getHolder();
        double dBall = bot.distanceTo(ball);

        if (bot.isKeeper()) {
            keeper(ball, p, ctrl, holder, now);
            return;
        }
        if (ctrl == bot) {
            attack(ball, p, now);
            return;
        }
        holdTicks = 0;
        Team ctrlTeam = ctrl != null ? MatchManager.teamOf(ctrl) : holder != null ? MatchManager.teamOf(holder) : Team.NONE;

        if (ball.restrictTeam == opp && now < ball.restrictUntil) {
            Vec3 home = MatchManager.dynamicPos(bot);
            if (dBall < 5) {
                Vec3 away = bot.position().subtract(ball.position()).multiply(1, 0, 1).normalize().scale(5);
                goTo(bot.position().add(away), 1.0);
            } else goTo(home, 1.0);
            return;
        }

        boolean loose = ctrl == null && holder == null;
        double dy = ball.getY() - bot.getY();
        if (loose && actCooldown == 0 && dBall < 2.8 && !ball.onGround()) {
            Vec3 goal = p.attackGoal(my);
            double dGoal = horiz(bot.position(), goal);
            if (dy > 1.1 && dy < 3.3) {
                Vec3 aim = dGoal < 22 ? goalAim(p, my, 0.4) : bot.position().add(my.attackDir() * 18, 0, (rnd().nextDouble() - 0.5) * 10);
                if (dGoal < 22 || ball.getDeltaMovement().length() > 0.3) {
                    MoveExecutor.perform(bot, Move.HEADER, 0.7F, aim);
                    actCooldown = 20;
                    return;
                }
            } else if (dy > 0.35 && dy <= 1.1 && rnd().nextFloat() < 0.35F) {
                if (dGoal < 26) {
                    boolean backToGoal = MoveExecutor.facing(bot).dot(MoveExecutor.flat(goal.subtract(bot.position()))) < -0.3;
                    Move mv = backToGoal && rnd().nextFloat() < 0.6F ? Move.BICYCLE : rnd().nextFloat() < 0.12F ? Move.SCORPION : Move.VOLLEY;
                    MoveExecutor.perform(bot, mv, 0.8F, goalAim(p, my, 0.5));
                } else {
                    MoveExecutor.perform(bot, Move.FIRST_TOUCH, 0, null);
                }
                actCooldown = 20;
                return;
            }
        }

        if (ctrlTeam == my && my.playing()) {
            support(ball, p);
            return;
        }

        LivingEntity chaser = MatchManager.chaser(my);
        LivingEntity second = MatchManager.secondChaser(my);
        if (bot == chaser || (bot == second && dBall < 9)) {
            if (ctrl != null && ctrlTeam != my) {
                double d = bot.distanceTo(ctrl);
                Vec3 front = ctrl.position().add(MoveExecutor.facing(ctrl).scale(0.9));
                goTo(front, 1.3);
                if (d < 2.4 && actCooldown == 0) {
                    float r = rnd().nextFloat();
                    Move mv = r < 0.12F + bot.statDefend * 0.12F && d > 1.3 ? Move.SLIDE : r < 0.85F ? Move.TACKLE : Move.SHOULDER;
                    MoveExecutor.perform(bot, mv, 0, ctrl.position());
                    actCooldown = 22 + rnd().nextInt(22);
                }
            } else if (holder != null) {
                goTo(MatchManager.dynamicPos(bot), 1.0);
            } else {
                Vec3 v = ball.getDeltaMovement();
                Vec3 inter = ball.position().add(v.x * Math.min(14, dBall * 2.2), 0, v.z * Math.min(14, dBall * 2.2));
                goTo(inter, 1.3);
                if (v.length() > 1.0 && dBall < 3.5 && actCooldown == 0 && ball.lastTouchTeam == opp && rnd().nextFloat() < 0.3F) {
                    MoveExecutor.perform(bot, Move.BLOCK, 0);
                    actCooldown = 30;
                }
            }
            return;
        }
        goTo(MatchManager.dynamicPos(bot), dBall < 15 ? 1.15 : 1.0);
    }

    private void support(FootballEntity ball, Pitch p) {
        Team my = bot.getTeam();
        Vec3 home = MatchManager.dynamicPos(bot);
        double u = p.teamU(my, home.x) + 0.1;
        double v = p.teamV(my, home.z);
        LivingEntity near = nearestOpponent(6);
        if (near != null) v += Math.signum(p.teamV(my, bot.getZ()) - p.teamV(my, near.getZ()) + 1e-3) * 0.12;
        goTo(p.point(my, Mth.clamp(u, 0.05, 0.9), Mth.clamp(v, -0.92, 0.92)), 1.15);
    }

    private void attack(FootballEntity ball, Pitch p, long now) {
        Team my = bot.getTeam();
        holdTicks++;
        Vec3 goal = p.attackGoal(my);
        double dGoal = horiz(bot.position(), goal);
        LivingEntity opp = nearestOpponent(9);
        double dOpp = opp == null ? 99 : bot.distanceTo(opp);
        FootData fd = FootData.of(bot);

        if (actCooldown == 0 && holdTicks > 6 && (holdTicks % 4 == 0)) {
            double angle = Math.abs(p.teamV(my, bot.getZ()));
            double chance = dGoal < 10 ? 0.75 : dGoal < 24 ? (24 - dGoal) / 24.0 * 0.5 + bot.statShot * 0.15 : 0;
            if (angle > 0.85) chance *= 0.4;
            if (rnd().nextDouble() < chance * 0.45) {
                shoot(p, dGoal, now, fd);
                return;
            }
            if (dOpp < 3.2) {
                if (rnd().nextFloat() < bot.statDribble * 0.5F) {
                    skill(fd, now, opp);
                    return;
                }
                if (tryPass(p, true)) return;
            } else if (holdTicks > 26 && rnd().nextFloat() < 0.07F + bot.statPass * 0.06F) {
                if (tryPass(p, false)) return;
            }
            double u = p.teamU(my, bot.getX());
            if (u > 0.76 && Math.abs(p.teamV(my, bot.getZ())) > 0.55 && rnd().nextFloat() < 0.12F) {
                MoveExecutor.perform(bot, Move.CROSS, 0.6F);
                actCooldown = 25;
                return;
            }
            if (dOpp > 7 && rnd().nextFloat() < 0.015F) {
                MoveExecutor.perform(bot, Move.SPEED_BURST, 0);
                actCooldown = 20;
                return;
            }
        }
        // dribble towards goal, steering around the nearest defender
        Vec3 aimPoint = new Vec3(goal.x, goal.y, p.midZ() + (bot.getZ() - p.midZ()) * 0.4);
        Vec3 dir = MoveExecutor.flat(aimPoint.subtract(bot.position()));
        if (opp != null && dOpp < 5.5) {
            Vec3 away = MoveExecutor.flat(bot.position().subtract(opp.position()));
            dir = MoveExecutor.flat(dir.add(away.scale(0.75 * (5.5 - dOpp) / 5.5)));
        }
        Vec3 t = p.clampInside(bot.position().add(dir.scale(4)), 1.5);
        goTo(t, 1.05 + bot.statSpeed * 0.12);
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
        Team my = bot.getTeam();
        Vec3 aim = goalAim(p, my, 1.0);
        float r = rnd().nextFloat();
        Move mv;
        if (now >= fd.superReadyAt && dGoal > 8 && dGoal < 26 && rnd().nextFloat() < 0.2F) {
            Move[] s = {Move.FIRE_SHOT, Move.LIGHTNING_SHOT, Move.TORNADO_SHOT, Move.EAGLE_SHOT};
            mv = s[rnd().nextInt(s.length)];
        } else if (keeperOffLine(p, my) && dGoal < 22 && r < 0.3F) {
            mv = Move.CHIP;
            aim = p.attackGoal(my);
        } else if (dGoal < 9 && r < 0.06F) {
            mv = Move.PANENKA;
            aim = p.attackGoal(my).add(0, 1.2, 0);
        } else if (r < 0.32F) mv = Move.POWER_SHOT;
        else if (r < 0.58F) mv = Move.SHOT;
        else if (r < 0.78F) mv = Move.FINESSE;
        else if (r < 0.86F) mv = Move.TRIVELA;
        else if (r < 0.93F) mv = Move.KNUCKLE;
        else mv = Move.RABONA_SHOT;
        MoveExecutor.perform(bot, mv, 0.55F + rnd().nextFloat() * 0.45F, aim);
        actCooldown = 30;
    }

    private boolean keeperOffLine(Pitch p, Team my) {
        for (LivingEntity e : MatchManager.members(my.opponent())) {
            if (e instanceof FootballerEntity k && k.isKeeper()) {
                return Math.abs(k.getX() - p.lineX(my.attackDir())) > 4.5;
            }
        }
        return true;
    }

    private void skill(FootData fd, long now, LivingEntity opp) {
        Move mv;
        if (now >= fd.superReadyAt && rnd().nextFloat() < 0.06F) mv = Move.GHOST_DRIBBLE;
        else {
            Move[] s = {Move.STEPOVER, Move.ELASTICO, Move.ROULETTE, Move.CRUYFF, Move.RAINBOW, Move.DRAGBACK, Move.SOMBRERO,
                    Move.NUTMEG, Move.STEPOVER, Move.ROULETTE, Move.SEAL};
            mv = s[rnd().nextInt(s.length)];
            if (mv == Move.SEAL && rnd().nextFloat() < 0.7F) mv = Move.ELASTICO;
        }
        MoveExecutor.perform(bot, mv, 0);
        actCooldown = 22;
    }

    private boolean tryPass(Pitch p, boolean pressured) {
        Team my = bot.getTeam();
        LivingEntity best = null;
        double bs = -1e9;
        double myU = p.teamU(my, bot.getX());
        for (LivingEntity m : MatchManager.members(my)) {
            if (m == bot || !m.isAlive()) continue;
            if (m instanceof FootballerEntity k && k.isKeeper() && myU > 0.3) continue;
            double d = bot.distanceTo(m);
            if (d < 4 || d > 42) continue;
            LivingEntity mo = nearestOpponentTo(m, 8);
            double open = mo == null ? 8 : mo.distanceTo(m);
            double fwd = p.teamU(my, m.getX()) - myU;
            double s = open * 0.35 + fwd * 8.0 - d * 0.05 + (pressured ? 1.0 : 0.0);
            if (!pressured && fwd < -0.05) s -= 3;
            if (s > bs) {
                bs = s;
                best = m;
            }
        }
        if (best == null || (!pressured && bs < 1.0)) return false;
        double d = bot.distanceTo(best);
        FootData fd = FootData.of(bot);
        long now = bot.level().getGameTime();
        Move mv;
        Vec3 target = best.position();
        float r = rnd().nextFloat();
        Vec3 toMate = MoveExecutor.flat(best.position().subtract(bot.position()));
        double facingDot = MoveExecutor.facing(bot).dot(toMate);
        if (now >= fd.superReadyAt && r < 0.04F) mv = Move.ICE_PASS;
        else if (facingDot < -0.5 && r < 0.5F) mv = Move.BACKHEEL_PASS;
        else if (d > 22) mv = Move.LOB_PASS;
        else if (p.teamU(my, best.getX()) > p.teamU(my, bot.getX()) + 0.08 && r < 0.45F) {
            mv = Move.THROUGH_PASS;
            target = best.position().add(my.attackDir() * 4.5, 0, 0);
        } else if (r < 0.06F) mv = Move.RABONA_PASS;
        else if (r < 0.12F) mv = Move.NO_LOOK_PASS;
        else mv = Move.SHORT_PASS;
        double err = (1.0 - bot.statPass) * 1.6;
        target = target.add(rnd().nextGaussian() * err, 0, rnd().nextGaussian() * err);
        if (mv == Move.NO_LOOK_PASS || mv == Move.BACKHEEL_PASS) {
            MoveExecutor.perform(bot, mv, 0.5F, target);
        } else MoveExecutor.perform(bot, mv, 0.5F, target);
        actCooldown = 18;
        holdTicks = 0;
        return true;
    }

    // ------------------------------------------------------------------ keeper

    private void keeper(FootballEntity ball, Pitch p, LivingEntity ctrl, LivingEntity holder, long now) {
        Team my = bot.getTeam();
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
        for (LivingEntity e : MatchManager.members(bot.getTeam().opponent())) if (e.distanceTo(ball) < my) return false;
        return true;
    }

    /** keeper tries to hold the ball; called from body collisions and dives */
    public static boolean tryCatch(FootballerEntity bot, FootballEntity ball) {
        if (!bot.isKeeper() || ball.getHolder() != null) return false;
        Pitch p = MatchManager.pitch();
        if (p == null || !MatchManager.isActive() || !p.inPenaltyArea(bot.getTeam(), bot.position())) return false;
        if (ball.restrictTeam != Team.NONE && ball.restrictTeam != bot.getTeam() && bot.level().getGameTime() < ball.restrictUntil) return false;
        double speed = ball.getDeltaMovement().length();
        float chance = bot.statKeeper * (speed > 2.0 ? 0.55F : speed > 1.4F ? 0.8F : 1.0F);
        if (ball.special != FootballEntity.SP_NONE) chance *= 0.45F;
        if (ball.lastTouchTeam == bot.getTeam() && speed < 1.0) chance = 0; // no back-pass catches
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
    private LivingEntity nearestOpponentTo(LivingEntity from, double r) {
        Team opp = bot.getTeam().opponent();
        LivingEntity best = null;
        double bd = r * r;
        List<LivingEntity> list = MatchManager.members(opp);
        for (LivingEntity e : list) {
            double d = e.distanceToSqr(from);
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
