package com.krolasyon.labirent.registry;

import com.krolasyon.labirent.Labirent;
import com.krolasyon.labirent.entity.BeetleBladeEntity;
import com.krolasyon.labirent.entity.BoxElevatorEntity;
import com.krolasyon.labirent.entity.GladerEntity;
import com.krolasyon.labirent.entity.GrieverEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    private ModEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Labirent.MODID);

    public static final RegistryObject<EntityType<GrieverEntity>> GRIEVER = ENTITIES.register("griever",
            () -> EntityType.Builder.of(GrieverEntity::new, MobCategory.MONSTER).sized(2.2F, 1.7F).clientTrackingRange(10).build("griever"));
    public static final RegistryObject<EntityType<GladerEntity>> GLADER = ENTITIES.register("glader",
            () -> EntityType.Builder.of(GladerEntity::new, MobCategory.MISC).sized(0.6F, 1.8F).clientTrackingRange(10).build("glader"));
    public static final RegistryObject<EntityType<BeetleBladeEntity>> BEETLE_BLADE = ENTITIES.register("beetle_blade",
            () -> EntityType.Builder.of(BeetleBladeEntity::new, MobCategory.MISC).sized(0.6F, 0.35F).clientTrackingRange(8).build("beetle_blade"));
    public static final RegistryObject<EntityType<BoxElevatorEntity>> BOX_ELEVATOR = ENTITIES.register("box_elevator",
            () -> EntityType.Builder.<BoxElevatorEntity>of(BoxElevatorEntity::new, MobCategory.MISC).sized(3.8F, 3.0F).clientTrackingRange(10)
                    .updateInterval(1).noSummon().build("box_elevator"));
}
