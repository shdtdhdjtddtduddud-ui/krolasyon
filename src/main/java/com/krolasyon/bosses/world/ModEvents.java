package com.krolasyon.bosses.world;

import com.krolasyon.bosses.KrolasyonBosses;
import com.krolasyon.bosses.entity.HellMob;
import com.krolasyon.bosses.faction.Faction;
import com.krolasyon.bosses.faction.PlayerData;
import com.krolasyon.bosses.net.Net;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Gameplay hooks on the Forge event bus: reputation for kills, data persistence, mana. */
@Mod.EventBusSubscriber(modid = KrolasyonBosses.MODID)
public final class ModEvents {
    private ModEvents() {}

    @SubscribeEvent
    public static void onKill(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof HellMob m)) return;
        Player killer = null;
        if (e.getSource().getEntity() instanceof Player p) killer = p;
        else if (e.getSource().getEntity() instanceof HellMob h) killer = h.getOwnerPlayer();
        if (killer == null) return;
        Faction f = m.faction();
        if (f.isHouse() && m.getOwnerUUID() == null) PlayerData.addRep(killer, f, m.isBossMob() ? -12 : -2, true);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) Net.syncTo(sp);
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone e) {
        e.getEntity().getPersistentData().put(PlayerData.ROOT, e.getOriginal().getPersistentData().getCompound(PlayerData.ROOT).copy());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) Net.syncTo(sp);
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            if (e.getTo().location().getPath().equals("azrakor")) PlayerData.init(sp);
            Net.syncTo(sp);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.side.isClient() || !(e.player instanceof ServerPlayer sp)) return;
        // mana regeneration: 3 per second (more for the sovereign); synced twice a second
        float before = PlayerData.mana(sp);
        float max = PlayerData.maxMana(sp);
        if (before < max) PlayerData.setMana(sp, before + (PlayerData.isSovereign(sp) ? 0.2F : 0.15F));
        if (sp.tickCount % 10 == 0 && Math.abs(PlayerData.mana(sp) - sp.getPersistentData().getFloat("krolasyon_last_sync")) >= 1F) {
            sp.getPersistentData().putFloat("krolasyon_last_sync", PlayerData.mana(sp));
            Net.syncTo(sp);
        }
    }
}
