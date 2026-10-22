package com.krolasyon.elvenrealm.world;

import com.krolasyon.elvenrealm.entity.ElfEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.Set;

/**
 * Hangi kentte hangi yeni (v2) sakinin yerleştirildiğini tutar: yeni kentlerde üretim sırasında, eski kentlerde oyuncu kente
 * ilk girdiğinde lord, taht muhafızları ve devriyeler eklenir; hiçbiri iki kez doğmaz.
 */
public final class CityCensus extends SavedData {
    private static final String ID = "elvenrealm_census";
    private final Set<String> placed = new HashSet<>();

    private static volatile CityCensus cached;
    private static volatile Object cachedServer;

    /** Dünya üretimi iş parçacıklarından da çağrılır; kayıt deposuna yalnızca ilk seferde (eşzamanlı olmadan) dokunulur. */
    public static CityCensus get(ServerLevel level) {
        Object srv = level.getServer();
        CityCensus c = cached;
        if (c != null && cachedServer == srv) return c;
        synchronized (CityCensus.class) {
            if (cached == null || cachedServer != srv) {
                cached = level.getServer().overworld().getDataStorage().computeIfAbsent(CityCensus::load, CityCensus::new, ID);
                cachedServer = srv;
            }
            return cached;
        }
    }

    private static CityCensus load(CompoundTag tag) {
        CityCensus c = new CityCensus();
        ListTag l = tag.getList("Placed", Tag.TAG_STRING);
        for (int i = 0; i < l.size(); i++) c.placed.add(l.getString(i));
        return c;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag l = new ListTag();
        for (String s : placed) l.add(StringTag.valueOf(s));
        tag.put("Placed", l);
        return tag;
    }

    public synchronized boolean has(long city, String post) {
        return placed.contains(city + ":" + post);
    }

    public synchronized void mark(long city, String post) {
        if (placed.add(city + ":" + post)) setDirty();
    }

    /** Oyuncu kente girdiğinde: yüklü bölgedeki eksik yeni sakinleri yerleştir. */
    public static void ensure(ServerLevel level, CityLayout.City c) {
        var gen = level.getChunkSource().getGenerator();
        var rs = level.getChunkSource().randomState();
        int y0 = CityLayout.base(c, gen, rs, level);
        if (y0 == Integer.MIN_VALUE) return;
        if (CityLayout.type(level.getSeed(), c, gen, rs, level) != CityType.CAPITAL) return;
        CityCensus census = get(level);
        CityPlan plan = null;
        AABB box = new AABB(c.cx() - 70, y0 - 20, c.cz() - 130, c.cx() + 70, y0 + 80, c.cz() + 60);
        for (String post : new String[]{"aerendil", "royal_l", "royal_r", "castle_r", "wall_l", "wall_r", "patrol_a"}) {
            if (census.has(c.seed(), post)) continue;
            if (plan == null) plan = ElvenCityFeature.plan(c, y0, CityType.CAPITAL);
            for (CityPlan.Npc n : plan.npcs) {
                if (!n.post().equals(post)) continue;
                BlockPos p = new BlockPos(n.x(), n.y(), n.z());
                if (!level.isLoaded(p)) continue;
                boolean exists = !level.getEntitiesOfClass(ElfEntity.class, box, e -> post.equals(e.post())).isEmpty();
                if (!exists) {
                    ElfEntity e = ElvenCityFeature.create(level, plan, n);
                    if (e == null) continue;
                    e.finalizeSpawn(level, level.getCurrentDifficultyAt(p), MobSpawnType.STRUCTURE, null, null);
                    level.addFreshEntity(e);
                }
                census.mark(c.seed(), post);
            }
        }
    }
}
