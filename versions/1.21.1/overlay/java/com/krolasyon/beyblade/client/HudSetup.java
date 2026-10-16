package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.LibraBeyblade;
import com.krolasyon.beyblade.compat.V;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Adds the beyblade HUD and the match scoreboard as GUI layers (1.21.1). */
@Mod.EventBusSubscriber(modid = LibraBeyblade.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class HudSetup {
    private HudSetup() {}

    private static final LibraHud LIBRA_HUD = new LibraHud();
    private static final MatchHud MATCH_HUD = new MatchHud();
    private static final LaunchHud LAUNCH_HUD = new LaunchHud();

    @SubscribeEvent
    public static void addLayers(AddGuiOverlayLayersEvent event) {
        event.getLayeredDraw().add(V.rl(LibraBeyblade.MODID, "libra_hud"),
                (g, delta) -> LIBRA_HUD.render(g, delta.getGameTimeDeltaPartialTick(true), g.guiWidth(), g.guiHeight()));
        event.getLayeredDraw().add(V.rl(LibraBeyblade.MODID, "match_hud"),
                (g, delta) -> MATCH_HUD.render(g, delta.getGameTimeDeltaPartialTick(true), g.guiWidth(), g.guiHeight()));
        event.getLayeredDraw().add(V.rl(LibraBeyblade.MODID, "launch_hud"),
                (g, delta) -> LAUNCH_HUD.render(g, delta.getGameTimeDeltaPartialTick(true), g.guiWidth(), g.guiHeight()));
    }
}
