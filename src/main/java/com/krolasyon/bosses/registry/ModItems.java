package com.krolasyon.bosses.registry;

import com.krolasyon.bosses.KrolasyonBosses;
import com.krolasyon.bosses.entity.mob.MobSpec;
import com.krolasyon.bosses.entity.mob.MobSpecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    private ModItems() {}

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, KrolasyonBosses.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, KrolasyonBosses.MODID);

    public static final RegistryObject<Item> SEAL_WARDEN_EGG = ITEMS.register("seal_warden_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.SEAL_WARDEN, 0xB89A8E, 0xF58ADB, new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> CRIMSON_HOUND_EGG = ITEMS.register("crimson_hound_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.CRIMSON_HOUND, 0x2E2230, 0xFF3A48, new Item.Properties().rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> REVENGE_EGG = ITEMS.register("revenge_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.REVENGE, 0x8A1620, 0xD8D8DE, new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> HEART_DEMON_EGG = ITEMS.register("heart_demon_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.HEART_DEMON, 0xD85060, 0x1A080E, new Item.Properties().rarity(Rarity.EPIC)));

    public static final java.util.Map<String, RegistryObject<Item>> MOB_EGGS = new java.util.LinkedHashMap<>();

    static {
        for (MobSpec s : MobSpecs.ALL.values()) {
            MOB_EGGS.put(s.id, ITEMS.register(s.id + "_spawn_egg",
                    () -> new ForgeSpawnEggItem(ModEntities.MOBS.get(s.id), s.eggA, s.eggB, new Item.Properties())));
        }
    }

    /** plain materials, relics and sigils (generated table) */
    public static final java.util.Map<String, RegistryObject<Item>> MATERIALS = new java.util.LinkedHashMap<>();

    static {
        for (String[] row : MaterialTable.ROWS) {
            Rarity rarity = switch (row[1]) {
                case "uncommon" -> Rarity.UNCOMMON;
                case "rare" -> Rarity.RARE;
                case "epic" -> Rarity.EPIC;
                default -> Rarity.COMMON;
            };
            MATERIALS.put(row[0], ITEMS.register(row[0], () -> row[0].equals("hell_spark")
                    ? new com.krolasyon.bosses.item.HellSparkItem(new Item.Properties().rarity(rarity))
                    : new Item(new Item.Properties().rarity(rarity))));
        }
    }

    /** swords and spell tomes (generated table) */
    public static final java.util.Map<String, RegistryObject<Item>> GEAR = new java.util.LinkedHashMap<>();

    static {
        for (Object[] r : GearTable.ROWS) {
            String id = (String) r[0];
            Rarity rarity = switch ((String) r[4]) {
                case "uncommon" -> Rarity.UNCOMMON;
                case "rare" -> Rarity.RARE;
                case "epic" -> Rarity.EPIC;
                default -> Rarity.COMMON;
            };
            int mana = (Integer) r[5], cd = (Integer) r[6];
            if (r[1].equals("sword")) {
                GEAR.put(id, ITEMS.register(id, () -> new com.krolasyon.bosses.item.AbilitySword(id, Math.round((Float) r[2]), (Float) r[3],
                        new Item.Properties().rarity(rarity).fireResistant(), mana, cd)));
            } else {
                GEAR.put(id, ITEMS.register(id, () -> new com.krolasyon.bosses.item.SpellTome(id, new Item.Properties().rarity(rarity).stacksTo(1), mana, cd)));
            }
        }
    }

    public static final RegistryObject<Item> CHRONICLE = ITEMS.register("chronicle",
            () -> new com.krolasyon.bosses.item.JournalItem(new Item.Properties().rarity(Rarity.UNCOMMON).stacksTo(1)));

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("bosses", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.krolasyonbosses"))
            .icon(() -> new ItemStack(SEAL_WARDEN_EGG.get()))
            .displayItems((params, output) -> {
                output.accept(SEAL_WARDEN_EGG.get());
                output.accept(CRIMSON_HOUND_EGG.get());
                output.accept(REVENGE_EGG.get());
                output.accept(HEART_DEMON_EGG.get());
                for (RegistryObject<Item> egg : MOB_EGGS.values()) output.accept(egg.get());
                output.accept(CHRONICLE.get());
                for (RegistryObject<Item> g : GEAR.values()) output.accept(g.get());
                for (RegistryObject<Item> m : MATERIALS.values()) output.accept(m.get());
                for (RegistryObject<Item> b : ModBlocks.ITEMS.values()) output.accept(b.get());
            })
            .build());
}
