package com.krolasyon.labirent.maze;

import net.minecraft.util.RandomSource;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Pure grid logic of the maze. The maze is a G x G grid of "units": odd/odd units are cells (always open),
 * even/even units are wall posts (always closed), mixed units are the walls between two cells.
 * Wall units are 2 blocks thick, cell units 4 blocks wide, so a cell + wall pair spans 6 blocks.
 */
public final class MazeLayout {
    public static final int G = 81;
    public static final int CELLS = 40;
    public static final int SIZE = 242;
    public static final int ORIGIN = -121;
    /** y of the floor block; walls stand on GROUND+1 .. GROUND+H */
    public static final int GROUND = 64;
    public static final int H = 26;
    public static final int GLADE_MIN = 30, GLADE_MAX = 50;
    public static final int DOOR_MIN = 37, DOOR_MAX = 43;
    public static final int CENTER = 40;
    public static final int SECTORS = 8;

    public static final int DOOR_N = 0, DOOR_E = 1, DOOR_S = 2, DOOR_W = 3;

    private final boolean[] open = new boolean[G * G];

    // ------------------------------------------------------------------ coordinate helpers
    public static int start(int u) { return ORIGIN + (u >> 1) * 6 + ((u & 1) == 1 ? 2 : 0); }

    public static int width(int u) { return (u & 1) == 1 ? 4 : 2; }

    public static int unitOf(int b) {
        int l = b - ORIGIN;
        if (l < 0 || l >= SIZE) return -1;
        int c = l / 6, r = l % 6;
        return r < 2 ? 2 * c : 2 * c + 1;
    }

    /** block center (x or z) of a unit */
    public static double mid(int u) { return start(u) + width(u) / 2.0; }

    public static int idx(int i, int j) { return i * G + j; }

    public static boolean inGrid(int i, int j) { return i >= 0 && j >= 0 && i < G && j < G; }

    public static boolean inGladeInterior(int i, int j) {
        return i > GLADE_MIN && i < GLADE_MAX && j > GLADE_MIN && j < GLADE_MAX;
    }

    public static boolean inGladeArea(int i, int j) {
        return i >= GLADE_MIN && i <= GLADE_MAX && j >= GLADE_MIN && j <= GLADE_MAX;
    }

    /** door index of a glade ring unit, or -1 */
    public static int door(int i, int j) {
        if (j == GLADE_MIN && i >= DOOR_MIN && i <= DOOR_MAX) return DOOR_N;
        if (j == GLADE_MAX && i >= DOOR_MIN && i <= DOOR_MAX) return DOOR_S;
        if (i == GLADE_MIN && j >= DOOR_MIN && j <= DOOR_MAX) return DOOR_W;
        if (i == GLADE_MAX && j >= DOOR_MIN && j <= DOOR_MAX) return DOOR_E;
        return -1;
    }

    public static boolean isCliffOpening(int i, int j) { return i == 0 && j >= DOOR_MIN && j <= DOOR_MAX; }

    public static boolean isCell(int i, int j) { return (i & 1) == 1 && (j & 1) == 1; }

    /** 0..7 sector around the glade, -1 inside the glade */
    public static int sector(int i, int j) {
        if (inGladeArea(i, j)) return -1;
        double a = Math.atan2(j - CENTER, i - CENTER) + Math.PI / 2 + Math.PI / 8; // sector 0 = north
        a = (a % (Math.PI * 2) + Math.PI * 2) % (Math.PI * 2);
        return Math.min(SECTORS - 1, (int) (a / (Math.PI * 2) * SECTORS));
    }

    // ------------------------------------------------------------------ queries
    public boolean isOpen(int i, int j) { return inGrid(i, j) && open[idx(i, j)]; }

    public boolean isOpenBlock(int x, int z) {
        int i = unitOf(x), j = unitOf(z);
        return i >= 0 && j >= 0 && open[idx(i, j)];
    }

    public byte[] toBytes() {
        byte[] b = new byte[G * G];
        for (int k = 0; k < b.length; k++) b[k] = (byte) (open[k] ? 1 : 0);
        return b;
    }

    public static MazeLayout fromBytes(byte[] b) {
        MazeLayout l = new MazeLayout();
        if (b == null || b.length != G * G) return null;
        for (int k = 0; k < b.length; k++) l.open[k] = b[k] != 0;
        return l;
    }

    public MazeLayout copy() {
        MazeLayout l = new MazeLayout();
        System.arraycopy(open, 0, l.open, 0, open.length);
        return l;
    }

    // ------------------------------------------------------------------ generation
    private static boolean mazeCell(int cx, int cy) {
        return cx >= 0 && cy >= 0 && cx < CELLS && cy < CELLS && !inGladeInterior(2 * cx + 1, 2 * cy + 1);
    }

