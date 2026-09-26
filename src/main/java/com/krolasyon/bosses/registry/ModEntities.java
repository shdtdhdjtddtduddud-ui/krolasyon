package com.krolasyon.bosses.registry;

import com.krolasyon.bosses.KrolasyonBosses;
import com.krolasyon.bosses.entity.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    private ModEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, KrolasyonBosses.MODID);

    public static final RegistryObject<EntityType<SealWardenEntity>> SEAL_WARDEN = ENTITIES.register("seal_warden",
            () -> EntityType.Builder.of(SealWardenEntity::new, MobCategory.MONSTER)
                    .sized(1.4F, 3.7F).fireImmune().clientTrackingRange(12).build("seal_warden"));

    public static final RegistryObject<EntityType<CrimsonHoundEntity>> CRIMSON_HOUND = ENTITIES.register("crimson_hound",
            () -> EntityType.Builder.of(CrimsonHoundEntity::new, MobCategory.MONSTER)
                    .sized(2.2F, 2.3F).fireImmune().clientTrackingRange(12).build("crimson_hound"));

    public static final RegistryObject<EntityType<EruptionEntity>> ERUPTION = ENTITIES.register("eruption",
            () -> EntityType.Builder.<EruptionEntity>of(EruptionEntity::new, MobCategory.MISC)
                    .sized(1.2F, 2.0F).fireImmune().clientTrackingRange(8).updateInterval(2).build("eruption"));

    public static final RegistryObject<EntityType<SealPrisonEntity>> SEAL_PRISON = ENTITIES.register("seal_prison",
            () -> EntityType.Builder.<SealPrisonEntity>of(SealPrisonEntity::new, MobCategory.MISC)
                    .sized(7.0F, 0.5F).fireImmune().clientTrackingRange(8).updateInterval(2).build("seal_prison"));

    public static final RegistryObject<EntityType<WatcherOrbEntity>> WATCHER = ENTITIES.register("watcher",
            () -> EntityType.Builder.<WatcherOrbEntity>of(WatcherOrbEntity::new, MobCategory.MISC)
                    .sized(0.8F, 0.8F).fireImmune().clientTrackingRange(10).updateInterval(1).build("watcher"));

    public static final RegistryObject<EntityType<SealBoltEntity>> SEAL_BOLT = ENTITIES.register("seal_bolt",
            () -> EntityType.Builder.<SealBoltEntity>of(SealBoltEntity::new, MobCategory.MISC)
                    .sized(0.4F, 0.4F).clientTrackingRange(8).updateInterval(1).build("seal_bolt"));

    public static final RegistryObject<EntityType<HellfireBallEntity>> HELLFIRE_BALL = ENTITIES.register("hellfire_ball",
            () -> EntityType.Builder.<HellfireBallEntity>of(HellfireBallEntity::new, MobCategory.MISC)
                    .sized(0.8F, 0.8F).fireImmune().clientTrackingRange(8).updateInterval(1).build("hellfire_ball"));

    public static final RegistryObject<EntityType<RevengeEntity>> REVENGE = ENTITIES.register("revenge",
            () -> EntityType.Builder.of(RevengeEntity::new, MobCategory.MONSTER)
                    .sized(1.3F, 4.0F).fireImmune().clientTrackingRange(12).build("revenge"));

    public static final RegistryObject<EntityType<HeartDemonEntity>> HEART_DEMON = ENTITIES.register("heart_demon",
            () -> EntityType.Builder.of(HeartDemonEntity::new, MobCategory.MONSTER)
                    .sized(1.2F, 3.3F).fireImmune().clientTrackingRange(12).build("heart_demon"));

    public static final RegistryObject<EntityType<BloodLanceEntity>> BLOOD_LANCE = ENTITIES.register("blood_lance",
            () -> EntityType.Builder.<BloodLanceEntity>of(BloodLanceEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(8).updateInterval(1).build("blood_lance"));

    public static final RegistryObject<EntityType<HeartOrbEntity>> HEART_ORB = ENTITIES.register("heart_orb",
            () -> EntityType.Builder.<HeartOrbEntity>of(HeartOrbEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(8).updateInterval(1).build("heart_orb"));
}
