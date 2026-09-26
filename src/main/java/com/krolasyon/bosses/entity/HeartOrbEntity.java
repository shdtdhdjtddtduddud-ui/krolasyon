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

import javax.annotation.Nullable;

/** Homing crimson heart fired from the Heartbreaker's chest sigil. */
public class HeartOrbEntity extends AbstractHurtingProjectile {
    @Nullable private LivingEntity target;
    private boolean empowered;

    public HeartOrbEntity(EntityType<? extends HeartOrbEntity> type, Level level) {
        super(type, level);
    }

    public void setup(Entity owner, @Nullable LivingEntity target, Vec3 from, Vec3 dir, boolean empowered) {
        this.setOwner(owner);
        this.target = target;
        this.empowered = empowered;
        this.moveTo(from.x, from.y, from.z, 0, 0);
        this.setDeltaMovement(dir.normalize().scale(0.55));
        this.xPower = this.yPower = this.zPower = 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.level().addParticle(BossEntity.dust(HeartDemonEntity.CRIMSON, 1.3F), getX(), getY() + 0.25, getZ(), 0, 0, 0);
            this.level().addParticle(BossEntity.dust(HeartDemonEntity.HEART_PINK, 0.9F), getX() + (random.nextDouble() - 0.5) * 0.3, getY() + 0.25 + (random.nextDouble() - 0.5) * 0.3, getZ() + (random.nextDouble() - 0.5) * 0.3, 0, 0, 0);
            if (random.nextInt(3) == 0) this.level().addParticle(ParticleTypes.HEART, getX(), getY() + 0.3, getZ(), 0, 0, 0);
            return;
        }
        if (target != null && target.isAlive() && this.tickCount > 6) {
            Vec3 want = target.getBoundingBox().getCenter().subtract(this.position()).normalize().scale(empowered ? 0.85 : 0.7);
            this.setDeltaMovement(this.getDeltaMovement().lerp(want, 0.14));
        }
        if (this.tickCount > 120) this.discard();
    }

    @Override
    protected boolean canHitEntity(Entity e) {
        if (!super.canHitEntity(e) || e instanceof HeartOrbEntity) return false;
        if (this.getOwner() instanceof net.minecraft.world.entity.player.Player p) return com.krolasyon.bosses.form.DemonForm.canHit(p, e);
        return !(this.getOwner() instanceof BossEntity boss) || boss.isHostileTo(e);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level().isClientSide()) return;
        Entity e = result.getEntity();
        e.invulnerableTime = 0;
        if (e.hurt(this.damageSources().indirectMagic(this, this.getOwner()), empowered ? 8F : 6F) && e instanceof LivingEntity le) {
            le.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
            le.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0));
            if (this.getOwner() instanceof net.minecraft.world.entity.player.Player p) p.heal(1.5F);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (this.level().isClientSide()) return;
        ServerLevel sl = (ServerLevel) this.level();
        sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, getX(), getY() + 0.3, getZ(), 6, 0.2, 0.2, 0.2, 0.15);
        sl.sendParticles(BossEntity.dust(HeartDemonEntity.CRIMSON, 1.8F), getX(), getY() + 0.3, getZ(), 18, 0.3, 0.3, 0.3, 0);
        sl.sendParticles(BossEntity.dust(HeartDemonEntity.HEART_PINK, 1.2F), getX(), getY() + 0.3, getZ(), 10, 0.1, 0.1, 0.1, 0.2);
        this.level().playSound(null, getX(), getY(), getZ(), ModSounds.HEART_HIT.get(), SoundSource.HOSTILE, 1.4F, 0.9F + random.nextFloat() * 0.3F);
        this.discard();
    }

    @Override
    protected ParticleOptions getTrailParticle() { return BossEntity.dust(HeartDemonEntity.CRIMSON, 0.8F); }

    @Override
    protected boolean shouldBurn() { return false; }

    @Override
    protected float getInertia() { return 1.0F; }

    @Override
    public boolean isOnFire() { return false; }
}
