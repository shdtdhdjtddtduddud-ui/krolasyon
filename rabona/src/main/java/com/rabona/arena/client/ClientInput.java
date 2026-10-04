package com.rabona.arena.client;

import com.rabona.arena.game.Move;
import com.rabona.arena.net.C2S;
import com.rabona.arena.net.Net;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/** Tus girdileri: 3 sut yuvasi (sarjli), pas, pas isteme, 3 calim yuvasi, yetenek, kamera, tekrar. */
public final class ClientInput {
    public static float charge;
    public static boolean charging;
    public static int chargingSlot;
    private static int chargeTicks;

    private ClientInput() {}

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) return;
        if (mc.screen != null) {
            charging = false;
            charge = 0;
            return;
        }
        while (Keys.MOVES.consumeClick()) mc.setScreen(new MoveScreen());
        while (Keys.MATCH.consumeClick()) mc.setScreen(new MatchScreen());
        while (Keys.CARDS.consumeClick()) mc.setScreen(new CardScreen());
        while (Keys.SUBS.consumeClick()) Net.toServer(new C2S.Menu(C2S.Menu.BENCH, 0));
        while (Keys.CAMERA.consumeClick()) CameraCtl.cycle();
        while (Keys.REPLAY.consumeClick()) Replay.toggle();
        if (Replay.playing()) {
            charging = false;
            return;
        }
        int side = side(p);
        boolean mod = p.isShiftKeyDown();
        // sut yuvalari: basili tut, birak
        int down = -1;
        for (int i = 0; i < Keys.SHOTS.length; i++) if (Keys.SHOTS[i].isDown()) { down = i; break; }
        if (down >= 0 && (!charging || down == chargingSlot)) {
            if (!charging) {
                charging = true;
                chargingSlot = down;
                chargeTicks = 0;
            }
            chargeTicks++;
            charge = Math.min(1.35f, chargeTicks / 22f);
        } else if (charging) {
            charging = false;
            float power = chargeTicks < 3 ? 0.35f : charge;
            send(1, ClientState.shotSlots[chargingSlot], power, side, mod);
            charge = 0;
        }
        for (var k : Keys.SHOTS) while (k.consumeClick()) { /* isDown ile islenir */ }
        while (Keys.PASS.consumeClick()) send(2, Move.PASS_SHORT, 1, side, mod);
        while (Keys.LOB.consumeClick()) send(3, Move.PASS_LOB, 1, side, mod);
        while (Keys.TACKLE.consumeClick()) send(4, Move.TACKLE, 1, side, mod || p.isSprinting());
        while (Keys.CALL.consumeClick()) send(5, Move.CALL_PASS, 1, 0, false);
        while (Keys.SWITCH.consumeClick()) send(6, Move.CALL_PASS, 1, 0, false);
        for (int i = 0; i < Keys.SKILLS.length; i++) {
            while (Keys.SKILLS[i].consumeClick()) send(0, ClientState.slots[i], 1, side, mod);
        }
        while (Keys.ABILITY.consumeClick()) send(0, ClientState.ability, 1, side, mod);
        while (Keys.CELEBRATE.consumeClick()) send(0, ClientState.celebration, 1, side, mod);
    }

    public static int side(LocalPlayer p) {
        return p.input.left && !p.input.right ? -1 : p.input.right && !p.input.left ? 1 : 0;
    }

    public static void send(int kind, Move m, float power, int side, boolean mod) {
        Net.toServer(new C2S.Act(kind, m.ordinal(), power, side, mod));
    }
}
