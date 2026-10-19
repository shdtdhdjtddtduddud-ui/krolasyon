package com.krolasyon.sylvanarchery.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * Orman Bekçisi Yayı. Yay XY düzleminde, kabza orijinde, ok +X yönüne atılır.
 * Köşeli taşlaşmış kollar, dikenli çıkıntılar, altın ve koyu sargılar, sallanan tüyler; çekildikçe kollar bükülür,
 * kiriş geri gelir ve bir ok yerleşir.
 */
public final class BowModel {
    private BowModel() {}

    private static final float[][] UPPER = {{0.10F, 0.17F}, {0.145F, 0.40F}, {0.095F, 0.62F}, {-0.005F, 0.82F}, {-0.105F, 0.97F}, {-0.15F, 1.05F}};

    public static void render(PoseStack ps, MultiBufferSource buf, float time, float pull, int light) {
        VertexConsumer vc = buf.getBuffer(Mesh.TYPES[Mesh.CUT]);
        TextureAtlasSprite limb = Mesh.sprite(Mesh.LIMB), gold = Mesh.sprite(Mesh.GOLD), wrap = Mesh.sprite(Mesh.WRAP);
        TextureAtlasSprite feather = Mesh.sprite(Mesh.FEATHER), string = Mesh.sprite(Mesh.STRING);
        float bend = pull * 0.30F;
        float[][] up = bent(UPPER, bend, 1), lo = bent(UPPER, bend, -1);

        // gövde (kabza)
        Mesh.beam(vc, ps, p(0.10F, -0.19F), p(0.10F, 0.19F), 0.05F, 0.055F, 0.05F, 0.055F, limb, 1, 1, 1, light);
        Mesh.beam(vc, ps, p(0.10F, -0.11F), p(0.10F, 0.11F), 0.058F, 0.063F, 0.058F, 0.063F, wrap, 1, 1, 1, light);
        for (int s = -1; s <= 1; s += 2) {
            Mesh.beam(vc, ps, p(0.10F, s * 0.11F), p(0.10F, s * 0.14F), 0.062F, 0.067F, 0.062F, 0.067F, gold, 1, 1, 1, light);
            // kabza yanındaki büyük pençe/dikenler
            Mesh.beam(vc, ps, p(0.13F, s * 0.20F), p(0.33F, s * 0.36F), 0.04F, 0.035F, 0.004F, 0.004F, limb, 0.95F, 0.95F, 1, light);
            Mesh.beam(vc, ps, p(0.14F, s * 0.14F), p(0.27F, s * 0.17F), 0.03F, 0.03F, 0.004F, 0.004F, limb, 0.9F, 0.9F, 0.95F, light);
        }
        for (int side = 0; side < 2; side++) {
            float[][] L = side == 0 ? up : lo;
            float sg = side == 0 ? 1 : -1;
            for (int i = 0; i < L.length - 1; i++) {
                float f0 = i / (float) (L.length - 1), f1 = (i + 1) / (float) (L.length - 1);
                Mesh.beam(vc, ps, p(L[i][0], L[i][1]), p(L[i + 1][0], L[i + 1][1]),
                        0.055F - 0.025F * f0, 0.05F - 0.028F * f0, 0.055F - 0.025F * f1, 0.05F - 0.028F * f1, limb, 1, 1, 1, light);
            }
            // açılı dikenler (karına doğru)
            spike(vc, ps, L[1], new float[]{1, 0.55F * sg}, 0.18F, limb, light);
            spike(vc, ps, L[2], new float[]{1, 0.05F * sg}, 0.12F, limb, light);
            spike(vc, ps, L[3], new float[]{0.55F, 1 * sg}, 0.10F, limb, light);
            // sargılar
            wrapAt(vc, ps, L[0], L[1], 0.15F, 0.45F, gold, 0.062F, 0.058F, light);
            wrapAt(vc, ps, L[2], L[3], 0.2F, 0.5F, gold, 0.052F, 0.048F, light);
            wrapAt(vc, ps, L[1], L[2], 0.35F, 0.6F, wrap, 0.058F, 0.052F, light);
            wrapAt(vc, ps, L[4], L[5], 0.0F, 1.0F, wrap, 0.04F, 0.034F, light);
            wrapAt(vc, ps, L[3], L[4], 0.6F, 0.9F, gold, 0.045F, 0.04F, light);
            // tüyler
            feather(vc, ps, L[2], L[3], sg, 0, time, feather, light);
            feather(vc, ps, L[3], L[4], sg, 1, time, feather, light);
            feather(vc, ps, L[1], L[2], sg, 2, time, feather, light);
        }
        // kiriş
        float[] tu = up[up.length - 1], tl = lo[lo.length - 1];
        float nockX = (tu[0] + tl[0]) * 0.5F - pull * 0.62F;
        float[] nock = p(nockX, 0);
        Mesh.beam(vc, ps, p(tu[0], tu[1]), nock, 0.006F, 0.006F, 0.006F, 0.006F, string, 1, 1, 1, light);
        Mesh.beam(vc, ps, nock, p(tl[0], tl[1]), 0.006F, 0.006F, 0.006F, 0.006F, string, 1, 1, 1, light);
        // yerleştirilmiş ok
        if (pull > 0.02F) {
            ps.pushPose();
            ps.translate(nockX + 0.5F, 0, 0.065F);
            ArrowModel.render(vc, ps, light, time, 0);
            ps.popPose();
        }
    }

