package com.krolasyon.alien;

import com.krolasyon.alien.entity.XenomorphEntity;
import com.krolasyon.alien.network.Net;
import com.krolasyon.alien.registry.*;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(AlienMod.MODID)
public class AlienMod {
    public static final String MODID = "alienisolation";

    public AlienMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(bus);
        ModItems.ITEMS.register(bus);
        ModItems.TABS.register(bus);
        ModEntities.ENTITIES.register(bus);
        ModSounds.SOUNDS.register(bus);
        bus.addListener(this::onAttributes);
        bus.addListener(this::onSetup);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, AlienConfig.SPEC);
        if (Boolean.getBoolean("alien.autotest")) {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> com.krolasyon.alien.client.AutoTest::init);
        }
    }

    private void onSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(Net::register);
    }

    private void onAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.XENOMORPH.get(), XenomorphEntity.createAttributes().build());
    }
}
