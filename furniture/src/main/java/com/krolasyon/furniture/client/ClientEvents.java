package com.krolasyon.furniture.client;

import com.krolasyon.furniture.FurnitureMod;
import com.krolasyon.furniture.client.model.FurnitureLayers;
import com.krolasyon.furniture.client.model.FurnitureModelLayers;
import com.krolasyon.furniture.client.render.*;
import com.krolasyon.furniture.client.screen.WardrobeScreen;
import com.krolasyon.furniture.registry.ModBlockEntities;
import com.krolasyon.furniture.registry.ModEntities;
import com.krolasyon.furniture.registry.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = FurnitureMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {}

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(FurnitureModelLayers.SOFA, FurnitureLayers::createSofa);
        event.registerLayerDefinition(FurnitureModelLayers.PIANO, FurnitureLayers::createPiano);
        event.registerLayerDefinition(FurnitureModelLayers.CHALKBOARD, FurnitureLayers::createChalkboard);
        event.registerLayerDefinition(FurnitureModelLayers.NIGHTSTAND, FurnitureLayers::createNightstand);
        event.registerLayerDefinition(FurnitureModelLayers.WARDROBE, FurnitureLayers::createWardrobe);
        event.registerLayerDefinition(FurnitureModelLayers.BROOM, FurnitureLayers::createBroom);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.SOFA.get(), SofaRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.PIANO.get(), PianoRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.CHALKBOARD.get(), ChalkboardRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.NIGHTSTAND.get(), NightstandRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.WARDROBE.get(), WardrobeRenderer::new);
        event.registerEntityRenderer(ModEntities.SEAT.get(), NoopRenderer::new);
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(ModMenus.WARDROBE.get(), WardrobeScreen::new));
    }
}
