package com.krolasyon.labirent.entity.ai;

import com.krolasyon.labirent.entity.GladerEntity;
import com.krolasyon.labirent.maze.ExploreTracker;
import com.krolasyon.labirent.maze.Glade;
import com.krolasyon.labirent.maze.MazeLayout;
import com.krolasyon.labirent.maze.MazeManager;
import com.krolasyon.labirent.maze.MazeState;
import com.krolasyon.labirent.team.Role;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BedBlock;

import java.util.EnumSet;

/** Daily routine of a Glader: job by day, home before the doors close, bed at night, captain's orders above all. */
public class GladerBrainGoal extends Goal {
    private final GladerEntity g;
    private final MazeNav nav;
    private int think;
    private boolean running;      // runner currently out in the maze
    private boolean forced;       // captain sent runners out
    private int runs;
    private int jobTimer;

    public GladerBrainGoal(GladerEntity g) {
        this.g = g;
        this.nav = new MazeNav(g);
        setFlags(EnumSet.of(Flag.MOVE));
    }

    public static void forceRun(GladerEntity g) {
        g.goalSelector.getAvailableGoals().forEach(w -> {
            if (w.getGoal() instanceof GladerBrainGoal b) b.forced = true;
        });
    }

    @Override
    public boolean canUse() { return g.getTarget() == null && !g.isPassenger(); }

    @Override
    public boolean canContinueToUse() { return canUse(); }

    @Override
    public void stop() { nav.stop(); }

    @Override
    public boolean requiresUpdateEveryTick() { return true; }

    @Override
    public void tick() {
        MazeManager m = MazeManager.get();
        if (m == null || !(g.level() instanceof ServerLevel level)) return;
        long time = level.getDayTime() % 24000L;
        boolean sunGone = m.state.has(MazeState.EV_SUN_GONE);
        boolean night = time >= 12500 && time < 23300;
        boolean dusk = time >= 10300 && time < 12500;
        boolean inGlade = Glade.inside(g.getX(), g.getZ());

        if (g.isSleeping()) {
            if (!night || g.getOrder() != GladerEntity.Order.WORK) g.stopSleeping();
            return;
        }
        if (g.isNewcomer()) {
            g.getNavigation().stop();
            return;
        }
        if (g.stumbling()) {
            g.getNavigation().stop();
            return;
        }
        long now = level.getGameTime();
        if (g.social().tick(level, now)) {
            nav.stop();
            return;
        }
        nav.tick();
        if (++think % 10 != 0) return;
        if (think % 40 == 0 && g.getOrder() == GladerEntity.Order.WORK && !running) g.social().consider(level, now, night, dusk);

        switch (g.getOrder()) {
            case FOLLOW -> { follow(level); return; }
            case STAY -> { moveNear(g.orderPos() == null ? g.blockPosition() : g.orderPos(), 2); return; }
            case GATHER -> {
                BlockPos p = g.orderPos();
                if (p == null || near(p, 3)) g.setOrder(GladerEntity.Order.STAY, p == null ? g.blockPosition() : p);
                else goAnywhere(p);
                return;
            }
            case RETURN -> {
                if (inGlade) {
                    g.setOrder(GladerEntity.Order.WORK, null);
                    running = false;
                } else goHome();
                return;
            }
            case DEFEND -> {
                moveNear(nearestDoor(), 3);
                return;
            }
            default -> {}
        }

        // ---- WORK: daily routine
        if (!inGlade && (dusk || night) && !sunGone) {
            goHome();
            return;
        }
        if (g.getRole() == Role.KOSUCU && (forced || (!night && !dusk && time > 200)) && !m.doors.closed()) {
            runner(m, level);
            return;
        }
        if (running) {
            // back from the maze
            running = false;
            runs++;
            int pct = ExploreTracker.percent(m.state);
            for (ServerPlayer p : level.players())
                if (p.distanceToSqr(g) < 40 * 40) g.say(p, Component.translatable("talk.labirent.runner_back", pct));
        }
        if (!inGlade) {
            goHome();
            return;
        }
        if (night && g.getRole() != Role.BEKCI) {
            sleep(level);
            return;
        }
        if (++jobTimer % 2 == 0) {
            if (!idleStyle(level, m)) Jobs.work(g, level, m);
        }
    }

