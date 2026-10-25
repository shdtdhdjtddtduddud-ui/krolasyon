package com.krolasyon.labirent.maze;

import com.krolasyon.labirent.block.CodeStoneBlock;
import com.krolasyon.labirent.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

/** The Cliff with the Griever Hole, the eight code stones and the hidden WICKED chamber behind the hole. */
public final class Landmarks {
    private Landmarks() {}

    // the Griever Hole floats just past the edge of the Cliff
    public static final int HOLE_X0 = -126, HOLE_X1 = -124, HOLE_Y0 = 61, HOLE_Y1 = 65, HOLE_Z0 = -2, HOLE_Z1 = 1;
    // the chamber far away in the void
    public static final int CX = 400, CY = 60, CZ = 0;
    public static final BlockPos CHAMBER_ARRIVAL = new BlockPos(CX + 3, CY + 1, CZ);
    public static final BlockPos TERMINAL = new BlockPos(CX + 14, CY + 1, CZ);
    public static final int EXIT_X = CX + 16;
    public static final int ESCAPE_X = CX + 42;

    public static void cliff(ServerLevel l) {
        Sculpt s = new Sculpt(l);
        RandomSource r = RandomSource.create(99);
        int edge = MazeLayout.ORIGIN;
        int z0 = MazeLayout.start(MazeLayout.DOOR_MIN) - 3, z1 = MazeLayout.start(MazeLayout.DOOR_MAX) + 6;
        // a rough, crumbling rim
        for (int z = z0; z <= z1; z++) {
            int bite = r.nextFloat() < 0.35F ? 1 : 0;
            if (bite > 0 && z > z0 + 2 && z < z1 - 2) s.air(edge, MazeLayout.GROUND - 8, z, edge, MazeLayout.GROUND, z);
        }
        // the face of the cliff drops away in jagged stone
        for (int x = edge; x <= edge + 8; x++)
            for (int z = z0 - 4; z <= z1 + 4; z++) {
                int depth = 6 + (int) (MazeBuilder.noise(x / 2.5F, z / 2.5F, 77) * 22) - (x - edge);
                for (int y = MazeLayout.GROUND - 6; y > MazeLayout.GROUND - 6 - depth; y--)
                    if (l.getBlockState(new BlockPos(x, MazeLayout.GROUND, z)).isSolid())
                        s.set(x, y, z, r.nextFloat() < 0.2F ? Blocks.TUFF.defaultBlockState() : Blocks.STONE.defaultBlockState());
            }
        s.fill(HOLE_X0, HOLE_Y0, HOLE_Z0, HOLE_X1, HOLE_Y1, HOLE_Z1, ModBlocks.GRIEVER_HOLE.get());
    }

