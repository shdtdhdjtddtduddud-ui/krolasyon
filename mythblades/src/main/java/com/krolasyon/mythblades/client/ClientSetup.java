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
        });
    }
}
