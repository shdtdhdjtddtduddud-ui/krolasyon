package com.krolasyon.skyislands.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Places islands on a jittered grid. Everything is derived from the world seed so every chunk can compute
 * the same islands independently.
 */
public final class IslandLayout {
    public static final int CELL = 112;
    private static final double CHANCE = 0.62;

    private IslandLayout() {}

    public static List<Island> cell(long worldSeed, int ci, int cj) {
        List<Island> out = new ArrayList<>();
        Random r = new Random(Noise.mix(worldSeed * 31L + ci * 341873128712L + cj * 132897987541L));
        if (r.nextDouble() > CHANCE) return out;
        int radius = 9 + (int) (23 * Math.pow(r.nextDouble(), 1.3));
        int x = ci * CELL + 26 + r.nextInt(CELL - 52);
        int z = cj * CELL + 26 + r.nextInt(CELL - 52);
        int baseY = 150 + r.nextInt(75);
        int depth = (int) (radius * (1.35 + 0.9 * r.nextDouble()));
        boolean giant = radius >= 20 && r.nextDouble() < 0.8;
        boolean pond = radius >= 13 && r.nextDouble() < 0.6;
        out.add(new Island(r.nextLong(), x, z, baseY, radius, depth, pond, giant, false));

        int sats = 1 + r.nextInt(radius >= 18 ? 6 : 3);
        for (int i = 0; i < sats; i++) {
            double ang = r.nextDouble() * Math.PI * 2;
            double dist = radius * 1.25 + 5 + r.nextDouble() * 16;
            int sr = 2 + r.nextInt(radius >= 18 ? 6 : 4);
            int sy = baseY - 14 + r.nextInt(30);
            out.add(new Island(r.nextLong(), x + (int) (Math.cos(ang) * dist), z + (int) (Math.sin(ang) * dist),
                    sy, sr, (int) (sr * (1.3 + r.nextDouble())), false, false, true));
        }
        return out;
    }

    /** All islands that may touch the given block-column rectangle. */
    public static List<Island> near(long worldSeed, int minX, int minZ, int maxX, int maxZ) {
        List<Island> out = new ArrayList<>();
        int margin = 96;
        for (int ci = Math.floorDiv(minX - margin, CELL); ci <= Math.floorDiv(maxX + margin, CELL); ci++) {
            for (int cj = Math.floorDiv(minZ - margin, CELL); cj <= Math.floorDiv(maxZ + margin, CELL); cj++) {
                for (Island is : cell(worldSeed, ci, cj)) {
                    int reach = is.reach();
                    if (is.cx() + reach >= minX && is.cx() - reach <= maxX && is.cz() + reach >= minZ && is.cz() - reach <= maxZ) {
                        out.add(is);
                    }
                }
            }
        }
        return out;
    }

    /** Nearest main island with at least {@code minRadius} to the given position (used by tests/commands). */
    public static Island nearestMain(long worldSeed, int x, int z, int minRadius, int searchCells) {
        Island best = null;
        long bestD = Long.MAX_VALUE;
        int ci0 = Math.floorDiv(x, CELL), cj0 = Math.floorDiv(z, CELL);
        for (int ci = ci0 - searchCells; ci <= ci0 + searchCells; ci++) {
            for (int cj = cj0 - searchCells; cj <= cj0 + searchCells; cj++) {
                for (Island is : cell(worldSeed, ci, cj)) {
                    if (is.satellite() || is.radius() < minRadius) continue;
                    long dx = is.cx() - x, dz = is.cz() - z;
                    long d = dx * dx + dz * dz;
                    if (d < bestD) {
                        bestD = d;
                        best = is;
                    }
                }
            }
        }
        return best;
    }
}
