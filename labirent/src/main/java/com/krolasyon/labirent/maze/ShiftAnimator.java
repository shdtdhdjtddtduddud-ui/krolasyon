package com.krolasyon.labirent.maze;

import com.krolasyon.labirent.net.Net;
import com.krolasyon.labirent.registry.ModBlocks;
import com.krolasyon.labirent.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/** The nightly rearrangement: walls of a few sections sink into the floor while new ones grind up. */
public final class ShiftAnimator {
    private static final int TICKS_PER_LAYER = 5;
    private List<int[]> rising = List.of();
    private List<int[]> sinking = List.of();
    private int layer = -1;
    private int timer;
    private MazeLayout target;
    private int sectorMask;

    public boolean active() { return layer >= 0; }

    public int sectorMask() { return sectorMask; }

    public void start(ServerLevel level, MazeState state, int night, int sectorCount) {
        RandomSource r = RandomSource.create(state.seed ^ (night * 0x9E3779B97F4A7C15L));
        boolean[] sectors = new boolean[MazeLayout.SECTORS];
        sectorMask = 0;
        int n = Math.min(MazeLayout.SECTORS, sectorCount);
        while (Integer.bitCount(sectorMask) < n) {
            int s = r.nextInt(MazeLayout.SECTORS);
            sectors[s] = true;
            sectorMask |= 1 << s;
        }
        MazeLayout old = state.layout;
        target = old.shift(r.nextLong(), sectors);
        rising = new ArrayList<>();
        sinking = new ArrayList<>();
        for (int i = 0; i < MazeLayout.G; i++)
            for (int j = 0; j < MazeLayout.G; j++) {
                boolean a = old.isOpen(i, j), b = target.isOpen(i, j);
                if (a && !b) rising.add(new int[]{i, j});
                else if (!a && b) sinking.add(new int[]{i, j});
            }
        // commit right away so a restart mid-animation can finish the job from the saved layout
        state.layout = target;
        state.shiftPendingMask = sectorMask;
        state.lastShiftNight = night;
        state.shiftCount++;
        state.shiftedSectorsMask = sectorMask;
        for (int i = 0; i < MazeLayout.G; i++)
            for (int j = 0; j < MazeLayout.G; j++) {
                int s = MazeLayout.sector(i, j);
                if (s >= 0 && (sectorMask & (1 << s)) != 0) state.known[MazeLayout.idx(i, j)] &= ~4;
            }
        state.setDirty();
        layer = 0;
        timer = 0;
        for (int[] u : rising) clearUnit(level, u[0], u[1]);
        StringBuilder sb = new StringBuilder();
        for (int s = 0; s < MazeLayout.SECTORS; s++) if (sectors[s]) sb.append(sb.length() > 0 ? ", " : "").append(s + 1);
        for (ServerPlayer p : level.players())
            p.displayClientMessage(Component.translatable("msg.labirent.shift", sb.toString()), true);
    }

    public void tick(ServerLevel level, MazeState state) {
        if (layer < 0) return;
        if (timer++ % TICKS_PER_LAYER != 0) return;
        int g = MazeLayout.GROUND;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int yUp = g + 1 + layer, yDown = g + MazeLayout.H - layer;
        for (int[] u : rising) {
            int sx = MazeLayout.start(u[0]), sz = MazeLayout.start(u[1]);
            for (int x = sx; x < sx + MazeLayout.width(u[0]); x++)
                for (int z = sz; z < sz + MazeLayout.width(u[1]); z++) level.setBlock(p.set(x, yUp, z), MazeBuilder.wallState(x, yUp, z), MazeBuilder.FLAGS);
        }
        for (int[] u : sinking) {
            int sx = MazeLayout.start(u[0]), sz = MazeLayout.start(u[1]);
            for (int x = sx; x < sx + MazeLayout.width(u[0]); x++)
                for (int z = sz; z < sz + MazeLayout.width(u[1]); z++) {
                    level.setBlock(p.set(x, yDown, z), Blocks.AIR.defaultBlockState(), MazeBuilder.FLAGS);
                    if (layer == MazeLayout.H - 1) level.setBlock(p.set(x, g, z), MazeBuilder.floorState(x, z), MazeBuilder.FLAGS);
                }
        }
        effects(level);
        layer++;
        if (layer >= MazeLayout.H) {
            layer = -1;
            state.shiftPendingMask = 0;
            state.setDirty();
        }
    }

