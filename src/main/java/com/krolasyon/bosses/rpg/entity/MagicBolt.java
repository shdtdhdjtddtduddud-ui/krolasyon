package com.krolasyon.bosses.rpg.entity;

import com.krolasyon.bosses.rpg.registry.RpgEntities;
import com.krolasyon.bosses.rpg.util.Combat;
import com.krolasyon.bosses.rpg.util.FX;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Generic glowing magic projectile used by monster abilities, player spells and sword skills.
 * Look (colour, size, trail) is synced; behaviour (damage, on-hit effect, homing, explosion) is server side only.
 */
public class MagicBolt extends Projectile {
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(MagicBolt.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> COLOR2 = SynchedEntityData.defineId(MagicBolt.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SIZE = SynchedEntityData.defineId(MagicBolt.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Byte> TRAIL = SynchedEntityData.defineId(MagicBolt.class, EntityDataSerializers.BYTE);

    /** trail styles */
    public static final byte FIRE = 0, ICE = 1, LIGHTNING = 2, EARTH = 3, WIND = 4, WATER = 5, LIGHT = 6, DARK = 7, NATURE = 8,
            ARCANE = 9, BLOOD = 10, SPIRIT = 11, POISON = 12, BONE = 13, SAND = 14, WEB = 15;

    public enum OnHit { NONE, BURN, FREEZE, SHOCK, POISON, ACID, WITHER, NAUSEA, WEB, HOLY, LIFESTEAL, KNOCK, BLIND, SLOW, ROOT, BLEED }

    public float damage = 4;
    public OnHit onHit = OnHit.NONE;
    public float explode = 0;
    public float gravity = 0;
    public int life = 80;
    public int pierce = 0;
    public float homing = 0;
    public int zapInterval = 0;
    @Nullable public LivingEntity homingTarget;
    private final it.unimi.dsi.fastutil.ints.IntOpenHashSet pierced = new it.unimi.dsi.fastutil.ints.IntOpenHashSet();

    public MagicBolt(EntityType<? extends MagicBolt> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static MagicBolt create(Level level, LivingEntity owner, int color, int color2, float size, byte trail) {
        MagicBolt b = new MagicBolt(RpgEntities.MAGIC_BOLT.get(), level);
        b.setOwner(owner);
        b.entityData.set(COLOR, color);
        b.entityData.set(COLOR2, color2);
        b.entityData.set(SIZE, size);
        b.entityData.set(TRAIL, trail);
        Vec3 eye = owner.getEyePosition().subtract(0, 0.25, 0);
        b.setPos(eye.x, eye.y, eye.z);
        return b;
    }

    public MagicBolt aim(Vec3 dir, double speed) {
        Vec3 d = dir.normalize().scale(speed);
        this.setDeltaMovement(d);
        double h = d.horizontalDistance();
        this.setYRot((float) (Math.atan2(d.x, d.z) * 180 / Math.PI));
        this.setXRot((float) (Math.atan2(d.y, h) * 180 / Math.PI));
        return this;
    }

    public MagicBolt at(LivingEntity target, double speed) {
        Vec3 to = target.position().add(0, target.getBbHeight() * 0.55, 0);
        return aim(to.subtract(this.position()), speed);
    }

    public MagicBolt dmg(float d, OnHit h) { this.damage = d; this.onHit = h; return this; }
    public MagicBolt boom(float r) { this.explode = r; return this; }
    public MagicBolt grav(float g) { this.gravity = g; return this; }
    public MagicBolt home(LivingEntity t, float strength) { this.homingTarget = t; this.homing = strength; return this; }
    public MagicBolt pierce(int n) { this.pierce = n; return this; }
    public MagicBolt life(int n) { this.life = n; return this; }
    public MagicBolt zap(int interval) { this.zapInterval = interval; return this; }

    public MagicBolt fire() {
        this.level().addFreshEntity(this);
        return this;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(COLOR, 0xFFFFFF);
        this.entityData.define(COLOR2, 0xFFFFFF);
        this.entityData.define(SIZE, 0.5F);
        this.entityData.define(TRAIL, (byte) 0);
    }

    public int color() { return this.entityData.get(COLOR); }
    public int color2() { return this.entityData.get(COLOR2); }
    public float size() { return this.entityData.get(SIZE); }
    public byte trail() { return this.entityData.get(TRAIL); }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) { return d < 128 * 128; }

    @Override
    public void tick() {
        super.tick();
        Vec3 v = this.getDeltaMovement();
        if (!this.level().isClientSide()) {
            if (homing > 0 && homingTarget != null && homingTarget.isAlive()) {
                Vec3 want = homingTarget.position().add(0, homingTarget.getBbHeight() * 0.5, 0).subtract(this.position()).normalize().scale(v.length());
                v = v.add(want.subtract(v).scale(homing));
                this.setDeltaMovement(v);
            }
            if (zapInterval > 0 && this.tickCount % zapInterval == 0) zapNearby();
            HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (hit.getType() != HitResult.Type.MISS) this.onHit(hit);
            if (this.isRemoved()) return;
            if (this.tickCount > life) {
                if (explode > 0) detonate(this.position());
                this.discard();
                return;
            }
        } else {
            trailFx();
        }
        this.setPos(this.getX() + v.x, this.getY() + v.y, this.getZ() + v.z);
        if (gravity != 0) this.setDeltaMovement(v.x * 0.995, v.y - gravity, v.z * 0.995);
        if (this.isInWater() && trail() == FIRE) {
            this.level().addParticle(ParticleTypes.SMOKE, getX(), getY(), getZ(), 0, 0.05, 0);
        }
    }

    private void zapNearby() {
        Entity owner = getOwner();
        List<LivingEntity> list = level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(4), this::canHitEntity);
        for (LivingEntity e : list) {
            FX.zigzag(level(), FX.dust(color(), 0.8F), position(), e.position().add(0, e.getBbHeight() * 0.5, 0), 5, 0.6);
            Combat.magic(owner, this, e, damage * 0.35F);
            break;
        }
    }

    private void trailFx() {
        Level l = this.level();
        float s = size();
        ParticleOptions p = trailParticle();
        for (int i = 0; i < 2; i++) {
            l.addParticle(p, getX() + (random.nextDouble() - 0.5) * s, getY() + (random.nextDouble() - 0.5) * s, getZ() + (random.nextDouble() - 0.5) * s, 0, 0, 0);
        }
        if (random.nextInt(2) == 0) l.addParticle(FX.dust(color(), color2(), s * 1.5F), getX(), getY(), getZ(), 0, 0, 0);
    }

    public ParticleOptions trailParticle() {
        return switch (trail()) {
            case FIRE -> ParticleTypes.FLAME;
            case ICE -> ParticleTypes.SNOWFLAKE;
            case LIGHTNING -> ParticleTypes.ELECTRIC_SPARK;
            case WIND -> ParticleTypes.CLOUD;
            case WATER -> ParticleTypes.BUBBLE_POP;
            case LIGHT -> ParticleTypes.END_ROD;
            case DARK -> ParticleTypes.SMOKE;
            case NATURE -> ParticleTypes.HAPPY_VILLAGER;
            case ARCANE -> ParticleTypes.WITCH;
            case BLOOD -> ParticleTypes.DAMAGE_INDICATOR;
            case SPIRIT -> ParticleTypes.SOUL_FIRE_FLAME;
            case POISON -> ParticleTypes.ITEM_SLIME;
            default -> FX.dust(color(), 1.0F);
        };
    }

    @Override
    protected boolean canHitEntity(Entity e) {
        if (!e.isAlive() || !(e instanceof LivingEntity) || e == getOwner() || pierced.contains(e.getId())) return false;
        Entity owner = getOwner();
        if (owner instanceof LivingEntity o && !Combat.canHarm(o, (LivingEntity) e)) return false;
        return !e.isSpectator();
    }

    @Override
    protected void onHitEntity(EntityHitResult r) {
        if (!(r.getEntity() instanceof LivingEntity e)) return;
        Entity owner = getOwner();
        Combat.magic(owner, this, e, damage);
        Combat.applyOnHit(owner, e, onHit, damage);
        impactFx(r.getLocation());
        if (pierce > 0) {
            pierce--;
            pierced.add(e.getId());
            return;
        }
        if (explode > 0) detonate(r.getLocation());
        this.discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult r) {
        if (onHit == OnHit.WEB) {
            BlockPos p = r.getBlockPos().relative(r.getDirection());
            com.krolasyon.bosses.rpg.util.TempBlocks.place(level(), p, Blocks.COBWEB.defaultBlockState(), 100);
        }
        impactFx(r.getLocation());
        if (explode > 0) detonate(r.getLocation());
        this.discard();
    }

    private void impactFx(Vec3 at) {
        FX.send(level(), FX.dust(color(), 1.6F), at, 14, size() * 0.6, 0.02);
        FX.send(level(), trailParticle(), at, 8, size() * 0.5, 0.06);
    }

    private void detonate(Vec3 at) {
        Entity owner = getOwner();
        FX.sound(level(), at, SoundEvents.GENERIC_EXPLODE, 1.0F, 1.2F + random.nextFloat() * 0.3F);
        FX.send(level(), ParticleTypes.EXPLOSION, at, 2, explode * 0.3, 0);
        FX.sphere(level(), FX.dust(color(), color2(), 2.0F), at, explode, (int) (24 * explode));
        FX.burstRing(level(), trailParticle(), at, 24, 0.35);
        AABB box = new AABB(at, at).inflate(explode);
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, box, this::canHitEntity)) {
            double d = e.position().distanceTo(at);
            if (d > explode + 1) continue;
            float f = (float) (1.0 - d / (explode + 1) * 0.6);
            Combat.magic(owner, this, e, damage * 0.7F * f);
            Combat.applyOnHit(owner, e, onHit, damage * 0.5F);
            Vec3 push = e.position().subtract(at).normalize().scale(0.6 * f);
            e.push(push.x, 0.25 * f, push.z);
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.discard();
    }
}
