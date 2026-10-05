package com.krolasyon.sololeveling.world;

import com.krolasyon.sololeveling.entity.MobKind;
import com.krolasyon.sololeveling.entity.SLMonster;
import com.krolasyon.sololeveling.registry.ModItems;
import com.krolasyon.sololeveling.system.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Guild membership, reputation and guild requests ("kill:<mob>" or "gate:<rank>"). */
public final class GuildManager {
    private GuildManager() {}

    public static Guild guildOfMaster(String role) {
        return switch (role) {
            case "master_hunters" -> Guild.HUNTERS;
            case "master_white_tiger" -> Guild.WHITE_TIGER;
            case "master_fiend" -> Guild.FIEND;
            case "master_knights" -> Guild.KNIGHTS;
            case "yoo_jinho" -> Guild.AHJIN;
            default -> null;
        };
    }

    public static Component join(ServerPlayer p, Guild g) {
        HunterData d = HunterCapability.get(p);
        if (d.guild != Guild.NONE) return Sys.t("guild.already");
        if (!d.licensed) return Sys.t("guild.need_license");
        if (d.rank.ordinal() < g.minRank.ordinal()) return Sys.t("guild.need_rank", Component.translatable(g.minRank.langKey()));
        if (g == Guild.AHJIN && d.gold < 1000) return Sys.t("guild.ahjin_gold");
        if (g == Guild.AHJIN) d.gold -= 1000;
        d.guild = g;
        d.guildRep = g == Guild.AHJIN ? 100 : 0;
        d.guildTask = "";
        d.markDirty();
        Sys.notify(p, Sys.REWARD, Sys.t("guild.title"), Sys.t("guild.joined", Component.translatable(g.langKey())));
        NewsManager.get(p.server).guildJoin(p, g);
        return Sys.t(g == Guild.AHJIN ? "guild.ahjin_founded" : "guild.welcome", Component.translatable(g.langKey()));
    }

    public static Component leave(ServerPlayer p) {
        HunterData d = HunterCapability.get(p);
        Guild g = d.guild;
        d.guild = Guild.NONE;
        d.guildRep = 0;
        d.guildTask = "";
        d.markDirty();
        return Sys.t("guild.left", Component.translatable(g.langKey()));
    }

    /** Gives a new task or turns in a finished one. */
    public static Component taskPage(ServerPlayer p) {
        HunterData d = HunterCapability.get(p);
        if (!d.guildTask.isEmpty() && d.guildTaskProgress >= d.guildTaskGoal) {
            int rep = 40 + d.guildTaskGoal * 3 + d.rank.ordinal() * 15;
            long gold = 100L + d.rank.ordinal() * 120L + d.guildTaskGoal * 10L;
            d.guildRep += rep;
            d.gold += gold;
            d.guildTask = "";
            d.markDirty();
            Progression.give(p, new ItemStack(ModItems.RANDOM_BOX.get()));
            Progression.addExp(p, 50L + d.level * 10L);
            Sys.notify(p, Sys.REWARD, Sys.t("guild.title"), Sys.t("guild.task_done", rep, gold));
            return Sys.t("guild.task_thanks");
        }
        if (d.guildTask.isEmpty()) newTask(p, d);
        return Sys.t("guild.task_current", describe(d), d.guildTaskProgress, d.guildTaskGoal);
    }

    private static void newTask(ServerPlayer p, HunterData d) {
        var r = p.getRandom();
        if (r.nextInt(3) == 0) {
            Rank gr = Rank.byOrdinal(Math.max(0, Math.min(Rank.S.ordinal(), d.rank.ordinal() - r.nextInt(2))));
            d.guildTask = "gate:" + gr.label;
            d.guildTaskGoal = 1 + r.nextInt(2);
        } else {
            List<MobKind> pool = new ArrayList<>();
            for (MobKind k : MobKind.values())
                if (!k.boss && k.rank.ordinal() <= d.rank.ordinal() + 1 && k.rank.ordinal() >= d.rank.ordinal() - 2) pool.add(k);
            if (pool.isEmpty()) pool.add(MobKind.GOBLIN);
            MobKind k = pool.get(r.nextInt(pool.size()));
            d.guildTask = "kill:" + k.id();
            d.guildTaskGoal = 8 + r.nextInt(12);
        }
        d.guildTaskProgress = 0;
        d.markDirty();
    }

    public static Component describe(HunterData d) {
        if (d.guildTask.startsWith("kill:"))
            return Sys.t("guild.task_kill", Component.translatable("entity.sololeveling." + d.guildTask.substring(5)));
        if (d.guildTask.startsWith("gate:")) return Sys.t("guild.task_gate", d.guildTask.substring(5));
        return Component.empty();
    }

    public static void onKill(ServerPlayer p, LivingEntity dead) {
        HunterData d = HunterCapability.get(p);
        if (d.guild == Guild.NONE || !d.guildTask.startsWith("kill:") || !(dead instanceof SLMonster m)) return;
        if (!d.guildTask.substring(5).equals(m.kind.id())) return;
        if (d.guildTaskProgress < d.guildTaskGoal) {
            d.guildTaskProgress++;
            d.markDirty();
            if (d.guildTaskProgress == d.guildTaskGoal) Sys.notify(p, Sys.QUEST, Sys.t("guild.title"), Sys.t("guild.task_ready"));
        }
    }

    public static void onGateCleared(ServerPlayer p, Rank rank) {
        HunterData d = HunterCapability.get(p);
        if (d.guild == Guild.NONE) return;
        d.guildRep += 10 + rank.ordinal() * 10;
        if (d.guildTask.startsWith("gate:") && d.guildTaskProgress < d.guildTaskGoal) {
            Rank need = null;
            for (Rank r : Rank.values()) if (r.label.equals(d.guildTask.substring(5))) need = r;
            if (need != null && rank.ordinal() >= need.ordinal()) {
                d.guildTaskProgress++;
                if (d.guildTaskProgress == d.guildTaskGoal) Sys.notify(p, Sys.QUEST, Sys.t("guild.title"), Sys.t("guild.task_ready"));
            }
        }
        d.markDirty();
    }

    /** Experience multiplier from guild perks. */
    public static float expBonus(HunterData d) {
        int gr = Guild.guildRankFor(d.guildRep);
        return switch (d.guild) {
            case HUNTERS -> 1.10F + gr * 0.03F;
            case AHJIN -> 1.05F + gr * 0.05F;
            case NONE -> 1F;
            default -> 1.05F + gr * 0.02F;
        };
    }
}
