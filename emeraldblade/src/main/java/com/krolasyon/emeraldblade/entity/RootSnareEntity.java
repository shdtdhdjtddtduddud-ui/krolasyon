package com.krolasyon.emeraldblade.entity;

import com.krolasyon.emeraldblade.EmeraldBlade;
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
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;

/** Hedefin etrafında yerden yükselen, onu yerine çivileyen kökler. */
public final class RootSnareEntity extends Entity {
    private static final EntityDataAccessor<Integer> TARGET = SynchedEntityData.defineId(RootSnareEntity.class, EntityDataSerializers.INT);
    public static final int LIFE = 70;

    @Nullable private LivingEntity attacker;

    public RootSnareEntity(EntityType<? extends RootSnareEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public static void snare(ServerLevel level, LivingEntity target, LivingEntity attacker) {
        for (RootSnareEntity s : level.getEntitiesOfClass(RootSnareEntity.class, target.getBoundingBox().inflate(1))) {
            if (s.getTargetId() == target.getId()) {
                s.tickCount = Math.min(s.tickCount, 10);
                return;
            }
        }
        RootSnareEntity s = new RootSnareEntity(EmeraldBlade.SNARE.get(), level);
        s.setPos(target.getX(), target.getY(), target.getZ());
        s.entityData.set(TARGET, target.getId());
        s.attacker = attacker;
        level.addFreshEntity(s);
        level.playSound(null, target.blockPosition(), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.HOSTILE, 1.4F, 0.6F);
        level.playSound(null, target.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.HOSTILE, 0.8F, 0.6F);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(TARGET, -1);
    }

    public int getTargetId() { return entityData.get(TARGET); }

    @Nullable
    public LivingEntity getTarget() {
        return level().getEntity(getTargetId()) instanceof LivingEntity le ? le : null;
    }

    @Override
    public void tick() {
        super.tick();
        LivingEntity t = getTarget();
        if (t != null) setPos(t.getX(), t.getY(), t.getZ());
        if (!(level() instanceof ServerLevel sl)) return;
        if (t == null || !t.isAlive() || tickCount > LIFE) {
            discard();
            return;
        }
        // yerine çivile
        t.setDeltaMovement(0, Math.min(0, t.getDeltaMovement().y), 0);
        t.hurtMarked = true;
        if (tickCount % 10 == 1) t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 5, false, false));
        if (tickCount % 20 == 0) {
            t.hurt(sl.damageSources().thorns(attacker != null ? attacker : this), 1.5F);
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, t.getX(), t.getY() + 0.5, t.getZ(), 4, 0.3, 0.3, 0.3, 0);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public boolean shouldBeSaved() { return false; }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
