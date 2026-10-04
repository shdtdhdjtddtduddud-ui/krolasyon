package com.krolasyon.bosses.rpg.town;

import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Parametric buildings drawn on a {@link Canvas}. Local origin = front-left corner at floor level, door on z = 0. */
public final class Buildings {
    private Buildings() {}

    private static final Direction N = Direction.NORTH, S = Direction.SOUTH, E = Direction.EAST, W = Direction.WEST;

    /** gabled roof along x covering x in [-1, w], z in [-1, d] starting at height y */
    public static void gableRoof(Canvas c, Palette p, int w, int d, int y) {
        int half = (d + 1) / 2;
        for (int i = 0; i <= half; i++) {
            int zf = -1 + i, zb = d - i;
            if (zf > zb) break;
            for (int x = -1; x <= w; x++) {
                if (zf == zb) c.slab(x, y + i, zf, p.roofSlab(), false);
                else {
                    c.stairs(x, y + i, zf, p.roof(), S, false);
                    c.stairs(x, y + i, zb, p.roof(), N, false);
                }
            }
            if (zf + 1 <= zb - 1 && i > 0) {
                c.fill(0, y + i, zf + 1, 0, y + i, zb - 1, p.wall());
                c.fill(w - 1, y + i, zf + 1, w - 1, y + i, zb - 1, p.wall());
            }
        }
    }

    private static void windowsAround(Canvas c, Palette p, int w, int d, int y, int every) {
        if (p.window() == Blocks.AIR) {
            for (int x = 2; x < w - 2; x += every) { c.set(x, y, 0, Blocks.AIR); c.set(x, y, d - 1, Blocks.AIR); }
            for (int z = 2; z < d - 2; z += every) { c.set(0, y, z, Blocks.AIR); c.set(w - 1, y, z, Blocks.AIR); }
            return;
        }
        for (int x = 2; x < w - 2; x += every) { c.set(x, y, 0, p.window()); c.set(x, y, d - 1, p.window()); }
        for (int z = 2; z < d - 2; z += every) { c.set(0, y, z, p.window()); c.set(w - 1, y, z, p.window()); }
    }

    /** walls, frame and floor of one storey; returns top y of the walls */
    private static int storey(Canvas c, Palette p, int w, int d, int y0, int h) {
        c.fill(0, y0, 0, w - 1, y0, d - 1, p.floor());
        c.walls(0, y0 + 1, 0, w - 1, y0 + h, d - 1, p.wall());
        c.air(1, y0 + 1, 1, w - 2, y0 + h, d - 2);
        for (int[] k : new int[][]{{0, 0}, {w - 1, 0}, {0, d - 1}, {w - 1, d - 1}}) c.pillar(k[0], y0, y0 + h, k[1], p.frame());
        c.fill(0, y0 + h, 0, w - 1, y0 + h, d - 1, p.frame() == Blocks.AIR ? p.wall() : p.wall());
        windowsAround(c, p, w, d, y0 + 2, 3);
        return y0 + h;
    }

    public static void clearFootprint(Canvas c, int w, int d, int h) {
        c.air(-1, 1, -1, w, h, d);
    }

