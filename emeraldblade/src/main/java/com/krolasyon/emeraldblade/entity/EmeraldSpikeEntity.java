package com.krolasyon.emeraldblade.entity;

import com.krolasyon.emeraldblade.EmeraldBlade;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.UUID;

/** Yerden fışkıran zümrüt kristal kümesi: gecikmeyle patlar, değdiği düşmanları yaralar, havaya atar ve köklere bağlar. */
public final class EmeraldSpikeEntity extends Entity {
    private static final EntityDataAccessor<Integer> DELAY = SynchedEntityData.defineId(EmeraldSpikeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(EmeraldSpikeEntity.class, EntityDataSerializers.FLOAT);
    public static final int LIFE = 46;

    @Nullable private UUID ownerId;
    @Nullable private LivingEntity owner;

    public EmeraldSpikeEntity(EntityType<? extends EmeraldSpikeEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public EmeraldSpikeEntity(Level level, double x, double y, double z, int delay, float scale, LivingEntity owner) {
        this(EmeraldBlade.SPIKE.get(), level);
        setPos(x, y, z);
        setYRot(level.random.nextFloat() * 360F);
        entityData.set(DELAY, delay);
        entityData.set(SCALE, scale);
        this.owner = owner;
        this.ownerId = owner.getUUID();
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DELAY, 0);
        entityData.define(SCALE, 1.0F);
    }

    public int getDelay() { return entityData.get(DELAY); }
    public float getScale() { return entityData.get(SCALE); }

    @Override
    public void tick() {
        super.tick();
        int delay = getDelay();
        if (level() instanceof ServerLevel sl) {
            if (tickCount == delay) erupt(sl);
            if (tickCount > delay + LIFE) discard();
        } else if (tickCount == delay) {
            for (int i = 0; i < 12; i++) {
                level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, level().getBlockState(blockPosition().below())),
                        getX() + (random.nextDouble() - 0.5), getY() + 0.1, getZ() + (random.nextDouble() - 0.5), 0, 0.3, 0);
            }
        }
    }

    private void erupt(ServerLevel sl) {
        float s = getScale();
        sl.playSound(null, blockPosition(), SoundEvents.EVOKER_FANGS_ATTACK, SoundSource.PLAYERS, 1.0F, 0.8F + random.nextFloat() * 0.3F);
        sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 1.2F, 0.7F);
        sl.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.EMERALD)), getX(), getY() + 0.8 * s, getZ(), 14, 0.3, 0.6, 0.3, 0.15);
        LivingEntity own = getOwner(sl);
        AABB box = new AABB(getX() - 0.8 * s, getY(), getZ() - 0.8 * s, getX() + 0.8 * s, getY() + 2.2 * s, getZ() + 0.8 * s);
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, box)) {
            if (e == own || !e.isAlive() || (own != null && e.isAlliedTo(own))) continue;
            e.hurt(sl.damageSources().indirectMagic(this, own), 9.0F * Math.min(1.3F, s));
            e.setDeltaMovement(e.getDeltaMovement().x * 0.3, 0.55 + 0.15 * s, e.getDeltaMovement().z * 0.3);
            e.hurtMarked = true;
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 3));
        }
    }

    @Nullable
    private LivingEntity getOwner(ServerLevel sl) {
        if (owner == null && ownerId != null && sl.getEntity(ownerId) instanceof LivingEntity le) owner = le;
        return owner;
    }

    @Override
    public boolean isPickable() { return false; }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) ownerId = tag.getUUID("Owner");
        entityData.set(DELAY, tag.getInt("Delay"));
        entityData.set(SCALE, tag.contains("Scale") ? tag.getFloat("Scale") : 1F);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerId != null) tag.putUUID("Owner", ownerId);
        tag.putInt("Delay", getDelay());
        tag.putFloat("Scale", getScale());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
