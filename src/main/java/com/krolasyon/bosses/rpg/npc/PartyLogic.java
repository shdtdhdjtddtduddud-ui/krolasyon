package com.krolasyon.bosses.rpg.npc;

import com.krolasyon.bosses.rpg.data.PlayerRpg;
import com.krolasyon.bosses.rpg.data.RpgWorldData;
import com.krolasyon.bosses.rpg.util.FX;
import com.krolasyon.bosses.rpg.world.Kingdom;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Companions, mercenaries and freed slaves. */
public final class PartyLogic {
    private PartyLogic() {}

    public static int maxParty(PlayerRpg d) { return Math.min(7, 2 + d.social / 2 + (d.guild >= 3 ? 1 : 0)); }

    public static String recruit(ServerPlayer p, RpgNpc npc, Relation rel, boolean hire) {
        PlayerRpg d = RpgWorldData.player(p);
        if (d.party.size() >= maxParty(d)) return "Ekibin zaten kalabalık. (En fazla " + maxParty(d) + " kişi; rütben yükseldikçe artar.)";
        boolean adventurer = npc.role() == NpcRole.ADVENTURER;
        if (hire) {
            int price = NpcDialog.hirePrice(npc);
            if (!NpcQuests.pay(p, price)) return "Kılıcım bedava değil. " + NpcQuests.coins(price) + " getir, seninle dünyanın sonuna kadar gelirim.";
            rel.add(10, 10);
        } else {
            int needA = adventurer ? 20 : 40, needT = adventurer ? 20 : 30;
            if (npc.role() == NpcRole.NOBLE && d.social < 4) return "Ben bir soyluyum! Bir köylünün peşinden gidemem.";
            if (rel.affinity < needA) return "Seni yeterince tanımıyorum. Belki biraz daha vakit geçirirsek...";
            if (rel.trust < needT) return "Hayatımı emanet edecek kadar sana güvenmiyorum. Henüz.";
        }
        join(p, npc, d);
        return adventurer ? "Ha! Sonunda gerçek bir macera! Kılıcım senin emrinde." : "Seninle geleceğim. Birlikte daha güçlüyüz.";
    }

    public static void join(ServerPlayer p, RpgNpc npc, PlayerRpg d) {
        npc.setLeader(p.getUUID());
        npc.setFlag(RpgNpc.F_PARTY, true);
        npc.setFlag(RpgNpc.F_FOLLOW, true);
        npc.setFlag(RpgNpc.F_STAY, false);
        npc.clearRestriction();
        npc.siteId = "";
        if (!d.party.contains(npc.getUUID())) d.party.add(npc.getUUID());
        FX.send(p.level(), ParticleTypes.HAPPY_VILLAGER, npc.position().add(0, npc.getBbHeight(), 0), 10, 0.4, 0.1);
        p.sendSystemMessage(Component.literal("§a" + npc.npcName() + " ekibine katıldı! §7(Konuşarak takip/bekle emri verebilirsin.)"));
    }

    public static void leave(ServerPlayer p, RpgNpc npc) {
        PlayerRpg d = RpgWorldData.player(p);
        d.party.remove(npc.getUUID());
        npc.setFlag(RpgNpc.F_PARTY, false);
        npc.setFlag(RpgNpc.F_FOLLOW, false);
        if (!npc.flag(RpgNpc.F_SPOUSE) && !npc.flag(RpgNpc.F_FAMILY) && !npc.flag(RpgNpc.F_SLAVE)) npc.setLeader(null);
        npc.home = npc.blockPosition();
    }

    public static String free(ServerPlayer p, RpgNpc npc, Relation rel, PlayerRpg d) {
        if (!npc.flag(RpgNpc.F_SLAVE)) return "Ben zaten özgürüm.";
        npc.setFlag(RpgNpc.F_SLAVE, false);
        npc.setFlag(RpgNpc.F_PARTY, false);
        npc.setFlag(RpgNpc.F_FOLLOW, false);
        npc.setLeader(null);
        d.party.remove(npc.getUUID());
        rel.add(70, 60);
        rel.rescued = true;
        npc.setRole(npc.race().ordinal() % 3 == 0 ? NpcRole.ADVENTURER : NpcRole.PEASANT);
        Bonds.add(p, npc, "freed");
        d.addRep(Kingdom.ALDORIA.ordinal(), 25);
        d.addRep(Kingdom.SYLVARIEN.ordinal(), 20);
        d.addRep(Kingdom.FELARIS.ordinal(), 20);
        d.fame += 5;
        npc.leaveAt = p.level().getGameTime() + 20 * 40;
        FX.spiral(p.level(), ParticleTypes.END_ROD, npc.position(), 2.4, 0.7, 3, 40);
        return "Ö-özgür müyüm? Gerçekten mi?! ... Ailemin yanına döneceğim. Adını asla unutmayacağım. Bir gün, bir yerde, bu iyiliğini ödeyeceğim!";
    }
}
