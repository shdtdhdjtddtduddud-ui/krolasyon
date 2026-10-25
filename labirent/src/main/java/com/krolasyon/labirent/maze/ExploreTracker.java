package com.krolasyon.labirent.maze;

import java.util.BitSet;

/** Auto-mapping: everything a Glader can see down the corridors is written into the shared Map Room map. */
public final class ExploreTracker {
    private final BitSet dirty = new BitSet(MazeLayout.G * MazeLayout.G);
    private int revealedThisTick;

    public BitSet dirty() { return dirty; }

    public static boolean known(byte b) { return (b & 1) != 0; }

    private void mark(MazeState s, int i, int j) {
        if (!MazeLayout.inGrid(i, j)) return;
        int k = MazeLayout.idx(i, j);
        byte nv = (byte) (1 | (s.layout.isOpen(i, j) ? 0 : 2) | 4);
        if (s.known[k] != nv) {
            s.known[k] = nv;
            dirty.set(k);
            revealedThisTick++;
        }
    }

    /** reveal from a block position; returns the number of newly mapped units */
    public int reveal(MazeState s, double x, double z, int range) {
        if (s.layout == null) return 0;
        revealedThisTick = 0;
        int i = MazeLayout.unitOf((int) Math.floor(x)), j = MazeLayout.unitOf((int) Math.floor(z));
        if (i < 0 || j < 0) return 0;
        for (int a = -1; a <= 1; a++)
            for (int b = -1; b <= 1; b++) mark(s, i + a, j + b);
        int[][] d = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] o : d) {
            for (int k = 1; k <= range; k++) {
                int ni = i + o[0] * k, nj = j + o[1] * k;
                if (!MazeLayout.inGrid(ni, nj)) break;
                mark(s, ni, nj);
                if (!s.layout.isOpen(ni, nj)) break;
                mark(s, ni + o[1], nj + o[0]);
                mark(s, ni - o[1], nj - o[0]);
            }
        }
        if (revealedThisTick > 0) s.setDirty();
        return revealedThisTick;
    }

    public void revealGlade(MazeState s) {
        for (int i = MazeLayout.GLADE_MIN; i <= MazeLayout.GLADE_MAX; i++)
            for (int j = MazeLayout.GLADE_MIN; j <= MazeLayout.GLADE_MAX; j++) mark(s, i, j);
    }

    /** percentage of the maze (outside the glade) that has been mapped */
    public static int percent(MazeState s) {
        int total = 0, seen = 0;
        for (int i = 0; i < MazeLayout.G; i++)
            for (int j = 0; j < MazeLayout.G; j++) {
                if (MazeLayout.inGladeArea(i, j)) continue;
                total++;
                if (known(s.known[MazeLayout.idx(i, j)])) seen++;
            }
        return total == 0 ? 0 : seen * 100 / total;
    }

    public static int sectorPercent(MazeState s, int sector) {
        int total = 0, seen = 0;
        for (int i = 0; i < MazeLayout.G; i++)
            for (int j = 0; j < MazeLayout.G; j++) {
                if (MazeLayout.sector(i, j) != sector) continue;
                total++;
                if (known(s.known[MazeLayout.idx(i, j)])) seen++;
            }
        return total == 0 ? 0 : seen * 100 / total;
    }
}
