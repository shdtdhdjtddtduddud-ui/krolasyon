package com.krolasyon.vocations.registry;

import com.krolasyon.vocations.VocationsMod;
import com.krolasyon.vocations.entity.MagicBoltEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class VocationEntities {
    private VocationEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITIES, VocationsMod.MODID);

    public static final RegistryObject<EntityType<MagicBoltEntity>> MAGIC_BOLT = ENTITY_TYPES.register("magic_bolt",
            () -> EntityType.Builder.<MagicBoltEntity>of(MagicBoltEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10).build("magic_bolt"));
}
