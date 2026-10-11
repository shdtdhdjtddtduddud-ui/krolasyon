package com.krolasyon.omnitrix.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/** Small procedural geometry kit for the effects (POSITION_COLOR and POSITION_COLOR_TEX quads). */
public final class Geo {
    private Geo() {}

    public static void v(VertexConsumer vc, Matrix4f m, Vec3 p, float r, float g, float b, float a) {
        vc.vertex(m, (float) p.x, (float) p.y, (float) p.z).color(r, g, b, a).endVertex();
    }

    public static void v(VertexConsumer vc, Matrix4f m, double x, double y, double z, int rgb, float a) {
        vc.vertex(m, (float) x, (float) y, (float) z).color(((rgb >> 16) & 255) / 255F, ((rgb >> 8) & 255) / 255F, (rgb & 255) / 255F, a).endVertex();
    }

    public static void t(VertexConsumer vc, Matrix4f m, Vec3 p, float r, float g, float b, float a, float u, float v) {
        vc.vertex(m, (float) p.x, (float) p.y, (float) p.z).color(r, g, b, a).uv(u, v).endVertex();
    }

    public static void quad(VertexConsumer vc, Matrix4f m, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int rgb, float al) {
        v(vc, m, a.x, a.y, a.z, rgb, al);
        v(vc, m, b.x, b.y, b.z, rgb, al);
        v(vc, m, c.x, c.y, c.z, rgb, al);
        v(vc, m, d.x, d.y, d.z, rgb, al);
    }

    public static void tri(VertexConsumer vc, Matrix4f m, Vec3 a, Vec3 b, Vec3 c, int rgb, float al) { quad(vc, m, a, b, c, c, rgb, al); }

    /** camera facing ribbon along a polyline */
    public static void ribbon(VertexConsumer vc, Matrix4f m, Vec3 cam, List<Vec3> pts, float width, int rgb, float alpha, boolean taper) {
        int n = pts.size() - 1;
        for (int i = 0; i < n; i++) {
            Vec3 p0 = pts.get(i), p1 = pts.get(i + 1);
            Vec3 seg = p1.subtract(p0);
            Vec3 w = seg.cross(p0.add(p1).scale(0.5).subtract(cam));
            if (w.lengthSqr() < 1e-10) continue;
            float wd0 = taper ? width * (1 - (float) i / n * 0.9F) : width, wd1 = taper ? width * (1 - (float) (i + 1) / n * 0.9F) : width;
            Vec3 n0 = w.normalize().scale(wd0 * 0.5), n1 = w.normalize().scale(wd1 * 0.5);
            quad(vc, m, p0.subtract(n0), p0.add(n0), p1.add(n1), p1.subtract(n1), rgb, alpha);
        }
    }

    /** glow + core ribbon */
    public static void beam(VertexConsumer vc, Matrix4f m, Vec3 cam, List<Vec3> pts, float width, int glow, int core, float alpha) {
        ribbon(vc, m, cam, pts, width * 3.5F, glow, alpha * 0.3F, false);
        ribbon(vc, m, cam, pts, width * 1.6F, glow, alpha * 0.6F, false);
        ribbon(vc, m, cam, pts, width * 0.6F, core, alpha, false);
    }

    public static List<Vec3> circle(Vec3 c, Vec3 ax1, Vec3 ax2, double r, int n, double rot) {
        List<Vec3> pts = new ArrayList<>(n + 1);
        for (int i = 0; i <= n; i++) {
            double a = i * Math.PI * 2 / n + rot;
            pts.add(c.add(ax1.scale(Math.cos(a) * r)).add(ax2.scale(Math.sin(a) * r)));
        }
        return pts;
    }

    public static List<Vec3> flatCircle(Vec3 c, double r, int n, double rot) { return circle(c, new Vec3(1, 0, 0), new Vec3(0, 0, 1), r, n, rot); }

    /** jagged polyline (midpoint displacement) */
    public static List<Vec3> jag(Vec3 a, Vec3 b, RandomSource r, float jag, int depth) {
        List<Vec3> pts = new ArrayList<>();
        pts.add(a);
        pts.add(b);
        double off = a.distanceTo(b) * jag;
        for (int d = 0; d < depth; d++) {
            List<Vec3> n = new ArrayList<>();
            for (int i = 0; i < pts.size() - 1; i++) {
                Vec3 p0 = pts.get(i), p1 = pts.get(i + 1);
                n.add(p0);
                n.add(p0.add(p1).scale(0.5).add(perp(p1.subtract(p0), r).scale((r.nextDouble() * 2 - 1) * off)));
            }
            n.add(pts.get(pts.size() - 1));
            pts = n;
            off *= 0.55;
        }
        return pts;
    }

    public static Vec3 perp(Vec3 dir, RandomSource r) {
        Vec3 rnd = new Vec3(r.nextDouble() - 0.5, r.nextDouble() - 0.5, r.nextDouble() - 0.5);
        Vec3 d = dir.lengthSqr() < 1e-8 ? new Vec3(0, 1, 0) : dir.normalize();
        Vec3 p = rnd.subtract(d.scale(rnd.dot(d)));
        return p.lengthSqr() < 1e-8 ? new Vec3(1, 0, 0) : p.normalize();
    }

