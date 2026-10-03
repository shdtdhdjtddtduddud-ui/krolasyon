package com.krolasyon.bosses.realm.client;

import com.krolasyon.bosses.realm.Realm;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/** draws the burning eclipse and the shattered moon of the Crimson Realm */
public final class RealmSkyRenderer {
    private RealmSkyRenderer() {}

    private static final ResourceLocation ECLIPSE = Realm.rl("textures/environment/eclipse.png");
    private static final ResourceLocation MOON = Realm.rl("textures/environment/shattered_moon.png");

    public static void render(ClientLevel level, int ticks, float partial, PoseStack pose, Matrix4f projection) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        float t = (ticks + partial) / 20F;

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-35F));
        pose.mulPose(Axis.XP.rotationDegrees(-38F));
        pose.mulPose(Axis.YP.rotationDegrees(t * 1.5F));
        quad(pose.last().pose(), ECLIPSE, 46F);
        pose.popPose();

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(120F));
        pose.mulPose(Axis.XP.rotationDegrees(-62F));
        pose.mulPose(Axis.ZP.rotationDegrees(t * 0.6F));
        quad(pose.last().pose(), MOON, 22F);
        pose.popPose();

        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private static void quad(Matrix4f m, ResourceLocation tex, float s) {
        RenderSystem.setShaderTexture(0, tex);
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bb.vertex(m, -s, 100F, -s).uv(0F, 0F).endVertex();
        bb.vertex(m, s, 100F, -s).uv(1F, 0F).endVertex();
        bb.vertex(m, s, 100F, s).uv(1F, 1F).endVertex();
        bb.vertex(m, -s, 100F, s).uv(0F, 1F).endVertex();
        BufferUploader.drawWithShader(bb.end());
    }
}
