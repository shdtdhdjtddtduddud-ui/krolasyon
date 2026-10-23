package com.krolasyon.mythblades.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * Alev hilali: kombonun son vuruşunda (ya da karşı koymadan sonra, dev boyutta) kılıçtan kopan ateş yayı. Düz uçar,
 * değdiği her düşmanı bir kez keser ve tutuşturur, duvara çarpınca alev saçarak dağılır.
 */
public final class EmberWaveEntity extends Projectile {
    private static final EntityDataAccessor<Boolean> BIG = SynchedEntityData.defineId(EmberWaveEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> TILT = SynchedEntityData.defineId(EmberWaveEntity.class, EntityDataSerializers.FLOAT);
    private final Set<Integer> hit = new HashSet<>();

    public EmberWaveEntity(EntityType<? extends EmberWaveEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
    }

    public void setup(boolean big, float tilt) {
        entityData.set(BIG, big);
        entityData.set(TILT, tilt);
    }

    public boolean big() {
        return entityData.get(BIG);
    }

    public float tilt() {
        return entityData.get(TILT);
    }

    public int life() {
        return big() ? 18 : 13;
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(BIG, false);
        entityData.define(TILT, 0F);
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 v = getDeltaMovement();
        Vec3 from = position(), to = from.add(v);
        float width = big() ? 3.4F : 2.0F;
        if (!level().isClientSide) {
            HitResult bh = level().clip(new ClipContext(from.add(0, 0.4, 0), to.add(0, 0.4, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (bh.getType() != HitResult.Type.MISS) {
                burst();
                discard();
                return;
            }
            Entity owner = getOwner();
            AABB box = getBoundingBox().expandTowards(v).inflate(width, big() ? 1.4 : 0.9, width);
            for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, box, x -> x != owner && x.isAlive() && !hit.contains(x.getId())
                    && !(x instanceof Player))) {
                if (owner instanceof LivingEntity lo && e.isAlliedTo(lo)) continue;
                hit.add(e.getId());
                e.hurt(damageSources().indirectMagic(this, owner), big() ? 16F : 8F);
                e.setSecondsOnFire(big() ? 8 : 5);
                e.knockback(big() ? 1.0 : 0.5, -v.x, -v.z);
                ((ServerLevel) level()).sendParticles(ParticleTypes.FLAME, e.getX(), e.getY() + e.getBbHeight() * 0.6, e.getZ(), 20, 0.3, 0.4, 0.3, 0.08);
                ((ServerLevel) level()).sendParticles(ParticleTypes.LAVA, e.getX(), e.getY() + e.getBbHeight() * 0.6, e.getZ(), 4, 0.3, 0.3, 0.3, 0);
                level().playSound(null, e.blockPosition(), SoundEvents.BLAZE_HURT, SoundSource.PLAYERS, 0.6F, 1.6F);
            }
            if (tickCount > life()) {
                burst();
                discard();
                return;
            }
        } else {
            // uçarken yaydan kopan közler ve duman
            Vec3 side = new Vec3(-v.z, 0, v.x).normalize();
            for (int i = 0; i < (big() ? 6 : 3); i++) {
                double s = (random.nextDouble() - 0.5) * 2 * width;
                level().addParticle(random.nextInt(3) == 0 ? ParticleTypes.SMALL_FLAME : ParticleTypes.FLAME, getX() + side.x * s, getY() + 0.4 + random.nextGaussian() * 0.2,
                        getZ() + side.z * s, v.x * 0.1, 0.03, v.z * 0.1);
            }
            if (random.nextInt(2) == 0) level().addParticle(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.5, getZ(), 0, 0.03, 0);
        }
        setPos(to.x, to.y, to.z);
    }

    private void burst() {
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.4, getZ(), big() ? 60 : 30, 0.8, 0.4, 0.8, 0.12);
            sl.sendParticles(ParticleTypes.LAVA, getX(), getY() + 0.4, getZ(), 8, 0.6, 0.3, 0.6, 0);
            sl.playSound(null, blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.8F, 0.7F);
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag t) {}

    @Override
    protected void readAdditionalSaveData(CompoundTag t) {}

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) {
        return d < 128 * 128;
    }
}
