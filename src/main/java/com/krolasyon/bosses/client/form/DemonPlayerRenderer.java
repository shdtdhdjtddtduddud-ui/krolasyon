package com.krolasyon.bosses.client.form;

import com.krolasyon.bosses.KrolasyonBosses;
import com.krolasyon.bosses.client.model.HeartDemonModel;
import com.krolasyon.bosses.client.render.GlowLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Renders a transformed player as the Heartbreaker Demon (with emissive layer and dash afterimages). */
public class DemonPlayerRenderer extends LivingEntityRenderer<AbstractClientPlayer, DemonPlayerModel> {
    public static final ResourceLocation TEXTURE = new ResourceLocation(KrolasyonBosses.MODID, "textures/entity/heart_demon.png");
    public static final ResourceLocation GLOW = new ResourceLocation(KrolasyonBosses.MODID, "textures/entity/heart_demon_glow.png");
    /** model units -> world: the boss is 0.5, the player form is a bit smaller (≈2.6 blocks to the horn tips) */
    public static final float SCALE = 0.4F;

    public DemonPlayerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new DemonPlayerModel(ctx.bakeLayer(HeartDemonModel.LAYER)), 0.75F);
        this.addLayer(new GlowLayer<>(this, GLOW));
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractClientPlayer player) { return TEXTURE; }

    @Override
    protected void scale(AbstractClientPlayer player, PoseStack pose, float partial) {
        pose.scale(SCALE, SCALE, SCALE);
    }

    @Override
    protected void setupRotations(AbstractClientPlayer p, PoseStack pose, float ageInTicks, float yaw, float partial) {
        super.setupRotations(p, pose, ageInTicks, yaw, partial);
        float swim = p.getSwimAmount(partial);
        if (swim > 0F && !p.isFallFlying()) {
            float target = p.isInWater() ? -90.0F - p.getXRot() : -90.0F;
            pose.mulPose(Axis.XP.rotationDegrees(Mth.lerp(swim, 0.0F, target)));
            if (p.isVisuallySwimming()) pose.translate(0.0F, -1.0F, 0.3F);
        }
    }

    @Override
    protected void renderNameTag(AbstractClientPlayer player, Component name, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.translate(0.0F, 0.6F, 0.0F);
        super.renderNameTag(player, name, pose, buffers, light);
        pose.popPose();
    }

    @Override
    public void render(AbstractClientPlayer p, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        super.render(p, yaw, partial, pose, buffers, light);
        DemonClient.Data d = DemonClient.get(p);
        if (d == null || d.ghosts.isEmpty() || p.isInvisible()) return;
        double px = Mth.lerp(partial, p.xo, p.getX());
        double py = Mth.lerp(partial, p.yo, p.getY());
        double pz = Mth.lerp(partial, p.zo, p.getZ());
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(TEXTURE));
        for (DemonClient.Ghost g : d.ghosts) {
            float life = 1F - (g.age + partial) / DemonClient.GHOST_LIFE;
            if (life <= 0F) continue;
            pose.pushPose();
            pose.translate(g.x - px, g.y - py, g.z - pz);
            pose.mulPose(Axis.YP.rotationDegrees(180.0F - g.yaw));
            pose.scale(-1.0F, -1.0F, 1.0F);
            pose.scale(SCALE, SCALE, SCALE);
            pose.translate(0.0F, -1.501F, 0.0F);
            this.model.renderToBuffer(pose, vc, 0xF000F0, OverlayTexture.NO_OVERLAY, 1.0F, 0.2F, 0.3F, 0.4F * life);
            pose.popPose();
        }
    }
}