    public static void house(Canvas c, Palette p, RandomSource r, int w, int d, boolean twoFloors) {
        int h = p.wallHeight();
        clearFootprint(c, w, d, h * 2 + 8);
        c.fill(-1, -1, -1, w, -1, d, p.foundation());
        int top = storey(c, p, w, d, 0, h);
        if (twoFloors) {
            c.fill(1, top, 1, w - 2, top, d - 2, p.floor());
            top = storey(c, p, w, d, top, h);
            c.set(w - 2, 1, d - 2, Blocks.LADDER.defaultBlockState().setValue(net.minecraft.world.level.block.LadderBlock.FACING, W));
            for (int y = 1; y < h * 2; y++) c.set(w - 2, y, d - 2, Blocks.LADDER.defaultBlockState().setValue(net.minecraft.world.level.block.LadderBlock.FACING, W));
            c.set(w - 2, h, d - 2, Blocks.LADDER.defaultBlockState().setValue(net.minecraft.world.level.block.LadderBlock.FACING, W));
        }
        gableRoof(c, p, w, d, top + 1);
        int dx = w / 2;
        c.air(dx, 1, 0, dx, p.doorHeight(), 0);
        if (p.doorHeight() <= 2) c.door(dx, 1, 0, p.door(), S);
        c.stairs(dx, 0, -1, p.roof(), S, false);
        c.hanging(dx, top - 1 > 2 ? 3 : 2, 1, p.light());
        // interior
        c.bed(1, 1, d - 3, p.bed(), N);
        c.set(w - 2, 1, 1, Blocks.CRAFTING_TABLE);
        c.facing(w - 2, 1, 2, Blocks.FURNACE, W);
        c.facing(1, 1, 1, Blocks.CHEST, E);
        c.set(dx, 1, d - 2, Blocks.OAK_FENCE);
        c.set(dx, 2, d - 2, Blocks.OAK_PRESSURE_PLATE);
        c.set(dx - 1, 1, d - 2, Blocks.OAK_STAIRS.defaultBlockState().setValue(net.minecraft.world.level.block.StairBlock.FACING, E));
        if (r.nextBoolean()) c.set(w - 2, 1, d - 3, Blocks.BARREL);
        if (r.nextBoolean()) c.set(w - 2, 2, 1, Blocks.POTTED_POPPY);
        for (int x = 2; x < w - 2; x++) for (int z = 2; z < d - 3; z++) if (r.nextInt(3) > 0) c.set(x, 1, z, p.carpet());
        // window boxes and front lantern
        c.set(dx + 1, 2, -1, p.light() == Blocks.SHROOMLIGHT ? Blocks.SHROOMLIGHT : Blocks.LANTERN);
    }

    public static void shack(Canvas c, Palette p, RandomSource r, int w, int d) {
        clearFootprint(c, w, d, 8);
        c.fill(0, 0, 0, w - 1, 0, d - 1, r.nextBoolean() ? Blocks.COARSE_DIRT : Blocks.PACKED_MUD);
        c.walls(0, 1, 0, w - 1, 3, d - 1, Blocks.OAK_PLANKS);
        c.air(1, 1, 1, w - 2, 3, d - 2);
        for (int i = 0; i < 4; i++) c.set(r.nextInt(w), 1 + r.nextInt(3), r.nextBoolean() ? 0 : d - 1, r.nextBoolean() ? Blocks.COBBLESTONE : Blocks.AIR);
        for (int x = -1; x <= w; x++) for (int z = -1; z <= d; z++) c.slab(x, 4, z, r.nextInt(5) == 0 ? Blocks.COBBLESTONE_SLAB : Blocks.OAK_SLAB, false);
        int dx = w / 2;
        c.air(dx, 1, 0, dx, 2, 0);
        c.set(dx, 1, 0, Blocks.AIR);
        c.bed(1, 1, d - 3, Blocks.BROWN_BED, N);
        c.set(w - 2, 1, d - 2, Blocks.BARREL);
        c.set(w - 2, 1, 1, Blocks.CAULDRON);
        c.hanging(dx, 3, d / 2, Blocks.LANTERN);
    }

