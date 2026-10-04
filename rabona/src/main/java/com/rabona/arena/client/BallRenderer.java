package com.rabona.arena.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.rabona.arena.RabonaArena;
import com.rabona.arena.entity.BallEntity;
import com.rabona.arena.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** UV kure olarak cizilen top + parlak iz seridi. */
public class BallRenderer extends EntityRenderer<BallEntity> {
    private static final int LAT = 14, LON = 24;
    private static final float[][] VERTS; // [i] = {x,y,z,u,v}
    private static final ResourceLocation[] TEX = new ResourceLocation[ModItems.BALL_SKINS.length];
    private static final ResourceLocation[] GLOW = new ResourceLocation[ModItems.BALL_SKINS.length];
    private static final boolean[] HAS_GLOW = {false, false, true, true, true, true};

    static {
        for (int i = 0; i < TEX.length; i++) {
            TEX[i] = RabonaArena.id("textures/entity/ball/" + ModItems.BALL_SKINS[i] + ".png");
            GLOW[i] = RabonaArena.id("textures/entity/ball/" + ModItems.BALL_SKINS[i] + "_glow.png");
        }
        VERTS = new float[(LAT + 1) * (LON + 1)][];
        for (int la = 0; la <= LAT; la++) {
            double th = Math.PI * la / LAT; // 0 = kuzey kutbu
            for (int lo = 0; lo <= LON; lo++) {
                double ph = 2 * Math.PI * lo / LON;
                float x = (float) (Math.sin(th) * Math.cos(ph));
                float y = (float) Math.cos(th);
                float z = (float) (Math.sin(th) * Math.sin(ph));
                VERTS[la * (LON + 1) + lo] = new float[]{x, y, z, (float) lo / LON, (float) la / LAT};
            }
        }
    }

