package com.krolasyon.bosses.realm.world;

import com.krolasyon.bosses.realm.Faction;
import com.krolasyon.bosses.realm.entity.RealmEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;

/** garrison of a freshly generated kingdom keep: the envoy and a few loyal soldiers */
public final class KeepGuards {
    private KeepGuards() {}

    static String[] garrison(Faction f) {
        return switch (f) {
            case ASH -> new String[]{"ash_knight", "ash_knight", "ash_walker"};
            case BLOOD -> new String[]{"blood_guard", "blood_guard", "blood_witch"};
            case SHADOW -> new String[]{"shadow_assassin", "shadow_assassin", "whisperer"};
            case LEGION -> new String[]{"legion_footsoldier", "legion_footsoldier", "legion_archer"};
            case SOUL -> new String[]{"phantom_knight", "phantom_knight", "soul_priest"};
        };
    }

    public static void spawn(WorldGenLevel l, BlockPos c, RandomSource r, Faction f) {
        place(l, f.id + "_envoy", c.offset(3, 1, 3), r);
        String[] g = garrison(f);
        int[][] spots = {{-5, -5}, {5, -5}, {0, 6}};
        for (int i = 0; i < g.length; i++) place(l, g[i], c.offset(spots[i][0], 0, spots[i][1]), r);
    }

    static void place(WorldGenLevel l, String id, BlockPos p, RandomSource r) {
        EntityType<? extends Mob> t = RealmEntities.type(id);
        if (t == null) return;
        Mob m = t.create(l.getLevel());
        if (m == null) return;
        m.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, r.nextFloat() * 360F, 0F);
        m.finalizeSpawn(l, l.getCurrentDifficultyAt(p), MobSpawnType.STRUCTURE, null, null);
        m.setPersistenceRequired();
        l.addFreshEntityWithPassengers(m);
    }
}
