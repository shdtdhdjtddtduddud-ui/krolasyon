package com.krolasyon.bosses.realm.portal;

import com.krolasyon.bosses.realm.block.RealmPortalBlock;
import com.krolasyon.bosses.realm.registry.RealmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import javax.annotation.Nullable;

/** Detects / builds rectangular portal frames made of Cursed Obsidian (any size from 2x3 to 21x21). */
public final class PortalShape {
    public final BlockPos bottomLeft;
    public final int width, height;
    public final Direction.Axis axis;

    private PortalShape(BlockPos bottomLeft, int width, int height, Direction.Axis axis) {
        this.bottomLeft = bottomLeft;
        this.width = width;
        this.height = height;
        this.axis = axis;
    }

    private static boolean frame(LevelAccessor l, BlockPos p) { return l.getBlockState(p).is(RealmBlocks.CURSED_OBSIDIAN.get()); }

    private static boolean empty(LevelAccessor l, BlockPos p) {
        BlockState s = l.getBlockState(p);
        return s.isAir() || s.is(BlockTags.FIRE);
    }

    public static Direction right(Direction.Axis axis) { return axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH; }

    @Nullable
    public static PortalShape find(LevelAccessor l, BlockPos start, Direction.Axis axis) {
        if (!empty(l, start)) return null;
        Direction right = right(axis), left = right.getOpposite();
        BlockPos p = start;
        int guard = 0;
        while (empty(l, p.below()) && guard++ < 22) p = p.below();
        if (!frame(l, p.below())) return null;
        guard = 0;
        while (empty(l, p.relative(left)) && frame(l, p.relative(left).below()) && guard++ < 22) p = p.relative(left);
        if (!frame(l, p.relative(left))) return null;
        BlockPos bl = p;
        int w = 0;
        while (w <= 21 && empty(l, bl.relative(right, w)) && frame(l, bl.relative(right, w).below())) w++;
        if (w < 2 || w > 21 || !frame(l, bl.relative(right, w))) return null;
        int h = 0;
        rows:
        while (h <= 21) {
            if (!frame(l, bl.relative(left).above(h)) || !frame(l, bl.relative(right, w).above(h))) break;
            for (int i = 0; i < w; i++) if (!empty(l, bl.relative(right, i).above(h))) break rows;
            h++;
        }
        if (h < 3 || h > 21) return null;
        for (int i = 0; i < w; i++) if (!frame(l, bl.relative(right, i).above(h))) return null;
        return new PortalShape(bl, w, h, axis);
    }

    public void fill(LevelAccessor l) {
        BlockState s = RealmBlocks.REALM_PORTAL.get().defaultBlockState().setValue(RealmPortalBlock.AXIS, axis);
        Direction right = right(axis);
        for (int i = 0; i < width; i++)
            for (int h = 0; h < height; h++) l.setBlock(bottomLeft.relative(right, i).above(h), s, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }

    /** try to light a frame from the empty block in front of the clicked frame face */
    @Nullable
    public static PortalShape tryLight(LevelAccessor l, BlockPos start) {
        for (Direction.Axis a : new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}) {
            PortalShape s = find(l, start, a);
            if (s != null) {
                s.fill(l);
                return s;
            }
        }
        return null;
    }

    /** builds a complete lit 2x3 portal standing on a small platform; returns the bottom-left interior block */
    public static BlockPos build(LevelAccessor l, BlockPos at, Direction.Axis axis, BlockState platform) {
        Direction right = right(axis);
        Direction side = right.getClockWise();
        BlockState frame = RealmBlocks.CURSED_OBSIDIAN.get().defaultBlockState();
        for (int i = -2; i <= 3; i++)
            for (int j = -2; j <= 2; j++) {
                BlockPos b = at.relative(right, i).relative(side, j);
                BlockState under = l.getBlockState(b.below());
                if (Math.abs(j) <= 1 && i >= -1 && i <= 2 || !under.isSolid()) l.setBlock(b.below(), platform, Block.UPDATE_CLIENTS);
                for (int h = 0; h <= 4; h++) l.setBlock(b.above(h), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        for (int i = -1; i <= 2; i++) {
            l.setBlock(at.relative(right, i).below(), frame, Block.UPDATE_CLIENTS);
            l.setBlock(at.relative(right, i).above(3), frame, Block.UPDATE_CLIENTS);
        }
        for (int h = 0; h < 3; h++) {
            l.setBlock(at.relative(right, -1).above(h), frame, Block.UPDATE_CLIENTS);
            l.setBlock(at.relative(right, 2).above(h), frame, Block.UPDATE_CLIENTS);
        }
        new PortalShape(at, 2, 3, axis).fill(l);
        return at;
    }

    /** a safe standing height at x/z of a (generated) level */
    public static int surfaceY(Level level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        int sea = level.getSeaLevel();
        if (y <= sea + 1) y = sea + 3;
        return Mth.clamp(y, level.getMinBuildHeight() + 6, level.getMaxBuildHeight() - 12);
    }
}
