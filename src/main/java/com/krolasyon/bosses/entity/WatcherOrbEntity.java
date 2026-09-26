package com.krolasyon.bosses.entity;

import com.krolasyon.bosses.registry.ModEntities;
import com.krolasyon.bosses.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;

/** Floating seal eye summoned by the Seal Warden. Orbits its master and fires seal bolts. Can be shot down. */
public class WatcherOrbEntity extends Entity {
    private static final EntityDataAccessor<Integer> DATA_OWNER = SynchedEntityData.defineId(WatcherOrbEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_INDEX = SynchedEntityData.defineId(WatcherOrbEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_COUNT = SynchedEntityData.defineId(WatcherOrbEntity.class, EntityDataSerializers.INT);
    public static final int LIFE = 320;

    public WatcherOrbEntity(EntityType<? extends WatcherOrbEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public void setup(SealWardenEntity owner, int index, int count) {
        this.entityData.set(DATA_OWNER, owner.getId());
        this.entityData.set(DATA_INDEX, index);
        this.entityData.set(DATA_COUNT, count);
        Vec3 p = orbit(owner, 0);
        this.moveTo(p.x, p.y, p.z, 0, 0);
    }

    public int getOwnerId() { return this.entityData.get(DATA_OWNER); }

    @Nullable
    public SealWardenEntity getOwner() {
        Entity e = this.level().getEntity(getOwnerId());
        return e instanceof SealWardenEntity w ? w : null;
    }

    private Vec3 orbit(Entity owner, float partial) {
        double t = (this.tickCount + partial) * 0.045 + this.entityData.get(DATA_INDEX) * Math.PI * 2 / Math.max(1, this.entityData.get(DATA_COUNT));
        double r = 3.6;
        return owner.getPosition(partial).add(Math.cos(t) * r, 3.4 + Math.sin(t * 2.3) * 0.5, Math.sin(t) * r);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_OWNER, -1);
        this.entityData.define(DATA_INDEX, 0);
        this.entityData.define(DATA_COUNT, 3);
    }

    @Override
    public void tick() {
        super.tick();
        SealWardenEntity owner = getOwner();
        if (owner == null || !owner.isAlive()) {
            if (!this.level().isClientSide()) pop();
            return;
        }
        if (this.level().isClientSide()) {
            if (random.nextInt(2) == 0)
                this.level().addParticle(BossEntity.dust(SealWardenEntity.PINK, 0.8F), getX() + (random.nextDouble() - 0.5) * 0.6, getY() + 0.4 + (random.nextDouble() - 0.5) * 0.6, getZ() + (random.nextDouble() - 0.5) * 0.6, 0, 0, 0);
            return;
        }
        Vec3 p = orbit(owner, 0);
        this.setPos(p.x, p.y, p.z);
        LivingEntity target = owner.getTarget();
        if (target != null) {
            Vec3 d = target.getEyePosition().subtract(p);
            this.setYRot((float) (Mth.atan2(d.z, d.x) * (180F / Math.PI)) - 90.0F);
            this.setXRot((float) (-(Mth.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * (180F / Math.PI))));
        } else {
            this.setYRot(this.getYRot() + 6F);
        }
        int interval = owner.isPhase2() ? 22 : 32;
        if (target != null && target.isAlive() && (this.tickCount + this.entityData.get(DATA_INDEX) * 9) % interval == 0 && this.tickCount > 20) {
            Vec3 from = this.position().add(0, 0.4, 0);
            Vec3 dir = target.getBoundingBox().getCenter().subtract(from);
            SealBoltEntity bolt = new SealBoltEntity(ModEntities.SEAL_BOLT.get(), this.level());
            bolt.setup(owner, from, dir);
            this.level().addFreshEntity(bolt);
            this.level().playSound(null, getX(), getY(), getZ(), ModSounds.BOLT_SHOOT.get(), SoundSource.HOSTILE, 1.2F, 0.9F + random.nextFloat() * 0.3F);
        }
        if (this.tickCount > LIFE) pop();
    }

    private void pop() {
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(BossEntity.dust(SealWardenEntity.PINK_LIGHT, 1.5F), getX(), getY() + 0.4, getZ(), 25, 0.3, 0.3, 0.3, 0);
            sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.4, getZ(), 10, 0.1, 0.1, 0.1, 0.15);
            this.level().playSound(null, getX(), getY(), getZ(), ModSounds.BOLT_HIT.get(), SoundSource.HOSTILE, 1.0F, 1.5F);
        }
        this.discard();
    }

    @Override
    public boolean isPickable() { return true; }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (this.isInvulnerableTo(src)) return false;
        if (src.getEntity() instanceof SealWardenEntity || src.getEntity() == null && src.getDirectEntity() == null) return false;
        if (!this.level().isClientSide()) pop();
        return true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) { this.discard(); }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
