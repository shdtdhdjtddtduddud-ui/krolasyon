package com.krolasyon.bosses.faction;

import com.krolasyon.bosses.KrolasyonBosses;
import com.krolasyon.bosses.entity.BossEntity;
import com.krolasyon.bosses.net.Net;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Server side of every conversation: envoys walk the player through three trials, rulers offer the oath or a duel.
 * All texts are translation keys (see gen/lore.py); the client turns them into words.
 */
public final class Dialogue {
    private Dialogue() {}

    public static final int HUNT = 8, OFFER = 8, TRIBUTE = 12;
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private record Opt(String label, Runnable action) {}

    private static final class Session {
        final int npcId;
        final Faction faction;
        final boolean envoy;
        List<Runnable> actions = new ArrayList<>();

        Session(int npcId, Faction faction, boolean envoy) {
            this.npcId = npcId;
            this.faction = faction;
            this.envoy = envoy;
        }
    }

    // ------------------------------------------------------------------ helpers
    public static String materialId(Faction f) {
        return switch (f) {
            case EMBER -> "ember_shard";
            case BONE -> "bone_dust";
            case BLOOD -> "blood_vial";
            case SHADOW -> "shadow_essence";
            case ROT -> "rot_spore";
            default -> "soul_ember";
        };
    }

    public static String swordId(Faction f) {
        return switch (f) {
            case EMBER -> "ember_brand";
            case BONE -> "bone_reaper";
            case BLOOD -> "blood_thirst";
            case SHADOW -> "shadow_fang";
            default -> "rot_cleaver";
        };
    }

    public static String tomeId(Faction f) {
        return switch (f) {
            case EMBER -> "tome_fireball";
            case BONE -> "tome_bone_prison";
            case BLOOD -> "tome_blood_pact";
            case SHADOW -> "tome_void_step";
            default -> "tome_thorn_burst";
        };
    }

    public static Item item(String id) {
        Item i = ForgeRegistries.ITEMS.getValue(new ResourceLocation(KrolasyonBosses.MODID, id));
        return i == null ? net.minecraft.world.item.Items.AIR : i;
    }

    public static int count(ServerPlayer p, Item it) {
        int n = 0;
        for (ItemStack s : p.getInventory().items) if (s.is(it)) n += s.getCount();
        return n;
    }

    public static boolean take(ServerPlayer p, Item it, int n) {
        if (count(p, it) < n) return false;
        int left = n;
        for (ItemStack s : p.getInventory().items) {
            if (!s.is(it)) continue;
            int d = Math.min(left, s.getCount());
            s.shrink(d);
            left -= d;
            if (left <= 0) break;
        }
        return true;
    }

    public static void give(ServerPlayer p, String id, int n) {
        ItemStack st = new ItemStack(item(id), n);
        if (!p.getInventory().add(st)) p.drop(st, false);
    }

    private static String houseKey(Faction f) { return "faction.krolasyonbosses." + f.id; }

    private static String enemies(Faction f) {
        StringBuilder sb = new StringBuilder();
        for (Faction o : Faction.HOUSES) if (f.isEnemyOf(o)) sb.append(sb.length() > 0 ? " / " : "").append(o.trName);
        return sb.toString();
    }

    private static String allies(Faction f) {
        StringBuilder sb = new StringBuilder();
        for (Faction o : Faction.HOUSES) if (f.isAllyOf(o)) sb.append(sb.length() > 0 ? " / " : "").append(o.trName);
        return sb.toString();
    }

    // ------------------------------------------------------------------ entry points
    public static void open(ServerPlayer sp, BossEntity npc) {
        Faction f = npc.houseOf();
        if (f == null) return;
        PlayerData.init(sp);
        Session s = new Session(npc.getId(), f, npc.isEnvoy());
        SESSIONS.put(sp.getUUID(), s);
        if (npc.isEnvoy()) envoyMain(sp, s, npc); else rulerMain(sp, s, npc);
    }

    public static void onChoice(ServerPlayer sp, int npcId, int index) {
        Session s = SESSIONS.get(sp.getUUID());
        if (s == null || s.npcId != npcId) return;
        if (index < 0 || index >= s.actions.size()) {
            SESSIONS.remove(sp.getUUID());
            return;
        }
        Runnable a = s.actions.get(index);
        s.actions = new ArrayList<>();
        a.run();
    }

    private static void page(ServerPlayer sp, Session s, BossEntity npc, String textKey, List<String> args, List<Opt> opts) {
        s.actions = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (Opt o : opts) {
            labels.add(o.label);
            s.actions.add(o.action);
        }
        Net.CH.send(PacketDistributor.PLAYER.with(() -> sp), new Net.DialogueMsg(s.npcId, npc.getType().getDescriptionId(), s.faction.color, textKey, args, labels));
    }

