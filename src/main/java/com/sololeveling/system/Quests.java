package com.sololeveling.system;

import com.sololeveling.player.ModCaps;
import com.sololeveling.player.SLPlayer;
import com.sololeveling.registry.ModItems;
import com.sololeveling.util.Ranks;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EntityType;

import java.util.Iterator;

public final class Quests {
    private Quests() {}

    public static final int MAX_ACTIVE = 4;

    public static long today(ServerPlayer sp) {
        return sp.getServer().overworld().getDayTime() / 24000L;
    }

    // ------------------------------------------------------------------ daily
    public static void checkDaily(ServerPlayer sp) {
        SLPlayer d = ModCaps.get(sp);
        long day = today(sp);
        if (d.dailyDay == day) return;
        boolean first = d.dailyDay < 0;
        if (!first && !d.dqClaimed && !dailyDone(d)) {
            d.fatigue = Math.min(100, d.fatigue + 30);
            Sys.warn(sp, "gui.sololeveling.daily_failed");
        }
        d.dailyDay = day;
        d.dqKills = 0; d.dqDist = 0; d.dqGates = 0; d.dqClaimed = false;
        d.dqKillTarget = Math.min(80, 15 + d.level);
        d.dqDistTarget = Math.min(4000, 800 + d.level * 30);
        Sys.notify(sp, Sys.QUEST, Component.translatable("gui.sololeveling.quest_arrived"), Component.translatable("gui.sololeveling.daily_name"));
        PlayerSync.sync(sp);
    }

    public static boolean dailyDone(SLPlayer d) {
        return d.dqKills >= d.dqKillTarget && d.dqDist >= d.dqDistTarget && (d.level < 10 || d.dqGates >= 1);
    }

    public static void claimDaily(ServerPlayer sp) {
        SLPlayer d = ModCaps.get(sp);
        if (d.dqClaimed || !dailyDone(d)) return;
        d.dqClaimed = true;
        long xp = 100 + d.level * 25L;
        long gold = 150 + d.level * 15L;
        d.gold += gold;
        d.points += 2;
        sp.getInventory().add(new ItemStack(ModItems.get("hp_potion_small"), 2));
        Sys.notify(sp, Sys.REWARD, Component.translatable("gui.sololeveling.daily_complete"), Component.translatable("gui.sololeveling.daily_reward", xp, gold));
        XpHandler.giveXp(sp, xp);
    }

    // ------------------------------------------------------------------ bounty
    private static final String[][] KILL_BY_RANK = {
            {"goblin"}, {"goblin", "orc"}, {"stone_soldier", "orc"}, {"venom_ant", "stone_soldier"},
            {"ice_elf", "hell_hound"}, {"giant_ant", "demon_knight"}, {"demon_knight", "giant_ant"}};

    public static void refreshOffers(ServerPlayer sp) {
        SLPlayer d = ModCaps.get(sp);
        long day = today(sp);
        if (d.bountyDay == day && !d.offers.isEmpty()) { PlayerSync.sync(sp); return; }
        d.bountyDay = day;
        d.offers.clear();
        RandomSource r = sp.getRandom();
        int ri = Ranks.index(d.rank());
        for (int i = 0; i < 4; i++) {
            SLPlayer.Quest q = new SLPlayer.Quest();
            int qr = Math.max(0, Math.min(6, ri + r.nextInt(3) - 1));
            double mul = Math.pow(1.9, qr);
            int t = r.nextInt(3);
            if (t == 0) {
                String[] pool = KILL_BY_RANK[qr];
                q.type = "kill"; q.target = pool[r.nextInt(pool.length)]; q.need = 4 + r.nextInt(8);
            } else if (t == 1 && qr >= 1) {
                q.type = "gate"; q.target = Ranks.ORDER[Math.max(0, qr - 1)]; q.need = 1;
            } else if (t == 2) {
                String[] cr = {"mana_crystal_e", "mana_crystal_d", "mana_crystal_c", "mana_crystal_b", "mana_crystal_a", "mana_crystal_s", "mana_crystal_s"};
                q.type = "collect"; q.target = cr[Math.max(0, qr - 1)]; q.need = 3 + r.nextInt(5);
            } else {
                q.type = "kill"; q.target = "any"; q.need = 10 + r.nextInt(15);
            }
            q.rewardGold = (int) (120 * mul * (1 + q.need / 10.0));
            q.rewardXp = (int) (90 * mul * (1 + q.need / 12.0));
            if (r.nextInt(4) == 0) { q.rewardItem = qr >= 3 ? "hp_potion_large" : "hp_potion_medium"; q.rewardCount = 1 + r.nextInt(2); }
            d.offers.add(q);
        }
        PlayerSync.sync(sp);
    }

