package com.krolasyon.bosses.morph.abilities;

import com.krolasyon.bosses.entity.FormProjectileEntity;
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

/** Ejder Muhafızı: Dragon Breath, Twin Dragon Heads, Scale Aegis, Dragon Dive, Crimson Dragon. */
public class DragonAbilities implements FormAbilities {
    public static final int AEGIS_TICKS = 160;

    @Override
    public void tick(ServerPlayer p, State s, ServerLevel sl, int id, int t) {
        Vec3 dir = horizontalLook(p);
        switch (id) {
            case 0 -> {   // Dragon Breath: the mask pours a cone of dragon fire
                if (t == 8) sound(p, ModSounds.BREATH.get(), 1.5F, 1.0F);
                if (t >= 10 && t <= 44) {
                    Vec3 look = p.getLookAngle();
                    Vec3 mouth = p.getEyePosition().add(0, 0.35, 0).add(look.scale(0.9));
                    for (int i = 0; i < 10; i++) {
                        Vec3 spread = look.add(p.getRandom().nextGaussian() * 0.12, p.getRandom().nextGaussian() * 0.08, p.getRandom().nextGaussian() * 0.12).normalize();
                        double sp = 0.6 + p.getRandom().nextDouble() * 0.5;
                        sl.sendParticles(ParticleTypes.FLAME, mouth.x, mouth.y, mouth.z, 0, spread.x, spread.y, spread.z, sp);
                        if (i % 3 == 0) sl.sendParticles(ParticleTypes.LARGE_SMOKE, mouth.x, mouth.y, mouth.z, 0, spread.x, spread.y, spread.z, sp * 0.6);
                    }
                    if (t % 4 == 0) {
                        for (LivingEntity e : targets(p, p.getBoundingBox().inflate(8, 3, 8))) {
                            Vec3 to = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(mouth);
                            double d = to.length();
                            if (d < 9 && to.normalize().dot(look) > 0.8) burn(p, e, 3.5F, 0.15, 0.05, 6);
                        }
                    }
                }
            }
            case 1 -> {   // Twin Dragon Heads: the golden pauldrons spit homing fireballs
                if (t == 11 || t == 15 || t == 19) {
                    sound(p, ModSounds.FIREBALL_WHOOSH.get(), 1.2F, 0.9F + t * 0.01F);
                    LivingEntity target = nearestAimed(p, 24);
                    float yaw = p.getYRot() * Mth.DEG_TO_RAD;
                    Vec3 right = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));
                    for (int side = -1; side <= 1; side += 2) {
                        Vec3 from = p.position().add(0, 2.2, 0).add(right.scale(side * 0.7));
                        Vec3 vel = p.getLookAngle().add(right.scale(side * 0.25)).normalize().scale(1.1);
                        FormProjectileEntity.shoot(p, FormProjectileEntity.FIREBALL, from, vel, 7F).homing(target);
                        sl.sendParticles(ParticleTypes.FLAME, from.x, from.y, from.z, 10, 0.1, 0.1, 0.1, 0.05);
                    }
                }
            }
            case 2 -> {   // Scale Aegis: whirl the glaive, golden fire shield and a repelling blast
                if (t == 6) {
                    sound(p, ModSounds.SHIELD_UP.get(), 1.4F, 1.0F);
                    s.buffTicks = AEGIS_TICKS;
                    p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, AEGIS_TICKS, 2, false, false));
                    p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, AEGIS_TICKS, 1, false, false));
                    Vec3 c = p.position();
                    ring(sl, c.add(0, 0.2, 0), 3.5, 40, ParticleTypes.FLAME, 0.15);
                    for (LivingEntity e : targets(p, new AABB(c, c).inflate(4.5, 2, 4.5))) burn(p, e, 6F, 1.6, 0.5, 5);
                }
            }
            case 3 -> {   // Dragon Dive: soar up and dive onto the target with the glaive
                if (t == 6) {
                    p.setDeltaMovement(dir.x * 0.9, 1.2, dir.z * 0.9);
                    p.hurtMarked = true;
                    sound(p, ModSounds.TSUNAMI_LEAP.get(), 1.0F, 1.2F);
                    sl.sendParticles(ParticleTypes.FLAME, p.getX(), p.getY(), p.getZ(), 30, 0.6, 0.1, 0.6, 0.1);
                }
                if (t > 6 && !s.slammed) sl.sendParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 1, p.getZ(), 6, 0.4, 0.6, 0.4, 0.02);
                if (t == 16 && !p.onGround()) {
                    p.setDeltaMovement(dir.x * 0.6, -2.4, dir.z * 0.6);
                    p.hurtMarked = true;
                    sound(p, ModSounds.DIVE.get(), 1.4F, 1.0F);
                }
                if (!s.slammed && t >= 10 && (p.onGround() || p.isInWater() || t >= 30)) {
                    s.slammed = true;
                    p.fallDistance = 0;
                    Vec3 c = p.position();
                    sl.sendParticles(ParticleTypes.FLAME, c.x, c.y + 0.3, c.z, 60, 1.5, 0.2, 1.5, 0.12);
                    sl.sendParticles(ParticleTypes.LAVA, c.x, c.y + 0.3, c.z, 20, 1.5, 0.2, 1.5, 0);
                    TsunamiWaveEntity.spawn(p, c, TsunamiWaveEntity.FIRE, 8F, 8F);
                    for (LivingEntity e : targets(p, new AABB(c, c).inflate(3.5, 2, 3.5))) burn(p, e, 12F, 1.0, 1.0, 6);
                }
            }
            case 4 -> {   // Crimson Dragon: a serpent of flame surges forward through everything
                if (t == 6) sound(p, ModSounds.DRAGON_SUMMON.get(), 1.8F, 1.0F);
                if (t < 16) {
                    Vec3 c = p.position().add(0, 1.5, 0);
                    double a = t * 0.8;
                    sl.sendParticles(ParticleTypes.FLAME, c.x + Math.cos(a) * 1.5, c.y + t * 0.1, c.z + Math.sin(a) * 1.5, 4, 0.1, 0.1, 0.1, 0.02);
                }
                if (t == 16) {
                    Vec3 look = p.getLookAngle();
                    Vec3 flat = new Vec3(look.x, Math.max(-0.2, Math.min(0.3, look.y)), look.z).normalize();
                    FormProjectileEntity.shoot(p, FormProjectileEntity.DRAGON, p.getEyePosition().add(flat.scale(1.5)), flat.scale(0.9), 12F);
                }
            }
            default -> {}
        }
    }

    @Override
    public void passive(ServerPlayer p, State s, ServerLevel sl) {
        if (s.buffTicks > 0 && s.form == Forms.DRAGON && p.tickCount % 3 == 0) {
            double a = p.tickCount * 0.3;
            for (int k = 0; k < 2; k++) {
                double b = a + k * Math.PI;
                sl.sendParticles(ParticleTypes.FLAME, p.getX() + Math.cos(b) * 1.1, p.getY() + 0.3 + (p.tickCount % 20) * 0.1, p.getZ() + Math.sin(b) * 1.1, 1, 0, 0, 0, 0);
            }
        }
    }

    static LivingEntity nearestAimed(ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        LivingEntity best = null;
        double bestScore = 0.85;
        for (LivingEntity e : targets(p, p.getBoundingBox().inflate(range))) {
            Vec3 to = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(eye);
            double dot = to.normalize().dot(look);
            if (dot > bestScore && to.length() < range) { bestScore = dot; best = e; }
        }
        return best;
    }

    @Override
    public void transformTick(ServerPlayer p, ServerLevel sl, int t) {
        FireFx.cocoon(p, sl, t, Forms.COLOR[Forms.DRAGON], ParticleTypes.FLAME, ParticleTypes.LAVA);
    }

    @Override
    public void transformBurst(ServerPlayer p, ServerLevel sl) {
        FireFx.burst(p, sl, Forms.COLOR2[Forms.DRAGON], ParticleTypes.FLAME, ParticleTypes.LARGE_SMOKE);
    }
}