    /** smooth wavy tendril a -> b */
    public static List<Vec3> wave(Vec3 a, Vec3 b, double amp, double waves, double phase, int steps) {
        Vec3 d = b.subtract(a);
        Vec3 n = d.lengthSqr() < 1e-8 ? new Vec3(0, 1, 0) : d.normalize();
        Vec3 up = Math.abs(n.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 p1 = n.cross(up).normalize(), p2 = n.cross(p1).normalize();
        List<Vec3> pts = new ArrayList<>(steps + 1);
        for (int i = 0; i <= steps; i++) {
            double f = (double) i / steps, env = Math.sin(f * Math.PI);
            double w = f * Math.PI * 2 * waves + phase;
            pts.add(a.add(d.scale(f)).add(p1.scale(Math.sin(w) * amp * env)).add(p2.scale(Math.cos(w * 0.7 + 1.3) * amp * 0.6 * env)));
        }
        return pts;
    }

    /** solid shaded crystal (elongated octahedron) from base to tip */
    public static void crystal(VertexConsumer vc, Matrix4f m, Vec3 base, Vec3 tip, double radius, int rgb, float alpha, double rot) {
        Vec3 d = tip.subtract(base);
        Vec3 n = d.normalize();
        Vec3 up = Math.abs(n.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 a1 = n.cross(up).normalize(), a2 = n.cross(a1).normalize();
        Vec3 mid = base.add(d.scale(0.3));
        Vec3[] ring = new Vec3[5];
        for (int i = 0; i < 5; i++) {
            double a = rot + i * Math.PI * 2 / 5;
            ring[i] = mid.add(a1.scale(Math.cos(a) * radius)).add(a2.scale(Math.sin(a) * radius));
        }
        for (int i = 0; i < 5; i++) {
            Vec3 p = ring[i], q = ring[(i + 1) % 5];
            float shade = 0.75F + 0.25F * (float) Math.cos(rot + i * 1.256);
            int c = shadeRgb(rgb, shade);
            tri(vc, m, p, q, tip, c, alpha);
            tri(vc, m, q, p, base, shadeRgb(rgb, shade * 0.75F), alpha);
        }
    }

    public static int shadeRgb(int rgb, float f) {
        int r = Mth.clamp((int) (((rgb >> 16) & 255) * f), 0, 255);
        int g = Mth.clamp((int) (((rgb >> 8) & 255) * f), 0, 255);
        int b = Mth.clamp((int) ((rgb & 255) * f), 0, 255);
        return (r << 16) | (g << 8) | b;
    }

    /** camera facing textured square */
    public static void sprite(VertexConsumer vc, Matrix4f m, Vec3 p, Vec3 right, Vec3 up, float size, float rot, float r, float g, float b, float a) {
        Vec3 r2 = right.scale(Mth.cos(rot)).add(up.scale(Mth.sin(rot))).scale(size);
        Vec3 u2 = up.scale(Mth.cos(rot)).subtract(right.scale(Mth.sin(rot))).scale(size);
        t(vc, m, p.subtract(r2).subtract(u2), r, g, b, a, 0, 1);
        t(vc, m, p.add(r2).subtract(u2), r, g, b, a, 1, 1);
        t(vc, m, p.add(r2).add(u2), r, g, b, a, 1, 0);
        t(vc, m, p.subtract(r2).add(u2), r, g, b, a, 0, 0);
    }

    /** textured quad in an arbitrary plane */
    public static void plane(VertexConsumer vc, Matrix4f m, Vec3 c, Vec3 ax, Vec3 ay, float r, float g, float b, float a) {
        t(vc, m, c.subtract(ax).subtract(ay), r, g, b, a, 0, 1);
        t(vc, m, c.add(ax).subtract(ay), r, g, b, a, 1, 1);
        t(vc, m, c.add(ax).add(ay), r, g, b, a, 1, 0);
        t(vc, m, c.subtract(ax).add(ay), r, g, b, a, 0, 0);
    }

    /** UV sphere of flat colour */
    public static void sphere(VertexConsumer vc, Matrix4f m, Vec3 c, double rx, double ry, double rz, int rgb, float a, int lat, int lon) {
        for (int i = 0; i < lat; i++) {
            double t0 = Math.PI * i / lat, t1 = Math.PI * (i + 1) / lat;
            for (int j = 0; j < lon; j++) {
                double p0 = Math.PI * 2 * j / lon, p1 = Math.PI * 2 * (j + 1) / lon;
                quad(vc, m, sp(c, rx, ry, rz, t0, p0), sp(c, rx, ry, rz, t0, p1), sp(c, rx, ry, rz, t1, p1), sp(c, rx, ry, rz, t1, p0),
                        shadeRgb(rgb, 0.7F + 0.3F * (float) Math.cos(t0)), a);
            }
        }
    }

    private static Vec3 sp(Vec3 c, double rx, double ry, double rz, double th, double ph) {
        return c.add(rx * Math.sin(th) * Math.cos(ph), ry * Math.cos(th), rz * Math.sin(th) * Math.sin(ph));
    }
}
