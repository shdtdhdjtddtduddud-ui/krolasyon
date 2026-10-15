package com.krolasyon.alien.registry;

import com.krolasyon.alien.AlienMod;
import com.krolasyon.alien.entity.ThrownDeviceEntity;
import com.krolasyon.alien.entity.XenomorphEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    private ModEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, AlienMod.MODID);

    public static final RegistryObject<EntityType<XenomorphEntity>> XENOMORPH = ENTITIES.register("xenomorph",
            () -> EntityType.Builder.of(XenomorphEntity::new, MobCategory.MONSTER)
                    .sized(0.8F, 1.95F).fireImmune().clientTrackingRange(10).updateInterval(2).build("xenomorph"));

    public static final RegistryObject<EntityType<ThrownDeviceEntity>> THROWN_DEVICE = ENTITIES.register("thrown_device",
            () -> EntityType.Builder.<ThrownDeviceEntity>of(ThrownDeviceEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(8).updateInterval(5).build("thrown_device"));
}
