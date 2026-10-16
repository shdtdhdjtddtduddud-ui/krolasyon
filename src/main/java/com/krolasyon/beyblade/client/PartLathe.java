package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.compat.VClient;
import com.krolasyon.beyblade.parts.BeyPart;
import com.krolasyon.beyblade.parts.Loadout;
import com.krolasyon.beyblade.parts.PartType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Spin tracks and performance tips drawn from their shapes (lathe profiles), so any track / tip can sit under any
 * wheel. Model units as in {@link BeyMesh}: wheel radius 1, y = 0 at the tip point.
 */
public final class PartLathe {
    private PartLathe() {}

    private static final int SEG = 36;
    /** a 14.5 mm track is half a wheel radius tall */
    private static final float PER_TENTH_MM = 0.5F / 145F;

    /** a profile point: radius, height, and whether it takes the accent colour */
    private record P(float r, float y, boolean accent) {}

    public static float tipTop(BeyPart.Tip t) {
        return switch (t) {
            case FINAL_DRIVE, FINAL_SURVIVE -> 0.42F;
            case WIDE_DEFENSE, ETERNAL_WIDE_DEFENSE, WIDE_BALL -> 0.38F;
            default -> 0.36F;
        };
    }

    public static float trackHeight(BeyPart track) { return track == null ? 0F : track.height() * PER_TENTH_MM; }

    /** where the wheel's underside sits with these parts fitted */
    public static float wheelBottom(Loadout lo) {
        BeyPart tip = lo.get(PartType.TIP);
        return (tip == null ? 0.05F : tipTop(tip.tip())) + trackHeight(lo.get(PartType.TRACK));
    }

    private static List<P> tipProfile(BeyPart.Tip t, float top) {
        List<P> p = new ArrayList<>();
        p.add(new P(0.26F, top, false));
        switch (t) {
            case FLAT, METAL_FLAT, EXTREME_FLAT -> {
                float c = t.contact;
                p.add(new P(0.31F, top - 0.08F, false));
                p.add(new P(0.31F, 0.12F, false));
                p.add(new P(c + 0.03F, 0.03F, true));
                p.add(new P(c, 0F, true));
            }
            case RUBBER_FLAT, RUBBER_SEMI_FLAT -> {
                p.add(new P(0.31F, top - 0.08F, false));
                p.add(new P(0.31F, 0.16F, false));
                p.add(new P(t.contact + 0.06F, 0.12F, true));
                p.add(new P(t.contact + 0.04F, 0.02F, true));
                p.add(new P(t.contact, 0F, true));
            }
            case SEMI_FLAT -> {
                p.add(new P(0.30F, top - 0.08F, false));
                p.add(new P(0.28F, 0.14F, false));
                p.add(new P(0.16F, 0.03F, true));
                p.add(new P(t.contact, 0F, true));
            }
            case SHARP, ETERNAL_SHARP, COAT_SHARP -> {
                p.add(new P(0.30F, top - 0.08F, false));
                p.add(new P(0.27F, 0.17F, false));
                p.add(new P(0.12F, 0.06F, true));
                p.add(new P(0.03F, 0.008F, true));
            }
            case DEFENSE -> {
                p.add(new P(0.34F, top - 0.08F, false));
                p.add(new P(0.34F, 0.18F, false));
                p.add(new P(0.22F, 0.06F, true));
                p.add(new P(0.06F, 0.005F, true));
            }
            case WIDE_DEFENSE, ETERNAL_WIDE_DEFENSE -> {
                p.add(new P(0.42F, top - 0.08F, false));
                p.add(new P(t == BeyPart.Tip.ETERNAL_WIDE_DEFENSE ? 0.46F : 0.44F, top - 0.16F, false));
                p.add(new P(0.32F, 0.1F, true));
                p.add(new P(0.1F, 0.01F, true));
            }
            case BALL, WIDE_BALL -> {
                float w = t == BeyPart.Tip.WIDE_BALL ? 0.4F : 0.31F;
                p.add(new P(w, top - 0.1F, false));
                for (int i = 1; i <= 4; i++) {
                    double a = i / 4.0 * Math.PI / 2;
                    float y = (float) ((top - 0.1F) * Math.cos(a));
                    p.add(new P((float) (w * Math.cos(a * 0.85)) * 0.95F, y, i >= 2));
                }
            }
            case FINAL_DRIVE, FINAL_SURVIVE -> {
                // 4D bottom: a wide hexagonal shell with the tip inside
                p.add(new P(0.36F, top - 0.02F, false));
                p.add(new P(0.38F, top - 0.12F, false));
                p.add(new P(0.36F, top - 0.2F, false));
                p.add(new P(0.22F, 0.1F, true));
                p.add(new P(t == BeyPart.Tip.FINAL_DRIVE ? 0.16F : 0.08F, 0.01F, true));
            }
            default -> {
                p.add(new P(0.3F, top - 0.08F, false));
                p.add(new P(0.1F, 0.02F, true));
            }
        }
        p.add(new P(0F, 0F, true));
        return p;
    }

