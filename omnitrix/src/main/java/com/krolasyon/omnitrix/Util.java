package com.krolasyon.omnitrix;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/** Targeting, damage and scheduling helpers shared by all alien powers. */
public final class Util {
    private record Task(ServerLevel level, long due, Consumer<ServerLevel> run) {}

    private static final List<Task> TASKS = new ArrayList<>();

    private Util() {}

    public static void schedule(ServerLevel l, int delay, Consumer<ServerLevel> r) { TASKS.add(new Task(l, l.getGameTime() + delay, r)); }

    static void runTasks() {
        if (TASKS.isEmpty()) return;
        List<Task> run = new ArrayList<>();
        for (Iterator<Task> it = TASKS.iterator(); it.hasNext(); ) {
            Task t = it.next();
            if (t.level.getGameTime() >= t.due) { run.add(t); it.remove(); }
        }
        for (Task t : run) t.run.accept(t.level);
    }

    public static void hit(LivingEntity target, float amount, Entity direct, ServerPlayer attacker) {
        if (!target.isAlive() || amount <= 0) return;
        target.invulnerableTime = 0;
        target.hurt(ModRegistry.alienDamage(direct, attacker), amount);
    }

    public static boolean canHit(ServerPlayer p, Entity e, boolean strict) {
        if (!(e instanceof LivingEntity le) || !le.isAlive() || e == p || e instanceof ArmorStand || le.isSpectator()) return false;
        if (e instanceof OwnableEntity o && o.getOwnerUUID() != null && o.getOwnerUUID().equals(p.getUUID())) return false;
        if (p.isAlliedTo(e) || Powers.isPossessedBy(e, p)) return false;
        if (e instanceof Player pl) return p.server.isPvpAllowed() && !pl.isCreative() && p.canHarmPlayer(pl);
        if (!strict) return true;
        return e instanceof Enemy || (e instanceof Mob m && m.getTarget() == p);
    }

    public static List<LivingEntity> around(ServerPlayer p, Vec3 c, double r, boolean strict) {
        List<LivingEntity> list = p.level().getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r),
                e -> canHit(p, e, strict) && e.position().distanceTo(c) <= r + e.getBbWidth());
        list.sort(Comparator.comparingDouble(e -> e.distanceToSqr(c)));
        return list;
    }

    /** targets inside a cone in front of the player */
    public static List<LivingEntity> cone(ServerPlayer p, double range, double cosAngle, boolean strict) {
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : around(p, eye, range, strict)) {
            Vec3 to = e.position().add(0, e.getBbHeight() / 2, 0).subtract(eye);
            if (to.length() < 1.2 || to.normalize().dot(look) >= cosAngle) out.add(e);
        }
        return out;
    }

    public static LivingEntity aimEntity(ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        Vec3 end = eye.add(look.scale(range));
        BlockHitResult bh = p.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (bh.getType() != HitResult.Type.MISS) end = bh.getLocation();
        EntityHitResult eh = ProjectileUtil.getEntityHitResult(p, eye, end, p.getBoundingBox().expandTowards(look.scale(range)).inflate(1.5),
                e -> canHit(p, e, false), eye.distanceToSqr(end));
        if (eh != null && eh.getEntity() instanceof LivingEntity le) return le;
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (LivingEntity e : around(p, eye, range, true)) {
            Vec3 to = e.position().add(0, e.getBbHeight() / 2, 0).subtract(eye);
            double cos = to.normalize().dot(look);
            if (cos < 0.95 || !p.hasLineOfSight(e)) continue;
            double score = (1 - cos) * 100 + to.length() * 0.05;
            if (score < bestScore) { bestScore = score; best = e; }
        }
        return best;
    }

    public static Vec3 aimPoint(ServerPlayer p, double range) {
        LivingEntity e = aimEntity(p, range);
        if (e != null) return e.position();
        Vec3 eye = p.getEyePosition();
        Vec3 end = eye.add(p.getLookAngle().scale(range));
        BlockHitResult bh = p.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        return ground(p.serverLevel(), (bh.getType() == HitResult.Type.MISS ? end : bh.getLocation()).add(0, 0.5, 0));
    }

    public static Vec3 ground(ServerLevel l, Vec3 pos) {
        Vec3 top = pos.add(0, 3, 0);
        BlockHitResult bh = l.clip(new ClipContext(top, top.add(0, -27, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null));
        return bh.getType() == HitResult.Type.MISS ? pos : bh.getLocation();
    }

    public static void push(LivingEntity e, Vec3 from, double strength, double up) {
        Vec3 d = e.position().subtract(from);
        if (d.lengthSqr() < 1e-4) d = new Vec3(0, 0, 1);
        e.knockback(strength, -d.x, -d.z);
        if (up > 0) e.setDeltaMovement(e.getDeltaMovement().add(0, up, 0));
        e.hurtMarked = true;
    }

    public static void particles(ServerLevel l, ParticleOptions p, Vec3 c, int n, double spread, double speed) {
        l.sendParticles(p, c.x, c.y, c.z, n, spread, spread, spread, speed);
    }

    public static void play(ServerLevel l, Vec3 pos, SoundEvent s, float vol, float pitch) {
        l.playSound(null, BlockPos.containing(pos), s, SoundSource.PLAYERS, vol, pitch);
    }

    public static Vec3 hand(Player p) {
        float yaw = p.getYRot() * (float) Math.PI / 180F;
        Vec3 side = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw)).scale(0.4);
        return p.getEyePosition().add(p.getLookAngle().scale(0.8)).add(side).add(0, -0.35, 0);
    }

    public static Vec3 flat(Vec3 v) {
        Vec3 f = new Vec3(v.x, 0, v.z);
        return f.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : f.normalize();
    }
}
