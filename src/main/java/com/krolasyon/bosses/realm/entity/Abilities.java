package com.krolasyon.bosses.realm.entity;

import com.krolasyon.bosses.entity.BossEntity;
import com.krolasyon.bosses.entity.EruptionEntity;
import com.krolasyon.bosses.realm.Allegiance;
import com.krolasyon.bosses.realm.entity.Ability.Element;
import com.krolasyon.bosses.realm.entity.Ability.Type;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** The shared attack engine of every realm creature, lord and the Tyrant. */
public final class Abilities {
    private Abilities() {}

    public static class Scratch {
        public final Set<Integer> hit = new HashSet<>();
        public Vec3 dir = Vec3.ZERO;
        public Vec3 point = Vec3.ZERO;
        public boolean flag;

        public void reset() {
            hit.clear();
            dir = Vec3.ZERO;
            point = Vec3.ZERO;
            flag = false;
        }
    }

    // ------------------------------------------------------------------ helpers
    public static ParticleOptions dust(int rgb, float size) {
        return new DustParticleOptions(new Vector3f(((rgb >> 16) & 255) / 255F, ((rgb >> 8) & 255) / 255F, (rgb & 255) / 255F), size);
    }

    public static ParticleOptions particle(Element el) {
        return switch (el) {
            case FIRE -> ParticleTypes.FLAME;
            case SOUL -> ParticleTypes.SOUL_FIRE_FLAME;
            case SHADOW -> ParticleTypes.REVERSE_PORTAL;
            case BRIMSTONE -> ParticleTypes.LAVA;
            case CRYSTAL -> ParticleTypes.END_ROD;
            case POISON -> ParticleTypes.ITEM_SLIME;
            case BONE -> ParticleTypes.WHITE_ASH;
            case BLOOD -> ParticleTypes.DAMAGE_INDICATOR;
        };
    }

    static boolean boss(Mob m) { return m instanceof BossEntity; }

    static boolean phase2(Mob m) { return m instanceof BossEntity b && b.isPhase2(); }

    public static float dmg(Mob m, Ability a) {
        return (float) (m.getAttributeValue(Attributes.ATTACK_DAMAGE) * a.power() * (phase2(m) ? 1.2 : 1.0));
    }

    public static boolean hostile(Mob m, Entity e) { return Allegiance.hostile(m, e); }

    public static Vec3 forward(Entity m) {
        float yaw = (m instanceof LivingEntity le ? le.yBodyRot : m.getYRot()) * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
    }

