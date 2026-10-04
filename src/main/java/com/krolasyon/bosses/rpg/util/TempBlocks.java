package com.krolasyon.bosses.rpg.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/** Blocks placed by spells / abilities (webs, ice walls, roots...) that turn back into what was there. */
public final class TempBlocks {
    private TempBlocks() {}

    private record Entry(ServerLevel level, BlockPos pos, BlockState placed, BlockState original, long expire) {}

    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static final Set<Long> TAKEN = new HashSet<>();

    /** places a block only into air / replaceable plants, restoring it after {@code ticks} */
    public static boolean place(Level level, BlockPos pos, BlockState state, int ticks) {
        if (!(level instanceof ServerLevel sl)) return false;
        BlockState cur = level.getBlockState(pos);
        if (!(cur.isAir() || cur.canBeReplaced()) || !cur.getFluidState().isEmpty() && !cur.getFluidState().isSource()) return false;
        if (cur.is(Blocks.BEDROCK) || TAKEN.contains(pos.asLong())) return false;
        level.setBlock(pos, state, 3);
        ENTRIES.add(new Entry(sl, pos.immutable(), state, cur.isAir() ? Blocks.AIR.defaultBlockState() : cur, sl.getGameTime() + ticks));
        TAKEN.add(pos.asLong());
        return true;
    }

    public static void tick(ServerLevel level) {
        if (ENTRIES.isEmpty()) return;
        long now = level.getGameTime();
        Iterator<Entry> it = ENTRIES.iterator();
        while (it.hasNext()) {
            Entry e = it.next();
            if (e.level != level || e.expire > now) continue;
            if (level.isLoaded(e.pos) && level.getBlockState(e.pos).is(e.placed.getBlock())) level.setBlock(e.pos, e.original, 3);
            TAKEN.remove(e.pos.asLong());
            it.remove();
        }
    }

    /** restores everything immediately (server stopping) */
    public static void flush() {
        for (Entry e : ENTRIES) {
            if (e.level.isLoaded(e.pos) && e.level.getBlockState(e.pos).is(e.placed.getBlock())) e.level.setBlock(e.pos, e.original, 3);
        }
        ENTRIES.clear();
        TAKEN.clear();
    }
}
