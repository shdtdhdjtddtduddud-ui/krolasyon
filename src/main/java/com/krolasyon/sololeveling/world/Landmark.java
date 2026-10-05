package com.krolasyon.sololeveling.world;

import com.krolasyon.sololeveling.registry.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

import static com.krolasyon.sololeveling.world.CityPlan.*;

/** Fixed buildings of Seoul. Columns are given in building-area coordinates (ax, az in 0..47, north = az 0). */
public enum Landmark {
    PLAZA(0, 0), ASSOCIATION(1, 0), HUNTERS(-1, 0), WHITE_TIGER(0, 1), FIEND(0, -1), KNIGHTS(1, 1), AHJIN(-1, -1),
    HOSPITAL(1, -1), MARKET(-1, 1), HOME(2, 0);

    final int sx, sz;

    Landmark(int sx, int sz) {
        this.sx = sx;
        this.sz = sz;
    }

    static Landmark at(int sx, int sz) {
        for (Landmark l : values()) if (l.sx == sx && l.sz == sz) return l;
        return null;
    }

    static final String[] NORTH_PORTALS = {"double_dungeon", "job_change", "demon_castle"};
    static final String[] SOUTH_PORTALS = {"jeju_island", "overworld", "home"};

    void column(CityPlan.Writer w, int x, int z, int ax, int az) {
        switch (this) {
            case PLAZA -> plaza(w, x, z, ax, az);
            case ASSOCIATION -> hall(w, x, z, ax, az, 6, 8, 41, 45, 118, b(Blocks.WHITE_CONCRETE), b(ModBlocks.GLASS_FACADE.get()), b(ModBlocks.CONCRETE_PANEL.get()), b(ModBlocks.ASSOCIATION_EMBLEM.get()), 0);
            case HUNTERS -> hall(w, x, z, ax, az, 8, 8, 39, 41, 92, b(Blocks.BLACK_CONCRETE), b(ModBlocks.DARK_WINDOW.get()), b(ModBlocks.NEON_GOLD.get()), b(ModBlocks.EMBLEM_HUNTERS.get()), 1);
            case WHITE_TIGER -> hall(w, x, z, ax, az, 8, 8, 39, 41, 74, b(Blocks.WHITE_CONCRETE), b(ModBlocks.GLASS_FACADE.get()), b(Blocks.SMOOTH_QUARTZ), b(ModBlocks.EMBLEM_WHITE_TIGER.get()), 2);
            case FIEND -> hall(w, x, z, ax, az, 8, 8, 39, 41, 66, b(ModBlocks.DARK_PANEL.get()), b(Blocks.RED_STAINED_GLASS), b(ModBlocks.NEON_RED.get()), b(ModBlocks.EMBLEM_FIEND.get()), 3);
            case KNIGHTS -> hall(w, x, z, ax, az, 8, 8, 39, 41, 58, b(Blocks.LIGHT_GRAY_CONCRETE), b(Blocks.LIGHT_BLUE_STAINED_GLASS), b(ModBlocks.NEON_BLUE.get()), b(ModBlocks.EMBLEM_KNIGHTS.get()), 4);
            case AHJIN -> hall(w, x, z, ax, az, 10, 8, 37, 39, 50, b(ModBlocks.DARK_PANEL.get()), b(Blocks.PURPLE_STAINED_GLASS), b(ModBlocks.NEON_PURPLE.get()), b(ModBlocks.EMBLEM_AHJIN.get()), 5);
            case HOSPITAL -> hall(w, x, z, ax, az, 6, 8, 41, 37, 34, b(Blocks.WHITE_CONCRETE), b(Blocks.LIGHT_BLUE_STAINED_GLASS), b(Blocks.WHITE_CONCRETE), b(ModBlocks.NEON_RED.get()), 6);
            case MARKET -> market(w, x, z, ax, az);
            case HOME -> {
                apartments(w, x, z, ax, az, sx, sz);
                if (ax == 24 && az == 4) w.npc(x, GROUND + 1, z, "jinah", 180);
            }
        }
    }

    // ------------------------------------------------------------------ gate plaza

