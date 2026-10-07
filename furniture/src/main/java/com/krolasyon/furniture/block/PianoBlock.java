package com.krolasyon.furniture.block;

import com.krolasyon.furniture.blockentity.PianoBlockEntity;
import com.krolasyon.furniture.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.Nullable;

public class PianoBlock extends WideBlock {
    private static final VoxelShape SHAPE = Block.box(0, 0, 1, 16, 26, 16);

    public PianoBlock(Properties props) { super(props); }

    @Override
    protected VoxelShape baseShape(WidePart part) { return SHAPE; }

    @Override
    protected int clearanceAbove() { return 1; }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            BlockPos base = basePos(state, pos);
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.krolasyon.furniture.client.ClientHooks.openPiano(base));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == WidePart.RIGHT ? new PianoBlockEntity(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide && state.getValue(PART) == WidePart.RIGHT
                ? ticker(type, ModBlockEntities.PIANO.get(), PianoBlockEntity::clientTick) : null;
    }
}
