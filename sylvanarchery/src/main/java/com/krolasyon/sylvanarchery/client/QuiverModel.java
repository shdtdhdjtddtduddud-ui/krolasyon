package com.krolasyon.sylvanarchery.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * Orman Sadağı: yeşil deri silindir, altın dikişler, üstte turkuaz taşlı altın plakalı bant, çapraz kayışlar,
 * tokalı alt kemer ve tüyleri yukarıda duran oklar (doluluk oranına göre).
 */
public final class QuiverModel {
    private QuiverModel() {}

    public static final float R = 0.15F;

    public static void render(PoseStack ps, MultiBufferSource buf, int light, float time, int arrows) {
        VertexConsumer vc = buf.getBuffer(Mesh.TYPES[Mesh.CUT]);
        TextureAtlasSprite leather = Mesh.sprite(Mesh.LEATHER), belt = Mesh.sprite(Mesh.BELT), gem = Mesh.sprite(Mesh.GEM);
        TextureAtlasSprite gold = Mesh.sprite(Mesh.GOLD), wrap = Mesh.sprite(Mesh.WRAP);

        // gövde
        Mesh.tube(vc, ps, new float[][]{{0, -0.34F, 0, R * 0.92F}, {0, -0.31F, 0, R}, {0, 0.30F, 0, R}}, 16, leather, 1, 1, 1, 1, light, 1);
        // alt kapak ve iç karanlık
        disc(vc, ps, -0.34F, R * 0.92F, wrap, -1, light);
        disc(vc, ps, 0.24F, R * 0.97F, wrap, 1, light);
        // kemerler
        Mesh.band(vc, ps, R + 0.008F, 0.265F, 0, 0.075F, 20, belt, light);
        Mesh.band(vc, ps, R + 0.010F, 0.03F, 0.5F, 0.055F, 20, belt, light);
        Mesh.band(vc, ps, R + 0.012F, -0.03F, -0.5F, 0.055F, 20, belt, light);
        Mesh.band(vc, ps, R + 0.008F, -0.24F, 0, 0.06F, 20, belt, light);
        // taşlı plakalar (üst bant) + çapraz kayış kesişimi + toka
        for (int i = 0; i < 6; i++) plate(vc, ps, i * 60F, 0.265F, 0.05F, gem, light);
        plate(vc, ps, 90, 0.0F, 0.04F, gem, light);
        plate(vc, ps, 270, 0.0F, 0.04F, gem, light);
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(-90));
        Mesh.beam(vc, ps, new float[]{-0.035F, -0.24F, R + 0.02F}, new float[]{0.035F, -0.24F, R + 0.02F}, 0.035F, 0.008F, 0.035F, 0.008F, gold, 1, 1, 1, light);
        ps.popPose();

        // oklar
        int n = Math.min(11, arrows);
        for (int i = 0; i < n; i++) {
            ps.pushPose();
            double a = i * 2.39996;
            float rr = i == 0 ? 0 : 0.035F + 0.075F * (float) Math.sqrt(i / 11.0);
            ps.translate(Math.cos(a) * rr, 0.2F + 0.025F * (i % 3), Math.sin(a) * rr);
            ps.mulPose(Axis.YP.rotationDegrees((float) Math.toDegrees(a)));
            ps.mulPose(Axis.ZP.rotationDegrees(-90 + (float) (Math.cos(a) * rr * 60)));
            ps.scale(0.75F, 0.75F, 0.75F);
            ArrowModel.render(vc, ps, light, time + i * 7, 0.3F);
            ps.popPose();
        }
    }

    private static void disc(VertexConsumer vc, PoseStack ps, float y, float r, TextureAtlasSprite s, float ny, int light) {
        int seg = 16;
        for (int i = 0; i < seg; i++) {
            double a0 = Math.PI * 2 * i / seg, a1 = Math.PI * 2 * (i + 1) / seg;
            Mesh.face(vc, ps, new float[][]{{0, y, 0, 0.5F, 0.5F}, {0, y, 0, 0.5F, 0.5F},
                    {(float) Math.cos(a1) * r, y, (float) Math.sin(a1) * r, 1, 0}, {(float) Math.cos(a0) * r, y, (float) Math.sin(a0) * r, 0, 0}}, s,
                    ny > 0 ? 0.35F : 0.8F, ny > 0 ? 0.35F : 0.8F, ny > 0 ? 0.35F : 0.8F, light);
        }
    }

    private static void plate(VertexConsumer vc, PoseStack ps, float yawDeg, float y, float size, TextureAtlasSprite s, int light) {
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(yawDeg));
        float z = R + 0.02F;
        Mesh.face(vc, ps, new float[][]{{-size, y - size, z, 0, 1}, {size, y - size, z, 1, 1}, {size, y + size, z, 1, 0}, {-size, y + size, z, 0, 0}}, s, 1, 1, 1, light);
        ps.popPose();
    }
}
