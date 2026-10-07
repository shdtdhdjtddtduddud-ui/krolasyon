package com.krolasyon.furniture.block;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class ShapeUtil {
    private ShapeUtil() {}

    /** rotates a shape authored for a NORTH-facing block (front toward -z) to the given facing */
    public static VoxelShape rotate(VoxelShape shape, Direction facing) {
        if (facing == Direction.NORTH) return shape;
        VoxelShape[] out = {Shapes.empty()};
        shape.forAllBoxes((x1, y1, z1, x2, y2, z2) -> {
            double a1, a2, b1, b2;
            switch (facing) {
                case EAST -> { a1 = 1 - z2; a2 = 1 - z1; b1 = x1; b2 = x2; }
                case SOUTH -> { a1 = 1 - x2; a2 = 1 - x1; b1 = 1 - z2; b2 = 1 - z1; }
                default -> { a1 = z1; a2 = z2; b1 = 1 - x2; b2 = 1 - x1; }
            }
            out[0] = Shapes.or(out[0], Shapes.box(a1, y1, b1, a2, y2, b2));
        });
        return out[0].optimize();
    }
}
