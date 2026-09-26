package com.krolasyon.bosses.entity;

import com.krolasyon.bosses.registry.ModSounds;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Blazing fireball launched from the hound's tail tips; bursts into a crimson explosion (no block damage). */
public class HellfireBallEntity extends AbstractHurtingProjectile {
    private static final EntityDataAccessor<Boolean> DATA_EMPOWERED = SynchedEntityData.defineId(HellfireBallEntity.class, EntityDataSerializers.BOOLEAN);

    public HellfireBallEntity(EntityType<? extends HellfireBallEntity> type, Level level) {
        super(type, level);
    }

    public void setup(Entity owner, Vec3 from, Vec3 dir, boolean empowered) {
        this.setOwner(owner);
        this.moveTo(from.x, from.y, from.z, this.getYRot(), this.getXRot());
        Vec3 d = dir.normalize();
        this.setDeltaMovement(d.scale(0.7).add(0, 0.12, 0));
        this.xPower = d.x * 0.07;
        this.yPower = d.y * 0.07;
        this.zPower = d.z * 0.07;
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
            for (int i = 0; i < 4; i++) {
                this.level().addParticle(i % 2 == 0 ? ParticleTypes.FLAME : BossEntity.dust(i == 1 ? CrimsonHoundEntity.RED : CrimsonHoundEntity.EMBER, 1.6F),
                        getX() + (random.nextDouble() - 0.5) * 0.5, getY() + 0.4 + (random.nextDouble() - 0.5) * 0.5, getZ() + (random.nextDouble() - 0.5) * 0.5, 0, 0.01, 0);
            }
            if (random.nextInt(2) == 0) this.level().addParticle(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.4, getZ(), 0, 0.02, 0);
        } else if (this.tickCount > 120) {
            this.discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity e) {
        if (!super.canHitEntity(e) || e instanceof HellfireBallEntity) return false;
        return !(this.getOwner() instanceof BossEntity boss) || boss.isHostileTo(e);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (this.level().isClientSide()) return;
        ServerLevel sl = (ServerLevel) this.level();
        boolean emp = this.entityData.get(DATA_EMPOWERED);
        double r = emp ? 3.2 : 2.7;
        sl.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.3, getZ(), 2, 0.3, 0.3, 0.3, 0);
        sl.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.3, getZ(), 45, 0.2, 0.2, 0.2, 0.28);
        sl.sendParticles(BossEntity.dust(CrimsonHoundEntity.RED, 2.4F), getX(), getY() + 0.3, getZ(), 30, r * 0.4, 0.4, r * 0.4, 0);
        sl.sendParticles(ParticleTypes.LAVA, getX(), getY() + 0.3, getZ(), 8, 0.4, 0.2, 0.4, 0);
        this.level().playSound(null, getX(), getY(), getZ(), ModSounds.FIREBALL_HIT.get(), SoundSource.HOSTILE, 2.5F, 0.9F + random.nextFloat() * 0.2F);
        Entity owner = this.getOwner();
        for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class, new AABB(position(), position()).inflate(r))) {
            if (owner instanceof BossEntity boss ? !boss.isHostileTo(e) : e == owner) continue;
            if (e.distanceToSqr(this) > r * r) continue;
            float dmg = (emp ? 11F : 9F) * (float) (1.0 - 0.4 * Math.sqrt(e.distanceToSqr(this)) / r);
            if (e.hurt(this.damageSources().explosion(this, owner), dmg)) {
                Vec3 k = e.position().subtract(position()).multiply(1, 0, 1);
                if (k.lengthSqr() > 1.0E-4) k = k.normalize().scale(0.8);
                e.setDeltaMovement(e.getDeltaMovement().add(k.x, 0.35, k.z));
                e.hurtMarked = true;
                e.setSecondsOnFire(5);
            }
        }
        this.discard();
    }

    @Override
    protected ParticleOptions getTrailParticle() { return ParticleTypes.FLAME; }

    @Override
    protected boolean shouldBurn() { return false; }

    @Override
    protected float getInertia() { return 0.97F; }
}
