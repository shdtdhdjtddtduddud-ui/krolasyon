package com.krolasyon.ben10.ability;

import com.krolasyon.ben10.Alien;
import com.krolasyon.ben10.Config;
import com.krolasyon.ben10.OmniData;
import com.krolasyon.ben10.Transform;
import com.krolasyon.ben10.net.Net;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

public final class Powers {
    private static final Kit[] KITS = {new HeatblastKit(), new FourArmsKit(), new DiamondheadKit(), new Xlr8Kit(), new GoopKit(),
            new BlowpokeKit(), new BigChillKit(), new EchoEchoKit(), new GhostfreakKit()};

    private Powers() {}

    public static Kit kit(Alien a) {
        return KITS[a.ordinal()];
    }

    public static void use(ServerPlayer p, int slot) {
        OmniData d = OmniData.get(p);
        if (d.alien == null || slot < 0 || slot > 3 || !p.isAlive()) return;
        if (d.ability >= 0 || d.cooldown[slot] > 0 || d.blocking) return;
        if (d.sinceTransform < 10) return;
        int cd = Math.max(1, Math.round(d.alien.cooldowns[slot] * Config.cd()));
        d.ability = slot;
        d.abilityTick = 0;
        d.cooldown[slot] = d.cooldownMax[slot] = cd;
        d.dirty = true;
        if (!kit(d.alien).start(p, d, slot)) {
            d.ability = -1;
            d.cooldown[slot] = 0;
        }
        Net.sync(p);
    }

    public static void guard(ServerPlayer p, boolean on) {
        OmniData d = OmniData.get(p);
        if (d.alien == null) on = false;
        if (on && d.ability >= 0) return;
        if (d.blocking == on) return;
        d.blocking = on;
        d.dirty = true;
        if (d.alien == Alien.BLOWPOKE) p.refreshDimensions();
        Net.sync(p);
    }

    public static void tick(ServerPlayer p, OmniData d) {
        Alien a = d.alien;
        Kit k = kit(a);
        if (d.ability >= 0) {
            int slot = d.ability;
            d.abilityTick++;
            k.tick(p, d, slot, d.abilityTick);
            if (d.alien == a && d.ability == slot && d.abilityTick >= a.durations[slot]) finish(p, d);
        }
        if (d.alien != a) return;
        k.passive(p, d);
        // guarding slows the alien down (Blowpoke cannot move while curled up)
        double slow = d.blocking ? (a == Alien.BLOWPOKE ? -1.0 : -0.55) : 0;
        var inst = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (inst != null) {
            var cur = inst.getModifier(Transform.GUARD_SLOW);
            if (slow == 0 && cur != null) inst.removeModifier(Transform.GUARD_SLOW);
            else if (slow != 0 && cur == null) inst.addTransientModifier(new AttributeModifier(Transform.GUARD_SLOW, "guard", slow, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
        p.setTicksFrozen(a.has(Alien.Flag.FREEZE_IMMUNE) ? 0 : p.getTicksFrozen());
        if (a.has(Alien.Flag.FIRE_IMMUNE)) p.clearFire();
        if (a.has(Alien.Flag.NIGHT_VISION) && p.tickCount % 100 == 0) {
            p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION, 320, 0, true, false, false));
        }
    }

    public static void finish(ServerPlayer p, OmniData d) {
        if (d.ability < 0) return;
        int slot = d.ability;
        kit(d.alien).end(p, d, slot);
        d.ability = -1;
        d.abilityTick = 0;
        d.dirty = true;
    }

    public static void cancel(ServerPlayer p, OmniData d) {
        if (d.alien != null && d.ability >= 0) finish(p, d);
        d.flags = 0;
        d.targetId = -1;
        var inst = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (inst != null) {
            inst.removeModifier(Transform.GUARD_SLOW);
            inst.removeModifier(Transform.BOOST);
        }
        p.setInvisible(false);
        p.noPhysics = false;
    }

    /** client: advance the mirrored ability timer so animations play smoothly between syncs */
    public static void clientTick(Player p, OmniData d) {
        if (d.alien != null && d.ability >= 0) d.abilityTick++;
        if (d.alien != null && d.timeLeft > 0 && Config.durationTicks() > 0) d.timeLeft--;
        if (d.recharge > 0) d.recharge--;
        for (int i = 0; i < 4; i++) if (d.cooldown[i] > 0) d.cooldown[i]--;
        d.blockAmountO = d.blockAmount;
        d.blockAmount += ((d.blocking ? 1F : 0F) - d.blockAmount) * 0.35F;
    }

    public static void commonTick(Player p, OmniData d) {
        kit(d.alien).common(p, d);
    }

    public static void onLand(Player p, OmniData d, float dist) {
        kit(d.alien).land(p, d, dist);
    }

    public static void onAttacked(LivingAttackEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        OmniData d = OmniData.get(p);
        if (d.alien == null) return;
        DamageSource src = e.getSource();
        if (d.alien.has(Alien.Flag.FIRE_IMMUNE) && src.is(DamageTypeTags.IS_FIRE)) {
            e.setCanceled(true);
            return;
        }
        if (d.alien.has(Alien.Flag.FREEZE_IMMUNE) && src.is(DamageTypeTags.IS_FREEZING)) {
            e.setCanceled(true);
            return;
        }
        Kit k = kit(d.alien);
        if (k.immune(p, d, src)) {
            e.setCanceled(true);
            return;
        }
        if (d.blocking && !src.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && facing(p, src) && k.guardCancel(p, d, src)) {
            e.setCanceled(true);
        }
    }

    public static void onHurt(LivingHurtEvent e) {
        DamageSource src = e.getSource();
        // alien melee
        if (src.getDirectEntity() instanceof ServerPlayer atk && src.getEntity() == atk && e.getEntity() != atk) {
            OmniData d = OmniData.get(atk);
            if (d.alien != null && !src.is(DamageTypeTags.IS_PROJECTILE) && src.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK) && d.ability < 0) {
                kit(d.alien).melee(atk, d, e.getEntity(), e);
            }
        }
        // guarding alien
        if (e.getEntity() instanceof ServerPlayer p) {
            OmniData d = OmniData.get(p);
            if (d.alien != null && d.blocking && !src.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && facing(p, src)) {
                e.setAmount(e.getAmount() * (1 - d.alien.blockReduction));
                LivingEntity atk = src.getEntity() instanceof LivingEntity le ? le : null;
                kit(d.alien).guardHit(p, d, src, atk);
            }
        }
    }

    /** guards only protect against attacks from the front half (Blowpoke's shell covers everything) */
    private static boolean facing(ServerPlayer p, DamageSource src) {
        OmniData d = OmniData.get(p);
        if (d.alien == Alien.BLOWPOKE || d.alien == Alien.GOOP) return true;
        Vec3 from = src.getSourcePosition();
        if (from == null) return true;
        Vec3 to = from.subtract(p.position());
        Vec3 look = Vec3.directionFromRotation(0, p.getYRot());
        return new Vec3(to.x, 0, to.z).normalize().dot(look) > -0.2;
    }

    public static Entity target(ServerPlayer p, OmniData d) {
        return d.targetId < 0 ? null : p.level().getEntity(d.targetId);
    }
}
