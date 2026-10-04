package com.krolasyon.bosses.rpg;

import com.krolasyon.bosses.rpg.registry.RpgEffects;
import com.krolasyon.bosses.rpg.registry.RpgEntities;
import net.minecraftforge.eventbus.api.IEventBus;

/** Entry point of the RPG layer, called from the main mod constructor. */
public final class RpgMod {
    private RpgMod() {}

    public static void init(IEventBus modBus) {
        RpgEntities.ENTITIES.register(modBus);
        RpgEffects.EFFECTS.register(modBus);
        modBus.addListener(RpgEntities::attributes);
    }
}
