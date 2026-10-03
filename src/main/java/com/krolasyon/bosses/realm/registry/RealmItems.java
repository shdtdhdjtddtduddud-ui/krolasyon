package com.krolasyon.bosses.realm.registry;

import com.krolasyon.bosses.KrolasyonBosses;
import com.krolasyon.bosses.realm.Faction;
import com.krolasyon.bosses.realm.item.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class RealmItems {
    private RealmItems() {}

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, KrolasyonBosses.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, KrolasyonBosses.MODID);
    public static final List<RegistryObject<? extends Item>> ORDER = new ArrayList<>();

    static {
        for (RegistryObject<? extends net.minecraft.world.level.block.Block> b : RealmBlocks.WITH_ITEM) {
            ORDER.add(ITEMS.register(b.getId().getPath(), () -> new BlockItem(b.get(), new Item.Properties())));
        }
    }

    private static <T extends Item> RegistryObject<T> reg(String name, Supplier<T> s) {
        RegistryObject<T> r = ITEMS.register(name, s);
        ORDER.add(r);
        return r;
    }

    private static RegistryObject<Item> simple(String name, Rarity rarity) {
        return reg(name, () -> new LoreItem(new Item.Properties().rarity(rarity), false));
    }

    private static RegistryObject<Item> shiny(String name, Rarity rarity, int stack) {
        return reg(name, () -> new LoreItem(new Item.Properties().rarity(rarity).stacksTo(stack), true));
    }

    // story
    public static final RegistryObject<Item> CHRONICLE = reg("crimson_chronicle", () -> new ChronicleItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> EMBER_KEY = reg("ember_key", () -> new LoreItem(new Item.Properties().durability(64).rarity(Rarity.UNCOMMON), true));
    public static final RegistryObject<Item> ASH_SIGIL = shiny("ash_sigil", Rarity.EPIC, 1);
    public static final RegistryObject<Item> BLOOD_SIGIL = shiny("blood_sigil", Rarity.EPIC, 1);
    public static final RegistryObject<Item> SHADOW_SIGIL = shiny("shadow_sigil", Rarity.EPIC, 1);
    public static final RegistryObject<Item> LEGION_SIGIL = shiny("legion_sigil", Rarity.EPIC, 1);
    public static final RegistryObject<Item> SOUL_SIGIL = shiny("soul_sigil", Rarity.EPIC, 1);
    public static final RegistryObject<Item> THRONE_KEY = shiny("throne_key", Rarity.EPIC, 1);
    public static final RegistryObject<Item> TYRANT_HEART = shiny("tyrant_heart", Rarity.EPIC, 16);

    // materials
    public static final RegistryObject<Item> RAW_INFERNAL_STEEL = simple("raw_infernal_steel", Rarity.COMMON);
    public static final RegistryObject<Item> INFERNAL_STEEL_INGOT = simple("infernal_steel_ingot", Rarity.UNCOMMON);
    public static final RegistryObject<Item> ARCANE_CRYSTAL = simple("arcane_crystal", Rarity.UNCOMMON);
    public static final RegistryObject<Item> ASH_ESSENCE = simple("ash_essence", Rarity.COMMON);
    public static final RegistryObject<Item> BLOOD_VIAL = simple("blood_vial", Rarity.COMMON);
    public static final RegistryObject<Item> SHADOW_SHARD = simple("shadow_shard", Rarity.COMMON);
    public static final RegistryObject<Item> BRIMSTONE = simple("brimstone", Rarity.COMMON);
    public static final RegistryObject<Item> SOUL_ESSENCE = simple("soul_essence", Rarity.COMMON);

    public static Item sigil(Faction f) {
        return switch (f) {
            case ASH -> ASH_SIGIL.get();
            case BLOOD -> BLOOD_SIGIL.get();
            case SHADOW -> SHADOW_SIGIL.get();
            case LEGION -> LEGION_SIGIL.get();
            case SOUL -> SOUL_SIGIL.get();
        };
    }

    public static Item essence(Faction f) {
        return switch (f) {
            case ASH -> ASH_ESSENCE.get();
            case BLOOD -> BLOOD_VIAL.get();
            case SHADOW -> SHADOW_SHARD.get();
            case LEGION -> BRIMSTONE.get();
            case SOUL -> SOUL_ESSENCE.get();
        };
    }

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("crimson_realm", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.krolasyonbosses.realm"))
            .icon(() -> new ItemStack(THRONE_KEY.get()))
            .displayItems((params, out) -> {
                for (RegistryObject<? extends Item> r : ORDER) out.accept(r.get());
            })
            .build());
}
