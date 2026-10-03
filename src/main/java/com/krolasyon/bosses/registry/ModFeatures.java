package com.krolasyon.bosses.registry;

import com.krolasyon.bosses.KrolasyonBosses;
import com.krolasyon.bosses.world.feature.DecorFeatures;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModFeatures {
    private ModFeatures() {}

    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, KrolasyonBosses.MODID);

    public static final RegistryObject<Feature<SimpleBlockConfiguration>> SPIRE = FEATURES.register("spire", () -> new DecorFeatures.Spire(SimpleBlockConfiguration.CODEC));
    public static final RegistryObject<Feature<SimpleBlockConfiguration>> DEAD_TREE = FEATURES.register("dead_tree", () -> new DecorFeatures.DeadTree(SimpleBlockConfiguration.CODEC));
    public static final RegistryObject<Feature<SimpleBlockConfiguration>> RIBS = FEATURES.register("ribs", () -> new DecorFeatures.Ribs(SimpleBlockConfiguration.CODEC));
    public static final RegistryObject<Feature<SimpleBlockConfiguration>> RUIN = FEATURES.register("ruin", () -> new DecorFeatures.Ruin(SimpleBlockConfiguration.CODEC));
    public static final RegistryObject<Feature<SimpleBlockConfiguration>> CRYSTALS = FEATURES.register("crystals", () -> new DecorFeatures.Crystals(SimpleBlockConfiguration.CODEC));
}
