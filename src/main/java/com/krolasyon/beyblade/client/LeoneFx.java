package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.LibraBeyblade;
import com.krolasyon.beyblade.entity.LeoneEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import static com.krolasyon.beyblade.entity.LeoneEntity.*;

/** Rock Leone special-move effects: emerald wind and stone. Pose is at the beyblade, unrotated. */
public final class LeoneFx {
    private LeoneFx() {}

    public static final ResourceLocation LION = com.krolasyon.beyblade.compat.V.rl(LibraBeyblade.MODID, "textures/fx/lion.png");
    public static final ResourceLocation FANG = com.krolasyon.beyblade.compat.V.rl(LibraBeyblade.MODID, "textures/fx/fang.png");
    private static final ResourceLocation STONE = com.krolasyon.beyblade.compat.V.rl("minecraft", "textures/block/stone.png");
    private static final ResourceLocation ROCK = com.krolasyon.beyblade.compat.V.rl("minecraft", "textures/block/cobblestone.png");

    private static final float[] WIND = {0.62F, 1F, 0.72F};
    private static final float[] EMERALD = {0.2F, 1F, 0.45F};
    private static final float[] EARTH = {0.95F, 0.78F, 0.45F};
    private static final float[] WHITE = {1F, 1F, 1F};
    private static final float[] BLACK = {0F, 0F, 0F};

    private static float[] k(float[] c, float k) { return new float[]{c[0] * k, c[1] * k, c[2] * k}; }

    private static float easeOut(float x) {
        x = Mth.clamp(x, 0F, 1F);
        return 1F - (1F - x) * (1F - x) * (1F - x);
    }

    /** the lion emblem: solid emerald silhouette (keeps its colour on bright sky) + additive glow */
    private static void emblem(MultiBufferSource buf, PoseStack pose, Quaternionf cam, double x, double y, double z, float size, float strength) {
        float a = Mth.clamp(strength, 0F, 1F);
        if (a <= 0.01F || size <= 0.01F) return;
        VertexConsumer vc = Fx.bufT(buf, LION, 0.9F * a);
        Fx.billboard(vc, pose, cam, x, y, z, size, 0F, new float[]{0.15F, 0.85F, 0.3F});
        Fx.alpha = 1F;
        vc = Fx.buf(buf, LION);
        Fx.billboard(vc, pose, cam, x, y, z, size * 1.04F, 0F, k(new float[]{0.6F, 1F, 0.3F}, 0.4F * a));
    }

    public static void render(LeoneEntity e, float partial, float t, double ex, double ey, double ez, PoseStack pose,
                              MultiBufferSource buf, Quaternionf cam, int light) {
        if (e.isShielded()) windWall(e, t, pose, buf, cam);
        float qa = t - e.quakeTime;
        if (qa >= 0 && qa < (Q_RINGS - 1) * Q_RING_GAP + Q_RING_LIFE + 18) quake(qa, t, pose, buf, light);
        float ba = t - e.cageBlastTime;
        if (ba >= 0 && ba < 26) cageBlast(e, ba, t, ex, ey, ez, pose, buf, cam);
        int ab = e.clientAbility;
        float at = e.abilityAge(partial);
        if (ab == 0 || at < 0) return;
        switch (ab) {
            case LE_FANG -> fangs(e, at, t, partial, ex, ey, ez, pose, buf, cam);
            case LE_POUNCE -> pounce(e, at, t, pose, buf, cam);
            case LE_CAGE -> cage(e, at, t, partial, ex, ey, ez, pose, buf, cam);
            case LE_GIANT -> giant(e, at, t, partial, ex, ey, ez, pose, buf, cam);
            default -> {}
        }
    }

