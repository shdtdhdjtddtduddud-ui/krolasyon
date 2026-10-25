package com.krolasyon.labirent.maze;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

import java.util.ArrayList;
import java.util.List;

/** Small structure-building helpers used by the glade and lab builders. */
public final class Sculpt {
    private final ServerLevel level;
    private final BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
    private final List<BlockPos> reshape = new ArrayList<>();

    public Sculpt(ServerLevel level) { this.level = level; }

    public ServerLevel level() { return level; }

    public void set(int x, int y, int z, BlockState s) {
        level.setBlock(p.set(x, y, z), s, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        Block b = s.getBlock();
        if (b instanceof FenceBlock || b instanceof IronBarsBlock || b instanceof WallBlock || b instanceof StairBlock) reshape.add(new BlockPos(x, y, z));
    }

    public void set(int x, int y, int z, Block b) { set(x, y, z, b.defaultBlockState()); }

    public void fill(int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) set(x, y, z, s);
    }

    public void fill(int x0, int y0, int z0, int x1, int y1, int z1, Block b) { fill(x0, y0, z0, x1, y1, z1, b.defaultBlockState()); }

    /** walls of a box (no floor / ceiling) */
    public void walls(int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
        fill(x0, y0, z0, x1, y1, z0, s);
        fill(x0, y0, z1, x1, y1, z1, s);
        fill(x0, y0, z0, x0, y1, z1, s);
        fill(x1, y0, z0, x1, y1, z1, s);
    }

    public void walls(int x0, int y0, int z0, int x1, int y1, int z1, Block b) { walls(x0, y0, z0, x1, y1, z1, b.defaultBlockState()); }

    public void air(int x0, int y0, int z0, int x1, int y1, int z1) { fill(x0, y0, z0, x1, y1, z1, Blocks.AIR); }

    public void door(int x, int y, int z, Block door, Direction facing) {
        BlockState s = door.defaultBlockState().setValue(DoorBlock.FACING, facing);
        set(x, y, z, s.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        set(x, y + 1, z, s.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }

    /** bed whose head lies at (x,z) and points towards facing */
    public void bed(int x, int y, int z, Block bed, Direction facing) {
        BlockState s = bed.defaultBlockState().setValue(BedBlock.FACING, facing);
        set(x, y, z, s.setValue(BedBlock.PART, BedPart.HEAD));
        set(x - facing.getStepX(), y, z - facing.getStepZ(), s.setValue(BedBlock.PART, BedPart.FOOT));
    }

    public BlockState stair(Block b, Direction facing, boolean top) {
        return b.defaultBlockState().setValue(StairBlock.FACING, facing).setValue(StairBlock.HALF, top ? Half.TOP : Half.BOTTOM);
    }

    public BlockState slab(Block b, boolean top) { return b.defaultBlockState().setValue(SlabBlock.TYPE, top ? SlabType.TOP : SlabType.BOTTOM); }

    public void sign(int x, int y, int z, Block sign, int rotation, String... lines) {
        set(x, y, z, sign.defaultBlockState().setValue(StandingSignBlock.ROTATION, rotation));
        BlockEntity be = level.getBlockEntity(new BlockPos(x, y, z));
        if (be instanceof SignBlockEntity s) {
            SignText t = new SignText();
            for (int k = 0; k < Math.min(4, lines.length); k++) t = t.setMessage(k, Component.literal(lines[k]));
            s.setText(t, true);
            s.setText(t, false);
            s.setChanged();
        }
    }

    /** let fences / panes / walls / stairs connect to their neighbours */
    public void finish() {
        for (BlockPos pos : reshape) {
            BlockState s = level.getBlockState(pos);
            BlockState n = Block.updateFromNeighbourShapes(s, level, pos);
            if (n != s) level.setBlock(pos, n, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
        reshape.clear();
    }
}
