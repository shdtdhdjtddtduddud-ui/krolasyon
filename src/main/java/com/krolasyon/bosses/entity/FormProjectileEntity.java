package com.krolasyon.bosses.entity;

import com.krolasyon.bosses.morph.MorphServer;
import com.krolasyon.bosses.registry.ModEntities;
import com.krolasyon.bosses.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/** All thrown / summoned projectiles of the fire forms. Moves itself, sweeps for targets and explodes by kind. */
public class FormProjectileEntity extends AbilityEntity {
    public static final int METEOR = 0, FIREBALL = 1, EMBER = 2, CRESCENT = 3, SPEAR = 4, DRAGON = 5, FALLING_SPEAR = 6;
    private static final EntityDataAccessor<Integer> DATA_KIND = SynchedEntityData.defineId(FormProjectileEntity.class, EntityDataSerializers.INT);
    private float damage = 6F;
    private int homing = -1;
    private final Set<Integer> hit = new HashSet<>();
    /** client-side trail for the dragon serpent */
    public final Deque<Vec3> trail = new ArrayDeque<>();

    public FormProjectileEntity(EntityType<? extends FormProjectileEntity> type, Level level) {
        super(type, level);
    }

    public static FormProjectileEntity shoot(Player owner, int kind, Vec3 from, Vec3 vel, float damage) {
        FormProjectileEntity e = new FormProjectileEntity(ModEntities.FORM_PROJECTILE.get(), owner.level());
        e.setOwner(owner);
        e.damage = damage;
        e.entityData.set(DATA_KIND, kind);
        e.moveTo(from.x, from.y, from.z, 0F, 0F);
        e.setDeltaMovement(vel);
        e.faceMotion();
        owner.level().addFreshEntity(e);
        return e;
    }

