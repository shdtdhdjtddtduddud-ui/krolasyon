package com.krolasyon.realweather.client;

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
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Overworld görünümü: yağışı {@link Precip} çizer; shader paketlerinde ayrıca derinlik testli pus katmanları. */
public final class WeatherEffects extends DimensionSpecialEffects.OverworldEffects {
    private static final ResourceLocation HAZE = new ResourceLocation(RealWeather.MODID, "textures/environment/haze.png");

    @Override
    public boolean renderSnowAndRain(ClientLevel level, int ticks, float pt, LightTexture lt, double camX, double camY, double camZ) {
        float precip = ClientWeather.state.precipitation || ClientWeather.intensity > 0.02F && ClientWeather.state != WeatherState.CLEAR
                ? ClientWeather.intensity : 0;
        float sandI = Math.max(ClientWeather.stormness * ClientWeather.intensity, Mth.clamp((ClientWeather.wind - 0.45F) * 1.2F, 0, 0.35F));
        haze(level, lt, pt);
        Precip.render(level, lt, pt, camX, camY, camZ, precip, sandI);
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
        // Krolasyon Fantasy blok ışığını sıcak renkle çarpar (pus turuncu parlar): ona yalnızca gök ışığı;
        // diğer paketler gök ışığında rengi soldurur: onlara tam ışık
        int light = ShaderCompat.packIsKrolasyon(now) ? LightTexture.pack(0, 15) : 0xF000F0;
        int n = 7;
        // katman başına saydamlık: görüş mesafesinde toplam örtü ~%95 * yoğunluk
        float per = 1 - (float) Math.pow(0.05, 1.0 / n);
        float scroll = (now + pt) * 0.002F;
        for (int i = n; i >= 1; i--) {
            float d = vis * (0.18F + 0.9F * i / n);
            float a = per * amt * (i == 1 ? 0.5F : 1F);
            float s = d * 3.0F;
            float cx = f.x() * d, cy = f.y() * d, cz = f.z() * d;
            float[][] q = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};
            for (float[] c : q) {
                float x = cx + (l.x() * c[0] + u.x() * c[1]) * s, y = cy + (l.y() * c[0] + u.y() * c[1]) * s, z = cz + (l.z() * c[0] + u.z() * c[1]) * s;
                bb.vertex(x, y, z).uv((c[0] + 1) * 0.5F + scroll * i, (c[1] + 1) * 0.5F).color(h[2] * b, h[3] * b, h[4] * b, a).uv2(light).endVertex();
            }
        }
        tess.end();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
    }

    /** Sıçramalar ve halkalar {@link Precip} içinde çizilir; vanilya yağmur sesleri yerine kendi döngülerimiz çalar. */
    @Override
    public boolean tickRain(ClientLevel level, int ticks, Camera camera) {
        return true;
    }
}
