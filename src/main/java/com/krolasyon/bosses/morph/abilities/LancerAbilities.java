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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import static com.krolasyon.bosses.morph.MorphServer.*;

/** Cehennem Mızrakçısı: Hell Lance, Impaling Charge, Tail Whirl, Lava Pillars, Hell Gate. */
public class LancerAbilities implements FormAbilities {
    @Override
    public void tick(ServerPlayer p, State s, ServerLevel sl, int id, int t) {
        Vec3 dir = horizontalLook(p);
        switch (id) {
            case 0 -> {   // Hell Lance: hurl the burning trident, it explodes into a ring of fire
                if (t == 5) sound(p, ModSounds.SPEAR_THROW.get(), 1.3F, 1.0F);
                if (t == 11) {
                    Vec3 look = p.getLookAngle();
                    FormProjectileEntity.shoot(p, FormProjectileEntity.SPEAR, p.getEyePosition().add(look.scale(1.2)), look.scale(2.4), 12F);
                }
            }
            case 1 -> {   // Impaling Charge: spear-first rush that skewers and drags enemies, then flings them
                if (t == 2) sound(p, ModSounds.CHARGE.get(), 1.4F, 1.0F);
                if (t >= 6 && t <= 18) {
                    p.setDeltaMovement(dir.x * 1.3, Math.min(p.getDeltaMovement().y, 0.1), dir.z * 1.3);
                    p.hurtMarked = true;
                    Vec3 tip = p.position().add(dir.scale(2.2)).add(0, 1.1, 0);
                    sl.sendParticles(ParticleTypes.FLAME, tip.x, tip.y, tip.z, 6, 0.3, 0.3, 0.3, 0.03);
                    sl.sendParticles(ParticleTypes.SMOKE, p.getX(), p.getY() + 0.2, p.getZ(), 3, 0.3, 0.1, 0.3, 0.01);
                    for (LivingEntity e : targets(p, new AABB(tip, tip).inflate(1.4))) {
                        if (s.hit.add(e.getId()) && burn(p, e, 8F, 0, 0, 4)) s.carried.add(e.getId());
                    }
                    for (int eid : s.carried) {
                        Entity e = sl.getEntity(eid);
                        if (e instanceof LivingEntity le && le.isAlive()) {
                            le.setDeltaMovement(p.getDeltaMovement().x, 0.05, p.getDeltaMovement().z);
                            le.teleportTo(tip.x, tip.y - le.getBbHeight() * 0.5, tip.z);
                            le.hurtMarked = true;
                        }
                    }
                }
                if (t == 19) {
                    for (int eid : s.carried) {
                        Entity e = sl.getEntity(eid);
                        if (e instanceof LivingEntity le && le.isAlive() && hit(p, le, 6F, 1.6, 0.8)) le.setSecondsOnFire(5);
                    }
                    s.carried.clear();
                    p.setDeltaMovement(p.getDeltaMovement().scale(0.2));
                    p.hurtMarked = true;
                }
            }
            case 2 -> {   // Tail Whirl: both arrow-tipped tails spin like blades, knocking everything into the air
                if (t == 3) sound(p, ModSounds.TAIL_LASH.get(), 1.4F, 1.2F);
                if (t >= 5 && t <= 23) {
                    Vec3 c = p.position();
                    double a = t * 0.75;
                    for (int k = 0; k < 2; k++) {
                        double b = a + k * Math.PI;
                        sl.sendParticles(ParticleTypes.FLAME, c.x + Math.cos(b) * 3.2, c.y + 0.7, c.z + Math.sin(b) * 3.2, 3, 0.1, 0.1, 0.1, 0.02);
                        sl.sendParticles(dust(0xFFC040, 1.0F), c.x + Math.cos(b) * 3.6, c.y + 0.7, c.z + Math.sin(b) * 3.6, 1, 0, 0, 0, 0);
                    }
                    for (LivingEntity e : targets(p, new AABB(c, c).inflate(4.5, 2, 4.5))) {
                        if (s.hit.add(e.getId()) && burn(p, e, 7F, 0.8, 1.0, 3))
                            e.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON, 60, 1));
                    }
                }
            }
            case 3 -> {   // Lava Pillars: stab the trident into the ground, rings of lava spikes surge outward
                if (t == 11) {
                    sound(p, ModSounds.LAVA_ERUPT.get(), 1.8F, 1.0F);
                    Vec3 c = p.position();
                    sl.sendParticles(ParticleTypes.LAVA, c.x, c.y + 0.2, c.z, 20, 0.8, 0.1, 0.8, 0);
                    for (int ring = 0; ring < 3; ring++) {
                        double r = 2.8 + ring * 2.8;
                        int n = 8 + ring * 4;
                        for (int i = 0; i < n; i++) {
                            double a = i * Math.PI * 2 / n + ring * 0.3;
                            TideGeyserEntity.spawn(p, ring == 1 ? TideGeyserEntity.BLOOD : TideGeyserEntity.FIRE,
                                    c.x + Math.cos(a) * r, c.z + Math.sin(a) * r, c.y, ring * 6, 9F, i == 0);
                        }
                    }
                }
            }
            case 4 -> {   // Hell Gate: rise into the air, open a burning gate that rains spears on the target
                if (t == 2) sound(p, ModSounds.HELL_GATE.get(), 1.8F, 1.0F);
                if (t == 10) {
                    Vec3 at = aimPoint(p, 26.0);
                    ZoneEntity.spawn(p, at.add(0, 0.05, 0), ZoneEntity.HELL_GATE, 5.5F);
                }
                if (t >= 10 && t < 58) {
                    Vec3 v = p.getDeltaMovement();
                    p.setDeltaMovement(v.x * 0.7, t < 20 ? 0.3 : 0.0, v.z * 0.7);
                    p.hurtMarked = true;
                    p.fallDistance = 0;
                    sl.sendParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 0.1, p.getZ(), 4, 0.4, 0.1, 0.4, 0.02);
                    sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.getX(), p.getY() + 1.2, p.getZ(), 1, 0.6, 0.6, 0.6, 0.01);
                }
            }
            default -> {}
        }
    }

    @Override
    public void transformTick(ServerPlayer p, ServerLevel sl, int t) {
        FireFx.cocoon(p, sl, t, Forms.COLOR[Forms.LANCER], ParticleTypes.FLAME, ParticleTypes.LAVA);
    }

    @Override
    public void transformBurst(ServerPlayer p, ServerLevel sl) {
        FireFx.burst(p, sl, Forms.COLOR2[Forms.LANCER], ParticleTypes.FLAME, ParticleTypes.SOUL_FIRE_FLAME);
    }
}
