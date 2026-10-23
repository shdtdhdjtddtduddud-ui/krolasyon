package com.krolasyon.mythblades.client;

import com.krolasyon.mythblades.MythBlades;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/** Efekt çizim yardımcıları: ateş dokuları, ışıyan (eklemeli) ve saydam katmanlar, alev animasyonu kareleri. */
public final class FxDraw {
    private FxDraw() {}

    public static final ResourceLocation FIRE = tex("fire_sheet"), SLASH = tex("slash"), SHOCK = tex("shock"), GLOW = tex("glow"),
            SHIELD = tex("shield"), SPEED = tex("speedlines");

    private static ResourceLocation tex(String n) {
        return new ResourceLocation(MythBlades.MODID, "textures/fx/" + n + ".png");
    }

    /** Eklemeli ışık (karanlıkta parlar). */
    public static RenderType glow(ResourceLocation t) {
        return FxTypes.additive(t);
    }

    /** Saydam, ışıktan etkilenmeyen gövde (gündüz de görünür). */
    public static RenderType body(ResourceLocation t) {
        return RenderType.entityTranslucentEmissive(t);
    }

    public static void v(VertexConsumer vc, Matrix4f m, float x, float y, float z, float u, float v, float r, float g, float b, float a) {
        vc.vertex(m, x, y, z).color(r, g, b, a).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(0, 1, 0).endVertex();
    }

    /** Alev animasyonu: 8x4 = 32 kare; kare numarasından {u0, v0, u1, v1}. */
    public static float[] frame(int f) {
        f = Math.floorMod(f, 32);
        float u0 = (f % 8) / 8F, v0 = (f / 8) / 4F;
        return new float[]{u0, v0, u0 + 1 / 8F, v0 + 1 / 4F};
    }
}
