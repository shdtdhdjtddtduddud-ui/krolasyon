package com.krolasyon.ben10.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** small immediate quad helpers for effect renderers */
public final class Quads {
    public static final int FULL = 0xF000F0;

    private Quads() {}

    public static void v(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y, float z, float u, float v, float r, float g, float b, float a, int light) {
        vc.vertex(m, x, y, z).color(r, g, b, a).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0, 1, 0).endVertex();
    }

    /** camera-facing square of half-size s */
    public static void billboard(PoseStack ps, VertexConsumer vc, float s, int rgb, float a, int light) {
        ps.pushPose();
        ps.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        quadXY(ps, vc, s, rgb, a, light, 0, 0, 1, 1);
        ps.popPose();
    }

    public static void quadXY(PoseStack ps, VertexConsumer vc, float s, int rgb, float a, int light, float u0, float v0, float u1, float v1) {
        Matrix4f m = ps.last().pose();
        Matrix3f n = ps.last().normal();
        float r = (rgb >> 16 & 255) / 255F, g = (rgb >> 8 & 255) / 255F, b = (rgb & 255) / 255F;
        v(vc, m, n, -s, -s, 0, u0, v1, r, g, b, a, light);
        v(vc, m, n, s, -s, 0, u1, v1, r, g, b, a, light);
        v(vc, m, n, s, s, 0, u1, v0, r, g, b, a, light);
        v(vc, m, n, -s, s, 0, u0, v0, r, g, b, a, light);
    }

    /** horizontal square (both faces) of half-size s at y */
    public static void flat(PoseStack ps, VertexConsumer vc, float s, float y, int rgb, float a, int light) {
        Matrix4f m = ps.last().pose();
        Matrix3f n = ps.last().normal();
        float r = (rgb >> 16 & 255) / 255F, g = (rgb >> 8 & 255) / 255F, b = (rgb & 255) / 255F;
        v(vc, m, n, -s, y, -s, 0, 0, r, g, b, a, light);
        v(vc, m, n, -s, y, s, 0, 1, r, g, b, a, light);
        v(vc, m, n, s, y, s, 1, 1, r, g, b, a, light);
        v(vc, m, n, s, y, -s, 1, 0, r, g, b, a, light);
        v(vc, m, n, s, y, -s, 1, 0, r, g, b, a, light);
        v(vc, m, n, s, y, s, 1, 1, r, g, b, a, light);
        v(vc, m, n, -s, y, s, 0, 1, r, g, b, a, light);
        v(vc, m, n, -s, y, -s, 0, 0, r, g, b, a, light);
    }

    /** vertical double-sided plane of width w, from y0 to y1, with scrolling v */
    public static void vertical(PoseStack ps, VertexConsumer vc, float w, float y0, float y1, float vOff, float vRep, int rgb, float a, int light) {
        Matrix4f m = ps.last().pose();
        Matrix3f n = ps.last().normal();
        float r = (rgb >> 16 & 255) / 255F, g = (rgb >> 8 & 255) / 255F, b = (rgb & 255) / 255F;
        float h = w / 2;
        v(vc, m, n, -h, y0, 0, 0, vOff + vRep, r, g, b, a, light);
        v(vc, m, n, h, y0, 0, 1, vOff + vRep, r, g, b, a, light);
        v(vc, m, n, h, y1, 0, 1, vOff, r, g, b, a, light);
        v(vc, m, n, -h, y1, 0, 0, vOff, r, g, b, a, light);
        v(vc, m, n, -h, y1, 0, 0, vOff, r, g, b, a, light);
        v(vc, m, n, h, y1, 0, 1, vOff, r, g, b, a, light);
        v(vc, m, n, h, y0, 0, 1, vOff + vRep, r, g, b, a, light);
        v(vc, m, n, -h, y0, 0, 0, vOff + vRep, r, g, b, a, light);
    }

    /** a beam (two crossed planes) from origin along +z for length L */
    public static void beam(PoseStack ps, VertexConsumer vc, float width, float L, float vOff, int rgb, float a, int light) {
        Matrix4f m = ps.last().pose();
        Matrix3f n = ps.last().normal();
        float r = (rgb >> 16 & 255) / 255F, g = (rgb >> 8 & 255) / 255F, b = (rgb & 255) / 255F;
        float h = width / 2;
        float rep = L / Math.max(0.1F, width * 2);
        for (int k = 0; k < 2; k++) {
            float x = k == 0 ? h : 0, y = k == 0 ? 0 : h;
            v(vc, m, n, -x, -y, 0, 0, vOff, r, g, b, a, light);
            v(vc, m, n, x, y, 0, 1, vOff, r, g, b, a, light);
            v(vc, m, n, x, y, L, 1, vOff + rep, r, g, b, a, light);
            v(vc, m, n, -x, -y, L, 0, vOff + rep, r, g, b, a, light);
            v(vc, m, n, -x, -y, L, 0, vOff + rep, r, g, b, a, light);
            v(vc, m, n, x, y, L, 1, vOff + rep, r, g, b, a, light);
            v(vc, m, n, x, y, 0, 1, vOff, r, g, b, a, light);
            v(vc, m, n, -x, -y, 0, 0, vOff, r, g, b, a, light);
        }
    }
}
