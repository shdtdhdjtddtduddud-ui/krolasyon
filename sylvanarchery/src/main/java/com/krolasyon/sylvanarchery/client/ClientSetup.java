package com.krolasyon.sylvanarchery.client;

import com.krolasyon.sylvanarchery.SylvanArchery;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;

public final class ClientSetup {
    private ClientSetup() {}

    public static void init(IEventBus modBus) {
        modBus.addListener(ClientSetup::registerRenderers);
        modBus.addListener(ClientSetup::addLayers);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(SylvanArchery.ARROW_ENTITY.get(), SylvanArrowRenderer::new);
    }

    private static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (String skin : event.getSkins()) {
            PlayerRenderer r = event.getSkin(skin);
            if (r != null) r.addLayer(new QuiverLayer(r));
        }
    }
}
