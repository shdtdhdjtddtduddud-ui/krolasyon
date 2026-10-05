package com.krolasyon.sololeveling.world;

import com.krolasyon.sololeveling.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * Procedural Seoul. Everything is a pure function of the world column (x, z) so every chunk can be generated on its
 * own without seams. The city is made of 64x64 "superblocks": a 16 wide road along the low x and low z edge and a
 * 48x48 building area. Some superblocks near the centre are fixed landmarks (Association, guild halls, plaza...).
 */
public final class CityPlan {
    public static final int GROUND = 63;
    public static final int SB = 64, ROAD = 16;
    public static final int CITY_RADIUS = 10;

    /** Receives generated content. */
    public interface Writer {
        void set(int x, int y, int z, BlockState s);

        void portal(int x, int y, int z, String target);

        void npc(int x, int y, int z, String role, float yaw);
    }

    private CityPlan() {}

    // ------------------------------------------------------------------ helpers

    static int hash(int a, int b, int c) {
        int h = a * 73856093 ^ b * 19349663 ^ c * 83492791;
        h ^= h >>> 13;
        h *= 0x5bd1e995;
        h ^= h >>> 15;
        return h & 0x7fffffff;
    }

    static int sbOf(int v) { return Math.floorDiv(v, SB); }

    static int local(int v) { return Math.floorMod(v, SB); }

    /** Door / arrival point of a landmark superblock (on the sidewalk in front of it). */
    public static BlockPos landmarkDoor(int sx, int sz) {
        if (sx == 0 && sz == 0) return new BlockPos(40, GROUND + 1, 52);
        return new BlockPos(sx * SB + 40, GROUND + 1, sz * SB + 13);
    }

    static BlockState b(Block b) { return b.defaultBlockState(); }

    static final BlockState AIR = Blocks.AIR.defaultBlockState();
    static final BlockState ASPHALT = b(ModBlocks.ASPHALT.get());

    // ------------------------------------------------------------------ entry point

    public static void column(Writer w, int x, int z) {
        int sx = sbOf(x), sz = sbOf(z);
        int lx = local(x), lz = local(z);
        int dist = Math.max(Math.abs(sx), Math.abs(sz));
        if (dist > CITY_RADIUS) {
            countryside(w, x, z);
            return;
        }
        if (lx < ROAD || lz < ROAD) {
            road(w, x, z, lx, lz, sx, sz);
            return;
        }
        int ax = lx - ROAD, az = lz - ROAD; // 0..47 inside the building area
        Landmark lm = Landmark.at(sx, sz);
        if (lm != null) {
            lm.column(w, x, z, ax, az);
            return;
        }
        int type = blockType(sx, sz, dist);
        switch (type) {
            case 0 -> towers(w, x, z, ax, az, sx, sz);
            case 1 -> apartments(w, x, z, ax, az, sx, sz);
            case 2 -> shops(w, x, z, ax, az, sx, sz);
            default -> park(w, x, z, ax, az, sx, sz);
        }
    }

    static int blockType(int sx, int sz, int dist) {
        int h = hash(sx, sz, 7) % 100;
        if (dist <= 3) return h < 60 ? 0 : h < 80 ? 2 : h < 92 ? 1 : 3;
        if (dist <= 6) return h < 30 ? 0 : h < 65 ? 1 : h < 85 ? 2 : 3;
        return h < 10 ? 0 : h < 55 ? 1 : h < 75 ? 2 : 3;
    }

    // ------------------------------------------------------------------ roads

