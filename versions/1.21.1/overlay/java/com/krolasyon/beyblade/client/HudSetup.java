package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.LibraBeyblade;
import com.krolasyon.beyblade.compat.VClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Draws the beyblade HUD and the match scoreboard after the vanilla HUD (1.21.1). */
@Mod.EventBusSubscriber(modid = LibraBeyblade.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class HudSetup {
    private HudSetup() {}

    private static final LibraHud LIBRA_HUD = new LibraHud();
    private static final MatchHud MATCH_HUD = new MatchHud();

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        GuiGraphics g = event.getGuiGraphics();
        float partial = VClient.partialTick();
        LIBRA_HUD.render(g, partial, g.guiWidth(), g.guiHeight());
        MATCH_HUD.render(g, partial, g.guiWidth(), g.guiHeight());
    }
}
