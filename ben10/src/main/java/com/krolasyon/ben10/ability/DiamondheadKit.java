package com.krolasyon.ben10.ability;

import com.krolasyon.ben10.ModRegistry;
import com.krolasyon.ben10.OmniData;
import com.krolasyon.ben10.entity.AlienProjectile;
import com.krolasyon.ben10.entity.DebrisEntity;
import com.krolasyon.ben10.entity.FxEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/** Diamondhead: crystal shard volley, ground spikes, crystal wall, crystal storm. Guard = crystal shield that reflects projectiles. */
public class DiamondheadKit extends Kit {
    @Override
    public boolean start(ServerPlayer p, OmniData d, int slot) {
        ServerLevel l = p.serverLevel();
        switch (slot) {
            case 0 -> Fx.sound(l, p.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.5F, 0.8F);
            case 1 -> Fx.sound(l, p.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1.5F, 0.7F);
            case 2 -> Fx.sound(l, p.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1.5F, 1.2F);
            case 3 -> {
                d.vec = Fx.rayEnd(p, 26);
                Fx.sound(l, p.position(), SoundEvents.BEACON_POWER_SELECT, 1.2F, 1.6F);
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
                if (t < 4) Fx.particles(l, Fx.dust(0x9CFFC8, 1F), Fx.rel(p, 0.8, 0.35, 0.4), 4, 0.1, 0.02);
                if (t == 4) {
                    Vec3 from = Fx.rel(p, 0.9, 0.3, 0.35);
                    LivingEntity aim = Fx.aimed(p, 40);
                    Vec3 dir = aim != null ? aim.position().add(0, aim.getBbHeight() / 2, 0).subtract(from).normalize() : p.getLookAngle();
                    for (int i = -2; i <= 2; i++) {
                        Vec3 v = rotY(dir, i * 7.5).add(0, Math.abs(i) * 0.02, 0).normalize();
                        AlienProjectile s = AlienProjectile.shoot(p, AlienProjectile.CRYSTAL, from, v.scale(2.4), 5.5F);
                        s.pierce = 1;
                        s.gravity = 0.004F;
                        s.life = 40;
                    }
                    Fx.sound(l, from, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.2F, 1.6F);
                    Fx.sound(l, from, SoundEvents.ARROW_SHOOT, 1F, 0.7F);
                }
            }
            case 1 -> {
                if (t >= 6 && t <= 20 && (t - 6) % 2 == 0) {
                    int i = (t - 6) / 2;
                    Vec3 dir = Fx.flatLook(p);
                    Vec3 at = p.position().add(dir.scale(1.8 + i * 1.6)).add(rotY(dir, 90).scale(Mth.sin(i * 1.7F) * 0.5));
                    double gy = Fx.groundY(l, at.x, at.y + 2, at.z);
                    Vec3 g = new Vec3(at.x, gy, at.z);
                    FxEntity.spawn(l, g, FxEntity.SPIKE, 50, 0.9F + i * 0.1F, 0x9CFFC8, p).yaw(p.getYRot() + i * 47);
                    Fx.sound(l, g, SoundEvents.AMETHYST_CLUSTER_PLACE, 1.4F, 0.6F + i * 0.06F);
                    BlockPos bp = BlockPos.containing(g).below();
                    var s = l.getBlockState(bp);
                    if (!s.isAir()) {
                        Fx.particles(l, new BlockParticleOption(ParticleTypes.BLOCK, s), g, 14, 0.4, 0.15);
                        for (int k = 0; k < 2; k++) DebrisEntity.spawn(l, g.add(0, 0.3, 0), s, new Vec3(l.random.nextGaussian() * 0.15, 0.4, l.random.nextGaussian() * 0.15));
                    }
                    for (LivingEntity e : Fx.around(p, g.add(0, 0.8, 0), 1.5 + i * 0.05)) {
                        Fx.hit(p, e, 9F);
                        Fx.push(e, new Vec3(e.getDeltaMovement().x * 0.3, 0.75, e.getDeltaMovement().z * 0.3));
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                    }
                }
            }
            case 2 -> {
                if (t == 6) wall(p, l);
            }
            case 3 -> {
                Vec3 c = d.vec;
                if (t < 10) {
                    Fx.particles(l, Fx.dust(0x9CFFC8, 1.6F), p.position().add(0, p.getBbHeight() + 0.5, 0), 6, 0.4, 0.05);
                    Fx.ring(l, Fx.dust(0x9CFFC8, 1.2F), c.add(0, 0.2, 0), 4.5, 30, 0);
                }
                if (t >= 10 && t <= 46) {
                    for (int k = 0; k < 2; k++) {
                        double a = l.random.nextDouble() * Math.PI * 2, r = Math.sqrt(l.random.nextDouble()) * 4.8;
                        Vec3 at = c.add(Math.cos(a) * r, 13 + l.random.nextDouble() * 3, Math.sin(a) * r);
                        AlienProjectile s = AlienProjectile.shoot(p, AlienProjectile.SHARD_FALL, at,
                                new Vec3(l.random.nextGaussian() * 0.05, -1.7, l.random.nextGaussian() * 0.05), 6.5F);
                        s.setScale(1.5F);
                        s.gravity = 0.03F;
                        s.life = 40;
                    }
                    if (t % 4 == 0) Fx.sound(l, c, SoundEvents.AMETHYST_BLOCK_CHIME, 1.5F, 0.6F + l.random.nextFloat() * 0.6F);
                }
            }
            default -> {}
        }
    }

