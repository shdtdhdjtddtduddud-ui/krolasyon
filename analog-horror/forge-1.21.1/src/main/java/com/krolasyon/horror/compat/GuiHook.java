package com.krolasyon.horror.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;

import java.util.function.Consumer;

/** HUD drawing hook (1.21.1: drawn at the end of the frame while no screen is open). */
public final class GuiHook {
    private GuiHook() {}

    public static void register(Consumer<GuiGraphics> drawer) {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, TickEvent.RenderTickEvent.class, e -> {
            if (e.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null || mc.screen != null || mc.options.hideGui) return;
            GuiGraphics g = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
            drawer.accept(g);
            g.flush();
        });
    }
}
