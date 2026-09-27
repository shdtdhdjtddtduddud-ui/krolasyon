package com.krolasyon.bosses.morph.abilities;

import com.krolasyon.bosses.entity.TideGeyserEntity;
import com.krolasyon.bosses.entity.TideVortexEntity;
import com.krolasyon.bosses.entity.TsunamiWaveEntity;
import com.krolasyon.bosses.morph.Aigoar;
import com.krolasyon.bosses.morph.FormAbilities;
import com.krolasyon.bosses.morph.Forms;
import com.krolasyon.bosses.morph.MorphServer;
import com.krolasyon.bosses.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

import static com.krolasyon.bosses.morph.MorphServer.*;

/** Aigoar, the abyssal tide lord: water abilities. */
public class AigoarAbilities implements FormAbilities {
    @Override
    public void tick(ServerPlayer p, State s, ServerLevel sl, int id, int t) {
        switch (id) {
            case Aigoar.REND -> rend(p, s, sl, t);
            case Aigoar.MAELSTROM -> maelstrom(p, sl, t);
            case Aigoar.GEYSER -> geyser(p, sl, t);
            case Aigoar.BEAM -> beam(p, s, sl, t);
            case Aigoar.TSUNAMI -> tsunami(p, s, sl, t);
            default -> {}
        }
    }

