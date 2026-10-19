package com.krolasyon.sylvanarchery;

import com.krolasyon.sylvanarchery.entity.SylvanArrowEntity;
import com.krolasyon.sylvanarchery.item.AddItemModifier;
import com.krolasyon.sylvanarchery.item.QuiverItem;
import com.krolasyon.sylvanarchery.item.SylvanArrowItem;
import com.krolasyon.sylvanarchery.item.SylvanBowItem;
import com.mojang.serialization.Codec;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
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

@Mod(SylvanArchery.MODID)
public final class SylvanArchery {
    public static final String MODID = "sylvanarchery";

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, MODID);

    public static final RegistryObject<Item> BOW = ITEMS.register("sylvan_bow",
            () -> new SylvanBowItem(new Item.Properties().durability(1200).rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> ARROW = ITEMS.register("sylvan_arrow",
            () -> new SylvanArrowItem(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> QUIVER = ITEMS.register("sylvan_quiver",
            () -> new QuiverItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static final RegistryObject<EntityType<SylvanArrowEntity>> ARROW_ENTITY = ENTITIES.register("sylvan_arrow",
            () -> EntityType.Builder.<SylvanArrowEntity>of(SylvanArrowEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(4).updateInterval(20).build("sylvan_arrow"));

    public static final RegistryObject<Codec<AddItemModifier>> ADD_ITEM = LOOT_MODIFIERS.register("add_item", () -> AddItemModifier.CODEC);

    public SylvanArchery() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(bus);
        ENTITIES.register(bus);
        LOOT_MODIFIERS.register(bus);
        bus.addListener(this::creativeTabs);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.krolasyon.sylvanarchery.client.ClientSetup.init(bus);
            if (Boolean.getBoolean("sylvanarchery.autotest")) com.krolasyon.sylvanarchery.test.AutoTest.init();
        }
    }

    private void creativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(BOW);
            event.accept(ARROW);
            event.accept(QUIVER);
        }
    }
}
