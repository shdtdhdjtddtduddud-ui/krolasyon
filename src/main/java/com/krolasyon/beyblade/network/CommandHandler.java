package com.krolasyon.beyblade.network;

import com.krolasyon.beyblade.entity.BeybladeEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import static com.krolasyon.beyblade.network.ModNetwork.*;

/** Server side handling of a key / command menu command (shared by every game version). */
public final class CommandHandler {
    private CommandHandler() {}

    public static void handle(ServerPlayer player, int cmd) {
        if (player == null) return;
        BeybladeEntity bey = BeybladeEntity.findActive(player);
        if (bey == null) {
            player.displayClientMessage(Component.translatable("msg.librabeyblade.none_active"), true);
            return;
        }
        if (cmd == CMD_RECALL) bey.recall();
        else if (cmd >= CMD_ORDER && cmd < CMD_ORDER + BeybladeEntity.ORDER_COUNT) bey.setOrder(cmd - CMD_ORDER);
        else if (cmd == CMD_SPIRIT) bey.releaseSpirit();
        else if (cmd == CMD_MODE_CYCLE) bey.setMode(bey.getMode() + 1);
        else if (cmd >= CMD_MODE && cmd < CMD_MODE + BeybladeEntity.MODE_COUNT) bey.setMode(cmd - CMD_MODE);
        else if (cmd >= 1 && cmd <= BeybladeEntity.ABILITY_COUNT) bey.tryAbility(cmd, true);
    }
}
