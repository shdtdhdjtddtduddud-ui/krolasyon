package com.krolasyon.elvenrealm.world;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Kadim Ağaç: kökleri toprağa yayılan, gövdesi hafifçe kıvrılan, dallarının uçlarında devasa yaprak bulutları
 * taşıyan dev ağaç. İlkel şekiller (konik kapsüller ve yassı küreler) olarak tanımlanır, her chunk kendi
 * kesitini rasterleştirir.
 */
public final class GiantTree {
    /** a→b doğru parçası, yarıçap ra→rb. */
    record Capsule(double ax, double ay, double az, double bx, double by, double bz, double ra, double rb, boolean root) {
        double minX() { return Math.min(ax, bx) - Math.max(ra, rb) - 1; }
        double maxX() { return Math.max(ax, bx) + Math.max(ra, rb) + 1; }
        double minZ() { return Math.min(az, bz) - Math.max(ra, rb) - 1; }
        double maxZ() { return Math.max(az, bz) + Math.max(ra, rb) + 1; }
        double minY() { return Math.min(ay, by) - Math.max(ra, rb) - 1; }
        double maxY() { return Math.max(ay, by) + Math.max(ra, rb) + 1; }

        /** Noktanın kapsül içinde olup olmadığı (pozitifse içeride). */
        double inside(double x, double y, double z) {
            double dx = bx - ax, dy = by - ay, dz = bz - az;
            double l2 = dx * dx + dy * dy + dz * dz;
            double t = l2 == 0 ? 0 : ((x - ax) * dx + (y - ay) * dy + (z - az) * dz) / l2;
            t = Math.max(0, Math.min(1, t));
            double px = ax + dx * t - x, py = ay + dy * t - y, pz = az + dz * t - z;
            double r = ra + (rb - ra) * t;
            return r - Math.sqrt(px * px + py * py + pz * pz);
        }
    }

    record Blob(double x, double y, double z, double r, double squash) {}

    public final long seed;
    public final int x, y, z;
    final List<Capsule> wood = new ArrayList<>();
    final List<Blob> leaves = new ArrayList<>();
    public final int minX, maxX, minZ, maxZ, minY, maxY;

    public GiantTree(long seed, int x, int y, int z) {
        this.seed = seed;
        this.x = x;
        this.y = y;
        this.z = z;
        Random r = new Random(seed);
        int height = 58 + r.nextInt(10);
        // gövde: kıvrımlı, aşağıda geniş taban
        double px = x + 0.5, py = y - 4, pz = z + 0.5;
        double driftX = (r.nextDouble() - 0.5) * 0.5, driftZ = (r.nextDouble() - 0.5) * 0.5;
        int segs = 10;
        double[][] spine = new double[segs + 1][];
        spine[0] = new double[]{px, py, pz};
        for (int i = 1; i <= segs; i++) {
            double t = i / (double) segs;
            px += driftX * 6 + Math.sin(t * 5 + r.nextDouble()) * 1.2;
            pz += driftZ * 6 + Math.cos(t * 4 + r.nextDouble()) * 1.2;
            py += height / (double) segs;
            spine[i] = new double[]{px, py, pz};
        }
        for (int i = 0; i < segs; i++) {
            double t0 = i / (double) segs, t1 = (i + 1) / (double) segs;
            wood.add(new Capsule(spine[i][0], spine[i][1], spine[i][2], spine[i + 1][0], spine[i + 1][1], spine[i + 1][2],
                    trunkR(t0), trunkR(t1), false));
        }
        // kökler
        int roots = 9 + r.nextInt(3);
        for (int i = 0; i < roots; i++) {
            double a = i * Math.PI * 2 / roots + r.nextDouble() * 0.4;
            double len = 13 + r.nextDouble() * 9;
            double mx = x + 0.5 + Math.cos(a) * len * 0.5, mz = z + 0.5 + Math.sin(a) * len * 0.5;
            double ex = x + 0.5 + Math.cos(a) * len, ez = z + 0.5 + Math.sin(a) * len;
            double sy = y + 5 + r.nextDouble() * 4;
            wood.add(new Capsule(x + 0.5 + Math.cos(a) * 4, sy, z + 0.5 + Math.sin(a) * 4, mx, y + 1.5 + r.nextDouble() * 1.5, mz, 3.6, 2.3, true));
            wood.add(new Capsule(mx, y + 1.5, mz, ex, y - 1.5, ez, 2.3, 0.9, true));
        }
        // dallar ve yaprak bulutları
        int branches = 8 + r.nextInt(3);
        for (int i = 0; i < branches; i++) {
            double t = 0.5 + 0.45 * i / (branches - 1.0) + r.nextDouble() * 0.04;
            int si = Math.min(segs - 1, (int) (t * segs));
            double[] s = spine[si];
            double a = i * 2.39996 + r.nextDouble() * 0.6;
            double len = 15 + r.nextDouble() * 9 - t * 4;
            double rise = 6 + r.nextDouble() * 10;
            double ex = s[0] + Math.cos(a) * len, ez = s[2] + Math.sin(a) * len, ey = s[1] + rise;
            double mx = s[0] + Math.cos(a) * len * 0.55, mz = s[2] + Math.sin(a) * len * 0.55, my = s[1] + rise * 0.3;
            double br = trunkR(t) * 0.55;
            wood.add(new Capsule(s[0], s[1], s[2], mx, my, mz, br, br * 0.7, false));
            wood.add(new Capsule(mx, my, mz, ex, ey, ez, br * 0.7, 1.1, false));
            leaves.add(new Blob(ex, ey + 1.5, ez, 9 + r.nextDouble() * 4, 0.62));
            leaves.add(new Blob(mx + (ex - mx) * 0.3, my + 4, mz + (ez - mz) * 0.3, 6 + r.nextDouble() * 3, 0.65));
        }
        double[] top = spine[segs];
        leaves.add(new Blob(top[0], top[1] + 4, top[2], 15 + r.nextDouble() * 3, 0.6));
        leaves.add(new Blob(top[0] + 6, top[1] - 4, top[2] - 5, 11, 0.6));
        leaves.add(new Blob(top[0] - 7, top[1] - 3, top[2] + 4, 11, 0.6));

        double a0 = Double.MAX_VALUE, a1 = -Double.MAX_VALUE, b0 = Double.MAX_VALUE, b1 = -Double.MAX_VALUE, c0 = Double.MAX_VALUE, c1 = -Double.MAX_VALUE;
        for (Capsule c : wood) {
            a0 = Math.min(a0, c.minX()); a1 = Math.max(a1, c.maxX());
            b0 = Math.min(b0, c.minZ()); b1 = Math.max(b1, c.maxZ());
            c0 = Math.min(c0, c.minY()); c1 = Math.max(c1, c.maxY());
        }
        for (Blob b : leaves) {
            a0 = Math.min(a0, b.x - b.r - 1); a1 = Math.max(a1, b.x + b.r + 1);
            b0 = Math.min(b0, b.z - b.r - 1); b1 = Math.max(b1, b.z + b.r + 1);
            c0 = Math.min(c0, b.y - b.r * b.squash - 8); c1 = Math.max(c1, b.y + b.r * b.squash + 1);
        }
        minX = (int) Math.floor(a0); maxX = (int) Math.ceil(a1);
        minZ = (int) Math.floor(b0); maxZ = (int) Math.ceil(b1);
        minY = (int) Math.floor(c0); maxY = (int) Math.ceil(c1);
    }

