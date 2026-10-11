package com.krolasyon.ben10.client;

import com.krolasyon.ben10.Alien;
import com.krolasyon.ben10.Ben10;
import com.krolasyon.ben10.OmniData;
import com.krolasyon.ben10.client.model.AlienModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

import java.util.EnumMap;
import java.util.Map;

/** Draws a transformed player as their alien (third person and the first-person arm). */
public final class AlienRenderer {
    private static final Map<Alien, AlienModel> MODELS = new EnumMap<>(Alien.class);
    private static final Map<Alien, ResourceLocation> TEX = new EnumMap<>(Alien.class);
    private static final Map<Alien, ResourceLocation> GLOW = new EnumMap<>(Alien.class);

    private AlienRenderer() {}

    public static AlienModel model(Alien a) {
        return MODELS.computeIfAbsent(a, k -> {
            try {
                return new AlienModel(Minecraft.getInstance().getEntityModels().bakeLayer(ClientSetup.layer(k.id)));
            } catch (Exception e) {
                Ben10.LOG.error("missing alien model {}", k.id, e);
                return null;
            }
        });
    }

    public static ResourceLocation tex(Alien a) {
        return TEX.computeIfAbsent(a, k -> new ResourceLocation(Ben10.MODID, "textures/alien/" + k.id + ".png"));
    }

    public static ResourceLocation glow(Alien a) {
        return GLOW.computeIfAbsent(a, k -> new ResourceLocation(Ben10.MODID, "textures/alien/" + k.id + "_glow.png"));
    }

    public static void render(AbstractClientPlayer p, OmniData d, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        Alien a = d.alien;
        AlienModel m = model(a);
        if (m == null) return;
        Animator.pose(m, p, d, pt);
        Minecraft mc = Minecraft.getInstance();
        float bodyYaw = Mth.rotLerp(pt, p.yBodyRotO, p.yBodyRot);
        float alpha = 1F;
        if (p.isInvisible()) alpha = mc.player != null && p.isInvisibleTo(mc.player) ? 0F : 0.18F;
        if (d.has(OmniData.F_PHASED)) alpha = Math.min(alpha, p == mc.player ? 0.35F : 0.22F);
        if (d.has(OmniData.F_POSSESS)) alpha = 0F;
        if (alpha <= 0.01F) return;
        float appear = (float) (mc.level == null ? 99 : mc.level.getGameTime() - d.clientTransformTime) + pt;
        float s = appear < 7 ? 0.55F + 0.45F * Mth.sin(appear / 7F * Mth.HALF_PI) : 1F;

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        if (p.deathTime > 0) {
            float f = Mth.sqrt(Math.min(1F, (p.deathTime + pt - 1.0F) / 20.0F * 1.6F));
            ps.mulPose(Axis.ZP.rotationDegrees(f * 90F));
        }
        ps.scale(-1.0F, -1.0F, 1.0F);
        ps.scale(s, s, s);
        ps.translate(0.0F, -1.501F, 0.0F);
        ps.scale(0.5F, 0.5F, 0.5F);
        int overlay = LivingEntityRenderer.getOverlayCoords(p, 0.0F);
        float g = 1F, r = 1F, b = 1F;
        if (appear < 10) {
            float k = 1 - appear / 10F;
            r = 1 - 0.6F * k;
            b = 1 - 0.7F * k;
        }
        RenderType main = alpha < 1F ? RenderType.entityTranslucent(tex(a)) : RenderType.entityCutoutNoCull(tex(a));
        VertexConsumer vc = buf.getBuffer(main);
        m.renderToBuffer(ps, vc, light, overlay, r, g, b, alpha);
        VertexConsumer gl = buf.getBuffer(RenderType.eyes(glow(a)));
        m.renderToBuffer(ps, gl, 0xF000F0, OverlayTexture.NO_OVERLAY, alpha, alpha, alpha, 1F);
        if (appear < 10) {
            // Omnitrix transformation glow shell
            float k = 1 - appear / 10F;
            ps.pushPose();
            float e = 1.04F + 0.1F * k;
            ps.translate(0, 48F / 16F * (1 - e), 0);
            ps.scale(e, e, e);
            m.renderToBuffer(ps, buf.getBuffer(RenderType.eyes(glow(a))), 0xF000F0, OverlayTexture.NO_OVERLAY, 0.3F * k, 1F * k, 0.2F * k, 1F);
            ps.popPose();
        }
        ps.popPose();
    }

    /** first-person arm: pose stack is already placed for the vanilla arm (pivot at -5, 2, 0 pixels) */
    public static void renderArm(AbstractClientPlayer p, OmniData d, HumanoidArm arm, PoseStack ps, MultiBufferSource buf, int light) {
        Alien a = d.alien;
        AlienModel m = model(a);
        if (m == null) return;
        boolean right = arm == HumanoidArm.RIGHT;
        ModelPart part = m.bone(right ? "right_arm" : "left_arm");
        if (part == null) return;
        m.reset();
        Animator.hideFx(m, d, 0);
        float k = Animator.firstPersonScale(a);
        ps.pushPose();
        ps.translate((right ? -5F : 5F) / 16F, 2F / 16F, 0);
        ps.scale(0.5F * k, 0.5F * k, 0.5F * k);
        // render the arm chain around its own pivot
        float px = part.x, py = part.y, pz = part.z;
        part.x = 0;
        part.y = 0;
        part.z = 0;
        part.xRot = 0;
        part.yRot = 0;
        part.zRot = 0;
        part.render(ps, buf.getBuffer(RenderType.entityCutoutNoCull(tex(a))), light, OverlayTexture.NO_OVERLAY);
        part.render(ps, buf.getBuffer(RenderType.eyes(glow(a))), 0xF000F0, OverlayTexture.NO_OVERLAY);
        part.x = px;
        part.y = py;
        part.z = pz;
        ps.popPose();
    }

    /** a static translucent "ghost" of an alien (XLR8 afterimages) at the current pose stack origin */
    public static void renderGhost(Alien a, float yaw, float age, float alpha, PoseStack ps, MultiBufferSource buf) {
        AlienModel m = model(a);
        if (m == null) return;
        m.reset();
        Animator.ghostPose(m, a, age);
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        ps.scale(-1.0F, -1.0F, 1.0F);
        ps.translate(0.0F, -1.501F, 0.0F);
        ps.scale(0.5F, 0.5F, 0.5F);
        int c = a.color;
        m.renderToBuffer(ps, buf.getBuffer(RenderType.eyes(tex(a))), 0xF000F0, OverlayTexture.NO_OVERLAY,
                ((c >> 16) & 255) / 255F * alpha, ((c >> 8) & 255) / 255F * alpha, (c & 255) / 255F * alpha, 1F);
        ps.popPose();
    }
}
