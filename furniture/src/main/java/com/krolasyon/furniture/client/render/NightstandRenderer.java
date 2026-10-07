package com.krolasyon.furniture.client.render;

import com.krolasyon.furniture.block.NightstandBlock;
import com.krolasyon.furniture.blockentity.NightstandBlockEntity;
import com.krolasyon.furniture.client.model.FurnitureModelLayers;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

public class NightstandRenderer implements BlockEntityRenderer<NightstandBlockEntity> {
    public static final ResourceLocation TEX = FurnitureModelLayers.tex("nightstand");
    private final ModelPart root, drawerUp, drawerDn;

    public NightstandRenderer(BlockEntityRendererProvider.Context ctx) {
        root = ctx.bakeLayer(FurnitureModelLayers.NIGHTSTAND);
        drawerUp = root.getChild("drawer_up");
        drawerDn = root.getChild("drawer_dn");
    }

    public static void pose(ModelPart up, ModelPart dn, float upOpen, float dnOpen) {
        up.resetPose();
        dn.resetPose();
        up.z -= 13.0F * upOpen;
        dn.z -= 13.0F * dnOpen;
        up.y -= 0.0F;
    }

    @Override
    public void render(NightstandBlockEntity be, float partial, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        BlockState st = be.getBlockState();
        if (!st.hasProperty(NightstandBlock.FACING)) return;
        float e = FurnitureDraw.openEase(be.prevOpen, be.open, partial);
        // the top drawer leads, the bottom one follows a moment later
        float lead = e;
        float follow = FurnitureDraw.openEase(Math.max(0, be.prevOpen - 0.2F) / 0.8F, Math.max(0, be.open - 0.2F) / 0.8F, partial);
        pose(drawerUp, drawerDn, lead, follow);
        FurnitureDraw.draw(root, TEX, ps, buf, light, overlay, 0.5D, 0.5D, st.getValue(NightstandBlock.FACING));
    }
}
