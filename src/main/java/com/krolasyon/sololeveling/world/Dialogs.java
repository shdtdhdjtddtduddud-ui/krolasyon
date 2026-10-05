package com.krolasyon.sololeveling.world;

import com.krolasyon.sololeveling.entity.HunterNpc;
import com.krolasyon.sololeveling.net.Net;
import com.krolasyon.sololeveling.registry.ModItems;
import com.krolasyon.sololeveling.system.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Conversations with NPCs. The server decides every page; the client only displays it. */
public final class Dialogs {
    private Dialogs() {}

    record Option(String id, Component label) {}

    public static void open(ServerPlayer p, HunterNpc npc) { page(p, npc, "main"); }

    public static void choose(ServerPlayer p, int npcId, String option) {
        Entity e = p.level().getEntity(npcId);
        if (!(e instanceof HunterNpc npc) || npc.distanceToSqr(p) > 8 * 8) return;
        page(p, npc, option);
    }

    private static Component t(String k, Object... a) { return Sys.t(k, a); }

    private static void page(ServerPlayer p, HunterNpc npc, String id) {
        HunterData d = HunterCapability.get(p);
        String role = npc.role();
        List<Option> opts = new ArrayList<>();
        Component text;
        if (id.equals("bye")) {
            Net.to(p, new Net.Open("close", new CompoundTag()));
            return;
        }
        Guild roleGuild = GuildManager.guildOfMaster(role);
        if (roleGuild != null) {
            text = guildPage(p, d, roleGuild, id, opts);
        } else switch (role) {
            case "receptionist" -> {
                switch (id) {
                    case "register" -> {
                        if (!d.awakened) text = t("dialog.receptionist.not_awakened");
                        else {
                            boolean first = !d.licensed;
                            Progression.reassess(p);
                            if (first) Progression.give(p, new ItemStack(ModItems.HUNTER_LICENSE.get()));
                            text = t(first ? "dialog.receptionist.registered" : "dialog.receptionist.reassessed", Component.translatable(d.rank.langKey()));
                        }
                    }
                    case "sell" -> {
                        long got = Shop.sellStones(p);
                        text = got > 0 ? t("dialog.receptionist.sold", got) : t("dialog.receptionist.no_stones");
                    }
                    case "gates" -> text = t("dialog.receptionist.gates", GateManager.count(p.server));
                    default -> text = t(d.licensed ? "dialog.receptionist.main_licensed" : "dialog.receptionist.main", p.getName(), Component.translatable(d.rank.langKey()));
                }
                opts.add(new Option("register", t(d.licensed ? "option.reassess" : "option.register")));
                opts.add(new Option("sell", t("option.sell_stones")));
                opts.add(new Option("gates", t("option.gates")));
            }
            case "chairman" -> {
                if (id.equals("jeju") && d.rank.ordinal() >= Rank.S.ordinal()) {
                    d.waypoints.add("jeju_island");
                    d.markDirty();
                    text = t("dialog.chairman.jeju_ok");
                    opts.add(new Option("go_jeju", t("option.go_jeju")));
                } else if (id.equals("go_jeju")) {
                    Regions.teleport(p, "jeju_island");
                    Net.to(p, new Net.Open("close", new CompoundTag()));
                    return;
                } else {
                    text = t(d.rank.ordinal() >= Rank.S.ordinal() ? "dialog.chairman.main_s" : d.rank.ordinal() >= Rank.B.ordinal() ? "dialog.chairman.main_b" : "dialog.chairman.main", p.getName());
                    if (d.rank.ordinal() >= Rank.S.ordinal()) opts.add(new Option("jeju", t("option.jeju")));
                }
            }
            case "woo_jinchul" -> {
                text = t("dialog.woo_jinchul." + (id.equals("tips") ? "tips" + p.getRandom().nextInt(4) : "main"));
                opts.add(new Option("tips", t("option.tips")));
            }
            case "cha_haein" -> text = t("dialog.cha_haein." + (d.job == Job.SHADOW_MONARCH ? "monarch" : "line" + p.getRandom().nextInt(4)));
            case "healer" -> {
                int cost = 20 + d.level * 2;
                if (id.equals("heal")) {
                    if (d.gold >= cost) {
                        d.gold -= cost;
                        p.setHealth(p.getMaxHealth());
                        p.removeAllEffects();
                        d.mana = d.maxMana();
                        d.fatigue = 0;
                        d.markDirty();
                        text = t("dialog.healer.healed");
                    } else text = t("dialog.no_gold");
                } else {
                    text = t("dialog.healer.main", cost);
                    opts.add(new Option("heal", t("option.heal", cost)));
                }
            }
            case "merchant" -> {
                Shop.open(p, Shop.MERCHANT);
                return;
            }
            case "blacksmith" -> {
                Shop.open(p, Shop.SMITH);
                return;
            }
            case "alchemist" -> {
                Shop.open(p, Shop.ALCHEMIST);
                return;
            }
            case "guide" -> {
                switch (id) {
                    case "map" -> {
                        Regions.openMap(p);
                        return;
                    }
                    case "portals" -> text = t("dialog.guide.portals");
                    case "system" -> text = t("dialog.guide.system");
                    default -> text = t("dialog.guide.main");
                }
                opts.add(new Option("portals", t("option.portals")));
                opts.add(new Option("system", t("option.system_help")));
                opts.add(new Option("map", t("option.map")));
            }
            case "reporter" -> {
                NewsManager.get(p.server).open(p);
                return;
            }
            case "jinah" -> {
                if (id.equals("rest")) {
                    p.setHealth(p.getMaxHealth());
                    d.mana = d.maxMana();
                    d.fatigue = 0;
                    p.getFoodData().eat(20, 1F);
                    d.markDirty();
                    text = t("dialog.jinah.rested");
                } else {
                    text = t("dialog.jinah.line" + p.getRandom().nextInt(4), p.getName());
                    opts.add(new Option("rest", t("option.rest")));
                }
            }
            case "hunter" -> text = t("dialog.hunter.line" + p.getRandom().nextInt(8));
            default -> text = t("dialog.citizen.line" + p.getRandom().nextInt(10));
        }
        opts.add(new Option("bye", t("option.bye")));
        CompoundTag tag = new CompoundTag();
        tag.putInt("npc", npc.getId());
        tag.putString("name", Component.Serializer.toJson(npc.getDisplayName()));
        tag.putString("text", Component.Serializer.toJson(text));
        tag.putString("skin", npc.skin());
        ListTag l = new ListTag();
        for (Option o : opts) {
            CompoundTag ot = new CompoundTag();
            ot.putString("id", o.id());
            ot.putString("label", Component.Serializer.toJson(o.label()));
            l.add(ot);
        }
        tag.put("options", l);
        tag.putLong("gold", d.gold);
        Net.to(p, new Net.Open("dialog", tag));
    }

    private static Component guildPage(ServerPlayer p, HunterData d, Guild g, String id, List<Option> opts) {
        Component text;
        Component gname = Component.translatable(g.langKey());
        switch (id) {
            case "join" -> text = GuildManager.join(p, g);
            case "leave" -> text = GuildManager.leave(p);
            case "task" -> text = GuildManager.taskPage(p);
            case "shop" -> {
                Shop.open(p, Shop.GUILD);
                return t("dialog.guild.shop");
            }
            default -> text = t("dialog.guild." + g.id() + (d.guild == g ? ".member" : ".main"), p.getName(), gname,
                    Component.translatable("sololeveling.guild_rank." + Guild.guildRankFor(d.guildRep)));
        }
        if (d.guild == g) {
            opts.add(new Option("task", t("option.guild_task")));
            opts.add(new Option("shop", t("option.guild_shop")));
            opts.add(new Option("leave", t("option.leave_guild")));
        } else if (d.guild == Guild.NONE) {
            opts.add(new Option("join", t(g == Guild.AHJIN ? "option.found_ahjin" : "option.join_guild", gname)));
        }
        return text;
    }
}
