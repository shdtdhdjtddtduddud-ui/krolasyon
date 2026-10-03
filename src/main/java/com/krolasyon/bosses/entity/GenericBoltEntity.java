package com.krolasyon.bosses.entity;

import com.krolasyon.bosses.registry.ModEntities;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

/** One projectile that serves every ranged attack of the realm; looks are driven by a colour and a particle style. */
public class GenericBoltEntity extends AbstractHurtingProjectile {
    public static final int ST_FIRE = 0, ST_SOUL = 1, ST_SMOKE = 2, ST_SPORE = 3, ST_VOID = 4, ST_BLOOD = 5, ST_BONE = 6, ST_LIGHT = 7;
    private static final EntityDataAccessor<Integer> DATA_COLOR = SynchedEntityData.defineId(GenericBoltEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_STYLE = SynchedEntityData.defineId(GenericBoltEntity.class, EntityDataSerializers.INT);

    private float damage = 5F;
    private float radius = 0F;
    private float gravity = 0F;
    private int life = 100;
    private String effect = "";
    private int effTicks, effLevel;
    private boolean pierce;

    public GenericBoltEntity(EntityType<? extends GenericBoltEntity> type, Level level) {
        super(type, level);
    }

    public static GenericBoltEntity create(Level level, Entity owner, Vec3 from, Vec3 dir, double speed, float damage, float radius, int color, int style) {
        GenericBoltEntity b = new GenericBoltEntity(ModEntities.BOLT.get(), level);
        b.setOwner(owner);
        b.moveTo(from.x, from.y, from.z, 0, 0);
        Vec3 d = dir.normalize();
        b.setDeltaMovement(d.scale(speed));
        b.xPower = 0;
        b.yPower = 0;
        b.zPower = 0;
        b.damage = damage;
        b.radius = radius;
        b.entityData.set(DATA_COLOR, color);
        b.entityData.set(DATA_STYLE, style);
        return b;
    }

    public GenericBoltEntity withEffect(String id, int ticks, int level) { this.effect = id; this.effTicks = ticks; this.effLevel = level; return this; }
    public GenericBoltEntity withGravity(float g) { this.gravity = g; return this; }
    public GenericBoltEntity withLife(int l) { this.life = l; return this; }
    public GenericBoltEntity piercing() { this.pierce = true; return this; }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_COLOR, 0xFF8030);
        this.entityData.define(DATA_STYLE, ST_FIRE);
    }

    public int color() { return this.entityData.get(DATA_COLOR); }
    public int style() { return this.entityData.get(DATA_STYLE); }

    @Override
    public void tick() {
        // plain ballistic flight (we do not want the hurting-projectile acceleration)
        if (gravity != 0) this.setDeltaMovement(this.getDeltaMovement().add(0, -gravity, 0));
        super.tick();
        if (this.level().isClientSide()) {
            trail();
        } else if (this.tickCount > life) {
            this.discard();
        }
    }

    private void trail() {
        int c = color();
        Level l = this.level();
        int st = style();
        for (int i = 0; i < 3; i++) {
            double ox = (random.nextDouble() - 0.5) * 0.3, oy = (random.nextDouble() - 0.5) * 0.3, oz = (random.nextDouble() - 0.5) * 0.3;
            l.addParticle(BossEntity.dust(c, 1.3F), getX() + ox, getY() + 0.2 + oy, getZ() + oz, 0, 0, 0);
        }
        ParticleOptions extra = switch (st) {
            case ST_FIRE -> ParticleTypes.FLAME;
            case ST_SOUL -> ParticleTypes.SOUL_FIRE_FLAME;
            case ST_SMOKE -> ParticleTypes.LARGE_SMOKE;
            case ST_SPORE -> ParticleTypes.SPORE_BLOSSOM_AIR;
            case ST_VOID -> ParticleTypes.PORTAL;
            case ST_BLOOD -> ParticleTypes.DRIPPING_LAVA;
            case ST_BONE -> ParticleTypes.ASH;
            default -> ParticleTypes.END_ROD;
        };
        l.addParticle(extra, getX(), getY() + 0.2, getZ(), (random.nextDouble() - 0.5) * 0.05, 0.02, (random.nextDouble() - 0.5) * 0.05);
    }

    @Override
    protected boolean canHitEntity(Entity e) {
        if (!super.canHitEntity(e) || e instanceof GenericBoltEntity) return false;
        Entity o = this.getOwner();
        return !(o instanceof BossEntity boss) || boss.isHostileTo(e);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (this.level().isClientSide()) return;
        ServerLevel sl = (ServerLevel) this.level();
        Entity owner = this.getOwner();
        int c = color();
        sl.sendParticles(BossEntity.dust(c, 2.0F), getX(), getY() + 0.2, getZ(), 20, 0.3, 0.3, 0.3, 0.05);
        sl.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.2, getZ(), 6, 0.2, 0.2, 0.2, 0.05);
        this.level().playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, radius > 0 ? 1.0F : 0.5F, 1.4F + random.nextFloat() * 0.3F);
        double r = Math.max(radius, 0.9);
        for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class, new AABB(position(), position()).inflate(r + 0.5))) {
            if (owner instanceof BossEntity boss ? !boss.isHostileTo(e) : e == owner) continue;
            if (radius <= 0 && result instanceof net.minecraft.world.phys.EntityHitResult ehr && ehr.getEntity() != e) continue;
            if (e.distanceToSqr(this) > (r + 0.8) * (r + 0.8)) continue;
            float d = damage * (radius > 0 ? (float) (1.0 - 0.35 * Math.sqrt(e.distanceToSqr(this)) / (r + 0.8)) : 1F);
            if (e.hurt(this.damageSources().indirectMagic(this, owner), d)) {
                if (radius > 0) {
                    Vec3 k = e.position().subtract(position()).multiply(1, 0, 1);
                    if (k.lengthSqr() > 1.0E-4) k = k.normalize().scale(0.5);
                    e.setDeltaMovement(e.getDeltaMovement().add(k.x, 0.25, k.z));
                    e.hurtMarked = true;
                }
                if (!effect.isEmpty()) {
                    MobEffect me = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(effect));
                    if (me != null) e.addEffect(new MobEffectInstance(me, effTicks, effLevel));
                }
                if (style() == ST_FIRE) e.setSecondsOnFire(4);
            }
        }
        if (radius > 0) {
            for (int i = 0; i < 24; i++) {
                double a = i * Math.PI * 2 / 24;
                sl.sendParticles(BossEntity.dust(c, 1.6F), getX(), getY() + 0.3, getZ(), 0, Math.cos(a), 0.05, Math.sin(a), 0.4 + radius * 0.1);
            }
        }
        if (!pierce || result.getType() == HitResult.Type.BLOCK) this.discard();
    }

    @Override
    protected ParticleOptions getTrailParticle() { return ParticleTypes.SMOKE; }

    @Override
    protected boolean shouldBurn() { return false; }

    @Override
    protected float getInertia() { return 1.0F; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
    }
}
