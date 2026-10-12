package com.krolasyon.ben10.ability;

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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.ArrayList;
import java.util.List;

/** Four Arms: ground slam, thunder clap, mighty leap, grab & throw (mobs or ripped-up boulders). Guard = four-arm block. */
public class FourArmsKit extends Kit {
    /** thrown mobs keep flying and crash into things */
    private static final class Thrown {
        final LivingEntity e;
        final ServerPlayer by;
        int t;

        Thrown(LivingEntity e, ServerPlayer by) {
            this.e = e;
            this.by = by;
        }
    }

    private final List<Thrown> thrown = new ArrayList<>();

    @Override
    public boolean start(ServerPlayer p, OmniData d, int slot) {
        ServerLevel l = p.serverLevel();
        switch (slot) {
            case 0 -> Fx.sound(l, p.position(), SoundEvents.RAVAGER_ROAR, 0.8F, 1.3F);
            case 1 -> Fx.sound(l, p.position(), SoundEvents.RAVAGER_ROAR, 0.6F, 1.6F);
            case 2 -> {
                if (!p.onGround() && !p.isInWater()) return false;
                d.set(OmniData.F_AIRBORNE, false);
            }
            case 3 -> d.targetId = -1;
            default -> {}
        }
        return true;
    }

    @Override
    public void tick(ServerPlayer p, OmniData d, int slot, int t) {
        ServerLevel l = p.serverLevel();
        switch (slot) {
            case 0 -> {
                if (t == 11) {
                    Vec3 c = p.position().add(Fx.flatLook(p).scale(1.6));
                    Fx.shockwave(p, c, 6.5, 12F, 0.9, 0.95, 20);
                    FxEntity.spawn(l, c.add(0, 0.05, 0), FxEntity.RING, 14, 8F, 0xFFE0C0, p);
                    Fx.sound(l, c, SoundEvents.GENERIC_EXPLODE, 1.5F, 0.6F);
                    Fx.sound(l, c, SoundEvents.ANVIL_LAND, 1F, 0.5F);
                    Fx.shatter(l, c, 3.0, 12);
                }
            }
            case 1 -> {
                if (t == 8) {
                    Vec3 look = p.getLookAngle();
                    Fx.sound(l, p.position(), SoundEvents.WARDEN_SONIC_BOOM, 1.2F, 1.5F);
                    Fx.sound(l, p.position(), SoundEvents.LIGHTNING_BOLT_THUNDER, 0.5F, 1.8F);
                    Vec3 hands = Fx.rel(p, 1.2, 0, 0.5);
                    Fx.particles(l, ParticleTypes.EXPLOSION, hands, 2, 0.2, 0);
                    for (LivingEntity e : Fx.cone(p, 11, 38)) {
                        double k = 1 - e.distanceTo(p) / 13.0;
                        Fx.hit(p, e, (float) (7 * Math.max(0.4, k)));
                        Fx.push(e, look.scale(2.3 * Math.max(0.45, k)).add(0, 0.45, 0));
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 3));
                        e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0));
                        e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1));
                    }
                    Net.shake(p, 20, 0.6F);
                }
                if (t >= 8 && t <= 15) {
                    double dist = 1.2 + (t - 8) * 1.4;
                    Vec3 at = Fx.rel(p, dist, 0, 0.5);
                    FxEntity.spawn(l, at, FxEntity.SONIC_RING, 8, 1.2F + (t - 8) * 0.45F, 0xFFFFFF, p).yaw(p.getYRot());
                    Fx.particles(l, ParticleTypes.CLOUD, at, 6, 0.4, 0.05);
                    Fx.shatter(l, at, 1.6 + (t - 8) * 0.25, 6);
                }
            }
            case 2 -> {
                if (t == 5) {
                    Vec3 dir = Fx.flatLook(p);
                    double pitch = Math.max(0, -p.getXRot()) / 90.0;
                    Fx.push(p, dir.scale(1.5 - pitch * 0.6).add(0, 1.25 + pitch * 0.5, 0));
                    p.fallDistance = 0;
                    Fx.sound(l, p.position(), SoundEvents.RAVAGER_STEP, 1.5F, 0.6F);
                    Fx.shockwave(p, p.position(), 2.5, 3F, 0.4, 0.3, 8);
                    d.set(OmniData.F_AIRBORNE, true);
                }
                if (t > 5 && d.has(OmniData.F_AIRBORNE)) {
                    Fx.particles(l, ParticleTypes.CLOUD, p.position(), 2, 0.3, 0.02);
                    if (t > 9 && (p.onGround() || p.isInWater())) {
                        land(p, d);
                        Powers.finish(p, d);
                    }
                }
            }
            case 3 -> grabTick(p, d, l, t);
            default -> {}
        }
    }

    private void land(ServerPlayer p, OmniData d) {
        d.set(OmniData.F_AIRBORNE, false);
        ServerLevel l = p.serverLevel();
        Vec3 c = p.position();
        Fx.shockwave(p, c, 7, 15F, 1.1, 0.9, 26);
        FxEntity.spawn(l, c.add(0, 0.05, 0), FxEntity.RING, 16, 9F, 0xFFE0C0, p);
        Fx.sound(l, c, SoundEvents.GENERIC_EXPLODE, 1.8F, 0.55F);
        Fx.sound(l, c, SoundEvents.ANVIL_LAND, 1.2F, 0.4F);
        Fx.shatter(l, c, 3.5, 16);
        p.fallDistance = 0;
    }

    private void grabTick(ServerPlayer p, OmniData d, ServerLevel l, int t) {
        if (t == 5) {
            LivingEntity e = Fx.nearestInFront(p, 4.5, 55);
            if (e != null && e.getBbWidth() < 2.5F) {
                d.targetId = e.getId();
                d.set(OmniData.F_GRAB, true);
                Fx.sound(l, e.position(), SoundEvents.PLAYER_ATTACK_STRONG, 1F, 0.6F);
            } else if (Fx.grief()) {
                // tear a boulder out of the ground in front
                BlockHitResult hit = Fx.rayBlock(p, 5);
                BlockPos bp = hit.getType() == HitResult.Type.BLOCK ? hit.getBlockPos() : BlockPos.containing(p.position().add(Fx.flatLook(p).scale(1.5))).below();
                BlockState s = l.getBlockState(bp);
                if (s.isAir() || s.getDestroySpeed(l, bp) < 0 || s.getDestroySpeed(l, bp) > 30 || l.getBlockEntity(bp) != null || !s.getFluidState().isEmpty()) {
                    bp = p.blockPosition().below().relative(p.getDirection());
                    s = l.getBlockState(bp);
                }
                if (!s.isAir() && s.getDestroySpeed(l, bp) >= 0 && s.getDestroySpeed(l, bp) <= 30 && l.getBlockEntity(bp) == null && s.getFluidState().isEmpty()) {
                    l.removeBlock(bp, false);
                    Fx.particles(l, new BlockParticleOption(ParticleTypes.BLOCK, s), Vec3.atCenterOf(bp), 30, 0.5, 0.2);
                    Fx.sound(l, Vec3.atCenterOf(bp), s.getSoundType().getBreakSound(), 1.5F, 0.6F);
                    AlienProjectile b = AlienProjectile.shoot(p, AlienProjectile.BOULDER, Vec3.atCenterOf(bp), Vec3.ZERO, 16F);
                    b.setBlock(s);
                    b.setScale(1.3F);
                    b.setNoGravity(true);
                    b.life = 400;
                    d.targetId = b.getId();
                    d.set(OmniData.F_GRAB, true);
                }
            }
            if (!d.has(OmniData.F_GRAB)) {
                Powers.finish(p, d);
                d.cooldown[3] = 20;
            }
            return;
        }
        Entity e = Powers.target(p, d);
        if (e == null || !e.isAlive()) {
            if (t > 5 && t < 32) Powers.finish(p, d);
            return;
        }
        Vec3 over = p.position().add(0, p.getBbHeight() + 0.3, 0).add(Fx.flatLook(p).scale(0.2));
        if (t > 5 && t < 32) {
            Vec3 cur = e.position();
            Vec3 to = over.subtract(0, e instanceof AlienProjectile ? -0.2 : 0, 0);
            Vec3 pos = t < 14 ? cur.lerp(to, 0.35) : to;
            e.setPos(pos.x, pos.y, pos.z);
            e.setDeltaMovement(Vec3.ZERO);
            e.fallDistance = 0;
            e.hurtMarked = true;
            if (e instanceof ServerPlayer sp) sp.connection.teleport(pos.x, pos.y, pos.z, sp.getYRot(), sp.getXRot());
            if (e instanceof LivingEntity le && t % 10 == 0) Fx.hit(p, le, 1.5F);
        }
        if (t == 32) {
            Vec3 look = p.getLookAngle();
            Vec3 v = look.scale(2.3).add(0, 0.35, 0);
            Fx.sound(l, p.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.4F, 0.5F);
            Fx.sound(l, p.position(), SoundEvents.RAVAGER_ROAR, 0.7F, 1.4F);
            if (e instanceof AlienProjectile b) {
                b.setNoGravity(false);
                b.gravity = 0.04F;
                b.life = b.tickCount + 100;
                b.setDeltaMovement(v.scale(1.05));
            } else if (e instanceof LivingEntity le) {
                Fx.hit(p, le, 6F);
                Fx.push(le, v);
                thrown.add(new Thrown(le, p));
            }
            d.targetId = -1;
            d.set(OmniData.F_GRAB, false);
        }
    }

    @Override
    public void end(ServerPlayer p, OmniData d, int slot) {
        if (slot == 3 && d.has(OmniData.F_GRAB)) {
            Entity e = Powers.target(p, d);
            if (e instanceof AlienProjectile b) {
                b.setNoGravity(false);
                b.gravity = 0.05F;
            }
            d.set(OmniData.F_GRAB, false);
        }
        if (slot == 2) d.set(OmniData.F_AIRBORNE, false);
        d.targetId = -1;
    }

    @Override
    public void passive(ServerPlayer p, OmniData d) {
        ServerLevel l = p.serverLevel();
        if (p.onGround() && p.getDeltaMovement().horizontalDistanceSqr() > 0.01 && p.tickCount % 9 == 0) {
            BlockState s = l.getBlockState(p.blockPosition().below());
            if (!s.isAir()) Fx.particles(l, new BlockParticleOption(ParticleTypes.BLOCK, s), p.position(), 6, 0.4, 0.1);
        }
        thrown.removeIf(th -> {
            th.t++;
            LivingEntity e = th.e;
            if (!e.isAlive() || th.t > 40) return true;
            ServerLevel el = (ServerLevel) e.level();
            el.sendParticles(ParticleTypes.CLOUD, e.getX(), e.getY() + 0.5, e.getZ(), 1, 0.1, 0.1, 0.1, 0);
            if (th.t > 3 && (e.horizontalCollision || e.onGround())) {
                Fx.hit(th.by, e, 9F);
                for (LivingEntity o : Fx.around(th.by, e.position(), 2.5)) {
                    if (o != e) {
                        Fx.hit(th.by, o, 7F);
                        Fx.knock(o, e.position(), 0.9, 0.4);
                    }
                }
                Fx.particles(el, ParticleTypes.EXPLOSION, e.position(), 1, 0, 0);
                Fx.sound(el, e.position(), SoundEvents.GENERIC_EXPLODE, 0.8F, 1.2F);
                return true;
            }
            return false;
        });
    }

    @Override
    public void melee(ServerPlayer p, OmniData d, LivingEntity target, LivingHurtEvent e) {
        Fx.knock(target, p.position(), 1.3, 0.45);
        Fx.sound(p.serverLevel(), target.position(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1F, 0.6F);
        Fx.particles(p.serverLevel(), ParticleTypes.CRIT, target.position().add(0, target.getBbHeight() / 2, 0), 10, 0.3, 0.2);
    }

    @Override
    public void guardHit(ServerPlayer p, OmniData d, DamageSource src, LivingEntity attacker) {
        if (attacker != null && attacker.distanceTo(p) < 5) Fx.knock(attacker, p.position(), 1.0, 0.3);
        Fx.sound(p.serverLevel(), p.position(), SoundEvents.SHIELD_BLOCK, 1F, 0.6F);
    }

    @Override
    public void land(Player p, OmniData d, float distance) {
        if (p instanceof ServerPlayer sp && distance > 5 && d.ability != 2) {
            Fx.shockwave(sp, sp.position(), Math.min(5, distance / 2), Math.min(10, distance), 0.6, 0.5, 10);
            Fx.sound(sp.serverLevel(), sp.position(), SoundEvents.GENERIC_EXPLODE, 0.8F, 0.8F);
        }
    }
}
