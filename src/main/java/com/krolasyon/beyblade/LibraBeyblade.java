package com.krolasyon.beyblade;

import com.krolasyon.beyblade.network.ModNetwork;
import com.krolasyon.beyblade.registry.ModEntities;
import com.krolasyon.beyblade.registry.ModItems;
import com.krolasyon.beyblade.registry.ModSounds;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(LibraBeyblade.MODID)
public class LibraBeyblade {
    public static final String MODID = "librabeyblade";

    public LibraBeyblade() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        com.krolasyon.beyblade.registry.ModBlocks.BLOCKS.register(bus);
        ModEntities.ENTITIES.register(bus);
        ModItems.ITEMS.register(bus);
        ModItems.TABS.register(bus);
        ModSounds.SOUNDS.register(bus);
        bus.addListener(this::commonSetup);
        if (Boolean.getBoolean("libra.autotest")) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> com.krolasyon.beyblade.client.AutoTest::init);
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
    }
}
