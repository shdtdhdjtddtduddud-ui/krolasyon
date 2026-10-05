package com.krolasyon.sololeveling.system;

import com.krolasyon.sololeveling.registry.ModItems;
import com.krolasyon.sololeveling.world.Regions;
import com.krolasyon.sololeveling.world.WorldState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * "Daily Quest: Preparation to become powerful."
 * Push-ups = crouches, sit-ups = jumps, squats = melee hits, running = sprinted blocks.
 */
public final class DailyQuest {
    private DailyQuest() {}

    public static long today(ServerPlayer p) { return p.server.overworld().getDayTime() / 24000L; }

    public static void reset(ServerPlayer p, HunterData d, boolean announce) {
        d.dailyDay = today(p);
        java.util.Arrays.fill(d.daily, 0);
        d.dailyDone = false;
        d.dailyRewarded = false;
        d.markDirty();
        if (announce) Sys.notify(p, Sys.QUEST, Sys.t("daily.title"), Sys.t("daily.new"));
    }

    public static void tick(ServerPlayer p, HunterData d) {
        if (!d.awakened) return;
        long day = today(p);
        if (d.dailyDay < 0) {
            reset(p, d, false);
            return;
        }
        if (day != d.dailyDay) {
            boolean failed = !d.dailyDone && day == d.dailyDay + 1;
            reset(p, d, true);
            if (failed && !p.isCreative() && WorldState.get(p.server).penaltyEnabled && d.penaltyTicks <= 0) {
                Sys.notify(p, Sys.WARN, Sys.t("system.warning"), Sys.t("daily.penalty"));
                Scheduler.later(60, () -> Regions.sendToPenalty(p));
            }
        }
    }

    public static void progress(ServerPlayer p, int index, int amount) {
        HunterData d = HunterCapability.get(p);
        if (!d.awakened || d.dailyDone) return;
        int goal = HunterData.DAILY_GOAL[index];
        int before = d.daily[index];
        d.daily[index] = Math.min(goal, before + amount);
        if (d.daily[index] == before) return;
        if (before < goal && d.daily[index] >= goal) p.displayClientMessage(Sys.t("daily.part_done", Sys.t("daily.task" + index)), true);
        boolean all = true;
        for (int i = 0; i < 4; i++) if (d.daily[i] < HunterData.DAILY_GOAL[i]) all = false;
        if (all) {
            d.dailyDone = true;
            Sys.notify(p, Sys.QUEST, Sys.t("daily.title"), Sys.t("daily.complete"));
        }
        if (d.daily[index] / 10 != before / 10 || all) d.markDirty();
    }

    public static void claim(ServerPlayer p) {
        HunterData d = HunterCapability.get(p);
        if (!d.dailyDone || d.dailyRewarded) return;
        d.dailyRewarded = true;
        d.statPoints += 3;
        d.mana = d.maxMana();
        p.setHealth(p.getMaxHealth());
        p.getFoodData().eat(20, 1F);
        d.fatigue = 0;
        d.gold += 100 + d.level * 10L;
        Progression.give(p, new ItemStack(ModItems.RANDOM_BOX.get()));
        Sys.notify(p, Sys.REWARD, Sys.t("daily.reward_title"), Sys.t("daily.reward_body", 100 + d.level * 10));
        d.markDirty();
    }
}