    @Override
    public void passive(ServerPlayer p, State s, ServerLevel sl) {
        p.setAirSupply(p.getMaxAirSupply());
        if (p.isInWaterRainOrBubble() && p.tickCount % 30 == 0 && p.getHealth() < p.getMaxHealth()) p.heal(1.0F);
        if (p.isInWater() && p.tickCount % 20 == 0) {
            p.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 40, 0, true, false));
            p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, true, false));
        }
        if (p.isOnFire() && p.tickCount % 10 == 0) p.clearFire();
    }

    @Override
    public void transformBurst(ServerPlayer p, ServerLevel sl) {
        transformFx(p, sl, Aigoar.TRANSFORM_BURST);
    }

    @Override
    public void transformTick(ServerPlayer p, ServerLevel sl, int t) {
        if (t != Aigoar.TRANSFORM_BURST) transformFx(p, sl, t);
    }

    private static void transformFx(ServerPlayer p, ServerLevel sl, int t) {
        Vec3 c = p.position();
        if (t < Aigoar.TRANSFORM_BURST) {
            // rising spiral cocoon of water
            float k = t / (float) Aigoar.TRANSFORM_BURST;
            for (int i = 0; i < 3; i++) {
                double a = t * 0.55 + i * Math.PI * 2 / 3;
                double r = 1.6 - k * 0.9;
                double y = (t % 10) * 0.28;
                sl.sendParticles(dust(i == 0 ? 0x7FFFF6 : 0x2BC8D6, 0.9F), c.x + Math.cos(a) * r, c.y + y, c.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
                sl.sendParticles(ParticleTypes.SPLASH, c.x + Math.cos(a + 1) * r, c.y + y * 0.5, c.z + Math.sin(a + 1) * r, 2, 0, 0.1, 0, 0.1);
            }
            sl.sendParticles(ParticleTypes.FALLING_WATER, c.x, c.y + 3.2, c.z, 1, 0.6, 0.2, 0.6, 0);
            if (t % 3 == 0) sl.sendParticles(ParticleTypes.GLOW, c.x, c.y + 1.2, c.z, 2, 0.5, 0.8, 0.5, 0.02);
        } else if (t == Aigoar.TRANSFORM_BURST) {
            sl.sendParticles(ParticleTypes.SPLASH, c.x, c.y + 1.5, c.z, 220, 1.4, 1.4, 1.4, 0.4);
            sl.sendParticles(ParticleTypes.BUBBLE_POP, c.x, c.y + 1.5, c.z, 80, 1.2, 1.2, 1.2, 0.1);
            sl.sendParticles(ParticleTypes.GLOW, c.x, c.y + 1.6, c.z, 40, 1.0, 1.2, 1.0, 0.2);
            sl.sendParticles(ParticleTypes.NAUTILUS, c.x, c.y + 1.6, c.z, 60, 0.3, 0.4, 0.3, 1.2);
            ring(sl, c.add(0, 0.1, 0), 2.5, 40, dust(0x7FFFF6, 1.0F), 0);
            ring(sl, c.add(0, 0.1, 0), 4.0, 50, ParticleTypes.SPLASH, 0.1);
            for (LivingEntity e : targets(p, new AABB(c, c).inflate(5, 3, 5))) {
                hit(p, e, 5.0F, 1.4, 0.5);
            }
        } else if (t < Aigoar.TRANSFORM_BURST + 20 && t % 2 == 0) {
            sl.sendParticles(ParticleTypes.SPLASH, c.x, c.y + 0.1, c.z, 3, 0.8, 0.0, 0.8, 0.05);
        }
    }

    // 1) Tidal Rend — water-propelled dash through enemies, then a cross slash and a rising slash
    private static void rend(ServerPlayer p, State s, ServerLevel sl, int t) {
        Vec3 dir = horizontalLook(p);
        if (t == 0) sound(p, ModSounds.TIDAL_REND.get(), 1.2F, 1.0F);
        if (t == 4) {
            p.setDeltaMovement(dir.x * 2.1, 0.15, dir.z * 2.1);
            p.hurtMarked = true;
        }
        if (t >= 4 && t <= 12) {
            Vec3 c = p.position().add(0, 1.0, 0);
            sl.sendParticles(dust(0x3FE0E6, 0.8F), c.x, c.y, c.z, 3, 0.4, 0.6, 0.4, 0);
            sl.sendParticles(ParticleTypes.SPLASH, c.x, c.y - 0.8, c.z, 10, 0.4, 0.1, 0.4, 0.1);
            sl.sendParticles(ParticleTypes.BUBBLE_POP, c.x, c.y, c.z, 6, 0.4, 0.5, 0.4, 0.05);
            for (LivingEntity e : targets(p, p.getBoundingBox().inflate(1.6, 0.8, 1.6))) {
                if (s.hit.add(e.getId())) {
                    if (hit(p, e, 8.0F, 0.6, 0.35)) {
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 2));
                        sl.sendParticles(ParticleTypes.SWEEP_ATTACK, e.getX(), e.getY(0.5), e.getZ(), 1, 0, 0, 0, 0);
                    }
                }
            }
        }
        if (t == 11 || t == 15) {
            boolean rising = t == 15;
            sound(p, ModSounds.CLAW_SWIPE.get(), 1.0F, rising ? 0.8F : 1.1F);
            Vec3 c = p.getEyePosition().add(dir.scale(1.6));
            arc(sl, p, dir, rising);
            for (LivingEntity e : cone(p, dir, 4.8, 0.45)) {
                if (hit(p, e, rising ? 6.0F : 7.0F, rising ? 0.3 : 1.0, rising ? 0.85 : 0.25))
                    sl.sendParticles(ParticleTypes.SPLASH, e.getX(), e.getY(0.6), e.getZ(), 20, 0.3, 0.3, 0.3, 0.2);
            }
            sl.sendParticles(ParticleTypes.SWEEP_ATTACK, c.x, c.y - 0.4, c.z, 3, 0.8, 0.2, 0.8, 0);
        }
    }

    // 2) Maelstrom — hurls a vortex that drags enemies in and bursts
    private static void maelstrom(ServerPlayer p, ServerLevel sl, int t) {
        if (t == 0) sound(p, ModSounds.MAELSTROM.get(), 1.2F, 1.0F);
        if (t < 14) {
            // water gathering above the head
            Vec3 c = p.position().add(0, 3.3, 0);
            double a = t * 0.7;
            for (int i = 0; i < 4; i++) {
                double b = a + i * Math.PI / 2;
                sl.sendParticles(dust(0x3FE0E6, 0.9F), c.x + Math.cos(b) * 1.2, c.y, c.z + Math.sin(b) * 1.2, 1, 0, 0, 0, 0);
            }
            sl.sendParticles(ParticleTypes.BUBBLE_POP, c.x, c.y, c.z, 3, 0.5, 0.2, 0.5, 0.02);
        }
        if (t == 14) {
            Vec3 target = aimPoint(p, 22.0);
            Double gy = TideGeyserEntity.findGround(sl, target.x, target.z, target.y + 2, target.y - 8);
            Vec3 at = new Vec3(target.x, gy != null ? gy : target.y, target.z);
            TideVortexEntity.spawn(p, at);
            Vec3 from = p.getEyePosition();
            Vec3 d = at.add(0, 1, 0).subtract(from);
            int n = (int) (d.length() * 2);
            for (int i = 0; i < n; i++) {
                Vec3 q = from.add(d.scale(i / (double) n));
                sl.sendParticles(dust(0x7FFFF6, 1.0F), q.x, q.y, q.z, 1, 0.05, 0.05, 0.05, 0);
            }
        }
    }

    // 3) Abyssal Geysers — slam the ground, a line of geysers erupts forward plus a ring around
    private static void geyser(ServerPlayer p, ServerLevel sl, int t) {
        Vec3 dir = horizontalLook(p);
        if (t == 0) {
            p.setDeltaMovement(p.getDeltaMovement().x * 0.3, 0.5, p.getDeltaMovement().z * 0.3);
            p.hurtMarked = true;
        }
        if (t == 10) {
            sound(p, ModSounds.GEYSER_SLAM.get(), 1.4F, 1.0F);
            Vec3 c = p.position();
            ring(sl, c.add(0, 0.1, 0), 2.2, 36, ParticleTypes.SPLASH, 0.2);
            ring(sl, c.add(0, 0.1, 0), 3.0, 36, dust(0x2BC8D6, 1.0F), 0);
            sl.sendParticles(ParticleTypes.POOF, c.x, c.y + 0.2, c.z, 14, 1.2, 0.1, 1.2, 0.05);
            for (LivingEntity e : targets(p, new AABB(c, c).inflate(3.2, 1.5, 3.2))) hit(p, e, 5.0F, 0.8, 0.4);
            for (int i = 1; i <= 10; i++) {
                Vec3 q = c.add(dir.scale(1.2 + i * 1.7));
                TideGeyserEntity.spawn(p, q.x, q.z, c.y, 2 + i * 2, 9.0F, i % 2 == 1);
            }
            for (int i = 0; i < 7; i++) {
                double a = i * Math.PI * 2 / 7;
                TideGeyserEntity.spawn(p, c.x + Math.cos(a) * 3.4, c.z + Math.sin(a) * 3.4, c.y, 5, 7.0F, i == 0);
            }
        }
    }

    // 4) Pressure Beam — gather a water orb, then a piercing high pressure torrent
    private static void beam(ServerPlayer p, State s, ServerLevel sl, int t) {
        if (t == 0) {
            sound(p, ModSounds.BEAM_CHARGE.get(), 1.2F, 1.0F);
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, Forms.duration(Forms.AIGOAR, Aigoar.BEAM), 2, false, false));
        }
        Vec3 origin = beamOrigin(p);
        if (t < Aigoar.BEAM_START) {
            for (int i = 0; i < 3; i++) {
                Vec3 off = new Vec3(p.getRandom().nextGaussian(), p.getRandom().nextGaussian(), p.getRandom().nextGaussian()).normalize().scale(1.8);
                // nautilus particles fly from pos + velocity towards pos
                sl.sendParticles(ParticleTypes.NAUTILUS, origin.x, origin.y, origin.z, 0, off.x, off.y, off.z, 1.0);
            }
            sl.sendParticles(dust(0x7FFFF6, 1.0F), origin.x, origin.y, origin.z, 3, 0.15, 0.15, 0.15, 0);
            return;
        }
        if (t == Aigoar.BEAM_START) sound(p, ModSounds.BEAM_FIRE.get(), 1.4F, 1.0F);
        if (t >= Aigoar.BEAM_END) return;
        Vec3 look = p.getLookAngle();
        Vec3 end = origin.add(look.scale(Aigoar.BEAM_RANGE));
        BlockHitResult bh = sl.clip(new ClipContext(origin, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (bh.getType() != HitResult.Type.MISS) end = bh.getLocation();
        // impact
        sl.sendParticles(ParticleTypes.SPLASH, end.x, end.y, end.z, 12, 0.3, 0.3, 0.3, 0.3);
        sl.sendParticles(ParticleTypes.BUBBLE_POP, end.x, end.y, end.z, 4, 0.3, 0.3, 0.3, 0.05);
        if (t % 3 == 0) sl.sendParticles(ParticleTypes.CLOUD, end.x, end.y, end.z, 2, 0.2, 0.2, 0.2, 0.03);
        if (bh.getType() == HitResult.Type.BLOCK) {
            BlockPos fire = bh.getBlockPos().relative(bh.getDirection());
            if (sl.getBlockState(fire).is(Blocks.FIRE)) sl.removeBlock(fire, false);
        }
        if (t % 4 == 0) {
            Vec3 d = end.subtract(origin);
            AABB box = new AABB(origin, end).inflate(1.0);
            for (LivingEntity e : targets(p, box)) {
                Optional<Vec3> clip = e.getBoundingBox().inflate(0.7).clip(origin, end);
                if (clip.isEmpty() && !e.getBoundingBox().inflate(0.7).contains(origin)) continue;
                if (e.hurt(p.damageSources().playerAttack(p), 4.5F)) {
                    Vec3 push = d.normalize().scale(0.45);
                    e.setDeltaMovement(e.getDeltaMovement().add(push.x, 0.08, push.z));
                    e.hurtMarked = true;
                    e.clearFire();
                    sl.sendParticles(ParticleTypes.SPLASH, e.getX(), e.getY(0.5), e.getZ(), 16, 0.3, 0.4, 0.3, 0.2);
                }
            }
        }
    }

    // 5) Tsunami Crash — leap into a flip, crash down and release a giant expanding tidal wave
    private static void tsunami(ServerPlayer p, State s, ServerLevel sl, int t) {
        Vec3 dir = horizontalLook(p);
        if (t == 0) {
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 50, 1, false, false));
        }
        if (t == 6) {
            p.setDeltaMovement(dir.x * 0.55, 1.25, dir.z * 0.55);
            p.hurtMarked = true;
            sound(p, ModSounds.TSUNAMI_LEAP.get(), 1.2F, 1.0F);
            ring(sl, p.position().add(0, 0.1, 0), 1.5, 30, ParticleTypes.SPLASH, 0.2);
            sl.sendParticles(ParticleTypes.POOF, p.getX(), p.getY() + 0.1, p.getZ(), 10, 0.6, 0.1, 0.6, 0.05);
        }
        if (t > 6 && !s.slammed) {
            Vec3 c = p.position().add(0, 1.2, 0);
            sl.sendParticles(dust(0x3FE0E6, 0.8F), c.x, c.y, c.z, 3, 0.5, 0.8, 0.5, 0);
            sl.sendParticles(ParticleTypes.SPLASH, c.x, c.y - 0.6, c.z, 4, 0.5, 0.3, 0.5, 0.1);
        }
        if (t == 17 && !p.onGround()) {
            p.setDeltaMovement(dir.x * 0.3, -2.4, dir.z * 0.3);
            p.hurtMarked = true;
        }
        if (!s.slammed && t >= 10 && (p.onGround() || p.isInWater() || t >= 32)) {
            s.slammed = true;
            p.fallDistance = 0;
            Vec3 c = p.position();
            sound(p, ModSounds.TSUNAMI_CRASH.get(), 2.0F, 1.0F);
            TsunamiWaveEntity.spawn(p, c);
            ring(sl, c.add(0, 0.2, 0), 2.5, 30, ParticleTypes.POOF, 0.15);
            sl.sendParticles(ParticleTypes.SPLASH, c.x, c.y + 0.5, c.z, 200, 2.0, 0.6, 2.0, 0.5);
            sl.sendParticles(ParticleTypes.NAUTILUS, c.x, c.y + 1.0, c.z, 60, 0.4, 0.4, 0.4, 1.5);
            for (LivingEntity e : targets(p, new AABB(c, c).inflate(3.5, 2.0, 3.5))) hit(p, e, 10.0F, 1.0, 1.0);
        }
    }

    private static void arc(ServerLevel sl, Player p, Vec3 dir, boolean rising) {
        Vec3 c = p.position().add(0, rising ? 0.6 : 1.4, 0);
        Vec3 right = new Vec3(-dir.z, 0, dir.x);
        for (int i = 0; i <= 14; i++) {
            double a = -1.2 + i * (2.4 / 14);
            Vec3 q = c.add(dir.scale(Math.cos(a) * 2.6)).add(right.scale(Math.sin(a) * 2.6));
            if (rising) q = q.add(0, i * 0.18, 0);
            sl.sendParticles(dust(i % 2 == 0 ? 0x7FFFF6 : 0x2BC8D6, 0.9F), q.x, q.y, q.z, 1, 0, 0, 0, 0);
            if (i % 3 == 0) sl.sendParticles(ParticleTypes.SPLASH, q.x, q.y, q.z, 3, 0.1, 0.1, 0.1, 0.1);
        }
    }

}
