package com.krolasyon.voidtree.client;

import com.krolasyon.voidtree.VoidTree;
import com.krolasyon.voidtree.entity.ShadowCloneEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Shadow clone: player shaped, translucent void skin with glowing violet eyes and veins. */
public class ShadowCloneRenderer extends HumanoidMobRenderer<ShadowCloneEntity, PlayerModel<ShadowCloneEntity>> {
    private static final ResourceLocation SKIN = new ResourceLocation(VoidTree.MODID, "textures/entity/shadow_clone.png");
    private static final ResourceLocation GLOW = new ResourceLocation(VoidTree.MODID, "textures/entity/shadow_clone_glow.png");

    public ShadowCloneRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.4F);
        addLayer(new Glow(this));
    }

    @Override
    public ResourceLocation getTextureLocation(ShadowCloneEntity e) { return SKIN; }

    @Override
    protected RenderType getRenderType(ShadowCloneEntity e, boolean visible, boolean translucent, boolean glowing) {
        return RenderType.entityTranslucent(SKIN);
    }

    private static final class Glow extends RenderLayer<ShadowCloneEntity, PlayerModel<ShadowCloneEntity>> {
        Glow(RenderLayerParent<ShadowCloneEntity, PlayerModel<ShadowCloneEntity>> parent) { super(parent); }

        @Override
        public void render(PoseStack ps, MultiBufferSource buf, int light, ShadowCloneEntity e, float limbSwing, float limbAmount, float pt,
                           float age, float headYaw, float headPitch) {
            VertexConsumer vc = buf.getBuffer(RenderType.eyes(GLOW));
            float pulse = 0.65F + 0.35F * Mth.sin(age * 0.25F);
            getParentModel().renderToBuffer(ps, vc, 0xF000F0, OverlayTexture.NO_OVERLAY, pulse, pulse, pulse, 1F);
        }
    }
}
