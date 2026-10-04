package com.rabona.arena;

import com.rabona.arena.net.Net;
import com.rabona.arena.registry.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Rabona Arena - futbol modu.
 * Top fizigi, 40+ hareket, takimlar, botlar ve otomatik stadyum.
 */
@Mod(RabonaArena.MODID)
public class RabonaArena {
    public static final String MODID = "rabonaarena";
    public static final Logger LOG = LoggerFactory.getLogger("RabonaArena");

    public RabonaArena() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(bus);
        ModItems.ITEMS.register(bus);
        ModEntities.ENTITIES.register(bus);
        ModSounds.SOUNDS.register(bus);
        ModTabs.TABS.register(bus);
        bus.addListener(ModEntities::attributes);
        bus.addListener(this::setup);
    }

    private void setup(FMLCommonSetupEvent e) {
        e.enqueueWork(Net::init);
    }

    public static net.minecraft.resources.ResourceLocation id(String path) {
        return new net.minecraft.resources.ResourceLocation(MODID, path);
    }
}
