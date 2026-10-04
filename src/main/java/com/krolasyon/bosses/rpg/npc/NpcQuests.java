package com.krolasyon.bosses.rpg.npc;

import com.krolasyon.bosses.rpg.data.PlayerRpg;
import com.krolasyon.bosses.rpg.data.Quest;
import com.krolasyon.bosses.rpg.data.RpgWorldData;
import com.krolasyon.bosses.rpg.def.MonsterDef;
import com.krolasyon.bosses.rpg.def.RpgDefs;
import com.krolasyon.bosses.rpg.def.RpgDefs.RegionId;
import com.krolasyon.bosses.rpg.item.RpgItems;
import com.krolasyon.bosses.rpg.magic.PlayerMagic;
import com.krolasyon.bosses.rpg.world.Kingdom;
import com.krolasyon.bosses.rpg.world.Site;
import com.krolasyon.bosses.rpg.world.WorldMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/** Generates, tracks and pays out side quests. */
public final class NpcQuests {
    private NpcQuests() {}

    public static final int MAX_ACTIVE = 6;

    private static final Object[][] FETCH = {
            {Items.WHEAT, 16, "buğday"}, {Items.IRON_INGOT, 6, "demir külçesi"}, {Items.LEATHER, 8, "deri"}, {Items.COAL, 16, "kömür"},
            {Items.OAK_LOG, 24, "meşe kütüğü"}, {Items.COBBLESTONE, 48, "kırık taş"}, {Items.WHITE_WOOL, 12, "yün"}, {Items.APPLE, 8, "elma"},
            {Items.SALMON, 6, "somon"}, {Items.STRING, 12, "ip"}, {Items.FEATHER, 12, "tüy"}, {Items.HONEYCOMB, 4, "bal peteği"}
    };


    @Nullable
    private static MonsterDef randomMonsterNear(RandomSource r, RegionId region, int maxDanger) {
        List<MonsterDef> pool = new ArrayList<>();
        for (MonsterDef d : RpgDefs.MONSTERS) {
            if (d.danger() > maxDanger) continue;
            for (RegionId x : d.regions()) if (x == region) { pool.add(d); break; }
        }
        if (pool.isEmpty()) for (MonsterDef d : RpgDefs.MONSTERS) if (d.danger() <= maxDanger) pool.add(d);
        return pool.isEmpty() ? null : pool.get(r.nextInt(pool.size()));
    }

