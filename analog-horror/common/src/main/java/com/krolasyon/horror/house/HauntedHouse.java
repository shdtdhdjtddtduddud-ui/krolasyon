package com.krolasyon.horror.house;

import com.krolasyon.horror.block.FacingDecorBlock;
import com.krolasyon.horror.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
        fill(-3, 0, -3, 23, 24, 25, air);
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

        // windows (tinted, so the house stays dark inside)
        BlockState glass = b(Blocks.TINTED_GLASS);
        for (int[] w : new int[][]{{-1, 16}, {-1, 18}, {-1, 4}, {-1, 15}}) {
            int y = w[1] == 15 ? 8 : 2;
            if (w[1] == 4) y = 8;
            fill(w[0], y, w[1], w[0] + 1, y + 1, w[1], glass);
        }
        fill(20, 2, 4, 21, 3, 4, glass);
        fill(20, 8, 17, 21, 9, 17, glass);
        // front door + porch
        door(10, 1, 23, Blocks.DARK_OAK_DOOR, Direction.NORTH, false);
        opening(10, 1, 22);
        fill(8, 0, 24, 12, 0, 25, b(Blocks.SPRUCE_PLANKS));
        set(9, 1, 24, b(Blocks.LANTERN));
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
