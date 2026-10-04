package com.krolasyon.bosses.rpg.npc;

import com.krolasyon.bosses.rpg.data.PlayerRpg;
import com.krolasyon.bosses.rpg.data.RpgWorldData;
import com.krolasyon.bosses.rpg.magic.PlayerMagic;
import com.krolasyon.bosses.rpg.story.Story;
import com.krolasyon.bosses.rpg.world.Kingdom;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

/** Crime and consequence: hurting or killing people. */
public final class NpcEvents {
    private NpcEvents() {}

    private static boolean lawful(RpgNpc npc, ServerPlayer p, RpgWorldData w, PlayerRpg d) {
        if (npc.flag(RpgNpc.F_HOSTILE)) return false;
        if (npc.getTarget() == p) return false;
        return !(d.allegiance >= 0 && w.atWar(d.allegiance, npc.kingdomId()) && npc.isLawEnforcer());
    }

    public static void onHurtByPlayer(RpgNpc npc, ServerPlayer p, float amount) {
        RpgWorldData w = RpgWorldData.get(p.server);
        PlayerRpg d = RpgWorldData.player(p);
        Relation rel = npc.rel(p.getUUID());
        if (npc.leader() != null && npc.leader().equals(p.getUUID())) {
            rel.add(-10, -15);
            return;
        }
        if (!lawful(npc, p, w, d)) return;
        rel.add(-25, -30);
        rel.betrayed |= rel.affinity > 0;
        int k = npc.kingdomId();
        d.bounty[k] += 25;
        for (RpgNpc g : p.level().getEntitiesOfClass(RpgNpc.class, npc.getBoundingBox().inflate(24), n -> n.isLawEnforcer() && n.kingdomId() == k && n.leader() == null)) {
            if (g.getTarget() == null) g.setTarget(p);
        }
        if (d.bounty[k] >= 150 && d.bounty[k] - 25 < 150)
            p.sendSystemMessage(Component.literal("§c⚠ " + Kingdom.of(k).title + " muhafızları seni arıyor! (Ceza: " + NpcQuests.coins(d.bounty[k] * 2) + ")"));
    }

    public static void onDeath(RpgNpc npc, DamageSource src) {
        if (!(npc.level() instanceof net.minecraft.server.level.ServerLevel sl)) return;
        RpgWorldData w = RpgWorldData.get(sl);
        // family / party bookkeeping for whoever this person belonged to
        if (npc.leader() != null) {
            PlayerRpg owner = w.players.get(npc.leader());
            if (owner != null) {
                owner.party.remove(npc.getUUID());
                owner.children.remove(npc.getUUID());
                if (npc.getUUID().equals(owner.spouse)) {
                    owner.spouse = null;
                    ServerPlayer op = sl.getServer().getPlayerList().getPlayer(npc.leader());
                    if (op != null) op.sendSystemMessage(Component.literal("§8✝ Eşin " + owner.spouseName + " hayatını kaybetti... Yasın uzun sürecek."));
                    owner.spouseName = "";
                }
            }
        }
        for (PlayerRpg pr : w.players.values()) {
            PlayerRpg.Bond b = pr.bond(npc.getUUID());
            if (b != null && npc.leaveAt == 0) b.dead = true;
        }
        Story.onNpcDeath(sl, npc, src);
        if (!(src.getEntity() instanceof ServerPlayer p)) return;
        PlayerRpg d = RpgWorldData.player(p);
        if (npc.flag(RpgNpc.F_HOSTILE)) {
            NpcQuests.onKill(p, null, true);
            int here = com.krolasyon.bosses.rpg.world.WorldMap.kingdomAt(w, npc.getX(), npc.getZ());
            if (here >= 0) d.addRep(here, 4);
            d.addXp(25);
            PlayerMagic.sync(p);
            return;
        }
        int k = npc.kingdomId();
        if (d.allegiance >= 0 && w.atWar(d.allegiance, k) && npc.isLawEnforcer()) {
            d.warScore++;
            w.warPoints[d.allegiance]++;
            d.addRep(d.allegiance, 8);
            d.addRep(k, -10);
            d.addXp(40);
            p.displayClientMessage(Component.literal("§6⚔ Savaş puanı +1 (" + Kingdom.of(d.allegiance).title + ")"), true);
            PlayerMagic.sync(p);
            return;
        }
        if (npc.getTarget() == p && npc.isLawEnforcer() && d.bounty[k] > 0) {
            d.bounty[k] += 100;
            d.addRep(k, -40);
            return;
        }
        d.bounty[k] += npc.isChildNpc() ? 500 : 150;
        d.addRep(k, npc.role() == NpcRole.RULER ? -900 : npc.role() == NpcRole.NOBLE ? -200 : -60);
        for (RpgNpc witness : sl.getEntitiesOfClass(RpgNpc.class, npc.getBoundingBox().inflate(32), n -> n != npc && n.isAlive())) {
            witness.rel(p.getUUID()).add(-40, -40);
            if (witness.isLawEnforcer() && witness.kingdomId() == k) witness.setTarget(p);
        }
        p.sendSystemMessage(Component.literal("§4Bir masumun kanını döktün. " + Kingdom.of(k).title + " bunu unutmayacak."));
        PlayerMagic.sync(p);
    }
}
