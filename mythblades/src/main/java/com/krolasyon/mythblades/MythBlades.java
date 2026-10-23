package com.krolasyon.mythblades;

import com.krolasyon.mythblades.entity.RainbowWaveEntity;
import com.krolasyon.mythblades.item.AddItemModifier;
import com.krolasyon.mythblades.item.CrimsonVeinItem;
import com.krolasyon.mythblades.item.MossrootCleaverItem;
import com.krolasyon.mythblades.item.RainbowScimitarItem;
import com.mojang.serialization.Codec;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(MythBlades.MODID)
public final class MythBlades {
    public static final String MODID = "mythblades";

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);
    public static final DeferredRegister<net.minecraft.world.level.block.Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    public static final DeferredRegister<net.minecraft.world.level.levelgen.feature.Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, MODID);
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, MODID);

    public static final RegistryObject<Item> RAINBOW = ITEMS.register("rainbow_scimitar", RainbowScimitarItem::new);
    public static final RegistryObject<Item> CRIMSON = ITEMS.register("crimson_vein", CrimsonVeinItem::new);
    public static final RegistryObject<Item> MOSSROOT = ITEMS.register("mossroot_cleaver", MossrootCleaverItem::new);
    public static final RegistryObject<Item> EMBER = ITEMS.register("emberbrand", com.krolasyon.mythblades.item.EmberbrandItem::new);
    public static final RegistryObject<Item> SEAL = ITEMS.register("ember_seal", com.krolasyon.mythblades.item.EmberSealItem::new);

    public static final RegistryObject<com.krolasyon.mythblades.block.SwordAltarBlock> ALTAR = BLOCKS.register("sword_altar",
            com.krolasyon.mythblades.block.SwordAltarBlock::new);
    public static final RegistryObject<Item> ALTAR_ITEM = ITEMS.register("sword_altar",
            () -> new net.minecraft.world.item.BlockItem(ALTAR.get(), new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC)));
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<com.krolasyon.mythblades.block.SwordAltarBlockEntity>> ALTAR_BE =
            BLOCK_ENTITIES.register("sword_altar", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder
                    .of(com.krolasyon.mythblades.block.SwordAltarBlockEntity::new, ALTAR.get()).build(null));
    public static final RegistryObject<com.krolasyon.mythblades.world.AltarFeature> ALTAR_FEATURE = FEATURES.register("ember_altar",
            () -> new com.krolasyon.mythblades.world.AltarFeature(net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));

    public static final RegistryObject<EntityType<RainbowWaveEntity>> WAVE = ENTITIES.register("rainbow_wave",
            () -> EntityType.Builder.<RainbowWaveEntity>of(RainbowWaveEntity::new, MobCategory.MISC)
                    .sized(1.6F, 0.6F).clientTrackingRange(8).updateInterval(2).fireImmune().build("rainbow_wave"));

    public static final RegistryObject<EntityType<com.krolasyon.mythblades.entity.EmberWaveEntity>> EMBER_WAVE = ENTITIES.register("ember_wave",
            () -> EntityType.Builder.<com.krolasyon.mythblades.entity.EmberWaveEntity>of(com.krolasyon.mythblades.entity.EmberWaveEntity::new, MobCategory.MISC)
                    .sized(2.0F, 0.8F).clientTrackingRange(8).updateInterval(1).fireImmune().build("ember_wave"));

    public static final DeferredRegister<net.minecraft.sounds.SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MODID);
    public static final java.util.Map<String, RegistryObject<net.minecraft.sounds.SoundEvent>> SFX = new java.util.HashMap<>();

    static {
        for (String s : new String[]{"ember_swing1", "ember_swing2", "ember_swing3", "ember_dash", "ember_parry", "ember_block", "ember_impact", "ember_wave"}) {
            SFX.put(s, SOUNDS.register(s, () -> net.minecraft.sounds.SoundEvent.createVariableRangeEvent(new net.minecraft.resources.ResourceLocation(MODID, s))));
        }
    }

    public static final RegistryObject<Codec<AddItemModifier>> ADD_ITEM = LOOT_MODIFIERS.register("add_item", () -> AddItemModifier.CODEC);

    public MythBlades() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(bus);
        BLOCKS.register(bus);
        BLOCK_ENTITIES.register(bus);
        FEATURES.register(bus);
        SOUNDS.register(bus);
        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent e) -> e.enqueueWork(com.krolasyon.mythblades.network.FxNet::register));
        MinecraftForge.EVENT_BUS.addListener(com.krolasyon.mythblades.combat.EmberCombat::onPlayerTick);
        MinecraftForge.EVENT_BUS.addListener(com.krolasyon.mythblades.combat.EmberCombat::onAttack);
        MinecraftForge.EVENT_BUS.addListener(com.krolasyon.mythblades.combat.EmberCombat::onHurt);
        MinecraftForge.EVENT_BUS.addListener(com.krolasyon.mythblades.combat.EmberCombat::onFall);
        ENTITIES.register(bus);
        LOOT_MODIFIERS.register(bus);
        bus.addListener(this::creativeTabs);
        MinecraftForge.EVENT_BUS.addListener(MythBlades::onFall);
        MinecraftForge.EVENT_BUS.addListener(MythBlades::onServerTick);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.krolasyon.mythblades.client.ClientSetup.init(bus);
            if (Boolean.getBoolean("mythblades.autotest")) com.krolasyon.mythblades.test.AutoTest.init();
        }
    }

    private void creativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(RAINBOW);
            event.accept(CRIMSON);
            event.accept(MOSSROOT);
            event.accept(EMBER);
            event.accept(SEAL);
            event.accept(ALTAR_ITEM);
        }
    }

    /** Gökkuşağı Palası elindeyken bulutlar seni taşır: düşme hasarı yok. */
    private static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player p && (p.getMainHandItem().is(RAINBOW.get()) || p.getOffhandItem().is(RAINBOW.get()))) {
            event.setDamageMultiplier(0F);
        }
    }

    private static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) MossrootCleaverItem.tickWaves();
    }
}
