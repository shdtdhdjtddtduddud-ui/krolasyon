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
    private static final ResourceLocation BLOOD = tex("blood_spike"), BLOOD_GLOW = tex("blood_spike_glow");
    private static final ResourceLocation THORN = tex("thorn_spike"), THORN_GLOW = tex("thorn_spike_glow");
    private final SimpleEffectModel<EruptionEntity> crystal;
    private final SimpleEffectModel<EruptionEntity> fire;
    private final SimpleEffectModel<EruptionEntity> blood;
    private final SimpleEffectModel<EruptionEntity> thorn;

    public EruptionRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.crystal = new SimpleEffectModel<>(ctx.bakeLayer(SimpleEffectModel.CRYSTAL_SPIKE));
        this.fire = new SimpleEffectModel<>(ctx.bakeLayer(SimpleEffectModel.FIRE_PILLAR));
        this.blood = new SimpleEffectModel<>(ctx.bakeLayer(SimpleEffectModel.BLOOD_SPIKE));
        this.thorn = new SimpleEffectModel<>(ctx.bakeLayer(SimpleEffectModel.THORN_SPIKE));
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
        int kind = e.getKind();
        boolean flicker = kind == EruptionEntity.KIND_FIRE;
        float age = e.tickCount + partial;
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-e.getYRot()));
        float w = flicker ? 0.9F + 0.12F * Mth.sin(age * 1.7F) : 0.8F * Math.min(1F, s + 0.3F);
        float h = flicker ? (0.85F + 0.2F * Mth.sin(age * 2.3F)) * s : (kind == EruptionEntity.KIND_BLOOD ? 0.9F : 0.8F) * s;
        pose.scale(w, h, w);
        pose.scale(-1.0F, -1.0F, 1.0F);
        pose.translate(0.0F, -1.501F, 0.0F);
        SimpleEffectModel<EruptionEntity> model = switch (kind) {
            case EruptionEntity.KIND_CRYSTAL -> crystal;
            case EruptionEntity.KIND_BLOOD -> blood;
            case EruptionEntity.KIND_THORN -> thorn;
            default -> fire;
        };
        ResourceLocation tx = getTextureLocation(e);
        ResourceLocation gl = switch (kind) {
            case EruptionEntity.KIND_CRYSTAL -> CRYSTAL_GLOW;
            case EruptionEntity.KIND_BLOOD -> BLOOD_GLOW;
            case EruptionEntity.KIND_THORN -> THORN_GLOW;
            default -> FIRE_GLOW;
        };
        model.renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutout(tx)),
                flicker ? LightTexture.FULL_BRIGHT : light, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F);
        model.renderToBuffer(pose, buffers.getBuffer(RenderType.eyes(gl)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F);
        pose.popPose();
        super.render(e, yaw, partial, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(EruptionEntity e) {
        return switch (e.getKind()) {
            case EruptionEntity.KIND_CRYSTAL -> CRYSTAL;
            case EruptionEntity.KIND_BLOOD -> BLOOD;
            case EruptionEntity.KIND_THORN -> THORN;
            default -> FIRE;
        };
    }
}
