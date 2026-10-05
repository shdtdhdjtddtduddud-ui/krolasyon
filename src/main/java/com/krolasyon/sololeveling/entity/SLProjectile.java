package com.krolasyon.sololeveling.entity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import org.joml.Vector3f;

/** All magic projectiles: thrown daggers, ice shards, fireballs, venom, lightning orbs, mana bolts. */
public class SLProjectile extends Projectile {
    public enum Kind {
        SHADOW_DAGGER(0.0F, 40, 0.6F), ICE_SHARD(0.01F, 60, 0.5F), FIREBALL(0.0F, 80, 0.8F), VENOM(0.03F, 50, 0.6F),
        LIGHTNING_ORB(0.0F, 60, 0.9F), MANA_BOLT(0.0F, 60, 0.6F), STONE(0.04F, 80, 1.0F), FLAME_SLASH(0.0F, 20, 1.6F);

        public final float gravity;
        public final int life;
        public final float size;

        Kind(float gravity, int life, float size) {
            this.gravity = gravity;
            this.life = life;
            this.size = size;
        }
    }

    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(SLProjectile.class, EntityDataSerializers.INT);
    public float damage = 6;
    public float explode;
    public int pierce;

    public SLProjectile(EntityType<? extends SLProjectile> type, Level level) { super(type, level); }

    @Override
    protected void defineSynchedData() { entityData.define(KIND, 0); }

    public Kind kind() { return Kind.values()[Math.max(0, Math.min(Kind.values().length - 1, entityData.get(KIND)))]; }

    public SLProjectile setKind(Kind k) {
        entityData.set(KIND, k.ordinal());
        return this;
    }

    /** Launches from the shooter's eyes towards a direction. */
    public static SLProjectile shoot(LivingEntity shooter, Kind kind, Vec3 dir, double speed, float damage) {
        SLProjectile p = com.krolasyon.sololeveling.registry.ModEntities.PROJECTILE.get().create(shooter.level());
        p.setKind(kind);
        p.setOwner(shooter);
        p.damage = damage;
        Vec3 eye = shooter.getEyePosition().add(dir.normalize().scale(shooter.getBbWidth() * 0.7));
        p.setPos(eye.x, eye.y - 0.2, eye.z);
        p.setDeltaMovement(dir.normalize().scale(speed));
        ProjectileUtil.rotateTowardsMovement(p, 1F);
        shooter.level().addFreshEntity(p);
        return p;
    }

    @Override
    public void tick() {
        super.tick();
        Kind k = kind();
        if (tickCount > k.life) {
            if (!level().isClientSide) {
                if (explode > 0) boom(position());
                discard();
            }
            return;
        }
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS && !net.minecraftforge.event.ForgeEventFactory.onProjectileImpact(this, hit)) onHit(hit);
        Vec3 v = getDeltaMovement();
        setPos(getX() + v.x, getY() + v.y, getZ() + v.z);
        setDeltaMovement(v.scale(0.99).add(0, -k.gravity, 0));
        ProjectileUtil.rotateTowardsMovement(this, 0.5F);
        if (level().isClientSide) trail(k);
    }

    private void trail(Kind k) {
        ParticleOptions p = switch (k) {
            case SHADOW_DAGGER -> new DustParticleOptions(new Vector3f(0.45F, 0.3F, 0.95F), 1F);
            case ICE_SHARD -> ParticleTypes.SNOWFLAKE;
            case FIREBALL, FLAME_SLASH -> ParticleTypes.FLAME;
            case VENOM -> new DustParticleOptions(new Vector3f(0.4F, 0.9F, 0.3F), 1.2F);
            case LIGHTNING_ORB -> ParticleTypes.ELECTRIC_SPARK;
            case MANA_BOLT -> new DustParticleOptions(new Vector3f(0.3F, 0.7F, 1F), 1.2F);
            case STONE -> ParticleTypes.ASH;
        };
        for (int i = 0; i < 2; i++)
            level().addParticle(p, getX() + (random.nextDouble() - 0.5) * 0.2, getY() + (random.nextDouble() - 0.5) * 0.2, getZ() + (random.nextDouble() - 0.5) * 0.2, 0, 0, 0);
    }

    @Override
    protected boolean canHitEntity(Entity e) {
        if (!super.canHitEntity(e)) return false;
        Entity o = getOwner();
        if (o == null) return true;
        if (e == o || e.isAlliedTo(o)) return false;
        if (o instanceof SLMonster && e instanceof SLMonster) return false;
        if (o instanceof Player && e instanceof ShadowEntity s && s.getOwner() == o) return false;
        if (o instanceof ShadowEntity && e instanceof ShadowEntity) return false;
        return true;
    }

    @Override
    protected void onHitEntity(EntityHitResult r) {
        if (level().isClientSide) return;
        Entity e = r.getEntity();
        Entity o = getOwner();
        e.hurt(o instanceof LivingEntity le ? damageSources().mobProjectile(this, le) : damageSources().magic(), damage);
        if (e instanceof LivingEntity le) {
            switch (kind()) {
                case ICE_SHARD -> le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                case FIREBALL, FLAME_SLASH -> le.setSecondsOnFire(4);
                case VENOM -> le.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
                case LIGHTNING_ORB -> le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 4));
                default -> {}
            }
        }
        if (pierce-- > 0) return;
        if (explode > 0) boom(position());
        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult r) {
        super.onHitBlock(r);
        if (level().isClientSide) return;
        if (explode > 0) boom(r.getLocation());
        discard();
    }

    private void boom(Vec3 at) {
        if (!(level() instanceof ServerLevel sl)) return;
        sl.sendParticles(kind() == Kind.LIGHTNING_ORB ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.EXPLOSION, at.x, at.y, at.z, 6, explode * 0.3, explode * 0.3, explode * 0.3, 0.1);
        Entity o = getOwner();
        for (LivingEntity le : level().getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(explode))) {
            if (!canHitEntity(le)) continue;
            le.hurt(o instanceof LivingEntity l ? damageSources().mobProjectile(this, l) : damageSources().magic(), damage * 0.6F);
        }
    }

    @Override
    public boolean isNoGravity() { return true; }

    @Override
    protected void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        t.putInt("Kind", entityData.get(KIND));
        t.putFloat("Damage", damage);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        entityData.set(KIND, t.getInt("Kind"));
        damage = t.getFloat("Damage");
    }
}
