package com.krolasyon.bosses.client;

import com.krolasyon.bosses.KrolasyonBosses;
import com.krolasyon.bosses.client.model.CrimsonHoundModel;
import com.krolasyon.bosses.client.model.ModelLayers;
import com.krolasyon.bosses.client.model.SealWardenModel;
import com.krolasyon.bosses.client.model.SimpleEffectModel;
import com.krolasyon.bosses.client.render.*;
import com.krolasyon.bosses.client.morph.FormModel;
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
        event.registerLayerDefinition(FormModel.layer("aigoar"), ModelLayers::createAigoar);
        event.registerLayerDefinition(FormModel.layer("solar"), ModelLayers::createSolar);
        event.registerLayerDefinition(FormModel.layer("dragon"), ModelLayers::createDragon);
        event.registerLayerDefinition(FormModel.layer("flame"), ModelLayers::createFlame);
        event.registerLayerDefinition(FormModel.layer("shade"), ModelLayers::createShade);
        event.registerLayerDefinition(FormModel.layer("lancer"), ModelLayers::createLancer);
        event.registerLayerDefinition(FormProjectileRenderer.SPEAR_LAYER, ModelLayers::createHellSpear);
        event.registerLayerDefinition(TideGeyserRenderer.LAYER, ModelLayers::createTideGeyser);
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
        event.registerEntityRenderer(ModEntities.TIDE_VORTEX.get(), TideVortexRenderer::new);
        event.registerEntityRenderer(ModEntities.TIDE_GEYSER.get(), TideGeyserRenderer::new);
        event.registerEntityRenderer(ModEntities.TSUNAMI_WAVE.get(), TsunamiWaveRenderer::new);
        event.registerEntityRenderer(ModEntities.ZONE.get(), ZoneRenderer::new);
        event.registerEntityRenderer(ModEntities.FORM_PROJECTILE.get(), FormProjectileRenderer::new);
    }

    @SubscribeEvent
    public static void bakeMorph(EntityRenderersEvent.AddLayers event) {
        com.krolasyon.bosses.client.morph.FormRenderer.bake(event.getEntityModels());
    }

    @SubscribeEvent
    public static void registerKeys(net.minecraftforge.client.event.RegisterKeyMappingsEvent event) {
        for (net.minecraft.client.KeyMapping k : com.krolasyon.bosses.client.morph.ClientMorph.ABILITY_KEYS) event.register(k);
        event.register(com.krolasyon.bosses.client.morph.ClientMorph.TRANSFORM_KEY);
    }

    @SubscribeEvent
    public static void registerOverlays(net.minecraftforge.client.event.RegisterGuiOverlaysEvent event) {
        event.registerAbove(net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.HOTBAR.id(), "aigoar_abilities",
                com.krolasyon.bosses.client.morph.AigoarHud::render);
    }
}
