package com.krolasyon.bosses.realm.item;

import com.krolasyon.bosses.entity.EruptionEntity;
import com.krolasyon.bosses.realm.Allegiance;
import com.krolasyon.bosses.realm.entity.Abilities;
import com.krolasyon.bosses.realm.entity.Ability.Element;
import com.krolasyon.bosses.realm.entity.RealmEntities;
import com.krolasyon.bosses.realm.entity.RealmMob;
import com.krolasyon.bosses.realm.entity.SpellProjectile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/** Player spells shared by the tomes and the legendary weapons. */
public final class Spells {
    private Spells() {}

    public enum Spell {
        FIREBALL(15, 20), METEOR(45, 120), BLOOD_LANCE(20, 25), SHADOW_STEP(15, 20), SOUL_SHIELD(35, 400), CHAIN_LIGHTNING(30, 50),
        LAVA_WAVE(30, 60), SUMMON_IMPS(50, 600), LIFE_DRAIN(25, 40), FEAR(30, 200),
        ASH_NOVA(30, 60), EARTHSHATTER(25, 80), SOUL_WAVE(25, 60), METEOR_STRIKE(40, 100);

        public final int mana, cooldown;

        Spell(int mana, int cooldown) {
            this.mana = mana;
            this.cooldown = cooldown;
        }
    }

