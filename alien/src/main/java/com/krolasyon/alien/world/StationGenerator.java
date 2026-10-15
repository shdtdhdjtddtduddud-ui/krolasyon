package com.krolasyon.alien.world;

import com.krolasyon.alien.block.CeilingLightBlock;
import com.krolasyon.alien.block.LockerBlock;
import com.krolasyon.alien.entity.XenomorphEntity;
import com.krolasyon.alien.registry.ModBlocks;
import com.krolasyon.alien.registry.ModEntities;
import com.krolasyon.alien.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;

import java.util.*;

/**
 * Builds a 4x4 grid of Sevastopol rooms around the player: hydraulic doors and open bulkheads, ceiling + wall vents,
 * lockers, emergency save phones, terminals, debris, flickering lights and supply chests. Then releases the alien
 * inside the vent network, as far away from the players as possible.
 */
public final class StationGenerator {
    private StationGenerator() {}

    private static final int N = 4;        // rooms per side
    private static final int CELL = 14;    // wall-to-wall stride (13 wide rooms)
    private static final int H = 6;        // ceiling height above the floor

    private enum Theme { LAB, MEDICAL, CREW, STORAGE, MAINTENANCE, OFFICE }

    private record Room(int gx, int gz, Theme theme) {}

    private static ServerLevel L;
    private static RandomSource R;

    private static void set(BlockPos p, BlockState s) { L.setBlock(p, s, 2); }

    private static BlockState st(net.minecraftforge.registries.RegistryObject<? extends Block> b) { return b.get().defaultBlockState(); }

