package com.krolasyon.aethercore.client;

import com.krolasyon.aethercore.AetherCore;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;

public final class ClientSetup {
    private ClientSetup() {}

    public static void init(IEventBus modBus) {
        modBus.addListener(ClientSetup::registerRenderers);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(AetherCore.AETHER_CORE_BE.get(), AetherCoreRenderer::new);
    }
}
