package com.krolasyon.labirent.net;

import com.krolasyon.labirent.client.ClientState;
import com.krolasyon.labirent.client.screen.*;
import com.krolasyon.labirent.entity.GladerEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

/** Client-side packet handlers. Only ever loaded on the physical client. */
public final class ClientHooks {
    private ClientHooks() {}

    static void mapFull(Net.MapFull m) {
        if (m.known().length == ClientState.known.length) ClientState.known = m.known();
        ClientState.mapVersion++;
    }

    static void mapDelta(Net.MapDelta m) {
        for (int k = 0; k < m.idx().length; k++)
            if (m.idx()[k] >= 0 && m.idx()[k] < ClientState.known.length) ClientState.known[m.idx()[k]] = m.val()[k];
        ClientState.mapVersion++;
    }

    static void shake(Net.Shake m) {
        if (m.strength() >= ClientState.shake * ClientState.shakeTicks / Math.max(1F, ClientState.shakeTotal)) {
            ClientState.shake = m.strength();
            ClientState.shakeTicks = m.ticks();
            ClientState.shakeTotal = m.ticks();
        }
    }

    static void state(Net.State s) { ClientState.state = s; }

    static void speech(Net.Speech m) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        ClientState.bubbles.put(m.entityId(), new ClientState.Bubble(m.text(), mc.level.getGameTime()));
    }

    static void stumble(Net.Stumble m) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        ClientState.stumbles.put(m.entityId(), new ClientState.Fall(m.kind(), mc.level.getGameTime(), m.ticks()));
    }

    static void cinematic(Net.Cinematic c) {
        ClientState.cinematic = c.id();
        ClientState.cinematicTicks = 0;
    }

    static void open(Net.OpenScreen o) {
        Minecraft mc = Minecraft.getInstance();
        switch (o.type()) {
            case Net.SCREEN_MAP -> mc.setScreen(new MapScreen(o.arg() == 1));
            case Net.SCREEN_CAPTAIN -> mc.setScreen(new CaptainScreen());
            case Net.SCREEN_NPC -> {
                Entity e = mc.level == null ? null : mc.level.getEntity(o.arg());
                if (e instanceof GladerEntity g) mc.setScreen(new NpcScreen(g));
            }
            case Net.SCREEN_TERMINAL -> mc.setScreen(new TerminalScreen());
            case Net.SCREEN_JOURNAL -> mc.setScreen(JournalScreen.create());
            default -> {}
        }
    }
}
