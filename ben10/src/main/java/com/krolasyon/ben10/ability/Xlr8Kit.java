package com.krolasyon.ben10.ability;

import com.krolasyon.ben10.Alien;
import com.krolasyon.ben10.ModRegistry;
import com.krolasyon.ben10.OmniData;
import com.krolasyon.ben10.Transform;
import com.krolasyon.ben10.entity.FxEntity;
import com.krolasyon.ben10.net.Net;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.*;

/** XLR8: speed dash, cyclone, blitz combo, overdrive. Guard = evasion (dodges projectiles, often melee). */
public class Xlr8Kit extends Kit {
    private final Map<UUID, Set<Integer>> hitOnce = new HashMap<>();
    private final Map<UUID, List<Integer>> combo = new HashMap<>();

    static void afterimage(ServerPlayer p) {
        FxEntity.spawn(p.serverLevel(), p.position(), FxEntity.AFTERIMAGE, 10, 1F, Alien.XLR8.ordinal(), p).yaw(p.yBodyRot);
    }

    @Override
    public boolean start(ServerPlayer p, OmniData d, int slot) {
        ServerLevel l = p.serverLevel();
        hitOnce.put(p.getUUID(), new HashSet<>());
        switch (slot) {
            case 0 -> Fx.sound(l, p.position(), ModRegistry.S_WHOOSH.get(), 1.4F, 1.2F);
            case 1 -> {
                FxEntity.spawn(l, p.position(), FxEntity.TORNADO, 60, 1.25F, 0xFFFFFF, p).follow(p);
                Fx.sound(l, p.position(), SoundEvents.ELYTRA_FLYING, 1.2F, 1.6F);
            }
            case 2 -> {
                List<LivingEntity> t = Fx.around(p, p.position(), 14);
                t.sort(Comparator.comparingDouble(e -> e.distanceToSqr(p)));
                List<Integer> ids = new ArrayList<>();
                for (int i = 0; i < Math.min(7, t.size()); i++) ids.add(t.get(i).getId());
                if (ids.isEmpty()) {
                    p.displayClientMessage(net.minecraft.network.chat.Component.translatable("msg.krolben10.notarget"), true);
                    return false;
                }
                combo.put(p.getUUID(), ids);
            }
            case 3 -> {
                d.set(OmniData.F_OVERDRIVE, true);
                Transform.add(p, Attributes.MOVEMENT_SPEED, Transform.BOOST, 0.9, AttributeModifier.Operation.MULTIPLY_TOTAL);
                Fx.sound(l, p.position(), SoundEvents.BEACON_ACTIVATE, 1.2F, 2F);
                Fx.sound(l, p.position(), ModRegistry.S_WHOOSH.get(), 1.4F, 0.8F);
                FxEntity.spawn(l, p.position().add(0, 1, 0), FxEntity.FLASH, 10, 3F, 0x40D0FF, p);
            }
            default -> {}
        }
        return true;
    }

