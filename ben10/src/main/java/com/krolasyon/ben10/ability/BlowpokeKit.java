package com.krolasyon.ben10.ability;

import com.krolasyon.ben10.ModRegistry;
import com.krolasyon.ben10.OmniData;
import com.krolasyon.ben10.entity.DebrisEntity;
import com.krolasyon.ben10.entity.FxEntity;
import com.krolasyon.ben10.net.Net;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.*;

/**
 * Blowpoke (a Geochelone Aerio, the wind turtle): draws air in and blasts it out through the vents of its shell, hovers by
 * spinning its flippers like propellers. Wind Blast, Tornado, Propeller Spin, Hurricane. Guard = withdraw into the shell
 * behind a wind barrier. Passive: hover flight, no fall damage, heavy shell.
 */
public class BlowpokeKit extends Kit {
    private static final int WIND = 0xE8F4FF;
    private final Map<UUID, Map<Integer, Integer>> lastHit = new HashMap<>();
    private final Map<UUID, List<Twister>> twisters = new HashMap<>();

    /** a tornado that keeps travelling after the ability ended */
    private static final class Twister {
        final FxEntity fx;
        Vec3 pos, dir;
        int age;
        final int life;

        Twister(FxEntity fx, Vec3 pos, Vec3 dir, int life) {
            this.fx = fx;
            this.pos = pos;
            this.dir = dir;
            this.life = life;
        }
    }

    @Override
    public boolean start(ServerPlayer p, OmniData d, int slot) {
        ServerLevel l = p.serverLevel();
        lastHit.put(p.getUUID(), new HashMap<>());
        switch (slot) {
            case 0 -> Fx.sound(l, p.position(), SoundEvents.PHANTOM_SWOOP, 1.3F, 0.7F);
            case 1 -> Fx.sound(l, p.position(), SoundEvents.ELYTRA_FLYING, 1.0F, 1.4F);
            case 2 -> {
                Fx.sound(l, p.position(), ModRegistry.S_WHOOSH.get(), 1.4F, 0.6F);
                Fx.sound(l, p.position(), SoundEvents.ARMOR_EQUIP_TURTLE, 1.4F, 0.7F);
            }
            case 3 -> {
                Fx.sound(l, p.position(), SoundEvents.ELYTRA_FLYING, 1.6F, 0.6F);
                Fx.sound(l, p.position(), SoundEvents.TRIDENT_RIPTIDE_3, 1.4F, 0.6F);
                double gy = Fx.groundY(l, p.getX(), p.getY() + 0.5, p.getZ());
                FxEntity.spawn(l, new Vec3(p.getX(), gy, p.getZ()), FxEntity.TORNADO, 70, 3.0F, WIND, p);
            }
            default -> {}
        }
        return true;
    }

    // ---------------- wind helpers ----------------

