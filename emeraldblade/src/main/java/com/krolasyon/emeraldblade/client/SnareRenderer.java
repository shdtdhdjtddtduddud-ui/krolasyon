package com.krolasyon.emeraldblade.client;

import com.krolasyon.emeraldblade.entity.RootSnareEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;

/** Hedefin bacaklarından gövdesine doğru büyüyerek sarılan kökler; süre bitince geri çekilir. */
public final class SnareRenderer extends EntityRenderer<RootSnareEntity> {
    public SnareRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public boolean shouldRender(RootSnareEntity e, net.minecraft.client.renderer.culling.Frustum f, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(RootSnareEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buffers, int light) {
        LivingEntity target = e.getTarget();
        float w = target != null ? target.getBbWidth() : 0.6F, h = target != null ? target.getBbHeight() : 1.8F;
        float age = e.tickCount + pt;
        float grow = Math.min(1, age / 8F);
        float out = Math.max(0, Math.min(1, (RootSnareEntity.LIFE - age) / 8F));
        float g = grow * out;
        if (g <= 0.01F) return;
        if (target != null) {
            // hedefi takip et (konum farkını kapat)
            double dx = target.getX() - (e.xo + (e.getX() - e.xo) * pt), dz = target.getZ() - (e.zo + (e.getZ() - e.zo) * pt);
            double dy = target.getY() - (e.yo + (e.getY() - e.yo) * pt);
            ps.pushPose();
            ps.translate(dx, dy, dz);
        } else {
            ps.pushPose();
        }
        TextureAtlasSprite bark = Mesh.sprite(Mesh.BARK), vein = Mesh.sprite(Mesh.BARK_GLOW);
        int n = 6;
        float[][][] roots = new float[n + 3][][];
        for (int i = 0; i < n; i++) {
            double a0 = i * Math.PI * 2 / n;
            float top = h * (0.55F + 0.2F * (i % 3) / 2F) * g;
            int m = 14;
            float[][] pts = new float[m + 1][];
            for (int j = 0; j <= m; j++) {
                float s = j / (float) m;
                double a = a0 + s * 2.2 * (i % 2 == 0 ? 1 : -1);
                float rad = w * (0.95F - 0.35F * s) + 0.05F;
                float tip = s > 0.7F ? (s - 0.7F) * 3.3F : 0;
                float wig = (float) Math.sin(age * 0.25 + i * 1.3) * 0.06F * tip;
                pts[j] = new float[]{(float) Math.cos(a) * (rad + wig), -0.05F + top * s, (float) Math.sin(a) * (rad + wig),
                        (0.075F - 0.06F * s) * (0.6F + 0.4F * g)};
            }
            roots[i] = pts;
        }
        // yerde yayılan kök halkası
        for (int i = 0; i < 3; i++) {
            double a0 = i * Math.PI * 2 / 3 + 0.5;
            float[][] pts = new float[9][];
            for (int j = 0; j <= 8; j++) {
                double a = a0 + j * 0.35;
                float rad = (w * 0.9F + 0.15F + j * 0.03F) * g;
                pts[j] = new float[]{(float) Math.cos(a) * rad, 0.02F, (float) Math.sin(a) * rad, 0.06F - j * 0.005F};
            }
            roots[n + i] = pts;
        }
        VertexConsumer vc = buffers.getBuffer(Mesh.TYPES[Mesh.CUT]);
        for (float[][] r : roots) Mesh.tube(vc, ps, r, 6, bark, 1, 1, 1, 1, light, 1);
        vc = buffers.getBuffer(Mesh.TYPES[Mesh.GLOW]);
        float vi = 0.35F + 0.25F * (float) Math.sin(age * 0.3);
        for (float[][] r : roots) {
            Mesh.tube(vc, ps, r, 6, vein, vi, vi, vi, 1, Mesh.FULL_BRIGHT, 1.07F, false);
            Mesh.tube(vc, ps, r, 6, vein, vi, vi, vi, 1, Mesh.FULL_BRIGHT, 1.07F, true);
        }
        ps.popPose();
        super.render(e, yaw, pt, ps, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(RootSnareEntity e) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
