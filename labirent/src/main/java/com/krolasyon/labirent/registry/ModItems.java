package com.krolasyon.labirent.registry;

import com.krolasyon.labirent.Labirent;
import com.krolasyon.labirent.item.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class ModItems {
    private ModItems() {}

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Labirent.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Labirent.MODID);
    private static final List<RegistryObject<? extends Item>> ORDER = new ArrayList<>();

    static void blockItem(String name, RegistryObject<? extends Block> block) {
        ORDER.add(ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties())));
    }

    private static <T extends Item> RegistryObject<T> reg(String name, Supplier<T> s) {
        RegistryObject<T> r = ITEMS.register(name, s);
        ORDER.add(r);
        return r;
    }

    public static final RegistryObject<Item> JOURNAL = reg("glade_journal", () -> new JournalItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> CAPTAIN_BATON = reg("captain_baton", () -> new CaptainBatonItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> RUNNER_MAP = reg("runner_map", () -> new RunnerMapItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> GRIEF_SERUM = reg("grief_serum", () -> new SerumItem(new Item.Properties().stacksTo(8).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> BANDAGE = reg("bandage", () -> new BandageItem(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> GRIEVER_STINGER = reg("griever_stinger", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> GRIEVER_PLATE = reg("griever_plate", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> GRIEVER_CORE = reg("griever_core", () -> new GrieverCoreItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> BEETLE_EYE = reg("beetle_eye", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CODE_WORD = reg("code_word", () -> new CodeWordItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> MACHETE = reg("machete", () -> new SwordItem(Tiers.IRON, 4, -2.2F, new Item.Properties()));
    public static final RegistryObject<Item> SPEAR = reg("glade_spear", () -> new SpearItem(new Item.Properties().durability(400)));
    public static final RegistryObject<Item> FRYING_PAN = reg("frying_pan", () -> new FryingPanItem(new Item.Properties().durability(500)));
    public static final RegistryObject<Item> GLADE_STEW = reg("glade_stew", () -> new GladeFoodItem(new Item.Properties().stacksTo(16)
            .food(new FoodProperties.Builder().nutrition(10).saturationMod(1.0F)
                    .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 100, 0), 1.0F).build()), true));
    public static final RegistryObject<Item> GLADE_BREAD = reg("glade_bread", () -> new GladeFoodItem(new Item.Properties()
            .food(new FoodProperties.Builder().nutrition(6).saturationMod(0.8F).build()), false));
    public static final RegistryObject<Item> FRYPAN_BREAKFAST = reg("frypan_breakfast", () -> new GladeFoodItem(new Item.Properties().stacksTo(16)
            .food(new FoodProperties.Builder().nutrition(12).saturationMod(1.2F)
                    .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1200, 0), 1.0F).build()), true));
    public static final RegistryObject<Item> GRIEVER_SPAWN_EGG = reg("griever_spawn_egg", () -> new ForgeSpawnEggItem(ModEntities.GRIEVER, 0x2B2B2B, 0x8FA03A, new Item.Properties()));
    public static final RegistryObject<Item> GLADER_SPAWN_EGG = reg("glader_spawn_egg", () -> new ForgeSpawnEggItem(ModEntities.GLADER, 0x6B8F4A, 0xC8A27A, new Item.Properties()));
    public static final RegistryObject<Item> BEETLE_SPAWN_EGG = reg("beetle_blade_spawn_egg", () -> new ForgeSpawnEggItem(ModEntities.BEETLE_BLADE, 0x3A3A40, 0xFF2020, new Item.Properties()));

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("labirent", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.labirent"))
            .icon(() -> new ItemStack(CAPTAIN_BATON.get()))
            .displayItems((params, out) -> {
                for (RegistryObject<? extends Item> r : ORDER) out.accept(r.get());
            })
            .build());
}
