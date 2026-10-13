package com.krolasyon.beyblade.world;

import com.krolasyon.beyblade.LibraBeyblade;
import com.krolasyon.beyblade.entity.StadiumEntity;
import com.krolasyon.beyblade.registry.ModEntities;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Stadiums that appear on their own in the overworld: now and then a freshly generated chunk on fairly flat land is
 * picked, the ground around it is levelled and a stadium (with a supply chest by the blue launch pad) is set up.
 * Sites are kept at least {@link #SPACING} blocks apart.
 */
@Mod.EventBusSubscriber(modid = LibraBeyblade.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class StadiumWorldGen {
    private StadiumWorldGen() {}

    private static final Logger LOG = LogUtils.getLogger();
    public static final int CHANCE = 260, SPACING = 600, RADIUS = 19;
    private static final ConcurrentLinkedQueue<long[]> QUEUE = new ConcurrentLinkedQueue<>();
    private static final ResourceLocation CHEST_LOOT = new ResourceLocation(LibraBeyblade.MODID, "chests/stadium");

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!event.isNewChunk() || !(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        ChunkPos cp = event.getChunk().getPos();
        RandomSource r = RandomSource.create(level.getSeed() ^ (cp.x * 341873128712L + cp.z * 132897987541L));
        if (r.nextInt(CHANCE) != 0) return;
        if (Math.abs(cp.x) < 4 && Math.abs(cp.z) < 4) return;
        QUEUE.add(new long[]{cp.getMiddleBlockX(), cp.getMiddleBlockZ(), 0});
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        if (QUEUE.isEmpty() || level.getGameTime() % 20 != 0) return;
        Iterator<long[]> it = QUEUE.iterator();
        while (it.hasNext()) {
            long[] q = it.next();
            int x = (int) q[0], z = (int) q[1];
            if (++q[2] > 60) { it.remove(); continue; }
            if (!level.hasChunksAt(x - RADIUS - 2, z - RADIUS - 2, x + RADIUS + 2, z + RADIUS + 2)) continue;
            it.remove();
            Sites sites = level.getDataStorage().computeIfAbsent(Sites::load, Sites::new, LibraBeyblade.MODID + "_stadiums");
            if (sites.near(x, z)) continue;
            if (build(level, x, z)) {
                sites.add(x, z);
                LOG.info("[librabeyblade] a beyblade stadium appeared at {} {}", x, z);
            }
            break;
        }
    }

    private static boolean build(ServerLevel level, int x, int z) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (y <= level.getSeaLevel() || y > level.getMaxBuildHeight() - 20) return false;
        // reject water, steep or very uneven ground
        int lo = y, hi = y;
        for (int dx = -RADIUS; dx <= RADIUS; dx += 3) {
            for (int dz = -RADIUS; dz <= RADIUS; dz += 3) {
                if (dx * dx + dz * dz > RADIUS * RADIUS) continue;
                int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x + dx, z + dz);
                if (!level.getFluidState(new BlockPos(x + dx, h - 1, z + dz)).isEmpty()) return false;
                lo = Math.min(lo, h);
                hi = Math.max(hi, h);
            }
        }
        if (hi - lo > 6) return false;
        BlockState top = level.getBlockState(new BlockPos(x, y - 1, z));
        if (!top.is(BlockTags.DIRT) && !top.is(Blocks.SAND) && !top.is(Blocks.SNOW_BLOCK) && !top.is(Blocks.GRASS_BLOCK)) top = Blocks.GRASS_BLOCK.defaultBlockState();
        BlockState fill = top.is(Blocks.SAND) ? Blocks.SANDSTONE.defaultBlockState() : Blocks.DIRT.defaultBlockState();
        // level the ground: clear above, fill below
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > RADIUS + 0.5) continue;
                for (int dy = 0; dy < 14; dy++) {
                    BlockPos p = new BlockPos(x + dx, y + dy, z + dz);
                    if (!level.getBlockState(p).isAir()) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                }
                for (int dy = 1; dy <= 6; dy++) {
                    BlockPos p = new BlockPos(x + dx, y - dy, z + dz);
                    BlockState s = level.getBlockState(p);
                    if (dy == 1) level.setBlock(p, top, 2);
                    else if (s.isAir() || s.canBeReplaced() || !s.getFluidState().isEmpty()) level.setBlock(p, fill, 2);
                }
            }
        }
        // stone launch platforms under both pads
        for (int side = 0; side < 2; side++) {
            int pz = z + (side == 0 ? 17 : -18);
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = 0; dz <= 1; dz++) level.setBlock(new BlockPos(x + dx, y - 1, pz + dz), Blocks.SMOOTH_STONE.defaultBlockState(), 2);
        }
        StadiumEntity st = ModEntities.STADIUM.get().create(level);
        if (st == null) return false;
        st.moveTo(x, y, z, 0F, 0F);
        level.addFreshEntity(st);
        BlockPos chest = new BlockPos(x + 3, y, z + 17);
        level.setBlock(chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, net.minecraft.core.Direction.NORTH), 2);
        if (level.getBlockEntity(chest) instanceof ChestBlockEntity cbe) cbe.setLootTable(CHEST_LOOT, level.random.nextLong());
        return true;
    }

    /** remembered stadium sites, so they stay far apart */
    public static class Sites extends SavedData {
        private final List<long[]> list = new ArrayList<>();

        static Sites load(CompoundTag tag) {
            Sites s = new Sites();
            for (Tag t : tag.getList("Sites", Tag.TAG_LONG)) {
                BlockPos p = BlockPos.of(((LongTag) t).getAsLong());
                s.list.add(new long[]{p.getX(), p.getZ()});
            }
            return s;
        }

        boolean near(int x, int z) {
            for (long[] p : list) if ((p[0] - x) * (p[0] - x) + (p[1] - z) * (p[1] - z) < (long) SPACING * SPACING) return true;
            return false;
        }

        void add(int x, int z) {
            list.add(new long[]{x, z});
            setDirty();
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            ListTag l = new ListTag();
            for (long[] p : list) l.add(LongTag.valueOf(new BlockPos((int) p[0], 0, (int) p[1]).asLong()));
            tag.put("Sites", l);
            return tag;
        }
    }
}
