package com.rabona.arena.client;

import com.rabona.arena.game.Move;
import com.rabona.arena.net.S2C;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Sunucu paketlerinin istemci isleyicileri. */
public final class ClientHooks {
    private ClientHooks() {}

    public static void anim(S2C.Anim a) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Entity e = mc.level.getEntity(a.entity());
        if (e instanceof LivingEntity le) ClientAnims.play(le, Move.byId(a.move()), a.side());
    }

    public static void shake(S2C.Shake s) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        Entity e = mc.level.getEntity(s.entity());
        if (e == null) return;
        double d = e.distanceTo(mc.player);
        if (d < 16) ClientEvents.addShake(s.power() * (float) (1 - d / 16));
    }

    public static void match(S2C.MatchState m) { ClientState.setMatch(m); }

    public static void stats(S2C.Stats s) {
        ClientState.stats = s;
        ClientState.statsTime = System.currentTimeMillis();
    }

    public static void banner(S2C.Banner b) {
        ClientState.banner = b;
        ClientState.bannerTicks = b.type() == 1 ? 100 : 200;
        if (b.type() == 1) {
            ClientEvents.addShake(0.6f);
            Replay.onGoal();
        }
    }

    public static void profile(S2C.Profile p) {
        ClientState.profile = p;
        if (!p.opened().isEmpty()) {
            ClientState.packReveal = p.opened();
            ClientState.packRevealStart = ClientState.clientTicks;
        }
    }

    public static void feed(S2C.Feed f) { ClientState.addFeed(f.text()); }

    public static void openMenu(S2C.OpenMenu o) {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(o.which() == 1 ? new MatchScreen() : new MoveScreen());
    }
}
