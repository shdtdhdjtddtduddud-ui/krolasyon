package com.krolasyon.ben10;

import com.krolasyon.ben10.ability.Powers;
import com.krolasyon.ben10.net.Net;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Ben10.MODID)
public final class CommonEvents {
    private CommonEvents() {}

    private static OmniData peek(Player p) {
        try {
            return OmniData.get(p);
        } catch (Throwable t) {
            return null;
        }
    }

    @SubscribeEvent
    public static void size(EntityEvent.Size e) {
        if (!(e.getEntity() instanceof Player p)) return;
        Pose pose = e.getPose();
        if (pose == Pose.SLEEPING || pose == Pose.DYING || pose == Pose.SWIMMING || pose == Pose.FALL_FLYING || pose == Pose.SPIN_ATTACK) return;
        if (p.getUUID() == null || p.level() == null) return;
        OmniData d = peek(p);
        if (d == null || d.alien == null) return;
        Alien a = d.alien;
        float h = a.height, w = a.width;
        if (a == Alien.CANNONBOLT && (d.has(OmniData.F_ROLLING) || d.blocking)) {
            h = 1.3F;
            w = 0.9F;
        }
        if (a == Alien.GOOP && d.ability == 3) h = 0.9F;
        if (pose == Pose.CROUCHING) h *= 0.85F;
        e.setNewSize(EntityDimensions.scalable(w, h), false);
        e.setNewEyeHeight(h * (a == Alien.CANNONBOLT && h < 1.5F ? 0.75F : 0.86F));
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Player p = e.player;
        if (p instanceof ServerPlayer sp) {
            Transform.tick(sp);
        } else if (p.level().isClientSide) {
            Powers.clientTick(p, OmniData.get(p));
        }
        OmniData d = OmniData.get(p);
        if (d.alien != null) Powers.commonTick(p, d);
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) Transform.load(sp);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            OmniData d = OmniData.get(sp);
            Powers.cancel(sp, d);
            Transform.save(sp);
            OmniData.forget(sp);
        }
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            OmniData d = OmniData.get(sp);
            d.alien = null;
            d.ability = -1;
            d.flags = 0;
            d.blocking = false;
            Transform.removeModifiers(sp);
            Transform.updateFlight(sp, null);
            sp.refreshDimensions();
            Transform.save(sp);
            Net.sync(sp);
        }
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone e) {
        // the old player object is gone; keep only the recharge timer
        e.getEntity().getPersistentData().put(Ben10.MODID, new net.minecraft.nbt.CompoundTag());
    }

    @SubscribeEvent
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            OmniData d = OmniData.get(sp);
            if (d.alien != null) {
                Transform.applyModifiers(sp, d.alien);
                Transform.updateFlight(sp, d.alien);
            }
            sp.refreshDimensions();
            Net.sync(sp);
        }
    }

    @SubscribeEvent
    public static void track(PlayerEvent.StartTracking e) {
        if (e.getTarget() instanceof Player other && e.getEntity() instanceof ServerPlayer sp) Net.syncTo(sp, other);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void attack(LivingAttackEvent e) {
        Powers.onAttacked(e);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void hurt(LivingHurtEvent e) {
        Powers.onHurt(e);
    }

    @SubscribeEvent
    public static void fall(LivingFallEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        OmniData d = OmniData.get(p);
        if (d.alien == null) return;
        if (d.alien.has(Alien.Flag.NO_FALL)) {
            e.setDamageMultiplier(0);
        } else if (d.alien.has(Alien.Flag.HALF_FALL)) {
            e.setDamageMultiplier(0.3F);
            e.setDistance(Math.max(0, e.getDistance() - 6));
        }
        Powers.onLand(p, d, e.getDistance());
    }

    @SubscribeEvent
    public static void jump(LivingEvent.LivingJumpEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        OmniData d = OmniData.get(p);
        if (d.alien == null) return;
        if (d.alien.has(Alien.Flag.BIG_JUMP)) {
            Vec3 v = p.getDeltaMovement();
            p.setDeltaMovement(v.x * 1.4, v.y + 0.32, v.z * 1.4);
        } else if (d.alien == Alien.XLR8 || d.alien == Alien.GOOP) {
            Vec3 v = p.getDeltaMovement();
            p.setDeltaMovement(v.x, v.y + 0.1, v.z);
        }
    }

    @SubscribeEvent
    public static void effect(MobEffectEvent.Applicable e) {
        if (!(e.getEntity() instanceof Player p)) return;
        OmniData d = OmniData.get(p);
        if (d.alien == null) return;
        var eff = e.getEffectInstance().getEffect();
        if (d.alien.has(Alien.Flag.POISON_IMMUNE) && (eff == net.minecraft.world.effect.MobEffects.POISON || eff == net.minecraft.world.effect.MobEffects.WITHER)) {
            e.setResult(Event.Result.DENY);
        }
        if (d.alien == Alien.GHOSTFREAK && eff == net.minecraft.world.effect.MobEffects.DARKNESS) e.setResult(Event.Result.DENY);
    }
}
