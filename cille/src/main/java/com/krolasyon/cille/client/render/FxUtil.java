package com.krolasyon.cille.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class FxUtil {
    private FxUtil() {}

    /** additive beam made of two crossed quads (use with RenderType.lightning()) */
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

    /** double sided textured horizontal quad of half-size `s` centred at the pose origin */
    public static void flatQuad(PoseStack pose, VertexConsumer vc, float s, int light, float alpha) {
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        float[][] c = {{-s, -s, 0, 0}, {-s, s, 0, 1}, {s, s, 1, 1}, {s, -s, 1, 0}};
        for (int i = 0; i < 4; i++) tv(m, n, vc, c[i], light, alpha);
        for (int i = 3; i >= 0; i--) tv(m, n, vc, c[i], light, alpha);
    }

    private static void tv(Matrix4f m, Matrix3f n, VertexConsumer vc, float[] c, int light, float alpha) {
        vc.vertex(m, c[0], 0F, c[1]).color(1F, 1F, 1F, alpha).uv(c[2], c[3]).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light).normal(n, 0F, 1F, 0F).endVertex();
    }
}
