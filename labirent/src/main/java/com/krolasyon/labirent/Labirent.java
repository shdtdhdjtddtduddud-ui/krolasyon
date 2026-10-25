package com.krolasyon.labirent;

import com.krolasyon.labirent.entity.BeetleBladeEntity;
import com.krolasyon.labirent.entity.GladerEntity;
import com.krolasyon.labirent.entity.GrieverEntity;
import com.krolasyon.labirent.net.Net;
import com.krolasyon.labirent.registry.*;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Labirent.MODID)
public class Labirent {
    public static final String MODID = "labirent";

    public Labirent() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(bus);
        ModItems.ITEMS.register(bus);
        ModItems.TABS.register(bus);
        ModEntities.ENTITIES.register(bus);
        ModSounds.SOUNDS.register(bus);
        ModEffects.EFFECTS.register(bus);
        bus.addListener(this::onAttributes);
        bus.addListener(this::onSetup);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, LabConfig.SPEC);
        if (Boolean.getBoolean("labirent.autotest")) {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> com.krolasyon.labirent.client.AutoTest::init);
        }
    }

    private void onSetup(FMLCommonSetupEvent e) { e.enqueueWork(Net::register); }

    private void onAttributes(EntityAttributeCreationEvent e) {
        e.put(ModEntities.GRIEVER.get(), GrieverEntity.createAttributes().build());
        e.put(ModEntities.GLADER.get(), GladerEntity.createAttributes().build());
        e.put(ModEntities.BEETLE_BLADE.get(), BeetleBladeEntity.createAttributes().build());
    }
}
