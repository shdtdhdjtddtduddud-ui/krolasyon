package com.krolasyon.furniture.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** shared drawing + easing helpers. Model space: 1 unit = 1/32 block, ground at y = 48, front = -z. */
public final class FurnitureDraw {
    private FurnitureDraw() {}

    /** draws a ModelPart tree whose origin is the ground centre of the footprint, positioned at (cx, cz) inside the block */
    public static void draw(ModelPart root, ResourceLocation tex, PoseStack ps, MultiBufferSource buf, int light, int overlay, double cx, double cz, Direction facing) {
        ps.pushPose();
        ps.translate(cx, 0.0D, cz);
        ps.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        ps.translate(0.0D, 1.5D, 0.0D);
        ps.scale(-0.5F, -0.5F, 0.5F);
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(tex));
        root.render(ps, vc, light, overlay);
        ps.popPose();
    }

    public static float easeOutBack(float t) {
        float c1 = 1.2F;
        float c3 = c1 + 1.0F;
        float u = t - 1.0F;
        return 1.0F + c3 * u * u * u + c1 * u * u;
    }

    public static float smooth(float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    /** opening gets a little overshoot, closing is a plain smooth curve */
    public static float openEase(float prev, float cur, float partial) {
        float v = Mth.clamp(Mth.lerp(partial, prev, cur), 0.0F, 1.0F);
        return cur >= prev && v < 1.0F ? Math.max(0.0F, easeOutBack(v)) : smooth(v);
    }

    public static float sub(float v, float start) {
        return Mth.clamp((v - start) / (1.0F - start), 0.0F, 1.0F);
    }
}
