package com.krolasyon.futbol.game;

import com.krolasyon.futbol.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Builds a full stadium: striped pitch with all markings, goals with nets, corner flags, stands and floodlights. */
public final class PitchBuilder {
    private PitchBuilder() {}

    private static final int FLAGS = Block.UPDATE_CLIENTS;
    private static final int GRASS_X = Pitch.HL + 4;
    private static final int GRASS_Z = Pitch.HW + 4;
    private static final int ROWS = 6;
    private static final int OUT_X = GRASS_X + 2 + ROWS + 2;
    private static final int OUT_Z = GRASS_Z + 1 + ROWS + 2;

    public static Pitch build(ServerLevel level, BlockPos origin) {
        int cx = origin.getX(), cz = origin.getZ(), y = origin.getY() - 1;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        BlockState air = Blocks.AIR.defaultBlockState();

        // clear the volume and lay the ground
        for (int dx = -OUT_X; dx <= OUT_X; dx++) {
            for (int dz = -OUT_Z; dz <= OUT_Z; dz++) {
                int x = cx + dx, z = cz + dz;
                for (int h = 1; h <= 26; h++) {
                    p.set(x, y + h, z);
                    if (!level.getBlockState(p).isAir()) level.setBlock(p, air, FLAGS);
                }
                boolean grass = Math.abs(dx) <= GRASS_X && Math.abs(dz) <= GRASS_Z;
                for (int h = 1; h <= 3; h++) {
                    p.set(x, y - h, z);
                    BlockState s = level.getBlockState(p);
                    if (s.isAir() || !s.getFluidState().isEmpty()) level.setBlock(p, (grass ? Blocks.DIRT : Blocks.STONE).defaultBlockState(), FLAGS);
                }
                p.set(x, y, z);
                BlockState ground;
                if (grass) {
                    if (isLine(dx, dz)) ground = ModBlocks.LINE.get().defaultBlockState();
                    else ground = (Math.floorMod(dx + GRASS_X, 8) < 4 ? ModBlocks.GRASS_LIGHT : ModBlocks.GRASS_DARK).get().defaultBlockState();
                } else {
                    ground = ModBlocks.STAND_STEP.get().defaultBlockState();
                }
                level.setBlock(p, ground, FLAGS);
            }
        }

        // goals
        for (int s = -1; s <= 1; s += 2) {
            int gx = cx + Pitch.HL * s;
            for (int dz = -Pitch.GOAL_HALF; dz <= Pitch.GOAL_HALF; dz++) {
                set(level, p.set(gx, y + 4, cz + dz), ModBlocks.GOAL_POST.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z));
                for (int d = 1; d <= 2; d++) set(level, p.set(gx + d * s, y + 4, cz + dz), ModBlocks.GOAL_NET_ROOF.get().defaultBlockState());
                for (int h = 1; h <= 3; h++) set(level, p.set(gx + 2 * s, y + h, cz + dz), net(level, gx + 2 * s, y + h, cz + dz));
            }
            for (int h = 1; h <= 3; h++) {
                for (int side = -1; side <= 1; side += 2) {
                    set(level, p.set(gx, y + h, cz + Pitch.GOAL_HALF * side), ModBlocks.GOAL_POST.get().defaultBlockState());
                    set(level, p.set(gx + s, y + h, cz + Pitch.GOAL_HALF * side), ModBlocks.GOAL_NET.get().defaultBlockState());
                }
            }
            // second pass so the panes pick up their neighbours
            for (int dz = -Pitch.GOAL_HALF; dz <= Pitch.GOAL_HALF; dz++) {
                for (int d = 1; d <= 2; d++) {
                    for (int h = 1; h <= 3; h++) {
                        p.set(gx + d * s, y + h, cz + dz);
                        if (level.getBlockState(p).is(ModBlocks.GOAL_NET.get())) set(level, p, net(level, p.getX(), p.getY(), p.getZ()));
                    }
                }
            }
        }

        // corner flags
        for (int sx = -1; sx <= 1; sx += 2)
            for (int sz = -1; sz <= 1; sz += 2)
                set(level, p.set(cx + Pitch.HL * sx, y + 1, cz + Pitch.HW * sz), ModBlocks.CORNER_FLAG.get().defaultBlockState());