    private static void plaza(CityPlan.Writer w, int x, int z, int ax, int az) {
        int g = GROUND;
        int dx = ax - 24, dz = az - 24;
        int d2 = dx * dx + dz * dz;
        // floor pattern: concentric rings
        BlockState floor;
        if (d2 < 30) floor = b(Blocks.POLISHED_DEEPSLATE);
        else if (d2 < 42) floor = b(ModBlocks.RUNE_BRICKS.get());
        else {
            int ring = (int) Math.sqrt(d2) / 3;
            floor = ring % 2 == 0 ? b(Blocks.SMOOTH_QUARTZ) : b(Blocks.POLISHED_ANDESITE);
            if (Math.abs(dx) == Math.abs(dz)) floor = b(Blocks.POLISHED_BLACKSTONE);
        }
        w.set(x, g, z, floor);
        // central fountain with a floating mana crystal pillar
        if (d2 < 30 && d2 >= 12) {
            w.set(x, g + 1, z, d2 >= 22 ? b(Blocks.POLISHED_DEEPSLATE_WALL) : b(Blocks.WATER));
            if (d2 < 22) w.set(x, g, z, b(Blocks.DARK_PRISMARINE));
        }
        if (d2 < 12) {
            w.set(x, g + 1, z, b(Blocks.POLISHED_DEEPSLATE));
            if (d2 <= 2) {
                w.set(x, g + 2, z, b(Blocks.POLISHED_DEEPSLATE));
                if (d2 == 0) {
                    for (int y = 3; y <= 8; y++) w.set(x, g + y, z, y % 2 == 0 ? b(Blocks.AMETHYST_BLOCK) : b(ModBlocks.MANA_CRYSTAL_ORE.get()));
                    w.set(x, g + 9, z, b(Blocks.SEA_LANTERN));
                    w.set(x, g + 10, z, b(Blocks.AMETHYST_CLUSTER));
                }
            } else w.set(x, g + 2, z, b(Blocks.WATER));
        }
        // lamp posts on the diagonals
        if (Math.abs(dx) == 12 && Math.abs(dz) == 12) {
            for (int y = 1; y <= 4; y++) w.set(x, g + y, z, b(Blocks.POLISHED_BLACKSTONE_WALL));
            w.set(x, g + 5, z, b(Blocks.SEA_LANTERN));
        }
        // portal arches on the north and south edges
        for (int i = 0; i < 3; i++) {
            int cx = 8 + i * 16;
            arch(w, x, z, ax, az, cx, 4, NORTH_PORTALS[i], i);
            arch(w, x, z, ax, az, cx, 43, SOUTH_PORTALS[i], i + 3);
        }
        // LED news wall on the east side
        if (ax == 45 && az >= 18 && az <= 30) {
            for (int y = 1; y <= 6; y++) w.set(x, g + y, z, y == 1 || y == 6 ? b(Blocks.POLISHED_DEEPSLATE) : b(ModBlocks.NEWS_BOARD.get()).setValue(HorizontalDirectionalBlock.FACING, Direction.WEST));
        }
        if (ax == 2 && az >= 20 && az <= 28) {
            for (int y = 1; y <= 3; y++) w.set(x, g + y, z, b(Blocks.SPRUCE_SLAB));
        }
        // people
        if (ax == 24 && az == 16) w.npc(x, g + 1, z, "guide", 0);
        if (ax == 42 && az == 24) w.npc(x, g + 1, z, "reporter", 90);
        if (ax == 18 && az == 34) w.npc(x, g + 1, z, "citizen", 200);
        if (ax == 32 && az == 12) w.npc(x, g + 1, z, "hunter", 30);
    }

    private static final BlockState[] ARCH_ACCENT = {
            b(ModBlocks.NEON_GOLD.get()), b(ModBlocks.NEON_RED.get()), b(ModBlocks.NEON_PURPLE.get()),
            b(ModBlocks.NEON_BLUE.get()), b(Blocks.SEA_LANTERN), b(ModBlocks.NEON_GOLD.get())};

    /** A 7 wide, 8 tall gate arch whose opening is filled with portal blocks. */
    private static void arch(CityPlan.Writer w, int x, int z, int ax, int az, int cx, int cz, String target, int idx) {
        if (az != cz || ax < cx - 3 || ax > cx + 3) {
            if ((az == cz - 1 || az == cz + 1) && ax >= cx - 3 && ax <= cx + 3) w.set(x, GROUND, z, b(Blocks.POLISHED_BLACKSTONE_BRICKS));
            return;
        }
        int g = GROUND;
        int rel = ax - cx;
        w.set(x, g, z, b(Blocks.POLISHED_BLACKSTONE_BRICKS));
        for (int y = 1; y <= 8; y++) {
            boolean side = Math.abs(rel) == 3;
            boolean topBar = y >= 7;
            if (side || topBar) {
                BlockState s = side ? (y % 3 == 0 ? b(ModBlocks.RUNE_BRICKS.get()) : b(Blocks.POLISHED_BLACKSTONE_BRICKS)) : (y == 8 ? ARCH_ACCENT[idx] : b(Blocks.POLISHED_BLACKSTONE_BRICKS));
                if (y == 8 && side) s = b(Blocks.POLISHED_BLACKSTONE_BRICK_SLAB);
                w.set(x, g + y, z, s);
            } else {
                w.portal(x, g + y, z, target);
            }
        }
        if (rel == 0) w.set(x, g + 9, z, ARCH_ACCENT[idx]);
    }