    private static Opt leave(Session s, ServerPlayer sp) {
        return new Opt("dlg.opt.leave", () -> SESSIONS.remove(sp.getUUID()));
    }

    @javax.annotation.Nullable
    private static BossEntity npcOf(ServerPlayer sp, Session s) {
        Entity e = sp.level().getEntity(s.npcId);
        return e instanceof BossEntity b && b.isAlive() ? b : null;
    }

    // ------------------------------------------------------------------ envoy
    private static void envoyMain(ServerPlayer sp, Session s, BossEntity npc) {
        Faction f = s.faction;
        int stage = PlayerData.stage(sp, f);
        int oath = PlayerData.oath(sp, f);
        List<Opt> o = new ArrayList<>();
        String key;
        List<String> args = new ArrayList<>();
        if (oath != PlayerData.OATH_NONE) {
            key = "dlg.env.after";
            args.add(f.trName);
            o.add(new Opt("dlg.opt.tribute", () -> claimTribute(sp, s)));
            o.add(new Opt("dlg.opt.trade", () -> trade(sp, s)));
        } else {
            switch (stage) {
                case 0 -> {
                    key = "dlg.env.stage0";
                    args.add(f.trName); args.add(enemies(f)); args.add(String.valueOf(HUNT));
                    o.add(new Opt("dlg.opt.accept", () -> {
                        PlayerData.setStage(sp, f, 1);
                        PlayerData.setCounter(sp, "hunt_" + f.id, 0);
                        PlayerData.addRep(sp, f, 3, false);
                        BossEntity n = npcOf(sp, s);
                        if (n != null) page(sp, s, n, "dlg.env.accepted", List.of(enemies(f), String.valueOf(HUNT)), List.of(leave(s, sp)));
                    }));
                }
                case 1 -> {
                    int kills = PlayerData.counter(sp, "hunt_" + f.id);
                    if (kills >= HUNT) {
                        key = "dlg.env.stage1.done";
                        args.add(String.valueOf(HUNT));
                        o.add(new Opt("dlg.opt.report", () -> {
                            PlayerData.setStage(sp, f, 2);
                            PlayerData.addRep(sp, f, 20, true);
                            give(sp, "hellsteel_nugget", 3);
                            BossEntity n = npcOf(sp, s);
                            if (n != null) page(sp, s, n, "dlg.env.reward1", List.of(f.trName), List.of(new Opt("dlg.opt.continue", () -> envoyMain(sp, s, n)), leave(s, sp)));
                        }));
                    } else {
                        key = "dlg.env.stage1.wait";
                        args.add(String.valueOf(kills)); args.add(String.valueOf(HUNT)); args.add(enemies(f));
                    }
                }
                case 2 -> {
                    Item mat = item(materialId(f));
                    key = "dlg.env.stage2";
                    args.add(String.valueOf(OFFER)); args.add(Component.translatable("item.krolasyonbosses." + materialId(f)).getString()); args.add(String.valueOf(count(sp, mat)));
                    o.add(new Opt("dlg.opt.deliver", () -> {
                        BossEntity n = npcOf(sp, s);
                        if (n == null) return;
                        if (take(sp, mat, OFFER)) {
                            PlayerData.setStage(sp, f, 3);
                            PlayerData.addRep(sp, f, 25, true);
                            give(sp, "hellsteel_ingot", 1);
                            page(sp, s, n, "dlg.env.reward2", List.of(f.trName), List.of(new Opt("dlg.opt.continue", () -> envoyMain(sp, s, n)), leave(s, sp)));
                        } else {
                            page(sp, s, n, "dlg.env.missing", List.of(String.valueOf(OFFER)), List.of(new Opt("dlg.opt.back", () -> envoyMain(sp, s, n)), leave(s, sp)));
                        }
                    }));
                }
                default -> {
                    key = "dlg.env.stage3";
                    args.add(f.trName);
                    o.add(new Opt("dlg.opt.directions", () -> directions(sp, s)));
                    o.add(new Opt("dlg.opt.trade", () -> trade(sp, s)));
                }
            }
        }
        o.add(new Opt("dlg.opt.lore", () -> lore(sp, s, 0)));
        o.add(new Opt("dlg.opt.politics", () -> {
            BossEntity n = npcOf(sp, s);
            if (n != null) page(sp, s, n, "dlg.env.politics", List.of(f.trName, allies(f), enemies(f)), List.of(new Opt("dlg.opt.back", () -> envoyMain(sp, s, n)), leave(s, sp)));
        }));
        o.add(leave(s, sp));
        page(sp, s, npc, key, args, o);
    }

