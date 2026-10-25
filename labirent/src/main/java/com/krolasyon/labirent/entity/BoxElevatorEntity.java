package com.krolasyon.labirent.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

/** The Box itself: an iron cage that rises up the shaft carrying a newcomer. Moves on both sides for smooth motion. */
public class BoxElevatorEntity extends Entity {
    private static final EntityDataAccessor<Boolean> DATA_MOVING = SynchedEntityData.defineId(BoxElevatorEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_TARGET = SynchedEntityData.defineId(BoxElevatorEntity.class, EntityDataSerializers.FLOAT);
    public static final double SPEED = 0.075;
    private boolean released;
    private int life;

    public BoxElevatorEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_MOVING, false);
        entityData.define(DATA_TARGET, 0F);
    }

    public void setTarget(double y) { entityData.set(DATA_TARGET, (float) y); }

    public void go() { entityData.set(DATA_MOVING, true); }

    public boolean arrived() { return getY() >= entityData.get(DATA_TARGET) - 0.001; }

    public boolean moving() { return entityData.get(DATA_MOVING) && !arrived(); }

    public void release() { released = true; }

    public boolean released() { return released; }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && ++life > 2400) discard();
        if (moving()) {
            double ny = Math.min(entityData.get(DATA_TARGET), getY() + SPEED);
            setPos(getX(), ny, getZ());
        }
    }

    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps, boolean teleport) {
        // the client simulates the climb itself; only accept big corrections
        if (Math.abs(y - getY()) > 0.6) setPos(x, y, z);
    }

    @Override
    public double getPassengersRidingOffset() { return 0.05; }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction move) {
        move.accept(passenger, getX() - 0.5, getY() + 0.05, getZ() - 0.5);
    }

    @Override
    public boolean shouldRiderSit() { return false; }

    @Override
    public boolean isPickable() { return false; }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) { return d < 128 * 128; }

    @Override
    protected void readAdditionalSaveData(CompoundTag t) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag t) {}

    @Override
    public boolean isAttackable() { return false; }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
