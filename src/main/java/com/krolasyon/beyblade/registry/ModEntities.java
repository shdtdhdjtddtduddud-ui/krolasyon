package com.krolasyon.beyblade.registry;

import com.krolasyon.beyblade.LibraBeyblade;
import com.krolasyon.beyblade.entity.BeybladeEntity;
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
}
