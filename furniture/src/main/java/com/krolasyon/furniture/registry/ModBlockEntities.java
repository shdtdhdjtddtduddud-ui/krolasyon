package com.krolasyon.furniture.registry;

import com.krolasyon.furniture.FurnitureMod;
import com.krolasyon.furniture.blockentity.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    private ModBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, FurnitureMod.MODID);

    public static final RegistryObject<BlockEntityType<SofaBlockEntity>> SOFA = TYPES.register("sofa", () ->
            BlockEntityType.Builder.of(SofaBlockEntity::new, ModBlocks.SOFA.get()).build(null));
    public static final RegistryObject<BlockEntityType<PianoBlockEntity>> PIANO = TYPES.register("piano", () ->
            BlockEntityType.Builder.of(PianoBlockEntity::new, ModBlocks.PIANO.get()).build(null));
    public static final RegistryObject<BlockEntityType<ChalkboardBlockEntity>> CHALKBOARD = TYPES.register("chalkboard", () ->
            BlockEntityType.Builder.of(ChalkboardBlockEntity::new, ModBlocks.CHALKBOARD.get()).build(null));
    public static final RegistryObject<BlockEntityType<NightstandBlockEntity>> NIGHTSTAND = TYPES.register("nightstand", () ->
            BlockEntityType.Builder.of(NightstandBlockEntity::new, ModBlocks.NIGHTSTAND.get()).build(null));
    public static final RegistryObject<BlockEntityType<WardrobeBlockEntity>> WARDROBE = TYPES.register("wardrobe", () ->
            BlockEntityType.Builder.of(WardrobeBlockEntity::new, ModBlocks.WARDROBE.get()).build(null));
}
