package com.krolasyon.voidtree;

import com.krolasyon.voidtree.net.Net;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(VoidTree.MODID)
public class VoidTree {
    public static final String MODID = "voidtree";

    public VoidTree() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModRegistry.ITEMS.register(bus);
        ModRegistry.ENTITIES.register(bus);
        ModRegistry.SOUNDS.register(bus);
        ModRegistry.PARTICLES.register(bus);
        bus.addListener(VoidCapability::register);
        bus.addListener(this::setup);
        bus.addListener(this::tabs);
        bus.addListener(this::attributes);
        MinecraftForge.EVENT_BUS.addGenericListener(net.minecraft.world.entity.Entity.class, VoidCapability::attach);
        MinecraftForge.EVENT_BUS.addListener(VoidCapability::clone);
        MinecraftForge.EVENT_BUS.addListener(VoidCommand::register);
        if (Boolean.getBoolean("voidtree.autotest")) {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> com.krolasyon.voidtree.client.AutoTest::init);
        }
    }

    private void attributes(net.minecraftforge.event.entity.EntityAttributeCreationEvent e) {
        e.put(ModRegistry.SHADOW_CLONE.get(), com.krolasyon.voidtree.entity.ShadowCloneEntity.createAttributes().build());
    }

    private void setup(FMLCommonSetupEvent e) { e.enqueueWork(Net::register); }

    private void tabs(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES || e.getTabKey() == CreativeModeTabs.COMBAT) e.accept(ModRegistry.GRIMOIRE.get());
    }
}
