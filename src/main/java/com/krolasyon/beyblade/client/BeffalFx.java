package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.LibraBeyblade;
import com.krolasyon.beyblade.compat.V;
import com.krolasyon.beyblade.entity.BeffalEntity;
import com.krolasyon.beyblade.entity.BeybladeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import static com.krolasyon.beyblade.entity.BeffalEntity.*;

/** Evil Befall: peacock tail guard, the evil eyes, feather storms, rainbow mirages and the dark peacock requiem. */
public final class BeffalFx {
    private BeffalFx() {}

    public static final ResourceLocation PEACOCK = V.rl(LibraBeyblade.MODID, "textures/fx/peacock.png");
    public static final ResourceLocation FEATHER = V.rl(LibraBeyblade.MODID, "textures/fx/eyefeather.png");
    public static final ResourceLocation EYE = V.rl(LibraBeyblade.MODID, "textures/fx/evileye.png");
    private static final float[] PINK = {1F, 0.35F, 0.75F};
    private static final float[] VIOLET = {0.65F, 0.25F, 1F};
    private static final float[] GOLD = {1F, 0.8F, 0.35F};
    private static final float[] WHITE = {1F, 1F, 1F};
    private static final float[] BLACK = {0F, 0F, 0F};

    private static float[] k(float[] c, float k) { return new float[]{c[0] * k, c[1] * k, c[2] * k}; }

    private static float easeOut(float x) {
        x = Mth.clamp(x, 0F, 1F);
        return 1F - (1F - x) * (1F - x) * (1F - x);
    }

    private static float[] rainbow(float h, float k) {
        int c = java.awt.Color.HSBtoRGB(h - (float) Math.floor(h), 0.75F, 1F);
        return new float[]{((c >> 16) & 255) / 255F * k, ((c >> 8) & 255) / 255F * k, (c & 255) / 255F * k};
    }

    /** an upright sprite turned to the camera around the vertical axis, bottom centre at (x, y, z) */
    private static void upright(MultiBufferSource buf, ResourceLocation tex, Quaternionf cam, Matrix4f m, double x, double y, double z,
                                float w, float h, float a) {
        if (a <= 0.01F || w <= 0.01F) return;
        org.joml.Vector3f right = new org.joml.Vector3f(1, 0, 0);
        cam.transform(right);
        Vec3 r = new Vec3(right.x(), 0, right.z());
        if (r.lengthSqr() < 1.0E-4) r = new Vec3(1, 0, 0);
        r = r.normalize().scale(w / 2);
        VertexConsumer vc = Fx.bufT(buf, tex, a);
        float[] p0 = {(float) (x - r.x), (float) (y + h), (float) (z - r.z)};
        float[] p1 = {(float) (x - r.x), (float) y, (float) (z - r.z)};
        float[] p2 = {(float) (x + r.x), (float) y, (float) (z + r.z)};
        float[] p3 = {(float) (x + r.x), (float) (y + h), (float) (z + r.z)};
        Fx.quad(vc, m, p0, p1, p2, p3, 0F, 0F, 1F, 1F, WHITE, WHITE);
        Fx.alpha = 1F;
    }

