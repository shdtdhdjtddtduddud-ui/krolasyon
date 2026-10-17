package com.krolasyon.mirror.world;

import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The house the reflection builds in its own world, in a canonical frame where the mirror faces south (+z):
 * x is the viewer's right, z goes negative behind the mirror, y=0 is the mirror's floor level.
 */
public final class HouseBlueprint {
    public record Entry(int x, int y, int z, BlockState state) {}

    /** A pause in the building where the reflection knocks on the glass and asks for an item. */
    public record Request(int start, Item item, int count) {}

    public static final List<Entry> HOUSE;
    public static final List<Request> REQUESTS;
    /** Index of the foundation entry under column (x, z), or -1. */
    private static final int[][] FOUNDATION = new int[7][7];
    public static final int MIN_X = -4, MAX_X = 4, MIN_Z = -10, MAX_Z = -2, MAX_Y = 8;

    private static final List<Entry> list = new ArrayList<>();
    private static final List<Request> requests = new ArrayList<>();

    static {
        BlockState cobble = Blocks.COBBLESTONE.defaultBlockState();
        BlockState floor = Blocks.SPRUCE_PLANKS.defaultBlockState();
        BlockState planks = Blocks.OAK_PLANKS.defaultBlockState();
        BlockState log = Blocks.OAK_LOG.defaultBlockState();

        request(Items.COBBLESTONE, 16);
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z >= -9; z--) {
                FOUNDATION[x + 3][-3 - z] = list.size();
                boolean edge = Math.abs(x) == 3 || z == -3 || z == -9;
                add(x, -1, z, edge ? cobble : floor);
            }
        }

        request(Items.OAK_LOG, 8);
        for (int y = 0; y <= 3; y++) {
            for (int x = -3; x <= 3; x++) {
                for (int z = -3; z >= -9; z--) {
                    boolean side = Math.abs(x) == 3, frontBack = z == -3 || z == -9;
                    if (!side && !frontBack) continue;
                    if (x == 0 && z == -3 && y <= 1) continue;
                    if (isWindow(x, y, z)) continue;
                    add(x, y, z, side && frontBack ? log : planks);
                }
            }
        }

        request(Items.GLASS, 4);
        BlockState paneX = Blocks.GLASS_PANE.defaultBlockState().setValue(CrossCollisionBlock.EAST, true).setValue(CrossCollisionBlock.WEST, true);
        BlockState paneZ = Blocks.GLASS_PANE.defaultBlockState().setValue(CrossCollisionBlock.NORTH, true).setValue(CrossCollisionBlock.SOUTH, true);
        for (int y = 1; y <= 2; y++) {
            for (int x = -3; x <= 3; x++) {
                for (int z = -3; z >= -9; z--) {
                    if (isWindow(x, y, z)) add(x, y, z, Math.abs(x) == 3 ? paneZ : paneX);
                }
            }
        }
        BlockState door = Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH);
        add(0, 0, -3, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        add(0, 1, -3, door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));

        // gable roof, ridge running away from the mirror so the triangle faces the glass
        for (int k = 0; k <= 3; k++) {
            int y = 4 + k;
            for (int z = -2; z >= -10; z--) {
                add(-4 + k, y, z, Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST));
                add(4 - k, y, z, Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST));
            }
            if (k < 3) {
                for (int x = -3 + k; x <= 3 - k; x++) {
                    add(x, y, -3, planks);
                    add(x, y, -9, planks);
                }
            }
        }
        for (int z = -2; z >= -10; z--) add(0, 7, z, planks);

        request(Items.TORCH, 4);
        add(-2, 0, -8, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH));
        add(2, 0, -8, Blocks.CRAFTING_TABLE.defaultBlockState());
        add(2, 0, -5, Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH).setValue(BedBlock.PART, BedPart.FOOT));
        add(2, 0, -6, Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH).setValue(BedBlock.PART, BedPart.HEAD));
        add(-2, 0, -4, Blocks.LANTERN.defaultBlockState());
        add(0, 0, -8, Blocks.POTTED_POPPY.defaultBlockState());
        add(-2, 2, -8, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.SOUTH));
        add(2, 2, -8, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.SOUTH));
        add(-1, 2, -2, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.SOUTH));
        add(1, 2, -2, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.SOUTH));

        HOUSE = Collections.unmodifiableList(new ArrayList<>(list));
        REQUESTS = Collections.unmodifiableList(new ArrayList<>(requests));
    }

    private static boolean isWindow(int x, int y, int z) {
        if (y < 1 || y > 2) return false;
        return (z == -3 && Math.abs(x) == 2) || (Math.abs(x) == 3 && (z == -5 || z == -7)) || (z == -9 && x == 0);
    }

    private static void add(int x, int y, int z, BlockState s) { list.add(new Entry(x, y, z, s)); }

    private static void request(Item item, int count) { requests.add(new Request(list.size(), item, count)); }

    public static boolean inFootprint(int x, int z) { return Math.abs(x) <= 3 && z <= -3 && z >= -9; }

    public static int foundationIndex(int x, int z) { return inFootprint(x, z) ? FOUNDATION[x + 3][-3 - z] : -1; }

    /** Rotation that turns the canonical (south facing) house into one for a mirror facing {@code facing}. */
    public static Rotation rotationFor(Direction facing) {
        return switch (facing) {
            case WEST -> Rotation.CLOCKWISE_90;
            case NORTH -> Rotation.CLOCKWISE_180;
            case EAST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    private HouseBlueprint() {}
}
