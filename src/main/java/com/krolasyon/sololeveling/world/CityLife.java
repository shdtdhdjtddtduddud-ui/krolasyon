package com.krolasyon.sololeveling.world;

import com.krolasyon.sololeveling.entity.HunterNpc;
import com.krolasyon.sololeveling.registry.ModBlocks;
import com.krolasyon.sololeveling.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;

/** Keeps the streets of Seoul alive: citizens and patrolling hunters walk around the players. */
public final class CityLife {
    private static int timer;

    private CityLife() {}

    public static void tick(MinecraftServer s) {
        if (++timer % 100 != 0) return;
        ServerLevel l = s.getLevel(Regions.SEOUL);
        if (l == null) return;
        for (ServerPlayer p : l.players()) {
            int near = l.getEntitiesOfClass(HunterNpc.class, p.getBoundingBox().inflate(48), n -> !n.isStaff()).size();
            if (near >= 14) continue;
            for (int i = 0; i < 3; i++) {
                int x = p.getBlockX() + l.random.nextInt(81) - 40, z = p.getBlockZ() + l.random.nextInt(81) - 40;
                if (p.distanceToSqr(x, p.getY(), z) < 16 * 16) continue;
                BlockPos ground = new BlockPos(x, CityPlan.GROUND, z);
                if (!l.hasChunkAt(ground)) continue;
                var st = l.getBlockState(ground);
                if (!st.is(ModBlocks.SIDEWALK.get()) && !st.is(net.minecraft.world.level.block.Blocks.POLISHED_ANDESITE)) continue;
                if (!l.getBlockState(ground.above()).isAir() || !l.getBlockState(ground.above(2)).isAir()) continue;
                HunterNpc n = ModEntities.NPC.get().create(l);
                if (n == null) continue;
                n.moveTo(x + 0.5, CityPlan.GROUND + 1, z + 0.5, l.random.nextFloat() * 360, 0);
                n.setupRole(l.random.nextInt(5) == 0 ? "hunter" : "citizen", l.random);
                n.finalizeSpawn(l, l.getCurrentDifficultyAt(ground), MobSpawnType.NATURAL, null, null);
                l.addFreshEntity(n);
            }
        }
    }
}
