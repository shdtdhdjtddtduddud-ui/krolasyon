package com.krolasyon.aethercore.client;

import com.krolasyon.aethercore.AetherCore;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Procedurally built, animated model of the Aether Core: a plasma sphere in a glass shell, a rotating golden
 * equator band, pole caps, five curved armour plates that unfold and orbit when active, floating hexagon
 * panels and (for placed cores) the hexagonal shield dome.
 */
public final class CoreModel {
    private CoreModel() {}

    public static final int FULL_BRIGHT = 0xF000F0;

    private static final ResourceLocation SHELL = tex("aether_shell");
    private static final ResourceLocation SHELL_GLOW = tex("aether_shell_glow");
    private static final ResourceLocation BAND = tex("aether_band");
    private static final ResourceLocation BAND_GLOW = tex("aether_band_glow");
    private static final ResourceLocation ENERGY = tex("aether_core_energy");
    private static final ResourceLocation GEM = tex("aether_gem");
    private static final ResourceLocation HEX = tex("aether_hex");
    private static final ResourceLocation DOME = tex("aether_dome");

    private static ResourceLocation tex(String name) {
        return new ResourceLocation(AetherCore.MODID, "block/" + name);
    }

    private static TextureAtlasSprite sprite(ResourceLocation rl) {
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(rl);
    }

    /** Plate layout: yaw, pitch, roll (degrees), half width, half height (radians), has gem. */
    private static final float[][] PLATES = {
            {0, 4, 0, 0.42F, 0.58F, 1},
            {125, 38, 28, 0.80F, 0.24F, 0},
            {-98, -2, 8, 0.32F, 0.72F, 1},
            {150, -36, -24, 0.80F, 0.24F, 0},
            {215, 6, -6, 0.36F, 0.50F, 1},
    };

    private static float smooth(float t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }

