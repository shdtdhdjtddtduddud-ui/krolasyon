package com.krolasyon.bosses.entity;

import com.krolasyon.bosses.registry.ModSounds;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
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

/** Pink seal bolt fired by watcher orbs. */
public class SealBoltEntity extends AbstractHurtingProjectile {
    public SealBoltEntity(EntityType<? extends SealBoltEntity> type, Level level) {
        super(type, level);
    }

    public void setup(Entity owner, Vec3 from, Vec3 dir) {
        this.setOwner(owner);
        this.moveTo(from.x, from.y, from.z, this.getYRot(), this.getXRot());
        Vec3 d = dir.normalize();
        this.setDeltaMovement(d.scale(0.55));
        this.xPower = d.x * 0.06;
        this.yPower = d.y * 0.06;
        this.zPower = d.z * 0.06;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            for (int i = 0; i < 3; i++) {
                this.level().addParticle(BossEntity.dust(i == 0 ? SealWardenEntity.PINK_LIGHT : SealWardenEntity.PINK, 1.1F),
                        getX() + (random.nextDouble() - 0.5) * 0.25, getY() + 0.2 + (random.nextDouble() - 0.5) * 0.25, getZ() + (random.nextDouble() - 0.5) * 0.25, 0, 0, 0);
            }
            if (random.nextInt(3) == 0) this.level().addParticle(ParticleTypes.END_ROD, getX(), getY() + 0.2, getZ(), 0, 0, 0);
        } else if (this.tickCount > 100) {
            this.discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity e) {
        if (!super.canHitEntity(e) || e instanceof WatcherOrbEntity || e instanceof SealBoltEntity) return false;
        return !(this.getOwner() instanceof BossEntity boss) || boss.isHostileTo(e);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level().isClientSide()) return;
        Entity e = result.getEntity();
        Entity owner = this.getOwner();
        if (e.hurt(this.damageSources().indirectMagic(this, owner), 6.0F) && e instanceof LivingEntity le) {
            if (!(le instanceof BossEntity)) le.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0));
            le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide()) {
            ServerLevel sl = (ServerLevel) this.level();
            sl.sendParticles(BossEntity.dust(SealWardenEntity.PINK_LIGHT, 1.6F), getX(), getY() + 0.2, getZ(), 20, 0.25, 0.25, 0.25, 0);
            sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.2, getZ(), 8, 0.05, 0.05, 0.05, 0.12);
            this.level().playSound(null, getX(), getY(), getZ(), ModSounds.BOLT_HIT.get(), SoundSource.HOSTILE, 1.2F, 1.0F + random.nextFloat() * 0.3F);
            this.discard();
        }
    }

    @Override
    protected ParticleOptions getTrailParticle() { return BossEntity.dust(SealWardenEntity.MAGENTA, 1.0F); }

    @Override
    protected boolean shouldBurn() { return false; }

    @Override
    protected float getInertia() { return 0.98F; }

    @Override
    public boolean isOnFire() { return false; }
}
