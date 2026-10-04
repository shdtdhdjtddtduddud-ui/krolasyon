package com.krolasyon.bosses.rpg.town;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;

import java.util.List;
import java.util.function.Consumer;

/**
 * A rotated drawing surface. Local coordinates: +x right, +y up, -z front (the side with the door).
 * Every write becomes a queued operation so huge settlements can be built over many ticks.
 */
public class Canvas {
    public sealed interface Op permits SetOp, ActOp {}
    public record SetOp(long pos, BlockState state) implements Op {}
    public record ActOp(Consumer<ServerLevel> act) implements Op {}

    private final List<Op> ops;
    public final int ox, oy, oz, rot, sx, sz;

    public Canvas(List<Op> ops, int ox, int oy, int oz, int rot, int sx, int sz) {
        this.ops = ops;
        this.ox = ox; this.oy = oy; this.oz = oz;
        this.rot = Math.floorMod(rot, 4);
        this.sx = sx; this.sz = sz;
    }

    /** a canvas centred on (cx, cz) for a footprint of w x d */
    public static Canvas centred(List<Op> ops, int cx, int y, int cz, int rot, int w, int d) {
        return new Canvas(ops, cx, y, cz, rot, -w / 2, -d / 2);
    }

    public Rotation rotation() {
        return switch (rot) { case 1 -> Rotation.CLOCKWISE_90; case 2 -> Rotation.CLOCKWISE_180; case 3 -> Rotation.COUNTERCLOCKWISE_90; default -> Rotation.NONE; };
    }

    public BlockPos world(int x, int y, int z) {
        int lx = x + sx, lz = z + sz;
        int wx, wz;
        switch (rot) {
            case 1 -> { wx = -lz; wz = lx; }
            case 2 -> { wx = -lx; wz = -lz; }
            case 3 -> { wx = lz; wz = -lx; }
            default -> { wx = lx; wz = lz; }
        }
        return new BlockPos(ox + wx, oy + y, oz + wz);
    }

    /** a world direction for a local one */
    public Direction dir(Direction local) { return rotation().rotate(local); }

    public void set(int x, int y, int z, BlockState s) {
        ops.add(new SetOp(world(x, y, z).asLong(), s.rotate(rotation())));
    }

    public void set(int x, int y, int z, Block b) { set(x, y, z, b.defaultBlockState()); }

    public void act(Consumer<ServerLevel> a) { ops.add(new ActOp(a)); }

    public void fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState s) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) set(x, y, z, s);
    }

    public void fill(int x1, int y1, int z1, int x2, int y2, int z2, Block b) { fill(x1, y1, z1, x2, y2, z2, b.defaultBlockState()); }

    public void air(int x1, int y1, int z1, int x2, int y2, int z2) { fill(x1, y1, z1, x2, y2, z2, Blocks.AIR); }

    /** four walls of a box (no floor / ceiling) */
    public void walls(int x1, int y1, int z1, int x2, int y2, int z2, Block b) {
        fill(x1, y1, z1, x2, y2, z1, b);
        fill(x1, y1, z2, x2, y2, z2, b);
        fill(x1, y1, z1, x1, y2, z2, b);
        fill(x2, y1, z1, x2, y2, z2, b);
    }

    public void stairs(int x, int y, int z, Block b, Direction facing, boolean top) {
        BlockState s = b.defaultBlockState();
        if (s.hasProperty(StairBlock.FACING)) s = s.setValue(StairBlock.FACING, facing);
        if (s.hasProperty(StairBlock.HALF)) s = s.setValue(StairBlock.HALF, top ? Half.TOP : Half.BOTTOM);
        set(x, y, z, s);
    }

    public void slab(int x, int y, int z, Block b, boolean top) {
        BlockState s = b.defaultBlockState();
        if (s.hasProperty(SlabBlock.TYPE)) s = s.setValue(SlabBlock.TYPE, top ? net.minecraft.world.level.block.state.properties.SlabType.TOP : net.minecraft.world.level.block.state.properties.SlabType.BOTTOM);
        set(x, y, z, s);
    }

    public void door(int x, int y, int z, Block b, Direction facing) {
        BlockState s = b.defaultBlockState();
        if (!s.hasProperty(DoorBlock.HALF)) { set(x, y, z, s); return; }
        s = s.setValue(DoorBlock.FACING, facing);
        set(x, y, z, s.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        set(x, y + 1, z, s.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }

    /** bed with its foot at (x,z) pointing toward facing */
    public void bed(int x, int y, int z, Block b, Direction facing) {
        BlockState s = b.defaultBlockState();
        if (!s.hasProperty(BedBlock.PART)) return;
        s = s.setValue(BedBlock.FACING, facing);
        set(x, y, z, s.setValue(BedBlock.PART, BedPart.FOOT));
        set(x + facing.getStepX(), y, z + facing.getStepZ(), s.setValue(BedBlock.PART, BedPart.HEAD));
    }

    public void facing(int x, int y, int z, Block b, Direction facing) {
        BlockState s = b.defaultBlockState();
        if (s.hasProperty(HorizontalDirectionalBlock.FACING)) s = s.setValue(HorizontalDirectionalBlock.FACING, facing);
        set(x, y, z, s);
    }

    public void hanging(int x, int y, int z, Block lantern) {
        BlockState s = lantern.defaultBlockState();
        if (s.hasProperty(LanternBlock.HANGING)) s = s.setValue(LanternBlock.HANGING, true);
        set(x, y, z, s);
    }

    /** pillar of logs standing upright */
    public void pillar(int x, int y1, int y2, int z, Block b) {
        BlockState s = b.defaultBlockState();
        if (s.hasProperty(RotatedPillarBlock.AXIS)) s = s.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        fill(x, y1, z, x, y2, z, s);
    }
}
