package com.krolasyon.realweather.client;

import com.krolasyon.realweather.Climate;
import com.krolasyon.realweather.RealWeather;
import com.krolasyon.realweather.WeatherState;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;

import java.util.Random;

/**
 * Yağışı tek tek çizer: her damla, kar tanesi ve kum zerresi kameranın çevresindeki döngüsel bir hacimde gerçek fizik
 * değerleriyle düşer (damla çapına göre son hız, karın yavaş süzülüşü ve salınımı, rüzgârla sürüklenme). Görüntüdeki
 * uzunluk hızdan gelen hareket bulanıklığıdır; uzaktaki damlalar piksel altına inince incelmek yerine soluklaşır.
 * Çatı ve ağaç altına yağış girmez; damlalar zemine çarpınca taç gibi sıçrar, suya düşünce halka açar. Kum fırtınasında
 * yerde akan zerrelerin üstünde yuvarlanan toz bulutları ve fırtına yaklaşırken ufuktan gelen toz duvarı çizilir.
 */
public final class Precip {
    private Precip() {}

    private static final ResourceLocation DROP = new ResourceLocation(RealWeather.MODID, "textures/environment/rain_drop.png");
    private static final ResourceLocation FLAKES = new ResourceLocation(RealWeather.MODID, "textures/environment/snow_flakes.png");
    private static final ResourceLocation SPLASH = new ResourceLocation(RealWeather.MODID, "textures/environment/splash.png");
    private static final ResourceLocation RIPPLE = new ResourceLocation(RealWeather.MODID, "textures/environment/ripple.png");
    private static final ResourceLocation DUST = new ResourceLocation(RealWeather.MODID, "textures/environment/dust.png");

    private static final int MAX = 20000;
    private static final float[] R1 = new float[MAX], R2 = new float[MAX], R3 = new float[MAX], R4 = new float[MAX], R5 = new float[MAX],
            R6 = new float[MAX];

    static {
        Random r = new Random(7331);
        for (int i = 0; i < MAX; i++) {
            R1[i] = r.nextFloat();
            R2[i] = r.nextFloat();
            R3[i] = r.nextFloat();
            R4[i] = r.nextFloat();
            R5[i] = r.nextFloat();
            R6[i] = r.nextFloat();
        }
    }

    // ------------------------------------------------------------------ sütun önbelleği (tik başına bir kez)
    private static final int R = 24, N = 2 * R + 1;
    private static final int[] HEIGHT = new int[N * N], LIGHT = new int[N * N];
    private static final float[] SURFACE = new float[N * N];
    private static final byte[] KIND = new byte[N * N];
    private static final boolean[] WATER = new boolean[N * N];
    private static int ox = Integer.MIN_VALUE, oz, oy;
    private static long otick = -1;

