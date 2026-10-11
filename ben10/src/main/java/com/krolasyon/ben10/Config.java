package com.krolasyon.ben10;

import net.minecraftforge.common.ForgeConfigSpec;

public final class Config {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue DURATION;
    public static final ForgeConfigSpec.IntValue RECHARGE;
    public static final ForgeConfigSpec.BooleanValue GRIEFING;
    public static final ForgeConfigSpec.DoubleValue DAMAGE_MULT;
    public static final ForgeConfigSpec.DoubleValue COOLDOWN_MULT;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("omnitrix");
        DURATION = b.comment("Transformation time in seconds before the Omnitrix times out (0 = unlimited)").defineInRange("durationSeconds", 300, 0, 36000);
        RECHARGE = b.comment("Recharge time in seconds after timing out").defineInRange("rechargeSeconds", 20, 0, 3600);
        GRIEFING = b.comment("Abilities affect the world (melt/freeze/break soft blocks, temporary crystal and ice walls)").define("abilityGriefing", true);
        DAMAGE_MULT = b.comment("Multiplier for all ability damage").defineInRange("damageMultiplier", 1.0, 0.0, 20.0);
        COOLDOWN_MULT = b.comment("Multiplier for all ability cooldowns").defineInRange("cooldownMultiplier", 1.0, 0.0, 10.0);
        b.pop();
        SPEC = b.build();
    }

    private Config() {}

    public static int durationTicks() {
        try { return DURATION.get() * 20; } catch (Exception e) { return 6000; }
    }

    public static int rechargeTicks() {
        try { return RECHARGE.get() * 20; } catch (Exception e) { return 400; }
    }

    public static boolean grief() {
        try { return GRIEFING.get(); } catch (Exception e) { return true; }
    }

    public static float dmg() {
        try { return DAMAGE_MULT.get().floatValue(); } catch (Exception e) { return 1F; }
    }

    public static float cd() {
        try { return COOLDOWN_MULT.get().floatValue(); } catch (Exception e) { return 1F; }
    }
}
