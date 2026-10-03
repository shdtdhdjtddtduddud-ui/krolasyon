package com.krolasyon.bosses.item;

import com.krolasyon.bosses.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.ArrayList;
import java.util.List;

/** Lights a hellgate: strike a block of the frame and the empty rectangle inside it fills with the portal. */
public class HellSparkItem extends Item {
    public HellSparkItem(Properties props) { super(props); }

    private static boolean frame(Level l, BlockPos p) { return l.getBlockState(p).is(ModBlocks.get("hellgate_stone")); }

    private static boolean empty(Level l, BlockPos p) { return l.getBlockState(p).isAir(); }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos clicked = ctx.getClickedPos();
        if (!frame(level, clicked)) return InteractionResult.PASS;
        for (Direction.Axis axis : new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}) {
            Direction along = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
            // seed: an empty block next to the clicked frame block, inside the plane of the gate
            for (Direction d : new Direction[]{along, along.getOpposite(), Direction.UP, Direction.DOWN}) {
                BlockPos seed = clicked.relative(d);
                List<BlockPos> cells = fill(level, seed, along);
                if (cells != null) {
                    if (!level.isClientSide()) {
                        BlockState portal = ModBlocks.HELL_PORTAL.get().defaultBlockState().setValue(com.krolasyon.bosses.block.HellPortalBlock.AXIS, axis);
                        for (BlockPos c : cells) level.setBlock(c, portal, 2 | 16);
                        level.playSound(null, clicked, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0F, 0.6F);
                        level.playSound(null, clicked, SoundEvents.PORTAL_TRIGGER, SoundSource.BLOCKS, 0.4F, 0.5F);
                        if (ctx.getPlayer() != null && !ctx.getPlayer().isCreative()) ctx.getItemInHand().shrink(1);
                    }
                    return InteractionResult.sidedSuccess(level.isClientSide());
                }
            }
        }
        return InteractionResult.PASS;
    }

    /** flood fill of empty cells in the vertical plane through {@code seed}; null unless fully enclosed by frame blocks and between 2x3 and 21x21 */
    private static List<BlockPos> fill(Level l, BlockPos seed, Direction along) {
        if (!empty(l, seed)) return null;
        List<BlockPos> out = new ArrayList<>();
        java.util.ArrayDeque<BlockPos> q = new java.util.ArrayDeque<>();
        java.util.HashSet<BlockPos> seen = new java.util.HashSet<>();
        q.add(seed);
        seen.add(seed);
        while (!q.isEmpty()) {
            BlockPos p = q.poll();
            out.add(p);
            if (out.size() > 400) return null;
            for (Direction d : new Direction[]{along, along.getOpposite(), Direction.UP, Direction.DOWN}) {
                BlockPos n = p.relative(d);
                if (seen.contains(n)) continue;
                if (frame(l, n)) continue;
                if (empty(l, n)) { seen.add(n); q.add(n); } else return null;
            }
        }
        return out.size() >= 6 ? out : null;
    }
}
