package com.krolasyon.bosses.rpg.item;

import com.krolasyon.bosses.rpg.def.MonsterDef;
import com.krolasyon.bosses.rpg.def.RpgDefs;
import com.krolasyon.bosses.rpg.def.SpellDef;
import com.krolasyon.bosses.rpg.def.SwordDef;
import com.krolasyon.bosses.rpg.mob.RpgMonster;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Coins, essences, spell books, staffs and boss weapons dropped by monsters. */
public final class RpgLoot {
    private RpgLoot() {}

    public static ItemStack randomTome(RandomSource r, int minTier, int maxTier) {
        List<SpellDef> pool = new ArrayList<>();
        for (SpellDef s : RpgDefs.SPELLS) if (s.tier() >= minTier && s.tier() <= maxTier) pool.add(s);
        if (pool.isEmpty()) pool.addAll(RpgDefs.SPELLS);
        return SpellTomeItem.of(pool.get(r.nextInt(pool.size())));
    }

    public static SwordDef bossSword(MonsterDef boss) {
        List<SwordDef> pool = new ArrayList<>();
        for (SwordDef s : RpgDefs.SWORDS) if (s.tier() >= 4) pool.add(s);
        String id = boss.id();
        if (id.contains("dragon") || id.contains("drake")) for (SwordDef s : RpgDefs.SWORDS) if (s.id().equals("dragonbane")) return s;
        if (id.contains("lich") || id.contains("bone")) for (SwordDef s : RpgDefs.SWORDS) if (s.id().equals("dawnbreaker")) return s;
        if (id.contains("demon") || id.contains("ash")) for (SwordDef s : RpgDefs.SWORDS) if (s.id().equals("ashbringer")) return s;
        return pool.get(Math.floorMod(id.hashCode(), pool.size()));
    }

    public static void monsterLoot(RpgMonster m, DamageSource src, int looting, boolean byPlayer) {
        RandomSource r = m.getRandom();
        MonsterDef d = m.def();
        int danger = d.danger();
        if (d.boss()) {
            m.spawnAtLocation(new ItemStack(RpgItems.GOLD_COIN.get(), 3 + r.nextInt(6)));
            m.spawnAtLocation(new ItemStack(RpgItems.SILVER_COIN.get(), 10 + r.nextInt(20)));
            m.spawnAtLocation(randomTome(r, 3, 5));
            m.spawnAtLocation(randomTome(r, 2, 4));
            m.spawnAtLocation(new ItemStack(RpgItems.sword(bossSword(d).id())));
            m.spawnAtLocation(new ItemStack(RpgItems.TROPHY.get()));
            m.spawnAtLocation(new ItemStack(RpgItems.GREATER_MANA_POTION.get(), 2));
            if (r.nextInt(3) == 0) m.spawnAtLocation(new ItemStack(RpgItems.STAFFS.get(3 + r.nextInt(2)).get()));
            return;
        }
        if (!byPlayer && r.nextInt(3) != 0) return;
        int copper = r.nextInt(2 + danger * 3) + looting;
        if (copper > 0) m.spawnAtLocation(new ItemStack(RpgItems.COPPER_COIN.get(), copper));
        if (r.nextInt(100) < danger * 6) m.spawnAtLocation(new ItemStack(RpgItems.SILVER_COIN.get(), 1 + r.nextInt(danger)));
        if (r.nextInt(100) < 30 + looting * 10) m.spawnAtLocation(new ItemStack(RpgItems.ESSENCE.get(), 1 + (danger >= 4 ? 1 : 0)));
        if (r.nextInt(100) < danger * 3) m.spawnAtLocation(randomTome(r, 1, Math.min(5, danger)));
        if (r.nextInt(1000) < danger * 6) m.spawnAtLocation(new ItemStack(RpgItems.STAFFS.get(Math.min(4, Math.max(0, danger - 2))).get()));
        if (r.nextInt(100) < 5) m.spawnAtLocation(new ItemStack(RpgItems.MANA_POTION.get()));
        if (r.nextInt(100) < 4) m.spawnAtLocation(new ItemStack(RpgItems.HEALING_POTION.get()));
        if (r.nextInt(1000) < danger * 4) {
            List<SwordDef> pool = new ArrayList<>();
            for (SwordDef s : RpgDefs.SWORDS) if (s.tier() <= Math.max(1, danger - 1)) pool.add(s);
            if (!pool.isEmpty()) m.spawnAtLocation(new ItemStack(RpgItems.sword(pool.get(r.nextInt(pool.size())).id())));
        }
    }
}
