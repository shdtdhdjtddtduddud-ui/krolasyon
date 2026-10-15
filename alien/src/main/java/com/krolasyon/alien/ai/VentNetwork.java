package com.krolasyon.alien.ai;

import com.krolasyon.alien.block.VentBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.*;

/** Persistent set of every vent grate in a level: the alien's hidden highway. */
public class VentNetwork extends SavedData {
    private static final String NAME = "alienisolation_vents";
    private final Set<Long> vents = new HashSet<>();

    public static VentNetwork get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(VentNetwork::load, VentNetwork::new, NAME);
    }

    public static VentNetwork load(CompoundTag tag) {
        VentNetwork n = new VentNetwork();
        for (long l : tag.getLongArray("Vents")) n.vents.add(l);
        return n;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLongArray("Vents", vents.stream().mapToLong(Long::longValue).toArray());
        return tag;
    }

    public void add(BlockPos p) { if (vents.add(p.asLong())) setDirty(); }

    public void remove(BlockPos p) { if (vents.remove(p.asLong())) setDirty(); }

    public boolean isEmpty() { return vents.isEmpty(); }

    /** vents within [minDist, maxDist] of c, validated against the world (stale entries are dropped). */
    public List<BlockPos> within(ServerLevel level, Vec3 c, double minDist, double maxDist) {
        List<BlockPos> out = new ArrayList<>();
        List<Long> stale = new ArrayList<>();
        double min2 = minDist * minDist, max2 = maxDist * maxDist;
        for (long l : vents) {
            BlockPos p = BlockPos.of(l);
            double d = Vec3.atCenterOf(p).distanceToSqr(c);
            if (d < min2 || d > max2) continue;
            if (!level.isLoaded(p)) continue;
            if (!(level.getBlockState(p).getBlock() instanceof VentBlock)) { stale.add(l); continue; }
            out.add(p);
        }
        if (!stale.isEmpty()) { stale.forEach(vents::remove); setDirty(); }
        return out;
    }

    @Nullable
    public BlockPos nearest(ServerLevel level, Vec3 c, double maxDist, java.util.function.Predicate<BlockPos> ok) {
        BlockPos best = null;
        double bd = Double.MAX_VALUE;
        for (BlockPos p : within(level, c, 0, maxDist)) {
            double d = Vec3.atCenterOf(p).distanceToSqr(c);
            if (d < bd && ok.test(p)) { bd = d; best = p; }
        }
        return best;
    }

    /** where the alien stands when it enters or leaves this vent; null when the vent has no reachable floor. */
    @Nullable
    public static BlockPos exitFor(ServerLevel level, BlockPos vent) {
        // ceiling grate: drop down to the floor below
        if (level.getBlockState(vent.below()).isAir()) {
            BlockPos p = vent.below();
            for (int i = 0; i < 8; i++) {
                if (standable(level, p)) return p;
                p = p.below();
            }
        }
        // wall grate: step out sideways
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos p = vent.relative(d);
            if (standable(level, p)) return p;
            if (standable(level, p.below())) return p.below();
        }
        return null;
    }

    public static boolean standable(ServerLevel level, BlockPos p) {
        return level.getBlockState(p).getCollisionShape(level, p).isEmpty()
                && level.getBlockState(p.above()).getCollisionShape(level, p.above()).isEmpty()
                && level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP);
    }
}
