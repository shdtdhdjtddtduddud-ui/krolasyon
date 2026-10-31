package com.krolasyon.cille.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** a real round (UV) sphere with smooth normals — used for the marble, the reflect shield and the black hole */
public final class SmoothSphere {
    private SmoothSphere() {}

    public static final ResourceLocation MARBLE_GLASS = CilleRenderer.tex("marble_glass");

    public static void draw(PoseStack pose, VertexConsumer vc, float radius, int slices, int stacks,
                            float r, float g, float b, float a, int light) {
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        for (int i = 0; i < stacks; i++) {
            float v0 = i / (float) stacks, v1 = (i + 1) / (float) stacks;
            float lat0 = Mth.HALF_PI - v0 * Mth.PI, lat1 = Mth.HALF_PI - v1 * Mth.PI;
            for (int j = 0; j < slices; j++) {
                float u0 = j / (float) slices, u1 = (j + 1) / (float) slices;
                float lon0 = u0 * Mth.TWO_PI, lon1 = u1 * Mth.TWO_PI;
                vert(m, n, vc, radius, lat0, lon0, u0, v0, r, g, b, a, light);
                vert(m, n, vc, radius, lat1, lon0, u0, v1, r, g, b, a, light);
                vert(m, n, vc, radius, lat1, lon1, u1, v1, r, g, b, a, light);
                vert(m, n, vc, radius, lat0, lon1, u1, v0, r, g, b, a, light);
            }
        }
    }

    private static void vert(Matrix4f m, Matrix3f nm, VertexConsumer vc, float radius, float lat, float lon, float u, float v,
                             float r, float g, float b, float a, int light) {
        float cx = Mth.cos(lat) * Mth.cos(lon), cy = Mth.sin(lat), cz = Mth.cos(lat) * Mth.sin(lon);
        vc.vertex(m, cx * radius, cy * radius, cz * radius).color(r, g, b, a).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light).normal(nm, cx, cy, cz).endVertex();
    }

    /**
     * Glass marble of the given radius centred at the pose origin, with its tiny Cille curled up and turning slowly
     * inside it. `roll` tumbles the Cille while the marble rolls; the glass highlight stays put like real glass.
     */
    public static void marble(PoseStack pose, MultiBufferSource buffers, MiniCille mini, float radius, float age, float roll, int light) {
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(roll));
        pose.mulPose(Axis.YP.rotationDegrees(age * 3.5F));
        pose.mulPose(Axis.ZP.rotationDegrees(Mth.sin(age * 0.05F) * 12F));
        float s = radius * mini.scale;
        pose.scale(s, s, s);
        pose.scale(-1F, -1F, 1F);
        pose.translate(0, mini.offY, mini.offZ);
        mini.model.poseStatic(age / 20F);
        mini.model.renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(mini.texture)), light, OverlayTexture.NO_OVERLAY, mini.r, mini.g, mini.b, 1F);
        pose.popPose();

        draw(pose, buffers.getBuffer(RenderType.entityTranslucent(MARBLE_GLASS)), radius, 28, 18, mini.gr, mini.gg, mini.gb, 1F, light);
    }
}