    public static void codeStones(ServerLevel l, MazeState state) {
        Sculpt s = new Sculpt(l);
        for (int sec = 0; sec < MazeLayout.SECTORS; sec++) {
            int[] c = MazeLayout.codeStoneCell(sec);
            int x = MazeLayout.start(c[0]) + 1, z = MazeLayout.start(c[1]) + 1;
            s.set(x, MazeLayout.GROUND + 1, z, ModBlocks.CODE_STONE.get().defaultBlockState().setValue(CodeStoneBlock.SECTOR, sec));
            s.set(x + 1, MazeLayout.GROUND + 1, z + 1, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.LIT, true));
        }
    }

    public static BlockPos codeStonePos(int sec) {
        int[] c = MazeLayout.codeStoneCell(sec);
        return new BlockPos(MazeLayout.start(c[0]) + 1, MazeLayout.GROUND + 1, MazeLayout.start(c[1]) + 1);
    }

    public static void enqueueChamber(MazeBuilder b) {
        b.then(Landmarks::chamber);
        b.then(Landmarks::corridor);
    }

    private static void chamber(ServerLevel l) {
        Sculpt s = new Sculpt(l);
        int x0 = CX, x1 = CX + 16, z0 = CZ - 8, z1 = CZ + 8, y0 = CY, y1 = CY + 8;
        s.fill(x0, y0, z0, x1, y1, z1, ModBlocks.LAB_WALL.get());
        s.air(x0 + 1, y0 + 1, z0 + 1, x1 - 1, y1 - 1, z1 - 1);
        for (int x = x0 + 2; x < x1; x += 4)
            for (int z = z0 + 2; z < z1; z += 4) s.set(x, y1, z, ModBlocks.LAB_LIGHT.get());
        s.fill(x0 + 1, y0, z0 + 1, x1 - 1, y0, z1 - 1, Blocks.POLISHED_DEEPSLATE);
        // Griever pods along the walls
        for (int x = x0 + 3; x < x1 - 2; x += 3) {
            for (int z : new int[]{z0 + 1, z1 - 1}) {
                s.fill(x, y0 + 1, z, x, y0 + 4, z, Blocks.TINTED_GLASS);
                s.set(x, y0 + 5, z, Blocks.IRON_BLOCK);
                s.set(x, y0 + 6, z, Blocks.CHAIN);
            }
        }
        s.set(TERMINAL.getX(), TERMINAL.getY(), TERMINAL.getZ(), ModBlocks.CODE_TERMINAL.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST));
        s.fill(x1 - 1, y0 + 1, z0 + 3, x1 - 1, y0 + 3, z0 + 3, Blocks.IRON_BARS);
        s.fill(x1 - 1, y0 + 1, z1 - 3, x1 - 1, y0 + 3, z1 - 3, Blocks.IRON_BARS);
        s.set(x0 + 1, y0 + 3, CZ, ModBlocks.WICKED_PLATE.get());
        closeExit(l);
        s.finish();
    }

    public static void closeExit(ServerLevel l) {
        Sculpt s = new Sculpt(l);
        s.fill(EXIT_X, CY + 1, CZ - 1, EXIT_X, CY + 3, CZ + 1, ModBlocks.MAZE_DOOR.get());
    }

    public static void openExit(ServerLevel l) {
        Sculpt s = new Sculpt(l);
        s.air(EXIT_X, CY + 1, CZ - 1, EXIT_X, CY + 3, CZ + 1);
    }

    private static void corridor(ServerLevel l) {
        Sculpt s = new Sculpt(l);
        int x0 = EXIT_X, x1 = ESCAPE_X - 1;
        s.fill(x0, CY, CZ - 2, x1, CY + 5, CZ + 2, ModBlocks.LAB_WALL.get());
        s.air(x0 + 1, CY + 1, CZ - 1, x1, CY + 3, CZ + 1);
        for (int x = x0 + 3; x < x1; x += 5) s.set(x, CY + 4, CZ, ModBlocks.LAB_LIGHT.get());
        // the observation room at the end
        int ex0 = ESCAPE_X, ex1 = ESCAPE_X + 14;
        s.fill(ex0, CY, CZ - 8, ex1, CY + 9, CZ + 8, ModBlocks.LAB_WALL.get());
        s.air(ex0 + 1, CY + 1, CZ - 7, ex1 - 1, CY + 8, CZ + 7);
        s.air(ex0, CY + 1, CZ - 1, ex0, CY + 3, CZ + 1);
        s.fill(ex1, CY + 2, CZ - 6, ex1, CY + 7, CZ + 6, Blocks.GLASS);
        for (int x = ex0 + 2; x < ex1; x += 4)
            for (int z = CZ - 6; z <= CZ + 6; z += 4) s.set(x, CY + 9, z, ModBlocks.LAB_LIGHT.get());
        s.fill(ex0 + 2, CY + 4, CZ - 7, ex0 + 4, CY + 6, CZ - 7, ModBlocks.WICKED_PLATE.get());
        s.fill(ex0 + 2, CY + 4, CZ + 7, ex0 + 4, CY + 6, CZ + 7, ModBlocks.WICKED_PLATE.get());
        for (int z = CZ - 5; z <= CZ + 5; z += 2) {
            s.set(ex1 - 3, CY + 1, z, Blocks.SMOOTH_QUARTZ_STAIRS.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.StairBlock.FACING, Direction.WEST));
        }
        s.finish();
    }
}
