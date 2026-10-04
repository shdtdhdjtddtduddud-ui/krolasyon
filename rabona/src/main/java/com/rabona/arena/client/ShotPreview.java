package com.rabona.arena.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.rabona.arena.entity.BallEntity;
import com.rabona.arena.game.Move;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Serbest vurus / penalti nisan cizgisi (FIFA gibi): secili sut tipinin sunucudaki parametreleriyle
 * ayni fizik (yercekimi, hava direnci, Magnus) simule edilip noktali yorunge olarak cizilir.
 */
public final class ShotPreview {
    private ShotPreview() {}

    public static boolean active() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && ClientState.match != null && ClientState.match.phase() == 6
                && ClientState.match.spTaker() == mc.player.getId();
    }

    /** {hiz, minKalkis, kavisDerece, extraY, sabitVy(<=-9 yok), falsoTuru(0 yok,1 top,2 yan), falsoMiktari} */
    static double[] params(Move m, float p, int side, double dist) {
        return switch (m) {
            case SHOT_FINESSE -> new double[]{0.95 + 0.5 * p, 0.02, -9 * side, 0, -9, 2, 0.5 * side};
            case SHOT_CHIP -> new double[]{0.55 + 0.3 * p, 0.42, 0, 0, 0.45 + 0.12 * p, 1, -0.25};
            case SHOT_TRIVELA -> new double[]{1.2 + 0.55 * p, 0.0, 15 * side, 0, -9, 2, -0.78 * side};
            case SHOT_KNUCKLE -> new double[]{1.35 + 0.45 * p, 0.05, 0, 0, -9, 0, 0};
            case SHOT_RABONA -> new double[]{1.1 + 0.55 * p, 0.0, -5 * side, 0, -9, 2, 0.3 * side};
            case SHOT_PANENKA -> new double[]{0.5, 0.3, 0, 0, 0.36, 1, -0.2};
            case SHOT_TOEPOKE -> new double[]{1.2, -0.02, 0, 0, -9, 0, 0};
            case SHOT_DEADLEAF -> new double[]{1.2 + 0.4 * p, 0.12, 0, Math.min(3.2, 0.9 + dist * 0.09), -9, 1, 0.62};
            default -> new double[]{1.05 + 0.85 * p, -0.05, 0, 0, -9, 1, 0.12};
        };
    }

    public static List<Vec3> simulate(Move m, float power, int side, Vec3 from, Vec3 look) {
        int sd = side == 0 ? 1 : side;
        float p = Mth.clamp(power, 0, 1);
        double[] k = params(m, p, sd, 20);
        double speed = k[0];
        Vec3 aim = from.add(look.scale(25));
        Vec3 d = aim.add(0, k[3], 0).subtract(from);
        double hd = Math.sqrt(d.x * d.x + d.z * d.z);
        Vec3 dir = new Vec3(d.x, 0, d.z).normalize().yRot((float) Math.toRadians(k[2]));
        double tt = Math.max(3, hd / speed * 1.12);
        double over = Math.max(0, power - 1.0) * 30;
        double vy = (d.y + 0.5 * BallEntity.GRAVITY * tt * tt) / tt + over * 0.012;
        vy = Mth.clamp(vy, k[1], speed * 0.75 + 0.1);
        if (k[4] > -9) vy = Math.max(vy, k[4]);
        Vec3 v = dir.scale(speed).add(0, vy, 0);
        Vector3f w = new Vector3f();
        if (k[5] == 1) {
            Vec3 ax = new Vec3(0, 1, 0).cross(dir);
            w.set((float) (ax.x * k[6]), (float) (ax.y * k[6]), (float) (ax.z * k[6]));
        } else if (k[5] == 2) {
            w.set(0, (float) k[6], 0);
        }
        List<Vec3> pts = new ArrayList<>();
        Vec3 pos = from;
        double floor = from.y - BallEntity.RADIUS;
        for (int i = 0; i < 48; i++) {
            v = v.add(0, -BallEntity.GRAVITY, 0);
            Vec3 wv = new Vec3(w.x, w.y, w.z);
            v = v.add(wv.cross(v).scale(BallEntity.MAGNUS));
            double sp = v.length();
            v = v.scale(Math.max(0.9, 1 - BallEntity.AIR_DRAG * sp - 0.003));
            w.mul(0.988f);
            pos = pos.add(v);
            if (pos.y < floor + BallEntity.RADIUS) break;
            pts.add(pos);
        }
        return pts;
    }

    public static void render(PoseStack ps, float partial) {
        if (!active()) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        BallEntity ball = null;
        for (BallEntity b : mc.level.getEntitiesOfClass(BallEntity.class, p.getBoundingBox().inflate(4))) ball = b;
        if (ball == null) return;
        int slot = ClientInput.charging ? ClientInput.chargingSlot : 0;
        float power = ClientInput.charging ? ClientInput.charge : 0.75f;
        Move m = ClientState.shotSlots[slot];
        List<Vec3> pts = simulate(m, power, ClientInput.side(p), ball.position().add(0, BallEntity.RADIUS, 0), p.getViewVector(partial));
        Camera cam = mc.gameRenderer.getMainCamera();
        Vec3 c = cam.getPosition();
        Vector3f left = cam.getLeftVector(), up = cam.getUpVector();
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buf.getBuffer(RenderType.debugQuads());
        Matrix4f mat = ps.last().pose();
        for (int i = 1; i < pts.size(); i += 2) {
            Vec3 q = pts.get(i).subtract(c);
            float sz = 0.11f;
            int a = (int) (200 * (1 - (float) i / pts.size())) + 55;
            float x = (float) q.x, y = (float) q.y, z = (float) q.z;
            float[][] k = {{1, 1}, {1, -1}, {-1, -1}, {-1, 1}};
            // iki yuzlu cizim (kamera yonunden bagimsiz gorunsun)
            for (int r = 0; r < 2; r++) {
                for (int j = 0; j < 4; j++) {
                    float[] o = k[r == 0 ? j : 3 - j];
                    vtx(vc, mat, x + (left.x() * o[0] + up.x() * o[1]) * sz, y + (left.y() * o[0] + up.y() * o[1]) * sz,
                            z + (left.z() * o[0] + up.z() * o[1]) * sz, a);
                }
            }
        }
        buf.endBatch();
    }

    private static void vtx(VertexConsumer vc, Matrix4f m, float x, float y, float z, int a) {
        vc.vertex(m, x, y, z).color(255, 240, 120, a).endVertex();
    }
}
