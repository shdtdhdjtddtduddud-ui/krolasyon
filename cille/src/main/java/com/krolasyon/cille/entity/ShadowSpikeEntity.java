package com.krolasyon.cille.entity;

import com.krolasyon.cille.registry.ModEntities;
import com.krolasyon.cille.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;

/** A spike of solid shadow bursting out of the ground (telegraphed by a spreading dark stain). */
public class ShadowSpikeEntity extends Entity {
    private static final EntityDataAccessor<Integer> DATA_WARMUP = SynchedEntityData.defineId(ShadowSpikeEntity.class, EntityDataSerializers.INT);
    public static final int ACTIVE_TICKS = 24;

    @Nullable private KabkabaEntity owner;
    private float damage = 9F;
    private boolean struck;

    public ShadowSpikeEntity(EntityType<? extends ShadowSpikeEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static void spawn(KabkabaEntity owner, double x, double z, double baseY, int warmup, float damage) {
        Level level = owner.level();
        Double y = findGround(level, x, z, baseY + 4, baseY - 7);
        if (y == null) return;
        ShadowSpikeEntity e = new ShadowSpikeEntity(ModEntities.SHADOW_SPIKE.get(), level);
        e.owner = owner;
        e.damage = damage;
        e.moveTo(x, y, z, owner.getRandom().nextFloat() * 360F, 0F);
        e.entityData.set(DATA_WARMUP, warmup);
        level.addFreshEntity(e);
    }

    @Nullable
    public static Double findGround(Level level, double x, double z, double yMax, double yMin) {
        BlockPos pos = BlockPos.containing(x, yMax, z);
        int min = Mth.floor(yMin);
        while (pos.getY() >= min) {
            BlockPos below = pos.below();
            BlockState state = level.getBlockState(below);
            if (state.isFaceSturdy(level, below, Direction.UP)) {
                double extra = 0;
                if (!level.isEmptyBlock(pos)) {
                    VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
                    if (!shape.isEmpty()) extra = shape.max(Direction.Axis.Y);
                    if (extra >= 1.0) { pos = pos.below(); continue; }
                }
                return pos.getY() + extra;
            }
            pos = pos.below();
        }
        return null;
    }

    public int getWarmup() { return this.entityData.get(DATA_WARMUP); }

    @Override
    protected void defineSynchedData() { this.entityData.define(DATA_WARMUP, 0); }

    @Override
    public void tick() {
        super.tick();
        int t = this.tickCount - getWarmup();
        if (this.level().isClientSide()) {
            if (t < 0) {
                for (int i = 0; i < 2; i++)
                    this.level().addParticle(KabkabaEntity.dust(KabkabaEntity.INK, 1.6F), getX() + (random.nextDouble() - 0.5) * 1.6, getY() + 0.05, getZ() + (random.nextDouble() - 0.5) * 1.6, 0, 0.01, 0);
                if (random.nextInt(3) == 0) this.level().addParticle(ParticleTypes.SQUID_INK, getX(), getY() + 0.1, getZ(), 0, 0.02, 0);
            } else if (t < 12) {
                this.level().addParticle(random.nextBoolean() ? ParticleTypes.REVERSE_PORTAL : KabkabaEntity.dust(KabkabaEntity.VIOLET, 1.2F),
                        getX() + (random.nextDouble() - 0.5), getY() + random.nextDouble() * 2.2, getZ() + (random.nextDouble() - 0.5), 0, 0.05, 0);
            }
            return;
        }
        if (t == 0) {
            ServerLevel sl = (ServerLevel) this.level();
            this.level().playSound(null, getX(), getY(), getZ(), ModSounds.SHADOW_RISE.get(), SoundSource.HOSTILE, 1.0F, 0.8F + random.nextFloat() * 0.2F);
            BlockState below = this.level().getBlockState(this.blockPosition().below());
            if (!below.isAir()) sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, below), getX(), getY() + 0.2, getZ(), 18, 0.4, 0.2, 0.4, 0.15);
            sl.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 0.8, getZ(), 10, 0.3, 0.6, 0.3, 0.05);
            // shadow tears through plants, leaves, glass and snow around it
            if (owner != null) Terrain.shatterSoft(sl, owner, this.blockPosition(), 1, 2);
        }
        if (t >= 0 && t <= 3 && !struck) {
            struck = true;
            for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.35, 0.3, 0.35))) {
                if (owner == null ? e instanceof KabkabaEntity : !owner.isHostileTo(e)) continue;
                boolean hurt = owner != null ? e.hurt(damageSources().indirectMagic(this, owner), damage) : e.hurt(damageSources().magic(), damage);
                if (hurt) {
                    e.setDeltaMovement(e.getDeltaMovement().add(0, 0.7, 0));
                    e.hurtMarked = true;
                    e.addEffect(new MobEffectInstance(MobEffects.WITHER, 50, 0));
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
                }
            }
        }
        if (t > ACTIVE_TICKS) this.discard();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) { this.discard(); }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public boolean isPickable() { return false; }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) { return d < 96 * 96; }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
