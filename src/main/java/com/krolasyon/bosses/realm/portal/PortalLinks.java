package com.krolasyon.bosses.realm.portal;

import com.krolasyon.bosses.realm.registry.RealmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Remembers every realm portal of one dimension so travellers come out of an existing gate. */
public class PortalLinks extends SavedData {
    private final List<BlockPos> portals = new ArrayList<>();

    public static PortalLinks get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(PortalLinks::load, PortalLinks::new, "krolasyon_portals");
    }

    public static PortalLinks load(CompoundTag tag) {
        PortalLinks l = new PortalLinks();
        for (long v : tag.getLongArray("Portals")) l.portals.add(BlockPos.of(v));
        return l;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLongArray("Portals", portals.stream().mapToLong(BlockPos::asLong).toArray());
        return tag;
    }

    public void add(BlockPos p) {
        for (BlockPos q : portals) if (q.distManhattan(p) <= 6) return;
        portals.add(p.immutable());
        setDirty();
    }

    @Nullable
    public BlockPos nearest(ServerLevel level, BlockPos at, int radius, boolean validate) {
        List<BlockPos> sorted = new ArrayList<>(portals);
        sorted.sort(Comparator.comparingDouble(p -> horiz(p, at)));
        for (BlockPos p : sorted) {
            if (horiz(p, at) > (double) radius * radius) break;
            if (!validate) return p;
            if (level.getBlockState(p).is(RealmBlocks.REALM_PORTAL.get())) return p;
            portals.remove(p);
            setDirty();
        }
        return null;
    }

    private static double horiz(BlockPos a, BlockPos b) {
        double dx = a.getX() - b.getX(), dz = a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }
}
