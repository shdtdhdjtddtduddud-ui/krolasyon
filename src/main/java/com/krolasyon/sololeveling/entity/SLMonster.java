package com.krolasyon.sololeveling.entity;

import com.krolasyon.sololeveling.registry.ModEntities;
import com.krolasyon.sololeveling.registry.ModItems;
import com.krolasyon.sololeveling.registry.ModSounds;
import com.krolasyon.sololeveling.world.DungeonManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class of every Solo Leveling monster. Holds the {@link MobKind}, a synced "animation state" read by the
 * procedural models, an ability scheduler used by bosses and casters, boss bars and loot.
 */
public class SLMonster extends Monster {
    private static final EntityDataAccessor<Integer> ANIM = SynchedEntityData.defineId(SLMonster.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(SLMonster.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> ELITE = SynchedEntityData.defineId(SLMonster.class, EntityDataSerializers.BOOLEAN);

    public static final int A_NONE = 0, A_CAST = 1, A_SLAM = 2, A_ROAR = 3, A_SPIN = 4, A_LEAP = 5, A_SHOOT = 6, A_SUMMON = 7, A_BITE = 8;

    public final MobKind kind;
    protected final List<Ability> abilities = new ArrayList<>();
    protected Ability current;
    protected int abilityTick;
    protected int globalCooldown = 40;
    private ServerBossEvent bossBar;
    /** Dungeon instance id this monster belongs to (empty for wild monsters). */
    public String instance = "";
    // client side animation timing
    public int animStartTick;
    public int lastAnim;
    /** Casters keep their distance and only use abilities. */
    public boolean ranged;
    /** Extra effect applied by melee hits. */
    public java.util.function.Consumer<LivingEntity> hitEffect;
    /** Called once when a boss drops below half health. */
    public Runnable onPhase2;
    /** Set once a single-hit ability has connected. */
    public boolean abilityHit;
    /** The boss of a dungeon instance: killing it clears the dungeon. */
    public boolean instanceBoss;

    public SLMonster(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.kind = ModEntities.kindOf(type);
        this.xpReward = Math.max(5, kind.xp / 20);
        if (kind.boss) {
            bossBar = new ServerBossEvent(getDisplayName(), colorFor(), BossEvent.BossBarOverlay.NOTCHED_10);
            setPersistenceRequired();
        }
        setMaxUpStep(kind.width > 1.5F ? 1.6F : 1.1F);
    }

    private BossEvent.BossBarColor colorFor() {
        return switch (kind.rank) {
            case E, D -> BossEvent.BossBarColor.BLUE;
            case C -> BossEvent.BossBarColor.GREEN;
            case B -> BossEvent.BossBarColor.YELLOW;
            case A -> BossEvent.BossBarColor.RED;
            default -> BossEvent.BossBarColor.PURPLE;
        };
    }

    public static AttributeSupplier.Builder attributes(MobKind k) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, k.cappedHp())
                .add(Attributes.ATTACK_DAMAGE, k.damage)
                .add(Attributes.ARMOR, k.armor)
                .add(Attributes.ARMOR_TOUGHNESS, k.armor / 3)
                .add(Attributes.MOVEMENT_SPEED, k.speed)
                .add(Attributes.FOLLOW_RANGE, k.boss ? 48 : 32)
                .add(Attributes.KNOCKBACK_RESISTANCE, k.boss ? 0.9 : Math.min(0.8, k.width / 3))
                .add(Attributes.ATTACK_KNOCKBACK, k.width > 1.3F ? 1.0 : 0.3);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(ANIM, 0);
        entityData.define(PHASE, 0);
        entityData.define(ELITE, false);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (ANIM.equals(key)) {
            int a = entityData.get(ANIM);
            if (a != A_NONE) animStartTick = tickCount;
            lastAnim = a;
        }
    }

    public int getAnim() { return entityData.get(ANIM); }

    public void setAnim(int a) {
        entityData.set(ANIM, a);
        animStartTick = tickCount;
    }

    public int getPhase() { return entityData.get(PHASE); }

    public void setPhase(int p) { entityData.set(PHASE, p); }