    // ================= LION GALE FORCE WALL =================
    private static void windWall(LeoneEntity e, float t, PoseStack pose, MultiBufferSource buf, Quaternionf cam) {
        int rem = e.shieldRemaining();
        float age = WALL_TICKS - rem;
        float grow = easeOut(age / 8F);
        float fade = rem < 14 ? rem / 14F : 1F;
        float hit = Math.max(0F, 1F - (t - e.wallHitTime) / 7F);
        float R = (float) WALL_RADIUS * grow;
        Matrix4f m = pose.last().pose();
        VertexConsumer vc = Fx.buf(buf, Fx.WISP);
        Fx.band(vc, m, R, 0F, R * 1.12F, 2.3F, 40, k(WIND, (0.55F + hit * 0.5F) * fade), BLACK, 3F, t * 0.22F, 1.5F, -t * 0.03F);
        Fx.band(vc, m, R * 0.86F, 0F, R * 0.95F, 1.9F, 40, k(WHITE, (0.35F + hit * 0.4F) * fade), BLACK, 4F, -t * 0.3F, 1F, t * 0.04F);
        for (int i = 0; i < 8; i++) {
            Fx.helix(vc, m, 0.05F, 2.2F, R * 1.02F, R * 1.1F, i * Mth.PI / 4F + t * 0.45F, 0.7F, 0.12F, 24,
                    k(WHITE, 0.8F * fade), k(WIND, 0.1F * fade), t * 0.2F);
        }
        vc = Fx.buf(buf, Fx.SOFT);
        Fx.ring(vc, m, 0.04F, R - 0.35F, R + 0.35F, 48, BLACK, k(EMERALD, (0.8F + hit) * fade), 3F, t * 0.05F);
        emblem(buf, pose, cam, 0, 2.6 + 0.1 * Mth.sin(t * 0.2F), 0, 1.1F * grow, 0.8F * fade);
    }

    // ================= LION 100 FANG FURY =================
    private static void fangs(LeoneEntity e, float at, float t, float partial, double ex, double ey, double ez, PoseStack pose,
                              MultiBufferSource buf, Quaternionf cam) {
        Entity tgt = e.level().getEntity(e.getTargetId());
        if (tgt == null) return;
        Vec3 to = new Vec3(Mth.lerp(partial, tgt.xo, tgt.getX()) - ex, Mth.lerp(partial, tgt.yo, tgt.getY()) + tgt.getBbHeight() * 0.5 - ey,
                Mth.lerp(partial, tgt.zo, tgt.getZ()) - ez);
        Vec3 from = new Vec3(0, 0.5, 0);
        Vec3 d = to.subtract(from);
        Vec3 side = new Vec3(-d.z, 0, d.x);
        side = side.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : side.normalize();
        Matrix4f m = pose.last().pose();
        VertexConsumer vc = Fx.buf(buf, Fx.FLARE);
        Fx.billboard(vc, pose, cam, 0, 0.5, 0, 1.6F + 0.3F * Mth.sin(t * 2F), t * 8F, k(EMERALD, 0.7F));
        for (int k = 0; k < FANG_COUNT; k++) {
            float age = at - (FANG_FIRST + k * FANG_STEP);
            if (age < 0 || age >= FANG_FLIGHT + 2) continue;
            float f = Mth.clamp(age / FANG_FLIGHT, 0F, 1F);
            float sgn = k % 2 == 0 ? 1F : -1F;
            float swing = (1.0F + (k % 3) * 0.4F) * Mth.sin(f * Mth.PI);
            Vec3 p = from.add(d.scale(f)).add(side.scale(sgn * swing)).add(0, 0.6 * Mth.sin(f * Mth.PI), 0);
            float f0 = Math.max(0F, f - 0.3F);
            float swing0 = (1.0F + (k % 3) * 0.4F) * Mth.sin(f0 * Mth.PI);
            Vec3 p0 = from.add(d.scale(f0)).add(side.scale(sgn * swing0)).add(0, 0.6 * Mth.sin(f0 * Mth.PI), 0);
            if (age < FANG_FLIGHT) {
                vc = Fx.buf(buf, Fx.SOFT);
                Fx.beam(vc, m, p0, p, 0.3F, BLACK, k(WIND, 0.9F), 1F, 0F);
                vc = Fx.buf(buf, FANG);
                Fx.billboard(vc, pose, cam, p.x, p.y, p.z, 1.1F, k * 47F + age * 30F, k(new float[]{0.7F, 1F, 0.8F}, 1.3F));
            } else {
                float g = 1F - (age - FANG_FLIGHT) / 2F;
                vc = Fx.buf(buf, Fx.FLARE);
                Fx.billboard(vc, pose, cam, to.x, to.y, to.z, 1.4F * g, k * 33F, k(WIND, g));
            }
        }
    }

