package com.krolasyon.skyislands;

import com.krolasyon.skyislands.item.IslandSeedItem;
import com.krolasyon.skyislands.world.SkyIslandFeature;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(SkyIslands.MODID)
public final class SkyIslands {
    public static final String MODID = "skyislands";

    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> SKY_ISLANDS =
            FEATURES.register("sky_islands", () -> new SkyIslandFeature(NoneFeatureConfiguration.CODEC));
    public static final RegistryObject<Item> ISLAND_SEED =
            ITEMS.register("island_seed", () -> new IslandSeedItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));

    public SkyIslands() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        FEATURES.register(bus);
        ITEMS.register(bus);
        bus.addListener(this::creativeTabs);
        if (FMLEnvironment.dist == Dist.CLIENT && Boolean.getBoolean("skyislands.autotest")) {
            com.krolasyon.skyislands.test.AutoTest.init();
        }
    }

    private void creativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) event.accept(ISLAND_SEED);
    }
}
