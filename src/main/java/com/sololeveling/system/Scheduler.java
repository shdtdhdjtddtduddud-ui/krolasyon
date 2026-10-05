package com.sololeveling.system;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.sololeveling.SoloLeveling;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Tiny server-tick task scheduler. */
@Mod.EventBusSubscriber(modid = SoloLeveling.MODID)
public final class Scheduler {
    private Scheduler() {}

    private static final class Task {
        int delay;
        final Runnable run;
        Task(int d, Runnable r) { delay = d; run = r; }
    }

    private static final List<Task> TASKS = new ArrayList<>();
    private static final List<Task> ADD = new ArrayList<>();

    public static void later(int ticks, Runnable r) {
        synchronized (ADD) { ADD.add(new Task(Math.max(1, ticks), r)); }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        synchronized (ADD) { TASKS.addAll(ADD); ADD.clear(); }
        Iterator<Task> it = TASKS.iterator();
        while (it.hasNext()) {
            Task t = it.next();
            if (--t.delay <= 0) {
                it.remove();
                try { t.run.run(); } catch (Exception ex) { ex.printStackTrace(); }
            }
        }
    }
}
