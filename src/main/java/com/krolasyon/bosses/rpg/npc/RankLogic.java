package com.krolasyon.bosses.rpg.npc;

import com.krolasyon.bosses.rpg.data.PlayerRpg;
import com.krolasyon.bosses.rpg.item.RpgItems;
import com.krolasyon.bosses.rpg.util.FX;
import com.krolasyon.bosses.rpg.world.Kingdom;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

/** Social ranks: from penniless commoner to duke. The throne itself is won through the story. */
public final class RankLogic {
    private RankLogic() {}

    // rep, level, bossKills, cost (copper)
    private static final int[][] REQ = {
            {0, 1, 0, 0},
            {50, 3, 0, 0},
            {150, 6, 0, 100},
            {250, 10, 0, 300},
            {400, 15, 1, 1000},
            {550, 22, 3, 3000},
            {700, 30, 5, 6000},
            {850, 40, 8, 10000},
    };

    public static String request(ServerPlayer p, PlayerRpg d, RpgNpc npc) {
        int k = npc.kingdomId();
        int next = d.social + 1;
        boolean ruler = npc.role() == NpcRole.RULER;
        if (next >= PlayerRpg.SOCIAL.length - 1) return "Daha yükseğe çıkabileceğin tek yer taht. Ve tahtlar istenmez, kazanılır.";
        if (!ruler && next > 3) return "Ben sana bundan yüksek bir unvan veremem. Hükümdarın huzuruna çık.";
        if (next >= 3 && d.allegiance != k) return "Sadece bu krallığa bağlılık yemini edenlere unvan verilir.";
        int[] r = REQ[next];
        StringBuilder miss = new StringBuilder();
        if (d.repWith(k) < r[0]) miss.append("\n• Saygınlık: ").append(d.repWith(k)).append("/").append(r[0]);
        if (d.level < r[1]) miss.append("\n• Seviye: ").append(d.level).append("/").append(r[1]);
        if (d.bossKills < r[2]) miss.append("\n• Yenilen efsanevi canavar: ").append(d.bossKills).append("/").append(r[2]);
        if (next == 3 && d.guild < 2) miss.append("\n• Lonca sınıfı en az D olmalı");
        if (miss.length() > 0) return "\"" + PlayerRpg.SOCIAL[next] + "\" unvanı için henüz hazır değilsin:" + miss + (r[3] > 0 ? "\n• Bağış: " + NpcQuests.coins(r[3]) : "");
        if (!NpcQuests.pay(p, r[3])) return "Her şey hazır, ama hazineye " + NpcQuests.coins(r[3]) + " bağışlaman gerekiyor.";
        d.social = next;
        d.fame += next * 5;
        d.addRep(k, 25);
        FX.column(p.level(), ParticleTypes.TOTEM_OF_UNDYING, p.position(), 2.5, 0.8, 80);
        p.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, p.getSoundSource(), 1.0F, 1.0F);
        p.server.getPlayerList().broadcastSystemMessage(Component.literal("§6" + p.getName().getString() + ", " + Kingdom.of(k).title + " tarafından §e" + PlayerRpg.SOCIAL[next] + " §6unvanına layık görüldü!"), false);
        String gift = "";
        switch (next) {
            case 4 -> { NpcQuests.give(p, new ItemStack(RpgItems.sword("knight_oath"))); gift = "\nSana Şövalye Yemini kılıcını veriyorum. Onu onurla taşı."; }
            case 5 -> { NpcQuests.give(p, new ItemStack(RpgItems.DEED.get())); NpcQuests.giveCoins(p, 500); gift = "\nBu toprak tapusu artık senin. Kendine bir malikane kur."; }
            case 6 -> { NpcQuests.giveCoins(p, 1500); gift = "\nKont olarak kendi muhafızlarını toplayabilirsin. Ekibin daha da büyüyecek."; }
            case 7 -> { NpcQuests.give(p, new ItemStack(RpgItems.sword("crown_of_kings"))); gift = "\nDükalık sancağı ve Kralların Kılıcı artık senin."; }
            default -> {}
        }
        return switch (next) {
            case 1 -> "Artık sokakların hiçbir şeyi olmayan çocuğu değilsin. Halkımızdan birisin.";
            case 2 -> "Hür bir vatandaş olarak mülk edinebilir, ticaret yapabilirsin.";
            case 3 -> "Bir şövalyenin silahtarı olarak kılıç taşıma hakkı kazandın.";
            case 4 -> "Diz çök. ... Kalk, Sör. Artık bir şövalyesin!" + gift;
            default -> "Bugünden itibaren sen " + PlayerRpg.SOCIAL[next] + "sın. Krallık sana minnettar." + gift;
        };
    }
}
