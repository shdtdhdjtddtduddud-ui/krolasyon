package com.krolasyon.emeraldblade.client;

import com.krolasyon.emeraldblade.EmeraldBlade;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;

public final class ClientSetup {
    private ClientSetup() {}

    public static void init(IEventBus modBus) {
        modBus.addListener(ClientSetup::registerRenderers);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EmeraldBlade.SPIKE.get(), SpikeRenderer::new);
        event.registerEntityRenderer(EmeraldBlade.SNARE.get(), SnareRenderer::new);
    }
}
