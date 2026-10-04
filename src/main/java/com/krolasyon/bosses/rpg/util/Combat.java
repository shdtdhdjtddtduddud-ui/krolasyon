package com.krolasyon.bosses.rpg.util;

import com.krolasyon.bosses.rpg.entity.MagicBolt.OnHit;
import com.krolasyon.bosses.rpg.registry.RpgEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/** Who may hurt whom, and the shared damage / status helpers. */
public final class Combat {
    private Combat() {}

    /** implemented by entities that belong to a player (party members, summons, spouses...) or to a kingdom */
    public interface Allegiance {
        /** player that owns / leads this entity, or null */
        @Nullable UUID leader();
        /** kingdom index or -1 */
        default int kingdom() { return -1; }
        /** true if this entity currently wants to fight {@code other} */
        default boolean hostileTo(LivingEntity other) { return false; }
    }

    @Nullable
    public static UUID leaderOf(Entity e) {
        if (e instanceof Player p) return p.getUUID();
        if (e instanceof Allegiance a) return a.leader();
        return null;
    }

    public static boolean sameSide(LivingEntity a, LivingEntity b) {
        if (a == b) return true;
        UUID la = leaderOf(a), lb = leaderOf(b);
        if (la != null && la.equals(lb)) return true;
        if (a.isAlliedTo(b)) return true;
        boolean ea = a instanceof Enemy && !(a instanceof Allegiance), eb = b instanceof Enemy && !(b instanceof Allegiance);
        return ea && eb;
    }

    public static boolean canHarm(LivingEntity attacker, LivingEntity victim) {
        if (attacker == victim || !victim.isAlive() || victim instanceof ArmorStand) return false;
        if (victim instanceof Player p && (p.isCreative() || p.isSpectator())) return false;
        if (attacker instanceof Mob m && m.getTarget() == victim) return true;
        if (victim instanceof Mob m && m.getTarget() == attacker) return true;
        if (sameSide(attacker, victim)) return false;
        if (attacker instanceof Enemy && !(attacker instanceof Allegiance)) return true;
        if (attacker instanceof Allegiance a && a.hostileTo(victim)) return true;
        if (attacker instanceof Player || leaderOf(attacker) != null) {
            // players and their followers hit monsters and whatever they deliberately target
            return victim instanceof Enemy || victim instanceof Player || victim.getLastHurtByMob() == attacker
                    || (victim instanceof Allegiance va && va.hostileTo(attacker));
        }
        return victim instanceof Player || victim instanceof Allegiance;
    }