    public static Quest make(ServerPlayer p, RpgNpc npc, boolean guild) {
        RandomSource r = p.getRandom();
        PlayerRpg d = RpgWorldData.player(p);
        RpgWorldData w = RpgWorldData.get(p.server);
        Quest q = new Quest();
        q.giver = npc.getUUID();
        q.giverName = npc.npcName();
        q.kingdom = npc.kingdomId();
        q.guildQuest = guild;
        int maxDanger = Math.min(5, 1 + d.level / 6 + (guild ? Math.max(0, d.guild) / 2 : 0));
        RegionId region = com.krolasyon.bosses.rpg.world.WorldMap.regionAt(p.serverLevel(), npc.blockPosition());
        int kind = r.nextInt(guild ? 4 : 5);
        if (guild && d.guild >= 3 && r.nextInt(3) == 0) kind = 9;
        switch (kind) {
            case 0, 1 -> {
                MonsterDef m = randomMonsterNear(r, region, maxDanger);
                if (m == null) { kind = 2; break; }
                q.type = Quest.Type.KILL;
                q.target = m.id();
                q.targetName = m.name();
                q.count = 3 + r.nextInt(4);
                q.title = m.name() + " Tehdidi";
                q.rewardCopper = q.count * (6 + m.danger() * 8);
                q.rewardXp = q.count * (8 + m.danger() * 6);
            }
            case 2 -> {
                q.type = Quest.Type.KILL_ANY;
                q.count = 6 + r.nextInt(8);
                q.targetName = "canavar";
                q.title = "Yolları Temizle";
                q.rewardCopper = q.count * 8;
                q.rewardXp = q.count * 10;
            }
            case 3 -> {
                Object[] f = FETCH[r.nextInt(FETCH.length)];
                Item it = (Item) f[0];
                q.type = Quest.Type.FETCH;
                q.target = String.valueOf(ForgeRegistries.ITEMS.getKey(it));
                q.targetName = (String) f[2];
                q.count = (Integer) f[1];
                q.title = "Erzak Toplama";
                q.rewardCopper = 30 + r.nextInt(40);
                q.rewardXp = 40;
            }
            case 4 -> {
                Site s = null;
                for (int i = 0; i < 10 && s == null; i++) {
                    Site c = w.sites.get(r.nextInt(w.sites.size()));
                    if ((c.type == Site.Type.CITY || c.type == Site.Type.VILLAGE || c.type == Site.Type.CAPITAL) && c.distSq(npc.getX(), npc.getZ()) > 200 * 200 && c.distSq(npc.getX(), npc.getZ()) < 1600 * 1600) s = c;
                }
                if (s == null) { kind = 2; break; }
                q.type = Quest.Type.DELIVER;
                q.target = s.id;
                q.targetName = s.name;
                q.x = s.x;
                q.z = s.z;
                q.title = "Mektup: " + s.name;
                q.rewardCopper = 40 + (int) Math.sqrt(s.distSq(npc.getX(), npc.getZ())) / 8;
                q.rewardXp = 60;
                p.getInventory().add(new ItemStack(RpgItems.LETTER.get()));
            }
            case 9 -> {
                Site lair = null;
                double best = Double.MAX_VALUE;
                for (Site s : w.sites) {
                    if (s.type != Site.Type.LAIR || !s.bossAlive) continue;
                    double dd = s.distSq(p.getX(), p.getZ());
                    if (dd < best) { best = dd; lair = s; }
                }
                MonsterDef b = lair == null ? null : RpgDefs.BY_ID.get(lair.boss);
                if (b == null) { kind = 2; break; }
                q.type = Quest.Type.BOSS;
                q.target = b.id();
                q.targetName = b.name();
                q.x = lair.x;
                q.z = lair.z;
                q.title = "Efsanevi Av: " + b.name();
                q.rewardCopper = 1500;
                q.rewardXp = 1200;
                q.rewardGuild = 400;
            }
            default -> {}
        }
        if (q.title.isEmpty()) {
            q.type = Quest.Type.KILL_ANY;
            q.count = 8;
            q.targetName = "canavar";
            q.title = "Yolları Temizle";
            q.rewardCopper = 64;
            q.rewardXp = 80;
        }
        if (guild) {
            q.rewardGuild = Math.max(q.rewardGuild, 30 + q.rewardXp / 4);
            q.rewardCopper = (int) (q.rewardCopper * 1.3F);
        }
        q.rewardRep = 15 + q.rewardXp / 20;
        return q;
    }

    public static String brief(Quest q) {
        String where = q.type == Quest.Type.DELIVER || q.type == Quest.Type.BOSS ? " (" + q.x + ", " + q.z + " civarı)" : "";
        return q.title + ": " + q.describe() + where + "\nÖdül: " + coins(q.rewardCopper) + ", " + q.rewardXp + " TP" + (q.rewardGuild > 0 ? ", " + q.rewardGuild + " lonca puanı" : "");
    }

    public static String coins(int copper) {
        int g = copper / 100, s = (copper % 100) / 10, c = copper % 10;
        StringBuilder b = new StringBuilder();
        if (g > 0) b.append(g).append(" altın ");
        if (s > 0) b.append(s).append(" gümüş ");
        if (c > 0 || b.length() == 0) b.append(c).append(" bakır");
        return b.toString().trim();
    }

    public static void giveCoins(ServerPlayer p, int copper) {
        int g = copper / 100, s = (copper % 100) / 10, c = copper % 10;
        if (g > 0) give(p, new ItemStack(RpgItems.GOLD_COIN.get(), g));
        if (s > 0) give(p, new ItemStack(RpgItems.SILVER_COIN.get(), s));
        if (c > 0) give(p, new ItemStack(RpgItems.COPPER_COIN.get(), c));
    }

