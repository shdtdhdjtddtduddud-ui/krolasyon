package com.rabona.arena.registry;

import com.rabona.arena.RabonaArena;
import com.rabona.arena.entity.BallEntity;
import com.rabona.arena.entity.FootballerEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, RabonaArena.MODID);

    public static final RegistryObject<EntityType<BallEntity>> BALL = ENTITIES.register("ball",
            () -> EntityType.Builder.<BallEntity>of(BallEntity::new, MobCategory.MISC)
                    .sized(BallEntity.RADIUS * 2, BallEntity.RADIUS * 2)
                    .setTrackingRange(12).setUpdateInterval(1).setShouldReceiveVelocityUpdates(true)
                    .build("ball"));

    public static final RegistryObject<EntityType<FootballerEntity>> FOOTBALLER = ENTITIES.register("footballer",
            () -> EntityType.Builder.<FootballerEntity>of(FootballerEntity::new, MobCategory.MISC)
                    .sized(0.6f, 1.8f).setTrackingRange(12).setUpdateInterval(2)
                    .build("footballer"));

    public static void attributes(EntityAttributeCreationEvent e) {
        e.put(FOOTBALLER.get(), FootballerEntity.createAttributes().build());
    }
}
