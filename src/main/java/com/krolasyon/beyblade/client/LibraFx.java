package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.entity.BeybladeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.List;

import static com.krolasyon.beyblade.entity.BeybladeEntity.*;

/** Ability visual effects. The pose is at the beyblade's interpolated position, unrotated. */
public final class LibraFx {
    private LibraFx() {}

    private static final float[] LIME = {0.55F, 1F, 0.45F};
    private static final float[] CYAN = {0.4F, 1F, 0.85F};
    private static final float[] ORANGE = {1F, 0.5F, 0.12F};
    private static final float[] RED = {1F, 0.22F, 0.06F};
    private static final float[] SAND = {0.95F, 0.76F, 0.45F};
    private static final float[] GOLD = {1F, 0.8F, 0.32F};
    private static final float[] WHITE = {1F, 1F, 1F};
    private static final float[] BLACK = {0F, 0F, 0F};

    private static float[] k(float[] c, float k) { return new float[]{c[0] * k, c[1] * k, c[2] * k}; }

    private static float easeOut(float x) {
        x = Mth.clamp(x, 0F, 1F);
        return 1F - (1F - x) * (1F - x) * (1F - x);
    }

    public static void render(BeybladeEntity e, float partial, float t, double ex, double ey, double ez, PoseStack pose,
                              MultiBufferSource buf, Quaternionf cam) {
        int ab = e.clientAbility;
        float at = e.abilityAge(partial);
        if (e.isShielded()) shield(e, partial, t, pose, buf);
        float ia = t - e.impactTime;
        if (ia >= 0 && ia < 22) infernoImpact(ia, t, pose, buf, cam);
        float sa = t - e.strikeTime;
        if (sa >= 0 && sa < 18) judgmentStrike(e, sa, t, partial, ex, ey, ez, pose, buf);
        if (ab == AB_NONE || at < 0) return;
        double bx = e.abilityX - ex, by = e.abilityY - ey, bz = e.abilityZ - ez;
        switch (ab) {
            case AB_SONIC_WAVE -> sonicWave(at, t, pose, buf, cam);
            case AB_INFERNO -> inferno(at, t, bx, by, bz, pose, buf, cam);
            case AB_BUSTER -> buster(e, at, t, partial, ex, ey, ez, pose, buf, cam);
            case AB_JUDGMENT -> judgment(e, at, t, partial, bx, by, bz, ex, ey, ez, pose, buf, cam);
            default -> {}
        }
    }

    // ================= SONIC WAVE: resonance hover + three expanding sound walls =================
    private static void sonicWave(float at, float t, PoseStack pose, MultiBufferSource buf, Quaternionf cam) {
        float y = 0.32F;
        VertexConsumer vc = Fx.buf(buf, Fx.WAVE);
        Matrix4f m = pose.last().pose();
        for (int p : WAVE_PULSES) {
            float age = at - p;
            if (age < 0 || age >= WAVE_LIFE) continue;
            float front = 0.6F + age * WAVE_SPEED;
            float fade = 1F - age / WAVE_LIFE;
            Fx.ring(vc, m, y, front - 1.6F, front, 72, BLACK, k(CYAN, 1.3F * fade), 6F, t * 0.03F);
            Fx.ring(vc, m, y - 0.6F, front * 0.82F - 0.8F, front * 0.82F, 72, BLACK, k(LIME, 0.6F * fade), 6F, -t * 0.03F);
        }
        vc = Fx.buf(buf, Fx.SOFT);
        m = pose.last().pose();
        for (int p : WAVE_PULSES) {
            float age = at - p;
            if (age < 0 || age >= WAVE_LIFE) continue;
            float front = 0.6F + age * WAVE_SPEED;
            float fade = 1F - age / WAVE_LIFE;
            float h = 1.4F * (1F - age / WAVE_LIFE * 0.5F);
            Fx.band(vc, m, front, y - 0.5F, front + 0.15F, y + h, 72, k(CYAN, 0.55F * fade), BLACK, 8F, t * 0.05F, 0F, 0.5F);
        }
        // standing resonance rings hugging Libra
        for (int i = 0; i < 4; i++) {
            float f = (at * 0.07F + i / 4F) % 1F;
            float r = 0.5F + f * 1.6F;
            Fx.ring(vc, m, y + 0.05F + Mth.sin(t * 0.6F + i) * 0.05F, r - 0.12F, r, 48, k(LIME, (1F - f) * 0.7F), k(LIME, (1F - f) * 0.2F), 4F, 0F);
        }
        vc = Fx.buf(buf, Fx.FLARE);
        float pulse = 0F;
        for (int p : WAVE_PULSES) {
            float age = at - p;
            if (age >= 0 && age < 6) pulse = Math.max(pulse, 1F - age / 6F);
        }
        Fx.billboard(vc, pose, cam, 0, y, 0, 1.2F + pulse * 2.5F, t * 4F, k(CYAN, 0.6F + pulse * 0.6F));
    }

