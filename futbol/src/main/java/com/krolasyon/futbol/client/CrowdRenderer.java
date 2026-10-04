package com.krolasyon.futbol.client;

import com.krolasyon.futbol.FutbolMod;
import com.krolasyon.futbol.block.SeatBlock;
import com.krolasyon.futbol.registry.ModBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/** Draws supporters on every stadium seat: team coloured shirts, idle swaying, Mexican waves and wild goal celebrations. */
public final class CrowdRenderer {
    private CrowdRenderer() {}

    private static final ResourceLocation TEX = new ResourceLocation(FutbolMod.MODID, "textures/entity/fan.png");
    private static final int[] SKIN = {0xF2CBA7, 0xE0AC83, 0xC68A5E, 0xA0663F, 0x7A4A2B, 0xEAC09A};
    private static final int[] HAIR = {0x1A1410, 0x4A2E1A, 0xD9B25A, 0x2A1A10, 0x8D4A1E, 0x101010};

    private record Fan(double x, double y, double z, Direction face, int team, int shirt, int skin, int hair, int scarf, float seed, float angle, int light) {}

    private static final List<Fan> FANS = new ArrayList<>();
    private static int cachedKey = Integer.MIN_VALUE;
    private static long nextScan;

    private static void scan(ClientLevel level) {
        FANS.clear();
        int px = ClientState.px, py = ClientState.py, pz = ClientState.pz;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -50; dx <= 50; dx++) {
            for (int dz = -36; dz <= 36; dz++) {
                if (Math.abs(dx) <= 36 && Math.abs(dz) <= 24) continue;
                for (int h = 1; h <= 8; h++) {
                    m.set(px + dx, py + h, pz + dz);
                    BlockState s = level.getBlockState(m);
                    Block b = s.getBlock();
                    if (!(b instanceof SeatBlock)) continue;
                    long hash = (long) (px + dx) * 341873128712L + (long) (pz + dz) * 132897987541L + h * 7919L;
                    java.util.Random r = new java.util.Random(hash);
                    if (r.nextFloat() > 0.72F) continue;
                    int team = b == ModBlocks.SEAT_RED.get() ? 1 : b == ModBlocks.SEAT_BLUE.get() ? 2 : r.nextInt(3);
                    int shirt = team == 1 ? shadeRnd(0xC62828, r) : team == 2 ? shadeRnd(0x1565C0, r) : r.nextBoolean() ? 0xEEEEEE : 0x37474F;
                    if (r.nextFloat() < 0.15F) shirt = 0xF5F5F5;
                    int scarf = r.nextFloat() < 0.45F ? (team == 1 ? 0xFFD54F : team == 2 ? 0xFFFFFF : 0x9E9E9E) : -1;
                    double cx = px + dx + 0.5, cz = pz + dz + 0.5;
                    float ang = (float) Math.atan2(cz - (pz + 0.5), cx - (px + 0.5));
                    int light = LevelRenderer.getLightColor(level, m.above());
                    FANS.add(new Fan(cx, py + h, cz, s.getValue(HorizontalDirectionalBlock.FACING), team, shirt, SKIN[r.nextInt(SKIN.length)],
                            HAIR[r.nextInt(HAIR.length)], scarf, r.nextFloat() * 100F, ang, light));
                }
            }
        }
    }

    private static int shadeRnd(int c, java.util.Random r) {
        float k = 0.8F + r.nextFloat() * 0.35F;
        int rr = Math.min(255, (int) (((c >> 16) & 255) * k)), gg = Math.min(255, (int) (((c >> 8) & 255) * k)), bb = Math.min(255, (int) ((c & 255) * k));
        return (rr << 16) | (gg << 8) | bb;
    }

    public static void render(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || !ClientState.hasPitch) return;
        Vec3 cam = e.getCamera().getPosition();
        double cdx = cam.x - ClientState.px, cdz = cam.z - ClientState.pz;
        if (cdx * cdx + cdz * cdz > 160 * 160) return;
        int key = ClientState.px * 31 + ClientState.pz * 17 + ClientState.py;
        long now = level.getGameTime();
        if (key != cachedKey || now >= nextScan) {
            cachedKey = key;
            nextScan = now + 200;
            scan(level);
        }
        if (FANS.isEmpty()) return;
        float time = now + e.getPartialTick();
        int goalTeam = ClientState.goalTicks > 0 ? ClientState.goalTeam : 0;
        boolean wave = ClientState.state == 2 && (now / 1800) % 2 == 1;
        float wavePos = (time * 0.035F) % ((float) Math.PI * 2F) - (float) Math.PI;

        PoseStack ps = e.getPoseStack();
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(TEX));
        Matrix4f mat = ps.last().pose();
        Matrix3f nrm = ps.last().normal();
        for (Fan f : FANS) {
            double rx = f.x - cam.x, ry = f.y - cam.y, rz = f.z - cam.z;
            if (rx * rx + rz * rz > 110 * 110) continue;
            float excite = 0;
            if (goalTeam != 0) excite = (f.team == goalTeam || f.team == 0) ? 1F : -1F;
            if (wave && excite == 0) {
                float d = Mth.wrapDegrees((f.angle - wavePos) * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
                if (Math.abs(d) < 0.35F) excite = 0.8F * (1 - Math.abs(d) / 0.35F) + 0.2F;
            }
            drawFan(vc, mat, nrm, f, rx, ry, rz, time, excite);
        }
        buf.endBatch(RenderType.entityCutoutNoCull(TEX));
    }

    private static void drawFan(VertexConsumer vc, Matrix4f m, Matrix3f n, Fan f, double rx, double ry, double rz, float t, float excite) {
        Vec3 fwd = new Vec3(f.face.getStepX(), 0, f.face.getStepZ());
        Vec3 right = new Vec3(-fwd.z, 0, fwd.x);
        float sway = Mth.sin(t * 0.05F + f.seed) * 0.03F;
        float lift = 0, armUp = 0, jump = 0;
        if (excite > 0) {
            lift = 0.32F * excite;
            jump = Math.abs(Mth.sin(t * 0.42F + f.seed)) * 0.22F * excite;
            armUp = excite;
        } else if (excite < 0) {
            armUp = -1; // hands on head
        }
        double by = 0.5 + lift + jump;
        // origin slightly back on the seat
        double ox = rx - fwd.x * 0.06, oz = rz - fwd.z * 0.06;
        Base b = new Base(ox, ry, oz, fwd, right, sway);
        int light = f.light;
        // legs
        if (lift > 0.1F) {
            box(vc, m, n, b, -0.2, by - 0.5, -0.08, 0.2, by, 0.12, 0x2B2F3A, light);
        } else {
            box(vc, m, n, b, -0.2, by, -0.05, 0.2, by + 0.17, 0.42, 0x2B2F3A, light);
        }
        // torso
        box(vc, m, n, b, -0.24, by, -0.13, 0.24, by + 0.6, 0.13, f.shirt, light);
        if (f.scarf >= 0) box(vc, m, n, b, -0.25, by + 0.5, -0.14, 0.25, by + 0.6, 0.14, f.scarf, light);
        // head + hair
        box(vc, m, n, b, -0.18, by + 0.6, -0.17, 0.18, by + 0.96, 0.19, f.skin, light);
        box(vc, m, n, b, -0.19, by + 0.88, -0.18, 0.19, by + 0.99, 0.2, f.hair, light);
        // arms
        if (armUp > 0) {
            float wave = Mth.sin(t * 0.6F + f.seed) * 0.08F;
            box(vc, m, n, b, -0.38, by + 0.55, -0.06 + wave, -0.24, by + 1.2, 0.08 + wave, f.shirt, light);
            box(vc, m, n, b, 0.24, by + 0.55, -0.06 - wave, 0.38, by + 1.2, 0.08 - wave, f.shirt, light);
            box(vc, m, n, b, -0.38, by + 1.2, -0.06 + wave, -0.24, by + 1.32, 0.08 + wave, f.skin, light);
            box(vc, m, n, b, 0.24, by + 1.2, -0.06 - wave, 0.38, by + 1.32, 0.08 - wave, f.skin, light);
        } else if (armUp < 0) {
            box(vc, m, n, b, -0.36, by + 0.55, -0.06, -0.24, by + 1.0, 0.08, f.shirt, light);
            box(vc, m, n, b, 0.24, by + 0.55, -0.06, 0.36, by + 1.0, 0.08, f.shirt, light);
        } else {
            box(vc, m, n, b, -0.36, by + 0.1, -0.06, -0.24, by + 0.58, 0.1, f.shirt, light);
            box(vc, m, n, b, 0.24, by + 0.1, -0.06, 0.36, by + 0.58, 0.1, f.shirt, light);
        }
    }

    private record Base(double x, double y, double z, Vec3 fwd, Vec3 right, float sway) {}

    /** local coordinates: x = right, y = up, z = forward (towards the pitch) */
    private static void box(VertexConsumer vc, Matrix4f m, Matrix3f n, Base b, double x0, double y0, double z0, double x1, double y1, double z1, int color, int light) {
        int r = (color >> 16) & 255, g = (color >> 8) & 255, bl = color & 255;
        double sw = b.sway;
        float[][] c = new float[8][];
        int i = 0;
        for (int yy = 0; yy < 2; yy++)
            for (int zz = 0; zz < 2; zz++)
                for (int xx = 0; xx < 2; xx++) {
                    double lx = xx == 0 ? x0 : x1, ly = yy == 0 ? y0 : y1, lz = zz == 0 ? z0 : z1;
                    lx += sw * ly;
                    double wx = b.x + b.right.x * lx + b.fwd.x * lz;
                    double wz = b.z + b.right.z * lx + b.fwd.z * lz;
                    c[i++] = new float[]{(float) wx, (float) (b.y + ly), (float) wz};
                }
        // corner index = yy*4 + zz*2 + xx
        Vec3 R = b.right, F = b.fwd;
        quad(vc, m, n, c[4], c[5], c[7], c[6], 0, 1, 0, r, g, bl, light);              // top
        quad(vc, m, n, c[2], c[3], c[1], c[0], 0, -1, 0, r, g, bl, light);             // bottom
        quad(vc, m, n, c[6], c[7], c[3], c[2], (float) F.x, 0, (float) F.z, r, g, bl, light);   // front
        quad(vc, m, n, c[0], c[1], c[5], c[4], (float) -F.x, 0, (float) -F.z, r, g, bl, light); // back
        quad(vc, m, n, c[5], c[7], c[3], c[1], (float) R.x, 0, (float) R.z, r, g, bl, light);   // right
        quad(vc, m, n, c[0], c[2], c[6], c[4], (float) -R.x, 0, (float) -R.z, r, g, bl, light); // left
    }

    private static void quad(VertexConsumer vc, Matrix4f m, Matrix3f n, float[] a, float[] b, float[] c, float[] d, float nx, float ny, float nz,
                             int r, int g, int bl, int light) {
        v(vc, m, n, a, 0, 0, nx, ny, nz, r, g, bl, light);
        v(vc, m, n, b, 1, 0, nx, ny, nz, r, g, bl, light);
        v(vc, m, n, c, 1, 1, nx, ny, nz, r, g, bl, light);
        v(vc, m, n, d, 0, 1, nx, ny, nz, r, g, bl, light);
    }

    private static void v(VertexConsumer vc, Matrix4f m, Matrix3f n, float[] p, float u, float vv, float nx, float ny, float nz, int r, int g, int b, int light) {
        vc.vertex(m, p[0], p[1], p[2]).color(r, g, b, 255).uv(u, vv).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, nx, ny, nz).endVertex();
    }
}
