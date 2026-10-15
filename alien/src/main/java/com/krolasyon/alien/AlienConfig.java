package com.krolasyon.alien;

import net.minecraftforge.common.ForgeConfigSpec;

public final class AlienConfig {
    private AlienConfig() {}

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue INSTANT_KILL;
    public static final ForgeConfigSpec.BooleanValue PROTECT_STATION;
    public static final ForgeConfigSpec.BooleanValue NO_BLOCK_BREAKING;
    public static final ForgeConfigSpec.DoubleValue HEARING;
    public static final ForgeConfigSpec.DoubleValue SIGHT_RANGE;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("alien");
        INSTANT_KILL = b.comment("Uzayli yakaladigi oyuncuyu tek hamlede oldurur (oyundaki gibi). false: agir hasar verir.")
                .define("instantKill", true);
        HEARING = b.comment("Duyma mesafesi carpani").defineInRange("hearingMultiplier", 1.0D, 0.2D, 3.0D);
        SIGHT_RANGE = b.comment("Gorus mesafesi (blok)").defineInRange("sightRange", 34.0D, 8.0D, 64.0D);
        b.pop();
        b.push("world");
        PROTECT_STATION = b.comment("Hayatta kalma modunda istasyon bloklari kirilamaz").define("protectStationBlocks", true);
        NO_BLOCK_BREAKING = b.comment("Hayatta kalma/macera modunda hicbir blok kirilamaz").define("disableAllBlockBreaking", true);
        b.pop();
        SPEC = b.build();
    }
}
