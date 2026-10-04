package com.rabona.arena.game;

import com.rabona.arena.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Tam bir stadyum insa eder: cizgili cim, saha cizgileri, ceza sahalari, kaleler (direk + file),
 * korner bayraklari, LED reklam panolari, renkli tribunler ve isik kuleleri.
 * Islem tick'lere bolunur (sunucu takilmaz).
 */
public final class StadiumBuilder {
    private StadiumBuilder() {}

    private record Put(BlockPos pos, BlockState state) {}

    public static void build(ServerLevel level, BlockPos clicked, boolean alongX, ServerPlayer by) {
        BlockPos origin = clicked;
        Pitch p = new Pitch(origin, alongX);
        List<Put> jobs = new ArrayList<>();
        int L = Pitch.HALF_LEN, W = Pitch.HALF_WID;
        int outerA = L + 13, outerB = W + 12;

        BlockState grass = ModBlocks.PITCH_GRASS.get().defaultBlockState();
        BlockState grassDark = ModBlocks.PITCH_GRASS_DARK.get().defaultBlockState();
        BlockState line = ModBlocks.PITCH_LINE.get().defaultBlockState();
        BlockState concrete = ModBlocks.STADIUM_CONCRETE.get().defaultBlockState();
        BlockState step = ModBlocks.STADIUM_STEP.get().defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

        // 1) temel + temizlik
        for (int a = -outerA; a <= outerA; a++) {
            for (int b = -outerB; b <= outerB; b++) {
                boolean field = Math.abs(a) <= L + 6 && Math.abs(b) <= W + 4;
                for (int dy = -2; dy <= -1; dy++) jobs.add(new Put(p.block(a, b, dy), field ? Blocks.DIRT.defaultBlockState() : concrete));
                BlockState top;
                if (field) {
                    int stripe = Math.floorDiv(a + L, 5);
                    top = stripe % 2 == 0 ? grass : grassDark;
                    if (isLine(a, b)) top = line;
                } else {
                    top = concrete;
                }
                jobs.add(new Put(p.block(a, b, 0), top));
                for (int dy = 1; dy <= 16; dy++) jobs.add(new Put(p.block(a, b, dy), air));
            }
        }

        // 2) kaleler
        for (int side : new int[]{-1, 1}) goal(jobs, p, side);

        // 3) korner bayraklari
        for (int sa : new int[]{-1, 1})
            for (int sb : new int[]{-1, 1}) jobs.add(new Put(p.block(sa * L, sb * W, 1), ModBlocks.CORNER_FLAG.get().defaultBlockState()));

        // 4) LED panolar (yan cizgiler ve kale arkalari)
        for (int a = -L; a <= L; a++) {
            if (a == 0) continue;
            jobs.add(new Put(p.block(a, -(W + 3), 1), facing(ModBlocks.LED_BOARD.get(), p, 0, 1)));
            jobs.add(new Put(p.block(a, W + 3, 1), facing(ModBlocks.LED_BOARD.get(), p, 0, -1)));
        }
        for (int b = -W; b <= W; b++) {
            if (Math.abs(b) <= 6) continue;
            jobs.add(new Put(p.block(-(L + 5), b, 1), facing(ModBlocks.LED_BOARD.get(), p, 1, 0)));
            jobs.add(new Put(p.block(L + 5, b, 1), facing(ModBlocks.LED_BOARD.get(), p, -1, 0)));
        }

        // 5) tribunler: yanlar
        for (int row = 0; row < 6; row++) {
            int y = 1 + row;
            for (int a = -(L + 4); a <= L + 4; a++) {
                for (int sb : new int[]{-1, 1}) {
                    int b = sb * (W + 5 + row);
                    for (int dy = 1; dy < y; dy++) jobs.add(new Put(p.block(a, b, dy), step));
                    jobs.add(new Put(p.block(a, b, y), seatFor(p, a, row, 0, -sb)));
                }
            }
            // kale arkalari
            for (int b = -(W + 4); b <= W + 4; b++) {
                for (int sa : new int[]{-1, 1}) {
                    int a = sa * (L + 7 + row);
                    for (int dy = 1; dy < y; dy++) jobs.add(new Put(p.block(a, b, dy), step));
                    jobs.add(new Put(p.block(a, b, y), seatFor(p, sa * 50, row, -sa, 0)));
                }
            }
        }
        // ust korkuluk
        for (int a = -(L + 4); a <= L + 4; a++)
            for (int sb : new int[]{-1, 1}) jobs.add(new Put(p.block(a, sb * (W + 11), 7), Blocks.IRON_BARS.defaultBlockState()));
        for (int b = -(W + 4); b <= W + 4; b++)
            for (int sa : new int[]{-1, 1}) jobs.add(new Put(p.block(sa * (L + 13), b, 7), Blocks.IRON_BARS.defaultBlockState()));

        // 6) isik kuleleri
        for (int sa : new int[]{-1, 1})
            for (int sb : new int[]{-1, 1}) tower(jobs, p, sa * (L + 10), sb * (W + 9), sa, sb);

        // 7) gorunmez isik (gece maci)
        BlockState light = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15);
        for (int a = -L; a <= L; a += 7)
            for (int b = -W; b <= W; b += 7) jobs.add(new Put(p.block(a, b, 9), light));

