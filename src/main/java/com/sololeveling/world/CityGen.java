package com.sololeveling.world;

import com.sololeveling.entity.NpcEntity;
import com.sololeveling.gen.Content;
import com.sololeveling.registry.ModBlocks;
import com.sololeveling.registry.ModDimensions;
import com.sololeveling.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;

/** Deterministic, per-column procedural city generator used by the Hunter World regions. */
public final class CityGen {
    private CityGen() {}

    public static final int CELL = 40, ROAD = 8, LOT = 32;
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    // lot types
    private static final int BUILDING = 0, PARK = 1, PLAZA = 2, LOWRISE = 3, PARKING = 4, HQ = 5, GUILD = 6, GATEPLAZA = 7;

    private record Lot(int type, int h, int x0, int z0, int x1, int z1, int style, int hash) {}

    private static int mix(long a, long b, long c, long d) {
        long h = a * 0x9E3779B97F4A7C15L + b * 0xC2B2AE3D27D4EB4FL + c * 0x165667B19E3779F9L + d * 0x27D4EB2F165667C5L;
        h ^= (h >>> 29); h *= 0xBF58476D1CE4E5B9L; h ^= (h >>> 32);
        return (int) (h & 0x7fffffff);
    }

    private static BlockState s(Block b) { return b.defaultBlockState(); }

    // ------------------------------------------------------------------ palettes
    private static BlockState wall(int style) {
        return switch (style) {
            case 0 -> s(Blocks.WHITE_CONCRETE);
            case 1 -> s(Blocks.LIGHT_GRAY_CONCRETE);
            case 2 -> s(Blocks.GRAY_CONCRETE);
            case 3 -> s(Blocks.BLUE_CONCRETE);
            case 4 -> s(Blocks.BRICKS);
            case 5 -> s(Blocks.BLACK_CONCRETE);
            case 6 -> s(Blocks.SMOOTH_QUARTZ);
            case 7 -> s(Blocks.BLUE_CONCRETE);
            case 8 -> s(Blocks.RED_CONCRETE);
            default -> s(Blocks.WHITE_CONCRETE);
        };
    }

    private static BlockState trim(int style) {
        return switch (style) {
            case 0 -> s(Blocks.LIGHT_GRAY_CONCRETE);
            case 1 -> s(Blocks.GRAY_CONCRETE);
            case 2 -> s(Blocks.BLACK_CONCRETE);
            case 3 -> s(Blocks.WHITE_CONCRETE);
            case 4 -> s(Blocks.STONE_BRICKS);
            case 5 -> s(Blocks.GRAY_CONCRETE);
            case 6 -> s(Blocks.GOLD_BLOCK);
            case 7 -> s(Blocks.WHITE_CONCRETE);
            case 8 -> s(Blocks.BLACK_CONCRETE);
            default -> s(Blocks.BLACK_CONCRETE);
        };
    }

    private static BlockState glass(int style) {
        return switch (style) {
            case 1, 6, 7 -> s(Blocks.LIGHT_BLUE_STAINED_GLASS_PANE);
            case 2 -> s(Blocks.CYAN_STAINED_GLASS_PANE);
            case 5 -> s(Blocks.GRAY_STAINED_GLASS_PANE);
            case 8 -> s(Blocks.RED_STAINED_GLASS_PANE);
            default -> s(Blocks.GLASS_PANE);
        };
    }

    private static BlockState lot(int dummy) { return AIR; }

