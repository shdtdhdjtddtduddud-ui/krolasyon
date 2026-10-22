package com.krolasyon.smoothterrain;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Yumuşak Arazi: dünya üretilirken bir blokluk her basamağın dibine yarım blok (çimen, toprak, kum, çakıl, taş, kar)
 * koyar; tepeler ve dağlar daha yumuşak görünür, yokuşlar zıplamadan yürünür.
 */
@Mod(SmoothTerrain.MODID)
public final class SmoothTerrain {
    public static final String MODID = "smoothterrain";

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, MODID);
    public static final List<RegistryObject<Item>> SLAB_ITEMS = new ArrayList<>();

    public static final RegistryObject<Block> GRASS_SLAB = slab("grass_slab",
            () -> new GrassSlabBlock(BlockBehaviour.Properties.of().mapColor(MapColor.GRASS).strength(0.6F).sound(SoundType.GRASS)));
    public static final RegistryObject<Block> DIRT_SLAB = slab("dirt_slab", () -> soil(MapColor.DIRT, SoundType.GRAVEL));
    public static final RegistryObject<Block> COARSE_DIRT_SLAB = slab("coarse_dirt_slab", () -> soil(MapColor.DIRT, SoundType.GRAVEL));
    public static final RegistryObject<Block> PODZOL_SLAB = slab("podzol_slab", () -> soil(MapColor.PODZOL, SoundType.GRAVEL));
    public static final RegistryObject<Block> SAND_SLAB = slab("sand_slab", () -> new SlabBlock(BlockBehaviour.Properties.of().mapColor(MapColor.SAND)
            .strength(0.5F).sound(SoundType.SAND)));
    public static final RegistryObject<Block> RED_SAND_SLAB = slab("red_sand_slab", () -> new SlabBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_ORANGE).strength(0.5F).sound(SoundType.SAND)));
    public static final RegistryObject<Block> GRAVEL_SLAB = slab("gravel_slab", () -> new SlabBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE).strength(0.6F).sound(SoundType.GRAVEL)));

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> SMOOTH = FEATURES.register("smooth_steps",
            () -> new SmoothFeature(NoneFeatureConfiguration.CODEC));

    private static Block soil(MapColor c, SoundType s) {
        return new SlabBlock(BlockBehaviour.Properties.of().mapColor(c).strength(0.5F).sound(s));
    }

    private static RegistryObject<Block> slab(String id, java.util.function.Supplier<Block> b) {
        RegistryObject<Block> r = BLOCKS.register(id, b);
        SLAB_ITEMS.add(ITEMS.register(id, () -> new BlockItem(r.get(), new Item.Properties())));
        return r;
    }

    public SmoothTerrain() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus);
        ITEMS.register(bus);
        FEATURES.register(bus);
        bus.addListener(this::creativeTabs);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientColors.init(bus);
            if (Boolean.getBoolean("smoothterrain.autotest")) com.krolasyon.smoothterrain.test.AutoTest.init();
        }
    }

    private void creativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) for (RegistryObject<Item> i : SLAB_ITEMS) event.accept(i);
    }
}
