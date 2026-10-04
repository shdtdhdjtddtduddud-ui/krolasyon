package com.krolasyon.bosses.rpg.npc;

/** What an NPC does in the world. Decides look, equipment, AI, trades and dialogue options. */
public enum NpcRole {
    PEASANT("Köylü", Outfit.COMMON, false),
    BEGGAR("Dilenci", Outfit.RAGS, false),
    WORKER("Zanaatkâr", Outfit.WORKER, false),
    MERCHANT("Tüccar", Outfit.FINE, false),
    BLACKSMITH("Demirci", Outfit.SMITH, false),
    MAGE("Büyücü", Outfit.ROBE, true),
    PRIEST("Rahip", Outfit.PRIEST, false),
    GUARD("Muhafız", Outfit.COMMON, true),
    KNIGHT("Şövalye", Outfit.NOBLE, true),
    NOBLE("Soylu", Outfit.NOBLE, false),
    RULER("Hükümdar", Outfit.ROYAL, true),
    GUILD_MASTER("Lonca Ustası", Outfit.ADVENTURER, true),
    INNKEEPER("Hancı", Outfit.WORKER, false),
    SLAVER("Köle Tüccarı", Outfit.BANDIT, true),
    SLAVE("Köle", Outfit.RAGS, false),
    BANDIT("Haydut", Outfit.BANDIT, true),
    ADVENTURER("Maceracı", Outfit.ADVENTURER, true),
    CHILD("Çocuk", Outfit.COMMON, false),
    SOLDIER("Asker", Outfit.COMMON, true),
    FAMILY("Aile", Outfit.RAGS, false);

    public final String title;
    public final Outfit outfit;
    public final boolean fighter;

    NpcRole(String title, Outfit outfit, boolean fighter) {
        this.title = title;
        this.outfit = outfit;
        this.fighter = fighter;
    }

    public static NpcRole of(int i) { return values()[Math.floorMod(i, values().length)]; }

    public boolean trades() {
        return this == MERCHANT || this == BLACKSMITH || this == MAGE || this == PRIEST || this == INNKEEPER || this == GUILD_MASTER;
    }

    public enum Outfit { RAGS, COMMON, WORKER, FINE, SMITH, ROBE, PRIEST, NOBLE, ROYAL, BANDIT, ADVENTURER }
}
