package com.krolasyon.bosses.entity.mob;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.LinkedHashMap;
import java.util.Map;

/** Registry of all {@link MobSpec}s; the table itself is generated from gen/roster.py. */
public final class MobSpecs {
    private MobSpecs() {}

    public static final Map<String, MobSpec> ALL = new LinkedHashMap<>();

    static {
        MobSpecTable.fill(ALL);
    }

    public static MobSpec of(EntityType<?> type) {
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(type);
        return ALL.get(key.getPath());
    }
}