    // ================= SONIC SHIELD: rotating hexagonal resonance dome =================
    private static void shield(BeybladeEntity e, float partial, float t, PoseStack pose, MultiBufferSource buf) {
        int rem = e.shieldRemaining();
        float age = SHIELD_TICKS - rem + partial;
        float grow = age < 8 ? Mth.clamp(1F + 2.70158F * (float) Math.pow(age / 8F - 1F, 3) + 1.70158F * (float) Math.pow(age / 8F - 1F, 2), 0F, 1.2F) : 1F;
        float fadeOut = rem < 14 ? (rem / 14F) * (0.6F + 0.4F * Mth.sin(t * 2.2F)) : 1F;
        float hit = Math.max(0F, 1F - (t - e.shieldHitTime) / 9F);
        float R = 1.3F * grow;
        float kk = (0.5F + 0.1F * Mth.sin(t * 0.4F) + hit * 0.9F) * fadeOut;
        float[] col = e.isRival() ? new float[]{1F, 0.35F, 0.25F} : new float[]{0.35F, 1F, 0.6F};
        VertexConsumer vc = Fx.buf(buf, Fx.HEX);
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(t * 1.6F));
        Fx.sphere(vc, pose.last().pose(), 0.25F, R, Mth.PI * 0.62F, 12, 30, k(col, kk), 6F, 3F, 0F, t * 0.01F);
        pose.popPose();
        vc = Fx.buf(buf, Fx.SOFT);
        Matrix4f m = pose.last().pose();
        Fx.ring(vc, m, 0.04F, R - 0.35F, R + 0.12F, 48, BLACK, k(col, kk * 1.1F), 3F, t * 0.02F);
        Fx.sphere(vc, m, 0.25F, R * 1.01F, Mth.PI * 0.62F, 8, 24, k(col, kk * 0.18F), 1F, 0F, 0F, 0.5F);
        if (hit > 0) {
            Fx.sphere(vc, m, 0.25F, R * (1.02F + 0.25F * (1F - hit)), Mth.PI * 0.62F, 10, 24, k(WHITE, hit * 0.5F), 2F, 1F, 0F, 0F);
        }
    }

    // ================= INFERNO BLAST: drill -> sand/fire tornado -> meteor dive =================
    private static void inferno(float at, float t, double bx, double by, double bz, PoseStack pose, MultiBufferSource buf, Quaternionf cam) {
        pose.pushPose();
        pose.translate(bx, by, bz);
        Matrix4f m = pose.last().pose();
        if (at < INF_TORNADO + 4) {
            float g = Mth.clamp(at / 10F, 0F, 1F);
            float f = at > INF_TORNADO ? 1F - (at - INF_TORNADO) / 4F : 1F;
            VertexConsumer vc = Fx.buf(buf, Fx.WISP);
            Fx.ring(vc, m, 0.04F, 0.2F, 0.6F + 2.8F * g, 48, k(ORANGE, 1.3F * f), k(RED, 0.1F * f), 3F, -t * 0.08F);
            for (int i = 0; i < 4; i++) {
                Fx.helix(vc, m, 0.05F, 0.4F + 1.1F * g, 3.4F * g, 0.35F, t * 0.42F + i * Mth.HALF_PI, -1.3F, 0.4F, 26,
                        k(SAND, 0.9F * f), k(SAND, 0.15F * f), t * 0.12F);
            }
            vc = Fx.buf(buf, Fx.FLARE);
            Fx.billboard(vc, pose, cam, 0, 0.2, 0, 1.4F + 0.5F * Mth.sin(t * 1.3F), t * 9F, k(ORANGE, 0.9F * f));
        }
        if (at >= INF_TORNADO && at < INF_DIVE + 10) {
            float grow = easeOut((at - INF_TORNADO) / 7F);
            float fade = at > INF_DIVE ? 1F - (at - INF_DIVE) / 10F : 1F;
            float H = (float) INF_HEIGHT * grow + (1F - fade) * 2F;
            float wide = 1F + (1F - fade) * 0.6F;
            // sand wall (real colour, translucent) with a fire cone inside and flame ribbons spiralling up
            VertexConsumer vc = Fx.bufT(buf, Fx.WISP, 0.75F * fade);
            Fx.band(vc, m, 1.6F * wide, 0F, 3.1F * wide, H, 36, new float[]{0.86F, 0.66F, 0.38F}, new float[]{0.96F, 0.82F, 0.56F}, 3F, t * 0.14F, 2F, -t * 0.09F);
            vc = Fx.bufT(buf, Fx.WISP, 0.85F * fade);
            Fx.band(vc, m, 1.1F * wide, 0F, 2.3F * wide, H * 0.96F, 32, new float[]{1F, 0.42F, 0.05F}, new float[]{0.95F, 0.2F, 0.02F}, 2F, -t * 0.2F, 3F, -t * 0.16F);
            Fx.alpha = 1F;
            vc = Fx.buf(buf, Fx.WISP);
            for (int i = 0; i < 6; i++) {
                Fx.helix(vc, m, 0F, H, 0.9F * wide, 2.9F * wide, -t * 0.42F + i * Mth.TWO_PI / 6F, 2.3F, 0.7F, 44,
                        k(new float[]{1F, 0.55F, 0.12F}, 0.7F * fade), k(RED, 0.15F * fade), -t * 0.2F);
            }
            vc = Fx.buf(buf, Fx.SOFT);
            Fx.band(vc, m, 0.55F, 0F, 0.95F, H, 24, k(new float[]{1F, 0.7F, 0.3F}, 0.35F * fade), k(ORANGE, 0.08F * fade), 3F, t * 0.3F, 0F, 0.5F);
            Fx.ring(vc, m, 0.05F, 0F, 3.6F * wide, 48, k(ORANGE, 0.7F * fade), BLACK, 2F, 0F);
            vc = Fx.buf(buf, Fx.FLARE);
            Fx.billboard(vc, pose, cam, 0, H, 0, 4F * fade, -t * 3F, k(ORANGE, 0.5F * fade));
        }
        pose.popPose();
    }

    private static void infernoImpact(float ia, float t, PoseStack pose, MultiBufferSource buf, Quaternionf cam) {
        float fade = 1F - ia / 22F;
        float front = 0.5F + easeOut(ia / 14F) * (float) INF_IMPACT_RADIUS * 1.6F;
        Matrix4f m = pose.last().pose();
        VertexConsumer vc = Fx.buf(buf, Fx.WAVE);
        Fx.ring(vc, m, 0.08F, front - 2.2F, front, 72, BLACK, k(ORANGE, 1.6F * fade), 6F, 0F);
        Fx.ring(vc, m, 0.1F, front * 0.6F - 1F, front * 0.6F, 72, BLACK, k(SAND, 0.8F * fade), 6F, 0.3F);
        vc = Fx.bufT(buf, Fx.WISP, 0.9F * fade);
        Fx.band(vc, m, front, 0F, front + 0.4F, 1.8F * fade, 64, new float[]{1F, 0.45F, 0.06F}, new float[]{1F, 0.75F, 0.3F}, 8F, t * 0.1F, 1F, -t * 0.1F);
        Fx.alpha = 1F;
        vc = Fx.buf(buf, Fx.WISP);
        Fx.ring(vc, m, 0.06F, 0F, 3.2F, 48, k(ORANGE, 1.2F * fade * fade), k(RED, 0.2F * fade), 3F, t * 0.05F);
        if (ia < 12) {
            vc = Fx.buf(buf, Fx.FLARE);
            float f = 1F - ia / 12F;
            Fx.billboard(vc, pose, cam, 0, 0.6, 0, 9F * f, t * 2F, k(new float[]{1F, 0.75F, 0.35F}, 1.2F * f));
            vc = Fx.buf(buf, Fx.SOFT);
            Fx.beam(vc, pose.last().pose(), new Vec3(0, 0, 0), new Vec3(0, 7 * f + 1, 0), 2.6F * f, k(ORANGE, 1.2F * f), BLACK, 0.3F, -t * 0.2F);
        }
    }

    // ================= SONIC BUSTER: airborne charge + resonance cannon =================
    private static void buster(BeybladeEntity e, float at, float t, float partial, double ex, double ey, double ez, PoseStack pose,
                               MultiBufferSource buf, Quaternionf cam) {
        Entity tgt = e.level().getEntity(e.getTargetId());
        Vec3 from = new Vec3(0, 0.38, 0);
        Vec3 to = null;
        if (tgt != null) {
            to = new Vec3(Mth.lerp(partial, tgt.xo, tgt.getX()) - ex, Mth.lerp(partial, tgt.yo, tgt.getY()) + tgt.getBbHeight() * 0.5 - ey,
                    Mth.lerp(partial, tgt.zo, tgt.getZ()) - ez);
        }
        Vec3 dir = to != null && to.subtract(from).lengthSqr() > 1.0E-4 ? to.subtract(from).normalize() : new Vec3(1, 0, 0);
        Vec3 front = from.add(dir.scale(0.75));
        Matrix4f m = pose.last().pose();
        if (at < BUS_FIRE) {
            float ch = Mth.clamp((at - BUS_CHARGE) / (BUS_FIRE - BUS_CHARGE), 0F, 1F);
            VertexConsumer vc = Fx.buf(buf, Fx.WAVE);
            if (at >= BUS_CHARGE) {
                for (int i = 0; i < 3; i++) {
                    float f = (at * 0.16F + i / 3F) % 1F;
                    float r = 2.6F * (1F - f) + 0.2F;
                    Fx.ringAround(vc, m, front, dir, r - 0.35F, r, 40, k(CYAN, f * 1.2F), t * 0.05F);
                }
            }
            vc = Fx.buf(buf, Fx.SOFT);
            if (at >= BUS_CHARGE) {
                for (int i = 0; i < 8; i++) {
                    float f = (at * 0.11F + i / 8F) % 1F;
                    double a = i * Math.PI / 4 + t * 0.15;
                    Vec3 up = Math.abs(dir.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
                    Vec3 e1 = dir.cross(up).normalize(), e2 = dir.cross(e1).normalize();
                    Vec3 start = front.add(e1.scale(Math.cos(a) * 2.4 * (1 - f))).add(e2.scale(Math.sin(a) * 2.4 * (1 - f))).add(dir.scale(-0.6 * (1 - f)));
                    Fx.beam(vc, m, start, front, 0.07F, k(CYAN, 0.1F), k(WHITE, 0.9F * f), 1F, 0F);
                }
            }
            Fx.ring(vc, m, -0.3F, 0.2F, 1.2F + ch * 0.8F, 40, k(CYAN, 0.6F), BLACK, 2F, t * 0.05F);
            vc = Fx.buf(buf, Fx.FLARE);
            Fx.billboard(vc, pose, cam, front.x, front.y, front.z, 0.4F + 1.8F * ch + 0.2F * Mth.sin(t * 2F), t * 6F, k(new float[]{0.7F, 1F, 0.95F}, 0.6F + 0.6F * ch));
        }
        if (to != null && at >= BUS_FIRE && at < BUS_END + 5) {
            float ff = at < BUS_FIRE + 3 ? (at - BUS_FIRE) / 3F : (at > BUS_END ? 1F - (at - BUS_END) / 5F : 1F);
            float flick = 0.85F + 0.15F * Mth.sin(t * 3.3F);
            VertexConsumer vc = Fx.buf(buf, Fx.SOFT);
            Fx.beam(vc, m, front, to, 1.6F * ff, k(new float[]{0.2F, 0.9F, 0.65F}, 0.8F * ff), k(new float[]{0.2F, 0.9F, 0.65F}, 0.6F * ff), 0.5F, -t * 0.5F);
            Fx.beam(vc, m, front, to, 0.5F * ff, k(new float[]{0.85F, 1F, 1F}, 1.1F * ff * flick), k(WHITE, ff * flick), 0.7F, -t * 0.9F);
            vc = Fx.buf(buf, Fx.BOLT);
            Fx.beamV(vc, m, front, to, 0.9F * ff, k(CYAN, 0.7F * ff), 0.4F, -t * 0.35F);
            vc = Fx.buf(buf, Fx.WAVE);
            double len = to.subtract(front).length();
            for (int i = 0; i < 6; i++) {
                double s = ((at - BUS_FIRE) * 1.7 + i * len / 6.0) % Math.max(0.1, len);
                Vec3 c = front.add(dir.scale(s));
                float fr = (float) (1 - s / len);
                Fx.ringAround(vc, m, c, dir, 0.25F, 0.85F + 0.3F * fr, 32, k(CYAN, ff * (0.4F + 0.8F * fr)), 0F);
            }
            vc = Fx.buf(buf, Fx.FLARE);
            Fx.billboard(vc, pose, cam, front.x, front.y, front.z, 2.0F * ff * flick, t * 7F, k(WHITE, ff));
            Fx.billboard(vc, pose, cam, to.x, to.y, to.z, 3.2F * ff * flick, -t * 5F, k(new float[]{0.6F, 1F, 0.9F}, ff));
        }
    }

    // ================= JUDGMENT OF THE SCALES: golden sigil, giant balance, chains that drain spin =================
    private static void judgment(BeybladeEntity e, float at, float t, float partial, double bx, double by, double bz,
                                 double ex, double ey, double ez, PoseStack pose, MultiBufferSource buf, Quaternionf cam) {
        float dur = DURATION[AB_JUDGMENT];
        float in = Mth.clamp(at / 15F, 0F, 1F), out = at > dur - 14 ? Math.max(0F, (dur - at) / 14F) : 1F;
        float kk = in * out;
        pose.pushPose();
        pose.translate(bx, by, bz);
        VertexConsumer vc = Fx.buf(buf, Fx.SIGIL);
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(t * 0.8F));
        Fx.groundQuad(vc, pose.last().pose(), 0.06F, 6.2F * easeOut(in), k(GOLD, 0.95F * kk));
        pose.popPose();
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-t * 1.7F));
        Fx.groundQuad(vc, pose.last().pose(), 0.09F, 3.0F * easeOut(in), k(new float[]{1F, 0.95F, 0.7F}, 0.6F * kk));
        pose.popPose();

        // the balance, floating above
        float rise = easeOut((at - 4F) / 16F);
        pose.pushPose();
        pose.translate(0, 2.5F + 3.0F * rise + 0.15F * Mth.sin(t * 0.08F), 0);
        pose.mulPose(Axis.YP.rotationDegrees(t * 0.6F));
        float tiltDeg = at > JUD_LINK ? Mth.clamp((at - JUD_LINK) / 50F, 0F, 1F) * 16F : 0F;
        float th = tiltDeg * Mth.DEG_TO_RAD;
        Matrix4f m = pose.last().pose();
        vc = Fx.bufT(buf, Fx.SOFT, kk);
        float[] gold = {1F, 0.74F, 0.18F};
        Fx.beam(vc, m, new Vec3(0, -2.6, 0), new Vec3(0, 1.0, 0), 0.4F, gold, gold, 0.3F, 0F);
        Vec3 L = new Vec3(-3.2 * Math.cos(th), -3.2 * Math.sin(th), 0), Rr = new Vec3(3.2 * Math.cos(th), 3.2 * Math.sin(th), 0);
        Fx.beam(vc, m, L, Rr, 0.32F, gold, gold, 0.3F, t * 0.02F);
        for (Vec3 end : new Vec3[]{L, Rr}) {
            Vec3 pan = end.add(0, -2.1, 0);
            for (int i = 0; i < 3; i++) {
                double a = i * Math.PI * 2 / 3;
                Fx.beam(vc, m, end, pan.add(Math.cos(a) * 0.85, 0, Math.sin(a) * 0.85), 0.07F, gold, gold, 1F, 0F);
            }
            Fx.beam(vc, m, end.add(-0.25, 0.25, 0), end.add(0.25, -0.25, 0), 0.25F, new float[]{1F, 0.95F, 0.7F}, new float[]{1F, 0.95F, 0.7F}, 1F, 0F);
        }
        pose.popPose();
        // pans (horizontal discs) - drawn untilted around their own centres
        pose.pushPose();
        pose.translate(0, 2.5F + 3.0F * rise + 0.15F * Mth.sin(t * 0.08F), 0);
        pose.mulPose(Axis.YP.rotationDegrees(t * 0.6F));
        for (int s = -1; s <= 1; s += 2) {
            pose.pushPose();
            pose.translate(s * 3.2 * Math.cos(th), s * 3.2 * Math.sin(th) - 2.1, 0);
            Matrix4f pm = pose.last().pose();
            Fx.ring(vc, pm, 0F, 0F, 0.95F, 32, new float[]{0.85F, 0.6F, 0.12F}, new float[]{1F, 0.8F, 0.3F}, 2F, 0F);
            Fx.band(vc, pm, 0.95F, 0F, 0.7F, -0.3F, 32, new float[]{1F, 0.78F, 0.25F}, new float[]{0.75F, 0.5F, 0.1F}, 2F, 0F, 0F, 0.5F);
            pose.popPose();
        }
        Fx.alpha = 1F;
        vc = Fx.buf(buf, Fx.FLARE);
        Fx.billboard(vc, pose, cam, 0, 2.5F + 3.0F * rise + 1.2F, 0, 2.4F + 0.4F * Mth.sin(t * 0.7F), t * 2F, k(GOLD, kk));
        pose.popPose();
        pose.popPose();

        // chains to every enemy while draining
        if (at >= JUD_LINK && at < JUD_STRIKE + 4) {
            float lk = Mth.clamp((at - JUD_LINK) / 5F, 0F, 1F) * (at > JUD_STRIKE ? 1F - (at - JUD_STRIKE) / 4F : 1F);
            Vec3 from = new Vec3(0, 0.4, 0);
            List<Entity> enemies = e.enemiesWithin(JUD_RANGE);
            for (Entity en : enemies) {
                Vec3 pe = new Vec3(Mth.lerp(partial, en.xo, en.getX()) - ex, Mth.lerp(partial, en.yo, en.getY()) + en.getBbHeight() * 0.5 - ey,
                        Mth.lerp(partial, en.zo, en.getZ()) - ez);
                float flick = 0.75F + 0.25F * Mth.sin(t * 2.1F + en.getId());
                vc = Fx.buf(buf, Fx.BOLT);
                Fx.beamV(vc, pose.last().pose(), from, pe, 0.6F * lk, k(GOLD, 1.0F * flick * lk), 0.5F, -t * 0.3F);
                vc = Fx.buf(buf, Fx.SOFT);
                Fx.beam(vc, pose.last().pose(), pe, from, 0.18F * lk, k(WHITE, 0.4F * lk), k(GOLD, 0.9F * lk), 0.8F, -t * 0.6F);
                vc = Fx.buf(buf, Fx.FLARE);
                Fx.billboard(vc, pose, cam, pe.x, pe.y, pe.z, 1.3F * lk * flick, t * 5F, k(GOLD, 0.8F * lk));
            }
        }
    }

    private static void judgmentStrike(BeybladeEntity e, float sa, float t, float partial, double ex, double ey, double ez, PoseStack pose, MultiBufferSource buf) {
        float f = 1F - sa / 18F;
        for (Entity en : e.enemiesWithin(JUD_RANGE + 2)) {
            Vec3 feet = new Vec3(Mth.lerp(partial, en.xo, en.getX()) - ex, Mth.lerp(partial, en.yo, en.getY()) - ey, Mth.lerp(partial, en.zo, en.getZ()) - ez);
            VertexConsumer vc = Fx.bufT(buf, Fx.SOFT, f);
            Matrix4f m = pose.last().pose();
            Fx.beam(vc, m, feet.add(0, 28, 0), feet.add(0, -0.3, 0), 2.6F * f, new float[]{1F, 0.85F, 0.35F}, new float[]{1F, 0.7F, 0.15F}, 0.15F, -t * 0.3F);
            Fx.alpha = 1F;
            vc = Fx.buf(buf, Fx.SOFT);
            Fx.beam(vc, m, feet.add(0, 28, 0), feet.add(0, -0.3, 0), 0.7F * f, k(WHITE, 1.2F * f), k(WHITE, 1.2F * f), 0.15F, -t * 0.6F);
            vc = Fx.buf(buf, Fx.WAVE);
            pose.pushPose();
            pose.translate(feet.x, feet.y, feet.z);
            float r = 0.5F + easeOut(sa / 12F) * 3.5F;
            Fx.ring(vc, pose.last().pose(), 0.06F, r - 1.0F, r, 40, BLACK, k(GOLD, 1.3F * f), 4F, 0F);
            pose.popPose();
        }
    }
}
