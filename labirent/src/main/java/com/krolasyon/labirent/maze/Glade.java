package com.krolasyon.labirent.maze;

import com.krolasyon.labirent.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

/** Fixed layout of the Glade: the Box in the middle, Homestead, kitchen, gardens, Blood House, Deadheads, Map Room. */
public final class Glade {
    private Glade() {}

    public static final int Y = MazeLayout.GROUND + 1;          // first air block above the grass
    public static final int MIN = -29, MAX = 28;                 // interior block bounds
    // the Box: 4x4 pit, floor at Y-6
    public static final int BOX_MIN = -2, BOX_MAX = 1;
    public static final int BOX_FLOOR = MazeLayout.GROUND - 5;
    public static final int SHAFT_BOTTOM = 40;
    public static final BlockPos SPAWN = new BlockPos(-6, Y, 3);
    public static final BlockPos KITCHEN_CHEST = new BlockPos(10, Y, -8);
    public static final BlockPos KITCHEN_FIRE = new BlockPos(14, Y, -8);
    public static final BlockPos MED_CHEST = new BlockPos(21, Y + 5, -13);
    public static final BlockPos SLICER_CHEST = new BlockPos(26, Y, 26);
    public static final BlockPos GARDEN_CHEST = new BlockPos(8, Y, 8);
    public static final BlockPos MAP_TABLE = new BlockPos(-19, Y, -19);
    public static final BlockPos GATHER = new BlockPos(-1, Y, 6);
    public static final BlockPos HOMESTEAD_DOOR = new BlockPos(8, Y, -18);
    public static final BlockPos[] BEDS = {
            new BlockPos(11, Y, -23), new BlockPos(13, Y, -23), new BlockPos(15, Y, -23), new BlockPos(17, Y, -23),
            new BlockPos(19, Y, -23), new BlockPos(21, Y, -23), new BlockPos(18, Y + 5, -23), new BlockPos(20, Y + 5, -23),
            new BlockPos(-26, Y, -9), new BlockPos(-24, Y, -9), new BlockPos(-22, Y, -9), new BlockPos(-20, Y, -9),
            new BlockPos(-18, Y, -9), new BlockPos(-16, Y, -9)};

    /** a point just inside each door, facing the maze */
    public static BlockPos doorInside(int door) {
        return switch (door) {
            case MazeLayout.DOOR_N -> new BlockPos(0, Y, MIN + 3);
            case MazeLayout.DOOR_S -> new BlockPos(0, Y, MAX - 3);
            case MazeLayout.DOOR_W -> new BlockPos(MIN + 3, Y, 0);
            default -> new BlockPos(MAX - 3, Y, 0);
        };
    }

    public static boolean inside(double x, double z) { return x >= MIN && x < MAX + 1 && z >= MIN && z < MAX + 1; }

    public static boolean inside(BlockPos p) { return inside(p.getX() + 0.5, p.getZ() + 0.5); }

    // ------------------------------------------------------------------ build
    public static void enqueue(MazeBuilder b) {
        b.add(MazeBuilder.box(MIN, MazeLayout.GROUND - 7, MIN, MAX, MazeLayout.GROUND, MAX, (l, p) -> {
            int y = p.getY();
            BlockState s;
            if (y == MazeLayout.GROUND) s = Blocks.GRASS_BLOCK.defaultBlockState();
            else if (y >= MazeLayout.GROUND - 3) s = Blocks.DIRT.defaultBlockState();
            else s = MazeBuilder.subState(p.getX(), y, p.getZ());
            l.setBlock(p, s, MazeBuilder.FLAGS);
        }));
        b.add(MazeBuilder.box(MIN, Y, MIN, MAX, Y + 12, MAX, (l, p) -> l.setBlock(p, Blocks.AIR.defaultBlockState(), MazeBuilder.FLAGS)));
        b.then(Glade::paths);
        b.then(Glade::box);
        b.then(Glade::homestead);
        b.then(Glade::kitchen);
        b.then(Glade::gardens);
        b.then(Glade::bloodHouse);
        b.then(Glade::deadheads);
        b.then(Glade::mapRoom);
        b.then(Glade::sleepingArea);
        b.then(Glade::lamps);
    }