    // ------------------------------------------------------------------ lots
    private static Lot lotAt(Content.RegionDef reg, int gx, int gz, long seed) {
        int regH = reg.id().hashCode();
        int h = mix(seed, regH, gx, gz);
        int dist = Math.abs(gx) + Math.abs(gz);
        int x0 = 3, z0 = 3, x1 = 28, z1 = 28;
        if (gx == 0 && gz == 0) return new Lot(HQ, 62, 3, 3, 28, 28, 6, h);
        if (gx == 0 && gz == 1) return new Lot(GATEPLAZA, 0, 0, 0, 31, 31, 0, h);
        if (gx == -1 && gz == 0) return new Lot(GUILD, 15, 4, 4, 27, 27, 7, h);
        if (gx == 1 && gz == 0) return new Lot(GUILD, 15, 4, 4, 27, 27, 8, h);
        if (gx == 0 && gz == -1) return new Lot(GUILD, 15, 4, 4, 27, 27, 0, h);
        int roll = h % 100;
        String t = reg.type();
        if (roll < 12) return new Lot(PARK, 0, 0, 0, 31, 31, 0, h);
        if (roll < 20) return new Lot(PLAZA, 0, 0, 0, 31, 31, 0, h);
        if (roll < 28) return new Lot(PARKING, 0, 0, 0, 31, 31, 0, h);
        int inset = 3 + (h >> 8) % 4;
        int wx = inset + (h >> 12) % 3, wz = inset + (h >> 16) % 3;
        x0 = wx; z0 = wz; x1 = 31 - inset - (h >> 20) % 3; z1 = 31 - inset - (h >> 24) % 3;
        if (roll < 52) {
            int hh = 8 + (h >> 4) % 6;
            return new Lot(LOWRISE, hh, x0, z0, x1, z1, 4, h);
        }
        int base = t.equals("capital") ? 22 : (t.equals("harbor") ? 12 : 16);
        int var = t.equals("capital") ? 46 : 26;
        int hh = base + (h >> 3) % var - dist * (t.equals("capital") ? 2 : 1);
        hh = Math.max(10, hh);
        return new Lot(BUILDING, hh, x0, z0, x1, z1, (h >> 6) % 4, h);
    }

    // ------------------------------------------------------------------ entry point per column
    public static void column(WorldGenLevel level, Content.RegionDef reg, int x, int z, long seed) {
        int Y0 = ModDimensions.CITY_Y;
        int lx = x - reg.cx(), lz = z - reg.cz();
        double d = Math.hypot(lx, lz);
        int top = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
        int surf = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        if (d <= reg.radius()) {
            // ----- shape to plateau
            if (top < Y0) {
                for (int y = top + 1; y < Y0; y++) level.setBlock(m.set(x, y, z), y < Y0 - 3 ? s(Blocks.STONE) : s(Blocks.DIRT), 2);
            } else {
                int clearTo = Math.max(top, surf);
                for (int y = Y0 + 1; y <= clearTo; y++) level.setBlock(m.set(x, y, z), AIR, 2);
                for (int y = Y0 - 4; y < Y0; y++) level.setBlock(m.set(x, y, z), y < Y0 - 2 ? s(Blocks.STONE) : s(Blocks.DIRT), 2);
            }
            if (reg.type().equals("ruins")) ruinsColumn(level, reg, lx, lz, x, z, seed, Y0, m);
            else cityColumn(level, reg, lx, lz, x, z, seed, Y0, m);
        } else {
            // ----- smooth transition ring
            double t = Mth.clamp((d - reg.radius()) / Regions.MARGIN, 0, 1);
            double sm = t * t * (3 - 2 * t);
            int target = (int) Math.round(Y0 + (top - Y0) * sm);
            if (top > target) {
                for (int y = target + 1; y <= Math.max(top, surf); y++) level.setBlock(m.set(x, y, z), AIR, 2);
                level.setBlock(m.set(x, target, z), s(Blocks.GRASS_BLOCK), 2);
            } else if (top < target) {
                for (int y = top + 1; y < target; y++) level.setBlock(m.set(x, y, z), s(Blocks.DIRT), 2);
                level.setBlock(m.set(x, target, z), s(Blocks.GRASS_BLOCK), 2);
            }
        }
    }

