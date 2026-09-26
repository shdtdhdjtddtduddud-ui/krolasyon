package com.krolasyon.bosses.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class BeamRenderer {
    private BeamRenderer() {}

    /** crossed double-sided quads from a to b (POSITION_COLOR, e.g. RenderType.lightning()) */
    public static void beam(Matrix4f m, VertexConsumer vc, Vec3 a, Vec3 b, float radius, float r, float g, float bl, float alpha) {
        Vec3 d = b.subtract(a);
        if (d.lengthSqr() < 1.0E-6) return;
        Vec3 dn = d.normalize();
        Vec3 up = Math.abs(dn.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 u = dn.cross(up).normalize().scale(radius);
        Vec3 v = dn.cross(u).normalize().scale(radius);
        quad(m, vc, a, b, u, r, g, bl, alpha);
        quad(m, vc, a, b, v, r, g, bl, alpha);
    }

    private static void quad(Matrix4f m, VertexConsumer vc, Vec3 a, Vec3 b, Vec3 w, float r, float g, float bl, float alpha) {
        Vec3 p1 = a.add(w), p2 = a.subtract(w), p3 = b.subtract(w), p4 = b.add(w);
        v(m, vc, p1, r, g, bl, alpha); v(m, vc, p2, r, g, bl, alpha); v(m, vc, p3, r, g, bl, alpha); v(m, vc, p4, r, g, bl, alpha);
        v(m, vc, p4, r, g, bl, alpha); v(m, vc, p3, r, g, bl, alpha); v(m, vc, p2, r, g, bl, alpha); v(m, vc, p1, r, g, bl, alpha);
    }

    private static void v(Matrix4f m, VertexConsumer vc, Vec3 p, float r, float g, float b, float a) {
        vc.vertex(m, (float) p.x, (float) p.y, (float) p.z).color(r, g, b, a).endVertex();
    }
}
