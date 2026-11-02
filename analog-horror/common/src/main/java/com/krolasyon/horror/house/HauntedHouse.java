package com.krolasyon.horror.house;

import com.krolasyon.horror.block.FacingDecorBlock;
import com.krolasyon.horror.registry.ModBlocks;
import com.krolasyon.horror.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.BarrelBlock;
import com.krolasyon.horror.block.CounterBlock;
import com.krolasyon.horror.block.PropBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * The house the story takes place in, built from the reference photographs:
 * <ol>
 *   <li>ground floor hallway - floral wallpaper, wooden doors on both sides, dirty brown carpet, dome ceiling lamp</li>
 *   <li>living room - stained wallpaper, Persian rug, three worn armchairs around a coffee table, a CRT showing only static</li>
 *   <li>upstairs landing - olive walls, beige carpet, white banister over the stairwell, two balloons in front of a black doorway</li>
 *   <li>upstairs hallway - cream walls, framed pictures on the left, candle sconces on the right, runner rug,
 *   and a pitch-black doorway at the far end</li>
 * </ol>
 * Local coordinates: x east, z south, y up; the inner box spans x 0..20, z 0..22, floors at y 0 and 6. The entrance faces south.
 */
public final class HauntedHouse {
    private final ServerLevel level;
    private final BlockPos o;

    private HauntedHouse(ServerLevel level, BlockPos origin) {
        this.level = level;
        this.o = origin;
    }

    /** builds the house with its north-west inner corner at {@code origin} (origin.y = ground floor level) */
    public static void build(ServerLevel level, BlockPos origin) {
        new HauntedHouse(level, origin).build();
    }

    /** the house plus everything the story needs: notes, pickups, devices, the party room and Victor's attic */
    public static void buildStory(ServerLevel level, BlockPos origin) {
        HauntedHouse h = new HauntedHouse(level, origin);
        h.build();
        h.story();
        setLights(level, origin, 6, 17, false);
    }

    /** switches every story-controlled light (lamps, bulbs, sconces) between y0 and y1 (local) */
    public static void setLights(ServerLevel level, BlockPos o, int y0, int y1, boolean on) {
        for (int x = 0; x <= 20; x++)
            for (int y = y0; y <= y1; y++)
                for (int z = 0; z <= 22; z++) {
                    BlockPos p = o.offset(x, y, z);
                    BlockState st = level.getBlockState(p);
                    Block b = st.getBlock();
                    if ((b == ModBlocks.CEILING_LAMP.get() || b == ModBlocks.BULB_FIXTURE.get() || b == ModBlocks.CANDLE_SCONCE.get())
                            && st.getValue(BlockStateProperties.LIT) != on)
                        level.setBlock(p, st.setValue(BlockStateProperties.LIT, on), 3);
                }
    }

    private void note(int n, int x, int y, int z, boolean raised) {
        set(x, y, z, ModBlocks.NOTE_PAPER.get().defaultBlockState().setValue(PropBlock.NOTE, n).setValue(PropBlock.RAISED, raised));
    }

    public static BlockState item(int i, boolean raised) {
        return ModBlocks.STORY_ITEM.get().defaultBlockState().setValue(PropBlock.ITEM, i).setValue(PropBlock.RAISED, raised);
    }