    public FormProjectileEntity homing(LivingEntity target) {
        this.homing = target == null ? -1 : target.getId();
        return this;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_KIND, FIREBALL);
    }

    public int getKind() { return this.entityData.get(DATA_KIND); }

    private int life() {
        return switch (getKind()) { case CRESCENT -> 22; case DRAGON -> 50; case METEOR -> 80; default -> 60; };
    }

    private void faceMotion() {
        Vec3 v = getDeltaMovement();
        double h = v.horizontalDistance();
        this.setYRot((float) (Mth.atan2(v.x, v.z) * (180F / Math.PI)));
        this.setXRot((float) (Mth.atan2(v.y, h) * (180F / Math.PI)));
        this.yRotO = getYRot();
        this.xRotO = getXRot();
    }

    @Override
    public void tick() {
        super.tick();
        int kind = getKind();
        Vec3 v = getDeltaMovement();
        if (kind == DRAGON) {
            // serpentine sway sideways
            Vec3 side = new Vec3(-v.z, 0, v.x).normalize();
            v = v.add(side.scale(Math.cos(tickCount * 0.35) * 0.08));
        }
        if (homing >= 0 && !level().isClientSide()) {
            Entity t = level().getEntity(homing);
            if (t instanceof LivingEntity le && le.isAlive()) {
                Vec3 to = le.position().add(0, le.getBbHeight() * 0.5, 0).subtract(position()).normalize().scale(v.length());
                v = v.scale(0.8).add(to.scale(0.2));
            }
        }
        Vec3 from = position();
        Vec3 to = from.add(v);
        if (level().isClientSide()) {
            clientFx(from, kind);
            setPos(to.x, to.y, to.z);
            setDeltaMovement(v);
            faceMotion();
            return;
        }
        BlockHitResult bh = level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        boolean blocked = bh.getType() != HitResult.Type.MISS;
        Vec3 end = blocked ? bh.getLocation() : to;
        Player o = owner();
        float rad = switch (kind) { case METEOR -> 1.6F; case DRAGON -> 1.8F; case CRESCENT -> 1.5F; case SPEAR, FALLING_SPEAR -> 0.7F; default -> 0.6F; };
        AABB sweep = new AABB(from, end).inflate(rad);
        boolean pierce = kind == CRESCENT || kind == DRAGON;
        if (o != null) {
            for (LivingEntity e : targets(sweep)) {
                if (!hit.add(e.getId())) continue;
                if (pierce) {
                    if (MorphServer.burn(o, e, damage, kind == DRAGON ? 1.2 : 0.4, kind == DRAGON ? 0.6 : 0.2, 6))
                        ((ServerLevel) level()).sendParticles(ParticleTypes.FLAME, e.getX(), e.getY(0.5), e.getZ(), 12, 0.3, 0.4, 0.3, 0.05);
                } else {
                    impact(e.position().add(0, e.getBbHeight() * 0.5, 0), o);
                    return;
                }
            }
        }
        if (blocked) {
            if (o != null && !pierce) impact(end, o);
            else discard();
            return;
        }
        setPos(to.x, to.y, to.z);
        setDeltaMovement(v);
        faceMotion();
        if (tickCount > life()) {
            if (o != null && kind == METEOR) impact(position(), o);
            else discard();
        }
    }

    private void clientFx(Vec3 p, int kind) {
        switch (kind) {
            case METEOR -> {
                for (int i = 0; i < 6; i++)
                    level().addParticle(i % 3 == 0 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.FLAME, p.x + random.nextGaussian() * 0.8, p.y + random.nextGaussian() * 0.8, p.z + random.nextGaussian() * 0.8, 0, 0.1, 0);
                level().addParticle(ParticleTypes.LAVA, p.x, p.y, p.z, 0, 0, 0);
            }
            case DRAGON -> {
                trail.addFirst(p);
                while (trail.size() > 14) trail.removeLast();
                for (int i = 0; i < 4; i++)
                    level().addParticle(ParticleTypes.FLAME, p.x + random.nextGaussian() * 0.5, p.y + random.nextGaussian() * 0.5, p.z + random.nextGaussian() * 0.5, 0, 0.05, 0);
            }
            case CRESCENT -> level().addParticle(ParticleTypes.FLAME, p.x + random.nextGaussian() * 0.6, p.y, p.z + random.nextGaussian() * 0.6, 0, 0.02, 0);
            case SPEAR, FALLING_SPEAR -> {
                level().addParticle(ParticleTypes.FLAME, p.x, p.y, p.z, 0, 0.02, 0);
                level().addParticle(ParticleTypes.SMOKE, p.x, p.y, p.z, 0, 0.02, 0);
            }
            default -> {
                level().addParticle(ParticleTypes.FLAME, p.x + random.nextGaussian() * 0.15, p.y + random.nextGaussian() * 0.15, p.z + random.nextGaussian() * 0.15, 0, 0.02, 0);
                if (random.nextInt(2) == 0) level().addParticle(ParticleTypes.SMOKE, p.x, p.y, p.z, 0, 0.02, 0);
            }
        }
    }

    private void impact(Vec3 at, Player o) {
        ServerLevel sl = (ServerLevel) level();
        int kind = getKind();
        float radius = switch (kind) { case METEOR -> 5.5F; case SPEAR -> 3.0F; case FIREBALL -> 2.2F; case FALLING_SPEAR -> 2.0F; default -> 1.8F; };
        switch (kind) {
            case METEOR -> {
                MorphServer.sound(sl, at, ModSounds.FIRE_EXPLODE.get(), 2.6F, 0.8F);
                sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y + 0.5, at.z, 1, 0, 0, 0, 0);
                sl.sendParticles(ParticleTypes.LAVA, at.x, at.y + 0.5, at.z, 40, 2, 0.5, 2, 0);
                sl.sendParticles(ParticleTypes.FLAME, at.x, at.y + 1, at.z, 120, 2, 1.5, 2, 0.2);
                TsunamiWaveEntity.spawn(o, new Vec3(at.x, Math.floor(at.y + 0.01), at.z), TsunamiWaveEntity.FIRE, 11F, 8F);
                for (int i = 0; i < 8; i++) {
                    double a = i * Math.PI / 4;
                    TideGeyserEntity.spawn(o, TideGeyserEntity.FIRE, at.x + Math.cos(a) * 3.5, at.z + Math.sin(a) * 3.5, at.y, 2 + i, 7F, i == 0);
                }
            }
            case SPEAR -> {
                MorphServer.sound(sl, at, ModSounds.SPEAR_IMPACT.get(), 1.8F, 1.0F);
                sl.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 2, 0.4, 0.4, 0.4, 0);
                sl.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 50, 1, 1, 1, 0.15);
                for (int i = 0; i < 6; i++) {
                    double a = i * Math.PI / 3;
                    TideGeyserEntity.spawn(o, TideGeyserEntity.FIRE, at.x + Math.cos(a) * 2.2, at.z + Math.sin(a) * 2.2, at.y, 2 + i, 7F, i == 0);
                }
            }
            case FALLING_SPEAR -> {
                sl.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 20, 0.5, 0.5, 0.5, 0.1);
                sl.sendParticles(ParticleTypes.LAVA, at.x, at.y, at.z, 4, 0.3, 0.1, 0.3, 0);
                TideGeyserEntity.spawn(o, TideGeyserEntity.FIRE, at.x, at.z, at.y, 0, 5F, false);
            }
            default -> {
                MorphServer.sound(sl, at, ModSounds.FIREBALL_HIT.get(), kind == EMBER ? 0.6F : 1.2F, 0.9F + random.nextFloat() * 0.3F);
                sl.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, kind == EMBER ? 10 : 30, 0.5, 0.5, 0.5, 0.1);
                sl.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y, at.z, 4, 0.3, 0.3, 0.3, 0.02);
            }
        }
        for (LivingEntity e : targets(new AABB(at, at).inflate(radius))) {
            if (e.position().add(0, e.getBbHeight() * 0.5, 0).distanceTo(at) > radius + e.getBbWidth()) continue;
            MorphServer.burn(o, e, damage, kind == METEOR ? 1.4 : 0.6, kind == METEOR ? 0.9 : 0.35, 6);
        }
        discard();
    }
}
