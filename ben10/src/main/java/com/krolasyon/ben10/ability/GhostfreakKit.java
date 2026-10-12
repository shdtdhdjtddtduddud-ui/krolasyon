package com.krolasyon.ben10.ability;

import com.krolasyon.ben10.OmniData;
import com.krolasyon.ben10.Transform;
import com.krolasyon.ben10.entity.FxEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.Comparator;

/** Ghostfreak: shadow claw lunge, fear gaze, possession, phantom form. Guard = ghostly evasion. */
public class GhostfreakKit extends Kit {
    private static final int SHADE = 0x403050;

    @Override
    public boolean start(ServerPlayer p, OmniData d, int slot) {
        ServerLevel l = p.serverLevel();
        switch (slot) {
            case 0 -> Fx.sound(l, p.position(), SoundEvents.PHANTOM_SWOOP, 1.2F, 0.8F);
            case 1 -> Fx.sound(l, p.position(), SoundEvents.PHANTOM_AMBIENT, 1.5F, 0.5F);
            case 2 -> {
                LivingEntity e = Fx.aimed(p, 14);
                if (e == null) e = Fx.nearestInFront(p, 8, 40);
                if (e == null) {
                    p.displayClientMessage(net.minecraft.network.chat.Component.translatable("msg.krolben10.notarget"), true);
                    return false;
                }
                d.targetId = e.getId();
                Fx.sound(l, p.position(), SoundEvents.SOUL_ESCAPE, 2F, 0.6F);
            }
            case 3 -> {
                d.set(OmniData.F_PHASED, true);
                Transform.add(p, Attributes.MOVEMENT_SPEED, Transform.BOOST, 0.35, AttributeModifier.Operation.MULTIPLY_TOTAL);
                Fx.sound(l, p.position(), SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.4F, 0.6F);
                FxEntity.spawn(l, p.position(), FxEntity.SHADOW, 20, 1.2F, SHADE, p);
            }
            default -> {}
        }
        return true;
    }

