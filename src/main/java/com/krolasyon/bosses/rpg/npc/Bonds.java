package com.krolasyon.bosses.rpg.npc;

import com.krolasyon.bosses.rpg.data.PlayerRpg;
import com.krolasyon.bosses.rpg.data.RpgWorldData;
import net.minecraft.server.level.ServerPlayer;

/** Fate links between a player and NPCs they saved, freed or wronged. */
public final class Bonds {
    private Bonds() {}

    public static PlayerRpg.Bond add(ServerPlayer p, RpgNpc npc, String kind) {
        PlayerRpg d = RpgWorldData.player(p);
        PlayerRpg.Bond b = d.bond(npc.getUUID());
        if (b == null) {
            b = new PlayerRpg.Bond();
            b.npc = npc.getUUID();
            d.bonds.add(b);
        }
        b.name = npc.npcName();
        b.race = npc.race().ordinal();
        b.gender = npc.female() ? 1 : 0;
        b.variant = npc.variant();
        b.kingdom = npc.kingdomId();
        b.kind = kind;
        b.since = p.level().getGameTime();
        b.lastSeen = p.level().getGameTime();
        b.dead = false;
        return b;
    }
}