    private static List<P> trackProfile(BeyPart track, float bottom, float top) {
        List<P> p = new ArrayList<>();
        p.add(new P(0.30F, top, false));
        p.add(new P(0.30F, top - 0.04F, false));
        p.add(new P(0.21F, top - 0.08F, false));
        float h = top - bottom;
        // the track's own mechanism, as a ring at some height of the shaft
        float fr = 0, fy = 0, ft = 0.04F;
        switch (track.trait()) {
            case BOOST_DISK -> { fr = 0.52F; fy = 0.35F; ft = 0.06F; }
            case CLAW -> { fr = 0.42F; fy = 0.6F; }
            case WING -> { fr = 0.46F; fy = 0.45F; ft = 0.025F; }
            case UPPER_WING -> { fr = 0.46F; fy = 0.75F; ft = 0.03F; }
            case TORNADO -> { fr = 0.36F; fy = 0.5F; ft = 0.06F; }
            case DEFENSE_RING -> { fr = 0.44F; fy = 0.22F; ft = 0.05F; }
            case RUBBER_RING -> { fr = 0.34F; fy = 0.5F; ft = 0.08F; }
            case TWIN_HEIGHT -> { fr = 0.36F; fy = 0.65F; ft = 0.04F; }
            case GRAVITY -> { fr = 0.3F; fy = 0.5F; ft = 0.2F; }
            default -> {}
        }
        if (fr > 0 && h > 0.2F) {
            float yc = bottom + h * fy;
            p.add(new P(0.2F, yc + ft / 2 + 0.03F, false));
            p.add(new P(fr, yc + ft / 2, true));
            p.add(new P(fr, yc - ft / 2, true));
            p.add(new P(0.2F, yc - ft / 2 - 0.03F, false));
        }
        p.add(new P(0.19F, bottom + 0.04F, false));
        p.add(new P(0.26F, bottom, false));
        return p;
    }

    public static void render(PoseStack pose, VertexConsumer vc, int light, int overlay, Loadout lo) {
        BeyPart tip = lo.get(PartType.TIP), track = lo.get(PartType.TRACK);
        float bottom = tip == null ? 0.05F : tipTop(tip.tip());
        if (track != null) lathe(pose, vc, light, overlay, trackProfile(track, bottom, bottom + trackHeight(track)), track.color(), track.color2());
        if (tip != null) lathe(pose, vc, light, overlay, tipProfile(tip.tip(), bottom), tip.color(), tip.color2());
    }

    private static void lathe(PoseStack pose, VertexConsumer vc, int light, int overlay, List<P> prof, int col, int accent) {
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        for (int k = 0; k < prof.size() - 1; k++) {
            P a = prof.get(k), b = prof.get(k + 1);
            int c = b.accent ? accent : col;
            float cr = ((c >> 16) & 255) / 255F, cg = ((c >> 8) & 255) / 255F, cb = (c & 255) / 255F;
            float dr = b.r - a.r, dy = b.y - a.y;
            for (int i = 0; i < SEG; i++) {
                double t0 = 2 * Math.PI * i / SEG, t1 = 2 * Math.PI * (i + 1) / SEG;
                float[][] q = {{a.r, a.y, (float) t0, 0}, {b.r, b.y, (float) t0, 1}, {b.r, b.y, (float) t1, 1}, {a.r, a.y, (float) t1, 0}};
                for (float[] v : q) {
                    float cos = (float) Math.cos(v[2]), sin = (float) Math.sin(v[2]);
                    // outward normal of the cone segment
                    float nx = -dy * cos, ny = dr, nz = -dy * sin;
                    float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                    if (len < 1.0E-5F) { nx = 0; ny = -1; nz = 0; len = 1; }
                    float shade = 0.85F + 0.15F * (float) Math.sin(v[2] * 4);
                    VClient.vertex(vc, m, n, v[0] * cos, v[1], v[0] * sin, cr * shade, cg * shade, cb * shade, 1F,
                            v[2] / (float) (2 * Math.PI), v[3], overlay, light, nx / len, ny / len, nz / len);
                }
            }
        }
    }
}
