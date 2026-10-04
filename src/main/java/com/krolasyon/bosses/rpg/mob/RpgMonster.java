package com.krolasyon.bosses.rpg.mob;

import com.krolasyon.bosses.rpg.def.MonsterDef;
import com.krolasyon.bosses.rpg.def.RpgDefs;
import com.krolasyon.bosses.rpg.def.RpgDefs.Ability;
import com.krolasyon.bosses.rpg.def.RpgDefs.Archetype;
import com.krolasyon.bosses.rpg.registry.RpgEffects;
import com.krolasyon.bosses.rpg.registry.RpgEntities;
import com.krolasyon.bosses.rpg.util.Combat;
import com.krolasyon.bosses.rpg.util.FX;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
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
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * One class for every data-driven monster. The {@link MonsterDef} attached to the entity type decides stats,
 * look, abilities and passive traits.
 */
public class RpgMonster extends Monster implements RpgAnimatable {
    private static final EntityDataAccessor<Boolean> SMALL = SynchedEntityData.defineId(RpgMonster.class, EntityDataSerializers.BOOLEAN);
    public static final byte EVENT_CAST = 80;

    @Nullable private MonsterDef def;
    @Nullable private int[] cooldowns;
    protected int globalCooldown = 40;
    public int casting = -1;
    public int castTick;
    @Nullable protected LivingEntity castTarget;
    public boolean landed;
    public final IntOpenHashSet hitThisCast = new IntOpenHashSet();
    public final List<ItemStack> stolen = new ArrayList<>();

    // client animation state
    public int clientCast = -1;
    public float clientCastStart;

    public RpgMonster(EntityType<? extends RpgMonster> type, Level level) {
        super(type, level);
        MonsterDef d = def();
        if (d.flying()) {
            this.moveControl = new FlyingMoveControl(this, 20, d.has(RpgDefs.T_FLOATING));
            this.setPathfindingMalus(BlockPathTypes.DANGER_FIRE, -1.0F);
            this.setPathfindingMalus(BlockPathTypes.WATER, 8.0F);
        }
        if (d.has(RpgDefs.T_AQUATIC)) this.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
        if (d.has(RpgDefs.T_FIRE_IMMUNE)) { this.setPathfindingMalus(BlockPathTypes.LAVA, 8.0F); this.setPathfindingMalus(BlockPathTypes.DANGER_FIRE, 0.0F); }
        this.xpReward = 4 + (int) (d.hp() / 5);
        this.setMaxUpStep(d.scale() >= 1.4F ? 1.5F : 1.0F);
    }

    public MonsterDef def() {
        if (def == null) def = RpgEntities.defOf(this.getType());
        return def;
    }

    private int[] cooldowns() {
        if (cooldowns == null) {
            cooldowns = new int[def().abilities().length];
            for (int i = 0; i < cooldowns.length; i++) cooldowns[i] = 30 + this.random.nextInt(80);
        }
        return cooldowns;
    }

