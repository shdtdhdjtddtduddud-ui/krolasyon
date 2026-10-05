package com.krolasyon.sololeveling.world;

import com.krolasyon.sololeveling.entity.MobKind;
import com.krolasyon.sololeveling.entity.SLMonster;
import com.krolasyon.sololeveling.registry.ModBlocks;
import com.krolasyon.sololeveling.shadow.ShadowManager;
import com.krolasyon.sololeveling.system.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import static com.krolasyon.sololeveling.world.Structures.*;

/** Builds and manages the fixed special regions: Double Dungeon, Job Change hall, Demon Castle, Jeju Island, Penalty Zone. */
public final class RegionBuilder {
    static final int Y = 80;

    private RegionBuilder() {}

    static BlockState b(Block b) { return b.defaultBlockState(); }

    static BlockPos origin(String id) {
        Regions.Waypoint w = Regions.WAYPOINTS.get(id);
        return new BlockPos(w.pos().getX(), Y, w.pos().getZ());
    }

    public static BlockPos spawnOf(String id) { return origin(id).above(); }

    public static BlockPos nearestSpawn(BlockPos p) {
        for (String id : new String[]{"double_dungeon", "job_change", "demon_castle", "jeju_island", "penalty_zone"}) {
            BlockPos o = origin(id);
            if (Math.abs(p.getX() - o.getX()) < 400 && Math.abs(p.getZ() - o.getZ()) < 400) return o.above();
        }
        return null;
    }

    static AABB area(String id) {
        BlockPos o = origin(id);
        return new AABB(o.getX() - 120, 0, o.getZ() - 120, o.getX() + 120, 255, o.getZ() + 240);
    }

    public static void ensure(ServerLevel l, String id) {
        WorldState ws = WorldState.get(l.getServer());
        if (ws.builtRegions.contains(id)) return;
        BlockPos o = origin(id);
        switch (id) {
            case "double_dungeon" -> doubleDungeon(l, o);
            case "job_change" -> jobChange(l, o);
            case "demon_castle" -> demonCastle(l, o);
            case "jeju_island" -> jeju(l, o);
            case "penalty_zone" -> penalty(l, o);
            default -> {}
        }
        ws.builtRegions.add(id);
        ws.regionKills.remove(id);
        ws.setDirty();
        spawnBosses(l, id);
    }

    /** Respawns a region's boss when it has been dead for a while. */
    public static void onEnter(ServerLevel l, String id) {
        WorldState ws = WorldState.get(l.getServer());
        Long killed = ws.regionKills.get(id);
        if (killed == null) return;
        if (l.getGameTime() - killed < 20 * 60 * 10) return;
        ws.regionKills.remove(id);
        ws.setDirty();
        for (SLMonster m : l.getEntitiesOfClass(SLMonster.class, area(id))) m.discard();
        spawnBosses(l, id);
    }

    static String inst(String id) { return "region:" + id; }

    static void spawnBosses(ServerLevel l, String id) {
        BlockPos o = origin(id);
        int x = o.getX(), z = o.getZ();
        String in = inst(id);
        var r = l.random;
        switch (id) {
            case "double_dungeon" -> {
                spawn(l, MobKind.STATUE_OF_GOD, x + 0.5, Y + 3, z + 96.5, in).setYRot(180);
                for (int i = 0; i < 4; i++) spawn(l, MobKind.STONE_STATUE, x + (i % 2 == 0 ? -15.5 : 16.5), Y + 1, z + 52 + i * 9, in);
            }
            case "job_change" -> {
                spawn(l, MobKind.IGRIS, x + 0.5, Y + 3, z + 56.5, in);
                for (int i = 0; i < 8; i++) spawn(l, MobKind.CASTLE_KNIGHT, x + (i % 2 == 0 ? -5.5 : 6.5), Y + 1, z + 18 + (i / 2) * 9, in);
            }
            case "demon_castle" -> {
                spawn(l, MobKind.CERBERUS, x + 0.5, Y + 1, z + 40.5, in);
                for (int i = 0; i < 10; i++) spawn(l, MobKind.DEMON, x - 10 + r.nextInt(21), Y + 1, z + 75 + r.nextInt(55), in);
                spawn(l, MobKind.BARAN, x + 0.5, Y + 5, z + 172.5, in);
            }
            case "jeju_island" -> {
                for (int i = 0; i < 8; i++) {
                    double a = r.nextDouble() * Math.PI * 2;
                    spawn(l, MobKind.ANT_SOLDIER, x + Math.cos(a) * 28, Y + 3, z + 70 + Math.sin(a) * 28, in);
                }
                for (int i = 0; i < 5; i++) spawn(l, MobKind.ANT_SOLDIER, x - 6 + r.nextInt(13), Y + 1, z + 66 + r.nextInt(12), in);
                spawn(l, MobKind.BERU, x + 0.5, Y + 1, z + 72.5, in);
            }
            default -> {}
        }
    }