    // ================= KING LION CHARGE (pounce) =================
    private static void pounce(LeoneEntity e, float at, float t, PoseStack pose, MultiBufferSource buf, Quaternionf cam) {
        Matrix4f m = pose.last().pose();
        if (at < P_LEAP) {
            float f = at / P_LEAP;
            VertexConsumer vc = Fx.buf(buf, Fx.WAVE);
            for (int i = 0; i < 3; i++) {
                float g = (at * 0.12F + i / 3F) % 1F;
                float r = 3.5F * (1F - g) + 0.3F;
                Fx.ring(vc, m, 0.05F, r - 0.4F, r, 48, k(WIND, g), BLACK, 4F, 0F);
            }
            emblem(buf, pose, cam, 0, 1.0, 0, 2.6F * easeOut(f), f);
            return;
        }
        float end = P_DASH + P_DASH_TICKS + 6;
        if (at < end) {
            float g = at > end - 6 ? (end - at) / 6F : 1F;
            emblem(buf, pose, cam, 0, 0.45, 0, 3.6F + 0.2F * Mth.sin(t * 1.5F), g);
            VertexConsumer vc = Fx.buf(buf, Fx.FLARE);
            Fx.billboard(vc, pose, cam, 0, 0.4, 0, 2.6F * g, t * 6F, k(EMERALD, 0.7F * g));
            if (at >= P_DASH) {
                Vec3 vel = new Vec3(e.getX() - e.xo, e.getY() - e.yo, e.getZ() - e.zo);
                if (vel.lengthSqr() > 1.0E-3) {
                    Vec3 tail = vel.normalize().scale(-6);
                    vc = Fx.buf(buf, Fx.SOFT);
                    Fx.beam(vc, m, new Vec3(0, 0.4, 0), tail, 1.8F * g, k(WHITE, 0.9F * g), BLACK, 0.5F, t * 0.6F);
                    Fx.beam(vc, m, new Vec3(0, 0.4, 0), tail.scale(0.8), 0.7F * g, k(WIND, 1.1F * g), BLACK, 0.8F, t);
                }
            }
        }
    }

    // ================= ROCK CRUSHING QUAKE =================
    private static void quake(float qa, float t, PoseStack pose, MultiBufferSource buf, int light) {
        Matrix4f m = pose.last().pose();
        VertexConsumer vc = Fx.buf(buf, Fx.WAVE);
        for (int i = 0; i < Q_RINGS; i++) {
            float age = qa - i * Q_RING_GAP;
            if (age < 0 || age >= Q_RING_LIFE) continue;
            float fade = 1F - age / Q_RING_LIFE;
            float front = 0.8F + age * Q_SPEED * (1 + i * 0.25F);
            Fx.ring(vc, m, 0.06F, front - 1.4F, front, 64, BLACK, k(EARTH, 1.3F * fade), 6F, i * 0.3F);
        }
        vc = Fx.bufT(buf, Fx.WISP, 0.7F);
        for (int i = 0; i < Q_RINGS; i++) {
            float age = qa - i * Q_RING_GAP;
            if (age < 0 || age >= Q_RING_LIFE) continue;
            float fade = 1F - age / Q_RING_LIFE;
            float front = 0.8F + age * Q_SPEED * (1 + i * 0.25F);
            Fx.alpha = 0.7F * fade;
            Fx.band(vc, m, front - 0.2F, 0F, front + 0.2F, 1.1F * fade, 48, new float[]{0.6F, 0.5F, 0.38F}, new float[]{0.8F, 0.72F, 0.6F}, 6F, t * 0.05F, 1F, -t * 0.1F);
        }
        Fx.alpha = 1F;
        // stone spikes bursting out of the ground as each ring passes
        VertexConsumer stone = buf.getBuffer(RenderType.entityCutoutNoCull(STONE));
        spikes(stone, pose, qa, light, false);
        VertexConsumer rock = buf.getBuffer(RenderType.entityCutoutNoCull(ROCK));
        spikes(rock, pose, qa, light, true);
    }

