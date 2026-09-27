package com.krolasyon.bosses.morph.abilities;

import com.krolasyon.bosses.entity.FormProjectileEntity;
import com.krolasyon.bosses.entity.TideGeyserEntity;
import com.krolasyon.bosses.entity.ZoneEntity;
import com.krolasyon.bosses.morph.FormAbilities;
import com.krolasyon.bosses.morph.Forms;
import com.krolasyon.bosses.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import static com.krolasyon.bosses.morph.MorphServer.*;

/** Güneş Alevi Hükümdarı: Solar Cleave, Flame Cyclone, Solar Seal, Phoenix Dash, Solar Meteor. */
public class SolarAbilities implements FormAbilities {
    @Override
    public void tick(ServerPlayer p, State s, ServerLevel sl, int id, int t) {
        Vec3 dir = horizontalLook(p);
        switch (id) {
            case 0 -> {   // Solar Cleave: overhead greatsword slam, a line of fire pillars erupts forward
                if (t == 2) sound(p, ModSounds.HEAVY_SWING.get(), 1.2F, 0.8F);
                if (t == 11) {
                    sound(p, ModSounds.CLEAVE.get(), 1.6F, 1.0F);
                    Vec3 c = p.position();
                    Vec3 front = c.add(dir.scale(2.5));
                    sl.sendParticles(ParticleTypes.EXPLOSION, front.x, front.y + 0.3, front.z, 1, 0, 0, 0, 0);
                    ring(sl, front.add(0, 0.1, 0), 2.0, 30, ParticleTypes.FLAME, 0.1);
                    for (LivingEntity e : cone(p, dir, 4.5, 0.4)) burn(p, e, 10F, 0.8, 0.6, 6);
                    for (int i = 1; i <= 9; i++) {
                        Vec3 q = c.add(dir.scale(2 + i * 1.6));
                        TideGeyserEntity.spawn(p, TideGeyserEntity.FIRE, q.x, q.z, c.y, i * 2, 9F, i % 3 == 1);
                    }
                }
            }
            case 1 -> {   // Flame Cyclone: spin with the greatsword inside a ring of fire
                if (t == 4) sound(p, ModSounds.CYCLONE.get(), 1.4F, 1.0F);
                if (t >= 6 && t <= 29) {
                    Vec3 c = p.position();
                    double a = t * 0.9;
                    for (int k = 0; k < 3; k++) {
                        double b = a + k * Math.PI * 2 / 3;
                        sl.sendParticles(ParticleTypes.FLAME, c.x + Math.cos(b) * 3.2, c.y + 1.0 + k * 0.3, c.z + Math.sin(b) * 3.2, 3, 0.1, 0.1, 0.1, 0.02);
                    }
                    sl.sendParticles(ParticleTypes.SMALL_FLAME, c.x, c.y + 1, c.z, 6, 2.5, 0.5, 2.5, 0.02);
                    if (t % 4 == 2) {
                        for (LivingEntity e : targets(p, new AABB(c, c).inflate(4.2, 2, 4.2))) {
                            Vec3 in = c.subtract(e.position()).normalize().scale(0.15);
                            if (burn(p, e, 5F, 0.3, 0.1, 4)) e.setDeltaMovement(e.getDeltaMovement().add(in.x, 0.05, in.z));
                        }
                    }
                }
            }
            case 2 -> {   // Solar Seal: plunge the sword, a sun rune ignites and detonates
                if (t == 11) {
                    sound(p, ModSounds.SEAL_CHARGE.get(), 1.4F, 1.0F);
                    ZoneEntity.spawn(p, p.position().add(0, 0.05, 0), ZoneEntity.SUN_SEAL, 7F);
                    sl.sendParticles(ParticleTypes.LAVA, p.getX(), p.getY() + 0.2, p.getZ(), 12, 0.6, 0.1, 0.6, 0);
                }
            }
            case 3 -> {   // Phoenix Dash: burning dash through enemies leaving fire in the trail
                if (t == 2) sound(p, ModSounds.PHOENIX_DASH.get(), 1.3F, 1.0F);
                if (t == 6) {
                    p.setDeltaMovement(dir.x * 2.4, 0.15, dir.z * 2.4);
                    p.hurtMarked = true;
                }
                if (t >= 6 && t <= 16) {
                    Vec3 c = p.position().add(0, 1, 0);
                    sl.sendParticles(ParticleTypes.FLAME, c.x, c.y, c.z, 14, 0.5, 0.7, 0.5, 0.05);
                    sl.sendParticles(dust(Forms.COLOR2[Forms.SOLAR], 1.0F), c.x, c.y, c.z, 4, 0.4, 0.6, 0.4, 0);
                    if (t % 2 == 0) TideGeyserEntity.spawn(p, TideGeyserEntity.FIRE, p.getX(), p.getZ(), p.getY(), 4, 5F, false);
                    for (LivingEntity e : targets(p, p.getBoundingBox().inflate(1.6, 0.8, 1.6)))
                        if (s.hit.add(e.getId())) burn(p, e, 9F, 0.8, 0.5, 6);
                }
            }
            case 4 -> {   // Solar Meteor: raise the sword to the sky and call down a meteor
                if (t == 4) sound(p, ModSounds.METEOR_CALL.get(), 1.6F, 1.0F);
                if (t < 14) {
                    Vec3 tip = p.position().add(0, 4.2, 0);
                    sl.sendParticles(ParticleTypes.FLAME, tip.x, tip.y, tip.z, 6, 0.3, 0.3, 0.3, 0.03);
                    sl.sendParticles(ParticleTypes.END_ROD, tip.x, tip.y, tip.z, 2, 0.2, 0.2, 0.2, 0.05);
                }
                if (t == 14) {
                    Vec3 at = aimPoint(p, 24.0);
                    Vec3 from = at.add(-dir.x * 8, 22, -dir.z * 8);
                    Vec3 vel = at.subtract(from).normalize().scale(1.4);
                    FormProjectileEntity.shoot(p, FormProjectileEntity.METEOR, from, vel, 18F);
                }
            }
            default -> {}
        }
    }

    @Override
    public void transformTick(ServerPlayer p, ServerLevel sl, int t) {
        FireFx.cocoon(p, sl, t, Forms.COLOR[Forms.SOLAR], ParticleTypes.FLAME, ParticleTypes.END_ROD);
    }

    @Override
    public void transformBurst(ServerPlayer p, ServerLevel sl) {
        FireFx.burst(p, sl, Forms.COLOR2[Forms.SOLAR], ParticleTypes.FLAME, ParticleTypes.END_ROD);
    }
}
