package com.krolasyon.horror.compat;

import com.krolasyon.horror.HorrorMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Minecraft 1.20.1 specific glue. */
public final class Compat {
    private Compat() {}

    public static ResourceLocation id(String path) { return new ResourceLocation(HorrorMod.MODID, path); }

    public static IEventBus modBus() { return FMLJavaModLoadingContext.get().getModEventBus(); }
}