    /** the family hovel of the protagonist: shack with a sick bed, a small table and a chest of keepsakes */
    public static void familyHome(Canvas c, RandomSource r) {
        int w = 7, d = 7;
        clearFootprint(c, w, d, 8);
        c.fill(0, 0, 0, w - 1, 0, d - 1, Blocks.SPRUCE_PLANKS);
        c.walls(0, 1, 0, w - 1, 3, d - 1, Blocks.OAK_PLANKS);
        c.air(1, 1, 1, w - 2, 3, d - 2);
        for (int[] k : new int[][]{{0, 0}, {w - 1, 0}, {0, d - 1}, {w - 1, d - 1}}) c.pillar(k[0], 1, 3, k[1], Blocks.OAK_LOG);
        c.set(0, 2, 3, Blocks.AIR);
        c.set(w - 1, 2, 3, Blocks.AIR);
        for (int x = -1; x <= w; x++) for (int z = -1; z <= d; z++) c.slab(x, 4, z, Blocks.SPRUCE_SLAB, false);
        c.door(3, 1, 0, Blocks.SPRUCE_DOOR, Direction.SOUTH);
        c.bed(1, 1, 4, Blocks.WHITE_BED, N);
        c.bed(5, 1, 4, Blocks.BROWN_BED, N);
        c.bed(3, 1, 5, Blocks.RED_BED, W);
        c.set(5, 1, 1, Blocks.CRAFTING_TABLE);
        c.facing(1, 1, 1, Blocks.CHEST, E);
        c.set(3, 1, 3, Blocks.OAK_FENCE);
        c.set(3, 2, 3, Blocks.OAK_PRESSURE_PLATE);
        c.set(4, 1, 1, Blocks.CAULDRON);
        c.hanging(3, 3, 3, Blocks.LANTERN);
        c.set(2, 1, 1, Blocks.FLOWER_POT);
    }

    public static void mansion(Canvas c, Palette p, RandomSource r, int w, int d) {
        int h = p.wallHeight() + 1;
        clearFootprint(c, w + 4, d + 4, h * 2 + 10);
        c.fill(-3, 0, -4, w + 2, 0, d + 2, Blocks.GRASS_BLOCK);
        c.walls(-3, 1, -4, w + 2, 1, d + 2, p.fence());
        c.air(w / 2 - 1, 1, -4, w / 2 + 1, 1, -4);
        for (int i = 0; i < 10; i++) {
            int fx = -2 + r.nextInt(w + 4), fz = r.nextBoolean() ? -3 : d + 1;
            c.set(fx, 1, fz, r.nextBoolean() ? Blocks.ROSE_BUSH : Blocks.PEONY);
        }
        c.fill(-1, -1, -1, w, -1, d, p.foundation());
        int top = storey(c, p, w, d, 0, h);
        c.fill(1, top, 1, w - 2, top, d - 2, p.floor());
        top = storey(c, p, w, d, top, h);
        c.fill(0, h, 0, w - 1, h, 0, p.accent());
        gableRoof(c, p, w, d, top + 1);
        int dx = w / 2;
        c.air(dx - 1, 1, 0, dx, 2, 0);
        if (p.doorHeight() <= 2) { c.door(dx - 1, 1, 0, p.door(), S); c.door(dx, 1, 0, p.door(), S); }
        for (int z = 1; z < d - 1; z++) c.set(dx, 1, z, Blocks.RED_CARPET);
        for (int y = 1; y < h * 2; y++) c.set(w - 2, y, d - 2, Blocks.LADDER.defaultBlockState().setValue(net.minecraft.world.level.block.LadderBlock.FACING, W));
        c.set(w - 2, h, d - 2, Blocks.LADDER.defaultBlockState().setValue(net.minecraft.world.level.block.LadderBlock.FACING, W));
        c.bed(1, h + 1, d - 3, p.bed(), N);
        c.bed(3, h + 1, d - 3, p.bed(), N);
        c.fill(1, h + 1, 1, 1, h + 3, 3, Blocks.BOOKSHELF);
        c.fill(w - 2, 1, 1, w - 2, 2, 3, Blocks.BOOKSHELF);
        c.facing(1, 1, 1, Blocks.CHEST, E);
        c.hanging(dx, h - 1, d / 2, p.light());
        c.hanging(dx, h * 2 - 1, d / 2, p.light());
        c.set(1, 1, d - 2, Blocks.JUKEBOX);
        for (int x = 1; x < w - 1; x++) c.set(x, h + 1, 1, p.carpet());
        // balcony
        c.fill(dx - 2, h + 1, -1, dx + 2, h + 1, -1, p.floor());
        c.fill(dx - 2, h + 2, -2, dx + 2, h + 2, -2, p.fence());
        c.air(dx, h + 1, 0, dx, h + 2, 0);
    }