    private static Vec3 rotY(Vec3 v, double deg) {
        double r = Math.toRadians(deg), c = Math.cos(r), s = Math.sin(r);
        return new Vec3(v.x * c - v.z * s, v.y, v.x * s + v.z * c);
    }

    private void wall(ServerPlayer p, ServerLevel l) {
        Vec3 dir = Fx.flatLook(p);
        Vec3 side = rotY(dir, 90);
        Vec3 c = p.position().add(dir.scale(3.0));
        double gy = Fx.groundY(l, c.x, c.y + 1.5, c.z);
        int placed = 0;
        for (int w = -3; w <= 3; w++) {
            for (int h = 0; h < 4; h++) {
                if (Math.abs(w) == 3 && h == 3) continue;
                Vec3 at = c.add(side.scale(w)).add(0, 0, 0);
                BlockPos bp = BlockPos.containing(at.x, gy + h + 0.1, at.z);
                if (Fx.temp(l, bp, ModRegistry.CRYSTAL.get())) placed++;
                if (h == 0) Fx.particles(l, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.EMERALD_BLOCK.defaultBlockState()), Vec3.atCenterOf(bp), 6, 0.4, 0.1);
            }
            FxEntity.spawn(l, new Vec3(c.add(side.scale(w)).x, gy, c.add(side.scale(w)).z), FxEntity.SPIKE, 30, 0.7F, 0x9CFFC8, p).yaw(w * 61);
        }
        // shove enemies out of the wall
        for (Entity e : l.getEntities(p, new AABB(c, c).inflate(3.5, 3, 3.5), en -> en instanceof LivingEntity && Fx.valid(p, en))) {
            Fx.knock((LivingEntity) e, p.position(), 1.2, 0.5);
            Fx.hit(p, (LivingEntity) e, 4F);
        }
        Fx.sound(l, c, SoundEvents.AMETHYST_BLOCK_BREAK, 2F, 0.5F);
        Fx.sound(l, c, SoundEvents.AMETHYST_CLUSTER_PLACE, 2F, 0.6F);
        com.krolasyon.ben10.net.Net.shake(p, 12, 0.3F);
    }

    @Override
    public void passive(ServerPlayer p, OmniData d) {
        if (p.tickCount % 12 == 0) {
            p.serverLevel().sendParticles(Fx.dust(0xC8FFE8, 0.7F), p.getX(), p.getY() + 2.1, p.getZ(), 1, 0.3, 0.2, 0.3, 0);
        }
    }

    @Override
    public void melee(ServerPlayer p, OmniData d, LivingEntity target, LivingHurtEvent e) {
        e.setAmount(e.getAmount() + 2F);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
        Fx.particles(p.serverLevel(), new BlockParticleOption(ParticleTypes.BLOCK, Blocks.EMERALD_BLOCK.defaultBlockState()),
                target.position().add(0, target.getBbHeight() / 2, 0), 10, 0.2, 0.1);
        Fx.sound(p.serverLevel(), target.position(), SoundEvents.AMETHYST_BLOCK_HIT, 1F, 1.2F);
    }

    @Override
    public boolean guardCancel(ServerPlayer p, OmniData d, DamageSource src) {
        if (src.is(DamageTypeTags.IS_PROJECTILE) && src.getDirectEntity() != null) {
            Entity pr = src.getDirectEntity();
            Entity shooter = src.getEntity();
            Vec3 from = pr.position();
            Vec3 dir = shooter != null ? shooter.position().add(0, shooter.getBbHeight() / 2, 0).subtract(from).normalize() : pr.getDeltaMovement().scale(-1).normalize();
            pr.discard();
            AlienProjectile s = AlienProjectile.shoot(p, AlienProjectile.REFLECT, from.add(dir.scale(0.6)), dir.scale(2.4), 7F);
            s.life = 40;
            Fx.sound(p.serverLevel(), from, SoundEvents.AMETHYST_BLOCK_HIT, 1.4F, 1.6F);
            Fx.particles(p.serverLevel(), Fx.dust(0x9CFFC8, 1.2F), from, 10, 0.2, 0.05);
            return true;
        }
        return false;
    }

    @Override
    public void guardHit(ServerPlayer p, OmniData d, DamageSource src, LivingEntity attacker) {
        if (attacker != null && attacker.distanceTo(p) < 4) {
            Fx.hit(p, attacker, 3F);
            Fx.knock(attacker, p.position(), 0.5, 0.2);
        }
        Fx.sound(p.serverLevel(), p.position(), SoundEvents.AMETHYST_BLOCK_HIT, 1.2F, 0.8F);
    }
}
