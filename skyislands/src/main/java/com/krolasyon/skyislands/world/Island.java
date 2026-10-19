package com.krolasyon.skyislands.world;

/**
 * One floating island. {@code baseY} is the average grass level, {@code radius} the horizontal size and
 * {@code depth} how far the rocky underside hangs down.
 */
public record Island(long seed, int cx, int cz, int baseY, int radius, int depth,
                     boolean pond, boolean giantTree, boolean satellite) {

    /** Horizontal distance beyond which nothing of this island (incl. tree canopy) can exist. */
    public int reach() {
        return (int) Math.ceil(radius * 1.3) + (giantTree ? 28 : 8);
    }
}