    public static List<LivingEntity> victims(LivingEntity attacker, Vec3 c, double r) {
        return attacker.level().getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r, r * 0.9, r),
                e -> canHarm(attacker, e) && e.position().add(0, e.getBbHeight() * 0.5, 0).distanceToSqr(c) <= r * r);
    }

    public static List<LivingEntity> inCone(LivingEntity attacker, Vec3 origin, Vec3 dir, double range, double cosHalfAngle) {
        Vec3 d = dir.normalize();
        return attacker.level().getEntitiesOfClass(LivingEntity.class, new AABB(origin, origin).inflate(range), e -> {
            if (!canHarm(attacker, e)) return false;
            Vec3 to = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(origin);
            double len = to.length();
            return len <= range && (len < 0.8 || to.scale(1 / len).dot(d) >= cosHalfAngle);
        });
    }

    public static List<LivingEntity> onLine(LivingEntity attacker, Vec3 a, Vec3 b, double radius) {
        AABB box = new AABB(a, b).inflate(radius);
        Vec3 ab = b.subtract(a);
        double l2 = ab.lengthSqr();
        return attacker.level().getEntitiesOfClass(LivingEntity.class, box, e -> {
            if (!canHarm(attacker, e)) return false;
            Vec3 p = e.position().add(0, e.getBbHeight() * 0.5, 0);
            double t = l2 < 1e-6 ? 0 : Math.max(0, Math.min(1, p.subtract(a).dot(ab) / l2));
            return a.add(ab.scale(t)).distanceToSqr(p) <= (radius + e.getBbWidth() * 0.5) * (radius + e.getBbWidth() * 0.5);
        });
    }

    public static DamageSource source(@Nullable Entity owner, @Nullable Entity direct) {
        if (owner instanceof LivingEntity le) {
            if (direct != null && direct != owner) return le.damageSources().indirectMagic(direct, owner);
            if (owner instanceof Player p) return le.damageSources().playerAttack(p);
            return le.damageSources().mobAttack(le);
        }
        Entity any = direct != null ? direct : owner;
        return any != null ? any.damageSources().magic() : null;
    }

    public static boolean magic(@Nullable Entity owner, @Nullable Entity direct, LivingEntity victim, float amount) {
        DamageSource s = source(owner, direct);
        if (s == null) s = victim.damageSources().magic();
        return victim.hurt(s, Math.max(0.5F, amount * scaleFor(owner)));
    }

    public static boolean melee(LivingEntity attacker, LivingEntity victim, float amount, double knock, double lift) {
        DamageSource s = attacker instanceof Player p ? attacker.damageSources().playerAttack(p) : attacker.damageSources().mobAttack(attacker);
        if (victim.hurt(s, Math.max(0.5F, amount * scaleFor(attacker)))) {
            knock(attacker.position(), victim, knock, lift);
            return true;
        }
        return false;
    }

    public static void knock(Vec3 from, LivingEntity e, double knock, double lift) {
        if (knock == 0 && lift == 0) return;
        Vec3 d = e.position().subtract(from);
        d = new Vec3(d.x, 0, d.z);
        if (d.lengthSqr() < 1e-4) d = new Vec3(e.getRandom().nextDouble() - 0.5, 0, e.getRandom().nextDouble() - 0.5);
        d = d.normalize();
        double kr = 1.0 - e.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE) * 0.7;
        e.setDeltaMovement(e.getDeltaMovement().add(d.x * knock * kr, lift * kr, d.z * knock * kr));
        e.hurtMarked = true;
    }

    /** spell damage scales with the caster's RPG level through the player data hook */
    private static float scaleFor(@Nullable Entity owner) {
        return SCALER == null || owner == null ? 1.0F : SCALER.scale(owner);
    }

    public interface Scaler { float scale(Entity owner); }
    @Nullable public static Scaler SCALER;

    public static void effect(LivingEntity e, net.minecraft.world.effect.MobEffect eff, int ticks, int amp) {
        e.addEffect(new MobEffectInstance(eff, ticks, amp));
    }

    public static void applyOnHit(@Nullable Entity owner, LivingEntity e, OnHit h, float dmg) {
        switch (h) {
            case BURN -> e.setSecondsOnFire(4);
            case FREEZE -> { effect(e, MobEffects.MOVEMENT_SLOWDOWN, 60, 2); e.setTicksFrozen(Math.min(e.getTicksFrozen() + 80, 300)); }
            case SHOCK -> { effect(e, RpgEffects.STUN.get(), 15, 0); FX.send(e.level(), ParticleTypes.ELECTRIC_SPARK, e.position().add(0, 1, 0), 12, 0.4, 0.1); }
            case POISON -> effect(e, MobEffects.POISON, 100, 1);
            case ACID -> {
                effect(e, MobEffects.POISON, 60, 0);
                for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                    ItemStack st = e.getItemBySlot(slot);
                    if (!st.isEmpty() && st.isDamageableItem()) st.hurtAndBreak(4, e, x -> x.broadcastBreakEvent(slot));
                }
            }
            case WITHER -> effect(e, MobEffects.WITHER, 80, 1);
            case NAUSEA -> { effect(e, MobEffects.CONFUSION, 120, 0); effect(e, MobEffects.POISON, 60, 0); }
            case WEB -> { effect(e, MobEffects.MOVEMENT_SLOWDOWN, 80, 4); TempBlocks.place(e.level(), e.blockPosition(), net.minecraft.world.level.block.Blocks.COBWEB.defaultBlockState(), 80); }
            case HOLY -> {
                if (e.getMobType() == MobType.UNDEAD) { magic(owner, null, e, dmg * 0.8F); e.setSecondsOnFire(3); }
                effect(e, MobEffects.GLOWING, 100, 0);
            }
            case LIFESTEAL -> { if (owner instanceof LivingEntity o) o.heal(dmg * 0.5F); }
            case KNOCK -> { if (owner != null) knock(owner.position(), e, 1.2, 0.4); }
            case BLIND -> effect(e, MobEffects.BLINDNESS, 60, 0);
            case SLOW -> effect(e, MobEffects.MOVEMENT_SLOWDOWN, 80, 1);
            case ROOT -> { effect(e, MobEffects.MOVEMENT_SLOWDOWN, 60, 6); effect(e, MobEffects.JUMP, 60, 128); }
            case BLEED -> effect(e, RpgEffects.BLEED.get(), 100, 0);
            default -> {}
        }
    }
}
