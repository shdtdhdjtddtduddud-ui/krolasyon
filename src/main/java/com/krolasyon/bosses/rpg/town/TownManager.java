package com.krolasyon.bosses.rpg.town;

import com.krolasyon.bosses.rpg.data.RpgWorldData;
import com.krolasyon.bosses.rpg.def.MonsterDef;
import com.krolasyon.bosses.rpg.def.RpgDefs;
import com.krolasyon.bosses.rpg.mob.RpgMonster;
import com.krolasyon.bosses.rpg.registry.RpgEntities;
import com.krolasyon.bosses.rpg.util.FX;
import com.krolasyon.bosses.rpg.world.Site;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;

import java.util.*;

/** Builds settlements progressively when players come close, and wakes up the bosses of their lairs. */
public final class TownManager {
    private TownManager() {}

    private static final class Job {
        final Site site;
        final List<Canvas.Op> ops;
        int index;

        Job(Site site, List<Canvas.Op> ops) { this.site = site; this.ops = ops; }
    }

    private static final Map<String, Job> JOBS = new LinkedHashMap<>();

    private static int trigger(Site.Type t) {
        return switch (t) { case CAPITAL -> 200; case CITY -> 150; case VILLAGE -> 110; default -> 80; };
    }

    public static boolean building(String siteId) { return JOBS.containsKey(siteId); }

    /** checks once a second which sites near players need building */
    public static void scan(ServerLevel level, RpgWorldData w) {
        if (level.dimension() != net.minecraft.world.level.Level.OVERWORLD) return;
        for (ServerPlayer p : level.players()) {
            for (Site s : w.sites) {
                if (s.built || JOBS.containsKey(s.id)) continue;
                int t = trigger(s.type);
                if (s.distSq(p.getX(), p.getZ()) > t * t) continue;
                List<Canvas.Op> ops = SettlementGen.plan(level, s, w);
                JOBS.put(s.id, new Job(s, ops));
                w.setDirty();
                if (s.type == Site.Type.CAPITAL || s.type == Site.Type.CITY)
                    p.displayClientMessage(Component.literal("§7Ufukta " + s.name + " beliriyor..."), true);
            }
            lairs(level, w, p);
        }
    }

    /** runs build operations within a time budget */
    public static void tick(ServerLevel level, RpgWorldData w) {
        if (JOBS.isEmpty()) return;
        long until = System.nanoTime() + 18_000_000L;
        Iterator<Map.Entry<String, Job>> it = JOBS.entrySet().iterator();
        while (it.hasNext() && System.nanoTime() < until) {
            Job j = it.next().getValue();
            while (j.index < j.ops.size() && System.nanoTime() < until) {
                Canvas.Op op = j.ops.get(j.index++);
                try {
                    if (op instanceof Canvas.SetOp s) {
                        BlockPos pos = BlockPos.of(s.pos());
                        if (level.getBlockState(pos) != s.state()) level.setBlock(pos, s.state(), 2 | 32);
                    } else if (op instanceof Canvas.ActOp a) {
                        a.act().accept(level);
                    }
                } catch (Exception ignored) {
                }
            }
            if (j.index >= j.ops.size()) {
                j.site.built = true;
                w.setDirty();
                it.remove();
                for (ServerPlayer p : level.players()) {
                    if (j.site.distSq(p.getX(), p.getZ()) < 200 * 200 && (j.site.type == Site.Type.CAPITAL || j.site.type == Site.Type.CITY))
                        p.displayClientMessage(Component.literal("§6" + j.site.name + " §7— " + j.site.type.title), true);
                }
            }
        }
    }

    private static void lairs(ServerLevel level, RpgWorldData w, ServerPlayer p) {
        for (Site s : w.sites) {
            if (s.type != Site.Type.LAIR || !s.built || s.distSq(p.getX(), p.getZ()) > 44 * 44) continue;
            if (!s.bossAlive) {
                if (level.getGameTime() < s.bossRespawn) continue;
                s.bossAlive = true;
                w.setDirty();
            }
            MonsterDef def = RpgDefs.BY_ID.get(s.boss);
            EntityType<RpgMonster> type = def == null ? null : RpgEntities.typeOf(def.id());
            if (type == null) continue;
            BlockPos c = new BlockPos(s.x, s.y + 1, s.z);
            if (!level.getEntitiesOfClass(RpgMonster.class, new AABB(c).inflate(80), e -> e.getType() == type).isEmpty()) continue;
            if (level.getGameTime() - p.getPersistentData().getLong("LairWarn_" + s.id) < 20 * 60) continue;
            p.getPersistentData().putLong("LairWarn_" + s.id, level.getGameTime());
            RpgMonster boss = type.spawn(level, c.above(3), MobSpawnType.STRUCTURE);
            if (boss != null) {
                boss.setTarget(p);
                FX.column(level, ParticleTypes.SOUL_FIRE_FLAME, boss.position(), 8, 3, 150);
                FX.send(level, ParticleTypes.EXPLOSION_EMITTER, boss.position(), 1, 0, 0);
                level.playSound(null, c, SoundEvents.WITHER_SPAWN, p.getSoundSource(), 2.0F, 0.8F);
                p.sendSystemMessage(Component.literal("§4§l☠ " + def.name() + " uyandı! §r§c" + s.name + " artık bir savaş alanı."));
            }
        }
    }

    public static void onBossKilled(ServerLevel level, RpgMonster boss) {
        RpgWorldData w = RpgWorldData.get(level);
        for (Site s : w.sites) {
            if (s.type == Site.Type.LAIR && s.boss.equals(boss.def().id()) && s.distSq(boss.getX(), boss.getZ()) < 120 * 120) {
                s.bossAlive = false;
                s.bossRespawn = level.getGameTime() + 24000L * 3;
                w.setDirty();
            }
        }
    }

    public static void clear() { JOBS.clear(); }
}
