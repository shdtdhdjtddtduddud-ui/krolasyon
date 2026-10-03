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
    public static final RegistryObject<Item> THRONE_KEY = reg("throne_key", () -> new ThroneKeyItem(new Item.Properties().rarity(Rarity.EPIC).stacksTo(1).fireResistant()));
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

    public static final RegistryObject<Item> MANA_POTION = reg("mana_potion", () -> new ManaPotionItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> ALLY_BANNER = reg("ally_banner", () -> new AllyBannerItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> ASH_WAR_HORN = horn("ash_war_horn", Faction.ASH);
    public static final RegistryObject<Item> BLOOD_WAR_HORN = horn("blood_war_horn", Faction.BLOOD);
    public static final RegistryObject<Item> SHADOW_WAR_HORN = horn("shadow_war_horn", Faction.SHADOW);
    public static final RegistryObject<Item> LEGION_WAR_HORN = horn("legion_war_horn", Faction.LEGION);
    public static final RegistryObject<Item> SOUL_WAR_HORN = horn("soul_war_horn", Faction.SOUL);

    private static RegistryObject<Item> horn(String name, Faction f) {
        return reg(name, () -> new WarHornItem(new Item.Properties().stacksTo(4).rarity(Rarity.RARE), f));
    }

    // weapons
    public static final RegistryObject<Item> INFERNAL_STEEL_SWORD = reg("infernal_steel_sword", () -> new RealmWeapon(RealmTiers.INFERNAL, 3, -2.4F,
            new Item.Properties().fireResistant(), RealmWeapon.Kind.STEEL));
    public static final RegistryObject<Item> ASHBRINGER = reg("ashbringer", () -> new RealmWeapon(RealmTiers.LEGENDARY, 5, -2.8F,
            new Item.Properties().fireResistant().rarity(Rarity.EPIC), RealmWeapon.Kind.ASHBRINGER));
    public static final RegistryObject<Item> BLOODTHIRSTER = reg("bloodthirster", () -> new RealmWeapon(RealmTiers.LEGENDARY, 3, -2.2F,
            new Item.Properties().fireResistant().rarity(Rarity.EPIC), RealmWeapon.Kind.BLOODTHIRSTER));
    public static final RegistryObject<Item> SHADOWFANG = reg("shadowfang", () -> new RealmWeapon(RealmTiers.LEGENDARY, 1, -1.6F,
            new Item.Properties().fireResistant().rarity(Rarity.EPIC), RealmWeapon.Kind.SHADOWFANG));
    public static final RegistryObject<Item> WARLORD_HAMMER = reg("warlord_hammer", () -> new RealmWeapon(RealmTiers.LEGENDARY, 8, -3.3F,
            new Item.Properties().fireResistant().rarity(Rarity.EPIC), RealmWeapon.Kind.HAMMER));
    public static final RegistryObject<Item> SOUL_REAPER = reg("soul_reaper", () -> new RealmWeapon(RealmTiers.LEGENDARY, 5, -2.9F,
            new Item.Properties().fireResistant().rarity(Rarity.EPIC), RealmWeapon.Kind.REAPER));
    public static final RegistryObject<Item> SOVEREIGN_BLADE = reg("sovereign_blade", () -> new RealmWeapon(RealmTiers.LEGENDARY, 7, -2.5F,
            new Item.Properties().fireResistant().rarity(Rarity.EPIC), RealmWeapon.Kind.SOVEREIGN));

    // tomes
    public static final RegistryObject<Item> TOME_FIREBALL = tome("tome_fireball", Spells.Spell.FIREBALL);
    public static final RegistryObject<Item> TOME_METEOR = tome("tome_meteor", Spells.Spell.METEOR);
    public static final RegistryObject<Item> TOME_BLOOD_LANCE = tome("tome_blood_lance", Spells.Spell.BLOOD_LANCE);
    public static final RegistryObject<Item> TOME_SHADOW_STEP = tome("tome_shadow_step", Spells.Spell.SHADOW_STEP);
    public static final RegistryObject<Item> TOME_SOUL_SHIELD = tome("tome_soul_shield", Spells.Spell.SOUL_SHIELD);
    public static final RegistryObject<Item> TOME_CHAIN_LIGHTNING = tome("tome_chain_lightning", Spells.Spell.CHAIN_LIGHTNING);
    public static final RegistryObject<Item> TOME_LAVA_WAVE = tome("tome_lava_wave", Spells.Spell.LAVA_WAVE);
    public static final RegistryObject<Item> TOME_SUMMON_IMPS = tome("tome_summon_imps", Spells.Spell.SUMMON_IMPS);
    public static final RegistryObject<Item> TOME_LIFE_DRAIN = tome("tome_life_drain", Spells.Spell.LIFE_DRAIN);
    public static final RegistryObject<Item> TOME_FEAR = tome("tome_fear", Spells.Spell.FEAR);

    private static RegistryObject<Item> tome(String name, Spells.Spell s) {
        return reg(name, () -> new TomeItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), s));
    }

    // armour
    public static final RegistryObject<Item> INFERNAL_STEEL_HELMET = armor("infernal_steel_helmet", ArmorItem.Type.HELMET);
    public static final RegistryObject<Item> INFERNAL_STEEL_CHESTPLATE = armor("infernal_steel_chestplate", ArmorItem.Type.CHESTPLATE);
    public static final RegistryObject<Item> INFERNAL_STEEL_LEGGINGS = armor("infernal_steel_leggings", ArmorItem.Type.LEGGINGS);
    public static final RegistryObject<Item> INFERNAL_STEEL_BOOTS = armor("infernal_steel_boots", ArmorItem.Type.BOOTS);
    public static final RegistryObject<Item> SOVEREIGN_CROWN = reg("sovereign_crown", () -> new ArmorItem(RealmTiers.SOVEREIGN, ArmorItem.Type.HELMET,
            new Item.Properties().fireResistant().rarity(Rarity.EPIC)) {
        @Override
        public void appendHoverText(ItemStack stack, @javax.annotation.Nullable net.minecraft.world.level.Level level, java.util.List<Component> lines, TooltipFlag flag) {
            LoreItem.addLore(getDescriptionId(), lines);
        }
    });

    private static RegistryObject<Item> armor(String name, ArmorItem.Type t) {
        return reg(name, () -> new ArmorItem(RealmTiers.INFERNAL_ARMOR, t, new Item.Properties().fireResistant()));
    }

    // spawn eggs
    static {
        for (com.krolasyon.bosses.realm.entity.MobSpec m : com.krolasyon.bosses.realm.entity.GenSpecs.ALL) {
            var type = com.krolasyon.bosses.realm.entity.RealmEntities.TYPES.get(m.id());
            ORDER.add(ITEMS.register(m.id() + "_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(type, m.eggA(), m.eggB(),
                    new Item.Properties().rarity(m.boss() ? Rarity.EPIC : Rarity.COMMON))));
        }
    }

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
