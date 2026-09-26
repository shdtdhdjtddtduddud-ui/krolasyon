package com.krolasyon.bosses.entity;

import com.krolasyon.bosses.registry.ModEntities;
import com.krolasyon.bosses.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;

/** Ground eruption: crystal spike (Seal Warden) or hellfire pillar (Crimson Hound). */
public class EruptionEntity extends Entity {
    public static final int KIND_CRYSTAL = 0, KIND_FIRE = 1;
    private static final EntityDataAccessor<Integer> DATA_KIND = SynchedEntityData.defineId(EruptionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_WARMUP = SynchedEntityData.defineId(EruptionEntity.class, EntityDataSerializers.INT);

    public static final int ACTIVE_TICKS = 22;

    @Nullable private BossEntity owner;
    private float damage = 8F;
    private boolean struck;

    public EruptionEntity(EntityType<? extends EruptionEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static void spawn(BossEntity owner, double x, double z, double baseY, int kind, int warmup, float damage) {
        Level level = owner.level();
        Double y = findGround(level, x, z, baseY + 4, baseY - 7);
        if (y == null) return;
        EruptionEntity e = new EruptionEntity(ModEntities.ERUPTION.get(), level);
        e.owner = owner;
        e.damage = damage;
        e.moveTo(x, y, z, owner.getRandom().nextFloat() * 360F, 0F);
        e.entityData.set(DATA_KIND, kind);
        e.entityData.set(DATA_WARMUP, warmup);
        level.addFreshEntity(e);
    }

    /** highest standable surface between yMax and yMin, like the evoker fang placement */
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
                    BlockState here = level.getBlockState(pos);
                    VoxelShape shape = here.getCollisionShape(level, pos);
                    if (!shape.isEmpty()) extra = shape.max(Direction.Axis.Y);
                    if (extra >= 1.0) { pos = pos.below(); continue; }
                }
                return pos.getY() + extra;
            }
            pos = pos.below();
        }
        return null;
    }

    public int getKind() { return this.entityData.get(DATA_KIND); }
    public int getWarmup() { return this.entityData.get(DATA_WARMUP); }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_KIND, KIND_CRYSTAL);
        this.entityData.define(DATA_WARMUP, 0);
    }

    @Override
    public void tick() {
        super.tick();
        int warm = getWarmup();
        int t = this.tickCount - warm;
        boolean crystal = getKind() == KIND_CRYSTAL;
        if (this.level().isClientSide()) {
            if (t < 0) {
                // telegraph: glowing cracks on the ground
                if (random.nextInt(2) == 0) {
                    this.level().addParticle(crystal ? BossEntity.dust(SealWardenEntity.PINK, 1.0F) : ParticleTypes.SMALL_FLAME,
                            getX() + (random.nextDouble() - 0.5) * 1.2, getY() + 0.05, getZ() + (random.nextDouble() - 0.5) * 1.2, 0, 0.02, 0);
                }
            } else if (t < 14) {
                for (int i = 0; i < (crystal ? 2 : 4); i++) {
                    this.level().addParticle(crystal ? ParticleTypes.END_ROD : (i % 2 == 0 ? ParticleTypes.FLAME : ParticleTypes.LARGE_SMOKE),
                            getX() + (random.nextDouble() - 0.5), getY() + random.nextDouble() * (crystal ? 1.5 : 2.8), getZ() + (random.nextDouble() - 0.5),
                            (random.nextDouble() - 0.5) * 0.1, crystal ? 0.05 : 0.18, (random.nextDouble() - 0.5) * 0.1);
                }
            }
            return;
        }
        if (t == 0) {
            ServerLevel sl = (ServerLevel) this.level();
            this.level().playSound(null, getX(), getY(), getZ(), crystal ? ModSounds.CRYSTAL_ERUPT.get() : ModSounds.FISSURE.get(),
                    SoundSource.HOSTILE, crystal ? 0.9F : 1.0F, (crystal ? 1.2F : 0.8F) + random.nextFloat() * 0.4F);
            if (crystal) {
                sl.sendParticles(BossEntity.dust(SealWardenEntity.PINK_LIGHT, 1.6F), getX(), getY() + 0.3, getZ(), 14, 0.5, 0.3, 0.5, 0);
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 0.8, getZ(), 6, 0.3, 0.5, 0.3, 0.2);
            } else {
                sl.sendParticles(ParticleTypes.LAVA, getX(), getY() + 0.3, getZ(), 5, 0.3, 0.2, 0.3, 0);
                sl.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.5, getZ(), 18, 0.3, 0.8, 0.3, 0.08);
            }
        }
        if (t >= 0 && t <= 3 && !struck) {
            struck = true;
            for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.25, 0.4, 0.25))) {
                if (owner != null && !owner.isHostileTo(e)) continue;
                if (owner == null && e instanceof BossEntity) continue;
                boolean hurt = owner != null
                        ? e.hurt(crystal ? damageSources().indirectMagic(this, owner) : damageSources().mobAttack(owner), damage)
                        : e.hurt(damageSources().magic(), damage);
                if (hurt) {
                    e.setDeltaMovement(e.getDeltaMovement().add(0, crystal ? 0.75 : 0.6, 0));
                    e.hurtMarked = true;
                    if (!crystal) e.setSecondsOnFire(6);
                }
            }
        }
        if (t > ACTIVE_TICKS) this.discard();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.discard();
    }

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

    public static Vec3 unused() { return Vec3.ZERO; }
}
