package com.sololeveling.world;

import com.sololeveling.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/** Procedurally builds a themed dungeon (entry hall -> rooms -> boss hall) inside the dungeon dimension. */
public final class DungeonBuilder {
    private DungeonBuilder() {}

    public record MobSpot(BlockPos pos, int room) {}

    public static final class Layout {
        public BlockPos spawn, exit, boss;
        public final List<MobSpot> spots = new ArrayList<>();
        public int minX, maxX, minZ, maxZ;
    }

    /** theme palette: wall, floor, ceiling, pillar, light, accent */
    private record Palette(String wall, String floor, String ceil, String pillar, String light, String accent, boolean pools, String pool) {}

    private static Palette palette(String theme) {
        return switch (theme) {
            case "goblin_cave" -> new Palette("sololeveling:goblin_stone", "minecraft:coarse_dirt", "sololeveling:goblin_stone", "minecraft:stone_bricks", "minecraft:shroomlight", "minecraft:cobweb", false, "minecraft:water");
            case "temple" -> new Palette("sololeveling:temple_stone", "sololeveling:dungeon_floor", "sololeveling:temple_stone", "sololeveling:dungeon_pillar", "minecraft:glowstone", "minecraft:gold_block", false, "minecraft:water");
            case "venom_swamp" -> new Palette("sololeveling:swamp_stone", "minecraft:moss_block", "sololeveling:swamp_stone", "minecraft:mossy_stone_bricks", "minecraft:verdant_froglight", "minecraft:vine", true, "minecraft:water");
            case "ice_cave" -> new Palette("sololeveling:ice_bricks", "minecraft:packed_ice", "sololeveling:ice_bricks", "minecraft:blue_ice", "minecraft:sea_lantern", "minecraft:snow_block", false, "minecraft:water");
            case "hell_den" -> new Palette("minecraft:blackstone", "minecraft:basalt", "minecraft:blackstone", "minecraft:polished_blackstone_bricks", "minecraft:shroomlight", "minecraft:magma_block", true, "minecraft:lava");
            case "ant_nest" -> new Palette("sololeveling:ant_hive", "sololeveling:ant_hive", "sololeveling:ant_hive", "minecraft:brown_mushroom_block", "minecraft:shroomlight", "minecraft:cobweb", false, "minecraft:water");
            case "demon_castle" -> new Palette("sololeveling:demon_bricks", "minecraft:polished_blackstone", "sololeveling:demon_bricks", "minecraft:chiseled_polished_blackstone", "minecraft:soul_lantern", "minecraft:crying_obsidian", false, "minecraft:lava");
            default -> new Palette("minecraft:deepslate_bricks", "minecraft:blackstone", "minecraft:deepslate_bricks", "minecraft:polished_blackstone_bricks", "minecraft:shroomlight", "minecraft:magma_block", true, "minecraft:lava");
        };
    }

