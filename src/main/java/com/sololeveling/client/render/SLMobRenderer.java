package com.sololeveling.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sololeveling.SoloLeveling;
import com.sololeveling.client.ClientEvents;
import com.sololeveling.client.model.SLModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

public class SLMobRenderer<T extends Mob> extends MobRenderer<T, SLModel<T>> {
    private final ResourceLocation texture;
    private final float scale;

    public SLMobRenderer(EntityRendererProvider.Context ctx, String model, float scale, float shadow) {
        super(ctx, new SLModel<>(ctx.bakeLayer(ClientEvents.layer(model)), model), shadow * scale);
        this.scale = scale;
        this.texture = new ResourceLocation(SoloLeveling.MODID, "textures/entity/" + model + ".png");
        ResourceLocation glow = new ResourceLocation(SoloLeveling.MODID, "textures/entity/" + model + "_glow.png");
        this.addLayer(new RenderLayer<T, SLModel<T>>(this) {
            @Override
            public void render(PoseStack ps, MultiBufferSource buf, int light, T e, float limb, float limbAmt, float pt, float age, float yaw, float pitch) {
                if (e.isInvisible()) return;
                VertexConsumer vc = buf.getBuffer(RenderType.eyes(glow));
                getParentModel().renderToBuffer(ps, vc, 15728640, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F);
            }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(T e) { return texture; }

    @Override
    protected void scale(T e, PoseStack ps, float pt) { ps.scale(scale, scale, scale); }
}