    public static void onBossKilled(ServerLevel l, String id, SLMonster boss) {
        WorldState ws = WorldState.get(l.getServer());
        boolean finalBoss = switch (id) {
            case "double_dungeon" -> boss.kind == MobKind.STATUE_OF_GOD;
            case "job_change" -> boss.kind == MobKind.IGRIS;
            case "demon_castle" -> boss.kind == MobKind.BARAN;
            case "jeju_island" -> boss.kind == MobKind.BERU;
            default -> false;
        };
        for (ServerPlayer p : l.getEntitiesOfClass(ServerPlayer.class, area(id))) {
            HunterData d = HunterCapability.get(p);
            if (boss.kind == MobKind.CERBERUS) Sys.notify(p, Sys.INFO, Sys.t("system.title"), Sys.t("region.cerberus_down"));
            if (!finalBoss) continue;
            switch (id) {
                case "double_dungeon" -> {
                    d.flags.add("cleared_double_dungeon");
                    Sys.notify(p, Sys.QUEST, Sys.t("system.title"), Sys.t("region.double_cleared"));
                }
                case "job_change" -> {
                    if (d.job == Job.NONE && d.level >= 40) {
                        d.job = Job.NECROMANCER;
                        Progression.applyAttributes(p, d);
                        Progression.autoSlot(d);
                        Sys.notify(p, Sys.ARISE, Sys.t("job_change.title"), Sys.t("job_change.done"));
                        ShadowManager.onKill(p, boss);
                        Scheduler.later(40, () -> Sys.info(p, "job_change.arise_igris"));
                    }
                }
                case "demon_castle" -> Sys.notify(p, Sys.QUEST, Sys.t("system.title"), Sys.t("region.baran_down"));
                case "jeju_island" -> Sys.notify(p, Sys.QUEST, Sys.t("system.title"), Sys.t("region.jeju_cleared"));
                default -> {}
            }
            d.markDirty();
            Progression.checkQuest(p);
        }
        if (finalBoss) {
            ws.regionKills.put(id, l.getGameTime());
            ws.setDirty();
            DungeonBuilder.exitPortal(l, boss.blockPosition().offset(0, 0, -6), DungeonTheme.KNIGHT_CASTLE);
            if (id.equals("jeju_island") || id.equals("demon_castle"))
                NewsManager.get(l.getServer()).post(l.getServer(), 1, Sys.t("news." + id + "_cleared"), true);
        }
    }

    // ------------------------------------------------------------------ Double Dungeon