    private static void spikes(VertexConsumer vc, PoseStack pose, float qa, int light, boolean odd) {
        for (int i = 0; i < Q_RINGS; i++) {
            float speed = Q_SPEED * (1 + i * 0.25F);
            int n = 9 + i * 4;
            for (int j = 0; j < n; j++) {
                if ((j % 2 == 1) != odd) continue;
                float rho = 1.5F + (j % 3) * 1.3F + i * 0.7F;
                float age = qa - i * Q_RING_GAP - (rho - 0.8F) / speed;
                if (age < 0 || age >= 18) continue;
                float h;
                if (age < 3) h = age / 3F;
                else if (age < 11) h = 1F;
                else h = (18 - age) / 7F;
                float hmax = 0.7F + ((j * 7 + i * 3) % 5) * 0.22F;
                float a = j * Mth.TWO_PI / n + i * 0.37F;
                pose.pushPose();
                pose.translate(Mth.cos(a) * rho, -0.25F, Mth.sin(a) * rho);
                pose.mulPose(Axis.YP.rotationDegrees(-a * Mth.RAD_TO_DEG + 45F));
                pose.mulPose(Axis.ZP.rotationDegrees(12F + (j % 4) * 5F));
                spike(vc, pose, 0.32F + (j % 3) * 0.06F, hmax * h + 0.01F, light);
                pose.popPose();
            }
        }
    }

    /** four-sided rock spike (square base half-width w, apex at height h) */
    private static void spike(VertexConsumer vc, PoseStack pose, float w, float h, int light) {
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        float[][] base = {{-w, -w}, {w, -w}, {w, w}, {-w, w}};
        for (int i = 0; i < 4; i++) {
            float[] a = base[i], b = base[(i + 1) % 4];
            float mx = (a[0] + b[0]) / 2, mz = (a[1] + b[1]) / 2;
            float nl = (float) Math.sqrt(mx * mx + mz * mz + w * w * 0.25F);
            float nx = mx / nl, ny = w * 0.5F / nl, nz = mz / nl;
            sv(vc, m, n, a[0], 0, a[1], 0F, 1F, nx, ny, nz, light);
            sv(vc, m, n, b[0], 0, b[1], 1F, 1F, nx, ny, nz, light);
            sv(vc, m, n, 0, h, 0, 0.55F, 0F, nx, ny, nz, light);
            sv(vc, m, n, 0, h, 0, 0.45F, 0F, nx, ny, nz, light);
        }
    }

    private static void sv(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y, float z, float u, float v, float nx, float ny, float nz, int light) {
        com.krolasyon.beyblade.compat.VClient.vertex(vc, m, n, x, y, z, 1F, 1F, 1F, 1F, u, v, OverlayTexture.NO_OVERLAY, light, nx, ny, nz);
    }

    // ================= KING LION STORM CAGE =================
    private static void cage(LeoneEntity e, float at, float t, float partial, double ex, double ey, double ez, PoseStack pose,
                             MultiBufferSource buf, Quaternionf cam) {
        float form = easeOut(at / C_FORM);
        // after the blast the storm dies down slowly instead of vanishing
        float fade = at > C_BLAST ? Math.max(0F, 1F - (at - C_BLAST) / (float) C_FADE) : 1F;
        if (fade <= 0F) return;
        float H = 21F * form * (0.55F + 0.45F * fade);
        Vec3[] pil = new Vec3[4];
        for (int i = 0; i < 4; i++) pil[i] = pillar(e.cageX - ex, e.cageY - ey - 0.05, e.cageZ - ez, i, at);
        for (int i = 0; i < 4; i++) twister(pose, buf, pil[i], H, t, i, fade, 0.55F, 5.75F, 1F, 22, 2F);
        if (at >= C_FORM * 0.6F) {
            Matrix4f m = pose.last().pose();
            VertexConsumer vc;
            // storm swirling around the captive at the centre
            Vec3 c = new Vec3(e.cageX - ex, e.cageY - ey + C_HOLD_HEIGHT, e.cageZ - ez);
            vc = Fx.buf(buf, Fx.WAVE);
            for (int r = 0; r < 3; r++) {
                float tilt = r * 60F + t * 4F;
                Vec3 axis = new Vec3(Math.sin(Math.toRadians(tilt)), Math.cos(Math.toRadians(tilt)), Math.sin(Math.toRadians(tilt * 0.7)) * 0.5);
                Fx.ringAround(vc, m, c, axis, 1.0F, 1.35F, 40, k(GREY_LIGHT, 0.7F * fade), t * 0.05F);
            }
            emblem(buf, pose, cam, c.x, c.y + 3.2, c.z, 3.2F * form, 0.9F * fade);
        }
    }

