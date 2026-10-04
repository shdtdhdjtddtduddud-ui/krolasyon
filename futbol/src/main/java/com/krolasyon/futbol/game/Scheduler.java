package com.krolasyon.futbol.game;

import java.util.ArrayList;
import java.util.List;

/** Tiny server-side delayed task queue, ticked once per server tick. */
public final class Scheduler {
    private Scheduler() {}

    private record Task(long at, Runnable run) {}

    private static final List<Task> TASKS = new ArrayList<>();
    private static final List<Task> PENDING = new ArrayList<>();
    private static long now;

    public static void later(int ticks, Runnable r) {
        PENDING.add(new Task(now + Math.max(1, ticks), r));
    }

    /** runs {@code r} with index 0..times-1 every tick starting after {@code delay} ticks */
    public static void repeat(int delay, int times, java.util.function.IntConsumer r) {
        for (int i = 0; i < times; i++) {
            final int k = i;
            later(delay + i, () -> r.accept(k));
        }
    }

    public static void tick() {
        now++;
        TASKS.addAll(PENDING);
        PENDING.clear();
        List<Task> due = new ArrayList<>();
        TASKS.removeIf(t -> {
            if (t.at() <= now) {
                due.add(t);
                return true;
            }
            return false;
        });
        for (Task t : due) {
            try {
                t.run().run();
            } catch (Throwable e) {
                com.mojang.logging.LogUtils.getLogger().error("[futbol] scheduled task failed", e);
            }
        }
    }

    public static void clear() {
        TASKS.clear();
        PENDING.clear();
    }
}