    private static void lore(ServerPlayer sp, Session s, int n) {
        BossEntity npc = npcOf(sp, s);
        if (npc == null) return;
        Faction f = s.faction;
        List<Opt> o = new ArrayList<>();
        if (n < 2) o.add(new Opt("dlg.opt.more", () -> lore(sp, s, n + 1)));
        o.add(new Opt("dlg.opt.back", () -> { BossEntity b = npcOf(sp, s); if (b == null) return; if (b.isEnvoy()) envoyMain(sp, s, b); else rulerMain(sp, s, b); }));
        o.add(leave(s, sp));
        page(sp, s, npc, "dlg.lore." + f.id + "." + n, List.of(), o);
    }

    private static void directions(ServerPlayer sp, Session s) {
        BossEntity npc = npcOf(sp, s);
        if (npc == null) return;
        String text = "dlg.env.directions.none";
        List<String> args = new ArrayList<>();
        if (sp.level() instanceof ServerLevel sl) {
            TagKey<Structure> tag = TagKey.create(Registries.STRUCTURE, new ResourceLocation(KrolasyonBosses.MODID, s.faction.id + "_citadel"));
            BlockPos pos = sl.findNearestMapStructure(tag, sp.blockPosition(), 120, false);
            if (pos != null) {
                text = "dlg.env.directions";
                args.add(String.valueOf(pos.getX())); args.add(String.valueOf(pos.getZ()));
                args.add(String.valueOf((int) Math.sqrt(pos.distSqr(sp.blockPosition()))));
            }
        }
        page(sp, s, npc, text, args, List.of(new Opt("dlg.opt.back", () -> envoyMain(sp, s, npc)), leave(s, sp)));
    }

    // ------------------------------------------------------------------ trade / tribute
    private static void trade(ServerPlayer sp, Session s) {
        BossEntity npc = npcOf(sp, s);
        if (npc == null) return;
        Faction f = s.faction;
        String mat = Component.translatable("item.krolasyonbosses." + materialId(f)).getString();
        List<Opt> o = new ArrayList<>();
        o.add(new Opt("dlg.opt.trade1", () -> doTrade(sp, s, 8, 0, "hellsteel_ingot", 1)));
        o.add(new Opt("dlg.opt.trade2", () -> doTrade(sp, s, 10, 4, tomeId(f), 1)));
        o.add(new Opt("dlg.opt.trade3", () -> doTrade(sp, s, 16, 6, swordId(f), 1)));
        o.add(new Opt("dlg.opt.back", () -> { BossEntity b = npcOf(sp, s); if (b == null) return; if (b.isEnvoy()) envoyMain(sp, s, b); else rulerMain(sp, s, b); }));
        o.add(leave(s, sp));
        page(sp, s, npc, "dlg.trade", List.of(mat, f.trName), o);
    }

    private static void doTrade(ServerPlayer sp, Session s, int mats, int ingots, String result, int n) {
        BossEntity npc = npcOf(sp, s);
        if (npc == null) return;
        Faction f = s.faction;
        Item m = item(materialId(f)), ing = item("hellsteel_ingot");
        boolean ok = count(sp, m) >= mats && count(sp, ing) >= ingots;
        if (ok) {
            take(sp, m, mats);
            if (ingots > 0) take(sp, ing, ingots);
            give(sp, result, n);
        }
        page(sp, s, npc, ok ? "dlg.trade.ok" : "dlg.trade.fail", List.of(), List.of(new Opt("dlg.opt.trade", () -> trade(sp, s)), leave(s, sp)));
    }

    private static void claimTribute(ServerPlayer sp, Session s) {
        BossEntity npc = npcOf(sp, s);
        if (npc == null) return;
        Faction f = s.faction;
        long day = sp.level().getGameTime() / 24000L;
        String key = "tribute_" + f.id;
        boolean can = PlayerData.root(sp).getLong(key) != day + 1;
        if (can) {
            PlayerData.root(sp).putLong(key, day + 1);
            give(sp, materialId(f), 6);
            give(sp, "hellsteel_nugget", 4);
            if (PlayerData.oath(sp, f) == PlayerData.OATH_CONQUEROR) give(sp, "hellsteel_nugget", 4);
        }
        page(sp, s, npc, can ? "dlg.tribute.ok" : "dlg.tribute.wait", List.of(f.trName), List.of(new Opt("dlg.opt.back", () -> envoyMain(sp, s, npc)), leave(s, sp)));
    }

