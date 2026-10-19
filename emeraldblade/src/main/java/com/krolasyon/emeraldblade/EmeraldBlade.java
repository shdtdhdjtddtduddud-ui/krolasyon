package com.krolasyon.emeraldblade;

import com.krolasyon.emeraldblade.entity.EmeraldSpikeEntity;
import com.krolasyon.emeraldblade.entity.RootSnareEntity;
import com.krolasyon.emeraldblade.item.AddItemModifier;
import com.krolasyon.emeraldblade.item.EmeraldRootBladeItem;
import com.mojang.serialization.Codec;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(EmeraldBlade.MODID)
public final class EmeraldBlade {
    public static final String MODID = "emeraldblade";

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, MODID);

    public static final RegistryObject<Item> BLADE = ITEMS.register("emerald_root_blade", EmeraldRootBladeItem::new);

    public static final RegistryObject<EntityType<EmeraldSpikeEntity>> SPIKE = ENTITIES.register("emerald_spike",
            () -> EntityType.Builder.<EmeraldSpikeEntity>of(EmeraldSpikeEntity::new, MobCategory.MISC)
                    .sized(1.0F, 2.0F).clientTrackingRange(8).updateInterval(20).fireImmune().build("emerald_spike"));
    public static final RegistryObject<EntityType<RootSnareEntity>> SNARE = ENTITIES.register("root_snare",
            () -> EntityType.Builder.<RootSnareEntity>of(RootSnareEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F).clientTrackingRange(8).updateInterval(2).fireImmune().build("root_snare"));

    public static final RegistryObject<Codec<AddItemModifier>> ADD_ITEM = LOOT_MODIFIERS.register("add_item", () -> AddItemModifier.CODEC);

    public EmeraldBlade() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(bus);
        ENTITIES.register(bus);
        LOOT_MODIFIERS.register(bus);
        bus.addListener(this::creativeTabs);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.krolasyon.emeraldblade.client.ClientSetup.init(bus);
            if (Boolean.getBoolean("emeraldblade.autotest")) com.krolasyon.emeraldblade.test.AutoTest.init();
        }
    }

    private void creativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) event.accept(BLADE);
    }
}
