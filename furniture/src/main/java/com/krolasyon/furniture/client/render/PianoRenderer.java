package com.krolasyon.furniture.client.render;

import com.krolasyon.furniture.block.WideBlock;
import com.krolasyon.furniture.blockentity.PianoBlockEntity;
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

public class PianoRenderer implements BlockEntityRenderer<PianoBlockEntity> {
    public static final ResourceLocation TEX = FurnitureModelLayers.tex("piano");
    /** semitone (0..23 from C4) of each white / black key bone */
    public static final int[] WHITE = {0, 2, 4, 5, 7, 9, 11, 12, 14, 16, 17, 19, 21, 23};
    public static final int[] BLACK = {1, 3, 6, 8, 10, 13, 15, 18, 20, 22};

    private final ModelPart root, pedalA, pedalB;
    private final ModelPart[] white = new ModelPart[WHITE.length];
    private final ModelPart[] black = new ModelPart[BLACK.length];

    public PianoRenderer(BlockEntityRendererProvider.Context ctx) {
        root = ctx.bakeLayer(FurnitureModelLayers.PIANO);
        pedalA = root.getChild("pedal_a");
        pedalB = root.getChild("pedal_b");
        for (int i = 0; i < white.length; i++) white[i] = root.getChild("wk" + i);
        for (int i = 0; i < black.length; i++) black[i] = root.getChild("bk" + i);
    }

    @Override
    public void render(PianoBlockEntity be, float partial, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        BlockState st = be.getBlockState();
        if (!st.hasProperty(WideBlock.FACING)) return;
        Direction f = st.getValue(WideBlock.FACING);
        Direction toPartner = f.getCounterClockWise();
        for (int i = 0; i < white.length; i++) {
            white[i].resetPose();
            float k = Mth.lerp(partial, be.prevKey[WHITE[i]], be.key[WHITE[i]]);
            white[i].xRot = 0.20F * FurnitureDraw.smooth(k);
        }
        for (int i = 0; i < black.length; i++) {
            black[i].resetPose();
            float k = Mth.lerp(partial, be.prevKey[BLACK[i]], be.key[BLACK[i]]);
            black[i].xRot = 0.16F * FurnitureDraw.smooth(k);
        }
        float p = Mth.lerp(partial, be.prevPedal, be.pedal);
        pedalA.resetPose();
        pedalB.resetPose();
        pedalB.y += 1.4F * p;
        pedalA.y += 0.4F * p;
        FurnitureDraw.draw(root, TEX, ps, buf, light, overlay, 0.5D + toPartner.getStepX() * 0.5D, 0.5D + toPartner.getStepZ() * 0.5D, f);
    }

    @Override
    public boolean shouldRenderOffScreen(PianoBlockEntity be) { return true; }
}