    private static void paths(ServerLevel l) {
        Sculpt s = new Sculpt(l);
        RandomSource r = RandomSource.create(5);
        int g = MazeLayout.GROUND;
        for (int a = MIN; a <= MAX; a++)
            for (int w = -3; w <= 2; w++) {
                int flare = Math.max(0, Math.max(a - (MAX - 6), (MIN + 6) - a)) * 2;
                for (int k = w - (w < 0 ? flare : 0); k <= w + (w > 0 ? flare : 0); k++) {
                    BlockState st = r.nextFloat() < 0.75F ? Blocks.DIRT_PATH.defaultBlockState()
                            : r.nextBoolean() ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.GRAVEL.defaultBlockState();
                    if (Math.abs(k) <= 12) {
                        s.set(k, g, a, st);
                        s.set(a, g, k, st);
                    }
                }
            }
        // stone collar around the Box
        for (int x = -6; x <= 5; x++)
            for (int z = -6; z <= 5; z++)
                s.set(x, g, z, r.nextFloat() < 0.6F ? Blocks.STONE_BRICKS.defaultBlockState() : Blocks.CRACKED_STONE_BRICKS.defaultBlockState());
    }

    /** the Box: a caged pit in the middle with a shaft going down into the dark */
    public static void box(ServerLevel l) {
        Sculpt s = new Sculpt(l);
        int g = MazeLayout.GROUND;
        int a = BOX_MIN - 1, c = BOX_MAX + 1;
        s.fill(a, SHAFT_BOTTOM - 1, a, c, g, c, Blocks.POLISHED_ANDESITE);
        s.fill(BOX_MIN, SHAFT_BOTTOM, BOX_MIN, BOX_MAX, g, BOX_MAX, Blocks.AIR);
        for (int y = SHAFT_BOTTOM; y < BOX_FLOOR; y += 4) {
            s.set(a, y, -1, Blocks.IRON_BARS);
            s.set(c, y + 2, 0, Blocks.IRON_BARS);
        }
        s.fill(BOX_MIN, SHAFT_BOTTOM - 1, BOX_MIN, BOX_MAX, SHAFT_BOTTOM - 1, BOX_MAX, ModBlocks.BOX_FLOOR.get());
        s.fill(BOX_MIN, BOX_FLOOR, BOX_MIN, BOX_MAX, BOX_FLOOR, BOX_MAX, ModBlocks.BOX_FLOOR.get());
        // iron-barred inner walls of the Box
        for (int y = BOX_FLOOR + 1; y < g; y++) {
            for (int k = BOX_MIN; k <= BOX_MAX; k++) {
                s.set(k, y, a, Blocks.IRON_BLOCK);
                s.set(k, y, c, Blocks.IRON_BLOCK);
                s.set(a, y, k, Blocks.IRON_BLOCK);
                s.set(c, y, k, Blocks.IRON_BLOCK);
            }
        }
        for (int x = BOX_MIN; x <= BOX_MAX; x++) s.set(x, BOX_FLOOR + 1, c, Blocks.IRON_BARS);
        // ladder on the north side
        for (int y = BOX_FLOOR + 1; y < g; y++)
            s.set(-1, y, BOX_MIN, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH));
        closeBoxDoors(l);
        s.finish();
    }

    public static void closeBoxDoors(ServerLevel l) {
        Sculpt s = new Sculpt(l);
        for (int x = BOX_MIN; x <= BOX_MAX; x++)
            for (int z = BOX_MIN; z <= BOX_MAX; z++)
                s.set(x, MazeLayout.GROUND, z, Blocks.SPRUCE_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.HALF, Half.TOP)
                        .setValue(TrapDoorBlock.FACING, x < 0 ? Direction.EAST : Direction.WEST).setValue(TrapDoorBlock.OPEN, false));
    }

    public static void openBoxDoors(ServerLevel l) {
        Sculpt s = new Sculpt(l);
        for (int x = BOX_MIN; x <= BOX_MAX; x++)
            for (int z = BOX_MIN; z <= BOX_MAX; z++)
                s.set(x, MazeLayout.GROUND, z, Blocks.SPRUCE_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.HALF, Half.TOP)
                        .setValue(TrapDoorBlock.FACING, x < 0 ? Direction.EAST : Direction.WEST).setValue(TrapDoorBlock.OPEN, true));
    }

    public static void setBoxFloor(ServerLevel l, boolean present) {
        Sculpt s = new Sculpt(l);
        s.fill(BOX_MIN, BOX_FLOOR, BOX_MIN, BOX_MAX, BOX_FLOOR, BOX_MAX, present ? ModBlocks.BOX_FLOOR.get() : Blocks.AIR);
    }

    private static void homestead(ServerLevel l) {
        Sculpt s = new Sculpt(l);
        int x0 = 9, x1 = 23, z0 = -24, z1 = -12, y = Y;
        s.fill(x0, y - 1, z0, x1, y - 1, z1, Blocks.COBBLESTONE);
        s.walls(x0, y, z0, x1, y + 8, z1, Blocks.SPRUCE_PLANKS);
        s.air(x0 + 1, y, z0 + 1, x1 - 1, y + 8, z1 - 1);
        for (int x = x0; x <= x1; x += 7)
            for (int z = z0; z <= z1; z += 6) s.fill(x, y, z, x, y + 8, z, Blocks.STRIPPED_OAK_LOG);
        s.fill(x0, y + 4, z0, x1, y + 4, z1, Blocks.STRIPPED_OAK_LOG.defaultBlockState()
                .setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        s.fill(x0 + 1, y + 4, z0 + 1, x1 - 1, y + 4, z1 - 1, Blocks.SPRUCE_PLANKS);
        // windows
        for (int x = x0 + 2; x < x1; x += 3) {
            for (int dy : new int[]{1, 6}) {
                s.fill(x, y + dy, z0, x + 1, y + dy + 1, z0, Blocks.GLASS_PANE);
                s.fill(x, y + dy, z1, x + 1, y + dy + 1, z1, Blocks.GLASS_PANE);
            }
        }
        s.fill(x1, y + 1, z0 + 3, x1, y + 2, z0 + 4, Blocks.GLASS_PANE);
        s.fill(x1, y + 6, z0 + 3, x1, y + 7, z1 - 3, Blocks.GLASS_PANE);
        // door + porch
        s.door(x0, y, -18, Blocks.SPRUCE_DOOR, Direction.EAST);
        s.fill(x0 - 3, y - 1, -21, x0 - 1, y - 1, -15, Blocks.SPRUCE_PLANKS);
        s.fill(x0 - 3, y + 3, -21, x0 - 1, y + 3, -15, s.slab(Blocks.SPRUCE_SLAB, false));
        s.fill(x0 - 3, y, -21, x0 - 3, y + 2, -21, Blocks.SPRUCE_FENCE);
        s.fill(x0 - 3, y, -15, x0 - 3, y + 2, -15, Blocks.SPRUCE_FENCE);
        // gable roof along x, ridge at z = -18
        int ridge = -18;
        for (int k = 0; k <= 7; k++) {
            int ry = y + 9 + k;
            int zn = z0 - 1 + k, zs = z1 + 1 - k;
            if (zn > ridge) break;
            s.fill(x0 - 1, ry, zn, x1 + 1, ry, zn, s.stair(Blocks.DARK_OAK_STAIRS, Direction.SOUTH, false));
            if (zs > ridge) s.fill(x0 - 1, ry, zs, x1 + 1, ry, zs, s.stair(Blocks.DARK_OAK_STAIRS, Direction.NORTH, false));
            else s.fill(x0 - 1, ry, zs, x1 + 1, ry, zs, s.slab(Blocks.DARK_OAK_SLAB, false));
            if (zn + 1 <= zs - 1) {
                s.fill(x0, ry, zn + 1, x0, ry, zs - 1, Blocks.SPRUCE_PLANKS);
                s.fill(x1, ry, zn + 1, x1, ry, zs - 1, Blocks.SPRUCE_PLANKS);
            }
        }
        // ground floor: dormitory
        for (BlockPos b : BEDS) if (b.getY() == y && b.getX() > 0) s.bed(b.getX(), y, b.getZ(), Blocks.GREEN_BED, Direction.NORTH);
        s.fill(x0 + 1, y, z1 - 1, x0 + 2, y, z1 - 1, Blocks.BARREL);
        s.set(x1 - 1, y, z1 - 1, Blocks.CRAFTING_TABLE);
        s.set(x1 - 2, y, z1 - 1, Blocks.CHEST);
        s.set(x0 + 4, y + 3, z1 - 1, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        s.set(x1 - 4, y + 3, z1 - 1, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        s.set(16, y + 3, -20, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        // stairs hole + ladder to the med-jack floor
        s.air(x1 - 1, y + 4, z1 - 3, x1 - 1, y + 4, z1 - 3);
        for (int k = 0; k < 5; k++)
            s.set(x1 - 1, y + k, z1 - 3, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.WEST));
        // upper floor: med-jacks
        for (BlockPos b : BEDS) if (b.getY() == y + 5) s.bed(b.getX(), b.getY(), b.getZ(), Blocks.WHITE_BED, Direction.NORTH);
        s.set(MED_CHEST.getX(), MED_CHEST.getY(), MED_CHEST.getZ(), Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH));
        s.set(MED_CHEST.getX() - 1, MED_CHEST.getY(), MED_CHEST.getZ(), Blocks.BREWING_STAND);
        s.set(x0 + 2, y + 5, z1 - 1, Blocks.CAULDRON);
        s.set(12, y + 5, -14, Blocks.LANTERN);
        s.finish();
    }

    private static void kitchen(ServerLevel l) {
        Sculpt s = new Sculpt(l);
        int y = Y;
        s.fill(9, y - 1, -10, 18, y - 1, -5, Blocks.STONE_BRICKS);
        for (int[] c : new int[][]{{9, -10}, {18, -10}, {9, -5}, {18, -5}}) s.fill(c[0], y, c[1], c[0], y + 3, c[1], Blocks.SPRUCE_LOG);
        s.fill(8, y + 4, -11, 19, y + 4, -4, s.slab(Blocks.SPRUCE_SLAB, false));
        s.set(10, y, -10, Blocks.SMOKER.defaultBlockState().setValue(SmokerBlock.FACING, Direction.SOUTH));
        s.set(11, y, -10, Blocks.SMOKER.defaultBlockState().setValue(SmokerBlock.FACING, Direction.SOUTH));
        s.set(12, y, -10, Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, Direction.SOUTH));
        s.set(13, y, -10, Blocks.CRAFTING_TABLE);
        s.set(KITCHEN_FIRE.getX(), KITCHEN_FIRE.getY() - 1, KITCHEN_FIRE.getZ(), Blocks.STONE_BRICKS);
        s.set(KITCHEN_FIRE.getX(), KITCHEN_FIRE.getY(), KITCHEN_FIRE.getZ(), Blocks.CAMPFIRE);
        s.set(KITCHEN_CHEST.getX(), KITCHEN_CHEST.getY(), KITCHEN_CHEST.getZ(), Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH));
        s.set(KITCHEN_CHEST.getX(), KITCHEN_CHEST.getY(), KITCHEN_CHEST.getZ() + 1, Blocks.BARREL);
        // long eating table
        for (int x = 12; x <= 17; x++) {
            s.set(x, y, -6, Blocks.SPRUCE_FENCE);
            s.set(x, y + 1, -6, Blocks.SPRUCE_PRESSURE_PLATE);
        }
        s.fill(12, y, -5, 17, y, -5, s.stair(Blocks.SPRUCE_STAIRS, Direction.SOUTH, false));
        s.set(16, y, -10, Blocks.CAULDRON);
        s.set(17, y, -10, Blocks.BARREL);
        s.set(13, y + 3, -7, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        s.finish();
    }

    private static void gardens(ServerLevel l) {
        Sculpt s = new Sculpt(l);
        RandomSource r = RandomSource.create(17);
        int g = MazeLayout.GROUND;
        Block[] crops = {Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES, Blocks.BEETROOTS, Blocks.WHEAT};
        for (int x = 7; x <= 27; x++)
            for (int z = 7; z <= 18; z++) {
                if (x == 11 || x == 17 || x == 23) { s.set(x, g, z, Blocks.WATER); continue; }
                if (z == 12) { s.set(x, g, z, Blocks.DIRT_PATH); continue; }
                s.set(x, g, z, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7));
                Block c = crops[((x - 7) / 4 + (z > 12 ? 2 : 0)) % crops.length];
                BlockState cs = c.defaultBlockState();
                if (c instanceof CropBlock cb) cs = cb.getStateForAge(r.nextInt(cb.getMaxAge() + 1));
                s.set(x, g + 1, z, cs);
            }
        s.walls(6, Y, 6, 28, Y, 19, Blocks.OAK_FENCE.defaultBlockState());
        s.set(6, Y, 12, Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, Direction.EAST));
        s.set(GARDEN_CHEST.getX(), GARDEN_CHEST.getY(), GARDEN_CHEST.getZ(), Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH));
        s.set(GARDEN_CHEST.getX() + 1, GARDEN_CHEST.getY(), GARDEN_CHEST.getZ(), Blocks.COMPOSTER);
        s.set(GARDEN_CHEST.getX(), g, GARDEN_CHEST.getZ(), Blocks.DIRT);
        s.set(GARDEN_CHEST.getX() + 1, g, GARDEN_CHEST.getZ(), Blocks.DIRT);
        // little scarecrow
        s.set(22, Y, 15, Blocks.OAK_FENCE);
        s.set(22, Y + 1, 15, Blocks.HAY_BLOCK);
        s.set(22, Y + 2, 15, Blocks.CARVED_PUMPKIN);
        s.finish();
    }

    private static void bloodHouse(ServerLevel l) {
        Sculpt s = new Sculpt(l);
        s.walls(7, Y, 21, 20, Y, 27, Blocks.SPRUCE_FENCE.defaultBlockState());
        s.set(13, Y, 21, Blocks.SPRUCE_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, Direction.NORTH));
        s.set(9, Y, 25, Blocks.HAY_BLOCK);
        s.set(10, Y, 25, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        // barn
        s.fill(21, Y - 1, 21, 27, Y - 1, 27, Blocks.SPRUCE_PLANKS);
        s.walls(21, Y, 21, 27, Y + 4, 27, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState());
        s.air(22, Y, 22, 26, Y + 4, 26);
        s.fill(21, Y + 5, 21, 27, Y + 5, 27, Blocks.DARK_OAK_SLAB.defaultBlockState());
        s.air(23, Y, 21, 25, Y + 2, 21);
        s.set(SLICER_CHEST.getX(), SLICER_CHEST.getY(), SLICER_CHEST.getZ(), Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.WEST));
        s.set(26, Y, 22, Blocks.SMOKER.defaultBlockState().setValue(SmokerBlock.FACING, Direction.WEST));
        s.fill(22, Y, 26, 24, Y + 1, 26, Blocks.HAY_BLOCK);
        s.set(26, Y, 24, Blocks.GRINDSTONE);
        s.set(24, Y + 4, 24, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        s.finish();
    }

    private static void deadheads(ServerLevel l) {
        Sculpt s = new Sculpt(l);
        RandomSource r = RandomSource.create(23);
        var reg = l.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
        Holder<ConfiguredFeature<?, ?>> oak = reg.getHolderOrThrow(TreeFeatures.OAK);
        Holder<ConfiguredFeature<?, ?>> birch = reg.getHolderOrThrow(TreeFeatures.BIRCH);
        Holder<ConfiguredFeature<?, ?>> dark = reg.getHolderOrThrow(TreeFeatures.DARK_OAK);
        int placed = 0;
        for (int tries = 0; tries < 400 && placed < 26; tries++) {
            int x = -27 + r.nextInt(20), z = 7 + r.nextInt(20);
            if (x > -13 && z < 16) continue; // graveyard clearing
            BlockPos p = new BlockPos(x, Y, z);
            if (!l.getBlockState(p).isAir() || !l.getBlockState(p.below()).is(Blocks.GRASS_BLOCK)) continue;
            Holder<ConfiguredFeature<?, ?>> h = r.nextFloat() < 0.15F ? dark : r.nextFloat() < 0.4F ? birch : oak;
            if (h.value().place(l, l.getChunkSource().getGenerator(), r, p)) placed++;
        }
        for (int x = -28; x <= -7; x++)
            for (int z = 7; z <= 28; z++)
                if (r.nextFloat() < 0.18F && l.getBlockState(new BlockPos(x, Y, z)).isAir())
                    s.set(x, Y, z, r.nextFloat() < 0.6F ? Blocks.FERN : Blocks.GRASS);
        // graves of the ones the maze took
        String[] dead = {"Nick", "George", "Stan", "Tim", "Ray", "Mike"};
        for (int k = 0; k < dead.length; k++) {
            int x = -12 + (k % 3) * 2, z = 9 + (k / 3) * 4;
            s.set(x, MazeLayout.GROUND, z, Blocks.PODZOL);
            s.set(x, MazeLayout.GROUND, z + 1, Blocks.PODZOL);
            s.set(x, Y, z, Blocks.MOSSY_COBBLESTONE_WALL);
            s.set(x, Y + 1, z, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.LIT, true));
            s.sign(x, Y, z + 2, Blocks.SPRUCE_SIGN, 0, "", dead[k], "Labirent aldı", "");
        }
        s.finish();
    }

    private static void mapRoom(ServerLevel l) {
        Sculpt s = new Sculpt(l);
        int x0 = -24, x1 = -14, z0 = -24, z1 = -14;
        s.fill(x0, Y - 1, z0, x1, Y - 1, z1, Blocks.POLISHED_ANDESITE);
        s.walls(x0, Y, z0, x1, Y + 5, z1, Blocks.GRAY_CONCRETE);
        s.air(x0 + 1, Y, z0 + 1, x1 - 1, Y + 5, z1 - 1);
        s.fill(x0, Y + 6, z0, x1, Y + 6, z1, Blocks.GRAY_CONCRETE);
        s.fill(x0 + 3, Y + 2, z1, x1 - 3, Y + 2, z1, Blocks.IRON_BARS);
        s.door(x1, Y, -19, Blocks.IRON_DOOR, Direction.WEST);
        s.set(x1 + 1, Y + 1, -20, Blocks.STONE_BUTTON.defaultBlockState().setValue(ButtonBlock.FACE,
                net.minecraft.world.level.block.state.properties.AttachFace.WALL).setValue(ButtonBlock.FACING, Direction.EAST));
        s.set(x1 - 1, Y + 1, -20, Blocks.STONE_BUTTON.defaultBlockState().setValue(ButtonBlock.FACE,
                net.minecraft.world.level.block.state.properties.AttachFace.WALL).setValue(ButtonBlock.FACING, Direction.WEST));
        s.set(MAP_TABLE.getX(), MAP_TABLE.getY(), MAP_TABLE.getZ(), ModBlocks.MAP_TABLE.get());
        // one trunk per maze section
        for (int k = 0; k < 4; k++) {
            s.set(x0 + 2 + k * 2, Y, z0 + 1, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH));
            s.set(x0 + 1, Y, z0 + 2 + k * 2, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.EAST));
        }
        s.set(x0 + 1, Y + 5, z0 + 1, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        s.set(-19, Y + 5, -19, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        s.set(x1 - 2, Y, z0 + 2, Blocks.LECTERN);
        s.set(x1 - 1, Y, z0 + 1, Blocks.CARTOGRAPHY_TABLE);
        // the Slammer
        s.fill(-11, Y - 1, -27, -7, Y - 1, -23, Blocks.STONE_BRICKS);
        s.walls(-11, Y, -27, -7, Y + 3, -23, Blocks.STONE_BRICKS);
        s.air(-10, Y, -26, -8, Y + 2, -24);
        s.fill(-11, Y + 4, -27, -7, Y + 4, -23, Blocks.STONE_BRICK_SLAB.defaultBlockState());
        s.fill(-10, Y + 1, -23, -8, Y + 2, -23, Blocks.IRON_BARS);
        s.door(-9, Y, -23, Blocks.SPRUCE_DOOR, Direction.SOUTH);
        s.finish();
    }

    private static void sleepingArea(ServerLevel l) {
        Sculpt s = new Sculpt(l);
        for (BlockPos b : BEDS) if (b.getX() < 0) s.bed(b.getX(), b.getY(), b.getZ(), Blocks.BROWN_BED, Direction.NORTH);
        for (int x = -27; x <= -15; x += 4) {
            s.fill(x, Y, -11, x, Y + 3, -11, Blocks.SPRUCE_FENCE);
            s.fill(x, Y, -7, x, Y + 3, -7, Blocks.SPRUCE_FENCE);
        }
        s.fill(-27, Y + 4, -11, -15, Y + 4, -7, Blocks.SPRUCE_SLAB.defaultBlockState());
        s.finish();
    }

    private static void lamps(ServerLevel l) {
        Sculpt s = new Sculpt(l);
        int[][] posts = {{-5, -12}, {4, -12}, {-5, 11}, {4, 11}, {-12, -5}, {-12, 4}, {11, -5}, {11, 4}, {-5, -24}, {4, -24},
                {-5, 23}, {4, 23}, {-24, -5}, {-24, 4}, {23, 4}, {23, -5}, {-7, -7}, {6, 6}};
        for (int[] p : posts) {
            if (!l.getBlockState(new BlockPos(p[0], Y, p[1])).isAir()) continue;
            s.fill(p[0], Y, p[1], p[0], Y + 2, p[1], Blocks.SPRUCE_FENCE);
            s.set(p[0], Y + 3, p[1], Blocks.LANTERN);
        }
        s.finish();
    }
}
