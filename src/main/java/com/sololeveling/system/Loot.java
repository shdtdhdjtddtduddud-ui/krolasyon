package com.sololeveling.system;

import com.sololeveling.entity.SLMonster;
import com.sololeveling.registry.ModItems;
import com.sololeveling.util.Ranks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

public final class Loot {
    private Loot() {}

    private static final String[] CRYSTALS = {"mana_crystal_e", "mana_crystal_d", "mana_crystal_c", "mana_crystal_b", "mana_crystal_a", "mana_crystal_s", "mana_crystal_s"};

    public static void dropFor(LivingEntity e, boolean fromPlayer, double luck) {
        if (!(e.level() instanceof ServerLevel sl)) return;
        RandomSource r = sl.random;
        if (e instanceof SLMonster m) {
            int ri = Ranks.index(m.def.rank());
            double chance = (m.def.boss() ? 1.0 : 0.45) + luck * 0.02;
            if (r.nextDouble() < chance) put(sl, e, new ItemStack(ModItems.get(CRYSTALS[ri]), 1 + r.nextInt(m.def.boss() ? 4 : 2)));
            if (r.nextDouble() < 0.5) put(sl, e, new ItemStack(ModItems.get("gold_coin"), 1 + r.nextInt(2 + ri * 2)));
            if (r.nextDouble() < 0.08 + ri * 0.01) put(sl, e, new ItemStack(ModItems.get(ri >= 3 ? "mp_potion_large" : "hp_potion_small")));
            if (r.nextDouble() < 0.04) put(sl, e, new ItemStack(ModItems.get("shadow_essence")));
        } else {
            int ri = XpHandler.rankIdxForHealth(e.getMaxHealth());
            if (e.getMaxHealth() >= 18 && r.nextDouble() < 0.12 + luck * 0.01) put(sl, e, new ItemStack(ModItems.get(CRYSTALS[ri])));
            if (r.nextDouble() < 0.10) put(sl, e, new ItemStack(ModItems.get("gold_coin")));
        }
    }

    private static void put(ServerLevel l, LivingEntity e, ItemStack s) {
        ItemEntity it = new ItemEntity(l, e.getX(), e.getY() + 0.5, e.getZ(), s);
        it.setDeltaMovement((l.random.nextDouble() - 0.5) * 0.2, 0.25, (l.random.nextDouble() - 0.5) * 0.2);
        l.addFreshEntity(it);
    }
}