    public static boolean accept(ServerPlayer sp, int idx) {
        SLPlayer d = ModCaps.get(sp);
        if (idx < 0 || idx >= d.offers.size()) return false;
        if (d.quests.size() >= MAX_ACTIVE) { Sys.warn(sp, "gui.sololeveling.quest_full"); return false; }
        SLPlayer.Quest q = d.offers.remove(idx);
        d.quests.add(q);
        Sys.notify(sp, Sys.QUEST, Component.translatable("gui.sololeveling.quest_accepted"), questTitle(q));
        PlayerSync.sync(sp);
        return true;
    }

    public static Component questTitle(SLPlayer.Quest q) {
        Component target = switch (q.type) {
            case "gate" -> Ranks.tag(q.target);
            case "collect" -> Component.translatable("item.sololeveling." + q.target);
            default -> q.target.equals("any") ? Component.translatable("gui.sololeveling.any_monster") : Component.translatable("entity.sololeveling." + q.target);
        };
        return Component.translatable("gui.sololeveling.quest." + q.type, q.need, target);
    }

    public static void claim(ServerPlayer sp, int idx) {
        SLPlayer d = ModCaps.get(sp);
        if (idx < 0 || idx >= d.quests.size()) return;
        SLPlayer.Quest q = d.quests.get(idx);
        if (q.type.equals("collect")) {
            var item = ModItems.get(q.target);
            int have = 0;
            for (ItemStack s : sp.getInventory().items) if (s.getItem() == item) have += s.getCount();
            if (have < q.need) { Sys.warn(sp, "gui.sololeveling.quest_not_done"); return; }
            int left = q.need;
            for (ItemStack s : sp.getInventory().items) {
                if (s.getItem() == item && left > 0) { int t = Math.min(left, s.getCount()); s.shrink(t); left -= t; }
            }
            q.have = q.need;
        }
        if (!q.done()) { Sys.warn(sp, "gui.sololeveling.quest_not_done"); return; }
        d.quests.remove(idx);
        d.gold += q.rewardGold;
        if (!q.rewardItem.isEmpty()) sp.getInventory().add(new ItemStack(ModItems.get(q.rewardItem), q.rewardCount));
        Sys.notify(sp, Sys.REWARD, Component.translatable("gui.sololeveling.quest_complete"), Component.translatable("gui.sololeveling.daily_reward", q.rewardXp, q.rewardGold));
        XpHandler.giveXp(sp, q.rewardXp);
    }

    public static void abandon(ServerPlayer sp, int idx) {
        SLPlayer d = ModCaps.get(sp);
        if (idx >= 0 && idx < d.quests.size()) { d.quests.remove(idx); PlayerSync.sync(sp); }
    }

    // ------------------------------------------------------------------ progress hooks
    public static void onKill(ServerPlayer sp, LivingEntity victim) {
        SLPlayer d = ModCaps.get(sp);
        d.kills++;
        d.dqKills++;
        String id = EntityType.getKey(victim.getType()).getPath();
        boolean changed = false;
        for (SLPlayer.Quest q : d.quests) {
            if (!q.type.equals("kill") || q.done()) continue;
            if (q.target.equals("any") || q.target.equals(id)) { q.have++; changed = true; if (q.done()) Sys.notify(sp, Sys.QUEST, Component.translatable("gui.sololeveling.quest_ready"), questTitle(q)); }
        }
        if (d.dqKills == d.dqKillTarget) Sys.notify(sp, Sys.QUEST, Component.translatable("gui.sololeveling.daily_objective"), Component.translatable("gui.sololeveling.daily_kills_done"));
    }

    public static void onGateCleared(ServerPlayer sp, String rank) {
        SLPlayer d = ModCaps.get(sp);
        for (SLPlayer.Quest q : d.quests) {
            if (!q.type.equals("gate") || q.done()) continue;
            if (Ranks.index(rank) >= Ranks.index(q.target)) { q.have++; if (q.done()) Sys.notify(sp, Sys.QUEST, Component.translatable("gui.sololeveling.quest_ready"), questTitle(q)); }
        }
    }
}
