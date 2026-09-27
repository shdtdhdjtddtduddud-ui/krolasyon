package com.krolasyon.bosses.morph.abilities;

import com.krolasyon.bosses.entity.TideGeyserEntity;
import com.krolasyon.bosses.entity.TsunamiWaveEntity;
import com.krolasyon.bosses.morph.FormAbilities;
import com.krolasyon.bosses.morph.Forms;
import com.krolasyon.bosses.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import static com.krolasyon.bosses.morph.MorphServer.*;

/** Kızıl Gölge: Shadow Step, Blood Tempest, Shadow Tails, Black Thorns, Demon Awakening. */
public class ShadeAbilities implements FormAbilities {
    public static final int AWAKEN_TICKS = 200;

    @Override
    public void tick(ServerPlayer p, State s, ServerLevel sl, int id, int t) {
        Vec3 dir = horizontalLook(p);
        switch (id) {
            case 0 -> {   // Shadow Step: melt into shadow, reappear behind the target and backstab
                if (t == 1) sound(p, ModSounds.SHADOW_STEP.get(), 1.3F, 1.0F);
                if (t == 4) {
                    smoke(sl, p.position());
                    LivingEntity target = DragonAbilities.nearestAimed(p, 18);
                    Vec3 to;
                    if (target != null) {
                        Vec3 back = target.position().subtract(p.position()).multiply(1, 0, 1).normalize();
                        to = target.position().add(back.scale(1.4));
                        s.targetId = target.getId();
                    } else {
                        to = blinkTarget(p, dir, 9.0);
                    }
                    p.teleportTo(to.x, to.y, to.z);
                    if (target != null) {
                        Vec3 d = target.position().subtract(to);
                        float yaw = (float) (Mth.atan2(d.z, d.x) * (180F / Math.PI)) - 90F;
                        p.setYRot(yaw);
                        p.setYHeadRot(yaw);
                        p.connection.teleport(to.x, to.y, to.z, yaw, p.getXRot());
                    }
                    p.fallDistance = 0;
                    smoke(sl, to);
                }
                if (t == 8) {
                    sound(p, ModSounds.BLADE_SWING.get(), 1.2F, 0.8F);
                    LivingEntity target = s.targetId >= 0 && sl.getEntity(s.targetId) instanceof LivingEntity le ? le : null;
                    if (target != null && target.distanceTo(p) < 4.5) {
                        if (hit(p, target, 14F, 0.6, 0.3)) {
                            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 1));
                            sl.sendParticles(dust(0xD01828, 1.4F), target.getX(), target.getY(0.6), target.getZ(), 20, 0.3, 0.4, 0.3, 0);
                        }
                    }
                    for (LivingEntity e : cone(p, horizontalLook(p), 3.5, 0.5)) if (e != target) hit(p, e, 8F, 0.6, 0.3);
                }
            }
            case 1 -> {   // Blood Tempest: a flurry of five slashes draining blood
                if (t == 3 || t == 7 || t == 11 || t == 15 || t == 19 || t == 23) {
                    sound(p, ModSounds.BLADE_SWING.get(), 1.0F, 0.9F + t * 0.015F);
                    if (t == 3) sound(p, ModSounds.BLOOD_FLURRY.get(), 1.2F, 1.0F);
                    arc(sl, p, dir, 2.4, 1.1 + (t % 8 == 3 ? 0.4 : -0.1), dust(0xD01828, 1.1F), ParticleTypes.SMOKE);
                    for (LivingEntity e : cone(p, dir, 4.2, 0.35)) {
                        if (hit(p, e, 4.5F, 0.15, 0.05)) {
                            e.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
                            p.heal(0.8F);
                            sl.sendParticles(dust(0x8A0A14, 1.2F), e.getX(), e.getY(0.5), e.getZ(), 8, 0.2, 0.3, 0.2, 0);
                        }
                    }
                }
            }
            case 2 -> {   // Shadow Tails: the tattered tails whip in a full circle, dragging and hurling foes
                if (t == 3) sound(p, ModSounds.TAIL_LASH.get(), 1.4F, 1.0F);
                if (t >= 5 && t <= 22) {
                    Vec3 c = p.position();
                    double a = (t - 5) / 17.0 * Math.PI * 2 + p.getYRot() * Mth.DEG_TO_RAD;
                    for (int k = 1; k <= 5; k++) {
                        Vec3 q = c.add(Math.cos(a) * k * 1.1, 0.6, Math.sin(a) * k * 1.1);
                        sl.sendParticles(ParticleTypes.LARGE_SMOKE, q.x, q.y, q.z, 1, 0.05, 0.05, 0.05, 0.01);
                        if (k % 2 == 0) sl.sendParticles(dust(0xD01828, 1.0F), q.x, q.y, q.z, 1, 0, 0, 0, 0);
                    }
                    for (LivingEntity e : targets(p, new AABB(c, c).inflate(5.8, 2, 5.8))) {
                        if (!s.hit.add(e.getId())) continue;
                        if (hit(p, e, 8F, -0.6, 0.9)) {
                            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                            sl.sendParticles(ParticleTypes.SWEEP_ATTACK, e.getX(), e.getY(0.5), e.getZ(), 1, 0, 0, 0, 0);
                        }
                    }
                }
            }
            case 3 -> {   // Black Thorns: plunge the blade, rows of black thorns burst out in a fan
                if (t == 11) {
                    sound(p, ModSounds.THORN_ERUPT.get(), 1.6F, 0.8F);
                    Vec3 c = p.position();
                    sl.sendParticles(ParticleTypes.LARGE_SMOKE, c.x, c.y + 0.2, c.z, 20, 1, 0.1, 1, 0.02);
                    float yaw = p.getYRot();
                    for (int row = -2; row <= 2; row++) {
                        float a = (yaw + row * 22F) * Mth.DEG_TO_RAD;
                        Vec3 d = new Vec3(-Mth.sin(a), 0, Mth.cos(a));
                        for (int i = 1; i <= 7; i++) {
                            Vec3 q = c.add(d.scale(1 + i * 1.5));
                            TideGeyserEntity.spawn(p, TideGeyserEntity.THORN, q.x, q.z, c.y, i * 2, 8F, i == 1 && row == 0);
                        }
                    }
                }
            }
            case 4 -> {   // Demon Awakening: roar, a shockwave of dread, and a blood-hungry frenzy
                if (t == 2) sound(p, ModSounds.AWAKEN.get(), 1.8F, 1.0F);
                if (t < 16) {
                    Vec3 c = p.position().add(0, 1, 0);
                    sl.sendParticles(ParticleTypes.SMOKE, c.x, c.y, c.z, 4, 1.2, 1, 1.2, 0.0);
                    sl.sendParticles(dust(0xD01828, 1.0F), c.x, c.y, c.z, 4, 0.8, 1.0, 0.8, 0);
                }
                if (t == 16) {
                    Vec3 c = p.position();
                    s.buffTicks = AWAKEN_TICKS;
                    p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, AWAKEN_TICKS, 1, false, true));
                    p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, AWAKEN_TICKS, 1, false, true));
                    p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, AWAKEN_TICKS, 0, false, true));
                    ring(sl, c.add(0, 1.0, 0), 2.0, 40, dust(0xD01828, 1.4F), 0);
                    TsunamiWaveEntity.spawn(p, c, TsunamiWaveEntity.SHADOW, 11F, 7F);
                }
            }
            default -> {}
        }
    }

    private static void smoke(ServerLevel sl, Vec3 at) {
        sl.sendParticles(ParticleTypes.SMOKE, at.x, at.y + 1, at.z, 30, 0.4, 0.8, 0.4, 0.03);
        sl.sendParticles(dust(0xD01828, 1.2F), at.x, at.y + 1, at.z, 20, 0.4, 0.8, 0.4, 0);
        sl.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y + 1, at.z, 20, 0.3, 0.6, 0.3, 0.05);
    }

    @Override
    public void passive(ServerPlayer p, State s, ServerLevel sl) {
        if (s.buffTicks > 0 && s.form == Forms.SHADE && p.tickCount % 2 == 0) {
            sl.sendParticles(dust(0xD01828, 1.0F), p.getX(), p.getY() + 1.2, p.getZ(), 2, 0.4, 0.8, 0.4, 0);
            sl.sendParticles(ParticleTypes.SMOKE, p.getX(), p.getY() + 0.2, p.getZ(), 1, 0.3, 0.1, 0.3, 0.01);
        }
    }

    @Override
    public void transformTick(ServerPlayer p, ServerLevel sl, int t) {
        FireFx.cocoon(p, sl, t, Forms.COLOR[Forms.SHADE], ParticleTypes.SMOKE, ParticleTypes.REVERSE_PORTAL);
    }

    @Override
    public void transformBurst(ServerPlayer p, ServerLevel sl) {
        Vec3 c = p.position();
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, c.x, c.y + 1.5, c.z, 12, 1.2, 1.4, 1.2, 0.04);
        sl.sendParticles(ParticleTypes.SMOKE, c.x, c.y + 1.5, c.z, 40, 1.2, 1.4, 1.2, 0.06);
        sl.sendParticles(dust(0xD01828, 1.5F), c.x, c.y + 1.5, c.z, 60, 1.2, 1.4, 1.2, 0);
        ring(sl, c.add(0, 0.1, 0), 3.0, 40, ParticleTypes.LARGE_SMOKE, 0.05);
    }
}
