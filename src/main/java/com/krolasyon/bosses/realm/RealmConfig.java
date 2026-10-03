package com.krolasyon.bosses.realm;

import net.minecraftforge.common.ForgeConfigSpec;

public final class RealmConfig {
    private RealmConfig() {}

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue RIFTS;
    public static final ForgeConfigSpec.DoubleValue RIFT_CHANCE;
    public static final ForgeConfigSpec.IntValue RIFT_MIN_DAYS;
    public static final ForgeConfigSpec.IntValue PORTAL_WAIT;
    public static final ForgeConfigSpec.BooleanValue FACTION_WARS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("portal");
        RIFTS = b.comment("Crimson rifts: realm portals that tear open on their own near players at night").define("spontaneousRifts", true);
        RIFT_CHANCE = b.comment("Chance per player per 10 seconds of night that a rift opens").defineInRange("riftChance", 0.006, 0.0, 1.0);
        RIFT_MIN_DAYS = b.comment("Minimum in-game days between two rifts for the same player").defineInRange("riftCooldownDays", 2, 0, 100);
        PORTAL_WAIT = b.comment("Ticks a survival player must stand in a realm portal").defineInRange("portalWaitTicks", 60, 1, 400);
        b.pop();
        b.push("politics");
        FACTION_WARS = b.comment("Mobs of kingdoms at war attack each other").define("factionWars", true);
        b.pop();
        SPEC = b.build();
    }
}
