package com.krolasyon.furniture.client.render;

import com.krolasyon.furniture.block.WideBlock;
import com.krolasyon.furniture.blockentity.SofaBlockEntity;
import com.krolasyon.furniture.client.model.FurnitureModelLayers;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

public class SofaRenderer implements BlockEntityRenderer<SofaBlockEntity> {
    public static final ResourceLocation TEX = FurnitureModelLayers.tex("sofa");
    private final ModelPart root, seatA, seatB, backPad;

    public SofaRenderer(BlockEntityRendererProvider.Context ctx) {
        root = ctx.bakeLayer(FurnitureModelLayers.SOFA);
        seatA = root.getChild("seat_a");
        seatB = root.getChild("seat_b");
        backPad = root.getChild("back_pad");
    }

    /** cushions squash and the back pad leans when somebody sits (spring animation) */
    public static void pose(ModelPart seatA, ModelPart seatB, ModelPart backPad, float sit) {
        seatA.resetPose();
        seatB.resetPose();
        backPad.resetPose();
        seatA.y += 2.2F * sit;
        seatB.y += 2.2F * sit;
        backPad.xRot = -0.11F * sit;
        backPad.y += 0.8F * sit;
    }

    @Override
    public void render(SofaBlockEntity be, float partial, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        BlockState st = be.getBlockState();
        if (!st.hasProperty(WideBlock.FACING)) return;
        Direction f = st.getValue(WideBlock.FACING);
        Direction toPartner = f.getCounterClockWise();
        pose(seatA, seatB, backPad, Mth.lerp(partial, be.prevSit, be.sit));
        FurnitureDraw.draw(root, TEX, ps, buf, light, overlay, 0.5D + toPartner.getStepX() * 0.5D, 0.5D + toPartner.getStepZ() * 0.5D, f);
    }

    @Override
    public boolean shouldRenderOffScreen(SofaBlockEntity be) { return true; }
}
