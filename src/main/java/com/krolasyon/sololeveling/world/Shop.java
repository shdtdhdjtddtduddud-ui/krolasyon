package com.krolasyon.sololeveling.world;

import com.krolasyon.sololeveling.item.SpecialItem;
import com.krolasyon.sololeveling.net.Net;
import com.krolasyon.sololeveling.registry.ModItems;
import com.krolasyon.sololeveling.system.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.function.Supplier;

/** Gold shops: the System shop, market stalls and guild stores. Also buys magic stones. */
public final class Shop {
    public static final int SYSTEM = 0, MERCHANT = 1, SMITH = 2, ALCHEMIST = 3, GUILD = 4;

    record Entry(Supplier<? extends Item> item, int price, Rank minRank) {}

    private Shop() {}

    static List<Entry> entries(int type, Guild g) {
        return switch (type) {
            case SYSTEM -> List.of(
                    e(ModItems.HEALING_POTION, 60, Rank.E), e(ModItems.MANA_POTION, 70, Rank.E), e(ModItems.FATIGUE_POTION, 90, Rank.E),
                    e(ModItems.GREATER_HEALING_POTION, 350, Rank.C), e(ModItems.GREATER_MANA_POTION, 400, Rank.C),
                    e(ModItems.INSTANT_DUNGEON_KEY, 800, Rank.E), e(ModItems.RANDOM_BOX, 500, Rank.E), e(ModItems.RETURN_STONE, 150, Rank.E),
                    e(ModItems.RUNE_STONE_BLOODLUST, 2500, Rank.D), e(ModItems.RUNE_STONE_MUTILATION, 3500, Rank.C),
                    e(ModItems.RUNE_STONE_STEALTH, 4500, Rank.C), e(ModItems.RUNE_STONE_DAGGER_STORM, 9000, Rank.A),
                    e(ModItems.ELIXIR_OF_LIFE, 25000, Rank.S), e(ModItems.HOLY_WATER, 40000, Rank.S));
            case MERCHANT -> List.of(
                    e(ModItems.HEALING_POTION, 45, Rank.E), e(ModItems.MANA_POTION, 50, Rank.E), e(ModItems.ANTIDOTE, 40, Rank.E),
                    e(ModItems.FATIGUE_POTION, 70, Rank.E), e(ModItems.NEWSPAPER, 2, Rank.E), e(ModItems.RETURN_STONE, 120, Rank.E),
                    e(ModItems.INSTANT_DUNGEON_KEY, 900, Rank.D));
            case ALCHEMIST -> List.of(
                    e(ModItems.GREATER_HEALING_POTION, 300, Rank.D), e(ModItems.GREATER_MANA_POTION, 320, Rank.D),
                    e(ModItems.STRENGTH_ELIXIR, 450, Rank.C), e(ModItems.AGILITY_ELIXIR, 450, Rank.C), e(ModItems.PERCEPTION_DRAUGHT, 380, Rank.C),
                    e(ModItems.STEALTH_TONIC, 600, Rank.B), e(ModItems.ELIXIR_OF_LIFE, 30000, Rank.S));
            case SMITH -> List.of(
                    e(ModItems.STEEL_DAGGER, 300, Rank.E), e(ModItems.HUNTER_SWORD, 350, Rank.E),
                    e(ModItems.HUNTER_HELMET, 250, Rank.E), e(ModItems.HUNTER_CHEST, 400, Rank.E), e(ModItems.HUNTER_LEGS, 350, Rank.E), e(ModItems.HUNTER_BOOTS, 220, Rank.E),
                    e(ModItems.KNIGHT_KILLER, 3000, Rank.C), e(ModItems.ORC_WAR_AXE, 5000, Rank.B),
                    e(ModItems.ORC_HELMET, 1800, Rank.B), e(ModItems.ORC_CHEST, 2600, Rank.B), e(ModItems.ORC_LEGS, 2200, Rank.B), e(ModItems.ORC_BOOTS, 1600, Rank.B));
            case GUILD -> switch (g) {
                case HUNTERS -> List.of(e(ModItems.FLAME_STAFF, 12000, Rank.B), e(ModItems.GREATER_HEALING_POTION, 220, Rank.E), e(ModItems.HAEIN_SWORD, 20000, Rank.A));
                case WHITE_TIGER -> List.of(e(ModItems.STRENGTH_ELIXIR, 300, Rank.E), e(ModItems.AGILITY_ELIXIR, 300, Rank.E), e(ModItems.ORC_CHEST, 2000, Rank.C));
                case FIEND -> List.of(e(ModItems.ORC_WAR_AXE, 3800, Rank.C), e(ModItems.STEALTH_TONIC, 400, Rank.D), e(ModItems.RUNE_STONE_BLOODLUST, 2000, Rank.D));
                case KNIGHTS -> List.of(e(ModItems.KNIGHT_KILLER, 2400, Rank.D), e(ModItems.KNIGHT_HELMET, 6000, Rank.B), e(ModItems.KNIGHT_CHEST, 9000, Rank.B),
                        e(ModItems.KNIGHT_LEGS, 7500, Rank.B), e(ModItems.KNIGHT_BOOTS, 5000, Rank.B));
                case AHJIN -> List.of(e(ModItems.INSTANT_DUNGEON_KEY, 500, Rank.E), e(ModItems.ELIXIR_OF_LIFE, 18000, Rank.A), e(ModItems.HOLY_WATER, 30000, Rank.S),
                        e(ModItems.RUNE_STONE_DAGGER_STORM, 7000, Rank.A));
                default -> List.of();
            };
            default -> List.of();
        };
    }