    // ------------------------------------------------------------------ city column
    private static void cityColumn(WorldGenLevel level, Content.RegionDef reg, int lx, int lz, int x, int z, long seed, int Y0, BlockPos.MutableBlockPos m) {
        int ux = Math.floorMod(lx + 20, CELL), uz = Math.floorMod(lz + 20, CELL);
        int gx = Math.floorDiv(lx + 20, CELL), gz = Math.floorDiv(lz + 20, CELL);
        boolean roadX = ux < ROAD, roadZ = uz < ROAD;
        // ---------- roads
        if (roadX || roadZ) {
            BlockState road = s(Blocks.BLACK_CONCRETE);
            if (roadX && !roadZ) {
                if ((ux == 3 || ux == 4) && (lz & 7) < 4) road = s(Blocks.YELLOW_CONCRETE);
            } else if (roadZ && !roadX) {
                if ((uz == 3 || uz == 4) && (lx & 7) < 4) road = s(Blocks.YELLOW_CONCRETE);
            } else {
                // crossing: zebra
                if (ux == 0 || uz == 0) road = s(Blocks.GRAY_CONCRETE);
            }
            if ((roadX && (ux == 0 || ux == ROAD - 1) && !roadZ) || (roadZ && (uz == 0 || uz == ROAD - 1) && !roadX)) road = s(Blocks.GRAY_CONCRETE);
            level.setBlock(m.set(x, Y0, z), road, 2);
            return;
        }
        int a = ux - ROAD, b = uz - ROAD;
        Lot lot = lotAt(reg, gx, gz, seed);
        // sidewalk
        if (a < 2 || b < 2 || a > 29 || b > 29) {
            level.setBlock(m.set(x, Y0, z), ((a + b) & 1) == 0 ? s(Blocks.SMOOTH_STONE) : s(Blocks.STONE_BRICKS), 2);
            if ((a == 1 || a == 30) && (b == 1 || b == 30)) {
                for (int y = 1; y <= 4; y++) level.setBlock(m.set(x, Y0 + y, z), s(Blocks.STONE_BRICK_WALL), 2);
                level.setBlock(m.set(x, Y0 + 5, z), s(Blocks.LANTERN), 2);
            }
            return;
        }
        switch (lot.type()) {
            case PARK -> parkColumn(level, lot, a, b, x, z, Y0, m);
            case PLAZA -> plazaColumn(level, lot, a, b, x, z, Y0, m, false);
            case GATEPLAZA -> plazaColumn(level, lot, a, b, x, z, Y0, m, true);
            case PARKING -> parkingColumn(level, lot, a, b, x, z, Y0, m);
            default -> buildingColumn(level, lot, a, b, x, z, Y0, m);
        }
    }

