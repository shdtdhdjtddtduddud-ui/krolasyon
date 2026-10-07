package com.krolasyon.furniture.entity;

import com.krolasyon.furniture.block.SofaBlock;
import com.krolasyon.furniture.block.WideBlock;
import com.krolasyon.furniture.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;

/** invisible entity players ride while sitting on a sofa; sitting slowly regenerates health */
public class SeatEntity extends Entity {
    public SeatEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static boolean sit(Level level, BlockPos pos, double yOffset, Player player, Direction facing) {
        if (level.isClientSide) return false;
        List<SeatEntity> existing = level.getEntitiesOfClass(SeatEntity.class, new AABB(pos));
        if (!existing.isEmpty()) return false;
        SeatEntity seat = new SeatEntity(ModEntities.SEAT.get(), level);
        seat.setPos(pos.getX() + 0.5D, pos.getY() + yOffset, pos.getZ() + 0.5D);
        level.addFreshEntity(seat);
        player.setYRot(facing.toYRot());
        player.setXRot(10.0F);
        player.setYHeadRot(facing.toYRot());
        return player.startRiding(seat, true);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        BlockState st = level().getBlockState(blockPosition());
        if (getPassengers().isEmpty() || !(st.getBlock() instanceof SofaBlock)) {
            discard();
            return;
        }
        if (tickCount % 40 == 0) {
            for (Entity e : getPassengers()) {
                if (e instanceof LivingEntity le) {
                    le.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, false, true));
                }
            }
        }
    }

    @Override
    public double getPassengersRidingOffset() { return 0.0D; }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        BlockState st = level().getBlockState(blockPosition());
        if (st.getBlock() instanceof SofaBlock) {
            BlockPos out = blockPosition().relative(st.getValue(WideBlock.FACING));
            return Vec3.atBottomCenterOf(out);
        }
        return super.getDismountLocationForPassenger(passenger);
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
