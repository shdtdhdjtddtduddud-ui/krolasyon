package com.krolasyon.sololeveling.registry;

import com.krolasyon.sololeveling.SoloLeveling;
import com.krolasyon.sololeveling.entity.MobKind;
import com.krolasyon.sololeveling.item.SLArmorItem;
import com.krolasyon.sololeveling.item.SLPotionItem;
import com.krolasyon.sololeveling.item.SLWeaponItem;
import com.krolasyon.sololeveling.item.SLWeaponItem.Active;
import com.krolasyon.sololeveling.item.SLWeaponItem.Fx;
import com.krolasyon.sololeveling.item.SpecialItem;
import com.krolasyon.sololeveling.system.Rank;
import com.krolasyon.sololeveling.system.Skill;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;

public final class ModItems {
    public static final DeferredRegister<Item> REG = DeferredRegister.create(ForgeRegistries.ITEMS, SoloLeveling.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SoloLeveling.MODID);
    /** Item ids grouped for the asset generator and the creative tab. */
    public static final List<RegistryObject<? extends Item>> WEAPONS = new ArrayList<>(), POTIONS = new ArrayList<>(), ARMOR = new ArrayList<>(),
            MISC = new ArrayList<>(), EGGS = new ArrayList<>();

    // ---------------------------------------------------------------- weapons
    public static final RegistryObject<Item> RUSTY_DAGGER = weapon("rusty_dagger", 250, 4, -1.8F, true, Fx.NONE, Active.NONE, 0, "E", Rarity.COMMON);
    public static final RegistryObject<Item> STEEL_DAGGER = weapon("steel_dagger", 600, 6, -1.8F, true, Fx.NONE, Active.NONE, 0, "D", Rarity.COMMON);
    public static final RegistryObject<Item> KASAKA_VENOM_FANG = weapon("kasaka_venom_fang", 900, 8, -1.7F, true, Fx.VENOM, Active.NONE, 0, "C", Rarity.UNCOMMON);
    public static final RegistryObject<Item> KNIGHT_KILLER = weapon("knight_killer", 1200, 10, -1.7F, true, Fx.ARMOR_BREAK, Active.NONE, 0, "B", Rarity.UNCOMMON);
    public static final RegistryObject<Item> BARUKA_DAGGER = weapon("baruka_dagger", 1600, 12, -1.6F, true, Fx.FROST, Active.NONE, 0, "A", Rarity.RARE);
    public static final RegistryObject<Item> DEMON_KING_DAGGER = weapon("demon_king_dagger", 2200, 15, -1.6F, true, Fx.LIGHTNING, Active.SHADOW_SLASH, 60, "S", Rarity.RARE);
    public static final RegistryObject<Item> KAMISH_WRATH = weapon("kamish_wrath", 4000, 22, -1.5F, true, Fx.DRAGON_FLAME, Active.DRAGON_WAVE, 100, "S+", Rarity.EPIC);
    public static final RegistryObject<Item> HUNTER_SWORD = weapon("hunter_sword", 500, 7, -2.4F, false, Fx.NONE, Active.NONE, 0, "D", Rarity.COMMON);
    public static final RegistryObject<Item> CRIMSON_GREATSWORD = weapon("crimson_greatsword", 2500, 17, -2.9F, false, Fx.HEAVY, Active.GROUND_SLAM, 80, "A", Rarity.RARE);
    public static final RegistryObject<Item> DEMON_KING_LONGSWORD = weapon("demon_king_longsword", 3000, 19, -2.6F, false, Fx.LIGHTNING, Active.THUNDER, 70, "S", Rarity.EPIC);
    public static final RegistryObject<Item> HAEIN_SWORD = weapon("haein_sword", 2000, 14, -2.1F, false, Fx.SWIFT, Active.DASH_SLASH, 50, "S", Rarity.RARE);
    public static final RegistryObject<Item> ORC_WAR_AXE = weapon("orc_war_axe", 1500, 15, -3.0F, false, Fx.HEAVY, Active.GROUND_SLAM, 90, "A", Rarity.UNCOMMON);
    public static final RegistryObject<Item> FLAME_STAFF = weapon("flame_staff", 1200, 6, -2.6F, false, Fx.FIRE, Active.FIREBALL, 30, "S", Rarity.RARE);
    public static final RegistryObject<Item> ORB_OF_AVARICE = weapon("orb_of_avarice", 1800, 5, -2.4F, false, Fx.MANA, Active.MANA_BOLT, 16, "S", Rarity.EPIC);

