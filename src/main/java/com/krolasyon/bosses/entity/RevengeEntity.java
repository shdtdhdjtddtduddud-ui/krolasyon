package com.krolasyon.bosses.entity;

import com.krolasyon.bosses.registry.ModEntities;
import com.krolasyon.bosses.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** İntikam — crimson flesh revenant with a blood lance, an arm blade and a pool of blood at its feet. */
public class RevengeEntity extends BossEntity {
    @SuppressWarnings("unchecked")
    private static final EntityDataAccessor<Integer>[] DATA_DRAIN = new EntityDataAccessor[]{
            SynchedEntityData.defineId(RevengeEntity.class, EntityDataSerializers.INT),
            SynchedEntityData.defineId(RevengeEntity.class, EntityDataSerializers.INT),
            SynchedEntityData.defineId(RevengeEntity.class, EntityDataSerializers.INT),
            SynchedEntityData.defineId(RevengeEntity.class, EntityDataSerializers.INT)};

    public static final int MELEE = 0, LANCE = 1, TIDE = 2, DANCE = 3, DRAIN = 4, VENGEANCE = 5;
    public static final int BLOOD = 0xB0121E, BLOOD_DARK = 0x5A020C, BLOOD_LIGHT = 0xE04450;
    private static final int VENGEANCE_RELEASE = 44;

    private float vengeanceStored;
    private final Set<Integer> danceHit = new HashSet<>();

