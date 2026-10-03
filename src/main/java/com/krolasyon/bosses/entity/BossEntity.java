package com.krolasyon.bosses.entity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;

/**
 * Shared boss logic: boss bar, target selection (players + every monster around), ability state machine
 * synchronised to the client through entity events, phase 2, long death animation.
 */
public abstract class BossEntity extends Monster {
    private static final EntityDataAccessor<Boolean> DATA_RUNNING = SynchedEntityData.defineId(BossEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_PHASE2 = SynchedEntityData.defineId(BossEntity.class, EntityDataSerializers.BOOLEAN);

    public static final byte EVENT_ANIM_BASE = 100;
    public static final byte EVENT_PHASE2 = 99;
    public static final int DEATH_TICKS = 60;

    protected final ServerBossEvent bossEvent;

    // server ability state
    protected int currentAbility = -1;
    protected int abilityTick;
    protected final int[] cooldowns;
    protected int globalCooldown = 40;
    @Nullable protected LivingEntity abilityTarget;

    // client animation state
    public int clientAnimId = -1;
    public float clientAnimStart;
    public float runBlend, prevRunBlend;
    public int phaseFlash;

    /** false for ordinary (non boss) ability mobs: no boss bar, may despawn, can be pushed */
    protected final boolean isBoss;

    protected BossEntity(EntityType<? extends Monster> type, Level level, BossEvent.BossBarColor color, int abilityCount) {
        this(type, level, color, abilityCount, true);
    }

    protected BossEntity(EntityType<? extends Monster> type, Level level, BossEvent.BossBarColor color, int abilityCount, boolean boss) {
        super(type, level);
        this.isBoss = boss;
        this.bossEvent = new ServerBossEvent(this.getDisplayName(), color, BossEvent.BossBarOverlay.NOTCHED_10);
        this.bossEvent.setDarkenScreen(boss);
        this.cooldowns = new int[abilityCount];
        if (boss) {
            this.xpReward = 400;
            this.setPersistenceRequired();
            this.setMaxUpStep(1.5F);
        }
    }

    public boolean isBossMob() { return isBoss; }

    /** ordinary mobs have no second phase */
    protected boolean hasPhases() { return isBoss; }

    protected int deathTicks() { return DEATH_TICKS; }

    /** players this mob picks as a target on its own */
    protected boolean wantsToAttackPlayer(Player p) { return true; }

    // ------------------------------------------------------------------ setup
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_RUNNING, false);
        this.entityData.define(DATA_PHASE2, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new CastGoal(this));
        this.goalSelector.addGoal(2, new ChaseGoal(this));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.7D));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, e -> e instanceof Player p && wantsToAttackPlayer(p)));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 5, false, false, this::isMonsterPrey));
    }

    /** every monster around is prey (except bosses of the same kind). */
    protected boolean isMonsterPrey(LivingEntity e) {
        return e instanceof Enemy && e.getType() != this.getType() && !(e instanceof Player) && e.isAlive();
    }

    /** who area attacks are allowed to hurt: players, all monsters and the current target — never itself or passive mobs. */
    public boolean isHostileTo(Entity e) {
        if (!(e instanceof LivingEntity le) || e == this || !e.isAlive()) return false;
        if (e.getType() == this.getType()) return false;
        if (e instanceof Player p) return !p.isCreative() && !p.isSpectator();
        return e instanceof Enemy || e == this.getTarget() || le.getLastHurtMob() == this || (le instanceof Mob m && m.getTarget() == this);
    }

    public List<LivingEntity> hostilesIn(AABB box) {
        return this.level().getEntitiesOfClass(LivingEntity.class, box, this::isHostileTo);
    }

    public List<LivingEntity> hostilesAround(Vec3 c, double r) {
        return this.level().getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r, r * 0.8, r),
                e -> this.isHostileTo(e) && e.position().distanceToSqr(c) <= r * r * 1.2);
    }

    @Override
    public boolean removeWhenFarAway(double dist) { return isBoss ? false : super.removeWhenFarAway(dist); }

    @Override
    public AABB getBoundingBoxForCulling() { return this.getBoundingBox().inflate(3.0D); }

    @Override
    public boolean causeFallDamage(float dist, float mult, DamageSource src) { return false; }

    @Override
    protected boolean shouldDespawnInPeaceful() { return true; }

    @Override
    public boolean canChangeDimensions() { return false; }

    @Override
    public boolean isPushable() { return !isBoss; }

    @Override
    protected void doPush(Entity e) {
        if (e instanceof LivingEntity && isHostileTo(e)) super.doPush(e);
    }

    // ------------------------------------------------------------------ data
    public boolean isRunning() { return this.entityData.get(DATA_RUNNING); }
    public void setRunning(boolean b) { if (b != isRunning()) this.entityData.set(DATA_RUNNING, b); }
    public boolean isPhase2() { return this.entityData.get(DATA_PHASE2); }
    public boolean isCasting() { return currentAbility >= 0; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Phase2", isPhase2());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_PHASE2, tag.getBoolean("Phase2"));
        if (this.hasCustomName()) this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        if (isBoss) this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    // ------------------------------------------------------------------ abilities
    /** ability 0 is always the melee attack. */
    protected abstract int abilityDuration(int id);
    protected abstract int abilityCooldown(int id);
    protected abstract void tickAbility(int id, int t, @Nullable LivingEntity target);
    /** returns chosen ability or -1. */
    protected abstract int chooseAbility(LivingEntity target, double distSqr);
    protected abstract void onPhase2();
    protected abstract void ambientFx();
    protected abstract void deathFx(int t);
    public abstract double meleeReach();
    public abstract double walkSpeed();
    public abstract double runSpeed();

    protected boolean ready(int id) { return cooldowns[id] <= 0; }

    protected void startAbility(int id, LivingEntity target) {
        this.currentAbility = id;
        this.abilityTick = 0;
        this.abilityTarget = target;
        int cd = abilityCooldown(id);
        this.cooldowns[id] = isPhase2() ? (int) (cd * 0.6F) : cd;
        if (id != 0) this.globalCooldown = isPhase2() ? 25 : 45;
        this.getNavigation().stop();
        this.setRunning(false);
        this.level().broadcastEntityEvent(this, (byte) (EVENT_ANIM_BASE + id));
    }

    public int debugAbility() { return currentAbility; }
    public int debugAbilityTick() { return abilityTick; }

    /** used by the CI visual self-test to trigger a specific ability */
    public void debugForceAbility(int id, LivingEntity target) {
        this.setTarget(target);
        if (currentAbility >= 0) endAbility();
        java.util.Arrays.fill(cooldowns, 100000);
        this.globalCooldown = 100000;
        startAbility(id, target);
    }

    protected void endAbility() {
        this.currentAbility = -1;
        this.abilityTarget = null;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (isBoss) this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());

        if (hasPhases() && !isPhase2() && this.getHealth() < this.getMaxHealth() * 0.5F) {
            this.entityData.set(DATA_PHASE2, true);
            this.level().broadcastEntityEvent(this, EVENT_PHASE2);
            onPhase2();
        }

        for (int i = 0; i < cooldowns.length; i++) if (cooldowns[i] > 0) cooldowns[i]--;
        if (globalCooldown > 0) globalCooldown--;

        LivingEntity target = this.getTarget();
        if (target != null && (!target.isAlive() || (target instanceof Player p && (p.isCreative() || p.isSpectator())))) {
            this.setTarget(null);
            target = null;
        }

        if (currentAbility >= 0) {
            LivingEntity t = abilityTarget != null && abilityTarget.isAlive() ? abilityTarget : target;
            if (t != null && !isAirborneAbility()) faceTowards(t);
            tickAbility(currentAbility, abilityTick, t);
            abilityTick++;
            if (currentAbility >= 0 && abilityTick >= abilityDuration(currentAbility)) endAbility();
        } else if (target != null) {
            double d = this.distanceToSqr(target);
            int choice = chooseAbility(target, d);
            if (choice >= 0) startAbility(choice, target);
        }
    }

    protected boolean isAirborneAbility() { return false; }

    public void faceTowards(Entity t) {
        double dx = t.getX() - this.getX();
        double dz = t.getZ() - this.getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * (180F / Math.PI)) - 90.0F;
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
        this.getLookControl().setLookAt(t, 60.0F, 60.0F);
    }

    /** melee reach check against target bounding box */
    protected boolean inMeleeRange(LivingEntity t, double distSqr) {
        double r = meleeReach() + t.getBbWidth() * 0.5;
        return distSqr <= r * r;
    }

    // ------------------------------------------------------------------ tick
    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            prevRunBlend = runBlend;
            float goal = isRunning() ? 1F : 0F;
            runBlend += (goal - runBlend) * 0.18F;
            if (phaseFlash > 0) phaseFlash--;
            if (this.isAlive()) ambientFx();
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id >= EVENT_ANIM_BASE && id < EVENT_ANIM_BASE + 20) {
            this.clientAnimId = id - EVENT_ANIM_BASE;
            this.clientAnimStart = this.tickCount;
        } else if (id == EVENT_PHASE2) {
            this.phaseFlash = 40;
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    protected void tickDeath() {
        ++this.deathTime;
        deathFx(this.deathTime);
        if (this.deathTime >= deathTicks() && !this.level().isClientSide() && !this.isRemoved()) {
            this.level().broadcastEntityEvent(this, (byte) 60);
            this.remove(Entity.RemovalReason.KILLED);
        }
    }

    @Override
    public void die(DamageSource src) {
        super.die(src);
        this.currentAbility = -1;
        this.bossEvent.setProgress(0F);
    }

    // ------------------------------------------------------------------ helpers
    public void sound(SoundEvent s, float vol, float pitch) {
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), s, SoundSource.HOSTILE, vol, pitch);
    }

    public void soundAt(Vec3 p, SoundEvent s, float vol, float pitch) {
        this.level().playSound(null, p.x, p.y, p.z, s, SoundSource.HOSTILE, vol, pitch);
    }

    public ServerLevel serverLevel() { return (ServerLevel) this.level(); }

    public Vec3 forward() {
        float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
    }

    /** the boss's anatomical right side */
    public Vec3 right() {
        float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));
    }

    public static ParticleOptions dust(int rgb, float scale) {
        return new DustParticleOptions(new Vector3f(((rgb >> 16) & 255) / 255F, ((rgb >> 8) & 255) / 255F, (rgb & 255) / 255F), scale);
    }

    public void hit(LivingEntity e, DamageSource src, float amount, double knock, double lift) {
        if (e.hurt(src, amount)) {
            Vec3 d = e.position().subtract(this.position());
            d = new Vec3(d.x, 0, d.z);
            if (d.lengthSqr() < 1.0E-4) d = forward();
            d = d.normalize();
            double kr = 1.0 - e.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE) * 0.6;
            e.setDeltaMovement(e.getDeltaMovement().add(d.x * knock * kr, lift * kr, d.z * knock * kr));
            e.hurtMarked = true;
        }
    }

    // ------------------------------------------------------------------ goals
    /** holds movement/look while an ability is playing so wander/look goals can't interrupt it */
    static class CastGoal extends Goal {
        private final BossEntity boss;

        CastGoal(BossEntity boss) {
            this.boss = boss;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() { return boss.isCasting(); }

        @Override
        public void start() { boss.getNavigation().stop(); }
    }

    static class ChaseGoal extends Goal {
        private final BossEntity boss;
        private int repath;

        ChaseGoal(BossEntity boss) {
            this.boss = boss;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = boss.getTarget();
            return t != null && t.isAlive() && !boss.isCasting();
        }

        @Override
        public boolean canContinueToUse() { return canUse(); }

        @Override
        public void start() { repath = 0; }

        @Override
        public void stop() {
            boss.getNavigation().stop();
            boss.setRunning(false);
        }

        @Override
        public boolean requiresUpdateEveryTick() { return true; }

        @Override
        public void tick() {
            LivingEntity t = boss.getTarget();
            if (t == null) return;
            boss.getLookControl().setLookAt(t, 30.0F, 30.0F);
            double d = boss.distanceToSqr(t);
            boolean far = d > 10 * 10;
            boss.setRunning(far && boss.getNavigation().isInProgress());
            if (--repath <= 0) {
                repath = 4 + boss.getRandom().nextInt(5);
                double reach = boss.meleeReach() * 0.8;
                if (d > reach * reach) boss.getNavigation().moveTo(t, far ? boss.runSpeed() : boss.walkSpeed());
                else boss.getNavigation().stop();
            }
        }
    }
}
