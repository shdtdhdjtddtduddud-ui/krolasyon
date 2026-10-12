package com.krolasyon.ben10.ability;

import com.krolasyon.ben10.OmniData;
import com.krolasyon.ben10.entity.AlienProjectile;
import com.krolasyon.ben10.entity.FxEntity;
import com.krolasyon.ben10.net.Net;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/** Goop: acid spit, slime whip, engulf, goo wave. Guard = liquid body (projectiles pass through). */
public class GoopKit extends Kit {
    private static final int GOO = 0x7EE030;

    @Override
    public boolean start(ServerPlayer p, OmniData d, int slot) {
        ServerLevel l = p.serverLevel();
        switch (slot) {
            case 0 -> Fx.sound(l, p.position(), SoundEvents.SLIME_JUMP, 1F, 0.6F);
            case 1 -> Fx.sound(l, p.position(), SoundEvents.SLIME_SQUISH, 1.2F, 0.5F);
            case 2 -> {
                LivingEntity e = Fx.nearestInFront(p, 4.5, 60);
                if (e == null || e.getBbHeight() > 3.2F) {
                    p.displayClientMessage(net.minecraft.network.chat.Component.translatable("msg.krolben10.notarget"), true);
                    return false;
                }
                d.targetId = e.getId();
                Fx.sound(l, p.position(), SoundEvents.SLIME_BLOCK_FALL, 1.5F, 0.5F);
            }
            case 3 -> {
                Fx.sound(l, p.position(), SoundEvents.SLIME_SQUISH, 1.5F, 0.4F);
                p.refreshDimensions();
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
                if (t == 5) {
                    Vec3 from = Fx.rel(p, 0.7, 0, 0.25);
                    LivingEntity aim = Fx.aimed(p, 30);
                    Vec3 dir = aim != null ? aim.position().add(0, aim.getBbHeight() / 2, 0).subtract(from).normalize() : p.getLookAngle();
                    for (int i = 0; i < 3; i++) {
                        Vec3 v = i == 0 ? dir : dir.add(l.random.nextGaussian() * 0.08, l.random.nextGaussian() * 0.05, l.random.nextGaussian() * 0.08).normalize();
                        AlienProjectile a = AlienProjectile.shoot(p, AlienProjectile.ACID, from, v.scale(i == 0 ? 1.5 : 1.25), i == 0 ? 7F : 3F);
                        a.gravity = 0.025F;
                        a.setScale(i == 0 ? 1.3F : 0.8F);
                        a.life = 60;
                    }
                    Fx.sound(l, from, SoundEvents.LLAMA_SPIT, 1.2F, 0.6F);
                    Fx.particles(l, ParticleTypes.ITEM_SLIME, from, 10, 0.2, 0.1);
                }
            }
            case 1 -> {
                if (t >= 3 && t <= 8) {
                    double len = Math.min(9, (t - 2) * 1.8);
                    Vec3 a = Fx.rel(p, 0.6, 0.4, 0.4);
                    Vec3 b = a.add(p.getLookAngle().scale(len));
                    Fx.line(l, Fx.dust(GOO, 1.6F), a, b, 0.35, 0.05);
                    if (t == 6 || t == 8) {
                        for (LivingEntity e : Fx.beam(p, a, b, 1.1)) {
                            Fx.hit(p, e, 6F);
                            Vec3 pull = p.position().subtract(e.position()).normalize();
                            Fx.push(e, pull.scale(1.25).add(0, 0.35, 0));
                            e.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
                            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
                        }
                        Fx.shatter(l, b, 1.2, 4);
                        Fx.sound(l, b, SoundEvents.SLIME_ATTACK, 1.2F, 0.7F);
                    }
                }
            }
            case 2 -> {
                Entity e = Powers.target(p, d);
                if (!(e instanceof LivingEntity le) || !le.isAlive()) {
                    if (t > 2) Powers.finish(p, d);
                    return;
                }
                if (t == 4) d.set(OmniData.F_ENGULF, true);
                if (t >= 4 && t < 52) {
                    Vec3 in = p.position().add(0, 0.35, 0).add(Fx.flatLook(p).scale(0.1));
                    Vec3 pos = t < 9 ? le.position().lerp(in, 0.4) : in;
                    le.setPos(pos.x, pos.y, pos.z);
                    le.setDeltaMovement(Vec3.ZERO);
                    le.hurtMarked = true;
                    le.fallDistance = 0;
                    if (le instanceof ServerPlayer sp) sp.connection.teleport(pos.x, pos.y, pos.z, sp.getYRot(), sp.getXRot());
                    if (t % 8 == 0) {
                        Fx.hitMagic(p, le, 3.5F);
                        le.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 1));
                        Fx.sound(l, p.position(), SoundEvents.SLIME_SQUISH_SMALL, 1F, 0.6F);
                    }
                    Fx.particles(l, Fx.dust(GOO, 1.2F), p.position().add(0, 1, 0), 3, 0.5, 0.02);
                    if (t % 3 == 0) Fx.particles(l, ParticleTypes.BUBBLE_POP, p.position().add(0, 1, 0), 2, 0.4, 0.02);
                }
                if (t == 52) {
                    d.set(OmniData.F_ENGULF, false);
                    Fx.hit(p, le, 6F);
                    Fx.push(le, p.getLookAngle().scale(1.6).add(0, 0.5, 0));
                    Fx.sound(l, p.position(), SoundEvents.LLAMA_SPIT, 1.5F, 0.5F);
                    Fx.particles(l, ParticleTypes.ITEM_SLIME, p.position().add(0, 1, 0), 30, 0.5, 0.2);
                    d.targetId = -1;
                }
            }
            case 3 -> {
                if (t < 12) Fx.particles(l, Fx.dust(GOO, 1.5F), p.position().add(0, 0.2, 0), 6, 0.7, 0.02);
                if (t == 12) {
                    Vec3 c = p.position();
                    FxEntity.spawn(l, c.add(0, 0.08, 0), FxEntity.RING, 14, 8F, GOO, p);
                    FxEntity.spawn(l, c.add(0, 0.5, 0), FxEntity.FLASH, 10, 4F, GOO, p);
                    for (int i = 0; i < 7; i++) {
                        double a = i * Math.PI * 2 / 7, r = 2.5 + l.random.nextDouble() * 3.5;
                        double x = c.x + Math.cos(a) * r, z = c.z + Math.sin(a) * r;
                        FxEntity.spawn(l, new Vec3(x, Fx.groundY(l, x, c.y + 1, z) + 0.02, z), FxEntity.PUDDLE, 120, 1.4F + l.random.nextFloat(), GOO, p);
                    }
                    for (LivingEntity e : Fx.around(p, c.add(0, 0.5, 0), 7.5)) {
                        Fx.hit(p, e, 8F);
                        Fx.knock(e, c, 1.0, 0.65);
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
                        e.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
                    }
                    for (int i = 0; i < 80; i++) {
                        double a = l.random.nextDouble() * Math.PI * 2;
                        l.sendParticles(ParticleTypes.ITEM_SLIME, c.x, c.y + 0.3, c.z, 0, Math.cos(a), 0.3 + l.random.nextDouble() * 0.4, Math.sin(a), 0.6);
                    }
                    Fx.shatter(l, c, 3.5, 14);
                    Fx.sound(l, c, SoundEvents.SLIME_BLOCK_BREAK, 2F, 0.4F);
                    Fx.sound(l, c, SoundEvents.GENERIC_EXPLODE, 0.8F, 1.4F);
                    Net.shake(p, 16, 0.4F);
                }
            }
            default -> {}
        }
    }

    @Override
    public void end(ServerPlayer p, OmniData d, int slot) {
        if (slot == 2) {
            d.set(OmniData.F_ENGULF, false);
            d.targetId = -1;
        }
        if (slot == 3) p.refreshDimensions();
    }

    @Override
    public void passive(ServerPlayer p, OmniData d) {
        if (p.tickCount % 6 == 0) {
            p.serverLevel().sendParticles(Fx.dust(GOO, 0.8F), p.getX(), p.getY() + 0.3 + p.getRandom().nextDouble() * 1.4, p.getZ(), 1, 0.25, 0.1, 0.25, 0);
        }
        if (p.getDeltaMovement().y < -0.3 && !p.isShiftKeyDown()) {
            // the anti-gravity projector slows falls
            p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 10, 0, false, false));
        }
    }

    @Override
    public void melee(ServerPlayer p, OmniData d, LivingEntity target, LivingHurtEvent e) {
        target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 1));
        Fx.particles(p.serverLevel(), ParticleTypes.ITEM_SLIME, target.position().add(0, target.getBbHeight() / 2, 0), 10, 0.3, 0.1);
        Fx.sound(p.serverLevel(), target.position(), SoundEvents.SLIME_ATTACK, 1F, 0.8F);
    }

    @Override
    public boolean guardCancel(ServerPlayer p, OmniData d, DamageSource src) {
        if (src.is(DamageTypeTags.IS_PROJECTILE)) {
            Fx.particles(p.serverLevel(), ParticleTypes.ITEM_SLIME, p.position().add(0, 1, 0), 12, 0.4, 0.1);
            Fx.sound(p.serverLevel(), p.position(), SoundEvents.SLIME_SQUISH, 1F, 1.2F);
            return true;
        }
        return false;
    }

    @Override
    public void guardHit(ServerPlayer p, OmniData d, DamageSource src, LivingEntity attacker) {
        if (attacker != null && attacker.distanceTo(p) < 4) {
            attacker.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1));
            attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
        }
        Fx.sound(p.serverLevel(), p.position(), SoundEvents.SLIME_SQUISH, 1F, 0.7F);
    }
}