    /** a gust centred on c moving along dir: shoves mobs, items and projectiles, puts out fire, tears off plants and leaves */
    private void gust(ServerPlayer p, Vec3 c, Vec3 dir, double r, double force, float dmg, int griefMax) {
        ServerLevel l = p.serverLevel();
        Map<Integer, Integer> lh = lastHit.computeIfAbsent(p.getUUID(), u -> new HashMap<>());
        for (LivingEntity e : Fx.around(p, c, r)) {
            Integer last = lh.get(e.getId());
            if (dmg > 0 && (last == null || p.tickCount - last >= 10)) {
                lh.put(e.getId(), p.tickCount);
                Fx.hit(p, e, dmg);
            }
            double res = 1 - Math.min(1, e.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE)) * 0.6;
            e.setDeltaMovement(e.getDeltaMovement().scale(0.4).add(dir.scale(force * res)).add(0, 0.25 * force * res, 0));
            e.hurtMarked = true;
            e.fallDistance = 0;
            e.clearFire();
        }
        blowLoose(p, c, r, dir.scale(force));
        if (Fx.grief()) {
            int n = 0;
            int R = (int) Math.ceil(r);
            BlockPos o = BlockPos.containing(c);
            for (BlockPos bp : BlockPos.betweenClosed(o.offset(-R, -R, -R), o.offset(R, R, R))) {
                if (n >= griefMax) break;
                if (bp.distToCenterSqr(c) > r * r) continue;
                if (blowBlock(l, bp, dir.scale(force * 0.6))) n++;
            }
        }
    }

    /** non-living entities caught in the wind */
    private void blowLoose(ServerPlayer p, Vec3 c, double r, Vec3 v) {
        for (Entity e : p.level().getEntities(p, new AABB(c, c).inflate(r))) {
            if (e instanceof LivingEntity || e instanceof FxEntity) continue;
            if (e instanceof Projectile pr && pr.getOwner() == p) continue;
            if (e instanceof ItemEntity || e instanceof Projectile || e instanceof DebrisEntity || e instanceof net.minecraft.world.entity.item.FallingBlockEntity
                    || e instanceof net.minecraft.world.entity.vehicle.AbstractMinecart || e instanceof net.minecraft.world.entity.vehicle.Boat
                    || e instanceof net.minecraft.world.entity.item.PrimedTnt) {
                e.setDeltaMovement(e.getDeltaMovement().scale(0.3).add(v).add(0, 0.15, 0));
                e.hurtMarked = true;
            }
        }
    }

    private static boolean loose(BlockState s) {
        return s.is(BlockTags.LEAVES) || s.is(BlockTags.FLOWERS) || s.is(BlockTags.SAPLINGS) || s.is(Blocks.GRASS) || s.is(Blocks.TALL_GRASS)
                || s.is(Blocks.FERN) || s.is(Blocks.LARGE_FERN) || s.is(Blocks.DEAD_BUSH) || s.is(Blocks.SNOW) || s.is(Blocks.VINE)
                || s.is(Blocks.COBWEB) || s.is(BlockTags.WOOL_CARPETS) || s.is(Blocks.SUGAR_CANE);
    }

    /** fire is blown out, loose blocks are torn off and fly away as debris (they drop as items) */
    private static boolean blowBlock(ServerLevel l, BlockPos bp, Vec3 v) {
        BlockState s = l.getBlockState(bp);
        if (s.getBlock() instanceof BaseFireBlock) {
            l.removeBlock(bp, false);
            l.sendParticles(ParticleTypes.SMOKE, bp.getX() + 0.5, bp.getY() + 0.4, bp.getZ() + 0.5, 4, 0.2, 0.1, 0.2, 0.02);
            return true;
        }
        if (!loose(s)) return false;
        l.destroyBlock(bp, true);
        DebrisEntity.spawn(l, Vec3.atCenterOf(bp), s, v.add(0, 0.35 + l.random.nextDouble() * 0.3, 0));
        return true;
    }

    /** spiral wind particles around an axis from c */
    private static void swirl(ServerLevel l, Vec3 c, double r, double h, int t, int n) {
        for (int i = 0; i < n; i++) {
            double a = t * 0.7 + i * Math.PI * 2 / n;
            double y = (i % 4) * h / 4;
            double rr = r * (0.4 + y / Math.max(0.1, h) * 0.6);
            l.sendParticles(ParticleTypes.CLOUD, c.x + Math.cos(a) * rr, c.y + y, c.z + Math.sin(a) * rr, 0, -Math.sin(a), 0.15, Math.cos(a), 0.35);
        }
    }

    /** the five shell vents in front of the chest */
    private static Vec3 vents(ServerPlayer p) {
        return Fx.rel(p, 0.65, 0, 0.6);
    }

    @Override
    public void tick(ServerPlayer p, OmniData d, int slot, int t) {
        ServerLevel l = p.serverLevel();
        switch (slot) {
            case 0 -> {
                // Wind Blast: inhale through the shell, then a wide cone gust out of the vents
                Vec3 v = vents(p);
                if (t < 9) {
                    for (int i = 0; i < 3; i++) {
                        Vec3 from = v.add(p.getLookAngle().scale(4)).add(l.random.nextGaussian() * 1.3, l.random.nextGaussian() * 0.8, l.random.nextGaussian() * 1.3);
                        Vec3 vel = v.subtract(from).scale(0.25);
                        l.sendParticles(ParticleTypes.CLOUD, from.x, from.y, from.z, 0, vel.x, vel.y, vel.z, 1);
                    }
                }
                if (t == 9) {
                    Vec3 look = p.getLookAngle();
                    Fx.sound(l, p.position(), SoundEvents.ENDER_DRAGON_FLAP, 1.6F, 0.6F);
                    Fx.sound(l, p.position(), ModRegistry.S_WHOOSH.get(), 1.4F, 0.9F);
                    for (LivingEntity e : Fx.cone(p, 11, 32)) {
                        Fx.hit(p, e, 7F);
                        double res = 1 - Math.min(1, e.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE)) * 0.6;
                        Fx.push(e, look.scale(2.4 * res).add(0, 0.55 * res, 0));
                        e.clearFire();
                    }
                    Net.shake(p, 10, 0.35F);
                    FxEntity.spawn(l, v.add(look.scale(1.2)), FxEntity.SONIC_RING, 10, 2.2F, WIND, p).vec(look);
                }
                if (t >= 9 && t <= 14) {
                    Vec3 look = p.getLookAngle();
                    double dist = (t - 8) * 1.9;
                    Vec3 front = v.add(look.scale(dist));
                    gust(p, front, look, 1.6 + dist * 0.25, 0.9, 0, 6);
                    for (int i = 0; i < 14; i++) {
                        Vec3 s = v.add(look.scale(l.random.nextDouble() * dist)).add(l.random.nextGaussian() * dist * 0.2, l.random.nextGaussian() * dist * 0.15, l.random.nextGaussian() * dist * 0.2);
                        l.sendParticles(i % 3 == 0 ? ParticleTypes.POOF : ParticleTypes.CLOUD, s.x, s.y, s.z, 0, look.x, look.y, look.z, 0.9);
                    }
                    l.sendParticles(ParticleTypes.SWEEP_ATTACK, front.x, front.y, front.z, 1, 0.3, 0.3, 0.3, 0);
                }
            }
            case 1 -> {
                // Tornado: spin up a twister between the flippers and send it rolling forward
                Vec3 v = vents(p);
                if (t < 14) swirl(l, v.add(0, -0.4, 0), 0.9, 1.4, t, 6);
                if (t == 14) {
                    Vec3 dir = Fx.flatLook(p);
                    Vec3 at = p.position().add(dir.scale(2.2));
                    at = new Vec3(at.x, Fx.groundY(l, at.x, p.getY() + 1, at.z), at.z);
                    FxEntity fx = FxEntity.spawn(l, at, FxEntity.TORNADO, 140, 1.5F, WIND, p);
                    twisters.computeIfAbsent(p.getUUID(), u -> new ArrayList<>()).add(new Twister(fx, at, dir, 140));
                    Fx.sound(l, at, SoundEvents.ELYTRA_FLYING, 1.6F, 0.8F);
                    Fx.sound(l, at, ModRegistry.S_WHOOSH.get(), 1.4F, 0.6F);
                }
            }
            case 2 -> {
                // Propeller Spin: head and legs pulled in, flippers spinning like blades, flying where you look
                Map<Integer, Integer> lh = lastHit.computeIfAbsent(p.getUUID(), u -> new HashMap<>());
                Vec3 c = p.position().add(0, 0.9, 0);
                for (LivingEntity e : Fx.around(p, c, 2.3)) {
                    Integer last = lh.get(e.getId());
                    if (last != null && p.tickCount - last < 8) continue;
                    lh.put(e.getId(), p.tickCount);
                    Fx.hit(p, e, 6F);
                    Vec3 out = e.position().subtract(p.position());
                    Vec3 tan = new Vec3(-out.z, 0, out.x).normalize();
                    Fx.push(e, new Vec3(out.x, 0, out.z).normalize().scale(1.1).add(tan.scale(0.6)).add(0, 0.5, 0));
                    Fx.sound(l, e.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.1F, 0.7F);
                    l.sendParticles(ParticleTypes.SWEEP_ATTACK, e.getX(), e.getY() + 1, e.getZ(), 2, 0.3, 0.3, 0.3, 0);
                }
                if (t % 2 == 0) gust(p, c, p.getLookAngle(), 2.6, 0.35, 0, 3);
                swirl(l, p.position().add(0, 0.3, 0), 1.6, 1.2, t, 4);
                if (t % 6 == 0) Fx.sound(l, p.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 0.8F, 0.5F + (t % 12) * 0.03F);
                if (t % 16 == 0) Fx.sound(l, p.position(), SoundEvents.ELYTRA_FLYING, 0.7F, 1.2F);
                p.fallDistance = 0;
            }
            case 3 -> {
                // Hurricane: rise up and spin a giant vortex that sucks everything in, then burst it outwards
                Vec3 c = p.position();
                double gy = Fx.groundY(l, c.x, c.y + 0.5, c.z);
                Vec3 g = new Vec3(c.x, gy, c.z);
                p.fallDistance = 0;
                if (t < 64) {
                    for (LivingEntity e : Fx.around(p, g.add(0, 3, 0), 13)) {
                        Vec3 to = g.subtract(e.position());
                        double dist = Math.max(0.5, to.horizontalDistance());
                        Vec3 in = new Vec3(to.x, 0, to.z).normalize();
                        Vec3 tan = new Vec3(-in.z, 0, in.x);
                        double up = dist < 4 ? 0.2 : 0.05;
                        if (e.getY() > gy + 6) up = 0.0;
                        Fx.push(e, in.scale(dist > 3 ? 0.32 : 0.02).add(tan.scale(0.55)).add(0, up, 0));
                        e.fallDistance = 0;
                        e.clearFire();
                        if (t % 10 == 0) Fx.hit(p, e, 3F);
                    }
                    for (Entity e : l.getEntities(p, new AABB(g, g).inflate(13, 8, 13))) {
                        if (e instanceof Projectile pr && pr.getOwner() != p) {
                            Vec3 out = e.position().subtract(g);
                            e.setDeltaMovement(new Vec3(-out.z, 0.2, out.x).normalize().scale(0.8));
                            e.hurtMarked = true;
                        } else if (e instanceof ItemEntity || e instanceof DebrisEntity) {
                            Vec3 to = g.subtract(e.position());
                            Vec3 in = new Vec3(to.x, 0, to.z).normalize();
                            e.setDeltaMovement(in.scale(0.2).add(-in.z * 0.5, 0.12, in.x * 0.5));
                            e.hurtMarked = true;
                        }
                    }
                    if (Fx.grief() && t % 3 == 0) {
                        int n = 0;
                        for (int i = 0; i < 30 && n < 4; i++) {
                            double a = l.random.nextDouble() * Math.PI * 2, rr = 2 + l.random.nextDouble() * 10;
                            double x = g.x + Math.cos(a) * rr, z = g.z + Math.sin(a) * rr;
                            for (int dy = -1; dy <= 4; dy++) {
                                BlockPos bp = BlockPos.containing(x, gy + dy, z);
                                if (blowBlock(l, bp, new Vec3(-Math.sin(a) * 0.5, 0.3, Math.cos(a) * 0.5))) {
                                    n++;
                                    break;
                                }
                            }
                        }
                    }
                    // the vortex: rings of dust and cloud
                    for (int k = 0; k < 3; k++) {
                        double rr = 3 + k * 3.5;
                        for (int i = 0; i < 6; i++) {
                            double a = t * (0.35 - k * 0.07) + i * Math.PI / 3 + k;
                            double y = gy + 0.2 + l.random.nextDouble() * (1.5 + k * 1.5);
                            l.sendParticles(ParticleTypes.CLOUD, g.x + Math.cos(a) * rr, y, g.z + Math.sin(a) * rr, 0, -Math.sin(a), 0.1, Math.cos(a), 0.6);
                        }
                    }
                    if (t % 12 == 0) {
                        Fx.sound(l, c, SoundEvents.ELYTRA_FLYING, 1.4F, 0.5F);
                        Net.shake(p, 20, 0.25F);
                    }
                }
                if (t == 64) {
                    Fx.sound(l, c, SoundEvents.GENERIC_EXPLODE, 1.4F, 1.3F);
                    Fx.sound(l, c, ModRegistry.S_WHOOSH.get(), 1.6F, 0.5F);
                    for (LivingEntity e : Fx.around(p, g.add(0, 3, 0), 14)) {
                        Fx.hit(p, e, 10F);
                        Fx.knock(e, g, 2.4, 0.9);
                    }
                    blowLoose(p, g.add(0, 2, 0), 14, Vec3.ZERO);
                    FxEntity.spawn(l, g.add(0, 0.08, 0), FxEntity.RING, 14, 16F, WIND, p);
                    FxEntity.spawn(l, c.add(0, 1, 0), FxEntity.SONIC_RING, 12, 6F, WIND, p).vec(new Vec3(0, 1, 0));
                    l.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 0.5, c.z, 4, 1.5, 0.5, 1.5, 0);
                    Net.shake(p, 30, 0.9F);
                }
            }
            default -> {}
        }
    }

    @Override
    public void end(ServerPlayer p, OmniData d, int slot) {
        lastHit.remove(p.getUUID());
    }

    @Override
    public void passive(ServerPlayer p, OmniData d) {
        ServerLevel l = p.serverLevel();
        // hovering: air blown down through the shell vents
        if (p.getAbilities().flying && !p.onGround() && p.tickCount % 2 == 0) {
            l.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() - 0.1, p.getZ(), 1, 0.25, 0.05, 0.25, 0.02);
            if (p.tickCount % 40 == 0) Fx.sound(l, p.position(), SoundEvents.ELYTRA_FLYING, 0.25F, 1.6F);
        }
        List<Twister> list = twisters.get(p.getUUID());
        if (list == null || list.isEmpty()) return;
        Iterator<Twister> it = list.iterator();
        while (it.hasNext()) {
            Twister tw = it.next();
            tw.age++;
            if (tw.age >= tw.life || !tw.fx.isAlive()) {
                it.remove();
                continue;
            }
            // wander forward, hugging the ground
            Vec3 n = tw.pos.add(tw.dir.scale(0.16));
            double gy = Fx.groundY(l, n.x, n.y + 1.5, n.z);
            if (gy - tw.pos.y > 2.5) tw.dir = tw.dir.yRot((float) Math.PI * 0.6F);
            else n = new Vec3(n.x, gy, n.z);
            tw.dir = tw.dir.yRot((float) (Math.sin(tw.age * 0.07) * 0.03));
            tw.pos = n;
            tw.fx.setPos(n.x, n.y, n.z);
            for (LivingEntity e : Fx.around(p, n.add(0, 1.5, 0), 3.4)) {
                Vec3 to = n.subtract(e.position());
                Vec3 in = new Vec3(to.x, 0, to.z);
                in = in.lengthSqr() < 1e-3 ? Vec3.ZERO : in.normalize();
                Vec3 tan = new Vec3(-in.z, 0, in.x);
                double up = e.getY() < n.y + 4.5 ? 0.24 : -0.02;
                Fx.push(e, in.scale(0.18).add(tan.scale(0.45)).add(tw.dir.scale(0.16)).add(0, up, 0));
                e.fallDistance = 0;
                e.clearFire();
                if (tw.age % 10 == 0) Fx.hit(p, e, 3F);
            }
            for (Entity e : l.getEntities(p, new AABB(n, n).inflate(3, 4, 3))) {
                if (e instanceof ItemEntity || e instanceof DebrisEntity || (e instanceof Projectile pr && pr.getOwner() != p)) {
                    e.setDeltaMovement(new Vec3(n.z - e.getZ(), 0.25, e.getX() - n.x).normalize().scale(0.4));
                    e.hurtMarked = true;
                }
            }
            if (Fx.grief() && tw.age % 2 == 0) {
                for (int i = 0; i < 6; i++) {
                    BlockPos bp = BlockPos.containing(n.x + l.random.nextGaussian() * 1.6, n.y + l.random.nextInt(4) - 0.5, n.z + l.random.nextGaussian() * 1.6);
                    if (blowBlock(l, bp, new Vec3(l.random.nextGaussian() * 0.2, 0.5, l.random.nextGaussian() * 0.2))) break;
                }
            }
            if (tw.age % 3 == 0) {
                BlockState s = l.getBlockState(BlockPos.containing(n.x, n.y - 0.5, n.z));
                if (!s.isAir()) l.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, s), n.x, n.y + 0.2, n.z, 6, 0.8, 0.1, 0.8, 0.1);
            }
            if (tw.age % 20 == 0) Fx.sound(l, n, SoundEvents.ELYTRA_FLYING, 0.9F, 0.9F);
        }
    }

    @Override
    public void common(Player p, OmniData d) {
        if (!p.isControlledByLocalInstance()) return;
        Vec3 v = p.getDeltaMovement();
        if (d.ability == 2 && d.abilityTick > 3) {
            // propeller flight: fly where you look
            Vec3 look = p.getLookAngle();
            Vec3 want = look.scale(0.95);
            p.setDeltaMovement(v.add(want.subtract(v).scale(0.35)));
            p.fallDistance = 0;
        } else if (d.ability == 3) {
            // hurricane: rise about four blocks and hold there
            double vy = d.abilityTick < 14 ? 0.32 : d.abilityTick < 64 ? 0.0 : v.y;
            p.setDeltaMovement(v.x * 0.6, vy, v.z * 0.6);
            p.fallDistance = 0;
        } else if (d.ability == 1 && d.abilityTick < 14) {
            p.setDeltaMovement(v.x * 0.5, v.y, v.z * 0.5);
        }
    }

    @Override
    public void melee(ServerPlayer p, OmniData d, LivingEntity target, LivingHurtEvent e) {
        // every flipper slap comes with a gust
        Fx.knock(target, p.position(), 1.2, 0.35);
        Vec3 c = target.position().add(0, target.getBbHeight() * 0.5, 0);
        p.serverLevel().sendParticles(ParticleTypes.CLOUD, c.x, c.y, c.z, 6, 0.3, 0.3, 0.3, 0.08);
    }

    @Override
    public boolean guardCancel(ServerPlayer p, OmniData d, DamageSource src) {
        // the wind barrier blows projectiles away
        if (src.is(DamageTypeTags.IS_PROJECTILE) && src.getDirectEntity() != null) {
            var pr = src.getDirectEntity();
            Vec3 out = pr.position().subtract(p.position()).normalize();
            pr.setDeltaMovement(out.scale(0.9).add(0, 0.4, 0));
            pr.hurtMarked = true;
            Fx.sound(p.serverLevel(), pr.position(), ModRegistry.S_WHOOSH.get(), 0.8F, 1.5F);
            p.serverLevel().sendParticles(ParticleTypes.CLOUD, pr.getX(), pr.getY(), pr.getZ(), 6, 0.2, 0.2, 0.2, 0.05);
            return true;
        }
        return false;
    }

    @Override
    public void guardHit(ServerPlayer p, OmniData d, DamageSource src, LivingEntity attacker) {
        Fx.sound(p.serverLevel(), p.position(), SoundEvents.SHIELD_BLOCK, 1F, 0.5F);
        p.serverLevel().sendParticles(ParticleTypes.CRIT, p.getX(), p.getY() + 0.6, p.getZ(), 8, 0.4, 0.2, 0.4, 0.2);
        if (attacker != null && attacker.distanceTo(p) < 5) {
            Fx.knock(attacker, p.position(), 1.5, 0.45);
            Vec3 c = attacker.position().add(0, 1, 0);
            p.serverLevel().sendParticles(ParticleTypes.CLOUD, c.x, c.y, c.z, 10, 0.3, 0.4, 0.3, 0.1);
        }
    }
}
