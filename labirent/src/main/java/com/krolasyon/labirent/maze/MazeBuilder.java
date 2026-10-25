package com.krolasyon.labirent.maze;

import com.krolasyon.labirent.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.function.Consumer;

/**
 * Spreads huge block edits over many ticks. Jobs are generators of block placements; the builder runs them
 * in order within a per-tick time budget so the server never freezes.
 */
public final class MazeBuilder {
    public interface Job {
        /** place blocks until budget runs out; return true when finished */
        boolean run(ServerLevel level, Budget budget);
    }

    public static final class Budget {
        private final long deadline;

        Budget(long nanos) { this.deadline = System.nanoTime() + nanos; }

        public boolean ok() { return System.nanoTime() < deadline; }
    }

    private final ArrayDeque<Job> jobs = new ArrayDeque<>();
    private int completed;
    private int total;

    public void add(Job j) { jobs.add(j); total++; }

    /** run a one-off action in sequence with the queued jobs */
    public void then(Consumer<ServerLevel> action) {
        add((level, b) -> { action.accept(level); return true; });
    }

    public boolean idle() { return jobs.isEmpty(); }

    public float progress() { return total == 0 ? 1F : completed / (float) total; }

    public void clear() { jobs.clear(); completed = total = 0; }

    public void tick(ServerLevel level, long nanos) {
        Budget b = new Budget(nanos);
        while (!jobs.isEmpty()) {
            Job j = jobs.peek();
            boolean done;
            try {
                done = j.run(level, b);
            } catch (RuntimeException e) {
                com.mojang.logging.LogUtils.getLogger().error("Labirent build job failed", e);
                done = true;
            }
            if (done) {
                jobs.poll();
                completed++;
                if (jobs.isEmpty()) completed = total = 0;
            } else {
                return;
            }
        }
    }

    // ------------------------------------------------------------------ block palette
    public static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    static int hash(int x, int y, int z) {
        int h = x * 374761393 + y * 668265263 + z * 1274126177;
        h = (h ^ (h >>> 13)) * 1274126177;
        return h ^ (h >>> 16);
    }

    static float rand01(int x, int y, int z) { return (hash(x, y, z) & 0xFFFF) / 65535F; }

    /** smooth value noise in [0,1] */
    static float noise(float x, float z, int salt) {
        int x0 = (int) Math.floor(x), z0 = (int) Math.floor(z);
        float fx = x - x0, fz = z - z0;
        fx = fx * fx * (3 - 2 * fx);
        fz = fz * fz * (3 - 2 * fz);
        float a = rand01(x0, salt, z0), b = rand01(x0 + 1, salt, z0), c = rand01(x0, salt, z0 + 1), d = rand01(x0 + 1, salt, z0 + 1);
        return (a * (1 - fx) + b * fx) * (1 - fz) + (c * (1 - fx) + d * fx) * fz;
    }

    /** wall block for a position: ivy hangs from the top, moss creeps up from the floor */
    public static BlockState wallState(int x, int y, int z) {
        int top = MazeLayout.GROUND + MazeLayout.H;
        float n = noise(x / 3.3F + z / 7.1F, z / 3.3F - x / 9.7F, 11) * 0.7F + noise(x / 1.4F, z / 1.4F, 12) * 0.3F;
        int hang = (int) (n * n * MazeLayout.H * 1.25F);
        if (top - y < hang) return ModBlocks.MAZE_WALL_IVY.get().defaultBlockState();
        float m = noise(x / 4F, z / 4F, 13);
        int moss = (int) (m * 6) - 1;
        if (y - MazeLayout.GROUND <= moss) return ModBlocks.MAZE_WALL_MOSSY.get().defaultBlockState();
        float r = rand01(x, y, z);
        if (r < 0.07F) return ModBlocks.MAZE_WALL_CRACKED.get().defaultBlockState();
        if (r < 0.14F) return ModBlocks.MAZE_WALL_MOSSY.get().defaultBlockState();
        return ModBlocks.MAZE_WALL.get().defaultBlockState();
    }

    public static BlockState floorState(int x, int z) {
        float n = noise(x / 5F, z / 5F, 21);
        float r = rand01(x, 1, z);
        if (n > 0.68F || r < 0.04F) return ModBlocks.MAZE_FLOOR_MOSSY.get().defaultBlockState();
        if (n < 0.18F && r < 0.5F) return Blocks.COARSE_DIRT.defaultBlockState();
        return ModBlocks.MAZE_FLOOR.get().defaultBlockState();
    }

    public static BlockState subState(int x, int y, int z) {
        return rand01(x, y, z) < 0.15F ? Blocks.ANDESITE.defaultBlockState() : Blocks.STONE.defaultBlockState();
    }

    // ------------------------------------------------------------------ common jobs
    /** iterate a box of block positions, applying fn per position */
    public static Job box(int x0, int y0, int z0, int x1, int y1, int z1, PosFn fn) {
        return new Job() {
            int x = x0, y = y0, z = z0;
            final BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();

            @Override
            public boolean run(ServerLevel level, Budget b) {
                while (b.ok()) {
                    if (x > x1) return true;
                    fn.apply(level, p.set(x, y, z));
                    if (++y > y1) {
                        y = y0;
                        if (++z > z1) { z = z0; x++; }
                    }
                }
                return x > x1;
            }
        };
    }

    public interface PosFn {
        void apply(ServerLevel level, BlockPos.MutableBlockPos p);
    }

    /** ground under one unit + its wall (if closed) */
    public static void buildUnit(ServerLevel level, MazeLayout layout, int i, int j, BlockPos.MutableBlockPos p) {
        int sx = MazeLayout.start(i), sz = MazeLayout.start(j), w = MazeLayout.width(i), d = MazeLayout.width(j);
        boolean wall = !layout.isOpen(i, j);
        boolean edge = i == 0 || j == 0 || i == MazeLayout.G - 1 || j == MazeLayout.G - 1;
        for (int x = sx; x < sx + w; x++)
            for (int z = sz; z < sz + d; z++) {
                int depth = 4 + (int) (rand01(x, 7, z) * 3);
                if (edge) depth += (int) (noise(x / 3F, z / 3F, 31) * 9);
                for (int y = MazeLayout.GROUND - depth; y < MazeLayout.GROUND; y++) level.setBlock(p.set(x, y, z), subState(x, y, z), FLAGS);
                level.setBlock(p.set(x, MazeLayout.GROUND, z), wall ? ModBlocks.MAZE_WALL.get().defaultBlockState() : floorState(x, z), FLAGS);
                for (int y = MazeLayout.GROUND + 1; y <= MazeLayout.GROUND + MazeLayout.H; y++)
                    level.setBlock(p.set(x, y, z), wall ? wallState(x, y, z) : Blocks.AIR.defaultBlockState(), FLAGS);
            }
    }
}