    /** Ticks since the current special animation started (client and server). */
    public float animTime(float partial) { return tickCount - animStartTick + partial; }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true) {
            @Override
            public boolean canUse() { return current == null && !isRanged() && super.canUse(); }

            @Override
            public boolean canContinueToUse() { return current == null && !isRanged() && super.canContinueToUse(); }
        });
        goalSelector.addGoal(3, new KeepDistanceGoal());
        if (ModEntities.kindOf(getType()).speed > 0) {
            goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        }
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, SLMonster.class));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, ShadowEntity.class, true));
        targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, HunterNpc.class, 10, true, false, n -> ((HunterNpc) n).isFighter()));
    }

    /** Ranged monsters keep distance and only use abilities. */
    protected boolean isRanged() { return ranged; }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && getAnim() != A_NONE && current == null && tickCount - animStartTick > 40) setAnim(A_NONE);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (bossBar != null) bossBar.setProgress(getHealth() / getMaxHealth());
        if ((kind.boss || isElite()) && getPhase() == 0 && getHealth() < getMaxHealth() * 0.5F) {
            setPhase(1);
            if (onPhase2 != null) onPhase2.run();
        }
        LivingEntity target = getTarget();
        if (current != null) {
            abilityTick++;
            if (target != null && target.isAlive()) getLookControl().setLookAt(target, 30, 30);
            current.tick.run(this, target, abilityTick);
            if (abilityTick >= current.duration) {
                current = null;
                setAnim(A_NONE);
                globalCooldown = kind.boss ? 25 : 50;
            }
            return;
        }
        for (Ability a : abilities) if (a.cd > 0) a.cd--;
        if (globalCooldown > 0) {
            globalCooldown--;
            return;
        }
        if (target == null || !target.isAlive()) return;
        double dist = distanceTo(target);
        List<Ability> usable = new ArrayList<>();
        for (Ability a : abilities) {
            if (a.cd <= 0 && dist >= a.minRange && dist <= a.maxRange && a.minPhase <= getPhase() && (!a.needsSight || hasLineOfSight(target)))
                usable.add(a);
        }
        if (usable.isEmpty()) return;
        Ability a = usable.get(random.nextInt(usable.size()));
        startAbility(a);
    }

    public void startAbility(Ability a) {
        current = a;
        abilityTick = 0;
        abilityHit = false;
        a.cd = a.cooldown;
        getNavigation().stop();
        setAnim(a.anim);
        if (a.sound != null) playSound(a.sound, kind.boss ? 2.5F : 1.2F, 0.9F + random.nextFloat() * 0.2F);
        a.tick.run(this, getTarget(), 0);
    }

    /** Debug / autotest helper. */
    public void forceAbility(int index) {
        if (index >= 0 && index < abilities.size()) startAbility(abilities.get(index));
    }

    public int abilityCount() { return abilities.size(); }

    protected Ability ability(String name, int anim, int duration, int cooldown, double minRange, double maxRange, Ability.Tick tick) {
        Ability a = new Ability(name, anim, duration, cooldown, minRange, maxRange, tick);
        abilities.add(a);
        return a;
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (src.getEntity() instanceof SLMonster) return false;
        return super.hurt(src, amount * kind.damageTakenFactor());
    }

    @Override
    public boolean doHurtTarget(Entity e) {
        boolean ok = super.doHurtTarget(e);
        if (ok) onMeleeHit(e);
        return ok;
    }

    protected void onMeleeHit(Entity e) {
        if (hitEffect != null && e instanceof LivingEntity le) hitEffect.accept(le);
    }

    /** Casters circle at medium range from their target. */
    class KeepDistanceGoal extends Goal {
        KeepDistanceGoal() { setFlags(java.util.EnumSet.of(Flag.MOVE)); }

        @Override
        public boolean canUse() { return isRanged() && getTarget() != null && getTarget().isAlive() && current == null; }

        @Override
        public void tick() {
            LivingEntity t = getTarget();
            if (t == null) return;
            getLookControl().setLookAt(t, 30, 30);
            double d = distanceTo(t);
            if (tickCount % 10 != 0) return;
            if (d < 7) {
                Vec3 away = position().subtract(t.position()).normalize().scale(6);
                getNavigation().moveTo(getX() + away.x, getY(), getZ() + away.z, 1.2);
            } else if (d > 16 || !hasLineOfSight(t)) {
                getNavigation().moveTo(t, 1.0);
            } else {
                Vec3 side = position().subtract(t.position()).normalize().yRot((float) Math.PI / 2).scale(random.nextBoolean() ? 4 : -4);
                getNavigation().moveTo(getX() + side.x, getY(), getZ() + side.z, 0.9);
            }
        }
    }

    @Override
    public void die(DamageSource src) {
        super.die(src);
        if (!level().isClientSide && (kind.boss || instanceBoss) && !instance.isEmpty() && level() instanceof ServerLevel sl)
            DungeonManager.get(sl.getServer()).onBossKilled(sl, instance, this);
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource src, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(src, looting, recentlyHit);
        Item stone = ModItems.magicStone(kind.rank);
        int n = kind.boss ? 3 + random.nextInt(3) : (random.nextFloat() < 0.55F ? 1 : 0);
        if (n > 0) spawnAtLocation(new ItemStack(stone, n));
        for (MobLoot.Drop d : MobLoot.drops(kind)) {
            if (random.nextFloat() < d.chance() + looting * 0.02F) spawnAtLocation(new ItemStack(d.item().get(), d.min() + random.nextInt(d.max() - d.min() + 1)));
        }
    }

    /** Turns a normal monster into an elite dungeon boss (more health and damage, boss bar). */
    public void makeElite(Component name, float hpMult, float dmgMult) {
        instanceBoss = true;
        setCustomName(name);
        if (bossBar == null) bossBar = new ServerBossEvent(getDisplayName(), colorFor(), BossEvent.BossBarOverlay.NOTCHED_10);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(Math.min(1000, getAttribute(Attributes.MAX_HEALTH).getBaseValue() * hpMult));
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue() * dmgMult);
        getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.8);
        setHealth(getMaxHealth());
        setPersistenceRequired();
        entityData.set(ELITE, true);
    }

    public boolean isElite() { return entityData.get(ELITE); }

    @Override
    public void startSeenByPlayer(ServerPlayer p) {
        super.startSeenByPlayer(p);
        if (bossBar != null) bossBar.addPlayer(p);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer p) {
        super.stopSeenByPlayer(p);
        if (bossBar != null) bossBar.removePlayer(p);
    }

    @Override
    public void setCustomName(Component name) {
        super.setCustomName(name);
        if (bossBar != null) bossBar.setName(getDisplayName());
    }

    @Override
    public boolean removeWhenFarAway(double d) { return !kind.boss && !instanceBoss && instance.isEmpty() && super.removeWhenFarAway(d); }

    @Override
    public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        t.putString("SLInstance", instance);
        t.putInt("SLPhase", getPhase());
        t.putBoolean("SLBoss", instanceBoss);
        t.putBoolean("SLElite", isElite());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        instance = t.getString("SLInstance");
        setPhase(t.getInt("SLPhase"));
        instanceBoss = t.getBoolean("SLBoss");
        entityData.set(ELITE, t.getBoolean("SLElite"));
        if ((instanceBoss || isElite()) && bossBar == null) bossBar = new ServerBossEvent(getDisplayName(), colorFor(), BossEvent.BossBarOverlay.NOTCHED_10);
        if (bossBar != null) bossBar.setName(getDisplayName());
    }

    @Override
    protected SoundEvent getAmbientSound() { return ModSounds.mobAmbient(kind); }

    @Override
    protected SoundEvent getHurtSound(DamageSource src) { return ModSounds.mobHurt(kind); }

    @Override
    protected SoundEvent getDeathSound() { return kind.boss ? ModSounds.BOSS_DEATH.get() : SoundEvents.GENERIC_DEATH; }

    @Override
    protected float getSoundVolume() { return kind.boss ? 2F : 1F; }

    @Override
    public float getVoicePitch() { return (kind.width > 1.4F ? 0.7F : 1F) + (random.nextFloat() - 0.5F) * 0.15F; }

    @Override
    public boolean canBeLeashed(Player p) { return false; }

    @Override
    protected boolean shouldDespawnInPeaceful() { return instance.isEmpty() && !kind.boss; }

    // ----- helpers used by abilities -----

    public List<LivingEntity> enemiesAround(double r) {
        return level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(r), e -> e != this && e.isAlive() && isEnemy(e));
    }

    public boolean isEnemy(LivingEntity e) {
        if (e instanceof Player p) return !p.isCreative() && !p.isSpectator();
        if (e instanceof ShadowEntity) return true;
        if (e instanceof HunterNpc n) return n.isFighter();
        return e == getTarget();
    }

    public void dashTowards(Vec3 dir, double speed) {
        setDeltaMovement(dir.normalize().scale(speed).add(0, 0.1, 0));
        hurtMarked = true;
    }

    public void hurtAround(double r, float dmg, double knock) {
        for (LivingEntity e : enemiesAround(r)) {
            e.hurt(damageSources().mobAttack(this), dmg);
            Vec3 d = e.position().subtract(position()).normalize();
            e.push(d.x * knock, 0.35 * knock, d.z * knock);
            e.hurtMarked = true;
        }
    }

    /** One special move of a monster. */
    public static class Ability {
        @FunctionalInterface
        public interface Tick {
            void run(SLMonster self, LivingEntity target, int t);
        }

        public final String name;
        public final int anim, duration, cooldown;
        public final double minRange, maxRange;
        public final Tick tick;
        public int cd;
        public int minPhase;
        public boolean needsSight = true;
        public SoundEvent sound;

        Ability(String name, int anim, int duration, int cooldown, double minRange, double maxRange, Tick tick) {
            this.name = name;
            this.anim = anim;
            this.duration = duration;
            this.cooldown = cooldown;
            this.minRange = minRange;
            this.maxRange = maxRange;
            this.tick = tick;
            this.cd = cooldown / 3;
        }

        public Ability phase(int p) {
            minPhase = p;
            return this;
        }

        public Ability sound(SoundEvent s) {
            sound = s;
            return this;
        }

        public Ability noSight() {
            needsSight = false;
            return this;
        }
    }
}
