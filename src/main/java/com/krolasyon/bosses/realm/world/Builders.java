package com.krolasyon.bosses.realm.world;

import com.krolasyon.bosses.realm.Faction;
import com.krolasyon.bosses.realm.Realm;
import com.krolasyon.bosses.realm.block.LordAltarBlock;
import com.krolasyon.bosses.realm.portal.PortalShape;
import com.krolasyon.bosses.realm.registry.RealmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;

/** Hand written structure builders shared by world generation and in-game events. */
public final class Builders {
    private Builders() {}

    static final int FLAGS = Block.UPDATE_CLIENTS;

    static void set(WorldGenLevel l, BlockPos p, BlockState s) { l.setBlock(p, s, FLAGS); }

    static BlockState b(Block b) { return b.defaultBlockState(); }

    /** fills from p downward until hitting solid ground (max depth) */
    static void pillarDown(WorldGenLevel l, BlockPos p, BlockState s, int max) {
        for (int i = 0; i < max; i++) {
            BlockPos q = p.below(i);
            BlockState cur = l.getBlockState(q);
            if (i > 0 && cur.isSolid() && cur.getFluidState().isEmpty()) break;
            set(l, q, s);
        }
    }

    static void chest(WorldGenLevel l, BlockPos p, RandomSource r, String table, Direction facing) {
        set(l, p, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing));
        RandomizableContainerBlockEntity.setLootTable(l, r, p, Realm.rl(table));
    }

    // ------------------------------------------------------------------ overworld portal ruin
    /** ruined crimson gate; returns bottom-left interior of the portal */
    public static BlockPos portalRuin(WorldGenLevel l, BlockPos origin, RandomSource r, boolean lit) {
        Direction.Axis axis = r.nextBoolean() ? Direction.Axis.X : Direction.Axis.Z;
        Direction right = PortalShape.right(axis);
        Direction side = right.getClockWise();
        BlockState frame = b(RealmBlocks.CURSED_OBSIDIAN.get());
        BlockState[] debris = {b(Blocks.BLACKSTONE), b(Blocks.POLISHED_BLACKSTONE_BRICKS), b(Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS),
                b(RealmBlocks.INFERNAL_BRICKS.get()), b(Blocks.NETHERRACK), b(Blocks.MAGMA_BLOCK), b(RealmBlocks.INFERNAL_STONE.get())};
        int y = origin.getY();
        BlockPos base = new BlockPos(origin.getX(), y, origin.getZ());
        // scorched platform
        for (int i = -5; i <= 6; i++)
            for (int j = -4; j <= 4; j++) {
                double d = Math.sqrt(Math.pow(i - 0.5, 2) * 0.7 + j * j);
                if (d > 5.2 + r.nextFloat() * 1.3) continue;
                BlockPos p = base.relative(right, i).relative(side, j);
                int top = l.getHeight(Heightmap.Types.WORLD_SURFACE_WG, p.getX(), p.getZ());
                BlockState s = debris[r.nextInt(debris.length)];
                if (d < 3.4) {
                    pillarDown(l, p.below(), s, 6);
                    for (int h = 0; h < 6; h++) set(l, p.above(h), b(Blocks.AIR));
                } else if (top <= y + 1) {
                    set(l, new BlockPos(p.getX(), top - 1, p.getZ()), r.nextInt(3) == 0 ? b(Blocks.NETHERRACK) : s);
                    if (r.nextInt(7) == 0 && top - 1 < y + 2) set(l, new BlockPos(p.getX(), top, p.getZ()), b(Blocks.FIRE));
                }
            }
        // broken pillars
        for (int k = 0; k < 4; k++) {
            int i = (k % 2 == 0) ? -4 : 5, j = k < 2 ? -3 : 3;
            BlockPos p = base.relative(right, i).relative(side, j);
            int h = 1 + r.nextInt(4);
            for (int q = 0; q < h; q++) set(l, p.above(q), q == h - 1 && r.nextBoolean() ? b(Blocks.CHISELED_POLISHED_BLACKSTONE) : b(Blocks.POLISHED_BLACKSTONE_BRICKS));
            if (r.nextBoolean()) set(l, p.above(h), b(Blocks.SOUL_LANTERN));
        }
        // the frame (always complete so it can be lit)
        for (int i = -1; i <= 2; i++) {
            set(l, base.relative(right, i).below(), frame);
            set(l, base.relative(right, i).above(3), frame);
        }
        for (int h = 0; h < 3; h++) {
            set(l, base.relative(right, -1).above(h), frame);
            set(l, base.relative(right, 2).above(h), frame);
        }
        // crown spikes on top
        set(l, base.relative(right, -1).above(4), b(Blocks.POLISHED_BLACKSTONE_WALL));
        set(l, base.relative(right, 2).above(4), b(Blocks.POLISHED_BLACKSTONE_WALL));
        set(l, base.relative(right, -1).above(5), b(RealmBlocks.INFERNAL_LANTERN.get()));
        set(l, base.relative(right, 2).above(5), b(RealmBlocks.INFERNAL_LANTERN.get()));
        if (lit) {
            BlockState portal = RealmBlocks.REALM_PORTAL.get().defaultBlockState().setValue(com.krolasyon.bosses.realm.block.RealmPortalBlock.AXIS, axis);
            for (int i = 0; i < 2; i++) for (int h = 0; h < 3; h++) l.setBlock(base.relative(right, i).above(h), portal, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
        chest(l, base.relative(right, 4).relative(side, -2), r, "chests/portal_ruin", side);
        return base;
    }

    // ------------------------------------------------------------------ kingdom keeps
    record Palette(BlockState wall, BlockState wall2, BlockState floor, BlockState accent, BlockState light, BlockState top) {}

    static Palette palette(Faction f) {
        return switch (f) {
            case ASH -> new Palette(b(RealmBlocks.INFERNAL_BRICKS.get()), b(Blocks.POLISHED_BLACKSTONE_BRICKS), b(Blocks.POLISHED_BLACKSTONE),
                    b(Blocks.MAGMA_BLOCK), b(RealmBlocks.INFERNAL_LANTERN.get()), b(RealmBlocks.CHISELED_INFERNAL_BRICKS.get()));
            case BLOOD -> new Palette(b(Blocks.RED_NETHER_BRICKS), b(RealmBlocks.BLOOD_PLANKS.get()), b(Blocks.NETHER_WART_BLOCK),
                    b(RealmBlocks.BLOOD_CAP.get()), b(Blocks.SHROOMLIGHT), b(Blocks.CHISELED_NETHER_BRICKS));
            case SHADOW -> new Palette(b(Blocks.OBSIDIAN), b(Blocks.CRYING_OBSIDIAN), b(Blocks.POLISHED_BLACKSTONE_BRICKS),
                    b(RealmBlocks.SHADOW_CRYSTAL.get()), b(RealmBlocks.SHADOW_CRYSTAL.get()), b(RealmBlocks.SHADOW_PLANKS.get()));
            case LEGION -> new Palette(b(Blocks.POLISHED_BASALT), b(Blocks.BLACKSTONE), b(Blocks.SMOOTH_BASALT),
                    b(Blocks.GILDED_BLACKSTONE), b(Blocks.LANTERN), b(Blocks.CHISELED_POLISHED_BLACKSTONE));
            case SOUL -> new Palette(b(Blocks.BONE_BLOCK), b(Blocks.POLISHED_BLACKSTONE), b(Blocks.SOUL_SOIL),
                    b(Blocks.SOUL_SAND), b(Blocks.SOUL_LANTERN), b(Blocks.CHISELED_POLISHED_BLACKSTONE));
        };
    }

    /** octagonal walled keep with towers, a raised dais and the lord altar (or the Crimson Throne when faction is null) */
    public static void keep(WorldGenLevel l, BlockPos c, RandomSource r, Faction f) {
        Palette p = f == null
                ? new Palette(b(RealmBlocks.INFERNAL_BRICKS.get()), b(RealmBlocks.CHISELED_INFERNAL_BRICKS.get()), b(Blocks.POLISHED_BLACKSTONE_BRICKS),
                b(Blocks.GOLD_BLOCK), b(RealmBlocks.INFERNAL_LANTERN.get()), b(Blocks.GILDED_BLACKSTONE))
                : palette(f);
        int R = 9;
        for (int dx = -R; dx <= R; dx++)
            for (int dz = -R; dz <= R; dz++) {
                int ax = Math.abs(dx), az = Math.abs(dz);
                if (ax + az > R + 5) continue;
                BlockPos fl = c.offset(dx, -1, dz);
                boolean edge = ax == R || az == R || ax + az == R + 5;
                pillarDown(l, fl, ((dx + dz) & 1) == 0 ? p.floor() : p.wall2(), 10);
                for (int h = 0; h < 9; h++) set(l, fl.above(h + 1), b(Blocks.AIR));
                if (edge) {
                    boolean gate = (ax <= 1 && az == R) || (az <= 1 && ax == R);
                    int height = gate ? 0 : 5;
                    for (int h = 1; h <= height; h++) set(l, fl.above(h), (h % 3 == 0) ? p.wall2() : p.wall());
                    if (!gate && ((dx + dz) & 1) == 0) set(l, fl.above(height + 1), p.top());
                    if (gate) {
                        set(l, fl.above(4), p.wall());
                        set(l, fl.above(5), p.top());
                    }
                }
            }
        // corner towers
        int[][] towers = {{-7, -7}, {7, -7}, {-7, 7}, {7, 7}};
        for (int[] t : towers) {
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++)
                    for (int h = 0; h <= 8; h++) {
                        BlockPos q = c.offset(t[0] + dx, h, t[1] + dz);
                        boolean core = dx == 0 && dz == 0;
                        set(l, q, h == 8 ? (core ? p.light() : p.top()) : core ? p.accent() : p.wall());
                    }
        }
        // dais
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++) set(l, c.offset(dx, 0, dz), Math.abs(dx) == 2 || Math.abs(dz) == 2 ? p.wall2() : p.top());
        for (int[] t : new int[][]{{-3, -3}, {3, -3}, {-3, 3}, {3, 3}}) {
            set(l, c.offset(t[0], 0, t[1]), p.wall());
            set(l, c.offset(t[0], 1, t[1]), p.wall());
            set(l, c.offset(t[0], 2, t[1]), p.light());
        }
        if (f != null) {
            set(l, c.above(), RealmBlocks.LORD_ALTAR.get().defaultBlockState().setValue(LordAltarBlock.FACTION, f.ordinal()));
        } else {
            set(l, c.above(), RealmBlocks.CRIMSON_THRONE.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
            for (int dx = -1; dx <= 1; dx += 2) set(l, c.offset(dx, 1, 0), b(Blocks.GOLD_BLOCK));
        }
        chest(l, c.offset(-5, 0, 6), r, "chests/kingdom_keep", Direction.NORTH);
        if (r.nextBoolean()) chest(l, c.offset(5, 0, -6), r, "chests/kingdom_keep", Direction.SOUTH);
    }

    // ------------------------------------------------------------------ small decorations
    public static void crystalSpire(WorldGenLevel l, BlockPos c, RandomSource r) {
        int h = 6 + r.nextInt(9);
        double lx = (r.nextDouble() - 0.5) * 0.5, lz = (r.nextDouble() - 0.5) * 0.5;
        for (int y = -2; y < h; y++) {
            double rad = 1.8 * (1 - (double) y / h) + 0.2;
            int cx = (int) Math.round(lx * y), cz = (int) Math.round(lz * y);
            for (int dx = -2; dx <= 2; dx++)
                for (int dz = -2; dz <= 2; dz++)
                    if (dx * dx + dz * dz <= rad * rad) {
                        set(l, c.offset(cx + dx, y, cz + dz), (dx == 0 && dz == 0) || r.nextInt(3) == 0 ? b(RealmBlocks.SHADOW_CRYSTAL.get()) : b(Blocks.OBSIDIAN));
                    }
        }
    }

    public static void boneRibs(WorldGenLevel l, BlockPos c, RandomSource r) {
        boolean alongX = r.nextBoolean();
        int len = 4 + r.nextInt(4);
        for (int k = 0; k < len; k++) {
            int span = 3 + (k == 0 || k == len - 1 ? 0 : 1);
            for (int s = -1; s <= 1; s += 2)
                for (int y = 0; y <= 6; y++) {
                    int off = (int) Math.round(span * Math.sin(Math.PI * 0.5 * (1 - y / 7.0)) + 0.4);
                    BlockPos q = alongX ? c.offset(k * 2, y, s * off) : c.offset(s * off, y, k * 2);
                    set(l, q, b(Blocks.BONE_BLOCK).setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y));
                }
            BlockPos spine = alongX ? c.offset(k * 2, 6, 0) : c.offset(0, 6, k * 2);
            set(l, spine, b(Blocks.BONE_BLOCK).setValue(RotatedPillarBlock.AXIS, alongX ? Direction.Axis.X : Direction.Axis.Z));
            if (k < len - 1) set(l, alongX ? spine.offset(1, 0, 0) : spine.offset(0, 0, 1),
                    b(Blocks.BONE_BLOCK).setValue(RotatedPillarBlock.AXIS, alongX ? Direction.Axis.X : Direction.Axis.Z));
        }
        if (r.nextInt(3) == 0) set(l, c.above(), b(Blocks.SOUL_LANTERN));
    }

    public static void warSpikes(WorldGenLevel l, BlockPos c, RandomSource r) {
        int n = 3 + r.nextInt(4);
        for (int i = 0; i < n; i++) {
            BlockPos q = c.offset(r.nextInt(7) - 3, 0, r.nextInt(7) - 3);
            q = new BlockPos(q.getX(), l.getHeight(Heightmap.Types.WORLD_SURFACE_WG, q.getX(), q.getZ()), q.getZ());
            int h = 2 + r.nextInt(5);
            for (int y = 0; y < h; y++) set(l, q.above(y), y == h - 1 ? b(Blocks.BLACKSTONE_WALL) : b(Blocks.POLISHED_BASALT));
            if (r.nextInt(3) == 0) set(l, q.above(h), b(Blocks.LANTERN));
            else if (r.nextInt(2) == 0) set(l, q.above(h), b(Blocks.CHAIN));
        }
        if (r.nextInt(4) == 0) set(l, c.below(), b(Blocks.MAGMA_BLOCK));
    }

    public static void bloodPool(WorldGenLevel l, BlockPos c, RandomSource r) {
        int rad = 2 + r.nextInt(3);
        for (int dx = -rad - 1; dx <= rad + 1; dx++)
            for (int dz = -rad - 1; dz <= rad + 1; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz) + r.nextDouble() * 0.6;
                BlockPos q = c.offset(dx, -1, dz);
                if (d <= rad) {
                    set(l, q, b(RealmBlocks.COAGULATED_BLOOD.get()));
                    set(l, q.below(), b(Blocks.NETHER_WART_BLOCK));
                    set(l, q.above(), b(Blocks.AIR));
                } else if (d <= rad + 1.2 && r.nextInt(3) == 0 && l.getBlockState(q.above()).isAir()) {
                    set(l, q.above(), b(RealmBlocks.BLOOD_THORN.get()));
                }
            }
    }

    public static void charredTree(WorldGenLevel l, BlockPos c, RandomSource r) {
        int h = 4 + r.nextInt(5);
        BlockState log = b(Blocks.BASALT);
        for (int y = 0; y < h; y++) set(l, c.above(y), y == h - 1 ? b(Blocks.MAGMA_BLOCK) : log);
        for (int k = 0; k < 2 + r.nextInt(3); k++) {
            Direction d = Direction.Plane.HORIZONTAL.getRandomDirection(r);
            int y = 2 + r.nextInt(Math.max(1, h - 2));
            int len = 1 + r.nextInt(3);
            for (int i = 1; i <= len; i++) set(l, c.above(y + i / 2).relative(d, i), log.setValue(RotatedPillarBlock.AXIS, d.getAxis()));
        }
        if (r.nextBoolean()) set(l, c.relative(Direction.Plane.HORIZONTAL.getRandomDirection(r)), b(RealmBlocks.EMBER_FLOWER.get()));
    }
}
