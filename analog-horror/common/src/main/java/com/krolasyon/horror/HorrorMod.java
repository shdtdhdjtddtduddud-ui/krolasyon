package com.krolasyon.horror;

import com.krolasyon.horror.compat.Compat;
import com.krolasyon.horror.entity.HatmanEntity;
import com.krolasyon.horror.registry.ModBlocks;
import com.krolasyon.horror.registry.ModEntities;
import com.krolasyon.horror.registry.ModItems;
import com.krolasyon.horror.registry.ModSounds;
import com.krolasyon.horror.world.HauntDirector;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(HorrorMod.MODID)
public class HorrorMod {
    public static final String MODID = "krolasyonhorror";

    public HorrorMod() {
        IEventBus bus = Compat.modBus();
        ModEntities.REG.register(bus);
        ModBlocks.REG.register(bus);
        ModBlocks.TABS.register(bus);
        ModItems.REG.register(bus);
        ModSounds.REG.register(bus);
        bus.addListener(HorrorMod::attributes);
        bus.addListener(HorrorMod::creativeTab);
        MinecraftForge.EVENT_BUS.register(HauntDirector.class);
        MinecraftForge.EVENT_BUS.register(com.krolasyon.horror.house.HouseCommands.class);
        MinecraftForge.EVENT_BUS.register(com.krolasyon.horror.story.StoryDirector.class);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.krolasyon.horror.client.ClientSetup.init(bus);
        }
    }

    private static void attributes(EntityAttributeCreationEvent e) {
        e.put(ModEntities.HATMAN.get(), HatmanEntity.createAttributes().build());
    }

    private static void creativeTab(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == CreativeModeTabs.SPAWN_EGGS) e.accept(ModItems.HATMAN_SPAWN_EGG.get());
    }
}
