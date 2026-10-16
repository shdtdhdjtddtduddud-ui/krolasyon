package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.LibraBeyblade;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Puts the beyblade HUD and the match scoreboard on screen (1.20.1 overlay API; the ports replace this file). */
@Mod.EventBusSubscriber(modid = LibraBeyblade.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class HudSetup {
    private HudSetup() {}

    private static final LibraHud LIBRA_HUD = new LibraHud();
    private static final MatchHud MATCH_HUD = new MatchHud();

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("libra_hud", (gui, g, partial, w, h) -> LIBRA_HUD.render(g, partial, w, h));
        event.registerAboveAll("match_hud", (gui, g, partial, w, h) -> MATCH_HUD.render(g, partial, w, h));
    }
}
