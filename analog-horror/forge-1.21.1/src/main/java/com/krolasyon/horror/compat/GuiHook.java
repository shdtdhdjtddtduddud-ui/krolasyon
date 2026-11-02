package com.krolasyon.horror.compat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * HUD drawing hook (1.21.1). Forge 52 has no general GUI render event; the chat customisation event fires every frame
 * from the chat layer near the top of the HUD, so the overlays are drawn there (vanilla HUD stays as it is; the
 * cutscene letterbox covers the hotbar).
 */
public final class GuiHook {
    private GuiHook() {}

    public static void register(Consumer<GuiGraphics> drawer, BooleanSupplier hideVanillaHud) {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, CustomizeGuiOverlayEvent.Chat.class, e -> drawer.accept(e.getGuiGraphics()));
    }
}
