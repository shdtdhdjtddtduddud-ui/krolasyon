package com.krolasyon.bosses.realm.entity;

import com.krolasyon.bosses.realm.Allegiance;
import com.krolasyon.bosses.realm.Faction;
import com.krolasyon.bosses.realm.RealmConfig;
import com.krolasyon.bosses.realm.data.RealmData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.UUID;

/**
 * Generic creature of the Crimson Realm. Everything specific (stats, sounds, attacks) comes from its {@link MobSpec};
 * attacks run through the shared {@link Abilities} engine and are mirrored to the client as keyframe animations.
 */
public class RealmMob extends Monster implements FactionMember, AnimState.Holder, Allegiance.Member {
    private static final EntityDataAccessor<Boolean> DATA_RUNNING = SynchedEntityData.defineId(RealmMob.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_ALLY = SynchedEntityData.defineId(RealmMob.class, EntityDataSerializers.BOOLEAN);
    public static final byte EVENT_ANIM = 70;

    public final MobSpec spec;
    private final AnimState anim = new AnimState();
    protected final int[] cooldowns;
    protected int globalCooldown = 30;
    protected int current = -1, abilityTick;
    @Nullable protected LivingEntity abilityTarget;
    public final Abilities.Scratch scratch = new Abilities.Scratch();

    @Nullable private UUID ownerId;
    private int allyLife = -1;

    public RealmMob(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.spec = RealmEntities.spec(type);
        this.cooldowns = new int[spec.abilities().length];
        this.xpReward = spec.xp();
        this.setMaxUpStep(spec.height() > 2.2F ? 1.5F : 1.0F);
    }

    // ------------------------------------------------------------------ setup
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_RUNNING, false);
        this.entityData.define(DATA_ALLY, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new CastGoal(this));
        this.goalSelector.addGoal(2, new ChaseGoal(this));
        this.goalSelector.addGoal(5, new FollowOwnerGoal(this));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        addTargetGoals();
    }

    protected void addTargetGoals() {
        this.targetSelector.addGoal(1, new OwnerDefenseGoal(this));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::wantsToAttackPlayer));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 12, true, false, this::wantsToAttackMob));
    }

    @Nullable
    @Override
    public Faction faction() { return spec.faction(); }

    @Override
    public AnimState anim() { return anim; }

    // ------------------------------------------------------------------ allegiance
    public boolean isAlly() { return this.entityData.get(DATA_ALLY); }

    @Nullable
    public LivingEntity getOwner() {
        if (ownerId == null || !(this.level() instanceof ServerLevel sl)) return null;
        Entity e = sl.getEntity(ownerId);
        return e instanceof LivingEntity le ? le : null;
    }

    /** turns this creature into a temporary ally of a player */
    public void makeAlly(Player owner, int ticks) {
        this.ownerId = owner.getUUID();
        this.allyLife = ticks;
        this.entityData.set(DATA_ALLY, true);
        this.setPersistenceRequired();
        this.setTarget(null);
    }

    protected boolean wantsToAttackPlayer(LivingEntity e) {
        if (!(e instanceof Player p) || isAlly() || p.isCreative() || p.isSpectator()) return false;
        Faction f = faction();
        return f == null || !RealmData.get(p).peaceful(f);
    }

    protected boolean wantsToAttackMob(LivingEntity e) {
        if (e instanceof Player || !e.isAlive()) return false;
        if (isAlly()) {
            LivingEntity owner = getOwner();
            return owner instanceof Player p && e instanceof Enemy && !(e instanceof RealmMob rm && rm.isAlly()) && Allegiance.playerMayHit(p, e)
                    && e.distanceToSqr(this) < 200;
        }
        if (!RealmConfig.FACTION_WARS.get()) return false;
        Faction f = faction();
        return f != null && e instanceof FactionMember fm && !(e instanceof RealmMob rm && rm.isAlly()) && f.atWarWith(fm.faction())
                && this.random.nextInt(3) == 0;
    }

    @Override
    public boolean isHostileTo(Entity other) {
        if (!(other instanceof LivingEntity le) || other == this || !other.isAlive()) return false;
        if (isAlly()) {
            LivingEntity owner = getOwner();
            if (other == owner || (other instanceof RealmMob rm && rm.isAlly())) return false;
            return other == getTarget() || (owner instanceof Player p && Allegiance.playerMayHit(p, le));
        }
        if (other instanceof RealmMob rm && rm.isAlly()) return true;
        if (other == getTarget() || other == getLastHurtByMob()) return true;
        if (other instanceof Player p) return wantsToAttackPlayer(p);
        Faction f = faction();
        if (other instanceof FactionMember fm) return f != null && fm.faction() != null && f.atWarWith(fm.faction());
        return other instanceof Mob m && m.getTarget() == this;
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        if (super.isAlliedTo(other)) return true;
        if (isAlly()) return other == getOwner() || (other instanceof RealmMob rm && rm.isAlly());
        Faction f = faction();
        return f != null && other instanceof FactionMember fm && fm.faction() == f && !(other instanceof RealmMob rm && rm.isAlly());
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return super.canAttack(target) && !isAlliedTo(target);
    }

    // ------------------------------------------------------------------ abilities
    public boolean isRunning() { return this.entityData.get(DATA_RUNNING); }

    public void setRunning(boolean b) { if (b != isRunning()) this.entityData.set(DATA_RUNNING, b); }

    public boolean isCasting() { return current >= 0; }

    public Ability ability(int i) { return spec.abilities()[i]; }

    public void startAbility(int id, LivingEntity target) {
        Ability a = ability(id);
        current = id;
        abilityTick = 0;
        abilityTarget = target;
        cooldowns[id] = a.cooldown() + random.nextInt(Math.max(1, a.cooldown() / 4 + 1));
        if (!a.melee()) globalCooldown = 30 + random.nextInt(20);
        scratch.reset();
        getNavigation().stop();
        setRunning(false);
        this.level().broadcastEntityEvent(this, (byte) (EVENT_ANIM + a.anim()));
    }

    protected int chooseAbility(LivingEntity target, double distSqr) {
        double d = Math.sqrt(distSqr);
        int total = 0;
        int[] w = new int[cooldowns.length];
        for (int i = 0; i < cooldowns.length; i++) {
            Ability a = ability(i);
            if (cooldowns[i] > 0 || d < a.minRange() || d > a.maxRange() + target.getBbWidth() * 0.5) continue;
            if (!a.melee() && globalCooldown > 0) continue;
            if (!a.melee() && a.type() != Ability.Type.HEAL && a.type() != Ability.Type.SHIELD && a.type() != Ability.Type.ROAR
                    && a.type() != Ability.Type.SUMMON && a.type() != Ability.Type.VANISH && !hasLineOfSight(target)) continue;
            if (a.type() == Ability.Type.HEAL && getHealth() > getMaxHealth() * 0.7F && random.nextInt(4) != 0) continue;
            w[i] = a.weight();
            total += w[i];
        }
        if (total == 0 || (tickCount % 5 != 0 && !(d < 3.5))) return -1;
        int roll = random.nextInt(total);
        for (int i = 0; i < w.length; i++) {
            roll -= w[i];
            if (roll < 0) return i;
        }
        return -1;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        for (int i = 0; i < cooldowns.length; i++) if (cooldowns[i] > 0) cooldowns[i]--;
        if (globalCooldown > 0) globalCooldown--;
        if (allyLife > 0 && --allyLife == 0) {
            ((ServerLevel) level()).sendParticles(ParticleTypes.POOF, getX(), getY() + getBbHeight() / 2, getZ(), 20, 0.4, 0.6, 0.4, 0.02);
            discard();
            return;
        }
        LivingEntity target = getTarget();
        if (target != null && (!target.isAlive() || (target instanceof Player p && (p.isCreative() || p.isSpectator())))) {
            setTarget(null);
            target = null;
        }
        if (current >= 0) {
            LivingEntity t = abilityTarget != null && abilityTarget.isAlive() ? abilityTarget : target;
            Ability a = ability(current);
            if (t != null && a.type() != Ability.Type.CHARGE && a.type() != Ability.Type.LEAP) Abilities.face(this, t);
            Abilities.tick(this, a, abilityTick, t, scratch);
            abilityTick++;
            if (current >= 0 && abilityTick >= a.duration()) {
                current = -1;
                abilityTarget = null;
            }
        } else if (target != null) {
            int c = chooseAbility(target, distanceToSqr(target));
            if (c >= 0) startAbility(c, target);
        }
    }

    public double attackReach() {
        for (Ability a : spec.abilities()) if (a.melee()) return a.maxRange();
        return 2.0;
    }

    // ------------------------------------------------------------------ misc
    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            anim.tick(isRunning());
            if (isAlive()) Abilities.ambientFx(this);
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id >= EVENT_ANIM && id < EVENT_ANIM + 8) anim.play(id - EVENT_ANIM, tickCount);
        else super.handleEntityEvent(id);
    }

    @Override
    protected void tickDeath() {
        ++this.deathTime;
        if (this.deathTime >= 30 && !this.level().isClientSide() && !this.isRemoved()) {
            this.level().broadcastEntityEvent(this, (byte) 60);
            this.remove(Entity.RemovalReason.KILLED);
        }
    }

    @Override
    public boolean doHurtTarget(Entity e) {
        return super.doHurtTarget(e);
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (isAlly() && src.getEntity() != null && src.getEntity() == getOwner()) return false;
        return super.hurt(src, amount);
    }

    @Override
    public boolean removeWhenFarAway(double d) { return !isAlly() && super.removeWhenFarAway(d); }

    /** realm creatures live in the light of lava and embers: no darkness preference */
    @Override
    public float getWalkTargetValue(BlockPos pos, net.minecraft.world.level.LevelReader level) { return 0F; }

    @Override
    protected boolean shouldDespawnInPeaceful() { return !isAlly(); }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() { return MobSpec.sound(spec.ambient()); }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource src) { return MobSpec.sound(spec.hurt()); }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() { return MobSpec.sound(spec.death()); }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        SoundEvent s = MobSpec.sound(spec.step());
        if (s != null) playSound(s, 0.5F, spec.pitch());
        else super.playStepSound(pos, state);
    }

    @Override
    public float getVoicePitch() { return spec.pitch() * (0.9F + random.nextFloat() * 0.2F); }

    @Override
    public boolean causeFallDamage(float dist, float mult, DamageSource src) {
        return spec.flying() ? false : super.causeFallDamage(dist, mult, src);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, MobSpawnType reason, @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        return super.finalizeSpawn(level, diff, reason, data, tag);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (ownerId != null) {
            tag.putUUID("AllyOwner", ownerId);
            tag.putInt("AllyLife", allyLife);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("AllyOwner")) {
            ownerId = tag.getUUID("AllyOwner");
            allyLife = tag.getInt("AllyLife");
            entityData.set(DATA_ALLY, true);
        }
    }

    // ------------------------------------------------------------------ goals
    static class CastGoal extends Goal {
        private final RealmMob mob;

        CastGoal(RealmMob mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() { return mob.isCasting(); }

        @Override
        public void start() { mob.getNavigation().stop(); }
    }

    /** chases the target; ranged creatures keep their preferred distance */
    static class ChaseGoal extends Goal {
        private final RealmMob mob;
        private int repath;

        ChaseGoal(RealmMob mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = mob.getTarget();
            return t != null && t.isAlive() && !mob.isCasting();
        }

        @Override
        public void stop() {
            mob.getNavigation().stop();
            mob.setRunning(false);
        }

        @Override
        public boolean requiresUpdateEveryTick() { return true; }

        @Override
        public void tick() {
            LivingEntity t = mob.getTarget();
            if (t == null) return;
            mob.getLookControl().setLookAt(t, 30F, 30F);
            double d = Math.sqrt(mob.distanceToSqr(t));
            double prefer = mob.spec.preferRange();
            boolean far = d > 9;
            mob.setRunning(far && mob.getNavigation().isInProgress());
            if (--repath > 0) return;
            repath = 4 + mob.getRandom().nextInt(5);
            double speed = far ? 1.25 : 1.0;
            if (prefer > 0 && d < prefer * 0.55 && mob.hasLineOfSight(t)) {
                var away = mob.position().subtract(t.position()).normalize().scale(5).add(mob.position());
                mob.getNavigation().moveTo(away.x, away.y, away.z, 1.15);
            } else if (prefer > 0 && d < prefer && mob.hasLineOfSight(t)) {
                mob.getNavigation().stop();
            } else if (d > mob.attackReach() * 0.75 + t.getBbWidth() * 0.5) {
                mob.getNavigation().moveTo(t, speed);
            } else {
                mob.getNavigation().stop();
            }
        }
    }

    static class FollowOwnerGoal extends Goal {
        private final RealmMob mob;

        FollowOwnerGoal(RealmMob mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity o = mob.getOwner();
            return mob.isAlly() && mob.getTarget() == null && o != null && o.distanceToSqr(mob) > 36;
        }

        @Override
        public void tick() {
            LivingEntity o = mob.getOwner();
            if (o == null) return;
            if (o.distanceToSqr(mob) > 900) mob.teleportTo(o.getX() + mob.random.nextInt(3) - 1, o.getY(), o.getZ() + mob.random.nextInt(3) - 1);
            else mob.getNavigation().moveTo(o, 1.2);
        }
    }

    /** allies defend their owner and attack what the owner attacks */
    static class OwnerDefenseGoal extends TargetGoal {
        private final RealmMob mob;
        @Nullable private LivingEntity pick;

        OwnerDefenseGoal(RealmMob mob) {
            super(mob, false);
            this.mob = mob;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (!mob.isAlly()) return false;
            LivingEntity o = mob.getOwner();
            if (o == null) return false;
            LivingEntity a = o.getLastHurtByMob();
            if (a == null || !a.isAlive() || a == mob || (a instanceof RealmMob rm && rm.isAlly())) a = o.getLastHurtMob();
            if (a == null || !a.isAlive() || a == mob || a instanceof Player || (a instanceof RealmMob rm2 && rm2.isAlly())) return false;
            pick = a;
            return canAttack(a, TargetingConditions.DEFAULT);
        }

        @Override
        public void start() {
            mob.setTarget(pick);
            super.start();
        }
    }

    static float yawTo(Entity a, Entity b) {
        return (float) (Mth.atan2(b.getZ() - a.getZ(), b.getX() - a.getX()) * (180F / Math.PI)) - 90F;
    }
}
