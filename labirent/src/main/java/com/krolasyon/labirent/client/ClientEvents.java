package com.krolasyon.labirent.client;

import com.krolasyon.labirent.Labirent;
import com.krolasyon.labirent.client.model.BeetleBladeModel;
import com.krolasyon.labirent.client.model.GrieverModel;
import com.krolasyon.labirent.client.model.ModelLayers;
import com.krolasyon.labirent.client.render.*;
import com.krolasyon.labirent.registry.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Labirent.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {}

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions e) {
        e.registerLayerDefinition(GrieverModel.LAYER, ModelLayers::createGriever);
        e.registerLayerDefinition(BeetleBladeModel.LAYER, ModelLayers::createBeetleBlade);
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(ModEntities.GRIEVER.get(), GrieverRenderer::new);
        e.registerEntityRenderer(ModEntities.GLADER.get(), GladerRenderer::new);
        e.registerEntityRenderer(ModEntities.BEETLE_BLADE.get(), BeetleBladeRenderer::new);
        e.registerEntityRenderer(ModEntities.BOX_ELEVATOR.get(), BoxElevatorRenderer::new);
    }

    @SubscribeEvent
    public static void keys(RegisterKeyMappingsEvent e) {
        e.register(Keys.MAP);
        e.register(Keys.CAPTAIN);
        e.register(Keys.JOURNAL);
        e.register(Keys.MINIMAP);
    }

    @SubscribeEvent
    public static void sky(net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent e) {
        e.register(new net.minecraft.resources.ResourceLocation(Labirent.MODID, "maze"), new MazeSkyEffects());
    }

    @SubscribeEvent
    public static void overlays(RegisterGuiOverlaysEvent e) {
        e.registerAboveAll("labirent_hud", Hud::render);
    }
}
