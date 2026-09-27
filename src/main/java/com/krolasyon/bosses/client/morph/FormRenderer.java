package com.krolasyon.bosses.client.morph;

import com.krolasyon.bosses.KrolasyonBosses;
import com.krolasyon.bosses.morph.Forms;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

/** Draws a transformed player as its form (third person) and the form's arm / weapon (first person). */
public final class FormRenderer {
    private FormRenderer() {}

    static final ResourceLocation[] TEXTURE = new ResourceLocation[Forms.COUNT];
    static final ResourceLocation[] GLOW = new ResourceLocation[Forms.COUNT];
    /** model units -> world: every form stands about 2.8-2.9 blocks tall */
    static final float[] SCALE = {0.46F, 0.47F, 0.46F, 0.46F, 0.46F, 0.47F};
    static final FormModel[] MODELS = new FormModel[Forms.COUNT];

    static {
        for (int i = 0; i < Forms.COUNT; i++) {
            TEXTURE[i] = new ResourceLocation(KrolasyonBosses.MODID, "textures/entity/" + Forms.KEY[i] + ".png");
            GLOW[i] = new ResourceLocation(KrolasyonBosses.MODID, "textures/entity/" + Forms.KEY[i] + "_glow.png");
        }
    }

    public static void bake(EntityModelSet models) {
        for (int i = 0; i < Forms.COUNT; i++) MODELS[i] = new FormModel(models.bakeLayer(FormModel.layer(Forms.KEY[i])), i);
    }

    static boolean ready(int form) { return Forms.valid(form) && MODELS[form] != null; }

    public static void render(AbstractClientPlayer p, ClientMorph.CState st, float partial, PoseStack ps, MultiBufferSource buffers, int light) {
        int f = st.form;
        if (!ready(f)) return;
        FormModel model = MODELS[f];
        boolean invisible = p.isInvisible();
        Minecraft mc = Minecraft.getInstance();
        if (invisible && mc.player != null && p.isInvisibleTo(mc.player)) return;

        ps.pushPose();
        float bodyYaw = Mth.rotLerp(partial, p.yBodyRotO, p.yBodyRot);
        ps.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        float swim = p.getSwimAmount(partial);
        if (p.isFallFlying()) {
            float fl = (float) p.getFallFlyingTicks() + partial;
            float f1 = Mth.clamp(fl * fl / 100.0F, 0.0F, 1.0F);
            if (!p.isAutoSpinAttack()) ps.mulPose(Axis.XP.rotationDegrees(f1 * (-90.0F - p.getXRot())));
        } else if (swim > 0.0F) {
            float target = p.isInWater() ? -90.0F - p.getXRot() : -90.0F;
            ps.mulPose(Axis.XP.rotationDegrees(Mth.lerp(swim, 0.0F, target)));
            if (p.isVisuallySwimming()) ps.translate(0.0F, -1.0F, 0.3F);
        }
        ps.scale(-1.0F, -1.0F, 1.0F);
        // the inventory preview draws the player full-bright inside a container screen: shrink so the whole form fits
        float sc = SCALE[f];
        if (light == LightTexture.FULL_BRIGHT && mc.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>) sc *= 0.6F;
        ps.scale(sc, sc, sc);
        ps.translate(0.0F, -1.501F, 0.0F);

        model.setup(p, st, partial);
        int overlay = LivingEntityRenderer.getOverlayCoords(p, 0.0F);
        float alpha = invisible ? 0.15F : 1.0F;
        RenderType rt = invisible ? RenderType.itemEntityTranslucentCull(TEXTURE[f]) : RenderType.entityCutoutNoCull(TEXTURE[f]);
        model.renderToBuffer(ps, buffers.getBuffer(rt), light, overlay, 1.0F, 1.0F, 1.0F, alpha);
        if (!invisible) model.renderToBuffer(ps, buffers.getBuffer(RenderType.eyes(GLOW[f])), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        ps.popPose();
    }

    /** first person: the form's arm (and weapon, shrunk) in place of the vanilla hand */
    public static void renderArm(AbstractClientPlayer p, PoseStack ps, MultiBufferSource buffers, int light, float equip, float swing, HumanoidArm side) {
        ClientMorph.CState st = ClientMorph.get(p.getId());
        if (st == null || !ready(st.form)) return;
        FormModel model = MODELS[st.form];
        ps.pushPose();
        boolean right = side != HumanoidArm.LEFT;
        float f = right ? 1.0F : -1.0F;
        float f1 = Mth.sqrt(swing);
        float f2 = -0.3F * Mth.sin(f1 * (float) Math.PI);
        float f3 = 0.4F * Mth.sin(f1 * ((float) Math.PI * 2F));
        float f4 = -0.4F * Mth.sin(swing * (float) Math.PI);
        ps.translate(f * (f2 + 0.64000005F), f3 - 0.6F + equip * -0.6F, f4 - 0.71999997F);
        ps.mulPose(Axis.YP.rotationDegrees(f * 45.0F));
        float f5 = Mth.sin(swing * swing * (float) Math.PI);
        float f6 = Mth.sin(f1 * (float) Math.PI);
        ps.mulPose(Axis.YP.rotationDegrees(f * f6 * 70.0F));
        ps.mulPose(Axis.ZP.rotationDegrees(f * f5 * -20.0F));
        ps.translate(f * -1.0F, 3.6F, 3.5F);
        ps.mulPose(Axis.ZP.rotationDegrees(f * 120.0F));
        ps.mulPose(Axis.XP.rotationDegrees(200.0F));
        ps.mulPose(Axis.YP.rotationDegrees(f * -135.0F));
        ps.translate(f * 5.6F, 0.0F, 0.0F);
        if (!right) ps.scale(-1.0F, 1.0F, 1.0F);

        model.setup(p, st, Minecraft.getInstance().getFrameTime());
        ModelPart arm = model.rightArm;
        float ox = arm.x, oy = arm.y, oz = arm.z, rx = arm.xRot, ry = arm.yRot, rz = arm.zRot;
        arm.x = -5.0F / 0.42F;
        arm.y = 1.0F / 0.42F;
        arm.z = 0.0F;
        arm.xRot = 0.0F;
        arm.yRot = 0.0F;
        arm.zRot = 0.1F;
        if (model.rightPauldron != null) model.rightPauldron.visible = false;
        ModelPart w = model.weapon;
        if (w != null) {
            // blade forward like a held sword, smaller so it does not cover the screen
            w.xRot = -1.35F;
            w.yRot = 0.0F;
            w.zRot = 0.0F;
            w.xScale = w.yScale = w.zScale = 0.55F;
        }
        ps.scale(0.42F, 0.42F, 0.42F);
        arm.render(ps, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE[st.form])), light, OverlayTexture.NO_OVERLAY);
        arm.render(ps, buffers.getBuffer(RenderType.eyes(GLOW[st.form])), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        if (model.rightPauldron != null) model.rightPauldron.visible = true;
        if (w != null) w.xScale = w.yScale = w.zScale = 1.0F;
        arm.x = ox; arm.y = oy; arm.z = oz; arm.xRot = rx; arm.yRot = ry; arm.zRot = rz;
        ps.popPose();
    }
}
