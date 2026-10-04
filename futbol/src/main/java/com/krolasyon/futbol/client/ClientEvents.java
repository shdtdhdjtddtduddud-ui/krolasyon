package com.krolasyon.futbol.client;

import com.krolasyon.futbol.FutbolMod;
import com.krolasyon.futbol.game.Move;
import com.krolasyon.futbol.network.MoveC2S;
import com.krolasyon.futbol.network.Net;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FutbolMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {}

    public static final int MAX_CHARGE = 25;

    public static void send(Move m, float charge) { Net.toServer(new MoveC2S(m.ordinal(), charge)); }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        ClientState.clientTicks++;
        if (ClientState.goalTicks > 0) ClientState.goalTicks--;
        if (ClientState.shake > 0) ClientState.shake--;
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) {
            ClientState.charging = false;
            return;
        }
        ClientState.refreshBalls();
        if (ClientState.pendingMenu && mc.screen == null && ++ClientState.pendingMenuDelay > 40) {
            ClientState.pendingMenu = false;
            mc.setScreen(new MatchScreen());
        }
        if (mc.screen != null) {
            ClientState.charging = false;
            return;
        }
        // shot: hold to charge, release to fire
        while (Keys.SHOOT.consumeClick()) {
            if (!ClientState.charging) {
                ClientState.charging = true;
                ClientState.chargeTicks = 0;
            }
        }
        if (ClientState.charging) {
            if (Keys.SHOOT.isDown()) {
                ClientState.chargeTicks = Math.min(MAX_CHARGE, ClientState.chargeTicks + 1);
            } else {
                ClientState.charging = false;
                float c = ClientState.chargeTicks / (float) MAX_CHARGE;
                Move m = p.isShiftKeyDown() ? Move.FINESSE : p.isSprinting() ? Move.POWER_SHOT : Move.SHOT;
                send(m, c);
            }
        }
        while (Keys.PASS.consumeClick()) send(p.isShiftKeyDown() ? Move.LOB_PASS : p.isSprinting() ? Move.THROUGH_PASS : Move.SHORT_PASS, 0.5F);
        while (Keys.TACKLE.consumeClick()) send(p.isShiftKeyDown() || p.isSprinting() ? Move.SLIDE : Move.TACKLE, 0F);
        while (Keys.SKILL.consumeClick()) send(ClientState.favSkill, 0.7F);
        while (Keys.SUPER.consumeClick()) send(ClientState.favSuper, 1F);
        while (Keys.CELEBRATE.consumeClick()) send(ClientState.favCeleb, 0F);
        while (Keys.MOVES.consumeClick()) mc.setScreen(new MoveScreen());
        while (Keys.MATCH.consumeClick()) mc.setScreen(new MatchScreen());
    }

    @SubscribeEvent
    public static void preRender(RenderPlayerEvent.Pre event) {
        event.getPoseStack().pushPose();
        ClientAnims.applyRoot(event.getPoseStack(), event.getEntity(), event.getPartialTick());
    }

    @SubscribeEvent
    public static void postRender(RenderPlayerEvent.Post event) { event.getPoseStack().popPose(); }

    @SubscribeEvent
    public static void camera(ViewportEvent.ComputeCameraAngles event) {
        if (ClientState.shake <= 0) return;
        float t = (float) (ClientState.clientTicks + event.getPartialTick());
        float a = ClientState.shake * 0.35F;
        event.setRoll(event.getRoll() + (float) Math.sin(t * 2.7F) * a);
        event.setPitch(event.getPitch() + (float) Math.cos(t * 3.1F) * a * 0.6F);
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientAnims.clear();
        ClientState.hasPitch = false;
        ClientState.state = 0;
        ClientState.TEAMS.clear();
        ClientState.NUMBERS.clear();
    }
}
