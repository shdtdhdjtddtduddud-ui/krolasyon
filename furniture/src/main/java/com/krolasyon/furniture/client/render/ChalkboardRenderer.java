package com.krolasyon.furniture.client.render;

import com.krolasyon.furniture.block.ChalkboardBlock;
import com.krolasyon.furniture.blockentity.ChalkboardBlockEntity;
import com.krolasyon.furniture.client.model.FurnitureModelLayers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

public class ChalkboardRenderer implements BlockEntityRenderer<ChalkboardBlockEntity> {
    public static final ResourceLocation TEX = FurnitureModelLayers.tex("chalkboard");
    public static final ResourceLocation TEX_CLEAN = FurnitureModelLayers.tex("chalkboard_clean");
    private final ModelPart root;
    private final Font font;

    public ChalkboardRenderer(BlockEntityRendererProvider.Context ctx) {
        root = ctx.bakeLayer(FurnitureModelLayers.CHALKBOARD);
        font = ctx.getFont();
    }

    @Override
    public void render(ChalkboardBlockEntity be, float partial, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        BlockState st = be.getBlockState();
        if (!st.hasProperty(ChalkboardBlock.FACING)) return;
        Direction f = st.getValue(ChalkboardBlock.FACING);
        boolean text = be.hasText();
        FurnitureDraw.draw(root, text ? TEX_CLEAN : TEX, ps, buf, light, overlay, 0.5D, 0.5D, f);
        if (!text) return;

        // text: placed on the slate (front plane z = -2u, centre height 25u), written left -> right as seen from the front
        ps.pushPose();
        ps.translate(0.5D, 0.0D, 0.5D);
        ps.mulPose(Axis.YP.rotationDegrees(180.0F - f.toYRot()));
        ps.translate(0.0D, 25.0D / 32.0D, -2.06D / 32.0D);
        float s = 0.0072F;
        ps.scale(-s, -s, -s);
        int color = be.getTextColor() | 0xFF000000;
        boolean glow = be.isGlow();
        int lightTex = glow ? LightTexture.FULL_BRIGHT : light;
        int lineH = 10;
        float top = -(ChalkboardBlockEntity.LINES * lineH) / 2.0F + 1.0F;
        for (int i = 0; i < ChalkboardBlockEntity.LINES; i++) {
            String line = be.getLine(i);
            if (line.isEmpty()) continue;
            int w = font.width(line);
            float x = -w / 2.0F;
            float y = top + i * lineH;
            float scale = w > 72 ? 72.0F / w : 1.0F;
            ps.pushPose();
            ps.translate(0.0D, y + 4.0F, 0.0D);
            ps.scale(scale, scale, 1.0F);
            ps.translate(0.0D, -4.0F, 0.0D);
            if (glow) {
                int outline = darken(color);
                font.drawInBatch8xOutline(Component.literal(line).getVisualOrderText(), x, 0.0F, color, outline, ps.last().pose(), buf, lightTex);
            } else {
                font.drawInBatch(line, x, 0.0F, color, false, ps.last().pose(), buf, Font.DisplayMode.NORMAL, 0, lightTex);
            }
            ps.popPose();
        }
        ps.popPose();
    }

    private static int darken(int c) {
        int r = (int) (((c >> 16) & 0xFF) * 0.4F), g = (int) (((c >> 8) & 0xFF) * 0.4F), b = (int) ((c & 0xFF) * 0.4F);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
