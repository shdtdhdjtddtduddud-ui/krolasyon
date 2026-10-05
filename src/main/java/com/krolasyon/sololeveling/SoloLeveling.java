package com.krolasyon.sololeveling;

import com.krolasyon.sololeveling.net.Net;
import com.krolasyon.sololeveling.registry.*;
import com.krolasyon.sololeveling.system.HunterCapability;
import com.krolasyon.sololeveling.system.SystemEvents;
import com.krolasyon.sololeveling.world.ModWorldgen;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(SoloLeveling.MODID)
public class SoloLeveling {
    public static final String MODID = "sololeveling";

    public SoloLeveling() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.REG.register(bus);
        ModBlocks.BE.register(bus);
        ModItems.REG.register(bus);
        ModItems.TABS.register(bus);
        ModEntities.REG.register(bus);
        ModSounds.REG.register(bus);
        ModWorldgen.FEATURES.register(bus);
        bus.addListener(ModEntities::attributes);
        bus.addListener(HunterCapability::register);
        bus.addListener(this::setup);
        MinecraftForge.EVENT_BUS.addGenericListener(net.minecraft.world.entity.Entity.class, HunterCapability::attach);
        MinecraftForge.EVENT_BUS.addListener(HunterCapability::clone);
        MinecraftForge.EVENT_BUS.register(new SystemEvents());
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.krolasyon.sololeveling.client.ClientSetup.init(bus));
    }

    private void setup(FMLCommonSetupEvent e) {
        e.enqueueWork(Net::init);
    }

    public static ResourceLocation id(String path) { return new ResourceLocation(MODID, path); }
}
