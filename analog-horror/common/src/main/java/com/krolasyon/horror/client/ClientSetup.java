package com.krolasyon.horror.client;

import com.krolasyon.horror.compat.HatmanRenderer;
import com.krolasyon.horror.entity.HatmanEntity;
import com.krolasyon.horror.registry.ModEntities;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;

public final class ClientSetup {
    private ClientSetup() {}

    public static void init(IEventBus modBus) {
        modBus.addListener(ClientSetup::renderers);
        HatmanEntity.CLIENT_TICK = ClientFx::onHatmanTick;
        HatmanEntity.CLIENT_EVENT = ClientFx::onHatmanEvent;
        MinecraftForge.EVENT_BUS.register(ClientFx.class);
        MinecraftForge.EVENT_BUS.register(HorrorOverlay.class);
        if (Boolean.getBoolean("krolasyon.autotest")) com.krolasyon.horror.compat.AutoTest.init();
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(ModEntities.HATMAN.get(), HatmanRenderer::new);
    }
}
