package com.sololeveling.registry;

import com.sololeveling.SoloLeveling;
import com.sololeveling.world.CityFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModFeatures {
    private ModFeatures() {}

    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, SoloLeveling.MODID);
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> CITY = FEATURES.register("city", CityFeature::new);
}