    public static void palace(Canvas c, Palette p, RandomSource r, int w, int d) {
        int h = 9;
        clearFootprint(c, w + 2, d + 2, h + 22);
        c.fill(-1, -1, -1, w, 0, d, p.cityWall());
        c.walls(0, 1, 0, w - 1, h, d - 1, p.cityWall());
        c.air(1, 1, 1, w - 2, h, d - 2);
        c.fill(1, 0, 1, w - 2, 0, d - 2, p.plaza());
        c.fill(0, h + 1, 0, w - 1, h + 1, d - 1, p.cityWall());
        for (int x = 0; x < w; x += 2) { c.set(x, h + 2, 0, p.wallTop()); c.set(x, h + 2, d - 1, p.wallTop()); }
        for (int z = 0; z < d; z += 2) { c.set(0, h + 2, z, p.wallTop()); c.set(w - 1, h + 2, z, p.wallTop()); }
        // corner towers
        for (int[] k : new int[][]{{-2, -2}, {w - 3, -2}, {-2, d - 3}, {w - 3, d - 3}}) {
            c.walls(k[0], 0, k[1], k[0] + 4, h + 6, k[1] + 4, p.cityWall());
            c.air(k[0] + 1, 1, k[1] + 1, k[0] + 3, h + 6, k[1] + 3);
            for (int i = 0; i < 5; i += 2) { c.set(k[0] + i, h + 7, k[1], p.wallTop()); c.set(k[0] + i, h + 7, k[1] + 4, p.wallTop()); c.set(k[0], h + 7, k[1] + i, p.wallTop()); c.set(k[0] + 4, h + 7, k[1] + i, p.wallTop()); }
            c.set(k[0] + 2, h + 7, k[1] + 2, p.light());
            c.fill(k[0] + 2, h + 8, k[1] + 2, k[0] + 2, h + 10, k[1] + 2, p.frame());
            c.set(k[0] + 2, h + 11, k[1] + 2, p.accent());
        }
        // gate
        int dx = w / 2;
        c.air(dx - 1, 1, 0, dx + 1, 4, 0);
        for (int z = 0; z < d - 4; z++) c.set(dx, 1, z, Blocks.RED_CARPET);
        // windows
        for (int x = 3; x < w - 3; x += 4) { c.fill(x, 4, 0, x, 6, 0, p.window()); c.fill(x, 4, d - 1, x, 6, d - 1, p.window()); }
        for (int z = 3; z < d - 3; z += 4) { c.fill(0, 4, z, 0, 6, z, p.window()); c.fill(w - 1, 4, z, w - 1, 6, z, p.window()); }
        // pillars inside
        for (int z = 3; z < d - 4; z += 4) { c.pillar(dx - 4, 1, h, z, p.frame()); c.pillar(dx + 4, 1, h, z, p.frame()); c.set(dx - 4, h - 1, z + 1, p.light()); }
        // banners
        for (int z = 3; z < d - 4; z += 4) { c.fill(1, 3, z, 1, 7, z, p.accent()); c.fill(w - 2, 3, z, w - 2, 7, z, p.accent()); }
        // throne
        int tz = d - 3;
        c.fill(dx - 2, 1, tz - 1, dx + 2, 1, tz + 1, Blocks.GOLD_BLOCK);
        c.stairs(dx, 2, tz, Blocks.QUARTZ_STAIRS, S, false);
        c.fill(dx - 1, 2, tz + 1, dx + 1, 4, tz + 1, Blocks.GOLD_BLOCK);
        c.set(dx - 1, 2, tz, Blocks.QUARTZ_SLAB);
        c.set(dx + 1, 2, tz, Blocks.QUARTZ_SLAB);
        c.hanging(dx, h - 1, d / 2, p.light());
        c.hanging(dx, h - 1, d / 4, p.light());
        c.hanging(dx, h - 1, 3 * d / 4, p.light());
        c.facing(2, 1, d - 2, Blocks.CHEST, E);
    }

