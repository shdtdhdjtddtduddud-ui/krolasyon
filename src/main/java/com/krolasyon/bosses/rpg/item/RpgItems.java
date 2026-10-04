package com.krolasyon.bosses.rpg.item;

import com.krolasyon.bosses.KrolasyonBosses;
import com.krolasyon.bosses.rpg.def.MonsterDef;
import com.krolasyon.bosses.rpg.def.RpgDefs;
import com.krolasyon.bosses.rpg.def.SpellDef;
import com.krolasyon.bosses.rpg.def.SwordDef;
import com.krolasyon.bosses.rpg.registry.RpgEntities;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RpgItems {
    private RpgItems() {}

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, KrolasyonBosses.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, KrolasyonBosses.MODID);

    public static final RegistryObject<Item> COPPER_COIN = ITEMS.register("copper_coin", () -> new CoinItem(1, "Bakır Sikke", new Item.Properties()));
    public static final RegistryObject<Item> SILVER_COIN = ITEMS.register("silver_coin", () -> new CoinItem(10, "Gümüş Sikke", new Item.Properties()));
    public static final RegistryObject<Item> GOLD_COIN = ITEMS.register("gold_coin", () -> new CoinItem(100, "Altın Sikke", new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> SPELL_TOME = ITEMS.register("spell_tome", () -> new SpellTomeItem(new Item.Properties().stacksTo(1)));

    public static final List<RegistryObject<Item>> STAFFS = new ArrayList<>();
    private static final String[] STAFF_NAMES = {"Çırak Asası", "Büyücü Asası", "Usta Büyücü Asası", "Başbüyücü Asası", "Kadim Yıldız Asası"};

    private static RegistryObject<Item> misc(String id, RpgMiscItem.Kind kind, String title, String desc, Item.Properties p) {
        return ITEMS.register(id, () -> new RpgMiscItem(kind, title, desc, p));
    }

    public static final RegistryObject<Item> MANA_POTION = misc("mana_potion", RpgMiscItem.Kind.MANA_POTION, "Mana İksiri", "40 mana yeniler.", new Item.Properties().stacksTo(16));
    public static final RegistryObject<Item> GREATER_MANA_POTION = misc("greater_mana_potion", RpgMiscItem.Kind.GREATER_MANA_POTION, "Büyük Mana İksiri", "120 mana yeniler.", new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON));
    public static final RegistryObject<Item> HEALING_POTION = misc("healing_potion", RpgMiscItem.Kind.HEALING_POTION, "Şifa İksiri", "Yaralarını sarar.", new Item.Properties().stacksTo(16));
    public static final RegistryObject<Item> RING = misc("engagement_ring", RpgMiscItem.Kind.RING, "Nişan Yüzüğü", "Sevdiğin birine evlenme teklif etmek için yanında taşı.", new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    public static final RegistryObject<Item> SEAL = misc("ancient_seal", RpgMiscItem.Kind.SEAL, "Kadim Mühür", "Mühürlü bir canavarı çağırır.", new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
    public static final RegistryObject<Item> ESSENCE = misc("monster_essence", RpgMiscItem.Kind.ESSENCE, "Canavar Özü", "Maceracılar Loncası bunu iyi fiyata alır.", new Item.Properties());
    public static final RegistryObject<Item> HERB = misc("healing_herb", RpgMiscItem.Kind.HERB, "Şifa Otu", "Ateşli hastalığa iyi gelen nadir bir ot.", new Item.Properties());
    public static final RegistryObject<Item> ELF_PENDANT = misc("elf_pendant", RpgMiscItem.Kind.ELF_PENDANT, "Lyra'nın Kolyesi", "Gümüşyaprak hanedanının mührünü taşıyan bir kolye.", new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    public static final RegistryObject<Item> ROYAL_SEAL = misc("royal_seal", RpgMiscItem.Kind.ROYAL_SEAL, "Kraliyet Mührü", "Aldoria tahtının mührü. Taşıyana kapılar açılır.", new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
    public static final RegistryObject<Item> DEED = misc("house_deed", RpgMiscItem.Kind.DEED, "Ev Tapusu", "Sağ tıkla: durduğun yeri evin yap.", new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final RegistryObject<Item> TROPHY = misc("boss_trophy", RpgMiscItem.Kind.TROPHY, "Zafer Ganimeti", "Yenilmiş bir efsanevi canavarın parçası. Loncada büyük şöhret getirir.", new Item.Properties().rarity(Rarity.EPIC));
    public static final RegistryObject<Item> LETTER = misc("sealed_letter", RpgMiscItem.Kind.LETTER, "Mühürlü Mektup", "Sahibine ulaştırılması gereken bir mektup.", new Item.Properties().stacksTo(1));

    public static final Map<String, RegistryObject<Item>> SWORDS = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<Item>> EGGS = new LinkedHashMap<>();

    static {
        for (int i = 0; i < STAFF_NAMES.length; i++) {
            int t = i;
            STAFFS.add(ITEMS.register("staff_" + (i + 1), () -> new StaffItem(t, STAFF_NAMES[t], new Item.Properties().stacksTo(1).rarity(t >= 4 ? Rarity.EPIC : t >= 2 ? Rarity.RARE : Rarity.COMMON))));
        }
        for (SwordDef s : RpgDefs.SWORDS) SWORDS.put(s.id(), ITEMS.register(s.id(), () -> new RpgSwordItem(s)));
        for (MonsterDef d : RpgDefs.MONSTERS) egg(d);
        for (MonsterDef d : RpgDefs.BOSSES) egg(d);
    }

    private static void egg(MonsterDef d) {
        EGGS.put(d.id(), ITEMS.register(d.id() + "_spawn_egg", () -> new ForgeSpawnEggItem(RpgEntities.MOBS.get(d.id()), d.baseColor(), d.eyeColor(),
                new Item.Properties().rarity(d.boss() ? Rarity.EPIC : Rarity.COMMON)) {
            @Override
            public Component getName(ItemStack stack) { return Component.literal(d.name() + (d.boss() ? " (Boss)" : "") + " Yumurtası"); }
        }));
    }

    public static Item sword(String id) { return SWORDS.get(id).get(); }

    public static final RegistryObject<CreativeModeTab> TAB_MOBS = TABS.register("rpg_monsters", () -> CreativeModeTab.builder()
            .title(Component.literal("Krolasyon: Canavarlar"))
            .icon(() -> new ItemStack(EGGS.get("dire_wolf").get()))
            .displayItems((params, out) -> { for (MonsterDef d : RpgDefs.MONSTERS) out.accept(EGGS.get(d.id()).get()); })
            .build());

    public static final RegistryObject<CreativeModeTab> TAB_BOSSES = TABS.register("rpg_bosses", () -> CreativeModeTab.builder()
            .title(Component.literal("Krolasyon: Bosslar"))
            .icon(() -> new ItemStack(EGGS.get("dragon_queen_vaelith").get()))
            .displayItems((params, out) -> {
                for (MonsterDef d : RpgDefs.BOSSES) out.accept(EGGS.get(d.id()).get());
                for (MonsterDef d : RpgDefs.BOSSES) out.accept(RpgMiscItem.seal(d));
            })
            .build());

    public static final RegistryObject<CreativeModeTab> TAB_GEAR = TABS.register("rpg_gear", () -> CreativeModeTab.builder()
            .title(Component.literal("Krolasyon: Silahlar ve Büyüler"))
            .icon(() -> new ItemStack(SWORDS.get("dragonbane").get()))
            .displayItems((params, out) -> {
                for (RegistryObject<Item> s : SWORDS.values()) out.accept(s.get());
                for (RegistryObject<Item> s : STAFFS) out.accept(s.get());
                for (SpellDef s : RpgDefs.SPELLS) out.accept(SpellTomeItem.of(s));
            })
            .build());

    public static final RegistryObject<CreativeModeTab> TAB_ITEMS = TABS.register("rpg_items", () -> CreativeModeTab.builder()
            .title(Component.literal("Krolasyon: Eşyalar"))
            .icon(() -> new ItemStack(GOLD_COIN.get()))
            .displayItems((params, out) -> {
                out.accept(COPPER_COIN.get()); out.accept(SILVER_COIN.get()); out.accept(GOLD_COIN.get());
                out.accept(MANA_POTION.get()); out.accept(GREATER_MANA_POTION.get()); out.accept(HEALING_POTION.get());
                out.accept(RING.get()); out.accept(DEED.get()); out.accept(ESSENCE.get()); out.accept(HERB.get());
                out.accept(ELF_PENDANT.get()); out.accept(ROYAL_SEAL.get()); out.accept(TROPHY.get()); out.accept(LETTER.get());
            })
            .build());
}
