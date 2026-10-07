package com.krolasyon.furniture.registry;

import com.krolasyon.furniture.FurnitureMod;
import com.krolasyon.furniture.block.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    private ModBlocks() {}

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, FurnitureMod.MODID);

    private static BlockBehaviour.Properties wood() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(2.0F, 3.0F)
                .sound(SoundType.WOOD).noOcclusion();
    }

    public static final RegistryObject<Block> SOFA = BLOCKS.register("sofa", () -> new SofaBlock(wood()));
    public static final RegistryObject<Block> PIANO = BLOCKS.register("piano", () -> new PianoBlock(wood()));
    public static final RegistryObject<Block> CHALKBOARD = BLOCKS.register("chalkboard", () -> new ChalkboardBlock(wood()));
    public static final RegistryObject<Block> NIGHTSTAND = BLOCKS.register("nightstand", () -> new NightstandBlock(wood()));
    public static final RegistryObject<Block> WARDROBE = BLOCKS.register("wardrobe", () -> new WardrobeBlock(wood()));
}
