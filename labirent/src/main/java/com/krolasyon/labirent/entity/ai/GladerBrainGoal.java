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
        nav.tick();
        if (++think % 10 != 0) return;

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
        if (++jobTimer % 2 == 0) Jobs.work(g, level, m);
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
