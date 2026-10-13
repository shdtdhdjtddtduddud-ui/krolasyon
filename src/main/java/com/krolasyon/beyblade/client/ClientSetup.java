package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.LibraBeyblade;
import com.krolasyon.beyblade.registry.ModEntities;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = LibraBeyblade.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    public static final String CATEGORY = "key.categories.librabeyblade";
    public static final KeyMapping RECALL_KEY = new KeyMapping("key.librabeyblade.recall", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY);
    public static final KeyMapping[] ABILITY_KEYS = {
            new KeyMapping("key.librabeyblade.ability1", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY),
            new KeyMapping("key.librabeyblade.ability2", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, CATEGORY),
            new KeyMapping("key.librabeyblade.ability3", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, CATEGORY),
            new KeyMapping("key.librabeyblade.ability4", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, CATEGORY),
            new KeyMapping("key.librabeyblade.ability5", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, CATEGORY),
    };

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.LIBRA.get(), BeybladeRenderer::new);
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(RECALL_KEY);
        for (KeyMapping k : ABILITY_KEYS) event.register(k);
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("libra_hud", new LibraHud());
    }
}
