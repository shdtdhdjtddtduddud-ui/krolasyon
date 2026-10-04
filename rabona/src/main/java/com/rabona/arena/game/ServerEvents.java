package com.rabona.arena.game;

import com.rabona.arena.RabonaArena;
import com.rabona.arena.net.Net;
import com.rabona.arena.net.S2C;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RabonaArena.MODID)
public final class ServerEvents {
    private ServerEvents() {}

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Scheduler.tick();
        Match.get(e.getServer()).tick();
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer sp)) return;
        MoveLogic.tick(sp);
        Athlete a = Athlete.of(sp);
        boolean cds = false;
        for (int c : a.cooldowns) if (c > 0) { cds = true; break; }
        if (sp.tickCount % 4 == 0 && (a.dirty || cds)) {
            a.dirty = false;
            a.lastSentStamina = a.stamina;
            a.lastSentEnergy = a.energy;
            Net.toPlayer(sp, S2C.Stats.of(a));
        }
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            Athlete.of(sp).dirty = true;
            Match.get(sp.server).sync();
            Cards.sync(sp, java.util.List.of());
        }
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent e) {
        RabonaCommands.register(e.getDispatcher());
    }

    @SubscribeEvent
    public static void started(ServerStartedEvent e) {
        Scheduler.clear();
        Athlete.clearAll();
        Match.shutdown();
        Match.get(e.getServer());
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent e) {
        Scheduler.clear();
        Athlete.clearAll();
        Match.shutdown();
    }
}
