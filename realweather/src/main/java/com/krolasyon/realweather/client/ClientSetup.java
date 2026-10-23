package com.krolasyon.realweather.client;

import com.krolasyon.realweather.RealWeather;
import com.krolasyon.realweather.client.particle.WeatherParticles;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;

public final class ClientSetup {
    private ClientSetup() {}

    private static final ResourceLocation SAND_VIGNETTE = new ResourceLocation(RealWeather.MODID, "textures/misc/sand_vignette.png");
    private static final ResourceLocation FROST_VIGNETTE = new ResourceLocation(RealWeather.MODID, "textures/misc/frost_vignette.png");
    private static final ResourceLocation WET_VIGNETTE = new ResourceLocation(RealWeather.MODID, "textures/misc/wet_vignette.png");

    public static void init(IEventBus bus) {
        bus.addListener((RegisterDimensionSpecialEffectsEvent e) -> e.register(BuiltinDimensionTypes.OVERWORLD_EFFECTS, new WeatherEffects()));
        bus.addListener((RegisterParticleProvidersEvent e) -> {
            reg(e, RealWeather.SNOWFLAKE, "snowflake");
            reg(e, RealWeather.LEAF, "leaf");
            reg(e, RealWeather.SAND, "sand");
            reg(e, RealWeather.FIREFLY, "firefly");
            reg(e, RealWeather.RIPPLE, "ripple");
            reg(e, RealWeather.MIST, "mist");
            reg(e, RealWeather.BREATH, "breath");
            reg(e, RealWeather.SPLASH, "splash");
        });
        bus.addListener((RegisterGuiOverlaysEvent e) -> e.registerAbove(VanillaGuiOverlay.FROSTBITE.id(), "weather_vignette", (gui, g, pt, w, h) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.level.dimension() != Level.OVERWORLD) return;
            if (!mc.options.getCameraType().isFirstPerson()) return;
            float e1 = ClientWeather.exposure;
            vignette(g, SAND_VIGNETTE, Math.min(1, ClientWeather.sandAmt * 1.1F) * e1 * 0.28F, w, h);
            vignette(g, FROST_VIGNETTE, ClientWeather.blizzardAmt * e1 * 0.6F, w, h);
            vignette(g, WET_VIGNETTE, ClientWeather.rainAmt * ClientWeather.stormness * e1 * 0.35F, w, h);
        }));
        MinecraftForge.EVENT_BUS.addListener(ClientWeather::tick);
        MinecraftForge.EVENT_BUS.addListener(ClientWeather::fog);
        MinecraftForge.EVENT_BUS.addListener(ClientWeather::fogColor);
        MinecraftForge.EVENT_BUS.addListener(ClientWeather::onJoin);
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> ClientWeather.reset());
    }

    private static void vignette(net.minecraft.client.gui.GuiGraphics g, ResourceLocation tex, float a, int w, int h) {
        if (a < 0.01F) return;
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.setColor(1, 1, 1, Math.min(1, a));
        g.blit(tex, 0, 0, 0, 0, w, h, w, h);
        g.setColor(1, 1, 1, 1);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    private static void reg(RegisterParticleProvidersEvent e, RegistryObject<SimpleParticleType> t, String kind) {
        e.registerSpriteSet(t.get(), s -> new WeatherParticles.Provider(s, kind));
    }
}
