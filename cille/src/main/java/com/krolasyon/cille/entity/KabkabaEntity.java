package com.krolasyon.cille.entity;

import com.krolasyon.cille.registry.ModEntities;
import com.krolasyon.cille.registry.ModItems;
import com.krolasyon.cille.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;

/** Kabkaba — the dark Cille: black hole, shadow step, energy drain, reflection, shadow spikes, dark guard, crater slam. */
public class KabkabaEntity extends CilleEntity {
    public static final int BLACK_HOLE = 1, BLINK = 2, DRAIN = 3, REFLECT = 4, SHADOW = 5, GUARD = 6, SLAM = 7, DODGE = 8;
    private static final int[] COMMANDABLE = {BLACK_HOLE, BLINK, DRAIN, REFLECT, SHADOW, GUARD, SLAM};

    private boolean slamDone;

    public KabkabaEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setMaxUpStep(1.5F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 320.0D)
                .add(Attributes.ARMOR, 12.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 4.0D)
                .add(Attributes.ATTACK_DAMAGE, 14.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9D);
    }

    @Override public String cilleName() { return "kabkaba"; }
    @Override public Item marbleItem() { return ModItems.KABKABA_MISKETI.get(); }
    @Override public int[] commandableAbilities() { return COMMANDABLE; }
    @Override public boolean needsTarget(int id) { return id == BLACK_HOLE || id == BLINK || id == DRAIN || id == SHADOW; }
    @Override public double meleeReach() { return 3.2; }
    @Override protected int defendAbility() { return ready(GUARD) ? GUARD : ready(REFLECT) ? REFLECT : -1; }
    @Override protected boolean isDefensive(int id) { return id == GUARD || id == REFLECT; }
    @Override protected boolean facesTarget(int id) { return id != SLAM && id != DODGE; }

    @Override
    protected void onAttackCommand(LivingEntity t) {
        if (!isCasting() && distanceToSqr(t) > 12 * 12 && ready(BLINK)) startAbility(BLINK, t);
        else super.onAttackCommand(t);
    }

    @Override
    protected void onOwnerAttacked(LivingEntity atk) {
        super.onOwnerAttacked(atk);
        if (!isCasting() && ready(BLINK) && distanceToSqr(atk) > 64) startAbility(BLINK, atk);
    }

    @Override
    protected int abilityDuration(int id) {
        return switch (id) {
            case MELEE -> 16;
            case BLACK_HOLE -> 44;
            case BLINK -> 26;
            case DRAIN -> 60;
            case REFLECT -> 70;
            case SHADOW -> 40;
            case GUARD -> 30;
            case SLAM -> 40;
            case DODGE -> 9;
            default -> 20;
        };
    }

    @Override
    protected int abilityCooldown(int id) {
        return switch (id) {
            case MELEE -> 18;
            case BLACK_HOLE -> 420;
            case BLINK -> 150;
            case DRAIN -> 280;
            case REFLECT -> 320;
            case SHADOW -> 200;
            case GUARD -> 560;
            case SLAM -> 240;
            case DODGE -> 70;
            default -> 0;
        };
    }

    @Override
    protected void onAbilityStart(int id) { slamDone = false; }

    /** high battle instinct: dodge projectiles, counter melee hits, raise defences when hurt */
    @Override
    protected void instinct(DamageSource src, @Nullable LivingEntity la) {
        Entity direct = src.getDirectEntity();
        if (isCasting() && currentAbility != MELEE) return;
        if (getHealth() < getMaxHealth() * 0.4F && ready(GUARD) && random.nextFloat() < 0.5F) {
            startAbility(GUARD, la);
        } else if (direct instanceof Projectile && ready(REFLECT) && random.nextFloat() < 0.35F) {
            startAbility(REFLECT, la);
        } else if (direct instanceof Projectile && ready(DODGE) && random.nextFloat() < 0.5F) {
            startAbility(DODGE, la);
        } else {
            counterMelee(la, src);
        }
    }

    @Override
    protected boolean reactToProjectile(Projectile p) {
        if (!ready(DODGE)) return false;
        startAbility(DODGE, p.getOwner() instanceof LivingEntity l ? l : null);
        return true;
    }

    @Override
    protected void releaseTick(ServerLevel sl, int t) {
        switch (t) {
            case 14 -> {
                    sound(ModSounds.ROAR.get(), 4.0F, 1.0F);
                    Vec3 mouth = mouthPos();
                    sl.sendParticles(ParticleTypes.SONIC_BOOM, mouth.x, mouth.y, mouth.z, 1, 0, 0, 0, 0);
                    for (int i = 0; i < 72; i++) {
                        double a = i * Math.PI * 2 / 72;
                        sl.sendParticles(dust(i % 3 == 0 ? VIOLET : DARK, 2.4F), getX(), getY() + 0.4, getZ(), 0, Math.cos(a), 0.05, Math.sin(a), 0.8);
                    }
                    for (LivingEntity e : hostilesAround(position(), 7)) {
                        if (e instanceof CilleEntity) continue;
                        knock(e, 1.4, 0.4);
                        if (e instanceof Player) e.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0));
                    }
            }
            default -> {}
        }
    }

    @Override
    protected void onPhase2(ServerLevel sl) {
        sound(ModSounds.ROAR.get(), 4.0F, 0.8F);
        sl.sendParticles(ParticleTypes.SONIC_BOOM, getX(), getY() + 1.6, getZ(), 1, 0, 0, 0, 0);
        for (LivingEntity e : hostilesAround(position(), 8)) knock(e, 1.8, 0.5);
    }

    /** approximate world position of the mouth */
    @Override
    public Vec3 mouthPos() {
        return position().add(forward().scale(3.1)).add(0, 1.55, 0);
    }

    @Override
    protected int chooseAbility(LivingEntity target, double distSqr) {
        double dist = Math.sqrt(distSqr);
        double reach = 3.2 + target.getBbWidth() * 0.5;
        boolean melee = dist <= reach && ready(MELEE);
        if (globalCooldown > 0 || this.tickCount % 5 != 0) return melee ? MELEE : -1;
        if (melee && random.nextFloat() < 0.45F) return MELEE;
        boolean los = this.hasLineOfSight(target);
        int crowd = hostilesAround(target.position(), 5).size();
        int[] opts = new int[8];
        int[] w = new int[8];
        int n = 0, total = 0;
        if (ready(BLACK_HOLE) && dist > 5 && dist < 26 && los) { opts[n] = BLACK_HOLE; w[n] = 3 + crowd; total += w[n++]; }
        if (ready(BLINK) && dist > 7 && dist < 40) { opts[n] = BLINK; w[n] = dist > 14 ? 5 : 3; total += w[n++]; }
        if (ready(DRAIN) && dist > 3 && dist < 16 && los) { opts[n] = DRAIN; w[n] = getHealth() < getMaxHealth() * 0.6F ? 5 : 2; total += w[n++]; }
        if (ready(REFLECT) && dist < 24) { opts[n] = REFLECT; w[n] = this.tickCount - lastProjectileHit < 80 ? 6 : (recentHits > 3 ? 3 : 1); total += w[n++]; }
        if (ready(SHADOW) && dist > 3 && dist < 22 && los) { opts[n] = SHADOW; w[n] = 3; total += w[n++]; }
        if (ready(GUARD) && getHealth() < getMaxHealth() * 0.45F) { opts[n] = GUARD; w[n] = 4; total += w[n++]; }
        if (ready(SLAM) && dist < 10 && this.onGround()) { opts[n] = SLAM; w[n] = 2 + crowd; total += w[n++]; }
        if (n == 0 || random.nextFloat() > 0.55F) return melee ? MELEE : -1;
        int roll = random.nextInt(total);
        for (int i = 0; i < n; i++) {
            roll -= w[i];
            if (roll < 0) return opts[i];
        }
        return opts[0];
    }

    @Override
    protected void tickAbility(int id, int t, @Nullable LivingEntity target) {
        ServerLevel sl = serverLevel();
        switch (id) {
            case MELEE -> {
                if (t == 3) sound(ModSounds.CLAW.get(), 1.6F, 0.9F + random.nextFloat() * 0.2F);
                if (t == 7) clawArc(sl, (float) getAttributeValue(Attributes.ATTACK_DAMAGE), 1.0);
                if (t == 9 && target != null && distanceToSqr(target) < 20) clawArc(sl, (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.6F, 0.4);
            }
            case BLACK_HOLE -> tickBlackHole(sl, t, target);
            case BLINK -> tickBlink(sl, t, target);
            case DRAIN -> tickDrain(sl, t, target);
            case REFLECT -> {
                if (t == 3) {
                    setShield(SHIELD_REFLECT);
                    sound(ModSounds.REFLECT.get(), 2.0F, 1.0F);
                    sl.sendParticles(ParticleTypes.FLASH, getX(), getY() + 1.4, getZ(), 1, 0, 0, 0, 0);
                }
                if (t == 66) setShield(shieldTimer > 0 ? SHIELD_GUARD : SHIELD_NONE);
            }
            case SHADOW -> tickShadow(sl, t, target);
            case GUARD -> {
                if (t == 6) sound(ModSounds.GUARD.get(), 2.0F, 0.9F);
                if (t == 10) {
                    setShield(SHIELD_GUARD);
                    shieldTimer = isPhase2() ? 200 : 140;
                    this.heal(12F);
                    this.removeEffect(MobEffects.WEAKNESS);
                    this.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                    for (int i = 0; i < 60; i++) {
                        double a = i * Math.PI * 2 / 60;
                        sl.sendParticles(dust(i % 2 == 0 ? DARK : VIOLET, 2.2F), getX() + Math.cos(a) * 2.2, getY() + 0.3 + (i % 5) * 0.5, getZ() + Math.sin(a) * 2.2, 1, 0, 0.05, 0, 0);
                    }
                }
            }
            case SLAM -> tickSlam(sl, t, target);
            case DODGE -> {
                if (t == 2) {
                    Vec3 side = right().scale(random.nextBoolean() ? 1 : -1);
                    Vec3 from = position();
                    for (double d : new double[]{5.5, 4.0, -4.0, 2.5}) {
                        Vec3 p = from.add(side.scale(d));
                        if (this.randomTeleport(p.x, p.y + 1, p.z, false)) break;
                    }
                    shadowPuff(sl, from);
                    shadowPuff(sl, position());
                    sound(ModSounds.BLINK.get(), 1.5F, 1.0F);
                }
            }
            default -> {}
        }
    }

    private void clawArc(ServerLevel sl, float dmg, double knock) {
        Vec3 f = forward();
        Vec3 c = position().add(f.scale(2.6)).add(0, 1.0, 0);
        for (int i = 0; i < 14; i++) {
            double a = (i / 13.0 - 0.5) * Math.PI * 0.9;
            Vec3 d = f.yRot((float) a).scale(3.0);
            sl.sendParticles(dust(i % 2 == 0 ? VIOLET : DARK, 1.6F), getX() + d.x, getY() + 1.3 + i * 0.05, getZ() + d.z, 1, 0, 0, 0, 0);
        }
        sl.sendParticles(ParticleTypes.SWEEP_ATTACK, c.x, c.y, c.z, 2, 0.4, 0.1, 0.4, 0);
        for (LivingEntity e : hostilesIn(new AABB(c, c).inflate(2.5, 1.8, 2.5))) {
            Vec3 to = e.position().subtract(position());
            if (to.horizontalDistanceSqr() > 0.01 && to.normalize().dot(f) < -0.2) continue;
            hit(e, damageSources().mobAttack(this), dmg, knock, 0.3);
        }
    }

    private void tickBlackHole(ServerLevel sl, int t, @Nullable LivingEntity target) {
        if (t == 4) sound(ModSounds.ROAR.get(), 3.0F, 0.75F);
        Vec3 mouth = mouthPos();
        if (t >= 8 && t < 24) {
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, mouth.x, mouth.y + 1.5, mouth.z, 6, 1.5, 1.0, 1.5, 0.05);
            sl.sendParticles(dust(INK, 2.5F), mouth.x, mouth.y + 1.5, mouth.z, 4, 0.4, 0.4, 0.4, 0);
        }
        if (t == 24) {
            Vec3 at;
            if (target != null) {
                Vec3 d = target.position().subtract(position());
                if (d.length() > 22) d = d.normalize().scale(22);
                at = position().add(d).add(0, 2.2, 0);
            } else {
                at = position().add(forward().scale(10)).add(0, 2.2, 0);
            }
            BlackHoleEntity hole = new BlackHoleEntity(ModEntities.BLACK_HOLE.get(), this.level());
            hole.setup(this, at, isPhase2());
            this.level().addFreshEntity(hole);
            sound(ModSounds.HOLE_OPEN.get(), 3.0F, 1.0F);
            sl.sendParticles(ParticleTypes.FLASH, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        }
    }

    private void tickBlink(ServerLevel sl, int t, @Nullable LivingEntity target) {
        if (t == 2) {
            sound(ModSounds.BLINK.get(), 2.0F, 0.8F);
            shadowPuff(sl, position());
        }
        if (t == 9) {
            Vec3 from = position();
            boolean ok = false;
            if (target != null) {
                Vec3 look = target.getLookAngle().multiply(1, 0, 1);
                if (look.lengthSqr() < 1.0E-4) look = target.position().subtract(from).multiply(1, 0, 1);
                look = look.normalize();
                Vec3[] tries = {look.scale(-3.4), look.yRot(1.2F).scale(-3.4), look.yRot(-1.2F).scale(-3.4), look.scale(3.4)};
                for (Vec3 off : tries) {
                    Vec3 p = target.position().add(off);
                    if (this.randomTeleport(p.x, p.y + 1, p.z, false)) { ok = true; break; }
                }
            }
            if (!ok) {
                Vec3 p = from.add(forward().scale(8));
                this.randomTeleport(p.x, p.y + 1, p.z, false);
            }
            if (target != null) faceTowards(target);
            shadowPuff(sl, from);
            shadowPuff(sl, position());
            sl.sendParticles(ParticleTypes.SCULK_SOUL, getX(), getY() + 1.2, getZ(), 12, 0.8, 0.8, 0.8, 0.05);
            sound(ModSounds.BLINK.get(), 2.0F, 0.95F);
        }
        if (t == 16) {
            sound(ModSounds.CLAW.get(), 2.0F, 0.75F);
            float dmg = (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.3F;
            Vec3 c = position().add(forward().scale(2.4)).add(0, 1.0, 0);
            for (LivingEntity e : hostilesIn(new AABB(c, c).inflate(2.8, 1.8, 2.8))) {
                hit(e, damageSources().mobAttack(this), dmg, 1.2, 0.45);
                if (e instanceof Player) e.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 50, 0));
            }
            sl.sendParticles(ParticleTypes.SWEEP_ATTACK, c.x, c.y, c.z, 3, 0.6, 0.2, 0.6, 0);
        }
    }

    private void tickDrain(ServerLevel sl, int t, @Nullable LivingEntity target) {
        if (target == null || !target.isAlive() || distanceToSqr(target) > 18 * 18 || !this.hasLineOfSight(target)) {
            if (t >= 10) { endAbility(); return; }
            return;
        }
        if (t == 8) sound(ModSounds.DRAIN.get(), 2.5F, 1.0F);
        if (t == 10) setBeam(target.getId());
        if (t >= 10 && t <= 56 && t % 5 == 0) {
            float before = target.getHealth();
            float dmg = isPhase2() ? 3.2F : 2.5F;
            target.hurt(damageSources().indirectMagic(this, this), dmg);
            float dealt = Math.max(0, before - target.getHealth());
            this.heal(dealt + 1.0F);
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            if (target instanceof Player p) p.causeFoodExhaustion(3.0F);
            Vec3 m = target.getBoundingBox().getCenter();
            sl.sendParticles(dust(PALE, 1.4F), m.x, m.y, m.z, 8, 0.3, 0.4, 0.3, 0);
        }
        if (t == 30 || t == 50) {
            // steal one beneficial effect
            for (MobEffectInstance inst : new ArrayList<>(target.getActiveEffects())) {
                if (inst.getEffect().isBeneficial()) {
                    target.removeEffect(inst.getEffect());
                    this.addEffect(new MobEffectInstance(inst));
                    break;
                }
            }
        }
        if (t == 57) setBeam(-1);
    }

    private void tickShadow(ServerLevel sl, int t, @Nullable LivingEntity target) {
        if (t == 2) sound(ModSounds.ROAR.get(), 2.5F, 1.0F);
        float dmg = isPhase2() ? 12F : 9F;
        if (t == 14 && target != null) {
            // unexpected: spikes erupt right under the target from its own shadow
            Vec3 p = target.position().add(target.getDeltaMovement().scale(6));
            ShadowSpikeEntity.spawn(this, p.x, p.z, target.getY(), 6, dmg);
            for (int i = 0; i < 6; i++) {
                double a = i * Math.PI / 3 + random.nextDouble() * 0.4;
                ShadowSpikeEntity.spawn(this, p.x + Math.cos(a) * 1.9, p.z + Math.sin(a) * 1.9, target.getY(), 8 + i, dmg * 0.8F);
            }
            stompFx(sl);
        }
        if (t == 22) {
            Vec3 dir = target != null ? target.position().subtract(position()).multiply(1, 0, 1).normalize() : forward();
            for (int i = 0; i < 12; i++) {
                Vec3 p = position().add(dir.scale(2.5 + i * 1.4));
                ShadowSpikeEntity.spawn(this, p.x, p.z, getY(), 2 + i, dmg);
            }
            stompFx(sl);
        }
        if (t == 30) {
            for (int ring = 0; ring < 2; ring++) {
                int n = ring == 0 ? 8 : 14;
                double r = ring == 0 ? 3.6 : 6.0;
                for (int i = 0; i < n; i++) {
                    double a = i * Math.PI * 2 / n + ring * 0.2;
                    ShadowSpikeEntity.spawn(this, getX() + Math.cos(a) * r, getZ() + Math.sin(a) * r, getY(), 2 + ring * 4, dmg * 0.8F);
                }
            }
            stompFx(sl);
        }
    }

    private void stompFx(ServerLevel sl) {
        sound(ModSounds.SLAM.get(), 1.4F, 1.0F);
        Vec3 c = position().add(forward().scale(2.4));
        BlockState below = this.level().getBlockState(BlockPos.containing(c.x, getY() - 0.5, c.z));
        if (!below.isAir()) sl.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, below), c.x, getY() + 0.1, c.z, 30, 1.2, 0.1, 1.2, 0.2);
        sl.sendParticles(dust(INK, 3.0F), c.x, getY() + 0.2, c.z, 24, 1.5, 0.1, 1.5, 0);
    }

    private void tickSlam(ServerLevel sl, int t, @Nullable LivingEntity target) {
        if (t == 2) sound(ModSounds.ROAR.get(), 2.5F, 0.9F);
        if (t == 10) {
            Vec3 dest = target != null ? target.position() : position().add(forward().scale(6));
            Vec3 d = dest.subtract(position());
            double horiz = Math.min(Math.sqrt(d.x * d.x + d.z * d.z), 14.0);
            Vec3 dir = new Vec3(d.x, 0, d.z);
            dir = dir.lengthSqr() < 1.0E-4 ? forward() : dir.normalize();
            this.setDeltaMovement(dir.x * horiz / 9.0, 0.95, dir.z * horiz / 9.0);
            this.hasImpulse = true;
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.2, getZ(), 20, 1.0, 0.1, 1.0, 0.05);
        }
        if (t > 10 && t < 24) sl.sendParticles(dust(DARK, 2.0F), getX(), getY() + 1.2, getZ(), 4, 1.0, 0.8, 1.0, 0);
        if (!slamDone && ((t > 14 && this.onGround()) || t == 26)) {
            slamDone = true;
            sound(ModSounds.SLAM.get(), 4.0F, 0.9F);
            Vec3 c = position();
            float dmg = isPhase2() ? 18F : 15F;
            for (LivingEntity e : hostilesAround(c, 6.0)) {
                hit(e, damageSources().mobAttack(this), dmg * (float) (1.0 - 0.4 * e.distanceTo(this) / 6.0), 1.6, 0.75);
                if (e instanceof Player) e.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0));
            }
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y + 0.3, c.z, 1, 0, 0, 0, 0);
            for (int ring = 0; ring < 3; ring++) {
                for (int i = 0; i < 48; i++) {
                    double a = i * Math.PI * 2 / 48;
                    sl.sendParticles(ring == 2 ? ParticleTypes.SQUID_INK : dust(ring == 0 ? INK : VIOLET, 2.6F), c.x + Math.cos(a) * 1.5, c.y + 0.2, c.z + Math.sin(a) * 1.5,
                            0, Math.cos(a), 0.03, Math.sin(a), 0.5 + ring * 0.3);
                }
            }
            Terrain.crater(sl, this, c, isPhase2() ? 4.6 : 3.8, 22, 3.0F);
        }
    }

    @Override
    protected void clientFx() {
        Level l = this.level();
        boolean p2 = isPhase2();
        if (random.nextInt(p2 ? 2 : 4) == 0) {
            Vec3 f = forward();
            double along = (random.nextDouble() - 0.2) * 2.4;
            l.addParticle(random.nextInt(3) == 0 ? ParticleTypes.SMOKE : dust(random.nextBoolean() ? DARK : (p2 ? VIOLET : INK), 1.2F),
                    getX() + f.x * along + (random.nextDouble() - 0.5) * 2.2, getY() + 1.6 + random.nextDouble() * 1.4,
                    getZ() + f.z * along + (random.nextDouble() - 0.5) * 2.2, 0, 0.03, 0);
        }
        if (p2 && random.nextInt(6) == 0) {
            Vec3 m = mouthPos();
            l.addParticle(ParticleTypes.SCULK_SOUL, m.x, m.y + 0.6, m.z, 0, 0.02, 0);
        }
        int beam = getBeamTarget();
        if (beam >= 0) {
            Entity t = l.getEntity(beam);
            if (t != null) {
                Vec3 from = t.getBoundingBox().getCenter();
                Vec3 to = mouthPos();
                Vec3 d = to.subtract(from);
                for (int i = 0; i < 3; i++) {
                    Vec3 p = from.add((random.nextDouble() - 0.5) * 0.6, (random.nextDouble() - 0.5) * 0.8, (random.nextDouble() - 0.5) * 0.6);
                    l.addParticle(i == 0 ? ParticleTypes.SOUL : dust(i == 1 ? PALE : VIOLET, 1.0F), p.x, p.y, p.z, d.x * 0.08, d.y * 0.08, d.z * 0.08);
                }
            }
        }
        int sh = getShield();
        if (sh != SHIELD_NONE && random.nextInt(2) == 0) {
            double a = random.nextDouble() * Math.PI * 2, b = random.nextDouble() * Math.PI - Math.PI / 2;
            double r = 2.4;
            l.addParticle(sh == SHIELD_REFLECT ? ParticleTypes.END_ROD : dust(DARK, 1.5F),
                    getX() + Math.cos(a) * Math.cos(b) * r, getY() + 1.4 + Math.sin(b) * r, getZ() + Math.sin(a) * Math.cos(b) * r, 0, 0, 0);
        }
        if (clientAnimId == BLINK || clientAnimId == DODGE) {
            for (int i = 0; i < 4; i++)
                l.addParticle(ParticleTypes.SQUID_INK, getX() + (random.nextDouble() - 0.5) * 2.5, getY() + random.nextDouble() * 2, getZ() + (random.nextDouble() - 0.5) * 2.5, 0, 0.02, 0);
        }
    }

    private void shadowPuff(ServerLevel sl, Vec3 p) {
        sl.sendParticles(ParticleTypes.SQUID_INK, p.x, p.y + 1.2, p.z, 40, 1.0, 1.0, 1.0, 0.05);
        sl.sendParticles(dust(INK, 3.0F), p.x, p.y + 0.6, p.z, 30, 1.2, 0.6, 1.2, 0);
        sl.sendParticles(ParticleTypes.REVERSE_PORTAL, p.x, p.y + 1.2, p.z, 30, 0.8, 1.0, 0.8, 0.2);
    }


    @Override protected SoundEvent getAmbientSound() { return ModSounds.AMBIENT.get(); }
    @Override protected SoundEvent getHurtSound(DamageSource src) { return ModSounds.HURT.get(); }
    @Override protected SoundEvent getDeathSoundEvent() { return ModSounds.DEATH.get(); }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(ModSounds.STEP.get(), 0.9F, 0.85F + random.nextFloat() * 0.25F);
    }
}