    private void story() {
        // living room: VCR, fireplace
        set(5, 1, 13, f(ModBlocks.VCR.get(), Direction.SOUTH));
        BlockState brick = b(Blocks.BRICKS);
        fill(1, 1, 17, 1, 4, 17, brick);
        fill(1, 1, 19, 1, 4, 19, brick);
        fill(1, 2, 18, 1, 4, 18, brick);
        set(1, 1, 18, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false).setValue(CampfireBlock.FACING, Direction.EAST));
        // storage: the fuse box
        set(16, 2, 1, f(ModBlocks.FUSE_BOX.get(), Direction.SOUTH).setValue(CounterBlock.FUSES, 0));
        // notes and pickups appear chapter by chapter - see StoryProps
        set(2, 7, 2, b(Blocks.CHEST));
        // Mira's party room
        set(16, 7, 5, Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
        set(16, 8, 5, b(Blocks.CAKE));
        candle(Blocks.BLACK_CANDLE, 14, 4);
        candle(Blocks.RED_CANDLE, 18, 4);
        candle(Blocks.BLUE_CANDLE, 14, 6);
        candle(Blocks.WHITE_CANDLE, 18, 6);
        set(13, 9, 10, b(ModBlocks.BALLOON_WHITE.get()));
        set(19, 9, 9, b(ModBlocks.BALLOON_GOLD.get()));
        set(17, 10, 2, b(ModBlocks.BALLOON_WHITE.get()));
        set(14, 10, 3, b(ModBlocks.BALLOON_GOLD.get()));
        set(16, 10, 6, b(ModBlocks.BULB_FIXTURE.get()));
        // porch mailbox
        set(12, 1, 24, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP));
    }

    private void candle(Block c, int x, int z) {
        set(x, 7, z, c.defaultBlockState().setValue(CandleBlock.CANDLES, 1).setValue(CandleBlock.LIT, false));
    }

    // ======================================================================== exterior
    private static BlockState st(Block stairs, Direction f, Half h) {
        return stairs.defaultBlockState().setValue(StairBlock.FACING, f).setValue(StairBlock.HALF, h);
    }

    /** a framed window with a sill, header and open shutters, cut through the shell and the inner wall */
    private void window(int a0, int y0, int w, int h, Direction out) {
        BlockState outer = b(Blocks.GRAY_STAINED_GLASS), inner = b(Blocks.GLASS);
        BlockState shutter = Blocks.DARK_OAK_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.FACING, out).setValue(TrapDoorBlock.OPEN, true);
        BlockState sill = st(Blocks.SPRUCE_STAIRS, out.getOpposite(), Half.TOP), header = Blocks.DARK_OAK_SLAB.defaultBlockState();
        for (int i = -1; i <= w; i++) {
            for (int j = -1; j <= h; j++) {
                int[] p = wallPos(a0 + i, out);
                boolean inside = i >= 0 && i < w && j >= 0 && j < h;
                if (inside) {
                    set(p[0], y0 + j, p[1], outer);
                    set(p[0] - out.getStepX(), y0 + j, p[1] - out.getStepZ(), inner);
                }
                int fx = p[0] + out.getStepX(), fz = p[1] + out.getStepZ();
                if (j == -1 && i >= 0 && i < w) set(fx, y0 - 1, fz, sill);
                else if (j == h && i >= -1 && i <= w) set(fx, y0 + h, fz, header);
                else if ((i == -1 || i == w) && j >= 0 && j < h) set(fx, y0 + j, fz, shutter);
            }
        }
    }

    /** shell coordinate of position {@code a} along the wall facing {@code out} */
    private static int[] wallPos(int a, Direction out) {
        return switch (out) {
            case SOUTH -> new int[]{a, 23};
            case NORTH -> new int[]{a, -1};
            case WEST -> new int[]{-1, a};
            default -> new int[]{21, a};
        };
    }

    private void exterior() {
        BlockState plankA = b(Blocks.SPRUCE_PLANKS), plankB = b(Blocks.DARK_OAK_PLANKS), stone = b(Blocks.STONE_BRICKS),
                mossy = b(Blocks.MOSSY_STONE_BRICKS), log = b(Blocks.DARK_OAK_LOG);
        // clapboard siding, stone plinth, belt beam
        for (int y = 0; y <= 11; y++) {
            for (int x = -1; x <= 21; x++)
                for (int z : new int[]{-1, 23}) {
                    BlockState bs = y <= 1 ? ((x * 7 + y * 3) % 5 == 0 ? mossy : stone) : y == 5 ? b(Blocks.STRIPPED_DARK_OAK_WOOD) : (y % 2 == 0 ? plankA : plankB);
                    if (!(z == 23 && x == 10 && (y == 1 || y == 2))) set(x, y, z, bs);
                }
            for (int z = -1; z <= 23; z++)
                for (int x : new int[]{-1, 21}) {
                    BlockState bs = y <= 1 ? ((z * 7 + y * 3) % 5 == 0 ? mossy : stone) : y == 5 ? b(Blocks.STRIPPED_DARK_OAK_WOOD) : (y % 2 == 0 ? plankA : plankB);
                    set(x, y, z, bs);
                }
        }
        for (int[] c : new int[][]{{-1, -1}, {21, -1}, {-1, 23}, {21, 23}, {-1, 5}, {-1, 11}, {-1, 17}, {21, 5}, {21, 11}, {7, -1}, {13, -1}, {7, 23}, {13, 23}})
            fill(c[0], 2, c[1], c[0], 11, c[1], log);
        // skirt around the plinth
        for (int x = -2; x <= 22; x++) {
            set(x, 0, -2, st(Blocks.STONE_BRICK_STAIRS, Direction.SOUTH, Half.BOTTOM));
        }
        for (int z = -1; z <= 23; z++) {
            set(-2, 0, z, st(Blocks.STONE_BRICK_STAIRS, Direction.EAST, Half.BOTTOM));
            set(22, 0, z, st(Blocks.STONE_BRICK_STAIRS, Direction.WEST, Half.BOTTOM));
        }
        // windows
        window(3, 2, 3, 2, Direction.SOUTH); window(15, 2, 3, 2, Direction.SOUTH);
        window(3, 8, 3, 2, Direction.SOUTH); window(15, 8, 3, 2, Direction.SOUTH);
        window(14, 2, 1, 2, Direction.WEST); window(20, 2, 1, 2, Direction.WEST); window(4, 2, 2, 2, Direction.WEST);
        window(3, 8, 2, 2, Direction.WEST); window(14, 8, 2, 2, Direction.WEST); window(18, 8, 2, 2, Direction.WEST);
        window(4, 2, 1, 2, Direction.EAST); window(4, 8, 3, 2, Direction.EAST);
        window(3, 2, 2, 2, Direction.NORTH); window(18, 2, 2, 2, Direction.NORTH); window(4, 8, 3, 2, Direction.NORTH); window(15, 8, 3, 2, Direction.NORTH);
        // gables: round attic windows, bargeboard trim, finials
        for (int z : new int[]{-1, 23}) {
            for (int[] g : new int[][]{{10, 15}, {10, 16}, {10, 17}, {9, 16}, {11, 16}}) set(g[0], g[1], z, b(Blocks.GRAY_STAINED_GLASS));
            for (int[] g : new int[][]{{10, 14}, {10, 18}, {8, 16}, {12, 16}, {9, 15}, {11, 15}, {9, 17}, {11, 17}}) set(g[0], g[1], z, b(Blocks.STRIPPED_DARK_OAK_LOG));
            for (int i = 0; i <= 11; i++) {
                int zz = z == 23 ? 25 : -3;
                set(-2 + i, 12 + i - 1, zz, st(Blocks.DARK_OAK_STAIRS, Direction.WEST, Half.TOP));
                set(22 - i, 12 + i - 1, zz, st(Blocks.DARK_OAK_STAIRS, Direction.EAST, Half.TOP));
            }
            set(10, 24, z == 23 ? 24 : -2, b(Blocks.LIGHTNING_ROD));
        }
        // chimneys with smoke
        for (int[] c : new int[][]{{2, 6}, {17, 15}}) {
            fill(c[0], 11, c[1], c[0] + 1, 21, c[1] + 1, b(Blocks.BRICKS));
            set(c[0], 21, c[1], Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        }
        // wrap-around porch
        fill(-3, 0, 24, 23, 0, 28, b(Blocks.DARK_OAK_PLANKS));
        fill(-3, 0, 28, 23, 0, 28, b(Blocks.STRIPPED_DARK_OAK_WOOD));
        fill(-4, 5, 24, 24, 5, 29, Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
        for (int x = -4; x <= 24; x++) set(x, 5, 30, st(Blocks.DARK_OAK_STAIRS, Direction.NORTH, Half.TOP));
        for (int x : new int[]{-3, 2, 7, 13, 18, 23}) {
            fill(x, 1, 28, x, 4, 28, b(Blocks.STRIPPED_DARK_OAK_LOG));
            set(x, 4, 29, st(Blocks.DARK_OAK_STAIRS, Direction.NORTH, Half.TOP));
        }
        for (int x = -2; x <= 22; x++) if ((x < 9 || x > 11) && x != 2 && x != 7 && x != 13 && x != 18) set(x, 1, 28, b(Blocks.DARK_OAK_FENCE));
        for (int z = 24; z <= 27; z++) { set(-3, 1, z, b(Blocks.DARK_OAK_FENCE)); set(23, 1, z, b(Blocks.DARK_OAK_FENCE)); }
        for (int x = 9; x <= 11; x++) set(x, 0, 29, st(Blocks.DARK_OAK_STAIRS, Direction.NORTH, Half.BOTTOM));
        for (int x : new int[]{0, 10, 20}) set(x, 4, 26, Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true));
        set(4, 1, 25, st(Blocks.SPRUCE_STAIRS, Direction.NORTH, Half.BOTTOM));
        set(6, 1, 26, st(Blocks.SPRUCE_STAIRS, Direction.WEST, Half.BOTTOM));
        set(8, 1, 29, Blocks.JACK_O_LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.CarvedPumpkinBlock.FACING, Direction.SOUTH));
        set(12, 1, 29, Blocks.CARVED_PUMPKIN.defaultBlockState().setValue(net.minecraft.world.level.block.CarvedPumpkinBlock.FACING, Direction.SOUTH));
        set(-3, 4, 24, b(Blocks.COBWEB)); set(23, 4, 27, b(Blocks.COBWEB));
        tower();
        grounds();
    }

    /** a square corner turret with a steep spire and one faintly lit window */
    private void tower() {
        int x0 = 22, z0 = 16, x1 = 27, z1 = 21, top = 16;
        for (int y = 0; y <= top; y++)
            for (int x = x0; x <= x1; x++)
                for (int z = z0; z <= z1; z++) {
                    boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
                    boolean corner = (x == x0 || x == x1) && (z == z0 || z == z1);
                    if (!edge) { set(x, y, z, Blocks.AIR.defaultBlockState()); continue; }
                    set(x, y, z, corner ? b(Blocks.DARK_OAK_LOG) : y <= 1 ? b(Blocks.STONE_BRICKS) : y == 5 || y == 11 ? b(Blocks.STRIPPED_DARK_OAK_WOOD)
                            : y % 2 == 0 ? b(Blocks.SPRUCE_PLANKS) : b(Blocks.DARK_OAK_PLANKS));
                }
        for (int y : new int[]{3, 8, 13}) {
            for (int dy = 0; dy < 2; dy++) {
                set(24, y + dy, z1, b(Blocks.GRAY_STAINED_GLASS)); set(25, y + dy, z1, b(Blocks.GRAY_STAINED_GLASS));
                set(x1, y + dy, 18, b(Blocks.GRAY_STAINED_GLASS)); set(x1, y + dy, 19, b(Blocks.GRAY_STAINED_GLASS));
            }
            set(24, y - 1, z1 + 1, st(Blocks.SPRUCE_STAIRS, Direction.NORTH, Half.TOP)); set(25, y - 1, z1 + 1, st(Blocks.SPRUCE_STAIRS, Direction.NORTH, Half.TOP));
        }
        set(24, 9, 18, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 7));
        // spire
        for (int i = 0; i <= 3; i++) {
            int y = top + 1 + i * 2, a0 = x0 - 1 + i, a1 = x1 + 1 - i, b0 = z0 - 1 + i, b1 = z1 + 1 - i;
            for (int dy = 0; dy < 2; dy++) {
                for (int x = a0; x <= a1; x++) {
                    set(x, y + dy, b0, st(Blocks.DEEPSLATE_TILE_STAIRS, Direction.SOUTH, Half.BOTTOM));
                    set(x, y + dy, b1, st(Blocks.DEEPSLATE_TILE_STAIRS, Direction.NORTH, Half.BOTTOM));
                }
                for (int z = b0 + 1; z < b1; z++) {
                    set(a0, y + dy, z, st(Blocks.DEEPSLATE_TILE_STAIRS, Direction.EAST, Half.BOTTOM));
                    set(a1, y + dy, z, st(Blocks.DEEPSLATE_TILE_STAIRS, Direction.WEST, Half.BOTTOM));
                }
                if (a1 - a0 > 1) fill(a0 + 1, y + dy, b0 + 1, a1 - 1, y + dy, b1 - 1, b(Blocks.DEEPSLATE_TILES));
            }
        }
        fill(24, 25, 18, 25, 25, 19, b(Blocks.DEEPSLATE_TILES));
        fill(24, 26, 18, 24, 27, 18, b(Blocks.DARK_OAK_FENCE));
        set(24, 28, 18, b(Blocks.LIGHTNING_ROD));
    }

    /** overgrown front yard: iron fence with stone pillars, gravel path, lamp posts, dead trees, weeds */
    private void grounds() {
        net.minecraft.util.RandomSource r = net.minecraft.util.RandomSource.create(1998L);
        for (int x = -9; x <= 31; x++)
            for (int z = -7; z <= 40; z++) {
                if (x >= -4 && x <= 28 && z >= -3 && z <= 30) continue;
                int k = r.nextInt(10);
                if (k == 0) set(x, -1, z, b(Blocks.COARSE_DIRT));
                else if (k == 1) set(x, -1, z, b(Blocks.PODZOL));
            }
        for (int z = 30; z <= 40; z++)
            for (int x = 9; x <= 11; x++) set(x, -1, z, (x + z) % 4 == 0 ? b(Blocks.COARSE_DIRT) : b(Blocks.DIRT_PATH));
        // perimeter fence
        for (int x = -9; x <= 31; x++)
            for (int z : new int[]{-7, 40}) {
                if (z == 40 && x >= 8 && x <= 12) continue;
                set(x, 0, z, x % 5 == 0 ? b(Blocks.STONE_BRICK_WALL) : b(Blocks.IRON_BARS));
                set(x, 1, z, x % 5 == 0 ? b(Blocks.STONE_BRICK_WALL) : b(Blocks.IRON_BARS));
            }
        for (int z = -7; z <= 40; z++)
            for (int x : new int[]{-9, 31}) {
                set(x, 0, z, z % 5 == 0 ? b(Blocks.STONE_BRICK_WALL) : b(Blocks.IRON_BARS));
                set(x, 1, z, z % 5 == 0 ? b(Blocks.STONE_BRICK_WALL) : b(Blocks.IRON_BARS));
            }
        for (int x : new int[]{7, 13}) {
            fill(x, 0, 40, x, 2, 40, b(Blocks.STONE_BRICKS));
            set(x, 3, 40, b(Blocks.LANTERN));
        }
        // lamp posts along the path
        for (int[] p : new int[][]{{7, 34}, {13, 34}}) {
            fill(p[0], 0, p[1], p[0], 2, p[1], b(Blocks.DARK_OAK_FENCE));
            set(p[0], 3, p[1], b(Blocks.LANTERN));
        }
        // dead trees
        for (int[] t : new int[][]{{-5, 33, 8}, {26, 34, 9}, {-6, -3, 7}, {28, 2, 8}}) deadTree(t[0], t[1], t[2], r);
        // weeds
        for (int i = 0; i < 160; i++) {
            int x = -8 + r.nextInt(39), z = -6 + r.nextInt(46);
            if (x >= -4 && x <= 28 && z >= -3 && z <= 30) continue;
            if (x >= 8 && x <= 12 && z >= 29) continue;
            int k = r.nextInt(3);
            set(x, 0, z, k == 0 ? b(Blocks.DEAD_BUSH) : k == 1 ? b(Blocks.FERN) : b(Blocks.SWEET_BERRY_BUSH));
        }
    }

    private void deadTree(int x, int z, int h, net.minecraft.util.RandomSource r) {
        fill(x, 0, z, x, h, z, b(Blocks.DARK_OAK_LOG));
        Direction[] dirs = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        for (int i = 0; i < 4; i++) {
            Direction d = dirs[(i + r.nextInt(4)) % 4];
            int y = h - 3 + i % 3, len = 2 + r.nextInt(2);
            for (int k = 1; k <= len; k++) {
                int bx = x + d.getStepX() * k, bz = z + d.getStepZ() * k, by = y + k / 2;
                set(bx, by, bz, Blocks.DARK_OAK_LOG.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, d.getAxis()));
                if (k == len) { set(bx, by + 1, bz, b(Blocks.DARK_OAK_FENCE)); set(bx + d.getStepX(), by + 1, bz + d.getStepZ(), b(Blocks.DARK_OAK_FENCE)); }
            }
        }
        set(x, h + 1, z, b(Blocks.DARK_OAK_FENCE));
    }

    /** Victor's workshop in the roof space, reached by a padlocked hatch from the upstairs hallway */
    private void attic() {
        fill(0, 12, 0, 20, 12, 22, b(Blocks.DARK_OAK_PLANKS));
        for (int y = 7; y <= 11; y++) set(9, y, 10, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.EAST));
        set(9, 12, 10, Blocks.SPRUCE_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.HALF, Half.TOP).setValue(TrapDoorBlock.OPEN, false)
                .setValue(TrapDoorBlock.FACING, Direction.SOUTH));
        for (int x = 6; x <= 14; x++) {
            set(x, 13, 1, f(ModBlocks.CRT_TV.get(), Direction.SOUTH));
            set(x, 14, 1, Blocks.DARK_OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
            set(x, 15, 1, f(ModBlocks.CRT_TV.get(), Direction.SOUTH));
        }
        for (int x = 8; x <= 11; x++) {
            set(x, 13, 4, b(Blocks.STRIPPED_DARK_OAK_WOOD));
            set(x, 14, 4, f(ModBlocks.DIAL.get(), Direction.SOUTH));
        }
        set(12, 13, 4, Blocks.BLAST_FURNACE.defaultBlockState());
        set(12, 14, 4, b(Blocks.LIGHTNING_ROD));
        set(7, 13, 8, b(Blocks.LECTERN));
        set(10, 16, 10, b(ModBlocks.BULB_FIXTURE.get()));
        set(10, 16, 5, b(ModBlocks.BULB_FIXTURE.get()));
        set(4, 13, 2, b(Blocks.COBWEB)); set(16, 15, 20, b(Blocks.COBWEB)); set(5, 13, 20, b(Blocks.BARREL));
    }

    /** where the camera / player should stand to start: just inside the front door, looking down the hallway */
    public static BlockPos entrance(BlockPos origin) { return origin.offset(10, 1, 21); }

    private void set(int x, int y, int z, BlockState s) { level.setBlock(o.offset(x, y, z), s, 2 | 16); }

    private void fill(int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) set(x, y, z, s);
    }

    private static BlockState b(Block blk) { return blk.defaultBlockState(); }

    private static BlockState f(Block blk, Direction d) { return blk.defaultBlockState().setValue(FacingDecorBlock.FACING, d); }

    /** paints the walls around a room's interior (x0..x1, z0..z1) for one storey */
    private void room(int x0, int z0, int x1, int z1, int y0, int y1, Block wall) {
        BlockState w = b(wall);
        fill(x0 - 1, y0, z0 - 1, x1 + 1, y1, z0 - 1, w);
        fill(x0 - 1, y0, z1 + 1, x1 + 1, y1, z1 + 1, w);
        fill(x0 - 1, y0, z0 - 1, x0 - 1, y1, z1 + 1, w);
        fill(x1 + 1, y0, z0 - 1, x1 + 1, y1, z1 + 1, w);
        fill(x0, y0, z0, x1, y1, z1, Blocks.AIR.defaultBlockState());
    }

    private void door(int x, int y, int z, Block door, Direction facing, boolean open) {
        BlockState s = door.defaultBlockState().setValue(DoorBlock.FACING, facing).setValue(DoorBlock.OPEN, open);
        set(x, y, z, s.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        set(x, y + 1, z, s.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }

    private void opening(int x, int y, int z) {
        set(x, y, z, Blocks.AIR.defaultBlockState());
        set(x, y + 1, z, Blocks.AIR.defaultBlockState());
    }

    private void build() {
        BlockState air = Blocks.AIR.defaultBlockState();
        // ---------------------------------------------------------------- clear + shell
        fill(-9, 0, -7, 31, 30, 40, air);
        fill(-9, -4, -7, 31, -2, 40, b(Blocks.DIRT));
        fill(-9, -1, -7, 31, -1, 40, b(Blocks.GRASS_BLOCK));
        fill(-1, -1, -1, 21, -1, 23, b(Blocks.COBBLED_DEEPSLATE));
        BlockState planks = b(Blocks.SPRUCE_PLANKS), log = b(Blocks.DARK_OAK_LOG), beam = b(Blocks.STRIPPED_DARK_OAK_LOG);
        fill(-1, 0, -1, 21, 11, -1, planks);
        fill(-1, 0, 23, 21, 11, 23, planks);
        fill(-1, 0, -1, -1, 11, 23, planks);
        fill(21, 0, -1, 21, 11, 23, planks);
        for (int[] c : new int[][]{{-1, -1}, {21, -1}, {-1, 23}, {21, 23}}) fill(c[0], 0, c[1], c[0], 11, c[1], log);
        fill(-1, 5, -1, 21, 5, -1, beam); fill(-1, 5, 23, 21, 5, 23, beam);
        // roof: gable, ridge running north-south
        for (int i = 0; i <= 11; i++) {
            int y = 12 + i, xl = -2 + i, xr = 22 - i;
            for (int z = -2; z <= 24; z++) {
                set(xl, y, z, Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST));
                set(xr, y, z, Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST));
            }
            if (xr - xl > 1) {
                fill(xl + 1, y, -1, xr - 1, y, -1, planks);
                fill(xl + 1, y, 23, xr - 1, y, 23, planks);
            }
        }
        for (int z = -2; z <= 24; z++) set(10, 23, z, Blocks.DARK_OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));

        // ---------------------------------------------------------------- floors / ceilings
        fill(0, 0, 0, 20, 0, 22, b(Blocks.DARK_OAK_PLANKS));
        fill(0, 5, 0, 20, 5, 22, b(ModBlocks.CEILING_PLASTER.get()));
        fill(0, 6, 0, 20, 6, 22, b(Blocks.DARK_OAK_PLANKS));
        fill(0, 11, 0, 20, 11, 22, b(ModBlocks.CEILING_PLASTER.get()));
        fill(0, 1, 0, 20, 4, 22, b(ModBlocks.WALL_CREAM.get()));
        fill(0, 7, 0, 20, 10, 22, b(ModBlocks.WALL_CREAM.get()));

        groundFloor();
        upperFloor();
        attic();

        door(10, 1, 23, Blocks.DARK_OAK_DOOR, Direction.NORTH, false);
        opening(10, 1, 22);
        exterior();
    }

    private void groundFloor() {
        // rooms (later rooms paint over shared walls)
        room(1, 1, 7, 11, 1, 4, ModBlocks.WALL_CREAM.get());                 // kitchen
        room(13, 1, 19, 11, 1, 4, ModBlocks.WALL_CREAM.get());               // storage
        room(1, 13, 7, 21, 1, 4, ModBlocks.WALLPAPER_FLORAL_STAINED.get());  // living room
        room(13, 13, 19, 21, 1, 4, ModBlocks.WALLPAPER_FLORAL.get());        // stair hall
        room(9, 6, 11, 21, 1, 4, ModBlocks.WALLPAPER_FLORAL.get());          // hallway (photo 4)

        // hallway: dirty brown carpet, dome lamps, wooden doors on both sides
        fill(9, 1, 6, 11, 1, 21, b(ModBlocks.CARPET_BROWN_DIRTY.get()));
        set(10, 4, 9, b(ModBlocks.CEILING_LAMP.get()));
        set(10, 4, 17, b(ModBlocks.CEILING_LAMP.get()));
        door(8, 1, 17, Blocks.MANGROVE_DOOR, Direction.WEST, false);
        door(8, 1, 8, Blocks.MANGROVE_DOOR, Direction.WEST, false);
        door(12, 1, 16, Blocks.MANGROVE_DOOR, Direction.EAST, false);
        door(12, 1, 8, Blocks.MANGROVE_DOOR, Direction.EAST, false);
        set(9, 1, 7, b(Blocks.COBWEB));

        // living room (photo 3)
        for (int x = 1; x <= 7; x++)
            for (int z = 14; z <= 21; z++) {
                boolean edgeN = z == 14, edgeS = z == 21, edgeW = x == 1, edgeE = x == 7;
                BlockState s;
                if (edgeN) s = f(ModBlocks.RUG_PERSIAN_EDGE.get(), Direction.NORTH);
                else if (edgeS) s = f(ModBlocks.RUG_PERSIAN_EDGE.get(), Direction.SOUTH);
                else if (edgeW) s = f(ModBlocks.RUG_PERSIAN_EDGE.get(), Direction.WEST);
                else if (edgeE) s = f(ModBlocks.RUG_PERSIAN_EDGE.get(), Direction.EAST);
                else s = b(ModBlocks.RUG_PERSIAN.get());
                set(x, 1, z, s);
            }
        set(4, 1, 13, f(ModBlocks.CRT_TV.get(), Direction.SOUTH));
        set(5, 1, 14, b(Blocks.TRIPWIRE));
        set(6, 1, 14, b(Blocks.TRIPWIRE));
        set(4, 1, 17, b(ModBlocks.COFFEE_TABLE.get()));
        set(2, 1, 16, f(ModBlocks.ARMCHAIR.get(), Direction.EAST));
        set(2, 1, 18, f(ModBlocks.ARMCHAIR.get(), Direction.EAST));
        set(6, 1, 18, f(ModBlocks.ARMCHAIR.get(), Direction.WEST));
        set(4, 4, 17, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 6));   // the cold pool of light on the table
        set(1, 3, 13, f(ModBlocks.PICTURE_PORTRAIT.get(), Direction.SOUTH));
        set(1, 4, 21, b(Blocks.COBWEB));
        set(7, 4, 13, b(Blocks.COBWEB));

        // kitchen + storage: abandoned
        set(2, 1, 1, b(Blocks.SMOKER)); set(3, 1, 1, b(Blocks.CAULDRON)); set(4, 1, 1, b(Blocks.BARREL));
        set(4, 1, 6, b(ModBlocks.COFFEE_TABLE.get()));
        set(1, 4, 1, b(Blocks.COBWEB)); set(7, 4, 11, b(Blocks.COBWEB));
        set(13, 1, 1, b(Blocks.BARREL)); set(14, 1, 1, b(Blocks.CHEST)); set(19, 1, 11, b(Blocks.COBWEB)); set(19, 4, 1, b(Blocks.COBWEB));
        set(16, 4, 6, b(ModBlocks.BULB_FIXTURE.get()));

        // stair hall: stairs climbing north along the east wall to the landing at z = 16
        fill(13, 1, 13, 19, 1, 21, b(ModBlocks.CARPET_BROWN_DIRTY.get()));
        fill(18, 1, 17, 19, 6, 21, Blocks.AIR.defaultBlockState());
        for (int i = 0; i < 5; i++) {
            BlockState st = Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
            fill(18, 1 + i, 21 - i, 19, 1 + i, 21 - i, st);
            if (i > 0) fill(18, 1, 21 - i, 19, i, 21 - i, b(Blocks.DARK_OAK_PLANKS));
        }
        set(15, 4, 17, b(ModBlocks.CEILING_LAMP.get()));
    }

    private void upperFloor() {
        room(13, 1, 19, 11, 7, 10, ModBlocks.WALLPAPER_FLORAL_STAINED.get());   // the black room behind the balloons
        room(1, 1, 11, 7, 7, 10, ModBlocks.WALLPAPER_FLORAL_STAINED.get());     // the dark bedroom at the end of the hall
        room(1, 9, 7, 21, 7, 10, ModBlocks.WALL_CREAM.get());                    // old bedroom
        room(13, 13, 19, 21, 7, 10, ModBlocks.WALL_OLIVE.get());                 // landing (photo 2)
        room(9, 9, 11, 21, 7, 10, ModBlocks.WALL_CREAM.get());                   // hallway (photo 1)

        // re-open the stairwell through the landing floor
        fill(18, 5, 17, 19, 6, 21, Blocks.AIR.defaultBlockState());
        fill(18, 5, 17, 19, 5, 17, Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH));

        // landing: beige carpet, banister around the stairwell, balloons before a black doorway
        for (int x = 13; x <= 19; x++)
            for (int z = 13; z <= 21; z++) if (!(x >= 18 && z >= 17)) set(x, 7, z, b(ModBlocks.CARPET_BEIGE.get()));
        for (int z = 17; z <= 21; z++) set(17, 7, z, f(ModBlocks.BANISTER.get(), Direction.EAST));
        set(17, 7, 16, b(ModBlocks.BANISTER_POST.get()));
        opening(15, 7, 12);                                        // the black doorway
        set(15, 9, 13, b(ModBlocks.BALLOON_WHITE.get()));
        set(14, 8, 14, b(ModBlocks.BALLOON_GOLD.get()));
        set(14, 10, 19, b(ModBlocks.BULB_FIXTURE.get()));

        // hallway: runner rug, pictures on the left wall, candle sconces on the right, dark doorway at the end
        for (int z = 9; z <= 21; z++) set(10, 7, z, f(ModBlocks.RUNNER_RUG.get(), Direction.NORTH));
        opening(12, 7, 20);
        door(12, 7, 20, Blocks.DARK_OAK_DOOR, Direction.EAST, true);
        set(9, 8, 18, f(ModBlocks.PICTURE_SKETCH.get(), Direction.EAST));
        set(9, 8, 16, f(ModBlocks.PICTURE_PORTRAIT.get(), Direction.EAST));
        set(9, 8, 15, f(ModBlocks.PICTURE_LANDSCAPE.get(), Direction.EAST));
        set(9, 8, 12, f(ModBlocks.PICTURE_SKETCH.get(), Direction.EAST));
        set(11, 8, 15, f(ModBlocks.CANDLE_SCONCE.get(), Direction.WEST));
        set(11, 8, 13, f(ModBlocks.CANDLE_SCONCE.get(), Direction.WEST));
        set(10, 10, 18, b(ModBlocks.BULB_FIXTURE.get()));
        opening(10, 7, 8);                                         // pitch black room beyond
        door(8, 7, 19, Blocks.DARK_OAK_DOOR, Direction.WEST, false);

        // old bedroom
        set(2, 7, 10, b(Blocks.CHEST));
        set(6, 7, 20, b(Blocks.BARREL));
        set(1, 10, 21, b(Blocks.COBWEB));
        // dark bedroom: nothing but cobwebs
        set(1, 10, 1, b(Blocks.COBWEB)); set(11, 10, 7, b(Blocks.COBWEB)); set(19, 10, 1, b(Blocks.COBWEB));
    }
}
