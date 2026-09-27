package com.krolasyon.bosses.morph.abilities;

import com.krolasyon.bosses.entity.FormProjectileEntity;
import com.krolasyon.bosses.entity.TsunamiWaveEntity;
import com.krolasyon.bosses.entity.ZoneEntity;
import com.krolasyon.bosses.morph.FormAbilities;
import com.krolasyon.bosses.morph.Forms;
import com.krolasyon.bosses.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import static com.krolasyon.bosses.morph.MorphServer.*;

/** Alev Ruhu: Flame Step, Ember Rain, Inferno Nova, Crescent Flames, Ashen Phoenix. */
public class FlameAbilities implements FormAbilities {
    @Override
    public void tick(ServerPlayer p, State s, ServerLevel sl, int id, int t) {
        Vec3 dir = horizontalLook(p);
        switch (id) {
            case 0 -> {   // Flame Step: burst into flame and reappear far ahead, exploding at both ends
                if (t == 2) sound(p, ModSounds.FLAME_BLINK.get(), 1.3F, 1.0F);
                if (t == 5) {
                    Vec3 from = p.position();
                    Vec3 to = blinkTarget(p, p.getLookAngle().multiply(1, 0, 1).normalize(), 10.0);
                    flameBurst(p, sl, from);
                    p.teleportTo(to.x, to.y, to.z);
                    p.fallDistance = 0;
                    s.anchor = to;
                }
                if (t == 9) flameBurst(p, sl, p.position());
            }
            case 1 -> {   // Ember Rain: raise the blade, the sky rains burning embers on the target area
                if (t == 6) sound(p, ModSounds.EMBER_RAIN.get(), 1.4F, 1.0F);
                if (t == 12) {
                    Vec3 at = aimPoint(p, 22.0);
                    ZoneEntity.spawn(p, at.add(0, 0.05, 0), ZoneEntity.EMBER_RAIN, 5.5F);
                    Vec3 tip = p.position().add(0, 4, 0);
                    sl.sendParticles(ParticleTypes.FLAME, tip.x, tip.y, tip.z, 30, 0.2, 1.5, 0.2, 0.1);
                }
            }
            case 2 -> {   // Inferno Nova: gather the fire inward, then explode in a flaming ring
                if (t < 14) {
                    Vec3 c = p.position().add(0, 1.2, 0);
                    for (int i = 0; i < 4; i++) {
                        Vec3 off = new Vec3(p.getRandom().nextGaussian(), p.getRandom().nextGaussian() * 0.5, p.getRandom().nextGaussian()).normalize().scale(3);
                        sl.sendParticles(ParticleTypes.FLAME, c.x + off.x, c.y + off.y, c.z + off.z, 0, -off.x, -off.y, -off.z, 0.12);
                    }
                }
                if (t == 12) sound(p, ModSounds.NOVA.get(), 1.8F, 1.0F);
                if (t == 14) {
                    Vec3 c = p.position();
                    sl.sendParticles(ParticleTypes.LAVA, c.x, c.y + 1, c.z, 20, 0.6, 0.6, 0.6, 0);
                    sl.sendParticles(ParticleTypes.FLAME, c.x, c.y + 1, c.z, 120, 0.4, 0.4, 0.4, 0.45);
                    TsunamiWaveEntity.spawn(p, c, TsunamiWaveEntity.FIRE, 12F, 10F);
                    for (LivingEntity e : targets(p, new AABB(c, c).inflate(3.5, 2, 3.5))) burn(p, e, 10F, 1.4, 0.7, 8);
                }
            }
            case 3 -> {   // Crescent Flames: three blade swings each launching a fan of fire crescents
                if (t == 3 || t == 8 || t == 13) {
                    sound(p, ModSounds.CRESCENT.get(), 1.2F, 0.9F + t * 0.02F);
                    float yaw = p.getYRot();
                    for (int k = -1; k <= 1; k++) {
                        float a = (yaw + k * 14F) * Mth.DEG_TO_RAD;
                        Vec3 d = new Vec3(-Mth.sin(a), 0, Mth.cos(a));
                        FormProjectileEntity.shoot(p, FormProjectileEntity.CRESCENT, p.position().add(0, 1.1, 0).add(d.scale(1.2)), d.scale(1.25), 7F);
                    }
                    arc(sl, p, dir, 2.2, 1.2, ParticleTypes.FLAME, ParticleTypes.SMALL_FLAME);
                }
            }
            case 4 -> {   // Ashen Phoenix: rise wreathed in flame, scorching everything around, then crash down
                if (t == 4) sound(p, ModSounds.PHOENIX_RISE.get(), 1.6F, 1.0F);
                if (t >= 10 && t < 70) {
                    double vy = t < 22 ? 0.35 : 0.02 * Math.sin(t * 0.3);
                    Vec3 v = p.getDeltaMovement();
                    p.setDeltaMovement(v.x * 0.8 + dir.x * 0.05, vy, v.z * 0.8 + dir.z * 0.05);
                    p.hurtMarked = true;
                    p.fallDistance = 0;
                    Vec3 c = p.position().add(0, 1, 0);
                    for (int k = 0; k < 2; k++) {
                        double a = t * 0.4 + k * Math.PI;
                        sl.sendParticles(ParticleTypes.FLAME, c.x + Math.cos(a) * 1.4, c.y, c.z + Math.sin(a) * 1.4, 4, 0.2, 0.4, 0.2, 0.03);
                    }
                    sl.sendParticles(ParticleTypes.FLAME, c.x, c.y - 1.2, c.z, 8, 0.4, 0.2, 0.4, 0.02);
                    if (t % 10 == 0) {
                        for (LivingEntity e : targets(p, new AABB(c, c).inflate(5, 6, 5))) burn(p, e, 4F, 0.2, 0.1, 5);
                        for (LivingEntity e : targets(p, new AABB(c, c).inflate(14, 12, 14))) {
                            if (p.getRandom().nextInt(3) == 0) {
                                Vec3 from = c.add(0, 1, 0);
                                Vec3 vel = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(from).normalize().scale(1.1);
                                FormProjectileEntity.shoot(p, FormProjectileEntity.FIREBALL, from, vel, 5F).homing(e);
                            }
                        }
                    }
                }
                if (t == 70) {
                    p.setDeltaMovement(0, -2.4, 0);
                    p.hurtMarked = true;
                }
                if (!s.slammed && t > 70 && (p.onGround() || t >= 78)) {
                    s.slammed = true;
                    p.fallDistance = 0;
                    Vec3 c = p.position();
                    sound(p, ModSounds.FIRE_EXPLODE.get(), 1.8F, 1.1F);
                    TsunamiWaveEntity.spawn(p, c, TsunamiWaveEntity.FIRE, 9F, 8F);
                    for (LivingEntity e : targets(p, new AABB(c, c).inflate(3.5, 2, 3.5))) burn(p, e, 10F, 1.0, 0.8, 6);
                }
            }
            default -> {}
        }
    }

