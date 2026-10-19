package com.krolasyon.skyislands.world;

import net.minecraft.core.BlockPos;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

import java.util.List;

/**
 * Builds one island into a {@link BlockSink}. Every block is a pure function of the island and its position,
 * so neighbouring chunks agree on the shape without sharing state.
 */
public final class IslandBuilder {
    private IslandBuilder() {}

    private static final List<Block> FLOWERS = List.of(Blocks.DANDELION, Blocks.POPPY, Blocks.CORNFLOWER, Blocks.ALLIUM,
            Blocks.AZURE_BLUET, Blocks.OXEYE_DAISY, Blocks.LILY_OF_THE_VALLEY, Blocks.PINK_TULIP, Blocks.WHITE_TULIP);
    private static final List<ResourceKey<ConfiguredFeature<?, ?>>> TREES = List.of(TreeFeatures.FANCY_OAK, TreeFeatures.OAK,
            TreeFeatures.BIRCH, TreeFeatures.CHERRY, TreeFeatures.AZALEA_TREE, TreeFeatures.FANCY_OAK);

    public static void build(Island is, BlockSink sink, RandomSource random) {
        int reach = (int) Math.ceil(is.radius() * 1.3) + 1;
        int x0 = Math.max(sink.minX(), is.cx() - reach), x1 = Math.min(sink.maxX(), is.cx() + reach);
        int z0 = Math.max(sink.minZ(), is.cz() - reach), z1 = Math.min(sink.maxZ(), is.cz() + reach);
        long s = is.seed();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();

        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                IslandShape.Column c = IslandShape.column(is, x, z);
                if (!c.inside()) continue;
                int lo = Math.max(c.bottom(), sink.minY()), hi = Math.min(c.top(), sink.maxY());
                for (int y = lo; y <= hi; y++) {
                    sink.set(p.set(x, y, z), solid(is, c, x, y, z));
                }
                if (c.pond()) {
                    for (int y = c.top() + 1; y <= c.water(); y++) sink.set(p.set(x, y, z), Blocks.WATER.defaultBlockState());
                    if (Noise.hash(s + 17, x, 0, z) < 0.08) sink.set(p.set(x, c.water() + 1, z), Blocks.LILY_PAD.defaultBlockState());
                } else {
                    decorateTop(is, sink, c, x, z);
                }
                decorateUnder(is, sink, c, x, z);
            }
        }

        if (!is.satellite()) waterfalls(is, sink);
        if (is.giantTree()) {
            GiantTree.build(is, sink);
        } else {
            smallTrees(is, sink, random);
        }
    }

    private static BlockState solid(Island is, IslandShape.Column c, int x, int y, int z) {
        long s = is.seed();
        int fromTop = c.top() - y;
        int fromBottom = y - c.bottom();
        double h = Noise.hash(s + 5, x, y, z);
        if (c.pond()) {
            if (fromTop == 0) return h < 0.45 ? Blocks.SAND.defaultBlockState() : h < 0.8 ? Blocks.CLAY.defaultBlockState() : Blocks.GRAVEL.defaultBlockState();
            if (fromTop <= 2) return Blocks.DIRT.defaultBlockState();
        }
        if (fromTop == 0) return Blocks.GRASS_BLOCK.defaultBlockState();
        if (fromTop <= 3) {
            // yeşil kaplı yamaçlar
            if (c.d() > 0.86 && h < 0.5) return Blocks.MOSS_BLOCK.defaultBlockState();
            return h < 0.08 ? Blocks.ROOTED_DIRT.defaultBlockState() : Blocks.DIRT.defaultBlockState();
        }
        if (c.d() > 0.9 && fromTop <= 7 && h < 0.35) return Blocks.MOSS_BLOCK.defaultBlockState();
        if (fromBottom <= 1 && Noise.value2(s + 9, x * 0.3, z * 0.3) > 0.55) return Blocks.MOSS_BLOCK.defaultBlockState();
        if (fromTop > 4) {
            double o = Noise.hash(s + 6, x, y, z);
            if (o < 0.012) return Blocks.COAL_ORE.defaultBlockState();
            if (o < 0.020) return Blocks.IRON_ORE.defaultBlockState();
            if (o < 0.026) return Blocks.COPPER_ORE.defaultBlockState();
            if (o < 0.028) return Blocks.GOLD_ORE.defaultBlockState();
            if (o < 0.0288) return Blocks.EMERALD_ORE.defaultBlockState();
            if (o < 0.0294) return Blocks.DIAMOND_ORE.defaultBlockState();
            if (o < 0.0300) return Blocks.AMETHYST_BLOCK.defaultBlockState();
        }
        double n = Noise.fbm3(s + 7, x * 0.11, y * 0.11, z * 0.11, 2);
        if (fromBottom <= 3 && h < 0.5) return h < 0.25 ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState();
        if (n < 0.34) return Blocks.ANDESITE.defaultBlockState();
        if (n > 0.70) return Blocks.TUFF.defaultBlockState();
        if (n > 0.66) return Blocks.CALCITE.defaultBlockState();
        return Blocks.STONE.defaultBlockState();
    }

    private static void decorateTop(Island is, BlockSink sink, IslandShape.Column c, int x, int z) {
        BlockPos above = new BlockPos(x, c.top() + 1, z);
        if (!sink.isAir(above)) return;
        double r = Noise.hash(is.seed() + 11, x, 0, z);
        if (r < 0.22) {
            sink.set(above, Blocks.GRASS.defaultBlockState());
        } else if (r < 0.27 && sink.isAir(above.above())) {
            sink.set(above, Blocks.TALL_GRASS.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER));
            sink.set(above.above(), Blocks.TALL_GRASS.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));
        } else if (r < 0.315) {
            // çiçek tarlaları: aynı türden kümeler
            int idx = (int) (Noise.value2(is.seed() + 12, x * 0.15, z * 0.15) * FLOWERS.size() * 0.999);
            sink.set(above, FLOWERS.get(Math.max(0, Math.min(FLOWERS.size() - 1, idx))).defaultBlockState());
        } else if (r < 0.319) {
            sink.set(above, (r < 0.317 ? Blocks.AZALEA : Blocks.FLOWERING_AZALEA).defaultBlockState());
        }
    }

    private static void decorateUnder(Island is, BlockSink sink, IslandShape.Column c, int x, int z) {
        int y = c.bottom() - 1;
        if (y <= sink.minY() + 2) return;
        BlockPos below = new BlockPos(x, y, z);
        if (!sink.isAir(below)) return;
        double r = Noise.hash(is.seed() + 13, x, 1, z);
        if (r < 0.035) {
            // parlayan meyveli sarkan sarmaşıklar
            int len = 2 + (int) (Noise.hash(is.seed() + 14, x, 2, z) * 12);
            for (int i = 0; i < len; i++) {
                BlockPos v = below.below(i);
                if (!sink.isAir(v)) break;
                boolean berries = Noise.hash(is.seed() + 15, x, v.getY(), z) < 0.3;
                boolean last = i == len - 1 || !sink.isAir(v.below());
                BlockState st = (last ? Blocks.CAVE_VINES : Blocks.CAVE_VINES_PLANT).defaultBlockState().setValue(CaveVines.BERRIES, berries);
                sink.set(v, st);
                if (last) break;
            }
        } else if (r < 0.05) {
            sink.set(below, Blocks.HANGING_ROOTS.defaultBlockState());
        } else if (r < 0.053) {
            sink.set(below, Blocks.SPORE_BLOSSOM.defaultBlockState());
        }
    }

    private static void waterfalls(Island is, BlockSink sink) {
        if (is.radius() < 10) return;
        int count = 1 + is.radius() / 9;
        for (int k = 0; k < count; k++) {
            double ang = Noise.hash(is.seed() + 21, k, 0, 0) * Math.PI * 2;
            double cos = Math.cos(ang), sin = Math.sin(ang);
            int lx = 0, lz = 0;
            boolean found = false;
            IslandShape.Column last = null;
            for (double t = 0; t < is.radius() * 1.4; t += 0.5) {
                int x = is.cx() + (int) Math.round(cos * t), z = is.cz() + (int) Math.round(sin * t);
                IslandShape.Column c = IslandShape.column(is, x, z);
                if (!c.inside()) break;
                if (!c.pond()) {
                    lx = x;
                    lz = z;
                    last = c;
                    found = true;
                }
            }
            if (!found) continue;
            // dere: kenardan içeri doğru 3 blokluk kanal, kenarda 1-2 blok genişlik
            int width = 1 + (int) (Noise.hash(is.seed() + 22, k, 0, 0) * 2);
            for (int step = 0; step < 3; step++) {
                int x = lx - (int) Math.round(cos * step), z = lz - (int) Math.round(sin * step);
                for (int w = 0; w < width; w++) {
                    int wx = x + (int) Math.round(-sin * w), wz = z + (int) Math.round(cos * w);
                    IslandShape.Column c = IslandShape.column(is, wx, wz);
                    if (!c.inside() || c.pond()) continue;
                    BlockPos wp = new BlockPos(wx, c.top(), wz);
                    if (!sink.contains(wp)) continue;
                    sink.set(wp.above(), Blocks.AIR.defaultBlockState());
                    sink.water(wp);
                }
            }
        }
    }

    private static void smallTrees(Island is, BlockSink sink, RandomSource random) {
        int count = is.satellite() ? (is.radius() >= 4 && Noise.hash(is.seed(), 3, 3, 3) < 0.6 ? 1 : 0) : Math.max(1, is.radius() / 6);
        for (int i = 0; i < count; i++) {
            double ang = Noise.hash(is.seed() + 31, i, 0, 0) * Math.PI * 2;
            double rr = is.radius() * 0.55 * Math.sqrt(Noise.hash(is.seed() + 32, i, 0, 0));
            int x = is.cx() + (int) Math.round(Math.cos(ang) * rr), z = is.cz() + (int) Math.round(Math.sin(ang) * rr);
            IslandShape.Column c = IslandShape.column(is, x, z);
            if (!c.inside() || c.pond() || c.d() > 0.7) continue;
            BlockPos pos = new BlockPos(x, c.top() + 1, z);
            if (!sink.contains(pos)) continue;
            sink.set(pos, Blocks.AIR.defaultBlockState());
            int t = (int) (Noise.hash(is.seed() + 33, i, 0, 0) * TREES.size() * 0.999);
            sink.tree(TREES.get(t), pos, random);
        }
    }
}
