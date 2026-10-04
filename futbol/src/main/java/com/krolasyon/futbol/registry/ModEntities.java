package com.krolasyon.futbol.registry;

import com.krolasyon.futbol.FutbolMod;
import com.krolasyon.futbol.entity.FootballEntity;
import com.krolasyon.futbol.entity.FootballerEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    private ModEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, FutbolMod.MODID);

    public static final RegistryObject<EntityType<FootballEntity>> BALL = ENTITIES.register("football",
            () -> EntityType.Builder.<FootballEntity>of(FootballEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(12).updateInterval(1).build("football"));

    public static final RegistryObject<EntityType<FootballerEntity>> FOOTBALLER = ENTITIES.register("footballer",
            () -> EntityType.Builder.of(FootballerEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F).clientTrackingRange(12).updateInterval(2).build("footballer"));
}