    private static double trunkR(double t) {
        return 3.4 + 4.2 * Math.pow(1 - t, 1.6) + 4.5 * Math.exp(-t * 18);
    }

    public boolean overlaps(int x0, int z0, int x1, int z1) {
        return maxX >= x0 && minX <= x1 && maxZ >= z0 && minZ <= z1;
    }

    private double n(int salt, int x, int y, int z) {
        return Noise.hash(seed + salt, x, y, z);
    }

    public boolean isWood(int bx, int by, int bz) {
        double cx = bx + 0.5, cy = by + 0.5, cz = bz + 0.5;
        // kabuk pürüzü
        double rough = (n(1, bx, by / 2, bz) - 0.5) * 0.9;
        for (Capsule c : wood) {
            if (cx < c.minX() || cx > c.maxX() || cz < c.minZ() || cz > c.maxZ() || cy < c.minY() || cy > c.maxY()) continue;
            if (c.inside(cx, cy, cz) + rough > 0) return true;
        }
        return false;
    }

    public boolean isRoot(int bx, int by, int bz) {
        double cx = bx + 0.5, cy = by + 0.5, cz = bz + 0.5;
        for (Capsule c : wood) if (c.root && c.inside(cx, cy, cz) > 0) return true;
        return false;
    }

    public boolean isLeaf(int bx, int by, int bz) {
        double cx = bx + 0.5, cy = by + 0.5, cz = bz + 0.5;
        for (Blob b : leaves) {
            double dx = cx - b.x, dy = (cy - b.y) / b.squash, dz = cz - b.z;
            double d2 = dx * dx + dy * dy + dz * dz;
            if (d2 > b.r * b.r) continue;
            double d = Math.sqrt(d2) / b.r;
            // kenarlar düzensiz, içi seyrek boşluklu
            double edge = 0.82 + 0.18 * Noise.value3(seed + 7, bx * 0.3, by * 0.3, bz * 0.3);
            if (d < edge) return n(2, bx, by, bz) > 0.04;
        }
        return false;
    }

    /** Bu blok için ağaç bloğu (yoksa null). */
    public BlockState blockAt(int bx, int by, int bz) {
        if (isWood(bx, by, bz)) {
            if (isRoot(bx, by, bz) && !isWood(bx, by + 1, bz) && n(3, bx, by, bz) < 0.5) return Blocks.MOSS_BLOCK.defaultBlockState();
            return Blocks.OAK_WOOD.defaultBlockState();
        }
        if (isLeaf(bx, by, bz)) {
            double r = n(4, bx, by, bz);
            if (r < 0.006 && isLeaf(bx, by + 1, bz) && isLeaf(bx, by - 1, bz)) return Blocks.SHROOMLIGHT.defaultBlockState();
            BlockState l = r < 0.68 ? Blocks.OAK_LEAVES.defaultBlockState() : r < 0.9 ? Blocks.AZALEA_LEAVES.defaultBlockState()
                    : Blocks.FLOWERING_AZALEA_LEAVES.defaultBlockState();
            return l.setValue(LeavesBlock.PERSISTENT, true);
        }
        // yaprakların altından sarkan parlak meyveli sarmaşıklar
        for (int k = 1; k <= 5; k++) {
            if (isLeaf(bx, by + k, bz)) {
                if (n(5, bx, by + k, bz) > 0.035) return null;
                int len = 2 + (int) (n(6, bx, by + k, bz) * 4);
                if (k > len) return null;
                for (int j = 1; j < k; j++) if (isWood(bx, by + j, bz)) return null;
                BlockState v = k == len ? Blocks.CAVE_VINES.defaultBlockState() : Blocks.CAVE_VINES_PLANT.defaultBlockState();
                return v.setValue(CaveVines.BERRIES, n(7, bx, by, bz) < 0.5);
            }
            if (isWood(bx, by + k, bz)) return null;
        }
        return null;
    }
}
