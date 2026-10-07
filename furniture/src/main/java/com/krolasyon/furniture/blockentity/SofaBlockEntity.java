package com.krolasyon.furniture.blockentity;

import com.krolasyon.furniture.block.WideBlock;
import com.krolasyon.furniture.entity.SeatEntity;
import com.krolasyon.furniture.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class SofaBlockEntity extends BlockEntity {
    /** spring-driven cushion squash (client only): 0 = relaxed, 1 = somebody is sitting */
    public float sit, prevSit, vel;
    private boolean occupied;

    public SofaBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOFA.get(), pos, state);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, SofaBlockEntity be) {
        if (level.getGameTime() % 3 == 0) {
            Direction f = state.getValue(WideBlock.FACING);
            AABB box = new AABB(pos).minmax(new AABB(pos.relative(f.getCounterClockWise())));
            be.occupied = !level.getEntitiesOfClass(SeatEntity.class, box).isEmpty();
        }
        be.prevSit = be.sit;
        float target = be.occupied ? 1.0F : 0.0F;
        be.vel += (target - be.sit) * 0.30F;
        be.vel *= 0.60F;
        be.sit += be.vel;
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(2.0D, 2.0D, 2.0D);
    }
}
