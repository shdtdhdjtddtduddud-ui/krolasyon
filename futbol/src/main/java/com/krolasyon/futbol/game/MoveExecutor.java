package com.krolasyon.futbol.game;

import com.krolasyon.futbol.entity.FootballEntity;
import com.krolasyon.futbol.entity.FootballerEntity;
import com.krolasyon.futbol.network.Net;
import com.krolasyon.futbol.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/** Server side execution of every {@link Move}: validation, animation broadcast, timed ball effect. */
public final class MoveExecutor {
    private MoveExecutor() {}

    // ------------------------------------------------------------------ entry points

    public static boolean perform(LivingEntity a, Move m, float charge) { return perform(a, m, charge, null); }

    public static boolean perform(LivingEntity a, Move move, float charge, @Nullable Vec3 aim) {
        if (a.level().isClientSide || !a.isAlive()) return false;
        ServerLevel lvl = (ServerLevel) a.level();
        long now = lvl.getGameTime();
        FootData fd = FootData.of(a);
        if (now < fd.busyUntil || now < fd.stunnedUntil) return false;
        Move m = contextual(a, move);
        if (m.isSuper() && now < fd.superReadyAt) {
            msg(a, "Süper yetenek hazır değil: " + ((fd.superReadyAt - now) / 20 + 1) + " sn", ChatFormatting.RED);
            return false;
        }
        Long ready = fd.readyAt.get(m);
        if (ready != null && now < ready) {
            msg(a, m.title + " bekleme süresinde: " + ((ready - now) / 20 + 1) + " sn", ChatFormatting.GRAY);
            return false;
        }
        FootballEntity ball = findBall(a, m);
        if (m.needsBall && ball == null) {
            msg(a, "Top yakında değil!", ChatFormatting.GRAY);
            return false;
        }
        if (m.needsBall && ball.frozen) {
            msg(a, "Başlama düdüğünü bekle!", ChatFormatting.YELLOW);
            return false;
        }
        int variant = variant(a, m, ball, aim);
        fd.busyUntil = now + m.lock;
        fd.lastMove = m.ordinal();
        if (m.reuseTicks() > 0) fd.readyAt.put(m, now + m.reuseTicks());
        if (m.isSuper()) {
            fd.superReadyAt = now + m.cooldown;
            lvl.playSound(null, a.getX(), a.getY(), a.getZ(), ModSounds.SUPER_CHARGE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            msg(a, "✦ " + m.title + "!", ChatFormatting.LIGHT_PURPLE);
        }
        if (aim != null && a instanceof Mob) face(a, aim);
        Net.sendAnim(a, m, variant, m.reuseTicks());
        if (ball != null && m.needsBall && ball.getController() == null && ball.getHolder() == null && !airborne(ball, a)
                && m != Move.FIRST_TOUCH && m.cat != Move.Cat.KEEPER) {
            ball.setController(a);
        }
        prelude(a, m, lvl, variant);
        final FootballEntity b = ball;
        if (m.impact <= 0) {
            effect(a, m, b, charge, aim, variant);
        } else {
            Scheduler.later(m.impact, () -> {
                if (a.isAlive() && !a.isRemoved()) effect(a, m, b, charge, aim, variant);
            });
        }
        return true;
    }

    /** left click on the ball */
    public static void basicKick(Player p, FootballEntity b) {
        long now = p.level().getGameTime();
        FootData fd = FootData.of(p);
        if (now < fd.busyUntil || b.frozen || now < fd.stunnedUntil) return;
        if (p.distanceToSqr(b) > 3.2 * 3.2) return;
        LivingEntity c = b.getController();
        if (c != null && c != p) return;
        if (b.getHolder() != null) return;
        fd.busyUntil = now + 6;
        Vec3 look = p.getLookAngle();
        Vec3 h = flat(look);
        double speed = p.isSprinting() ? 1.35 : 0.95;
        double vy = Mth.clamp(0.12 + look.y * speed * 0.6, 0.03, 0.9);
        b.kick(p, h.scale(speed).add(0, vy, 0), Vec3.ZERO, speed > 1.2 ? FootballEntity.TR_FAST : FootballEntity.TR_NONE, FootballEntity.SP_NONE);
        b.playBallSound(ModSounds.KICK.get(), 0.8F, 1.0F + p.getRandom().nextFloat() * 0.15F);
        Net.sendAnim(p, Move.SHORT_PASS, 0, 0);
    }

    // ------------------------------------------------------------------ helpers

    private static Move contextual(LivingEntity a, Move m) {
        FootballEntity held = nearestBall(a, 3.0);
        if (held != null && held.getHolder() == a) {
            if (m.cat == Move.Cat.PASS) return Move.KEEPER_THROW;
            if (m.isShot()) return Move.LOB_PASS;
        }
        if (m != Move.SHOT && m != Move.POWER_SHOT && m != Move.FINESSE) return m;
        FootballEntity b = nearestBall(a, 3.0);
        if (b == null || b.getController() == a || b.getController() != null) return m;
        double dy = b.getY() - a.getY();
        if (dy > 1.25 && dy < 3.2) return Move.HEADER;
        if (dy > 0.35 && b.getDeltaMovement().y != 0 && !b.onGround()) return Move.VOLLEY;
        return m;
    }

    private static boolean airborne(FootballEntity b, LivingEntity a) { return b.getY() - a.getY() > 0.35 && !b.onGround(); }

    public static FootballEntity nearestBall(LivingEntity a, double r) {
        FootballEntity best = null;
        double bd = r * r;
        for (FootballEntity b : a.level().getEntitiesOfClass(FootballEntity.class, a.getBoundingBox().inflate(r))) {
            double d = b.distanceToSqr(a.getX(), a.getY() + 0.3, a.getZ());
            if (d < bd) {
                bd = d;
                best = b;
            }
        }
        return best;
    }

    private static FootballEntity findBall(LivingEntity a, Move m) {
        double r = switch (m) {
            case HEADER, VOLLEY, BICYCLE, SCORPION -> 3.3;
            case FIRST_TOUCH, PUNCH, DIVE -> 4.0;
            case TACKLE, SHOULDER -> 3.2;
            case SLIDE, BLOCK -> 6.0;
            default -> 2.6;
        };
        FootballEntity b = nearestBall(a, r);
        if (b == null) return null;
        if (!m.needsBall) return b;
        LivingEntity c = b.getController();
        LivingEntity h = b.getHolder();
        if (m == Move.KEEPER_THROW) return (h == a || c == a) ? b : null;
        if (h != null && h != a) return null;
        if (c != null && c != a) return null;
        return b;
    }

    private static int variant(LivingEntity a, Move m, FootballEntity b, Vec3 aim) {
        RandomSource r = a.getRandom();
        switch (m) {
            case DIVE -> {
                Vec3 target = aim != null ? aim : b != null ? b.position() : a.position().add(right(facing(a)));
                Vec3 to = target.subtract(a.position());
                return to.dot(right(facing(a))) >= 0 ? 0 : 1;
            }
            case STEPOVER, ELASTICO, ROULETTE, CRUYFF, DANCE, BODY_FEINT -> {
                return r.nextInt(2);
            }
            case SHOT, POWER_SHOT, SHORT_PASS, THROUGH_PASS, LOB_PASS, VOLLEY -> {
                return a instanceof FootballerEntity && r.nextFloat() < 0.25F ? 1 : 0;
            }
            default -> {
                return 0;
            }
        }
    }

    public static Vec3 facing(LivingEntity e) { return Vec3.directionFromRotation(0, e.getYRot()); }

    /** right hand side of a horizontal facing vector */
    public static Vec3 right(Vec3 f) { return new Vec3(-f.z, 0, f.x); }

    /** rotate horizontal vector to the right by deg */
    public static Vec3 rotY(Vec3 f, double deg) {
        double t = Math.toRadians(deg), c = Math.cos(t), s = Math.sin(t);
        return new Vec3(f.x * c - f.z * s, f.y, f.x * s + f.z * c);
    }

    public static Vec3 flat(Vec3 v) {
        Vec3 h = new Vec3(v.x, 0, v.z);
        return h.lengthSqr() < 1.0E-8 ? new Vec3(0, 0, 1) : h.normalize();
    }

    private static void face(LivingEntity a, Vec3 target) {
        double dx = target.x - a.getX(), dz = target.z - a.getZ();
        if (dx * dx + dz * dz < 1.0E-4) return;
        float yaw = (float) (Mth.atan2(dz, dx) * (180F / Math.PI)) - 90F;
        a.setYRot(yaw);
        a.setYHeadRot(yaw);
        a.yBodyRot = yaw;
    }

    private static void msg(LivingEntity a, String s, ChatFormatting c) {
        if (a instanceof Player p) p.displayClientMessage(Component.literal(s).withStyle(c), true);
    }

    private static void particles(ServerLevel l, ParticleOptions p, Vec3 at, int n, double spread, double speed) {
        l.sendParticles(p, at.x, at.y, at.z, n, spread, spread, spread, speed);
    }

    private static void push(LivingEntity a, Vec3 v) {
        a.setDeltaMovement(v);
        a.hurtMarked = true;
    }

    private static void sound(LivingEntity a, SoundEvent s, float vol, float pitch) {
        a.level().playSound(null, a.getX(), a.getY(), a.getZ(), s, SoundSource.PLAYERS, vol, pitch);
    }

    private static boolean ballNear(LivingEntity a, FootballEntity b, double r) {
        return b != null && b.isAlive() && b.distanceToSqr(a.getX(), a.getY() + 0.3, a.getZ()) < r * r
                && (b.getController() == null || b.getController() == a) && (b.getHolder() == null || b.getHolder() == a);
    }

    // ------------------------------------------------------------------ trajectory solvers

    /** vertical launch speed so that a ball launched with horizontal speed h is dy higher after dist blocks */
    public static double solveVy(double dist, double h, double dy) {
        double lo = -0.4, hi = 1.6;
        for (int it = 0; it < 22; it++) {
            double vy = (lo + hi) * 0.5;
            double y = heightAt(dist, h, vy);
            if (y < dy) lo = vy;
            else hi = vy;
        }
        return (lo + hi) * 0.5;
    }

    private static double heightAt(double dist, double h, double vy) {
        double x = 0, y = 0, vx = h;
        for (int i = 0; i < 160; i++) {
            vy = (vy - FootballEntity.GRAVITY) * FootballEntity.AIR;
            vx *= FootballEntity.AIR;
            x += vx;
            y += vy;
            if (x >= dist) return y;
        }
        return -100;
    }

    /** horizontal speed so that a ball launched with vy lands dist blocks away */
    public static double solveLobH(double dist, double vy) {
        int t = 0;
        double y = 0, v = vy;
        double sumDrag = 0, drag = 1;
        while (t < 200) {
            v = (v - FootballEntity.GRAVITY) * FootballEntity.AIR;
            y += v;
            drag *= FootballEntity.AIR;
            sumDrag += drag;
            t++;
            if (y <= 0 && v < 0) break;
        }
        return Mth.clamp(dist / Math.max(sumDrag, 1), 0.05, 3.0);
    }

    // ------------------------------------------------------------------ aiming

    private record Aim(Vec3 dir, double dist, double dy) {}

    /** shot aim: bots pass an explicit target, players use their view with a gentle goal assist */
    private static Aim shotAim(LivingEntity a, FootballEntity b, Vec3 aim) {
        Vec3 bc = b.center();
        if (aim != null) {
            Vec3 to = aim.subtract(bc);
            return new Aim(flat(to), Math.sqrt(to.x * to.x + to.z * to.z), to.y);
        }
        Vec3 look = a.getLookAngle();
        Vec3 fh = flat(look);
        Pitch p = MatchManager.pitch();
        Team t = MatchManager.teamOf(a);
        if (p != null && t.playing() && MatchManager.isActive()) {
            Vec3 g = p.attackGoal(t);
            double gx = g.x;
            if (Math.signum(fh.x) == Math.signum(gx - bc.x) && Math.abs(fh.x) > 0.2) {
                double tHit = (gx - bc.x) / fh.x;
                double zHit = Mth.clamp(bc.z + fh.z * tHit, p.midZ() - 3.0, p.midZ() + 3.0);
                Vec3 target = new Vec3(gx, p.floor() + Mth.clamp(1.0 + look.y * 6.0, 0.4, 2.9), zHit);
                Vec3 to = target.subtract(bc);
                Vec3 dir = flat(to);
                double d = Math.sqrt(to.x * to.x + to.z * to.z);
                if (dir.dot(fh) > Math.cos(Math.toRadians(32)) && d < 42) return new Aim(dir, d, to.y);
            }
        }
        // free aim: distance 22, height from view pitch
        return new Aim(fh, -1, look.y);
    }

    private static Vec3 shotVel(Aim aim, double speed, double liftBias) {
        double vy;
        if (aim.dist > 0) vy = solveVy(aim.dist, speed, aim.dy) + liftBias;
        else vy = Mth.clamp(0.16 + aim.dy * speed * 0.55, 0.02, 1.0) + liftBias;
        return aim.dir.scale(speed).add(0, vy, 0);
    }

    /** spin around Y producing curve towards the side that contains point (or sign s if point is null) */
    private static Vec3 curl(double amount) { return new Vec3(0, amount, 0); }

    private static Vec3 topspin(Vec3 dir, double k) { return new Vec3(dir.z, 0, -dir.x).scale(k); }

    // ------------------------------------------------------------------ pass targets

    public static List<LivingEntity> mates(LivingEntity a) {
        Team t = MatchManager.teamOf(a);
        List<LivingEntity> out = new ArrayList<>();
        if (t.playing() && MatchManager.isActive()) {
            for (LivingEntity e : MatchManager.members(t)) if (e != a && e.isAlive()) out.add(e);
        } else {
            for (LivingEntity e : a.level().getEntitiesOfClass(LivingEntity.class, a.getBoundingBox().inflate(40),
                    e -> e != a && e.isAlive() && (e instanceof Player p && !p.isSpectator() || e instanceof FootballerEntity))) {
                if (t.playing() && MatchManager.teamOf(e) != t) continue;
                out.add(e);
            }
        }
        return out;
    }

    @Nullable
    private static LivingEntity passTarget(LivingEntity a, Vec3 pref, double minCos, double maxCos) {
        LivingEntity best = null;
        double bs = -1e9;
        for (LivingEntity m : mates(a)) {
            Vec3 to = m.position().subtract(a.position());
            double d = Math.sqrt(to.x * to.x + to.z * to.z);
            if (d < 2.5 || d > 48) continue;
            double cos = flat(to).dot(pref);
            if (cos < minCos || cos > maxCos) continue;
            double s = cos * 2.0 - d / 35.0;
            if (s > bs) {
                bs = s;
                best = m;
            }
        }
        return best;
    }

    private static Vec3 leadPoint(LivingEntity m, double lead) {
        Team t = MatchManager.teamOf(m);
        Vec3 dir = t.playing() && MatchManager.isActive() ? new Vec3(t.attackDir(), 0, 0) : facing(m);
        Vec3 f = facing(m);
        Vec3 mix = flat(dir.add(f));
        return m.position().add(mix.scale(lead));
    }

    private static Vec3 passPoint(LivingEntity a, Vec3 pref, Vec3 aim, double defaultDist, boolean through) {
        if (aim != null) return aim;
        LivingEntity t = passTarget(a, pref, Math.cos(Math.toRadians(55)), 1.01);
        if (t != null) return through ? leadPoint(t, 5.0) : t.position();
        return a.position().add(pref.scale(defaultDist));
    }

    private static void groundPass(LivingEntity a, FootballEntity b, Vec3 target, double mult, int trail, int sp) {
        Vec3 to = target.subtract(b.position());
        double d = Math.sqrt(to.x * to.x + to.z * to.z);
        double speed = Mth.clamp(0.14 + d * 0.047, 0.42, 2.3) * mult;
        b.kick(a, flat(to).scale(speed).add(0, 0.04, 0), Vec3.ZERO, d > 26 ? FootballEntity.TR_FAST : trail, sp);
        b.playBallSound(ModSounds.PASS.get(), 0.75F, 0.95F + a.getRandom().nextFloat() * 0.15F);
    }

    private static void lobPass(LivingEntity a, FootballEntity b, Vec3 target, double vyBase, Vec3 spin, int trail) {
        Vec3 to = target.subtract(b.position());
        double d = Math.sqrt(to.x * to.x + to.z * to.z);
        double vy = Mth.clamp(vyBase + d * 0.011, 0.45, 1.1);
        double h = solveLobH(d, vy);
        b.kick(a, flat(to).scale(h).add(0, vy, 0), spin, trail, FootballEntity.SP_NONE);
        b.playBallSound(ModSounds.KICK.get(), 0.7F, 1.15F);
    }

    // ------------------------------------------------------------------ preludes (start of the move)

    private static void prelude(LivingEntity a, Move m, ServerLevel l, int variant) {
        Vec3 f = facing(a);
        switch (m) {
            case BICYCLE -> push(a, new Vec3(0, 0.5, 0).add(f.scale(-0.05)));
            case HEADER, PUNCH -> push(a, a.getDeltaMovement().multiply(1, 0, 1).add(0, 0.42, 0));
            case SCORPION -> push(a, f.scale(0.3).add(0, 0.35, 0));
            case EAGLE_SHOT -> push(a, new Vec3(0, 0.55, 0));
            case BLOCK -> push(a, new Vec3(0, 0.45, 0));
            case SIUU -> push(a, new Vec3(0, 0.6, 0));
            case BACKFLIP -> push(a, f.scale(-0.12).add(0, 0.65, 0));
            case DIVE -> {
                Vec3 side = right(f).scale(variant == 0 ? 1 : -1);
                Scheduler.later(2, () -> push(a, side.scale(0.95).add(0, 0.32, 0)));
            }
            case FIRE_SHOT -> Scheduler.repeat(0, 12, k -> {
                double ang = k * 0.8;
                for (int i = 0; i < 3; i++) {
                    double aa = ang + i * 2.094;
                    l.sendParticles(ParticleTypes.FLAME, a.getX() + Math.cos(aa) * 0.9, a.getY() + 0.2 + k * 0.12, a.getZ() + Math.sin(aa) * 0.9, 1, 0, 0, 0, 0.01);
                }
            });
            case LIGHTNING_SHOT -> Scheduler.later(8, () -> {
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(l);
                if (bolt != null) {
                    Vec3 at = a.position().add(right(f).scale(-2.2)).add(f.scale(-1.0));
                    bolt.moveTo(at.x, at.y, at.z);
                    bolt.setVisualOnly(true);
                    l.addFreshEntity(bolt);
                }
                particles(l, ParticleTypes.ELECTRIC_SPARK, a.position().add(0, 1, 0), 40, 0.6, 0.4);
            });
            case TORNADO_SHOT -> Scheduler.repeat(0, 10, k -> {
                double ang = k * 1.2;
                for (int i = 0; i < 4; i++) {
                    double aa = ang + i * 1.57;
                    l.sendParticles(ParticleTypes.CLOUD, a.getX() + Math.cos(aa) * 1.2, a.getY() + 0.1 + k * 0.18, a.getZ() + Math.sin(aa) * 1.2, 1, 0, 0, 0, 0.0);
                }
            });
            case GHOST_DRIBBLE -> {
                FootData.of(a).evadeUntil = l.getGameTime() + 85;
                a.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 85, 2, false, false));
                sound(a, SoundEvents.SOUL_ESCAPE, 1.2F, 0.8F);
                Scheduler.repeat(0, 42, k -> {
                    if (!a.isAlive()) return;
                    l.sendParticles(ParticleTypes.SOUL, a.getX(), a.getY() + 0.8, a.getZ(), 2, 0.3, 0.5, 0.3, 0.01);
                    l.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, a.getX(), a.getY() + 0.2, a.getZ(), 1, 0.2, 0.1, 0.2, 0.0);
                });
            }
            case KNEE_SLIDE -> Scheduler.repeat(3, 18, k -> {
                if (!a.isAlive()) return;
                push(a, facing(a).scale(0.75 * (1 - k / 20.0)).add(0, a.getDeltaMovement().y, 0));
                l.sendParticles(ParticleTypes.POOF, a.getX(), a.getY() + 0.1, a.getZ(), 1, 0.2, 0.0, 0.2, 0.01);
            });
            case AIRPLANE -> a.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1, false, false));
            case DANCE -> Scheduler.repeat(0, 8, k -> l.sendParticles(ParticleTypes.NOTE, a.getX(), a.getY() + 2.3, a.getZ(), 1, 0.4, 0.2, 0.4, 1.0));
            default -> {}
        }
    }

    // ------------------------------------------------------------------ effects (impact tick)

    private static void effect(LivingEntity a, Move m, FootballEntity b, float charge, Vec3 aim, int variant) {
        ServerLevel l = (ServerLevel) a.level();
        long now = l.getGameTime();
        RandomSource r = a.getRandom();
        FootData fd = FootData.of(a);
        Vec3 f = facing(a);
        double side = variant == 0 ? 1 : -1; // +1 = right
        charge = Mth.clamp(charge, 0F, 1F);

        if (m.needsBall) {
            double reach = switch (m) {
                case HEADER, VOLLEY, BICYCLE, SCORPION -> 3.4;
                case FIRST_TOUCH -> 4.0;
                default -> 2.7;
            };
            if (!ballNear(a, b, reach) || b.frozen) {
                msg(a, "Iska!", ChatFormatting.GRAY);
                return;
            }
        }

        switch (m) {
            // ---------------- passes
            case SHORT_PASS -> groundPass(a, b, passPoint(a, flat(a.getLookAngle()), aim, 12, false), 1.0, FootballEntity.TR_NONE, 0);
            case THROUGH_PASS -> groundPass(a, b, passPoint(a, flat(a.getLookAngle()), aim, 18, true), 1.08, FootballEntity.TR_NONE, 0);
            case LOB_PASS -> {
                Vec3 t = passPoint(a, flat(a.getLookAngle()), aim, 24, false);
                lobPass(a, b, t, 0.55, topspin(flat(t.subtract(b.position())), -0.6), FootballEntity.TR_NONE);
            }
            case BACKHEEL_PASS -> {
                Vec3 back = f.scale(-1);
                LivingEntity mate = passTarget(a, back, Math.cos(Math.toRadians(70)), 1.01);
                Vec3 t = aim != null ? aim : mate != null ? mate.position() : a.position().add(back.scale(10));
                groundPass(a, b, t, 1.0, FootballEntity.TR_NONE, 0);
                b.playBallSound(ModSounds.SKILL.get(), 0.6F, 1.2F);
            }
            case RABONA_PASS -> {
                Vec3 t = passPoint(a, flat(a.getLookAngle()), aim, 16, false);
                double d = t.distanceTo(b.position());
                if (d > 20) lobPass(a, b, t, 0.5, curl(-0.5), FootballEntity.TR_CURL);
                else groundPass(a, b, t, 1.02, FootballEntity.TR_CURL, 0);
                b.playBallSound(ModSounds.SKILL.get(), 0.6F, 1.0F);
            }
            case NO_LOOK_PASS -> {
                Vec3 look = flat(a.getLookAngle());
                LivingEntity t = passTarget(a, look, Math.cos(Math.toRadians(140)), Math.cos(Math.toRadians(35)));
                Vec3 tp = aim != null ? aim : t != null ? t.position() : a.position().add(rotY(look, 90).scale(12));
                groundPass(a, b, tp, 1.05, FootballEntity.TR_NONE, 0);
            }
            case CROSS -> {
                Vec3 t = aim;
                Team tm = MatchManager.teamOf(a);
                Pitch p = MatchManager.pitch();
                if (t == null) {
                    if (p != null && tm.playing() && MatchManager.isActive()) {
                        Vec3 g = p.attackGoal(tm);
                        t = new Vec3(g.x - tm.attackDir() * 8.0, g.y, g.z + (r.nextDouble() - 0.5) * 6);
                        LivingEntity best = null;
                        double bd = 1e9;
                        for (LivingEntity e : mates(a)) {
                            double d = e.distanceToSqr(t);
                            if (d < bd && d < 100) {
                                bd = d;
                                best = e;
                            }
                        }
                        if (best != null) t = best.position();
                    } else {
                        t = a.position().add(flat(a.getLookAngle()).scale(22));
                    }
                }
                Vec3 dir = flat(t.subtract(b.position()));
                Vec3 goalward = p != null && tm.playing() ? flat(p.attackGoal(tm).subtract(b.position())) : dir;
                double s = new Vec3(dir.z, 0, -dir.x).dot(goalward) >= 0 ? 0.55 : -0.55;
                lobPass(a, b, t.add(dir.scale(-2.0)), 0.62, curl(s), FootballEntity.TR_CURL);
            }
            // ---------------- shots
            case SHOT -> {
                double speed = 1.15 + charge * 1.25;
                Aim am = shotAim(a, b, aim);
                b.kick(a, shotVel(am, speed, 0), Vec3.ZERO, speed > 1.9 ? FootballEntity.TR_FAST : FootballEntity.TR_NONE, 0);
                b.playBallSound(ModSounds.KICK.get(), 0.7F + charge * 0.5F, 1.1F - charge * 0.2F);
            }
            case FINESSE -> {
                double speed = 1.25 + charge * 0.7;
                Aim am = shotAim(a, b, aim);
                Vec3 dir = rotY(am.dir, 9 * side);
                b.kick(a, shotVel(new Aim(dir, am.dist, am.dy), speed, 0.04), curl(1.15 * side), FootballEntity.TR_CURL, 0);
                b.playBallSound(ModSounds.KICK.get(), 0.8F, 1.15F);
            }
            case POWER_SHOT -> {
                double speed = 2.05 + charge * 0.6;
                Aim am = shotAim(a, b, aim);
                b.kick(a, shotVel(am, speed, 0.03), topspin(am.dir, 0.55), FootballEntity.TR_FAST, 0);
                b.playBallSound(ModSounds.KICK_POWER.get(), 1.2F, 0.95F);
                particles(l, ParticleTypes.EXPLOSION, b.center(), 1, 0, 0);
                push(a, f.scale(-0.15).add(0, a.getDeltaMovement().y, 0));
            }
            case RABONA_SHOT -> {
                double speed = 1.5 + charge * 0.6;
                Aim am = shotAim(a, b, aim);
                Vec3 dir = rotY(am.dir, 7);
                b.kick(a, shotVel(new Aim(dir, am.dist, am.dy), speed, 0.03), curl(0.8), FootballEntity.TR_CURL, 0);
                b.playBallSound(ModSounds.KICK.get(), 0.9F, 1.05F);
                b.playBallSound(ModSounds.SKILL.get(), 0.5F, 1.0F);
            }
            case CHIP -> {
                Aim am = shotAim(a, b, aim);
                double d = am.dist > 0 ? am.dist : 18;
                double vy = Mth.clamp(0.55 + d * 0.006, 0.55, 0.8);
                double h = solveLobH(d + 1.5, vy);
                b.kick(a, am.dir.scale(h).add(0, vy, 0), topspin(am.dir, -0.6), FootballEntity.TR_GOLD, 0);
                b.playBallSound(ModSounds.KICK.get(), 0.6F, 1.35F);
            }
            case BICYCLE -> {
                Vec3 back = aim != null ? flat(aim.subtract(b.position())) : flat(a.getLookAngle()).scale(-1);
                double speed = 1.6 + charge * 0.6;
                Aim am = aim != null ? shotAim(a, b, aim) : new Aim(back, -1, 0.05);
                b.setPos(a.getX() + f.x * 0.2, a.getY() + 1.6, a.getZ() + f.z * 0.2);
                b.kick(a, shotVel(am, speed, 0), Vec3.ZERO, FootballEntity.TR_FAST, 0);
                b.playBallSound(ModSounds.KICK_POWER.get(), 1.0F, 1.2F);
                particles(l, ParticleTypes.CRIT, b.center(), 20, 0.3, 0.4);
            }
            case VOLLEY -> {
                double speed = 1.7 + charge * 0.5;
                Aim am = shotAim(a, b, aim);
                b.kick(a, shotVel(am, speed, 0), topspin(am.dir, 0.3), FootballEntity.TR_FAST, 0);
                b.playBallSound(ModSounds.KICK_POWER.get(), 0.9F, 1.15F);
            }
            case HEADER -> {
                double speed = 1.05 + charge * 0.45;
                Aim am = shotAim(a, b, aim);
                Vec3 v = am.dist > 0 ? shotVel(am, speed, 0) : am.dir.scale(speed).add(0, Mth.clamp(a.getLookAngle().y * speed, -0.4, 0.55), 0);
                b.kick(a, v, Vec3.ZERO, FootballEntity.TR_NONE, 0);
                b.playBallSound(ModSounds.HEADER.get(), 1.0F, 1.0F);
            }
            case KNUCKLE -> {
                double speed = 1.7 + charge * 0.5;
                Aim am = shotAim(a, b, aim);
                b.kick(a, shotVel(am, speed, 0.03), Vec3.ZERO, FootballEntity.TR_KNUCKLE, FootballEntity.SP_KNUCKLE);
                b.playBallSound(ModSounds.KICK_POWER.get(), 1.0F, 1.3F);
            }
            case SCORPION -> {
                b.setPos(a.getX() - f.x * 0.2, a.getY() + 1.4, a.getZ() - f.z * 0.2);
                Aim am = aim != null ? shotAim(a, b, aim) : new Aim(f, -1, 0.15);
                b.kick(a, shotVel(am, 1.45 + charge * 0.4, 0.04), Vec3.ZERO, FootballEntity.TR_GOLD, 0);
                b.playBallSound(ModSounds.KICK.get(), 1.0F, 1.25F);
                b.playBallSound(ModSounds.SKILL.get(), 0.7F, 0.9F);
            }
            case PANENKA -> {
                Aim am = shotAim(a, b, aim);
                double d = am.dist > 0 ? am.dist : 11;
                double vy = 0.42;
                double h = Mth.clamp(solveLobH(d + 2.0, vy), 0.35, 1.2);
                b.kick(a, am.dir.scale(h).add(0, vy, 0), topspin(am.dir, -0.3), FootballEntity.TR_GOLD, 0);
                b.playBallSound(ModSounds.KICK.get(), 0.5F, 1.5F);
            }
            case TRIVELA -> {
                double speed = 1.3 + charge * 0.7;
                Aim am = shotAim(a, b, aim);
                Vec3 dir = rotY(am.dir, -9 * side);
                b.kick(a, shotVel(new Aim(dir, am.dist, am.dy), speed, 0.04), curl(-1.2 * side), FootballEntity.TR_CURL, 0);
                b.playBallSound(ModSounds.KICK.get(), 0.85F, 1.2F);
            }
            // ---------------- skills
            case RAINBOW -> {
                b.setPos(a.getX() - f.x * 0.45, a.getY() + 0.15, a.getZ() - f.z * 0.45);
                b.kick(a, f.scale(0.25).add(0, 0.47, 0), Vec3.ZERO, FootballEntity.TR_GOLD, 0);
                b.immune(a, 14);
                b.noBodyCollision(14);
                fd.evadeUntil = now + 30;
                b.playBallSound(ModSounds.SKILL.get(), 0.9F, 1.1F);
            }
            case STEPOVER -> {
                b.kick(a, rotY(f, 45 * side).scale(0.42), Vec3.ZERO, 0, 0);
                b.immune(a, 4);
                fd.evadeUntil = now + 30;
                b.playBallSound(ModSounds.SKILL.get(), 0.7F, 1.25F);
            }
            case ELASTICO -> {
                b.kick(a, rotY(f, 70 * side).scale(0.2), Vec3.ZERO, 0, 0);
                b.immune(a, 3);
                fd.evadeUntil = now + 30;
                Scheduler.later(3, () -> {
                    if (b.isAlive() && b.getController() == null && b.getHolder() == null && b.distanceTo(a) < 2.5) {
                        b.kick(a, rotY(facing(a), -40 * side).scale(0.48), Vec3.ZERO, 0, 0);
                        b.immune(a, 3);
                        b.playBallSound(ModSounds.SKILL.get(), 0.8F, 1.4F);
                    }
                });
            }
            case ROULETTE -> {
                b.kick(a, rotY(f, -35 * side).scale(0.38), Vec3.ZERO, 0, 0);
                b.immune(a, 4);
                fd.evadeUntil = now + 35;
                b.playBallSound(ModSounds.SKILL.get(), 0.8F, 0.95F);
            }
            case CRUYFF -> {
                b.kick(a, rotY(f, 150 * side).scale(0.4), Vec3.ZERO, 0, 0);
                b.immune(a, 4);
                fd.evadeUntil = now + 25;
                b.playBallSound(ModSounds.SKILL.get(), 0.7F, 1.15F);
            }
            case NUTMEG -> {
                b.kick(a, f.scale(0.58).add(0, 0.02, 0), Vec3.ZERO, FootballEntity.TR_GOLD, 0);
                b.immune(a, 5);
                b.noBodyCollision(16);
                fd.evadeUntil = now + 25;
                b.playBallSound(ModSounds.SKILL.get(), 0.8F, 1.3F);
            }
            case BODY_FEINT -> {
                b.kick(a, rotY(f, -38 * side).scale(0.44), Vec3.ZERO, 0, 0);
                b.immune(a, 4);
                fd.evadeUntil = now + 28;
                push(a, rotY(f, -40 * side).scale(0.35).add(0, a.getDeltaMovement().y, 0));
                b.playBallSound(ModSounds.SKILL.get(), 0.6F, 1.1F);
            }
            case DRAGBACK -> {
                b.kick(a, f.scale(-0.24), Vec3.ZERO, 0, 0);
                b.immune(a, 2);
                fd.evadeUntil = now + 20;
            }
            case SEAL -> {
                b.setController(a);
                b.special = FootballEntity.SP_SEAL;
                b.specialOwner = a;
                b.specialTimer = 0;
                b.setTrail(FootballEntity.TR_GOLD);
                b.playBallSound(ModSounds.SKILL.get(), 0.9F, 1.4F);
            }
            case JUGGLE -> {
                b.setController(a);
                b.special = FootballEntity.SP_JUGGLE;
                b.specialOwner = a;
                b.specialTimer = 0;
                fd.evadeUntil = now + 20;
            }
            case SOMBRERO -> {
                b.kick(a, f.scale(0.2).add(0, 0.56, 0), Vec3.ZERO, FootballEntity.TR_GOLD, 0);
                b.immune(a, 10);
                b.noBodyCollision(14);
                fd.evadeUntil = now + 30;
                b.playBallSound(ModSounds.SKILL.get(), 0.8F, 1.2F);
            }
            case SPEED_BURST -> {
                a.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 50, 1, false, false));
                particles(l, ParticleTypes.CLOUD, a.position().add(0, 0.2, 0), 10, 0.3, 0.05);
                if (b != null && b.getController() == a) {
                    b.kick(a, f.scale(0.7).add(0, 0.03, 0), Vec3.ZERO, 0, 0);
                    b.immune(a, 6);
                    fd.evadeUntil = now + 15;
                }
            }
            case FIRST_TOUCH -> {
                b.setDeltaMovement(Vec3.ZERO);
                b.setController(a);
                b.playBallSound(ModSounds.PASS.get(), 0.6F, 0.8F);
                particles(l, ParticleTypes.CRIT, b.center(), 8, 0.2, 0.1);
            }
            // ---------------- defence
            case TACKLE -> tackle(a, b, l, now, 0.7, 3.0);
            case SLIDE -> {
                sound(a, ModSounds.SLIDE.get(), 1.0F, 1.0F);
                Scheduler.repeat(0, 12, k -> {
                    if (!a.isAlive()) return;
                    push(a, f.scale(0.95 * (1 - k / 14.0)).add(0, a.getDeltaMovement().y, 0));
                    l.sendParticles(ParticleTypes.POOF, a.getX(), a.getY() + 0.1, a.getZ(), 2, 0.25, 0.05, 0.25, 0.01);
                    slideHit(a, b, f, l);
                });
            }
            case SHOULDER -> {
                LivingEntity t = nearestOpponent(a, 2.6, Math.cos(Math.toRadians(75)));
                if (t != null) {
                    Vec3 dir = flat(t.position().subtract(a.position()));
                    t.knockback(0.7, -dir.x, -dir.z);
                    t.hurtMarked = true;
                    sound(a, ModSounds.TACKLE.get(), 0.9F, 0.8F);
                    if (b != null && b.getController() == t) {
                        FootData td = FootData.of(t);
                        if (now >= td.evadeUntil && r.nextFloat() < 0.55F + defend(a) * 0.25F) {
                            td.stunnedUntil = now + 15;
                            b.setController(null);
                            b.immune(t, 12);
                        }
                    }
                }
            }
            case BLOCK -> Scheduler.repeat(0, 12, k -> {
                if (b == null || !b.isAlive() || !a.isAlive()) return;
                Vec3 v = b.getDeltaMovement();
                if (v.length() > 0.5 && b.center().distanceTo(a.position().add(0, 1.0, 0)) < 1.7 && !b.isImmune(a)) {
                    b.kick(a, v.scale(-0.25).add(0, 0.25, 0), Vec3.ZERO, 0, 0);
                    sound(a, ModSounds.TACKLE.get(), 1.0F, 1.2F);
                    particles(l, ParticleTypes.CRIT, b.center(), 12, 0.2, 0.3);
                }
            });
            // ---------------- keeper
            case DIVE -> Scheduler.repeat(0, 10, k -> {
                if (b == null || !b.isAlive() || !a.isAlive() || b.getHolder() != null) return;
                if (b.center().distanceTo(a.position().add(0, 0.8, 0)) < 2.2 && (b.getController() == null)) {
                    if (a instanceof FootballerEntity bot && BotBrain.tryCatch(bot, b)) return;
                    Vec3 v = b.getDeltaMovement();
                    Vec3 away = keeperClear(a, b);
                    b.kick(a, away.scale(Math.max(0.5, v.length() * 0.45)).add(0, 0.35, 0), Vec3.ZERO, 0, 0);
                    sound(a, ModSounds.CATCH.get(), 1.0F, 1.3F);
                    MatchManager.onSave(a);
                }
            });
            case PUNCH -> {
                if (b != null && b.getHolder() == null && b.getController() == null && b.center().distanceTo(a.getEyePosition()) < 3.0) {
                    Vec3 away = keeperClear(a, b);
                    b.kick(a, away.scale(1.2).add(0, 0.55, 0), Vec3.ZERO, FootballEntity.TR_FAST, 0);
                    sound(a, ModSounds.TACKLE.get(), 1.2F, 0.7F);
                    MatchManager.onSave(a);
                }
            }
            case KEEPER_THROW -> {
                Vec3 t = aim != null ? aim : passPoint(a, flat(a.getLookAngle()), null, 20, false);
                b.setHolder(null);
                b.setPos(a.getX() + f.x * 0.6, a.getY() + 1.2, a.getZ() + f.z * 0.6);
                Vec3 to = t.subtract(b.position());
                double d = Math.sqrt(to.x * to.x + to.z * to.z);
                double vy = Mth.clamp(0.25 + d * 0.01, 0.25, 0.7);
                b.kick(a, flat(to).scale(solveLobH(d, vy)).add(0, vy, 0), Vec3.ZERO, 0, 0);
                b.immune(a, 8);
                b.playBallSound(ModSounds.PASS.get(), 0.9F, 0.8F);
            }
            // ---------------- supers
            case FIRE_SHOT -> {
                Aim am = shotAim(a, b, aim);
                b.kick(a, shotVel(am, 2.5, 0.02), Vec3.ZERO, FootballEntity.TR_FIRE, FootballEntity.SP_FIRE);
                b.playBallSound(ModSounds.KICK_POWER.get(), 1.5F, 0.8F);
                b.playBallSound(SoundEvents.FIRECHARGE_USE, 1.2F, 0.8F);
                particles(l, ParticleTypes.FLAME, b.center(), 50, 0.4, 0.15);
                particles(l, ParticleTypes.EXPLOSION, b.center(), 2, 0.2, 0);
                Net.shake(a, 8);
            }
            case LIGHTNING_SHOT -> {
                Aim am = shotAim(a, b, aim);
                b.kick(a, shotVel(am, 2.2, 0.02), Vec3.ZERO, FootballEntity.TR_LIGHTNING, FootballEntity.SP_LIGHTNING);
                b.playBallSound(ModSounds.KICK_POWER.get(), 1.4F, 1.3F);
                b.playBallSound(SoundEvents.TRIDENT_THUNDER, 0.7F, 1.4F);
                particles(l, ParticleTypes.ELECTRIC_SPARK, b.center(), 60, 0.4, 0.6);
                Net.shake(a, 6);
            }
            case TORNADO_SHOT -> {
                Aim am = shotAim(a, b, aim);
                b.kick(a, shotVel(am, 1.75, 0.05), Vec3.ZERO, FootballEntity.TR_TORNADO, FootballEntity.SP_TORNADO);
                b.playBallSound(ModSounds.KICK_POWER.get(), 1.3F, 1.0F);
                b.playBallSound(SoundEvents.ELYTRA_FLYING, 0.6F, 1.6F);
                Net.shake(a, 5);
            }
            case EAGLE_SHOT -> {
                Vec3 target;
                Team tm = MatchManager.teamOf(a);
                Pitch p = MatchManager.pitch();
                if (aim != null) target = aim;
                else if (p != null && tm.playing() && MatchManager.isActive())
                    target = p.attackGoal(tm).add(0, 0.6 + r.nextDouble() * 1.8, (r.nextDouble() - 0.5) * 5.0);
                else target = a.position().add(flat(a.getLookAngle()).scale(26)).add(0, 1, 0);
                Vec3 dir = flat(target.subtract(b.position()));
                b.kick(a, dir.scale(0.35).add(0, 0.95, 0), Vec3.ZERO, FootballEntity.TR_EAGLE, FootballEntity.SP_EAGLE);
                b.specialTarget = target;
                b.playBallSound(ModSounds.KICK_POWER.get(), 1.2F, 1.4F);
                b.playBallSound(SoundEvents.PHANTOM_SWOOP, 1.0F, 1.2F);
                particles(l, ParticleTypes.END_ROD, b.center(), 30, 0.3, 0.2);
            }
            case GHOST_DRIBBLE -> particles(l, ParticleTypes.SOUL, a.position().add(0, 1, 0), 30, 0.5, 0.05);
            case ICE_PASS -> {
                Vec3 t = passPoint(a, flat(a.getLookAngle()), aim, 18, true);
                groundPass(a, b, t, 1.15, FootballEntity.TR_ICE, FootballEntity.SP_ICE);
                b.playBallSound(SoundEvents.GLASS_BREAK, 0.8F, 1.6F);
                particles(l, ParticleTypes.SNOWFLAKE, b.center(), 30, 0.4, 0.05);
            }
            // ---------------- celebrations
            case SIUU -> Scheduler.later(14, () -> {
                l.sendParticles(ParticleTypes.FIREWORK, a.getX(), a.getY() + 0.2, a.getZ(), 30, 0.5, 0.1, 0.5, 0.15);
                l.sendParticles(new DustParticleOptions(new Vector3f(1F, 0.85F, 0.2F), 2F), a.getX(), a.getY() + 1, a.getZ(), 25, 0.8, 0.8, 0.8, 0);
                sound(a, ModSounds.CROWD_CHEER.get(), 0.7F, 1.2F);
            });
            case BACKFLIP -> l.sendParticles(ParticleTypes.CLOUD, a.getX(), a.getY(), a.getZ(), 10, 0.3, 0.0, 0.3, 0.02);
            case KNEE_SLIDE, AIRPLANE, DANCE -> l.sendParticles(new DustParticleOptions(new Vector3f(1F, 1F, 1F), 1.5F), a.getX(), a.getY() + 1, a.getZ(), 10, 0.5, 0.5, 0.5, 0);
            default -> {}
        }
    }

    private static float defend(LivingEntity a) { return a instanceof FootballerEntity bot ? bot.statDefend : 0.75F; }

    private static float dribble(LivingEntity a) { return a instanceof FootballerEntity bot ? bot.statDribble : 0.7F; }

    private static void tackle(LivingEntity a, FootballEntity b, ServerLevel l, long now, double base, double range) {
        Vec3 f = facing(a);
        push(a, f.scale(0.5).add(0, a.getDeltaMovement().y, 0));
        sound(a, ModSounds.TACKLE.get(), 0.8F, 1.1F);
        if (b == null || !b.isAlive()) return;
        LivingEntity c = b.getController();
        if (c == a) return;
        if (c != null) {
            if (a.distanceTo(c) > range) return;
            Team ta = MatchManager.teamOf(a);
            if (ta.playing() && ta == MatchManager.teamOf(c)) return;
            FootData cd = FootData.of(c);
            double p = base + 0.3 * (defend(a) - dribble(c));
            if (now < cd.evadeUntil) p *= 0.15;
            if (b.special == FootballEntity.SP_SEAL || b.special == FootballEntity.SP_JUGGLE) p *= 0.6;
            if (a.getRandom().nextDouble() < p) {
                b.special = FootballEntity.SP_NONE;
                cd.stunnedUntil = now + 12;
                b.setController(a);
                b.immune(c, 15);
                l.sendParticles(ParticleTypes.CRIT, b.getX(), b.getY() + 0.3, b.getZ(), 12, 0.3, 0.2, 0.3, 0.2);
                msg(a, "Top kapıldı!", ChatFormatting.GREEN);
                msg(c, "Top kaptırıldı!", ChatFormatting.RED);
            } else {
                FootData.of(a).stunnedUntil = now + 10;
                msg(a, "Müdahale boşa çıktı", ChatFormatting.GRAY);
                if (now < cd.evadeUntil) l.sendParticles(ParticleTypes.ENCHANTED_HIT, c.getX(), c.getY() + 1, c.getZ(), 10, 0.3, 0.4, 0.3, 0.2);
            }
        } else if (b.getHolder() == null && b.distanceTo(a) < 2.8) {
            b.setController(a);
        }
    }

    private static void slideHit(LivingEntity a, FootballEntity b, Vec3 f, ServerLevel l) {
        long now = l.getGameTime();
        if (b != null && b.isAlive() && b.getHolder() == null && b.center().distanceTo(a.position().add(0, 0.3, 0)) < 1.6 && !b.isImmune(a)) {
            LivingEntity c = b.getController();
            boolean ok = c == null || now >= FootData.of(c).evadeUntil || a.getRandom().nextFloat() < 0.15F;
            if (ok) {
                if (c != null) {
                    FootData.of(c).stunnedUntil = now + 20;
                    b.immune(c, 15);
                }
                b.kick(a, rotY(f, (a.getRandom().nextDouble() - 0.5) * 50).scale(0.8).add(0, 0.12, 0), Vec3.ZERO, 0, 0);
                b.immune(a, 20);
                sound(a, ModSounds.TACKLE.get(), 1.0F, 0.9F);
            }
        }
        for (LivingEntity e : l.getEntitiesOfClass(LivingEntity.class, a.getBoundingBox().inflate(0.6, 0, 0.6).move(f.scale(0.5)),
                e -> e != a && e.isAlive() && (e instanceof Player || e instanceof FootballerEntity))) {
            Team ta = MatchManager.teamOf(a);
            if (ta.playing() && ta == MatchManager.teamOf(e)) continue;
            FootData ed = FootData.of(e);
            if (ed.stunnedUntil > now) continue;
            ed.stunnedUntil = now + 20;
            e.knockback(0.5, -f.x, -f.z);
            e.hurtMarked = true;
        }
    }

    @Nullable
    private static LivingEntity nearestOpponent(LivingEntity a, double range, double minCos) {
        Team ta = MatchManager.teamOf(a);
        Vec3 f = facing(a);
        LivingEntity best = null;
        double bd = range * range;
        for (LivingEntity e : a.level().getEntitiesOfClass(LivingEntity.class, a.getBoundingBox().inflate(range),
                e -> e != a && e.isAlive() && (e instanceof Player p && !p.isSpectator() || e instanceof FootballerEntity))) {
            if (ta.playing() && MatchManager.teamOf(e) == ta) continue;
            Vec3 to = e.position().subtract(a.position());
            if (flat(to).dot(f) < minCos) continue;
            double d = to.lengthSqr();
            if (d < bd) {
                bd = d;
                best = e;
            }
        }
        return best;
    }

    private static Vec3 keeperClear(LivingEntity a, FootballEntity b) {
        Team t = MatchManager.teamOf(a);
        Vec3 base = t.playing() ? new Vec3(t.attackDir(), 0, 0) : facing(a);
        Vec3 side = new Vec3(0, 0, (b.getZ() - a.getZ()) >= 0 ? 1 : -1);
        return flat(base.add(side.scale(0.8)));
    }
}
