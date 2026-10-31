package com.krolasyon.cille.client.render;

import com.krolasyon.cille.CilleMod;
import com.krolasyon.cille.client.model.KabkabaModel;
import com.krolasyon.cille.entity.KabkabaEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class KabkabaRenderer extends MobRenderer<KabkabaEntity, KabkabaModel> {
    private static final ResourceLocation TEXTURE = tex("kabkaba");
    private static final ResourceLocation SHIELD = tex("shield_sphere");
    public static final float SCALE = 0.4F;

    public KabkabaRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new KabkabaModel(ctx.bakeLayer(KabkabaModel.LAYER)), 1.7F);
        this.addLayer(new GlowLayer<>(this, tex("kabkaba_glow")));
    }

    static ResourceLocation tex(String n) {
        return new ResourceLocation(CilleMod.MODID, "textures/entity/" + n + ".png");
    }

    @Override
    public ResourceLocation getTextureLocation(KabkabaEntity e) { return TEXTURE; }

    @Override
    protected void scale(KabkabaEntity e, PoseStack pose, float partial) {
        pose.scale(SCALE, SCALE, SCALE);
    }

    /** no vanilla sideways death flip: the model plays its own death animation */
    @Override
    protected void setupRotations(KabkabaEntity e, PoseStack pose, float ageInTicks, float yaw, float partial) {
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
    }

    @Override
    protected float getFlipDegrees(KabkabaEntity e) { return 0F; }

    @Override
    public void render(KabkabaEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        // hide the first ticks until the entrance animation starts (it grows out of the marble)
        if (e.tickCount < 3 && e.clientAnimId < 0 && e.deathTime == 0) return;
        super.render(e, yaw, partial, pose, buffers, light);
        float age = e.tickCount + partial;
        float bodyYaw = Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot) * Mth.DEG_TO_RAD;
        Vec3 fwd = new Vec3(-Mth.sin(bodyYaw), 0, Mth.cos(bodyYaw));

        // energy drain beam: from the victim into Kabkaba's open jaws
        int beam = e.getBeamTarget();
        if (beam >= 0) {
            Entity t = e.level().getEntity(beam);
            if (t != null) {
                Vec3 origin = e.getPosition(partial);
                Vec3 mouth = fwd.scale(3.1).add(0, 1.55, 0);
                Vec3 to = t.getPosition(partial).add(0, t.getBbHeight() * 0.55, 0).subtract(origin);
                VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
                float pulse = 0.75F + 0.25F * Mth.sin(age * 0.9F);
                FxUtil.beam(pose.last().pose(), vc, mouth, to, 0.42F * pulse, 0.35F, 0.12F, 0.6F, 0.35F);
                FxUtil.beam(pose.last().pose(), vc, mouth, to, 0.2F, 0.6F, 0.4F, 1.0F, 0.6F);
                FxUtil.beam(pose.last().pose(), vc, mouth, to, 0.07F, 0.95F, 0.9F, 1.0F, 0.9F);
            }
        }

        // reflect / guard shell
        int sh = e.getShield();
        if (sh != KabkabaEntity.SHIELD_NONE && e.isAlive()) {
            pose.pushPose();
            pose.translate(0, 1.45, 0);
            float s = (sh == KabkabaEntity.SHIELD_REFLECT ? 2.8F : 2.6F) * (1F + 0.03F * Mth.sin(age * 0.6F));
            pose.mulPose(Axis.YP.rotationDegrees(age * 3F));
            pose.scale(1F, 0.9F, 1F);
            boolean refl = sh == KabkabaEntity.SHIELD_REFLECT;
            float r = refl ? 0.95F : 0.35F, g = refl ? 0.85F : 0.32F, b = refl ? 1.0F : 0.6F;
            SmoothSphere.draw(pose, buffers.getBuffer(RenderType.entityTranslucent(SHIELD)), s, 32, 20, r, g, b, refl ? 0.8F : 0.65F, LightTexture.FULL_BRIGHT);
            pose.popPose();
        }
    }
}
