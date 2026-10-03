package com.krolasyon.bosses.realm;

import com.krolasyon.bosses.KrolasyonBosses;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public final class Realm {
    private Realm() {}

    public static final ResourceKey<Level> REALM = ResourceKey.create(Registries.DIMENSION, rl("crimson_realm"));

    public static ResourceLocation rl(String path) { return new ResourceLocation(KrolasyonBosses.MODID, path); }

    public static boolean inRealm(Level level) { return level.dimension() == REALM; }
}
