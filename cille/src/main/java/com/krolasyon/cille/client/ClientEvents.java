package com.krolasyon.cille.client;

import com.krolasyon.cille.CilleMod;
import com.krolasyon.cille.client.model.KabkabaModel;
import com.krolasyon.cille.client.model.ModelLayers;
import com.krolasyon.cille.client.model.SimpleModel;
import com.krolasyon.cille.client.render.*;
import com.krolasyon.cille.registry.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CilleMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {}

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(KabkabaModel.LAYER, ModelLayers::createKabkaba);
        event.registerLayerDefinition(SimpleModel.SPHERE, ModelLayers::createSphere);
        event.registerLayerDefinition(SimpleModel.SHADOW_SPIKE, ModelLayers::createShadowSpike);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.KABKABA.get(), KabkabaRenderer::new);
        event.registerEntityRenderer(ModEntities.MARBLE.get(), MarbleRenderer::new);
        event.registerEntityRenderer(ModEntities.BLACK_HOLE.get(), BlackHoleRenderer::new);
        event.registerEntityRenderer(ModEntities.SHADOW_SPIKE.get(), ShadowSpikeRenderer::new);
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        for (KeyMapping k : CilleKeys.all()) event.register(k);
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("kabkaba_status", CilleHud.INSTANCE);
    }
}
