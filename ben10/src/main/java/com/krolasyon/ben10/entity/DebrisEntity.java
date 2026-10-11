package com.krolasyon.ben10.entity;

import com.krolasyon.ben10.ModRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Purely visual chunk of ground that flies up from impacts and vanishes (the world is never changed). */
public class DebrisEntity extends Entity {
    private static final EntityDataAccessor<Integer> BLOCK = SynchedEntityData.defineId(DebrisEntity.class, EntityDataSerializers.INT);
    private int life = 40;
    public float spinX, spinZ;

    public DebrisEntity(EntityType<? extends DebrisEntity> type, Level level) {
        super(type, level);
        spinX = (random.nextFloat() - 0.5F) * 40;
        spinZ = (random.nextFloat() - 0.5F) * 40;
        life = 35 + random.nextInt(30);
    }

    public static void spawn(ServerLevel l, Vec3 at, BlockState s, Vec3 vel) {
        if (s.getRenderShape() != RenderShape.MODEL || s.isAir()) return;
        DebrisEntity e = new DebrisEntity(ModRegistry.DEBRIS.get(), l);
        e.entityData.set(BLOCK, Block.getId(s));
        e.setPos(at.x, at.y, at.z);
        e.setDeltaMovement(vel);
        l.addFreshEntity(e);
    }

    public BlockState block() {
        return Block.stateById(entityData.get(BLOCK));
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(BLOCK, 0);
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 v = getDeltaMovement().add(0, -0.06, 0);
        move(MoverType.SELF, v);
        Vec3 after = getDeltaMovement();
        if (onGround()) {
            setDeltaMovement(after.x * 0.6, Math.abs(v.y) > 0.15 ? -v.y * 0.35 : 0, after.z * 0.6);
            spinX *= 0.7F;
            spinZ *= 0.7F;
        } else {
            setDeltaMovement(after.scale(0.98));
        }
        if (!level().isClientSide && tickCount > life) discard();
    }

    public int life() {
        return life;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag t) {
        life = 0;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag t) {}

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }
}
