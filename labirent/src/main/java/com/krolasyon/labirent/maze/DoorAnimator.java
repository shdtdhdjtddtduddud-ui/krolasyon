package com.krolasyon.labirent.maze;

import com.krolasyon.labirent.net.Net;
import com.krolasyon.labirent.registry.ModBlocks;
import com.krolasyon.labirent.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** The four giant doors: two halves slide in from the sides at dusk and part again at dawn. */
public final class DoorAnimator {
    public static final int STEPS = 11;               // 22 columns, two per step
    private static final int COLS = 22;
    private int step = -1;                            // 0 = open, STEPS = closed, -1 = unknown
    private int timer;

    public int step() { return Math.max(0, step); }

    public boolean closed() { return step >= STEPS; }

    public boolean open() { return step == 0; }

    /** block position of column c, thickness t (0/1) of door d at height y */
    private static BlockPos pos(int d, int c, int t, int y) {
        int along = MazeLayout.start(MazeLayout.DOOR_MIN) + c;
        return switch (d) {
            case MazeLayout.DOOR_N -> new BlockPos(along, y, MazeLayout.start(MazeLayout.GLADE_MIN) + t);
            case MazeLayout.DOOR_S -> new BlockPos(along, y, MazeLayout.start(MazeLayout.GLADE_MAX) + t);
            case MazeLayout.DOOR_W -> new BlockPos(MazeLayout.start(MazeLayout.GLADE_MIN) + t, y, along);
            default -> new BlockPos(MazeLayout.start(MazeLayout.GLADE_MAX) + t, y, along);
        };
    }

    public static BlockPos doorCenter(int d) { return pos(d, COLS / 2, 0, MazeLayout.GROUND + 4); }

    public void tick(ServerLevel level, boolean wantClosed) {
        if (step < 0) step = level.getBlockState(pos(0, 0, 0, MazeLayout.GROUND + 2)).is(ModBlocks.MAZE_DOOR.get()) ? STEPS : 0;
        if (wantClosed && step < STEPS) {
            if (step == 0 && timer == 0) {
                for (int d = 0; d < 4; d++) sound(level, d, ModSounds.DOOR_CLOSE.get(), 6F);
                timer = 30;  // the grind starts before anything moves
            }
            if (--timer > 0) return;
            timer = 14;
            setColumns(level, step, true);
            step++;
            if (step == STEPS) {
                for (int d = 0; d < 4; d++) {
                    sound(level, d, ModSounds.DOOR_SLAM.get(), 8F);
                    shake(level, d, 1.0F);
                }
            } else {
                for (int d = 0; d < 4; d++) shake(level, d, 0.25F);
            }
        } else if (!wantClosed && step > 0) {
            if (step == STEPS && timer == 0) {
                for (int d = 0; d < 4; d++) sound(level, d, ModSounds.DOOR_OPEN.get(), 6F);
                timer = 30;
            }
            if (--timer > 0) return;
            timer = 12;
            step--;
            setColumns(level, step, false);
            for (int d = 0; d < 4; d++) shake(level, d, 0.2F);
        } else {
            timer = 0;
        }
    }

    /** close or open the two columns belonging to step k on every door */
    private void setColumns(ServerLevel level, int k, boolean close) {
        int[] cols = {k, COLS - 1 - k};
        for (int d = 0; d < 4; d++) {
            for (int c : cols) {
                if (close) clearEntities(level, d, c);
                for (int t = 0; t < 2; t++)
                    for (int y = MazeLayout.GROUND + 1; y <= MazeLayout.GROUND + MazeLayout.H; y++) {
                        BlockState s = close ? ModBlocks.MAZE_DOOR.get().defaultBlockState() : Blocks.AIR.defaultBlockState();
                        level.setBlock(pos(d, c, t, y), s, MazeBuilder.FLAGS);
                    }
                BlockPos base = pos(d, c, 0, MazeLayout.GROUND + 1);
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ModBlocks.MAZE_WALL.get().defaultBlockState()),
                        base.getX() + 0.5, base.getY() + 0.3, base.getZ() + 0.5, 14, 0.8, 0.3, 0.8, 0.05);
                level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, base.getX() + 0.5, base.getY() + 0.2, base.getZ() + 0.5, 2, 0.6, 0.1, 0.6, 0.01);
                level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, ModBlocks.MAZE_WALL_IVY.get().defaultBlockState()),
                        base.getX() + 0.5, base.getY() + MazeLayout.H - 2, base.getZ() + 0.5, 6, 0.6, 1.5, 0.6, 0.0);
            }
        }
    }

    /** whoever stands in a closing column is shoved out: Gladers into the Glade, monsters into the maze */
    private void clearEntities(ServerLevel level, int d, int c) {
        BlockPos a = pos(d, c, 0, MazeLayout.GROUND + 1), b = pos(d, c, 1, MazeLayout.GROUND + 4);
        AABB box = new AABB(a, b.offset(1, 1, 1)).inflate(0.3);
        for (Entity e : level.getEntitiesOfClass(Entity.class, box, en -> en instanceof LivingEntity && en.isAlive())) {
            boolean monster = e instanceof Enemy;
            double sign = monster ? -1 : 1; // towards the glade centre (+1) or away (-1)
            double nx = 0, nz = 0;
            switch (d) {
                case MazeLayout.DOOR_N -> nz = 1;
                case MazeLayout.DOOR_S -> nz = -1;
                case MazeLayout.DOOR_W -> nx = 1;
                default -> nx = -1;
            }
            e.teleportTo(e.getX() + nx * sign * 3, e.getY(), e.getZ() + nz * sign * 3);
        }
    }

    private static void sound(ServerLevel level, int d, net.minecraft.sounds.SoundEvent ev, float vol) {
        BlockPos c = doorCenter(d);
        level.playSound(null, c, ev, SoundSource.BLOCKS, vol, 1F);
    }

    private static void shake(ServerLevel level, int d, float strength) {
        BlockPos c = doorCenter(d);
        for (ServerPlayer p : level.players()) {
            double dist = p.distanceToSqr(c.getX(), c.getY(), c.getZ());
            if (dist < 50 * 50) Net.shake(p, strength * (float) (1 - Math.sqrt(dist) / 50), 20);
        }
    }
}
