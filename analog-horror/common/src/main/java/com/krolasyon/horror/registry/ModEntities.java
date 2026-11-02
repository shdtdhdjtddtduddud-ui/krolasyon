package com.krolasyon.horror.registry;

import com.krolasyon.horror.HorrorMod;
import com.krolasyon.horror.entity.HatmanEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> REG = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, HorrorMod.MODID);

    public static final RegistryObject<EntityType<HatmanEntity>> HATMAN = REG.register("hatman",
            () -> EntityType.Builder.of(HatmanEntity::new, MobCategory.MONSTER)
                    .sized(0.8F, 3.2F).clientTrackingRange(16).updateInterval(2).build("hatman"));

    private ModEntities() {}
}
