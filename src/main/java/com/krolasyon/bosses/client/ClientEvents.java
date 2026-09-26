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
        event.registerLayerDefinition(com.krolasyon.bosses.client.model.RevengeModel.LAYER, ModelLayers::createRevenge);
        event.registerLayerDefinition(com.krolasyon.bosses.client.model.HeartDemonModel.LAYER, ModelLayers::createHeartDemon);
        event.registerLayerDefinition(SimpleEffectModel.BLOOD_SPIKE, ModelLayers::createBloodSpike);
        event.registerLayerDefinition(SimpleEffectModel.THORN_SPIKE, ModelLayers::createThornSpike);
        event.registerLayerDefinition(SimpleEffectModel.BLOOD_LANCE, ModelLayers::createBloodLance);
    }

    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        com.krolasyon.bosses.client.form.DemonClient.renderer = new com.krolasyon.bosses.client.form.DemonPlayerRenderer(event.getContext());
    }

    @SubscribeEvent
    public static void registerKeys(net.minecraftforge.client.event.RegisterKeyMappingsEvent event) {
        for (net.minecraft.client.KeyMapping k : com.krolasyon.bosses.client.form.KeyBinds.ABILITIES) event.register(k);
    }

    @SubscribeEvent
    public static void registerOverlays(net.minecraftforge.client.event.RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("demon_form", com.krolasyon.bosses.client.form.DemonHud::render);
    }

    @SubscribeEvent
    public static void clientSetup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
        com.krolasyon.bosses.form.DemonForm.clientDemon = com.krolasyon.bosses.client.form.DemonClient::isDemon;
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
        event.registerEntityRenderer(ModEntities.REVENGE.get(), RevengeRenderer::new);
        event.registerEntityRenderer(ModEntities.HEART_DEMON.get(), HeartDemonRenderer::new);
        event.registerEntityRenderer(ModEntities.BLOOD_LANCE.get(), BloodLanceRenderer::new);
        event.registerEntityRenderer(ModEntities.HEART_ORB.get(), NoopRenderer::new);
    }
}
