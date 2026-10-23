package com.krolasyon.realweather.client;

import com.krolasyon.realweather.Climate;
import com.krolasyon.realweather.RealWeather;
import com.krolasyon.realweather.WeatherState;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;

/**
 * Overworld görünümü: yağmur, kar ve kum perdelerini kendisi çizer. Perdeler rüzgârla eğilir, yoğunluğa göre
 * kalınlaşır (yoğunken her sütunda iki katman), kar tipide yatay savrulur, kum yere yakın akar. Yağmur sıçramaları,
 * su halkaları ve yaprak damlaları da burada üretilir.
 */
public final class WeatherEffects extends DimensionSpecialEffects.OverworldEffects {
    private static final ResourceLocation RAIN = new ResourceLocation(RealWeather.MODID, "textures/environment/rain.png");
    private static final ResourceLocation SNOW = new ResourceLocation(RealWeather.MODID, "textures/environment/snow.png");
    private static final ResourceLocation SAND = new ResourceLocation(RealWeather.MODID, "textures/environment/sand.png");
    private static final ResourceLocation HAZE = new ResourceLocation(RealWeather.MODID, "textures/environment/haze.png");

    @Override
    public boolean renderSnowAndRain(ClientLevel level, int ticks, float pt, LightTexture lt, double camX, double camY, double camZ) {
        float precip = ClientWeather.state.precipitation || ClientWeather.intensity > 0.02F && ClientWeather.state != WeatherState.CLEAR
                ? ClientWeather.intensity : 0;
        float sandI = Math.max(ClientWeather.stormness * ClientWeather.intensity, Mth.clamp((ClientWeather.wind - 0.45F) * 1.2F, 0, 0.35F));
        haze(level, lt, pt);
        if (precip < 0.01F && sandI < 0.03F) return true;
        lt.turnOnLightLayer();
        int cx = Mth.floor(camX), cy = Mth.floor(camY), cz = Mth.floor(camZ);
        int rad = Minecraft.useFancyGraphics() ? 13 : 8;
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder b = tess.getBuilder();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(Minecraft.useShaderTransparency());
        RenderSystem.setShader(GameRenderer::getParticleShader);
        int mode = -1;
        float time = ticks + pt;
        double wx = ClientWeather.windX, wz = ClientWeather.windZ;
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        Climate.Kind[] kinds = new Climate.Kind[(2 * rad + 1) * (2 * rad + 1)];
        for (int z = cz - rad; z <= cz + rad; z++) {
            for (int x = cx - rad; x <= cx + rad; x++) {
                double dx = x + 0.5 - camX, dz = z + 0.5 - camZ;
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > rad + 0.5) continue;
                mp.set(x, cy, z);
                // sütun iklimi (4x4 bloklarda bir hesaplanıp paylaşılır)
                int ki = (Math.floorDiv(z - cz + rad, 1)) * (2 * rad + 1) + (x - cx + rad);
                Climate.Kind kind = kinds[ki];
                if (kind == null) {
                    kind = Climate.kind(level, mp);
                    kinds[ki] = kind;
                }
                float amount = switch (kind) {
                    case RAIN, SNOW -> precip;
                    case SAND -> sandI;
                    case DRY -> sandI * 0.25F;
                };
                if (amount < 0.01F) continue;
                int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                int y0 = Math.max(cy - rad, ground), y1 = Math.max(cy + rad, ground);
                if (kind == Climate.Kind.SAND || kind == Climate.Kind.DRY) {
                    // kum yere yakın akar
                    y0 = ground;
                    y1 = Math.min(Math.max(cy + 6, ground + 3), ground + 14);
                    if (y1 <= y0) continue;
                }
                if (y0 == y1) continue;
                int want = kind == Climate.Kind.RAIN ? 0 : kind == Climate.Kind.SNOW ? 1 : 2;
                if (mode != want) {
                    if (mode >= 0) tess.end();
                    mode = want;
                    RenderSystem.setShaderTexture(0, want == 0 ? RAIN : want == 1 ? SNOW : SAND);
                    b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
                }
                long seed = (long) x * x * 3121 + x * 45238971L ^ (long) z * z * 418711 + z * 13761L;
                RandomSource rnd = RandomSource.create(seed);
                mp.set(x, Math.max(ground, cy), z);
                int light = LevelRenderer.getLightColor(level, mp);
                float fade = (float) (1 - (dist / (rad + 0.5)) * (dist / (rad + 0.5)));
                int layers = amount > 0.55F && want != 2 ? 2 : 1;
                for (int layer = 0; layer < layers; layer++) {
                    // perde düzlemi kameraya dik (her katman biraz döndürülmüş)
                    double ang = Math.atan2(dz, dx) + Math.PI / 2 + (layer == 0 ? 0 : 0.6 + rnd.nextDouble() * 0.5);
                    double hx = Math.cos(ang) * 0.5, hz = Math.sin(ang) * 0.5;
                    double ox = x + 0.5 - camX + (layer == 0 ? 0 : rnd.nextDouble() - 0.5), oz = z + 0.5 - camZ + (layer == 0 ? 0 : rnd.nextDouble() - 0.5);
                    float r = 1, g = 1, bl = 1, alpha, u0, v0, v1, uScroll = 0;
                    double slant;
                    float h = y1 - y0;
                    if (want == 0) {
                        float speed = 0.2F + 0.12F * rnd.nextFloat() + 0.08F * precip;
                        v0 = -time * speed + rnd.nextFloat() * 4;
                        alpha = (0.35F + 0.5F * amount) * fade * (0.75F + 0.25F * rnd.nextFloat());
                        r = 0.78F;
                        g = 0.84F;
                        bl = 0.92F;
                        slant = 0.55;
                    } else if (want == 1) {
                        float speed = 0.012F + 0.008F * rnd.nextFloat() + 0.03F * ClientWeather.blizzardAmt;
                        v0 = -time * speed + rnd.nextFloat() * 4;
                        uScroll = (float) (time * 0.004 * Math.sin(seed % 7 + time * 0.01) + time * 0.012 * Math.hypot(wx, wz) * (1 + ClientWeather.stormness * 3));
                        alpha = (0.5F + 0.5F * amount) * fade;
                        slant = 1.4 + 2.0 * ClientWeather.stormness;
                    } else {
                        v0 = (float) Math.sin(time * 0.05 + seed) * 0.05F;
                        uScroll = (float) (time * (0.05 + 0.08 * amount) * (1 + rnd.nextFloat()));
                        // uzaktaki perdeler daha görünür (toz duvarı), yakındakiler ince (önü kapatmasın)
                        alpha = Mth.clamp(amount * 0.3F, 0, 0.28F) * fade * (float) Mth.clamp(dist / 6.0, 0.25, 1.0);
                        r = 0.86F;
                        g = 0.70F;
                        bl = 0.48F;
                        slant = 0.15;
                    }
                    v1 = v0 + h * 0.25F;
                    u0 = uScroll + layer * 0.5F;
                    // rüzgâr eğimi: perdenin üstü rüzgârın tersine kayar
                    double sx = -wx * slant * h * 0.25, sz = -wz * slant * h * 0.25;
                    double yb = y0 - camY, yt = y1 - camY;
                    b.vertex(ox - hx + sx, yt, oz - hz + sz).uv(u0, v0).color(r, g, bl, alpha).uv2(light).endVertex();
                    b.vertex(ox + hx + sx, yt, oz + hz + sz).uv(u0 + 1, v0).color(r, g, bl, alpha).uv2(light).endVertex();
                    b.vertex(ox + hx, yb, oz + hz).uv(u0 + 1, v1).color(r, g, bl, alpha).uv2(light).endVertex();
                    b.vertex(ox - hx, yb, oz - hz).uv(u0, v1).color(r, g, bl, alpha).uv2(light).endVertex();
                }
            }
        }
        if (mode >= 0) tess.end();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        lt.turnOffLightLayer();
        return true;
    }

    /**
     * Shader paketi vanilya sisini yok sayıyorsa: kameranın önünde, uzaklaştıkça yoğunlaşan ve derinlik testiyle arazinin
     * arkasında kalan yarı saydam pus katmanları. Yakındaki tepe net, arkasındaki uzak dağ puslu görünür; gök de örtülür.
     */
    private static void haze(ClientLevel level, LightTexture lt, float pt) {
        long now = level.getGameTime();
        if (!ShaderCompat.shaders(now)) return;
        float[] h = ClientWeather.haze();
        float amt = h[0], vis = h[1];
        if (amt < 0.03F) return;
        Camera cam = Minecraft.getInstance().gameRenderer.getMainCamera();
        org.joml.Vector3f f = cam.getLookVector(), u = cam.getUpVector(), l = cam.getLeftVector();
        float b = Mth.clamp(level.getSkyDarken(pt) * 1.1F, 0.1F, 1);
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder bb = tess.getBuilder();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getParticleShader);
        RenderSystem.setShaderTexture(0, HAZE);
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        int n = 7;
        // katman başına saydamlık: görüş mesafesinde toplam örtü ~%95 * yoğunluk
        float per = 1 - (float) Math.pow(0.05, 1.0 / n);
        float scroll = (now + pt) * 0.002F;
        for (int i = n; i >= 1; i--) {
            float d = vis * (0.18F + 0.9F * i / n);
            float a = per * amt * (i == 1 ? 0.5F : 1F);
            float s = d * 2.2F;
            float cx = f.x() * d, cy = f.y() * d, cz = f.z() * d;
            float[][] q = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};
            for (float[] c : q) {
                float x = cx + (l.x() * c[0] + u.x() * c[1]) * s, y = cy + (l.y() * c[0] + u.y() * c[1]) * s, z = cz + (l.z() * c[0] + u.z() * c[1]) * s;
                bb.vertex(x, y, z).uv((c[0] + 1) * 0.5F + scroll * i, (c[1] + 1) * 0.5F).color(h[2] * b, h[3] * b, h[4] * b, a).uv2(0xF000F0).endVertex();
            }
        }
        tess.end();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
    }

    /** Sıçramalar, su halkaları; vanilya yağmur sesleri yerine kendi döngülerimiz çalar. */
    @Override
    public boolean tickRain(ClientLevel level, int ticks, Camera camera) {
        float rain = ClientWeather.rainAmt;
        if (rain < 0.03F || Minecraft.getInstance().isPaused()) return true;
        RandomSource r = level.random;
        BlockPos c = camera.getBlockPosition();
        int n = (int) (rain * rain * 90);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int i = 0; i < n; i++) {
            int x = c.getX() + r.nextInt(25) - 12, z = c.getZ() + r.nextInt(25) - 12;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            if (Math.abs(y - c.getY()) > 12) continue;
            p.set(x, y - 1, z);
            if (Climate.kind(level, p) != Climate.Kind.RAIN) continue;
            BlockState s = level.getBlockState(p);
            double px = x + r.nextDouble(), pz = z + r.nextDouble();
            if (s.getFluidState().is(Fluids.WATER) && s.getFluidState().isSource()) {
                level.addParticle(RealWeather.RIPPLE.get(), px, y - 1 + 0.9, pz, 0, 0, 0);
            } else if (s.is(net.minecraft.tags.BlockTags.LEAVES)) {
                if (r.nextInt(4) == 0) level.addParticle(ParticleTypes.RAIN, px, y + 0.02, pz, 0, 0, 0);
            } else {
                double top = y - 1 + s.getShape(level, p).max(net.minecraft.core.Direction.Axis.Y);
                level.addParticle(r.nextInt(3) == 0 ? RealWeather.SPLASH.get() : ParticleTypes.RAIN, px, top + 0.02, pz, 0, 0, 0);
            }
        }
        return true;
    }
}
