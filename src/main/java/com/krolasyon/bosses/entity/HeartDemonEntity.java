package com.krolasyon.bosses.entity;

import com.krolasyon.bosses.registry.ModEntities;
import com.krolasyon.bosses.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

/** Kalp Kırıcı İblis — horned crimson demon with a glowing heart sigil, black thorns and a scythe tail. */
public class HeartDemonEntity extends BossEntity {
    public static final int MELEE = 0, WHIP = 1, HEART = 2, DASH = 3, CAGE = 4, SPIN = 5;
    public static final int CRIMSON = 0xE0405A, SHADOW = 0x1E060C, HEART_PINK = 0xFFB0B8, THORN_RED = 0x8C1028;

    private final Set<Integer> dashHit = new HashSet<>();
    private Vec3 dashDir = Vec3.ZERO;

    public HeartDemonEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.PINK, 6);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 400.0D)
                .add(Attributes.ARMOR, 9.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 4.0D)
                .add(Attributes.ATTACK_DAMAGE, 13.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.31D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9D);
    }

    @Override public com.krolasyon.bosses.faction.Faction houseOf() { return com.krolasyon.bosses.faction.Faction.BLOOD; }
    @Override public boolean isSpeaker() { return true; }
    @Override public double meleeReach() { return 3.6; }
    @Override public double walkSpeed() { return 0.9; }
    @Override public double runSpeed() { return 1.5; }

    public Vec3 heartPos() { return position().add(forward().scale(0.45)).add(0, 2.6, 0); }

    // ------------------------------------------------------------------ abilities
    @Override
    protected int abilityDuration(int id) {
        return switch (id) {
            case MELEE -> 20;
            case WHIP -> 24;
            case HEART -> 36;
            case DASH -> 20;
            case CAGE -> 30;
            case SPIN -> 32;
            default -> 20;
        };
    }

    @Override
    protected int abilityCooldown(int id) {
        return switch (id) {
            case MELEE -> 22;
            case WHIP -> 120;
            case HEART -> 170;
            case DASH -> 150;
            case CAGE -> 230;
            case SPIN -> 220;
            default -> 100;
        };
    }

    @Override
    protected int chooseAbility(LivingEntity target, double distSqr) {
        double dist = Math.sqrt(distSqr);
        boolean melee = inMeleeRange(target, distSqr) && ready(MELEE);
        if (globalCooldown > 0 || this.tickCount % 7 != 0) return melee ? MELEE : -1;
        if (melee && random.nextFloat() < 0.5F) return MELEE;
        int[] opts = new int[6];
        int[] w = new int[6];
        int n = 0, total = 0;
        if (ready(WHIP) && dist > 3.5 && dist < 10) { opts[n] = WHIP; w[n++] = 3; total += 3; }
        if (ready(HEART) && dist > 4 && dist < 28 && this.hasLineOfSight(target)) { opts[n] = HEART; w[n++] = 3; total += 3; }
        if (ready(DASH) && dist > 5 && dist < 16) { opts[n] = DASH; w[n++] = 3; total += 3; }
        if (ready(CAGE) && dist < 20) { opts[n] = CAGE; w[n++] = 2; total += 2; }
        if (ready(SPIN) && dist < 6) { opts[n] = SPIN; w[n++] = 3; total += 3; }
        if (n == 0 || random.nextFloat() > 0.6F) return melee ? MELEE : -1;
        int roll = random.nextInt(total);
        for (int i = 0; i < n; i++) {
            roll -= w[i];
            if (roll < 0) return opts[i];
        }
        return opts[0];
    }

    @Override
    protected boolean isAirborneAbility() { return currentAbility == SPIN || (currentAbility == DASH && abilityTick >= 6 && abilityTick <= 13); }

    @Override
    protected void tickAbility(int id, int t, @Nullable LivingEntity target) {
        ServerLevel sl = serverLevel();
        switch (id) {
            case MELEE -> {
                if (t == 3 || t == 11) sound(ModSounds.FRENZY_SLASH.get(), 1.5F, 0.7F + t * 0.02F);
                if (t == 7 || t == 14) claw(sl, (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.65F, t == 7 ? 1 : -1);
            }
            case WHIP -> {
                if (t == 6) sound(ModSounds.DEMON_AMBIENT.get(), 1.5F, 1.2F);
                if (t == 10) {
                    sound(ModSounds.THORN_WHIP.get(), 2.5F, 1.0F);
                    Vec3 f = target != null ? target.position().subtract(position()).multiply(1, 0, 1).normalize() : forward();
                    for (float a : new float[]{-0.35F, 0F, 0.35F}) {
                        Vec3 d = f.yRot(a);
                        for (int i = 1; i <= 18; i++) {
                            Vec3 p = position().add(d.scale(i * 0.5)).add(0, 1.8 - i * 0.03, 0);
                            sl.sendParticles(i % 3 == 0 ? dust(CRIMSON, 1.3F) : dust(SHADOW, 1.6F), p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0);
                        }
                    }
                    float dmg = isPhase2() ? 11F : 9F;
                    for (LivingEntity e : hostilesAround(position(), 9.5)) {
                        Vec3 to = e.position().subtract(position()).multiply(1, 0, 1);
                        if (to.lengthSqr() > 0.01 && to.normalize().dot(f) < 0.55) continue;
                        if (e.hurt(damageSources().mobAttack(this), dmg)) {
                            Vec3 pull = position().subtract(e.position()).multiply(1, 0, 1);
                            double len = pull.length();
                            if (len > 2.5) {
                                pull = pull.normalize().scale(Math.min(1.6, len * 0.2));
                                e.setDeltaMovement(pull.x, 0.35, pull.z);
                                e.hurtMarked = true;
                            }
                            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                            sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, e.getX(), e.getY() + 1, e.getZ(), 4, 0.3, 0.3, 0.3, 0.1);
                        }
                    }
                }
            }
            case HEART -> {
                if (t == 2) sound(ModSounds.HEART_SHOOT.get(), 1.5F, 0.6F);
                if (t < 12) {
                    Vec3 h = heartPos();
                    sl.sendParticles(dust(HEART_PINK, 1.2F), h.x, h.y, h.z, 3, 0.25, 0.25, 0.25, 0);
                }
                int count = isPhase2() ? 7 : 5;
                if (t >= 12 && t < 12 + count * 3 && (t - 12) % 3 == 0 && target != null) {
                    int k = (t - 12) / 3;
                    Vec3 from = heartPos();
                    float spread = (k - (count - 1) / 2F) * 0.35F;
                    Vec3 dir = forward().yRot(spread).add(0, 0.35, 0);
                    HeartOrbEntity orb = new HeartOrbEntity(ModEntities.HEART_ORB.get(), this.level());
                    orb.setup(this, target, from, dir, isPhase2());
                    this.level().addFreshEntity(orb);
                    soundAt(from, ModSounds.HEART_SHOOT.get(), 1.5F, 1.0F + k * 0.06F);
                    sl.sendParticles(ParticleTypes.HEART, from.x, from.y, from.z, 2, 0.2, 0.2, 0.2, 0);
                }
            }
            case DASH -> {
                if (t == 0) {
                    dashHit.clear();
                    dashDir = target != null ? target.position().subtract(position()).multiply(1, 0, 1).normalize() : forward();
                }
                if (t < 6) sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 3, 0.4, 0.6, 0.4, 0.02);
                if (t == 6) sound(ModSounds.SHADOW_DASH.get(), 2.5F, 1.0F);
                if (t >= 6 && t <= 13) {
                    this.setDeltaMovement(dashDir.x * 1.7, Math.min(0, getDeltaMovement().y), dashDir.z * 1.7);
                    this.hasImpulse = true;
                    sl.sendParticles(dust(SHADOW, 2.2F), getX(), getY() + 1.4, getZ(), 8, 0.4, 0.8, 0.4, 0);
                    sl.sendParticles(dust(CRIMSON, 1.4F), getX(), getY() + 1.4, getZ(), 4, 0.4, 0.8, 0.4, 0);
                    float dmg = isPhase2() ? 13F : 11F;
                    for (LivingEntity e : hostilesIn(getBoundingBox().inflate(1.3, 0.5, 1.3))) {
                        if (!dashHit.add(e.getId())) continue;
                        hit(e, damageSources().mobAttack(this), dmg, 0.6, 0.5);
                        Vec3 side = new Vec3(-dashDir.z, 0, dashDir.x).scale(random.nextBoolean() ? 1 : -1);
                        e.setDeltaMovement(e.getDeltaMovement().add(side.scale(0.9)));
                        sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, e.getX(), e.getY() + 1, e.getZ(), 5, 0.3, 0.3, 0.3, 0.1);
                    }
                    if (t % 2 == 0) EruptionEntity.spawn(this, getX() - dashDir.x * 1.5, getZ() - dashDir.z * 1.5, getY(), EruptionEntity.KIND_THORN, 4, 6F);
                }
                if (t == 14) this.setDeltaMovement(getDeltaMovement().multiply(0.2, 1, 0.2));
            }
            case CAGE -> {
                if (t == 4) sound(ModSounds.DEMON_AMBIENT.get(), 1.8F, 0.8F);
                if (t < 12 && target != null) {
                    sl.sendParticles(dust(SHADOW, 1.5F), target.getX(), target.getY() + 0.1, target.getZ(), 6, 1.5, 0.05, 1.5, 0);
                }
                if (t == 12 && target != null) {
                    sound(ModSounds.THORN_ERUPT.get(), 2.5F, 0.8F);
                    float dmg = isPhase2() ? 12F : 10F;
                    int n = isPhase2() ? 14 : 10;
                    for (int i = 0; i < n; i++) {
                        double a = i * Math.PI * 2 / n;
                        EruptionEntity.spawn(this, target.getX() + Math.cos(a) * 2.8, target.getZ() + Math.sin(a) * 2.8, target.getY(), EruptionEntity.KIND_THORN, i % 3, dmg * 0.6F);
                    }
                    EruptionEntity.spawn(this, target.getX(), target.getZ(), target.getY(), EruptionEntity.KIND_THORN, 9, dmg);
                    for (LivingEntity e : hostilesAround(target.position(), 3.0)) {
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 6));
                        e.addEffect(new MobEffectInstance(MobEffects.JUMP, 50, 128));
                    }
                }
            }
            case SPIN -> tickSpin(sl, t);
            default -> {}
        }
    }

    private void claw(ServerLevel sl, float dmg, int side) {
        Vec3 f = forward();
        for (int i = 0; i < 10; i++) {
            double a = (i / 9.0 - 0.5) * Math.PI * side;
            Vec3 d = f.yRot((float) a).scale(2.6);
            sl.sendParticles(dust(i % 2 == 0 ? CRIMSON : SHADOW, 1.5F), getX() + d.x, getY() + 1.8 - i * 0.05, getZ() + d.z, 1, 0, 0, 0, 0);
        }
        Vec3 c = position().add(f.scale(1.9)).add(0, 1.2, 0);
        sl.sendParticles(ParticleTypes.SWEEP_ATTACK, c.x, c.y, c.z, 1, 0.3, 0.1, 0.3, 0);
        for (LivingEntity e : hostilesIn(new AABB(c, c).inflate(2.2, 1.6, 2.2))) {
            Vec3 to = e.position().subtract(position());
            if (to.horizontalDistanceSqr() > 0.01 && to.normalize().dot(f) < -0.2) continue;
            hit(e, damageSources().mobAttack(this), dmg, 0.7, 0.2);
        }
    }

    private void tickSpin(ServerLevel sl, int t) {
        if (t == 4 || t == 16) sound(ModSounds.SCYTHE_SPIN.get(), 2.0F, 0.9F + t * 0.01F);
        if (t >= 6 && t <= 26) {
            double base = t * 0.9;
            for (int k = 0; k < 2; k++) {
                double a = base + k * Math.PI;
                for (int j = 0; j < 4; j++) {
                    double aa = a - j * 0.18;
                    sl.sendParticles(dust(j == 0 ? 0xFF5A6A : CRIMSON, 1.8F - j * 0.3F), getX() + Math.cos(aa) * 3.4, getY() + 0.6, getZ() + Math.sin(aa) * 3.4, 1, 0, 0, 0, 0);
                }
            }
            sl.sendParticles(dust(SHADOW, 1.5F), getX(), getY() + 1.2, getZ(), 4, 1.5, 0.6, 1.5, 0);
            for (LivingEntity e : hostilesAround(position(), 6.5)) {
                Vec3 pull = position().subtract(e.position()).multiply(1, 0, 1);
                if (pull.lengthSqr() > 4) {
                    e.setDeltaMovement(e.getDeltaMovement().add(pull.normalize().scale(0.18)));
                    e.hurtMarked = true;
                }
                if (t % 4 == 0 && e.distanceToSqr(this) < 4.2 * 4.2) {
                    e.invulnerableTime = 0;
                    hit(e, damageSources().mobAttack(this), isPhase2() ? 5F : 4F, 0.2, 0.3);
                }
            }
        }
        if (t == 28) {
            sound(ModSounds.THORN_WHIP.get(), 2.5F, 0.7F);
            sl.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 1, getZ(), 2, 0.5, 0.2, 0.5, 0);
            for (int i = 0; i < 60; i++) {
                double a = i * Math.PI * 2 / 60;
                sl.sendParticles(dust(i % 2 == 0 ? CRIMSON : SHADOW, 2.2F), getX(), getY() + 0.6, getZ(), 0, Math.cos(a), 0.05, Math.sin(a), 0.9);
            }
            for (LivingEntity e : hostilesAround(position(), 6.5)) hit(e, damageSources().mobAttack(this), isPhase2() ? 10F : 8F, 1.9, 0.5);
        }
    }

    @Override
    protected void onPhase2() {
        sound(ModSounds.DEMON_PHASE.get(), 4.0F, 1.0F);
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.36D);
        ServerLevel sl = serverLevel();
        sl.sendParticles(ParticleTypes.FLASH, getX(), getY() + 2.5, getZ(), 2, 0, 0, 0, 0);
        sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, getX(), getY() + 2.5, getZ(), 30, 1.5, 1, 1.5, 0.3);
        for (int i = 0; i < 90; i++) {
            double a = i * Math.PI * 2 / 90;
            sl.sendParticles(dust(i % 2 == 0 ? CRIMSON : SHADOW, 2.5F), getX(), getY() + 0.5, getZ(), 0, Math.cos(a), 0.15, Math.sin(a), 0.9);
        }
        for (int i = 0; i < 10; i++) {
            double a = i * Math.PI / 5;
            EruptionEntity.spawn(this, getX() + Math.cos(a) * 4, getZ() + Math.sin(a) * 4, getY(), EruptionEntity.KIND_THORN, 2, 7F);
        }
        for (LivingEntity e : hostilesAround(position(), 8)) hit(e, damageSources().mobAttack(this), 6F, 2.0, 0.5);
        this.heal(20F);
    }

    // ------------------------------------------------------------------ fx
    @Override
    protected void ambientFx() {
        Level l = this.level();
        boolean p2 = isPhase2();
        if (random.nextInt(p2 ? 1 : 2) == 0) {
            l.addParticle(random.nextInt(3) == 0 ? dust(CRIMSON, 0.9F) : ParticleTypes.SMOKE, getX() + (random.nextDouble() - 0.5) * 1.6,
                    getY() + 2.2 + random.nextDouble() * 0.9, getZ() + (random.nextDouble() - 0.5) * 1.6, 0, 0.03, 0);
        }
        if (random.nextInt(p2 ? 4 : 10) == 0) {
            Vec3 h = heartPos();
            l.addParticle(dust(HEART_PINK, 0.8F), h.x + (random.nextDouble() - 0.5) * 0.4, h.y + (random.nextDouble() - 0.5) * 0.4, h.z + (random.nextDouble() - 0.5) * 0.4, 0, 0.02, 0);
        }
        if (p2 && random.nextInt(10) == 0) l.addParticle(ParticleTypes.DAMAGE_INDICATOR, getX(), getY() + 3.2, getZ(), 0, 0.1, 0);
        if (clientAnimId == DASH || clientAnimId == SPIN) {
            for (int i = 0; i < 3; i++) l.addParticle(dust(SHADOW, 1.8F), getX() + (random.nextDouble() - 0.5) * 2, getY() + random.nextDouble() * 3, getZ() + (random.nextDouble() - 0.5) * 2, 0, 0, 0);
        }
        if (phaseFlash > 0) {
            for (int i = 0; i < 4; i++) l.addParticle(dust(CRIMSON, 2F), getX() + (random.nextDouble() - 0.5) * 3, getY() + random.nextDouble() * 3.5, getZ() + (random.nextDouble() - 0.5) * 3, 0, 0.1, 0);
        }
    }

    @Override
    protected void deathFx(int t) {
        if (this.level().isClientSide()) {
            for (int i = 0; i < 3; i++) {
                this.level().addParticle(i == 0 ? ParticleTypes.LARGE_SMOKE : dust(i == 1 ? CRIMSON : SHADOW, 1.6F), getX() + (random.nextDouble() - 0.5) * 2,
                        getY() + random.nextDouble() * 3, getZ() + (random.nextDouble() - 0.5) * 2, 0, 0.05, 0);
            }
        } else {
            if (t == 1) sound(ModSounds.DEMON_DEATH.get(), 4.0F, 1.0F);
            if (t == DEATH_TICKS - 1) {
                ServerLevel sl = serverLevel();
                sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, getX(), getY() + 1.5, getZ(), 30, 1, 1, 1, 0.3);
                sl.sendParticles(dust(SHADOW, 3F), getX(), getY() + 1, getZ(), 100, 1.5, 1.0, 1.5, 0);
                sl.sendParticles(dust(HEART_PINK, 2F), getX(), getY() + 1.5, getZ(), 40, 0.6, 0.6, 0.6, 0);
                sound(ModSounds.HEART_HIT.get(), 3.0F, 0.5F);
            }
        }
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource src, int looting, boolean hitByPlayer) {
        super.dropCustomDeathLoot(src, looting, hitByPlayer);
        this.spawnAtLocation(new ItemStack(Items.NETHER_STAR));
        this.spawnAtLocation(new ItemStack(Items.GOLDEN_APPLE, 2 + random.nextInt(2)));
        this.spawnAtLocation(new ItemStack(Items.NETHERITE_SCRAP, 2 + random.nextInt(2)));
        this.spawnAtLocation(new ItemStack(Items.EMERALD, 8 + random.nextInt(8)));
    }

    // ------------------------------------------------------------------ sounds
    @Override protected SoundEvent getAmbientSound() { return ModSounds.DEMON_AMBIENT.get(); }
    @Override protected SoundEvent getHurtSound(DamageSource src) { return ModSounds.DEMON_HURT.get(); }
    @Override protected SoundEvent getDeathSound() { return null; }
    @Override protected float getSoundVolume() { return 2.0F; }
    @Override public int getAmbientSoundInterval() { return 150; }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(ModSounds.DEMON_STEP.get(), 0.8F, 0.9F + random.nextFloat() * 0.2F);
    }
}