    private void effects(ServerLevel level) {
        RandomSource r = level.random;
        List<List<int[]>> both = List.of(rising, sinking);
        for (List<int[]> list : both) {
            if (list.isEmpty()) continue;
            for (int k = 0; k < 6; k++) {
                int[] u = list.get(r.nextInt(list.size()));
                double x = MazeLayout.mid(u[0]), z = MazeLayout.mid(u[1]);
                double y = list == rising ? MazeLayout.GROUND + 1 + layer : MazeLayout.GROUND + MazeLayout.H - layer;
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ModBlocks.MAZE_WALL.get().defaultBlockState()), x, y, z, 6, 1, 0.2, 1, 0.05);
                level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, MazeLayout.GROUND + 1.2, z, 1, 0.8, 0.1, 0.8, 0.005);
            }
        }
        if (layer % 6 == 0) {
            for (ServerPlayer pl : level.players()) {
                int[] near = nearest(pl);
                if (near == null) continue;
                double dx = MazeLayout.mid(near[0]) - pl.getX(), dz = MazeLayout.mid(near[1]) - pl.getZ();
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist < 70) {
                    level.playSound(null, MazeLayout.mid(near[0]), MazeLayout.GROUND + 3, MazeLayout.mid(near[1]),
                            ModSounds.WALL_GRIND.get(), SoundSource.BLOCKS, 4F, 0.85F + r.nextFloat() * 0.3F);
                    Net.shake(pl, (float) Math.max(0.05, 0.5 * (1 - dist / 70)), 34);
                } else if (layer == 0) {
                    pl.playNotifySound(ModSounds.WALL_GRIND.get(), SoundSource.AMBIENT, 0.35F, 0.6F);
                }
            }
        }
    }

    private int[] nearest(ServerPlayer p) {
        int[] best = null;
        double bd = Double.MAX_VALUE;
        for (List<int[]> list : List.of(rising, sinking))
            for (int[] u : list) {
                double dx = MazeLayout.mid(u[0]) - p.getX(), dz = MazeLayout.mid(u[1]) - p.getZ();
                double d = dx * dx + dz * dz;
                if (d < bd) { bd = d; best = u; }
            }
        return best;
    }

    /** push anything standing where a wall is about to rise into the nearest open neighbour */
    private void clearUnit(ServerLevel level, int i, int j) {
        int sx = MazeLayout.start(i), sz = MazeLayout.start(j);
        AABB box = new AABB(sx, MazeLayout.GROUND + 1, sz, sx + MazeLayout.width(i), MazeLayout.GROUND + MazeLayout.H, sz + MazeLayout.width(j));
        List<Entity> list = level.getEntitiesOfClass(Entity.class, box, e -> e instanceof LivingEntity && e.isAlive());
        if (list.isEmpty()) return;
        int[][] d = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] o : d) {
            int ni = i + o[0], nj = j + o[1];
            if (target.isOpen(ni, nj)) {
                for (Entity e : list) e.teleportTo(MazeLayout.mid(ni), MazeLayout.GROUND + 1, MazeLayout.mid(nj));
                return;
            }
        }
    }

    /** instant rebuild of whole sectors (used after a restart interrupted an animation) */
    public static void rebuildSectors(MazeBuilder b, MazeState state, int mask) {
        List<int[]> units = new ArrayList<>();
        for (int i = 0; i < MazeLayout.G; i++)
            for (int j = 0; j < MazeLayout.G; j++) {
                int s = MazeLayout.sector(i, j);
                if (s >= 0 && (mask & (1 << s)) != 0) units.add(new int[]{i, j});
            }
        b.add(new MazeBuilder.Job() {
            int k;
            final BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();

            @Override
            public boolean run(ServerLevel level, MazeBuilder.Budget budget) {
                while (k < units.size() && budget.ok()) {
                    int[] u = units.get(k++);
                    MazeBuilder.buildUnit(level, state.layout, u[0], u[1], p);
                }
                return k >= units.size();
            }
        });
    }
}
