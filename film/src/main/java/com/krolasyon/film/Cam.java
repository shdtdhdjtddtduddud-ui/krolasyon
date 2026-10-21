package com.krolasyon.film;

import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Bir karenin kamera durumu ve kamera yolu yardımcıları. */
public record Cam(Vec3 pos, float yaw, float pitch, float roll, float fov) {

    public static Cam look(Vec3 p, Vec3 target, float roll, float fov) {
        double dx = target.x - p.x, dy = target.y - p.y, dz = target.z - p.z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        return new Cam(p, yaw, pitch, roll, fov);
    }

    public static Cam look(Vec3 p, Vec3 target) { return look(p, target, 0, 0); }

    public static float ease(float u) {
        u = Math.max(0, Math.min(1, u));
        return (float) (0.5 - 0.5 * Math.cos(Math.PI * u));
    }

    /** Hafif yumuşatma: ortada sabit hıza yakın. */
    public static float soft(float u) {
        u = Math.max(0, Math.min(1, u));
        return 0.7F * u + 0.3F * ease(u);
    }

    public static Vec3 v(double x, double y, double z) { return new Vec3(x, y, z); }

    public static Vec3 lerp(Vec3 a, Vec3 b, double u) { return a.add(b.subtract(a).scale(u)); }

    /** Doğrusal geçiş: konum a→b, bakış hedefi la→lb. */
    public static Cam move(Vec3 a, Vec3 b, Vec3 la, Vec3 lb, float u, float roll0, float roll1, float fov) {
        float e = soft(u);
        return look(lerp(a, b, e), lerp(la, lb, e), roll0 + (roll1 - roll0) * e, fov);
    }

    /** Merkez çevresinde yörünge (derece). */
    public static Cam orbit(Vec3 c, double r, double h, double a0, double a1, float u, Vec3 lookOff, float fov) {
        double a = Math.toRadians(a0 + (a1 - a0) * soft(u));
        Vec3 p = c.add(Math.cos(a) * r, h, Math.sin(a) * r);
        return look(p, c.add(lookOff), 0, fov);
    }

    /** Catmull-Rom eğrisi üzerinde nokta. */
    public static Vec3 spline(List<Vec3> pts, float u) {
        int n = pts.size() - 1;
        float f = Math.max(0, Math.min(1, u)) * n;
        int i = Math.min(n - 1, (int) f);
        float t = f - i;
        Vec3 p0 = pts.get(Math.max(0, i - 1)), p1 = pts.get(i), p2 = pts.get(i + 1), p3 = pts.get(Math.min(n, i + 2));
        double t2 = t * t, t3 = t2 * t;
        return p1.scale(2).add(p2.subtract(p0).scale(t)).add(p0.scale(2).subtract(p1.scale(5)).add(p2.scale(4)).subtract(p3).scale(t2))
                .add(p1.scale(3).subtract(p0).subtract(p2.scale(3)).add(p3).scale(t3)).scale(0.5);
    }

    public static Vec3 dir(float yawDeg, float pitchDeg) { return Vec3.directionFromRotation(pitchDeg, yawDeg); }
}
