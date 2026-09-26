package com.krolasyon.bosses.client.render;

import com.krolasyon.bosses.KrolasyonBosses;
import com.krolasyon.bosses.client.model.SimpleEffectModel;
import com.krolasyon.bosses.entity.EruptionEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class EruptionRenderer extends EntityRenderer<EruptionEntity> {
    private static final ResourceLocation CRYSTAL = tex("crystal_spike"), CRYSTAL_GLOW = tex("crystal_spike_glow");
    private static final ResourceLocation FIRE = tex("fire_pillar"), FIRE_GLOW = tex("fire_pillar_glow");
    private final SimpleEffectModel<EruptionEntity> crystal;
    private final SimpleEffectModel<EruptionEntity> fire;

    public EruptionRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.crystal = new SimpleEffectModel<>(ctx.bakeLayer(SimpleEffectModel.CRYSTAL_SPIKE));
        this.fire = new SimpleEffectModel<>(ctx.bakeLayer(SimpleEffectModel.FIRE_PILLAR));
    }

    static ResourceLocation tex(String n) {
        return new ResourceLocation(KrolasyonBosses.MODID, "textures/entity/" + n + ".png");
    }

    @Override
    public void render(EruptionEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        float t = e.tickCount + partial - e.getWarmup();
        if (t < 0) return;
        float s;
        if (t < 3F) s = Mth.sin(t / 3F * Mth.HALF_PI) * 1.15F;
        else if (t < 5F) s = 1.15F - (t - 3F) / 2F * 0.15F;
        else if (t < 14F) s = 1F;
        else s = Mth.clamp(1F - (t - 14F) / 7F, 0F, 1F);
        if (s <= 0.01F) return;
        boolean isCrystal = e.getKind() == EruptionEntity.KIND_CRYSTAL;
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-e.getYRot()));
        float w = isCrystal ? 0.8F : 0.9F + 0.12F * Mth.sin((e.tickCount + partial) * 1.7F);
        float h = isCrystal ? 0.8F * s : (0.85F + 0.2F * Mth.sin((e.tickCount + partial) * 2.3F)) * s;
        pose.scale(w * (isCrystal ? Math.min(1F, s + 0.3F) : 1F), h, w * (isCrystal ? Math.min(1F, s + 0.3F) : 1F));
        pose.scale(-1.0F, -1.0F, 1.0F);
        pose.translate(0.0F, -1.501F, 0.0F);
        SimpleEffectModel<EruptionEntity> model = isCrystal ? crystal : fire;
        model.renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutout(isCrystal ? CRYSTAL : FIRE)),
                isCrystal ? light : LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F);
        model.renderToBuffer(pose, buffers.getBuffer(RenderType.eyes(isCrystal ? CRYSTAL_GLOW : FIRE_GLOW)),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F);
        pose.popPose();
        super.render(e, yaw, partial, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(EruptionEntity e) {
        return e.getKind() == EruptionEntity.KIND_CRYSTAL ? CRYSTAL : FIRE;
    }
}