    public BallRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        shadowRadius = 0.28f;
        shadowStrength = 0.9f;
    }

    @Override
    public void render(BallEntity e, float yaw, float partial, PoseStack ps, MultiBufferSource buf, int light) {
        if (Replay.playing() && !Replay.isPuppet(e)) return;
        int skin = Mth.clamp(e.getSkin(), 0, TEX.length - 1);
        ps.pushPose();
        ps.translate(0, BallEntity.RADIUS, 0);
        Quaternionf q = new Quaternionf(e.rotO).slerp(e.rot, partial);
        ps.mulPose(q);
        float r = BallEntity.RADIUS;
        ps.scale(r, r, r);
        sphere(ps, buf.getBuffer(RenderType.entityCutoutNoCull(TEX[skin])), light, 255);
        if (HAS_GLOW[skin]) {
            ps.scale(1.01f, 1.01f, 1.01f);
            sphere(ps, buf.getBuffer(RenderType.eyes(GLOW[skin])), 0xF000F0, 255);
        }
        ps.popPose();
        trail(e, partial, ps, buf);
        super.render(e, yaw, partial, ps, buf, light);
    }

    private static void sphere(PoseStack ps, VertexConsumer vc, int light, int alpha) {
        Matrix4f m = ps.last().pose();
        Matrix3f n = ps.last().normal();
        for (int la = 0; la < LAT; la++) {
            for (int lo = 0; lo < LON; lo++) {
                float[] a = VERTS[la * (LON + 1) + lo];
                float[] b = VERTS[(la + 1) * (LON + 1) + lo];
                float[] c = VERTS[(la + 1) * (LON + 1) + lo + 1];
                float[] d = VERTS[la * (LON + 1) + lo + 1];
                v(vc, m, n, a, light, alpha);
                v(vc, m, n, b, light, alpha);
                v(vc, m, n, c, light, alpha);
                v(vc, m, n, d, light, alpha);
            }
        }
    }

    private static void v(VertexConsumer vc, Matrix4f m, Matrix3f n, float[] p, int light, int alpha) {
        vc.vertex(m, p[0], p[1], p[2]).color(255, 255, 255, alpha).uv(p[3], p[4]).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light).normal(n, p[0], p[1], p[2]).endVertex();
    }

    /** Hizli/ozel vuruslarda isik seridi. */
    private void trail(BallEntity e, float partial, PoseStack ps, MultiBufferSource buf) {
        int fx = e.getEffect();
        Vec3 vel = e.getDeltaMovement();
        if (fx == BallEntity.FX_NONE && vel.length() < 0.85) return;
        List<Vec3> pts = new ArrayList<>();
        Vec3 here = new Vec3(Mth.lerp(partial, e.xo, e.getX()), Mth.lerp(partial, e.yo, e.getY()) + BallEntity.RADIUS, Mth.lerp(partial, e.zo, e.getZ()));
        pts.add(here);
        Iterator<Vec3> it = e.trailIterator();
        int max = fx == BallEntity.FX_NONE || fx == BallEntity.FX_POWER ? 6 : 13;
        while (it.hasNext() && pts.size() < max) {
            Vec3 p = it.next();
            if (p.distanceToSqr(pts.get(pts.size() - 1)) > 64) break;
            pts.add(p);
        }
        if (pts.size() < 3) return;
        Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        VertexConsumer vc = buf.getBuffer(RenderType.lightning());
        Matrix4f m = ps.last().pose();
        float baseW = fx == BallEntity.FX_FIRE || fx == BallEntity.FX_TORNADO ? 0.3f : fx == BallEntity.FX_NONE ? 0.12f : 0.2f;
        int n = pts.size();
        for (int i = 0; i < n - 1; i++) {
            Vec3 a = pts.get(i), b = pts.get(i + 1);
            float fa = 1 - (float) i / (n - 1), fb = 1 - (float) (i + 1) / (n - 1);
            Vec3 dir = b.subtract(a);
            if (dir.lengthSqr() < 1e-6) continue;
            Vec3 view = a.subtract(cam);
            Vec3 side = dir.cross(view).normalize();
            Vec3 sa = side.scale(baseW * fa), sb = side.scale(baseW * fb);
            int[] ca = color(fx, i, e.tickCount), cb = color(fx, i + 1, e.tickCount);
            Vec3 ra = a.subtract(here).add(0, BallEntity.RADIUS, 0), rb = b.subtract(here).add(0, BallEntity.RADIUS, 0);
            int aa = (int) (200 * fa), ab = (int) (200 * fb);
            vtx(vc, m, ra.add(sa), ca, aa);
            vtx(vc, m, ra.subtract(sa), ca, aa);
            vtx(vc, m, rb.subtract(sb), cb, ab);
            vtx(vc, m, rb.add(sb), cb, ab);
        }
    }

    private static void vtx(VertexConsumer vc, Matrix4f m, Vec3 p, int[] c, int a) {
        vc.vertex(m, (float) p.x, (float) p.y, (float) p.z).color(c[0], c[1], c[2], a).endVertex();
    }

    private static int[] color(int fx, int i, int tick) {
        return switch (fx) {
            case BallEntity.FX_FIRE -> i < 3 ? new int[]{255, 230, 120} : new int[]{255, 90 - Math.min(80, i * 6), 10};
            case BallEntity.FX_THUNDER -> new int[]{140, 230, 255};
            case BallEntity.FX_TORNADO -> new int[]{220, 235, 240};
            case BallEntity.FX_GHOST -> new int[]{90, 255, 230};
            case BallEntity.FX_ICE -> new int[]{170, 220, 255};
            case BallEntity.FX_RAINBOW -> {
                float h = ((tick * 0.05f) + i * 0.08f) % 1f;
                int rgb = Mth.hsvToRgb(h, 0.8f, 1f);
                yield new int[]{(rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255};
            }
            case BallEntity.FX_CURL -> new int[]{120, 200, 255};
            case BallEntity.FX_TRIVELA -> i % 2 == 0 ? new int[]{255, 120, 60} : new int[]{255, 210, 90};
            case BallEntity.FX_LEAF -> new int[]{150 + i * 6, 210, 70};
            case BallEntity.FX_KNUCKLE -> new int[]{255, 255, 200};
            default -> new int[]{200, 200, 200};
        };
    }

    @Override
    public ResourceLocation getTextureLocation(BallEntity e) {
        return TEX[Mth.clamp(e.getSkin(), 0, TEX.length - 1)];
    }
}
