package com.krolasyon.omnitrix.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.util.Mth;

/** The Higgsfield Omnitrix mesh strapped on the player's left wrist; the dial lifts while it is being activated. */
public class WatchLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public WatchLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) { super(parent); }

    @Override
    public void render(PoseStack ps, MultiBufferSource buf, int light, AbstractClientPlayer pl, float limb, float limbAmount, float pt,
                       float age, float headYaw, float headPitch) {
        ClientState.Look look = ClientState.look(pl);
        if (!look.wearing || look.alien() != null || pl.isInvisible()) return;
        ps.pushPose();
        getParentModel().leftArm.translateAndRotate(ps);
        // model space here is y-down, 1/16 block per unit already applied: wrist is ~9 px below the shoulder pivot
        ps.translate(0.04F, 0.56F, 0F);
        // watch space: band hole along z, dial facing +y -> hole along the arm, dial facing outwards
        ps.mulPose(Axis.YP.rotationDegrees(-90));
        ps.mulPose(Axis.XP.rotationDegrees(-90));
        float lift = 0;
        Minecraft mc = Minecraft.getInstance();
        if (pl == mc.player && mc.level != null) {
            float t = (mc.level.getGameTime() - ClientEvents.watchAnim + pt) / 10F;
            if (t >= 0 && t < 1) lift = Mth.sin(t * Mth.PI);
        }
        ps.translate(0, lift * 0.06F, 0);
        AlienRenderer.renderWatch(ps, buf, light, 0.42F, false);
        ps.popPose();
    }
}