    /** personality shows when there is a spare moment; returns true if it took over this tick */
    private boolean idleStyle(ServerLevel level, MazeManager m) {
        Personality per = g.personality();
        net.minecraft.util.RandomSource r = g.getRandom();
        switch (per.idle) {
            case TAG_ALONG -> {
                // Chuck tags along after the captain (or anyone) when they are around
                Player p = level.getNearestPlayer(g, 18);
                if (p != null && Glade.inside(p.getX(), p.getZ()) && (jobTimer / 30) % 3 != 0) {
                    double d = g.distanceToSqr(p);
                    if (d > 4.5 * 4.5) g.getNavigation().moveTo(p, d > 100 ? 1.3 : 1.0);
                    else {
                        g.getNavigation().stop();
                        g.getLookControl().setLookAt(p);
                    }
                    return true;
                }
            }
            case BROOD -> {
                // Gally keeps his distance from the captain and stares
                Player cap = m.state.captain == null ? null : level.getPlayerByUUID(m.state.captain);
                if (cap != null && g.distanceToSqr(cap) < 5 * 5) {
                    net.minecraft.world.phys.Vec3 away = g.position().subtract(cap.position()).normalize().scale(6);
                    g.getNavigation().moveTo(g.getX() + away.x, g.getY(), g.getZ() + away.z, 0.9);
                    g.getLookControl().setLookAt(cap);
                    return true;
                }
                if (cap != null && g.distanceToSqr(cap) < 14 * 14 && r.nextInt(4) == 0) {
                    g.getLookControl().setLookAt(cap);
                }
            }
            case JOG -> {
                // Minho never sits still: laps around the Glade when not in the maze
                if (g.getRole() == Role.KOSUCU && r.nextInt(3) == 0 && g.getNavigation().isDone()) {
                    int corner = (jobTimer / 6) % 4;
                    int[][] c = {{-22, -2}, {-2, 22}, {22, -2}, {-2, -22}};
                    g.getNavigation().moveTo(c[corner][0] + 0.5, Glade.Y, c[corner][1] + 0.5, 1.15);
                    return true;
                }
            }
            case PATROL -> {
                if (r.nextInt(5) == 0 && g.getNavigation().isDone() && g.getRole() != Role.SIFACI) {
                    BlockPos d = Glade.doorInside(r.nextInt(4));
                    g.getNavigation().moveTo(d.getX() + 0.5, d.getY(), d.getZ() + 0.5, 0.9);
                    return true;
                }
            }
            default -> {}
        }
        return false;
    }

    // ------------------------------------------------------------------ helpers
    private void runner(MazeManager m, ServerLevel level) {
        running = true;
        if (g.tickCount % 20 == 0) m.explore.reveal(m.state, g.getX(), g.getZ(), 18);
        if (nav.active()) return;
        int bestI = -1, bestJ = -1;
        for (int tries = 0; tries < 40; tries++) {
            int i = 2 * g.getRandom().nextInt(MazeLayout.CELLS) + 1, j = 2 * g.getRandom().nextInt(MazeLayout.CELLS) + 1;
            if (MazeLayout.inGladeArea(i, j)) continue;
            int d = Math.abs(i - MazeLayout.CENTER) + Math.abs(j - MazeLayout.CENTER);
            if (d > 22 + runs * 6) continue;
            bestI = i;
            bestJ = j;
            if (!ExploreTracker.known(m.state.known[MazeLayout.idx(i, j)])) break;
        }
        forced = false;
        if (bestI >= 0) nav.goTo(bestI, bestJ, 1.15);
    }

    private void goHome() {
        if (nav.active()) return;
        int di = MazeLayout.CENTER + 1, dj = MazeLayout.CENTER + 5;
        if (!nav.goTo(di, dj, 1.25)) g.getNavigation().moveTo(Glade.GATHER.getX(), Glade.GATHER.getY(), Glade.GATHER.getZ(), 1.25);
    }

    private void goAnywhere(BlockPos p) {
        if (Glade.inside(p) && Glade.inside(g.getX(), g.getZ())) {
            g.getNavigation().moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 1.15);
            return;
        }
        if (nav.active()) return;
        int i = MazeLayout.unitOf(p.getX()), j = MazeLayout.unitOf(p.getZ());
        if (i < 0 || j < 0 || !nav.goTo(i, j, 1.2)) g.getNavigation().moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 1.15);
    }

    private void moveNear(BlockPos p, double r) {
        if (!near(p, r)) goAnywhere(p);
    }

    private boolean near(BlockPos p, double r) { return g.distanceToSqr(p.getX() + 0.5, p.getY(), p.getZ() + 0.5) < r * r; }

    private void follow(ServerLevel level) {
        Player p = g.followTarget() == null ? null : level.getPlayerByUUID(g.followTarget());
        if (p == null) {
            g.setOrder(GladerEntity.Order.WORK, null);
            return;
        }
        double d = g.distanceToSqr(p);
        if (d > 48 * 48) {
            nav.stop();
            goAnywhere(p.blockPosition());
        } else if (d > 9) {
            nav.stop();
            g.getNavigation().moveTo(p, d > 100 ? 1.35 : 1.1);
        } else g.getNavigation().stop();
    }

    private BlockPos nearestDoor() {
        BlockPos best = Glade.doorInside(0);
        for (int d = 1; d < 4; d++)
            if (g.distanceToSqr(Glade.doorInside(d).getCenter()) < g.distanceToSqr(best.getCenter())) best = Glade.doorInside(d);
        return best;
    }

    private void sleep(ServerLevel level) {
        BlockPos bed = Jobs.claimBed(g, level);
        if (bed == null) {
            moveNear(g.home(), 3);
            return;
        }
        if (near(bed, 1.8)) {
            if (level.getBlockState(bed).getBlock() instanceof BedBlock && !level.getBlockState(bed).getValue(BedBlock.OCCUPIED)) {
                g.getNavigation().stop();
                g.startSleeping(bed);
            } else g.setBed(null);
        } else g.getNavigation().moveTo(bed.getX() + 0.5, bed.getY(), bed.getZ() + 0.5, 1.0);
    }
}
