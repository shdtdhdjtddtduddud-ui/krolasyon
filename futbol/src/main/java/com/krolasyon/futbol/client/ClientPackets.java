package com.krolasyon.futbol.client;

import com.krolasyon.futbol.game.Move;
import com.krolasyon.futbol.network.AnimS2C;
import com.krolasyon.futbol.network.GoalS2C;
import com.krolasyon.futbol.network.MatchS2C;
import com.krolasyon.futbol.network.ShakeS2C;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class ClientPackets {
    private ClientPackets() {}

    public static void anim(AnimS2C msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Entity e = mc.level.getEntity(msg.entity);
        Move m = Move.byId(msg.move);
        if (!(e instanceof LivingEntity le) || m == null) return;
        ClientAnims.start(le, m, msg.variant);
        if (mc.player != null && e == mc.player && msg.cooldown > 0) {
            ClientState.superCooldownEnd = ClientState.clientTicks + msg.cooldown;
            ClientState.superCooldownTotal = msg.cooldown;
        }
    }

    public static void match(MatchS2C msg) { ClientState.update(msg); }

    public static void goal(GoalS2C msg) {
        ClientState.goalTicks = 90;
        ClientState.goalTeam = msg.team;
        ClientState.goalScorer = msg.scorer;
        ClientState.goalOwn = msg.own;
        ClientState.red = msg.red;
        ClientState.blue = msg.blue;
        ClientState.shake = Math.max(ClientState.shake, 10);
    }

    public static void shake(ShakeS2C msg) { ClientState.shake = Math.max(ClientState.shake, msg.strength); }

    public static void openMenu() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            ClientState.pendingMenu = true;
            ClientState.pendingMenuDelay = 0;
            return;
        }
        if (mc.screen == null) mc.setScreen(new MatchScreen());
        else {
            ClientState.pendingMenu = true;
            ClientState.pendingMenuDelay = 0;
        }
    }
}