    public static void temple(Canvas c, Palette p, RandomSource r, int w, int d) {
        int h = p.wallHeight() + 4;
        clearFootprint(c, w + 2, d + 2, h + 12);
        c.fill(-1, -1, -1, w, 0, d, p.foundation());
        c.walls(0, 1, 0, w - 1, h, d - 1, Blocks.SMOOTH_QUARTZ);
        c.air(1, 1, 1, w - 2, h, d - 2);
        c.fill(1, 0, 1, w - 2, 0, d - 2, Blocks.POLISHED_DIORITE);
        for (int z = 2; z < d - 2; z += 3) {
            c.fill(0, 3, z, 0, h - 2, z, Blocks.YELLOW_STAINED_GLASS_PANE);
            c.fill(w - 1, 3, z, w - 1, h - 2, z, Blocks.YELLOW_STAINED_GLASS_PANE);
            c.pillar(2, 1, h, z, Blocks.QUARTZ_PILLAR);
            c.pillar(w - 3, 1, h, z, Blocks.QUARTZ_PILLAR);
        }
        gableRoof(c, p, w, d, h + 1);
        int dx = w / 2;
        c.air(dx - 1, 1, 0, dx + 1, 4, 0);
        for (int z = 3; z < d - 4; z += 2) {
            c.stairs(dx - 2, 1, z, Blocks.OAK_STAIRS, S, false);
            c.stairs(dx + 2, 1, z, Blocks.OAK_STAIRS, S, false);
            c.stairs(dx - 3, 1, z, Blocks.OAK_STAIRS, S, false);
            c.stairs(dx + 3, 1, z, Blocks.OAK_STAIRS, S, false);
        }
        c.fill(dx - 1, 1, d - 3, dx + 1, 1, d - 3, Blocks.GOLD_BLOCK);
        c.set(dx, 2, d - 3, Blocks.ENCHANTING_TABLE);
        c.set(dx - 1, 2, d - 3, Blocks.CANDLE);
        c.set(dx + 1, 2, d - 3, Blocks.CANDLE);
        c.set(dx, h - 1, d - 3, Blocks.BELL);
        c.set(dx, h, d - 2, Blocks.BEACON);
        c.hanging(dx, h - 1, d / 2, Blocks.LANTERN);
    }

    public static void guildHall(Canvas c, Palette p, RandomSource r, int w, int d) {
        house(c, p, r, w, d, false);
        int dx = w / 2;
        for (int x = 2; x < w - 2; x++) c.set(x, 1, d / 2, Blocks.SPRUCE_SLAB);
        c.fill(1, 2, d - 1, w - 2, 4, d - 1, Blocks.SPRUCE_PLANKS);
        for (int x = 2; x < w - 2; x += 2) c.set(x, 3, d - 2, Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(net.minecraft.world.level.block.WallSignBlock.FACING, N));
        c.set(dx, 1, d - 2, Blocks.LECTERN);
        c.set(1, 1, 2, Blocks.ANVIL);
        c.fill(w - 2, 1, 2, w - 2, 1, 4, Blocks.BARREL);
    }

    public static void tavern(Canvas c, Palette p, RandomSource r, int w, int d) {
        house(c, p, r, w, d, true);
        for (int x = 1; x < w - 2; x++) c.set(x, 1, d - 3, Blocks.BARREL);
        c.set(2, 2, d - 3, Blocks.BREWING_STAND);
        for (int i = 0; i < 2; i++) {
            int tx = 2 + i * 3;
            c.set(tx, 1, 3, Blocks.OAK_FENCE);
            c.set(tx, 2, 3, Blocks.OAK_PRESSURE_PLATE);
        }
        c.set(w - 2, 1, 1, Blocks.SMOKER);
    }