    private void baseOpenings() {
        Arrays.fill(open, false);
        for (int i = 0; i < G; i++)
            for (int j = 0; j < G; j++) {
                if (isCell(i, j) || inGladeInterior(i, j) || door(i, j) >= 0) open[idx(i, j)] = true;
            }
        for (int u = DOOR_MIN; u <= DOOR_MAX; u++) {
            // plazas in front of the four doors and at the cliff
            open[idx(u, GLADE_MIN - 1)] = true;
            open[idx(u, GLADE_MAX + 1)] = true;
            open[idx(GLADE_MIN - 1, u)] = true;
            open[idx(GLADE_MAX + 1, u)] = true;
            open[idx(1, u)] = true;
            open[idx(0, u)] = true;
        }
    }

    public static MazeLayout generate(long seed) {
        MazeLayout l = new MazeLayout();
        l.baseOpenings();
        RandomSource r = RandomSource.create(seed);
        // growing tree: mostly newest cell (long winding corridors), sometimes random (branches)
        boolean[] seen = new boolean[CELLS * CELLS];
        List<int[]> active = new ArrayList<>();
        int sx = 19, sy = 14;
        seen[sx * CELLS + sy] = true;
        active.add(new int[]{sx, sy});
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!active.isEmpty()) {
            int pick = r.nextFloat() < 0.8F ? active.size() - 1 : r.nextInt(active.size());
            int[] c = active.get(pick);
            int[] order = {0, 1, 2, 3};
            for (int k = 3; k > 0; k--) { int s = r.nextInt(k + 1); int t = order[k]; order[k] = order[s]; order[s] = t; }
            boolean carved = false;
            for (int o : order) {
                int nx = c[0] + dirs[o][0], ny = c[1] + dirs[o][1];
                if (!mazeCell(nx, ny) || seen[nx * CELLS + ny]) continue;
                seen[nx * CELLS + ny] = true;
                l.open[idx(c[0] + nx + 1, c[1] + ny + 1)] = true;
                active.add(new int[]{nx, ny});
                carved = true;
                break;
            }
            if (!carved) active.remove(pick);
        }
        l.addLoops(r, null, 0.07F);
        l.ensureConnected(r);
        return l;
    }

    /** extra openings so there is more than one route */
    private void addLoops(RandomSource r, boolean[] onlySectors, float chance) {
        for (int i = 1; i < G - 1; i++)
            for (int j = 1; j < G - 1; j++) {
                if (((i + j) & 1) == 0 || open[idx(i, j)] || inGladeArea(i, j)) continue;
                if (!edgeBetweenMazeCells(i, j)) continue;
                if (onlySectors != null && (sector(i, j) < 0 || !onlySectors[sector(i, j)])) continue;
                if (r.nextFloat() < chance) open[idx(i, j)] = true;
            }
    }

    private static boolean edgeBetweenMazeCells(int i, int j) {
        if ((i & 1) == 0) return i > 0 && i < G - 1 && mazeCell((i - 2) / 2, (j - 1) / 2) && mazeCell(i / 2, (j - 1) / 2);
        return j > 0 && j < G - 1 && mazeCell((i - 1) / 2, (j - 2) / 2) && mazeCell((i - 1) / 2, j / 2);
    }

    /**
     * The nightly shift: every wall touching a cell of the given sectors is torn down and the region is
     * re-woven with randomized Kruskal, keeping everything outside those sectors untouched.
     */
    public MazeLayout shift(long seed, boolean[] sectors) {
        MazeLayout l = copy();
        RandomSource r = RandomSource.create(seed);
        List<int[]> candidates = new ArrayList<>();
        int[] parent = new int[CELLS * CELLS];
        for (int k = 0; k < parent.length; k++) parent[k] = k;
        for (int i = 1; i < G - 1; i++)
            for (int j = 1; j < G - 1; j++) {
                if (((i + j) & 1) == 0 || !edgeBetweenMazeCells(i, j)) continue;
                int ax, ay, bx, by;
                if ((i & 1) == 0) { ax = (i - 2) / 2; bx = i / 2; ay = by = (j - 1) / 2; }
                else { ax = bx = (i - 1) / 2; ay = (j - 2) / 2; by = j / 2; }
                int sa = sector(2 * ax + 1, 2 * ay + 1), sb = sector(2 * bx + 1, 2 * by + 1);
                boolean touch = (sa >= 0 && sectors[sa]) || (sb >= 0 && sectors[sb]);
                if (touch && !plaza(i, j)) {
                    l.open[idx(i, j)] = false;
                    candidates.add(new int[]{i, j, ax * CELLS + ay, bx * CELLS + by});
                } else if (l.open[idx(i, j)]) {
                    union(parent, ax * CELLS + ay, bx * CELLS + by);
                }
            }
        for (int k = candidates.size() - 1; k > 0; k--) {
            int s = r.nextInt(k + 1);
            int[] t = candidates.get(k); candidates.set(k, candidates.get(s)); candidates.set(s, t);
        }
        for (int[] c : candidates) {
            if (find(parent, c[2]) != find(parent, c[3])) {
                union(parent, c[2], c[3]);
                l.open[idx(c[0], c[1])] = true;
            }
        }
        l.addLoops(r, sectors, 0.06F);
        l.ensureConnected(r);
        return l;
    }

    private static boolean plaza(int i, int j) {
        boolean doorBand = i >= DOOR_MIN && i <= DOOR_MAX || j >= DOOR_MIN && j <= DOOR_MAX;
        return doorBand && (i == GLADE_MIN - 1 || i == GLADE_MAX + 1 || j == GLADE_MIN - 1 || j == GLADE_MAX + 1 || i == 1 || i == 0);
    }

    private static int find(int[] p, int a) {
        while (p[a] != a) { p[a] = p[p[a]]; a = p[a]; }
        return a;
    }

    private static void union(int[] p, int a, int b) { p[find(p, a)] = find(p, b); }

    /** safety net: every cell must be reachable from the glade */
    private void ensureConnected(RandomSource r) {
        for (int guard = 0; guard < 400; guard++) {
            boolean[] reach = reachable();
            int bad = -1;
            for (int i = 1; i < G && bad < 0; i += 2)
                for (int j = 1; j < G; j += 2)
                    if (!reach[idx(i, j)]) { bad = idx(i, j); break; }
            if (bad < 0) return;
            int bi = bad / G, bj = bad % G;
            int[][] d = {{2, 0}, {-2, 0}, {0, 2}, {0, -2}};
            boolean fixed = false;
            for (int[] o : d) {
                int ni = bi + o[0], nj = bj + o[1];
                if (inGrid(ni, nj) && reach[idx(ni, nj)] && !inGladeInterior(ni, nj)) {
                    open[idx(bi + o[0] / 2, bj + o[1] / 2)] = true;
                    fixed = true;
                    break;
                }
            }
            if (!fixed) {
                int[] o = d[r.nextInt(4)];
                int wi = bi + o[0] / 2, wj = bj + o[1] / 2;
                if (inGrid(wi, wj) && wi > 0 && wj > 0 && wi < G - 1 && wj < G - 1 && !inGladeArea(wi, wj)) open[idx(wi, wj)] = true;
            }
        }
    }

    public boolean[] reachable() {
        boolean[] seen = new boolean[G * G];
        ArrayDeque<Integer> q = new ArrayDeque<>();
        int s = idx(CENTER + 1, CENTER + 1);
        seen[s] = true;
        q.add(s);
        while (!q.isEmpty()) {
            int c = q.poll();
            int i = c / G, j = c % G;
            int[][] d = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (int[] o : d) {
                int ni = i + o[0], nj = j + o[1];
                if (!inGrid(ni, nj)) continue;
                int n = idx(ni, nj);
                if (seen[n] || !open[n]) continue;
                seen[n] = true;
                q.add(n);
            }
        }
        return seen;
    }

    /** BFS path over open units (doors treated as given by doorsOpen). Returns unit indices from start to goal, or null. */
    public List<Integer> path(int si, int sj, int gi, int gj, boolean doorsOpen, int maxNodes) {
        if (!inGrid(si, sj) || !inGrid(gi, gj)) return null;
        int start = idx(si, sj), goal = idx(gi, gj);
        int[] prev = new int[G * G];
        Arrays.fill(prev, -2);
        ArrayDeque<Integer> q = new ArrayDeque<>();
        prev[start] = -1;
        q.add(start);
        int n = 0;
        int[][] d = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!q.isEmpty() && n++ < maxNodes) {
            int c = q.poll();
            if (c == goal) break;
            int i = c / G, j = c % G;
            for (int[] o : d) {
                int ni = i + o[0], nj = j + o[1];
                if (!inGrid(ni, nj)) continue;
                int k = idx(ni, nj);
                if (prev[k] != -2 || !open[k]) continue;
                if (!doorsOpen && door(ni, nj) >= 0) continue;
                prev[k] = c;
                q.add(k);
            }
        }
        if (prev[goal] == -2) return null;
        List<Integer> out = new ArrayList<>();
        for (int c = goal; c != -1; c = prev[c]) out.add(0, c);
        return out;
    }

    /** a deterministic far dead end per sector for the code stones */
    public static int[] codeStoneCell(int sector) {
        // fixed positions roughly in the middle of each sector ring, 2/3 of the way out
        double a = sector * Math.PI * 2 / SECTORS - Math.PI / 2;
        double rad = 27;
        int i = (int) Math.round(CENTER + Math.cos(a) * rad);
        int j = (int) Math.round(CENTER + Math.sin(a) * rad);
        if ((i & 1) == 0) i++;
        if ((j & 1) == 0) j++;
        return new int[]{Math.max(1, Math.min(G - 2, i)), Math.max(1, Math.min(G - 2, j))};
    }
}
