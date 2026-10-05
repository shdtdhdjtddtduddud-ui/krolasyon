package com.krolasyon.sololeveling.system;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Tiny server-side delayed task queue. */
public final class Scheduler {
    private static final List<Task> TASKS = new ArrayList<>();
    private static final List<Task> PENDING = new ArrayList<>();

    private record Task(int[] ticks, Runnable r) {}

    private Scheduler() {}

    public static void later(int ticks, Runnable r) { PENDING.add(new Task(new int[]{ticks}, r)); }

    public static void tick() {
        TASKS.addAll(PENDING);
        PENDING.clear();
        Iterator<Task> it = TASKS.iterator();
        while (it.hasNext()) {
            Task t = it.next();
            if (--t.ticks[0] <= 0) {
                it.remove();
                try {
                    t.r.run();
                } catch (RuntimeException e) {
                    com.mojang.logging.LogUtils.getLogger().error("Scheduled task failed", e);
                }
            }
        }
    }

    public static void clear() {
        TASKS.clear();
        PENDING.clear();
    }
}