    private static void giant(LeoneEntity e, float at, float t, float partial, double ex, double ey, double ez, PoseStack pose,
                             MultiBufferSource buf, Quaternionf cam) {
        // client storm age counts from when Leone reached the middle (EV_CAGE)
        if (e.giantTime < e.clientAbilityStart) {
            // still running to the centre: wind gathering around it
            VertexConsumer vc = Fx.buf(buf, Fx.WAVE);
            Fx.ring(vc, pose.last().pose(), 0.06F, 0.6F, 1.6F, 32, BLACK, k(GREY_LIGHT, 0.8F), 3F, -t * 0.1F);
            return;
        }
        float ca = t - e.giantTime;
        float form = easeOut(ca / G_FORM);
        // after the blast the storm dies down slowly instead of vanishing
        float fade = ca > G_END ? Math.max(0F, 1F - (ca - G_END) / (float) G_FADE) : 1F;
        if (fade <= 0F) return;
        float H = (float) G_TORNADO_H * form * (0.5F + 0.5F * fade);
        Vec3 base = new Vec3(e.cageX - ex, e.cageY - ey - 0.05, e.cageZ - ez);
        twister(pose, buf, base, H, t, 0, fade, 3.2F, (float) G_TOP_R, 4F, 44, 6F);
        if (ca >= G_FORM * 0.6F) {
            Matrix4f m = pose.last().pose();
            // wind rings around Leone in the eye, and the lion emblem high in the funnel
            Vec3 c = base.add(0, 1.2, 0);
            VertexConsumer vc = Fx.buf(buf, Fx.WAVE);
            for (int r = 0; r < 3; r++) {
                float tilt = r * 60F + t * 4F;
                Vec3 axis = new Vec3(Math.sin(Math.toRadians(tilt)), Math.cos(Math.toRadians(tilt)), Math.sin(Math.toRadians(tilt * 0.7)) * 0.5);
                Fx.ringAround(vc, m, c, axis, 1.0F, 1.35F, 40, k(GREY_LIGHT, 0.7F * fade), t * 0.05F);
            }
            emblem(buf, pose, cam, base.x, base.y + 14, base.z, 7F * form, 0.9F * fade);
        }
    }

    private static final float[] GREY_LIGHT = {0.78F, 0.8F, 0.82F};

