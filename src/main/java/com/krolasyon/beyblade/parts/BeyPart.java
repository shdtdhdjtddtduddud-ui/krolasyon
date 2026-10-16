package com.krolasyon.beyblade.parts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One beyblade part (Metal Fight system): face bolt, energy ring (clear wheel), fusion wheel (metal wheel), spin track
 * or performance tip. Stats are the part's contribution to the assembled beyblade; a stock combo sums to about 50 per
 * stat. Wheels also choose the 3D top that is drawn, tracks set the height and tips the shape and movement.
 */
public record BeyPart(String id, PartType type, int attack, int defense, int stamina, int speed, float weight,
                      int rarity, String mesh, int height, Tip tip, Trait trait, int color, int color2) {

    /** how a performance tip behaves on the stadium floor */
    public enum Tip {
        NONE(0.30F), FLAT(0.22F), RUBBER_FLAT(0.26F), EXTREME_FLAT(0.30F), SEMI_FLAT(0.12F), SHARP(0.0F), ETERNAL_SHARP(0.0F),
        DEFENSE(0.08F), WIDE_DEFENSE(0.1F), ETERNAL_WIDE_DEFENSE(0.12F), BALL(0.12F), WIDE_BALL(0.18F), METAL_FLAT(0.2F),
        RUBBER_SEMI_FLAT(0.14F), FINAL_DRIVE(0.18F), FINAL_SURVIVE(0.06F), COAT_SHARP(0.02F);

        /** radius of the flat contact at the bottom (0 = a point) */
        public final float contact;

        Tip(float contact) { this.contact = contact; }

        public boolean rubber() { return this == RUBBER_FLAT || this == RUBBER_SEMI_FLAT || this == COAT_SHARP; }

        /** eternal / 4D tips spin freely and lose less spin in clashes */
        public boolean freeSpin() { return this == ETERNAL_SHARP || this == ETERNAL_WIDE_DEFENSE || this == FINAL_DRIVE || this == FINAL_SURVIVE; }
    }

    /** special effects of a part (mostly tracks) */
    public enum Trait { NONE, BOOST_DISK, CLAW, WING, UPPER_WING, TORNADO, DEFENSE_RING, RUBBER_RING, TWIN_HEIGHT, GRAVITY, SPIRIT, FOUR_D }

    public String nameKey() { return "part.librabeyblade." + id; }

    public String itemId() { return "part_" + id; }

    // ---------------- the catalogue ----------------
    private static final Map<String, BeyPart> ALL = new LinkedHashMap<>();
    private static final Map<PartType, List<BeyPart>> BY_TYPE = new LinkedHashMap<>();

    private static BeyPart add(BeyPart p) {
        ALL.put(p.id, p);
        BY_TYPE.computeIfAbsent(p.type, k -> new ArrayList<>()).add(p);
        return p;
    }

    private static void bolt(String id, int atk, int def, int sta, int spd, int rarity, int col, int col2) {
        add(new BeyPart(id, PartType.BOLT, atk, def, sta, spd, 1.0F, rarity, "", 0, Tip.NONE, Trait.SPIRIT, col, col2));
    }

    private static void ring(String id, int atk, int def, int sta, int spd, float w, int rarity, int col, int col2) {
        add(new BeyPart(id, PartType.RING, atk, def, sta, spd, w, rarity, "", 0, Tip.NONE, Trait.NONE, col, col2));
    }

    private static void wheel(String id, String mesh, int atk, int def, int sta, int spd, float w, int rarity, int col, int col2) {
        add(new BeyPart(id, PartType.WHEEL, atk, def, sta, spd, w, rarity, mesh, 0, Tip.NONE, Trait.NONE, col, col2));
    }

    private static void track(String id, int h, Trait trait, int atk, int def, int sta, int spd, float w, int rarity, int col, int col2) {
        add(new BeyPart(id, PartType.TRACK, atk, def, sta, spd, w, rarity, "", h, Tip.NONE, trait, col, col2));
    }

    private static void tip(String id, Tip tip, int atk, int def, int sta, int spd, float w, int rarity, int col, int col2) {
        add(new BeyPart(id, PartType.TIP, atk, def, sta, spd, w, rarity, "", 0, tip, tip == Tip.FINAL_DRIVE || tip == Tip.FINAL_SURVIVE ? Trait.FOUR_D : Trait.NONE, col, col2));
    }

    static {
        // ---- face bolts: carry the bit-beast's spirit (Blader Spirit gain) ----
        bolt("bolt_libra", 2, 3, 6, 2, 2, 0x5ACB4A, 0xF0F0F0);
        bolt("bolt_ldrago", 7, 1, 2, 3, 2, 0xC81E2A, 0x2A2A40);
        bolt("bolt_leone", 1, 7, 3, 2, 2, 0x2FA24A, 0xE8E8E8);
        bolt("bolt_eagle", 2, 4, 6, 1, 2, 0x8A40D0, 0xF0E0FF);
        bolt("bolt_pegasus", 6, 2, 2, 5, 2, 0x2E6EE0, 0xFFFFFF);
        bolt("bolt_tempo", 3, 3, 4, 3, 2, 0xD0D8E8, 0x303040);
        bolt("bolt_variares", 7, 3, 1, 3, 2, 0xD81E1E, 0xFFD040);
        bolt("bolt_beffal", 3, 4, 4, 3, 2, 0xC040B0, 0xFFC0E8);
        bolt("bolt_bigbang", 7, 2, 3, 5, 3, 0x40A0E0, 0xFF5070);
        bolt("bolt_phoenix", 3, 5, 6, 2, 3, 0x6A3AA0, 0xE8D8FF);
        bolt("bolt_inferno", 6, 3, 3, 4, 3, 0xF0B020, 0x601020);
        bolt("bolt_standard", 2, 2, 2, 2, 0, 0xB0B4BC, 0x404048);
        // ---- energy rings (clear wheels) ----
        ring("ring_libra", 3, 4, 8, 3, 3.0F, 2, 0x8ADF5A, 0x2E7A2A);
        ring("ring_ldrago", 9, 2, 3, 4, 3.2F, 2, 0xE02030, 0x600810);
        ring("ring_leone", 2, 9, 4, 2, 3.4F, 2, 0x3ACB5A, 0x105A20);
        ring("ring_eagle", 3, 5, 8, 2, 3.0F, 2, 0x9A50E0, 0x40108A);
        ring("ring_pegasus", 8, 2, 3, 7, 2.8F, 2, 0x3A7AF0, 0x0A2A80);
        ring("ring_tempo", 4, 4, 5, 4, 3.0F, 2, 0x606878, 0xE0E8F0);
        ring("ring_variares", 9, 4, 2, 4, 3.2F, 2, 0xD02020, 0x300808);
        ring("ring_beffal", 4, 5, 5, 4, 3.1F, 2, 0xF06AB0, 0x701050);
        ring("ring_bigbang", 9, 3, 4, 6, 3.3F, 3, 0x2040C0, 0xF04080);
        ring("ring_phoenix", 3, 6, 9, 2, 3.2F, 3, 0x8A3AE0, 0x30106A);
        ring("ring_inferno", 7, 4, 4, 5, 3.2F, 3, 0x801830, 0xF0B020);
        ring("ring_standard", 3, 3, 3, 3, 3.0F, 0, 0xC8D0E0, 0x6A7080);
        // ---- fusion wheels (metal wheels): the bulk of attack / defense / stamina and weight ----
        wheel("wheel_libra", "libra", 8, 12, 22, 8, 33.0F, 2, 0xB8BEC8, 0x5ACB4A);
        wheel("wheel_ldrago", "ldrago", 24, 8, 8, 12, 37.0F, 2, 0x9098A8, 0xC81E2A);
        wheel("wheel_leone", "leone", 6, 26, 12, 6, 36.0F, 2, 0xA8B0B8, 0x2FA24A);
        wheel("wheel_eagle", "eagle", 8, 16, 22, 4, 40.0F, 2, 0xB0B0C0, 0x8A40D0);
        wheel("wheel_pegasus", "pegasus", 24, 6, 8, 16, 32.0F, 2, 0xC8CCD8, 0x2E6EE0);
        wheel("wheel_tempo", "tempo", 14, 14, 14, 10, 36.0F, 2, 0xC0C4CC, 0xD0D8E8);
        wheel("wheel_variares", "variares", 26, 12, 6, 10, 37.0F, 2, 0xB8BCC4, 0xD81E1E);
        wheel("wheel_beffal", "beffal", 14, 16, 16, 8, 39.0F, 2, 0xC8CCD4, 0xF06AB0);
        wheel("wheel_bigbang", "bigbang", 28, 12, 10, 14, 42.0F, 3, 0xD0D4DC, 0x2040C0);
        wheel("wheel_phoenix", "phoenix", 8, 20, 26, 6, 41.0F, 3, 0xC0C4CC, 0x8A3AE0);
        wheel("wheel_inferno", "inferno", 22, 14, 14, 12, 40.0F, 3, 0xC8C8D0, 0xF0B020);
        int[][] commonStats = {{22, 8, 6, 12}, {12, 12, 12, 12}, {6, 22, 12, 6}, {20, 6, 8, 14}, {6, 10, 22, 6},
                {12, 10, 12, 14}, {22, 8, 8, 12}, {8, 10, 22, 8}, {8, 22, 10, 8}, {14, 12, 10, 14}};
        String[] commonIds = {"iron_bull", "frost_wolf", "jade_turtle", "thunder_viper", "shadow_bat", "sky_falcon",
                "crimson_scorpion", "golden_phoenix", "ocean_shark", "storm_fox"};
        int[] commonCol = {0xD83228, 0x6EC8FF, 0x2FA24A, 0xF5D21E, 0x6B2FB5, 0x3E8EF0, 0xA0102A, 0xFFB020, 0x1A4FA0, 0xFF7A1A};
        for (int i = 0; i < commonIds.length; i++) {
            int[] s = commonStats[i];
            wheel("wheel_" + commonIds[i], "common_" + i, s[0] - 4, s[1] - 4, s[2] - 4, s[3] - 2, 30.0F, 0, 0xB0B4BC, commonCol[i]);
        }
        // ---- spin tracks: height (85 = 8.5 mm) and special mechanisms ----
        track("track_85", 85, Trait.NONE, 3, 6, 4, 6, 1.4F, 0, 0x50545C, 0x8A9098);
        track("track_90", 90, Trait.NONE, 3, 6, 4, 5, 1.5F, 0, 0x50545C, 0x8A9098);
        track("track_100", 100, Trait.NONE, 4, 5, 5, 5, 1.6F, 0, 0x50545C, 0x8A9098);
        track("track_105", 105, Trait.NONE, 5, 4, 5, 5, 1.6F, 0, 0x404650, 0x8090A0);
        track("track_125", 125, Trait.NONE, 5, 4, 6, 4, 1.8F, 0, 0x404650, 0x8090A0);
        track("track_145", 145, Trait.NONE, 5, 4, 6, 4, 2.0F, 0, 0x404650, 0x8090A0);
        track("track_230", 230, Trait.GRAVITY, 9, 1, 4, 3, 2.8F, 1, 0x383C44, 0xA0A8B8);
        track("track_bd145", 145, Trait.BOOST_DISK, 3, 10, 6, 2, 4.0F, 1, 0x30343C, 0xC0C4CC);
        track("track_c145", 145, Trait.CLAW, 9, 3, 4, 4, 2.4F, 1, 0x30343C, 0xD8DCE4);
        track("track_w105", 105, Trait.WING, 4, 3, 9, 5, 2.0F, 1, 0x2A6ACA, 0xD0E0FF);
        track("track_uw145", 145, Trait.UPPER_WING, 8, 4, 5, 4, 2.4F, 1, 0xC04090, 0xF0B0D8);
        track("track_t125", 125, Trait.TORNADO, 3, 4, 10, 4, 1.9F, 1, 0x3A6A3A, 0xA0E0A0);
        track("track_d125", 125, Trait.DEFENSE_RING, 2, 10, 6, 3, 2.3F, 1, 0x30343C, 0xF0F0F0);
        track("track_r145", 145, Trait.RUBBER_RING, 4, 9, 4, 3, 2.2F, 1, 0x202024, 0xD02020);
        track("track_th170", 170, Trait.TWIN_HEIGHT, 7, 5, 5, 3, 2.6F, 2, 0x30343C, 0xF0C040);
        // ---- performance tips: shape, grip and movement ----
        tip("tip_f", Tip.FLAT, 8, 3, 2, 10, 1.0F, 0, 0xE8E8EC, 0x606068);
        tip("tip_rf", Tip.RUBBER_FLAT, 10, 3, 1, 13, 1.1F, 1, 0x2A2A30, 0xD02020);
        tip("tip_r2f", Tip.RUBBER_FLAT, 9, 4, 2, 12, 1.2F, 1, 0x30343C, 0x40C0E0);
        tip("tip_xf", Tip.EXTREME_FLAT, 11, 2, 1, 15, 1.0F, 2, 0xF0F0F0, 0x2060E0);
        tip("tip_sf", Tip.SEMI_FLAT, 6, 5, 5, 7, 1.0F, 0, 0xE0E0E4, 0x707078);
        tip("tip_s", Tip.SHARP, 1, 4, 12, 2, 0.9F, 0, 0xE8E8EC, 0x606068);
        tip("tip_es", Tip.ETERNAL_SHARP, 1, 4, 15, 2, 1.2F, 1, 0x60C060, 0xE0FFE0);
        tip("tip_cs", Tip.COAT_SHARP, 3, 5, 10, 4, 1.0F, 1, 0x2A2A30, 0xF0F0F0);
        tip("tip_d", Tip.DEFENSE, 2, 10, 8, 2, 1.1F, 0, 0xE8E8EC, 0x606068);
        tip("tip_wd", Tip.WIDE_DEFENSE, 2, 11, 10, 2, 1.3F, 0, 0xF0F0F4, 0x707078);
        tip("tip_ewd", Tip.ETERNAL_WIDE_DEFENSE, 2, 11, 12, 2, 1.5F, 1, 0xA040B0, 0xF0C0F0);
        tip("tip_b", Tip.BALL, 3, 9, 7, 4, 1.1F, 0, 0xE8E8EC, 0x606068);
        tip("tip_wb", Tip.WIDE_BALL, 3, 12, 6, 3, 1.3F, 0, 0x2A2A30, 0x707078);
        tip("tip_mf", Tip.METAL_FLAT, 10, 4, 3, 9, 2.4F, 1, 0xC8CCD4, 0x707880);
        tip("tip_rsf", Tip.RUBBER_SEMI_FLAT, 8, 6, 3, 9, 1.2F, 1, 0x30C0D0, 0x104050);
        tip("tip_fd", Tip.FINAL_DRIVE, 8, 7, 8, 9, 2.6F, 2, 0x3A8AE0, 0xE04060);
        tip("tip_fs", Tip.FINAL_SURVIVE, 4, 7, 11, 5, 2.4F, 2, 0x2A2A44, 0xC0C0D0);
    }

    public static BeyPart get(String id) { return ALL.get(id); }

    public static java.util.Collection<BeyPart> all() { return Collections.unmodifiableCollection(ALL.values()); }

    public static List<BeyPart> of(PartType t) { return BY_TYPE.getOrDefault(t, List.of()); }

    /** stock parts of each beyblade kind: bolt, ring, wheel, track, tip */
    public static String[] stock(String kind) {
        if (kind.startsWith("common_")) {
            int v = Integer.parseInt(kind.substring(7));
            String[] ids = {"iron_bull", "frost_wolf", "jade_turtle", "thunder_viper", "shadow_bat", "sky_falcon",
                    "crimson_scorpion", "golden_phoenix", "ocean_shark", "storm_fox"};
            String[] tips = {"tip_f", "tip_sf", "tip_d", "tip_f", "tip_s", "tip_sf", "tip_f", "tip_s", "tip_d", "tip_sf"};
            return new String[]{"bolt_standard", "ring_standard", "wheel_" + ids[Math.floorMod(v, 10)], "track_100", tips[Math.floorMod(v, 10)]};
        }
        return switch (kind) {
            case "ldrago" -> new String[]{"bolt_ldrago", "ring_ldrago", "wheel_ldrago", "track_145", "tip_fs"};
            case "leone" -> new String[]{"bolt_leone", "ring_leone", "wheel_leone", "track_145", "tip_wb"};
            case "eagle" -> new String[]{"bolt_eagle", "ring_eagle", "wheel_eagle", "track_145", "tip_wd"};
            case "pegasus" -> new String[]{"bolt_pegasus", "ring_pegasus", "wheel_pegasus", "track_105", "tip_rf"};
            case "tempo" -> new String[]{"bolt_tempo", "ring_tempo", "wheel_tempo", "track_145", "tip_wd"};
            case "variares" -> new String[]{"bolt_variares", "ring_variares", "wheel_variares", "track_145", "tip_wb"};
            case "beffal" -> new String[]{"bolt_beffal", "ring_beffal", "wheel_beffal", "track_uw145", "tip_ewd"};
            default -> new String[]{"bolt_libra", "ring_libra", "wheel_libra", "track_t125", "tip_es"};
        };
    }
}