    /**
     * Renders the core centred at the current pose origin.
     *
     * @param time  animation time in ticks
     * @param open  0 = dormant (plates closed), 1 = active (plates unfolded)
     * @param flash shield impact flash 0..1
     */
    public static void render(PoseStack pose, MultiBufferSource buffers, float time, float open, float flash, int light, int overlay) {
        // Tampon kaynakları farklı render türleri arasında geçişte önceki toplu çizimi kapatır;
        // bu yüzden model üç ayrı geçişte çizilir: opak, parlayan, yarı saydam.
        RenderType[] types = {RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS), RenderType.eyes(InventoryMenu.BLOCK_ATLAS),
                RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS)};
        for (int pass = 0; pass < 3; pass++) {
            renderPass(pass, buffers.getBuffer(types[pass]), pose, time, open, flash, light, overlay);
        }
    }

    private static final int CUT = 0, GLOW = 1, TRANS = 2;

    private static void renderPass(int pass, VertexConsumer vc, PoseStack pose, float time, float open, float flash, int light, int overlay) {
        float o = smooth(open);
        pose.pushPose();
        float bob = (float) Math.sin(time * 0.05) * (0.03F + 0.04F * o);
        pose.translate(0, bob, 0);

        TextureAtlasSprite shell = sprite(SHELL), shellGlow = sprite(SHELL_GLOW), band = sprite(BAND), bandGlow = sprite(BAND_GLOW);
        TextureAtlasSprite energy = sprite(ENERGY), gem = sprite(GEM), hex = sprite(HEX);

        float pulse = 0.85F + 0.15F * (float) Math.sin(time * 0.18);
        float glowI = Math.min(1, (0.35F + 0.65F * o) * pulse + flash * 0.5F);
        float rc = 0.24F + 0.03F * o + 0.008F * (float) Math.sin(time * 0.2);

        // ---------------- plazma çekirdek + cam kabuk ----------------
        if (pass == TRANS) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(time * 2.2F));
            pose.mulPose(Axis.XP.rotationDegrees(18));
            sphere(vc, pose, rc, 12, 20, energy, 0.75F + 0.25F * glowI, 0.9F + 0.1F * glowI, 1, 0.95F, FULL_BRIGHT, overlay, false);
            pose.popPose();
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(-time * 3.1F));
            pose.mulPose(Axis.ZP.rotationDegrees(25));
            sphere(vc, pose, rc * 1.12F, 12, 20, energy, 0.6F, 0.85F, 1, 0.35F + 0.25F * glowI, FULL_BRIGHT, overlay, false);
            pose.popPose();
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(time * 0.4F));
            sphere(vc, pose, rc * 1.32F, 10, 18, hex, 0.7F, 0.9F, 1, 0.18F + 0.12F * o, FULL_BRIGHT, overlay, true);
            pose.popPose();
        }

        // ---------------- ekvator bandı ve kutup kapakları ----------------
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-time * 1.4F));
        float rb = rc * 1.36F;
        if (pass == CUT) ring(vc, pose, rb, rb + 0.035F, 0.055F, 36, band, light, overlay);
        if (pass == GLOW) ringGlow(vc, pose, rb + 0.0365F, 0.055F, 36, bandGlow, glowI);
        pose.popPose();
        if (pass != TRANS) {
            for (int s = -1; s <= 1; s += 2) {
                pose.pushPose();
                pose.mulPose(Axis.YP.rotationDegrees(time * 1.0F * s));
                pose.mulPose(Axis.XP.rotationDegrees(s * -90));
                float capR = rc * 1.28F + 0.02F * (1 - o);
                plate(pass, vc, pose, capR, 0.04F, 0.36F, 0.36F, 0, shell, shellGlow, null, glowI, light, overlay);
                pose.popPose();
            }

            // ---------------- zırh plakaları ----------------
            float spin = time * (0.25F + 0.9F * o);
            for (int i = 0; i < PLATES.length; i++) {
                float[] pl = PLATES[i];
                pose.pushPose();
                float wob = (float) Math.sin(time * 0.07 + i * 1.7);
                pose.mulPose(Axis.YP.rotationDegrees(pl[0] + spin * (1 + 0.15F * (i % 2)) + wob * 4 * o));
                pose.mulPose(Axis.XP.rotationDegrees(-pl[1] - wob * 3 * o));
                pose.mulPose(Axis.ZP.rotationDegrees(pl[2]));
                float r = (0.36F + 0.03F * (i % 3)) * (1 - o) + (0.50F + 0.04F * (i % 2)) * o;
                // açıldığında plakalar dışarı doğru kayar ve hafif açılı durur
                pose.translate(0, 0, 0.13F * o + 0.02F * wob * o);
                pose.mulPose(Axis.XP.rotationDegrees(8 * o * (i % 2 == 0 ? 1 : -1)));
                plate(pass, vc, pose, r, 0.06F, pl[3], pl[4], pl[5] > 0 ? 1 : 0, shell, shellGlow, gem, glowI, light, overlay);
                pose.popPose();
            }
        }

        // ---------------- süzülen altıgen paneller ----------------
        if (pass == TRANS && o > 0.02F) {
            for (int i = 0; i < 7; i++) {
                pose.pushPose();
                float a = time * (0.9F + 0.17F * i) + i * 51.4F;
                pose.mulPose(Axis.YP.rotationDegrees(a));
                pose.translate(0, (float) Math.sin(time * 0.04 + i * 2.1) * 0.35F, 0.78F + 0.1F * (i % 3));
                float flick = 0.45F + 0.35F * (float) Math.sin(time * (0.21 + i * 0.03) + i);
                float sz = 0.09F + 0.03F * (i % 3);
                float al = o * Math.max(0.1F, flick) + flash * 0.4F;
                quad(vc, pose, sz, hex, 0.75F, 0.92F, 1, Math.min(1, al), FULL_BRIGHT, overlay);
                pose.popPose();
            }
        }
        pose.popPose();
    }

    /** Hexagonal shield dome around a placed core. {@code cam} is the camera position relative to the dome centre. */
    public static void renderDome(PoseStack pose, MultiBufferSource buffers, float radius, float time, float dome, float flash, Vector3f cam) {
        if (dome < 0.01F) return;
        float grow = smooth(Math.min(1, dome * 1.4F));
        float R = radius * (0.1F + 0.9F * grow);
        TextureAtlasSprite tile = sprite(DOME);
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS));
        Matrix4f m = pose.last().pose();
        Matrix3f nm = pose.last().normal();
        int lat = 20, lon = 40;
        float shimmer = (float) Math.sin(time * 0.05);
        for (int i = 0; i < lat; i++) {
            for (int j = 0; j < lon; j++) {
                float[][] pts = new float[4][];
                int[][] idx = {{i, j}, {i + 1, j}, {i + 1, j + 1}, {i, j + 1}};
                for (int k = 0; k < 4; k++) {
                    double th = Math.PI * idx[k][0] / lat, ph = Math.PI * 2 * idx[k][1] / lon + time * 0.0015;
                    float nx = (float) (Math.sin(th) * Math.cos(ph)), ny = (float) Math.cos(th), nz = (float) (Math.sin(th) * Math.sin(ph));
                    float px = nx * R, py = ny * R, pz = nz * R;
                    float vx = px - cam.x(), vy = py - cam.y(), vz = pz - cam.z();
                    float vl = (float) Math.sqrt(vx * vx + vy * vy + vz * vz) + 1e-4F;
                    float f = 1 - Math.abs((nx * vx + ny * vy + nz * vz) / vl);
                    float wave = 0.5F + 0.5F * (float) Math.sin(ny * 8 - time * 0.12 + shimmer);
                    float a = dome * (0.03F + 0.32F * f * f * f + 0.05F * wave) + flash * (0.25F + 0.2F * f);
                    pts[k] = new float[]{px, py, pz, nx, ny, nz, Math.min(0.85F, a)};
                }
                float[][] uv = {{0, 0}, {0, 1}, {1, 1}, {1, 0}};
                for (int k = 0; k < 4; k++) {
                    float[] p = pts[k];
                    vc.vertex(m, p[0], p[1], p[2]).color(0.55F + 0.45F * flash, 0.85F, 1.0F, p[6])
                            .uv(tile.getU0() + (tile.getU1() - tile.getU0()) * uv[k][0], tile.getV0() + (tile.getV1() - tile.getV0()) * uv[k][1])
                            .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(FULL_BRIGHT).normal(nm, p[3], p[4], p[5]).endVertex();
                }
            }
        }
    }

    // ===================================================================== geometri

    private static void vtx(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y, float z, float r, float g, float b, float a,
                            float u, float v, int light, int overlay, float nx, float ny, float nz) {
        vc.vertex(m, x, y, z).color(r, g, b, a).uv(u, v).overlayCoords(overlay).uv2(light).normal(n, nx, ny, nz).endVertex();
    }

    private static float su(TextureAtlasSprite s, float t) { return s.getU0() + (s.getU1() - s.getU0()) * t; }
    private static float sv(TextureAtlasSprite s, float t) { return s.getV0() + (s.getV1() - s.getV0()) * t; }

    /** UV sphere; {@code tile} repeats the sprite on every face (used for the hexagon glass). */
    private static void sphere(VertexConsumer vc, PoseStack pose, float r, int lat, int lon, TextureAtlasSprite s,
                               float cr, float cg, float cb, float a, int light, int overlay, boolean tile) {
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        for (int i = 0; i < lat; i++) {
            for (int j = 0; j < lon; j++) {
                int[][] idx = {{i, j}, {i + 1, j}, {i + 1, j + 1}, {i, j + 1}};
                float[][] uvt = {{0, 0}, {0, 1}, {1, 1}, {1, 0}};
                for (int k = 0; k < 4; k++) {
                    double th = Math.PI * idx[k][0] / lat, ph = Math.PI * 2 * idx[k][1] / lon;
                    float nx = (float) (Math.sin(th) * Math.cos(ph)), ny = (float) Math.cos(th), nz = (float) (Math.sin(th) * Math.sin(ph));
                    float u = tile ? uvt[k][0] : (idx[k][1] % (lon / 2)) / (float) (lon / 2);
                    float v = tile ? uvt[k][1] : idx[k][0] / (float) lat;
                    if (!tile && idx[k][1] % (lon / 2) == 0 && idx[k][1] != j) u = 1;
                    vtx(vc, m, n, nx * r, ny * r, nz * r, cr, cg, cb, a, su(s, u), sv(s, v), light, overlay, nx, ny, nz);
                }
            }
        }
    }

    /** Thick band (cylinder shell) around the Y axis. */
    private static void ring(VertexConsumer vc, PoseStack pose, float rIn, float rOut, float h, int seg, TextureAtlasSprite s, int light, int overlay) {
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        float y0 = -h / 2, y1 = h / 2;
        for (int i = 0; i < seg; i++) {
            double a0 = Math.PI * 2 * i / seg, a1 = Math.PI * 2 * (i + 1) / seg;
            float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
            float u0 = (i % 8) / 8F, u1 = (i % 8 + 1) / 8F;
            // dış
            vtx(vc, m, n, c0 * rOut, y0, s0 * rOut, 1, 1, 1, 1, su(s, u0), sv(s, 1), light, overlay, c0, 0, s0);
            vtx(vc, m, n, c0 * rOut, y1, s0 * rOut, 1, 1, 1, 1, su(s, u0), sv(s, 0), light, overlay, c0, 0, s0);
            vtx(vc, m, n, c1 * rOut, y1, s1 * rOut, 1, 1, 1, 1, su(s, u1), sv(s, 0), light, overlay, c1, 0, s1);
            vtx(vc, m, n, c1 * rOut, y0, s1 * rOut, 1, 1, 1, 1, su(s, u1), sv(s, 1), light, overlay, c1, 0, s1);
            // iç
            vtx(vc, m, n, c0 * rIn, y0, s0 * rIn, 0.6F, 0.6F, 0.65F, 1, su(s, u0), sv(s, 1), light, overlay, -c0, 0, -s0);
            vtx(vc, m, n, c1 * rIn, y0, s1 * rIn, 0.6F, 0.6F, 0.65F, 1, su(s, u1), sv(s, 1), light, overlay, -c1, 0, -s1);
            vtx(vc, m, n, c1 * rIn, y1, s1 * rIn, 0.6F, 0.6F, 0.65F, 1, su(s, u1), sv(s, 0), light, overlay, -c1, 0, -s1);
            vtx(vc, m, n, c0 * rIn, y1, s0 * rIn, 0.6F, 0.6F, 0.65F, 1, su(s, u0), sv(s, 0), light, overlay, -c0, 0, -s0);
            // üst / alt kenar
            for (int side = 0; side < 2; side++) {
                float y = side == 0 ? y1 : y0, ny = side == 0 ? 1 : -1;
                vtx(vc, m, n, c0 * rIn, y, s0 * rIn, 0.85F, 0.75F, 0.5F, 1, su(s, u0), sv(s, 0.05F), light, overlay, 0, ny, 0);
                vtx(vc, m, n, c0 * rOut, y, s0 * rOut, 0.85F, 0.75F, 0.5F, 1, su(s, u0), sv(s, 0.1F), light, overlay, 0, ny, 0);
                vtx(vc, m, n, c1 * rOut, y, s1 * rOut, 0.85F, 0.75F, 0.5F, 1, su(s, u1), sv(s, 0.1F), light, overlay, 0, ny, 0);
                vtx(vc, m, n, c1 * rIn, y, s1 * rIn, 0.85F, 0.75F, 0.5F, 1, su(s, u1), sv(s, 0.05F), light, overlay, 0, ny, 0);
            }
        }
    }

    private static void ringGlow(VertexConsumer vc, PoseStack pose, float r, float h, int seg, TextureAtlasSprite s, float intensity) {
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        float y0 = -h / 2, y1 = h / 2;
        for (int i = 0; i < seg; i++) {
            double a0 = Math.PI * 2 * i / seg, a1 = Math.PI * 2 * (i + 1) / seg;
            float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
            float u0 = (i % 8) / 8F, u1 = (i % 8 + 1) / 8F;
            vtx(vc, m, n, c0 * r, y0, s0 * r, intensity, intensity, intensity, 1, su(s, u0), sv(s, 1), FULL_BRIGHT, OverlayTexture.NO_OVERLAY, c0, 0, s0);
            vtx(vc, m, n, c0 * r, y1, s0 * r, intensity, intensity, intensity, 1, su(s, u0), sv(s, 0), FULL_BRIGHT, OverlayTexture.NO_OVERLAY, c0, 0, s0);
            vtx(vc, m, n, c1 * r, y1, s1 * r, intensity, intensity, intensity, 1, su(s, u1), sv(s, 0), FULL_BRIGHT, OverlayTexture.NO_OVERLAY, c1, 0, s1);
            vtx(vc, m, n, c1 * r, y0, s1 * r, intensity, intensity, intensity, 1, su(s, u1), sv(s, 1), FULL_BRIGHT, OverlayTexture.NO_OVERLAY, c1, 0, s1);
        }
    }

    /** Direction on the sphere for plate-local angles (around +Z). */
    private static Vector3f dir(float a, float b) {
        return new Vector3f((float) (Math.sin(a) * Math.cos(b)), (float) Math.sin(b), (float) (Math.cos(a) * Math.cos(b)));
    }

    /**
     * Curved armour plate: a thick spherical patch centred on +Z with tapered (crescent) ends, a glowing channel
     * overlay and an optional raised gem lens in the middle.
     */
    private static void plate(int pass, VertexConsumer vc, PoseStack pose, float r, float th, float hw, float hh, int gemFlag,
                              TextureAtlasSprite s, TextureAtlasSprite sg, TextureAtlasSprite gem, float glowI, int light, int overlay) {
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        int nu = 10, nv = 6;
        float[][][] outer = new float[nu + 1][nv + 1][];
        float[][][] inner = new float[nu + 1][nv + 1][];
        for (int i = 0; i <= nu; i++) {
            float tu = i / (float) nu;
            float a = -hw + 2 * hw * tu;
            float k = a / hw;
            float taper = 1 - 0.38F * k * k;
            for (int j = 0; j <= nv; j++) {
                float tv = j / (float) nv;
                float b = (-hh + 2 * hh * tv) * taper;
                Vector3f d = dir(a, b);
                float ro = r + th * (0.75F + 0.25F * taper);
                outer[i][j] = new float[]{d.x * ro, d.y * ro, d.z * ro, d.x, d.y, d.z, tu, tv};
                inner[i][j] = new float[]{d.x * r, d.y * r, d.z * r, -d.x, -d.y, -d.z, tu, tv};
            }
        }
        for (int i = 0; i < nu; i++) {
            for (int j = 0; j < nv; j++) {
                if (pass == CUT) {
                    emitQuad(vc, m, n, s, outer[i][j], outer[i + 1][j], outer[i + 1][j + 1], outer[i][j + 1], 1, 1, 1, 1, light, overlay, 0);
                    emitQuad(vc, m, n, s, inner[i][j], inner[i][j + 1], inner[i + 1][j + 1], inner[i + 1][j], 0.55F, 0.55F, 0.62F, 1, light, overlay, 0);
                } else emitQuad(vc, m, n, sg, outer[i][j], outer[i + 1][j], outer[i + 1][j + 1], outer[i][j + 1], glowI, glowI, glowI, 1,
                        FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0.0015F);
            }
        }
        // kenar duvarları (altın)
        for (int i = 0; i < nu && pass == CUT; i++) {
            edge(vc, m, n, s, outer[i][0], outer[i + 1][0], inner[i + 1][0], inner[i][0], light, overlay);
            edge(vc, m, n, s, outer[i + 1][nv], outer[i][nv], inner[i][nv], inner[i + 1][nv], light, overlay);
        }
        for (int j = 0; j < nv && pass == CUT; j++) {
            edge(vc, m, n, s, outer[0][j + 1], outer[0][j], inner[0][j], inner[0][j + 1], light, overlay);
            edge(vc, m, n, s, outer[nu][j], outer[nu][j + 1], inner[nu][j + 1], inner[nu][j], light, overlay);
        }
        // mücevher merceği
        if (gemFlag > 0 && gem != null) {
            int g = 6;
            float gw = Math.min(hw, hh) * 0.42F, gh = gw * 1.25F;
            float[][][] lens = new float[g + 1][g + 1][];
            for (int i = 0; i <= g; i++) {
                for (int j = 0; j <= g; j++) {
                    float x = i / (float) g * 2 - 1, y = j / (float) g * 2 - 1;
                    float bump = 0.035F * Math.max(0, 1 - (x * x + y * y) * 0.5F);
                    Vector3f d = dir(x * gw, y * gh);
                    float rr = r + th + 0.004F + bump;
                    lens[i][j] = new float[]{d.x * rr, d.y * rr, d.z * rr, d.x, d.y, d.z, i / (float) g, j / (float) g};
                }
            }
            float gi = Math.min(1.0F, 0.5F + glowI * 0.6F);
            for (int i = 0; i < g; i++) {
                for (int j = 0; j < g; j++) {
                    if (pass == CUT) emitQuad(vc, m, n, gem, lens[i][j], lens[i + 1][j], lens[i + 1][j + 1], lens[i][j + 1], 0.35F, 0.45F, 0.6F, 1, light, overlay, 0);
                    else emitQuad(vc, m, n, gem, lens[i][j], lens[i + 1][j], lens[i + 1][j + 1], lens[i][j + 1], gi, gi, gi, 1,
                            FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0.001F);
                }
            }
        }
    }

    private static void emitQuad(VertexConsumer vc, Matrix4f m, Matrix3f n, TextureAtlasSprite s, float[] a, float[] b, float[] c, float[] d,
                                 float cr, float cg, float cb, float ca, int light, int overlay, float push) {
        for (float[] p : new float[][]{a, b, c, d}) {
            vtx(vc, m, n, p[0] + p[3] * push, p[1] + p[4] * push, p[2] + p[5] * push, cr, cg, cb, ca,
                    su(s, p[6]), sv(s, p[7]), light, overlay, p[3], p[4], p[5]);
        }
    }

    private static void edge(VertexConsumer vc, Matrix4f m, Matrix3f n, TextureAtlasSprite s, float[] a, float[] b, float[] c, float[] d,
                             int light, int overlay) {
        Vector3f e1 = new Vector3f(b[0] - a[0], b[1] - a[1], b[2] - a[2]);
        Vector3f e2 = new Vector3f(d[0] - a[0], d[1] - a[1], d[2] - a[2]);
        Vector3f nn = e1.cross(e2);
        if (nn.lengthSquared() < 1e-12F) return;
        nn.normalize();
        // altın kenar: dokunun en dış (altın) şeridini kullan
        float u0 = 0.02F, u1 = 0.06F;
        float[][] uv = {{u0, 0.2F}, {u0, 0.8F}, {u1, 0.8F}, {u1, 0.2F}};
        float[][] p = {a, b, c, d};
        for (int k = 0; k < 4; k++) {
            vtx(vc, m, n, p[k][0], p[k][1], p[k][2], 1, 0.95F, 0.85F, 1, su(s, uv[k][0]), sv(s, uv[k][1]), light, overlay, nn.x, nn.y, nn.z);
        }
    }

    /** Camera-independent flat quad in the XY plane facing +Z (for the floating hexagons). */
    private static void quad(VertexConsumer vc, PoseStack pose, float size, TextureAtlasSprite s, float r, float g, float b, float a, int light, int overlay) {
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        vtx(vc, m, n, -size, -size, 0, r, g, b, a, s.getU0(), s.getV1(), light, overlay, 0, 0, 1);
        vtx(vc, m, n, size, -size, 0, r, g, b, a, s.getU1(), s.getV1(), light, overlay, 0, 0, 1);
        vtx(vc, m, n, size, size, 0, r, g, b, a, s.getU1(), s.getV0(), light, overlay, 0, 0, 1);
        vtx(vc, m, n, -size, size, 0, r, g, b, a, s.getU0(), s.getV0(), light, overlay, 0, 0, 1);
    }
}
