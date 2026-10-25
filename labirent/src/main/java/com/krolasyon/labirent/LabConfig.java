package com.krolasyon.labirent;

import net.minecraftforge.common.ForgeConfigSpec;

public final class LabConfig {
    private LabConfig() {}

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue AUTO_JOIN;
    public static final ForgeConfigSpec.IntValue MONTH_DAYS;
    public static final ForgeConfigSpec.IntValue GRIEVERS_PER_NIGHT;
    public static final ForgeConfigSpec.IntValue SHIFT_SECTORS;
    public static final ForgeConfigSpec.BooleanValue ALLOW_WALL_CLIMB;
    public static final ForgeConfigSpec.BooleanValue HARDCORE_VENOM;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("labirent");
        AUTO_JOIN = b.comment("Oyunculari otomatik olarak Ayrik'a (labirent boyutuna) goturur").define("autoJoin", true);
        MONTH_DAYS = b.comment("Bir 'Minecraft ayi' kac oyun gunudur (Kutu bu araliklarla yeni bir Ayrikli getirir)").defineInRange("monthDays", 8, 1, 120);
        GRIEVERS_PER_NIGHT = b.comment("Her gece labirentte dolasan temel Kederci sayisi").defineInRange("grieversPerNight", 4, 0, 40);
        SHIFT_SECTORS = b.comment("Her gece yer degistiren labirent bolumu sayisi (1-8)").defineInRange("shiftSectors", 3, 1, 8);
        ALLOW_WALL_CLIMB = b.comment("Duvarlarin ustune cikmaya izin ver (hile)").define("allowWallClimb", false);
        HARDCORE_VENOM = b.comment("Serumsuz Kederci zehri oldurur").define("hardcoreVenom", true);
        b.pop();
        SPEC = b.build();
    }
}