    // ------------------------------------------------------------------ ruler
    private static void rulerMain(ServerPlayer sp, Session s, BossEntity npc) {
        Faction f = s.faction;
        int stage = PlayerData.stage(sp, f);
        int oath = PlayerData.oath(sp, f);
        int rep = PlayerData.rep(sp, f);
        List<Opt> o = new ArrayList<>();
        String key;
        List<String> args = new ArrayList<>();
        args.add(f.trName);
        if (oath == PlayerData.OATH_ALLY) {
            key = "dlg.ruler.ally";
            o.add(new Opt("dlg.opt.tribute", () -> claimTributeRuler(sp, s)));
            o.add(new Opt("dlg.opt.trade", () -> trade(sp, s)));
        } else if (oath == PlayerData.OATH_CONQUEROR) {
            key = "dlg.ruler.conqueror";
            o.add(new Opt("dlg.opt.tribute", () -> claimTributeRuler(sp, s)));
        } else if (stage < 3) {
            key = "dlg.ruler.early";
        } else if (rep >= PlayerData.ALLY_AT) {
            key = "dlg.ruler.offer";
            o.add(new Opt("dlg.opt.swear", () -> swear(sp, s)));
        } else {
            key = "dlg.ruler.tribute";
            args.add(String.valueOf(TRIBUTE)); args.add(Component.translatable("item.krolasyonbosses." + materialId(f)).getString()); args.add(String.valueOf(rep));
            o.add(new Opt("dlg.opt.pay", () -> {
                BossEntity n = npcOf(sp, s);
                if (n == null) return;
                if (take(sp, item(materialId(f)), TRIBUTE)) {
                    PlayerData.addRep(sp, f, 15, true);
                    page(sp, s, n, "dlg.ruler.paid", List.of(), List.of(new Opt("dlg.opt.continue", () -> rulerMain(sp, s, n)), leave(s, sp)));
                } else {
                    page(sp, s, n, "dlg.env.missing", List.of(String.valueOf(TRIBUTE)), List.of(new Opt("dlg.opt.back", () -> rulerMain(sp, s, n)), leave(s, sp)));
                }
            }));
        }
        if (oath == PlayerData.OATH_NONE) o.add(new Opt("dlg.opt.challenge", () -> {
            BossEntity n = npcOf(sp, s);
            if (n == null) return;
            page(sp, s, n, "dlg.ruler.challenge", List.of(f.trName), List.of(new Opt("dlg.opt.fight", () -> {
                BossEntity b = npcOf(sp, s);
                SESSIONS.remove(sp.getUUID());
                if (b != null) b.provoke(sp);
            }), new Opt("dlg.opt.back", () -> rulerMain(sp, s, n))));
        }));
        o.add(new Opt("dlg.opt.lore", () -> lore(sp, s, 0)));
        o.add(leave(s, sp));
        page(sp, s, npc, key, args, o);
    }

    private static void claimTributeRuler(ServerPlayer sp, Session s) {
        BossEntity npc = npcOf(sp, s);
        if (npc == null) return;
        Faction f = s.faction;
        long day = sp.level().getGameTime() / 24000L;
        String key = "tribute_" + f.id;
        boolean can = PlayerData.root(sp).getLong(key) != day + 1;
        if (can) {
            PlayerData.root(sp).putLong(key, day + 1);
            give(sp, materialId(f), 8);
            give(sp, "hellsteel_nugget", 6);
        }
        page(sp, s, npc, can ? "dlg.tribute.ok" : "dlg.tribute.wait", List.of(f.trName), List.of(new Opt("dlg.opt.back", () -> rulerMain(sp, s, npc)), leave(s, sp)));
    }

    private static void swear(ServerPlayer sp, Session s) {
        BossEntity npc = npcOf(sp, s);
        if (npc == null) return;
        Faction f = s.faction;
        PlayerData.setOath(sp, f, PlayerData.OATH_ALLY);
        PlayerData.setStage(sp, f, 4);
        PlayerData.addRep(sp, f, 30, true);
        give(sp, f.id + "_sigil", 1);
        give(sp, swordId(f), 1);
        sp.server.getPlayerList().broadcastSystemMessage(Component.translatable("msg.oath.sworn", sp.getDisplayName(), f.trName).withStyle(st -> st.withColor(f.color)), false);
        page(sp, s, npc, "dlg.ruler.oath_done", List.of(f.trName), List.of(new Opt("dlg.opt.continue", () -> rulerMain(sp, s, npc)), leave(s, sp)));
    }

    // ------------------------------------------------------------------ called when a ruler falls
    public static void conquered(ServerPlayer killer, Faction f) {
        if (PlayerData.oath(killer, f) == PlayerData.OATH_NONE) PlayerData.setOath(killer, f, PlayerData.OATH_CONQUEROR);
        PlayerData.setStage(killer, f, 4);
        give(killer, f.id + "_sigil", 1);
        int rep = PlayerData.rep(killer, f);
        PlayerData.addRep(killer, f, 40 - rep, false);
        for (Faction o : Faction.HOUSES) {
            if (f.isAllyOf(o)) PlayerData.addRep(killer, o, -12, false);
            else if (f.isEnemyOf(o)) PlayerData.addRep(killer, o, 12, false);
        }
        killer.server.getPlayerList().broadcastSystemMessage(Component.translatable("msg.house.conquered", killer.getDisplayName(), f.trName).withStyle(st -> st.withColor(f.color)), false);
    }
}