    /**
     * One realistic grey tornado: a funnel narrow at the ground and wide at the top whose axis bends and sways over time,
     * built from stacked rings. Translucent (not additive) so it keeps its storm-grey colour against the sky. Two
     * counter-scrolling layers give the twisting look; dark debris bands spiral up the outside.
     */
    private static void twister(PoseStack pose, MultiBufferSource buf, Vec3 base, float H, float t, int seed, float fade,
                                float baseR, float topR, float swayK, int SEG, float tile) {
        if (H < 0.2F) return;
        int SL = 26;
        float[][] cx = new float[SL + 1][3];
        float[] rad = new float[SL + 1];
        for (int j = 0; j <= SL; j++) {
            float f = j / (float) SL, h = f * H;
            // bent, swaying axis: a slow lean plus a travelling S-curve
            float sway = Mth.sin(t * 0.11F + seed * 1.9F) * 1.1F * f * f * swayK;
            float wig = Mth.sin(h * 0.55F / swayK - t * 0.32F + seed) * 0.45F * f * swayK;
            float wig2 = Mth.cos(h * 0.42F / swayK - t * 0.27F + seed * 2.3F) * 0.45F * f * swayK;
            cx[j][0] = (float) base.x + sway + wig;
            cx[j][1] = (float) base.y + h;
            cx[j][2] = (float) base.z + sway * 0.6F + wig2;
            rad[j] = (baseR + (float) Math.pow(f, 1.5) * (topR - baseR) + 0.12F * baseR * Mth.sin(t * 0.7F + j)) * (0.6F + 0.4F * fade);
        }
        Matrix4f m = pose.last().pose();
        for (int layer = 0; layer < 3; layer++) {
            float rs = layer == 0 ? 1F : (layer == 1 ? 0.72F : 1.12F);
            float a = (layer == 0 ? 0.85F : (layer == 1 ? 0.95F : 0.4F)) * fade;
            float[] cb = layer == 1 ? new float[]{0.36F, 0.37F, 0.39F} : (layer == 0 ? new float[]{0.56F, 0.57F, 0.6F} : new float[]{0.75F, 0.76F, 0.78F});
            float[] ct = layer == 1 ? new float[]{0.5F, 0.51F, 0.54F} : new float[]{0.7F, 0.71F, 0.74F};
            VertexConsumer vc = Fx.bufT(buf, Fx.SMOKE, a);
            float spin = (layer == 1 ? -1.1F : 0.75F) * t * 0.09F;
            for (int j = 0; j < SL; j++) {
                float f0 = j / (float) SL, f1 = (j + 1) / (float) SL;
                float[] c0 = Fx.lerp(f0, cb, ct), c1 = Fx.lerp(f1, cb, ct);
                float tw0 = spin + f0 * 1.6F, tw1 = spin + f1 * 1.6F;
                for (int i = 0; i < SEG; i++) {
                    float a0 = i * Mth.TWO_PI / SEG, a1 = (i + 1) * Mth.TWO_PI / SEG;
                    float r0 = rad[j] * rs, r1 = rad[j + 1] * rs;
                    float[] p00 = {cx[j][0] + Mth.cos(a0) * r0, cx[j][1], cx[j][2] + Mth.sin(a0) * r0};
                    float[] p01 = {cx[j + 1][0] + Mth.cos(a0) * r1, cx[j + 1][1], cx[j + 1][2] + Mth.sin(a0) * r1};
                    float[] p11 = {cx[j + 1][0] + Mth.cos(a1) * r1, cx[j + 1][1], cx[j + 1][2] + Mth.sin(a1) * r1};
                    float[] p10 = {cx[j][0] + Mth.cos(a1) * r0, cx[j][1], cx[j][2] + Mth.sin(a1) * r0};
                    float u0 = i / (float) SEG * tile, u1 = (i + 1) / (float) SEG * tile;
                    Fx.v(vc, m, p00[0], p00[1], p00[2], c0[0], c0[1], c0[2], u0 + tw0, f0 * tile - t * 0.04F);
                    Fx.v(vc, m, p01[0], p01[1], p01[2], c1[0], c1[1], c1[2], u0 + tw1, f1 * tile - t * 0.04F);
                    Fx.v(vc, m, p11[0], p11[1], p11[2], c1[0], c1[1], c1[2], u1 + tw1, f1 * tile - t * 0.04F);
                    Fx.v(vc, m, p10[0], p10[1], p10[2], c0[0], c0[1], c0[2], u1 + tw0, f0 * tile - t * 0.04F);
                    Fx.v(vc, m, p10[0], p10[1], p10[2], c0[0], c0[1], c0[2], u1 + tw0, f0 * tile - t * 0.04F);
                    Fx.v(vc, m, p11[0], p11[1], p11[2], c1[0], c1[1], c1[2], u1 + tw1, f1 * tile - t * 0.04F);
                    Fx.v(vc, m, p01[0], p01[1], p01[2], c1[0], c1[1], c1[2], u0 + tw1, f1 * tile - t * 0.04F);
                    Fx.v(vc, m, p00[0], p00[1], p00[2], c0[0], c0[1], c0[2], u0 + tw0, f0 * tile - t * 0.04F);
                }
            }
            Fx.alpha = 1F;
        }
        // debris bands spiralling up the outside of the funnel
        VertexConsumer vc = Fx.bufT(buf, Fx.SMOKE, 0.7F * fade);
        for (int s = 0; s < 3; s++) {
            float ph = t * 0.35F + s * Mth.TWO_PI / 3F + seed;
            for (int j = 0; j < SL; j++) {
                float f0 = j / (float) SL, f1 = (j + 1) / (float) SL;
                float a0 = ph + f0 * 9F, a1 = ph + f1 * 9F;
                float r0 = rad[j] * 1.05F, r1 = rad[j + 1] * 1.05F;
                float w = (0.18F + f0 * 0.3F) * baseR * 0.6F;
                float x0 = cx[j][0] + Mth.cos(a0) * r0, z0 = cx[j][2] + Mth.sin(a0) * r0, x1 = cx[j + 1][0] + Mth.cos(a1) * r1, z1 = cx[j + 1][2] + Mth.sin(a1) * r1;
                float[] dc = {0.3F, 0.29F, 0.28F};
                Fx.quad(vc, m, new float[]{x0, cx[j][1] - w, z0}, new float[]{x0, cx[j][1] + w, z0}, new float[]{x1, cx[j + 1][1] + w, z1},
                        new float[]{x1, cx[j + 1][1] - w, z1}, f0 * 3F, 0F, f1 * 3F, 1F, dc, dc);
            }
        }
        Fx.alpha = 1F;
        // dust skirt where the funnel touches the ground
        pose.pushPose();
        pose.translate(cx[0][0], cx[0][1], cx[0][2]);
        Matrix4f pm = pose.last().pose();
        vc = Fx.bufT(buf, Fx.SMOKE, 0.5F * fade);
        Fx.band(vc, pm, baseR * 0.2F, 0F, baseR * 1.4F, baseR * 0.4F, 32, new float[]{0.5F, 0.47F, 0.42F}, new float[]{0.62F, 0.6F, 0.56F}, 2F, -t * 0.12F, 1F, 0F);
        Fx.alpha = 1F;
        pose.popPose();
    }

