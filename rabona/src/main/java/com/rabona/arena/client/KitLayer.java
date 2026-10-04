package com.rabona.arena.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.rabona.arena.RabonaArena;
import com.rabona.arena.entity.FootballerEntity;
import com.rabona.arena.game.Team;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Takim formasi (forma, sort, tozluk, krampon) + sirt numarasi. */
public class KitLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {
    static final ResourceLocation RED = RabonaArena.id("textures/entity/kit/red.png");
    static final ResourceLocation BLUE = RabonaArena.id("textures/entity/kit/blue.png");
    static final ResourceLocation RED_GK = RabonaArena.id("textures/entity/kit/red_gk.png");
    static final ResourceLocation BLUE_GK = RabonaArena.id("textures/entity/kit/blue_gk.png");
    static final ResourceLocation DIGITS = RabonaArena.id("textures/entity/kit/digits.png");

    private final PlayerModel<T> kit;

    public KitLayer(RenderLayerParent<T, M> parent, PlayerModel<T> kit) {
        super(parent);
        this.kit = kit;
    }

    @Override
    public void render(PoseStack ps, MultiBufferSource buf, int light, T e, float limbSwing, float limbAmount, float partial,
                       float age, float headYaw, float headPitch) {
        if (e.isInvisible()) return;
        Team t = ClientState.teamOf(e);
        if (!t.playing()) return;
        boolean gk = e instanceof FootballerEntity f && f.isKeeper();
        ResourceLocation tex = t == Team.RED ? (gk ? RED_GK : RED) : (gk ? BLUE_GK : BLUE);
        getParentModel().copyPropertiesTo(kit);
        ClientAnims.sync(kit);
        kit.hat.visible = false;
        kit.head.visible = false;
        ClientAnims.Active act = ClientAnims.get(e, age);
        boolean shirtless = act != null && act.move() == com.rabona.arena.game.Move.CELEB_SHIRT_OFF && age - act.start() > 10;
        kit.body.visible = kit.jacket.visible = !shirtless;
        kit.rightArm.visible = kit.leftArm.visible = kit.rightSleeve.visible = kit.leftSleeve.visible = !shirtless;
        renderColoredCutoutModel(kit, tex, ps, buf, light, e, 1f, 1f, 1f);
        kit.body.visible = kit.jacket.visible = true;
        kit.rightArm.visible = kit.leftArm.visible = kit.rightSleeve.visible = kit.leftSleeve.visible = true;
        if (shirtless) {
            renderShirtInHand(ps, buf, light, tex);
            return;
        }
        int number = e instanceof FootballerEntity f ? f.getNumber() : ClientState.numberOf(e.getUUID());
        if (number > 0) renderNumber(ps, buf, light, number);
    }

    /** Cikarilan forma: sag elde sallanan kumas. */
    private void renderShirtInHand(PoseStack ps, MultiBufferSource buf, int light, ResourceLocation tex) {
        ps.pushPose();
        kit.rightArm.translateAndRotate(ps);
        ps.translate(-0.05, 0.62, 0);
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(tex));
        Matrix4f m = ps.last().pose();
        Matrix3f n = ps.last().normal();
        // govde on yuzunun UV'si (20..28, 20..32) / 64
        float u0 = 20 / 64f, u1 = 28 / 64f, v0 = 20 / 64f, v1 = 32 / 64f;
        float w = 0.32f, h = 0.5f;
        vc.vertex(m, -w, 0, 0).color(255, 255, 255, 255).uv(u0, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0, 0, 1).endVertex();
        vc.vertex(m, -w, h, 0.05f).color(255, 255, 255, 255).uv(u0, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0, 0, 1).endVertex();
        vc.vertex(m, w, h, -0.05f).color(255, 255, 255, 255).uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0, 0, 1).endVertex();
        vc.vertex(m, w, 0, 0).color(255, 255, 255, 255).uv(u1, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0, 0, 1).endVertex();
        ps.popPose();
    }

    private void renderNumber(PoseStack ps, MultiBufferSource buf, int light, int number) {
        ps.pushPose();
        kit.body.translateAndRotate(ps);
        String s = Integer.toString(Math.min(99, number));
        float dw = 0.13f, h = 0.33f;
        float total = dw * s.length();
        float x0 = -total / 2;
        float y0 = 0.14f, z = 0.163f;
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(DIGITS));
        Matrix4f m = ps.last().pose();
        Matrix3f n = ps.last().normal();
        for (int i = 0; i < s.length(); i++) {
            int d = s.charAt(i) - '0';
            float u0 = d / 10f, u1 = (d + 1) / 10f;
            // arkadan bakildiginda soldan saga okunsun diye x ekseni ters
            float xa = -(x0 + i * dw), xb = -(x0 + (i + 1) * dw);
            quad(vc, m, n, xa, xb, y0, y0 + h, z, u0, u1, light);
        }
        ps.popPose();
    }

    private static void quad(VertexConsumer vc, Matrix4f m, Matrix3f n, float x0, float x1, float y0, float y1, float z,
                             float u0, float u1, int light) {
        vc.vertex(m, x0, y0, z).color(255, 255, 255, 255).uv(u0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0, 0, 1).endVertex();
        vc.vertex(m, x0, y1, z).color(255, 255, 255, 255).uv(u0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0, 0, 1).endVertex();
        vc.vertex(m, x1, y1, z).color(255, 255, 255, 255).uv(u1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0, 0, 1).endVertex();
        vc.vertex(m, x1, y0, z).color(255, 255, 255, 255).uv(u1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0, 0, 1).endVertex();
    }
}
