package com.krolasyon.furniture.block;

import com.krolasyon.furniture.blockentity.SofaBlockEntity;
import com.krolasyon.furniture.entity.SeatEntity;
import com.krolasyon.furniture.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class SofaBlock extends WideBlock {
    public static final double SEAT_Y = 0.30D;

    private static final VoxelShape SEAT = Block.box(0, 0, 0, 16, 9.5, 16);
    private static final VoxelShape BACK = Block.box(0, 9.5, 11, 16, 18, 16);
    private static final VoxelShape ARM_R = Block.box(12, 9.5, 0, 16, 12.5, 16);
    private static final VoxelShape ARM_L = Block.box(0, 9.5, 0, 4, 12.5, 16);

    public SofaBlock(Properties props) { super(props); }

    @Override
    protected VoxelShape baseShape(WidePart part) {
        return Shapes.or(SEAT, BACK, part == WidePart.RIGHT ? ARM_R : ARM_L);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isShiftKeyDown() || player.isPassenger()) return InteractionResult.PASS;
        if (!level.isClientSide) {
            SeatEntity.sit(level, pos, SEAT_Y, player, state.getValue(FACING));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == WidePart.RIGHT ? new SofaBlockEntity(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide && state.getValue(PART) == WidePart.RIGHT
                ? ticker(type, ModBlockEntities.SOFA.get(), SofaBlockEntity::clientTick) : null;
    }
}