    static void doubleDungeon(ServerLevel l, BlockPos o) {
        int x = o.getX(), z = o.getZ();
        BlockState stone = b(ModBlocks.TEMPLE_STONE.get()), pillar = b(ModBlocks.TEMPLE_PILLAR.get());
        // entrance tunnel (cave)
        room(l, x - 4, Y, z - 4, x + 4, Y + 7, z + 36, b(Blocks.DEEPSLATE), b(Blocks.COBBLED_DEEPSLATE), b(Blocks.DEEPSLATE));
        for (int k = 0; k < 36; k += 6) {
            set(l, x - 3, Y + 4, z + k, b(Blocks.SOUL_LANTERN));
            set(l, x + 3, Y + 4, z + k, b(Blocks.SOUL_LANTERN));
        }
        DungeonBuilder.exitPortal(l, new BlockPos(x, Y + 1, z - 3), DungeonTheme.STATUE_HALL);
        // commandment tablets
        String[] cmd = {"commandment1", "commandment2", "commandment3"};
        for (int i = 0; i < 3; i++) {
            set(l, x - 3, Y + 2, z + 26 + i * 3, stone);
            sign(l, new BlockPos(x - 2, Y + 2, z + 26 + i * 3), Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, Direction.EAST),
                    Component.translatable("sololeveling.region." + cmd[i] + ".a"), Component.translatable("sololeveling.region." + cmd[i] + ".b"));
        }
        // the hall
        int hx0 = x - 24, hx1 = x + 24, hz0 = z + 36, hz1 = z + 104, top = Y + 34;
        room(l, hx0, Y, hz0, hx1, top, hz1, stone, stone, stone);
        fill(l, x - 3, Y + 1, hz0, x + 3, Y + 7, hz0, b(Blocks.AIR));
        for (int xx = hx0 + 1; xx < hx1; xx++)
            for (int zz = hz0 + 1; zz < hz1; zz++) {
                boolean aisle = Math.abs(xx - x) <= 2;
                set(l, xx, Y, zz, aisle ? b(Blocks.CHISELED_SANDSTONE) : ((xx + zz) & 1) == 0 ? stone : b(Blocks.SMOOTH_SANDSTONE));
            }
        for (int zz = hz0 + 6; zz < hz1 - 12; zz += 10)
            for (int side = -1; side <= 1; side += 2) {
                int px = x + side * 13;
                for (int y = Y + 1; y < top; y++) {
                    set(l, px, y, zz, pillar);
                    set(l, px + 1, y, zz, pillar);
                    set(l, px, y, zz + 1, pillar);
                    set(l, px + 1, y, zz + 1, pillar);
                }
                set(l, px + (side < 0 ? 2 : -1), Y + 10, zz, b(Blocks.SEA_LANTERN));
                // statue niches
                int sx = x + side * 20;
                fill(l, sx - 1, Y + 1, zz + 3, sx + 1, Y + 1, zz + 5, b(Blocks.SMOOTH_SANDSTONE));
            }
        for (int zz = hz0 + 2; zz < hz1; zz += 4) {
            set(l, hx0 + 1, top - 3, zz, b(Blocks.SEA_LANTERN));
            set(l, hx1 - 1, top - 3, zz, b(Blocks.SEA_LANTERN));
        }
        // throne of the Statue of God
        int tz = z + 96;
        fill(l, x - 8, Y + 1, tz - 4, x + 8, Y + 2, hz1 - 1, stone);
        fill(l, x - 7, Y + 3, tz + 3, x + 7, Y + 22, hz1 - 1, stone);
        fill(l, x - 8, Y + 3, tz - 2, x - 6, Y + 8, tz + 4, stone);
        fill(l, x + 6, Y + 3, tz - 2, x + 8, Y + 8, tz + 4, stone);
        for (int y = Y + 10; y <= Y + 20; y += 5) set(l, x, y, tz + 2, b(ModBlocks.RUNE_BRICKS.get()));
        fill(l, x - 2, Y + 1, tz - 9, x + 2, Y + 1, tz - 5, b(Blocks.GOLD_BLOCK));
        fill(l, x - 1, Y + 2, tz - 7, x + 1, Y + 2, tz - 7, b(Blocks.CANDLE));
    }

    // ------------------------------------------------------------------ Job Change hall

    static void jobChange(ServerLevel l, BlockPos o) {
        int x = o.getX(), z = o.getZ();
        BlockState wall = b(Blocks.STONE_BRICKS), floor = b(Blocks.POLISHED_DEEPSLATE);
        room(l, x - 14, Y, z - 4, x + 14, Y + 22, z + 66, wall, floor, b(Blocks.DEEPSLATE_TILES));
        DungeonBuilder.exitPortal(l, new BlockPos(x, Y + 1, z - 3), DungeonTheme.KNIGHT_CASTLE);
        for (int zz = z - 3; zz < z + 66; zz++) {
            for (int xx = x - 2; xx <= x + 2; xx++) set(l, xx, Y + 1, zz, b(Blocks.RED_CARPET));
            set(l, x - 3, Y, zz, b(Blocks.GOLD_BLOCK));
            set(l, x + 3, Y, zz, b(Blocks.GOLD_BLOCK));
        }
        for (int zz = z + 4; zz < z + 60; zz += 8)
            for (int side = -1; side <= 1; side += 2) {
                int px = x + side * 10;
                for (int y = Y + 1; y < Y + 22; y++) set(l, px, y, zz, b(Blocks.POLISHED_DEEPSLATE));
                set(l, px - side, Y + 8, zz, b(Blocks.SOUL_LANTERN));
                for (int y = Y + 10; y < Y + 18; y++) set(l, x + side * 13, y, zz, b(Blocks.RED_WOOL));
            }
        // throne
        fill(l, x - 5, Y + 1, z + 54, x + 5, Y + 2, z + 65, b(Blocks.POLISHED_BLACKSTONE_BRICKS));
        fill(l, x - 2, Y + 3, z + 62, x + 2, Y + 10, z + 64, b(Blocks.POLISHED_BLACKSTONE_BRICKS));
        set(l, x, Y + 11, z + 63, b(Blocks.REDSTONE_BLOCK));
        for (int y = Y + 4; y <= Y + 9; y++) set(l, x, y, z + 61, b(Blocks.RED_WOOL));
    }

    // ------------------------------------------------------------------ Demon Castle

    static void demonCastle(ServerLevel l, BlockPos o) {
        int x = o.getX(), z = o.getZ();
        BlockState brick = b(ModBlocks.DEMON_BRICKS.get()), black = b(Blocks.BLACKSTONE), pol = b(Blocks.POLISHED_BLACKSTONE_BRICKS);
        // courtyard
        room(l, x - 30, Y, z - 6, x + 30, Y + 30, z + 60, brick, black, b(Blocks.AIR));
        fill(l, x - 29, Y + 14, z - 5, x + 29, Y + 30, z + 59, b(Blocks.AIR));
        DungeonBuilder.exitPortal(l, new BlockPos(x, Y + 1, z - 5), DungeonTheme.DEMON_RUINS);
        for (int i = 0; i < 8; i++) {
            int lx = x - 24 + (i % 4) * 16, lz = z + 12 + (i / 4) * 26;
            fill(l, lx - 1, Y, lz - 1, lx + 1, Y, lz + 1, b(Blocks.LAVA));
        }
        for (int zz = z; zz < z + 60; zz += 10) {
            for (int y = Y + 1; y < Y + 12; y++) {
                set(l, x - 29, y, zz, pol);
                set(l, x + 29, y, zz, pol);
            }
            set(l, x - 28, Y + 12, zz, b(Blocks.SHROOMLIGHT));
            set(l, x + 28, Y + 12, zz, b(Blocks.SHROOMLIGHT));
        }
        // castle hall
        room(l, x - 18, Y, z + 60, x + 18, Y + 26, z + 140, brick, b(Blocks.POLISHED_BLACKSTONE), brick);
        fill(l, x - 4, Y + 1, z + 60, x + 4, Y + 12, z + 60, b(Blocks.AIR));
        for (int zz = z + 64; zz < z + 140; zz += 8)
            for (int side = -1; side <= 1; side += 2) {
                for (int y = Y + 1; y < Y + 26; y++) set(l, x + side * 11, y, zz, pol);
                set(l, x + side * 10, Y + 14, zz, b(Blocks.SHROOMLIGHT));
            }
        for (int zz = z + 61; zz < z + 140; zz++) for (int xx = x - 2; xx <= x + 2; xx++) set(l, xx, Y, zz, b(Blocks.CRIMSON_NYLIUM));
        // throne room
        room(l, x - 24, Y, z + 140, x + 24, Y + 36, z + 186, brick, b(Blocks.POLISHED_BLACKSTONE_BRICKS), brick);
        fill(l, x - 5, Y + 1, z + 140, x + 5, Y + 14, z + 140, b(Blocks.AIR));
        fill(l, x - 10, Y + 1, z + 164, x + 10, Y + 4, z + 185, pol);
        for (int s = 0; s < 4; s++) fill(l, x - 6, Y + 1 + s, z + 160 + s, x + 6, Y + 1 + s, z + 160 + s, b(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS).setValue(StairBlock.FACING, Direction.SOUTH));
        fill(l, x - 3, Y + 5, z + 181, x + 3, Y + 18, z + 184, b(Blocks.CRYING_OBSIDIAN));
        for (int i = 0; i < 6; i++) {
            set(l, x - 20 + i * 8, Y + 30, z + 150, b(Blocks.SHROOMLIGHT));
            set(l, x - 20 + i * 8, Y + 30, z + 175, b(Blocks.SHROOMLIGHT));
        }
    }

    // ------------------------------------------------------------------ Jeju Island

    static void jeju(ServerLevel l, BlockPos o) {
        int x = o.getX(), z = o.getZ();
        int cz = z + 70;
        // sea
        fill(l, x - 80, Y - 3, z - 20, x + 80, Y - 3, z + 160, b(Blocks.SAND));
        fill(l, x - 80, Y - 2, z - 20, x + 80, Y - 1, z + 160, b(Blocks.WATER));
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int xx = x - 60; xx <= x + 60; xx++)
            for (int zz = cz - 70; zz <= cz + 70; zz++) {
                double d = Math.sqrt((xx - x) * (xx - x) + (zz - cz) * (zz - cz) * 0.8);
                double n = Math.sin(xx * 0.15) * 3 + Math.cos(zz * 0.12) * 3;
                if (d > 56 + n) continue;
                int h = (int) Math.max(0, Math.min(6, (56 + n - d) / 5));
                for (int y = Y - 2; y <= Y + h - 1; y++) l.setBlock(m.set(xx, y, zz), d > 50 + n ? b(Blocks.SAND) : b(Blocks.DIRT), FLAGS);
                l.setBlock(m.set(xx, Y + h, zz), d > 50 + n ? b(Blocks.SAND) : b(Blocks.GRASS_BLOCK), FLAGS);
                if (d < 48 && ((xx * 31 + zz * 17) & 63) == 0) {
                    for (int y = 1; y <= 4; y++) l.setBlock(m.set(xx, Y + h + y, zz), b(Blocks.JUNGLE_LOG), FLAGS);
                    l.setBlock(m.set(xx, Y + h + 5, zz), b(Blocks.JUNGLE_LEAVES).setValue(LeavesBlock.PERSISTENT, true), FLAGS);
                }
            }
        // the ant nest: a big mound with a chamber inside
        int r = 22;
        for (int xx = x - r; xx <= x + r; xx++)
            for (int zz = cz - r; zz <= cz + r; zz++)
                for (int y = 0; y <= 18; y++) {
                    double d = Math.sqrt((xx - x) * (xx - x) + (zz - cz) * (zz - cz) + (y * 1.3) * (y * 1.3));
                    if (d > r) continue;
                    boolean hollow = d < r - 4 && y >= 1;
                    l.setBlock(m.set(xx, Y + y, zz), hollow ? b(Blocks.AIR) : ((xx + zz + y) % 5 == 0 ? b(Blocks.MUD) : b(ModBlocks.HIVE_WALL.get())), FLAGS);
                }
        fill(l, x - r + 1, Y, cz - r + 1, x + r - 1, Y, cz + r - 1, b(Blocks.PACKED_MUD));
        // tunnel entrance from the south
        fill(l, x - 2, Y + 1, cz - r - 2, x + 2, Y + 5, cz - r + 6, b(Blocks.AIR));
        for (int i = 0; i < 10; i++) set(l, x - 14 + i * 3, Y + 12, cz - 8 + (i % 3) * 8, b(Blocks.PEARLESCENT_FROGLIGHT));
        // landing beach + portal
        fill(l, x - 6, Y, z - 4, x + 6, Y, z + 4, b(Blocks.SMOOTH_SANDSTONE));
        fill(l, x - 1, Y, z + 4, x + 1, Y, z + 22, b(Blocks.SPRUCE_PLANKS));
        for (int k = z + 6; k <= z + 22; k += 4) {
            set(l, x - 2, Y + 1, k, b(Blocks.SPRUCE_FENCE));
            set(l, x - 2, Y + 2, k, b(Blocks.LANTERN));
        }
        DungeonBuilder.exitPortal(l, new BlockPos(x, Y + 1, z - 4), DungeonTheme.ANT_NEST);
    }

    // ------------------------------------------------------------------ Penalty Zone

    static void penalty(ServerLevel l, BlockPos o) {
        int x = o.getX(), z = o.getZ();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int xx = x - 50; xx <= x + 50; xx++)
            for (int zz = z - 50; zz <= z + 50; zz++) {
                int h = (int) Math.round(Math.sin(xx * 0.11) * 1.6 + Math.cos(zz * 0.09 + xx * 0.04) * 1.6 + 1.6);
                boolean edge = Math.abs(xx - x) == 50 || Math.abs(zz - z) == 50;
                for (int y = Y - 2; y <= Y + h; y++) l.setBlock(m.set(xx, y, zz), y < Y ? b(Blocks.SANDSTONE) : b(Blocks.SAND), FLAGS);
                if (edge) for (int y = Y + h + 1; y <= Y + 10; y++) l.setBlock(m.set(xx, y, zz), b(Blocks.BARRIER), FLAGS);
                if (!edge && ((xx * 13 + zz * 7) & 127) == 0) {
                    for (int y = 1; y <= 3; y++) l.setBlock(m.set(xx, Y + h + y, zz), b(Blocks.CACTUS), FLAGS);
                }
            }
        fill(l, x - 2, Y + 5, z - 2, x + 2, Y + 5, z + 2, b(Blocks.SMOOTH_SANDSTONE));
        fill(l, x - 2, Y + 1, z - 2, x + 2, Y + 4, z + 2, b(Blocks.AIR));
    }

    /** Keeps a few giant centipedes hunting the punished player. */
    public static void penaltyWave(ServerLevel l, ServerPlayer p) {
        BlockPos o = origin("penalty_zone");
        if (p.distanceToSqr(o.getX(), Y, o.getZ()) > 80 * 80) return;
        int near = l.getEntitiesOfClass(SLMonster.class, p.getBoundingBox().inflate(40), m -> m.kind == MobKind.GIANT_CENTIPEDE).size();
        HunterData d = HunterCapability.get(p);
        int max = 3 + d.level / 10;
        for (int i = near; i < Math.min(max, near + 2); i++) {
            double a = l.random.nextDouble() * Math.PI * 2;
            int sx = (int) (p.getX() + Math.cos(a) * 16), sz = (int) (p.getZ() + Math.sin(a) * 16);
            if (Math.abs(sx - o.getX()) > 46 || Math.abs(sz - o.getZ()) > 46) continue;
            SLMonster m = spawn(l, MobKind.GIANT_CENTIPEDE, sx + 0.5, Y + 6, sz + 0.5, inst("penalty_zone"));
            if (m != null) m.setTarget(p);
        }
    }
}
