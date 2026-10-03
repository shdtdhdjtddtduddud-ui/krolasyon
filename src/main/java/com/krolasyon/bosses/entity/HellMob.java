package com.krolasyon.bosses.entity;

import com.krolasyon.bosses.entity.mob.Ab;
import com.krolasyon.bosses.entity.mob.MobAbilities;
import com.krolasyon.bosses.entity.mob.MobSpec;
import com.krolasyon.bosses.entity.mob.MobSpecs;
import com.krolasyon.bosses.faction.Faction;
import com.krolasyon.bosses.faction.PlayerData;
import com.krolasyon.bosses.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Generic creature of Azrakor: everything (stats, abilities, faction, flying) comes from its {@link MobSpec}.
 * It belongs to a house; it attacks players only while that house distrusts them and fights the houses
 * that are its enemies.
 */
public class HellMob extends BossEntity {
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER = SynchedEntityData.defineId(HellMob.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> DATA_SHIELD = SynchedEntityData.defineId(HellMob.class, EntityDataSerializers.BOOLEAN);

    private MobSpec specCache;

    // per-ability scratch state (server side)
    public final Set<Integer> abHits = new HashSet<>();
    public int abState;
    public Vec3 abVec = Vec3.ZERO;
    public final List<Entity> minions = new ArrayList<>();
    private float shieldReduce = 0.8F, shieldReflect = 0F;

    public HellMob(EntityType<? extends HellMob> type, Level level) {
        super(type, level, BossEvent.BossBarColor.WHITE, MobSpecs.of(type).abilities.size(), false);
        MobSpec s = spec();
        this.xpReward = s.xp;
        this.setMaxUpStep(1.0F);
        if (s.flying) {
            this.moveControl = new FlyingMoveControl(this, 20, true);
            this.setNoGravity(true);
        }
    }

    public MobSpec spec() {
        if (specCache == null) specCache = MobSpecs.of(getType());
        return specCache;
    }

    public Faction faction() { return spec().faction; }

    public static AttributeSupplier.Builder createAttributes(MobSpec s) {
        AttributeSupplier.Builder b = Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, s.hp)
                .add(Attributes.ARMOR, s.armor)
                .add(Attributes.ATTACK_DAMAGE, s.attack)
                .add(Attributes.MOVEMENT_SPEED, s.speed)
                .add(Attributes.FOLLOW_RANGE, s.follow)
                .add(Attributes.KNOCKBACK_RESISTANCE, s.kbResist);
        if (s.flying) b.add(Attributes.FLYING_SPEED, s.speed * 1.6);
        return b;
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        if (MobSpecs.of(getType()).flying) {
            FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
            nav.setCanOpenDoors(false);
            nav.setCanFloat(true);
            nav.setCanPassDoors(true);
            return nav;
        }
        return super.createNavigation(level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_OWNER, Optional.empty());
        this.entityData.define(DATA_SHIELD, false);
    }

