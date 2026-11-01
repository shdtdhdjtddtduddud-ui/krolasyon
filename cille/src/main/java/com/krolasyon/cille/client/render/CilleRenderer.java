package com.krolasyon.cille.client.render;

import com.krolasyon.cille.CilleMod;
import com.krolasyon.cille.client.model.CilleModel;
import com.krolasyon.cille.entity.CilleEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** shared Cille renderer: own death animation (no flip), glowing layer, technique beam and shield shells */
public abstract class CilleRenderer<T extends CilleEntity, M extends CilleModel<T>> extends MobRenderer<T, M> {
    public static final ResourceLocation WHITE = tex("white");
    protected static final ResourceLocation SHIELD = tex("shield_sphere");
    protected static final ResourceLocation ELECTRIC = tex("electric_sphere");
    private final ResourceLocation texture;
    private final float scale;

    protected CilleRenderer(EntityRendererProvider.Context ctx, M model, String name, float shadow, float scale) {
        super(ctx, model, shadow);
        // smooth meshes carry their own vertex colours and glow; they are drawn over a plain white texture
        this.texture = WHITE;
        this.scale = scale;
    }

    public static ResourceLocation tex(String n) {
        return new ResourceLocation(CilleMod.MODID, "textures/entity/" + n + ".png");
    }

    @Override
    public ResourceLocation getTextureLocation(T e) { return texture; }

    @Override
    protected void scale(T e, PoseStack pose, float partial) { pose.scale(scale, scale, scale); }

    @Override
    protected void setupRotations(T e, PoseStack pose, float ageInTicks, float yaw, float partial) {
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
    }

    @Override
    protected float getFlipDegrees(T e) { return 0F; }

    /** mouth offset (blocks) relative to the entity position, in the body's facing frame: forward, up */
    protected abstract float mouthForward();
    protected abstract float mouthUp();
    /** beam colours: outer, middle, core (rgb) */
    protected abstract float[] beamColors();
    protected float shieldRadius() { return 2.8F; }
    protected float shieldCenter() { return 1.45F; }

    @Override
    public void render(T e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        // hide the first ticks until the entrance animation starts (it grows out of the marble)
        if (e.tickCount < 3 && e.clientAnimId < 0 && e.deathTime == 0) return;
        super.render(e, yaw, partial, pose, buffers, light);
        float age = e.tickCount + partial;
        float bodyYaw = Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot) * Mth.DEG_TO_RAD;
        Vec3 fwd = new Vec3(-Mth.sin(bodyYaw), 0, Mth.cos(bodyYaw));

        int beam = e.getBeamTarget();
        if (beam >= 0) {
            Entity t = e.level().getEntity(beam);
            if (t != null) {
                Vec3 origin = e.getPosition(partial);
                Vec3 mouth = fwd.scale(mouthForward()).add(0, mouthUp(), 0);
                Vec3 to = t.getPosition(partial).add(0, t.getBbHeight() * 0.55, 0).subtract(origin);
                VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
                float pulse = 0.75F + 0.25F * Mth.sin(age * 0.9F);
                float[] c = beamColors();
                FxUtil.beam(pose.last().pose(), vc, mouth, to, 0.45F * pulse, c[0], c[1], c[2], 0.35F);
                FxUtil.beam(pose.last().pose(), vc, mouth, to, 0.22F, c[3], c[4], c[5], 0.6F);
                FxUtil.beam(pose.last().pose(), vc, mouth, to, 0.08F, c[6], c[7], c[8], 0.9F);
            }
        }

        int sh = e.getShield();
        if (sh != CilleEntity.SHIELD_NONE && e.isAlive()) {
            pose.pushPose();
            pose.translate(0, shieldCenter(), 0);
            float s = shieldRadius() * (1F + 0.03F * Mth.sin(age * 0.6F));
            pose.mulPose(Axis.YP.rotationDegrees(age * 3F));
            pose.scale(1F, 0.85F, 1F);
            float r, g, b, a;
            switch (sh) {
                case CilleEntity.SHIELD_REFLECT -> { r = 0.95F; g = 0.85F; b = 1.0F; a = 0.8F; }
                case CilleEntity.SHIELD_ICE -> { r = 0.7F; g = 0.95F; b = 1.0F; a = 0.85F; }
                case CilleEntity.SHIELD_SAND -> { r = 1.0F; g = 0.78F; b = 0.42F; a = 0.85F; }
                default -> { r = 0.35F; g = 0.32F; b = 0.6F; a = 0.65F; }
            }
            if (sh == CilleEntity.SHIELD_ELECTRIC) {
                // crackling lightning shell: flickers and re-rolls its pattern every few ticks
                pose.mulPose(Axis.XP.rotationDegrees((e.tickCount / 2) * 47F));
                float fl = 0.75F + 0.25F * ((e.tickCount * 7) % 5) / 4F;
                SmoothSphere.draw(pose, buffers.getBuffer(RenderType.entityTranslucentEmissive(ELECTRIC)), s, 32, 20, 0.8F * fl, 0.9F * fl, 1.0F, fl, LightTexture.FULL_BRIGHT);
            } else {
                SmoothSphere.draw(pose, buffers.getBuffer(RenderType.entityTranslucent(SHIELD)), s, 32, 20, r, g, b, a, LightTexture.FULL_BRIGHT);
            }
            pose.popPose();
        }
    }
}
