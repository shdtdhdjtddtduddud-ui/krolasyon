package com.krolasyon.sololeveling;

import com.krolasyon.sololeveling.entity.MobKind;
import com.krolasyon.sololeveling.entity.SLMonster;
import com.krolasyon.sololeveling.registry.ModEntities;
import com.krolasyon.sololeveling.system.Rank;
import com.krolasyon.sololeveling.system.Scheduler;
import com.krolasyon.sololeveling.world.*;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import org.slf4j.Logger;

/** CI-only dedicated server smoke test (-Dsololeveling.servertest=true). */
public final class ServerSelfTest {
    private static final Logger LOG = LogUtils.getLogger();

    private ServerSelfTest() {}

    public static void start(MinecraftServer s) {
        LOG.info("[SERVERTEST] start");
        Scheduler.later(40, () -> step(s, "seoul", () -> {
            ServerLevel seoul = s.getLevel(Regions.SEOUL);
            for (int cx = -6; cx <= 6; cx++) for (int cz = -6; cz <= 6; cz++) seoul.getChunk(cx, cz);
            LOG.info("[SERVERTEST] seoul block at plaza: {}", seoul.getBlockState(new BlockPos(40, 63, 52)));
            LOG.info("[SERVERTEST] npcs in seoul: {}", seoul.getEntities(ModEntities.NPC.get(), e -> true).size());
        }));
        Scheduler.later(80, () -> step(s, "regions", () -> {
            ServerLevel dl = s.getLevel(Regions.DUNGEON);
            for (String id : new String[]{"double_dungeon", "job_change", "demon_castle", "jeju_island", "penalty_zone"}) {
                long t = System.currentTimeMillis();
                RegionBuilder.ensure(dl, id);
                LOG.info("[SERVERTEST] region {} built in {} ms", id, System.currentTimeMillis() - t);
            }
        }));
        Scheduler.later(120, () -> step(s, "dungeons", () -> {
            ServerLevel dl = s.getLevel(Regions.DUNGEON);
            for (DungeonTheme th : DungeonTheme.values()) {
                long t = System.currentTimeMillis();
                DungeonManager.Instance in = DungeonManager.get(s).create(s, Rank.C, false, th);
                DungeonBuilder.build(dl, in.origin, th, in.rank, false, in.id, in.seed);
                LOG.info("[SERVERTEST] dungeon {} built in {} ms", th, System.currentTimeMillis() - t);
            }
        }));
        Scheduler.later(160, () -> step(s, "mobs", () -> {
            ServerLevel ow = s.overworld();
            BlockPos base = ow.getSharedSpawnPos().above(2);
            for (MobKind k : MobKind.values()) {
                SLMonster m = ModEntities.mob(k).create(ow);
                m.moveTo(base.getX() + k.ordinal() * 6, base.getY(), base.getZ(), 0, 0);
                ow.addFreshEntity(m);
                Zombie z = EntityType.ZOMBIE.create(ow);
                z.moveTo(m.getX() + 3, m.getY(), m.getZ());
                ow.addFreshEntity(z);
                m.setTarget(z);
                for (int i = 0; i < m.abilityCount(); i++) {
                    int idx = i;
                    Scheduler.later(5 + i * 50, () -> step(s, k + " ability " + idx, () -> m.forceAbility(idx)));
                }
            }
        }));
        Scheduler.later(160 + 400, () -> step(s, "gates", () -> {
            ServerLevel seoul = s.getLevel(Regions.SEOUL);
            for (int i = 0; i < 3; i++) GateManager.spawnSeoul(s, seoul, seoul.random);
            LOG.info("[SERVERTEST] gates {}", GateManager.count(s));
            NewsManager.get(s).post(s, 5, net.minecraft.network.chat.Component.literal("test"), false);
        }));
        Scheduler.later(160 + 520, () -> {
            LOG.info("[SERVERTEST] done, stopping");
            s.halt(false);
        });
    }

    private static void step(MinecraftServer s, String name, Runnable r) {
        try {
            r.run();
            LOG.info("[SERVERTEST] ok {}", name);
        } catch (Throwable t) {
            LOG.error("[SERVERTEST] FAILED {}", name, t);
        }
    }
}
