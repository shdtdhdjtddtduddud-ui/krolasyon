package com.krolasyon.horror.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** HUD drawing hook (1.21.1: drawn at the end of the frame; the vanilla HUD is hidden during cutscenes). */
public final class GuiHook {
    private GuiHook() {}

    private static boolean forced, saved;

    public static void register(Consumer<GuiGraphics> drawer, BooleanSupplier hideVanillaHud) {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, TickEvent.RenderTickEvent.class, e -> {
            Minecraft mc = Minecraft.getInstance();
            if (e.phase == TickEvent.Phase.START) {
                boolean hide = hideVanillaHud.getAsBoolean();
                if (hide && !forced) {
                    saved = mc.options.hideGui;
                    mc.options.hideGui = true;
                    forced = true;
                } else if (!hide && forced) {
                    mc.options.hideGui = saved;
                    forced = false;
                }
                return;
            }
            if (mc.level == null || mc.player == null || mc.screen != null) return;
            GuiGraphics g = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
            drawer.accept(g);
            g.flush();
        });
    }
}