    public RevengeEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.RED, 6);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 450.0D)
                .add(Attributes.ARMOR, 10.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 5.0D)
                .add(Attributes.ATTACK_DAMAGE, 15.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        for (EntityDataAccessor<Integer> d : DATA_DRAIN) this.entityData.define(d, -1);
    }

    /** entities currently tethered by the hemorrhage beams (client side) */
    public List<Entity> getDrainTargets() {
        List<Entity> out = new ArrayList<>();
        for (EntityDataAccessor<Integer> d : DATA_DRAIN) {
            int id = this.entityData.get(d);
            if (id >= 0) {
                Entity e = this.level().getEntity(id);
                if (e != null) out.add(e);
            }
        }
        return out;
    }

    private void clearDrain() {
        for (EntityDataAccessor<Integer> d : DATA_DRAIN) this.entityData.set(d, -1);
    }

    @Override public com.krolasyon.bosses.faction.Faction houseOf() { return com.krolasyon.bosses.faction.Faction.BLOOD; }
    @Override public double meleeReach() { return 4.6; }
    @Override public double walkSpeed() { return 0.9; }
    @Override public double runSpeed() { return 1.4; }

    public Vec3 chestPos(float partial) { return this.getPosition(partial).add(0, 2.85, 0); }

    public Vec3 spearHand() { return position().add(right().scale(0.55)).add(forward().scale(0.4)).add(0, 3.7, 0); }

    // ------------------------------------------------------------------ abilities
    @Override
    protected int abilityDuration(int id) {
        return switch (id) {
            case MELEE -> 22;
            case LANCE -> 30;
            case TIDE -> 34;
            case DANCE -> 30;
            case DRAIN -> 50;
            case VENGEANCE -> 60;
            default -> 20;
        };
    }

    @Override
    protected int abilityCooldown(int id) {
        return switch (id) {
            case MELEE -> 28;
            case LANCE -> 140;
            case TIDE -> 200;
            case DANCE -> 180;
            case DRAIN -> 300;
            case VENGEANCE -> 480;
            default -> 100;
        };
    }

    @Override
    protected int chooseAbility(LivingEntity target, double distSqr) {
        double dist = Math.sqrt(distSqr);
        boolean melee = inMeleeRange(target, distSqr) && ready(MELEE);
        if (globalCooldown > 0 || this.tickCount % 8 != 0) return melee ? MELEE : -1;
        if (melee && random.nextFloat() < 0.5F) return MELEE;
        int[] opts = new int[6];
        int[] w = new int[6];
        int n = 0, total = 0;
        if (ready(LANCE) && dist > 5 && dist < 30 && this.hasLineOfSight(target)) { opts[n] = LANCE; w[n++] = 4; total += 4; }
        if (ready(TIDE) && dist < 12) { opts[n] = TIDE; w[n++] = 3; total += 3; }
        if (ready(DANCE) && dist < 14) { opts[n] = DANCE; w[n++] = 3; total += 3; }
        if (ready(DRAIN) && dist < 13 && getHealth() < getMaxHealth() * 0.9F) { opts[n] = DRAIN; w[n++] = 2; total += 2; }
        if (ready(VENGEANCE) && getHealth() < getMaxHealth() * 0.75F) { opts[n] = VENGEANCE; w[n++] = 2; total += 2; }
        if (n == 0 || random.nextFloat() > 0.6F) return melee ? MELEE : -1;
        int roll = random.nextInt(total);
        for (int i = 0; i < n; i++) {
            roll -= w[i];
            if (roll < 0) return opts[i];
        }
        return opts[0];
    }

    @Override
    protected void startAbility(int id, LivingEntity target) {
        super.startAbility(id, target);
        if (id == VENGEANCE) vengeanceStored = 0;
        if (id == DANCE) danceHit.clear();
    }

    @Override
    protected void endAbility() {
        if (currentAbility == DRAIN) clearDrain();
        super.endAbility();
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (!this.level().isClientSide() && currentAbility == VENGEANCE && abilityTick < VENGEANCE_RELEASE && amount > 0) {
            vengeanceStored += amount;
            amount *= 0.4F;
            ((ServerLevel) this.level()).sendParticles(dust(BLOOD_LIGHT, 1.8F), getX(), getY() + 2.5, getZ(), 12, 0.6, 1.0, 0.6, 0);
        }
        return super.hurt(src, amount);
    }

    @Override
    protected void tickAbility(int id, int t, @Nullable LivingEntity target) {
        ServerLevel sl = serverLevel();
        switch (id) {
            case MELEE -> {
                if (t == 4) sound(ModSounds.SPEAR_THRUST.get(), 1.6F, 0.9F);
                if (t == 9) {
                    // spear thrust: long narrow line in front
                    Vec3 f = forward();
                    for (int i = 1; i <= 10; i++) {
                        Vec3 p = position().add(f.scale(i * 0.5)).add(0, 2.2, 0);
                        sl.sendParticles(dust(i % 2 == 0 ? BLOOD : BLOOD_LIGHT, 1.3F), p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0);
                    }
                    for (LivingEntity e : hostilesIn(getBoundingBox().expandTowards(f.scale(5.2)).inflate(0.6, 0.2, 0.6))) {
                        Vec3 to = e.position().subtract(position()).multiply(1, 0, 1);
                        if (to.lengthSqr() > 0.01 && to.normalize().dot(f) < 0.45) continue;
                        hit(e, damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.85F, 1.0, 0.2);
                    }
                }
                if (t == 14) sound(ModSounds.BLADE_SLASH.get(), 1.6F, 1.0F);
                if (t == 16) bladeArc(sl, (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.7F, 4.2, -1, null);
            }
            case LANCE -> {
                if (t == 2) sound(ModSounds.VENGEANCE_CHARGE.get(), 1.2F, 1.6F);
                if (t < 14) {
                    Vec3 h = spearHand();
                    sl.sendParticles(dust(BLOOD, 1.2F), h.x, h.y, h.z, 3, 0.3, 0.3, 0.3, 0);
                }
                if (t == 14 && target != null) {
                    Vec3 from = spearHand();
                    Vec3 aim = target.getBoundingBox().getCenter().add(target.getDeltaMovement().scale(6));
                    BloodLanceEntity lance = new BloodLanceEntity(ModEntities.BLOOD_LANCE.get(), this.level());
                    lance.setup(this, from, aim.subtract(from), isPhase2());
                    this.level().addFreshEntity(lance);
                    sound(ModSounds.LANCE_THROW.get(), 2.0F, 1.0F);
                    if (isPhase2()) {
                        for (float a : new float[]{0.25F, -0.25F}) {
                            BloodLanceEntity l2 = new BloodLanceEntity(ModEntities.BLOOD_LANCE.get(), this.level());
                            l2.setup(this, from, aim.subtract(from).yRot(a), true);
                            this.level().addFreshEntity(l2);
                        }
                    }
                }
            }
            case TIDE -> {
                if (t == 4) sound(ModSounds.REVENGE_AMBIENT.get(), 2.0F, 0.7F);
                if (t < 14) sl.sendParticles(dust(BLOOD, 1.5F), getX(), getY() + 0.2, getZ(), 6, 1.5, 0.1, 1.5, 0);
                if (t == 14) {
                    sound(ModSounds.BLOOD_WAVE.get(), 3.0F, 0.8F);
                    float dmg = isPhase2() ? 12F : 9F;
                    double[] radii = isPhase2() ? new double[]{3, 5.5, 8, 10.5} : new double[]{3, 5.5, 8};
                    for (int ring = 0; ring < radii.length; ring++) {
                        int count = (int) (radii[ring] * 2.3);
                        double off = random.nextDouble() * Math.PI;
                        for (int i = 0; i < count; i++) {
                            double a = off + i * Math.PI * 2 / count;
                            EruptionEntity.spawn(this, getX() + Math.cos(a) * radii[ring], getZ() + Math.sin(a) * radii[ring], getY(), EruptionEntity.KIND_BLOOD, 1 + ring * 4, dmg);
                        }
                    }
                    for (int i = 0; i < 60; i++) {
                        double a = i * Math.PI * 2 / 60;
                        sl.sendParticles(dust(i % 2 == 0 ? BLOOD : BLOOD_DARK, 2.4F), getX() + Math.cos(a), getY() + 0.3, getZ() + Math.sin(a), 0, Math.cos(a), 0.12, Math.sin(a), 0.9);
                    }
                    for (LivingEntity e : hostilesAround(position(), 4.0)) hit(e, damageSources().mobAttack(this), dmg, 1.6, 0.6);
                }
            }
            case DANCE -> {
                if (t == 4 || t == 12 || t == 20) {
                    Vec3 dir = target != null ? target.position().subtract(position()).multiply(1, 0, 1).normalize() : forward();
                    double dist = target != null ? this.distanceTo(target) : 5;
                    double push = dist > 3 ? Math.min(1.5, 0.35 + dist * 0.12) : 0.2;
                    this.setDeltaMovement(dir.x * push, 0.15, dir.z * push);
                    this.hasImpulse = true;
                    sound(ModSounds.BLADE_SLASH.get(), 1.8F, 0.8F + t * 0.02F);
                }
                if (t == 6 || t == 14 || t == 22) {
                    float dmg = isPhase2() ? 10F : 8F;
                    bladeArc(sl, dmg, 4.4, t == 14 ? 1 : -1, danceHit);
                    danceHit.clear();
                    if (t == 22) {
                        Vec3 f = forward();
                        for (int i = 1; i <= 7; i++) {
                            Vec3 p = position().add(f.scale(1.5 + i * 1.3));
                            EruptionEntity.spawn(this, p.x, p.z, getY(), EruptionEntity.KIND_BLOOD, i, dmg * 0.8F);
                        }
                    }
                }
            }
            case DRAIN -> tickDrain(sl, t);
            case VENGEANCE -> tickVengeance(sl, t);
            default -> {}
        }
    }

    private void bladeArc(ServerLevel sl, float dmg, double reach, int side, @Nullable Set<Integer> once) {
        Vec3 f = forward();
        for (int i = 0; i < 16; i++) {
            double a = (i / 15.0 - 0.5) * Math.PI * 1.1 * side;
            Vec3 d = f.yRot((float) a).scale(reach * 0.85);
            sl.sendParticles(dust(i % 3 == 0 ? BLOOD_LIGHT : BLOOD, 1.7F), getX() + d.x, getY() + 1.4 + i * 0.05, getZ() + d.z, 1, 0, 0, 0, 0);
        }
        Vec3 c = position().add(f.scale(reach * 0.5)).add(0, 1.4, 0);
        sl.sendParticles(ParticleTypes.SWEEP_ATTACK, c.x, c.y, c.z, 2, 0.5, 0.2, 0.5, 0);
        for (LivingEntity e : hostilesIn(new AABB(c, c).inflate(reach * 0.6, 1.8, reach * 0.6))) {
            if (once != null && !once.add(e.getId())) continue;
            Vec3 to = e.position().subtract(position());
            if (to.horizontalDistanceSqr() > 0.01 && to.normalize().dot(f) < -0.2) continue;
            hit(e, damageSources().mobAttack(this), dmg, 0.8, 0.25);
            e.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
        }
    }

    private void tickDrain(ServerLevel sl, int t) {
        if (t == 4) sound(ModSounds.DRAIN.get(), 2.5F, 1.0F);
        if (t == 8) {
            List<LivingEntity> near = hostilesAround(position(), 14);
            near.removeIf(e -> !this.hasLineOfSight(e));
            near.sort((a, b) -> Double.compare(a.distanceToSqr(this), b.distanceToSqr(this)));
            for (int i = 0; i < DATA_DRAIN.length; i++) this.entityData.set(DATA_DRAIN[i], i < near.size() ? near.get(i).getId() : -1);
        }
        if (t == 26) sound(ModSounds.DRAIN.get(), 2.0F, 0.85F);
        if (t >= 10 && t <= 46) {
            Vec3 chest = chestPos(1F);
            for (int i = 0; i < DATA_DRAIN.length; i++) {
                int id = this.entityData.get(DATA_DRAIN[i]);
                if (id < 0) continue;
                Entity e = this.level().getEntity(id);
                if (!(e instanceof LivingEntity le) || !le.isAlive() || le.distanceToSqr(this) > 20 * 20) {
                    this.entityData.set(DATA_DRAIN[i], -1);
                    continue;
                }
                Vec3 from = le.getBoundingBox().getCenter();
                Vec3 d = chest.subtract(from);
                // blood droplets flowing toward the boss
                for (int k = 0; k < 2; k++) {
                    double u = random.nextDouble();
                    Vec3 p = from.add(d.scale(u));
                    sl.sendParticles(dust(k == 0 ? BLOOD : BLOOD_LIGHT, 1.2F), p.x, p.y, p.z, 0, d.x, d.y, d.z, 0.04);
                }
                if (t % 5 == 0) {
                    float dmg = isPhase2() ? 3.5F : 2.5F;
                    le.invulnerableTime = 0;
                    if (le.hurt(damageSources().indirectMagic(this, this), dmg)) this.heal(dmg * 0.8F);
                    le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 2));
                    le.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0));
                }
            }
        }
        if (t == 47) clearDrain();
    }

    private void tickVengeance(ServerLevel sl, int t) {
        if (t == 0) sound(ModSounds.VENGEANCE_CHARGE.get(), 3.0F, 0.8F);
        if (t < VENGEANCE_RELEASE) {
            double r = 3.5 - (t % 10) * 0.3;
            for (int i = 0; i < 4; i++) {
                double a = t * 0.4 + i * Math.PI / 2;
                sl.sendParticles(dust(i % 2 == 0 ? BLOOD : BLOOD_DARK, 1.6F), getX() + Math.cos(a) * r, getY() + 0.5 + (t % 20) * 0.15, getZ() + Math.sin(a) * r, 0,
                        -Math.cos(a), 0.05, -Math.sin(a), 0.2);
            }
            if (t % 10 == 0) sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, getX(), getY() + 3, getZ(), 3, 0.5, 0.5, 0.5, 0.1);
        }
        if (t == VENGEANCE_RELEASE) {
            sound(ModSounds.VENGEANCE_BURST.get(), 4.0F, 1.0F);
            float dmg = 8F + Math.min(vengeanceStored * 0.9F, 34F);
            double r = 10;
            sl.sendParticles(ParticleTypes.FLASH, getX(), getY() + 2, getZ(), 2, 0, 0, 0, 0);
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 1, getZ(), 1, 0, 0, 0, 0);
            for (int ring = 0; ring < 3; ring++) {
                for (int i = 0; i < 72; i++) {
                    double a = i * Math.PI * 2 / 72;
                    sl.sendParticles(dust(ring == 1 ? BLOOD_LIGHT : BLOOD, 2.8F), getX(), getY() + 0.5 + ring * 1.2, getZ(), 0, Math.cos(a), 0.05, Math.sin(a), 1.1 + ring * 0.2);
                }
            }
            for (int ring = 1; ring <= 3; ring++) {
                int count = ring * 7;
                for (int i = 0; i < count; i++) {
                    double a = i * Math.PI * 2 / count;
                    EruptionEntity.spawn(this, getX() + Math.cos(a) * ring * 3, getZ() + Math.sin(a) * ring * 3, getY(), EruptionEntity.KIND_BLOOD, ring * 2, dmg * 0.5F);
                }
            }
            for (LivingEntity e : hostilesAround(position(), r)) {
                float falloff = (float) (1.0 - 0.5 * e.distanceTo(this) / r);
                hit(e, damageSources().indirectMagic(this, this), dmg * falloff, 2.2, 0.8);
                e.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
            }
            this.heal(vengeanceStored * 0.3F);
            vengeanceStored = 0;
        }
    }

    @Override
    protected void onPhase2() {
        sound(ModSounds.REVENGE_PHASE.get(), 4.0F, 1.0F);
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.33D);
        ServerLevel sl = serverLevel();
        sl.sendParticles(ParticleTypes.FLASH, getX(), getY() + 3, getZ(), 2, 0, 0, 0, 0);
        for (int i = 0; i < 90; i++) {
            double a = i * Math.PI * 2 / 90;
            sl.sendParticles(dust(i % 2 == 0 ? BLOOD : BLOOD_LIGHT, 2.6F), getX(), getY() + 0.5, getZ(), 0, Math.cos(a), 0.2, Math.sin(a), 1.0);
        }
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI / 6;
            EruptionEntity.spawn(this, getX() + Math.cos(a) * 4, getZ() + Math.sin(a) * 4, getY(), EruptionEntity.KIND_BLOOD, 2, 8F);
        }
        for (LivingEntity e : hostilesAround(position(), 8)) hit(e, damageSources().mobAttack(this), 6F, 2.0, 0.6);
        this.heal(20F);
    }

    // ------------------------------------------------------------------ fx
    @Override
    protected void ambientFx() {
        Level l = this.level();
        boolean p2 = isPhase2();
        // dripping blood from the body and weapons
        for (int i = 0; i < (p2 ? 3 : 1); i++) {
            l.addParticle(dust(random.nextBoolean() ? BLOOD : BLOOD_DARK, 1.0F), getX() + (random.nextDouble() - 0.5) * 1.6, getY() + 0.8 + random.nextDouble() * 2.8,
                    getZ() + (random.nextDouble() - 0.5) * 1.6, 0, -0.08, 0);
        }
        if (random.nextInt(3) == 0) {
            double a = random.nextDouble() * Math.PI * 2, r = 0.8 + random.nextDouble() * 1.2;
            l.addParticle(dust(BLOOD, 1.3F), getX() + Math.cos(a) * r, getY() + 0.05, getZ() + Math.sin(a) * r, 0, 0.06, 0);
        }
        if (p2 && random.nextInt(8) == 0) l.addParticle(ParticleTypes.DAMAGE_INDICATOR, getX(), getY() + 3.4, getZ(), 0, 0.1, 0);
        if (clientAnimId == VENGEANCE) {
            for (int i = 0; i < 2; i++) l.addParticle(dust(BLOOD_LIGHT, 1.5F), getX() + (random.nextDouble() - 0.5) * 3, getY() + random.nextDouble() * 4, getZ() + (random.nextDouble() - 0.5) * 3, 0, 0, 0);
        }
        if (phaseFlash > 0) {
            for (int i = 0; i < 4; i++) l.addParticle(dust(BLOOD_LIGHT, 2F), getX() + (random.nextDouble() - 0.5) * 3, getY() + random.nextDouble() * 4, getZ() + (random.nextDouble() - 0.5) * 3, 0, 0.1, 0);
        }
    }

    @Override
    protected void deathFx(int t) {
        if (this.level().isClientSide()) {
            for (int i = 0; i < 3; i++) {
                this.level().addParticle(dust(random.nextBoolean() ? BLOOD : BLOOD_DARK, 1.6F), getX() + (random.nextDouble() - 0.5) * 2, getY() + random.nextDouble() * 3,
                        getZ() + (random.nextDouble() - 0.5) * 2, 0, -0.05, 0);
            }
        } else {
            if (t == 1) sound(ModSounds.REVENGE_DEATH.get(), 4.0F, 1.0F);
            if (t == 25) sound(ModSounds.BLOOD_WAVE.get(), 2.5F, 0.6F);
            if (t == DEATH_TICKS - 1) {
                ServerLevel sl = serverLevel();
                sl.sendParticles(dust(BLOOD, 3F), getX(), getY() + 1, getZ(), 140, 1.8, 1.0, 1.8, 0);
                sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, getX(), getY() + 1, getZ(), 20, 1, 1, 1, 0.2);
                sound(ModSounds.VENGEANCE_BURST.get(), 2.5F, 0.6F);
            }
        }
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource src, int looting, boolean hitByPlayer) {
        super.dropCustomDeathLoot(src, looting, hitByPlayer);
        this.spawnAtLocation(new ItemStack(Items.NETHER_STAR));
        this.spawnAtLocation(new ItemStack(Items.REDSTONE_BLOCK, 4 + random.nextInt(4)));
        this.spawnAtLocation(new ItemStack(Items.NETHERITE_SCRAP, 2 + random.nextInt(2)));
        this.spawnAtLocation(new ItemStack(Items.DIAMOND, 3 + random.nextInt(3)));
    }

    // ------------------------------------------------------------------ sounds
    @Override protected SoundEvent getAmbientSound() { return ModSounds.REVENGE_AMBIENT.get(); }
    @Override protected SoundEvent getHurtSound(DamageSource src) { return ModSounds.REVENGE_HURT.get(); }
    @Override protected SoundEvent getDeathSound() { return null; }
    @Override protected float getSoundVolume() { return 2.0F; }
    @Override public int getAmbientSoundInterval() { return 160; }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(ModSounds.REVENGE_STEP.get(), 0.9F, 0.85F + random.nextFloat() * 0.2F);
    }
}
