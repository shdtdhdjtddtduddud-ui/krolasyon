package com.sololeveling.world;

import com.sololeveling.gen.Content;
import com.sololeveling.registry.ModDimensions;

public final class Regions {
    private Regions() {}

    public static final int MARGIN = 30;

    public static Content.RegionDef find(String id) {
        for (Content.RegionDef r : Content.REGIONS) if (r.id().equals(id)) return r;
        return null;
    }

    /** region whose plateau (incl. transition margin) covers the block column, or null */
    public static Content.RegionDef at(int x, int z) {
        Content.RegionDef best = null;
        double bd = Double.MAX_VALUE;
        for (Content.RegionDef r : Content.REGIONS) {
            double d = Math.hypot(x - r.cx(), z - r.cz());
            if (d <= r.radius() + MARGIN && d < bd) { bd = d; best = r; }
        }
        return best;
    }

    public static double[] arrival(Content.RegionDef r) {
        return new double[]{r.cx() + 4.5, ModDimensions.CITY_Y + 1.3, r.cz() + 44.5};
    }
}