        // long side stands
        for (int t = -1; t <= 1; t += 2) {
            Direction face = t > 0 ? Direction.NORTH : Direction.SOUTH;
            for (int r = 0; r < ROWS; r++) {
                int z = cz + t * (GRASS_Z + 2 + r);
                for (int dx = -GRASS_X; dx <= GRASS_X; dx++) {
                    int x = cx + dx;
                    for (int h = 1; h <= r; h++) set(level, p.set(x, y + h, z), ModBlocks.STAND_STEP.get().defaultBlockState());
                    if (Math.floorMod(dx, 13) == 0) continue; // aisle
                    Block seat = dx < -2 ? ModBlocks.SEAT_RED.get() : dx > 2 ? ModBlocks.SEAT_BLUE.get() : ModBlocks.SEAT_WHITE.get();
                    if (r == ROWS - 1 && Math.abs(dx) % 7 == 3) seat = ModBlocks.SEAT_WHITE.get();
                    set(level, p.set(x, y + r + 1, z), seat.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, face));
                }
            }
            int wz = cz + t * (GRASS_Z + 2 + ROWS);
            for (int dx = -GRASS_X - 1; dx <= GRASS_X + 1; dx++)
                for (int h = 1; h <= ROWS + 2; h++) set(level, p.set(cx + dx, y + h, wz), ModBlocks.STAND_STEP.get().defaultBlockState());
        }
        // end stands behind the goals
        for (int s = -1; s <= 1; s += 2) {
            Direction face = s > 0 ? Direction.WEST : Direction.EAST;
            Block seat = s < 0 ? ModBlocks.SEAT_RED.get() : ModBlocks.SEAT_BLUE.get();
            for (int r = 0; r < ROWS; r++) {
                int x = cx + s * (GRASS_X + 2 + r);
                for (int dz = -GRASS_Z + 2; dz <= GRASS_Z - 2; dz++) {
                    int z = cz + dz;
                    for (int h = 1; h <= r; h++) set(level, p.set(x, y + h, z), ModBlocks.STAND_STEP.get().defaultBlockState());
                    if (Math.floorMod(dz, 11) == 0) continue;
                    set(level, p.set(x, y + r + 1, z), seat.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, face));
                }
            }
            int wx = cx + s * (GRASS_X + 2 + ROWS);
            for (int dz = -GRASS_Z + 1; dz <= GRASS_Z - 1; dz++)
                for (int h = 1; h <= ROWS + 2; h++) set(level, p.set(wx, y + h, cz + dz), ModBlocks.STAND_STEP.get().defaultBlockState());
        }

        // floodlight towers in the four corners
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                int tx = cx + sx * (GRASS_X + 3), tz = cz + sz * (GRASS_Z + 3);
                for (int h = 1; h <= 18; h++) set(level, p.set(tx, y + h, tz), Blocks.POLISHED_ANDESITE.defaultBlockState());
                Direction face = sz > 0 ? Direction.NORTH : Direction.SOUTH;
                for (int i = -1; i <= 1; i++)
                    for (int h = 19; h <= 20; h++)
                        set(level, p.set(tx + i, y + h, tz), ModBlocks.FLOODLIGHT.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, face));
            }
        }

        // invisible light grid so night matches stay bright and mob free
        BlockState light = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15);
        for (int dx = -GRASS_X; dx <= GRASS_X; dx += 6)
            for (int dz = -GRASS_Z; dz <= GRASS_Z; dz += 6) {
                p.set(cx + dx, y + 5, cz + dz);
                if (level.getBlockState(p).isAir()) set(level, p, light);
            }

        Pitch pitch = new Pitch(level.dimension().location().toString(), cx, y, cz);
        PitchData d = PitchData.get(level.getServer());
        d.pitch = pitch;
        d.setDirty();
        return pitch;
    }

    private static BlockState net(ServerLevel level, int x, int y, int z) {
        BlockState s = ModBlocks.GOAL_NET.get().getStateForPlacement(new DirectionalPlaceContext(level, new BlockPos(x, y, z),
                Direction.DOWN, ItemStack.EMPTY, Direction.UP));
        return s != null ? s : ModBlocks.GOAL_NET.get().defaultBlockState();
    }

    private static void set(ServerLevel level, BlockPos pos, BlockState s) { level.setBlock(pos, s, FLAGS); }

    /** pitch markings in block offsets from the centre spot */
    static boolean isLine(int dx, int dz) {
        int ax = Math.abs(dx), az = Math.abs(dz);
        if (ax > Pitch.HL || az > Pitch.HW) return false;
        if (az == Pitch.HW || ax == Pitch.HL) return true;          // touch & goal lines
        if (dx == 0) return true;                                    // halfway line
        double r = Math.sqrt(dx * dx + dz * dz);
        if (Math.abs(r - 7.0) < 0.5) return true;                    // centre circle
        int bx = Pitch.HL - Pitch.BOX_DEPTH;                         // penalty area
        if (ax == bx && az <= Pitch.BOX_HALF) return true;
        if (az == Pitch.BOX_HALF && ax >= bx) return true;
        int gx = Pitch.HL - 4;                                       // goal area
        if (ax == gx && az <= 7) return true;
        if (az == 7 && ax >= gx) return true;
        int px = Pitch.HL - 8;                                       // penalty spot + arc
        if (ax == px && dz == 0) return true;
        double pr = Math.sqrt((ax - px) * (ax - px) + dz * dz);
        if (ax < bx && Math.abs(pr - 7.0) < 0.5) return true;
        double cr = Math.sqrt((Pitch.HL - ax) * (Pitch.HL - ax) + (Pitch.HW - az) * (Pitch.HW - az)); // corner arcs
        return cr >= 1.0 && cr < 2.0;
    }
}
