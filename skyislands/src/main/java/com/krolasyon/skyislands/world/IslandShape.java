package com.krolasyon.skyislands.world;

/** Pure island geometry (no Minecraft types), shared by world generation, the Island Seed and the giant tree. */
public final class IslandShape {
    private IslandShape() {}

    /** Shape of one column of an island. */
    public record Column(boolean inside, double d, int top, int bottom, boolean pond, int water) {
        static final Column OUTSIDE = new Column(false, 2, 0, 0, false, 0);
    }

    public static Column column(Island is, int x, int z) {
        double R = is.radius();
        double dx = x - is.cx(), dz = z - is.cz();
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist > R * 1.3 + 1) return Column.OUTSIDE;
        long s = is.seed();
        double nx = dx / Math.max(R, 4) * 1.8, nz = dz / Math.max(R, 4) * 1.8;
        double edge = R * (0.74 + 0.52 * Noise.fbm2(s, nx + 5.3, nz + 2.1, 3));
        if (dist >= edge) return Column.OUTSIDE;
        double d = dist / edge;

        double dome = Math.min(R * 0.14, 5.0);
        int top = is.baseY() + (int) Math.round((1 - d * d) * dome + (Noise.fbm2(s + 1, x * 0.09, z * 0.09, 2) - 0.5) * 2.5);
        double under = is.depth() * Math.pow(1 - d, 0.85) * (0.6 + 0.7 * Noise.fbm2(s + 2, nx * 1.5, nz * 1.5, 3));
        double spike = Math.max(0, Noise.fbm2(s + 3, x * 0.18, z * 0.18, 2) - 0.52) * is.depth() * 2.8 * (1 - d);
        int bottom = top - 3 - (int) Math.round(under + spike);

        boolean pond = false;
        int water = 0;
        if (is.pond()) {
            double pa = Noise.hash(s, 7, 7, 7) * Math.PI * 2;
            double off = R * (is.giantTree() ? 0.42 : 0.15);
            double pR = Math.max(2.2, R * 0.24);
            double px = is.cx() + Math.cos(pa) * off, pz = is.cz() + Math.sin(pa) * off;
            double pd = Math.sqrt((x - px) * (x - px) + (z - pz) * (z - pz)) / pR;
            if (pd < 1.45) top = Math.max(top, is.baseY() + 1);
            if (pd < 1.0) {
                pond = true;
                water = is.baseY();
                top = is.baseY() - 1 - (int) Math.round((1 - pd * pd) * 2.5);
            }
        }
        return new Column(true, d, top, Math.min(bottom, top - 1), pond, water);
    }

}
