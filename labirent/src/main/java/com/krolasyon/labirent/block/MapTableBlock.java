package com.krolasyon.labirent.block;

import com.krolasyon.labirent.maze.MazeManager;
import com.krolasyon.labirent.net.Net;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The Map Room table: shows the shared map of the maze and the code words found so far. */
public class MapTableBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 14, 16);

    public MapTableBlock(Properties p) { super(p); }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState s, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer sp) {
            MazeManager m = MazeManager.get();
            if (m != null) {
                m.story.onMapTable(sp);
                Net.sendFullMap(sp, m.state);
                Net.sendState(sp, m);
            }
            Net.open(sp, Net.SCREEN_MAP, 1);
        }
        return InteractionResult.CONSUME;
    }
}
