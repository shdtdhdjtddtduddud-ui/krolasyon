package com.krolasyon.futbol;

import com.krolasyon.futbol.entity.FootballerEntity;
import com.krolasyon.futbol.network.Net;
import com.krolasyon.futbol.registry.ModBlocks;
import com.krolasyon.futbol.registry.ModEntities;
import com.krolasyon.futbol.registry.ModItems;
import com.krolasyon.futbol.registry.ModSounds;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(FutbolMod.MODID)
public class FutbolMod {
    public static final String MODID = "krolasyonfutbol";

    public FutbolMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(bus);
        ModItems.ITEMS.register(bus);
        ModItems.TABS.register(bus);
        ModEntities.ENTITIES.register(bus);
        ModSounds.SOUNDS.register(bus);
        bus.addListener(this::onAttributes);
        Net.register();
        if (Boolean.getBoolean("krolasyon.autotest")) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> com.krolasyon.futbol.client.AutoTest::init);
        }
    }

    private void onAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.FOOTBALLER.get(), FootballerEntity.createAttributes().build());
    }
}
