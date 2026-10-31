package com.krolasyon.cille.client.render;

import com.krolasyon.cille.entity.MarbleEntity;
import com.krolasyon.cille.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** thrown marble: smooth 3D glass ball, its Cille tumbling inside as it rolls */
public class MarbleRenderer extends EntityRenderer<MarbleEntity> {
    private final EntityModelSet models;

    public MarbleRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.models = ctx.getModelSet();
        this.shadowRadius = 0.15F;
    }

    @Override
    public void render(MarbleEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        float age = e.tickCount + partial;
        float spin = Mth.lerp(partial, e.prevSpin, e.spin);
        ItemStack st = e.getItem();
        Item item = st.isEmpty() ? ModItems.KABKABA_MISKETI.get() : st.getItem();
        pose.pushPose();
        pose.translate(0, 0.15, 0);
        pose.mulPose(Axis.YP.rotationDegrees(-e.getYRot()));
        SmoothSphere.marble(pose, buffers, MiniCille.forItem(item, models), 0.15F, age, spin, light);
        pose.popPose();
        super.render(e, yaw, partial, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(MarbleEntity e) { return SmoothSphere.MARBLE_GLASS; }
}
