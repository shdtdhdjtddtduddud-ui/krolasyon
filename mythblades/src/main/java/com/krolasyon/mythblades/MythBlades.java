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
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, MODID);

    public static final RegistryObject<Item> RAINBOW = ITEMS.register("rainbow_scimitar", RainbowScimitarItem::new);
    public static final RegistryObject<Item> CRIMSON = ITEMS.register("crimson_vein", CrimsonVeinItem::new);
    public static final RegistryObject<Item> MOSSROOT = ITEMS.register("mossroot_cleaver", MossrootCleaverItem::new);

    public static final RegistryObject<EntityType<RainbowWaveEntity>> WAVE = ENTITIES.register("rainbow_wave",
            () -> EntityType.Builder.<RainbowWaveEntity>of(RainbowWaveEntity::new, MobCategory.MISC)
                    .sized(1.6F, 0.6F).clientTrackingRange(8).updateInterval(2).fireImmune().build("rainbow_wave"));

    public static final RegistryObject<Codec<AddItemModifier>> ADD_ITEM = LOOT_MODIFIERS.register("add_item", () -> AddItemModifier.CODEC);

    public MythBlades() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(bus);
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
