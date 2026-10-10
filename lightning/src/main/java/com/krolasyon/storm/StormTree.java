package com.krolasyon.storm;

import com.krolasyon.storm.net.Net;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(StormTree.MODID)
public class StormTree {
    public static final String MODID = "stormtree";

    public StormTree() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModRegistry.ITEMS.register(bus);
        ModRegistry.ENTITIES.register(bus);
        ModRegistry.SOUNDS.register(bus);
        ModRegistry.PARTICLES.register(bus);
        bus.addListener(StormCapability::register);
        bus.addListener(this::setup);
        bus.addListener(this::tabs);
        MinecraftForge.EVENT_BUS.addGenericListener(net.minecraft.world.entity.Entity.class, StormCapability::attach);
        MinecraftForge.EVENT_BUS.addListener(StormCapability::clone);
        MinecraftForge.EVENT_BUS.addListener(StormCommand::register);
        if (Boolean.getBoolean("stormtree.autotest")) {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> com.krolasyon.storm.client.AutoTest::init);
        }
    }

    private void setup(FMLCommonSetupEvent e) { e.enqueueWork(Net::register); }

    private void tabs(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES || e.getTabKey() == CreativeModeTabs.COMBAT) e.accept(ModRegistry.STORM_TOME.get());
    }
}
