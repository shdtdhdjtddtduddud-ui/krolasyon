package com.krolasyon.futbol.client;

import com.krolasyon.futbol.FutbolMod;
import com.krolasyon.futbol.game.Team;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Renders the team kit (shirt, shorts, socks, boots, keeper gloves) plus the shirt number over any humanoid. */
public class JerseyLayer<T extends LivingEntity, M extends PlayerModel<T>> extends RenderLayer<T, M> {
    public static final ModelLayerLocation WIDE = new ModelLayerLocation(new ResourceLocation(FutbolMod.MODID, "jersey"), "main");
    public static final ModelLayerLocation SLIM = new ModelLayerLocation(new ResourceLocation(FutbolMod.MODID, "jersey_slim"), "main");
    private static final ResourceLocation RED = tex("jersey_red");
    private static final ResourceLocation BLUE = tex("jersey_blue");
    private static final ResourceLocation RED_GK = tex("jersey_red_gk");
    private static final ResourceLocation BLUE_GK = tex("jersey_blue_gk");
    private static final ResourceLocation DIGITS = tex("numbers");

    private final PlayerModel<T> wide;
    private final PlayerModel<T> slim;

    private static ResourceLocation tex(String n) { return new ResourceLocation(FutbolMod.MODID, "textures/entity/" + n + ".png"); }

    public JerseyLayer(RenderLayerParent<T, M> parent, EntityModelSet models) {
        super(parent);
        this.wide = new PlayerModel<>(models.bakeLayer(WIDE), false);
        this.slim = new PlayerModel<>(models.bakeLayer(SLIM), true);
    }

    @Override
    public void render(PoseStack ps, MultiBufferSource buf, int light, T e, float limbSwing, float limbSwingAmount, float partial,
                       float age, float headYaw, float headPitch) {
        if (e.isInvisible()) return;
        Team t = ClientState.teamOf(e);
        if (!t.playing()) return;
        boolean gk = ClientState.keeper(e);
        ResourceLocation tex = t == Team.RED ? (gk ? RED_GK : RED) : (gk ? BLUE_GK : BLUE);
        boolean isSlim = e instanceof AbstractClientPlayer p && "slim".equals(p.getModelName());
        PlayerModel<T> m = isSlim ? slim : wide;
        getParentModel().copyPropertiesTo(m);
        m.setAllVisible(true);
        m.head.visible = false;
        m.hat.visible = false;
        m.jacket.visible = false;
        m.leftSleeve.visible = false;
        m.rightSleeve.visible = false;
        m.leftPants.visible = false;
        m.rightPants.visible = false;
        int overlay = LivingEntityRenderer.getOverlayCoords(e, 0.0F);
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(tex));
        m.renderToBuffer(ps, vc, light, overlay, 1F, 1F, 1F, 1F);

        int number = ClientState.numberOf(e);
        if (number > 0) {
            ps.pushPose();
            m.body.translateAndRotate(ps);
            drawNumber(ps, buf.getBuffer(RenderType.entityCutoutNoCull(DIGITS)), light, number, 2.38F / 16F, 3.2F / 16F, 3.9F / 16F, true);
            drawNumber(ps, buf.getBuffer(RenderType.entityCutoutNoCull(DIGITS)), light, number, -2.38F / 16F, 2.4F / 16F, 2.0F / 16F, false);
            ps.popPose();
        }
    }

    /** digits texture: 10 glyphs of 16x24 px in a 256x32 atlas */
    private static void drawNumber(PoseStack ps, VertexConsumer vc, int light, int number, float z, float top, float h, boolean back) {
        String s = Integer.toString(Math.min(number, 99));
        float gw = h * 0.62F;
        float total = gw * s.length();
        PoseStack.Pose pose = ps.last();
        Matrix4f mat = pose.pose();
        Matrix3f nrm = pose.normal();
        float nz = back ? 1F : -1F;
        // seen from behind the body, model +x is on the viewer's left; from the front it is on the right
        float xStart = back ? total / 2F : -total / 2F;
        float dir = back ? -1F : 1F;
        float xOff = back ? 0F : 1.6F / 16F;
        for (int i = 0; i < s.length(); i++) {
            int d = s.charAt(i) - '0';
            float u0 = d * 16F / 256F, u1 = (d * 16F + 16F) / 256F, v0 = 0F, v1 = 24F / 32F;
            float xa = xOff + xStart + dir * gw * i;
            float xb = xa + dir * gw;
            float y0 = top, y1 = top + h;
            vertex(vc, mat, nrm, xa, y0, z, u0, v0, light, nz);
            vertex(vc, mat, nrm, xa, y1, z, u0, v1, light, nz);
            vertex(vc, mat, nrm, xb, y1, z, u1, v1, light, nz);
            vertex(vc, mat, nrm, xb, y0, z, u1, v0, light, nz);
        }
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y, float z, float u, float v, int light, float nz) {
        vc.vertex(m, x, y, z).color(255, 255, 255, 255).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0F, 0F, nz).endVertex();
    }
}
