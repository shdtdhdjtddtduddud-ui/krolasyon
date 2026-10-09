package com.krolasyon.vocations;

import com.krolasyon.vocations.registry.VocationEntities;
import com.krolasyon.vocations.registry.VocationItems;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(VocationsMod.MODID)
public class VocationsMod {
    public static final String MODID = "krolasyonvocations";

    public VocationsMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        VocationItems.ITEMS.register(bus);
        VocationItems.TABS.register(bus);
        VocationEntities.ENTITY_TYPES.register(bus);
    }
}
