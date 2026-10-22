package com.krolasyon.smoothterrain;

import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.world.level.GrassColor;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.IEventBus;

/** Çimen yarım bloğunun rengi biyomun çimen rengini izler. */
public final class ClientColors {
    private ClientColors() {}

    public static void init(IEventBus bus) {
        bus.addListener((RegisterColorHandlersEvent.Block e) -> e.register((state, level, pos, tint) ->
                level != null && pos != null ? BiomeColors.getAverageGrassColor(level, pos) : GrassColor.getDefaultColor(), SmoothTerrain.GRASS_SLAB.get()));
        bus.addListener((RegisterColorHandlersEvent.Item e) -> e.register((stack, tint) -> GrassColor.get(0.5, 1.0), SmoothTerrain.GRASS_SLAB.get()));
    }
}
