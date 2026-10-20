package com.krolasyon.advgear.client;

import com.krolasyon.advgear.AdvGear;
import com.krolasyon.advgear.network.Net;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

public final class ClientSetup {
    public static final KeyMapping GEAR_KEY = new KeyMapping("key.advgear.gear", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.categories.advgear");
    public static final KeyMapping BACKPACK_KEY = new KeyMapping("key.advgear.backpack", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.advgear");

    private ClientSetup() {}

    public static void init(IEventBus bus) {
        bus.addListener(ClientSetup::setup);
        bus.addListener(ClientSetup::addLayers);
        bus.addListener(ClientSetup::renderers);
        bus.addListener((RegisterKeyMappingsEvent e) -> {
            e.register(GEAR_KEY);
            e.register(BACKPACK_KEY);
        });
        MinecraftForge.EVENT_BUS.addListener(ClientSetup::keys);
        MinecraftForge.EVENT_BUS.addListener(Slosh::tick);
        MinecraftForge.EVENT_BUS.addListener(OreSight::render);
        MinecraftForge.EVENT_BUS.addListener(ClientSetup::inventoryButton);
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> ClientGear.clear());
    }

    private static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(AdvGear.GEAR_MENU.get(), GearScreen::new);
            MenuScreens.register(chest(AdvGear.BACKPACK_2.get()), ContainerScreen::new);
            MenuScreens.register(chest(AdvGear.BACKPACK_3.get()), ContainerScreen::new);
            MenuScreens.register(chest(AdvGear.BACKPACK_4.get()), ContainerScreen::new);
        });
    }

    @SuppressWarnings("unchecked")
    private static net.minecraft.world.inventory.MenuType<net.minecraft.world.inventory.ChestMenu> chest(net.minecraft.world.inventory.MenuType<?> t) {
        return (net.minecraft.world.inventory.MenuType<net.minecraft.world.inventory.ChestMenu>) t;
    }

    private static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (String skin : event.getSkins()) {
            PlayerRenderer r = event.getSkin(skin);
            if (r != null) r.addLayer(new GearLayer(r));
        }
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(AdvGear.POISON_FLASK_ENTITY.get(), ThrownItemRenderer::new);
    }

    private static void keys(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        while (GEAR_KEY.consumeClick()) if (mc.screen == null) Net.CHANNEL.sendToServer(new Net.Open(0));
        while (BACKPACK_KEY.consumeClick()) if (mc.screen == null) Net.CHANNEL.sendToServer(new Net.Open(1));
    }

    /** Envanter ekranına kuşanım düğmesi. */
    private static void inventoryButton(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen inv)) return;
        event.addListener(Button.builder(Component.literal("✦"), b -> Net.CHANNEL.sendToServer(new Net.Open(0)))
                .bounds(inv.getGuiLeft() + 152, inv.getGuiTop() - 20, 22, 18)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("gui.advgear.gear"))).build());
    }
}
