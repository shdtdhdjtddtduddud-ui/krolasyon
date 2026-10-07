package com.krolasyon.furniture;

import com.krolasyon.furniture.network.ModNetwork;
import com.krolasyon.furniture.registry.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(FurnitureMod.MODID)
public class FurnitureMod {
    public static final String MODID = "krolasyonfurniture";

    public FurnitureMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(bus);
        ModItems.ITEMS.register(bus);
        ModItems.TABS.register(bus);
        ModBlockEntities.TYPES.register(bus);
        ModEntities.TYPES.register(bus);
        ModMenus.MENUS.register(bus);
        ModSounds.SOUNDS.register(bus);
        ModNetwork.init();
        if (Boolean.getBoolean("krolasyon.furniture.autotest")) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> com.krolasyon.furniture.client.AutoTest::init);
        }
    }
}
