package com.krolasyon.ben10.ability;

import com.krolasyon.ben10.ModRegistry;
import com.krolasyon.ben10.OmniData;
import com.krolasyon.ben10.entity.AlienProjectile;
import com.krolasyon.ben10.entity.FxEntity;
import com.krolasyon.ben10.net.Net;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/** Big Chill: freezing breath, ice prison, ice spear, blizzard. Guard = wing cloak that freezes attackers. */
public class BigChillKit extends Kit {
    private static final int ICE = 0xA8DCFF;

    static void freeze(LivingEntity e, int ticks) {
        e.setTicksFrozen(Math.min(400, Math.max(e.getTicksFrozen(), 0) + ticks));
    }

    @Override
    public boolean start(ServerPlayer p, OmniData d, int slot) {
        ServerLevel l = p.serverLevel();
        switch (slot) {
            case 0 -> Fx.sound(l, p.position(), SoundEvents.PLAYER_HURT_FREEZE, 1.2F, 0.5F);
            case 1 -> {
                LivingEntity e = Fx.aimed(p, 22);
                if (e == null) e = Fx.nearestInFront(p, 10, 35);
                if (e == null) {
                    p.displayClientMessage(net.minecraft.network.chat.Component.translatable("msg.krolben10.notarget"), true);
                    return false;
                }
                d.targetId = e.getId();
                Fx.sound(l, p.position(), SoundEvents.POWDER_SNOW_PLACE, 1.4F, 0.6F);
            }
            case 2 -> Fx.sound(l, p.position(), SoundEvents.TRIDENT_RETURN, 1F, 1.5F);
            case 3 -> {
                Fx.sound(l, p.position(), SoundEvents.ELYTRA_FLYING, 1.2F, 0.5F);
                Fx.push(p, new Vec3(0, 0.75, 0));
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
                if (t < 4 || t > 46) return;
                Vec3 look = p.getLookAngle();
                Vec3 from = Fx.rel(p, 0.8, 0, 0.2);
                for (int i = 0; i < 8; i++) {
                    Vec3 v = look.add(l.random.nextGaussian() * 0.1, l.random.nextGaussian() * 0.1, l.random.nextGaussian() * 0.1).normalize();
                    double sp = 0.5 + l.random.nextDouble() * 0.5;
                    l.sendParticles(i % 3 == 0 ? ParticleTypes.SNOWFLAKE : (i % 3 == 1 ? Fx.dust(ICE, 1.4F) : ParticleTypes.ITEM_SNOWBALL),
                            from.x, from.y, from.z, 0, v.x, v.y, v.z, sp);
                }
                if (t % 2 == 0) {
                    for (LivingEntity e : Fx.cone(p, 9, 22)) {
                        Fx.hit(p, e, 2.2F);
                        freeze(e, 24);
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
                        if (e.getTicksFrozen() > 200 && t % 6 == 0) {
                            Fx.hit(p, e, 3F);
                            Fx.particles(l, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ICE.defaultBlockState()), e.position().add(0, 1, 0), 8, 0.3, 0.1);
                        }
                    }
                }
                if (t % 3 == 0) {
                    Vec3 end = Fx.rayEnd(p, 9);
                    Vec3 dir = end.subtract(from);
                    for (int i = 2; i <= 9; i++) {
                        BlockPos bp = BlockPos.containing(from.add(dir.scale(i / 9.0)));
                        for (BlockPos q : new BlockPos[]{bp, bp.below(), bp.below(2)}) Fx.frost(l, q, true);
                    }
                }
                if (t % 5 == 0) Fx.sound(l, p.position(), SoundEvents.POWDER_SNOW_STEP, 1.5F, 0.6F);
            }
            case 1 -> {
                if (!(Powers.target(p, d) instanceof LivingEntity e) || !e.isAlive()) return;
                if (t < 6) Fx.line(l, Fx.dust(ICE, 1.2F), Fx.rel(p, 0.8, 0, 0.3), e.position().add(0, e.getBbHeight() / 2, 0), 0.5, 0.05);
                if (t == 6) {
                    Fx.hit(p, e, 7F);
                    freeze(e, 300);
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 254, false, false));
                    e.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 120, 3));
                    e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 2));
                    e.setDeltaMovement(Vec3.ZERO);
                    AABB bb = e.getBoundingBox();
                    BlockPos lo = BlockPos.containing(bb.minX - 0.6, bb.minY, bb.minZ - 0.6), hi = BlockPos.containing(bb.maxX + 0.6, bb.maxY + 0.6, bb.maxZ + 0.6);
                    for (BlockPos bp : BlockPos.betweenClosed(lo, hi)) {
                        boolean shell = bp.getX() == lo.getX() || bp.getX() == hi.getX() || bp.getZ() == lo.getZ() || bp.getZ() == hi.getZ() || bp.getY() == hi.getY();
                        if (shell) Fx.temp(l, bp.immutable(), ModRegistry.FROST.get());
                    }
                    for (int i = 0; i < 6; i++) {
                        double a = i * Math.PI / 3;
                        Vec3 at = e.position().add(Math.cos(a) * 1.3, 0, Math.sin(a) * 1.3);
                        FxEntity.spawn(l, new Vec3(at.x, Fx.groundY(l, at.x, at.y + 1, at.z), at.z), FxEntity.ICE_SPIKE, 110, 0.7F, ICE, p).yaw(i * 60);
                    }
                    Fx.particles(l, ParticleTypes.SNOWFLAKE, e.position().add(0, 1, 0), 40, 0.6, 0.1);
                    Fx.sound(l, e.position(), SoundEvents.GLASS_PLACE, 1.5F, 0.6F);
                    Fx.sound(l, e.position(), SoundEvents.PLAYER_HURT_FREEZE, 1.5F, 0.8F);
                }
            }
            case 2 -> {
                if (t < 5) Fx.particles(l, ParticleTypes.SNOWFLAKE, Fx.rel(p, 0.7, 0.45, 0.2), 3, 0.1, 0.01);
                if (t == 5) {
                    Vec3 from = Fx.rel(p, 1.0, 0.35, 0.25);
                    LivingEntity aim = Fx.aimed(p, 40);
                    Vec3 dir = aim != null ? aim.position().add(0, aim.getBbHeight() / 2, 0).subtract(from).normalize() : p.getLookAngle();
                    AlienProjectile s = AlienProjectile.shoot(p, AlienProjectile.ICE_SPEAR, from, dir.scale(2.6), 11F);
                    s.pierce = 2;
                    s.gravity = 0.008F;
                    s.life = 50;
                    s.setScale(1.2F);
                    Fx.sound(l, from, SoundEvents.TRIDENT_THROW, 1.2F, 0.7F);
                    Fx.sound(l, from, SoundEvents.GLASS_BREAK, 0.6F, 1.8F);
                }
            }
            case 3 -> {
                Vec3 c = p.position();
                if (t < 8) Fx.push(p, new Vec3(p.getDeltaMovement().x * 0.5, 0.18, p.getDeltaMovement().z * 0.5));
                if (t == 8) {
                    p.getAbilities().flying = true;
                    p.onUpdateAbilities();
                }
                for (int i = 0; i < 14; i++) {
                    double a = l.random.nextDouble() * Math.PI * 2, r = Math.sqrt(l.random.nextDouble()) * 9;
                    double x = c.x + Math.cos(a) * r, z = c.z + Math.sin(a) * r;
                    l.sendParticles(i % 2 == 0 ? ParticleTypes.SNOWFLAKE : ParticleTypes.CLOUD, x, c.y + 2 + l.random.nextDouble() * 3, z, 0,
                            -Math.sin(a) * 0.6, -0.25, Math.cos(a) * 0.6, 1.0);
                }
                if (t % 10 == 0) {
                    FxEntity.spawn(l, new Vec3(c.x, Fx.groundY(l, c.x, c.y, c.z) + 0.05, c.z), FxEntity.RING, 16, 10F, ICE, p);
                    for (LivingEntity e : Fx.around(p, c, 9.5)) {
                        Fx.hit(p, e, 3F);
                        freeze(e, 50);
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
                        Vec3 tan = new Vec3(-(e.getZ() - c.z), 0, e.getX() - c.x).normalize().scale(0.3);
                        Fx.push(e, e.getDeltaMovement().add(tan));
                    }
                    Fx.sound(l, c, SoundEvents.ELYTRA_FLYING, 0.8F, 0.6F);
                }
                if (t % 4 == 0) {
                    for (int i = 0; i < 6; i++) {
                        double a = l.random.nextDouble() * Math.PI * 2, r = l.random.nextDouble() * 7;
                        double x = c.x + Math.cos(a) * r, z = c.z + Math.sin(a) * r;
                        double gy = Fx.groundY(l, x, c.y + 1, z);
                        BlockPos bp = BlockPos.containing(x, gy + 0.1, z);
                        Fx.frost(l, bp, true);
                        Fx.frost(l, bp.below(), false);
                    }
                }
                if (t == 94) {
                    for (int i = 0; i < 10; i++) {
                        double a = i * Math.PI * 2 / 10;
                        double x = c.x + Math.cos(a) * 4.5, z = c.z + Math.sin(a) * 4.5;
                        FxEntity.spawn(l, new Vec3(x, Fx.groundY(l, x, c.y, z), z), FxEntity.ICE_SPIKE, 60, 1.1F, ICE, p).yaw(i * 36);
                    }
                    for (LivingEntity e : Fx.around(p, c, 9.5)) {
                        Fx.hit(p, e, 9F);
                        freeze(e, 160);
                        Fx.push(e, new Vec3(0, 0.7, 0));
                    }
                    Fx.sound(l, c, SoundEvents.GLASS_BREAK, 2F, 0.5F);
                    Fx.sound(l, c, SoundEvents.GENERIC_EXPLODE, 0.8F, 1.6F);
                    Net.shake(p, 18, 0.6F);
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
        ServerLevel l = p.serverLevel();
        if (p.tickCount % 5 == 0) l.sendParticles(ParticleTypes.SNOWFLAKE, p.getX(), p.getY() + 1.2, p.getZ(), 1, 0.4, 0.6, 0.4, 0.01);
        if (p.tickCount % 4 == 0) {
            BlockPos feet = p.blockPosition();
            for (int dx = -2; dx <= 2; dx++)
                for (int dz = -2; dz <= 2; dz++) {
                    if (dx * dx + dz * dz > 5) continue;
                    for (int dy = -2; dy <= 0; dy++) {
                        BlockPos q = feet.offset(dx, dy, dz);
                        if (l.getBlockState(q).is(Blocks.WATER) && l.getBlockState(q.above()).isAir()) Fx.frost(l, q, false);
                    }
                }
        }
        if (d.blocking && p.tickCount % 3 == 0) l.sendParticles(Fx.dust(ICE, 1.2F), p.getX(), p.getY() + 1.2, p.getZ(), 3, 0.5, 0.7, 0.5, 0);
    }

    @Override
    public void melee(ServerPlayer p, OmniData d, LivingEntity target, LivingHurtEvent e) {
        freeze(target, 90);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
        Fx.particles(p.serverLevel(), ParticleTypes.SNOWFLAKE, target.position().add(0, target.getBbHeight() / 2, 0), 12, 0.3, 0.05);
        Fx.sound(p.serverLevel(), target.position(), SoundEvents.PLAYER_HURT_FREEZE, 0.6F, 1.3F);
    }

    @Override
    public void guardHit(ServerPlayer p, OmniData d, DamageSource src, LivingEntity attacker) {
        if (attacker != null && attacker.distanceTo(p) < 5) {
            freeze(attacker, 200);
            attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 3));
            Fx.particles(p.serverLevel(), ParticleTypes.SNOWFLAKE, attacker.position().add(0, 1, 0), 20, 0.4, 0.05);
        }
        Fx.sound(p.serverLevel(), p.position(), SoundEvents.GLASS_HIT, 1F, 0.8F);
    }

    static float lerp(float a, float b, float t) {
        return Mth.lerp(t, a, b);
    }
}
