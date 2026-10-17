package com.krolasyon.mirror;

import net.minecraftforge.common.ForgeConfigSpec;

public final class Config {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue MIMIC_SECONDS;
    public static final ForgeConfigSpec.IntValue AWAKEN_SECONDS;
    public static final ForgeConfigSpec.IntValue BUILD_INTERVAL_TICKS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        MIMIC_SECONDS = b.comment("How long the reflection only copies your movements after the mirror is placed")
                .defineInRange("mimicSeconds", 120, 1, 36000);
        AWAKEN_SECONDS = b.comment("How long the reflection hesitates, waves and knocks before it starts building")
                .defineInRange("awakenSeconds", 60, 1, 36000);
        BUILD_INTERVAL_TICKS = b.comment("Ticks between two blocks placed by the reflection in its world")
                .defineInRange("buildIntervalTicks", 15, 1, 1200);
        SPEC = b.build();
    }

    private Config() {}
}
