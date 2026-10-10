package com.krolasyon.storm.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/** Procedural jagged lightning geometry drawn as camera facing ribbons (glow + core layers). */
public final class ArcGen {
    private ArcGen() {}

    public static final float[] GLOW = {0.38F, 0.22F, 1.0F};
    public static final float[] MID = {0.25F, 0.62F, 1.0F};
    public static final float[] CORE = {0.88F, 0.97F, 1.0F};

    /** midpoint displacement polyline */
    public static List<Vec3> jag(Vec3 a, Vec3 b, RandomSource r, float jag, int depth) {
        List<Vec3> pts = new ArrayList<>();
        pts.add(a);
        pts.add(b);
        double off = a.distanceTo(b) * jag;
        for (int d = 0; d < depth; d++) {
            List<Vec3> n = new ArrayList<>(pts.size() * 2);
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

    /** full arc: glow, mid and core ribbons, plus forks */
    public static void arc(VertexConsumer vc, Matrix4f m, Vec3 cam, Vec3 a, Vec3 b, long seed, float width, float alpha, float jag, int depth, int forks) {
        RandomSource r = RandomSource.create(seed);
        List<Vec3> pts = jag(a, b, r, jag, depth);
        layers(vc, m, cam, pts, width, alpha);
        for (int f = 0; f < forks && pts.size() > 4; f++) {
            int i = 1 + r.nextInt(pts.size() - 3);
            Vec3 s = pts.get(i);
            Vec3 dir = pts.get(i + 1).subtract(s);
            double len = a.distanceTo(b) * (0.15 + r.nextDouble() * 0.25);
            Vec3 e = s.add(dir.normalize().scale(len * 0.6)).add(perp(dir, r).scale(len * 0.6));
            layers(vc, m, cam, jag(s, e, r, jag, Math.max(1, depth - 2)), width * 0.5F, alpha * 0.8F);
        }
    }

    public static void layers(VertexConsumer vc, Matrix4f m, Vec3 cam, List<Vec3> pts, float width, float alpha) {
        ribbon(vc, m, cam, pts, width * 4.5F, GLOW, alpha * 0.22F);
        ribbon(vc, m, cam, pts, width * 2.0F, MID, alpha * 0.55F);
        ribbon(vc, m, cam, pts, width * 0.7F, CORE, alpha);
    }

    public static void ribbon(VertexConsumer vc, Matrix4f m, Vec3 cam, List<Vec3> pts, float width, float[] c, float alpha) {
        if (alpha <= 0.003F) return;
        for (int i = 0; i < pts.size() - 1; i++) {
            Vec3 p0 = pts.get(i), p1 = pts.get(i + 1);
            Vec3 seg = p1.subtract(p0);
            Vec3 view = p0.add(p1).scale(0.5).subtract(cam);
            Vec3 w = seg.cross(view);
            if (w.lengthSqr() < 1e-10) continue;
            w = w.normalize().scale(width * 0.5);
            // extend segment slightly so joints have no gaps
            Vec3 ext = seg.normalize().scale(width * 0.25);
            Vec3 q0 = p0.subtract(ext), q1 = p1.add(ext);
            v(vc, m, q0.subtract(w), c, alpha);
            v(vc, m, q0.add(w), c, alpha);
            v(vc, m, q1.add(w), c, alpha);
            v(vc, m, q1.subtract(w), c, alpha);
        }
    }

    /** camera facing quad (POSITION_COLOR) */
    public static void glowQuad(VertexConsumer vc, Matrix4f m, Vec3 p, Vec3 right, Vec3 up, float size, float[] c, float alpha) {
        Vec3 rr = right.scale(size), uu = up.scale(size);
        v(vc, m, p.subtract(rr).subtract(uu), c, alpha);
        v(vc, m, p.add(rr).subtract(uu), c, alpha);
        v(vc, m, p.add(rr).add(uu), c, alpha);
        v(vc, m, p.subtract(rr).add(uu), c, alpha);
    }

    /** camera facing textured quad (POSITION_COLOR_TEX) */
    public static void sprite(VertexConsumer vc, Matrix4f m, Vec3 p, Vec3 right, Vec3 up, float size, float r, float g, float b, float alpha) {
        Vec3 rr = right.scale(size), uu = up.scale(size);
        t(vc, m, p.subtract(rr).subtract(uu), r, g, b, alpha, 0, 1);
        t(vc, m, p.add(rr).subtract(uu), r, g, b, alpha, 1, 1);
        t(vc, m, p.add(rr).add(uu), r, g, b, alpha, 1, 0);
        t(vc, m, p.subtract(rr).add(uu), r, g, b, alpha, 0, 0);
    }

    public static void v(VertexConsumer vc, Matrix4f m, Vec3 p, float[] c, float a) {
        vc.vertex(m, (float) p.x, (float) p.y, (float) p.z).color(c[0], c[1], c[2], a).endVertex();
    }

    public static void t(VertexConsumer vc, Matrix4f m, Vec3 p, float r, float g, float b, float a, float u, float v) {
        vc.vertex(m, (float) p.x, (float) p.y, (float) p.z).color(r, g, b, a).uv(u, v).endVertex();
    }
}
