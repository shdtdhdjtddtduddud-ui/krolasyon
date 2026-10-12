package com.krolasyon.ben10.ability;

import com.krolasyon.ben10.OmniData;
import com.krolasyon.ben10.entity.FxEntity;
import com.krolasyon.ben10.net.Net;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.*;

/** Cannonbolt: rolling charge, cannonball launch, bounce bomb, ricochet between enemies. Guard = armoured shell. */
public class CannonboltKit extends Kit {
    private final Map<UUID, Map<Integer, Integer>> lastHit = new HashMap<>();
    private final Map<UUID, List<Integer>> chain = new HashMap<>();

    @Override
    public boolean start(ServerPlayer p, OmniData d, int slot) {
        ServerLevel l = p.serverLevel();
        lastHit.put(p.getUUID(), new HashMap<>());
        d.counter = 0;
        switch (slot) {
            case 0 -> {
                d.set(OmniData.F_ROLLING, true);
                p.refreshDimensions();
                Fx.sound(l, p.position(), SoundEvents.ARMOR_EQUIP_NETHERITE, 1.5F, 0.6F);
            }
            case 1, 2 -> {
                d.set(OmniData.F_ROLLING, true);
                p.refreshDimensions();
                Fx.sound(l, p.position(), SoundEvents.ARMOR_EQUIP_NETHERITE, 1.5F, 0.8F);
            }
            case 3 -> {
                List<LivingEntity> t = Fx.around(p, p.position(), 16);
                t.sort(Comparator.comparingDouble(e -> e.distanceToSqr(p)));
                List<Integer> ids = new ArrayList<>();
                for (int i = 0; i < Math.min(5, t.size()); i++) ids.add(t.get(i).getId());
                if (ids.isEmpty()) {
                    p.displayClientMessage(net.minecraft.network.chat.Component.translatable("msg.krolben10.notarget"), true);
                    return false;
                }
                chain.put(p.getUUID(), ids);
                d.set(OmniData.F_ROLLING, true);
                p.refreshDimensions();
            }
            default -> {}
        }
        return true;
    }

