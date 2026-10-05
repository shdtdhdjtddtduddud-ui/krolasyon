package com.sololeveling.registry;

import com.sololeveling.SoloLeveling;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public final class ModDimensions {
    private ModDimensions() {}

    public static final ResourceKey<Level> HUNTER_WORLD = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(SoloLeveling.MODID, "hunter_world"));
    public static final ResourceKey<Level> DUNGEON = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(SoloLeveling.MODID, "dungeon"));
    public static final int CITY_Y = 72;
}
