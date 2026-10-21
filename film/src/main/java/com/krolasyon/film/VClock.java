package com.krolasyon.film;

import net.minecraft.Util;

/**
 * Sanal saat: kayıt sırasında oyun zamanı gerçek zamandan kopar ve her çizilen kare tam 1/FPS saniye ilerletir.
 * Hem istemci zamanlayıcısı hem tümleşik sunucu {@link Util#getMillis()} kullandığından ikisi de kareye kilitlenir.
 */
public final class VClock {
    private static volatile boolean fixed;
    private static volatile long base, offset, frames, frameNs = 1_000_000_000L / 30;

    private VClock() {}

    public static long now() {
        return fixed ? base + frames * frameNs : System.nanoTime() - offset;
    }

    public static void install(int fps) {
        frameNs = 1_000_000_000L / fps;
        Util.timeSource = VClock::now;
    }

    public static synchronized void fix() {
        if (fixed) return;
        base = now();
        frames = 0;
        fixed = true;
    }

    public static synchronized void free() {
        if (!fixed) return;
        long v = now();
        offset = System.nanoTime() - v;
        fixed = false;
    }

    public static boolean fixed() { return fixed; }

    public static void step() { frames++; }

    public static long millis() { return now() / 1_000_000L; }
}