    // ------------------------------------------------------------------ buildings
    private static void buildingColumn(WorldGenLevel level, Lot lot, int a, int b, int x, int z, int Y0, BlockPos.MutableBlockPos m) {
        boolean inside = a >= lot.x0() && a <= lot.x1() && b >= lot.z0() && b <= lot.z1();
        BlockState floor = lot.type() == HQ || lot.type() == GUILD ? s(Blocks.POLISHED_ANDESITE) : s(Blocks.STONE_BRICKS);
        level.setBlock(m.set(x, Y0, z), inside ? (lot.type() == HQ ? s(Blocks.SMOOTH_QUARTZ) : s(Blocks.POLISHED_ANDESITE)) : s(Blocks.GRASS_BLOCK), 2);
        if (!inside) {
            // front garden: little bushes
            if ((lot.hash() + a * 7 + b * 13) % 11 == 0) level.setBlock(m.set(x, Y0 + 1, z), s(Blocks.AZALEA_LEAVES), 2);
            return;
        }
        int h = lot.h();
        boolean wallX = a == lot.x0() || a == lot.x1();
        boolean wallZ = b == lot.z0() || b == lot.z1();
        boolean edge = wallX || wallZ;
        boolean corner = wallX && wallZ;
        int midA = (lot.x0() + lot.x1()) / 2;
        int fh0 = lot.type() == HQ ? 9 : 5;
        BlockState wall = wall(lot.style()), trim = trim(lot.style()), pane = glass(lot.style());
        boolean ladderCol = a == lot.x0() + 1 && b == lot.z0() + 1;
        for (int dy = 1; dy <= h + 2; dy++) {
            BlockState st = null;
            if (dy == h) {
                st = ladderCol ? null : s(Blocks.SMOOTH_STONE);
                if (edge) st = trim;
            } else if (dy == h + 1) {
                if (edge) st = s(Blocks.STONE_BRICK_WALL);
                else if (((a * 31 + b * 17 + lot.hash()) % 29) == 0) st = s(Blocks.IRON_BLOCK);   // rooftop AC unit
                else if (a == lot.x0() + 3 && b == lot.z0() + 3 && h > 30) st = s(Blocks.LIGHTNING_ROD);
            } else if (dy == h + 2) {
                if (a == lot.x0() + 3 && b == lot.z0() + 3 && h > 30) st = s(Blocks.LIGHTNING_ROD);
            } else if (dy < h) {
                boolean slab = dy >= fh0 && ((dy - fh0) % 5 == 0);
                if (edge) {
                    int wy = dy < fh0 ? dy : (dy - fh0) % 5;
                    boolean window;
                    if (dy < fh0) window = dy >= 2 && dy <= 4 && lot.type() != HQ || (lot.type() == HQ && dy >= 2 && dy <= fh0 - 3);
                    else window = wy == 2 || wy == 3;
                    boolean door = wallZ && b == lot.z1() && Math.abs(a - midA) <= 1 && dy <= 4;
                    if (door) st = dy == 4 ? trim : null;
                    else if (corner) st = trim;
                    else if (slab && dy >= fh0) st = trim;
                    else if (window && ((a + b) & 3) != 0) st = pane;
                    else st = wall;
                    if (lot.type() == HQ && b == lot.z1() && !door && isEmblem(a, dy, lot)) st = s(Blocks.SEA_LANTERN);
                    if (lot.type() == GUILD && !door && wallZ && b == lot.z1() && dy >= 9 && dy <= 10) st = trim;
                } else if (ladderCol) {
                    st = s(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.EAST);
                } else if (slab) {
                    st = (a % 6 == 3 && b % 6 == 3) ? s(Blocks.SEA_LANTERN) : s(Blocks.SMOOTH_STONE_SLAB);
                    if (st.is(Blocks.SMOOTH_STONE_SLAB)) st = s(Blocks.SMOOTH_STONE);
                } else if (dy == 1 && lot.type() == HQ && b == lot.z1() - 3 && a > lot.x0() + 6 && a < lot.x1() - 6) {
                    st = s(Blocks.QUARTZ_BLOCK);          // reception desk
                } else if (dy == 2 && lot.type() == HQ && b == lot.z1() - 3 && a > lot.x0() + 6 && a < lot.x1() - 6 && a % 4 == 0) {
                    st = s(Blocks.LANTERN);
                } else if (dy == 1 && lot.type() == GUILD && b == lot.z0() + 4 && Math.abs(a - midA) <= 3) {
                    st = s(Blocks.CRAFTING_TABLE);
                }
            }
            level.setBlock(m.set(x, Y0 + dy, z), st == null ? AIR : st, 2);
        }
        // hall banners (guild)
        if (lot.type() == GUILD && wallZ && b == lot.z1() && Math.abs(a - midA) == 5) {
            Block banner = lot.style() == 7 ? Blocks.BLUE_WOOL : (lot.style() == 8 ? Blocks.RED_WOOL : Blocks.WHITE_WOOL);
            for (int dy = 5; dy <= 10; dy++) level.setBlock(m.set(x, Y0 + dy, z + 1), s(banner), 2);
        }
    }

    private static boolean isEmblem(int a, int dy, Lot lot) {
        // big "H" on the south face of the Hunters Association tower
        int cx = (lot.x0() + lot.x1()) / 2;
        int rel = a - cx;
        if (dy < 22 || dy > 40) return false;
        boolean bars = Math.abs(rel) >= 4 && Math.abs(rel) <= 5;
        boolean cross = dy >= 30 && dy <= 32 && Math.abs(rel) <= 5;
        return bars || cross;
    }

