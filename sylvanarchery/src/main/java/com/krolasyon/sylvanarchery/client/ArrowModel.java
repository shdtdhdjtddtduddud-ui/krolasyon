package com.krolasyon.sylvanarchery.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/** Orman Oku: ince gövde, altın baklava uç, üç turkuaz tüy; kuyruk x = -0.5, uç x = +0.62. */
public final class ArrowModel {
    private ArrowModel() {}

    public static void render(VertexConsumer vc, PoseStack ps, int light, float time, float flutter) {
        TextureAtlasSprite shaft = Mesh.sprite(Mesh.SHAFT), gold = Mesh.sprite(Mesh.GOLD), feather = Mesh.sprite(Mesh.FEATHER);
        Mesh.tube(vc, ps, new float[][]{{-0.5F, 0, 0, 0.014F}, {0.42F, 0, 0, 0.014F}}, 6, shaft, 1, 1, 1, 1, light, 1);
        // gez (altın)
        Mesh.tube(vc, ps, new float[][]{{-0.52F, 0, 0, 0.018F}, {-0.48F, 0, 0, 0.018F}}, 6, gold, 1, 1, 1, 1, light, 1);
        Mesh.tube(vc, ps, new float[][]{{0.39F, 0, 0, 0.02F}, {0.43F, 0, 0, 0.02F}}, 6, gold, 1, 1, 1, 1, light, 1);
        // baklava uç: iki dik düzlemde
        for (int k = 0; k < 2; k++) {
            ps.pushPose();
            ps.mulPose(Axis.XP.rotationDegrees(k * 90));
            Mesh.face(vc, ps, new float[][]{{0.64F, 0, 0, 0.5F, 0}, {0.47F, 0.06F, 0, 1, 0.5F}, {0.41F, 0, 0, 0.5F, 1}, {0.47F, -0.06F, 0, 0, 0.5F}},
                    gold, 1, 1, 1, light);
            ps.popPose();
        }
        // tüyler
        for (int k = 0; k < 3; k++) {
            ps.pushPose();
            ps.mulPose(Axis.XP.rotationDegrees(k * 120 + (float) Math.sin(time * 0.3 + k) * 4 * flutter));
            Mesh.face(vc, ps, new float[][]{{-0.48F, 0.012F, 0, 0, 1}, {-0.24F, 0.012F, 0, 0, 0}, {-0.27F, 0.055F, 0, 1, 0.1F}, {-0.48F, 0.085F, 0, 1, 1}},
                    feather, 1, 1, 1, light);
            ps.popPose();
        }
    }
}
