package com.krolasyon.bosses.realm.story;

import com.krolasyon.bosses.KrolasyonBosses;
import com.krolasyon.bosses.realm.Faction;
import com.krolasyon.bosses.realm.Realm;
import com.krolasyon.bosses.realm.data.RealmData;
import com.krolasyon.bosses.realm.entity.FactionMember;
import com.krolasyon.bosses.realm.net.RealmNet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = KrolasyonBosses.MODID)
public final class RealmEvents {
    private RealmEvents() {}

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) RealmNet.sync(p);
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone e) {
        e.getOriginal().reviveCaps();
        if (e.getOriginal().getPersistentData().contains(RealmData.KEY)) {
            e.getEntity().getPersistentData().put(RealmData.KEY, e.getOriginal().getPersistentData().getCompound(RealmData.KEY).copy());
        }
        e.getOriginal().invalidateCaps();
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) RealmNet.sync(p);
    }

    @SubscribeEvent
    public static void onChangeDim(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            if (e.getTo() == Realm.REALM) Story.enterRealm(p);
            else RealmNet.sync(p);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase == TickEvent.Phase.END && e.player instanceof ServerPlayer p && p.tickCount % 20 == 0 && p.isAlive()) {
            PlayerPowers.tick(p);
            if (p.tickCount % 100 == 0) {
                RealmData d = RealmData.get(p);
                if (Story.computeChapter(p, d) > d.chapter) Story.update(p, d);
            }
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        LivingEntity victim = e.getEntity();
        if (victim.level().isClientSide() || !(victim instanceof FactionMember fm)) return;
        DamageSource src = e.getSource();
        ServerPlayer killer = src.getEntity() instanceof ServerPlayer sp ? sp : null;
        if (killer == null && src.getEntity() instanceof com.krolasyon.bosses.realm.entity.RealmMob rm && rm.getOwner() instanceof ServerPlayer sp) killer = sp;
        if (fm.isTyrant()) {
            List<ServerPlayer> near = victim.level().getEntitiesOfClass(ServerPlayer.class, victim.getBoundingBox().inflate(64));
            for (ServerPlayer p : near) Politics.crown(p);
            if (killer != null && !near.contains(killer)) Politics.crown(killer);
            return;
        }
        if (killer == null) return;
        Faction f = fm.faction();
        if (f == null) return;
        if (fm.isLord()) Politics.conquer(killer, f);
        else Politics.onKill(killer, f, fm.isEnvoy());
    }

    /** Ash blessing: fire cannot touch you */
    @SubscribeEvent
    public static void onAttack(LivingAttackEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && e.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FIRE)
                && RealmData.get(p).bound(Faction.ASH)) {
            e.setCanceled(true);
            p.clearFire();
        }
    }

    /** Blood blessing: melee strikes steal life */
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent e) {
        Entity src = e.getSource().getDirectEntity();
        if (src instanceof ServerPlayer p && e.getSource().getEntity() == p && RealmData.get(p).bound(Faction.BLOOD)) {
            p.heal(e.getAmount() * 0.12F);
        }
    }

    /** Shadow blessing: blindness and darkness cannot take hold */
    @SubscribeEvent
    public static void onEffect(MobEffectEvent.Applicable e) {
        if (e.getEntity() instanceof Player p && !p.level().isClientSide()
                && (e.getEffectInstance().getEffect() == MobEffects.BLINDNESS || e.getEffectInstance().getEffect() == MobEffects.DARKNESS)
                && RealmData.get(p).bound(Faction.SHADOW)) {
            e.setResult(Event.Result.DENY);
        }
    }
}