    private static float[] p(float x, float y) {
        return new float[]{x, y, 0};
    }

    /** Limb points rotated progressively around the grip pivot (upper: sg = 1, lower: sg = -1). */
    private static float[][] bent(float[][] base, float bend, float sg) {
        float[][] out = new float[base.length][];
        float px = base[0][0], py = base[0][1];
        for (int i = 0; i < base.length; i++) {
            float f = i / (float) (base.length - 1);
            float a = bend * (0.4F + 0.6F * f);
            float dx = base[i][0] - px, dy = base[i][1] - py;
            float c = (float) Math.cos(a), s = (float) Math.sin(a);
            // üst kol saat yönünün tersine (okçuya doğru) döner; alt kol bunun aynası
            out[i] = new float[]{px + dx * c - dy * s, (py + dx * s + dy * c) * sg};
        }
        return out;
    }

    private static void spike(VertexConsumer vc, PoseStack ps, float[] at, float[] dir, float len, TextureAtlasSprite s, int light) {
        float l = (float) Math.sqrt(dir[0] * dir[0] + dir[1] * dir[1]);
        Mesh.beam(vc, ps, p(at[0], at[1]), p(at[0] + dir[0] / l * len, at[1] + dir[1] / l * len),
                0.035F, 0.03F, 0.004F, 0.004F, s, 0.92F, 0.92F, 0.97F, light);
    }

    private static void wrapAt(VertexConsumer vc, PoseStack ps, float[] a, float[] b, float t0, float t1, TextureAtlasSprite s,
                               float w, float t, int light) {
        float[] p0 = p(a[0] + (b[0] - a[0]) * t0, a[1] + (b[1] - a[1]) * t0);
        float[] p1 = p(a[0] + (b[0] - a[0]) * t1, a[1] + (b[1] - a[1]) * t1);
        Mesh.beam(vc, ps, p0, p1, w, t, w, t, s, 1, 1, 1, light);
    }

    /** Two overlapping feathers tied to a limb, pointing outward away from the belly and swaying. */
    private static void feather(VertexConsumer vc, PoseStack ps, float[] a, float[] b, float sg, int k, float time, TextureAtlasSprite s, int light) {
        float ax = (a[0] + b[0]) * 0.5F, ay = (a[1] + b[1]) * 0.5F;
        double base = Math.atan2(sg * 0.55, -1.0);
        for (int j = 0; j < 2; j++) {
            double ang = base + (j - 0.5) * 0.35 + Math.sin(time * 0.08 + k * 1.3 + j) * 0.12;
            float dx = (float) Math.cos(ang), dy = (float) Math.sin(ang);
            float len = 0.30F - 0.05F * j - 0.04F * k, w = 0.045F;
            float z = (j - 0.5F) * 0.03F;
            Mesh.face(vc, ps, new float[][]{
                    {ax, ay, z - w, 0, 1}, {ax, ay, z + w, 1, 1},
                    {ax + dx * len, ay + dy * len, z + w * 0.6F, 1, 0}, {ax + dx * len, ay + dy * len, z - w * 0.6F, 0, 0}}, s, 1, 1, 1, light);
        }
    }
}
