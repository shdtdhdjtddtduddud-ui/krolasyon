package com.rabona.arena.client;

import com.rabona.arena.game.Move;
import com.rabona.arena.net.C2S;
import com.rabona.arena.net.Net;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/** Tus girdileri: sut sarji, pas, calim yuvalari, yetenek. */
public final class ClientInput {
    public static float charge;
    public static boolean charging;
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
        // menuler
        while (Keys.MOVES.consumeClick()) mc.setScreen(new MoveScreen());
        while (Keys.MATCH.consumeClick()) mc.setScreen(new MatchScreen());
        while (Keys.STYLE.consumeClick()) {
            Move[] st = ClientState.SHOT_STYLES;
            int i = 0;
            for (int k = 0; k < st.length; k++) if (st[k] == ClientState.shotStyle) i = k;
            ClientState.shotStyle = st[(i + 1) % st.length];
            ClientState.save();
            p.displayClientMessage(Component.translatable("msg.rabonaarena.style_set", ClientState.shotStyle.title()), true);
        }
        int side = side(p);
        boolean mod = p.isShiftKeyDown();
        // sut: basili tut, birak
        if (Keys.SHOOT.isDown()) {
            if (!charging) {
                charging = true;
                chargeTicks = 0;
            }
            chargeTicks++;
            charge = Math.min(1.35f, chargeTicks / 22f);
        } else if (charging) {
            charging = false;
            float power = chargeTicks < 3 ? 0.35f : charge;
            send(1, ClientState.shotStyle, power, side, mod);
            charge = 0;
        }
        while (Keys.SHOOT.consumeClick()) { /* isDown ile islenir */ }
        while (Keys.PASS.consumeClick()) send(2, Move.PASS_SHORT, 1, side, mod);
        while (Keys.LOB.consumeClick()) send(3, Move.PASS_LOB, 1, side, mod);
        while (Keys.TACKLE.consumeClick()) send(4, Move.TACKLE, 1, side, mod || p.isSprinting());
        for (int i = 0; i < Keys.SKILLS.length; i++) {
            while (Keys.SKILLS[i].consumeClick()) send(0, ClientState.slots[i], 1, side, mod);
        }
        while (Keys.ABILITY.consumeClick()) send(0, ClientState.ability, 1, side, mod);
        while (Keys.CELEBRATE.consumeClick()) send(0, ClientState.celebration, 1, side, mod);
    }

    public static int side(LocalPlayer p) {
        float l = p.input.leftImpulse;
        return l > 0.1f ? -1 : l < -0.1f ? 1 : 0;
    }

    public static void send(int kind, Move m, float power, int side, boolean mod) {
        Net.toServer(new C2S.Act(kind, m.ordinal(), power, side, mod));
    }
}