    public static void face(Mob m, Entity t) {
        double dx = t.getX() - m.getX(), dz = t.getZ() - m.getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * (180F / Math.PI)) - 90F;
        m.setYRot(yaw);
        m.yBodyRot = yaw;
        m.yHeadRot = yaw;
        m.getLookControl().setLookAt(t, 60F, 60F);
    }

    static void sound(Mob m, SoundEvent s, float vol, float pitch) {
        m.level().playSound(null, m.getX(), m.getY(), m.getZ(), s, SoundSource.HOSTILE, vol, pitch);
    }

    static void soundAt(Level l, Vec3 p, SoundEvent s, float vol, float pitch) {
        l.playSound(null, p.x, p.y, p.z, s, SoundSource.HOSTILE, vol, pitch);
    }

    static DamageSource source(Mob m, Element el) {
        return switch (el) {
            case SHADOW, SOUL, CRYSTAL -> m.damageSources().indirectMagic(m, m);
            default -> m.damageSources().mobAttack(m);
        };
    }

    /** damage + knockback + elemental effect */
    public static boolean strike(Mob m, LivingEntity e, float amount, Element el, double knock, double lift) {
        if (!e.hurt(source(m, el), amount)) return false;
        Vec3 d = e.position().subtract(m.position()).multiply(1, 0, 1);
        if (d.lengthSqr() < 1.0E-4) d = forward(m);
        d = d.normalize();
        double kr = 1.0 - e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) * 0.7;
        e.setDeltaMovement(e.getDeltaMovement().add(d.x * knock * kr, lift * kr, d.z * knock * kr));
        e.hurtMarked = true;
        applyElement(m, e, el, amount, 1);
        return true;
    }

    public static void applyElement(@Nullable LivingEntity src, LivingEntity e, Element el, float amount, int strength) {
        switch (el) {
            case FIRE -> e.setSecondsOnFire(3 + strength * 2);
            case BLOOD -> { if (src != null) src.heal(amount * 0.3F); }
            case SHADOW -> e.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40 + strength * 20, 0), src);
            case SOUL -> e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50 + strength * 20, strength), src);
            case POISON -> e.addEffect(new MobEffectInstance(MobEffects.POISON, 60 + strength * 30, 0), src);
            case BONE -> e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60 + strength * 20, 0), src);
            case BRIMSTONE -> e.setSecondsOnFire(2);
            default -> {}
        }
    }

    public static List<LivingEntity> hostilesAround(Mob m, Vec3 c, double r) {
        return m.level().getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r, r * 0.8 + 1, r),
                e -> hostile(m, e) && e.position().distanceToSqr(c) <= r * r * 1.15);
    }

    public static void ring(ServerLevel sl, Vec3 c, double r, ParticleOptions p, int n, double vy) {
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n;
            sl.sendParticles(p, c.x + Math.cos(a) * r, c.y + 0.15, c.z + Math.sin(a) * r, 1, 0.05, 0.05, 0.05, 0);
            if (vy > 0) sl.sendParticles(p, c.x + Math.cos(a) * r * 0.5, c.y + 0.2, c.z + Math.sin(a) * r * 0.5, 0, Math.cos(a), vy, Math.sin(a), 0.3);
        }
    }

    public static void line(ServerLevel sl, Vec3 a, Vec3 b, ParticleOptions p, double step) {
        Vec3 d = b.subtract(a);
        int n = (int) Math.max(1, d.length() / step);
        for (int i = 0; i <= n; i++) {
            Vec3 q = a.add(d.scale(i / (double) n));
            sl.sendParticles(p, q.x, q.y, q.z, 1, 0.03, 0.03, 0.03, 0);
        }
    }

    public static void burst(Mob m, Element el, float r, int count) {
        if (!(m.level() instanceof ServerLevel sl)) return;
        Vec3 c = m.position().add(0, m.getBbHeight() * 0.5, 0);
        sl.sendParticles(particle(el), c.x, c.y, c.z, count, r * 0.3, m.getBbHeight() * 0.3, r * 0.3, 0.15);
        sl.sendParticles(dust(el.color, 2.0F), c.x, c.y, c.z, count, r * 0.4, m.getBbHeight() * 0.35, r * 0.4, 0);
        sl.sendParticles(dust(el.light, 1.2F), c.x, c.y, c.z, count / 2, r * 0.4, m.getBbHeight() * 0.35, r * 0.4, 0);
    }

    static Vec3 eye(Mob m) { return m.position().add(0, m.getBbHeight() * 0.75, 0); }

    static Vec3 aim(LivingEntity t) { return t.position().add(0, t.getBbHeight() * 0.55, 0); }

    // ------------------------------------------------------------------ engine
    public static void tick(Mob m, Ability a, int t, @Nullable LivingEntity target, Scratch s) {
        if (!(m.level() instanceof ServerLevel sl)) return;
        Element el = a.element();
        int hit = a.hit();
        float dmg = dmg(m, a);
        if (t == 0) windup(m, a, sl);
        switch (a.type()) {
            case MELEE -> {
                if (t == hit) {
                    sound(m, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.6F + m.getRandom().nextFloat() * 0.3F);
                    double reach = a.maxRange() + 0.6;
                    Vec3 f = forward(m);
                    boolean any = false;
                    for (LivingEntity e : hostilesAround(m, m.position(), reach + 1)) {
                        Vec3 to = e.position().subtract(m.position()).multiply(1, 0, 1);
                        double dist = to.length() - e.getBbWidth() * 0.5;
                        if (dist > reach) continue;
                        if (to.lengthSqr() > 0.5 && to.normalize().dot(f) < 0.15) continue;
                        any |= strike(m, e, dmg, el, 0.5 + a.radius() * 0.1, 0.15);
                    }
                    if (!any && target != null && m.distanceTo(target) - target.getBbWidth() * 0.5 <= reach && hostile(m, target)) strike(m, target, dmg, el, 0.5, 0.15);
                    Vec3 c = m.position().add(f.scale(Math.min(reach, 2.0))).add(0, m.getBbHeight() * 0.5, 0);
                    sl.sendParticles(ParticleTypes.SWEEP_ATTACK, c.x, c.y, c.z, 1, 0, 0, 0, 0);
                    sl.sendParticles(dust(el.color, 1.3F), c.x, c.y, c.z, 8, 0.6, 0.3, 0.6, 0);
                }
            }
            case LEAP -> {
                if (t == hit && target != null) {
                    Vec3 d = target.position().subtract(m.position());
                    double h = Math.sqrt(d.x * d.x + d.z * d.z);
                    double flight = Mth.clamp(h / 0.9, 8, 18);
                    m.setDeltaMovement(d.x / flight, 0.55 + h * 0.035, d.z / flight);
                    m.hasImpulse = true;
                    s.flag = true;
                    sound(m, SoundEvents.RAVAGER_STEP, 1.2F, 1.2F);
                    sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, m.getX(), m.getY() + 0.1, m.getZ(), 6, 0.4, 0.1, 0.4, 0.02);
                }
                if (s.flag && t > hit + 3 && (m.onGround() || t >= a.duration() - 1)) {
                    s.flag = false;
                    slam(m, sl, m.position(), Math.max(2.5F, a.radius()), dmg, el, 0.6);
                }
            }
            case CHARGE -> {
                if (t == hit) {
                    s.dir = target != null ? target.position().subtract(m.position()).multiply(1, 0, 1).normalize() : forward(m);
                    if (s.dir.lengthSqr() < 0.01) s.dir = forward(m);
                    sound(m, SoundEvents.RAVAGER_ROAR, 1.0F, 1.3F);
                }
                if (t >= hit && t < a.duration() - 3) {
                    double sp = boss(m) ? 0.95 : 0.75;
                    m.setDeltaMovement(s.dir.x * sp, m.getDeltaMovement().y, s.dir.z * sp);
                    float yaw = (float) (Mth.atan2(s.dir.z, s.dir.x) * (180F / Math.PI)) - 90F;
                    m.setYRot(yaw);
                    m.yBodyRot = yaw;
                    for (LivingEntity e : m.level().getEntitiesOfClass(LivingEntity.class, m.getBoundingBox().inflate(0.7), e -> hostile(m, e))) {
                        if (s.hit.add(e.getId())) strike(m, e, dmg, el, 1.6, 0.5);
                    }
                    if (t % 2 == 0) sl.sendParticles(dust(el.color, 1.6F), m.getX(), m.getY() + 0.3, m.getZ(), 4, 0.4, 0.2, 0.4, 0);
                    if (m.horizontalCollision && t > hit + 3) {
                        sound(m, SoundEvents.GENERIC_EXPLODE, 0.8F, 1.3F);
                        sl.sendParticles(ParticleTypes.EXPLOSION, m.getX(), m.getY() + 1, m.getZ(), 1, 0, 0, 0, 0);
                        m.setDeltaMovement(Vec3.ZERO);
                    }
                }
            }
            case BOLT -> {
                int k = t - hit;
                if (k >= 0 && k % 3 == 0 && k / 3 < a.count() && target != null) {
                    Vec3 from = eye(m).add(forward(m).scale(m.getBbWidth() * 0.6));
                    Vec3 dir = aim(target).subtract(from).normalize();
                    double spread = a.count() > 1 ? 0.06 : 0.0;
                    dir = dir.add(m.getRandom().nextGaussian() * spread, m.getRandom().nextGaussian() * spread * 0.5, m.getRandom().nextGaussian() * spread).normalize();
                    SpellProjectile.shoot(m, from, dir.scale(1.05), el, dmg, 0.0F, a.radius(), false);
                    sound(m, el == Element.FIRE || el == Element.BRIMSTONE ? SoundEvents.BLAZE_SHOOT : SoundEvents.EVOKER_CAST_SPELL, 0.9F, 1.1F + m.getRandom().nextFloat() * 0.3F);
                }
            }
            case BOMB -> {
                if (t == hit && target != null) {
                    for (int i = 0; i < Math.max(1, a.count()); i++) {
                        Vec3 from = eye(m);
                        Vec3 to = target.position().add(i == 0 ? 0 : m.getRandom().nextGaussian() * 2.5, 0, i == 0 ? 0 : m.getRandom().nextGaussian() * 2.5);
                        lob(m, from, to, el, dmg, Math.max(2.0F, a.radius()));
                    }
                    sound(m, SoundEvents.GENERIC_EXPLODE, 0.5F, 1.6F);
                    sound(m, SoundEvents.BLAZE_SHOOT, 1.0F, 0.6F);
                }
            }
            case SLAM -> {
                if (t == hit) {
                    Vec3 c = m.position().add(forward(m).scale(Math.min(1.5, m.getBbWidth())));
                    slam(m, sl, c, a.radius(), dmg, el, 0.7);
                }
            }
            case ERUPT -> {
                if (t == hit) {
                    Vec3 dir = target != null ? target.position().subtract(m.position()).multiply(1, 0, 1).normalize() : forward(m);
                    if (dir.lengthSqr() < 0.01) dir = forward(m);
                    int kind = switch (el) {
                        case FIRE, BRIMSTONE -> EruptionEntity.KIND_FIRE;
                        case BLOOD -> EruptionEntity.KIND_BLOOD;
                        case CRYSTAL, SOUL -> EruptionEntity.KIND_CRYSTAL;
                        default -> EruptionEntity.KIND_THORN;
                    };
                    float[] angles = phase2(m) ? new float[]{0F, 0.4F, -0.4F} : (a.radius() > 1 ? new float[]{0F, 0.35F, -0.35F} : new float[]{0F});
                    for (float ang : angles) {
                        Vec3 d = dir.yRot(ang);
                        for (int i = 0; i < a.count(); i++) {
                            Vec3 p = m.position().add(d.scale(1.8 + i * 1.25));
                            EruptionEntity.spawn(m, p.x, p.z, m.getY(), kind, 1 + i, dmg);
                        }
                    }
                    sound(m, SoundEvents.EVOKER_PREPARE_ATTACK, 1.0F, 0.8F);
                }
            }
            case SUMMON -> {
                if (t == hit) summon(m, a, target);
            }
            case BLINK -> {
                if (t == hit) blink(m, target, sl);
            }
            case DRAIN, BEAM -> {
                if (t >= hit && (t - hit) % 5 == 0 && target != null && m.distanceTo(target) <= a.maxRange() + 1 && m.hasLineOfSight(target)) {
                    Vec3 from = eye(m), to = aim(target);
                    line(sl, from, to, dust(el.color, 1.2F), 0.4);
                    line(sl, from, to, dust(el.light, 0.8F), 0.9);
                    if (target.hurt(m.damageSources().indirectMagic(m, m), dmg * 0.45F)) {
                        if (a.type() == Type.DRAIN) {
                            m.heal(dmg * 0.45F);
                            sl.sendParticles(ParticleTypes.HEART, m.getX(), m.getY() + m.getBbHeight() + 0.3, m.getZ(), 1, 0.3, 0.1, 0.3, 0);
                        } else {
                            applyElement(m, target, el, dmg, 1);
                        }
                    }
                    if ((t - hit) % 10 == 0) sound(m, a.type() == Type.DRAIN ? SoundEvents.WARDEN_HEARTBEAT : SoundEvents.BEACON_AMBIENT, 1.0F, 1.4F);
                }
            }
            case AURA -> {
                if (t == hit) {
                    float r = a.radius();
                    for (LivingEntity e : hostilesAround(m, m.position(), r)) {
                        e.hurt(m.damageSources().indirectMagic(m, m), dmg * 0.5F);
                        applyElement(m, e, el, dmg, 2);
                        if (el == Element.SHADOW) e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1), m);
                    }
                    for (int k = 1; k <= 3; k++) ring(sl, m.position(), r * k / 3.0, dust(el.color, 1.8F), (int) (r * 8), 0);
                    sl.sendParticles(particle(el), m.getX(), m.getY() + 1, m.getZ(), 40, r * 0.4, 0.8, r * 0.4, 0.05);
                    sound(m, SoundEvents.EVOKER_CAST_SPELL, 1.4F, 0.7F);
                }
            }
            case HEAL -> {
                if (t == hit) {
                    float amount = m.getMaxHealth() * 0.12F * a.power();
                    m.heal(amount);
                    for (LivingEntity e : m.level().getEntitiesOfClass(LivingEntity.class, m.getBoundingBox().inflate(a.radius()), e -> e != m && m.isAlliedTo(e))) {
                        e.heal(amount);
                        sl.sendParticles(ParticleTypes.HEART, e.getX(), e.getY() + e.getBbHeight() + 0.2, e.getZ(), 2, 0.3, 0.2, 0.3, 0);
                    }
                    sl.sendParticles(ParticleTypes.HEART, m.getX(), m.getY() + m.getBbHeight() + 0.3, m.getZ(), 4, 0.4, 0.2, 0.4, 0);
                    ring(sl, m.position(), a.radius(), dust(el.light, 1.5F), 40, 0);
                    sound(m, SoundEvents.ILLUSIONER_CAST_SPELL, 1.2F, 1.3F);
                }
            }
            case PULL -> {
                if (t == hit && target != null && m.distanceTo(target) <= a.maxRange() + 1) {
                    Vec3 d = m.position().subtract(target.position());
                    Vec3 pull = d.normalize().scale(Math.min(1.8, d.length() * 0.22)).add(0, 0.35, 0);
                    target.setDeltaMovement(pull);
                    target.hurtMarked = true;
                    target.hurt(source(m, el), dmg * 0.6F);
                    applyElement(m, target, el, dmg, 1);
                    line(sl, eye(m), aim(target), dust(0x6A6A70, 1.4F), 0.35);
                    line(sl, eye(m), aim(target), dust(el.color, 1.0F), 0.7);
                    sound(m, SoundEvents.CHAIN_BREAK, 1.5F, 0.6F);
                }
            }
            case EXPLODE -> {
                if (t > 2 && t < hit && t % 3 == 0) sl.sendParticles(dust(el.light, 1.5F), m.getX(), m.getY() + 0.5, m.getZ(), 6, 0.4, 0.3, 0.4, 0);
                if (t == hit) {
                    slam(m, sl, m.position(), a.radius(), dmg, el, 0.5);
                    sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, m.getX(), m.getY() + 0.5, m.getZ(), 1, 0, 0, 0, 0);
                    sound(m, SoundEvents.GENERIC_EXPLODE, 1.5F, 1.1F);
                    m.discard();
                }
            }
            case SHIELD -> {
                if (t == hit) {
                    m.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 2));
                    m.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, boss(m) ? 4 : 1));
                    burst(m, el, 1.5F, 30);
                    sound(m, SoundEvents.ARMOR_EQUIP_NETHERITE, 1.5F, 0.6F);
                    sound(m, SoundEvents.BEACON_ACTIVATE, 0.8F, 1.6F);
                }
            }
            case WEB -> {
                if (t == hit && target != null && m.distanceTo(target) <= a.maxRange() + 1) {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 4), m);
                    target.addEffect(new MobEffectInstance(MobEffects.JUMP, 60, 128), m);
                    target.hurt(source(m, el), dmg * 0.4F);
                    line(sl, eye(m), aim(target), dust(0xE8E8E8, 1.0F), 0.3);
                    sl.sendParticles(dust(0xFFFFFF, 1.6F), target.getX(), target.getY() + 0.5, target.getZ(), 30, 0.5, 0.6, 0.5, 0);
                    sound(m, SoundEvents.SLIME_SQUISH, 1.2F, 0.6F);
                }
            }
            case STUN -> {
                if (t == hit) {
                    for (LivingEntity e : hostilesAround(m, m.position(), a.radius())) {
                        strike(m, e, dmg, el, 1.0, 0.4);
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3), m);
                        e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 1), m);
                        if (e instanceof Player) e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0), m);
                    }
                    for (int k = 1; k <= 4; k++) ring(sl, m.position(), a.radius() * k / 4.0, dust(el.light, 2.0F), (int) (a.radius() * 10), 0);
                    sound(m, SoundEvents.BELL_BLOCK, 3.0F, 0.5F);
                    sound(m, SoundEvents.BELL_RESONATE, 2.0F, 0.6F);
                }
            }
            case METEOR -> {
                if (t == hit && target != null) s.point = target.position();
                int k = t - hit;
                if (k >= 0 && k % 4 == 0 && k / 4 < a.count()) {
                    Vec3 c = target != null && k % 8 == 0 ? target.position() : s.point;
                    double r = a.radius();
                    Vec3 land = c.add(m.getRandom().nextGaussian() * r * 0.5, 0, m.getRandom().nextGaussian() * r * 0.5);
                    Vec3 from = land.add(m.getRandom().nextGaussian() * 3, 18 + m.getRandom().nextInt(6), m.getRandom().nextGaussian() * 3);
                    Vec3 v = land.subtract(from).normalize().scale(1.2);
                    SpellProjectile p = SpellProjectile.shoot(m, from, v, el, dmg, 0.02F, 3.0F, false);
                    p.setBig(true);
                    if (k == 0) sound(m, SoundEvents.WITHER_SHOOT, 1.4F, 0.5F);
                }
            }
            case SPIN -> {
                if (t >= hit && (t - hit) % 4 == 0) {
                    for (LivingEntity e : hostilesAround(m, m.position(), a.radius())) strike(m, e, dmg * 0.55F, el, 0.9, 0.2);
                    ring(sl, m.position().add(0, m.getBbHeight() * 0.45, 0), a.radius() * 0.8, dust(el.color, 1.6F), 20, 0);
                    sl.sendParticles(ParticleTypes.SWEEP_ATTACK, m.getX(), m.getY() + m.getBbHeight() * 0.5, m.getZ(), 3, a.radius() * 0.4, 0.1, a.radius() * 0.4, 0);
                    sound(m, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.8F + (t - hit) * 0.02F);
                }
            }
            case VANISH -> {
                if (t == hit) {
                    m.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 80, 0));
                    m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 1));
                    sl.sendParticles(ParticleTypes.LARGE_SMOKE, m.getX(), m.getY() + 1, m.getZ(), 30, 0.4, 0.7, 0.4, 0.02);
                    sound(m, SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.2F, 1.0F);
                }
            }
            case ROAR -> {
                if (t == hit) {
                    for (LivingEntity e : m.level().getEntitiesOfClass(LivingEntity.class, m.getBoundingBox().inflate(a.radius()), e -> e != m && m.isAlliedTo(e))) {
                        e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 160, 0));
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 160, 0));
                    }
                    m.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 160, 0));
                    for (LivingEntity e : hostilesAround(m, m.position(), a.radius())) {
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 1), m);
                        e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0), m);
                    }
                    ring(sl, m.position(), 2.0, dust(el.color, 2.0F), 30, 0.3);
                    sound(m, SoundEvents.RAVAGER_ROAR, 2.0F, boss(m) ? 0.6F : 0.9F);
                }
            }
            case BREATH -> {
                if (t >= hit && t % 2 == 0) {
                    Vec3 f = target != null ? aim(target).subtract(eye(m)).normalize() : forward(m);
                    Vec3 o = eye(m).add(f.scale(m.getBbWidth() * 0.5));
                    for (int i = 0; i < 5; i++) {
                        Vec3 v = f.add(m.getRandom().nextGaussian() * 0.12, m.getRandom().nextGaussian() * 0.08, m.getRandom().nextGaussian() * 0.12).scale(0.6 + m.getRandom().nextDouble() * 0.5);
                        sl.sendParticles(particle(el), o.x, o.y, o.z, 0, v.x, v.y, v.z, 1.0);
                    }
                    sl.sendParticles(dust(el.color, 1.8F), o.x + f.x * 2, o.y + f.y * 2, o.z + f.z * 2, 4, 0.5, 0.3, 0.5, 0);
                    if ((t - hit) % 6 == 0) {
                        double len = a.maxRange();
                        for (LivingEntity e : hostilesAround(m, m.position(), len + 1)) {
                            Vec3 to = aim(e).subtract(o);
                            if (to.length() > len || to.normalize().dot(f) < 0.8) continue;
                            e.hurt(source(m, el), dmg * 0.4F);
                            applyElement(m, e, el, dmg, 1);
                        }
                        sound(m, el == Element.FIRE ? SoundEvents.FIRECHARGE_USE : SoundEvents.SOUL_ESCAPE, 1.0F, 0.7F);
                    }
                }
            }
            case NOVA -> {
                if (t == hit) {
                    int n = Math.max(6, a.count());
                    for (int i = 0; i < n; i++) {
                        double ang = i * Math.PI * 2 / n + m.getRandom().nextDouble() * 0.1;
                        Vec3 d = new Vec3(Math.cos(ang), 0.05, Math.sin(ang));
                        SpellProjectile.shoot(m, eye(m).add(d.scale(m.getBbWidth() * 0.6)), d.scale(0.75), el, dmg * 0.7F, 0.0F, a.radius(), false);
                    }
                    sound(m, SoundEvents.EVOKER_CAST_SPELL, 1.5F, 0.6F);
                }
            }
        }
    }

    static void windup(Mob m, Ability a, ServerLevel sl) {
        if (a.melee()) return;
        Element el = a.element();
        sl.sendParticles(dust(el.color, 1.4F), m.getX(), m.getY() + m.getBbHeight() * 0.6, m.getZ(), 10, m.getBbWidth() * 0.6, m.getBbHeight() * 0.3, m.getBbWidth() * 0.6, 0);
        if (boss(m)) sl.sendParticles(particle(el), m.getX(), m.getY() + m.getBbHeight() * 0.6, m.getZ(), 12, m.getBbWidth() * 0.5, m.getBbHeight() * 0.3, m.getBbWidth() * 0.5, 0.02);
    }

    public static void slam(Mob m, ServerLevel sl, Vec3 c, float r, float dmg, Element el, double lift) {
        for (LivingEntity e : hostilesAround(m, c, r)) {
            double f = 1.0 - 0.45 * Math.sqrt(e.position().distanceToSqr(c)) / r;
            strike(m, e, (float) (dmg * f), el, 1.0, lift);
        }
        sl.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 0.3, c.z, 1, 0, 0, 0, 0);
        for (int k = 1; k <= 3; k++) ring(sl, c, r * k / 3.0, dust(el.color, 2.0F), (int) (r * 7), 0);
        ring(sl, c, r * 0.7, particle(el), 16, 0.15);
        sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, c.x, c.y + 0.1, c.z, 8, r * 0.3, 0.1, r * 0.3, 0.01);
        soundAt(sl, c, SoundEvents.GENERIC_EXPLODE, 1.2F, 0.7F);
        soundAt(sl, c, SoundEvents.RAVAGER_STEP, 2.0F, 0.5F);
    }

    public static void lob(LivingEntity m, Vec3 from, Vec3 to, Element el, float dmg, float aoe) {
        Vec3 d = to.subtract(from);
        double h = Math.sqrt(d.x * d.x + d.z * d.z);
        double T = Mth.clamp(h * 1.3, 12, 40);
        double g = 0.05;
        Vec3 v = new Vec3(d.x / T, (d.y + 0.5 * g * T * T) / T, d.z / T);
        SpellProjectile p = SpellProjectile.shoot(m, from, v, el, dmg, (float) g, aoe, false);
        p.setBig(true);
    }

    public static void summon(Mob m, Ability a, @Nullable LivingEntity target) {
        if (!(m.level() instanceof ServerLevel sl)) return;
        EntityType<? extends Mob> type = RealmEntities.type(a.summon());
        if (type == null) return;
        int existing = sl.getEntities(type, m.getBoundingBox().inflate(24), e -> true).size();
        int n = Math.min(a.count(), 6 - existing);
        for (int i = 0; i < n; i++) {
            Mob s = type.create(sl);
            if (s == null) continue;
            double ang = m.getRandom().nextDouble() * Math.PI * 2;
            double r = 1.5 + m.getRandom().nextDouble() * 2.0;
            BlockPos bp = BlockPos.containing(m.getX() + Math.cos(ang) * r, m.getY() + 0.5, m.getZ() + Math.sin(ang) * r);
            s.moveTo(bp.getX() + 0.5, bp.getY(), bp.getZ() + 0.5, m.getRandom().nextFloat() * 360F, 0);
            s.finalizeSpawn(sl, sl.getCurrentDifficultyAt(bp), MobSpawnType.MOB_SUMMONED, null, null);
            if (target != null && hostile(m, target)) s.setTarget(target);
            sl.addFreshEntity(s);
            sl.sendParticles(dust(a.element().color, 2.0F), s.getX(), s.getY() + 0.5, s.getZ(), 20, 0.4, 0.6, 0.4, 0);
            sl.sendParticles(particle(a.element()), s.getX(), s.getY() + 0.2, s.getZ(), 12, 0.3, 0.1, 0.3, 0.05);
        }
        if (n > 0) sound(m, SoundEvents.EVOKER_PREPARE_SUMMON, 1.3F, 0.8F);
    }

    static void blink(Mob m, @Nullable LivingEntity target, ServerLevel sl) {
        Vec3 old = m.position();
        for (int attempt = 0; attempt < 10; attempt++) {
            Vec3 dest;
            if (target != null && attempt < 6) {
                Vec3 back = forward(target).scale(-(2.0 + m.getRandom().nextDouble()));
                dest = target.position().add(back).add(m.getRandom().nextGaussian() * attempt * 0.4, 0, m.getRandom().nextGaussian() * attempt * 0.4);
            } else {
                dest = old.add(m.getRandom().nextGaussian() * 6, 0, m.getRandom().nextGaussian() * 6);
            }
            BlockPos bp = BlockPos.containing(dest);
            for (int dy = 2; dy >= -3; dy--) {
                BlockPos q = bp.above(dy);
                if (!m.level().getBlockState(q.below()).isSolid()) continue;
                AABB box = m.getType().getAABB(q.getX() + 0.5, q.getY(), q.getZ() + 0.5);
                if (m.level().noCollision(m, box) && !m.level().containsAnyLiquid(box)) {
                    m.teleportTo(q.getX() + 0.5, q.getY(), q.getZ() + 0.5);
                    sl.sendParticles(ParticleTypes.REVERSE_PORTAL, old.x, old.y + 1, old.z, 40, 0.4, 0.8, 0.4, 0.05);
                    sl.sendParticles(ParticleTypes.LARGE_SMOKE, old.x, old.y + 1, old.z, 10, 0.3, 0.6, 0.3, 0.01);
                    sl.sendParticles(ParticleTypes.REVERSE_PORTAL, m.getX(), m.getY() + 1, m.getZ(), 40, 0.4, 0.8, 0.4, 0.05);
                    soundAt(sl, old, SoundEvents.ENDERMAN_TELEPORT, 1.0F, 0.8F);
                    sound(m, SoundEvents.ENDERMAN_TELEPORT, 1.0F, 0.8F);
                    if (target != null) face(m, target);
                    return;
                }
            }
        }
    }

    /** client side ambience of every realm creature */
    public static void ambientFx(Mob m) {
        Level l = m.level();
        if (m.getRandom().nextInt(m instanceof BossEntity ? 2 : 7) != 0) return;
        Element el = null;
        if (m instanceof RealmMob rm && rm.spec.abilities().length > 0) el = rm.spec.abilities()[0].element();
        if (m instanceof RealmBoss rb && rb.spec.abilities().length > 0) el = rb.spec.abilities()[0].element();
        if (el == null) return;
        double x = m.getRandomX(0.7), y = m.getY() + m.getRandom().nextDouble() * m.getBbHeight(), z = m.getRandomZ(0.7);
        if (m.getRandom().nextBoolean()) l.addParticle(dust(el.color, 0.9F), x, y, z, 0, 0.02, 0);
        else if (el != Element.BLOOD && el != Element.POISON) l.addParticle(particle(el), x, y, z, 0, 0.01, 0);
        if (m instanceof RealmMob rm && rm.isAlly() && m.getRandom().nextInt(3) == 0) {
            l.addParticle(ParticleTypes.HAPPY_VILLAGER, x, m.getY() + m.getBbHeight() + 0.3, z, 0, 0, 0);
        }
    }
}
