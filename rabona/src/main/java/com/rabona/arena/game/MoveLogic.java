package com.rabona.arena.game;

import com.rabona.arena.entity.BallEntity;
import com.rabona.arena.entity.FootballerEntity;
import com.rabona.arena.item.CleatsItem;
import com.rabona.arena.net.Net;
import com.rabona.arena.net.S2C;
import com.rabona.arena.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/** Hareketlerin sunucu tarafi: kosullar, zamanlama ve topa/rakiplere etkileri. */
public final class MoveLogic {
    private MoveLogic() {}

    /** Hareket istatistigi (test gunlugu icin). */
    public static final java.util.Map<String, Integer> COUNTS = new java.util.TreeMap<>();

    // ================================================================ baslatma
    public static boolean tryPerform(LivingEntity actor, Move m, float power, int side, Vec3 target) {
        if (!(actor.level() instanceof ServerLevel)) return false;
        Athlete a = Athlete.of(actor);
        if (a.stun > 0 && m.cat != Move.Cat.REACTION) return false;
        if (a.busy() && m.cat != Move.Cat.REACTION) return false;
        if (a.cooldowns[m.ordinal()] > 0) return false;
        if (m.cat == Move.Cat.CELEBRATION && Match.get(actor.getServer()).phase == Match.Phase.PLAYING && ballNear(actor, 6) != null) {
            return false;
        }
        if (a.stamina < m.stamina) {
            tell(actor, Component.translatable("msg.rabonaarena.tired").withStyle(ChatFormatting.GOLD));
            return false;
        }
        if (a.energy < m.energyCost()) {
            tell(actor, Component.translatable("msg.rabonaarena.no_energy", m.energyCost()).withStyle(ChatFormatting.YELLOW));
            return false;
        }
        BallEntity ball = null;
        switch (m.req) {
            case BALL -> {
                ball = footBall(actor);
                if (ball == null) return false;
            }
            case AIR -> {
                ball = airBall(actor);
                if (ball == null) return false;
            }
            case HELD -> {
                ball = heldBall(actor);
                if (ball == null) return false;
            }
            default -> {}
        }
        a.current = m;
        a.moveTick = 0;
        a.side = side;
        a.power = power;
        a.target = target;
        a.stamina -= m.stamina;
        a.energy -= m.energyCost();
        a.dirty = true;
        a.cooldowns[m.ordinal()] = m.cooldown;
        Net.toTrackingAndSelf(actor, new S2C.Anim(actor.getId(), m.ordinal(), side));
        COUNTS.merge(m.cat.name(), 1, Integer::sum);
        start(actor, a, m, ball);
        if (m.impact == 0) impact(actor, a, m);
        return true;
    }