    private static void refresh(ClientLevel level, int cx, int cy, int cz) {
        long t = level.getGameTime();
        if (t == otick && cx - R == ox && cz - R == oz && cy == oy) return;
        otick = t;
        ox = cx - R;
        oz = cz - R;
        oy = cy;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int j = 0; j < N; j++) {
            for (int i = 0; i < N; i++) {
                int x = ox + i, z = oz + j, k = j * N + i;
                int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                HEIGHT[k] = h;
                // iklim 2x2 blokta bir (biyom çözünürlüğü zaten 4 blok)
                if ((i & 1) == 0 && (j & 1) == 0) {
                    p.set(x, Math.max(h, cy), z);
                    KIND[k] = (byte) Climate.kind(level, p).ordinal();
                } else {
                    KIND[k] = KIND[(j & ~1) * N + (i & ~1)];
                }
                p.set(x, h - 1, z);
                BlockState s = level.getBlockState(p);
                WATER[k] = s.getFluidState().is(Fluids.WATER);
                float top = s.isAir() || WATER[k] ? (WATER[k] ? 0.89F : 0) : (float) s.getShape(level, p).max(Direction.Axis.Y);
                SURFACE[k] = h - 1 + (top <= 0 ? 1 : top);
                p.set(x, Math.max(h, cy), z);
                LIGHT[k] = LevelRenderer.getLightColor(level, p);
            }
        }
    }

    private static int col(double x, double z) {
        int i = Mth.floor(x) - ox, j = Mth.floor(z) - oz;
        return i < 0 || j < 0 || i >= N || j >= N ? -1 : j * N + i;
    }

    private static double wrap(double v, double size) {
        return v - size * Math.floor(v / size);
    }

    private static float smooth(float e0, float e1, float x) {
        float t = Mth.clamp((x - e0) / (e1 - e0), 0, 1);
        return t * t * (3 - 2 * t);
    }

    private static void bind(ResourceLocation tex) {
        // doğrusal süzgeç: kare piksel yerine yumuşak kenar
        Minecraft.getInstance().getTextureManager().getTexture(tex).setFilter(true, false);
        RenderSystem.setShaderTexture(0, tex);
    }

    // ------------------------------------------------------------------ çizim

    public static void render(ClientLevel level, LightTexture lt, float pt, double camX, double camY, double camZ, float precip, float sandI) {
        Minecraft mc = Minecraft.getInstance();
        float q = Minecraft.useFancyGraphics() ? 1F : 0.55F;
        double time = level.getGameTime() + (double) pt;
        refresh(level, Mth.floor(camX), Mth.floor(camY), Mth.floor(camZ));
        lastSand = sandI;
        if (precip < 0.01F && sandI < 0.02F) return;

        lt.turnOnLightLayer();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(Minecraft.useShaderTransparency());
        RenderSystem.setShader(GameRenderer::getParticleShader);
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder b = tess.getBuilder();

        if (precip >= 0.01F) {
            bind(DROP);
            b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
            rain(b, precip, q, time, camX, camY, camZ);
            tess.end();
            bind(SPLASH);
            b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
            splashes(b, precip, q, time, camX, camY, camZ, false);
            tess.end();
            bind(RIPPLE);
            b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
            splashes(b, precip, q, time, camX, camY, camZ, true);
            tess.end();
            bind(FLAKES);
            b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
            snow(b, precip, q, time, camX, camY, camZ);
            tess.end();
        }
        if (sandI >= 0.02F) {
            bind(DROP);
            b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
            grains(b, sandI, q, time, camX, camY, camZ);
            tess.end();
        }
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        lt.turnOffLightLayer();
    }

    private static float lastSand;

    /**
     * Toz bulutları ve toz duvarı parçacık aşamasında çizilir: shader paketleri hava geçişinde dokuları kendi yağmur
     * stiline çevirir, parçacık geçişinde ise olduğu gibi gösterir.
     */
    public static void renderDust(net.minecraftforge.client.event.RenderLevelStageEvent e) {
        if (e.getStage() != net.minecraftforge.client.event.RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || level.dimension() != net.minecraft.world.level.Level.OVERWORLD) return;
        float sandI = lastSand, wall = wallAmount();
        if (sandI < 0.02F && wall < 0.01F) return;
        float q = Minecraft.useFancyGraphics() ? 1F : 0.55F;
        float pt = e.getPartialTick();
        double time = level.getGameTime() + (double) pt;
        net.minecraft.world.phys.Vec3 c = e.getCamera().getPosition();
        LightTexture lt = mc.gameRenderer.lightTexture();
        com.mojang.blaze3d.vertex.PoseStack mv = RenderSystem.getModelViewStack();
        mv.pushPose();
        mv.mulPoseMatrix(e.getPoseStack().last().pose());
        RenderSystem.applyModelViewMatrix();
        lt.turnOnLightLayer();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getParticleShader);
        bind(DUST);
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder b = tess.getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        if (sandI >= 0.02F) billows(level, b, sandI, q, time, c.x, c.y, c.z);
        if (wall >= 0.01F) wall(level, b, wall, time, c.x, c.y, c.z);
        flush(b);
        tess.end();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        lt.turnOffLightLayer();
        mv.popPose();
        RenderSystem.applyModelViewMatrix();
    }

    /**
     * Hıza göre uzatılmış, kameraya dönük dörtgen. (x,y,z) kameraya göre baş noktası, (vx,vy,vz) yön (birim),
     * len uzunluk, w yarı genişlik. Doku v=0 kuyruk, v=1 baş.
     */
    private static void streak(BufferBuilder b, double x, double y, double z, double vx, double vy, double vz, double len, double w,
                               float u0, float u1, float v0, float v1, float r, float g, float bl, float a, int light) {
        // yan vektör = yön x bakış (kameradan noktaya)
        double sx = vy * z - vz * y, sy = vz * x - vx * z, sz = vx * y - vy * x;
        double sl = Math.sqrt(sx * sx + sy * sy + sz * sz);
        if (sl < 1e-6) return;
        sx *= w / sl;
        sy *= w / sl;
        sz *= w / sl;
        double tx = x - vx * len, ty = y - vy * len, tz = z - vz * len;
        b.vertex(tx - sx, ty - sy, tz - sz).uv(u0, v0).color(r, g, bl, a).uv2(light).endVertex();
        b.vertex(tx + sx, ty + sy, tz + sz).uv(u1, v0).color(r, g, bl, a).uv2(light).endVertex();
        b.vertex(x + sx, y + sy, z + sz).uv(u1, v1).color(r, g, bl, a).uv2(light).endVertex();
        b.vertex(x - sx, y - sy, z - sz).uv(u0, v1).color(r, g, bl, a).uv2(light).endVertex();
    }

    // ------------------------------------------------------------------ yağmur

    private static void rain(BufferBuilder b, float amount, float q, double time, double cx, double cy, double cz) {
        final double HX = 30, HY = 22;
        int n = (int) (Mth.clamp(amount, 0, 1.2F) * 9000 * q);
        double wx = ClientWeather.windX * 0.85, wz = ClientWeather.windZ * 0.85;
        float shade = 0.82F + 0.1F * (1 - ClientWeather.stormness);
        for (int i = 0; i < n; i++) {
            // damla çapı 0.5-4 mm (küçükler çok); son hız (Atlas): v = 9.65 - 10.3 e^(-0.6 d) m/s
            float d = 0.5F + 3.5F * R1[i] * R1[i];
            double vt = (9.65 - 10.3 * Math.exp(-0.6 * d)) / 20.0;
            double x = wrap(R2[i] * HX + wx * time - cx + HX / 2, HX) - HX / 2;
            double y = wrap(R3[i] * HY - vt * time - cy + HY / 2, HY) - HY / 2;
            double z = wrap(R4[i] * HX + wz * time - cz + HX / 2, HX) - HX / 2;
            double rh = Math.sqrt(x * x + z * z), dist = Math.sqrt(rh * rh + y * y);
            if (dist < 0.6) continue;
            float fade = (1 - smooth(10, 15, (float) rh)) * (1 - smooth(7.5F, 11, (float) Math.abs(y)));
            if (fade <= 0.01F) continue;
            int k = col(x + cx, z + cz);
            if (k < 0 || KIND[k] != 0 || y + cy < HEIGHT[k]) continue;
            double vx = wx, vy = -vt, vz = wz;
            double sp = Math.sqrt(vx * vx + vy * vy + vz * vz);
            // hareket bulanıklığı: ~1/22 sn pozlama
            double len = sp * 20 * 0.06;
            double w0 = 0.0035 + 0.0018 * d, wmin = dist * 0.0016;
            double w = Math.max(w0, wmin);
            float a = (0.42F + 0.28F * Math.min(amount, 1)) * fade * (float) Math.max(0.45, w0 / w) * (0.7F + 0.3F * R5[i]);
            streak(b, x, y, z, vx / sp, vy / sp, vz / sp, len, w, 0, 1, 0, 1, shade, shade + 0.04F, shade + 0.1F, a, LIGHT[k]);
        }
    }

    // ------------------------------------------------------------------ sıçrama tacı ve su halkası

    private static int hash(int a, int b) {
        int h = a * 0x9E3779B1 ^ b * 0x85EBCA6B;
        h ^= h >>> 16;
        h *= 0x7FEB352D;
        h ^= h >>> 15;
        h *= 0x846CA68B;
        h ^= h >>> 16;
        return h;
    }

    private static void splashes(BufferBuilder b, float amount, float q, double time, double cx, double cy, double cz, boolean water) {
        int n = (int) (Mth.clamp(amount, 0, 1.2F) * 900 * q);
        org.joml.Vector3f left = Minecraft.getInstance().gameRenderer.getMainCamera().getLeftVector();
        double lx = left.x(), lz = left.z(), ll = Math.sqrt(lx * lx + lz * lz);
        if (ll < 1e-4) {
            lx = 1;
            lz = 0;
        } else {
            lx /= ll;
            lz /= ll;
        }
        int bx = Mth.floor(cx), bz = Mth.floor(cz);
        for (int i = 0; i < n; i++) {
            double period = 6 + 4 * R1[i];
            double tt = time / period + R2[i];
            int cycle = (int) Math.floor(tt);
            float age = (float) (tt - cycle);
            int h = hash(i, cycle);
            double x = bx - 14 + (h & 0xFFFF) / 65536.0 * 28 + 0.5, z = bz - 14 + (h >>> 16) / 65536.0 * 28 + 0.5;
            int k = col(x, z);
            if (k < 0 || KIND[k] != 0 || WATER[k] != water) continue;
            double y = SURFACE[k] + 0.01;
            double rx = x - cx, ry = y - cy, rz = z - cz;
            double dist = Math.sqrt(rx * rx + ry * ry + rz * rz);
            float fade = 1 - smooth(9, 14, (float) dist);
            if (fade <= 0 || Math.abs(ry) > 10) continue;
            int light = LIGHT[k];
            if (water) {
                double s = 0.04 + 0.32 * age;
                float a = 0.55F * (1 - age) * fade;
                b.vertex(rx - s, ry, rz - s).uv(0, 0).color(0.85F, 0.9F, 1F, a).uv2(light).endVertex();
                b.vertex(rx - s, ry, rz + s).uv(0, 1).color(0.85F, 0.9F, 1F, a).uv2(light).endVertex();
                b.vertex(rx + s, ry, rz + s).uv(1, 1).color(0.85F, 0.9F, 1F, a).uv2(light).endVertex();
                b.vertex(rx + s, ry, rz - s).uv(1, 0).color(0.85F, 0.9F, 1F, a).uv2(light).endVertex();
            } else {
                if (age > 0.6F) continue;
                float f = age / 0.6F;
                int frame = Math.min(3, (int) (f * 4));
                double w = 0.07 + 0.05 * f, hgt = w * 1.0;
                float a = 0.55F * fade * (1 - f * 0.5F);
                float u0 = frame * 0.25F, u1 = u0 + 0.25F;
                double ax = lx * w, az = lz * w;
                b.vertex(rx - ax, ry, rz - az).uv(u0, 1).color(0.85F, 0.9F, 0.97F, a).uv2(light).endVertex();
                b.vertex(rx + ax, ry, rz + az).uv(u1, 1).color(0.85F, 0.9F, 0.97F, a).uv2(light).endVertex();
                b.vertex(rx + ax, ry + hgt, rz + az).uv(u1, 0).color(0.85F, 0.9F, 0.97F, a).uv2(light).endVertex();
                b.vertex(rx - ax, ry + hgt, rz - az).uv(u0, 0).color(0.85F, 0.9F, 0.97F, a).uv2(light).endVertex();
            }
        }
    }

    // ------------------------------------------------------------------ kar

    private static void snow(BufferBuilder b, float amount, float q, double time, double cx, double cy, double cz) {
        float storm = ClientWeather.stormness;
        final double HX = 26, HY = 18;
        int n = (int) (Mth.clamp(amount, 0, 1.2F) * (15000 + 5000 * storm) * q);
        n = Math.min(n, MAX);
        double drift = 0.45 + 1.0 * storm;
        double wx = ClientWeather.windX * drift, wz = ClientWeather.windZ * drift;
        for (int i = 0; i < n; i++) {
            // kar tanesi 2-10 mm, son hız 0.5-1.5 m/s (büyük kümeler biraz daha hızlı)
            float size = 0.022F + 0.05F * R1[i] * R1[i];
            double vt = (0.55 + 0.5 * R1[i] + 0.4 * R5[i]) / 20.0;
            // türbülans: her tanenin kendi salınımı (yaklaşık 2-6 sn periyot)
            double om = 0.05 + 0.1 * R6[i], amp = 0.25 + 0.45 * R5[i] * (1 + storm);
            double ph = R2[i] * 40;
            double sx = Math.sin(time * om + ph) * amp, sz = Math.cos(time * om * 0.83 + ph * 1.7) * amp;
            double dsx = Math.cos(time * om + ph) * amp * om, dsz = -Math.sin(time * om * 0.83 + ph * 1.7) * amp * om * 0.83;
            double x = wrap(R2[i] * HX + wx * time - cx + HX / 2, HX) - HX / 2 + sx;
            double y = wrap(R3[i] * HY - vt * time - cy + HY / 2, HY) - HY / 2;
            double z = wrap(R4[i] * HX + wz * time - cz + HX / 2, HX) - HX / 2 + sz;
            double rh = Math.sqrt(x * x + z * z), dist = Math.sqrt(rh * rh + y * y);
            if (dist < 0.35) continue;
            float fade = (1 - smooth(8.5F, 13, (float) rh)) * (1 - smooth(6, 9, (float) Math.abs(y)));
            if (fade <= 0.01F) continue;
            int k = col(x + cx, z + cz);
            if (k < 0 || KIND[k] != 1 || y + cy < HEIGHT[k]) continue;
            double vx = wx + dsx, vy = -vt, vz = wz + dsz;
            double sp = Math.sqrt(vx * vx + vy * vy + vz * vz);
            double smin = dist * 0.0022;
            double s = Math.max(size, smin);
            float a = 0.95F * fade * (float) Math.max(0.5, size / s);
            // hızlı (tipide) taneler pozlamada çizgi olur; yavaş olanlar yuvarlak kalır
            double blur = sp * 20 * (0.008 + 0.016 * storm);
            double len = Math.max(2 * s, blur + s);
            int v = i & 3;
            float u0 = (v & 1) * 0.5F, v0 = (v >> 1) * 0.5F;
            double ux = vx / sp, uy = vy / sp, uz = vz / sp;
            // başı tanenin önüne taşı ki merkez doğru yerde kalsın
            streak(b, x + ux * len * 0.5, y + uy * len * 0.5, z + uz * len * 0.5, ux, uy, uz, len, s, u0, u0 + 0.5F, v0, v0 + 0.5F,
                    0.95F, 0.96F, 1F, a, LIGHT[k]);
        }
    }

    // ------------------------------------------------------------------ kum: yere yakın akan zerreler

    private static void grains(BufferBuilder b, float amount, float q, double time, double cx, double cy, double cz) {
        final double HX = 30, HY = 12;
        int n = (int) (Mth.clamp(amount, 0, 1.2F) * 6000 * q);
        double ws = Math.max(0.15, Math.hypot(ClientWeather.windX, ClientWeather.windZ));
        double dx = ClientWeather.windX / ws, dz = ClientWeather.windZ / ws;
        for (int i = 0; i < n; i++) {
            double sp = ws * (1.1 + 0.9 * R1[i]);
            double hop = Math.sin(time * (0.2 + 0.3 * R5[i]) + R6[i] * 30) * 0.06;
            double x = wrap(R2[i] * HX + dx * sp * time - cx + HX / 2, HX) - HX / 2;
            double y = wrap(R3[i] * HY - cy + HY / 2, HY) - HY / 2;
            double z = wrap(R4[i] * HX + dz * sp * time - cz + HX / 2, HX) - HX / 2;
            int k = col(x + cx, z + cz);
            if (k < 0 || KIND[k] < 2) continue;
            double above = y + cy - HEIGHT[k];
            if (above < 0) continue;
            double rh = Math.sqrt(x * x + z * z), dist = Math.sqrt(rh * rh + y * y);
            if (dist < 0.5) continue;
            float fade = (1 - smooth(9, 15, (float) rh)) * (float) Math.exp(-above / (1.5 + 4 * amount));
            if (KIND[k] == 3) fade *= 0.25F;
            if (fade < 0.01F) continue;
            double vx = dx * sp, vy = hop, vz = dz * sp;
            double l = Math.sqrt(vx * vx + vy * vy + vz * vz);
            double w0 = 0.004, w = Math.max(w0, dist * 0.0012);
            float a = 0.7F * fade * (float) Math.max(0.35, w0 / w);
            float k2 = 0.8F + 0.3F * R5[i];
            streak(b, x, y, z, vx / l, vy / l, vz / l, l * 20 * 0.025, w, 0, 1, 0, 1, 0.86F * k2, 0.68F * k2, 0.46F * k2, a, LIGHT[k]);
        }
    }

    // bulutlar uzaktan yakına sıralanıp çizilir (derinlik yazan shader paketlerinde de kenarlar yumuşak kalsın)
    private static final int PMAX = 600;
    private static final double[] PX = new double[PMAX], PY = new double[PMAX], PZ = new double[PMAX], PS = new double[PMAX], PD = new double[PMAX];
    private static final float[] PR = new float[PMAX], PA = new float[PMAX], PB = new float[PMAX];
    private static final int[] PV = new int[PMAX];
    private static final Integer[] ORDER = new Integer[PMAX];
    private static int pn;

    private static void queue(double x, double y, double z, double s, float roll, int variant, float a, float bright) {
        if (pn >= PMAX) return;
        PX[pn] = x;
        PY[pn] = y;
        PZ[pn] = z;
        PS[pn] = s;
        PD[pn] = x * x + y * y + z * z;
        PR[pn] = roll;
        PV[pn] = variant;
        PA[pn] = a;
        PB[pn] = bright;
        pn++;
    }

    private static void flush(BufferBuilder b) {
        var cam = Minecraft.getInstance().gameRenderer.getMainCamera();
        org.joml.Vector3f lv = cam.getLeftVector(), uv = cam.getUpVector();
        for (int i = 0; i < pn; i++) ORDER[i] = i;
        java.util.Arrays.sort(ORDER, 0, pn, (x, y) -> Double.compare(PD[y], PD[x]));
        for (int k = 0; k < pn; k++) {
            int i = ORDER[k];
            puff(b, PX[i], PY[i], PZ[i], PS[i], PR[i], PV[i], PA[i], PB[i], lv, uv);
        }
        pn = 0;
    }

    /** Kameraya dönük, dönen yumuşak bulut. Renk alttan üste koyulaşmadan açılır. */
    private static void puff(BufferBuilder b, double x, double y, double z, double s, float roll, int variant, float a, float bright,
                             org.joml.Vector3f lv, org.joml.Vector3f uv) {
        float u0 = (variant & 1) * 0.5F, v0 = (variant >> 1 & 1) * 0.5F;
        float c = Mth.cos(roll), sn = Mth.sin(roll);
        float[][] q = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};
        float[][] t = {{u0, v0 + 0.5F}, {u0 + 0.5F, v0 + 0.5F}, {u0 + 0.5F, v0}, {u0, v0}};
        int light = LightTexture.pack(0, 15);
        for (int k = 0; k < 4; k++) {
            float px = q[k][0] * c - q[k][1] * sn, py = q[k][0] * sn + q[k][1] * c;
            double vx = x + (lv.x() * px + uv.x() * py) * s, vy = y + (lv.y() * px + uv.y() * py) * s, vz = z + (lv.z() * px + uv.z() * py) * s;
            // alt kısım gölgede (koyu turuncu), üst güneşte (açık kum rengi)
            float h = Mth.clamp((float) ((vy - y) / s) * 0.5F + 0.5F, 0, 1);
            float k2 = bright * (0.72F + 0.32F * h);
            b.vertex(vx, vy, vz).uv(t[k][0], t[k][1]).color(0.88F * k2, 0.66F * k2, 0.42F * k2, a).uv2(light).endVertex();
        }
    }

    // ------------------------------------------------------------------ toz bulutları (fırtınanın içinde)

    private static void billows(ClientLevel level, BufferBuilder b, float amount, float q, double time, double cx, double cy, double cz) {
        var cam = Minecraft.getInstance().gameRenderer.getMainCamera();
        org.joml.Vector3f lv = cam.getLeftVector(), uv = cam.getUpVector();
        final double HX = 110;
        int n = (int) (Mth.clamp(amount, 0, 1.2F) * 260 * (0.5F + 0.5F * q));
        double wx = ClientWeather.windX * 0.75, wz = ClientWeather.windZ * 0.75;
        float bright = Mth.clamp(level.getSkyDarken(1) * 1.1F, 0.12F, 1);
        float sandW = Mth.clamp(ClientWeather.sandW + ClientWeather.dryW * 0.3F, 0, 1);
        for (int i = 0; i < n; i++) {
            double x = wrap(R2[i] * HX + wx * time - cx + HX / 2, HX) - HX / 2;
            double z = wrap(R4[i] * HX + wz * time - cz + HX / 2, HX) - HX / 2;
            double rh = Math.sqrt(x * x + z * z);
            int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x + cx), Mth.floor(z + cz));
            double s = 3.5 + 6 * R1[i];
            double y = ground - cy + s * 0.45 + R3[i] * R3[i] * 14;
            double dist = Math.sqrt(rh * rh + y * y);
            // yakındakiler ekranı kapatmasın, kenarlar döngüyü belli etmesin
            float fade = smooth(3, 10, (float) dist) * (1 - smooth(42, 55, (float) rh));
            if (fade < 0.01F) continue;
            float a = (0.10F + 0.12F * R5[i]) * Math.min(amount * 1.3F, 1) * sandW * fade;
            queue(x, y, z, s, (float) (R6[i] * 6.28 + time * 0.004 * (R5[i] - 0.5)), i & 3, a, bright);
        }
    }

    // ------------------------------------------------------------------ toz duvarı (fırtına ufuktan gelir)

    /** 0..1: kum fırtınası yaklaşıyorsa ya da yeni başladıysa duvarın görünürlüğü. */
    private static float wallAmount() {
        float sandW = ClientWeather.sandW;
        if (sandW < 0.3F) return 0;
        WeatherState s = ClientWeather.state, nx = ClientWeather.next;
        float a = 0;
        if (s != WeatherState.STORM && nx == WeatherState.STORM) a = Mth.clamp(1 - ClientWeather.ticksLeft / 3600F, 0, 1);
        else if (s == WeatherState.STORM) a = Mth.clamp(1 - ClientWeather.sandAmt * 1.6F, 0, 1);
        return a * sandW;
    }

    private static void wall(ClientLevel level, BufferBuilder b, float amount, double time, double cx, double cy, double cz) {
        var cam = Minecraft.getInstance().gameRenderer.getMainCamera();
        org.joml.Vector3f lv = cam.getLeftVector(), uv = cam.getUpVector();
        double ws = Math.hypot(ClientWeather.windX, ClientWeather.windZ);
        double dx = ws < 1e-4 ? 1 : ClientWeather.windX / ws, dz = ws < 1e-4 ? 0 : ClientWeather.windZ / ws;
        // duvar rüzgârın geldiği yönde; fırtına yaklaştıkça mesafe kısalır
        double near;
        if (ClientWeather.state == WeatherState.STORM) near = Math.max(6, 40 - 60 * ClientWeather.sandAmt);
        else near = 40 + 150 * Mth.clamp(ClientWeather.ticksLeft / 3600F, 0, 1);
        // sisin içinde kaybolmasın: görüş mesafesinin yarısından uzağa konmaz
        near = Math.min(near, RenderSystem.getShaderFogEnd() * 0.5);
        double px = -dz, pz = dx;
        float bright = Mth.clamp(level.getSkyDarken(1) * 1.1F, 0.12F, 1);
        int camGround = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(cx), Mth.floor(cz));
        for (int i = 0; i < 260; i++) {
            int row = i % 4;
            double lat = (R1[i] - 0.5) * 2 * 260;
            double dep = near + R2[i] * 25 + row * 8;
            double s = 14 + 14 * R3[i] + row * 5;
            double x = -dx * dep + px * lat, z = -dz * dep + pz * lat;
            // kenarlara doğru alçalan, tepesi kabaran duvar
            double crown = 1 - Math.pow(Math.abs(lat) / 270, 2);
            if (crown <= 0) continue;
            int g = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x + cx), Mth.floor(z + cz));
            if (g <= level.getMinBuildHeight() + 1) g = camGround;
            g = Math.max(g, level.getSeaLevel());
            double y = g - cy + s * 0.35 + row * 17 * crown + R4[i] * 7 * crown;
            float a = (0.7F + 0.25F * R5[i]) * amount * (float) Math.min(1, crown * 2.5) * (row == 3 ? 0.7F : 1);
            queue(x, y, z, s, (float) (R6[i] * 6.28 + time * 0.0025 * (R5[i] - 0.5)), i & 3, Math.min(a, 0.95F), bright * (0.78F + 0.08F * row));
        }
    }
}
