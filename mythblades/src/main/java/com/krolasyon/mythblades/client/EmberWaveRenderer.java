package com.krolasyon.mythblades.client;

import com.krolasyon.mythblades.entity.EmberWaveEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Alev hilali: ileri doğru kavisli, eğimli ateş yayı (gövde + ışık katmanı), üstünde yanan animasyonlu alev dilleri;
 * uçarken genişler, sonunda söner.
 */
public final class EmberWaveRenderer extends EntityRenderer<EmberWaveEntity> {
    public EmberWaveRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(EmberWaveEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        float age = e.tickCount + pt;
        float life = e.life();
        float grow = Math.min(1F, 0.35F + age * 0.18F);
        float fade = age > life - 4 ? Math.max(0, (life - age) / 4F) : 1F;
        float R = (e.big() ? 3.9F : 2.5F) * grow;
        ps.pushPose();
        ps.translate(0, 0.45, 0);
        Matrix4f m = ps.last().pose();
        Vec3 f = Vec3.directionFromRotation(e.getXRot(), e.getYRot());
        Vec3 right = f.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 up = right.cross(f).normalize();
        double tilt = Math.toRadians(e.tilt());
        Vec3 b = right.scale(Math.cos(tilt)).add(up.scale(Math.sin(tilt)));
        Vec3 c = f.scale(-R * 0.75);
        double span = Math.toRadians(e.big() ? 75 : 65);
        // iki kat: biraz kaydırılmış ikinci yay derinlik verir
        EmberFx.arc(buf, m, c, f, b, -span, span, R * 0.55F, R, fade, FxDraw.SLASH, 1.2F);
        EmberFx.arc(buf, m, c.add(up.scale(0.18)), f, b, span * 0.9, -span * 0.9, R * 0.7F, R * 0.95F, fade * 0.6F, FxDraw.SLASH, 0.8F);
        // yayın üstünde yanan alev dilleri
        int n = e.big() ? 14 : 9;
        for (int i = 0; i < n; i++) {
            double th = Mth.lerp(i / (double) (n - 1), -span * 0.9, span * 0.9);
            Vec3 p = c.add(f.scale(Math.cos(th) * R * 0.92)).add(b.scale(Math.sin(th) * R * 0.92));
            float h = (e.big() ? 1.6F : 1.0F) * fade * (0.7F + 0.3F * (float) Math.cos(th));
            float w = e.big() ? 0.5F : 0.35F;
            float[] fr = FxDraw.frame((int) (age * 1.2F) + i * 7);
            for (int pass = 0; pass < 2; pass++) {
                VertexConsumer vc = buf.getBuffer(pass == 0 ? FxDraw.body(FxDraw.FIRE) : FxDraw.glow(FxDraw.FIRE));
                float k = pass == 0 ? fade : fade * 0.8F;
                Vec3 sd = b.scale(w);
                FxDraw.v(vc, m, (float) (p.x - sd.x), (float) (p.y - sd.y), (float) (p.z - sd.z), fr[0], fr[3], 1, 1, 1, k);
                FxDraw.v(vc, m, (float) (p.x - sd.x + up.x * h), (float) (p.y - sd.y + up.y * h), (float) (p.z - sd.z + up.z * h), fr[0], fr[1], 1, 1, 1, k);
                FxDraw.v(vc, m, (float) (p.x + sd.x + up.x * h), (float) (p.y + sd.y + up.y * h), (float) (p.z + sd.z + up.z * h), fr[2], fr[1], 1, 1, 1, k);
                FxDraw.v(vc, m, (float) (p.x + sd.x), (float) (p.y + sd.y), (float) (p.z + sd.z), fr[2], fr[3], 1, 1, 1, k);
            }
        }
        ps.popPose();
        super.render(e, yaw, pt, ps, buf, light);
    }

    @Override
    public ResourceLocation getTextureLocation(EmberWaveEntity e) {
        return FxDraw.FIRE;
    }
}
