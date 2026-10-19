package com.krolasyon.sylvanarchery.entity;

import com.krolasyon.sylvanarchery.SylvanArchery;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/** Orman Oku: uçarken en yakın düşmana hafifçe yönelir, ışıltılı iz bırakır. Rüzgar okları deler ve daha parlak iz bırakır. */
public final class SylvanArrowEntity extends AbstractArrow {
    private static final EntityDataAccessor<Boolean> WIND = SynchedEntityData.defineId(SylvanArrowEntity.class, EntityDataSerializers.BOOLEAN);

    public SylvanArrowEntity(EntityType<? extends SylvanArrowEntity> type, Level level) {
        super(type, level);
        setBaseDamage(3.0);
    }

    public SylvanArrowEntity(Level level, LivingEntity shooter) {
        super(SylvanArchery.ARROW_ENTITY.get(), shooter, level);
        setBaseDamage(3.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(WIND, false);
    }

    public void setWind(boolean wind) { entityData.set(WIND, wind); }
    public boolean isWind() { return entityData.get(WIND); }

    @Override
    public void tick() {
        super.tick();
        if (inGround) return;
        if (level().isClientSide) {
            if (tickCount > 1) {
                level().addParticle(ParticleTypes.GLOW, getX(), getY(), getZ(), 0, 0, 0);
                if (isWind() && tickCount % 2 == 0) {
                    level().addParticle(ParticleTypes.HAPPY_VILLAGER, getX(), getY(), getZ(), 0, 0, 0);
                }
            }
            return;
        }
        if (tickCount > 3 && tickCount < 80) home();
    }

    private void home() {
        Vec3 vel = getDeltaMovement();
        double speed = vel.length();
        if (speed < 0.3) return;
        Vec3 dir = vel.scale(1 / speed);
        LivingEntity best = null;
        double bestScore = 0;
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, new AABB(blockPosition()).inflate(14))) {
            if (!(e instanceof Enemy) || !e.isAlive() || e == getOwner()) continue;
            Vec3 to = e.getBoundingBox().getCenter().subtract(position());
            double dist = to.length();
            if (dist < 0.5) continue;
            double dot = to.scale(1 / dist).dot(dir);
            if (dot < 0.72) continue;
            double score = dot / (1 + dist * 0.15);
            if (score > bestScore) {
                bestScore = score;
                best = e;
            }
        }
        if (best == null) return;
        Vec3 to = best.getBoundingBox().getCenter().subtract(position()).normalize();
        Vec3 nd = dir.scale(0.86).add(to.scale(0.14)).normalize();
        setDeltaMovement(nd.scale(speed));
    }

    @Override
    protected ItemStack getPickupItem() {
        return new ItemStack(SylvanArchery.ARROW.get());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Wind", isWind());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setWind(tag.getBoolean("Wind"));
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