    // ------------------------------------------------------------------ parks, plazas, parking
    private static void parkColumn(WorldGenLevel level, Lot lot, int a, int b, int x, int z, int Y0, BlockPos.MutableBlockPos m) {
        int h = lot.hash();
        boolean path = (a >= 15 && a <= 16) || (b >= 15 && b <= 16);
        boolean pond = lot.hash() % 4 == 0 && a >= 11 && a <= 20 && b >= 11 && b <= 20 && !path ? Math.hypot(a - 15.5, b - 15.5) < 4.2 : false;
        if (pond) {
            level.setBlock(m.set(x, Y0, z), s(Blocks.WATER), 2);
            level.setBlock(m.set(x, Y0 - 1, z), s(Blocks.CLAY), 2);
            return;
        }
        level.setBlock(m.set(x, Y0, z), path ? s(Blocks.DIRT_PATH) : s(Blocks.GRASS_BLOCK), 2);
        if (!path) {
            int r = (h + a * 31 + b * 17) % 23;
            if (r == 0) level.setBlock(m.set(x, Y0 + 1, z), s(Blocks.POPPY), 2);
            else if (r == 1) level.setBlock(m.set(x, Y0 + 1, z), s(Blocks.DANDELION), 2);
            else if (r == 2 || r == 3) level.setBlock(m.set(x, Y0 + 1, z), s(Blocks.GRASS), 2);
        }
        // trees (3 fixed positions per lot)
        for (int t = 0; t < 3; t++) {
            int tx = 4 + Math.floorMod(mix(h, t, 1, 7), 24), tz = 4 + Math.floorMod(mix(h, t, 2, 9), 24);
            if (Math.abs(tx - 15.5) < 2.5 || Math.abs(tz - 15.5) < 2.5) continue;
            int th = 4 + Math.floorMod(mix(h, t, 3, 5), 3);
            int dx = a - tx, dz = b - tz;
            if (dx == 0 && dz == 0) {
                for (int y = 1; y <= th; y++) level.setBlock(m.set(x, Y0 + y, z), s(Blocks.OAK_LOG), 2);
            }
            for (int y = th - 1; y <= th + 2; y++) {
                int rad = y >= th + 2 ? 1 : 2;
                if (Math.abs(dx) <= rad && Math.abs(dz) <= rad && !(Math.abs(dx) == rad && Math.abs(dz) == rad && y != th) && !(dx == 0 && dz == 0 && y < th + 1)) {
                    level.setBlock(m.set(x, Y0 + y, z), s(Blocks.OAK_LEAVES).setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true), 2);
                }
            }
        }
        // benches
        if (path && ((a == 14 || a == 17) && b == 12) ) level.setBlock(m.set(x, Y0 + 1, z), s(Blocks.SPRUCE_STAIRS), 2);
    }

    private static void plazaColumn(WorldGenLevel level, Lot lot, int a, int b, int x, int z, int Y0, BlockPos.MutableBlockPos m, boolean gate) {
        boolean check = ((a / 2 + b / 2) & 1) == 0;
        level.setBlock(m.set(x, Y0, z), check ? s(Blocks.POLISHED_ANDESITE) : s(Blocks.STONE_BRICKS), 2);
        if (gate) {
            double dd = Math.hypot(a - 16, b - 16);
            if (dd < 6.5 && dd > 5.0) level.setBlock(m.set(x, Y0, z), s(Blocks.LIGHT_BLUE_CONCRETE), 2);
            if (dd <= 1.0) level.setBlock(m.set(x, Y0, z), s(Blocks.QUARTZ_BLOCK), 2);
            if (a == 16 && b == 16) level.setBlock(m.set(x, Y0 + 1, z), ModBlocks.get("teleport_pad").defaultBlockState(), 2);
            // boards
            if (a == 11 && b == 10) level.setBlock(m.set(x, Y0 + 1, z), ModBlocks.get("bounty_board").defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH), 2);
            if (a == 21 && b == 10) level.setBlock(m.set(x, Y0 + 1, z), ModBlocks.get("news_board").defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH), 2);
            // lamps
            if ((a == 6 || a == 25) && (b == 6 || b == 25)) {
                for (int y = 1; y <= 3; y++) level.setBlock(m.set(x, Y0 + y, z), s(Blocks.QUARTZ_PILLAR), 2);
                level.setBlock(m.set(x, Y0 + 4, z), ModBlocks.get("mana_lamp").defaultBlockState(), 2);
            }
            // benches
            if ((a == 8 || a == 23) && (b == 19 || b == 22)) level.setBlock(m.set(x, Y0 + 1, z), s(Blocks.QUARTZ_STAIRS).setValue(net.minecraft.world.level.block.StairBlock.FACING, Direction.NORTH), 2);
            return;
        }
        // fountain
        double dd = Math.hypot(a - 15.5, b - 15.5);
        if (dd < 4.2) {
            if (dd > 3.0) level.setBlock(m.set(x, Y0 + 1, z), s(Blocks.QUARTZ_SLAB), 2);
            else level.setBlock(m.set(x, Y0, z), s(Blocks.WATER), 2);
        }
        if (dd < 0.9) {
            for (int y = 1; y <= 3; y++) level.setBlock(m.set(x, Y0 + y, z), s(Blocks.QUARTZ_PILLAR), 2);
            level.setBlock(m.set(x, Y0 + 4, z), s(Blocks.WATER), 2);
        }
    }

    private static void parkingColumn(WorldGenLevel level, Lot lot, int a, int b, int x, int z, int Y0, BlockPos.MutableBlockPos m) {
        level.setBlock(m.set(x, Y0, z), s(Blocks.GRAY_CONCRETE), 2);
        if (a % 7 == 0 && b > 3 && b < 28) level.setBlock(m.set(x, Y0, z), s(Blocks.WHITE_CONCRETE), 2);
        // cars: 4x2 blocks in stalls
        int stall = (a - 1) / 7;
        int ca = 2 + stall * 7;
        if (a >= ca && a <= ca + 4 - 1 && b >= 4 && b < 28) {
            int row = (b - 4) / 6;
            int rowStart = 4 + row * 6;
            boolean has = ((lot.hash() >> (row + stall)) & 1) == 0;
            if (has && b >= rowStart && b <= rowStart + 3 && a - ca <= 2) {
                Block[] cols = {Blocks.RED_CONCRETE, Blocks.BLUE_CONCRETE, Blocks.WHITE_CONCRETE, Blocks.BLACK_CONCRETE, Blocks.YELLOW_CONCRETE, Blocks.LIGHT_GRAY_CONCRETE};
                Block c = cols[Math.floorMod(lot.hash() + row * 3 + stall, cols.length)];
                level.setBlock(m.set(x, Y0 + 1, z), s(c), 2);
                if (b > rowStart && b < rowStart + 3) level.setBlock(m.set(x, Y0 + 2, z), s(Blocks.GLASS), 2);
            }
        }
    }

    // ------------------------------------------------------------------ Jeju ruins
    private static void ruinsColumn(WorldGenLevel level, Content.RegionDef reg, int lx, int lz, int x, int z, long seed, int Y0, BlockPos.MutableBlockPos m) {
        int h = mix(seed, lx >> 2, lz >> 2, 3);
        level.setBlock(m.set(x, Y0, z), (h % 5 == 0) ? s(Blocks.COARSE_DIRT) : (h % 7 == 0 ? s(Blocks.GRAVEL) : s(Blocks.DIRT)), 2);
        // plaza with pad at (4,44)
        if (Math.abs(lx - 4) <= 12 && Math.abs(lz - 44) <= 12) {
            boolean check = ((lx / 2 + lz / 2) & 1) == 0;
            level.setBlock(m.set(x, Y0, z), check ? s(Blocks.POLISHED_ANDESITE) : s(Blocks.STONE_BRICKS), 2);
            if (lx == 4 && lz == 44) level.setBlock(m.set(x, Y0 + 1, z), ModBlocks.get("teleport_pad").defaultBlockState(), 2);
            if (lx == -4 && lz == 38) level.setBlock(m.set(x, Y0 + 1, z), ModBlocks.get("bounty_board").defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH), 2);
            if (lx == 12 && lz == 38) level.setBlock(m.set(x, Y0 + 1, z), ModBlocks.get("news_board").defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH), 2);
            return;
        }
        // hive mounds
        for (int k = 0; k < 14; k++) {
            int mh = mix(seed, reg.cx(), k, 11);
            double ang = (mh % 628) / 100.0, rad = 18 + (mh >> 10) % 60;
            int mx = (int) (Math.cos(ang) * rad), mz = (int) (Math.sin(ang) * rad);
            double dd = Math.hypot(lx - mx, lz - mz);
            double rr = 5 + (mh >> 5) % 5;
            if (dd < rr) {
                int hh = (int) ((rr - dd) / rr * (4 + (mh >> 8) % 5)) + 1;
                for (int y = 1; y <= hh; y++) level.setBlock(m.set(x, Y0 + y, z), ((mh + y) % 9 == 0) ? s(Blocks.CAVE_AIR) : ModBlocks.get("ant_hive").defaultBlockState(), 2);
            }
        }
        // scorched marks / bones
        if (h % 43 == 0) level.setBlock(m.set(x, Y0 + 1, z), s(Blocks.BONE_BLOCK), 2);
        if (h % 61 == 0) level.setBlock(m.set(x, Y0 + 1, z), s(Blocks.COBWEB), 2);
    }

    // ------------------------------------------------------------------ NPCs
    public record NpcSpawn(String role, String skin, String name, String guild, int lx, int lz) {}

    private static final String[] NAMES = {"Kim Min-Jun", "Lee Seo-Yeon", "Park Ji-Hoon", "Choi Ha-Eun", "Jung Woo-Jin", "Kang Soo-Ah", "Yoon Tae-Hyun", "Han Ji-Min",
            "Song Joon-Ho", "Lim Da-Hye", "Shin Dong-Hyun", "Oh Se-Jin", "Bae Min-Ji", "Seo Kang-Dae", "Hwang Yu-Na", "Kwon Hyun-Woo"};

    public static List<NpcSpawn> npcs(Content.RegionDef reg, long seed) {
        List<NpcSpawn> l = new ArrayList<>();
        // HQ lobby
        l.add(new NpcSpawn("receptionist", "chairman", "Go Gun-Hee", "", 3, 7));
        l.add(new NpcSpawn("receptionist", "receptionist", "Han Song-Yi", "", -3, 11));
        l.add(new NpcSpawn("receptionist", "receptionist", "Lee Min-Ah", "", 9, 11));
        // gate plaza (lot origin lx=-12, lz=28)
        l.add(new NpcSpawn("merchant", "merchant", "Mr. Hwang", "", 0, 47));
        l.add(new NpcSpawn("healer", "healer", "Han Ye-Rin", "", 9, 47));
        l.add(new NpcSpawn("journalist", "journalist", "Seo Ji-Won", "", -2, 41));
        l.add(new NpcSpawn("hunter", "hunter", "Hunter Kim", "", 12, 44));
        l.add(new NpcSpawn("hunter", "hunter", "Hunter Park", "", -5, 45));
        // guild halls
        l.add(new NpcSpawn("guild_master", "cha_hae_in", "Cha Hae-In", "hunters", -37, 3));
        l.add(new NpcSpawn("guild_master", "hunter", "Choi Jong-In", "ahjin", 43, 3));
        l.add(new NpcSpawn("guild_master", "baek_yoon_ho", "Baek Yoon-Ho", "white_tiger", 3, -37));
        // citizens on lots
        for (int gx = -3; gx <= 3; gx++) {
            for (int gz = -3; gz <= 3; gz++) {
                int h = mix(seed, reg.id().hashCode(), gx, gz * 31 + 7);
                if (h % 100 < 38) {
                    int lx = gx * CELL - 12 + 1 + (h >> 4) % 30;
                    int lz = gz * CELL - 12 + 1;
                    if (Math.hypot(lx, lz) < reg.radius() - 6)
                        l.add(new NpcSpawn("citizen", (h >> 9) % 3 == 0 ? "hunter" : "citizen", NAMES[(h >> 3) % NAMES.length], "", lx, lz));
                }
            }
        }
        return l;
    }

    public static void spawnNpcsInChunk(WorldGenLevel level, Content.RegionDef reg, int cx0, int cz0, long seed) {
        int Y0 = ModDimensions.CITY_Y;
        if (reg.type().equals("ruins")) {
            int gx = reg.cx() + 4, gz = reg.cz() + 24;
            if (gx >= cx0 && gx < cx0 + 16 && gz >= cz0 && gz < cz0 + 16) {
                com.sololeveling.entity.GateEntity g = ModEntities.GATE.get().create(level.getLevel());
                if (g != null) {
                    g.moveTo(gx + 0.5, Y0 + 1, gz + 0.5, 0F, 0F);
                    g.setup("S", "ant_nest", false, com.sololeveling.entity.GateEntity.ENTRANCE);
                    g.age2 = Integer.MIN_VALUE / 2;
                    level.addFreshEntity(g);
                }
            }
        }
        for (NpcSpawn n : npcs(reg, seed)) {
            int x = reg.cx() + n.lx(), z = reg.cz() + n.lz();
            if (x < cx0 || x >= cx0 + 16 || z < cz0 || z >= cz0 + 16) continue;
            var type = ModEntities.NPCS.get(n.role()).get();
            NpcEntity e = type.create(level.getLevel());
            if (e == null) continue;
            e.moveTo(x + 0.5, Y0 + 1, z + 0.5, (float) (Math.abs(mix(seed, x, z, 1)) % 360), 0);
            e.setup(n.role(), n.skin(), n.name(), n.guild());
            e.restrictTo(new BlockPos(x, Y0 + 1, z), 14);
            e.setPersistenceRequired();
            level.addFreshEntity(e);
        }
    }
}
