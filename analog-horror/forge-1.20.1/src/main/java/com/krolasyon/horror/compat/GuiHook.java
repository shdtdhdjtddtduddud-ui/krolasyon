package com.krolasyon.horror.compat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;

import java.util.function.Consumer;

/** HUD drawing hook (1.20.1: after the vanilla GUI). */
public final class GuiHook {
    private GuiHook() {}

    public static void register(Consumer<GuiGraphics> drawer) {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, RenderGuiEvent.Post.class, e -> drawer.accept(e.getGuiGraphics()));
    }
}
