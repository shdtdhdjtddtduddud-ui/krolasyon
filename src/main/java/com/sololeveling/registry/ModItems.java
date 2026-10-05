package com.sololeveling.registry;

import com.sololeveling.SoloLeveling;
import com.sololeveling.gen.Content;
import com.sololeveling.item.*;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ModItems {
    private ModItems() {}

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SoloLeveling.MODID);

    public static final Map<String, RegistryObject<Item>> WEAPONS = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<Item>> CONSUMABLES = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<Item>> MATERIALS = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<Item>> ARMOR = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<Item>> BLOCK_ITEMS = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<Item>> EGGS = new LinkedHashMap<>();

    static {
        for (Content.WeaponDef d : Content.WEAPONS) WEAPONS.put(d.id(), ITEMS.register(d.id(), () -> new SLSwordItem(d)));
        for (Content.StaffDef d : Content.STAVES) WEAPONS.put(d.id(), ITEMS.register(d.id(), () -> new StaffItem(d)));
        for (Content.PotionDef d : Content.POTIONS) CONSUMABLES.put(d.id(), ITEMS.register(d.id(), () -> new PotionItem(d)));
        for (Content.CrystalDef d : Content.CRYSTALS) MATERIALS.put(d.id(), ITEMS.register(d.id(), () -> new CrystalItem(d)));
        for (String id : Content.MISC) MATERIALS.put(id, ITEMS.register(id, () -> new MiscItem(id)));
        for (Content.ArmorDef d : Content.ARMOR) {
            for (ArmorItem.Type t : ArmorItem.Type.values()) {
                String id = d.id() + "_" + t.getName();
                ARMOR.put(id, ITEMS.register(id, () -> new SLArmorItem(d, t)));
            }
        }
        for (Content.BlockDef d : Content.BLOCKS) {
            BLOCK_ITEMS.put(d.id(), ITEMS.register(d.id(), () -> new BlockItem(ModBlocks.get(d.id()), new Item.Properties())));
        }
    }

    /** spawn eggs are registered after entity types exist */
    public static void registerEggs() {
        for (Content.MonsterDef d : Content.MONSTERS)
            EGGS.put(d.id(), ITEMS.register(d.id() + "_spawn_egg", () -> new ForgeSpawnEggItem(ModEntities.MONSTERS.get(d.id()), d.egg1(), d.egg2(), new Item.Properties().rarity(Rarity.EPIC))));
        for (Content.ShadowDef d : Content.SHADOWS)
            EGGS.put(d.id(), ITEMS.register(d.id() + "_spawn_egg", () -> new ForgeSpawnEggItem(ModEntities.SHADOWS.get(d.id()), 0x14082A, 0xA060FF, new Item.Properties().rarity(Rarity.EPIC))));
        for (String r : Content.NPC_ROLES)
            EGGS.put("npc_" + r, ITEMS.register("npc_" + r + "_spawn_egg", () -> new ForgeSpawnEggItem(ModEntities.NPCS.get(r), 0x2A3A6A, 0xE0E8FF, new Item.Properties())));
    }

    static { registerEggs(); }

    public static Item get(String id) {
        for (Map<String, RegistryObject<Item>> m : java.util.List.of(WEAPONS, CONSUMABLES, MATERIALS, ARMOR, BLOCK_ITEMS, EGGS)) {
            RegistryObject<Item> o = m.get(id);
            if (o != null) return o.get();
        }
        throw new IllegalArgumentException("no item " + id);
    }
}