    static void sound(ServerPlayer p, SoundEvent s, float v, float pitch) {
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), s, SoundSource.PLAYERS, v, pitch);
    }

    static Vec3 eye(ServerPlayer p) { return p.getEyePosition().add(p.getLookAngle().scale(0.6)).subtract(0, 0.15, 0); }

    static Vec3 lookPoint(ServerPlayer p, double range) {
        Vec3 from = p.getEyePosition(), to = from.add(p.getLookAngle().scale(range));
        BlockHitResult r = p.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, p));
        return r.getType() == HitResult.Type.MISS ? to : r.getLocation();
    }

    @Nullable
    static LivingEntity lookEntity(ServerPlayer p, double range) {
        Vec3 from = p.getEyePosition(), to = from.add(p.getLookAngle().scale(range));
        Vec3 blocked = lookPoint(p, range);
        EntityHitResult r = ProjectileUtil.getEntityHitResult(p, from, blocked, new AABB(from, to).inflate(1.5),
                e -> e instanceof LivingEntity le && Allegiance.playerMayHit(p, le), range * range);
        return r != null && r.getEntity() instanceof LivingEntity le ? le : null;
    }

    public static boolean cast(ServerPlayer p, Spell s) {
        ServerLevel sl = p.serverLevel();
        switch (s) {
            case FIREBALL -> {
                SpellProjectile.shoot(p, eye(p), p.getLookAngle().scale(1.4), Element.FIRE, 9F, 0F, 2.5F, false);
                sound(p, SoundEvents.BLAZE_SHOOT, 1F, 1F);
            }
            case METEOR, METEOR_STRIKE -> {
                Vec3 c = lookPoint(p, 40);
                int n = s == Spell.METEOR ? 9 : 6;
                for (int i = 0; i < n; i++) {
                    Vec3 land = c.add(p.getRandom().nextGaussian() * 3, 0, p.getRandom().nextGaussian() * 3);
                    Vec3 from = land.add(p.getRandom().nextGaussian() * 4, 22 + i * 3, p.getRandom().nextGaussian() * 4);
                    SpellProjectile m = SpellProjectile.shoot(p, from, land.subtract(from).normalize().scale(1.3), Element.FIRE, s == Spell.METEOR ? 11F : 14F, 0.01F, 3.2F, false);
                    m.setBig(true);
                }
                sound(p, SoundEvents.WITHER_SHOOT, 1.2F, 0.6F);
            }
            case BLOOD_LANCE -> {
                SpellProjectile.shoot(p, eye(p), p.getLookAngle().scale(2.0), Element.BLOOD, 11F, 0F, 0F, true);
                sound(p, SoundEvents.TRIDENT_THROW, 1.2F, 0.7F);
            }
            case SHADOW_STEP -> {
                Vec3 to = lookPoint(p, 14);
                Vec3 dir = p.getLookAngle();
                Vec3 dest = to.subtract(dir.scale(0.8));
                BlockPos bp = BlockPos.containing(dest);
                for (int dy = 0; dy < 4; dy++) {
                    AABB box = p.getDimensions(p.getPose()).makeBoundingBox(bp.getX() + 0.5, bp.getY() + dy, bp.getZ() + 0.5);
                    if (sl.noCollision(p, box)) {
                        sl.sendParticles(ParticleTypes.REVERSE_PORTAL, p.getX(), p.getY() + 1, p.getZ(), 40, 0.3, 0.7, 0.3, 0.05);
                        sl.sendParticles(ParticleTypes.LARGE_SMOKE, p.getX(), p.getY() + 1, p.getZ(), 10, 0.3, 0.6, 0.3, 0.01);
                        p.teleportTo(bp.getX() + 0.5, bp.getY() + dy, bp.getZ() + 0.5);
                        p.fallDistance = 0;
                        sl.sendParticles(ParticleTypes.REVERSE_PORTAL, p.getX(), p.getY() + 1, p.getZ(), 40, 0.3, 0.7, 0.3, 0.05);
                        sound(p, SoundEvents.ENDERMAN_TELEPORT, 1F, 1.2F);
                        return true;
                    }
                }
                return false;
            }
            case SOUL_SHIELD -> {
                p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 2));
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 0));
                for (int k = 0; k < 3; k++) Abilities.ring(sl, p.position().add(0, k * 0.6, 0), 1.2, ParticleTypes.SOUL_FIRE_FLAME, 20, 0);
                sound(p, SoundEvents.BEACON_ACTIVATE, 1F, 1.5F);
                sound(p, SoundEvents.SOUL_ESCAPE, 1.5F, 0.8F);
            }
            case CHAIN_LIGHTNING -> {
                LivingEntity first = lookEntity(p, 18);
                if (first == null) {
                    List<LivingEntity> near = sl.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(12), e -> Allegiance.playerMayHit(p, e) && p.hasLineOfSight(e));
                    if (near.isEmpty()) return false;
                    near.sort((a, b) -> Double.compare(a.distanceToSqr(p), b.distanceToSqr(p)));
                    first = near.get(0);
                }
                List<LivingEntity> chain = new ArrayList<>();
                LivingEntity cur = first;
                Vec3 from = eye(p);
                for (int i = 0; i < 6 && cur != null; i++) {
                    chain.add(cur);
                    Vec3 to = cur.position().add(0, cur.getBbHeight() * 0.6, 0);
                    zigzag(sl, from, to);
                    cur.hurt(p.damageSources().indirectMagic(p, p), 9F - i);
                    final LivingEntity last = cur;
                    from = to;
                    cur = sl.getEntitiesOfClass(LivingEntity.class, last.getBoundingBox().inflate(7), e -> Allegiance.playerMayHit(p, e) && !chain.contains(e))
                            .stream().min((a, b) -> Double.compare(a.distanceToSqr(last), b.distanceToSqr(last))).orElse(null);
                }
                sound(p, SoundEvents.LIGHTNING_BOLT_IMPACT, 1.0F, 1.6F);
            }
            case LAVA_WAVE -> {
                Vec3 dir = p.getLookAngle().multiply(1, 0, 1).normalize();
                for (int i = 0; i < 13; i++) {
                    Vec3 q = p.position().add(dir.scale(1.6 + i * 1.2));
                    EruptionEntity.spawn(p, q.x, q.z, p.getY(), EruptionEntity.KIND_FIRE, 1 + i, 9F);
                }
                sound(p, SoundEvents.EVOKER_PREPARE_ATTACK, 1F, 0.7F);
            }
            case SUMMON_IMPS -> {
                EntityType<? extends Mob> t = RealmEntities.type("infernal_imp");
                if (t == null) return false;
                for (int i = 0; i < 3; i++) {
                    Mob m = t.create(sl);
                    if (!(m instanceof RealmMob rm)) continue;
                    double a = i * Math.PI * 2 / 3;
                    rm.moveTo(p.getX() + Math.cos(a) * 2, p.getY(), p.getZ() + Math.sin(a) * 2, p.getYRot(), 0);
                    rm.finalizeSpawn(sl, sl.getCurrentDifficultyAt(rm.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
                    rm.makeAlly(p, 1200);
                    sl.addFreshEntity(rm);
                    sl.sendParticles(ParticleTypes.FLAME, rm.getX(), rm.getY() + 0.5, rm.getZ(), 20, 0.3, 0.5, 0.3, 0.05);
                }
                sound(p, SoundEvents.EVOKER_PREPARE_SUMMON, 1F, 1.2F);
            }
            case LIFE_DRAIN -> {
                LivingEntity t = lookEntity(p, 16);
                if (t == null) return false;
                if (t.hurt(p.damageSources().indirectMagic(p, p), 8F)) p.heal(6F);
                Abilities.line(sl, t.position().add(0, t.getBbHeight() * 0.6, 0), eye(p), Abilities.dust(0xC01030, 1.3F), 0.35);
                sl.sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 2.1, p.getZ(), 3, 0.3, 0.1, 0.3, 0);
                sound(p, SoundEvents.WARDEN_HEARTBEAT, 1.5F, 1.4F);
            }
            case FEAR -> {
                for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(10), e -> Allegiance.playerMayHit(p, e))) {
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 1), p);
                    e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 1), p);
                    e.hurt(p.damageSources().indirectMagic(p, p), 3F);
                    if (e instanceof Mob m) {
                        m.setTarget(null);
                        Vec3 away = m.position().subtract(p.position()).normalize().scale(10).add(m.position());
                        m.getNavigation().moveTo(away.x, away.y, away.z, 1.5);
                    }
                    sl.sendParticles(ParticleTypes.SCULK_SOUL, e.getX(), e.getY() + e.getBbHeight() + 0.2, e.getZ(), 3, 0.2, 0.1, 0.2, 0.02);
                }
                Abilities.ring(sl, p.position(), 5, ParticleTypes.SCULK_SOUL, 40, 0);
                sound(p, SoundEvents.WARDEN_ROAR, 1.2F, 1.5F);
            }
            case ASH_NOVA -> {
                for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(5.5), e -> Allegiance.playerMayHit(p, e))) {
                    e.hurt(p.damageSources().playerAttack(p), 11F);
                    e.setSecondsOnFire(6);
                    Vec3 k = e.position().subtract(p.position()).multiply(1, 0, 1).normalize();
                    e.setDeltaMovement(k.x * 1.1, 0.45, k.z * 1.1);
                    e.hurtMarked = true;
                }
                for (int r = 1; r <= 5; r++) Abilities.ring(sl, p.position(), r, r % 2 == 0 ? ParticleTypes.FLAME : ParticleTypes.WHITE_ASH, r * 10, 0.1);
                sl.sendParticles(ParticleTypes.EXPLOSION, p.getX(), p.getY() + 0.5, p.getZ(), 2, 0.5, 0.2, 0.5, 0);
                sound(p, SoundEvents.GENERIC_EXPLODE, 1.2F, 0.8F);
            }
            case EARTHSHATTER -> {
                for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(6, 2, 6), e -> Allegiance.playerMayHit(p, e))) {
                    e.hurt(p.damageSources().playerAttack(p), 12F);
                    e.setDeltaMovement(e.getDeltaMovement().add(0, 0.95, 0));
                    e.hurtMarked = true;
                }
                for (int r = 1; r <= 6; r++) Abilities.ring(sl, p.position(), r, Abilities.dust(0x8A8A90, 2.0F), r * 9, 0);
                sl.sendParticles(ParticleTypes.EXPLOSION, p.getX(), p.getY() + 0.2, p.getZ(), 3, 1, 0.1, 1, 0);
                sound(p, SoundEvents.ANVIL_LAND, 1.0F, 0.5F);
                sound(p, SoundEvents.GENERIC_EXPLODE, 1.0F, 0.6F);
            }
            case SOUL_WAVE -> {
                for (int i = 0; i < 14; i++) {
                    double a = i * Math.PI * 2 / 14;
                    Vec3 d = new Vec3(Math.cos(a), 0.02, Math.sin(a));
                    SpellProjectile.shoot(p, p.position().add(0, 1.1, 0).add(d), d.scale(0.9), Element.SOUL, 8F, 0F, 0F, true);
                }
                sound(p, SoundEvents.SOUL_ESCAPE, 2F, 0.6F);
            }
        }
        return true;
    }

    static void zigzag(ServerLevel sl, Vec3 a, Vec3 b) {
        Vec3 d = b.subtract(a);
        int n = (int) Math.max(3, d.length() * 2);
        Vec3 prev = a;
        for (int i = 1; i <= n; i++) {
            Vec3 q = a.add(d.scale(i / (double) n));
            if (i < n) q = q.add(sl.random.nextGaussian() * 0.25, sl.random.nextGaussian() * 0.25, sl.random.nextGaussian() * 0.25);
            Abilities.line(sl, prev, q, Abilities.dust(0xB0D0FF, 0.9F), 0.25);
            prev = q;
        }
        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, b.x, b.y, b.z, 10, 0.2, 0.3, 0.2, 0.2);
    }
}
