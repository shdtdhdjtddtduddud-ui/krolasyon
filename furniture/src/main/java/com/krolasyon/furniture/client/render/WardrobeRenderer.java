package com.krolasyon.furniture.client.render;

import com.krolasyon.furniture.block.WardrobeBlock;
import com.krolasyon.furniture.blockentity.WardrobeBlockEntity;
import com.krolasyon.furniture.client.model.FurnitureModelLayers;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

public class WardrobeRenderer implements BlockEntityRenderer<WardrobeBlockEntity> {
    public static final ResourceLocation TEX = FurnitureModelLayers.tex("wardrobe");
    private final ModelPart root, doorR, doorL, drawer;

    public WardrobeRenderer(BlockEntityRendererProvider.Context ctx) {
        root = ctx.bakeLayer(FurnitureModelLayers.WARDROBE);
        doorR = root.getChild("door_r");
        doorL = root.getChild("door_l");
        drawer = root.getChild("drawer");
    }

    public static void pose(ModelPart doorR, ModelPart doorL, ModelPart drawer, float doors, float drawerOpen) {
        doorR.resetPose();
        doorL.resetPose();
        drawer.resetPose();
        doorR.yRot = -1.95F * doors;
        doorL.yRot = 1.95F * doors;
        drawer.z -= 12.0F * drawerOpen;
    }

    @Override
    public void render(WardrobeBlockEntity be, float partial, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        BlockState st = be.getBlockState();
        if (!st.hasProperty(WardrobeBlock.FACING)) return;
        float doors = FurnitureDraw.openEase(be.prevOpen, be.open, partial);
        float drawerOpen = FurnitureDraw.openEase(Math.max(0, be.prevOpen - 0.35F) / 0.65F, Math.max(0, be.open - 0.35F) / 0.65F, partial);
        pose(doorR, doorL, drawer, doors, drawerOpen);
        FurnitureDraw.draw(root, TEX, ps, buf, light, overlay, 0.5D, 0.5D, st.getValue(WardrobeBlock.FACING));
    }
}
