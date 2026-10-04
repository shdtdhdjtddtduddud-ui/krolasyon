package com.rabona.arena.client.anim;

import java.util.ArrayList;
import java.util.List;

/**
 * Anahtar kare animasyonu. Kanallar: bas, govde, kollar, bacaklar (derece), kok donusu (derece)
 * ve kok konumu (blok; x = sag, y = yukari, z = ileri). Catmull-Rom ile yumusak ara degerleme.
 */
public final class Anim {
    public static final int HEAD = 0, BODY = 1, RARM = 2, LARM = 3, RLEG = 4, LLEG = 5, ROOT = 6, POS = 7, CH = 8;

    public final int length;
    public final float pivot;
    @SuppressWarnings("unchecked")
    final List<float[]>[] keys = new List[CH];

    Anim(int length, float pivot) {
        this.length = length;
        this.pivot = pivot;
    }

    public boolean has(int ch) { return keys[ch] != null && !keys[ch].isEmpty(); }

    /** t aninda kanal degeri (x,y,z) ya da null. */
    public float[] eval(int ch, float t) {
        List<float[]> k = keys[ch];
        if (k == null || k.isEmpty()) return null;
        if (t <= k.get(0)[0]) return copy(k.get(0));
        int n = k.size();
        if (t >= k.get(n - 1)[0]) return copy(k.get(n - 1));
        int i = 0;
        while (i < n - 1 && k.get(i + 1)[0] <= t) i++;
        float[] p1 = k.get(i), p2 = k.get(i + 1);
        float[] p0 = i > 0 ? k.get(i - 1) : p1;
        float[] p3 = i + 2 < n ? k.get(i + 2) : p2;
        float u = (t - p1[0]) / Math.max(1e-4f, p2[0] - p1[0]);
        float[] out = new float[3];
        for (int c = 0; c < 3; c++) out[c] = cr(p0[c + 1], p1[c + 1], p2[c + 1], p3[c + 1], u);
        return out;
    }

    private static float[] copy(float[] k) { return new float[]{k[1], k[2], k[3]}; }

    private static float cr(float p0, float p1, float p2, float p3, float u) {
        float u2 = u * u, u3 = u2 * u;
        return 0.5f * ((2 * p1) + (-p0 + p2) * u + (2 * p0 - 5 * p1 + 4 * p2 - p3) * u2 + (-p0 + 3 * p1 - 3 * p2 + p3) * u3);
    }

    /** Baslangic/bitis harmanlama agirligi. */
    public float weight(float t) {
        float in = Math.min(1, t / 2.5f);
        float out = Math.min(1, (length - t) / 3.5f);
        return Math.max(0, Math.min(in, out));
    }

    // ================================================================ kurucu
    public static B of(int length) { return new B(length, 0.9f); }

    public static B of(int length, float pivot) { return new B(length, pivot); }

    public static final class B {
        private final Anim a;
        private float t;

        B(int len, float pivot) { a = new Anim(len, pivot); }

        public B at(float time) {
            t = time;
            return this;
        }

        private B put(int ch, float x, float y, float z) {
            if (a.keys[ch] == null) a.keys[ch] = new ArrayList<>();
            List<float[]> l = a.keys[ch];
            l.removeIf(k -> k[0] == t);
            l.add(new float[]{t, x, y, z});
            l.sort((p, q) -> Float.compare(p[0], q[0]));
            return this;
        }

        public B head(float x, float y, float z) { return put(HEAD, x, y, z); }

        public B body(float x, float y, float z) { return put(BODY, x, y, z); }

        public B ra(float x, float y, float z) { return put(RARM, x, y, z); }

        public B la(float x, float y, float z) { return put(LARM, x, y, z); }

        public B rl(float x, float y, float z) { return put(RLEG, x, y, z); }

        public B ll(float x, float y, float z) { return put(LLEG, x, y, z); }

        public B root(float x, float y, float z) { return put(ROOT, x, y, z); }

        public B pos(float x, float y, float z) { return put(POS, x, y, z); }

        /** Tum uzuvlar durusta. */
        public B rest() {
            return head(0, 0, 0).body(0, 0, 0).ra(0, 0, 5).la(0, 0, -5).rl(0, 0, 0).ll(0, 0, 0);
        }

        public B arms(float rx, float rz, float lx, float lz) { return ra(rx, 0, rz).la(lx, 0, lz); }

        public B legs(float rx, float lx) { return rl(rx, 0, 0).ll(lx, 0, 0); }

        public Anim build() { return a; }
    }
}
