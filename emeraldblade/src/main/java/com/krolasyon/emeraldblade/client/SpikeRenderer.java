package com.krolasyon.emeraldblade.client;

import com.krolasyon.emeraldblade.entity.EmeraldSpikeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

import java.util.Random;

/** Yerden fırlayıp (geri yaylanarak) çıkan, sonunda toprağa gömülen zümrüt kristal kümesi. */
public final class SpikeRenderer extends EntityRenderer<EmeraldSpikeEntity> {
    public SpikeRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        shadowRadius = 0.4F;
    }

    @Override
    public void render(EmeraldSpikeEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buffers, int light) {
        float t = e.tickCount + pt - e.getDelay();
        if (t <= 0) return;
        float rise = easeOutBack(Math.min(1, t / 4F));
        float end = EmeraldSpikeEntity.LIFE - t;
        float sink = end < 10 ? Math.max(0, end / 10F) : 1;
        float s = e.getScale();
        Random r = new Random(e.getId() * 31L);

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(e.getYRot()));
        ps.translate(0, -(1 - sink) * 1.6F * s, 0);
        // [eğim x, eğim z, yükseklik, genişlik, ofset x, ofset z]
        float[][] crystals = new float[5][];
        crystals[0] = new float[]{r.nextFloat() * 10 - 5, r.nextFloat() * 10 - 5, 1.9F, 0.24F, 0, 0};
        for (int i = 1; i < 5; i++) {
            double a = i * Math.PI / 2 + r.nextFloat();
            crystals[i] = new float[]{(float) Math.cos(a) * (25 + r.nextFloat() * 15), (float) Math.sin(a) * (25 + r.nextFloat() * 15),
                    0.8F + r.nextFloat() * 0.6F, 0.12F + r.nextFloat() * 0.06F, (float) Math.sin(a) * 0.22F, -(float) Math.cos(a) * 0.22F};
        }
        TextureAtlasSprite crystal = Mesh.sprite(Mesh.CRYSTAL), core = Mesh.sprite(Mesh.CORE), bark = Mesh.sprite(Mesh.BARK);
        for (int pass = 0; pass < 3; pass++) {
            VertexConsumer vc = buffers.getBuffer(Mesh.TYPES[pass]);
            if (pass == Mesh.CUT) {
                // tabanda toprağı yaran kökler
                for (int i = 0; i < 4; i++) {
                    double a = i * Math.PI / 2 + 0.4;
                    float c = (float) Math.cos(a), sn = (float) Math.sin(a);
                    float g = rise * s;
                    Mesh.tube(vc, ps, Mesh.spline(new float[][]{{0, 0.15F * g, 0, 0.07F * s}, {c * 0.35F * s, 0.1F * g, sn * 0.35F * s, 0.05F * s},
                            {c * 0.7F * s, 0.02F, sn * 0.7F * s, 0.03F * s}, {c * 0.9F * s, -0.05F, sn * 0.9F * s, 0.01F * s}}, 4), 6, bark, 1, 1, 1, 1, light, 1);
                }
                continue;
            }
            for (float[] c : crystals) {
                ps.pushPose();
                ps.translate(c[4] * s, 0, c[5] * s);
                ps.mulPose(Axis.XP.rotationDegrees(c[0]));
                ps.mulPose(Axis.ZP.rotationDegrees(c[1]));
                float h = c[2] * s * rise, w = c[3] * s;
                if (pass == Mesh.TRANS) {
                    float[][] rings = {{-0.2F, w, w, 0, 0, 1}, {h * 0.72F, w, w, 0, 0, 1}, {h, 0, 0, 0, 0, 1}};
                    Mesh.loft(vc, ps, Mesh.HEX, rings, crystal, 0.75F, 1, 0.82F, 0.82F, Mesh.FULL_BRIGHT, true);
                } else {
                    float b = 0.55F + 0.45F * Math.max(0, 1 - t / 8F);
                    float[][] rings = {{-0.1F, w * 0.5F, w * 0.5F, 0, 0, b}, {h * 0.68F, w * 0.5F, w * 0.5F, 0, 0, b}, {h * 0.92F, 0, 0, 0, 0, b}};
                    Mesh.loft(vc, ps, Mesh.HEX, rings, core, 1, 1, 1, 1, Mesh.FULL_BRIGHT, true);
                    Mesh.loft(vc, ps, Mesh.reversed(Mesh.HEX), rings, core, 1, 1, 1, 1, Mesh.FULL_BRIGHT, true);
                }
                ps.popPose();
            }
        }
        ps.popPose();
        super.render(e, yaw, pt, ps, buffers, light);
    }

    private static float easeOutBack(float x) {
        float c1 = 1.9F, c3 = c1 + 1;
        return 1 + c3 * (float) Math.pow(x - 1, 3) + c1 * (float) Math.pow(x - 1, 2);
    }

    @Override
    public ResourceLocation getTextureLocation(EmeraldSpikeEntity e) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
