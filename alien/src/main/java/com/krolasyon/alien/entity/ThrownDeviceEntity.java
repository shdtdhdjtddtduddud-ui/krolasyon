package com.krolasyon.alien.entity;

import com.krolasyon.alien.ai.NoiseSystem;
import com.krolasyon.alien.registry.ModEntities;
import com.krolasyon.alien.registry.ModItems;
import com.krolasyon.alien.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;

/** A thrown noisemaker or flare. Once it lands it beeps / burns for a while. */
public class ThrownDeviceEntity extends ThrowableItemProjectile {
    public static final int NOISEMAKER = 0, FLARE = 1;
    private static final EntityDataAccessor<Byte> DATA_KIND = SynchedEntityData.defineId(ThrownDeviceEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> DATA_LANDED = SynchedEntityData.defineId(ThrownDeviceEntity.class, EntityDataSerializers.BOOLEAN);

    private int life;
    @Nullable private BlockPos light;

    public ThrownDeviceEntity(EntityType<? extends ThrownDeviceEntity> type, Level level) { super(type, level); }

    public ThrownDeviceEntity(Level level, LivingEntity owner, int kind) {
        super(ModEntities.THROWN_DEVICE.get(), owner, level);
        this.entityData.set(DATA_KIND, (byte) kind);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_KIND, (byte) 0);
        this.entityData.define(DATA_LANDED, false);
    }

    public int kind() { return this.entityData.get(DATA_KIND); }
    public boolean landed() { return this.entityData.get(DATA_LANDED); }

    @Override
    protected Item getDefaultItem() { return kind() == FLARE ? ModItems.FLARE.get() : ModItems.NOISEMAKER.get(); }

    @Override
    protected float getGravity() { return 0.045F; }

    private void land(Vec3 at) {
        this.setPos(at);
        this.setDeltaMovement(Vec3.ZERO);
        this.setNoGravity(true);
        this.entityData.set(DATA_LANDED, true);
        this.playSound(kind() == FLARE ? ModSounds.FLARE.get() : ModSounds.NOISEMAKER.get(), 0.8F, 1.0F);
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        if (landed()) return;
        Vec3 n = Vec3.atLowerCornerOf(hit.getDirection().getNormal()).scale(0.12);
        land(hit.getLocation().add(n));
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        if (!landed()) land(this.position());
    }

    @Override
    public void tick() {
        if (!landed()) {
            super.tick();
            return;
        }
        this.baseTick();
        life++;
        Level level = this.level();
        boolean flare = kind() == FLARE;
        if (level.isClientSide) {
            if (flare) {
                level.addParticle(ParticleTypes.FLAME, getX(), getY() + 0.1, getZ(), (random.nextDouble() - 0.5) * 0.02, 0.03, (random.nextDouble() - 0.5) * 0.02);
                if (random.nextInt(2) == 0)
                    level.addParticle(new DustParticleOptions(new Vector3f(1F, 0.1F, 0.1F), 1.2F), getX(), getY() + 0.15, getZ(), 0, 0.05, 0);
                if (random.nextInt(3) == 0) level.addParticle(ParticleTypes.SMOKE, getX(), getY() + 0.2, getZ(), 0, 0.04, 0);
            } else if (life % 20 < 3) {
                level.addParticle(new DustParticleOptions(new Vector3f(1F, 0.15F, 0.05F), 0.8F), getX(), getY() + 0.12, getZ(), 0, 0, 0);
            }
            return;
        }
        ServerLevel sl = (ServerLevel) level;
        if (flare) {
            BlockPos p = this.blockPosition();
            if (light == null && sl.getBlockState(p).isAir()) {
                sl.setBlock(p, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15), 3);
                light = p.immutable();
            }
            if (life % 40 == 1) NoiseSystem.emit(sl, this.position(), 9, NoiseSystem.Kind.FLARE, this.getOwner());
            if (life % 30 == 1) this.playSound(ModSounds.FLARE.get(), 0.4F, 0.8F + random.nextFloat() * 0.3F);
            if (life > 600) this.discard();
        } else {
            if (life % 20 == 1) {
                this.playSound(ModSounds.NOISEMAKER.get(), 1.6F, 1.0F);
                NoiseSystem.emit(sl, this.position(), 28, NoiseSystem.Kind.NOISEMAKER, null);
            }
            if (life > 220) this.discard();
        }
    }

    /** the alien swats a decoy it reached. */
    public void smash() {
        this.playSound(ModSounds.LOCKER.get(), 1.0F, 1.6F);
        this.discard();
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide && light != null && this.level().getBlockState(light).is(Blocks.LIGHT))
            this.level().setBlock(light, Blocks.AIR.defaultBlockState(), 3);
        super.remove(reason);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("Kind", (byte) kind());
        tag.putBoolean("Landed", landed());
        tag.putInt("Life", life);
        if (light != null) tag.putLong("Light", light.asLong());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_KIND, tag.getByte("Kind"));
        this.entityData.set(DATA_LANDED, tag.getBoolean("Landed"));
        life = tag.getInt("Life");
        if (tag.contains("Light")) light = BlockPos.of(tag.getLong("Light"));
    }
}
