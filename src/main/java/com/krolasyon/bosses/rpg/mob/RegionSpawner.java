package com.krolasyon.bosses.rpg.mob;

import com.krolasyon.bosses.rpg.data.RpgWorldData;
import com.krolasyon.bosses.rpg.def.MonsterDef;
import com.krolasyon.bosses.rpg.def.RpgDefs;
import com.krolasyon.bosses.rpg.def.RpgDefs.RegionId;
import com.krolasyon.bosses.rpg.registry.RpgEntities;
import com.krolasyon.bosses.rpg.world.Site;
import com.krolasyon.bosses.rpg.world.WorldMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Spawns the monsters of the region a player is in. Settlements are kept safe. */
public final class RegionSpawner {
    private RegionSpawner() {}

    private static final Map<RegionId, List<MonsterDef>> POOLS = new EnumMap<>(RegionId.class);

    private static List<MonsterDef> pool(RegionId r) {
        return POOLS.computeIfAbsent(r, k -> {
            List<MonsterDef> l = new ArrayList<>();
            for (MonsterDef d : RpgDefs.MONSTERS) for (RegionId x : d.regions()) if (x == k) { l.add(d); break; }
            return l;
        });
    }

    public static void tick(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        if (level.getDifficulty() == Difficulty.PEACEFUL || p.isSpectator()) return;
        RpgWorldData w = RpgWorldData.get(level);
        Site inside = WorldMap.siteAt(w, p.getX(), p.getZ());
        if (inside != null && inside.type != Site.Type.CAMP) return;
        int near = level.getEntitiesOfClass(RpgMonster.class, p.getBoundingBox().inflate(64)).size();
        boolean night = level.isNight() || level.dimension() != net.minecraft.world.level.Level.OVERWORLD;
        int cap = night ? 9 : 5;
        if (near >= cap) return;
        RegionId region = WorldMap.regionAt(level, p.blockPosition());
        List<MonsterDef> pool = pool(region);
        if (pool.isEmpty()) return;
        RandomSource r = p.getRandom();
        MonsterDef d = null;
        for (int i = 0; i < 6 && d == null; i++) {
            MonsterDef c = pool.get(r.nextInt(pool.size()));
            if (c.has(RpgDefs.T_NIGHT_ONLY) && !night) continue;
            if (c.has(RpgDefs.T_AQUATIC) && r.nextInt(3) != 0) continue;
            int danger = c.danger();
            if (danger > region.danger + 2 && r.nextInt(3) != 0) continue;
            d = c;
        }
        if (d == null) return;
        EntityType<RpgMonster> type = RpgEntities.typeOf(d.id());
        if (type == null) return;
        boolean underground = region == RegionId.CRYSTAL_HOLLOW && p.getY() < 40;
        for (int attempt = 0; attempt < 8; attempt++) {
            double a = r.nextDouble() * Math.PI * 2, dist = 24 + r.nextDouble() * 20;
            int x = (int) (p.getX() + Math.cos(a) * dist), z = (int) (p.getZ() + Math.sin(a) * dist);
            if (WorldMap.siteAt(w, x, z) != null && inside == null) continue;
            BlockPos pos;
            if (underground) {
                pos = null;
                for (int dy = -8; dy <= 8 && pos == null; dy++) {
                    BlockPos q = new BlockPos(x, (int) p.getY() + dy, z);
                    if (level.getBlockState(q).isAir() && level.getBlockState(q.above()).isAir() && level.getBlockState(q.below()).isSolid()) pos = q;
                }
                if (pos == null) continue;
            } else {
                int y = com.krolasyon.bosses.rpg.util.Heights.loadedGround(level, x, z);
                if (y == Integer.MIN_VALUE) continue;
                pos = new BlockPos(x, y, z);
                boolean water = !level.getFluidState(pos.below()).isEmpty();
                if (water && !d.has(RpgDefs.T_AQUATIC) && !d.flying()) continue;
                if (d.flying()) pos = pos.above(3 + r.nextInt(5));
                if (!night && level.canSeeSky(pos) && d.has(RpgDefs.T_SUN_BURN)) continue;
            }
            int group = d.has(RpgDefs.T_PACK) ? 2 + r.nextInt(3) : 1;
            for (int i = 0; i < group; i++) {
                RpgMonster m = type.spawn(level, pos.offset(r.nextInt(3) - 1, 0, r.nextInt(3) - 1), MobSpawnType.NATURAL);
                if (m != null && r.nextInt(5) == 0) m.setTarget(p);
            }
            return;
        }
    }
}
