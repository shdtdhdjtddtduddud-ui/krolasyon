package com.krolasyon.elvenrealm.client;

import com.krolasyon.elvenrealm.network.Net;
import net.minecraft.client.Minecraft;

/** Paketlerden istemci koduna köprü (sunucuda yüklenmez). */
public final class ClientHooks {
    private ClientHooks() {}

    public static void dialogue(Net.OpenDialogue msg) {
        Minecraft mc = Minecraft.getInstance();
        if (msg.entityId() < 0) {
            if (mc.screen instanceof DialogueScreen) mc.setScreen(null);
            return;
        }
        if (mc.screen instanceof DialogueScreen d && d.entityId() == msg.entityId()) d.update(msg);
        else mc.setScreen(new DialogueScreen(msg));
    }
}