    @Override
    public void tick(ServerPlayer p, OmniData d, int slot, int t) {
        ServerLevel l = p.serverLevel();
        Set<Integer> hit = hitOnce.computeIfAbsent(p.getUUID(), u -> new HashSet<>());
        switch (slot) {
            case 0 -> {
                if (t <= 6) {
                    Vec3 look = p.getLookAngle();
                    Vec3 v = new Vec3(look.x, look.y * 0.4, look.z).normalize().scale(2.1);
                    Vec3 before = p.position();
                    Fx.push(p, v);
                    afterimage(p);
                    Fx.particles(l, ParticleTypes.CLOUD, before.add(0, 0.3, 0), 4, 0.2, 0.02);
                    for (LivingEntity e : Fx.beam(p, before.add(0, 0.9, 0), before.add(v.scale(1.3)).add(0, 0.9, 0), 1.3)) {
                        if (hit.add(e.getId())) {
                            Fx.hit(p, e, 8F);
                            Fx.push(e, v.normalize().scale(0.9).add(0, 0.5, 0));
                            Fx.particles(l, ParticleTypes.SWEEP_ATTACK, e.position().add(0, 1, 0), 1, 0, 0);
                            Fx.sound(l, e.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1F, 1.6F);
                        }
                    }
                }
                if (t == 7) Fx.push(p, p.getDeltaMovement().scale(0.25));
            }
            case 1 -> {
                Vec3 c = p.position();
                double ang = t * 0.9;
                if (t % 2 == 0) {
                    Vec3 at = c.add(Math.cos(ang) * 1.6, 0, Math.sin(ang) * 1.6);
                    FxEntity.spawn(l, at, FxEntity.AFTERIMAGE, 6, 1F, Alien.XLR8.ordinal(), p).yaw((float) Math.toDegrees(ang) + 180);
                }
                for (LivingEntity e : Fx.around(p, c.add(0, 1, 0), 8)) {
                    Vec3 to = c.subtract(e.position());
                    double dist = Math.max(0.5, to.horizontalDistance());
                    Vec3 in = new Vec3(to.x, 0, to.z).normalize();
                    Vec3 tan = new Vec3(-in.z, 0, in.x);
                    double up = dist < 3 ? 0.22 : 0.08;
                    Fx.push(e, in.scale(dist > 2 ? 0.28 : -0.05).add(tan.scale(0.45)).add(0, up, 0));
                    e.fallDistance = 0;
                    if (t % 8 == 0) Fx.hit(p, e, 2.5F);
                }
                if (t % 10 == 0) {
                    Fx.sound(l, c, SoundEvents.ELYTRA_FLYING, 0.8F, 1.8F);
                    Fx.shatter(l, c.add(0, 1, 0), 3, 6);
                }
                if (t == 56) {
                    for (LivingEntity e : Fx.around(p, c.add(0, 1, 0), 9)) {
                        Fx.hit(p, e, 7F);
                        Fx.knock(e, c, 1.8, 1.0);
                    }
                    Fx.sound(l, c, SoundEvents.GENERIC_EXPLODE, 1F, 1.6F);
                    FxEntity.spawn(l, c.add(0, 0.1, 0), FxEntity.RING, 12, 8F, 0xD0F0FF, p);
                    Net.shake(p, 16, 0.4F);
                }
            }
            case 2 -> {
                List<Integer> ids = combo.getOrDefault(p.getUUID(), List.of());
                if ((t - 3) % 5 == 0 && t >= 3) {
                    int i = (t - 3) / 5;
                    if (i >= ids.size()) {
                        Powers.finish(p, d);
                        return;
                    }
                    if (!(l.getEntity(ids.get(i)) instanceof LivingEntity e) || !e.isAlive()) return;
                    afterimage(p);
                    Vec3 behind = e.position().add(e.position().subtract(p.position()).multiply(1, 0, 1).normalize().scale(1.4));
                    var clip = l.clip(new ClipContext(e.position().add(0, 0.5, 0), behind.add(0, 0.5, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
                    if (clip.getType() == HitResult.Type.BLOCK) behind = e.position();
                    Vec3 face = e.position().subtract(behind);
                    float yaw = (float) (Mth.atan2(face.z, face.x) * Mth.RAD_TO_DEG) - 90F;
                    p.teleportTo(l, behind.x, behind.y, behind.z, yaw, p.getXRot());
                    p.setYHeadRot(yaw);
                    Fx.hit(p, e, 7F);
                    Fx.push(e, face.normalize().scale(0.6).add(0, 0.55, 0));
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2));
                    Fx.particles(l, ParticleTypes.SWEEP_ATTACK, e.position().add(0, 1, 0), 2, 0.3, 0);
                    Fx.particles(l, ParticleTypes.CRIT, e.position().add(0, 1, 0), 12, 0.4, 0.3);
                    Fx.sound(l, e.position(), SoundEvents.PLAYER_ATTACK_CRIT, 1.2F, 1.4F);
                    Fx.sound(l, e.position(), ModRegistry.S_WHOOSH.get(), 0.8F, 1.6F);
                }
            }
            case 3 -> {
                if (t % 10 == 0) {
                    for (LivingEntity e : Fx.around(p, p.position(), 12)) {
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 14, 3, false, false));
                        e.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 14, 2, false, false));
                    }
                }
                if (p.getDeltaMovement().horizontalDistanceSqr() > 0.02 && t % 2 == 0) afterimage(p);
                Fx.particles(l, Fx.dust(0x40D0FF, 0.8F), p.position().add(0, 1, 0), 2, 0.3, 0);
            }
            default -> {}
        }
    }

    @Override
    public void end(ServerPlayer p, OmniData d, int slot) {
        if (slot == 3) {
            d.set(OmniData.F_OVERDRIVE, false);
            var inst = p.getAttribute(Attributes.MOVEMENT_SPEED);
            if (inst != null) inst.removeModifier(Transform.BOOST);
        }
        hitOnce.remove(p.getUUID());
        combo.remove(p.getUUID());
    }

    @Override
    public void passive(ServerPlayer p, OmniData d) {
        ServerLevel l = p.serverLevel();
        if (p.isSprinting() && p.onGround() && p.tickCount % 2 == 0) {
            l.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.1, p.getZ(), 1, 0.15, 0, 0.15, 0.01);
        }
        BlockPos below = BlockPos.containing(p.getX(), p.getY() - 0.2, p.getZ());
        if (l.getFluidState(below).is(FluidTags.WATER) && p.getDeltaMovement().horizontalDistanceSqr() > 0.02) {
            l.sendParticles(ParticleTypes.SPLASH, p.getX(), p.getY() + 0.1, p.getZ(), 6, 0.3, 0, 0.3, 0.1);
        }
    }

    @Override
    public void common(Player p, OmniData d) {
        if (!p.isControlledByLocalInstance() || p.isShiftKeyDown()) return;
        // run across water
        Vec3 v = p.getDeltaMovement();
        BlockPos feet = BlockPos.containing(p.getX(), p.getY() - 0.05, p.getZ());
        boolean water = p.level().getFluidState(feet).is(FluidTags.WATER) || p.level().getFluidState(feet.above()).is(FluidTags.WATER);
        if (water && !p.isUnderWater() && v.horizontalDistanceSqr() > 0.015) {
            double surface = feet.getY() + 1 + (p.level().getFluidState(feet.above()).is(FluidTags.WATER) ? 1 : 0) - 0.1;
            double dy = surface - p.getY();
            p.setDeltaMovement(v.x * 1.06, Math.max(v.y, Math.min(0.25, dy * 0.5)), v.z * 1.06);
            p.fallDistance = 0;
        }
    }

    @Override
    public void melee(ServerPlayer p, OmniData d, LivingEntity target, LivingHurtEvent e) {
        if (d.has(OmniData.F_OVERDRIVE)) e.setAmount(e.getAmount() * 1.8F);
        Fx.particles(p.serverLevel(), ParticleTypes.SWEEP_ATTACK, target.position().add(0, 1, 0), 1, 0, 0);
    }

    @Override
    public boolean guardCancel(ServerPlayer p, OmniData d, DamageSource src) {
        boolean proj = src.is(DamageTypeTags.IS_PROJECTILE);
        if (proj || p.getRandom().nextFloat() < 0.55F) {
            afterimage(p);
            Vec3 side = new Vec3(-Fx.flatLook(p).z, 0, Fx.flatLook(p).x).scale(p.getRandom().nextBoolean() ? 1 : -1);
            Fx.push(p, side.scale(0.9).add(0, 0.15, 0));
            Fx.sound(p.serverLevel(), p.position(), ModRegistry.S_WHOOSH.get(), 0.8F, 1.8F);
            return true;
        }
        return false;
    }
}
