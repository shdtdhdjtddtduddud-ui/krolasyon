package com.krolasyon.bosses.rpg.npc;

import com.krolasyon.bosses.rpg.data.PlayerRpg;
import com.krolasyon.bosses.rpg.data.RpgWorldData;
import com.krolasyon.bosses.rpg.def.RpgDefs;
import com.krolasyon.bosses.rpg.def.RpgDefs.School;
import com.krolasyon.bosses.rpg.def.SpellDef;
import com.krolasyon.bosses.rpg.def.SwordDef;
import com.krolasyon.bosses.rpg.item.RpgItems;
import com.krolasyon.bosses.rpg.item.SpellTomeItem;
import com.krolasyon.bosses.rpg.world.Kingdom;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.ArrayList;
import java.util.List;

/** Shop inventories, priced in coins. */
public final class NpcTrades {
    private NpcTrades() {}

    /** price in copper → up to two coin stacks */
    private static ItemStack[] price(int copper) {
        copper = Math.max(1, copper);
        if (copper >= 1000) {
            int g = Math.min(64, copper / 100);
            int s = Math.min(64, (copper - g * 100) / 10);
            return new ItemStack[]{new ItemStack(RpgItems.GOLD_COIN.get(), g), s > 0 ? new ItemStack(RpgItems.SILVER_COIN.get(), s) : ItemStack.EMPTY};
        }
        if (copper >= 100) {
            int s = Math.min(64, copper / 10);
            int c = copper - s * 10;
            return new ItemStack[]{new ItemStack(RpgItems.SILVER_COIN.get(), s), c > 0 ? new ItemStack(RpgItems.COPPER_COIN.get(), c) : ItemStack.EMPTY};
        }
        if (copper > 64) {
            int s = copper / 10;
            int c = copper - s * 10;
            return new ItemStack[]{new ItemStack(RpgItems.SILVER_COIN.get(), s), c > 0 ? new ItemStack(RpgItems.COPPER_COIN.get(), c) : ItemStack.EMPTY};
        }
        return new ItemStack[]{new ItemStack(RpgItems.COPPER_COIN.get(), copper), ItemStack.EMPTY};
    }

    private static void sell(MerchantOffers o, ItemStack item, int copper, int uses) {
        ItemStack[] p = price(copper);
        o.add(new MerchantOffer(p[0], p[1], item, uses, 1, 0.05F));
    }

    private static void buy(MerchantOffers o, ItemStack wanted, int copper, int uses) {
        ItemStack pay = copper >= 100 ? new ItemStack(RpgItems.SILVER_COIN.get(), Math.min(64, copper / 10)) : new ItemStack(RpgItems.COPPER_COIN.get(), Math.max(1, copper));
        o.add(new MerchantOffer(wanted, ItemStack.EMPTY, pay, uses, 1, 0.05F));
    }

    private static School[] schools(int kingdom) {
        return switch (Kingdom.of(kingdom)) {
            case ALDORIA -> new School[]{School.LIGHT, School.FIRE, School.WATER};
            case SYLVARIEN -> new School[]{School.NATURE, School.WIND, School.LIGHT, School.ARCANE};
            case KHAZDUR -> new School[]{School.EARTH, School.FIRE, School.LIGHTNING};
            case INFERNAX -> new School[]{School.FIRE, School.DARK, School.BLOOD};
            case YMIRHEIM -> new School[]{School.ICE, School.EARTH, School.LIGHTNING};
            case GORMASH -> new School[]{School.BLOOD, School.FIRE, School.SPIRIT};
            case FELARIS -> new School[]{School.NATURE, School.SPIRIT, School.WIND};
            case VALDREN -> new School[]{School.DARK, School.LIGHTNING, School.ICE};
            case MERIDIA -> new School[]{School.WATER, School.ARCANE, School.WIND, School.LIGHTNING};
            case NOCTHERA -> new School[]{School.DARK, School.ARCANE, School.SPIRIT};
        };
    }