    // ---------------------------------------------------------------- potions
    public static final RegistryObject<Item> HEALING_POTION = potion("healing_potion", SLPotionItem.Kind.HEALING, Rarity.COMMON);
    public static final RegistryObject<Item> GREATER_HEALING_POTION = potion("greater_healing_potion", SLPotionItem.Kind.GREATER_HEALING, Rarity.UNCOMMON);
    public static final RegistryObject<Item> MANA_POTION = potion("mana_potion", SLPotionItem.Kind.MANA, Rarity.COMMON);
    public static final RegistryObject<Item> GREATER_MANA_POTION = potion("greater_mana_potion", SLPotionItem.Kind.GREATER_MANA, Rarity.UNCOMMON);
    public static final RegistryObject<Item> ELIXIR_OF_LIFE = potion("elixir_of_life", SLPotionItem.Kind.ELIXIR_OF_LIFE, Rarity.EPIC);
    public static final RegistryObject<Item> HOLY_WATER = potion("holy_water_of_life", SLPotionItem.Kind.HOLY_WATER, Rarity.EPIC);
    public static final RegistryObject<Item> FATIGUE_POTION = potion("fatigue_recovery_potion", SLPotionItem.Kind.FATIGUE_RECOVERY, Rarity.COMMON);
    public static final RegistryObject<Item> STRENGTH_ELIXIR = potion("strength_elixir", SLPotionItem.Kind.STRENGTH, Rarity.UNCOMMON);
    public static final RegistryObject<Item> AGILITY_ELIXIR = potion("agility_elixir", SLPotionItem.Kind.AGILITY, Rarity.UNCOMMON);
    public static final RegistryObject<Item> PERCEPTION_DRAUGHT = potion("perception_draught", SLPotionItem.Kind.PERCEPTION, Rarity.UNCOMMON);
    public static final RegistryObject<Item> STEALTH_TONIC = potion("stealth_tonic", SLPotionItem.Kind.STEALTH, Rarity.UNCOMMON);
    public static final RegistryObject<Item> ANTIDOTE = potion("antidote", SLPotionItem.Kind.ANTIDOTE, Rarity.COMMON);

    // ---------------------------------------------------------------- armor
    public static final RegistryObject<Item> HUNTER_HELMET = armor("hunter_helmet", SLArmorItem.Mat.HUNTER, ArmorItem.Type.HELMET, Rarity.COMMON);
    public static final RegistryObject<Item> HUNTER_CHEST = armor("hunter_chestplate", SLArmorItem.Mat.HUNTER, ArmorItem.Type.CHESTPLATE, Rarity.COMMON);
    public static final RegistryObject<Item> HUNTER_LEGS = armor("hunter_leggings", SLArmorItem.Mat.HUNTER, ArmorItem.Type.LEGGINGS, Rarity.COMMON);
    public static final RegistryObject<Item> HUNTER_BOOTS = armor("hunter_boots", SLArmorItem.Mat.HUNTER, ArmorItem.Type.BOOTS, Rarity.COMMON);
    public static final RegistryObject<Item> ORC_HELMET = armor("high_orc_helmet", SLArmorItem.Mat.HIGH_ORC, ArmorItem.Type.HELMET, Rarity.UNCOMMON);
    public static final RegistryObject<Item> ORC_CHEST = armor("high_orc_chestplate", SLArmorItem.Mat.HIGH_ORC, ArmorItem.Type.CHESTPLATE, Rarity.UNCOMMON);
    public static final RegistryObject<Item> ORC_LEGS = armor("high_orc_leggings", SLArmorItem.Mat.HIGH_ORC, ArmorItem.Type.LEGGINGS, Rarity.UNCOMMON);
    public static final RegistryObject<Item> ORC_BOOTS = armor("high_orc_boots", SLArmorItem.Mat.HIGH_ORC, ArmorItem.Type.BOOTS, Rarity.UNCOMMON);
    public static final RegistryObject<Item> KNIGHT_HELMET = armor("crimson_knight_helmet", SLArmorItem.Mat.CRIMSON_KNIGHT, ArmorItem.Type.HELMET, Rarity.RARE);
    public static final RegistryObject<Item> KNIGHT_CHEST = armor("crimson_knight_chestplate", SLArmorItem.Mat.CRIMSON_KNIGHT, ArmorItem.Type.CHESTPLATE, Rarity.RARE);
    public static final RegistryObject<Item> KNIGHT_LEGS = armor("crimson_knight_leggings", SLArmorItem.Mat.CRIMSON_KNIGHT, ArmorItem.Type.LEGGINGS, Rarity.RARE);
    public static final RegistryObject<Item> KNIGHT_BOOTS = armor("crimson_knight_boots", SLArmorItem.Mat.CRIMSON_KNIGHT, ArmorItem.Type.BOOTS, Rarity.RARE);
    public static final RegistryObject<Item> MONARCH_HELMET = armor("shadow_monarch_helmet", SLArmorItem.Mat.SHADOW_MONARCH, ArmorItem.Type.HELMET, Rarity.EPIC);
    public static final RegistryObject<Item> MONARCH_CHEST = armor("shadow_monarch_chestplate", SLArmorItem.Mat.SHADOW_MONARCH, ArmorItem.Type.CHESTPLATE, Rarity.EPIC);
    public static final RegistryObject<Item> MONARCH_LEGS = armor("shadow_monarch_leggings", SLArmorItem.Mat.SHADOW_MONARCH, ArmorItem.Type.LEGGINGS, Rarity.EPIC);
    public static final RegistryObject<Item> MONARCH_BOOTS = armor("shadow_monarch_boots", SLArmorItem.Mat.SHADOW_MONARCH, ArmorItem.Type.BOOTS, Rarity.EPIC);