        Match.get(level.getServer()).setPitch(p);
        if (by != null) by.displayClientMessage(Component.translatable("msg.rabonaarena.building", jobs.size()).withStyle(ChatFormatting.GREEN), false);
        runJobs(level, jobs, 0, by);
    }

    private static void runJobs(ServerLevel level, List<Put> jobs, int from, ServerPlayer by) {
        int to = Math.min(jobs.size(), from + 6000);
        for (int i = from; i < to; i++) {
            Put j = jobs.get(i);
            level.setBlock(j.pos(), j.state(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
        if (to < jobs.size()) {
            Scheduler.later(1, () -> runJobs(level, jobs, to, by));
        } else if (by != null) {
            by.displayClientMessage(Component.translatable("msg.rabonaarena.built").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        }
    }

    /** Saha cizgisi mi? */
    static boolean isLine(int a, int b) {
        int L = Pitch.HALF_LEN, W = Pitch.HALF_WID;
        int aa = Math.abs(a), ab = Math.abs(b);
        if (aa > L || ab > W) return false;
        if (aa == L || ab == W) return true;
        if (a == 0) return true;
        double r = Math.sqrt(a * a + b * b);
        if (Math.abs(r - Pitch.CIRCLE) < 0.5) return true;
        if (r < 0.8) return true; // orta nokta
        // ceza sahasi
        int boxIn = L - Pitch.BOX_DEPTH;
        if (aa == boxIn && ab <= Pitch.BOX_HALF) return true;
        if (ab == Pitch.BOX_HALF && aa >= boxIn) return true;
        int smallIn = L - Pitch.SMALL_DEPTH;
        if (aa == smallIn && ab <= Pitch.SMALL_HALF) return true;
        if (ab == Pitch.SMALL_HALF && aa >= smallIn) return true;
        // penalti noktasi
        int spot = L - Pitch.PEN_SPOT;
        if (aa == spot && b == 0) return true;
        // ceza yayi
        double dr = Math.sqrt((aa - spot) * (aa - spot) + b * b);
        if (aa < boxIn && Math.abs(dr - 7) < 0.5) return true;
        // korner yaylari
        double cr = Math.sqrt((aa - L) * (aa - L) + (ab - W) * (ab - W));
        return Math.abs(cr - 1.5) < 0.5;
    }

    private static void goal(List<Put> jobs, Pitch p, int side) {
        int L = Pitch.HALF_LEN;
        int halfW = 4, h = 3, depth = 3;
        Direction.Axis bAxis = p.alongX ? Direction.Axis.Z : Direction.Axis.X;
        BlockState postV = ModBlocks.GOAL_POST.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        BlockState postB = ModBlocks.GOAL_POST.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, bAxis);
        BlockState net = ModBlocks.GOAL_NET.get().defaultBlockState();
        int lineA = side * L;
        // direkler + ust direk
        for (int y = 1; y <= h; y++) {
            jobs.add(new Put(p.block(lineA, -halfW, y), postV));
            jobs.add(new Put(p.block(lineA, halfW, y), postV));
        }
        for (int b = -halfW; b <= halfW; b++) jobs.add(new Put(p.block(lineA, b, h + 1), postB));
        // file: arka, yanlar, cati
        for (int d = 1; d <= depth; d++) {
            int a = lineA + side * d;
            for (int y = 1; y <= h + 1; y++) {
                jobs.add(new Put(p.block(a, -halfW, y), net));
                jobs.add(new Put(p.block(a, halfW, y), net));
            }
            for (int b = -halfW + 1; b <= halfW - 1; b++) jobs.add(new Put(p.block(a, b, h + 1), net));
        }
        for (int y = 1; y <= h; y++)
            for (int b = -halfW + 1; b <= halfW - 1; b++) jobs.add(new Put(p.block(lineA + side * depth, b, y), net));
        // zemindeki arka cerceve
        for (int d = 1; d <= depth; d++) {
            jobs.add(new Put(p.block(lineA + side * d, -halfW, 0), ModBlocks.PITCH_LINE.get().defaultBlockState()));
        }
        jobs.add(new Put(p.block(lineA, 0, 0), ModBlocks.PITCH_LINE.get().defaultBlockState()));
    }

    private static BlockState seatFor(Pitch p, int a, int row, int fa, int fb) {
        Block b;
        if (row == 5) b = ModBlocks.SEAT_GOLD.get();
        else if (Math.abs(a) <= 3) b = ModBlocks.SEAT_WHITE.get();
        else if (a < 0) b = (row % 2 == 0 || Math.floorMod(a, 7) != 0) ? ModBlocks.SEAT_RED.get() : ModBlocks.SEAT_WHITE.get();
        else b = (row % 2 == 0 || Math.floorMod(a, 7) != 0) ? ModBlocks.SEAT_BLUE.get() : ModBlocks.SEAT_WHITE.get();
        return facing(b, p, fa, fb);
    }

    /** yerel (da, db) yonune bakan durum. */
    private static BlockState facing(Block b, Pitch p, int da, int db) {
        BlockPos o = p.block(0, 0, 0);
        BlockPos t = p.block(da, db, 0);
        Direction d = Direction.fromDelta(t.getX() - o.getX(), 0, t.getZ() - o.getZ());
        if (d == null || d.getAxis() == Direction.Axis.Y) d = Direction.NORTH;
        return b.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, d);
    }

    private static void tower(List<Put> jobs, Pitch p, int a, int b, int sa, int sb) {
        BlockState pole = Blocks.POLISHED_DEEPSLATE_WALL.defaultBlockState();
        for (int y = 1; y <= 18; y++) jobs.add(new Put(p.block(a, b, y), y < 3 ? Blocks.POLISHED_DEEPSLATE.defaultBlockState() : pole));
        BlockState lamp = facing(ModBlocks.FLOODLIGHT.get(), p, -sa, 0);
        BlockState lamp2 = facing(ModBlocks.FLOODLIGHT.get(), p, 0, -sb);
        for (int dy = 18; dy <= 20; dy++) {
            for (int d = -1; d <= 1; d++) {
                jobs.add(new Put(p.block(a - sa, b + d, dy), lamp));
                jobs.add(new Put(p.block(a + d, b - sb, dy), lamp2));
            }
        }
        jobs.add(new Put(p.block(a, b, 19), Blocks.POLISHED_DEEPSLATE.defaultBlockState()));
    }
}
