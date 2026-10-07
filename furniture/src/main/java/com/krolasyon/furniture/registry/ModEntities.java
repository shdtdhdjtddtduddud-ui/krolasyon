package com.krolasyon.furniture.registry;

import com.krolasyon.furniture.FurnitureMod;
import com.krolasyon.furniture.entity.SeatEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    private ModEntities() {}

    public static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, FurnitureMod.MODID);

    public static final RegistryObject<EntityType<SeatEntity>> SEAT = TYPES.register("seat", () ->
            EntityType.Builder.<SeatEntity>of(SeatEntity::new, MobCategory.MISC)
                    .sized(0.01F, 0.01F).fireImmune().noSave().clientTrackingRange(10).updateInterval(20)
                    .setShouldReceiveVelocityUpdates(false).build("seat"));
}
