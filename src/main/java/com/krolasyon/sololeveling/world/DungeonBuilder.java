package com.krolasyon.sololeveling.world;

import com.krolasyon.sololeveling.entity.MobKind;
import com.krolasyon.sololeveling.entity.SLMonster;
import com.krolasyon.sololeveling.registry.ModBlocks;
import com.krolasyon.sololeveling.registry.ModItems;
import com.krolasyon.sololeveling.system.Rank;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/** Builds a dungeon instance (rooms + corridors) for a gate. */
public final class DungeonBuilder {
    public static final int Y0 = 80;
    static final int CELL = 26;

    public record Result(BlockPos spawn, BlockPos bossRoom, BlockPos exitPortal) {}

    record Room(int x0, int z0, int x1, int z1, int h, boolean boss, boolean start) {
        int cx() { return (x0 + x1) / 2; }

        int cz() { return (z0 + z1) / 2; }
    }

    private DungeonBuilder() {}

    public static Result build(ServerLevel l, BlockPos origin, DungeonTheme theme, Rank rank, boolean red, String instance, long seed) {
        RandomSource r = RandomSource.create(seed);
        int n = 4 + Math.min(5, rank.ordinal());
        // random walk over a grid of cells
        List<int[]> cells = new ArrayList<>();
        Set<Long> used = new HashSet<>();
        int cx = 0, cz = 0;
        cells.add(new int[]{0, 0});
        used.add(0L);
        int guard = 0;
        while (cells.size() < n && guard++ < 500) {
            int[] dirs = {0, 1, 2, 3};
            int d = dirs[r.nextInt(4)];
            int nx = cx + (d == 0 ? 1 : d == 1 ? -1 : 0), nz = cz + (d == 2 ? 1 : d == 3 ? -1 : 0);
            if (nx < 0 || nz < -2 || nz > 2) continue;
            long key = ((long) nx << 32) | (nz & 0xffffffffL);
            if (used.contains(key)) {
                cx = nx;
                cz = nz;
                continue;
            }
            used.add(key);
            cells.add(new int[]{nx, nz});
            cx = nx;
            cz = nz;
        }
        // rooms
        int bx = origin.getX(), bz = origin.getZ();
        int baseH = switch (theme.style) {
            case FOREST -> 13;
            case TEMPLE -> 11;
            case NEST -> 8;
            default -> 7;
        };
        List<Room> rooms = new ArrayList<>();
        for (int i = 0; i < cells.size(); i++) {
            int[] c = cells.get(i);
            boolean boss = i == cells.size() - 1, start = i == 0;
            int size = boss ? CELL - 3 : 13 + r.nextInt(6);
            int ox = bx + c[0] * CELL + (CELL - size) / 2, oz = bz + c[1] * CELL + (CELL - size) / 2;
            rooms.add(new Room(ox, oz, ox + size - 1, oz + size - 1, boss ? baseH + 5 : baseH + r.nextInt(3), boss, start));
        }
        // carve map
        Map<Long, Integer> carve = new HashMap<>();
        for (Room rm : rooms) carveRoom(carve, rm, theme.style, r);
        for (int i = 1; i < rooms.size(); i++) carveCorridor(carve, rooms.get(i - 1), rooms.get(i), theme.style == DungeonTheme.Style.FOREST ? 5 : 3);
        // pass 1: floor, air, ceiling and walls
        BlockState wall = theme.wall.get().defaultBlockState(), floor = theme.floor.get().defaultBlockState(), accent = theme.accent.get().defaultBlockState();
        BlockState ceil = theme.style == DungeonTheme.Style.FOREST ? theme.deco.get().defaultBlockState().trySetValue(LeavesBlock.PERSISTENT, true) : wall;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        Set<Long> wallCols = new HashSet<>();
        for (long key : carve.keySet()) {
            int x = (int) (key >> 32), z = (int) key;
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++) {
                    long k2 = pack(x + dx, z + dz);
                    if (!carve.containsKey(k2)) wallCols.add(k2);
                }
        }
        for (var e : carve.entrySet()) {
            int x = (int) (e.getKey() >> 32), z = (int) (long) e.getKey();
            int h = e.getValue();
            l.setBlock(m.set(x, Y0, z), (x * 7 + z * 13) % 11 == 0 ? accent : floor, Structures.FLAGS);
            l.setBlock(m.set(x, Y0 - 1, z), wall, Structures.FLAGS);
            for (int y = 1; y <= h; y++) l.setBlock(m.set(x, Y0 + y, z), Blocks.AIR.defaultBlockState(), Structures.FLAGS);
            l.setBlock(m.set(x, Y0 + h + 1, z), ceil, Structures.FLAGS);
            l.setBlock(m.set(x, Y0 + h + 2, z), wall, Structures.FLAGS);
        }
        for (long key : wallCols) {
            int x = (int) (key >> 32), z = (int) key;
            int h = 0;
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) h = Math.max(h, carve.getOrDefault(pack(x + dx, z + dz), 0));
            for (int y = -1; y <= h + 2; y++) {
                BlockState s = wall;
                if (theme.style != DungeonTheme.Style.CAVE && theme.style != DungeonTheme.Style.NEST && y == 3 && ((x + z) & 1) == 0) s = accent;
                if ((theme.style == DungeonTheme.Style.CAVE || theme.style == DungeonTheme.Style.NEST) && r.nextInt(5) == 0) s = accent;
                l.setBlock(m.set(x, Y0 + y, z), s, Structures.FLAGS);
            }
        }
        // pass 2: decoration, lights, loot and monsters
        BlockPos spawn = null, bossPos = null, exit = null;
        for (Room rm : rooms) {
            decorate(l, rm, theme, r, carve);
            if (rm.start) {
                spawn = new BlockPos(rm.cx(), Y0 + 1, rm.cz());
                exit = new BlockPos(rm.cx(), Y0 + 1, rm.z0 + 1);
                if (!red) exitPortal(l, exit, theme);
            } else if (rm.boss) {
                bossPos = new BlockPos(rm.cx(), Y0 + 1, rm.cz());
                spawnBoss(l, rm, theme, rank, instance, r);
            } else {
                populate(l, rm, theme, rank, instance, r);
                if (r.nextInt(3) == 0) chest(l, new BlockPos(rm.x1 - 1, Y0 + 1, rm.z1 - 1), rank, r);
            }
        }
        if (spawn == null) spawn = origin.above(Y0 + 1 - origin.getY());
        if (bossPos == null) bossPos = spawn;
        return new Result(spawn, bossPos, exit == null ? spawn : exit);
    }

    static long pack(int x, int z) { return ((long) x << 32) | (z & 0xffffffffL); }

    private static void carveRoom(Map<Long, Integer> carve, Room rm, DungeonTheme.Style style, RandomSource r) {
        double rx = (rm.x1 - rm.x0) / 2.0 + 0.5, rz = (rm.z1 - rm.z0) / 2.0 + 0.5;
        double cx = (rm.x0 + rm.x1) / 2.0, cz = (rm.z0 + rm.z1) / 2.0;
        for (int x = rm.x0; x <= rm.x1; x++)
            for (int z = rm.z0; z <= rm.z1; z++) {
                int h = rm.h;
                if (style == DungeonTheme.Style.CAVE || style == DungeonTheme.Style.NEST) {
                    double dx = (x - cx) / rx, dz = (z - cz) / rz;
                    double d = dx * dx + dz * dz;
                    double noise = Math.sin(x * 0.7 + z * 0.3) * 0.08 + Math.cos(z * 0.6 - x * 0.2) * 0.08;
                    if (d > 1 + noise) continue;
                    h = Math.max(4, (int) Math.round(rm.h * (1.15 - d * 0.55)));
                }
                carve.merge(pack(x, z), h, Math::max);
            }
    }

    private static void carveCorridor(Map<Long, Integer> carve, Room a, Room b, int half) {
        int x = a.cx(), z = a.cz();
        int tx = b.cx(), tz = b.cz();
        while (x != tx) {
            for (int w = -half / 2 - 1; w <= half / 2 + 1; w++) carve.merge(pack(x, z + w), 5, Math::max);
            x += Integer.signum(tx - x);
        }
        while (z != tz) {
            for (int w = -half / 2 - 1; w <= half / 2 + 1; w++) carve.merge(pack(x + w, z), 5, Math::max);
            z += Integer.signum(tz - z);
        }
    }

    private static void decorate(ServerLevel l, Room rm, DungeonTheme t, RandomSource r, Map<Long, Integer> carve) {
        BlockState light = t.light.get().defaultBlockState();
        BlockState accent = t.accent.get().defaultBlockState();
        BlockState deco = t.deco.get().defaultBlockState();
        int cx = rm.cx(), cz = rm.cz();
        switch (t.style) {
            case HALLS, TEMPLE -> {
                // pillars on a grid with lights on top
                for (int x = rm.x0 + 3; x <= rm.x1 - 3; x += 5)
                    for (int z = rm.z0 + 3; z <= rm.z1 - 3; z += 5) {
                        if (Math.abs(x - cx) <= 2 && Math.abs(z - cz) <= 2) continue;
                        Integer h = carve.get(pack(x, z));
                        if (h == null) continue;
                        BlockState pil = t == DungeonTheme.STATUE_HALL ? ModBlocks.TEMPLE_PILLAR.get().defaultBlockState() : accent;
                        for (int y = 1; y <= h; y++) Structures.set(l, x, Y0 + y, z, pil);
                        Structures.set(l, x + 1, Y0 + h - 1, z, light.getBlock() instanceof LanternBlock ? light.setValue(LanternBlock.HANGING, false) : light);
                    }
                // carpet / deco runner
                if (t.deco.get() instanceof CarpetBlock) for (int z = rm.z0 + 1; z <= rm.z1 - 1; z++) for (int x = cx - 1; x <= cx + 1; x++)
                    if (carve.containsKey(pack(x, z))) Structures.set(l, x, Y0 + 1, z, deco);
            }
            case FOREST -> {
                for (int i = 0; i < (rm.x1 - rm.x0) / 2; i++) {
                    int x = rm.x0 + 1 + r.nextInt(rm.x1 - rm.x0 - 1), z = rm.z0 + 1 + r.nextInt(rm.z1 - rm.z0 - 1);
                    if (Math.abs(x - cx) < 3 && Math.abs(z - cz) < 3) continue;
                    Integer h = carve.get(pack(x, z));
                    if (h == null) continue;
                    BlockState trunk = t.wall.get().defaultBlockState();
                    for (int y = 1; y <= h; y++) Structures.set(l, x, Y0 + y, z, trunk);
                    for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
                        if ((dx != 0 || dz != 0) && carve.containsKey(pack(x + dx, z + dz)))
                            Structures.set(l, x + dx, Y0 + h - r.nextInt(3), z + dz, deco.trySetValue(LeavesBlock.PERSISTENT, true));
                }
                for (int i = 0; i < 6; i++) {
                    int x = rm.x0 + 1 + r.nextInt(rm.x1 - rm.x0 - 1), z = rm.z0 + 1 + r.nextInt(rm.z1 - rm.z0 - 1);
                    Integer h = carve.get(pack(x, z));
                    if (h != null) Structures.set(l, x, Y0 + h, z, light);
                }
                if (t == DungeonTheme.ICE_FOREST) for (int i = 0; i < 30; i++) {
                    int x = rm.x0 + r.nextInt(rm.x1 - rm.x0 + 1), z = rm.z0 + r.nextInt(rm.z1 - rm.z0 + 1);
                    if (carve.containsKey(pack(x, z))) Structures.set(l, x, Y0 + 1, z, Blocks.SNOW.defaultBlockState());
                }
            }
            default -> {
                // caves and nests: stalagmites, glowing patches
                for (int i = 0; i < 8; i++) {
                    int x = rm.x0 + r.nextInt(rm.x1 - rm.x0 + 1), z = rm.z0 + r.nextInt(rm.z1 - rm.z0 + 1);
                    Integer h = carve.get(pack(x, z));
                    if (h == null || (Math.abs(x - cx) < 3 && Math.abs(z - cz) < 3)) continue;
                    int len = 1 + r.nextInt(3);
                    for (int y = 0; y < len; y++) Structures.set(l, x, Y0 + h - y, z, t.wall.get().defaultBlockState());
                }
                for (int i = 0; i < 5; i++) {
                    int x = rm.x0 + r.nextInt(rm.x1 - rm.x0 + 1), z = rm.z0 + r.nextInt(rm.z1 - rm.z0 + 1);
                    Integer h = carve.get(pack(x, z));
                    if (h == null) continue;
                    Structures.set(l, x, Y0 + h + 1, z, light.getBlock() instanceof LanternBlock ? Blocks.GLOWSTONE.defaultBlockState() : light);
                }
                for (int i = 0; i < 4; i++) {
                    int x = rm.x0 + r.nextInt(rm.x1 - rm.x0 + 1), z = rm.z0 + r.nextInt(rm.z1 - rm.z0 + 1);
                    if (carve.containsKey(pack(x, z))) Structures.set(l, x, Y0, z, ModBlocks.MANA_CRYSTAL_ORE.get().defaultBlockState());
                }
                if (t.light.get() instanceof LanternBlock) for (int i = 0; i < 4; i++) {
                    int x = rm.x0 + r.nextInt(rm.x1 - rm.x0 + 1), z = rm.z0 + r.nextInt(rm.z1 - rm.z0 + 1);
                    if (carve.containsKey(pack(x, z))) Structures.set(l, x, Y0 + 1, z, t.light.get().defaultBlockState());
                }
            }
        }
        // always some light in the middle of the ceiling
        Integer h = carve.get(pack(cx, cz));
        if (h != null) Structures.set(l, cx, Y0 + h + 1, cz, Blocks.SHROOMLIGHT.defaultBlockState());
    }

    private static void populate(ServerLevel l, Room rm, DungeonTheme t, Rank rank, String instance, RandomSource r) {
        int count = 2 + r.nextInt(2) + rank.ordinal() / 2;
        for (int i = 0; i < count; i++) {
            MobKind k = t.mobs.get(r.nextInt(t.mobs.size()));
            int x = rm.x0 + 2 + r.nextInt(Math.max(1, rm.x1 - rm.x0 - 3)), z = rm.z0 + 2 + r.nextInt(Math.max(1, rm.z1 - rm.z0 - 3));
            Structures.spawn(l, k, x + 0.5, Y0 + 1, z + 0.5, instance);
        }
    }

    private static void spawnBoss(ServerLevel l, Room rm, DungeonTheme t, Rank rank, String instance, RandomSource r) {
        // dais
        BlockState accent = t.accent.get().defaultBlockState();
        for (int x = rm.cx() - 3; x <= rm.cx() + 3; x++) for (int z = rm.cz() - 3; z <= rm.cz() + 3; z++) Structures.set(l, x, Y0, z, accent);
        SLMonster boss = Structures.spawn(l, t.boss, rm.cx() + 0.5, Y0 + 1, rm.cz() + 0.5, instance);
        if (boss != null) {
            if (t.eliteBoss) boss.makeElite(Component.translatable("sololeveling.elite." + t.id()), 3.5F + rank.ordinal() * 0.5F, 1.6F);
            else boss.instanceBoss = true;
        }
        for (int i = 0; i < 2 + rank.ordinal() / 2; i++) {
            MobKind k = t.mobs.get(r.nextInt(t.mobs.size()));
            double a = r.nextDouble() * Math.PI * 2;
            Structures.spawn(l, k, rm.cx() + Math.cos(a) * 6, Y0 + 1, rm.cz() + Math.sin(a) * 6, instance);
        }
        chest(l, new BlockPos(rm.x1 - 2, Y0 + 1, rm.z1 - 2), rank, r);
        chest(l, new BlockPos(rm.x0 + 2, Y0 + 1, rm.z1 - 2), rank, r);
    }

    public static void exitPortal(ServerLevel l, BlockPos at, DungeonTheme t) {
        BlockState frame = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        BlockState accent = ModBlocks.RUNE_BRICKS.get().defaultBlockState();
        for (int dx = -2; dx <= 2; dx++) for (int dy = 0; dy <= 4; dy++) {
            boolean edge = Math.abs(dx) == 2 || dy == 4;
            if (edge) Structures.set(l, at.getX() + dx, at.getY() + dy, at.getZ(), dy == 4 && dx == 0 ? accent : frame);
            else Structures.portal(l, new BlockPos(at.getX() + dx, at.getY() + dy, at.getZ()), "return");
        }
    }

    static void chest(ServerLevel l, BlockPos pos, Rank rank, RandomSource r) {
        l.setBlock(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.from2DDataValue(r.nextInt(4))), 3);
        if (!(l.getBlockEntity(pos) instanceof ChestBlockEntity c)) return;
        int slot = 0;
        c.setItem(slot++, new ItemStack(ModItems.magicStone(rank), 1 + r.nextInt(4)));
        c.setItem(slot++, new ItemStack(r.nextBoolean() ? ModItems.HEALING_POTION.get() : ModItems.MANA_POTION.get(), 1 + r.nextInt(3)));
        if (r.nextInt(3) == 0) c.setItem(slot++, new ItemStack(ModItems.RANDOM_BOX.get()));
        if (rank.ordinal() >= Rank.C.ordinal() && r.nextInt(4) == 0) c.setItem(slot++, new ItemStack(ModItems.GREATER_HEALING_POTION.get()));
        if (rank.ordinal() >= Rank.B.ordinal() && r.nextInt(6) == 0) c.setItem(slot++, new ItemStack(ModItems.INSTANT_DUNGEON_KEY.get()));
        if (r.nextInt(12) == 0) c.setItem(slot, new ItemStack(ModItems.RUNE_STONE_BLOODLUST.get()));
        for (int i = 0; i < 27; i++) {
            int j = r.nextInt(27);
            ItemStack a = c.getItem(i), b = c.getItem(j);
            c.setItem(i, b);
            c.setItem(j, a);
        }
    }
}
