package com.sololeveling.system;

import com.sololeveling.entity.SLMonster;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Recently slain hostile creatures that can be raised as shadows. */
public final class Corpses {
    private Corpses() {}

    public static final class Corpse {
        public final ResourceKey<Level> dim;
        public final double x, y, z;
        public final String shadowType;
        public final int rankIdx;
        public final boolean boss;
        public final long time;
        public final Component name;
        public final float maxHp;

        Corpse(ResourceKey<Level> dim, double x, double y, double z, String shadowType, int rankIdx, boolean boss, long time, Component name, float maxHp) {
            this.dim = dim; this.x = x; this.y = y; this.z = z; this.shadowType = shadowType; this.rankIdx = rankIdx;
            this.boss = boss; this.time = time; this.name = name; this.maxHp = maxHp;
        }
    }

    private static final List<Corpse> LIST = new ArrayList<>();
    public static final long LIFETIME = 20 * 60;

    public static synchronized void add(LivingEntity e) {
        String type;
        int rank;
        boolean boss = false;
        if (e instanceof SLMonster m) {
            type = m.def.shadow();
            if (type == null) return;
            rank = com.sololeveling.util.Ranks.index(m.def.rank());
            boss = m.def.boss();
        } else {
            rank = XpHandler.rankIdxForHealth(e.getMaxHealth());
            if (e instanceof Spider || e instanceof net.minecraft.world.entity.animal.Wolf) type = "wolf";
            else if (e instanceof Witch || e instanceof Evoker || e instanceof Blaze || e instanceof Ghast || e instanceof Illusioner) type = "mage";
            else if (e instanceof Ravager || e instanceof net.minecraft.world.entity.monster.warden.Warden || e instanceof net.minecraft.world.entity.monster.hoglin.Hoglin || e.getBbHeight() > 2.6F) type = "tank";
            else type = "soldier";
            boss = e instanceof net.minecraft.world.entity.boss.wither.WitherBoss || e instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon;
        }
        long now = e.level().getGameTime();
        LIST.add(new Corpse(e.level().dimension(), e.getX(), e.getY(), e.getZ(), type, rank, boss, now, e.getDisplayName(), e.getMaxHealth()));
        if (LIST.size() > 200) LIST.remove(0);
    }

    public static synchronized Corpse nearest(ServerPlayer sp, double radius) {
        long now = sp.level().getGameTime();
        Corpse best = null;
        double bd = radius * radius;
        Iterator<Corpse> it = LIST.iterator();
        while (it.hasNext()) {
            Corpse c = it.next();
            if (now - c.time > LIFETIME) { it.remove(); continue; }
            if (c.dim != sp.level().dimension()) continue;
            double d = sp.distanceToSqr(c.x, c.y, c.z);
            if (d < bd) { bd = d; best = c; }
        }
        return best;
    }

    public static synchronized void remove(Corpse c) { LIST.remove(c); }
}