    static void road(Writer w, int x, int z, int lx, int lz, int sx, int sz) {
        boolean alongX = lz < ROAD; // road running along the x axis (strip in z)
        boolean alongZ = lx < ROAD;
        int g = GROUND;
        if (alongX && alongZ) {
            // intersection with zebra crossings at the edges
            boolean zebra = (lx >= 3 && lx <= 12 && (lz <= 2 || lz >= 13) && lx % 2 == 0) || (lz >= 3 && lz <= 12 && (lx <= 2 || lx >= 13) && lz % 2 == 0);
            boolean corner = (lx <= 2 || lx >= 13) && (lz <= 2 || lz >= 13);
            w.set(x, g, z, corner ? b(ModBlocks.SIDEWALK.get()) : zebra ? b(ModBlocks.ROAD_CROSSING.get()) : ASPHALT);
            if (corner && lx == 14 && lz == 14) trafficLight(w, x, z);
            return;
        }
        int r = alongX ? lz : lx;          // across the road
        int t = alongX ? x : z;            // along the road
        if (r <= 2 || r >= 13) {
            w.set(x, g, z, b(ModBlocks.SIDEWALK.get()));
            boolean curb = r == 2 || r == 13;
            if (curb) w.set(x, g + 1, z, Blocks.SMOOTH_STONE_SLAB.defaultBlockState());
            int m = Math.floorMod(t, 16);
            if ((r == 1 || r == 14) && m == 8) streetLamp(w, x, z, alongX, r == 1);
            if ((r == 1 || r == 14) && m == 0 && hash(x, z, 3) % 3 != 0) planterTree(w, x, z);
            if ((r == 1 || r == 14) && m == 12 && hash(x, z, 4) % 4 == 0) {
                Direction face = alongX ? (r == 1 ? Direction.SOUTH : Direction.NORTH) : (r == 1 ? Direction.EAST : Direction.WEST);
                w.set(x, g + 1, z, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, face.getOpposite()));
            }
            if ((r == 1 || r == 14) && m == 4 && hash(t / 64, r, 9) % 5 == 0) newsKiosk(w, x, z);
            return;
        }
        boolean centre = (r == 7 || r == 8) && Math.floorMod(t, 6) < 3;
        boolean lane = (r == 5 || r == 10) && Math.floorMod(t, 8) < 3;
        w.set(x, g, z, centre ? b(ModBlocks.ROAD_LINE.get()) : lane ? b(ModBlocks.ROAD_CROSSING.get()) : ASPHALT);
        // parked cars along the lanes
        if ((r == 4 || r == 11) && hash(Math.floorDiv(t, 6), r, sx * 31 + sz) % 9 == 0) car(w, x, z, Math.floorMod(t, 6), alongX, hash(Math.floorDiv(t, 6), r, 77));
    }

    static void streetLamp(Writer w, int x, int z, boolean alongX, boolean low) {
        int g = GROUND;
        for (int y = 1; y <= 5; y++) w.set(x, g + y, z, Blocks.DARK_OAK_FENCE.defaultBlockState());
        w.set(x, g + 6, z, Blocks.SMOOTH_STONE_SLAB.defaultBlockState());
        w.set(x, g + 5, z, Blocks.IRON_BARS.defaultBlockState());
        w.set(x, g + 6, z, Blocks.SEA_LANTERN.defaultBlockState());
        w.set(x, g + 7, z, Blocks.SMOOTH_STONE_SLAB.defaultBlockState());
    }

    static void planterTree(Writer w, int x, int z) {
        int g = GROUND;
        w.set(x, g, z, Blocks.GRASS_BLOCK.defaultBlockState());
        for (int y = 1; y <= 3; y++) w.set(x, g + y, z, Blocks.BIRCH_LOG.defaultBlockState());
        w.set(x, g + 4, z, Blocks.BIRCH_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
        w.set(x, g + 5, z, Blocks.BIRCH_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
    }

    static void newsKiosk(Writer w, int x, int z) {
        int g = GROUND;
        w.set(x, g + 1, z, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
        w.set(x, g + 2, z, ModBlocks.NEWS_BOARD.get().defaultBlockState());
        w.set(x, g + 3, z, Blocks.POLISHED_DEEPSLATE_SLAB.defaultBlockState());
    }

    static void trafficLight(Writer w, int x, int z) {
        int g = GROUND;
        for (int y = 1; y <= 4; y++) w.set(x, g + y, z, Blocks.IRON_BARS.defaultBlockState());
        w.set(x, g + 5, z, Blocks.BLACK_CONCRETE.defaultBlockState());
        w.set(x, g + 6, z, ModBlocks.NEON_RED.get().defaultBlockState());
        w.set(x, g + 7, z, Blocks.BLACK_CONCRETE.defaultBlockState());
    }

    /** Small parked car: 3 long, 1 wide column slice. t = position along the car (0..5, car uses 1..3). */
    static void car(Writer w, int x, int z, int t, boolean alongX, int h) {
        if (t < 1 || t > 3) return;
        BlockState[] paint = {b(Blocks.WHITE_CONCRETE), b(Blocks.BLACK_CONCRETE), b(Blocks.GRAY_CONCRETE), b(Blocks.RED_CONCRETE), b(Blocks.BLUE_CONCRETE), b(Blocks.LIGHT_GRAY_CONCRETE)};
        BlockState c = paint[h % paint.length];
        int g = GROUND;
        w.set(x, g + 1, z, c);
        if (t == 2) w.set(x, g + 2, z, Blocks.BLACK_STAINED_GLASS.defaultBlockState());
        else w.set(x, g + 2, z, Blocks.BLACK_CARPET.defaultBlockState());
    }

    // ------------------------------------------------------------------ lots

    static void lotGround(Writer w, int x, int z, boolean grass) {
        int g = GROUND;
        if (grass) w.set(x, g, z, Blocks.GRASS_BLOCK.defaultBlockState());
        else w.set(x, g, z, Math.floorMod(x + z, 2) == 0 ? b(Blocks.POLISHED_ANDESITE) : b(ModBlocks.SIDEWALK.get()));
    }

    /** 2x2 lots of 24x24 with a tower each. */
    static void towers(Writer w, int x, int z, int ax, int az, int sx, int sz) {
        int lotX = ax / 24, lotZ = az / 24;
        int px = ax % 24, pz = az % 24;
        int h = hash(sx * 2 + lotX, sz * 2 + lotZ, 11);
        int size = 14 + h % 6;              // 14..19 footprint
        int off = (24 - size) / 2;
        int height = 30 + (h >> 4) % 80;    // 30..109
        int dist = Math.max(Math.abs(sx), Math.abs(sz));
        if (dist > 4) height = height / 2 + 10;
        int style = (h >> 9) % 4;
        int bx = px - off, bz = pz - off;
        if (bx < 0 || bz < 0 || bx >= size || bz >= size) {
            lotGround(w, x, z, false);
            if ((px == 1 || px == 22) && (pz == 1 || pz == 22)) planterTree(w, x, z);
            return;
        }
        lotGround(w, x, z, false);
        tower(w, x, z, bx, bz, size, size, height, style, h, lotZ == 0 ? Direction.NORTH : Direction.SOUTH);
    }

    /**
     * Writes one column of a box shaped high-rise. (bx,bz) is the column inside the footprint (w x d).
     * Styles: 0 glass tower, 1 concrete office, 2 dark tower with neon, 3 white tower with glass bands.
     */
    static void tower(Writer w, int x, int z, int bx, int bz, int wd, int dp, int height, int style, int h, Direction doorSide) {
        int g = GROUND;
        boolean edgeX = bx == 0 || bx == wd - 1, edgeZ = bz == 0 || bz == dp - 1;
        boolean corner = edgeX && edgeZ;
        boolean perim = edgeX || edgeZ;
        boolean stepped = height > 60;
        int base = stepped ? height - 14 : height;
        boolean inner = bx >= 2 && bz >= 2 && bx < wd - 2 && bz < dp - 2;
        boolean innerEdge = inner && (bx == 2 || bz == 2 || bx == wd - 3 || bz == dp - 3);
        int top = perim ? base : (stepped && inner ? height : base);
        BlockState frame, glass, lit, band;
        switch (style) {
            case 0 -> { frame = b(Blocks.LIGHT_GRAY_CONCRETE); glass = b(ModBlocks.GLASS_FACADE.get()); lit = b(ModBlocks.OFFICE_WINDOW.get()); band = b(ModBlocks.CONCRETE_PANEL.get()); }
            case 1 -> { frame = b(ModBlocks.CONCRETE_PANEL.get()); glass = b(ModBlocks.DARK_WINDOW.get()); lit = b(ModBlocks.OFFICE_WINDOW.get()); band = b(Blocks.SMOOTH_STONE); }
            case 2 -> { frame = b(ModBlocks.DARK_PANEL.get()); glass = b(ModBlocks.DARK_WINDOW.get()); lit = b(ModBlocks.OFFICE_WINDOW.get()); band = b(h % 2 == 0 ? ModBlocks.NEON_BLUE.get() : ModBlocks.NEON_PURPLE.get()); }
            default -> { frame = b(Blocks.WHITE_CONCRETE); glass = b(ModBlocks.GLASS_FACADE.get()); lit = b(ModBlocks.OFFICE_WINDOW.get()); band = b(Blocks.WHITE_CONCRETE); }
        }
        for (int y = 1; y <= top; y++) {
            boolean wall = perim || (stepped && innerEdge && y > base);
            if (wall) {
                BlockState s;
                if (corner || (stepped && innerEdge && (bx == 2 || bx == wd - 3) && (bz == 2 || bz == dp - 3))) s = frame;
                else if (y % 4 == 0) s = band;
                else if (y <= 4 && perim) {
                    boolean doorWall = (doorSide == Direction.NORTH && bz == 0) || (doorSide == Direction.SOUTH && bz == dp - 1);
                    int mid = wd / 2;
                    if (doorWall && (bx == mid || bx == mid - 1) && y <= 3) s = AIR;
                    else s = b(Blocks.GLASS);
                } else {
                    int col = (edgeZ || bz == 2 || bz == dp - 3) ? bx : bz;
                    if (col % 3 == 0 && style != 0) s = frame;
                    else s = hash(x / 2, y / 4, z / 2 + h) % 10 < 3 ? lit : glass;
                }
                w.set(x, g + y, z, s);
            } else if (y % 4 == 0 && y < top) {
                w.set(x, g + y, z, b(Blocks.SMOOTH_STONE));
            } else if (y == 1 && (bx * 7 + bz * 3) % 11 == 0) {
                w.set(x, g + y, z, b(Blocks.SPRUCE_SLAB));
            }
        }
        boolean wallTop = perim || (stepped && innerEdge);
        w.set(x, g + top + 1, z, wallTop ? b(Blocks.SMOOTH_STONE_SLAB) : b(Blocks.SMOOTH_STONE));
        if (!wallTop) w.set(x, g + top, z, b(Blocks.SMOOTH_STONE));
        int rx = wd / 2, rz = dp / 2;
        if (bx == rx && bz == rz) {
            int ty = g + height + 2;
            int n = 6 + h % 10;
            for (int y = 0; y < n; y++) w.set(x, ty + y, z, b(Blocks.IRON_BARS));
            w.set(x, ty + n, z, b(ModBlocks.NEON_RED.get()));
        }
        if (!stepped && !perim && (bx == 3 || bx == 4) && (bz == 3 || bz == 4)) {
            w.set(x, g + height + 1, z, b(Blocks.LIGHT_GRAY_CONCRETE));
            w.set(x, g + height + 2, z, b(Blocks.IRON_TRAPDOOR));
        }
    }

    /** Korean style apartment slabs: two long blocks with balconies. */
    static void apartments(Writer w, int x, int z, int ax, int az, int sx, int sz) {
        int h = hash(sx, sz, 21);
        int height = 36 + h % 30;
        int g = GROUND;
        boolean inA = az >= 6 && az < 16, inB = az >= 30 && az < 40;
        boolean inX = ax >= 4 && ax < 44;
        if (!(inX && (inA || inB))) {
            boolean grass = (az >= 18 && az < 28) && ax > 6 && ax < 42;
            lotGround(w, x, z, grass);
            if (grass && hash(x, z, 5) % 23 == 0) smallTree(w, x, z);
            if (!grass && az >= 18 && az < 28 && (ax <= 6 || ax >= 42) && hash(ax / 3, az, 6) % 5 == 0) car(w, x, z, 2, true, hash(x, z, 1));
            return;
        }
        lotGround(w, x, z, false);
        int bx = ax - 4, bz = (inA ? az - 6 : az - 30);
        boolean edgeX = bx == 0 || bx == 39, edgeZ = bz == 0 || bz == 9;
        boolean balconySide = inA ? bz == 9 : bz == 0;
        int num = (h >> 5) % 9 + 1;
        for (int y = 1; y <= height; y++) {
            if (edgeX || edgeZ) {
                BlockState s;
                if (edgeX) s = (y % 3 == 0 || bz % 3 == 0) ? b(Blocks.WHITE_CONCRETE) : b(Blocks.LIGHT_GRAY_CONCRETE);
                else if (y % 3 == 0) s = b(Blocks.WHITE_CONCRETE);
                else if (bx % 4 == 0) s = b(Blocks.WHITE_CONCRETE);
                else if (balconySide) s = y % 3 == 1 ? b(Blocks.WHITE_STAINED_GLASS) : (hash(x, y / 3, z) % 4 == 0 ? b(ModBlocks.OFFICE_WINDOW.get()) : b(Blocks.LIGHT_BLUE_STAINED_GLASS));
                else s = (bx % 4 == 2 && y % 3 == 2) ? b(Blocks.GLASS) : b(Blocks.WHITE_CONCRETE);
                if (y <= 3 && edgeZ && !balconySide && (bx == 19 || bx == 20)) s = y == 3 ? b(Blocks.SMOOTH_QUARTZ_SLAB) : AIR;
                // painted building number on the end wall
                if (edgeX && y >= height - 10 && y <= height - 4 && bz >= 3 && bz <= 6 && digit(num, bz - 3, height - 4 - y)) s = b(Blocks.BLUE_CONCRETE);
                w.set(x, g + y, z, s);
            } else if (y % 3 == 0) w.set(x, g + y, z, b(Blocks.SMOOTH_STONE));
        }
        w.set(x, g + height + 1, z, (edgeX || edgeZ) ? b(Blocks.WHITE_CONCRETE) : b(Blocks.GRAY_CONCRETE));
        if (edgeX || edgeZ) w.set(x, g + height + 2, z, b(Blocks.SMOOTH_QUARTZ_SLAB));
        if (bx == 20 && bz == 5) {
            for (int y = 1; y <= 4; y++) w.set(x, g + height + 1 + y, z, b(Blocks.WHITE_CONCRETE));
            w.set(x, g + height + 6, z, b(ModBlocks.NEON_RED.get()));
        }
    }

    /** 3x5 pixel font for digits 1..9 (column cx 0..3 maps to 0..2 + spacing). */
    static boolean digit(int n, int cx, int cy) {
        if (cx > 2 || cy < 0 || cy > 6) return false;
        String[] font = {
                "010110010010010010111", "111001001111100100111", "111001001111001001111", "101101101111001001001", "111100100111001001111",
                "111100100111101101111", "111001001010010010010", "111101101111101101111", "111101101111001001111"};
        String f = font[Math.max(0, Math.min(8, n - 1))];
        int row = 6 - cy;
        return f.charAt(row * 3 + cx) == '1';
    }

    static void smallTree(Writer w, int x, int z) {
        int g = GROUND;
        for (int y = 1; y <= 4; y++) w.set(x, g + y, z, b(Blocks.OAK_LOG));
        for (int y = 3; y <= 6; y++) w.set(x, g + y + 1, z, Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
    }

    /** Low shops in a 4x4 grid of 12x12 with neon signs. */
    static void shops(Writer w, int x, int z, int ax, int az, int sx, int sz) {
        int cx = ax / 12, cz = az / 12, px = ax % 12, pz = az % 12;
        int h = hash(sx * 4 + cx, sz * 4 + cz, 31);
        int g = GROUND;
        int height = 6 + h % 12;
        boolean inside = px >= 1 && px <= 10 && pz >= 1 && pz <= 10;
        if (!inside) {
            lotGround(w, x, z, false);
            return;
        }
        lotGround(w, x, z, false);
        boolean edge = px == 1 || px == 10 || pz == 1 || pz == 10;
        BlockState[] walls = {b(Blocks.BRICKS), b(Blocks.WHITE_TERRACOTTA), b(Blocks.LIGHT_GRAY_CONCRETE), b(Blocks.CYAN_TERRACOTTA), b(Blocks.ORANGE_TERRACOTTA), b(Blocks.SMOOTH_SANDSTONE), b(ModBlocks.CONCRETE_PANEL.get())};
        BlockState[] neon = {b(ModBlocks.NEON_BLUE.get()), b(ModBlocks.NEON_RED.get()), b(ModBlocks.NEON_PURPLE.get()), b(ModBlocks.NEON_GOLD.get())};
        BlockState wall = walls[h % walls.length];
        boolean front = pz == 1;
        for (int y = 1; y <= height; y++) {
            BlockState s = null;
            if (edge) {
                if (front && y <= 3) s = (px == 5 || px == 6) && y <= 2 ? AIR : (y == 3 ? wall : b(Blocks.GLASS));
                else if (front && y == 4) s = neon[(h >> 3) % neon.length];
                else if (y % 4 == 2 && px % 3 == 1) s = hash(x, y, z) % 3 == 0 ? b(ModBlocks.OFFICE_WINDOW.get()) : b(Blocks.GLASS);
                else s = wall;
            } else if (y == 4 || y == 8 || y == 12) s = b(Blocks.SPRUCE_PLANKS);
            if (s != null) w.set(x, g + y, z, s);
        }
        w.set(x, g + height + 1, z, edge ? b(Blocks.STONE_BRICK_SLAB) : b(Blocks.GRAY_CONCRETE));
        if (!edge && pz == 3 && px >= 3 && px <= 8) w.set(x, g + 1, z, b(Blocks.SPRUCE_SLAB).setValue(SlabBlock.TYPE, SlabType.TOP));
        if (!edge && pz == 8 && px % 2 == 0) w.set(x, g + 1, z, b(Blocks.BARREL));
        if (!edge && px == 5 && pz == 5) w.set(x, g + 3, z, b(Blocks.LANTERN).setValue(LanternBlock.HANGING, true));
    }

    /** City park with paths, trees, benches and a pond. */
    static void park(Writer w, int x, int z, int ax, int az, int sx, int sz) {
        int g = GROUND;
        int dx = ax - 24, dz = az - 24;
        int d2 = dx * dx + dz * dz;
        boolean path = Math.abs(dx) <= 1 || Math.abs(dz) <= 1 || (d2 >= 100 && d2 <= 144);
        if (d2 < 49) {
            w.set(x, g, z, b(Blocks.WATER));
            w.set(x, g - 1, z, b(Blocks.SAND));
            if (d2 < 2) {
                w.set(x, g, z, b(Blocks.STONE_BRICKS));
                w.set(x, g + 1, z, b(Blocks.STONE_BRICK_WALL));
                w.set(x, g + 2, z, b(Blocks.SEA_LANTERN));
            }
            return;
        }
        if (d2 < 64) {
            w.set(x, g, z, b(Blocks.STONE_BRICKS));
            w.set(x, g + 1, z, b(Blocks.STONE_BRICK_SLAB));
            return;
        }
        if (path) {
            w.set(x, g, z, b(Blocks.GRAVEL));
            if (d2 >= 100 && d2 <= 144 && hash(x, z, 8) % 17 == 0) w.set(x, g + 1, z, b(Blocks.SPRUCE_SLAB));
            return;
        }
        w.set(x, g, z, b(Blocks.GRASS_BLOCK));
        int th = hash(x, z, 13);
        if (th % 29 == 0 && ax > 2 && az > 2 && ax < 45 && az < 45) cherryTree(w, x, z, th);
        else if (th % 7 == 0) w.set(x, g + 1, z, b(th % 2 == 0 ? Blocks.GRASS : Blocks.POPPY));
        if ((ax == 0 || az == 0 || ax == 47 || az == 47) && Math.floorMod(ax + az, 2) == 0) w.set(x, g + 1, z, b(Blocks.SPRUCE_FENCE));
    }

    static void cherryTree(Writer w, int x, int z, int h) {
        int g = GROUND;
        int th = 4 + h % 3;
        for (int y = 1; y <= th; y++) w.set(x, g + y, z, b(Blocks.CHERRY_LOG));
        w.set(x, g + th + 1, z, Blocks.CHERRY_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
        w.set(x, g + th + 2, z, Blocks.CHERRY_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
    }

    static void countryside(Writer w, int x, int z) {
        int h = hash(x, z, 99);
        if (h % 61 == 0) {
            int th = 4 + h % 3;
            for (int y = 1; y <= th; y++) w.set(x, GROUND + y, z, b(Blocks.OAK_LOG));
            for (int y = th - 1; y <= th + 1; y++) w.set(x, GROUND + y, z, Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
        } else if (h % 9 == 0) w.set(x, GROUND + 1, z, b(Blocks.GRASS));
    }

    static BlockState stair(Block s, Direction f, boolean top) {
        return s.defaultBlockState().setValue(StairBlock.FACING, f).setValue(StairBlock.HALF, top ? Half.TOP : Half.BOTTOM);
    }
}