    // ------------------------------------------------------------------ goals / politics
    @Override
    protected void registerGoals() {
        boolean flying = MobSpecs.of(getType()).flying;
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new CastGoal(this));
        if (flying) this.goalSelector.addGoal(2, new FlyChaseGoal(this));
        else this.goalSelector.addGoal(2, new ChaseGoal(this));
        this.goalSelector.addGoal(6, new FollowOwnerLiteGoal(this));
        if (flying) this.goalSelector.addGoal(7, new FlyWanderGoal(this));
        else this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.6D));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(0, new OwnerDefendGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, e -> e instanceof Player p && wantsToAttackPlayer(p)));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 6, true, false, this::isMonsterPrey));
    }

    @Nullable
    public UUID getOwnerUUID() { return this.entityData.get(DATA_OWNER).orElse(null); }

    public void setOwnerUUID(@Nullable UUID id) { this.entityData.set(DATA_OWNER, Optional.ofNullable(id)); }

    @Nullable
    public Player getOwnerPlayer() {
        UUID id = getOwnerUUID();
        return id == null ? null : this.level().getPlayerByUUID(id);
    }

    public void setSummoner(Entity e) {}

    public int countMinions() {
        minions.removeIf(e -> e == null || !e.isAlive());
        return minions.size();
    }

    @Override
    protected boolean wantsToAttackPlayer(Player p) {
        UUID o = getOwnerUUID();
        if (o != null) return false;
        if (!faction().isHouse()) return true;
        return PlayerData.isHostileTo(p, faction());
    }

    @Override
    protected boolean isMonsterPrey(LivingEntity e) {
        if (!(e instanceof HellMob m) || m == this || !m.isAlive()) return false;
        Player me = getOwnerPlayer();
        Player them = m.getOwnerPlayer();
        if (getOwnerUUID() != null) {
            if (getOwnerUUID().equals(m.getOwnerUUID())) return false;
            return me != null && m.wantsToAttackPlayer(me) || m.getOwnerUUID() == null && m.getTarget() == me && me != null;
        }
        if (m.getOwnerUUID() != null) return them != null && wantsToAttackPlayer(them);
        return faction().isEnemyOf(m.faction());
    }

    @Override
    public boolean isHostileTo(Entity e) {
        if (!(e instanceof LivingEntity le) || e == this || !e.isAlive()) return false;
        if (e instanceof Player p) {
            if (p.isCreative() || p.isSpectator()) return false;
            if (p.getUUID().equals(getOwnerUUID())) return false;
            return p == getTarget() || wantsToAttackPlayer(p);
        }
        if (e instanceof HellMob m) return isMonsterPrey(m) || m.getTarget() == this || getTarget() == m;
        return e == getTarget() || (e instanceof Mob mob && mob.getTarget() == this);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (target instanceof Player p && p.getUUID().equals(getOwnerUUID())) return false;
        return super.canAttack(target);
    }

    // ------------------------------------------------------------------ abilities
    @Override protected int abilityDuration(int id) { return spec().abilities.get(id).dur; }
    @Override protected int abilityCooldown(int id) { return spec().abilities.get(id).cd; }
    @Override public double meleeReach() { return spec().reach; }
    @Override public double walkSpeed() { return 1.0; }
    @Override public double runSpeed() { return 1.25; }
    @Override protected int deathTicks() { return spec().deathTicks; }
    @Override protected void onPhase2() {}

    @Override
    protected boolean isAirborneAbility() {
        return currentAbility >= 0 && spec().abilities.get(currentAbility).airborne;
    }

    @Override
    protected void tickAbility(int id, int t, @Nullable LivingEntity target) {
        MobAbilities.tick(this, spec().abilities.get(id), t, target);
    }

    @Override
    protected void endAbility() {
        super.endAbility();
        if (isShielded()) setShielded(false, 0, 0);
    }

    @Override
    protected int chooseAbility(LivingEntity target, double distSqr) {
        List<Ab> abs = spec().abilities;
        boolean primary = ready(0) && inMeleeRange(target, distSqr) && inRange(abs.get(0), distSqr, target);
        if (globalCooldown > 0 || this.tickCount % 4 != 0) return primary ? 0 : -1;
        int total = 0;
        int[] w = new int[abs.size()];
        for (int i = 1; i < abs.size(); i++) {
            Ab ab = abs.get(i);
            if (!ready(i) || !inRange(ab, distSqr, target)) continue;
            w[i] = ab.weight;
            total += w[i];
        }
        if (total == 0 || random.nextFloat() > 0.7F) return primary ? 0 : -1;
        int roll = random.nextInt(total);
        for (int i = 1; i < abs.size(); i++) {
            roll -= w[i];
            if (roll < 0 && w[i] > 0) return i;
        }
        return primary ? 0 : -1;
    }

    private boolean inRange(Ab ab, double distSqr, LivingEntity target) {
        double d = Math.sqrt(distSqr);
        if (d < ab.minR || d > ab.maxR) return false;
        return !ab.needLos || this.hasLineOfSight(target);
    }

    // ------------------------------------------------------------------ shield
    public boolean isShielded() { return this.entityData.get(DATA_SHIELD); }

    public void setShielded(boolean on, float reduce, float reflect) {
        this.entityData.set(DATA_SHIELD, on);
        this.shieldReduce = reduce;
        this.shieldReflect = reflect;
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (isShielded() && !src.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            amount *= (1F - shieldReduce);
            if (!this.level().isClientSide()) {
                ((ServerLevel) this.level()).sendParticles(ParticleTypes.CRIT, getX(), getY() + getBbHeight() * 0.6, getZ(), 6, 0.4, 0.4, 0.4, 0.2);
                if (shieldReflect > 0 && src.getEntity() instanceof LivingEntity attacker && attacker != this) attacker.hurt(this.damageSources().thorns(this), amount * shieldReflect * 4F);
            }
        }
        return super.hurt(src, amount);
    }

    // ------------------------------------------------------------------ persistence
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        UUID o = getOwnerUUID();
        if (o != null) tag.putUUID("Owner", o);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) setOwnerUUID(tag.getUUID("Owner"));
        if (getOwnerUUID() != null) this.setPersistenceRequired();
    }

    // ------------------------------------------------------------------ fx
    @Override
    protected void ambientFx() {
        if (random.nextInt(8) != 0) return;
        Level l = this.level();
        double x = getX() + (random.nextDouble() - 0.5) * getBbWidth();
        double y = getY() + random.nextDouble() * getBbHeight();
        double z = getZ() + (random.nextDouble() - 0.5) * getBbWidth();
        switch (faction()) {
            case EMBER -> l.addParticle(random.nextBoolean() ? ParticleTypes.FLAME : ParticleTypes.SMALL_FLAME, x, y, z, 0, 0.02, 0);
            case BONE -> l.addParticle(ParticleTypes.ASH, x, y, z, 0, -0.01, 0);
            case BLOOD -> l.addParticle(ParticleTypes.DRIPPING_LAVA, x, y, z, 0, 0, 0);
            case SHADOW -> l.addParticle(ParticleTypes.SQUID_INK, x, y, z, 0, 0.01, 0);
            case ROT -> l.addParticle(ParticleTypes.SPORE_BLOSSOM_AIR, x, y, z, 0, -0.01, 0);
            default -> l.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0.02, 0);
        }
        if (isShielded()) l.addParticle(ParticleTypes.CRIT, x, y, z, 0, 0, 0);
    }

    @Override
    protected void deathFx(int t) {
        if (this.level().isClientSide()) {
            int n = Math.max(1, t / 6);
            for (int i = 0; i < n; i++) {
                this.level().addParticle(dust(faction().color, 1.4F), getX() + (random.nextDouble() - 0.5) * getBbWidth(), getY() + random.nextDouble() * getBbHeight(),
                        getZ() + (random.nextDouble() - 0.5) * getBbWidth(), 0, 0.04, 0);
            }
            if (t % 3 == 0) this.level().addParticle(ParticleTypes.LARGE_SMOKE, getX(), getY() + getBbHeight() * 0.5, getZ(), 0, 0.03, 0);
        }
    }

    // ------------------------------------------------------------------ sounds
    @Override protected SoundEvent getAmbientSound() { return ModSounds.voice(spec().voice).ambient(); }
    @Override protected SoundEvent getHurtSound(DamageSource src) { return ModSounds.voice(spec().voice).hurt(); }
    @Override protected SoundEvent getDeathSound() { return ModSounds.voice(spec().voice).death(); }
    @Override public float getVoicePitch() { return spec().pitch + (random.nextFloat() - 0.5F) * 0.12F; }
    @Override protected float getSoundVolume() { return spec().volume; }
    @Override public int getAmbientSoundInterval() { return 180; }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        if (spec().flying) return;
        this.playSound(ModSounds.voice(spec().voice).step(), 0.5F * spec().volume, spec().pitch * (0.9F + random.nextFloat() * 0.2F));
    }

    // ------------------------------------------------------------------ spawning
    public static boolean checkSpawn(EntityType<? extends HellMob> type, net.minecraft.world.level.ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, net.minecraft.util.RandomSource rand) {
        if (level.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) return false;
        if (reason == MobSpawnType.SPAWNER || reason == MobSpawnType.SPAWN_EGG || reason == MobSpawnType.COMMAND || reason == MobSpawnType.MOB_SUMMONED || reason == MobSpawnType.STRUCTURE) return true;
        MobSpec s = MobSpecs.of(type);
        BlockPos below = pos.below();
        if (s.flying) return level.getBlockState(pos).isAir();
        return level.getBlockState(below).isValidSpawn(level, below, type) && level.getBlockState(pos).getFluidState().isEmpty();
    }

    // ------------------------------------------------------------------ extra goals
    /** flying chase: move to a point hovering above the target's head */
    static class FlyChaseGoal extends Goal {
        private final HellMob mob;
        private int repath;

        FlyChaseGoal(HellMob mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = mob.getTarget();
            return t != null && t.isAlive() && !mob.isCasting();
        }

        @Override
        public boolean canContinueToUse() { return canUse(); }

        @Override
        public boolean requiresUpdateEveryTick() { return true; }

        @Override
        public void stop() { mob.getNavigation().stop(); }

        @Override
        public void tick() {
            LivingEntity t = mob.getTarget();
            if (t == null) return;
            mob.getLookControl().setLookAt(t, 40.0F, 40.0F);
            if (--repath > 0) return;
            repath = 6 + mob.getRandom().nextInt(5);
            double hover = mob.spec().hoverHeight;
            double dx = t.getX() - mob.getX(), dz = t.getZ() - mob.getZ();
            double horiz = Math.sqrt(dx * dx + dz * dz);
            double reach = mob.meleeReach() * 0.75;
            double y = t.getY() + (horiz < reach * 1.2 ? Math.min(hover, 1.0) : hover);
            if (horiz > reach || Math.abs(y - mob.getY()) > 1.2) mob.getNavigation().moveTo(t.getX(), y, t.getZ(), 1.0);
            else mob.getNavigation().stop();
        }
    }

    static class FlyWanderGoal extends Goal {
        private final HellMob mob;

        FlyWanderGoal(HellMob mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() { return mob.getTarget() == null && mob.getNavigation().isDone() && mob.getRandom().nextInt(40) == 0; }

        @Override
        public boolean canContinueToUse() { return mob.getNavigation().isInProgress(); }

        @Override
        public void start() {
            double a = mob.getRandom().nextDouble() * Math.PI * 2;
            double d = 6 + mob.getRandom().nextDouble() * 8;
            double gy = mob.level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int) mob.getX(), (int) mob.getZ());
            double y = Math.max(mob.getY() - 3, Math.min(mob.getY() + 4, Math.max(gy + 2, mob.getY() + (mob.getRandom().nextDouble() - 0.4) * 4)));
            mob.getNavigation().moveTo(mob.getX() + Math.cos(a) * d, y, mob.getZ() + Math.sin(a) * d, 0.7);
        }
    }

    /** summoned soldiers keep close to the player that called them */
    static class FollowOwnerLiteGoal extends Goal {
        private final HellMob mob;
        private int repath;

        FollowOwnerLiteGoal(HellMob mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            Player o = mob.getOwnerPlayer();
            return o != null && mob.getTarget() == null && mob.distanceToSqr(o) > 36;
        }

        @Override
        public boolean canContinueToUse() {
            Player o = mob.getOwnerPlayer();
            return o != null && mob.getTarget() == null && mob.distanceToSqr(o) > 9;
        }

        @Override
        public void tick() {
            Player o = mob.getOwnerPlayer();
            if (o == null) return;
            if (--repath <= 0) {
                repath = 10;
                if (mob.distanceToSqr(o) > 24 * 24) mob.randomTeleport(o.getX(), o.getY(), o.getZ(), true);
                else mob.getNavigation().moveTo(o, 1.15);
            }
        }
    }

    /** soldiers owned by a player attack whatever hurts that player or what the player hurts */
    static class OwnerDefendGoal extends net.minecraft.world.entity.ai.goal.target.TargetGoal {
        private final HellMob mob;
        @Nullable private LivingEntity wanted;

        OwnerDefendGoal(HellMob mob) {
            super(mob, false);
            this.mob = mob;
            this.setFlags(EnumSet.of(Goal.Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            Player o = mob.getOwnerPlayer();
            if (o == null) return false;
            LivingEntity c = o.getLastHurtByMob();
            if (c == null || !c.isAlive() || c == mob) c = o.getLastHurtMob();
            if (c == null || !c.isAlive() || c instanceof Player || (c instanceof HellMob h && mob.getOwnerUUID().equals(h.getOwnerUUID()))) return false;
            wanted = c;
            return true;
        }

        @Override
        public void start() {
            mob.setTarget(wanted);
            super.start();
        }
    }
}
