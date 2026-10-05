package com.sololeveling.entity;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Generic magic projectile: kinds ice, shadow, fire, poison, beam. */
public class MagicBoltEntity extends Projectile {
    private static final EntityDataAccessor<String> KIND = SynchedEntityData.defineId(MagicBoltEntity.class, EntityDataSerializers.STRING);
    private float damage = 6;
    private int life = 0;
    private boolean friendlyToShadows = true;

    public MagicBoltEntity(EntityType<? extends MagicBoltEntity> type, Level level) {
        super(type, level);
        this.noPhysics = false;
    }

    public void setup(String kind, float damage) {
        this.entityData.set(KIND, kind);
        this.damage = damage;
    }

    public String kind() { return this.entityData.get(KIND); }

    @Override
    protected void defineSynchedData() { this.entityData.define(KIND, "shadow"); }

    @Override
    public void tick() {
        super.tick();
        life++;
        if (life > 60) { discard(); return; }
        Vec3 mov = getDeltaMovement();
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS && !net.minecraftforge.event.ForgeEventFactory.onProjectileImpact(this, hit)) onHit(hit);
        setPos(getX() + mov.x, getY() + mov.y, getZ() + mov.z);
        if (!isNoGravity() && kind().equals("poison")) setDeltaMovement(mov.add(0, -0.02, 0));
        if (level().isClientSide) {
            ParticleOptions p = switch (kind()) {
                case "ice" -> ParticleTypes.SNOWFLAKE;
                case "fire" -> ParticleTypes.FLAME;
                case "poison" -> ParticleTypes.ITEM_SLIME;
                case "beam" -> ParticleTypes.CRIT;
                default -> ParticleTypes.SOUL_FIRE_FLAME;
            };
            for (int i = 0; i < 2; i++)
                level().addParticle(p, getX() - mov.x * i * 0.5, getY() - mov.y * i * 0.5, getZ() - mov.z * i * 0.5, 0, 0, 0);
        }
    }

    @Override
    protected boolean canHitEntity(Entity e) {
        if (!super.canHitEntity(e)) return false;
        Entity o = getOwner();
        if (o == null) return true;
        if (e == o) return false;
        if (o instanceof net.minecraft.world.entity.player.Player && e instanceof ShadowEntity s && s.isOwnedBy(o)) return false;
        if (o instanceof ShadowEntity && (e instanceof ShadowEntity || e instanceof net.minecraft.world.entity.player.Player)) return false;
        if (o instanceof SLMonster && e instanceof SLMonster) return false;
        return !(e instanceof NpcEntity);
    }

    @Override
    protected void onHitEntity(EntityHitResult r) {
        if (level().isClientSide) return;
        Entity t = r.getEntity();
        Entity o = getOwner();
        DamageSource src = damageSources().indirectMagic(this, o);
        t.hurt(src, damage);
        if (t instanceof LivingEntity le) {
            switch (kind()) {
                case "ice" -> le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                case "poison" -> le.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
                case "fire" -> le.setSecondsOnFire(5);
                case "shadow" -> le.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
                default -> { }
            }
        }
        burst();
        discard();
    }

    @Override
    protected void onHitBlock(net.minecraft.world.phys.BlockHitResult r) {
        super.onHitBlock(r);
        if (!level().isClientSide) { burst(); discard(); }
    }

    private void burst() {
        if (level() instanceof ServerLevel sl) {
            ParticleOptions p = switch (kind()) {
                case "ice" -> ParticleTypes.SNOWFLAKE;
                case "fire" -> ParticleTypes.LAVA;
                case "poison" -> ParticleTypes.SPORE_BLOSSOM_AIR;
                case "beam" -> ParticleTypes.CRIT;
                default -> ParticleTypes.SOUL;
            };
            sl.sendParticles(p, getX(), getY(), getZ(), 14, 0.3, 0.3, 0.3, 0.05);
            sl.playSound(null, blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 0.6F, 1.4F);
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        t.putString("kind", kind()); t.putFloat("dmg", damage);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        setup(t.getString("kind"), t.getFloat("dmg"));
    }

    @Override public boolean isNoGravity() { return !kind().equals("poison"); }
}