    // ---------------------------------------------------------------- magic stones (value in gold)
    public static final RegistryObject<Item> MAGIC_STONE_E = stone("magic_stone_e", 5, Rarity.COMMON);
    public static final RegistryObject<Item> MAGIC_STONE_D = stone("magic_stone_d", 15, Rarity.COMMON);
    public static final RegistryObject<Item> MAGIC_STONE_C = stone("magic_stone_c", 40, Rarity.UNCOMMON);
    public static final RegistryObject<Item> MAGIC_STONE_B = stone("magic_stone_b", 100, Rarity.UNCOMMON);
    public static final RegistryObject<Item> MAGIC_STONE_A = stone("magic_stone_a", 260, Rarity.RARE);
    public static final RegistryObject<Item> MAGIC_STONE_S = stone("magic_stone_s", 700, Rarity.EPIC);

    // ---------------------------------------------------------------- monster materials
    public static final RegistryObject<Item> GOBLIN_EAR = material("goblin_ear", 2);
    public static final RegistryObject<Item> LYCAN_FANG = material("lycan_fang", 8);
    public static final RegistryObject<Item> KASAKA_SCALE = material("kasaka_scale", 40);
    public static final RegistryObject<Item> STATUE_FRAGMENT = material("statue_fragment", 30);
    public static final RegistryObject<Item> ICE_BEAR_PELT = material("ice_bear_pelt", 45);
    public static final RegistryObject<Item> ELF_FROST_CRYSTAL = material("elf_frost_crystal", 50);
    public static final RegistryObject<Item> ORC_TUSK = material("orc_tusk", 60);
    public static final RegistryObject<Item> DEMON_HORN = material("demon_horn", 90);
    public static final RegistryObject<Item> CERBERUS_FANG = material("cerberus_fang", 250);
    public static final RegistryObject<Item> ANT_CARAPACE = material("ant_carapace", 120);
    public static final RegistryObject<Item> RED_KNIGHT_PLUME = material("red_knight_plume", 300);
    public static final RegistryObject<Item> BARAN_HEART = material("baran_heart", 1500);

    // ---------------------------------------------------------------- special
    public static final RegistryObject<Item> RETURN_STONE = special("return_stone", SpecialItem.Kind.RETURN_STONE, null, 0, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final RegistryObject<Item> INSTANT_DUNGEON_KEY = special("instant_dungeon_key", SpecialItem.Kind.INSTANT_DUNGEON_KEY, null, 0, new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON));
    public static final RegistryObject<Item> DEMON_CASTLE_KEY = special("demon_castle_key", SpecialItem.Kind.DEMON_CASTLE_KEY, null, 0, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
    public static final RegistryObject<Item> RANDOM_BOX = special("random_box", SpecialItem.Kind.RANDOM_BOX, null, 0, new Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON));
    public static final RegistryObject<Item> HUNTER_LICENSE = special("hunter_license", SpecialItem.Kind.HUNTER_LICENSE, null, 0, new Item.Properties().stacksTo(1));
    public static final RegistryObject<Item> NEWSPAPER = special("newspaper", SpecialItem.Kind.NEWSPAPER, null, 0, new Item.Properties().stacksTo(16));
    public static final RegistryObject<Item> RUNE_STONE_BLOODLUST = rune("rune_stone_bloodlust", Skill.BLOODLUST);
    public static final RegistryObject<Item> RUNE_STONE_MUTILATION = rune("rune_stone_mutilation", Skill.MUTILATION);
    public static final RegistryObject<Item> RUNE_STONE_STEALTH = rune("rune_stone_stealth", Skill.STEALTH);
    public static final RegistryObject<Item> RUNE_STONE_DAGGER_STORM = rune("rune_stone_dagger_storm", Skill.DAGGER_STORM);

