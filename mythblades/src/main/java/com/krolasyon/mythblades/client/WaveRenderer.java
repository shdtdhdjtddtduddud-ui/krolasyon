package com.krolasyon.mythblades.client;

import com.krolasyon.mythblades.entity.RainbowWaveEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

/** Yatay, ileriye doğru kavisli gökkuşağı hilali; uçarken genişler, sonunda incelir. */
public final class WaveRenderer extends EntityRenderer<RainbowWaveEntity> {
    private static final int[] BANDS = {0xFF5A6E, 0xFFA14A, 0xFFE95A, 0x6CE07A, 0x5AB8FF, 0x7A6CFF, 0xC86CFF};

    public WaveRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    private static void arc(Vox v) {
        float R = 22F, W = 5.6F, off = 12F;
        v.fill(-R, -0.9F, -R, R, 0.9F, R, (x, y, z) -> {
            float zz = z + off;
            float r = (float) Math.sqrt(x * x + zz * zz);
            float a = (float) Math.atan2(x, zz);
            float span = 1.2F;
            if (Math.abs(a) > span) return false;
            float taper = 1 - (float) Math.pow(Math.abs(a) / span, 3);
            return r <= R && r >= R - W * taper && Math.abs(y) <= 0.9F * taper + 0.2F;
        }, (x, y, z, i, j, k) -> {
            float zz = z + off;
            float r = (float) Math.sqrt(x * x + zz * zz);
            int b = (int) Math.max(0, Math.min(6, (R - r) / W * 7));
            return Vox.argb(BANDS[b], 0.55F);
        }, Vox.TRANS, true);
    }

    @Override
    public void render(RainbowWaveEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        float age = e.tickCount + pt;
        ps.pushPose();
        ps.translate(0, 0.3, 0);
        ps.mulPose(Axis.YP.rotationDegrees(-e.getYRot()));
        ps.mulPose(Axis.XP.rotationDegrees(e.getXRot()));
        float grow = Math.min(1F, 0.45F + age * 0.12F) * (age > RainbowWaveEntity.LIFE - 4 ? Math.max(0.1F, (RainbowWaveEntity.LIFE - age) / 4F) : 1F);
        ps.scale(grow / 16F, 1 / 16F, grow / 16F);
        BladeModels.mesh("wave", WaveRenderer::arc).render(ps, buf, 0xF000F0);
        ps.popPose();
        super.render(e, yaw, pt, ps, buf, light);
    }

    @Override
    public ResourceLocation getTextureLocation(RainbowWaveEntity e) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
