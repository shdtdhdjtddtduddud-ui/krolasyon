package com.krolasyon.mythblades.client;

import com.krolasyon.mythblades.block.SwordAltarBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/**
 * Taştaki kılıç: bıçak aşağıda, üçte biri taşa gömülü. Mühürsüz denemede sarsılır; çekilirken önce titreyip taşı
 * çatlatır, sonra akkor kesilerek yükselir.
 */
public final class AltarRenderer implements BlockEntityRenderer<SwordAltarBlockEntity> {
    /** Kılıç ölçeği (blok/piksel) ve taşa gömülü bıçak boyu (piksel). */
    private static final float S = 2.0F / 16F, SUNK = 7.5F;

    public AltarRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(SwordAltarBlockEntity be, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        if (be.getLevel() == null) return;
        boolean pulling = be.pulling();
        if (!be.hasSword() && !pulling) return;
        float time = be.getLevel().getGameTime() + pt;
        // sunak bloğu katı olduğundan ışığı üstündeki bloktan al
        light = net.minecraft.client.renderer.LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos().above());
        float p = be.progress(pt);
        // yükselme: ilk %40 titreme (az kıpırdar), sonra yumuşakça yukarı
        float rise = 0, shake = 0, heat = 0;
        if (pulling) {
            float a = Math.min(1, p / 0.4F);
            shake = (p < 0.45F ? a : Math.max(0, 1 - (p - 0.45F) * 4)) * 0.9F;
            float b = Math.max(0, (p - 0.4F) / 0.6F);
            rise = 0.6F * a + (SUNK + 14F) * (b * b * (3 - 2 * b));
            heat = Math.min(1, p * 1.3F);
        }
        float w = (time - be.wobbleStart) / SwordAltarBlockEntity.WOBBLE;
        if (w >= 0 && w < 1) shake = Math.max(shake, (1 - w) * 0.7F);
        ps.pushPose();
        ps.translate(0.5, 1.0, 0.5);
        ps.scale(S, S, S);
        // kabza yukarı: kılıcı ters çevir, guard taşın üstünde
        float guardY = E_TOTAL - SUNK + rise;
        ps.translate(0, guardY, 0);
        if (shake > 0) {
            ps.translate(Math.sin(time * 3.1) * 0.35 * shake, 0, Math.cos(time * 2.7) * 0.3 * shake);
            ps.mulPose(Axis.ZP.rotationDegrees((float) Math.sin(time * 2.3) * 2.2F * shake));
        }
        ps.mulPose(Axis.YP.rotationDegrees(35));
        ps.mulPose(Axis.ZP.rotationDegrees(180));
        BladeModels.ember(ps, buf, light, time, heat * 0.9F + 0.1F);
        ps.popPose();
    }

    /** Bıçağın siperden uca uzunluğu (piksel). */
    private static final float E_TOTAL = 26.4F;

    @Override
    public boolean shouldRenderOffScreen(SwordAltarBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
