package com.krolasyon.alien.client.render;

import com.krolasyon.alien.AlienMod;
import com.krolasyon.alien.client.model.XenomorphModel;
import com.krolasyon.alien.entity.XenomorphEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class XenomorphRenderer extends MobRenderer<XenomorphEntity, XenomorphModel> {
    private static final ResourceLocation TEX = new ResourceLocation(AlienMod.MODID, "textures/entity/xenomorph.png");

    public XenomorphRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new XenomorphModel(ctx.bakeLayer(XenomorphModel.LAYER)), 0.55F);
    }

    @Override
    public ResourceLocation getTextureLocation(XenomorphEntity e) { return TEX; }

    @Override
    public void render(XenomorphEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buf, int light) {
        if (e.isHidden()) return;
        super.render(e, yaw, partial, pose, buf, light);
    }

    @Override
    protected float getFlipDegrees(XenomorphEntity e) { return 0F; }
}