    private void crush(ServerPlayer p, double radius, float dmg, double knock) {
        Map<Integer, Integer> lh = lastHit.computeIfAbsent(p.getUUID(), u -> new HashMap<>());
        Vec3 v = p.getDeltaMovement();
        Vec3 dir = v.horizontalDistanceSqr() > 1e-3 ? new Vec3(v.x, 0, v.z).normalize() : Fx.flatLook(p);
        for (LivingEntity e : Fx.around(p, p.position().add(0, 0.6, 0), radius)) {
            Integer last = lh.get(e.getId());
            if (last != null && p.tickCount - last < 10) continue;
            lh.put(e.getId(), p.tickCount);
            Fx.hit(p, e, dmg);
            Fx.push(e, dir.scale(knock).add(0, 0.5, 0));
            Fx.sound(p.serverLevel(), e.position(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.2F, 0.6F);
            Fx.particles(p.serverLevel(), ParticleTypes.CRIT, e.position().add(0, 1, 0), 10, 0.3, 0.2);
        }
    }

    private void groundDust(ServerPlayer p) {
        ServerLevel l = p.serverLevel();
        BlockState s = l.getBlockState(p.blockPosition().below());
        if (!s.isAir()) Fx.particles(l, new BlockParticleOption(ParticleTypes.BLOCK, s), p.position().add(0, 0.1, 0), 5, 0.4, 0.1);
    }

    private void impact(ServerPlayer p, double r, float dmg) {
        ServerLevel l = p.serverLevel();
        Vec3 c = p.position();
        Fx.shockwave(p, c, r, dmg, 1.2, 0.7, 14);
        FxEntity.spawn(l, c.add(0, 0.06, 0), FxEntity.RING, 12, (float) r * 1.3F, 0xE8F0C0, p);
        Fx.sound(l, c, SoundEvents.GENERIC_EXPLODE, 1.3F, 0.8F);
        Fx.sound(l, c, SoundEvents.ANVIL_LAND, 0.8F, 0.6F);
        Fx.shatter(l, c.add(0, 0.5, 0), r * 0.55, 10);
    }

    @Override
    public void tick(ServerPlayer p, OmniData d, int slot, int t) {
        ServerLevel l = p.serverLevel();
        switch (slot) {
            case 0 -> {
                crush(p, 1.3, 8F, 1.3);
                if (t % 2 == 0) {
                    groundDust(p);
                    Vec3 front = p.position().add(Fx.flatLook(p).scale(0.9)).add(0, 0.6, 0);
                    Fx.shatter(l, front, 1.2, 3);
                }
                if (t % 7 == 0) Fx.sound(l, p.position(), SoundEvents.RAVAGER_STEP, 0.8F, 0.7F);
            }
            case 1 -> {
                if (t == 6) {
                    Vec3 look = p.getLookAngle();
                    Fx.push(p, look.scale(2.7).add(0, 0.25, 0));
                    Fx.sound(l, p.position(), SoundEvents.GENERIC_EXPLODE, 1.0F, 1.6F);
                    Fx.particles(l, ParticleTypes.EXPLOSION, p.position().add(0, 0.6, 0), 1, 0, 0);
                }
                if (t > 6) {
                    Fx.particles(l, ParticleTypes.CLOUD, p.position().add(0, 0.6, 0), 3, 0.3, 0.02);
                    boolean contact = !Fx.around(p, p.position().add(0, 0.6, 0), 1.3).isEmpty();
                    if (contact || (t > 8 && p.horizontalCollision) || (t > 12 && p.onGround()) || t >= 28) {
                        impact(p, 4.5, 13F);
                        Net.shake(p, 18, 0.7F);
                        Powers.finish(p, d);
                    }
                }
            }
            case 2 -> {
                if (t == 4 || (d.counter > 0 && d.counter < 3 && d.vec.x == -1)) {
                    double up = t == 4 ? 1.25 : 1.0 - d.counter * 0.15;
                    Fx.push(p, Fx.flatLook(p).scale(0.55).add(0, up, 0));
                    d.vec = new Vec3(t, 0, 0);
                    Fx.sound(l, p.position(), SoundEvents.SLIME_JUMP, 1.2F, 0.5F);
                }
                if (d.vec.x > 0 && t > d.vec.x + 6 && p.onGround()) {
                    d.counter++;
                    impact(p, 4.0 + d.counter * 0.6, 8F + d.counter * 2);
                    Net.shake(p, 16, 0.5F);
                    d.vec = new Vec3(-1, 0, 0);
                    if (d.counter >= 3) Powers.finish(p, d);
                }
            }
            case 3 -> {
                List<Integer> ids = chain.getOrDefault(p.getUUID(), List.of());
                if (d.counter >= ids.size()) {
                    Powers.finish(p, d);
                    return;
                }
                if (!(l.getEntity(ids.get(d.counter)) instanceof LivingEntity e) || !e.isAlive()) {
                    d.counter++;
                    return;
                }
                Vec3 to = e.position().add(0, e.getBbHeight() * 0.4, 0).subtract(p.position().add(0, 0.6, 0));
                if (to.length() < 1.8) {
                    Fx.hit(p, e, 10F);
                    Fx.push(e, to.normalize().scale(1.2).add(0, 0.6, 0));
                    Fx.sound(l, e.position(), SoundEvents.ANVIL_PLACE, 0.8F, 1.5F);
                    Fx.particles(l, ParticleTypes.EXPLOSION, e.position().add(0, 1, 0), 1, 0, 0);
                    Fx.push(p, to.normalize().scale(-0.6).add(0, 0.6, 0));
                    d.counter++;
                    d.vec = new Vec3(t, 0, 0);
                } else if (t - d.vec.x > 3) {
                    Fx.push(p, to.normalize().scale(1.7).add(0, 0.12, 0));
                    Fx.particles(l, ParticleTypes.CLOUD, p.position().add(0, 0.6, 0), 2, 0.2, 0.01);
                }
            }
            default -> {}
        }
    }

    @Override
    public void end(ServerPlayer p, OmniData d, int slot) {
        d.set(OmniData.F_ROLLING, false);
        p.refreshDimensions();
        lastHit.remove(p.getUUID());
        chain.remove(p.getUUID());
        d.vec = Vec3.ZERO;
    }

    @Override
    public void common(Player p, OmniData d) {
        if (!p.isControlledByLocalInstance()) return;
        if (d.ability == 0 && d.has(OmniData.F_ROLLING)) {
            Vec3 v = p.getDeltaMovement();
            Vec3 dir = Vec3.directionFromRotation(0, p.getYRot()).scale(0.95);
            p.setDeltaMovement(v.x + (dir.x - v.x) * 0.3, v.y, v.z + (dir.z - v.z) * 0.3);
        }
    }

    @Override
    public void melee(ServerPlayer p, OmniData d, LivingEntity target, LivingHurtEvent e) {
        Fx.knock(target, p.position(), 0.9, 0.3);
    }

    @Override
    public boolean guardCancel(ServerPlayer p, OmniData d, DamageSource src) {
        if (src.is(DamageTypeTags.IS_PROJECTILE) && src.getDirectEntity() != null) {
            var pr = src.getDirectEntity();
            pr.setDeltaMovement(pr.getDeltaMovement().scale(-0.5).add(0, 0.2, 0));
            Fx.sound(p.serverLevel(), pr.position(), SoundEvents.SHIELD_BLOCK, 1F, 1.4F);
            Fx.particles(p.serverLevel(), ParticleTypes.CRIT, pr.position(), 6, 0.1, 0.1);
            return true;
        }
        return false;
    }

    @Override
    public void guardHit(ServerPlayer p, OmniData d, DamageSource src, LivingEntity attacker) {
        Fx.sound(p.serverLevel(), p.position(), SoundEvents.SHIELD_BLOCK, 1F, 0.5F);
        Fx.particles(p.serverLevel(), ParticleTypes.CRIT, p.position().add(0, 0.6, 0), 8, 0.4, 0.2);
    }
}