    public static AttributeSupplier.Builder attributes(MonsterDef d) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, d.hp())
                .add(Attributes.ATTACK_DAMAGE, d.atk())
                .add(Attributes.MOVEMENT_SPEED, d.speed())
                .add(Attributes.FLYING_SPEED, d.speed() * 1.6)
                .add(Attributes.FOLLOW_RANGE, d.boss() ? 48 : 32)
                .add(Attributes.ARMOR, d.armor())
                .add(Attributes.ARMOR_TOUGHNESS, d.boss() ? 6 : 0)
                .add(Attributes.ATTACK_KNOCKBACK, d.has(RpgDefs.T_HEAVY) ? 1.0 : 0.2)
                .add(Attributes.KNOCKBACK_RESISTANCE, d.boss() ? 0.9 : d.has(RpgDefs.T_HEAVY) ? 0.7 : Math.min(0.5, (d.scale() - 1) * 0.5));
    }

    // ------------------------------------------------------------------ setup
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SMALL, false);
    }

    public boolean isSmall() { return this.entityData.get(SMALL); }

    public void setSmall(boolean b) {
        this.entityData.set(SMALL, b);
        this.refreshDimensions();
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (SMALL.equals(key)) this.refreshDimensions();
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return super.getDimensions(pose).scale(isSmall() ? 0.55F : 1.0F);
    }

    public float renderScale() { return def().scale() * (isSmall() ? 0.55F : 1.0F); }

    @Override
    protected PathNavigation createNavigation(Level level) {
        MonsterDef d = RpgEntities.defOf(this.getType());
        if (d != null && d.flying()) {
            FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
            nav.setCanOpenDoors(false);
            nav.setCanFloat(true);
            nav.setCanPassDoors(true);
            return nav;
        }
        return super.createNavigation(level);
    }

    @Override
    protected void registerGoals() {
        MonsterDef d = RpgEntities.defOf(this.getType());
        boolean fly = d != null && d.flying();
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new CastGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, fly ? 1.0 : 1.15, true));
        if (fly) this.goalSelector.addGoal(6, new WaterAvoidingRandomFlyingGoal(this, 0.8));
        else this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                e -> e instanceof AbstractVillager || (e instanceof Combat.Allegiance && !(e instanceof net.minecraft.world.entity.monster.Enemy))));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, MobSpawnType reason, @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        SpawnGroupData r = super.finalizeSpawn(level, diff, reason, data, tag);
        this.setHealth(this.getMaxHealth());
        return r;
    }

    // ------------------------------------------------------------------ properties
    public boolean isBoss() { return def().boss(); }
    public float powerMultiplier() { return isSmall() ? 0.5F : 1.0F; }

    @Override
    public MobType getMobType() {
        if (def().has(RpgDefs.T_UNDEAD)) return MobType.UNDEAD;
        Archetype a = def().arch();
        if (a == Archetype.ARACHNID || a == Archetype.INSECT) return MobType.ARTHROPOD;
        if (def().has(RpgDefs.T_AQUATIC)) return MobType.WATER;
        return MobType.UNDEFINED;
    }

    @Override
    public boolean canBreatheUnderwater() { return def().has(RpgDefs.T_AQUATIC) || def().has(RpgDefs.T_UNDEAD) || def().arch() == Archetype.GOLEM; }

    @Override
    public boolean causeFallDamage(float dist, float mult, DamageSource src) {
        if (def().flying() || def().has(RpgDefs.T_HEAVY) || isBoss()) return false;
        return super.causeFallDamage(dist, mult, src);
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
        if (!def().flying()) super.checkFallDamage(y, onGround, state, pos);
    }

    @Override
    public Component getTypeName() { return Component.literal(def().name()); }

    @Override
    protected SoundEvent getAmbientSound() {
        boolean undead = def().has(RpgDefs.T_UNDEAD);
        return switch (def().arch()) {
            case HUMANOID -> undead ? SoundEvents.SKELETON_AMBIENT : SoundEvents.PILLAGER_AMBIENT;
            case BRUTE -> SoundEvents.RAVAGER_AMBIENT;
            case QUADRUPED -> def().scale() > 1.3F ? SoundEvents.POLAR_BEAR_AMBIENT : SoundEvents.WOLF_GROWL;
            case ARACHNID -> SoundEvents.SPIDER_AMBIENT;
            case INSECT -> SoundEvents.SILVERFISH_AMBIENT;
            case FLYER -> SoundEvents.PHANTOM_AMBIENT;
            case SLIME -> SoundEvents.SLIME_SQUISH;
            case SERPENT -> SoundEvents.GUARDIAN_AMBIENT_LAND;
            case GOLEM -> SoundEvents.IRON_GOLEM_STEP;
            case FLOATER -> undead ? SoundEvents.VEX_AMBIENT : SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM;
            case TREANT -> SoundEvents.AZALEA_LEAVES_STEP;
            case CRUSTACEAN -> SoundEvents.TURTLE_AMBIENT_LAND;
        };
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource src) {
        boolean undead = def().has(RpgDefs.T_UNDEAD);
        return switch (def().arch()) {
            case HUMANOID -> undead ? SoundEvents.SKELETON_HURT : SoundEvents.PILLAGER_HURT;
            case BRUTE -> SoundEvents.RAVAGER_HURT;
            case QUADRUPED -> def().scale() > 1.3F ? SoundEvents.POLAR_BEAR_HURT : SoundEvents.WOLF_HURT;
            case ARACHNID -> SoundEvents.SPIDER_HURT;
            case INSECT -> SoundEvents.SILVERFISH_HURT;
            case FLYER -> SoundEvents.PHANTOM_HURT;
            case SLIME -> SoundEvents.SLIME_HURT;
            case SERPENT -> SoundEvents.GUARDIAN_HURT_LAND;
            case GOLEM -> SoundEvents.IRON_GOLEM_HURT;
            case FLOATER -> SoundEvents.VEX_HURT;
            case TREANT -> SoundEvents.WOOD_BREAK;
            case CRUSTACEAN -> SoundEvents.TURTLE_HURT;
        };
    }

    @Override
    protected SoundEvent getDeathSound() {
        boolean undead = def().has(RpgDefs.T_UNDEAD);
        return switch (def().arch()) {
            case HUMANOID -> undead ? SoundEvents.SKELETON_DEATH : SoundEvents.PILLAGER_DEATH;
            case BRUTE -> SoundEvents.RAVAGER_DEATH;
            case QUADRUPED -> def().scale() > 1.3F ? SoundEvents.POLAR_BEAR_DEATH : SoundEvents.WOLF_DEATH;
            case ARACHNID -> SoundEvents.SPIDER_DEATH;
            case INSECT -> SoundEvents.SILVERFISH_DEATH;
            case FLYER -> SoundEvents.PHANTOM_DEATH;
            case SLIME -> SoundEvents.SLIME_DEATH;
            case SERPENT -> SoundEvents.GUARDIAN_DEATH_LAND;
            case GOLEM -> SoundEvents.IRON_GOLEM_DEATH;
            case FLOATER -> SoundEvents.VEX_DEATH;
            case TREANT -> SoundEvents.WOOD_BREAK;
            case CRUSTACEAN -> SoundEvents.TURTLE_DEATH;
        };
    }

    @Override
    public float getVoicePitch() {
        return Mth.clamp(1.6F - renderScale() * 0.35F, 0.45F, 1.8F) + (this.random.nextFloat() - 0.5F) * 0.15F;
    }

    @Override
    protected float getSoundVolume() { return Math.min(2.5F, 0.7F + renderScale() * 0.3F); }

    @Override
    public int getAmbientSoundInterval() { return 160; }

    // ------------------------------------------------------------------ geometry helpers
    public Vec3 forward() {
        float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
    }

    public Vec3 right() {
        float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));
    }

    public void faceTowards(Entity t) {
        double dx = t.getX() - this.getX();
        double dz = t.getZ() - this.getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * (180F / Math.PI)) - 90.0F;
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
        this.getLookControl().setLookAt(t, 60.0F, 60.0F);
    }

    // ------------------------------------------------------------------ abilities
    public boolean isCasting() { return casting >= 0; }

    @Nullable public Ability castingAbility() {
        int c = this.level().isClientSide() ? clientCast : casting;
        return c >= 0 && c < def().abilities().length ? def().abilities()[c] : null;
    }

    protected float cooldownScale() { return 1.0F; }

    protected void startCast(int idx, LivingEntity target) {
        Ability a = def().abilities()[idx];
        AbilityLogic.Info in = AbilityLogic.info(a);
        this.casting = idx;
        this.castTick = 0;
        this.castTarget = target;
        this.landed = false;
        this.hitThisCast.clear();
        cooldowns()[idx] = (int) (in.cooldown() * cooldownScale() * (0.8F + this.random.nextFloat() * 0.4F));
        this.globalCooldown = (int) ((isBoss() ? 20 : 35) * cooldownScale()) + this.random.nextInt(20);
        if (!AbilityLogic.selfMoving(a)) this.getNavigation().stop();
        this.level().broadcastEntityEvent(this, (byte) (EVENT_CAST + idx));
    }

    protected void endCast() {
        if (casting >= 0 && def().abilities()[casting] == Ability.BURROW || casting >= 0 && def().abilities()[casting] == Ability.SHADOW_STEP)
            this.setInvisible(false);
        this.casting = -1;
        this.castTarget = null;
    }

    /** forces an ability, used by the test command */
    public void forceCast(int idx, LivingEntity target) {
        if (idx < 0 || idx >= def().abilities().length) return;
        if (casting >= 0) endCast();
        this.setTarget(target);
        startCast(idx, target);
    }

    private int pickAbility(LivingEntity target) {
        Ability[] ab = def().abilities();
        int[] cd = cooldowns();
        double dist = Math.sqrt(this.distanceToSqr(target)) - target.getBbWidth() * 0.5 - this.getBbWidth() * 0.4;
        int start = this.random.nextInt(ab.length);
        for (int k = 0; k < ab.length; k++) {
            int i = (start + k) % ab.length;
            if (cd[i] > 0) continue;
            AbilityLogic.Info in = AbilityLogic.info(ab[i]);
            if (dist < in.minRange() || dist > in.maxRange()) continue;
            Ability a = ab[i];
            if (a == Ability.HEAL && this.getHealth() > this.getMaxHealth() * 0.6F) continue;
            if (a == Ability.SUMMON && def().summon() == null) continue;
            if ((a == Ability.STONE_SKIN || a == Ability.ENRAGE) && this.getHealth() > this.getMaxHealth() * 0.85F && !isBoss()) continue;
            if (a == Ability.DIVE_BOMB && !def().flying()) continue;
            return i;
        }
        return -1;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        int[] cd = cooldowns();
        for (int i = 0; i < cd.length; i++) if (cd[i] > 0) cd[i]--;
        if (globalCooldown > 0) globalCooldown--;

        LivingEntity target = this.getTarget();
        if (target != null && (!target.isAlive() || target instanceof Player p && (p.isCreative() || p.isSpectator()))) {
            this.setTarget(null);
            target = null;
        }
        if (casting >= 0) {
            Ability a = def().abilities()[casting];
            AbilityLogic.Info in = AbilityLogic.info(a);
            LivingEntity t = castTarget != null && castTarget.isAlive() ? castTarget : target;
            if (t != null && !AbilityLogic.selfMoving(a) && a != Ability.SPIN_ATTACK) faceTowards(t);
            AbilityLogic.tick(this, a, castTick, t, in);
            castTick++;
            if (casting >= 0 && castTick >= in.duration()) endCast();
        } else if (target != null && globalCooldown <= 0 && this.getSensing().hasLineOfSight(target) && !this.hasEffect(RpgEffects.STUN.get())) {
            int i = pickAbility(target);
            if (i >= 0) startCast(i, target);
        }

        MonsterDef d = def();
        if (d.has(RpgDefs.T_REGEN) && this.tickCount % 40 == 0 && this.getHealth() < this.getMaxHealth()) this.heal(1.0F + d.hp() * 0.01F);
        if (d.has(RpgDefs.T_SUN_BURN) && this.isSunBurnTick()) this.setSecondsOnFire(8);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide() && this.isAlive()) ambientFx();
    }

    protected void ambientFx() {
        MonsterDef d = def();
        if (this.tickCount % 4 != 0 || this.isInvisible()) return;
        double w = this.getBbWidth() * 0.6, h = this.getBbHeight();
        double x = this.getX() + (this.random.nextDouble() - 0.5) * w * 2, y = this.getY() + this.random.nextDouble() * h, z = this.getZ() + (this.random.nextDouble() - 0.5) * w * 2;
        if (d.has(RpgDefs.T_FIRE_IMMUNE) && this.random.nextInt(3) == 0) this.level().addParticle(ParticleTypes.FLAME, x, y, z, 0, 0.02, 0);
        else if (d.has(RpgDefs.T_FROST_TOUCH) && this.random.nextInt(2) == 0) this.level().addParticle(ParticleTypes.SNOWFLAKE, x, y, z, 0, -0.02, 0);
        else if (d.has(RpgDefs.T_SHOCK_TOUCH) && this.random.nextInt(3) == 0) this.level().addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, 0, 0, 0);
        else if (d.has(RpgDefs.T_POISON_TOUCH) && this.random.nextInt(4) == 0) this.level().addParticle(FX.dust(0x80D020, 0.8F), x, y, z, 0, 0, 0);
        else if (d.has(RpgDefs.T_UNDEAD) && d.glow() && this.random.nextInt(4) == 0) this.level().addParticle(ParticleTypes.SOUL, x, y, z, 0, 0.02, 0);
        else if (d.glow() && this.random.nextInt(5) == 0) this.level().addParticle(FX.dust(d.eyeColor(), 0.7F), x, y, z, 0, 0, 0);
        if (isCasting() || clientCast >= 0) {
            Ability a = castingAbility();
            if (a != null && this.tickCount - clientCastStart < AbilityLogic.info(a).windup())
                this.level().addParticle(FX.dust(AbilityLogic.info(a).color(), 1.0F), x, y, z, 0, 0.05, 0);
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id >= EVENT_CAST && id < EVENT_CAST + 8) {
            this.clientCast = id - EVENT_CAST;
            this.clientCastStart = this.tickCount;
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() && clientCast >= 0) {
            Ability a = castingAbility();
            if (a == null || this.tickCount - clientCastStart > AbilityLogic.info(a).duration() + 2) clientCast = -1;
        }
    }

    // ------------------------------------------------------------------ animation
    @Override
    public Archetype archetype() { return def().arch(); }

    @Override
    public int bodyParts() { return def().parts(); }

    @Nullable
    @Override
    public AbilityLogic.Style castStyle() {
        Ability a = castingAbility();
        return a == null ? null : AbilityLogic.info(a).style();
    }

    @Override
    public float castAge(float partialTick) { return clientCast < 0 ? -1 : this.tickCount - clientCastStart + partialTick; }

    @Override
    public int castWindup() {
        Ability a = castingAbility();
        return a == null ? 10 : AbilityLogic.info(a).windup();
    }

    @Override
    public boolean airborne() { return def().flying() && !this.onGround(); }

    // ------------------------------------------------------------------ combat
    @Override
    public boolean doHurtTarget(Entity e) {
        boolean hit = super.doHurtTarget(e);
        if (hit && e instanceof LivingEntity le) {
            MonsterDef d = def();
            if (d.has(RpgDefs.T_POISON_TOUCH)) Combat.effect(le, MobEffects.POISON, 80, 0);
            if (d.has(RpgDefs.T_FIRE_TOUCH)) le.setSecondsOnFire(4);
            if (d.has(RpgDefs.T_FROST_TOUCH)) { Combat.effect(le, MobEffects.MOVEMENT_SLOWDOWN, 60, 1); le.setTicksFrozen(Math.min(le.getTicksFrozen() + 60, 240)); }
            if (d.has(RpgDefs.T_WITHER_TOUCH)) Combat.effect(le, MobEffects.WITHER, 60, 0);
            if (d.has(RpgDefs.T_LIFESTEAL)) this.heal(d.atk() * 0.4F);
            if (d.has(RpgDefs.T_SHOCK_TOUCH) && this.random.nextInt(3) == 0) {
                Combat.effect(le, RpgEffects.STUN.get(), 15, 0);
                FX.send(level(), ParticleTypes.ELECTRIC_SPARK, le.position().add(0, 1, 0), 15, 0.4, 0.1);
            }
        }
        return hit;
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (this.isInvulnerableTo(src)) return false;
        MonsterDef d = def();
        if (d.has(RpgDefs.T_EVASIVE) && src.getDirectEntity() != src.getEntity() && this.random.nextInt(3) == 0 && !this.level().isClientSide()) {
            FX.send(level(), ParticleTypes.POOF, this.position().add(0, this.getBbHeight() * 0.5, 0), 8, 0.3, 0.02);
            return false;
        }
        boolean r = super.hurt(src, amount);
        if (r && d.has(RpgDefs.T_THORNS) && src.getDirectEntity() instanceof LivingEntity att && src.getDirectEntity() == src.getEntity() && att != this) {
            att.hurt(this.damageSources().thorns(this), Math.max(1.0F, amount * 0.25F));
        }
        if (r && casting < 0 && src.getEntity() instanceof LivingEntity att && att != this && getTarget() == null) this.setTarget(att);
        return r;
    }

    @Override
    public void die(DamageSource src) {
        super.die(src);
        if (this.level().isClientSide()) return;
        MonsterDef d = def();
        if (d.has(RpgDefs.T_EXPLODE_DEATH)) {
            FX.send(level(), ParticleTypes.EXPLOSION_EMITTER, this.position(), 1, 0, 0);
            FX.sound(this, SoundEvents.GENERIC_EXPLODE, 1.2F, 1.0F);
            for (LivingEntity e : Combat.victims(this, this.position(), 3.5)) {
                Combat.magic(this, null, e, d.atk() * 1.5F);
                if (d.has(RpgDefs.T_FIRE_IMMUNE)) e.setSecondsOnFire(4);
                else Combat.effect(e, MobEffects.POISON, 80, 1);
            }
        }
        if (d.has(RpgDefs.T_SPLIT_DEATH) && !isSmall() && this.level() instanceof ServerLevel sl) {
            for (int i = 0; i < 2; i++) {
                Entity e = this.getType().create(sl);
                if (e instanceof RpgMonster child) {
                    child.setSmall(true);
                    child.moveTo(this.getX() + (i == 0 ? 0.4 : -0.4), this.getY() + 0.2, this.getZ(), this.random.nextFloat() * 360, 0);
                    child.getAttribute(Attributes.MAX_HEALTH).setBaseValue(Math.max(4, d.hp() * 0.35F));
                    child.setHealth(child.getMaxHealth());
                    child.setTarget(this.getTarget());
                    sl.addFreshEntity(child);
                }
            }
        }
        for (ItemStack st : stolen) this.spawnAtLocation(st);
        stolen.clear();
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource src, int looting, boolean byPlayer) {
        super.dropCustomDeathLoot(src, looting, byPlayer);
        if (!isSmall()) com.krolasyon.bosses.rpg.item.RpgLoot.monsterLoot(this, src, looting, byPlayer);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Small", isSmall());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setSmall(tag.getBoolean("Small"));
    }

    // ------------------------------------------------------------------ goals
    static class CastGoal extends Goal {
        private final RpgMonster mob;

        CastGoal(RpgMonster mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() { return mob.isCasting(); }

        @Override
        public void start() {
            Ability a = mob.castingAbility();
            if (a == null || !AbilityLogic.selfMoving(a)) mob.getNavigation().stop();
        }
    }
}
