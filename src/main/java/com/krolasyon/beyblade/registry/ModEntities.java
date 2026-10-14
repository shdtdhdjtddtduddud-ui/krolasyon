package com.krolasyon.beyblade.registry;

import com.krolasyon.beyblade.LibraBeyblade;
import com.krolasyon.beyblade.entity.BeybladeEntity;
import com.krolasyon.beyblade.entity.CommonBeybladeEntity;
import com.krolasyon.beyblade.entity.LDragoEntity;
import com.krolasyon.beyblade.entity.LeoneEntity;
import com.krolasyon.beyblade.entity.EagleEntity;
import com.krolasyon.beyblade.entity.PegasusEntity;
import com.krolasyon.beyblade.entity.TempoEntity;
import com.krolasyon.beyblade.entity.VariaresEntity;
import com.krolasyon.beyblade.entity.StadiumEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    private ModEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, LibraBeyblade.MODID);

    public static final RegistryObject<EntityType<BeybladeEntity>> LIBRA = ENTITIES.register("libra",
            () -> EntityType.Builder.<BeybladeEntity>of(BeybladeEntity::new, MobCategory.MISC)
                    .sized(BeybladeEntity.WIDTH, BeybladeEntity.HEIGHT).fireImmune()
                    .clientTrackingRange(10).updateInterval(1).setShouldReceiveVelocityUpdates(true)
                    .build("libra"));

    public static final RegistryObject<EntityType<LDragoEntity>> LDRAGO = ENTITIES.register("ldrago",
            () -> EntityType.Builder.<LDragoEntity>of(LDragoEntity::new, MobCategory.MISC)
                    .sized(BeybladeEntity.WIDTH, BeybladeEntity.HEIGHT).fireImmune()
                    .clientTrackingRange(10).updateInterval(1).setShouldReceiveVelocityUpdates(true)
                    .build("ldrago"));

    public static final RegistryObject<EntityType<LeoneEntity>> LEONE = ENTITIES.register("leone",
            () -> EntityType.Builder.<LeoneEntity>of(LeoneEntity::new, MobCategory.MISC)
                    .sized(BeybladeEntity.WIDTH, BeybladeEntity.HEIGHT).fireImmune()
                    .clientTrackingRange(10).updateInterval(1).setShouldReceiveVelocityUpdates(true)
                    .build("leone"));

    public static final RegistryObject<EntityType<StadiumEntity>> STADIUM = ENTITIES.register("stadium",
            () -> EntityType.Builder.<StadiumEntity>of(StadiumEntity::new, MobCategory.MISC)
                    .sized(1.4F, 0.5F).fireImmune().clientTrackingRange(16).updateInterval(40)
                    .build("stadium"));

    public static final RegistryObject<EntityType<EagleEntity>> EAGLE = ENTITIES.register("eagle",
            () -> EntityType.Builder.<EagleEntity>of(EagleEntity::new, MobCategory.MISC)
                    .sized(BeybladeEntity.WIDTH, BeybladeEntity.HEIGHT).fireImmune()
                    .clientTrackingRange(10).updateInterval(1).setShouldReceiveVelocityUpdates(true)
                    .build("eagle"));

    public static final RegistryObject<EntityType<PegasusEntity>> PEGASUS = ENTITIES.register("pegasus",
            () -> EntityType.Builder.<PegasusEntity>of(PegasusEntity::new, MobCategory.MISC)
                    .sized(BeybladeEntity.WIDTH, BeybladeEntity.HEIGHT).fireImmune()
                    .clientTrackingRange(10).updateInterval(1).setShouldReceiveVelocityUpdates(true)
                    .build("pegasus"));

    public static final RegistryObject<EntityType<TempoEntity>> TEMPO = ENTITIES.register("tempo",
            () -> EntityType.Builder.<TempoEntity>of(TempoEntity::new, MobCategory.MISC)
                    .sized(BeybladeEntity.WIDTH, BeybladeEntity.HEIGHT).fireImmune()
                    .clientTrackingRange(10).updateInterval(1).setShouldReceiveVelocityUpdates(true)
                    .build("tempo"));

    public static final RegistryObject<EntityType<VariaresEntity>> VARIARES = ENTITIES.register("variares",
            () -> EntityType.Builder.<VariaresEntity>of(VariaresEntity::new, MobCategory.MISC)
                    .sized(BeybladeEntity.WIDTH, BeybladeEntity.HEIGHT).fireImmune()
                    .clientTrackingRange(10).updateInterval(1).setShouldReceiveVelocityUpdates(true)
                    .build("variares"));

    public static final RegistryObject<EntityType<CommonBeybladeEntity>> COMMON = ENTITIES.register("common",
            () -> EntityType.Builder.<CommonBeybladeEntity>of(CommonBeybladeEntity::new, MobCategory.MISC)
                    .sized(BeybladeEntity.WIDTH, BeybladeEntity.HEIGHT).fireImmune()
                    .clientTrackingRange(10).updateInterval(1).setShouldReceiveVelocityUpdates(true)
                    .build("common"));
}