    public static void smithy(Canvas c, Palette p, RandomSource r, int w, int d) {
        clearFootprint(c, w, d, 10);
        c.fill(-1, -1, -1, w, -1, d, p.foundation());
        c.fill(0, 0, 0, w - 1, 0, d - 1, Blocks.STONE_BRICKS);
        c.walls(0, 1, d - 1, w - 1, 4, d - 1, p.wall());
        c.fill(0, 1, 0, 0, 4, d - 1, p.wall());
        for (int x = 0; x < w; x += w - 1) c.pillar(x, 1, 4, 0, p.frame());
        c.fill(0, 5, 0, w - 1, 5, d - 1, p.roofSlab());
        c.set(2, 1, 2, Blocks.ANVIL);
        c.facing(w - 2, 1, d - 2, Blocks.BLAST_FURNACE, N);
        c.set(w - 3, 1, d - 2, Blocks.LAVA_CAULDRON);
        c.set(w - 2, 1, 2, Blocks.GRINDSTONE);
        c.set(1, 1, d - 2, Blocks.SMITHING_TABLE);
        c.facing(3, 1, d - 2, Blocks.CHEST, N);
        c.fill(w - 2, 2, d - 2, w - 2, 4, d - 2, Blocks.BRICKS);
        c.set(w - 2, 5, d - 2, Blocks.CAMPFIRE);
        c.hanging(w / 2, 4, d / 2, Blocks.LANTERN);
    }

    public static void mageTower(Canvas c, Palette p, RandomSource r) {
        int rad = 4, h = 18;
        clearFootprint(c, rad * 2 + 1, rad * 2 + 1, h + 8);
        for (int y = -1; y <= h; y++) for (int x = -rad; x <= rad; x++) for (int z = -rad; z <= rad; z++) {
            double dd = Math.sqrt(x * x + z * z);
            if (dd > rad + 0.5) continue;
            boolean edge = dd > rad - 0.6;
            if (y == -1 || y == 0 || y == 6 || y == 12) c.set(x + rad, y, z + rad, edge ? p.foundation() : (y == 0 ? p.floor() : Blocks.BOOKSHELF == p.floor() ? p.floor() : p.floor()));
            else if (edge) c.set(x + rad, y, z + rad, (y % 6 == 3 && (x == 0 || z == 0)) ? Blocks.PURPLE_STAINED_GLASS_PANE : p.foundation());
            else c.set(x + rad, y, z + rad, Blocks.AIR);
        }
        for (int y = 0; y <= h; y++) c.set(rad, y, rad, Blocks.LADDER.defaultBlockState().setValue(net.minecraft.world.level.block.LadderBlock.FACING, Direction.SOUTH));
        c.set(rad, 0, rad - 1, p.floor());
        for (int y = 1; y < 6; y++) c.set(rad, y, rad - 1, Blocks.STONE_BRICKS);
        for (int x = -rad + 1; x <= rad - 1; x++) for (int z = -rad + 1; z <= rad - 1; z++) {
            if (x == 0 && z == 0) continue;
            if (Math.abs(x) == rad - 1 || Math.abs(z) == rad - 1) { c.set(x + rad, 7, z + rad, Blocks.BOOKSHELF); c.set(x + rad, 8, z + rad, Blocks.BOOKSHELF); }
        }
        for (int y = 6; y <= 12; y += 6) c.set(rad, y, rad, Blocks.AIR);
        for (int y = 7; y < 12; y++) c.set(rad, y, rad - 1, Blocks.AIR);
        c.set(rad + 2, 13, rad, Blocks.ENCHANTING_TABLE);
        c.set(rad - 2, 13, rad, Blocks.BREWING_STAND);
        // dome
        for (int x = -rad; x <= rad; x++) for (int z = -rad; z <= rad; z++) for (int y = 0; y <= rad; y++) {
            double dd = Math.sqrt(x * x + z * z + y * y);
            if (dd > rad - 0.5 && dd < rad + 0.5) c.set(x + rad, h + 1 + y, z + rad, y > rad - 2 ? p.accent() : Blocks.PURPLE_STAINED_GLASS);
        }
        c.set(rad, h + 1, rad, Blocks.AMETHYST_CLUSTER);
        c.air(rad - 1, 1, 0, rad + 1, 3, 0);
        c.door(rad, 1, 0, p.door(), Direction.SOUTH);
        c.hanging(rad + 1, 5, rad + 1, p.light());
        c.hanging(rad + 1, 11, rad + 1, p.light());
    }