    private static void flameBurst(ServerPlayer p, ServerLevel sl, Vec3 at) {
        sl.sendParticles(ParticleTypes.FLAME, at.x, at.y + 1, at.z, 70, 0.5, 0.9, 0.5, 0.15);
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y + 1, at.z, 12, 0.4, 0.6, 0.4, 0.03);
        ring(sl, at.add(0, 0.1, 0), 2.2, 24, ParticleTypes.FLAME, 0.05);
        for (LivingEntity e : targets(p, new AABB(at, at).inflate(2.8, 2, 2.8))) burn(p, e, 8F, 1.0, 0.5, 6);
    }

    @Override
    public void passive(ServerPlayer p, State s, ServerLevel sl) {
        if (p.tickCount % 4 == 0) sl.sendParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 2.6, p.getZ(), 1, 0.2, 0.1, 0.2, 0.01);
    }

    @Override
    public void transformTick(ServerPlayer p, ServerLevel sl, int t) {
        FireFx.cocoon(p, sl, t, Forms.COLOR[Forms.FLAME], ParticleTypes.FLAME, ParticleTypes.SMALL_FLAME);
    }

    @Override
    public void transformBurst(ServerPlayer p, ServerLevel sl) {
        FireFx.burst(p, sl, Forms.COLOR2[Forms.FLAME], ParticleTypes.FLAME, ParticleTypes.LARGE_SMOKE);
    }
}