    /** an eye feather flying along dir, tip (the eye) at p; two crossed planes so it reads from any angle */
    private static void feather(VertexConsumer vc, Matrix4f m, Vec3 p, Vec3 dir, float len, float w) {
        Vec3 d = dir.normalize();
        Vec3 side = d.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < 1.0E-4) side = new Vec3(1, 0, 0);
        side = side.normalize().scale(w / 2);
        Vec3 up = side.cross(d).normalize().scale(w / 2);
        Vec3 tail = p.subtract(d.scale(len));
        for (Vec3 s : new Vec3[]{side, up}) {
            float[] a = {(float) (p.x - s.x), (float) (p.y - s.y), (float) (p.z - s.z)};
            float[] b = {(float) (tail.x - s.x), (float) (tail.y - s.y), (float) (tail.z - s.z)};
            float[] c = {(float) (tail.x + s.x), (float) (tail.y + s.y), (float) (tail.z + s.z)};
            float[] e = {(float) (p.x + s.x), (float) (p.y + s.y), (float) (p.z + s.z)};
            Fx.quad(vc, m, a, b, c, e, 0F, 0F, 1F, 1F, WHITE, WHITE);
        }
    }

    public static void render(BeffalEntity e, float partial, float t, double ex, double ey, double ez, PoseStack pose,
                              MultiBufferSource buf, Quaternionf cam) {
        Matrix4f m = pose.last().pose();
        if (e.isShielded()) {
            // Peacock Plumage Guard: the tail fanned out behind it, a pink hex wall around it
            float p = 0.8F + 0.2F * Mth.sin(t * 0.5F);
            upright(buf, PEACOCK, cam, m, 0, -0.1, 0, 3.6F + 0.15F * Mth.sin(t * 0.3F), 3.6F, 0.8F * p);
            VertexConsumer vc = Fx.bufT(buf, Fx.HEX, 0.45F * p);
            Fx.band(vc, m, 1.15F, 0F, 1.25F, 1.4F, 30, PINK, k(VIOLET, 0.4F), 3F, t * 0.05F, 1F, 0F);
            Fx.alpha = 1F;
            vc = Fx.buf(buf, Fx.SOFT);
            Fx.ring(vc, m, 0.05F, 0.9F, 1.3F, 40, k(PINK, 0.7F * p), BLACK, 1F, 0F);
        }
        renderGaze(e, t, ex, ey, ez, pose, buf, cam);
        renderVolleys(e, partial, t, pose, buf);
        float ms = t - e.mirageStrikeTime;
        if (ms >= 0 && ms < 8) {
            VertexConsumer vc = Fx.buf(buf, Fx.FLARE);
            Fx.billboard(vc, pose, cam, 0, 0.6, 0, 3F * (1F - ms / 8F), t * 10F, rainbow(t * 0.05F, 1.2F * (1F - ms / 8F)));
        }
        float hit = t - e.requiemHitTime;
        if (hit >= 0 && hit < 40) requiemImpact(e, hit, t, ex, ey, ez, pose, buf, cam);
        int ab = e.clientAbility;
        float at = e.abilityAge(partial);
        if (ab == 0 || at < 0) return;
        switch (ab) {
            case BF_MIRAGE -> mirage(e, at, t, pose, buf);
            case BF_REQUIEM -> requiem(e, at, t, ex, ey, ez, pose, buf, cam);
            default -> {}
        }
    }

    private static void renderGaze(BeffalEntity e, float t, double ex, double ey, double ez, PoseStack pose, MultiBufferSource buf, Quaternionf cam) {
        float g = t - e.gazeTime;
        if (g < 0 || g > 24 + HYPNO_TICKS) return;
        Entity tg = e.level().getEntity(e.gazeTargetId);
        Matrix4f m = pose.last().pose();
        if (g < 40) {
            // a ring of evil eyes opens around Befall and stares at the rival
            float open = easeOut(g / 10F) * Mth.clamp((40 - g) / 8F, 0F, 1F);
            VertexConsumer vc = Fx.bufT(buf, EYE, 0.95F * open);
            Vec3[] eyes = new Vec3[8];
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4 + t * 0.03;
                eyes[i] = new Vec3(Math.cos(a) * 1.5, 1.1 + 0.25 * Math.sin(t * 0.2 + i), Math.sin(a) * 1.5);
                Fx.billboard(vc, pose, cam, eyes[i].x, eyes[i].y, eyes[i].z, 0.75F * open, 0F, WHITE);
            }
            Fx.alpha = 1F;
            if (tg != null && g >= 10 && g < 26) {
                float f = 1F - Math.abs(g - 18) / 8F;
                Vec3 to = new Vec3(tg.getX() - ex, tg.getY() - ey + 0.3, tg.getZ() - ez);
                vc = Fx.buf(buf, Fx.SOFT);
                for (Vec3 p : eyes) Fx.beam(vc, m, p, to, 0.14F * f, k(PINK, 1.3F * f), k(VIOLET, 1.3F * f), 0.5F, 0F);
            }
        }
        if (tg != null && g >= 8) {
            // over the rival: one giant eye, and a hypnotic spiral around it while the trance lasts
            float life = Mth.clamp((24 + HYPNO_TICKS - g) / 12F, 0F, 1F) * Mth.clamp((g - 8) / 6F, 0F, 1F);
            double tx = tg.getX() - ex, ty = tg.getY() - ey, tz = tg.getZ() - ez;
            VertexConsumer vc = Fx.bufT(buf, EYE, 0.95F * life);
            Fx.billboard(vc, pose, cam, tx, ty + 1.9 + 0.1 * Math.sin(t * 0.15), tz, 1.5F * life, 0F, WHITE);
            Fx.alpha = 1F;
            pose.pushPose();
            pose.translate(tx, ty, tz);
            Matrix4f mt = pose.last().pose();
            vc = Fx.buf(buf, Fx.SOFT);
            for (int i = 0; i < 2; i++)
                Fx.helix(vc, mt, 0F, 1.4F, 0.9F, 0.3F, -t * 0.35F + i * Mth.PI, 2.2F, 0.12F, 30, k(VIOLET, 1.1F * life), k(PINK, 0.4F * life), t * 0.1F);
            vc = Fx.buf(buf, Fx.WAVE);
            Fx.ring(vc, mt, 0.06F, 0.4F, 1.1F, 40, BLACK, k(VIOLET, 0.9F * life), 3F, -t * 0.08F);
            pose.popPose();
        }
    }

    private static void renderVolleys(BeffalEntity e, float partial, float t, PoseStack pose, MultiBufferSource buf) {
        Matrix4f m = pose.last().pose();
        VertexConsumer vc = null;
        for (int s = 0; s < e.volleyTime.length; s++) {
            float a = t - e.volleyTime[s];
            if (a < 0 || a > 9) continue;
            if (vc == null) vc = Fx.bufT(buf, FEATHER, 0.95F);
            Fx.alpha = 0.95F * Mth.clamp((9 - a) / 3F, 0F, 1F);
            Vec3 dir = new Vec3(e.volleyDir[s][0], 0, e.volleyDir[s][2]);
            float dist = 0.6F + easeOut(a / 7F) * (float) VOLLEY_RANGE;
            for (int i = 0; i < VOLLEY_N; i++) {
                double rot = (i - (VOLLEY_N - 1) / 2.0) * 0.17;
                Vec3 d = new Vec3(dir.x * Math.cos(rot) - dir.z * Math.sin(rot), 0.02, dir.x * Math.sin(rot) + dir.z * Math.cos(rot));
                Vec3 p = d.scale(dist).add(0, 0.55 + 0.12 * Math.sin(i * 1.7 + a), 0);
                feather(vc, m, p, d, 1.3F, 0.55F);
            }
        }
        Fx.alpha = 1F;
    }

    private static void mirage(BeffalEntity e, float at, float t, PoseStack pose, MultiBufferSource buf) {
        float f = Mth.clamp(at / 8F, 0F, 1F) * Mth.clamp((BF_DURATION[BF_MIRAGE] - at) / 8F, 0F, 1F);
        // three rainbow illusions orbiting the real Befall
        for (int i = 0; i < MIRAGE_N; i++) {
            double a = t * 0.22 * e.spinDir() + i * Math.PI * 2 / MIRAGE_N;
            double x = Math.cos(a) * MIRAGE_R, z = Math.sin(a) * MIRAGE_R;
            pose.pushPose();
            pose.translate(x, 0.02, z);
            pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-t * 40F * e.spinDir()));
            pose.scale(BeybladeEntity.RADIUS, BeybladeEntity.RADIUS, BeybladeEntity.RADIUS);
            BeyMesh.BEFFAL.renderGhostDisc(pose, buf, 0xF000F0, 0.5F * f, BeyMesh.BEFFAL.TOP_Y - 0.2F);
            pose.popPose();
            pose.pushPose();
            pose.translate(x, 0, z);
            VertexConsumer vc = Fx.buf(buf, Fx.SOFT);
            Fx.ring(vc, pose.last().pose(), 0.04F, 0.3F, 0.65F, 24, rainbow(t * 0.03F + i / 3F, 0.8F * f), BLACK, 1F, 0F);
            pose.popPose();
        }
        Matrix4f m = pose.last().pose();
        VertexConsumer vc = Fx.buf(buf, Fx.WAVE);
        Fx.ring(vc, m, 0.06F, (float) MIRAGE_R - 0.35F, (float) MIRAGE_R + 0.35F, 60, BLACK, rainbow(t * 0.02F, 0.7F * f), 6F, t * 0.04F);
    }

    private static void requiem(BeffalEntity e, float at, float t, double ex, double ey, double ez, PoseStack pose, MultiBufferSource buf, Quaternionf cam) {
        Matrix4f m = pose.last().pose();
        float form = easeOut(at / REQ_FORM);
        float fade = Mth.clamp((REQ_BLAST + 14 - at) / 10F, 0F, 1F);
        // the giant dark peacock rises behind Befall and spreads its tail
        upright(buf, PEACOCK, cam, m, 0, -1.4, 0, 10.5F * form, 10.5F * form, 0.85F * fade);
        VertexConsumer vc = Fx.buf(buf, Fx.SOFT);
        Fx.ring(vc, m, -1.2F, 0.6F, 2.4F * form, 40, k(VIOLET, 0.8F * fade), BLACK, 1F, 0F);
        if (e.requiemTime < e.clientAbilityStart) return;
        double sx = e.reqX - ex, sy = e.reqY - ey, sz = e.reqZ - ez;
        // the ring of eyes around the rival, appearing one by one
        float shrink = at < REQ_FIRE ? 1F : 1F - 0.35F * Mth.clamp((at - REQ_FIRE) / (REQ_BLAST - REQ_FIRE), 0F, 1F);
        Vec3 target = new Vec3(sx, sy + 0.3, sz);
        Vec3[] eyes = new Vec3[REQ_EYES];
        for (int i = 0; i < REQ_EYES; i++) {
            double a = i * Math.PI * 2 / REQ_EYES + t * 0.04;
            eyes[i] = new Vec3(sx + Math.cos(a) * REQ_RING * shrink, sy + 1.6 + 0.4 * Math.sin(a * 2 + t * 0.1), sz + Math.sin(a) * REQ_RING * shrink);
        }
        vc = Fx.bufT(buf, EYE, 0.95F);
        for (int i = 0; i < REQ_EYES; i++) {
            float appear = Mth.clamp((at - 4 - i * 1.8F) / 4F, 0F, 1F);
            if (appear <= 0) continue;
            Fx.alpha = 0.95F * appear;
            Fx.billboard(vc, pose, cam, eyes[i].x, eyes[i].y, eyes[i].z, 1.1F * appear, 0F, WHITE);
        }
        Fx.alpha = 1F;
        if (at >= REQ_FIRE && at < REQ_BLAST) {
            float pulse = 0.6F + 0.4F * Mth.sin(t * 1.3F);
            vc = Fx.buf(buf, Fx.SOFT);
            for (int i = 0; i < REQ_EYES; i++)
                Fx.beam(vc, m, eyes[i], target, 0.18F * pulse, k(PINK, 1.4F * pulse), k(VIOLET, 1.4F * pulse), 0.5F, 0F);
            vc = Fx.buf(buf, Fx.FLARE);
            Fx.billboard(vc, pose, cam, target.x, target.y, target.z, 2.2F * pulse, t * 6F, k(PINK, pulse));
        }
        // a rainbow seal on the ground under the rival
        pose.pushPose();
        pose.translate(sx, sy + 0.08, sz);
        vc = Fx.buf(buf, Fx.WAVE);
        Fx.ring(vc, pose.last().pose(), 0F, (float) REQ_RING * shrink - 0.4F, (float) REQ_RING * shrink + 0.2F, 60, BLACK, rainbow(t * 0.02F, 1.0F * form), 8F, t * 0.03F);
        vc = Fx.buf(buf, Fx.SIGIL);
        Fx.groundQuad(vc, pose.last().pose(), 0.02F, (float) REQ_RING * shrink, k(VIOLET, 0.7F * form));
        pose.popPose();
    }

    private static void requiemImpact(BeffalEntity e, float hit, float t, double ex, double ey, double ez, PoseStack pose, MultiBufferSource buf, Quaternionf cam) {
        float f = 1F - hit / 40F;
        double sx = e.reqX - ex, sy = e.reqY - ey, sz = e.reqZ - ez;
        pose.pushPose();
        pose.translate(sx, sy + 0.1, sz);
        Matrix4f m = pose.last().pose();
        VertexConsumer vc = Fx.buf(buf, Fx.WAVE);
        float r = 0.5F + easeOut(hit / 14F) * 9F;
        Fx.ring(vc, m, 0.05F, r - 2F, r, 72, BLACK, k(PINK, 1.5F * f), 6F, 0F);
        Fx.ring(vc, m, 0.08F, r * 0.6F - 1F, r * 0.6F, 72, BLACK, rainbow(t * 0.05F, 1.2F * f), 6F, 0.3F);
        // the eye feathers burst outward
        vc = Fx.bufT(buf, FEATHER, 0.9F * f);
        float d = 0.5F + easeOut(hit / 12F) * 6F;
        for (int i = 0; i < 16; i++) {
            double a = i * Math.PI / 8 + 0.2;
            Vec3 dir = new Vec3(Math.cos(a), 0.25, Math.sin(a));
            feather(vc, m, dir.scale(d).add(0, 0.4, 0), dir, 1.4F, 0.6F);
        }
        Fx.alpha = 1F;
        if (hit < 12) {
            float g = 1F - hit / 12F;
            vc = Fx.buf(buf, Fx.FLARE);
            Fx.billboard(vc, pose, cam, 0, 1.2, 0, 12F * g, t * 2F, k(new float[]{1F, 0.5F, 0.85F}, 1.3F * g));
        }
        pose.popPose();
    }
}
