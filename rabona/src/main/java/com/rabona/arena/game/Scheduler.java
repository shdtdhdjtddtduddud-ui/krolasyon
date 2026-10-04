package com.rabona.arena.game;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Basit gecikmeli gorev zamanlayici (sunucu tick). */
public final class Scheduler {
    private static final List<long[]> TIMES = new ArrayList<>();
    private static final List<Runnable> TASKS = new ArrayList<>();
    private static long now;

    private Scheduler() {}

    public static void later(int ticks, Runnable r) {
        TIMES.add(new long[]{now + Math.max(1, ticks)});
        TASKS.add(r);
    }

    public static void tick() {
        now++;
        List<Runnable> run = new ArrayList<>();
        Iterator<long[]> ti = TIMES.iterator();
        Iterator<Runnable> ri = TASKS.iterator();
        while (ti.hasNext()) {
            long[] t = ti.next();
            Runnable r = ri.next();
            if (t[0] <= now) {
                run.add(r);
                ti.remove();
                ri.remove();
            }
        }
        for (Runnable r : run) {
            try {
                r.run();
            } catch (Exception e) {
                com.rabona.arena.RabonaArena.LOG.error("scheduled task failed", e);
            }
        }
    }

    public static void clear() {
        TIMES.clear();
        TASKS.clear();
    }
}