    private static BlockState st(String id) {
        Block b = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id));
        return b == null ? Blocks.STONE.defaultBlockState() : b.defaultBlockState();
    }

    private static final class B {
        final ServerLevel level;
        final Palette p;
        final BlockState wall, floor, ceil, pillar, light, air = Blocks.AIR.defaultBlockState();
        final int FY;
        B(ServerLevel l, Palette p, int fy) {
            level = l; this.p = p; FY = fy;
            wall = st(p.wall); floor = st(p.floor); ceil = st(p.ceil); pillar = st(p.pillar); light = st(p.light);
        }

        void set(int x, int y, int z, BlockState s) {
            level.setBlock(new BlockPos(x, y, z), s, 2);
        }

        void fill(int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
            BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
            for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++)
                    for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
                        level.setBlock(m.set(x, y, z), s, 2);
        }

        /** solid shell with hollow interior x0..x1 / z0..z1, height h */
        void room(int x0, int z0, int x1, int z1, int h) {
            fill(x0 - 1, FY - 2, z0 - 1, x1 + 1, FY + h + 1, z1 + 1, wall);
            fill(x0, FY, z0, x1, FY + h - 1, z1, air);
            fill(x0, FY - 1, z0, x1, FY - 1, z1, floor);
            fill(x0, FY + h, z0, x1, FY + h, z1, ceil);
        }

        void corridor(int x0, int x1, int zc, int w, int h) {
            int hw = w / 2;
            fill(x0, FY - 2, zc - hw - 1, x1, FY + h + 1, zc + hw + 1, wall);
            fill(x0, FY, zc - hw, x1, FY + h - 1, zc + hw, air);
            fill(x0, FY - 1, zc - hw, x1, FY - 1, zc + hw, floor);
            fill(x0, FY + h, zc - hw, x1, FY + h, zc + hw, ceil);
        }

        void pillarAt(int x, int z, int h) {
            for (int y = 0; y < h; y++) set(x, FY + y, z, pillar);
        }
    }

    public static Layout build(ServerLevel level, BlockPos origin, String theme, String rank, long seed, int rankIdx) {
        RandomSource rnd = RandomSource.create(seed * 31 + 7);
        Palette pal = palette(theme);
        int fy = origin.getY();
        int zc = origin.getZ();
        B b = new B(level, pal, fy);
        Layout lay = new Layout();
        int x = origin.getX();
        lay.minX = x - 2;
        lay.minZ = zc - 40;
        lay.maxZ = zc + 40;

        // ---------------- entry hall
        int hall = 13;
        b.room(x, zc - hall / 2, x + hall - 1, zc + hall / 2, 7);
        lightGrid(b, x, zc - hall / 2, x + hall - 1, zc + hall / 2, 7, 5);
        lay.exit = new BlockPos(x + 2, fy, zc);
        lay.spawn = new BlockPos(x + 6, fy, zc);
        // runic accent in hall floor
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
            if (Math.abs(dx) + Math.abs(dz) == 2 || (dx == 0 && dz == 0)) b.set(x + 6 + dx, fy - 1, zc + dz, st(pal.accent.contains("gold") || pal.accent.contains("obsidian") ? pal.accent : pal.light));
        x += hall;

        int rooms = 2 + Math.min(3, rankIdx / 2) + rnd.nextInt(2);
        for (int r = 0; r < rooms; r++) {
            int len = 9 + rnd.nextInt(4);
            b.corridor(x, x + len - 1, zc, 5, 6);
            for (int lx = x + 2; lx < x + len; lx += 4) b.set(lx, fy + 5, zc, b.light);
            if (pal.accent.contains("cobweb") || pal.accent.contains("vine")) {
                for (int i = 0; i < 3; i++) b.set(x + rnd.nextInt(len), fy + 5, zc - 2 + rnd.nextInt(5), st(pal.accent.contains("cobweb") ? pal.accent : "minecraft:air"));
            }
            x += len;
            int w = 17 + rnd.nextInt(4) * 2;
            int d = 17 + rnd.nextInt(4) * 2;
            int h = 8 + rnd.nextInt(3);
            int z0 = zc - d / 2, z1 = zc + d / 2;
            b.room(x, z0, x + w - 1, z1, h);
            lightGrid(b, x, z0, x + w - 1, z1, h, 4);
            // pillars
            for (int px : new int[]{x + 3, x + w - 4})
                for (int pz : new int[]{z0 + 3, z1 - 3}) b.pillarAt(px, pz, h);
            if (pal.pools) {
                int px = x + 6 + rnd.nextInt(Math.max(1, w - 12));
                int pz = z0 + 5 + rnd.nextInt(Math.max(1, d - 10));
                BlockState fluid = st(pal.pool);
                b.fill(px, fy - 1, pz, px + 2, fy - 1, pz + 2, fluid);
            }
            decorate(b, theme, pal, rnd, x, z0, x + w - 1, z1, fy, h, false);
            int mobs = 3 + rnd.nextInt(3) + rankIdx / 2;
            for (int m = 0; m < mobs; m++) {
                int mx = x + 3 + rnd.nextInt(w - 6), mz = z0 + 3 + rnd.nextInt(d - 6);
                lay.spots.add(new MobSpot(new BlockPos(mx, fy, mz), r));
            }
            x += w;
            lay.maxX = x;
        }

        // ---------------- boss hall
        int len = 12;
        b.corridor(x, x + len - 1, zc, 5, 6);
        for (int lx = x + 3; lx < x + len; lx += 5) b.set(lx, fy + 5, zc, b.light);
        x += len;
        boolean dragon = theme.equals("dragon_lair");
        int bw = dragon ? 61 : 35, bd = dragon ? 61 : 35, bh = dragon ? 30 : 16;
        int z0 = zc - bd / 2, z1 = zc + bd / 2;
        b.room(x, z0, x + bw - 1, z1, bh);
        lightGrid(b, x, z0, x + bw - 1, z1, bh, dragon ? 6 : 5);
        for (int i = 0; i < 4; i++) {
            int px = x + 6 + (i % 2) * (bw - 13);
            int pz = z0 + 6 + (i / 2) * (bd - 13);
            b.pillarAt(px, pz, bh);
            b.pillarAt(px + 1, pz, bh);
            b.pillarAt(px, pz + 1, bh);
            b.pillarAt(px + 1, pz + 1, bh);
        }
        // ceremonial dais
        int cx = x + bw / 2, cz = zc;
        for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) {
            if (Math.abs(dx) + Math.abs(dz) <= 6) b.set(cx + dx, fy - 1, cz + dz, Math.abs(dx) + Math.abs(dz) % 2 == 0 ? b.pillar : st(pal.light.equals("minecraft:glowstone") ? "minecraft:gold_block" : pal.accent));
        }
        if (pal.pools) {
            for (int k = 0; k < 5; k++) {
                int px = x + 4 + rnd.nextInt(bw - 8), pz = z0 + 4 + rnd.nextInt(bd - 8);
                if (Math.abs(px - cx) < 8 && Math.abs(pz - cz) < 8) continue;
                b.fill(px, fy - 1, pz, px + 2, fy - 1, pz + 2, st(pal.pool));
            }
        }
        decorate(b, theme, pal, rnd, x, z0, x + bw - 1, z1, fy, bh, true);
        lay.boss = new BlockPos(cx, fy, cz);
        lay.maxX = x + bw;
        lay.minZ = z0 - 2;
        lay.maxZ = z1 + 2;
        return lay;
    }

    private static void lightGrid(B b, int x0, int z0, int x1, int z1, int h, int step) {
        for (int x = x0 + step / 2; x <= x1; x += step)
            for (int z = z0 + step / 2; z <= z1; z += step)
                b.set(x, b.FY + h, z, b.light);
    }

    private static BlockState hangLantern(String id) {
        return st(id).setValue(LanternBlock.HANGING, true);
    }

    private static BlockState stalactite() {
        return Blocks.POINTED_DRIPSTONE.defaultBlockState().setValue(BlockStateProperties.VERTICAL_DIRECTION, Direction.DOWN);
    }

    private static void decorate(B b, String theme, Palette pal, RandomSource rnd, int x0, int z0, int x1, int z1, int fy, int h, boolean boss) {
        int w = x1 - x0 + 1, d = z1 - z0 + 1;
        int count = boss ? 60 : 26;
        int zc = (z0 + z1) / 2;
        // wall-side light strip so the rooms are never pitch dark
        for (int x = x0 + 2; x <= x1 - 1; x += boss ? 5 : 6) {
            b.set(x, fy + 3, z0, b.light);
            b.set(x, fy + 3, z1, b.light);
        }
        for (int i = 0; i < count; i++) {
            int x = x0 + 1 + rnd.nextInt(Math.max(1, w - 2));
            int z = z0 + 1 + rnd.nextInt(Math.max(1, d - 2));
            switch (theme) {
                case "goblin_cave" -> {
                    int r = rnd.nextInt(6);
                    if (r == 0) { b.set(x, fy, z, Blocks.POINTED_DRIPSTONE.defaultBlockState()); }
                    else if (r == 1) { b.set(x, fy + h - 1, z, stalactite()); }
                    else if (r == 2) b.set(x, fy, z, st("minecraft:bone_block"));
                    else if (r == 3) b.set(x, fy, z, st("minecraft:campfire"));
                    else if (r == 4) b.set(x, fy, z, st("minecraft:mossy_cobblestone"));
                    else b.set(x, fy + h - 1, z, st("minecraft:cobweb"));
                }
                case "temple" -> {
                    int r = rnd.nextInt(5);
                    if (r == 0) { b.pillarAt(x, z, 4); b.set(x, fy + 4, z, st("minecraft:soul_lantern")); }
                    else if (r == 1) b.set(x, fy, z, st("minecraft:chiseled_sandstone"));
                    else if (r == 2) b.set(x, fy, z, st("minecraft:gold_block"));
                    else if (r == 3) b.set(x, fy + h - 1, z, hangLantern("minecraft:lantern"));
                }
                case "venom_swamp" -> {
                    int r = rnd.nextInt(6);
                    if (r == 0) b.set(x, fy, z, st("minecraft:red_mushroom"));
                    else if (r == 1) b.set(x, fy, z, st("minecraft:brown_mushroom"));
                    else if (r == 2) b.set(x, fy, z, st("minecraft:moss_carpet"));
                    else if (r == 3) { for (int k = 0; k < 3 + rnd.nextInt(3); k++) b.set(x, fy + h - 1 - k, z, st("minecraft:vine")); }
                    else if (r == 4) b.set(x, fy, z, st("minecraft:mud"));
                    else b.set(x, fy, z, st("minecraft:fern"));
                }
                case "ice_cave" -> {
                    int r = rnd.nextInt(5);
                    if (r == 0) { for (int k = 0; k < 2 + rnd.nextInt(3); k++) b.set(x, fy + k, z, st("minecraft:blue_ice")); }
                    else if (r == 1) { b.set(x, fy + h - 1, z, stalactite()); b.set(x, fy + h - 2, z, stalactite()); }
                    else if (r == 2) b.set(x, fy, z, st("minecraft:snow"));
                    else if (r == 3) b.set(x, fy, z, st("minecraft:packed_ice"));
                    else b.set(x, fy, z, st("minecraft:powder_snow"));
                }
                case "hell_den" -> {
                    int r = rnd.nextInt(5);
                    if (r == 0) b.set(x, fy, z, st("minecraft:magma_block"));
                    else if (r == 1) { b.set(x, fy, z, st("minecraft:blackstone")); b.set(x, fy + 1, z, st("minecraft:fire")); }
                    else if (r == 2) { for (int k = 0; k < 3; k++) b.set(x, fy + h - 1 - k, z, st("minecraft:chain")); b.set(x, fy + h - 4, z, hangLantern("minecraft:soul_lantern")); }
                    else if (r == 3) b.set(x, fy, z, st("minecraft:bone_block"));
                    else b.set(x, fy, z, st("minecraft:soul_sand"));
                }
                case "ant_nest" -> {
                    int r = rnd.nextInt(5);
                    if (r == 0) b.set(x, fy, z, st("minecraft:turtle_egg").setValue(net.minecraft.world.level.block.TurtleEggBlock.EGGS, 1 + rnd.nextInt(4)));
                    else if (r == 1) b.set(x, fy, z, st("minecraft:honeycomb_block"));
                    else if (r == 2) b.set(x, fy + h - 1, z, stalactite());
                    else if (r == 3) b.set(x, fy, z, st("minecraft:cobweb"));
                    else b.set(x, fy, z, st("minecraft:brown_mushroom_block"));
                }
                case "demon_castle" -> {
                    int r = rnd.nextInt(6);
                    if (r == 0) { for (int k = 0; k < 4; k++) b.set(x, fy + k, z, st("minecraft:chiseled_polished_blackstone")); b.set(x, fy + 4, z, st("minecraft:soul_campfire")); }
                    else if (r == 1) { for (int k = 0; k < 4; k++) b.set(x, fy + h - 1 - k, z, st("minecraft:chain")); b.set(x, fy + h - 5, z, hangLantern("minecraft:soul_lantern")); }
                    else if (r == 2) b.set(x, fy, z, st("minecraft:red_carpet"));
                    else if (r == 3) b.set(x, fy, z, st("minecraft:wither_skeleton_skull"));
                    else if (r == 4) b.set(x, fy, z, st("minecraft:crying_obsidian"));
                    else b.set(x, fy, z, st("minecraft:gilded_blackstone"));
                }
                default -> {
                    int r = rnd.nextInt(5);
                    if (r == 0) { for (int k = 0; k < 2 + rnd.nextInt(3); k++) b.set(x, fy + k, z, st("minecraft:raw_gold_block")); }
                    else if (r == 1) b.set(x, fy, z, st("minecraft:gold_block"));
                    else if (r == 2) b.set(x, fy, z, st("minecraft:bone_block"));
                    else if (r == 3) { b.set(x, fy, z, st("minecraft:obsidian")); b.set(x, fy + 1, z, st("minecraft:obsidian")); b.set(x, fy + 2, z, st("minecraft:crying_obsidian")); }
                    else b.set(x, fy + h - 1, z, stalactite());
                }
            }
        }
        // ceremonial carpet / aisle toward the boss in castle & temple rooms
        if (theme.equals("demon_castle") || theme.equals("temple")) {
            for (int x = x0; x <= x1; x++) for (int dz = -1; dz <= 1; dz++)
                b.set(x, fy, zc + dz, st(theme.equals("temple") ? "minecraft:yellow_carpet" : "minecraft:red_carpet"));
        }
        if (boss) {
            // braziers at the four inner corners
            for (int[] c : new int[][]{{x0 + 3, z0 + 3}, {x1 - 3, z0 + 3}, {x0 + 3, z1 - 3}, {x1 - 3, z1 - 3}}) {
                b.set(c[0], fy, c[1], st("minecraft:stone_bricks"));
                b.set(c[0], fy + 1, c[1], st(theme.equals("ice_cave") ? "minecraft:soul_campfire" : "minecraft:campfire"));
            }
        }
    }
}