    public static void give(ServerPlayer p, ItemStack st) {
        if (!p.getInventory().add(st)) p.drop(st, false);
    }

    /** total coin value in inventory (copper) */
    public static int wallet(ServerPlayer p) {
        int v = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack st = p.getInventory().getItem(i);
            if (st.getItem() instanceof com.krolasyon.bosses.rpg.item.CoinItem c) v += c.value * st.getCount();
        }
        return v;
    }

    /** takes coins (making change), returns false if not enough */
    public static boolean pay(ServerPlayer p, int copper) {
        if (p.getAbilities().instabuild) return true;
        if (wallet(p) < copper) return false;
        int total = wallet(p);
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            if (p.getInventory().getItem(i).getItem() instanceof com.krolasyon.bosses.rpg.item.CoinItem) p.getInventory().setItem(i, ItemStack.EMPTY);
        }
        giveCoins(p, total - copper);
        return true;
    }

    public static int countItem(ServerPlayer p, Item it) {
        int n = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack st = p.getInventory().getItem(i);
            if (st.is(it)) n += st.getCount();
        }
        return n;
    }

    public static void takeItem(ServerPlayer p, Item it, int count) {
        for (int i = 0; i < p.getInventory().getContainerSize() && count > 0; i++) {
            ItemStack st = p.getInventory().getItem(i);
            if (st.is(it)) {
                int t = Math.min(count, st.getCount());
                st.shrink(t);
                count -= t;
            }
        }
    }

    public static void complete(ServerPlayer p, Quest q) {
        PlayerRpg d = RpgWorldData.player(p);
        d.quests.remove(q);
        d.questsDone++;
        giveCoins(p, q.rewardCopper);
        int lv = d.addXp(q.rewardXp);
        if (q.kingdom >= 0) d.addRep(q.kingdom, q.rewardRep);
        d.fame += 2;
        if (q.rewardGuild > 0 && d.addGuildXp(q.rewardGuild)) {
            p.sendSystemMessage(Component.literal("§6§lLonca rütben yükseldi: " + d.guildRank() + " sınıfı maceracı!"));
        }
        p.sendSystemMessage(Component.literal("§a✔ Görev tamamlandı: §f" + q.title + " §7(+" + coins(q.rewardCopper) + ", +" + q.rewardXp + " TP" + (q.kingdom >= 0 ? ", +" + q.rewardRep + " " + Kingdom.of(q.kingdom).title + " saygınlığı" : "") + ")"));
        p.playNotifySound(SoundEvents.PLAYER_LEVELUP, p.getSoundSource(), 0.6F, 1.4F);
        if (lv > 0) p.sendSystemMessage(Component.literal("§e§l★ Seviye atladın! Seviye " + d.level + " §7(+" + lv * 3 + " stat puanı, K tuşu)"));
        PlayerMagic.sync(p);
    }

    /** called when a monster dies to a player */
    public static void onKill(ServerPlayer p, @Nullable MonsterDef def, boolean bandit) {
        PlayerRpg d = RpgWorldData.player(p);
        boolean changed = false;
        for (Quest q : d.quests) {
            if (q.done()) continue;
            if (q.type == Quest.Type.KILL_ANY && def != null || q.type == Quest.Type.KILL && def != null && q.target.equals(def.id())
                    || q.type == Quest.Type.BOSS && def != null && q.target.equals(def.id()) || q.type == Quest.Type.BOUNTY && bandit) {
                q.progress++;
                changed = true;
                if (q.done()) p.sendSystemMessage(Component.literal("§a" + q.title + " hazır! Görevi veren kişiye geri dön: §f" + q.giverName));
                else p.displayClientMessage(Component.literal("§7" + q.title + ": " + q.progress + "/" + q.count), true);
            }
        }
        if (changed) PlayerMagic.sync(p);
    }
}
