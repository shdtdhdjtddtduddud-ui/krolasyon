package com.krolasyon.skyislands.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The "tree of life" crowning large islands: a flared trunk with surface roots, upward-curving branches and
 * fluffy leaf clouds. Evaluated per block so it can be generated chunk by chunk.
 */
public final class GiantTree {
    private GiantTree() {}

    private record Capsule(double ax, double ay, double az, double bx, double by, double bz, double r0, double r1) {
        /** Returns the capsule radius at the closest point if inside, otherwise -1. */
        boolean contains(double x, double y, double z) {
            double vx = bx - ax, vy = by - ay, vz = bz - az;
            double wx = x - ax, wy = y - ay, wz = z - az;
            double t = Math.max(0, Math.min(1, (wx * vx + wy * vy + wz * vz) / (vx * vx + vy * vy + vz * vz)));
            double dx = wx - vx * t, dy = wy - vy * t, dz = wz - vz * t;
            double r = r0 + (r1 - r0) * t;
            return dx * dx + dy * dy + dz * dz <= r * r;
        }
    }

    private record Blob(double x, double y, double z, double r) {}

    public static void build(Island is, BlockSink sink) {
        Random rnd = new Random(is.seed() ^ 0x5DEECE66DL);
        int gx = is.cx(), gz = is.cz();
        IslandShape.Column base = IslandShape.column(is, gx, gz);
        if (!base.inside()) return;
        int g0 = base.top() + 1;
        int height = 24 + is.radius() / 3 + rnd.nextInt(8);
        long s = is.seed() + 101;

        List<Capsule> branches = new ArrayList<>();
        List<Capsule> roots = new ArrayList<>();
        List<Blob> blobs = new ArrayList<>();
        int nb = 6 + rnd.nextInt(3);
        for (int i = 0; i < nb; i++) {
            double hb = height * (0.5 + 0.45 * rnd.nextDouble());
            double ang = i * Math.PI * 2 / nb + rnd.nextDouble() * 0.6;
            double len = 8 + rnd.nextDouble() * 7;
            double rise = 3 + rnd.nextDouble() * 4;
            double ex = gx + Math.cos(ang) * len, ey = g0 + hb + rise, ez = gz + Math.sin(ang) * len;
            branches.add(new Capsule(gx + 0.5, g0 + hb, gz + 0.5, ex, ey, ez, 1.6, 0.8));
            blobs.add(new Blob(ex, ey + 1, ez, 5.5 + rnd.nextDouble() * 2.5));
        }
        blobs.add(new Blob(gx + 0.5, g0 + height + 1, gz + 0.5, 9.5));
        blobs.add(new Blob(gx + 0.5 + rnd.nextDouble() * 4 - 2, g0 + height - 3, gz + 0.5 + rnd.nextDouble() * 4 - 2, 8.0));
        for (int i = 0; i < 6; i++) {
            double ang = i * Math.PI / 3 + rnd.nextDouble() * 0.5;
            double len = 5 + rnd.nextDouble() * 4;
            roots.add(new Capsule(gx + 0.5, g0 + 1.5, gz + 0.5, gx + 0.5 + Math.cos(ang) * len, g0 - 1.2, gz + 0.5 + Math.sin(ang) * len, 1.2, 0.5));
        }

        BlockState log = Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        BlockState wood = Blocks.OAK_WOOD.defaultBlockState();
        BlockState leaves = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        BlockState leaves2 = Blocks.AZALEA_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);

        int span = 27;
        int x0 = Math.max(sink.minX(), gx - span), x1 = Math.min(sink.maxX(), gx + span);
        int z0 = Math.max(sink.minZ(), gz - span), z1 = Math.min(sink.maxZ(), gz + span);
        int y0 = Math.max(sink.minY(), g0 - 4), y1 = Math.min(sink.maxY(), g0 + height + 14);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                for (int y = y0; y <= y1; y++) {
                    double px = x + 0.5, py = y + 0.5, pz = z + 0.5;
                    int dy = y - g0;
                    // gövde (tabanda genişleyen, hafifçe kıvrılan)
                    if (dy >= -3 && dy <= height) {
                        double h = Math.max(0, dy) / (double) height;
                        double wx = Math.sin(dy * 0.18 + s % 7) * 0.9, wz = Math.cos(dy * 0.15 + s % 5) * 0.9;
                        double rad = 1.8 + 1.6 * (1 - h) + 3.0 * Math.exp(-(dy + 1) / 2.5);
                        double ddx = px - (gx + 0.5 + wx * h), ddz = pz - (gz + 0.5 + wz * h);
                        if (ddx * ddx + ddz * ddz <= rad * rad) {
                            sink.set(p.set(x, y, z), log);
                            continue;
                        }
                    }
                    boolean woody = false;
                    for (Capsule c : branches) if (c.contains(px, py, pz)) { woody = true; break; }
                    if (!woody && dy <= 2) for (Capsule c : roots) if (c.contains(px, py, pz)) { woody = true; break; }
                    if (woody) {
                        sink.set(p.set(x, y, z), wood);
                        continue;
                    }
                    if (dy < 4) continue;
                    for (Blob b : blobs) {
                        double ex = px - b.x(), ey = (py - b.y()) * 1.5, ez = pz - b.z();
                        double q = (ex * ex + ey * ey + ez * ez) / (b.r() * b.r());
                        if (q > 1.3) continue;
                        double n = Noise.fbm3(s, px * 0.35, py * 0.35, pz * 0.35, 2);
                        if (q < 0.62 + 0.55 * n) {
                            p.set(x, y, z);
                            if (sink.isAir(p)) sink.set(p, Noise.hash(s, x, y, z) < 0.12 ? leaves2 : leaves);
                            break;
                        }
                    }
                }
            }
        }
    }
}
