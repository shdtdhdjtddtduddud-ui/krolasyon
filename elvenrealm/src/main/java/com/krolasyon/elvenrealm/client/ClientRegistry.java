package com.krolasyon.elvenrealm.client;

import com.krolasyon.elvenrealm.ElvenRealm;
import com.krolasyon.elvenrealm.entity.ElfKind;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;

/** İstemci kayıtları: model katmanları ve her elf türü için çizici. */
public final class ClientRegistry {
    private ClientRegistry() {}

    public static void init(IEventBus bus) {
        bus.addListener(ClientRegistry::layers);
        bus.addListener(ClientRegistry::renderers);
    }

    private static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ElfRenderer.SLIM, () -> ElfModel.createLayer(true));
        event.registerLayerDefinition(ElfRenderer.WIDE, () -> ElfModel.createLayer(false));
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        for (ElfKind k : ElfKind.values()) event.registerEntityRenderer(ElvenRealm.TYPES.get(k).get(), ctx -> new ElfRenderer(ctx, k));
    }
}
