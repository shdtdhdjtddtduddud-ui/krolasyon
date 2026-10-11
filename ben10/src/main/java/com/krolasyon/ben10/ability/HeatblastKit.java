package com.krolasyon.ben10.ability;

import com.krolasyon.ben10.OmniData;
import com.krolasyon.ben10.entity.AlienProjectile;
import com.krolasyon.ben10.entity.FxEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/** Heatblast: fireball, flamethrower, supernova, fire geysers. Guard = fire shield that burns attackers and projectiles. */
public class HeatblastKit extends Kit {
    @Override
    public boolean start(ServerPlayer p, OmniData d, int slot) {
        ServerLevel l = p.serverLevel();
        switch (slot) {
            case 0 -> Fx.sound(l, p.position(), SoundEvents.FIRECHARGE_USE, 0.6F, 1.4F);
            case 1 -> Fx.sound(l, p.position(), SoundEvents.BLAZE_SHOOT, 1F, 0.6F);
            case 2 -> {
                d.set(OmniData.F_CHARGING, true);
                Fx.sound(l, p.position(), SoundEvents.BEACON_ACTIVATE, 1.2F, 1.6F);
            }
            case 3 -> Fx.sound(l, p.position(), SoundEvents.BLAZE_AMBIENT, 1F, 0.6F);
            default -> {}
        }
        return true;
    }