    public static void stall(Canvas c, Palette p, RandomSource r, Block awning) {
        clearFootprint(c, 4, 4, 5);
        for (int[] k : new int[][]{{0, 0}, {3, 0}, {0, 3}, {3, 3}}) c.fill(k[0], 1, k[1], k[0], 3, k[1], p.fence());
        c.fill(0, 4, 0, 3, 4, 3, awning);
        c.fill(1, 1, 0, 2, 1, 0, Blocks.BARREL);
        c.set(1, 2, 0, r.nextBoolean() ? Blocks.MELON : Blocks.PUMPKIN);
        c.set(2, 2, 0, Blocks.HAY_BLOCK);
        c.set(3, 1, 2, Blocks.COMPOSTER);
    }

    public static void barracks(Canvas c, Palette p, RandomSource r, int w, int d) {
        house(c, p, r, w, d, false);
        for (int x = 1; x < w - 2; x += 2) c.bed(x, 1, d - 3, p.bed(), N);
        c.set(w - 2, 1, 1, Blocks.ANVIL);
        c.set(1, 1, 2, Blocks.GRINDSTONE);
        c.facing(w - 2, 1, 3, Blocks.CHEST, W);
    }

    public static void slaveMarket(Canvas c, Palette p, RandomSource r) {
        int w = 11, d = 9;
        clearFootprint(c, w, d, 8);
        c.fill(0, 0, 0, w - 1, 0, d - 1, Blocks.POLISHED_ANDESITE);
        c.fill(1, 1, 1, w - 2, 1, 2, Blocks.SPRUCE_PLANKS);
        for (int i = 0; i < 3; i++) {
            int x0 = 1 + i * 3;
            c.walls(x0, 1, 4, x0 + 2, 3, d - 2, Blocks.IRON_BARS);
            c.air(x0 + 1, 1, 5, x0 + 1, 3, d - 3);
            c.fill(x0, 4, 4, x0 + 2, 4, d - 2, Blocks.SPRUCE_SLAB);
            c.air(x0 + 1, 1, 4, x0 + 1, 2, 4);
        }
        c.hanging(w / 2, 3, 2, Blocks.LANTERN);
        c.set(w - 2, 2, 1, Blocks.CHAIN);
    }

    public static void well(Canvas c, Palette p) {
        clearFootprint(c, 5, 5, 6);
        c.walls(0, 0, 0, 4, 1, 4, p.foundation());
        c.fill(1, -3, 1, 3, 0, 3, Blocks.WATER);
        for (int[] k : new int[][]{{0, 0}, {4, 0}, {0, 4}, {4, 4}}) c.fill(k[0], 2, k[1], k[0], 3, k[1], p.fence());
        c.fill(0, 4, 0, 4, 4, 4, p.roofSlab());
        c.hanging(2, 3, 2, Blocks.CHAIN);
    }

    public static void fountain(Canvas c, Palette p, int rad) {
        for (int x = -rad; x <= rad; x++) for (int z = -rad; z <= rad; z++) {
            double dd = Math.sqrt(x * x + z * z);
            if (dd <= rad) c.set(x, 0, z, dd > rad - 1 ? p.accent() == Blocks.MAGMA_BLOCK ? Blocks.POLISHED_BLACKSTONE : Blocks.SMOOTH_STONE : p.accent() == Blocks.MAGMA_BLOCK ? Blocks.LAVA : Blocks.WATER);
            if (dd <= rad && dd > rad - 1) c.set(x, 1, z, p.wallTop());
        }
        c.fill(0, 0, 0, 0, 4, 0, Blocks.QUARTZ_PILLAR);
        c.set(0, 5, 0, p.accent() == Blocks.MAGMA_BLOCK ? Blocks.MAGMA_BLOCK : Blocks.WATER);
        c.set(1, 4, 0, p.light());
        c.set(-1, 4, 0, p.light());
    }

    public static void statue(Canvas c, Palette p) {
        c.fill(-1, 0, -1, 1, 1, 1, p.foundation());
        c.fill(0, 2, 0, 0, 4, 0, Blocks.POLISHED_ANDESITE);
        c.set(-1, 4, 0, Blocks.POLISHED_ANDESITE);
        c.set(1, 4, 0, Blocks.POLISHED_ANDESITE);
        c.set(0, 5, 0, Blocks.CHISELED_STONE_BRICKS);
        c.set(1, 5, 0, Blocks.IRON_BARS);
        c.set(1, 6, 0, Blocks.IRON_BARS);
    }