    // ------------------------------------------------------------------ halls (Association, guilds, hospital)

    /**
     * A landmark tower with an 8 high glass lobby, an emblem above the door (north side), a reception desk and staff.
     * kind: 0 association, 1..5 guilds, 6 hospital
     */
    private static void hall(CityPlan.Writer w, int x, int z, int ax, int az, int x0, int z0, int x1, int z1, int height,
                             BlockState frame, BlockState glass, BlockState band, BlockState emblem, int kind) {
        int g = GROUND;
        // forecourt
        if (ax < x0 || ax > x1 || az < z0 || az > z1) {
            BlockState f = (ax + az) % 2 == 0 ? b(Blocks.POLISHED_ANDESITE) : b(Blocks.SMOOTH_STONE);
            if (az < z0 && Math.abs(ax - (x0 + x1) / 2) <= 2) f = b(Blocks.POLISHED_BLACKSTONE_BRICKS);
            w.set(x, g, z, f);
            if (az == z0 - 3 && (ax == (x0 + x1) / 2 - 4 || ax == (x0 + x1) / 2 + 5)) {
                for (int y = 1; y <= 6; y++) w.set(x, g + y, z, y == 6 ? band : frame);
                w.set(x, g + 7, z, emblem);
            }
            if ((ax == x0 - 2 || ax == x1 + 2) && az % 6 == 0 && az > z0) planterTree(w, x, z);
            return;
        }
        int bx = ax - x0, bz = az - z0, wd = x1 - x0 + 1, dp = z1 - z0 + 1;
        boolean edgeX = bx == 0 || bx == wd - 1, edgeZ = bz == 0 || bz == dp - 1;
        boolean perim = edgeX || edgeZ, corner = edgeX && edgeZ;
        int mid = wd / 2;
        w.set(x, g, z, (bx + bz) % 2 == 0 ? b(Blocks.POLISHED_DIORITE) : b(Blocks.SMOOTH_QUARTZ));
        if (kind == 0 && !perim && (bx - mid) * (bx - mid) + (bz - 18) * (bz - 18) <= 9) w.set(x, g, z, emblem);
        for (int y = 1; y <= height; y++) {
            BlockState s = null;
            if (perim) {
                if (corner || (edgeZ && bx % 6 == 0) || (edgeX && bz % 6 == 0)) s = frame;
                else if (y == 8 || (y > 8 && (y - 8) % 4 == 0)) s = band;
                else if (y < 8) {
                    boolean door = bz == 0 && Math.abs(bx - mid) <= 1 && y <= 4;
                    s = door ? Blocks.AIR.defaultBlockState() : b(Blocks.GLASS);
                } else s = hash(x / 2, y / 4, z / 2 + kind) % 10 < 3 ? b(ModBlocks.OFFICE_WINDOW.get()) : glass;
                // emblem panel above the door
                if (bz == 0 && Math.abs(bx - mid) <= 2 && y >= 10 && y <= 14) s = (Math.abs(bx - mid) == 2 || y == 10 || y == 14) ? frame : emblem;
                // hospital red cross
                if (kind == 6 && bz == 0 && y >= 18 && y <= 26 && ((Math.abs(bx - mid) <= 1) || (y >= 21 && y <= 23 && Math.abs(bx - mid) <= 4))) s = emblem;
            } else if (y > 8 && (y - 8) % 4 == 0 && y < height) {
                s = b(Blocks.SMOOTH_STONE);
                // open atrium above the lobby
                if (Math.abs(bx - mid) <= 3 && bz >= 6 && bz <= 12 && y < 30) s = null;
            }
            if (s != null) w.set(x, g + y, z, s);
        }
        w.set(x, g + height + 1, z, perim ? b(Blocks.SMOOTH_STONE_SLAB) : b(Blocks.SMOOTH_STONE));
        if (bx == mid && bz == dp / 2) {
            for (int y = 2; y < 12; y++) w.set(x, g + height + y, z, b(Blocks.IRON_BARS));
            w.set(x, g + height + 12, z, band);
        }
        if (perim) return;
        // ---- lobby furniture
        int deskZ = dp - 10;
        if (bz == deskZ && Math.abs(bx - mid) <= 6) {
            w.set(x, g + 1, z, b(Blocks.SMOOTH_QUARTZ));
            w.set(x, g + 2, z, b(Blocks.SMOOTH_QUARTZ_SLAB));
        }
        if (bz == deskZ + 1 && Math.abs(bx - mid) <= 6 && Math.abs(bx - mid) % 3 == 0) w.set(x, g + 1, z, b(Blocks.SPRUCE_STAIRS).setValue(StairBlock.FACING, Direction.SOUTH));
        if (bz == dp - 2 && Math.abs(bx - mid) <= 5) for (int y = 1; y <= 6; y++) w.set(x, g + y, z, y == 3 || y == 4 ? emblem : frame);
        // sofas and plants
        if ((bz == 6 || bz == 7) && (bx == 3 || bx == wd - 4)) w.set(x, g + 1, z, b(Blocks.POTTED_BAMBOO));
        if (bz == 9 && bx >= 3 && bx <= 6) w.set(x, g + 1, z, b(Blocks.BLACK_WOOL));
        if (bz == 9 && bx >= wd - 7 && bx <= wd - 4) w.set(x, g + 1, z, b(Blocks.BLACK_WOOL));
        if (bx == 2 && bz == 12) w.set(x, g + 2, z, b(ModBlocks.NEWS_BOARD.get()).setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
        // elevators
        if ((bx == 1 || bx == wd - 2) && bz >= dp - 6 && bz <= dp - 3) for (int y = 1; y <= 7; y++) w.set(x, g + y, z, b(Blocks.IRON_BLOCK));
        // staff
        if (bx == mid && bz == deskZ + 2) w.npc(x, g + 1, z, staffRole(kind), 180);
        if (bx == mid - 6 && bz == 14) w.npc(x, g + 1, z, secondRole(kind), 160);
        if (bx == mid + 6 && bz == 12) w.npc(x, g + 1, z, thirdRole(kind), 200);
    }

    private static String staffRole(int kind) {
        return switch (kind) {
            case 0 -> "receptionist";
            case 1 -> "master_hunters";
            case 2 -> "master_white_tiger";
            case 3 -> "master_fiend";
            case 4 -> "master_knights";
            case 5 -> "yoo_jinho";
            default -> "healer";
        };
    }

    private static String secondRole(int kind) {
        return switch (kind) {
            case 0 -> "woo_jinchul";
            case 1 -> "cha_haein";
            case 6 -> "citizen";
            default -> "hunter";
        };
    }

    private static String thirdRole(int kind) {
        return switch (kind) {
            case 0 -> "chairman";
            case 6 -> "healer";
            default -> "hunter";
        };
    }

    // ------------------------------------------------------------------ market street

    private static void market(CityPlan.Writer w, int x, int z, int ax, int az) {
        int g = GROUND;
        boolean street = az >= 20 && az <= 27;
        w.set(x, g, z, street ? ((ax + az) % 3 == 0 ? b(Blocks.STONE_BRICKS) : b(Blocks.POLISHED_ANDESITE)) : b(ModBlocks.SIDEWALK.get()));
        if (street) {
            if (az == 23 && ax % 8 == 4) {
                for (int y = 1; y <= 4; y++) w.set(x, g + y, z, b(Blocks.DARK_OAK_FENCE));
                w.set(x, g + 5, z, b(Blocks.LANTERN));
            }
            if (ax == 10 && az == 24) w.npc(x, g + 1, z, "citizen", 90);
            if (ax == 30 && az == 22) w.npc(x, g + 1, z, "citizen", 270);
            return;
        }
        // stalls: 8 wide, 12 deep, on both sides of the street
        int stall = ax / 8, px = ax % 8;
        boolean north = az < 20;
        int pz = north ? 19 - az : az - 28; // 0 = street side
        if (pz > 11) return;
        int h = hash(stall, north ? 1 : 2, 55);
        BlockState[] wool = {b(Blocks.RED_WOOL), b(Blocks.BLUE_WOOL), b(Blocks.YELLOW_WOOL), b(Blocks.GREEN_WOOL), b(Blocks.ORANGE_WOOL), b(Blocks.PURPLE_WOOL)};
        BlockState awning = wool[h % wool.length];
        boolean side = px == 0 || px == 7;
        if (side || pz == 11) for (int y = 1; y <= 4; y++) w.set(x, g + y, z, b(Blocks.SPRUCE_PLANKS));
        if (pz <= 10) w.set(x, g + 5, z, pz == 0 ? (px % 2 == 0 ? awning : b(Blocks.WHITE_WOOL)) : b(Blocks.SPRUCE_PLANKS));
        if (pz == 2 && !side) {
            w.set(x, g + 1, z, b(Blocks.SPRUCE_PLANKS));
            w.set(x, g + 2, z, px % 3 == 0 ? b(Blocks.BARREL) : b(Blocks.SPRUCE_SLAB).setValue(SlabBlock.TYPE, SlabType.BOTTOM));
        }
        if (pz == 10 && !side && px % 2 == 1) w.set(x, g + 1, z, b(Blocks.CHEST).setValue(ChestBlock.FACING, north ? Direction.SOUTH : Direction.NORTH));
        if (px == 4 && pz == 5) {
            String role = stall == 1 && north ? "merchant" : stall == 4 && !north ? "blacksmith" : stall == 3 && north ? "alchemist" : null;
            if (role != null) w.npc(x, g + 1, z, role, north ? 0 : 180);
        }
    }
}
