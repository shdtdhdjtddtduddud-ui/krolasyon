package com.krolasyon.bosses.client.render;

import com.krolasyon.bosses.entity.TsunamiWaveEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Expanding ring wall of water with a foam crest, leaning outward. */
public class TsunamiWaveRenderer extends EntityRenderer<TsunamiWaveEntity> {
    public TsunamiWaveRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public boolean shouldRender(TsunamiWaveEntity e, Frustum f, double x, double y, double z) { return true; }

    @Override
    public void render(TsunamiWaveEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        float t = e.tickCount + partial;
        float r = e.radius(t);
        float h = e.height(t);
        float fade = Mth.clamp(1F - (t - TsunamiWaveEntity.LIFE) / 8F, 0F, 1F);
        if (fade <= 0.01F) return;
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucentEmissive(tex(e)));
        int seg = 48;
        // main wall leaning outward
        TideVortexRenderer.ring(pose, vc, r - 0.9F, r + 0.4F, 0F, Math.max(0.2F, h), t * 0.03F, 0.85F * fade, seg);
        // inner trailing wall, lower
        TideVortexRenderer.ring(pose, vc, r - 2.4F, r - 1.2F, 0F, Math.max(0.1F, h * 0.5F), -t * 0.02F, 0.55F * fade, seg);
        // wet ground behind the wave
        TideVortexRenderer.ring(pose, vc, Math.max(0.2F, r - 3.5F), r - 0.8F, 0.04F, 0.06F, t * 0.01F, 0.45F * fade, seg);
        super.render(e, yaw, partial, pose, buffers, light);
    }

    private static final ResourceLocation FIRE = EruptionRenderer.tex("fire_wave"), SHADOW = EruptionRenderer.tex("shadow_wave");

    static ResourceLocation tex(TsunamiWaveEntity e) {
        return switch (e.getKind()) { case TsunamiWaveEntity.FIRE -> FIRE; case TsunamiWaveEntity.SHADOW -> SHADOW; default -> TideVortexRenderer.WAVE; };
    }

    @Override
    public ResourceLocation getTextureLocation(TsunamiWaveEntity e) { return tex(e); }
}