    @Override
    public void tick(ServerPlayer p, OmniData d, int slot, int t) {
        ServerLevel l = p.serverLevel();
        switch (slot) {
            case 0 -> { // fireball
                Vec3 hand = Fx.rel(p, 0.6, 0.45, 0.45);
                if (t < 6) Fx.particles(l, ParticleTypes.FLAME, hand, 4, 0.08, 0.02);
                if (t == 6) {
                    Vec3 from = Fx.rel(p, 0.9, 0.3, 0.3);
                    LivingEntity aim = Fx.aimed(p, 40);
                    Vec3 dir = aim != null ? aim.position().add(0, aim.getBbHeight() / 2, 0).subtract(from).normalize() : p.getLookAngle();
                    AlienProjectile b = AlienProjectile.shoot(p, AlienProjectile.FIREBALL, from, dir.scale(1.7), 8F);
                    b.life = 60;
                    Fx.sound(l, from, SoundEvents.BLAZE_SHOOT, 1F, 1.1F);
                    Fx.particles(l, ParticleTypes.FLAME, from, 14, 0.15, 0.06);
                }
            }
            case 1 -> { // flamethrower
                if (t < 4) return;
                Vec3 look = p.getLookAngle();
                Vec3 from = Fx.rel(p, 0.9, 0, 0.35);
                for (int i = 0; i < 9; i++) {
                    Vec3 v = look.add(l.random.nextGaussian() * 0.11, l.random.nextGaussian() * 0.11, l.random.nextGaussian() * 0.11).normalize();
                    double sp = 0.55 + l.random.nextDouble() * 0.45;
                    l.sendParticles(i % 4 == 0 ? ParticleTypes.SMALL_FLAME : ParticleTypes.FLAME, from.x, from.y, from.z, 0, v.x, v.y, v.z, sp);
                }
                if (t % 3 == 0) l.sendParticles(ParticleTypes.LAVA, from.x, from.y, from.z, 1, 0.1, 0.1, 0.1, 0);
                if (t % 2 == 0) {
                    for (LivingEntity e : Fx.cone(p, 8, 24)) {
                        Fx.hitFire(p, e, 2.5F, 4);
                        Fx.push(e, e.getDeltaMovement().add(look.scale(0.12)));
                    }
                }
                if (t % 4 == 0) {
                    Vec3 end = Fx.rayEnd(p, 8);
                    Vec3 dir = end.subtract(from);
                    for (int i = 1; i <= 8; i++) {
                        Vec3 at = from.add(dir.scale(i / 8.0));
                        BlockPos bp = BlockPos.containing(at);
                        Fx.heat(l, bp, false);
                        Fx.heat(l, bp.below(), false);
                    }
                }
                if (t % 6 == 0) Fx.sound(l, p.position(), SoundEvents.FIRE_AMBIENT, 1.4F, 0.6F);
                if (t % 5 == 0) Fx.sound(l, p.position(), SoundEvents.BLAZE_BURN, 0.8F, 0.7F);
            }
            case 2 -> { // supernova
                Vec3 c = p.position().add(0, 1, 0);
                if (t <= 14) {
                    for (int i = 0; i < 10; i++) {
                        double a = l.random.nextDouble() * Math.PI * 2, b = l.random.nextDouble() * Math.PI - Math.PI / 2;
                        Vec3 off = new Vec3(Math.cos(a) * Math.cos(b), Math.sin(b), Math.sin(a) * Math.cos(b)).scale(3.5);
                        Vec3 at = c.add(off);
                        l.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 0, -off.x, -off.y, -off.z, 0.12);
                    }
                    if (t % 4 == 0) Fx.sound(l, c, SoundEvents.FIRE_AMBIENT, 1.5F, 0.5F + t * 0.06F);
                    if (t == 6) FxEntity.spawn(l, c, FxEntity.SPHERE, 10, -1.5F, 0xFFC040, p).follow(p);
                }
                if (t == 15) {
                    d.set(OmniData.F_CHARGING, false);
                    FxEntity.spawn(l, c, FxEntity.SPHERE, 16, 7.5F, 0xFFA020, p);
                    FxEntity.spawn(l, p.position().add(0, 0.1, 0), FxEntity.RING, 16, 9F, 0xFF8020, p);
                    Fx.sound(l, c, SoundEvents.GENERIC_EXPLODE, 2F, 0.7F);
                    Fx.sound(l, c, SoundEvents.FIRECHARGE_USE, 2F, 0.5F);
                    l.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y, c.z, 1, 0, 0, 0, 0);
                    for (int i = 0; i < 160; i++) {
                        Vec3 v = new Vec3(l.random.nextGaussian(), l.random.nextGaussian() * 0.6, l.random.nextGaussian()).normalize();
                        l.sendParticles(i % 3 == 0 ? ParticleTypes.LAVA : ParticleTypes.FLAME, c.x, c.y, c.z, 0, v.x, v.y, v.z, 0.6 + l.random.nextDouble() * 0.5);
                    }
                    for (LivingEntity e : Fx.around(p, c, 7.5)) {
                        float f = (float) (1 - e.distanceTo(p) / 9.0);
                        Fx.hitFire(p, e, 16 * Math.max(0.35F, f), 8);
                        Fx.knock(e, c, 1.7 * Math.max(0.4, f), 0.7);
                    }
                    for (BlockPos bp : Fx.disk(p.position(), 5.5, -2, 3)) Fx.heat(l, bp, true);
                    com.krolasyon.ben10.net.Net.shake(p, 30, 1.0F);
                }
                if (t > 15 && t % 3 == 0) Fx.ring(l, ParticleTypes.FLAME, p.position().add(0, 0.2, 0), 1.5 + (t - 15) * 0.5, 24, 0.05);
            }
            case 3 -> { // fire geysers in a line
                int first = 9;
                if (t >= first && (t - first) % 2 == 0 && (t - first) / 2 < 7) {
                    int i = (t - first) / 2;
                    Vec3 dir = Fx.flatLook(p);
                    Vec3 at = p.position().add(dir.scale(2.2 + i * 2.0));
                    double gy = Fx.groundY(l, at.x, at.y + 2, at.z);
                    Vec3 g = new Vec3(at.x, gy, at.z);
                    FxEntity.spawn(l, g, FxEntity.PILLAR, 22, 1.0F + i * 0.06F, 0xFFA020, p);
                    FxEntity.spawn(l, g.add(0, 0.05, 0), FxEntity.RING, 10, 2.2F, 0xFF7010, p);
                    Fx.sound(l, g, SoundEvents.FIRECHARGE_USE, 1.2F, 0.6F + i * 0.05F);
                    l.sendParticles(ParticleTypes.LAVA, g.x, g.y + 0.5, g.z, 10, 0.4, 0.3, 0.4, 0.2);
                    for (LivingEntity e : Fx.around(p, g.add(0, 1, 0), 1.9)) {
                        Fx.hitFire(p, e, 9F, 6);
                        Fx.push(e, new Vec3(e.getDeltaMovement().x * 0.3, 1.15, e.getDeltaMovement().z * 0.3));
                    }
                    BlockPos c = BlockPos.containing(g);
                    for (BlockPos bp : BlockPos.betweenClosed(c.offset(-1, -1, -1), c.offset(1, 1, 1))) Fx.heat(l, bp.immutable(), i % 2 == 0);
                    if (i == 0) com.krolasyon.ben10.net.Net.shake(p, 16, 0.5F);
                }
            }
            default -> {}
        }
    }

    @Override
    public void end(ServerPlayer p, OmniData d, int slot) {
        d.set(OmniData.F_CHARGING, false);
    }

    @Override
    public void passive(ServerPlayer p, OmniData d) {
        ServerLevel l = p.serverLevel();
        if (p.tickCount % 3 == 0) {
            l.sendParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 1.85, p.getZ(), 1, 0.12, 0.08, 0.12, 0.01);
        }
        if (p.getAbilities().flying) {
            l.sendParticles(ParticleTypes.FLAME, p.getX(), p.getY() - 0.1, p.getZ(), 3, 0.15, 0.05, 0.15, 0.03);
            if (p.tickCount % 2 == 0) l.sendParticles(ParticleTypes.SMALL_FLAME, p.getX(), p.getY() + 0.1, p.getZ(), 2, 0.1, 0, 0.1, 0.02);
        }
        if (p.tickCount % 10 == 0) {
            BlockPos feet = p.blockPosition();
            Fx.heat(l, feet, false);
            if (l.getBlockState(feet.below()).is(net.minecraft.world.level.block.Blocks.ICE)) Fx.heat(l, feet.below(), false);
        }
        if (p.isInWaterRainOrBubble()) {
            if (p.tickCount % 4 == 0) l.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 1, p.getZ(), 3, 0.3, 0.6, 0.3, 0.02);
            if (p.tickCount % 30 == 0) {
                Fx.sound(l, p.position(), SoundEvents.FIRE_EXTINGUISH, 0.6F, 1.2F);
                if (p.isInWater()) p.hurt(p.damageSources().drown(), 1F);
            }
        }
        if (d.blocking && p.tickCount % 2 == 0) {
            double a = p.tickCount * 0.5;
            for (int i = 0; i < 6; i++) {
                double b = a + i * Math.PI / 3;
                Vec3 at = p.position().add(Math.cos(b) * 0.9, 0.3 + (i % 3) * 0.6, Math.sin(b) * 0.9);
                l.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 1, 0.02, 0.02, 0.02, 0.01);
            }
        }
    }

    @Override
    public void melee(ServerPlayer p, OmniData d, LivingEntity target, LivingHurtEvent e) {
        if (!target.fireImmune()) target.setSecondsOnFire(5);
        p.serverLevel().sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(), 12, 0.2, 0.3, 0.2, 0.05);
        Fx.sound(p.serverLevel(), target.position(), SoundEvents.FIRECHARGE_USE, 0.5F, 1.6F);
    }

    @Override
    public boolean guardCancel(ServerPlayer p, OmniData d, DamageSource src) {
        if (src.is(DamageTypeTags.IS_PROJECTILE) && src.getDirectEntity() != null) {
            var pr = src.getDirectEntity();
            p.serverLevel().sendParticles(ParticleTypes.FLAME, pr.getX(), pr.getY(), pr.getZ(), 15, 0.2, 0.2, 0.2, 0.05);
            Fx.sound(p.serverLevel(), pr.position(), SoundEvents.FIRE_EXTINGUISH, 0.8F, 1.4F);
            pr.discard();
            return true;
        }
        return false;
    }

    @Override
    public void guardHit(ServerPlayer p, OmniData d, DamageSource src, LivingEntity attacker) {
        if (attacker != null && attacker.distanceTo(p) < 4) {
            attacker.setSecondsOnFire(6);
            Fx.knock(attacker, p.position(), 0.6, 0.2);
        }
        p.serverLevel().sendParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 1, p.getZ(), 20, 0.5, 0.6, 0.5, 0.06);
    }

    @Override
    public void common(Player p, OmniData d) {
        if (d.ability == 1 && d.abilityTick > 4) {
            // the flamethrower jet pushes Heatblast slightly backwards
            if (p.isControlledByLocalInstance()) p.setDeltaMovement(p.getDeltaMovement().subtract(p.getLookAngle().scale(0.012)));
        }
    }
}
