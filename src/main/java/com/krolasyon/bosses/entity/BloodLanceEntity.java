package com.krolasyon.bosses.entity;

import com.krolasyon.bosses.registry.ModSounds;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** The Revenge's thrown blood lance: impales, then bursts into a crown of blood spikes. */
public class BloodLanceEntity extends AbstractHurtingProjectile {
    private static final EntityDataAccessor<Boolean> DATA_EMPOWERED = SynchedEntityData.defineId(BloodLanceEntity.class, EntityDataSerializers.BOOLEAN);

    public BloodLanceEntity(EntityType<? extends BloodLanceEntity> type, Level level) {
        super(type, level);
    }

    public void setup(Entity owner, Vec3 from, Vec3 dir, boolean empowered) {
        this.setOwner(owner);
        this.moveTo(from.x, from.y, from.z, 0, 0);
        Vec3 d = dir.normalize();
        this.setDeltaMovement(d.scale(1.5));
        this.xPower = d.x * 0.05;
        this.yPower = d.y * 0.05;
        this.zPower = d.z * 0.05;
        this.setYRot((float) (Math.atan2(d.x, d.z) * 180 / Math.PI));
        this.setXRot((float) (Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * 180 / Math.PI));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
        this.entityData.set(DATA_EMPOWERED, empowered);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_EMPOWERED, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            for (int i = 0; i < 2; i++) {
                this.level().addParticle(BossEntity.dust(i == 0 ? RevengeEntity.BLOOD : RevengeEntity.BLOOD_DARK, 1.2F),
                        getX() + (random.nextDouble() - 0.5) * 0.3, getY() + 0.25, getZ() + (random.nextDouble() - 0.5) * 0.3, 0, -0.05, 0);
            }
        } else if (this.tickCount > 100) {
            this.discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity e) {
        if (!super.canHitEntity(e) || e instanceof BloodLanceEntity) return false;
        return !(this.getOwner() instanceof BossEntity boss) || boss.isHostileTo(e);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level().isClientSide()) return;
        Entity e = result.getEntity();
        float dmg = this.entityData.get(DATA_EMPOWERED) ? 18F : 15F;
        if (e.hurt(this.damageSources().mobProjectile(this, this.getOwner() instanceof LivingEntity le ? le : null), dmg) && e instanceof LivingEntity le) {
            le.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 1));
            le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
            Vec3 k = this.getDeltaMovement().normalize().scale(1.2);
            le.setDeltaMovement(le.getDeltaMovement().add(k.x, 0.3, k.z));
            le.hurtMarked = true;
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (this.level().isClientSide()) return;
        ServerLevel sl = (ServerLevel) this.level();
        sl.sendParticles(BossEntity.dust(RevengeEntity.BLOOD, 2.4F), getX(), getY() + 0.3, getZ(), 40, 0.6, 0.4, 0.6, 0);
        sl.sendParticles(BossEntity.dust(RevengeEntity.BLOOD_LIGHT, 1.6F), getX(), getY() + 0.3, getZ(), 20, 0.2, 0.2, 0.2, 0.3);
        this.level().playSound(null, getX(), getY(), getZ(), ModSounds.LANCE_IMPACT.get(), SoundSource.HOSTILE, 2.5F, 0.9F + random.nextFloat() * 0.2F);
        if (this.getOwner() instanceof BossEntity boss) {
            EruptionEntity.spawn(boss, getX(), getZ(), getY() + 1, EruptionEntity.KIND_BLOOD, 1, 9F);
            for (int i = 0; i < 6; i++) {
                double a = i * Math.PI / 3;
                EruptionEntity.spawn(boss, getX() + Math.cos(a) * 2.2, getZ() + Math.sin(a) * 2.2, getY() + 1, EruptionEntity.KIND_BLOOD, 3, 7F);
            }
        }
        this.discard();
    }

    @Override
    protected ParticleOptions getTrailParticle() { return BossEntity.dust(RevengeEntity.BLOOD, 1.0F); }

    @Override
    protected boolean shouldBurn() { return false; }

    @Override
    protected float getInertia() { return 0.99F; }

    @Override
    public boolean isOnFire() { return false; }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource src, float amount) { return false; }
}
