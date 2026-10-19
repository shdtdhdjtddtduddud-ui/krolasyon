package com.krolasyon.aethercore;

import com.krolasyon.aethercore.block.AetherCoreBlock;
import com.krolasyon.aethercore.block.AetherCoreBlockEntity;
import com.krolasyon.aethercore.block.AetherCoreItem;
import com.krolasyon.aethercore.block.ActiveCores;
import com.krolasyon.aethercore.world.ShrineFeature;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(AetherCore.MODID)
public final class AetherCore {
    public static final String MODID = "aethercore";

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, MODID);

    public static final RegistryObject<Block> AETHER_CORE = BLOCKS.register("aether_core", AetherCoreBlock::new);
    public static final RegistryObject<Item> AETHER_CORE_ITEM = ITEMS.register("aether_core",
            () -> new AetherCoreItem(AETHER_CORE.get(), new Item.Properties().rarity(Rarity.EPIC).fireResistant().stacksTo(1)));
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<AetherCoreBlockEntity>> AETHER_CORE_BE = BLOCK_ENTITIES.register("aether_core",
            () -> BlockEntityType.Builder.of(AetherCoreBlockEntity::new, AETHER_CORE.get()).build(null));
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> ANCIENT_SHRINE =
            FEATURES.register("ancient_shrine", () -> new ShrineFeature(NoneFeatureConfiguration.CODEC));

    public AetherCore() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        FEATURES.register(bus);
        bus.addListener(this::creativeTabs);
        MinecraftForge.EVENT_BUS.addListener(AetherCore::onSpawnCheck);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.krolasyon.aethercore.client.ClientSetup.init(bus);
            if (Boolean.getBoolean("aethercore.autotest")) com.krolasyon.aethercore.test.AutoTest.init();
        }
    }

    private void creativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS || event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(AETHER_CORE_ITEM);
        }
    }

    /** Aktif bir çekirdeğin kubbesi içinde doğal canavar doğmasını engeller. */
    private static void onSpawnCheck(MobSpawnEvent.PositionCheck event) {
        if (!(event.getEntity() instanceof Enemy)) return;
        MobSpawnType type = event.getSpawnType();
        if (type != MobSpawnType.NATURAL && type != MobSpawnType.REINFORCEMENT && type != MobSpawnType.PATROL) return;
        if (!(event.getLevel() instanceof Level level)) return;
        if (ActiveCores.isProtected(level, event.getX(), event.getY(), event.getZ())) event.setResult(Event.Result.DENY);
    }
}
