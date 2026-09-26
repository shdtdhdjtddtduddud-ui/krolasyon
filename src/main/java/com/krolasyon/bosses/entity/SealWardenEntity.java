package com.krolasyon.bosses.entity;

import com.krolasyon.bosses.registry.ModEntities;
import com.krolasyon.bosses.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
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

/** Mühür Bekçisi — crystal-crowned seal sorcerer with three floating eye orbs. */
public class SealWardenEntity extends BossEntity {
    private static final EntityDataAccessor<Integer> DATA_BEAM = SynchedEntityData.defineId(SealWardenEntity.class, EntityDataSerializers.INT);

    public static final int MELEE = 0, LASER = 1, CRYSTALS = 2, PRISON = 3, BLINK = 4, SUMMON = 5;
    public static final int PINK = 0xFF8ADB, PINK_LIGHT = 0xFFD5F9, MAGENTA = 0xC23C9E;

    public SealWardenEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.PINK, 6);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 420.0D)
                .add(Attributes.ARMOR, 12.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 6.0D)
                .add(Attributes.ATTACK_DAMAGE, 14.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.27D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_BEAM, -1);
    }

    @Nullable
    public Entity getBeamTarget() {
        int id = this.entityData.get(DATA_BEAM);
        return id < 0 ? null : this.level().getEntity(id);
    }

    private void setBeam(@Nullable Entity e) {
        this.entityData.set(DATA_BEAM, e == null ? -1 : e.getId());
    }

    @Override public double meleeReach() { return 4.2; }
    @Override public double walkSpeed() { return 0.9; }
    @Override public double runSpeed() { return 1.35; }

    /** world positions of the three seal orbs while casting: top, right, left */
    public Vec3[] orbPositions(float partial) {
        Vec3 p = this.getPosition(partial);
        float yaw = Mth.rotLerp(partial, this.yBodyRotO, this.yBodyRot) * Mth.DEG_TO_RAD;
        Vec3 f = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
        Vec3 r = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));
        return new Vec3[]{
                p.add(0, 4.15, 0).add(f.scale(0.15)),
                p.add(r.scale(1.15)).add(0, 2.3, 0).add(f.scale(0.15)),
                p.add(r.scale(-1.15)).add(0, 2.3, 0).add(f.scale(0.15))};
    }

    // ------------------------------------------------------------------ abilities
    @Override
    protected int abilityDuration(int id) {
        return switch (id) {
            case MELEE -> 20;
            case LASER -> 40;
            case CRYSTALS -> 32;
            case PRISON -> 30;
            case BLINK -> 20;
            case SUMMON -> 40;
            default -> 20;
        };
    }

    @Override
    protected int abilityCooldown(int id) {
        return switch (id) {
            case MELEE -> 26;
            case LASER -> 170;
            case CRYSTALS -> 200;
            case PRISON -> 260;
            case BLINK -> 150;
            case SUMMON -> 560;
            default -> 100;
        };
    }

    @Override
    protected int chooseAbility(LivingEntity target, double distSqr) {
        double dist = Math.sqrt(distSqr);
        boolean melee = inMeleeRange(target, distSqr) && ready(MELEE);
        if (globalCooldown > 0 || this.tickCount % 8 != 0) return melee ? MELEE : -1;
        if (melee && random.nextFloat() < 0.55F) return MELEE;
        int[] opts = new int[6];
        int[] w = new int[6];
        int n = 0, total = 0;
        if (ready(LASER) && dist < 24 && this.hasLineOfSight(target)) { opts[n] = LASER; w[n++] = 3; total += 3; }
        if (ready(CRYSTALS) && dist < 15) { opts[n] = CRYSTALS; w[n++] = 3; total += 3; }
        if (ready(PRISON) && dist > 3.5 && dist < 22) { opts[n] = PRISON; w[n++] = 2; total += 2; }
        if (ready(BLINK) && (dist > 11 || dist < 3)) { opts[n] = BLINK; w[n++] = dist > 11 ? 4 : 1; total += w[n - 1]; }
        if (ready(SUMMON) && countWatchers() == 0) { opts[n] = SUMMON; w[n++] = 2; total += 2; }
        if (n == 0 || random.nextFloat() > 0.6F) return melee ? MELEE : -1;
        int roll = random.nextInt(total);
        for (int i = 0; i < n; i++) {
            roll -= w[i];
            if (roll < 0) return opts[i];
        }
        return opts[0];
    }

    private int countWatchers() {
        return this.level().getEntitiesOfClass(WatcherOrbEntity.class, this.getBoundingBox().inflate(40), w -> w.getOwnerId() == this.getId()).size();
    }

    @Override
    protected void endAbility() {
        if (currentAbility == LASER) setBeam(null);
        super.endAbility();
    }

    @Override
    protected void tickAbility(int id, int t, @Nullable LivingEntity target) {
        ServerLevel sl = serverLevel();
        switch (id) {
            case MELEE -> {
                if (t == 2) sound(ModSounds.WARDEN_SWIPE.get(), 1.6F, 0.8F + random.nextFloat() * 0.2F);
                if (t == 7 || t == 11) {
                    Vec3 f = forward();
                    Vec3 c = position().add(f.scale(2.0)).add(0, 1.6, 0);
                    for (int i = 0; i < 14; i++) {
                        double a = (i / 13.0 - 0.5) * Math.PI * (t == 7 ? 1 : -1);
                        Vec3 d = f.yRot((float) a).scale(3.4);
                        sl.sendParticles(dust(i % 2 == 0 ? PINK : PINK_LIGHT, 1.6F), getX() + d.x, getY() + 2.0 - i * 0.07, getZ() + d.z, 1, 0, 0, 0, 0);
                    }
                    sl.sendParticles(ParticleTypes.SWEEP_ATTACK, c.x, c.y, c.z, 2, 0.6, 0.2, 0.6, 0);
                    float dmg = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * (t == 7 ? 0.7F : 0.8F);
                    for (LivingEntity e : hostilesIn(new AABB(c, c).inflate(2.6, 1.8, 2.6))) {
                        Vec3 to = e.position().subtract(position());
                        if (to.horizontalDistanceSqr() > 0.01 && to.normalize().dot(f) < -0.2) continue;
                        hit(e, damageSources().mobAttack(this), dmg, 0.9, 0.3);
                    }
                }
            }
            case LASER -> tickLaser(sl, t, target);
            case CRYSTALS -> tickCrystals(sl, t, target);
            case PRISON -> {
                if (t == 2) sound(ModSounds.PRISON_FORM.get(), 2.0F, 1.0F);
                if (t < 10) {
                    for (Vec3 o : orbPositions(1F))
                        sl.sendParticles(ParticleTypes.REVERSE_PORTAL, o.x, o.y, o.z, 4, 0.3, 0.3, 0.3, 0.05);
                }
                if (t == 10 && target != null) {
                    Double gy = EruptionEntity.findGround(this.level(), target.getX(), target.getZ(), target.getY() + 3, target.getY() - 8);
                    SealPrisonEntity prison = new SealPrisonEntity(ModEntities.SEAL_PRISON.get(), this.level());
                    prison.setup(this, target.getX(), gy == null ? target.getY() : gy, target.getZ(), isPhase2());
                    this.level().addFreshEntity(prison);
                    Vec3 from = orbPositions(1F)[0];
                    Vec3 to = target.position();
                    for (int i = 0; i <= 20; i++) {
                        Vec3 p = from.lerp(to, i / 20.0);
                        sl.sendParticles(dust(PINK_LIGHT, 1.2F), p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0);
                    }
                }
            }
            case BLINK -> tickBlink(sl, t, target);
            case SUMMON -> {
                if (t == 4) sound(ModSounds.SUMMON.get(), 2.5F, 1.0F);
                if (t < 22) {
                    double r = 3.5 - t * 0.12;
                    for (int i = 0; i < 3; i++) {
                        double a = t * 0.35 + i * Math.PI * 2 / 3;
                        sl.sendParticles(dust(PINK, 1.8F), getX() + Math.cos(a) * r, getY() + 1 + t * 0.12, getZ() + Math.sin(a) * r, 2, 0.05, 0.05, 0.05, 0);
                    }
                }
                if (t == 22) {
                    int count = isPhase2() ? 4 : 3;
                    for (int i = 0; i < count; i++) {
                        WatcherOrbEntity w = new WatcherOrbEntity(ModEntities.WATCHER.get(), this.level());
                        w.setup(this, i, count);
                        this.level().addFreshEntity(w);
                    }
                    sl.sendParticles(ParticleTypes.FLASH, getX(), getY() + 4, getZ(), 1, 0, 0, 0, 0);
                    sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 3.5, getZ(), 60, 1.5, 1.0, 1.5, 0.15);
                }
            }
            default -> {}
        }
    }

    private void tickLaser(ServerLevel sl, int t, @Nullable LivingEntity target) {
        Vec3[] orbs = orbPositions(1F);
        if (t == 0) sound(ModSounds.LASER_CHARGE.get(), 2.5F, 1.0F);
        if (t < 14) {
            for (Vec3 o : orbs) {
                for (int i = 0; i < 3; i++) {
                    Vec3 d = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).normalize().scale(1.6);
                    sl.sendParticles(dust(i == 0 ? PINK_LIGHT : PINK, 1.3F), o.x + d.x, o.y + d.y, o.z + d.z, 0, -d.x, -d.y, -d.z, 0.12);
                }
            }
        }
        if (t == 14) {
            sound(ModSounds.LASER_FIRE.get(), 3.0F, 1.0F);
            setBeam(target);
        }
        if (t >= 14 && t <= 34) {
            if (target == null || !target.isAlive()) {
                setBeam(null);
                return;
            }
            Vec3 aim = target.getBoundingBox().getCenter();
            if (t % 2 == 0) {
                float dmg = isPhase2() ? 3.5F : 2.6F;
                java.util.Set<LivingEntity> struck = new java.util.HashSet<>();
                for (Vec3 o : orbs) {
                    AABB span = new AABB(o, aim).inflate(1.0);
                    for (LivingEntity e : hostilesIn(span)) {
                        if (e.getBoundingBox().inflate(0.35).clip(o, aim).isPresent() || e == target) struck.add(e);
                    }
                }
                for (LivingEntity e : struck) {
                    e.invulnerableTime = 0;
                    e.hurt(damageSources().indirectMagic(this, this), dmg);
                    if (t % 6 == 0) e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
                }
            }
            sl.sendParticles(dust(PINK_LIGHT, 2.0F), aim.x, aim.y, aim.z, 6, 0.3, 0.3, 0.3, 0);
            sl.sendParticles(ParticleTypes.END_ROD, aim.x, aim.y, aim.z, 2, 0.2, 0.2, 0.2, 0.12);
            if (t % 5 == 0) sl.sendParticles(ParticleTypes.FLASH, aim.x, aim.y, aim.z, 1, 0, 0, 0, 0);
        }
        if (t == 35) setBeam(null);
    }

    private void tickCrystals(ServerLevel sl, int t, @Nullable LivingEntity target) {
        if (t == 4) sound(ModSounds.CRYSTAL_ERUPT.get(), 1.2F, 1.4F);
        if (t == 14) {
            sound(ModSounds.CRYSTAL_ERUPT.get(), 3.0F, 0.8F);
            float dmg = isPhase2() ? 13F : 10F;
            // shockwave rings of crystals
            double[] radii = isPhase2() ? new double[]{3, 5.5, 8, 10.5} : new double[]{3, 5.5, 8};
            for (int ring = 0; ring < radii.length; ring++) {
                int count = (int) (radii[ring] * 2.2);
                double off = random.nextDouble() * Math.PI;
                for (int i = 0; i < count; i++) {
                    double a = off + i * Math.PI * 2 / count;
                    EruptionEntity.spawn(this, getX() + Math.cos(a) * radii[ring], getZ() + Math.sin(a) * radii[ring],
                            getY(), EruptionEntity.KIND_CRYSTAL, 2 + ring * 5, dmg);
                }
            }
            // crystal line racing toward the target
            if (target != null) {
                Vec3 dir = target.position().subtract(position()).multiply(1, 0, 1).normalize();
                for (int i = 1; i <= 12; i++) {
                    Vec3 p = position().add(dir.scale(1.5 + i * 1.35));
                    EruptionEntity.spawn(this, p.x, p.z, getY(), EruptionEntity.KIND_CRYSTAL, 1 + i, dmg);
                }
            }
            for (int i = 0; i < 40; i++) {
                double a = i * Math.PI * 2 / 40;
                sl.sendParticles(dust(PINK, 2.0F), getX() + Math.cos(a) * 1.5, getY() + 0.2, getZ() + Math.sin(a) * 1.5, 0, Math.cos(a), 0.05, Math.sin(a), 0.5);
            }
            sl.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.5, getZ(), 2, 0.8, 0.1, 0.8, 0);
        }
    }

    private void tickBlink(ServerLevel sl, int t, @Nullable LivingEntity target) {
        if (t == 0) sound(ModSounds.BLINK.get(), 2.0F, 0.9F);
        if (t < 9) {
            for (int i = 0; i < 6; i++) {
                Vec3 d = new Vec3(random.nextGaussian(), random.nextGaussian() * 1.5, random.nextGaussian()).normalize().scale(2.5);
                sl.sendParticles(dust(i % 2 == 0 ? PINK : MAGENTA, 1.5F), getX() + d.x, getY() + 2 + d.y, getZ() + d.z, 0, -d.x, -d.y, -d.z, 0.15);
            }
        }
        if (t == 9 && target != null) {
            Vec3 old = position();
            Vec3 look = target.getLookAngle().multiply(1, 0, 1);
            if (look.lengthSqr() < 1.0E-3) look = target.position().subtract(old).multiply(1, 0, 1);
            look = look.normalize();
            Vec3[] tries = {look.scale(-3.2), look.yRot(1.2F).scale(-3.2), look.yRot(-1.2F).scale(-3.2), look.scale(3.0), Vec3.ZERO};
            for (Vec3 off : tries) {
                double x = target.getX() + off.x, z = target.getZ() + off.z;
                Double gy = EruptionEntity.findGround(this.level(), x, z, target.getY() + 4, target.getY() - 6);
                if (gy == null) continue;
                AABB box = this.getDimensions(this.getPose()).makeBoundingBox(x, gy, z);
                if (this.level().noCollision(this, box)) {
                    this.teleportTo(x, gy, z);
                    break;
                }
            }
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, old.x, old.y + 2, old.z, 80, 0.5, 1.5, 0.5, 0.3);
            sl.sendParticles(ParticleTypes.FLASH, getX(), getY() + 2, getZ(), 1, 0, 0, 0, 0);
            sl.sendParticles(dust(PINK_LIGHT, 2.5F), getX(), getY() + 2, getZ(), 50, 0.6, 1.6, 0.6, 0);
            soundAt(position(), ModSounds.BLINK.get(), 2.5F, 1.3F);
            faceTowards(target);
        }
        if (t == 12) {
            sound(ModSounds.PRISON_BURST.get(), 1.6F, 1.5F);
            for (int i = 0; i < 48; i++) {
                double a = i * Math.PI * 2 / 48;
                sl.sendParticles(dust(i % 2 == 0 ? PINK : PINK_LIGHT, 1.8F), getX() + Math.cos(a) * 0.8, getY() + 0.3, getZ() + Math.sin(a) * 0.8, 0, Math.cos(a), 0.02, Math.sin(a), 0.6);
            }
            for (LivingEntity e : hostilesAround(position(), 3.8)) {
                hit(e, damageSources().indirectMagic(this, this), isPhase2() ? 14F : 11F, 1.4, 0.45);
            }
        }
    }

    @Override
    protected void onPhase2() {
        sound(ModSounds.WARDEN_PHASE.get(), 4.0F, 1.0F);
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.32D);
        ServerLevel sl = serverLevel();
        sl.sendParticles(ParticleTypes.FLASH, getX(), getY() + 3, getZ(), 2, 0, 0, 0, 0);
        for (int i = 0; i < 90; i++) {
            double a = i * Math.PI * 2 / 90;
            sl.sendParticles(dust(i % 3 == 0 ? PINK_LIGHT : PINK, 2.5F), getX() + Math.cos(a), getY() + 0.5, getZ() + Math.sin(a), 0, Math.cos(a), 0.1, Math.sin(a), 0.9);
        }
        for (LivingEntity e : hostilesAround(position(), 9)) hit(e, damageSources().indirectMagic(this, this), 6F, 2.0, 0.6);
        this.heal(20F);
    }

    // ------------------------------------------------------------------ fx
    @Override
    protected void ambientFx() {
        Level l = this.level();
        Vec3[] orbs = orbPositions(1F);
        boolean p2 = isPhase2();
        for (Vec3 o : orbs) {
            if (random.nextInt(p2 ? 2 : 4) == 0)
                l.addParticle(dust(random.nextBoolean() ? PINK : PINK_LIGHT, 0.9F), o.x + (random.nextDouble() - 0.5) * 0.7, o.y + (random.nextDouble() - 0.5) * 0.7, o.z + (random.nextDouble() - 0.5) * 0.7, 0, 0.01, 0);
        }
        if (random.nextInt(12) == 0) {
            l.addParticle(ParticleTypes.END_ROD, getX() + (random.nextDouble() - 0.5) * 0.4, getY() + 4.2 + random.nextDouble() * 0.5, getZ() + (random.nextDouble() - 0.5) * 0.4, 0, 0.02, 0);
        }
        if (p2 && random.nextInt(2) == 0) {
            l.addParticle(ParticleTypes.REVERSE_PORTAL, getX() + (random.nextDouble() - 0.5) * 2, getY() + random.nextDouble() * 3.5, getZ() + (random.nextDouble() - 0.5) * 2, 0, 0.05, 0);
        }
        if (clientAnimId == LASER || clientAnimId == SUMMON) {
            for (Vec3 o : orbs) if (random.nextInt(3) == 0) l.addParticle(ParticleTypes.END_ROD, o.x, o.y, o.z, (random.nextDouble() - 0.5) * 0.1, (random.nextDouble() - 0.5) * 0.1, (random.nextDouble() - 0.5) * 0.1);
        }
        if (phaseFlash > 0) {
            for (int i = 0; i < 4; i++) l.addParticle(dust(PINK_LIGHT, 2F), getX() + (random.nextDouble() - 0.5) * 3, getY() + random.nextDouble() * 4, getZ() + (random.nextDouble() - 0.5) * 3, 0, 0.1, 0);
        }
    }

    @Override
    protected void deathFx(int t) {
        if (this.level().isClientSide()) {
            for (int i = 0; i < 3; i++) {
                ParticleOptions p = i == 0 ? ParticleTypes.END_ROD : dust(random.nextBoolean() ? PINK : PINK_LIGHT, 1.5F);
                this.level().addParticle(p, getX() + (random.nextDouble() - 0.5) * 1.5, getY() + random.nextDouble() * 3.5, getZ() + (random.nextDouble() - 0.5) * 1.5, 0, 0.05 + random.nextDouble() * 0.05, 0);
            }
        } else {
            if (t == 1) sound(ModSounds.WARDEN_DEATH.get(), 4.0F, 1.0F);
            if (t == 26) sound(ModSounds.CRYSTAL_ERUPT.get(), 2.0F, 1.6F);
            if (t == DEATH_TICKS - 1) {
                ServerLevel sl = serverLevel();
                sl.sendParticles(ParticleTypes.FLASH, getX(), getY() + 1, getZ(), 3, 0.5, 0.5, 0.5, 0);
                sl.sendParticles(dust(PINK, 3F), getX(), getY() + 1, getZ(), 120, 1.5, 1.2, 1.5, 0);
                sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1, getZ(), 60, 0.5, 0.5, 0.5, 0.35);
                sound(ModSounds.PRISON_BURST.get(), 3.0F, 0.7F);
            }
        }
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource src, int looting, boolean hitByPlayer) {
        super.dropCustomDeathLoot(src, looting, hitByPlayer);
        this.spawnAtLocation(new ItemStack(Items.NETHER_STAR));
        this.spawnAtLocation(new ItemStack(Items.AMETHYST_SHARD, 24 + random.nextInt(16)));
        this.spawnAtLocation(new ItemStack(Items.ENDER_PEARL, 4 + random.nextInt(4)));
        this.spawnAtLocation(new ItemStack(Items.DIAMOND, 3 + random.nextInt(3)));
    }

    // ------------------------------------------------------------------ sounds
    @Override protected SoundEvent getAmbientSound() { return ModSounds.WARDEN_AMBIENT.get(); }
    @Override protected SoundEvent getHurtSound(DamageSource src) { return ModSounds.WARDEN_HURT.get(); }
    @Override protected SoundEvent getDeathSound() { return null; }
    @Override protected float getSoundVolume() { return 2.0F; }
    @Override public int getAmbientSoundInterval() { return 160; }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(ModSounds.WARDEN_STEP.get(), 0.9F, 0.9F + random.nextFloat() * 0.2F);
    }
}
