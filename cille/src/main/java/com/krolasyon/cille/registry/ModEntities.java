package com.krolasyon.cille.registry;

import com.krolasyon.cille.CilleMod;
import com.krolasyon.cille.entity.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    private ModEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, CilleMod.MODID);

    public static final RegistryObject<EntityType<KabkabaEntity>> KABKABA = ENTITIES.register("kabkaba",
            () -> EntityType.Builder.of(KabkabaEntity::new, MobCategory.CREATURE)
                    .sized(2.4F, 2.9F).fireImmune().clientTrackingRange(12).build("kabkaba"));

    public static final RegistryObject<EntityType<MarbleEntity>> MARBLE = ENTITIES.register("marble",
            () -> EntityType.Builder.<MarbleEntity>of(MarbleEntity::new, MobCategory.MISC)
                    .sized(0.3F, 0.3F).clientTrackingRange(8).updateInterval(1).build("marble"));

    public static final RegistryObject<EntityType<BlackHoleEntity>> BLACK_HOLE = ENTITIES.register("black_hole",
            () -> EntityType.Builder.<BlackHoleEntity>of(BlackHoleEntity::new, MobCategory.MISC)
                    .sized(1.5F, 1.5F).fireImmune().clientTrackingRange(10).updateInterval(2).build("black_hole"));

    public static final RegistryObject<EntityType<ShadowSpikeEntity>> SHADOW_SPIKE = ENTITIES.register("shadow_spike",
            () -> EntityType.Builder.<ShadowSpikeEntity>of(ShadowSpikeEntity::new, MobCategory.MISC)
                    .sized(1.0F, 2.2F).fireImmune().clientTrackingRange(8).updateInterval(2).build("shadow_spike"));
}
