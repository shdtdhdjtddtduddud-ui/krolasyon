package com.krolasyon.sololeveling.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;

/** World-wide flags of the mod (stored with the overworld). */
public class WorldState extends SavedData {
    public boolean penaltyEnabled = true;
    public boolean overworldPortal;
    public BlockPos overworldPortalPos = BlockPos.ZERO;
    public final Set<String> builtRegions = new HashSet<>();
    public int nextInstance = 1;
    public final java.util.Map<String, Long> regionKills = new java.util.HashMap<>();

    public static WorldState get(MinecraftServer s) {
        return s.overworld().getDataStorage().computeIfAbsent(WorldState::load, WorldState::new, "sololeveling_world");
    }

    static WorldState load(CompoundTag t) {
        WorldState w = new WorldState();
        w.penaltyEnabled = !t.contains("penalty") || t.getBoolean("penalty");
        w.overworldPortal = t.getBoolean("owPortal");
        w.overworldPortalPos = BlockPos.of(t.getLong("owPortalPos"));
        for (Tag x : t.getList("regions", Tag.TAG_STRING)) w.builtRegions.add(x.getAsString());
        w.nextInstance = Math.max(1, t.getInt("nextInstance"));
        CompoundTag rk = t.getCompound("regionKills");
        for (String k : rk.getAllKeys()) w.regionKills.put(k, rk.getLong(k));
        return w;
    }

    @Override
    public CompoundTag save(CompoundTag t) {
        t.putBoolean("penalty", penaltyEnabled);
        t.putBoolean("owPortal", overworldPortal);
        t.putLong("owPortalPos", overworldPortalPos.asLong());
        ListTag l = new ListTag();
        for (String s : builtRegions) l.add(StringTag.valueOf(s));
        t.put("regions", l);
        t.putInt("nextInstance", nextInstance);
        CompoundTag rk = new CompoundTag();
        regionKills.forEach(rk::putLong);
        t.put("regionKills", rk);
        return t;
    }

    /** Builds a small gate-arch at world spawn that leads to Seoul. */
    public void ensureOverworldPortal(MinecraftServer s) {
        if (overworldPortal) return;
        ServerLevel ow = s.overworld();
        BlockPos spawn = ow.getSharedSpawnPos();
        BlockPos base = ow.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn.offset(4, 0, 4));
        Structures.portalArch(ow, base, "seoul_plaza", Blocks.POLISHED_DEEPSLATE.defaultBlockState(), com.krolasyon.sololeveling.registry.ModBlocks.RUNE_BRICKS.get().defaultBlockState());
        overworldPortal = true;
        overworldPortalPos = base;
        setDirty();
    }
}
