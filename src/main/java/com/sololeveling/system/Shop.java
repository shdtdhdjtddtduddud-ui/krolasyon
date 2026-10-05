package com.sololeveling.system;

import java.util.LinkedHashMap;
import java.util.Map;

/** Merchant price list (gold). */
public final class Shop {
    private Shop() {}

    public static final Map<String, Integer> PRICES = new LinkedHashMap<>();

    static {
        PRICES.put("hp_potion_small", 100);
        PRICES.put("hp_potion_medium", 400);
        PRICES.put("hp_potion_large", 1500);
        PRICES.put("mp_potion_small", 120);
        PRICES.put("mp_potion_large", 1800);
        PRICES.put("antidote", 80);
        PRICES.put("stamina_potion", 200);
        PRICES.put("hunter_dagger", 300);
        PRICES.put("iron_blade", 900);
        PRICES.put("mithril_sword", 5000);
        PRICES.put("hunter_helmet", 700);
        PRICES.put("hunter_chestplate", 1100);
        PRICES.put("hunter_leggings", 950);
        PRICES.put("hunter_boots", 650);
        PRICES.put("dungeon_key", 800);
        PRICES.put("red_gate_key", 4000);
        PRICES.put("world_map", 200);
        PRICES.put("rune_stone", 3000);
        PRICES.put("skill_scroll", 5000);
        PRICES.put("rebirth_elixir", 6000);
        PRICES.put("elixir_of_life", 8000);
    }
}
