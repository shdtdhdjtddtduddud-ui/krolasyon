package com.krolasyon.sololeveling.entity;

import com.krolasyon.sololeveling.registry.ModItems;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.function.Supplier;

/** Monster specific drops (magic stones are handled separately). */
public final class MobLoot {
    public record Drop(Supplier<? extends Item> item, float chance, int min, int max) {}

    private MobLoot() {}

    public static List<Drop> drops(MobKind k) {
        return switch (k) {
            case GOBLIN -> List.of(d(ModItems.GOBLIN_EAR, 0.5F), d(ModItems.HEALING_POTION, 0.05F));
            case HOBGOBLIN -> List.of(d(ModItems.GOBLIN_EAR, 0.8F, 1, 2), d(ModItems.STEEL_DAGGER, 0.03F));
            case STEEL_FANGED_LYCAN -> List.of(d(ModItems.LYCAN_FANG, 0.6F), d(ModItems.HEALING_POTION, 0.06F));
            case GIANT_CENTIPEDE -> List.of(d(ModItems.ANTIDOTE, 0.15F));
            case KASAKA -> List.of(d(ModItems.KASAKA_VENOM_FANG, 1F), d(ModItems.KASAKA_SCALE, 1F, 2, 5), d(ModItems.RANDOM_BOX, 1F));
            case STONE_STATUE -> List.of(d(ModItems.STATUE_FRAGMENT, 0.5F));
            case STATUE_OF_GOD -> List.of(d(ModItems.STATUE_FRAGMENT, 1F, 5, 10), d(ModItems.HOLY_WATER, 1F), d(ModItems.RANDOM_BOX, 1F, 2, 3));
            case CASTLE_KNIGHT -> List.of(d(ModItems.KNIGHT_KILLER, 0.04F), d(ModItems.MANA_POTION, 0.08F));
            case IGRIS -> List.of(d(ModItems.CRIMSON_GREATSWORD, 1F), d(ModItems.RED_KNIGHT_PLUME, 1F), d(ModItems.KNIGHT_HELMET, 0.5F), d(ModItems.KNIGHT_CHEST, 0.5F));
            case ICE_ELF -> List.of(d(ModItems.ELF_FROST_CRYSTAL, 0.4F), d(ModItems.MANA_POTION, 0.08F));
            case ICE_BEAR -> List.of(d(ModItems.ICE_BEAR_PELT, 0.7F));
            case BARUKA -> List.of(d(ModItems.BARUKA_DAGGER, 1F), d(ModItems.ELF_FROST_CRYSTAL, 1F, 3, 6), d(ModItems.RUNE_STONE_DAGGER_STORM, 0.3F));
            case HIGH_ORC -> List.of(d(ModItems.ORC_TUSK, 0.5F), d(ModItems.ORC_WAR_AXE, 0.03F), d(ModItems.ORC_HELMET, 0.04F), d(ModItems.ORC_CHEST, 0.03F));
            case KARGALGAN -> List.of(d(ModItems.ORB_OF_AVARICE, 0.6F), d(ModItems.ORC_TUSK, 1F, 3, 6), d(ModItems.GREATER_MANA_POTION, 1F, 2, 3));
            case DEMON -> List.of(d(ModItems.DEMON_HORN, 0.45F), d(ModItems.GREATER_HEALING_POTION, 0.05F));
            case CERBERUS -> List.of(d(ModItems.CERBERUS_FANG, 1F, 1, 3), d(ModItems.MONARCH_BOOTS, 0.4F), d(ModItems.RANDOM_BOX, 1F, 2, 3));
            case BARAN -> List.of(d(ModItems.DEMON_KING_LONGSWORD, 1F), d(ModItems.DEMON_KING_DAGGER, 1F, 1, 2), d(ModItems.BARAN_HEART, 1F), d(ModItems.MONARCH_CHEST, 0.6F), d(ModItems.MONARCH_HELMET, 0.6F));
            case ANT_SOLDIER -> List.of(d(ModItems.ANT_CARAPACE, 0.5F), d(ModItems.ELIXIR_OF_LIFE, 0.01F));
            case BERU -> List.of(d(ModItems.ANT_CARAPACE, 1F, 6, 10), d(ModItems.MONARCH_LEGS, 0.8F), d(ModItems.ELIXIR_OF_LIFE, 1F), d(ModItems.HAEIN_SWORD, 0.3F));
        };
    }

    private static Drop d(Supplier<? extends Item> i, float c) { return new Drop(i, c, 1, 1); }

    private static Drop d(Supplier<? extends Item> i, float c, int min, int max) { return new Drop(i, c, min, max); }
}
