package com.krolasyon.bosses.rpg.npc;

import com.krolasyon.bosses.rpg.data.PlayerRpg;
import com.krolasyon.bosses.rpg.data.RpgWorldData;
import com.krolasyon.bosses.rpg.registry.RpgEntities;
import com.krolasyon.bosses.rpg.world.Kingdom;
import com.krolasyon.bosses.rpg.world.Site;
import com.krolasyon.bosses.rpg.world.WorldMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;

/** Kingdom politics: daily drift of relations, wars, peace treaties, patrols and sieges. */
public final class WarLogic {
    private WarLogic() {}

    public static String status(RpgWorldData w, int k) {
        StringBuilder b = new StringBuilder(Kingdom.of(k).title + " için durum:\n");
        boolean any = false;
        for (int j = 0; j < Kingdom.COUNT; j++) {
            if (j == k) continue;
            if (w.war[k][j]) { b.append("§c⚔ ").append(Kingdom.of(j).title).append(" ile SAVAŞTA§r\n"); any = true; }
        }
        if (!any) b.append("Şu an kimseyle savaşta değiliz. Barış ne kadar sürer, bilinmez.\n");
        b.append("\nDostlarımız: ");
        for (int j = 0; j < Kingdom.COUNT; j++) if (j != k && w.relations[k][j] >= 30) b.append(Kingdom.of(j).capital).append(" ");
        b.append("\nDüşmanlarımız: ");
        for (int j = 0; j < Kingdom.COUNT; j++) if (j != k && w.relations[k][j] <= -40) b.append(Kingdom.of(j).capital).append(" ");
        b.append("\n\nSavaş puanımız: ").append(w.warPoints[k]);
        return b.toString();
    }

    /** once per in-game day */
    public static void daily(ServerLevel level, RpgWorldData w) {
        RandomSource r = level.random;
        for (int i = 0; i < Kingdom.COUNT; i++) for (int j = i + 1; j < Kingdom.COUNT; j++) {
            int drift = r.nextInt(7) - 3;
            if (w.war[i][j]) drift -= 1;
            w.changeRelation(i, j, drift);
            if (!w.war[i][j] && w.relations[i][j] <= -70 && r.nextInt(6) == 0) {
                w.setWar(i, j, true);
                w.news("§c" + Kingdom.of(i).title + ", " + Kingdom.of(j).title + "'na savaş ilan etti!");
                broadcast(level, "§4⚔ " + Kingdom.of(i).title + " ile " + Kingdom.of(j).title + " arasında savaş başladı!");
            } else if (w.war[i][j] && (r.nextInt(12) == 0 || Math.abs(w.warPoints[i] - w.warPoints[j]) > 60)) {
                int winner = w.warPoints[i] >= w.warPoints[j] ? i : j, loser = winner == i ? j : i;
                w.setWar(i, j, false);
                w.changeRelation(i, j, 40);
                w.warPoints[i] = 0;
                w.warPoints[j] = 0;
                w.news(Kingdom.of(winner).title + " ile " + Kingdom.of(loser).title + " barış antlaşması imzaladı. " + Kingdom.of(winner).capital + " zafer kutluyor.");
                broadcast(level, "§a☮ " + Kingdom.of(i).title + " ve " + Kingdom.of(j).title + " barış yaptı.");
            }
        }
        if (r.nextInt(3) == 0) {
            String[] events = {"Kuzeyden gelen kervanlar kurtadamların saldırısına uğradı.", "Altınliman'da baharat fiyatları ikiye katlandı.",
                    "Karak Dûm cüceleri yeni bir mithril damarı buldu!", "Lunareth'te elfler bin yıllık ağacın çiçek açtığını kutluyor.",
                    "Bir ejderhanın Ejderdişi Dağları üzerinde uçtuğu görüldü.", "Valdren lejyonları sınırda tatbikat yapıyor.",
                    "Kül Topraklarından kaçan köleler Aldoria'ya sığınıyor.", "Kızıl Bozkırda ork boyları arasında kan davası çıktı.",
                    "Gecegölge'den gelen bir suikastçının bir soyluyu öldürdüğü söyleniyor.", "Buzhisar devleri kış şenliği için hazırlanıyor."};
            w.news(events[r.nextInt(events.length)]);
        }
    }

    private static void broadcast(ServerLevel level, String msg) {
        level.getServer().getPlayerList().broadcastSystemMessage(Component.literal(msg), false);
    }

    /** spawns an enemy patrol near players who stand in the land of a kingdom at war */
    public static void patrols(ServerPlayer p, RpgWorldData w, PlayerRpg d) {
        ServerLevel level = p.serverLevel();
        int here = WorldMap.kingdomAt(w, p.getX(), p.getZ());
        if (here < 0) return;
        int enemy = -1;
        for (int j = 0; j < Kingdom.COUNT; j++) if (w.war[here][j]) { enemy = j; break; }
        if (enemy < 0) return;
        Site s = WorldMap.siteAt(w, p.getX(), p.getZ());
        RandomSource r = p.getRandom();
        boolean siege = s != null && (s.type == Site.Type.CAPITAL || s.type == Site.Type.CITY) && r.nextInt(4) == 0;
        int n = siege ? 6 + r.nextInt(5) : 3 + r.nextInt(3);
        double a = r.nextDouble() * Math.PI * 2;
        double dist = siege && s != null ? s.type.radius + 8 : 28;
        double cx = (siege && s != null ? s.x : p.getX()) + Math.cos(a) * dist, cz = (siege && s != null ? s.z : p.getZ()) + Math.sin(a) * dist;
        Kingdom ek = Kingdom.of(enemy);
        for (int i = 0; i < n; i++) {
            RpgNpc npc = RpgEntities.NPC.get().create(level);
            if (npc == null) continue;
            int x = (int) (cx + r.nextInt(7) - 3), z = (int) (cz + r.nextInt(7) - 3);
            int y = com.krolasyon.bosses.rpg.util.Heights.ground(level, x, z);
            npc.setup(ek.citizens()[r.nextInt(ek.citizens().length)], r.nextInt(4) == 0, i == 0 ? NpcRole.KNIGHT : NpcRole.SOLDIER, enemy, r);
            npc.moveTo(x + 0.5, y, z + 0.5, r.nextFloat() * 360, 0);
            npc.home = new BlockPos(s != null ? s.x : (int) p.getX(), y, s != null ? s.z : (int) p.getZ());
            npc.leaveAt = level.getGameTime() + 20 * 60 * 5;
            npc.getPersistentData().putBoolean("Raider", true);
            level.addFreshEntity(npc);
        }
        String where = s != null ? s.name : Kingdom.of(here).title + " toprakları";
        p.sendSystemMessage(Component.literal(siege ? "§4§l⚔ " + ek.title + " ordusu " + where + " şehrini kuşattı! Surları savun!" : "§c⚔ " + ek.title + " devriyesi " + where + " yakınında görüldü!"));
        level.playSound(null, p.blockPosition(), SoundEvents.RAID_HORN.value(), p.getSoundSource(), 2.0F, 1.0F);
    }
}
