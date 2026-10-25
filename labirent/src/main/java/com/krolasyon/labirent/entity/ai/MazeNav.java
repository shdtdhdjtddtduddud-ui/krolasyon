package com.krolasyon.labirent.entity.ai;

import com.krolasyon.labirent.maze.MazeLayout;
import com.krolasyon.labirent.maze.MazeManager;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Long-distance walking through the maze: a BFS over the maze grid gives corner waypoints,
 * vanilla navigation handles each short straight leg.
 */
public final class MazeNav {
    private final PathfinderMob mob;
    private final List<Vec3> points = new ArrayList<>();
    private int index;
    private int stuck;
    private Vec3 lastPos = Vec3.ZERO;
    private double speed = 1.0;

    public MazeNav(PathfinderMob mob) { this.mob = mob; }

    public boolean active() { return index < points.size(); }

    public void stop() {
        points.clear();
        index = 0;
        mob.getNavigation().stop();
    }

    /** plan to a unit; returns false if unreachable */
    public boolean goTo(int gi, int gj, double speed) {
        MazeManager m = MazeManager.get();
        if (m == null || m.state.layout == null) return false;
        int si = MazeLayout.unitOf(mob.getBlockX()), sj = MazeLayout.unitOf(mob.getBlockZ());
        if (si < 0 || sj < 0) return false;
        List<Integer> path = m.state.layout.path(si, sj, gi, gj, !m.doors.closed(), MazeLayout.G * MazeLayout.G);
        if (path == null) return false;
        points.clear();
        index = 0;
        this.speed = speed;
        int prevDir = -1;
        for (int k = 1; k < path.size(); k++) {
            int a = path.get(k - 1), b = path.get(k);
            int dir = b - a;
            if ((prevDir != -1 && dir != prevDir) || k % 6 == 0) add(a);
            prevDir = dir;
        }
        add(path.get(path.size() - 1));
        return !points.isEmpty();
    }

    private void add(int u) {
        points.add(new Vec3(MazeLayout.mid(u / MazeLayout.G), MazeLayout.GROUND + 1, MazeLayout.mid(u % MazeLayout.G)));
    }

    /** advance along the waypoints; returns true while still travelling */
    public boolean tick() {
        if (!active()) return false;
        Vec3 p = points.get(index);
        double dx = p.x - mob.getX(), dz = p.z - mob.getZ();
        if (dx * dx + dz * dz < 2.2) {
            index++;
            stuck = 0;
            if (!active()) {
                mob.getNavigation().stop();
                return false;
            }
            p = points.get(index);
        }
        if (mob.tickCount % 10 == 0) {
            if (mob.position().distanceToSqr(lastPos) < 0.05) stuck++;
            else stuck = 0;
            lastPos = mob.position();
            if (mob.getNavigation().isDone() || stuck > 0) mob.getNavigation().moveTo(p.x, p.y, p.z, speed);
            if (stuck > 6) {
                // hop to the waypoint if something keeps blocking us (e.g. a wall that just moved)
                mob.teleportTo(p.x, p.y, p.z);
                stuck = 0;
            }
        }
        return true;
    }
}
