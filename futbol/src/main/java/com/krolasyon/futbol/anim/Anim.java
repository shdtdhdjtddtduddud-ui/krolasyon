package com.krolasyon.futbol.anim;

/**
 * Keyframed full-body animation. Limb tracks are rotations in degrees (x, y, z) that are blended over the vanilla pose,
 * ROT rotates the whole body around the hips (pitch, yaw, roll) and POS offsets it (x left, y up, z forward, pixels).
 * Values are interpolated with Catmull-Rom splines for smooth, natural motion.
 */
public final class Anim {
    public final int length;
    private final float[][][] keys = new float[Part.values().length][][];

    public Anim(int length) { this.length = length; }

    /** groups of four floats: tick, x, y, z */
    public Anim p(Part part, float... tk) {
        int n = tk.length / 4;
        float[][] k = new float[n][];
        for (int i = 0; i < n; i++) k[i] = new float[]{tk[i * 4], tk[i * 4 + 1], tk[i * 4 + 2], tk[i * 4 + 3]};
        keys[part.ordinal()] = k;
        return this;
    }

    public boolean has(Part part) { return keys[part.ordinal()] != null; }

    /** blend weight of limb tracks: quick fade in, softer fade out */
    public float weight(float t) {
        float in = Math.min(1F, t / 2.0F);
        float out = Math.min(1F, (length - t) / 3.0F);
        return Math.max(0F, Math.min(in, out));
    }

    public void sample(Part part, float t, float[] out) {
        float[][] k = keys[part.ordinal()];
        if (k == null) {
            out[0] = out[1] = out[2] = 0F;
            return;
        }
        if (t <= k[0][0]) {
            copy(k[0], out);
            return;
        }
        int n = k.length;
        if (t >= k[n - 1][0]) {
            copy(k[n - 1], out);
            return;
        }
        int i = 0;
        while (i < n - 2 && t > k[i + 1][0]) i++;
        float[] p1 = k[i], p2 = k[i + 1];
        float[] p0 = i > 0 ? k[i - 1] : p1;
        float[] p3 = i + 2 < n ? k[i + 2] : p2;
        float span = p2[0] - p1[0];
        float u = span <= 0 ? 1F : (t - p1[0]) / span;
        for (int c = 0; c < 3; c++) out[c] = catmull(p0[c + 1], p1[c + 1], p2[c + 1], p3[c + 1], u);
    }

    private static void copy(float[] k, float[] out) {
        out[0] = k[1];
        out[1] = k[2];
        out[2] = k[3];
    }

    private static float catmull(float p0, float p1, float p2, float p3, float t) {
        float t2 = t * t, t3 = t2 * t;
        return 0.5F * ((2F * p1) + (-p0 + p2) * t + (2F * p0 - 5F * p1 + 4F * p2 - p3) * t2 + (-p0 + 3F * p1 - 3F * p2 + p3) * t3);
    }
}