    static {
        for (MobKind k : MobKind.values()) {
            int[] c = eggColors(k);
            EGGS.add(REG.register(k.id() + "_spawn_egg", () -> new ForgeSpawnEggItem(() -> ModEntities.mob(k), c[0], c[1], new Item.Properties())));
        }
    }

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.sololeveling"))
            .icon(() -> new ItemStack(KAMISH_WRATH.get()))
            .displayItems((params, out) -> {
                for (var l : List.of(WEAPONS, ARMOR, POTIONS, MISC, ModBlocks.ITEMS, EGGS)) for (var r : l) out.accept(r.get());
            }).build());

    private ModItems() {}

    private static RegistryObject<Item> weapon(String id, int dur, int dmg, float speed, boolean dagger, Fx fx, Active active, int cd, String rank, Rarity r) {
        RegistryObject<Item> o = REG.register(id, () -> new SLWeaponItem(dur, dmg, speed, dagger, fx, active, cd, rank, r));
        WEAPONS.add(o);
        return o;
    }

    private static RegistryObject<Item> potion(String id, SLPotionItem.Kind k, Rarity r) {
        RegistryObject<Item> o = REG.register(id, () -> new SLPotionItem(k, r));
        POTIONS.add(o);
        return o;
    }

    private static RegistryObject<Item> armor(String id, SLArmorItem.Mat m, ArmorItem.Type t, Rarity r) {
        RegistryObject<Item> o = REG.register(id, () -> new SLArmorItem(m, t, r));
        ARMOR.add(o);
        return o;
    }

    private static RegistryObject<Item> stone(String id, int value, Rarity r) {
        RegistryObject<Item> o = REG.register(id, () -> new SpecialItem(SpecialItem.Kind.MAGIC_STONE, null, value, new Item.Properties().rarity(r)));
        MISC.add(o);
        return o;
    }

    private static RegistryObject<Item> material(String id, int value) {
        RegistryObject<Item> o = REG.register(id, () -> new SpecialItem(SpecialItem.Kind.MATERIAL, null, value, new Item.Properties()));
        MISC.add(o);
        return o;
    }

    private static RegistryObject<Item> special(String id, SpecialItem.Kind k, Skill rune, int value, Item.Properties p) {
        RegistryObject<Item> o = REG.register(id, () -> new SpecialItem(k, rune, value, p));
        MISC.add(o);
        return o;
    }

    private static RegistryObject<Item> rune(String id, Skill s) {
        return special(id, SpecialItem.Kind.RUNE_STONE, s, 0, new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    public static Item magicStone(Rank r) {
        return switch (r) {
            case E -> MAGIC_STONE_E.get();
            case D -> MAGIC_STONE_D.get();
            case C -> MAGIC_STONE_C.get();
            case B -> MAGIC_STONE_B.get();
            case A -> MAGIC_STONE_A.get();
            default -> MAGIC_STONE_S.get();
        };
    }

    private static int[] eggColors(MobKind k) {
        return switch (k.category) {
            case HUMANOID -> new int[]{0x3E6B2F, 0xC9B26B};
            case BEAST -> new int[]{0x5C5C66, 0xD9D9E0};
            case INSECT -> new int[]{0x241B33, 0x9C6BFF};
            case CONSTRUCT -> new int[]{0x8C877A, 0xF2D27A};
            case KNIGHT -> new int[]{0x2B2B33, 0xC4242E};
            case DEMON -> new int[]{0x5E1212, 0xFF7A1A};
        };
    }

    public static String idOf(Item i) { return ForgeRegistries.ITEMS.getKey(i).getPath().toLowerCase(Locale.ROOT); }
}
