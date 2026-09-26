package com.krolasyon.bosses.client;

import com.krolasyon.bosses.KrolasyonBosses;
import com.krolasyon.bosses.client.model.CrimsonHoundModel;
import com.krolasyon.bosses.client.model.ModelLayers;
import com.krolasyon.bosses.client.model.SealWardenModel;
import com.krolasyon.bosses.client.model.SimpleEffectModel;
import com.krolasyon.bosses.client.render.*;
import com.krolasyon.bosses.registry.ModEntities;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KrolasyonBosses.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {}

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(SealWardenModel.LAYER, ModelLayers::createSealWarden);
        event.registerLayerDefinition(CrimsonHoundModel.LAYER, ModelLayers::createCrimsonHound);
        event.registerLayerDefinition(SimpleEffectModel.CRYSTAL_SPIKE, ModelLayers::createCrystalSpike);
        event.registerLayerDefinition(SimpleEffectModel.FIRE_PILLAR, ModelLayers::createFirePillar);
        event.registerLayerDefinition(SimpleEffectModel.WATCHER, ModelLayers::createWatcher);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SEAL_WARDEN.get(), SealWardenRenderer::new);
        event.registerEntityRenderer(ModEntities.CRIMSON_HOUND.get(), CrimsonHoundRenderer::new);
        event.registerEntityRenderer(ModEntities.ERUPTION.get(), EruptionRenderer::new);
        event.registerEntityRenderer(ModEntities.SEAL_PRISON.get(), SealPrisonRenderer::new);
        event.registerEntityRenderer(ModEntities.WATCHER.get(), WatcherRenderer::new);
        event.registerEntityRenderer(ModEntities.SEAL_BOLT.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.HELLFIRE_BALL.get(), NoopRenderer::new);
    }
}
