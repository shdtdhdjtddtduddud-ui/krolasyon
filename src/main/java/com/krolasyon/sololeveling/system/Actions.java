package com.krolasyon.sololeveling.system;

import com.krolasyon.sololeveling.net.Net;
import com.krolasyon.sololeveling.shadow.ShadowManager;
import com.krolasyon.sololeveling.world.Dialogs;
import com.krolasyon.sololeveling.world.NewsManager;
import com.krolasyon.sololeveling.world.Regions;
import com.krolasyon.sololeveling.world.Shop;
import net.minecraft.server.level.ServerPlayer;

/** Handles every request the client sends. All values are validated here. */
public final class Actions {
    private Actions() {}

    public static void handle(ServerPlayer p, String action, int a, int b, String s) {
        HunterData d = HunterCapability.get(p);
        switch (action) {
            case "awaken" -> {
                if (d.awakened) return;
                if (a == 1) {
                    d.awakened = true;
                    d.mana = d.maxMana();
                    Progression.applyAttributes(p, d);
                    Progression.autoSlot(d);
                    Sys.notify(p, Sys.INFO, Sys.t("system.title"), Sys.t("awaken.accepted"));
                    Progression.checkQuest(p);
                    DailyQuest.reset(p, d, false);
                } else {
                    Sys.warn(p, "awaken.declined");
                }
                d.markDirty();
            }
            case "awaken_prompt" -> {
                if (!d.awakened) Net.to(p, new Net.Open("awaken", new net.minecraft.nbt.CompoundTag()));
            }
            case "stat" -> {
                if (a >= 0 && a < Stat.values().length) Progression.addStat(p, Stat.values()[a], Math.max(1, Math.min(b, 100)));
            }
            case "slot" -> {
                if (a < 0 || a >= HunterData.SLOTS) return;
                Skill sk = Skill.byOrdinal(b);
                if (sk != null && (!sk.active || !d.hasSkill(sk))) return;
                for (int i = 0; i < HunterData.SLOTS; i++) if (d.slots[i] == sk) d.slots[i] = null;
                d.slots[a] = sk;
                d.markDirty();
            }
            case "skill" -> SkillExecutor.useSlot(p, a);
            case "arise" -> ShadowManager.arise(p);
            case "shadows" -> ShadowManager.toggle(p);
            case "shadow_summon" -> ShadowManager.summonOne(p, a);
            case "shadow_release" -> ShadowManager.release(p, a);
            case "title" -> {
                Title t = Title.byOrdinal(a);
                if (t == Title.NONE || d.titles.contains(t)) {
                    d.title = t;
                    Progression.applyAttributes(p, d);
                    d.markDirty();
                }
            }
            case "dialog" -> Dialogs.choose(p, a, s);
            case "shop" -> Shop.buy(p, b, s, Math.max(1, Math.min(a, 64)));
            case "sell" -> Shop.sellStones(p);
            case "travel" -> Regions.travelFromMap(p, s);
            case "daily_claim" -> DailyQuest.claim(p);
            case "news" -> NewsManager.get(p.server).open(p);
            case "map" -> Regions.openMap(p);
            case "system_shop" -> Shop.open(p, Shop.SYSTEM);
            default -> {}
        }
    }
}
