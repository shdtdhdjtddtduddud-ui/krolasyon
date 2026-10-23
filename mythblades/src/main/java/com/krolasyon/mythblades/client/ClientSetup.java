package com.krolasyon.mythblades.client;

import com.krolasyon.mythblades.MythBlades;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;

public final class ClientSetup {
    private ClientSetup() {}

    public static void init(IEventBus bus) {
        bus.addListener((EntityRenderersEvent.RegisterRenderers e) -> {
            e.registerEntityRenderer(MythBlades.WAVE.get(), WaveRenderer::new);
            e.registerBlockEntityRenderer(MythBlades.ALTAR_BE.get(), AltarRenderer::new);
            e.registerEntityRenderer(MythBlades.EMBER_WAVE.get(), EmberWaveRenderer::new);
        });
        bus.addListener((net.minecraftforge.client.event.RegisterKeyMappingsEvent e) -> e.register(EmberClient.DASH));
        bus.addListener((net.minecraftforge.client.event.RegisterGuiOverlaysEvent e) -> e.registerAboveAll("ember_screen",
                (gui, g, pt, w, h) -> EmberFx.overlay(g, pt, w, h)));
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(EmberClient::tick);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(EmberFx::render);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(EmberFx::cameraAngles);
    }
}
