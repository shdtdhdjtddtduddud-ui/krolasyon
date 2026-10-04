package com.krolasyon.bosses.rpg.util;

import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Tiny server-side scheduler for effects that unfold over several ticks (beams, novas, rains...). */
public final class Tasks {
    private Tasks() {}

    public interface Step {
        /** @return false to stop early */
        boolean run(int tick);
    }

    private record Task(ServerLevel level, int duration, Step step, int[] tick) {}

    private static final List<Task> TASKS = new ArrayList<>();
    private static final List<Task> PENDING = new ArrayList<>();

    public static void add(ServerLevel level, int duration, Step step) {
        PENDING.add(new Task(level, duration, step, new int[]{0}));
    }

    public static void tick(ServerLevel level) {
        if (!PENDING.isEmpty()) {
            TASKS.addAll(PENDING);
            PENDING.clear();
        }
        Iterator<Task> it = TASKS.iterator();
        while (it.hasNext()) {
            Task t = it.next();
            if (t.level != level) continue;
            boolean keep;
            try {
                keep = t.step.run(t.tick[0]);
            } catch (Exception e) {
                keep = false;
            }
            t.tick[0]++;
            if (!keep || t.tick[0] >= t.duration) it.remove();
        }
    }

    public static void clear() {
        TASKS.clear();
        PENDING.clear();
    }
}
