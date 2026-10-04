package com.krolasyon.futbol.game;

import com.krolasyon.futbol.FutbolMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FutbolMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ServerEvents {
    private ServerEvents() {}

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Scheduler.tick();
        MatchManager.tick(event.getServer());
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) { FutbolCommands.register(event.getDispatcher()); }

    @SubscribeEvent
    public static void onStarted(ServerStartedEvent event) { MatchManager.init(event.getServer()); }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent event) { MatchManager.shutdown(); }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            MatchManager.markDirty();
            MatchManager.welcome(sp);
            CardData.sync(sp, -1);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        MatchManager.markDirty();
        if (MatchManager.isActive()) com.krolasyon.futbol.game.Scheduler.later(1, MatchManager::rebalance);
    }
}
