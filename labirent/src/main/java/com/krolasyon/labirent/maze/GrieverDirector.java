package com.krolasyon.labirent.maze;

import com.krolasyon.labirent.LabConfig;
import com.krolasyon.labirent.entity.BeetleBladeEntity;
import com.krolasyon.labirent.entity.GrieverEntity;
import com.krolasyon.labirent.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** Grievers own the maze at night; beetle blades watch everybody all the time. */
public final class GrieverDirector {
    private static final AABB WORLD = new AABB(MazeLayout.ORIGIN - 10, 0, MazeLayout.ORIGIN - 10,
            MazeLayout.ORIGIN + MazeLayout.SIZE + 10, 200, MazeLayout.ORIGIN + MazeLayout.SIZE + 10);
    private final MazeManager m;
    private int raidNight = -1;

    GrieverDirector(MazeManager m) { this.m = m; }

    public void tick(ServerLevel level, int day, int phase, boolean night) {
        long t = level.getGameTime();
        if (t % 60 == 0) spawnGrievers(level, phase, night);
        if (t % 200 == 7) beetles(level);
        if (t % 40 == 3 && !night && !m.state.has(MazeState.EV_SUN_GONE)) retreat(level);
    }

    private int target(ServerLevel level, boolean night) {
        int players = level.players().size();
        if (players == 0) return 0;
        int base = LabConfig.GRIEVERS_PER_NIGHT.get() + players / 2 + Math.min(4, m.state.chapter / 2);
        boolean sunGone = m.state.has(MazeState.EV_SUN_GONE);
        if (night) return base + (sunGone ? 2 : 0);
        return sunGone ? base / 2 : (m.state.chapter >= 3 ? 1 : 0);
    }

    private void spawnGrievers(ServerLevel level, int phase, boolean night) {
        List<GrieverEntity> all = level.getEntitiesOfClass(GrieverEntity.class, WORLD);
        int want = target(level, night);
        if (all.size() >= want) {
            raid(level, all, phase, night);
            return;
        }
        RandomSource r = level.random;
        List<ServerPlayer> players = level.players();
        for (int tries = 0; tries < 30; tries++) {
            int ci = r.nextInt(MazeLayout.CELLS), cj = r.nextInt(MazeLayout.CELLS);
            int i = 2 * ci + 1, j = 2 * cj + 1;
            if (MazeLayout.inGladeArea(i, j)) continue;
            double x = MazeLayout.mid(i), z = MazeLayout.mid(j);
            boolean near = false, tooNear = false;
            for (ServerPlayer p : players) {
                double d = p.distanceToSqr(x, p.getY(), z);
                if (d < 26 * 26) tooNear = true;
                if (d < 110 * 110) near = true;
            }
            if (!near || tooNear) continue;
            GrieverEntity g = ModEntities.GRIEVER.get().create(level);
            if (g == null) return;
            g.moveTo(x, MazeLayout.GROUND + 1, z, r.nextFloat() * 360F, 0F);
            g.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(x, MazeLayout.GROUND + 1, z)), MobSpawnType.EVENT, null, null);
            level.addFreshEntity(g);
            return;
        }
    }

    /** once the sun is gone, a few Grievers come for the Glade every night */
    private void raid(ServerLevel level, List<GrieverEntity> all, int phase, boolean night) {
        if (!night || !m.state.has(MazeState.EV_SUN_GONE)) return;
        int nightNo = (int) ((level.getDayTime() + 10000L) / 24000L);
        if (raidNight == nightNo || phase < 15000) return;
        raidNight = nightNo;
        int n = 0;
        for (GrieverEntity g : all) {
            if (n >= 2 + m.state.chapter / 3) break;
            g.setRaider(true);
            n++;
        }
    }

    private void retreat(ServerLevel level) {
        for (GrieverEntity g : level.getEntitiesOfClass(GrieverEntity.class, WORLD)) {
            if (g.getTarget() != null || g.isRaider()) continue;
            if (level.getNearestPlayer(g, 20) != null) continue;
            if (level.random.nextFloat() < 0.25F) {
                level.sendParticles(ParticleTypes.LARGE_SMOKE, g.getX(), g.getY() + 0.8, g.getZ(), 20, 0.8, 0.5, 0.8, 0.02);
                g.discard();
            }
        }
    }

    private void beetles(ServerLevel level) {
        List<BeetleBladeEntity> all = level.getEntitiesOfClass(BeetleBladeEntity.class, WORLD);
        for (BeetleBladeEntity b : all) if (level.getNearestPlayer(b, 64) == null) b.discard();
        if (all.size() >= 4 + level.players().size() * 2) return;
        RandomSource r = level.random;
        for (ServerPlayer p : level.players()) {
            if (r.nextFloat() < 0.5F) continue;
            double a = r.nextFloat() * Math.PI * 2, d = 12 + r.nextFloat() * 18;
            int x = (int) (p.getX() + Math.cos(a) * d), z = (int) (p.getZ() + Math.sin(a) * d);
            if (!m.state.layout.isOpenBlock(x, z)) continue;
            BeetleBladeEntity b = ModEntities.BEETLE_BLADE.get().create(level);
            if (b == null) return;
            b.moveTo(x + 0.5, MazeLayout.GROUND + 1, z + 0.5, r.nextFloat() * 360F, 0F);
            level.addFreshEntity(b);
        }
    }
}