    public static MerchantOffers create(RpgNpc npc) {
        MerchantOffers o = new MerchantOffers();
        RandomSource r = RandomSource.create(npc.getUUID().getLeastSignificantBits());
        switch (npc.role()) {
            case MERCHANT -> {
                sell(o, new ItemStack(Items.BREAD, 4), 4, 16);
                sell(o, new ItemStack(Items.COOKED_BEEF, 3), 9, 12);
                sell(o, new ItemStack(RpgItems.HEALING_POTION.get()), 35, 6);
                sell(o, new ItemStack(RpgItems.MANA_POTION.get()), 40, 6);
                sell(o, new ItemStack(Items.TORCH, 8), 6, 12);
                sell(o, new ItemStack(Items.ARROW, 16), 12, 8);
                sell(o, new ItemStack(RpgItems.RING.get()), 300, 2);
                sell(o, new ItemStack(RpgItems.DEED.get()), 900, 1);
                sell(o, new ItemStack(Items.SADDLE), 120, 1);
                sell(o, new ItemStack(Items.MAP), 20, 4);
                buy(o, new ItemStack(Items.WHEAT, 16), 4, 16);
                buy(o, new ItemStack(Items.LEATHER, 6), 8, 12);
                buy(o, new ItemStack(Items.EMERALD), 30, 16);
                buy(o, new ItemStack(Items.DIAMOND), 150, 8);
            }
            case BLACKSMITH -> {
                sell(o, new ItemStack(Items.IRON_SWORD), 60, 4);
                sell(o, new ItemStack(Items.IRON_CHESTPLATE), 120, 3);
                sell(o, new ItemStack(Items.IRON_HELMET), 70, 3);
                sell(o, new ItemStack(Items.IRON_LEGGINGS), 100, 3);
                sell(o, new ItemStack(Items.IRON_BOOTS), 60, 3);
                sell(o, new ItemStack(Items.SHIELD), 45, 4);
                sell(o, new ItemStack(Items.BOW), 40, 3);
                List<SwordDef> pool = new ArrayList<>();
                int maxTier = npc.kingdomId() == Kingdom.KHAZDUR.ordinal() ? 4 : 3;
                for (SwordDef s : RpgDefs.SWORDS) if (s.tier() <= maxTier && s.tier() >= 1) pool.add(s);
                for (int i = 0; i < 4 && !pool.isEmpty(); i++) {
                    SwordDef s = pool.remove(r.nextInt(pool.size()));
                    sell(o, new ItemStack(RpgItems.sword(s.id())), 80 + s.tier() * s.tier() * 90, 1);
                }
                buy(o, new ItemStack(Items.IRON_INGOT, 4), 12, 16);
                buy(o, new ItemStack(Items.GOLD_INGOT, 2), 20, 12);
                buy(o, new ItemStack(Items.COAL, 16), 6, 16);
            }
            case MAGE -> {
                School[] sc = schools(npc.kingdomId());
                List<SpellDef> pool = new ArrayList<>();
                for (SpellDef s : RpgDefs.SPELLS) {
                    if (s.tier() > 3) continue;
                    for (School x : sc) if (s.school() == x) pool.add(s);
                }
                for (int i = 0; i < 6 && !pool.isEmpty(); i++) {
                    SpellDef s = pool.remove(r.nextInt(pool.size()));
                    sell(o, SpellTomeItem.of(s), 30 + s.tier() * s.tier() * 60, 1);
                }
                sell(o, new ItemStack(RpgItems.STAFFS.get(0).get()), 80, 2);
                sell(o, new ItemStack(RpgItems.STAFFS.get(1).get()), 400, 1);
                if (r.nextBoolean()) sell(o, new ItemStack(RpgItems.STAFFS.get(2).get()), 1200, 1);
                sell(o, new ItemStack(RpgItems.MANA_POTION.get(), 2), 70, 8);
                sell(o, new ItemStack(RpgItems.GREATER_MANA_POTION.get()), 150, 4);
                buy(o, new ItemStack(RpgItems.ESSENCE.get(), 3), 15, 16);
                buy(o, new ItemStack(Items.AMETHYST_SHARD, 4), 10, 12);
            }
            case PRIEST -> {
                sell(o, new ItemStack(RpgItems.HEALING_POTION.get(), 2), 50, 8);
                sell(o, new ItemStack(Items.GOLDEN_APPLE), 250, 2);
                for (SpellDef s : RpgDefs.SPELLS) if (s.school() == School.LIGHT && s.tier() <= 2) sell(o, SpellTomeItem.of(s), 40 + s.tier() * 60, 1);
                sell(o, new ItemStack(RpgItems.HERB.get()), 200, 3);
            }
            case INNKEEPER -> {
                sell(o, new ItemStack(Items.BREAD, 3), 3, 16);
                sell(o, new ItemStack(Items.COOKED_PORKCHOP, 2), 6, 16);
                sell(o, new ItemStack(Items.MUSHROOM_STEW), 5, 8);
                sell(o, new ItemStack(Items.HONEY_BOTTLE, 2), 8, 8);
                sell(o, new ItemStack(Items.CAKE), 20, 2);
                sell(o, new ItemStack(Items.WHITE_BED), 30, 2);
                buy(o, new ItemStack(Items.CARROT, 12), 3, 16);
                buy(o, new ItemStack(Items.SWEET_BERRIES, 12), 3, 16);
            }
            case GUILD_MASTER -> {
                buy(o, new ItemStack(RpgItems.ESSENCE.get(), 2), 12, 32);
                buy(o, new ItemStack(RpgItems.TROPHY.get()), 300, 8);
                sell(o, new ItemStack(RpgItems.HEALING_POTION.get(), 3), 90, 6);
                sell(o, new ItemStack(Items.IRON_SWORD), 55, 4);
                sell(o, new ItemStack(Items.SPYGLASS), 60, 2);
                sell(o, new ItemStack(Items.COMPASS), 30, 2);
            }
            default -> {}
        }
        return o;
    }

    /** discount in percent based on how much the NPC likes the player and the player's standing */
    public static int discountPercent(RpgNpc npc, ServerPlayer p) {
        Relation rel = npc.rel(p.getUUID());
        PlayerRpg d = RpgWorldData.player(p);
        int disc = rel.affinity / 4 + d.repWith(npc.kingdomId()) / 60 + d.social * 2;
        if (rel.rescued) disc += 15;
        if (npc.traits[2] > 70) disc -= 10;
        return Math.max(-40, Math.min(40, disc));
    }
}