    public static void tent(Canvas c, RandomSource r) {
        Block wool = r.nextBoolean() ? Blocks.BROWN_WOOL : r.nextBoolean() ? Blocks.BLACK_WOOL : Blocks.RED_WOOL;
        clearFootprint(c, 5, 5, 5);
        for (int z = 0; z < 5; z++) {
            c.set(0, 1, z, wool); c.set(4, 1, z, wool);
            c.set(1, 2, z, wool); c.set(3, 2, z, wool);
            c.set(2, 3, z, wool);
        }
        c.bed(2, 1, 1, Blocks.BROWN_BED, Direction.SOUTH);
        c.set(1, 1, 4, Blocks.BARREL);
    }

    public static void farm(Canvas c, RandomSource r, int w, int d) {
        clearFootprint(c, w, d, 3);
        Block crop = r.nextInt(3) == 0 ? Blocks.CARROTS : r.nextBoolean() ? Blocks.POTATOES : Blocks.WHEAT;
        c.walls(-1, 1, -1, w, 1, d, Blocks.OAK_FENCE);
        c.set(w / 2, 1, -1, Blocks.OAK_FENCE_GATE);
        for (int x = 0; x < w; x++) for (int z = 0; z < d; z++) {
            if (x == w / 2) { c.set(x, 0, z, Blocks.WATER); continue; }
            c.set(x, 0, z, Blocks.FARMLAND.defaultBlockState().setValue(net.minecraft.world.level.block.FarmBlock.MOISTURE, 7));
            net.minecraft.world.level.block.state.BlockState s = crop.defaultBlockState();
            if (crop instanceof CropBlock cb) s = cb.getStateForAge(r.nextInt(cb.getMaxAge() + 1));
            c.set(x, 1, z, s);
        }
    }

    public static void ruin(Canvas c, Palette p, RandomSource r, int w, int d) {
        for (int x = 0; x < w; x++) for (int z = 0; z < d; z++) {
            boolean edge = x == 0 || z == 0 || x == w - 1 || z == d - 1;
            c.set(x, 0, z, r.nextInt(3) == 0 ? Blocks.MOSSY_COBBLESTONE : Blocks.COBBLESTONE);
            if (edge) {
                int hh = r.nextInt(4);
                for (int y = 1; y <= hh; y++) c.set(x, y, z, r.nextBoolean() ? Blocks.MOSSY_STONE_BRICKS : Blocks.CRACKED_STONE_BRICKS);
            } else if (r.nextInt(12) == 0) c.set(x, 1, z, Blocks.COBWEB);
        }
        c.facing(w / 2, 1, d / 2, Blocks.CHEST, Direction.NORTH);
    }

    /** a ring of broken pillars around an altar, themed by the materials passed in */
    public static void lair(Canvas c, RandomSource r, Block floor, Block pillar, Block accent, Block glow, int rad) {
        for (int x = -rad; x <= rad; x++) for (int z = -rad; z <= rad; z++) {
            double dd = Math.sqrt(x * x + z * z);
            if (dd > rad) continue;
            c.set(x, 0, z, r.nextInt(5) == 0 ? accent : floor);
            for (int y = 1; y < 12; y++) c.set(x, y, z, Blocks.AIR);
        }
        int n = 10;
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n;
            int px = (int) Math.round(Math.cos(a) * (rad - 2)), pz = (int) Math.round(Math.sin(a) * (rad - 2));
            int h = 3 + r.nextInt(6);
            for (int y = 1; y <= h; y++) c.set(px, y, pz, pillar);
            if (h > 6) c.set(px, h + 1, pz, glow);
        }
        c.fill(-2, 1, -2, 2, 1, 2, accent);
        c.fill(-1, 2, -1, 1, 2, 1, floor);
        c.set(0, 3, 0, glow);
    }
}
