package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.entity.BeybladeEntity;
import com.krolasyon.beyblade.entity.CommonBeybladeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import static com.krolasyon.beyblade.entity.CommonBeybladeEntity.*;

/** Effects of the ordinary beyblades, tinted with each one's colours. Pose is at the beyblade, unrotated. */
public final class CommonFx {
    private CommonFx() {}

    private static final float[] BLACK = {0F, 0F, 0F};

    private static float[] k(float[] c, float k) { return new float[]{c[0] * k, c[1] * k, c[2] * k}; }

    private static float easeOut(float x) {
        x = Mth.clamp(x, 0F, 1F);
        return 1F - (1F - x) * (1F - x) * (1F - x);
    }

    public static void render(CommonBeybladeEntity e, float partial, float t, double ex, double ey, double ez, PoseStack pose,
                              MultiBufferSource buf, Quaternionf cam) {
        float[] c1 = e.variant().rgb(true), c2 = e.variant().rgb(false);
        Matrix4f m = pose.last().pose();
        if (e.isShielded()) {
            // Spin Guard: a hex dome in the beyblade's colour
            float pulse = 0.8F + 0.2F * Mth.sin(t * 0.5F);
            VertexConsumer vc = Fx.bufT(buf, Fx.HEX, 0.35F * pulse);
            Fx.sphere(vc, m, 0.15F, 0.95F, Mth.HALF_PI * 1.1F, 8, 24, c1, 3F, 2F, t * 0.01F, 0F);
            Fx.alpha = 1F;
            vc = Fx.buf(buf, Fx.SOFT);
            Fx.ring(vc, m, 0.06F, 0.8F, 1.05F, 32, k(c1, 0.7F * pulse), BLACK, 1F, 0F);
        }
        float sa = t - e.strikeTime;
        if (sa >= 0 && sa < 12) {
            float f = 1F - sa / 12F;
            VertexConsumer vc = Fx.buf(buf, Fx.FLARE);
            Fx.billboard(vc, pose, cam, 0, 0.4, 0, 3.2F * easeOut(sa / 3F) * f + 0.2F, t * 8F, k(c2, 1.2F * f));
            vc = Fx.buf(buf, Fx.WAVE);
            float r = 0.4F + easeOut(sa / 10F) * 3.2F;
            Fx.ring(vc, m, 0.08F, r - 0.8F, r, 48, BLACK, k(c1, 1.3F * f), 4F, 0F);
        }
        int ab = e.clientAbility;
        if (ab == BeybladeEntity.AB_NONE) return;
        float at = e.abilityAge(partial);
        if (ab == CM_RUSH) {
            // Power Rush: colour streaks swirling round the top
            float f = Mth.clamp(1F - at / 30F, 0F, 1F);
            VertexConsumer vc = Fx.buf(buf, Fx.SOFT);
            for (int i = 0; i < 3; i++)
                Fx.helix(vc, m, 0.1F, 0.9F, 0.75F, 0.95F, t * 0.6F + i * Mth.TWO_PI / 3F, -0.8F * e.spinDir(), 0.22F, 16, k(c1, f), k(c2, 0.4F * f), t * 0.2F);
            return;
        }
        if (ab != CM_BURST) return;
        int mode = e.variant().mode();
        VertexConsumer vc;
        switch (mode) {
            case BeybladeEntity.MODE_ATTACK -> {
                // Upper Smash: a rising pillar of colour
                float f = Mth.clamp(1F - at / 18F, 0F, 1F);
                vc = Fx.buf(buf, Fx.BOLT);
                Fx.beamV(vc, m, new Vec3(0, 0, 0), new Vec3(0, 5.5 * easeOut(at / 5F), 0), 1.2F, k(c2, 1.2F * f), 0.4F, -t * 0.3F);
                vc = Fx.buf(buf, Fx.WAVE);
                Fx.band(vc, m, 0.6F, 0F, 1.8F, 2.5F * easeOut(at / 6F), 32, k(c1, f), BLACK, 2F, t * 0.1F, 1F, 0F);
            }
            case BeybladeEntity.MODE_DEFENSE -> {
                // Fortress Wave: a dome that bursts outwards
                float g = Mth.clamp(at / 6F, 0F, 1F);
                float f = at < 6 ? 1F : Mth.clamp(1F - (at - 6F) / 16F, 0F, 1F);
                vc = Fx.bufT(buf, Fx.HEX, 0.45F * f);
                Fx.sphere(vc, m, 0F, 1.2F + (at < 6 ? 0F : easeOut((at - 6F) / 10F) * 4.3F), Mth.HALF_PI, 8, 32, c1, 4F, 2F, 0F, 0F);
                Fx.alpha = 1F;
                vc = Fx.buf(buf, Fx.WAVE);
                float r = 1F + (at < 6 ? 0F : easeOut((at - 6F) / 12F) * 5F);
                Fx.ring(vc, m, 0.08F, r - 1.2F, r, 64, BLACK, k(c2, 1.2F * f * g), 6F, 0F);
            }
            case BeybladeEntity.MODE_STAMINA -> {
                // Spin Recovery: light spiralling into the top
                float f = Mth.clamp(Math.min(at / 6F, (40F - at) / 8F), 0F, 1F);
                vc = Fx.buf(buf, Fx.SOFT);
                for (int i = 0; i < 4; i++)
                    Fx.helix(vc, m, 2.6F, 0.2F, 1.6F, 0.4F, -t * 0.25F + i * Mth.HALF_PI, 1.5F, 0.18F, 24, k(c2, 0.3F * f), k(c1, f), -t * 0.1F);
                Fx.ring(vc, m, 0.06F, 0F, 1.4F, 32, k(c1, 0.6F * f), BLACK, 1F, 0F);
            }
            default -> {
                // Counter Burst: a sharp radial flash
                float f = Mth.clamp(1F - at / 14F, 0F, 1F);
                vc = Fx.buf(buf, Fx.WAVE);
                float r = 0.5F + easeOut(at / 8F) * 3.4F;
                Fx.ring(vc, m, 0.1F, r - 1F, r, 48, BLACK, k(c1, 1.4F * f), 5F, 0F);
                vc = Fx.buf(buf, Fx.SIGIL);
                Fx.groundQuad(vc, m, 0.05F, 2.4F * easeOut(at / 4F), k(c2, f));
            }
        }
    }
}
