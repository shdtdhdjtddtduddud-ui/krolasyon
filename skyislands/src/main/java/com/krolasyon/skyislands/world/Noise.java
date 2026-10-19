package com.krolasyon.skyislands.world;

/** Deterministic, allocation-free value noise used to shape the islands. */
public final class Noise {
    private Noise() {}

    public static long mix(long h) {
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= h >>> 33;
        return h;
    }

    public static double hash(long seed, int x, int y, int z) {
        long h = mix(seed ^ (x * 0x9E3779B97F4A7C15L) ^ (y * 0xC2B2AE3D27D4EB4FL) ^ (z * 0x165667B19E3779F9L));
        return (h >>> 11) * 0x1.0p-53;
    }

    private static double smooth(double t) {
        return t * t * (3.0 - 2.0 * t);
    }

    public static double value2(long seed, double x, double z) {
        int ix = (int) Math.floor(x), iz = (int) Math.floor(z);
        double fx = smooth(x - ix), fz = smooth(z - iz);
        double a = hash(seed, ix, 0, iz), b = hash(seed, ix + 1, 0, iz);
        double c = hash(seed, ix, 0, iz + 1), d = hash(seed, ix + 1, 0, iz + 1);
        return lerp(lerp(a, b, fx), lerp(c, d, fx), fz);
    }

    public static double value3(long seed, double x, double y, double z) {
        int ix = (int) Math.floor(x), iy = (int) Math.floor(y), iz = (int) Math.floor(z);
        double fx = smooth(x - ix), fy = smooth(y - iy), fz = smooth(z - iz);
        double x00 = lerp(hash(seed, ix, iy, iz), hash(seed, ix + 1, iy, iz), fx);
        double x10 = lerp(hash(seed, ix, iy + 1, iz), hash(seed, ix + 1, iy + 1, iz), fx);
        double x01 = lerp(hash(seed, ix, iy, iz + 1), hash(seed, ix + 1, iy, iz + 1), fx);
        double x11 = lerp(hash(seed, ix, iy + 1, iz + 1), hash(seed, ix + 1, iy + 1, iz + 1), fx);
        return lerp(lerp(x00, x10, fy), lerp(x01, x11, fy), fz);
    }

    public static double fbm2(long seed, double x, double z, int octaves) {
        double sum = 0, amp = 0.5, norm = 0;
        for (int i = 0; i < octaves; i++) {
            sum += amp * value2(seed + i * 7919L, x, z);
            norm += amp;
            x = x * 2.03 + 17.1;
            z = z * 2.03 - 9.3;
            amp *= 0.5;
        }
        return sum / norm;
    }

    public static double fbm3(long seed, double x, double y, double z, int octaves) {
        double sum = 0, amp = 0.5, norm = 0;
        for (int i = 0; i < octaves; i++) {
            sum += amp * value3(seed + i * 7919L, x, y, z);
            norm += amp;
            x *= 2.02; y *= 2.02; z *= 2.02;
            amp *= 0.5;
        }
        return sum / norm;
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