    /** Tepki animasyonu (sersemleme, dusme, yakalama). */
    public static void react(LivingEntity e, Move m, int stun) {
        Athlete a = Athlete.of(e);
        a.current = m;
        a.moveTick = 0;
        a.stun = Math.max(a.stun, stun);
        Net.toTrackingAndSelf(e, new S2C.Anim(e.getId(), m.ordinal(), 0));
        if (e instanceof Player p && stun > 0) {
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, stun, 3, false, false, false));
        }
        BallEntity b = controlled(e);
        if (b != null && m != Move.CATCH) b.setController(null, false);
    }

    // ================================================================ tick
    public static void tick(LivingEntity actor) {
        Athlete a = Athlete.of(actor);
        a.tick();
        Vec3 p = actor.position();
        if (a.lastPos != null) a.vel = a.vel.scale(0.5).add(p.subtract(a.lastPos).scale(0.5));
        a.lastPos = p;
        boolean inMatch = Match.teamOf(actor).playing();
        // dayaniklilik
        if (actor.isSprinting()) {
            a.stamina = Math.max(0, a.stamina - (inMatch ? 0.22f : 0.12f));
            if (a.stamina < 3 && actor instanceof Player pl) pl.setSprinting(false);
        } else {
            a.stamina = Math.min(100, a.stamina + (a.current == null ? 0.32f : 0.1f));
        }
        if (inMatch && actor.tickCount % 40 == 0) a.addEnergy(1);
        if (Math.abs(a.stamina - a.lastSentStamina) >= 1 || Math.abs(a.energy - a.lastSentEnergy) >= 1) a.dirty = true;

        abilityTick(actor, a);

        Move m = a.current;
        if (m == null) return;
        a.moveTick++;
        if (a.moveTick == m.impact) impact(actor, a, m);
        during(actor, a, m, a.moveTick);
        if (a.moveTick >= m.duration) a.current = null;
    }

    // ================================================================ top bulma
    public static BallEntity ballNear(LivingEntity e, double r) {
        BallEntity best = null;
        double bd = r * r;
        for (BallEntity b : e.level().getEntitiesOfClass(BallEntity.class, e.getBoundingBox().inflate(r))) {
            double d = b.center().distanceToSqr(e.position().add(0, 0.5, 0));
            if (d < bd) { bd = d; best = b; }
        }
        return best;
    }

    public static BallEntity controlled(LivingEntity e) {
        for (BallEntity b : e.level().getEntitiesOfClass(BallEntity.class, e.getBoundingBox().inflate(4))) {
            if (b.getControllerId() == e.getId()) return b;
        }
        return null;
    }

    /** Ayaktaki ya da ayak dibindeki top. */
    public static BallEntity footBall(LivingEntity e) {
        BallEntity c = controlled(e);
        if (c != null) return c.isHeld() ? null : c;
        BallEntity b = ballNear(e, 2.6);
        if (b == null || b.isHeld()) return null;
        LivingEntity other = b.getController();
        if (other != null && other != e) return null;
        double h = horiz(b.center(), e.position());
        double rel = b.getY() - e.getY();
        return h < 2.0 && rel < 1.0 && rel > -0.6 ? b : null;
    }

    public static BallEntity airBall(LivingEntity e) {
        BallEntity b = ballNear(e, 3.4);
        if (b == null || b.isHeld()) return null;
        if (b.getController() != null && b.getController() != e) return null;
        double h = horiz(b.center(), e.position());
        double rel = b.center().y - e.getY();
        return h < 3.0 && rel > 0.45 && rel < 3.2 ? b : null;
    }

    public static BallEntity heldBall(LivingEntity e) {
        BallEntity c = controlled(e);
        return c != null && c.isHeld() ? c : null;
    }

    static double horiz(Vec3 a, Vec3 b) {
        double dx = a.x - b.x, dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    // ================================================================ yon yardimcilari
    public static Vec3 face(LivingEntity e) {
        float y = (e instanceof Player ? e.getYRot() : e.getYHeadRot()) * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(y), 0, Mth.cos(y));
    }

    static Vec3 right(Vec3 f) { return new Vec3(-f.z, 0, f.x); }

    static Vec3 flat(Vec3 v) {
        Vec3 h = new Vec3(v.x, 0, v.z);
        return h.lengthSqr() < 1e-8 ? new Vec3(0, 0, 1) : h.normalize();
    }

    static Vec3 rotY(Vec3 v, double deg) { return v.yRot((float) Math.toRadians(deg)); }

    /** ust yonlu falso: up x v (topspin icin +). */
    static Vector3f topspin(Vec3 dir, float amount) {
        Vec3 ax = new Vec3(0, 1, 0).cross(flat(dir));
        return new Vector3f((float) ax.x * amount, (float) ax.y * amount, (float) ax.z * amount);
    }

    static Vector3f sidespin(float amount) { return new Vector3f(0, amount, 0); }

    // ================================================================ hedefleme
    /** Bakis/hedef yonune gore sut hizi: nisan noktasina ulasacak dikey hiz hesaplanir. */
    static Vec3 aimShot(LivingEntity actor, Athlete a, BallEntity ball, double speed, double minLift, double curveDeg) {
        Vec3 from = ball.center();
        Vec3 aim = a.target;
        Vec3 look = actor.getLookAngle();
        if (aim == null) {
            Match match = Match.get(actor.getServer());
            Vec3 goal = match.targetGoal(actor);
            Vec3 f = flat(look);
            if (goal != null && match.pitch != null) {
                Vec3 g = goal.subtract(from);
                double dist = Math.sqrt(g.x * g.x + g.z * g.z);
                if (dist < 45 && f.dot(flat(g)) > 0.35) {
                    Pitch pt = match.pitch;
                    // bakis isininin kale cizgisini kestigi nokta
                    double side = Math.signum(pt.a(goal) == 0 ? 1 : pt.a(goal));
                    double la = pt.a(from), lb = pt.b(from);
                    Vec3 lf = new Vec3(pt.axisA().dot(look), look.y, pt.axisB().dot(look));
                    double t = Math.abs(lf.x) < 1e-3 ? dist : (side * pt.goalLineA() - la) / lf.x;
                    if (t < 0) t = dist;
                    double hitB = Mth.clamp(lb + lf.z * t, -Pitch.GOAL_HALF_W + 0.55, Pitch.GOAL_HALF_W - 0.55);
                    double hitY = Mth.clamp(actor.getEyeY() + look.y * t, pt.surfaceY() + 0.25, pt.surfaceY() + Pitch.GOAL_H - 0.3);
                    Vec3 assisted = pt.world(side * (pt.goalLineA() + 0.3), hitB, hitY);
                    Vec3 free = from.add(look.scale(dist));
                    aim = free.scale(0.3).add(assisted.scale(0.7));
                }
            }
            if (aim == null) {
                aim = from.add(look.scale(25)).add(0, 1.0, 0);
            }
        }
        Vec3 d = aim.subtract(from);
        double hd = Math.sqrt(d.x * d.x + d.z * d.z);
        Vec3 dir = flat(d);
        // hata: yorgunluk + asiri yukleme
        float tired = 1 - a.stamina / 100f;
        double over = Math.max(0, a.power - 1.0) * 30;
        double err = tired * 3.5 + over + (actor instanceof FootballerEntity fb ? (100 - fb.shooting) * 0.07 : 0);
        dir = rotY(dir, (actor.getRandom().nextDouble() - 0.5) * 2 * err + curveDeg);
        double tt = Math.max(3, hd / speed * 1.12);
        double vy = (d.y + 0.5 * BallEntity.GRAVITY * tt * tt) / tt;
        vy += over * 0.012;
        vy = Mth.clamp(vy, minLift, speed * 0.75 + 0.1);
        return dir.scale(speed).add(0, vy, 0);
    }

    /** En uygun takim arkadasi (bakis konisinde). */
    static LivingEntity findMate(LivingEntity actor, Vec3 dir, double maxAngleCos) {
        Team t = Match.teamOf(actor);
        LivingEntity best = null;
        double bestScore = -1e9;
        for (LivingEntity e : actor.level().getEntitiesOfClass(LivingEntity.class, actor.getBoundingBox().inflate(48),
                o -> o != actor && o.isAlive() && (o instanceof Player || o instanceof FootballerEntity))) {
            if (t.playing() ? Match.teamOf(e) != t : !(e instanceof Player || e instanceof FootballerEntity)) continue;
            if (!t.playing() && Match.teamOf(e).playing()) continue;
            Vec3 to = e.position().subtract(actor.position());
            double dist = to.length();
            if (dist < 2.5) continue;
            double cos = flat(to).dot(dir);
            if (cos < maxAngleCos) continue;
            double score = cos * 30 - dist * 0.35;
            if (score > bestScore) { bestScore = score; best = e; }
        }
        return best;
    }

    static Vec3 groundPass(Vec3 from, Vec3 to) {
        Vec3 d = to.subtract(from);
        double dist = Math.sqrt(d.x * d.x + d.z * d.z);
        double v = Mth.clamp(dist / 19.0 + 0.16, 0.35, 1.55);
        return flat(d).scale(v).add(0, 0.02, 0);
    }

    static Vec3 loftPass(Vec3 from, Vec3 to, double height) {
        Vec3 d = to.subtract(from);
        double dist = Math.sqrt(d.x * d.x + d.z * d.z) * 0.88;
        double tt = Mth.clamp(13 + dist * 0.5 * height, 14, 44);
        double vh = dist / tt * 1.15;
        double vy = 0.5 * BallEntity.GRAVITY * tt;
        return flat(d).scale(vh).add(0, vy, 0);
    }

    static Vec3 passTarget(LivingEntity actor, Athlete a, boolean lead, double fallback) {
        if (a.target != null) return a.target;
        Vec3 dir = flat(actor.getLookAngle());
        LivingEntity mate = findMate(actor, dir, 0.72);
        if (mate == null) return actor.position().add(dir.scale(fallback));
        Athlete ma = Athlete.of(mate);
        Vec3 p = mate.position();
        if (lead) {
            Vec3 att = Match.get(actor.getServer()).attackDir(actor);
            p = p.add(ma.vel.scale(14)).add(att == null ? dir.scale(4) : att.scale(4));
        } else {
            p = p.add(ma.vel.scale(6));
        }
        return p;
    }

    // ================================================================ baslangic etkileri
    private static void start(LivingEntity actor, Athlete a, Move m, BallEntity ball) {
        ServerLevel sl = (ServerLevel) actor.level();
        Vec3 f = face(actor);
        switch (m) {
            case SLIDE -> {
                push(actor, f.scale(0.78).add(0, 0.05, 0));
                sound(actor, ModSounds.SLIDE.get(), 1f, 1f);
            }
            case GK_DIVE_LEFT, GK_DIVE_RIGHT -> {
                int s = m == Move.GK_DIVE_LEFT ? -1 : 1;
                push(actor, right(f).scale(0.72 * s).add(0, 0.36, 0));
            }
            case BLOCK -> push(actor, actor.getDeltaMovement().add(0, 0.42, 0));
            case PRESS -> {
                BallEntity b = ballNear(actor, 20);
                Vec3 to = b == null ? f : flat(b.position().subtract(actor.position()));
                push(actor, to.scale(0.6).add(0, 0.05, 0));
                actor.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30, 1, false, false, true));
            }
            case DIVING_HEADER -> push(actor, f.scale(0.55).add(0, 0.25, 0));
            case BICYCLE, SCORPION -> push(actor, actor.getDeltaMovement().multiply(0.3, 0, 0.3).add(0, 0.42, 0));
            case ROULETTE, SEAL, AROUND_WORLD, STEPOVER, ELASTICO -> a.evade = Math.max(a.evade, m.duration + 4);
            case CELEB_SIU -> push(actor, new Vec3(0, 0.55, 0));
            case CELEB_BACKFLIP -> push(actor, f.scale(-0.15).add(0, 0.6, 0));
            case CELEB_KNEESLIDE -> {
                push(actor, f.scale(0.9));
                sound(actor, ModSounds.SLIDE.get(), 1f, 0.8f);
            }
            case AB_FIRE, AB_TORNADO, AB_THUNDER, AB_GHOST, AB_MAGNET, AB_ICE -> {
                particles(sl, ParticleTypes.FLASH, actor.position().add(0, 1, 0), 1, 0, 0);
                Net.toTrackingAndSelf(actor, new S2C.Shake(actor.getId(), 0.4f));
            }
            default -> {}
        }
        if (ball != null && m.cat == Move.Cat.SHOT) a.shots++;
    }

    // ================================================================ etki ani
    private static void impact(LivingEntity actor, Athlete a, Move m) {
        ServerLevel sl = (ServerLevel) actor.level();
        Vec3 f = face(actor);
        Vec3 r = right(f);
        float p = Mth.clamp(a.power, 0, 1);
        boolean cleats = CleatsItem.wearing(actor);
        double boost = cleats ? 1.1 : 1.0;
        int side = a.side == 0 ? 1 : a.side;
        BallEntity ball = switch (m.req) {
            case BALL -> footBall(actor);
            case AIR -> airBall(actor);
            case HELD -> heldBall(actor);
            default -> null;
        };
        if (m.req != Move.Req.NONE && ball == null) return; // iskaladi
        switch (m) {
            // ---------------- PASLAR
            case PASS_SHORT -> ball.kick(actor, groundPass(ball.center(), passTarget(actor, a, false, 12)), new Vector3f(), 0, 0);
            case PASS_THROUGH -> ball.kick(actor, groundPass(ball.center(), passTarget(actor, a, true, 18)).scale(1.05), new Vector3f(), 0, 0);
            case PASS_LOB -> ball.kick(actor, loftPass(ball.center(), passTarget(actor, a, true, 24), 1.0), topspin(f, -0.15f), 0, 0);
            case PASS_CROSS -> {
                Vec3 to = passTarget(actor, a, true, 26);
                Vec3 v = loftPass(ball.center(), to, 0.8);
                ball.kick(actor, rotY(v, 7 * side), sidespin(-0.3f * side), BallEntity.FX_CURL, 30);
            }
            case PASS_BACKHEEL -> {
                Vec3 to = a.target != null ? a.target : actor.position().subtract(f.scale(10));
                LivingEntity mate = a.target == null ? findMate(actor, f.scale(-1), 0.6) : null;
                if (mate != null) to = mate.position();
                ball.kick(actor, groundPass(ball.center(), to), new Vector3f(), 0, 0);
            }
            case PASS_NOLOOK -> {
                Vec3 sideDir = r.scale(side);
                LivingEntity mate = findMate(actor, sideDir, 0.3);
                Vec3 to = a.target != null ? a.target : mate != null ? mate.position() : actor.position().add(sideDir.scale(12));
                ball.kick(actor, groundPass(ball.center(), to), new Vector3f(), 0, 0);
                affectOpponents(actor, 4, 0.2, 0.5, 14);
            }
            case PASS_RABONA -> {
                Vec3 to = passTarget(actor, a, true, 22);
                ball.kick(actor, rotY(loftPass(ball.center(), to, 0.9), -6 * side), sidespin(0.32f * side), BallEntity.FX_RAINBOW, 30);
                a.addEnergy(3);
            }
            // ---------------- SUTLAR
            case SHOT_POWER -> {
                Vec3 v = aimShot(actor, a, ball, (1.05 + 0.85 * p) * boost, -0.05, 0);
                ball.kick(actor, v, topspin(v, 0.12f), p > 0.8 ? BallEntity.FX_POWER : 0, 30);
                if (p > 0.85) Net.toTrackingAndSelf(actor, new S2C.Shake(actor.getId(), 0.35f));
            }
            case SHOT_FINESSE -> {
                Vec3 v = aimShot(actor, a, ball, (0.95 + 0.5 * p) * boost, 0.02, -9 * side);
                ball.kick(actor, v, sidespin(0.5f * side), BallEntity.FX_CURL, 40);
            }
            case SHOT_CHIP -> {
                Vec3 v = aimShot(actor, a, ball, 0.55 + 0.3 * p, 0.42, 0);
                ball.kick(actor, new Vec3(v.x, Math.max(v.y, 0.45 + 0.12 * p), v.z), topspin(v, -0.25f), 0, 0);
            }
            case SHOT_TRIVELA -> {
                Vec3 v = aimShot(actor, a, ball, (1.15 + 0.55 * p) * boost, 0.0, 10 * side);
                ball.kick(actor, v, sidespin(-0.55f * side), BallEntity.FX_CURL, 40);
            }
            case SHOT_KNUCKLE -> {
                Vec3 v = aimShot(actor, a, ball, (1.35 + 0.45 * p) * boost, 0.05, 0);
                ball.kick(actor, v, new Vector3f(), BallEntity.FX_KNUCKLE, 40);
            }
            case SHOT_RABONA -> {
                Vec3 v = aimShot(actor, a, ball, (1.1 + 0.55 * p) * boost, 0.0, -5 * side);
                ball.kick(actor, v, sidespin(0.3f * side), BallEntity.FX_RAINBOW, 35);
                a.addEnergy(5);
            }
            case SHOT_PANENKA -> {
                Vec3 v = aimShot(actor, a, ball, 0.5, 0.3, 0);
                ball.kick(actor, new Vec3(v.x, 0.36, v.z), topspin(v, -0.2f), 0, 0);
                a.addEnergy(4);
            }
            case SHOT_TOEPOKE -> {
                Vec3 v = aimShot(actor, a, ball, 1.2 * boost, -0.02, 0);
                ball.kick(actor, v, new Vector3f(), 0, 0);
            }
            // ---------------- HAVA
            case HEADER -> {
                Vec3 look = actor.getLookAngle();
                Vec3 v = a.target != null ? aimShot(actor, a, ball, 0.95, -0.2, 0) : look.scale(0.95).add(0, 0.12, 0);
                ball.kick(actor, v, new Vector3f(), 0, 0);
            }
            case DIVING_HEADER -> {
                Vec3 v = aimShot(actor, a, ball, 1.35 * boost, -0.15, 0);
                ball.kick(actor, v, new Vector3f(), BallEntity.FX_POWER, 20);
            }
            case VOLLEY -> {
                Vec3 v = aimShot(actor, a, ball, (1.4 + 0.4 * p) * boost, -0.08, 0);
                ball.kick(actor, v, topspin(v, 0.25f), BallEntity.FX_POWER, 25);
            }
            case BICYCLE -> {
                Vec3 v = aimShot(actor, a, ball, 1.65 * boost, -0.12, 0);
                ball.kick(actor, v, topspin(v, 0.3f), BallEntity.FX_POWER, 30);
                Net.toTrackingAndSelf(actor, new S2C.Shake(actor.getId(), 0.5f));
                a.addEnergy(8);
            }
            case SCORPION -> {
                Vec3 v = aimShot(actor, a, ball, 1.15 * boost, 0.05, 0);
                ball.kick(actor, v, topspin(v, -0.2f), BallEntity.FX_RAINBOW, 30);
                a.addEnergy(10);
            }
            case CHEST_CONTROL -> {
                ball.setDeltaMovement(f.scale(0.12).add(0, -0.1, 0));
                ball.setSpin(new Vector3f());
                ball.setController(actor, false);
            }
            // ---------------- CALIMLAR
            case RAINBOW -> {
                Vec3 behind = actor.position().subtract(f.scale(0.35));
                ball.setController(null, false);
                ball.setPos(behind.x, actor.getY() + 0.05, behind.z);
                ball.kick(actor, f.scale(0.17).add(0, 0.45, 0), topspin(f, -0.1f), BallEntity.FX_RAINBOW, 30);
                ball.makeImmune(actor, 14);
                Scheduler.later(5, () -> push(actor, f.scale(0.30)));
                skillBeat(actor, a, 4.5, 0.7, 26);
            }
            case ELASTICO -> {
                ball.setController(null, false);
                ball.kick(actor, r.scale(0.22 * side), new Vector3f(), 0, 0);
                ball.makeImmune(actor, 3);
                Scheduler.later(3, () -> {
                    if (ball.isAlive() && ball.isFree()) {
                        Vec3 nd = f.scale(0.38).add(r.scale(-0.3 * side));
                        ball.setDeltaMovement(nd);
                        push(actor, nd.scale(1.1));
                        sound(actor, ModSounds.SKILL.get(), 0.8f, 1.2f);
                    }
                });
                skillBeat(actor, a, 3.5, 0.6, 22);
            }
            case ROULETTE -> {
                push(actor, r.scale(0.30 * side).add(f.scale(0.2)));
                skillBeat(actor, a, 3.5, 0.55, 20);
            }
            case STEPOVER -> {
                Scheduler.later(4, () -> push(actor, r.scale(-0.38 * side).add(f.scale(0.25))));
                skillBeat(actor, a, 3.5, 0.45, 16);
            }
            case CRUYFF -> {
                ball.setController(null, false);
                ball.setDeltaMovement(f.scale(-0.3).add(r.scale(0.08 * side)));
                ball.makeImmune(actor, 4);
                turnAround(actor);
                skillBeat(actor, a, 3, 0.5, 18);
            }
            case DRAGBACK -> {
                ball.setController(null, false);
                ball.setDeltaMovement(f.scale(-0.26));
                ball.makeImmune(actor, 3);
                push(actor, f.scale(-0.3));
                skillBeat(actor, a, 3, 0.35, 12);
            }
            case CROQUETA -> {
                ball.setController(null, false);
                ball.setDeltaMovement(r.scale(0.36 * side).add(f.scale(0.05)));
                push(actor, r.scale(0.35 * side).add(f.scale(0.08)));
                skillBeat(actor, a, 3, 0.55, 16);
            }
            case SOMBRERO -> {
                ball.setController(null, false);
                ball.kick(actor, f.scale(0.17).add(0, 0.5, 0), topspin(f, -0.12f), BallEntity.FX_RAINBOW, 20);
                ball.makeImmune(actor, 10);
                for (LivingEntity o : opponents(actor, 4, 0.3)) ball.makeImmune(o, 22);
                Scheduler.later(6, () -> push(actor, f.scale(0.29)));
                skillBeat(actor, a, 4, 0.6, 22);
            }
            case NUTMEG -> {
                ball.setController(null, false);
                ball.kick(actor, f.scale(0.62), new Vector3f(), 0, 0);
                ball.makeImmune(actor, 6);
                for (LivingEntity o : opponents(actor, 4, 0.4)) ball.makeImmune(o, 25);
                push(actor, r.scale(0.25 * side).add(f.scale(0.3)));
                Scheduler.later(5, () -> push(actor, f.scale(0.32).add(r.scale(-0.2 * side))));
                skillBeat(actor, a, 4, 0.7, 24);
            }
            case CHOP -> {
                ball.setController(null, false);
                Vec3 nd = r.scale(-0.4 * side).add(f.scale(-0.08));
                ball.setDeltaMovement(nd);
                ball.makeImmune(actor, 3);
                push(actor, nd.scale(1.05));
                skillBeat(actor, a, 3, 0.5, 16);
            }
            case HEEL_FLICK -> {
                ball.setController(null, false);
                Vec3 behind = actor.position().subtract(f.scale(0.3));
                ball.setPos(behind.x, actor.getY() + 0.1, behind.z);
                ball.kick(actor, f.scale(0.26).add(0, 0.5, 0), topspin(f, 0.15f), BallEntity.FX_RAINBOW, 20);
                ball.makeImmune(actor, 12);
                Scheduler.later(4, () -> push(actor, f.scale(0.27)));
                skillBeat(actor, a, 4, 0.6, 20);
            }
            case SEAL -> {
                ball.setController(actor, false);
                ball.airDribbleTicks = 70;
                a.evade = 70;
                a.addEnergy(8);
            }
            case AROUND_WORLD -> {
                ball.setController(actor, false);
                ball.airDribbleTicks = 28;
                ball.juggle = true;
                a.addEnergy(12);
                tell(actor, Component.translatable("msg.rabonaarena.style").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            case SPEED_BURST -> {
                ball.kick(actor, f.scale(0.78), new Vector3f(), 0, 0);
                ball.makeImmune(actor, 4);
                push(actor, f.scale(0.43));
                actor.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 2, false, false, true));
            }
            case BODY_FEINT -> {
                // govdeyle bir tarafa yatip ters yone cikis; top ayakta kalir
                Scheduler.later(4, () -> push(actor, r.scale(-0.3 * side).add(f.scale(0.16))));
                a.evade = Math.max(a.evade, 12);
                skillBeat(actor, a, 3.2, 0.5, 16);
            }
            case FAKE_SHOT -> {
                List<LivingEntity> fooled = opponents(actor, 7, 0.2);
                for (LivingEntity o : fooled) {
                    if (o instanceof FootballerEntity fb && fb.isKeeper()) {
                        MoveLogic.tryPerform(fb, actor.getRandom().nextBoolean() ? Move.GK_DIVE_LEFT : Move.GK_DIVE_RIGHT, 1, 0, null);
                    }
                }
                skillBeat(actor, a, 5, 0.65, 22);
            }
            // ---------------- SAVUNMA
            case TACKLE -> tackle(actor, a, 2.1, 0.66);
            case SHOULDER -> {
                for (LivingEntity o : opponents(actor, 1.9, 0.0)) {
                    Vec3 away = flat(o.position().subtract(actor.position()));
                    push(o, away.scale(0.55).add(0, 0.15, 0));
                    BallEntity b = controlled(o);
                    if (b != null && !b.isHeld() && actor.getRandom().nextFloat() < 0.55 && Athlete.of(o).evade <= 0) {
                        b.setController(actor, false);
                        a.tackles++;
                        a.addEnergy(6);
                    }
                    react(o, Move.STUMBLE, 10);
                    sound(actor, ModSounds.TACKLE.get(), 1f, 0.9f);
                    break;
                }
            }
            // ---------------- KALECI
            case GK_PUNT -> {
                ball.setController(null, false);
                Vec3 v = f.scale(1.25 + 0.3 * p).add(0, 0.7, 0);
                ball.kick(actor, v, topspin(f, -0.1f), BallEntity.FX_POWER, 20);
            }
            case GK_THROW -> {
                ball.setController(null, false);
                Vec3 to = passTarget(actor, a, false, 16);
                ball.kick(actor, groundPass(ball.center(), to).scale(1.1).add(0, 0.12, 0), new Vector3f(), 0, 0);
            }
            // ---------------- YETENEKLER
            case AB_FIRE -> {
                Vec3 v = aimShot(actor, a, ball, 2.15 * boost, -0.02, 0);
                ball.kick(actor, v, new Vector3f(), BallEntity.FX_FIRE, 70);
                sound(actor, ModSounds.FIRE.get(), 1.6f, 1f);
                particles(sl, ParticleTypes.FLAME, ball.center(), 40, 0.4, 0.2);
                Net.toTrackingAndSelf(actor, new S2C.Shake(actor.getId(), 0.8f));
            }
            case AB_TORNADO -> {
                Vec3 v = aimShot(actor, a, ball, 1.45 * boost, 0.05, 0);
                ball.kick(actor, v, sidespin(0.2f * side), BallEntity.FX_TORNADO, 60);
                sound(actor, ModSounds.WIND.get(), 1.4f, 1f);
                particles(sl, ParticleTypes.CLOUD, ball.center(), 30, 0.6, 0.15);
            }
            case AB_THUNDER -> {
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(sl);
                if (bolt != null) {
                    bolt.moveTo(actor.getX(), actor.getY(), actor.getZ());
                    bolt.setVisualOnly(true);
                    sl.addFreshEntity(bolt);
                }
                actor.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 3, false, false, true));
                actor.addEffect(new MobEffectInstance(MobEffects.JUMP, 120, 1, false, false, true));
                a.thunder = 120;
                sound(actor, ModSounds.THUNDER.get(), 1.5f, 1f);
            }
            case AB_GHOST -> {
                actor.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 100, 0, false, false, true));
                actor.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 1, false, false, true));
                a.ghost = 100;
                a.evade = Math.max(a.evade, 100);
                sound(actor, ModSounds.GHOST.get(), 1.4f, 1f);
                particles(sl, ParticleTypes.SOUL, actor.position().add(0, 1, 0), 30, 0.5, 0.05);
            }
            case AB_MAGNET -> {
                a.magnet = 50;
                sound(actor, ModSounds.MAGNET.get(), 1.4f, 1f);
            }
            case AB_ICE -> {
                sound(actor, ModSounds.ICE.get(), 1.5f, 1f);
                particles(sl, ParticleTypes.SNOWFLAKE, actor.position().add(0, 1, 0), 120, 3.5, 0.05);
                for (LivingEntity o : opponents(actor, 8, -1.1)) {
                    o.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 70, 2, false, true, true));
                    react(o, Move.STUMBLE, 24);
                    particles(sl, ParticleTypes.SNOWFLAKE, o.position().add(0, 1, 0), 30, 0.4, 0.02);
                }
            }
            default -> {}
        }
        if (ball != null) ball.lastMove = m.ordinal();
    }

    // ================================================================ hareket suresince
    private static void during(LivingEntity actor, Athlete a, Move m, int t) {
        Vec3 f = face(actor);
        switch (m) {
            case SLIDE -> {
                if (t < 12) {
                    double sp = 0.7 * (1 - t / 13.0);
                    Vec3 v = actor.getDeltaMovement();
                    push(actor, new Vec3(f.x * sp, Math.min(v.y, 0), f.z * sp));
                    if (t % 3 == 0) particles((ServerLevel) actor.level(), ParticleTypes.COMPOSTER, actor.position(), 4, 0.3, 0.05);
                    slideHit(actor, a);
                }
            }
            case GK_DIVE_LEFT, GK_DIVE_RIGHT -> {
                if (t >= 1 && t <= 16) diveCatch(actor, a, m == Move.GK_DIVE_LEFT ? -1 : 1);
            }
            case BLOCK -> {
                if (t <= 13) blockBall(actor);
            }
            case ROULETTE -> {
                BallEntity b = controlled(actor);
                if (b == null && t < 8) {
                    BallEntity n = footBall(actor);
                    if (n != null && n.isFree()) n.setController(actor, false);
                }
            }
            case CELEB_KNEESLIDE -> {
                if (t < 20) {
                    double sp = 0.7 * (1 - t / 22.0);
                    push(actor, new Vec3(f.x * sp, actor.getDeltaMovement().y, f.z * sp));
                    if (t % 2 == 0) particles((ServerLevel) actor.level(), ParticleTypes.COMPOSTER, actor.position(), 3, 0.3, 0.05);
                }
            }
            case CELEB_SIU -> {
                if (t == 16) {
                    particles((ServerLevel) actor.level(), ParticleTypes.TOTEM_OF_UNDYING, actor.position().add(0, 1, 0), 60, 0.6, 0.4);
                    sound(actor, ModSounds.CROWD_GOAL.get(), 0.8f, 1.2f);
                }
            }
            case CELEB_PLANE, CELEB_DANCE -> {
                if (t % 6 == 0) particles((ServerLevel) actor.level(), ParticleTypes.NOTE, actor.position().add(0, 2.2, 0), 1, 0.3, 0);
            }
            case DIVING_HEADER -> {
                if (t > 3 && t < 12) {
                    BallEntity b = airBall(actor);
                    if (b != null && t > m.impact && b.isFree() && !b.isImmune(actor) && b.center().distanceTo(actor.position().add(f.scale(0.6)).add(0, 0.6, 0)) < 1.3) {
                        Vec3 v = aimShot(actor, a, b, 1.3, -0.15, 0);
                        b.kick(actor, v, new Vector3f(), BallEntity.FX_POWER, 20);
                    }
                }
            }
            default -> {}
        }
    }

    private static void abilityTick(LivingEntity actor, Athlete a) {
        ServerLevel sl = (ServerLevel) actor.level();
        if (a.thunder > 0 && a.thunder % 2 == 0) {
            particles(sl, ParticleTypes.ELECTRIC_SPARK, actor.position().add(0, 1, 0), 4, 0.35, 0.2);
        }
        if (a.ghost > 0 && a.ghost % 3 == 0) {
            particles(sl, ParticleTypes.SOUL_FIRE_FLAME, actor.position().add(0, 0.2, 0), 2, 0.25, 0.01);
        }
        if (a.magnet > 0) {
            BallEntity b = ballNear(actor, 16);
            if (b != null && b.getController() != actor && !b.isHeld()) {
                LivingEntity c = b.getController();
                if (c != null && c.distanceTo(actor) < 8 && Athlete.of(c).evade <= 0) b.setController(null, false);
                if (b.isFree()) {
                    Vec3 to = actor.position().add(face(actor).scale(0.8)).subtract(b.position());
                    double d = to.length();
                    if (d < 1.0) {
                        b.setController(actor, false);
                        a.magnet = 0;
                    } else {
                        b.setDeltaMovement(to.normalize().scale(Math.min(0.85, 0.25 + d * 0.06)).add(0, d > 4 ? 0.06 : 0, 0));
                        if (a.magnet % 2 == 0) particles(sl, ParticleTypes.ENCHANT, b.center(), 6, 0.3, 0.3);
                    }
                }
            }
        }
    }

    // ================================================================ savunma yardimcilari
    private static void tackle(LivingEntity actor, Athlete a, double reach, double base) {
        Vec3 f = face(actor);
        for (LivingEntity o : opponents(actor, reach, 0.1)) {
            BallEntity b = controlled(o);
            if (b == null || b.isHeld()) continue;
            Athlete oa = Athlete.of(o);
            double chance = base + defend(actor) * 0.003 - dribble(o) * 0.0025;
            if (oa.evade > 0) chance -= 0.45;
            if (b.airDribbleTicks > 0) chance -= 0.3;
            Vec3 of = face(o);
            if (of.dot(f) > 0.5) chance -= 0.15; // arkadan
            if (actor.getRandom().nextDouble() < chance) {
                b.setController(actor, false);
                react(o, Move.STUMBLE, 10);
                a.tackles++;
                a.addEnergy(6);
                sound(actor, ModSounds.TACKLE.get(), 1f, 1.1f);
            } else {
                react(actor, Move.STUMBLE, 14);
                Athlete.of(o).addEnergy(4);
                if (o instanceof ServerPlayer sp)
                    sp.displayClientMessage(Component.translatable("msg.rabonaarena.dodged").withStyle(ChatFormatting.AQUA), true);
            }
            return;
        }
        BallEntity free = ballNear(actor, reach);
        if (free != null && free.isFree() && !free.isImmune(actor)) {
            free.setController(actor, false);
        }
    }

    private static void slideHit(LivingEntity actor, Athlete a) {
        Vec3 f = face(actor);
        BallEntity b = ballNear(actor, 2.2);
        if (b != null && !b.isHeld() && b.getController() != actor && horiz(b.center(), actor.position().add(f.scale(0.5))) < 1.2
                && b.getY() - actor.getY() < 0.9 && !b.isImmune(actor)) {
            LivingEntity victim = b.getController();
            if (victim == null || Athlete.of(victim).evade <= 0 || actor.getRandom().nextFloat() < 0.25) {
                b.kick(actor, f.scale(0.5).add(right(f).scale((actor.getRandom().nextDouble() - 0.5) * 0.4)).add(0, 0.12, 0), new Vector3f(), 0, 0);
                if (victim != null) {
                    react(victim, Move.FALL, 26);
                    a.tackles++;
                    a.addEnergy(8);
                }
                sound(actor, ModSounds.TACKLE.get(), 1.2f, 0.9f);
            }
        }
        for (LivingEntity o : opponents(actor, 1.2, 0.2)) {
            if (Athlete.of(o).stun <= 0 && Athlete.of(o).evade <= 0) react(o, Move.FALL, 22);
        }
    }

    private static void blockBall(LivingEntity actor) {
        BallEntity b = ballNear(actor, 2.5);
        if (b == null || !b.isFree() || b.isImmune(actor)) return;
        Vec3 c = actor.position().add(0, 1.0, 0);
        Vec3 bc = b.center();
        if (Math.abs(bc.y - c.y) < 1.4 && horiz(bc, c) < 1.15 && b.getDeltaMovement().length() > 0.35) {
            Vec3 v = b.getDeltaMovement();
            b.kick(actor, new Vec3(-v.x * 0.3, Math.abs(v.y) * 0.3 + 0.2, -v.z * 0.3), new Vector3f(), 0, 0);
            tell(actor, Component.translatable("msg.rabonaarena.blocked").withStyle(ChatFormatting.GREEN));
        }
    }

    private static void diveCatch(LivingEntity gk, Athlete a, int s) {
        BallEntity b = ballNear(gk, 3.5);
        if (b == null || !b.isFree() || b.isImmune(gk)) return;
        Vec3 f = face(gk);
        Vec3 hands = gk.position().add(right(f).scale(0.8 * s)).add(0, 0.6, 0);
        Vec3 body = gk.position().add(0, 0.7, 0);
        Vec3 bc = b.center();
        if (bc.distanceTo(hands) > 1.5 && bc.distanceTo(body) > 1.15) return;
        catchOrParry(gk, a, b);
    }

    public static void catchOrParry(LivingEntity gk, Athlete a, BallEntity b) {
        Vec3 v = b.getDeltaMovement();
        double sp = v.length();
        double skill = gk instanceof FootballerEntity fb ? fb.getSkill() : 78;
        double chance = 0.45 + skill * 0.005 - sp * 0.22;
        int fx = b.getEffect();
        if (fx == BallEntity.FX_FIRE || fx == BallEntity.FX_THUNDER) chance = -1;
        if (fx == BallEntity.FX_KNUCKLE || fx == BallEntity.FX_TORNADO) chance -= 0.25;
        a.saves++;
        a.addEnergy(10);
        if (gk.getRandom().nextDouble() < chance) {
            b.setController(gk, true);
            b.setEffect(BallEntity.FX_NONE, 0);
            sound(gk, ModSounds.CATCH.get(), 1.2f, 1f);
            Net.toTrackingAndSelf(gk, new S2C.Anim(gk.getId(), Move.CATCH.ordinal(), 0));
        } else {
            Vec3 out = new Vec3(-v.x * 0.35, 0.32 + gk.getRandom().nextDouble() * 0.2, -v.z * 0.35)
                    .add(face(gk).scale(0.15)).add(right(face(gk)).scale((gk.getRandom().nextDouble() - 0.5) * 0.6));
            b.kick(gk, out, new Vector3f(), 0, 0);
            sound(gk, ModSounds.CATCH.get(), 1.4f, 0.7f);
            if (fx == BallEntity.FX_FIRE) {
                push(gk, flat(v).scale(0.9).add(0, 0.35, 0));
                react(gk, Move.FALL, 30);
                particles((ServerLevel) gk.level(), ParticleTypes.FLAME, gk.position().add(0, 1, 0), 30, 0.5, 0.1);
            }
        }
    }

    /** Calim basarisi: ondeki rakipler dengesini kaybeder. */
    private static void skillBeat(LivingEntity actor, Athlete a, double radius, double chance, int stun) {
        boolean beat = false;
        for (LivingEntity o : opponents(actor, radius, 0.1)) {
            double c = chance + dribble(actor) * 0.003 - defend(o) * 0.003;
            if (actor.getRandom().nextDouble() < c) {
                react(o, Move.STUMBLE, stun);
                beat = true;
            }
        }
        a.skills++;
        a.addEnergy(beat ? 9 : 3);
        sound(actor, ModSounds.SKILL.get(), 0.9f, 1f);
        if (beat) {
            particles((ServerLevel) actor.level(), ParticleTypes.CRIT, actor.position().add(0, 1, 0), 12, 0.5, 0.2);
            if (actor instanceof ServerPlayer sp)
                sp.displayClientMessage(Component.translatable("msg.rabonaarena.beat").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD), true);
        }
    }

    private static void affectOpponents(LivingEntity actor, double r, double cone, double chance, int stun) {
        for (LivingEntity o : opponents(actor, r, cone)) {
            if (actor.getRandom().nextDouble() < chance) react(o, Move.STUMBLE, stun);
        }
    }

    public static List<LivingEntity> opponents(LivingEntity actor, double r, double cone) {
        Team t = Match.teamOf(actor);
        Vec3 f = face(actor);
        AABB box = actor.getBoundingBox().inflate(r, 1.5, r);
        return actor.level().getEntitiesOfClass(LivingEntity.class, box, o -> {
            if (o == actor || !o.isAlive() || o.isSpectator()) return false;
            if (!(o instanceof Player || o instanceof FootballerEntity)) return false;
            Team ot = Match.teamOf(o);
            if (t.playing() && ot == t) return false;
            Vec3 to = o.position().subtract(actor.position());
            if (to.length() > r) return false;
            return cone < -1 || flat(to).dot(f) >= cone;
        });
    }

    static double defend(LivingEntity e) { return e instanceof FootballerEntity f ? f.defending : 75; }

    static double dribble(LivingEntity e) { return e instanceof FootballerEntity f ? f.dribbling : 80; }

    // ================================================================ dusuk seviye
    public static void push(LivingEntity e, Vec3 v) {
        e.setDeltaMovement(v);
        e.hurtMarked = true;
        e.hasImpulse = true;
    }

    private static void turnAround(LivingEntity e) {
        float yaw = e.getYRot() + 180;
        if (e instanceof ServerPlayer sp) {
            sp.connection.teleport(sp.getX(), sp.getY(), sp.getZ(), yaw, sp.getXRot());
        } else {
            e.setYRot(yaw);
            e.setYHeadRot(yaw);
            e.yBodyRot = yaw;
        }
    }

    static void sound(LivingEntity e, SoundEvent s, float vol, float pitch) {
        e.level().playSound(null, e.getX(), e.getY(), e.getZ(), s, SoundSource.PLAYERS, vol, pitch);
    }

    static void particles(ServerLevel sl, ParticleOptions p, Vec3 at, int n, double spread, double speed) {
        sl.sendParticles(p, at.x, at.y, at.z, n, spread, spread * 0.6, spread, speed);
    }

    static void tell(LivingEntity e, Component c) {
        if (e instanceof ServerPlayer sp) sp.displayClientMessage(c, true);
    }
}
