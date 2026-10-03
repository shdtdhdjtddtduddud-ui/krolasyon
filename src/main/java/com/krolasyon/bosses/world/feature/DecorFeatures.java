package com.krolasyon.bosses.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;

/** Scenery features for the Azrakor biomes. Each one takes a block state provider, so a single class serves every kingdom palette. */
public final class DecorFeatures {
    private DecorFeatures() {}

    private static boolean solidGround(WorldGenLevel level, BlockPos pos) {
        BlockState b = level.getBlockState(pos.below());
        return !b.isAir() && b.getFluidState().isEmpty() && b.isSolidRender(level, pos.below()) && level.getBlockState(pos).isAir();
    }

    private static void put(WorldGenLevel level, BlockPos p, BlockState s) {
        BlockState cur = level.getBlockState(p);
        if (cur.isAir() || cur.canBeReplaced()) level.setBlock(p, s, 2);
    }

    // ------------------------------------------------------------------ tapering rock / bone / crystal spire
    public static class Spire extends Feature<SimpleBlockConfiguration> {
        public Spire(Codec<SimpleBlockConfiguration> codec) { super(codec); }

        @Override
        public boolean place(FeaturePlaceContext<SimpleBlockConfiguration> ctx) {
            WorldGenLevel level = ctx.level();
            BlockPos o = ctx.origin();
            RandomSource r = ctx.random();
            if (!solidGround(level, o)) return false;
            int h = 8 + r.nextInt(18);
            double base = 1.8 + r.nextDouble() * 2.2;
            double lx = (r.nextDouble() - 0.5) * 0.35, lz = (r.nextDouble() - 0.5) * 0.35;
            for (int y = -2; y < h; y++) {
                double t = Math.max(0, y) / (double) h;
                double rad = base * (1.0 - t) * (1.0 - t * 0.3) + 0.4;
                int cx = Mth.floor(lx * y), cz = Mth.floor(lz * y);
                int ri = (int) Math.ceil(rad);
                for (int dx = -ri; dx <= ri; dx++) {
                    for (int dz = -ri; dz <= ri; dz++) {
                        double d = Math.sqrt(dx * dx + dz * dz) + (r.nextDouble() - 0.5) * 0.5;
                        if (d <= rad) put(level, o.offset(cx + dx, y, cz + dz), ctx.config().toPlace().getState(r, o.offset(cx + dx, y, cz + dz)));
                    }
                }
            }
            return true;
        }
    }

    // ------------------------------------------------------------------ bare twisted tree with branches
    public static class DeadTree extends Feature<SimpleBlockConfiguration> {
        public DeadTree(Codec<SimpleBlockConfiguration> codec) { super(codec); }

        @Override
        public boolean place(FeaturePlaceContext<SimpleBlockConfiguration> ctx) {
            WorldGenLevel level = ctx.level();
            BlockPos o = ctx.origin();
            RandomSource r = ctx.random();
            if (!solidGround(level, o)) return false;
            int h = 7 + r.nextInt(8);
            double x = 0, z = 0;
            double dx = (r.nextDouble() - 0.5) * 0.4, dz = (r.nextDouble() - 0.5) * 0.4;
            for (int y = 0; y < h; y++) {
                x += dx; z += dz;
                if (r.nextInt(4) == 0) { dx += (r.nextDouble() - 0.5) * 0.3; dz += (r.nextDouble() - 0.5) * 0.3; }
                BlockPos p = o.offset(Mth.floor(x), y, Mth.floor(z));
                put(level, p, ctx.config().toPlace().getState(r, p));
                if (y < 2) put(level, p.relative(Direction.Plane.HORIZONTAL.getRandomDirection(r)), ctx.config().toPlace().getState(r, p));
                if (y > h / 3 && r.nextInt(3) == 0) branch(level, ctx, p, r, 3 + r.nextInt(4));
            }
            branch(level, ctx, o.offset(Mth.floor(x), h, Mth.floor(z)), r, 3);
            return true;
        }

        private void branch(WorldGenLevel level, FeaturePlaceContext<SimpleBlockConfiguration> ctx, BlockPos from, RandomSource r, int len) {
            Direction d = Direction.Plane.HORIZONTAL.getRandomDirection(r);
            BlockPos p = from;
            for (int i = 0; i < len; i++) {
                p = p.relative(d);
                if (r.nextInt(2) == 0) p = p.above();
                put(level, p, ctx.config().toPlace().getState(r, p));
            }
        }
    }

