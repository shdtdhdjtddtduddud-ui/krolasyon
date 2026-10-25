package com.krolasyon.labirent.maze;

import com.krolasyon.labirent.Labirent;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

public final class Dims {
    private Dims() {}

    public static final ResourceKey<Level> MAZE = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(Labirent.MODID, "maze"));

    @Nullable
    public static ServerLevel maze(MinecraftServer server) { return server.getLevel(MAZE); }

    public static boolean isMaze(Level level) { return level.dimension() == MAZE; }
}
