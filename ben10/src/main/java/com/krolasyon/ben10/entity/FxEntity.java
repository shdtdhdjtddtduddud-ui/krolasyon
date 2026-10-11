package com.krolasyon.ben10.entity;

import com.krolasyon.ben10.ModRegistry;
import com.krolasyon.ben10.ability.Fx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Visual effect actors (flashes, shockwave rings, fire geysers, crystal spikes, tornado, beams, puddles, afterimages). */
public class FxEntity extends Entity {
    public static final int FLASH = 0, SONIC_RING = 1, RING = 2, PILLAR = 3, SPIKE = 4, TORNADO = 5, BEAM = 6, SPHERE = 7, PUDDLE = 8,
            AFTERIMAGE = 9, ICE_SPIKE = 10, SHADOW = 11;
    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(FxEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LIFE = SynchedEntityData.defineId(FxEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SIZE = SynchedEntityData.defineId(FxEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(FxEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Vector3f> VEC = SynchedEntityData.defineId(FxEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Integer> FOLLOW = SynchedEntityData.defineId(FxEntity.class, EntityDataSerializers.INT);
    private int owner = -1;
    private Vec3 followOffset = Vec3.ZERO;
    public float damagePerSecond = 3F;

    public FxEntity(EntityType<? extends FxEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public static FxEntity spawn(ServerLevel l, Vec3 at, int kind, int life, float size, int color, Entity owner) {
        FxEntity e = new FxEntity(ModRegistry.FX.get(), l);
        e.entityData.set(KIND, kind);
        e.entityData.set(LIFE, life);
        e.entityData.set(SIZE, size);
        e.entityData.set(COLOR, color);
        e.owner = owner == null ? -1 : owner.getId();
        e.setPos(at.x, at.y, at.z);
        if (owner != null) e.setYRot(owner.getYRot());
        l.addFreshEntity(e);
        return e;
    }

    public FxEntity vec(Vec3 v) {
        entityData.set(VEC, new Vector3f((float) v.x, (float) v.y, (float) v.z));
        return this;
    }

    public FxEntity follow(Entity e) {
        entityData.set(FOLLOW, e.getId());
        followOffset = position().subtract(e.position());
        return this;
    }

    public FxEntity yaw(float y) {
        setYRot(y);
        yRotO = y;
        return this;
    }

    public int kind() { return entityData.get(KIND); }

    public int life() { return entityData.get(LIFE); }

    public float size() { return entityData.get(SIZE); }

    public int color() { return entityData.get(COLOR); }

    public Vector3f vec() { return entityData.get(VEC); }

    @Override
    protected void defineSynchedData() {
        entityData.define(KIND, 0);
        entityData.define(LIFE, 20);
        entityData.define(SIZE, 1F);
        entityData.define(COLOR, 0xFFFFFF);
        entityData.define(VEC, new Vector3f());
        entityData.define(FOLLOW, -1);
    }

    @Override
    public void tick() {
        super.tick();
        int f = entityData.get(FOLLOW);
        if (f >= 0) {
            Entity t = level().getEntity(f);
            if (t != null) setPos(t.position().add(followOffset));
        }
        if (level().isClientSide) {
            clientParticles();
            return;
        }
        if (kind() == PUDDLE && tickCount % 10 == 0 && level().getEntity(owner) instanceof ServerPlayer p) {
            float r = size();
            for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, new AABB(position(), position()).inflate(r, 1.0, r))) {
                if (!Fx.valid(p, e)) continue;
                Fx.hitMagic(p, e, damagePerSecond * 0.5F);
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2));
                e.addEffect(new MobEffectInstance(MobEffects.POISON, 40, 0));
            }
        }
        if (tickCount >= life()) discard();
    }

    private void clientParticles() {
        var r = random;
        switch (kind()) {
            case PILLAR -> {
                if (tickCount < life() - 4) for (int i = 0; i < 4; i++) {
                    level().addParticle(r.nextBoolean() ? ParticleTypes.FLAME : ParticleTypes.LAVA, getX() + r.nextGaussian() * 0.3, getY() + r.nextDouble() * 3.5 * size(),
                            getZ() + r.nextGaussian() * 0.3, r.nextGaussian() * 0.03, 0.25, r.nextGaussian() * 0.03);
                }
            }
            case PUDDLE -> {
                if (r.nextInt(3) == 0) level().addParticle(Fx.dust(0x7EE030, 1F), getX() + r.nextGaussian() * size() * 0.5, getY() + 0.05,
                        getZ() + r.nextGaussian() * size() * 0.5, 0, 0.02, 0);
                if (r.nextInt(8) == 0) level().addParticle(ParticleTypes.BUBBLE_POP, getX() + r.nextGaussian() * size() * 0.4, getY() + 0.1,
                        getZ() + r.nextGaussian() * size() * 0.4, 0, 0.02, 0);
            }
            case TORNADO -> {
                for (int i = 0; i < 6; i++) {
                    double h = r.nextDouble() * 4.5 * size();
                    double a = (tickCount * 0.6 + r.nextDouble() * Math.PI * 2);
                    double rad = 0.3 + h * 0.35;
                    level().addParticle(ParticleTypes.CLOUD, getX() + Math.cos(a) * rad, getY() + h, getZ() + Math.sin(a) * rad,
                            -Math.sin(a) * 0.3, 0.05, Math.cos(a) * 0.3);
                }
            }
            default -> {}
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) {
        return d < 128 * 128;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag t) {
        entityData.set(LIFE, 0);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag t) {}

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }
}