    // ------------------------------------------------------------------ half-buried giant ribcage
    public static class Ribs extends Feature<SimpleBlockConfiguration> {
        public Ribs(Codec<SimpleBlockConfiguration> codec) { super(codec); }

        @Override
        public boolean place(FeaturePlaceContext<SimpleBlockConfiguration> ctx) {
            WorldGenLevel level = ctx.level();
            BlockPos o = ctx.origin();
            RandomSource r = ctx.random();
            if (!solidGround(level, o)) return false;
            boolean alongX = r.nextBoolean();
            int n = 4 + r.nextInt(4);
            int rad = 4 + r.nextInt(3);
            for (int k = 0; k < n; k++) {
                int off = k * 2;
                double sc = 1.0 - Math.abs(k - n / 2.0) / (n * 0.8);
                for (int step = 0; step <= 24; step++) {
                    double a = Math.PI * step / 24.0;
                    int side = (int) Math.round(Math.cos(a) * rad * sc);
                    int up = (int) Math.round(Math.sin(a) * rad * sc * 1.1) - 1;
                    BlockPos p = alongX ? o.offset(off, up, side) : o.offset(side, up, off);
                    put(level, p, ctx.config().toPlace().getState(r, p));
                    if (up < 1 && r.nextInt(3) == 0) put(level, p.below(), ctx.config().toPlace().getState(r, p));
                }
            }
            for (int k = 0; k < n * 2; k++) {
                BlockPos p = alongX ? o.offset(k, 0, 0) : o.offset(0, 0, k);
                put(level, p, ctx.config().toPlace().getState(r, p));
            }
            return true;
        }
    }

    // ------------------------------------------------------------------ crumbling walls and pillar stumps
    public static class Ruin extends Feature<SimpleBlockConfiguration> {
        public Ruin(Codec<SimpleBlockConfiguration> codec) { super(codec); }

        @Override
        public boolean place(FeaturePlaceContext<SimpleBlockConfiguration> ctx) {
            WorldGenLevel level = ctx.level();
            BlockPos o = ctx.origin();
            RandomSource r = ctx.random();
            if (!solidGround(level, o)) return false;
            int w = 5 + r.nextInt(5), d = 5 + r.nextInt(5);
            for (int x = 0; x < w; x++) {
                for (int z = 0; z < d; z++) {
                    boolean edge = x == 0 || z == 0 || x == w - 1 || z == d - 1;
                    BlockPos g = o.offset(x, 0, z);
                    if (!level.getBlockState(g.below()).isSolidRender(level, g.below())) continue;
                    if (edge) {
                        int hh = r.nextInt(5);
                        if (r.nextInt(4) == 0) continue;
                        for (int y = 0; y <= hh; y++) put(level, g.above(y), ctx.config().toPlace().getState(r, g.above(y)));
                    } else if (r.nextInt(3) == 0) {
                        put(level, g, ctx.config().toPlace().getState(r, g));
                    }
                }
            }
            for (int k = 0; k < 2; k++) {
                BlockPos g = o.offset(r.nextInt(w), 0, r.nextInt(d));
                int hh = 4 + r.nextInt(4);
                for (int y = 0; y <= hh; y++) put(level, g.above(y), ctx.config().toPlace().getState(r, g.above(y)));
            }
            return true;
        }
    }

    // ------------------------------------------------------------------ cluster of slanted crystal shards
    public static class Crystals extends Feature<SimpleBlockConfiguration> {
        public Crystals(Codec<SimpleBlockConfiguration> codec) { super(codec); }

        @Override
        public boolean place(FeaturePlaceContext<SimpleBlockConfiguration> ctx) {
            WorldGenLevel level = ctx.level();
            BlockPos o = ctx.origin();
            RandomSource r = ctx.random();
            if (!solidGround(level, o)) return false;
            int n = 4 + r.nextInt(5);
            for (int k = 0; k < n; k++) {
                double ax = (r.nextDouble() - 0.5) * 1.3, az = (r.nextDouble() - 0.5) * 1.3;
                int len = 3 + r.nextInt(9);
                BlockPos start = o.offset(r.nextInt(5) - 2, 0, r.nextInt(5) - 2);
                for (int y = 0; y < len; y++) {
                    BlockPos p = start.offset(Mth.floor(ax * y), y, Mth.floor(az * y));
                    put(level, p, ctx.config().toPlace().getState(r, p));
                    if (y < len / 2) put(level, p.east(), ctx.config().toPlace().getState(r, p));
                }
            }
            return true;
        }
    }
}
