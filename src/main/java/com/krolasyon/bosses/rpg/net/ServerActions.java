package com.krolasyon.bosses.rpg.net;

import com.krolasyon.bosses.rpg.data.PlayerRpg;
import com.krolasyon.bosses.rpg.data.RpgWorldData;
import com.krolasyon.bosses.rpg.magic.PlayerMagic;
import com.krolasyon.bosses.rpg.npc.NpcDialog;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Handles requests coming from the client UI and key bindings. */
public final class ServerActions {
    private ServerActions() {}

    public static void handle(ServerPlayer p, byte kind, CompoundTag t) {
        switch (kind) {
            case RpgNet.CAST -> PlayerMagic.castSelected(p);
            case RpgNet.CYCLE -> PlayerMagic.cycle(p, t.getInt("Dir"));
            case RpgNet.STAT -> PlayerMagic.allocate(p, t.getInt("Stat"));
            case RpgNet.CHOICE -> NpcDialog.choose(p, t.getInt("Npc"), t.getString("Id"));
            case RpgNet.OPEN -> PlayerMagic.sync(p);
            case RpgNet.SELECT -> PlayerMagic.select(p, t.getInt("Idx"));
            case RpgNet.QUEST_DROP -> {
                PlayerRpg d = RpgWorldData.player(p);
                int i = t.getInt("Idx");
                if (i >= 0 && i < d.quests.size()) {
                    String title = d.quests.remove(i).title;
                    p.sendSystemMessage(Component.literal("§7Görevi bıraktın: " + title));
                }
                PlayerMagic.sync(p);
            }
            default -> {}
        }
    }
}