    private static void cageBlast(LeoneEntity e, float ba, float t, double ex, double ey, double ez, PoseStack pose, MultiBufferSource buf, Quaternionf cam) {
        float f = 1F - ba / 26F;
        Vec3 c = new Vec3(e.cageX - ex, e.cageY - ey, e.cageZ - ez);
        pose.pushPose();
        pose.translate(c.x, c.y, c.z);
        Matrix4f m = pose.last().pose();
        VertexConsumer vc = Fx.buf(buf, Fx.WAVE);
        float r = 0.5F + easeOut(ba / 12F) * 8F;
        Fx.ring(vc, m, 0.08F, r - 1.8F, r, 64, BLACK, k(GREY_LIGHT, 1.5F * f), 6F, 0F);
        vc = Fx.buf(buf, Fx.SOFT);
        Fx.sphere(vc, m, C_HOLD_HEIGHT_F, 0.5F + easeOut(ba / 10F) * 4.5F, Mth.PI, 12, 24, k(GREY_LIGHT, 0.5F * f * f), 2F, 1F, 0F, 0F);
        vc = Fx.buf(buf, Fx.FLARE);
        Fx.billboard(vc, pose, cam, 0, C_HOLD_HEIGHT, 0, 8F * f, t * 3F, k(new float[]{0.8F, 1F, 0.85F}, 1.1F * f));
        emblem(buf, pose, cam, 0, C_HOLD_HEIGHT + 2.5, 0, 6F * easeOut(ba / 6F), f);
        pose.popPose();
    }

    private static final float C_HOLD_HEIGHT_F = (float) C_HOLD_HEIGHT;
}