    public static void generate(ServerLevel level, ServerPlayer player, long seed) {
        L = level;
        R = RandomSource.create(seed);
        BlockPos pp = player.blockPosition();
        int x0 = pp.getX() - CELL / 2, z0 = pp.getZ() - CELL / 2, y0 = pp.getY() - 1;
        int size = N * CELL;

        // remove older aliens so there is only ever one hunter
        for (XenomorphEntity x : level.getEntitiesOfClass(XenomorphEntity.class, new AABB(pp).inflate(256))) x.discard();

        Room[][] rooms = new Room[N][N];
        Theme[] themes = Theme.values();
        for (int i = 0; i < N; i++)
            for (int j = 0; j < N; j++)
                rooms[i][j] = new Room(i, j, (i == 0 && j == 0) ? Theme.CREW : themes[R.nextInt(themes.length)]);

        // ---------------------------------------------------------------- shell
        for (int x = 0; x <= size; x++) {
            for (int z = 0; z <= size; z++) {
                boolean wallX = x % CELL == 0, wallZ = z % CELL == 0;
                Room room = rooms[Math.min(N - 1, x / CELL)][Math.min(N - 1, z / CELL)];
                for (int y = -1; y <= H + 1; y++) {
                    BlockPos p = new BlockPos(x0 + x, y0 + y, z0 + z);
                    BlockState s;
                    if (y == -1) s = st(ModBlocks.STATION_TRIM);
                    else if (y == 0) s = floor(room, x, z);
                    else if (y == H) s = ceiling(room, x, z);
                    else if (y == H + 1) s = st(ModBlocks.STATION_CEILING);
                    else if (wallX || wallZ) s = wall(room, y, x, z);
                    else s = Blocks.AIR.defaultBlockState();
                    set(p, s);
                }
            }
        }

        // ---------------------------------------------------------------- connections (random spanning tree + loops)
        Set<String> edges = new HashSet<>();
        boolean[][] seen = new boolean[N][N];
        Deque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{0, 0});
        seen[0][0] = true;
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!stack.isEmpty()) {
            int[] c = stack.peek();
            List<int[]> next = new ArrayList<>();
            for (int[] d : dirs) {
                int nx = c[0] + d[0], nz = c[1] + d[1];
                if (nx >= 0 && nz >= 0 && nx < N && nz < N && !seen[nx][nz]) next.add(new int[]{nx, nz});
            }
            if (next.isEmpty()) { stack.pop(); continue; }
            int[] n = next.get(R.nextInt(next.size()));
            seen[n[0]][n[1]] = true;
            edges.add(key(c[0], c[1], n[0], n[1]));
            stack.push(n);
        }
        for (int k = 0; k < 6; k++) {
            int ax = R.nextInt(N), az = R.nextInt(N);
            int[] d = dirs[R.nextInt(4)];
            int bx = ax + d[0], bz = az + d[1];
            if (bx >= 0 && bz >= 0 && bx < N && bz < N) edges.add(key(ax, az, bx, bz));
        }
        for (String e : edges) {
            String[] s = e.split(",");
            opening(x0, y0, z0, Integer.parseInt(s[0]), Integer.parseInt(s[1]), Integer.parseInt(s[2]), Integer.parseInt(s[3]));
        }

        // ---------------------------------------------------------------- furnish every room
        List<BlockPos> ventPositions = new ArrayList<>();
        BlockPos startSave = null;
        int farX = N - 1, farZ = N - 1;
        int flameX = R.nextInt(N), flameZ = R.nextInt(N);
        if (flameX == 0 && flameZ == 0) { flameX = N - 1; flameZ = N / 2; }
        int saves = 0;
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                Room room = rooms[i][j];
                int rx = x0 + i * CELL, rz = z0 + j * CELL;
                boolean start = i == 0 && j == 0;
                lights(room, rx, y0, rz, start);
                ventPositions.addAll(vents(rx, y0, rz));
                lockers(rx, y0, rz, start ? 2 : 1 + R.nextInt(3));
                furniture(room, rx, y0, rz);
                debris(rx, y0, rz, room.theme() == Theme.MAINTENANCE || room.theme() == Theme.STORAGE ? 0.12F : 0.06F);
                if (start) {
                    startSave = saveStation(rx, y0, rz);
                    starterChest(rx, y0, rz);
                } else if ((saves < 3 && R.nextInt(4) == 0) || (i == N - 1 && j == N - 1)) {
                    saveStation(rx, y0, rz);
                    saves++;
                }
                if (i == flameX && j == flameZ) flameChest(rx, y0, rz);
                else if (room.theme() == Theme.STORAGE || R.nextInt(4) == 0) supplyChest(rx, y0, rz);
            }
        }

        // ---------------------------------------------------------------- players in, alien released far away
        List<ServerPlayer> crew = new ArrayList<>();
        crew.add(player);
        ServerPlayer other = null;
        double best = Double.MAX_VALUE;
        for (ServerPlayer p : level.players()) {
            if (p == player || p.isSpectator()) continue;
            double d = p.distanceToSqr(player);
            if (d < best) { best = d; other = p; }
        }
        if (other != null) crew.add(other); // up to two survivors

        BlockPos startCenter = new BlockPos(x0 + CELL / 2, y0 + 1, z0 + CELL / 2);
        for (int k = 0; k < crew.size(); k++) {
            ServerPlayer p = crew.get(k);
            p.teleportTo(level, startCenter.getX() + 0.5 + k * 1.5, startCenter.getY(), startCenter.getZ() + 0.5, 225F, 0F);
            if (startSave != null) p.setRespawnPosition(level.dimension(), startCenter, 225F, true, false);
            p.connection.send(new ClientboundSetTitlesAnimationPacket(20, 80, 30));
            p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("SEVASTOPOL").withStyle(ChatFormatting.WHITE)));
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("message.alienisolation.intro").withStyle(ChatFormatting.GRAY)));
            p.sendSystemMessage(Component.translatable("message.alienisolation.help").withStyle(ChatFormatting.DARK_GREEN));
        }

        XenomorphEntity alien = ModEntities.XENOMORPH.get().create(level);
        if (alien != null) {
            BlockPos farVent = null;
            double fd = -1;
            for (BlockPos v : ventPositions) {
                double d = v.distSqr(startCenter);
                if (d > fd) { fd = d; farVent = v; }
            }
            BlockPos spawn = farVent != null ? farVent : new BlockPos(x0 + farX * CELL + CELL / 2, y0 + 1, z0 + farZ * CELL + CELL / 2);
            alien.moveTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, 0F, 0F);
            level.addFreshEntity(alien);
            if (farVent != null) alien.startInVents(500 + R.nextInt(300));
        }
        L = null;
    }

    private static String key(int ax, int az, int bx, int bz) {
        if (ax > bx || (ax == bx && az > bz)) return bx + "," + bz + "," + ax + "," + az;
        return ax + "," + az + "," + bx + "," + bz;
    }

    // ------------------------------------------------------------------ surfaces
    private static BlockState floor(Room r, int x, int z) {
        int lx = x % CELL, lz = z % CELL;
        if (r.theme() == Theme.MAINTENANCE && (lx % 4 == 2 || lz % 4 == 2)) return st(ModBlocks.FLOOR_GRATE);
        if ((lx == 1 || lx == CELL - 1 || lz == 1 || lz == CELL - 1) && r.theme() != Theme.MEDICAL) return st(ModBlocks.FLOOR_GRATE);
        return st(ModBlocks.STATION_FLOOR);
    }

    private static BlockState ceiling(Room r, int x, int z) {
        return st(ModBlocks.STATION_CEILING);
    }

    private static BlockState wall(Room r, int y, int x, int z) {
        if (y == 1) return st(ModBlocks.STATION_WALL_DARK);
        if (y == H - 1) return st(ModBlocks.STATION_TRIM);
        int lx = x % CELL, lz = z % CELL;
        boolean corner = (x % CELL == 0) && (z % CELL == 0);
        if (corner) return st(ModBlocks.STATION_TRIM);
        int along = x % CELL == 0 ? lz : lx;
        if (along % 4 == 0) return st(ModBlocks.STATION_TRIM);
        return switch (r.theme()) {
            case LAB -> y == 3 ? st(ModBlocks.STATION_WALL_GREEN) : st(ModBlocks.STATION_WALL_DIRTY);
            case MEDICAL -> y == 3 ? st(ModBlocks.STATION_WALL_GREEN) : st(ModBlocks.STATION_WALL);
            case MAINTENANCE -> y == 2 && along % 4 == 2 ? st(ModBlocks.HAZARD_PANEL) : st(ModBlocks.STATION_WALL_DARK);
            case STORAGE -> y == 2 ? st(ModBlocks.HAZARD_PANEL) : st(ModBlocks.STATION_WALL_DIRTY);
            default -> st(ModBlocks.STATION_WALL);
        };
    }

    // ------------------------------------------------------------------ openings
    private static void opening(int x0, int y0, int z0, int ax, int az, int bx, int bz) {
        boolean alongX = ax != bx; // the shared wall is perpendicular to x
        int wx = alongX ? x0 + bx * CELL : x0 + ax * CELL + CELL / 2;
        int wz = alongX ? z0 + az * CELL + CELL / 2 : z0 + bz * CELL;
        boolean door = R.nextInt(5) < 3;
        if (door) {
            Block d = R.nextBoolean() ? ModBlocks.LAB_DOOR.get() : ModBlocks.SECURITY_DOOR.get();
            Direction facing = alongX ? Direction.EAST : Direction.SOUTH;
            for (int k = 0; k < 2; k++) {
                int x = alongX ? wx : wx - 1 + k, z = alongX ? wz - 1 + k : wz;
                DoorHingeSide hinge = k == 0 ? DoorHingeSide.LEFT : DoorHingeSide.RIGHT;
                BlockState low = d.defaultBlockState().setValue(DoorBlock.FACING, facing).setValue(DoorBlock.HINGE, hinge)
                        .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
                set(new BlockPos(x, y0 + 1, z), low);
                set(new BlockPos(x, y0 + 2, z), low.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
                set(new BlockPos(x, y0 + 3, z), st(ModBlocks.HAZARD_PANEL));
            }
            for (int k : new int[]{-2, 2}) {
                int x = alongX ? wx : wx + (k < 0 ? -2 : 1), z = alongX ? wz + (k < 0 ? -2 : 1) : wz;
                for (int y = 1; y <= 3; y++) set(new BlockPos(x, y0 + y, z), st(ModBlocks.STATION_TRIM));
            }
        } else {
            // open bulkhead, 3 wide 3 tall, framed
            for (int k = -1; k <= 1; k++) {
                int x = alongX ? wx : wx + k, z = alongX ? wz + k : wz;
                for (int y = 1; y <= 3; y++) set(new BlockPos(x, y0 + y, z), Blocks.AIR.defaultBlockState());
                set(new BlockPos(x, y0 + 4, z), st(ModBlocks.STATION_TRIM));
                set(new BlockPos(x, y0, z), st(ModBlocks.FLOOR_GRATE));
            }
            for (int k : new int[]{-2, 2}) {
                int x = alongX ? wx : wx + k, z = alongX ? wz + k : wz;
                for (int y = 1; y <= 4; y++) set(new BlockPos(x, y0 + y, z), st(ModBlocks.STATION_TRIM));
            }
        }
    }

    // ------------------------------------------------------------------ fixtures
    private static void lights(Room r, int rx, int y0, int rz, boolean start) {
        for (int x : new int[]{4, 10}) {
            for (int z : new int[]{3, 7, 11}) {
                BlockPos p = new BlockPos(rx + x, y0 + H, rz + z);
                boolean dead = !start && R.nextInt(7) == 0;
                boolean flicker = !start && !dead && R.nextInt(4) == 0;
                set(p, ModBlocks.CEILING_LIGHT.get().defaultBlockState()
                        .setValue(CeilingLightBlock.LIT, !dead).setValue(CeilingLightBlock.FLICKER, flicker));
                if (flicker) L.scheduleTick(p, ModBlocks.CEILING_LIGHT.get(), 20 + R.nextInt(80));
            }
        }
        if (r.theme() == Theme.MAINTENANCE || r.theme() == Theme.STORAGE) {
            set(new BlockPos(rx + 2, y0 + H, rz + 2), st(ModBlocks.EMERGENCY_LIGHT));
            set(new BlockPos(rx + CELL - 2, y0 + H, rz + CELL - 2), st(ModBlocks.EMERGENCY_LIGHT));
        }
    }

    private static List<BlockPos> vents(int rx, int y0, int rz) {
        List<BlockPos> out = new ArrayList<>();
        for (int tries = 0; tries < 10; tries++) {
            int vx = 2 + R.nextInt(CELL - 4), vz = 2 + R.nextInt(CELL - 4);
            BlockPos c = new BlockPos(rx + vx, y0 + H, rz + vz);
            if (!L.getBlockState(c).is(ModBlocks.STATION_CEILING.get())) continue;
            set(c, st(ModBlocks.VENT));
            out.add(c);
            break;
        }
        if (R.nextInt(5) < 2) {
            // low wall duct on one side of the room
            boolean side = R.nextBoolean();
            int along = 3 + R.nextInt(CELL - 6);
            BlockPos w = side ? new BlockPos(rx, y0 + 1, rz + along) : new BlockPos(rx + along, y0 + 1, rz);
            if (L.getBlockState(w).is(ModBlocks.STATION_WALL_DARK.get())) {
                set(w, st(ModBlocks.VENT));
                out.add(w);
            }
        }
        return out;
    }

    /** spot along a wall, inside the room, with its facing pointing into the room. */
    private static Object[] wallSpot(int rx, int y0, int rz) {
        int side = R.nextInt(4);
        int along = 2 + R.nextInt(CELL - 4);
        return switch (side) {
            case 0 -> new Object[]{new BlockPos(rx + 1, y0 + 1, rz + along), Direction.EAST};
            case 1 -> new Object[]{new BlockPos(rx + CELL - 1, y0 + 1, rz + along), Direction.WEST};
            case 2 -> new Object[]{new BlockPos(rx + along, y0 + 1, rz + 1), Direction.SOUTH};
            default -> new Object[]{new BlockPos(rx + along, y0 + 1, rz + CELL - 1), Direction.NORTH};
        };
    }

    /** p and the blocks above are empty and the wall behind is solid (never blocks a doorway or bulkhead). */
    private static boolean free(BlockPos p, int height, Direction facing) {
        for (int i = 0; i < height; i++) if (!L.getBlockState(p.above(i)).isAir()) return false;
        for (int i = 0; i < Math.max(2, height); i++) {
            BlockState w = L.getBlockState(p.above(i).relative(facing.getOpposite()));
            if (w.isAir() || w.getBlock() instanceof DoorBlock) return false;
        }
        for (Direction d : new Direction[]{facing.getClockWise(), facing.getCounterClockWise()}) {
            BlockState w = L.getBlockState(p.relative(d).relative(facing.getOpposite()));
            if (w.getBlock() instanceof DoorBlock || w.isAir()) return false;
        }
        return true;
    }

    private static void lockers(int rx, int y0, int rz, int count) {
        for (int k = 0, tries = 0; k < count && tries < 20; tries++) {
            Object[] s = wallSpot(rx, y0, rz);
            BlockPos p = (BlockPos) s[0];
            Direction f = (Direction) s[1];
            if (!free(p, 3, f)) continue;
            BlockState low = ModBlocks.LOCKER.get().defaultBlockState().setValue(LockerBlock.FACING, f);
            set(p, low);
            set(p.above(), low.setValue(LockerBlock.HALF, DoubleBlockHalf.UPPER));
            // lockers come in pairs
            BlockPos q = p.relative(f.getClockWise());
            if (R.nextBoolean() && free(q, 3, f)) {
                set(q, low);
                set(q.above(), low.setValue(LockerBlock.HALF, DoubleBlockHalf.UPPER));
            }
            k++;
        }
    }

    private static BlockPos saveStation(int rx, int y0, int rz) {
        for (int tries = 0; tries < 20; tries++) {
            Object[] s = wallSpot(rx, y0, rz);
            BlockPos p = ((BlockPos) s[0]).above();
            Direction f = (Direction) s[1];
            if (!free(p.below(), 3, f)) continue;
            set(p, ModBlocks.SAVE_STATION.get().defaultBlockState().setValue(com.krolasyon.alien.block.SaveStationBlock.FACING, f));
            return p;
        }
        return null;
    }

    private static void furniture(Room r, int rx, int y0, int rz) {
        int desks = switch (r.theme()) {
            case OFFICE -> 6;
            case LAB, CREW -> 4;
            case MEDICAL -> 3;
            default -> 1;
        };
        for (int k = 0; k < desks; k++) {
            int x = 3 + R.nextInt(CELL - 6), z = 3 + R.nextInt(CELL - 6);
            BlockPos p = new BlockPos(rx + x, y0 + 1, rz + z);
            if (!L.getBlockState(p).isAir() || !L.getBlockState(p.above()).isAir()) continue;
            set(p, st(ModBlocks.DESK));
            if ((r.theme() == Theme.OFFICE || r.theme() == Theme.LAB || R.nextInt(3) == 0) && R.nextBoolean()) {
                Direction f = Direction.Plane.HORIZONTAL.getRandomDirection(R);
                set(p.above(), ModBlocks.TERMINAL.get().defaultBlockState().setValue(com.krolasyon.alien.block.TerminalBlock.FACING, f));
            }
            // desks are pushed together in rows
            BlockPos q = p.relative(R.nextBoolean() ? Direction.EAST : Direction.SOUTH);
            if (L.getBlockState(q).isAir() && L.getBlockState(q.above()).isAir() && R.nextBoolean()) set(q, st(ModBlocks.DESK));
        }
        if (r.theme() == Theme.STORAGE || r.theme() == Theme.MAINTENANCE) {
            for (int k = 0; k < 5; k++) {
                int x = 2 + R.nextInt(CELL - 4), z = 2 + R.nextInt(CELL - 4);
                BlockPos p = new BlockPos(rx + x, y0 + 1, rz + z);
                if (!L.getBlockState(p).isAir()) continue;
                set(p, R.nextBoolean() ? Blocks.BARREL.defaultBlockState() : st(ModBlocks.STATION_TRIM));
                if (R.nextInt(3) == 0 && L.getBlockState(p.above()).isAir()) set(p.above(), Blocks.BARREL.defaultBlockState());
            }
        }
    }

    private static void debris(int rx, int y0, int rz, float chance) {
        for (int x = 1; x < CELL; x++) {
            for (int z = 1; z < CELL; z++) {
                if (R.nextFloat() >= chance) continue;
                BlockPos p = new BlockPos(rx + x, y0 + 1, rz + z);
                if (L.getBlockState(p).isAir() && !L.getBlockState(p.below()).isAir()) set(p, st(ModBlocks.DEBRIS));
            }
        }
    }

    // ------------------------------------------------------------------ chests
    private static ChestBlockEntity chest(int rx, int y0, int rz) {
        for (int tries = 0; tries < 20; tries++) {
            Object[] s = wallSpot(rx, y0, rz);
            BlockPos p = (BlockPos) s[0];
            if (!free(p, 2, (Direction) s[1])) continue;
            set(p, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, (Direction) s[1]));
            BlockEntity be = L.getBlockEntity(p);
            if (be instanceof ChestBlockEntity c) return c;
        }
        return null;
    }

    private static void fill(ChestBlockEntity c, ItemStack... items) {
        if (c == null) return;
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < c.getContainerSize(); i++) slots.add(i);
        Collections.shuffle(slots, new Random(R.nextLong()));
        for (int i = 0; i < items.length && i < slots.size(); i++) c.setItem(slots.get(i), items[i]);
    }

    private static void starterChest(int rx, int y0, int rz) {
        fill(chest(rx, y0, rz), new ItemStack(ModItems.MOTION_TRACKER.get()), new ItemStack(ModItems.MOTION_TRACKER.get()),
                new ItemStack(ModItems.FLASHLIGHT.get()), new ItemStack(ModItems.FLASHLIGHT.get()),
                new ItemStack(ModItems.NOISEMAKER.get(), 3), new ItemStack(ModItems.FLARE.get(), 3), new ItemStack(ModItems.MEDKIT.get(), 2));
    }

    private static void flameChest(int rx, int y0, int rz) {
        fill(chest(rx, y0, rz), new ItemStack(ModItems.FLAMETHROWER.get()), new ItemStack(ModItems.FUEL.get(), 2),
                new ItemStack(ModItems.FLARE.get(), 1));
    }

    private static void supplyChest(int rx, int y0, int rz) {
        List<ItemStack> l = new ArrayList<>();
        if (R.nextBoolean()) l.add(new ItemStack(ModItems.NOISEMAKER.get(), 1 + R.nextInt(2)));
        if (R.nextBoolean()) l.add(new ItemStack(ModItems.FLARE.get(), 1 + R.nextInt(2)));
        if (R.nextInt(3) == 0) l.add(new ItemStack(ModItems.MEDKIT.get()));
        if (R.nextInt(3) == 0) l.add(new ItemStack(ModItems.FUEL.get()));
        if (l.isEmpty()) return;
        fill(chest(rx, y0, rz), l.toArray(new ItemStack[0]));
    }
}
