package com.krolasyon.horror.compat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** HUD drawing hook (1.20.1: after the vanilla GUI; vanilla overlays are hidden during cutscenes). */
public final class GuiHook {
    private GuiHook() {}

    public static void register(Consumer<GuiGraphics> drawer, BooleanSupplier hideVanillaHud) {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, RenderGuiEvent.Post.class, e -> drawer.accept(e.getGuiGraphics()));
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, RenderGuiOverlayEvent.Pre.class, e -> {
            if (hideVanillaHud.getAsBoolean()) e.setCanceled(true);
        });
    }
}