    @Override
    public void tick(ServerPlayer p, OmniData d, int slot, int t) {
        ServerLevel l = p.serverLevel();
        switch (slot) {
            case 0 -> {
                if (t >= 2 && t <= 4) Fx.push(p, p.getLookAngle().scale(1.2).add(0, 0.05, 0));
                if (t == 5) {
                    for (LivingEntity e : Fx.cone(p, 4.8, 70)) {
                        Fx.hit(p, e, 9F);
                        e.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 1));
                        Fx.knock(e, p.position(), 0.7, 0.3);
                        Fx.particles(l, ParticleTypes.SCULK_SOUL, e.position().add(0, 1, 0), 6, 0.3, 0.05);
                    }
                    Vec3 at = Fx.rel(p, 2, 0, 0.5);
                    Fx.particles(l, ParticleTypes.SWEEP_ATTACK, at, 3, 0.6, 0);
                    Fx.particles(l, ParticleTypes.SQUID_INK, at, 16, 0.6, 0.05);
                    Fx.sound(l, at, SoundEvents.PHANTOM_BITE, 1.4F, 0.7F);
                    Fx.sound(l, at, SoundEvents.PLAYER_ATTACK_SWEEP, 1F, 0.6F);
                }
                if (t == 7) Fx.push(p, p.getDeltaMovement().scale(0.3));
            }
            case 1 -> {
                if (t < 8) Fx.particles(l, ParticleTypes.SCULK_SOUL, p.position().add(0, 1.3, 0), 3, 0.3, 0.02);
                if (t == 8) {
                    Fx.sound(l, p.position(), SoundEvents.GHAST_SCREAM, 1.2F, 0.5F);
                    Fx.sound(l, p.position(), SoundEvents.WARDEN_ROAR, 0.6F, 1.6F);
                    FxEntity.spawn(l, p.position().add(0, 1.3, 0), FxEntity.SPHERE, 14, 6F, 0x8040A0, p);
                    for (LivingEntity e : Fx.cone(p, 13, 75)) {
                        Fx.hitMagic(p, e, 4F);
                        e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 1));
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                        FxEntity.spawn(l, e.position(), FxEntity.SHADOW, 30, 0.9F, SHADE, null).follow(e);
                        if (e instanceof Player pl) {
                            pl.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 0));
                            pl.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0));
                            pl.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
                        } else if (e instanceof Mob m) {
                            m.setTarget(null);
                            if (m instanceof PathfinderMob pm) {
                                Vec3 away = e.position().add(e.position().subtract(p.position()).multiply(1, 0, 1).normalize().scale(14));
                                pm.getNavigation().moveTo(away.x, away.y, away.z, 1.7);
                            }
                        }
                    }
                }
                if (t > 8 && t < 28 && t % 4 == 0) {
                    for (Mob m : l.getEntitiesOfClass(Mob.class, new AABB(p.position(), p.position()).inflate(14), m -> m.hasEffect(MobEffects.WEAKNESS))) {
                        if (m.getTarget() == p) m.setTarget(null);
                        if (m instanceof PathfinderMob pm && pm.getNavigation().isDone()) {
                            Vec3 away = m.position().add(m.position().subtract(p.position()).multiply(1, 0, 1).normalize().scale(10));
                            pm.getNavigation().moveTo(away.x, away.y, away.z, 1.6);
                        }
                    }
                }
            }
            case 2 -> {
                Entity te = Powers.target(p, d);
                if (!(te instanceof LivingEntity e) || !e.isAlive()) {
                    if (t > 1) Powers.finish(p, d);
                    return;
                }
                if (t < 8) {
                    Fx.line(l, ParticleTypes.SCULK_SOUL, p.position().add(0, 1.2, 0), e.position().add(0, 1, 0), 0.6, 0.05);
                    Vec3 to = e.position().subtract(p.position());
                    Fx.push(p, to.normalize().scale(Math.min(1.2, to.length() * 0.4)));
                }
                if (t == 8) {
                    d.set(OmniData.F_POSSESS, true);
                    p.setInvisible(true);
                    Fx.sound(l, e.position(), SoundEvents.SOUL_ESCAPE, 2F, 0.4F);
                    FxEntity.spawn(l, e.position(), FxEntity.SHADOW, 30, 1.3F, SHADE, null).follow(e);
                    if (e instanceof Mob m) {
                        m.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 120, 1));
                        m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 1));
                        m.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0));
                    }
                }
                if (t >= 8 && d.has(OmniData.F_POSSESS)) {
                    Vec3 in = e.position().add(0, Math.max(0, e.getBbHeight() - p.getBbHeight()) * 0.5, 0);
                    p.teleportTo(in.x, in.y, in.z);
                    p.fallDistance = 0;
                    if (t % 3 == 0) Fx.particles(l, ParticleTypes.SCULK_SOUL, e.position().add(0, e.getBbHeight() * 0.6, 0), 2, 0.3, 0.02);
                    if (e instanceof Mob m && t % 10 == 0) {
                        LivingEntity victim = l.getEntitiesOfClass(LivingEntity.class, e.getBoundingBox().inflate(16),
                                        o -> o != e && o != p && o.isAlive() && !(o instanceof Player pl && (pl.isCreative() || pl == p)) && Fx.valid(p, o))
                                .stream().min(Comparator.comparingDouble(o -> o.distanceToSqr(e))).orElse(null);
                        m.setTarget(victim);
                    }
                    if (e instanceof Player pl && t % 10 == 0) {
                        pl.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0));
                        pl.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0));
                        pl.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2));
                    }
                    if (t % 20 == 0) Fx.hitMagic(p, e, 2.5F);
                }
            }
            case 3 -> {
                if (t % 2 == 0) Fx.particles(l, ParticleTypes.SQUID_INK, p.position().add(0, 1, 0), 2, 0.3, 0.01);
                if (t % 10 == 0) {
                    for (Mob m : l.getEntitiesOfClass(Mob.class, new AABB(p.position(), p.position()).inflate(20), m -> m.getTarget() == p)) m.setTarget(null);
                    for (LivingEntity e : Fx.around(p, p.position().add(0, 1, 0), 1.6)) {
                        Fx.hitMagic(p, e, 3F);
                        e.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 1));
                        Fx.particles(l, ParticleTypes.SCULK_SOUL, e.position().add(0, 1, 0), 4, 0.3, 0.02);
                    }
                }
            }
            default -> {}
        }
    }

    @Override
    public void end(ServerPlayer p, OmniData d, int slot) {
        ServerLevel l = p.serverLevel();
        if (slot == 2 && d.has(OmniData.F_POSSESS)) {
            Entity te = Powers.target(p, d);
            d.set(OmniData.F_POSSESS, false);
            p.setInvisible(false);
            if (te instanceof LivingEntity e && e.isAlive()) {
                Fx.hitMagic(p, e, 10F);
                e.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
                if (e instanceof Mob m) m.setTarget(null);
                Vec3 out = e.position().subtract(Fx.flatLook(p).scale(1.8));
                p.teleportTo(out.x, e.getY(), out.z);
                Fx.particles(l, ParticleTypes.SCULK_SOUL, e.position().add(0, 1, 0), 30, 0.5, 0.1);
                Fx.sound(l, e.position(), SoundEvents.SOUL_ESCAPE, 2F, 1.2F);
            }
        }
        if (slot == 3) {
            d.set(OmniData.F_PHASED, false);
            var inst = p.getAttribute(Attributes.MOVEMENT_SPEED);
            if (inst != null) inst.removeModifier(Transform.BOOST);
            Fx.sound(l, p.position(), SoundEvents.ILLUSIONER_MIRROR_MOVE, 1F, 1.2F);
        }
        d.targetId = -1;
    }

    @Override
    public boolean immune(ServerPlayer p, OmniData d, DamageSource src) {
        return d.has(OmniData.F_PHASED) || d.has(OmniData.F_POSSESS);
    }

    @Override
    public void passive(ServerPlayer p, OmniData d) {
        ServerLevel l = p.serverLevel();
        if (p.tickCount % 8 == 0) l.sendParticles(ParticleTypes.SMOKE, p.getX(), p.getY() + 0.4, p.getZ(), 1, 0.2, 0.1, 0.2, 0.005);
        if (p.getDeltaMovement().y < -0.2 && !p.isShiftKeyDown() && !p.getAbilities().flying) {
            p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 10, 0, false, false));
        }
    }

    @Override
    public void melee(ServerPlayer p, OmniData d, LivingEntity target, LivingHurtEvent e) {
        target.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
        Fx.particles(p.serverLevel(), ParticleTypes.SCULK_SOUL, target.position().add(0, target.getBbHeight() / 2, 0), 5, 0.2, 0.02);
        Fx.sound(p.serverLevel(), target.position(), SoundEvents.PHANTOM_BITE, 0.8F, 0.9F);
    }

    @Override
    public boolean guardCancel(ServerPlayer p, OmniData d, DamageSource src) {
        if (p.getRandom().nextFloat() < 0.5F) {
            FxEntity.spawn(p.serverLevel(), p.position(), FxEntity.SHADOW, 14, 1F, SHADE, p);
            Fx.sound(p.serverLevel(), p.position(), SoundEvents.ILLUSIONER_MIRROR_MOVE, 0.8F, 1.5F);
            return true;
        }
        return false;
    }
}
