package com.krolasyon.alien.client;

import com.krolasyon.alien.AlienMod;
import com.krolasyon.alien.client.model.XenoLayer;
import com.krolasyon.alien.client.model.XenomorphModel;
import com.krolasyon.alien.client.render.XenomorphRenderer;
import com.krolasyon.alien.item.FlashlightItem;
import com.krolasyon.alien.registry.ModEntities;
import com.krolasyon.alien.registry.ModItems;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = AlienMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions e) {
        e.registerLayerDefinition(XenomorphModel.LAYER, XenoLayer::create);
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(ModEntities.XENOMORPH.get(), XenomorphRenderer::new);
        e.registerEntityRenderer(ModEntities.THROWN_DEVICE.get(), ctx -> new ThrownItemRenderer<>(ctx, 1.0F, true));
    }

    @SubscribeEvent
    public static void overlays(RegisterGuiOverlaysEvent e) {
        e.registerBelowAll("atmosphere", Hud::atmosphere);
        e.registerAboveAll("locker", Hud::locker);
        e.registerAboveAll("tracker", Hud::tracker);
        e.registerAboveAll("kill", Hud::kill);
        e.registerAboveAll("objective", Hud::objective);
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent e) {
        e.enqueueWork(() -> ItemProperties.register(ModItems.FLASHLIGHT.get(), new ResourceLocation(AlienMod.MODID, "on"),
                (stack, level, entity, seed) -> FlashlightItem.isOn(stack) ? 1F : 0F));
    }
}