    private static Entry e(Supplier<? extends Item> i, int price, Rank r) { return new Entry(i, price, r); }

    public static void open(ServerPlayer p, int type) {
        HunterData d = HunterCapability.get(p);
        CompoundTag t = new CompoundTag();
        t.putInt("type", type);
        t.putLong("gold", d.gold);
        t.putInt("guild", d.guild.ordinal());
        ListTag l = new ListTag();
        for (Entry e : entries(type, d.guild)) {
            CompoundTag et = new CompoundTag();
            et.putString("item", ForgeRegistries.ITEMS.getKey(e.item().get()).toString());
            et.putInt("price", discounted(d, type, e.price()));
            et.putBoolean("locked", d.rank.ordinal() < e.minRank().ordinal());
            et.putString("rank", e.minRank().label);
            l.add(et);
        }
        t.put("items", l);
        Net.to(p, new Net.Open("shop", t));
    }

    private static int discounted(HunterData d, int type, int price) {
        if (type == GUILD) return Math.max(1, (int) (price * (1 - Guild.guildRankFor(d.guildRep) * 0.05)));
        return price;
    }

    public static void buy(ServerPlayer p, int type, String itemId, int count) {
        HunterData d = HunterCapability.get(p);
        if (type == SYSTEM && !d.awakened) return;
        for (Entry e : entries(type, d.guild)) {
            String id = ForgeRegistries.ITEMS.getKey(e.item().get()).toString();
            if (!id.equals(itemId)) continue;
            if (d.rank.ordinal() < e.minRank().ordinal()) {
                Sys.warn(p, "shop.rank_locked", e.minRank().label);
                return;
            }
            long cost = (long) discounted(d, type, e.price()) * count;
            if (d.gold < cost) {
                Sys.warn(p, "shop.no_gold");
                return;
            }
            d.gold -= cost;
            d.markDirty();
            Progression.give(p, new ItemStack(e.item().get(), count));
            p.displayClientMessage(Sys.t("shop.bought", new ItemStack(e.item().get(), count).getHoverName(), cost), true);
            open(p, type);
            return;
        }
    }

    /** Sells every magic stone and monster material in the inventory to the Association. */
    public static long sellStones(ServerPlayer p) {
        HunterData d = HunterCapability.get(p);
        long total = 0;
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof SpecialItem sp && (sp.kind == SpecialItem.Kind.MAGIC_STONE || sp.kind == SpecialItem.Kind.MATERIAL)) {
                total += (long) sp.value * s.getCount();
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
        if (total > 0) {
            d.gold += total;
            d.markDirty();
            Sys.notify(p, Sys.REWARD, Sys.t("association.title"), Sys.t("shop.sold", total));
        }
        return total;
    }
}
