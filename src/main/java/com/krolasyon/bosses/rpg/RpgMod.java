package com.krolasyon.bosses.rpg;

import com.krolasyon.bosses.rpg.item.RpgItems;
import com.krolasyon.bosses.rpg.net.RpgNet;
import com.krolasyon.bosses.rpg.registry.RpgEffects;
import com.krolasyon.bosses.rpg.registry.RpgEntities;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/** Entry point of the RPG layer, called from the main mod constructor. */
public final class RpgMod {
    private RpgMod() {}

    public static void init(IEventBus modBus) {
        RpgEntities.ENTITIES.register(modBus);
        RpgEffects.EFFECTS.register(modBus);
        RpgItems.ITEMS.register(modBus);
        RpgItems.TABS.register(modBus);
        modBus.addListener(RpgEntities::attributes);
        modBus.addListener((FMLCommonSetupEvent e) -> e.enqueueWork(RpgNet::register));
    }
}
