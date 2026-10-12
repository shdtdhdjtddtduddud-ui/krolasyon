package com.krolasyon.ben10.ability;

import com.krolasyon.ben10.ModRegistry;
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

import java.util.ArrayList;
import java.util.List;

/** Ultimate Echo Echo: sticky sonic discs, sonic beam, homing disc barrage, sonic doom. Guard = spinning disc wall. */
public class EchoEchoKit extends Kit {
    private static final int CYAN = 0x30E8FF;

    @Override
    public boolean start(ServerPlayer p, OmniData d, int slot) {
        ServerLevel l = p.serverLevel();
        switch (slot) {
            case 0 -> Fx.sound(l, p.position(), ModRegistry.S_SONIC.get(), 1F, 1.6F);
            case 1 -> Fx.sound(l, p.position(), SoundEvents.BEACON_ACTIVATE, 1F, 1.8F);
            case 2 -> Fx.sound(l, p.position(), ModRegistry.S_SONIC.get(), 1.2F, 1.2F);
            case 3 -> {
                LivingEntity e = Fx.aimed(p, 24);
                d.vec = e != null ? e.position().add(0, e.getBbHeight() / 2, 0) : Fx.rayEnd(p, 20);
                d.targetId = e == null ? -1 : e.getId();
                Fx.sound(l, p.position(), SoundEvents.WARDEN_SONIC_CHARGE, 1.2F, 1.3F);
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
                    Vec3 from = Fx.rel(p, 0.9, 0.3, 0.3);
                    LivingEntity aim = Fx.aimed(p, 30);
                    Vec3 dir = aim != null ? aim.position().add(0, aim.getBbHeight() / 2, 0).subtract(from).normalize() : p.getLookAngle();
                    for (int i = -1; i <= 1; i++) {
                        double r = Math.toRadians(i * 9);
                        Vec3 v = new Vec3(dir.x * Math.cos(r) - dir.z * Math.sin(r), dir.y, dir.x * Math.sin(r) + dir.z * Math.cos(r));
                        AlienProjectile a = AlienProjectile.shoot(p, AlienProjectile.DISC, from, v.scale(1.6), 5F);
                        a.gravity = 0.01F;
                        a.life = 60;
                    }
                    Fx.sound(l, from, SoundEvents.TRIDENT_THROW, 1F, 1.6F);
                }
            }
            case 1 -> {
                if (t < 6 || t > 46) return;
                Vec3 a = Fx.rel(p, 0.8, 0, 0.4);
                Vec3 b = Fx.rayEnd(p, 18);
                if (t % 3 == 0) FxEntity.spawn(l, a, FxEntity.BEAM, 4, 0.9F, CYAN, p).vec(b.subtract(a));
                if (t % 4 == 0) FxEntity.spawn(l, a, FxEntity.SONIC_RING, 8, 1.6F, CYAN, p).yaw(p.getYRot());
                if (t % 2 == 0) {
                    Vec3 dir = b.subtract(a).normalize();
                    for (LivingEntity e : Fx.beam(p, a, b, 1.2)) {
                        Fx.hit(p, e, 2.8F);
                        Fx.push(e, e.getDeltaMovement().add(dir.scale(0.22)));
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2));
                        e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0));
                    }
                }
                if (t % 4 == 0) {
                    Vec3 dir = b.subtract(a);
                    for (int i = 1; i <= 6; i++) Fx.shatter(l, a.add(dir.scale(i / 6.0)), 1.0, 2);
                    Fx.particles(l, Fx.dust(CYAN, 1F), b, 6, 0.3, 0.05);
                }
                if (t % 5 == 0) Fx.sound(l, p.position(), ModRegistry.S_SONIC.get(), 1F, 0.8F + l.random.nextFloat() * 0.3F);
            }
            case 2 -> {
                if (t >= 4 && t <= 26 && t % 2 == 0) {
                    List<LivingEntity> tg = Fx.around(p, p.position(), 20);
                    int k = (t - 4) / 2;
                    double ang = k * 2.4;
                    Vec3 from = p.position().add(Math.cos(ang) * 1.1, 1.6 + Math.sin(k * 1.3) * 0.5, Math.sin(ang) * 1.1);
                    Vec3 v;
                    AlienProjectile a;
                    if (!tg.isEmpty()) {
                        LivingEntity e = tg.get(k % tg.size());
                        v = e.position().add(0, e.getBbHeight() / 2, 0).subtract(from).normalize().add(0, 0.4, 0).normalize().scale(1.1);
                        a = AlienProjectile.shoot(p, AlienProjectile.HOMING_DISC, from, v, 4.5F);
                        a.homing = e.getId();
                    } else {
                        v = p.getLookAngle().add(l.random.nextGaussian() * 0.15, l.random.nextGaussian() * 0.1, l.random.nextGaussian() * 0.15).normalize().scale(1.4);
                        a = AlienProjectile.shoot(p, AlienProjectile.HOMING_DISC, from, v, 4.5F);
                    }
                    a.life = 70;
                    Fx.sound(l, from, ModRegistry.S_SONIC.get(), 0.6F, 1.8F);
                }
            }
            case 3 -> {
                Vec3 c = d.vec;
                Entity te = Powers.target(p, d);
                if (te != null && te.isAlive()) c = te.position().add(0, te.getBbHeight() / 2, 0);
                if (t >= 4 && t <= 18 && t % 2 == 0) {
                    int k = (t - 4) / 2;
                    double a = k * Math.PI * 2 / 8, el = (k % 2 == 0 ? 0.35 : -0.25);
                    Vec3 at = c.add(Math.cos(a) * 3 * Math.cos(el), Math.sin(el) * 3 + 0.3, Math.sin(a) * 3 * Math.cos(el));
                    AlienProjectile disc = AlienProjectile.shoot(p, AlienProjectile.DISC, at, Vec3.ZERO, 3F);
                    disc.plant();
                    disc.setNoGravity(true);
                    disc.setScale(1.3F);
                    Fx.line(l, Fx.dust(CYAN, 1F), p.position().add(0, 1.6, 0), at, 0.6, 0.02);
                    Fx.sound(l, at, SoundEvents.AMETHYST_BLOCK_PLACE, 1F, 1.6F);
                }
                if (t > 18 && t < 34) {
                    FxEntity.spawn(l, c, FxEntity.SONIC_RING, 6, 3.5F - (t - 18) * 0.15F, CYAN, p).yaw(t * 25);
                    if (t % 3 == 0) Fx.sound(l, c, ModRegistry.S_SONIC.get(), 1.2F, 0.6F + (t - 18) * 0.06F);
                    for (LivingEntity e : Fx.around(p, c, 3.2)) {
                        e.setDeltaMovement(e.getDeltaMovement().scale(0.3));
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 5));
                    }
                }
                if (t == 34) {
                    FxEntity.spawn(l, c, FxEntity.SPHERE, 16, 7F, CYAN, p);
                    FxEntity.spawn(l, c, FxEntity.FLASH, 12, 5F, 0xFFFFFF, p);
                    for (LivingEntity e : Fx.around(p, c, 6.5)) {
                        float f = (float) Math.max(0.4, 1 - e.position().distanceTo(c) / 8);
                        Fx.hit(p, e, 18F * f);
                        Fx.knock(e, c, 1.6 * f, 0.6);
                        e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 0));
                        e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1));
                    }
                    Fx.shatter(l, c, 6, 40);
                    Fx.sound(l, c, SoundEvents.WARDEN_SONIC_BOOM, 2F, 0.7F);
                    Fx.sound(l, c, SoundEvents.GENERIC_EXPLODE, 1F, 1.5F);
                    l.sendParticles(ParticleTypes.SONIC_BOOM, c.x, c.y, c.z, 1, 0, 0, 0, 0);
                    for (AlienProjectile disc : l.getEntitiesOfClass(AlienProjectile.class, new net.minecraft.world.phys.AABB(c, c).inflate(4), x -> x.getOwner() == p && x.stuck() == -1)) disc.discard();
                    Net.shake(p, 30, 0.9F);
                }
            }
            default -> {}
        }
    }

    @Override
    public void end(ServerPlayer p, OmniData d, int slot) {
        d.targetId = -1;
    }

    @Override
    public void passive(ServerPlayer p, OmniData d) {
        if (p.getDeltaMovement().y < -0.25 && !p.isShiftKeyDown() && !p.getAbilities().flying) {
            p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 10, 0, false, false));
        }
        if (p.tickCount % 10 == 0) p.serverLevel().sendParticles(Fx.dust(CYAN, 0.7F), p.getX(), p.getY() + 1.5, p.getZ(), 2, 0.5, 0.6, 0.5, 0);
    }

    @Override
    public void melee(ServerPlayer p, OmniData d, LivingEntity target, LivingHurtEvent e) {
        target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
        FxEntity.spawn(p.serverLevel(), target.position().add(0, target.getBbHeight() / 2, 0), FxEntity.SONIC_RING, 8, 1.6F, CYAN, p).yaw(p.getYRot());
        Fx.shatter(p.serverLevel(), target.position().add(0, 1, 0), 1.6, 3);
        Fx.sound(p.serverLevel(), target.position(), ModRegistry.S_SONIC.get(), 0.7F, 1.5F);
    }

    @Override
    public boolean guardCancel(ServerPlayer p, OmniData d, DamageSource src) {
        if (src.is(DamageTypeTags.IS_PROJECTILE)) {
            if (src.getDirectEntity() != null) src.getDirectEntity().discard();
            FxEntity.spawn(p.serverLevel(), p.position().add(Fx.flatLook(p).scale(0.8)).add(0, 1.2, 0), FxEntity.SONIC_RING, 8, 2F, CYAN, p).yaw(p.getYRot());
            Fx.sound(p.serverLevel(), p.position(), ModRegistry.S_SONIC.get(), 0.8F, 1.7F);
            return true;
        }
        return false;
    }

    @Override
    public void guardHit(ServerPlayer p, OmniData d, DamageSource src, LivingEntity attacker) {
        if (attacker != null && attacker.distanceTo(p) < 5) {
            Fx.knock(attacker, p.position(), 1.2, 0.3);
            attacker.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0));
        }
        Fx.sound(p.serverLevel(), p.position(), ModRegistry.S_SONIC.get(), 0.8F, 1.2F);
    }
}
