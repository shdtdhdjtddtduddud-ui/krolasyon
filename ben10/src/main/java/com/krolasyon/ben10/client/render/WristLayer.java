package com.krolasyon.ben10.client.render;

import com.krolasyon.ben10.Ben10;
import com.krolasyon.ben10.OmniData;
import com.krolasyon.ben10.client.ClientSetup;
import com.krolasyon.ben10.client.model.AlienModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** the Omnitrix watch on the human player's left wrist */
public class WristLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation TEX = new ResourceLocation(Ben10.MODID, "textures/alien/watch.png");
    private static final ResourceLocation GLOW = new ResourceLocation(Ben10.MODID, "textures/alien/watch_glow.png");
    private AlienModel watch;

    public WristLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack ps, MultiBufferSource buf, int light, AbstractClientPlayer p, float limb, float limbAmt, float pt, float age, float headYaw, float headPitch) {
        OmniData d = OmniData.get(p);
        if (d.alien != null || !d.has(OmniData.F_WATCH)) return;
        if (watch == null) watch = new AlienModel(Minecraft.getInstance().getEntityModels().bakeLayer(ClientSetup.layer("watch")));
        ps.pushPose();
        getParentModel().leftArm.translateAndRotate(ps);
        ps.translate(1.0F / 16F, 7.5F / 16F, 0);
        ps.scale(0.5F, 0.5F, 0.5F);
        watch.root().render(ps, buf.getBuffer(RenderType.entityCutoutNoCull(TEX)), light, OverlayTexture.NO_OVERLAY);
        float pulse = d.recharge > 0 ? 0.35F : 0.75F + 0.25F * (float) Math.sin(age * 0.15F);
        watch.root().render(ps, buf.getBuffer(RenderType.eyes(GLOW)), Quads.FULL, OverlayTexture.NO_OVERLAY, pulse, pulse, pulse, 1F);
        ps.popPose();
    }
}
