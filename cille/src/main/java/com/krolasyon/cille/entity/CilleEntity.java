package com.krolasyon.cille.entity;

import com.krolasyon.cille.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Shared Cille behaviour: lives inside a marble, bound to its Cahan (the first player to throw the marble), obeys only
 * that player's battle commands, fights rival Cilles and monsters, never dies while owned (retreats into its marble),
 * ability state machine synced to the client as entity events. Without an owner it is a wild, boss-class beast.
 */
public abstract class CilleEntity extends PathfinderMob {
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER = SynchedEntityData.defineId(CilleEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> DATA_STAY = SynchedEntityData.defineId(CilleEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_RUNNING = SynchedEntityData.defineId(CilleEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_PHASE2 = SynchedEntityData.defineId(CilleEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_SHIELD = SynchedEntityData.defineId(CilleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_BEAM = SynchedEntityData.defineId(CilleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_STANCE = SynchedEntityData.defineId(CilleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_EMPOWERED = SynchedEntityData.defineId(CilleEntity.class, EntityDataSerializers.BOOLEAN);

    /** battle stances chosen by the Cahan */
    public static final int STANCE_AGGRESSIVE = 0, STANCE_DEFENSIVE = 1, STANCE_PASSIVE = 2;
    /** commands the Cahan can give (see CommandPacket); CMD_ABILITY + ability id casts that ability */
    public static final int CMD_ATTACK = 0, CMD_STOP = 1, CMD_DEFEND = 2, CMD_RETREAT = 3, CMD_MOVE = 4, CMD_STAY = 5,
            CMD_RECALL = 6, CMD_AGGRESSIVE = 7, CMD_ABILITY = 20;
    /** ability 0 is always the basic attack; 30/31 are the shared entrance and recall animations */
    public static final int MELEE = 0, INTERRUPTED = 29, RELEASE = 30, RECALL = 31, MAX_ABILITIES = 32;
    public static final int SHIELD_NONE = 0, SHIELD_REFLECT = 1, SHIELD_GUARD = 2, SHIELD_ICE = 3;
    public static final byte EVENT_ANIM_BASE = 70;
    public static final int DEATH_TICKS = 60;
    public static final int DARK = 0x1A1430, VIOLET = 0x8A4CFF, PALE = 0xD8CCFF, INK = 0x05030C;
    private static final ThreadLocal<Boolean> REFLECTING = ThreadLocal.withInitial(() -> false);

    protected final ServerBossEvent bossEvent;

    // server state
    protected int currentAbility = -1;
    protected int abilityTick;
    protected final int[] cooldowns = new int[MAX_ABILITIES];
    protected int globalCooldown = 30;
    @Nullable protected LivingEntity abilityTarget;
    protected int shieldTimer;
    protected float shieldHealth;
    protected int recallTicks = -1;
    protected boolean recallDefeated;
    protected boolean released;
    protected int lastProjectileHit = -1000;
    protected int recentHits;
    protected int retreatTicks;
    protected int moveTicks;
    protected int commandLock;
    @Nullable protected Vec3 movePoint;
    protected String ownerName = "";
    @Nullable protected CompoundTag marbleTag;

    // client animation state
    public int clientAnimId = -1;
    public float clientAnimStart;
    public float runBlend, prevRunBlend, sitBlend, prevSitBlend;

    protected CilleEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.bossEvent = new ServerBossEvent(Component.translatable(type.getDescriptionId()), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
        this.xpReward = 200;
        this.setPersistenceRequired();
        this.bossEvent.setVisible(false);
    }

    // ------------------------------------------------------------------ per-Cille definition
    /** short id used for translation keys, e.g. "kabkaba" -> ability.cille.kabkaba.1 */
    public abstract String cilleName();
    /** the marble item this Cille lives in */
    public abstract Item marbleItem();
    /** abilities the Cahan can order, in menu / hot key order */
    public abstract int[] commandableAbilities();
    /** whether the ability needs an enemy to aim at */
    public abstract boolean needsTarget(int id);
    protected abstract int abilityDuration(int id);
    protected abstract int abilityCooldown(int id);
    protected abstract void tickAbility(int id, int t, @Nullable LivingEntity target);
    /** chosen ability for the current target or -1 */
    protected abstract int chooseAbility(LivingEntity target, double distSqr);
    /** battle instinct when hit (dodges, counters, defences) */
    protected abstract void instinct(DamageSource src, @Nullable LivingEntity attacker);
    /** an enemy projectile is flying straight at us; return true if handled */
    protected abstract boolean reactToProjectile(Projectile p);
    protected abstract void releaseTick(ServerLevel sl, int t);
    protected abstract void onPhase2(ServerLevel sl);
    protected abstract void clientFx();
    public abstract Vec3 mouthPos();
    public abstract double meleeReach();
    /** whether the body should keep facing the target while this ability plays */
    protected boolean facesTarget(int id) { return true; }

    public String abilityKey(int id) { return "ability.cille." + cilleName() + "." + id; }

    // ------------------------------------------------------------------ movement hooks (flying Cilles override)
    public void travelTo(double x, double y, double z, double speed) { this.getNavigation().moveTo(x, y, z, speed); }
    public void travelTo(Entity e, double speed) { this.getNavigation().moveTo(e, speed); }
    public void stopTravel() { this.getNavigation().stop(); }
    public boolean isTravelling() { return this.getNavigation().isInProgress(); }
    protected Goal wanderGoal() { return new WildStrollGoal(this); }

    // ------------------------------------------------------------------ data
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_OWNER, Optional.empty());
        this.entityData.define(DATA_STAY, false);
        this.entityData.define(DATA_RUNNING, false);
        this.entityData.define(DATA_PHASE2, false);
        this.entityData.define(DATA_SHIELD, SHIELD_NONE);
        this.entityData.define(DATA_BEAM, -1);
        this.entityData.define(DATA_STANCE, STANCE_AGGRESSIVE);
        this.entityData.define(DATA_EMPOWERED, false);
    }

    @Nullable public UUID getOwnerUUID() { return this.entityData.get(DATA_OWNER).orElse(null); }
    public boolean isOwned() { return this.entityData.get(DATA_OWNER).isPresent(); }
    public boolean isStaying() { return this.entityData.get(DATA_STAY); }
    public void setStaying(boolean b) { this.entityData.set(DATA_STAY, b); }
    public boolean isRunning() { return this.entityData.get(DATA_RUNNING); }
    public void setRunning(boolean b) { if (b != isRunning()) this.entityData.set(DATA_RUNNING, b); }
    public boolean isPhase2() { return this.entityData.get(DATA_PHASE2); }
    public int getShield() { return this.entityData.get(DATA_SHIELD); }
    protected void setShield(int s) { this.entityData.set(DATA_SHIELD, s); }
    public int getBeamTarget() { return this.entityData.get(DATA_BEAM); }
    protected void setBeam(int id) { this.entityData.set(DATA_BEAM, id); }
    public int getStance() { return this.entityData.get(DATA_STANCE); }
    public void setStance(int s) { this.entityData.set(DATA_STANCE, Mth.clamp(s, 0, 2)); }
    public boolean isEmpowered() { return this.entityData.get(DATA_EMPOWERED); }
    protected void setEmpowered(boolean b) { this.entityData.set(DATA_EMPOWERED, b); }
    public boolean isCasting() { return currentAbility >= 0; }
    public int currentAbility() { return currentAbility; }
    public boolean isRecalling() { return recallTicks >= 0; }

    @Nullable
    public Player getOwner() {
        UUID id = getOwnerUUID();
        return id == null ? null : this.level().getPlayerByUUID(id);
    }

    public void setOwner(@Nullable UUID id, String name) {
        this.entityData.set(DATA_OWNER, Optional.ofNullable(id));
        this.ownerName = name == null ? "" : name;
        updateBossName();
    }

    protected void updateBossName() {
        if (isOwned()) this.bossEvent.setName(Component.translatable("entity.cille.owned", this.getDisplayName(), ownerName));
        else this.bossEvent.setName(this.getDisplayName());
        this.bossEvent.setColor(isOwned() ? BossEvent.BossBarColor.BLUE : BossEvent.BossBarColor.PURPLE);
    }

    /** called when the marble bursts */
    public void fromMarble(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        this.marbleTag = tag == null ? new CompoundTag() : tag.copy();
        if (tag != null && tag.hasUUID("Owner")) setOwner(tag.getUUID("Owner"), tag.getString("OwnerName"));
        if (tag != null && tag.contains("Health")) this.setHealth(Mth.clamp(tag.getFloat("Health"), 1F, this.getMaxHealth()));
        if (stack.hasCustomHoverName()) this.setCustomName(stack.getHoverName());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        UUID o = getOwnerUUID();
        if (o != null) tag.putUUID("Owner", o);
        tag.putString("OwnerName", ownerName);
        tag.putBoolean("Staying", isStaying());
        tag.putBoolean("Released", released);
        tag.putBoolean("Phase2", isPhase2());
        tag.putInt("Stance", getStance());
        if (marbleTag != null) tag.put("Marble", marbleTag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) setOwner(tag.getUUID("Owner"), tag.getString("OwnerName"));
        setStaying(tag.getBoolean("Staying"));
        released = tag.getBoolean("Released");
        this.entityData.set(DATA_PHASE2, tag.getBoolean("Phase2"));
        setStance(tag.getInt("Stance"));
        if (tag.contains("Marble")) marbleTag = tag.getCompound("Marble");
        updateBossName();
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        updateBossName();
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override public boolean causeFallDamage(float dist, float mult, DamageSource src) { return false; }
    @Override public boolean canChangeDimensions() { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public AABB getBoundingBoxForCulling() { return this.getBoundingBox().inflate(4.0D, 4.0D, 4.0D); }
    @Override public boolean canBeLeashed(Player p) { return false; }

    // ------------------------------------------------------------------ friend / foe
    /** Cille bond: who this Cille is allowed to fight at all */
    public boolean canFight(@Nullable Entity e) {
        if (!(e instanceof LivingEntity) || e == this || !e.isAlive()) return false;
        if (e instanceof Player p && (p.isCreative() || p.isSpectator())) return false;
        if (e instanceof net.minecraft.world.entity.decoration.ArmorStand) return false;
        UUID o = getOwnerUUID();
        if (o != null) {
            if (e.getUUID().equals(o)) return false;
            if (e instanceof CilleEntity k && o.equals(k.getOwnerUUID())) return false;
            if (e instanceof OwnableEntity t && o.equals(t.getOwnerUUID())) return false;
            return true;
        }
        return !(e instanceof CilleEntity k && !k.isOwned() && k.getType() == this.getType());
    }

    /** who area attacks hurt: enemies only — never the owner, its Cilles/pets or bystanding animals/villagers */
    public boolean isHostileTo(@Nullable Entity e) {
        if (!canFight(e)) return false;
        LivingEntity le = (LivingEntity) e;
        if (e == this.getTarget() || e == abilityTarget) return true;
        if (e instanceof Enemy || e instanceof CilleEntity) return true;
        if (le.getLastHurtMob() == this) return true;
        if (e instanceof Mob m && (m.getTarget() == this || (m.getTarget() != null && m.getTarget() == getOwner()))) return true;
        return !isOwned() && e instanceof Player;
    }

    @Override
    public boolean isAlliedTo(Entity e) {
        UUID o = getOwnerUUID();
        if (o != null && (e.getUUID().equals(o) || (e instanceof CilleEntity k && o.equals(k.getOwnerUUID())))) return true;
        return super.isAlliedTo(e);
    }

    public List<LivingEntity> hostilesAround(Vec3 c, double r) {
        return this.level().getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r, r * 0.8, r),
                e -> isHostileTo(e) && e.position().distanceToSqr(c) <= r * r * 1.2);
    }

    public List<LivingEntity> hostilesIn(AABB box) {
        return this.level().getEntitiesOfClass(LivingEntity.class, box, this::isHostileTo);
    }

    private boolean autoTarget(LivingEntity e) {
        if (!canFight(e)) return false;
        if (e instanceof CilleEntity) return true;                 // rival Cilles always fight
        if (isOwned()) return e instanceof Enemy && !(e instanceof net.minecraft.world.entity.monster.Creeper);
        return e instanceof Enemy || e instanceof Player;
    }

    protected void updateTarget() {
        if (retreatTicks > 0 || getStance() == STANCE_PASSIVE) {
            if (getTarget() != null) setTarget(null);
            return;
        }
        LivingEntity cur = getTarget();
        if (cur != null && (!cur.isAlive() || !canFight(cur) || distanceToSqr(cur) > 48 * 48)) {
            setTarget(null);
            cur = null;
            commandLock = 0;
        }
        if (commandLock > 0 && cur != null) return;          // the Cahan picked this target
        boolean defensive = getStance() == STANCE_DEFENSIVE;
        Player owner = getOwner();
        if (owner != null) {
            LivingEntity atk = owner.getLastHurtByMob();
            if (atk != null && owner.tickCount - owner.getLastHurtByMobTimestamp() < 80 && canFight(atk)) {
                if (cur != atk) onOwnerAttacked(atk);
                return;
            }
            LivingEntity hit = owner.getLastHurtMob();
            if (!defensive && hit != null && owner.tickCount - owner.getLastHurtMobTimestamp() < 80 && canFight(hit) && cur != hit && !isStaying()) {
                setTarget(hit);
                return;
            }
        }
        LivingEntity atk = getLastHurtByMob();
        if (atk != null && this.tickCount - getLastHurtByMobTimestamp() < 160 && canFight(atk) && cur != atk) {
            if (isStaying()) setStaying(false);
            setTarget(atk);
            return;
        }
        if (cur != null || isStaying() || defensive || moveTicks > 0 || this.tickCount % 10 != 0) return;
        double r = isOwned() ? 18 : 28;
        LivingEntity best = null;
        double bd = Double.MAX_VALUE;
        for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(r, 10, r), this::autoTarget)) {
            double d = e.distanceToSqr(this) * (e instanceof CilleEntity ? 0.25 : 1.0);
            if (d < bd && this.hasLineOfSight(e)) { bd = d; best = e; }
        }
        if (best != null) setTarget(best);
    }

    /** strong Cille bond: the owner was hit — rage and go for the attacker */
    protected void onOwnerAttacked(LivingEntity atk) {
        setTarget(atk);
        if (isStaying()) setStaying(false);
        this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 0, false, true));
    }

    // ------------------------------------------------------------------ goals
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new HoldGoal(this));
        this.goalSelector.addGoal(2, new CommandMoveGoal(this));
        this.goalSelector.addGoal(3, new ChaseGoal(this));
        this.goalSelector.addGoal(4, new FollowOwnerGoal(this));
        this.goalSelector.addGoal(7, wanderGoal());
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    // ------------------------------------------------------------------ interaction (only the Cahan commands its Cille)
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!isOwned()) return InteractionResult.PASS;
        boolean isOwner = player.getUUID().equals(getOwnerUUID());
        if (this.level().isClientSide()) return isOwner ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
        if (!isOwner) {
            player.displayClientMessage(Component.translatable("msg.cille.not_owner", this.getDisplayName(), ownerName).withStyle(ChatFormatting.DARK_PURPLE), true);
            this.playSound(getAmbientSound(), 1.5F, 0.7F);
            return InteractionResult.CONSUME;
        }
        if (isRecalling()) return InteractionResult.CONSUME;
        if (player.isShiftKeyDown()) {
            startRecall(false);
            tell(player, "msg.cille.recall", this.getDisplayName());
        } else {
            setStaying(!isStaying());
            if (isStaying()) { setTarget(null); stopTravel(); }
            tell(player, isStaying() ? "msg.cille.stay" : "msg.cille.follow");
        }
        return InteractionResult.CONSUME;
    }

    // ------------------------------------------------------------------ damage, instincts, defeat
    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (this.level().isClientSide()) return super.hurt(src, amount);
        if (isRecalling()) return false;
        boolean bypass = src.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
        if (currentAbility == RELEASE && !bypass) return false;
        Entity atk = src.getEntity();
        int shield = getShield();
        if (shield == SHIELD_REFLECT && !bypass) {
            if (atk instanceof LivingEntity la && la != this && !REFLECTING.get()) {
                REFLECTING.set(true);
                try {
                    la.hurt(damageSources().thorns(this), amount);
                } finally {
                    REFLECTING.set(false);
                }
                Vec3 m = la.getBoundingBox().getCenter();
                ServerLevel sl = serverLevel();
                sl.sendParticles(dust(VIOLET, 2.0F), m.x, m.y, m.z, 20, 0.4, 0.5, 0.4, 0);
                sl.sendParticles(ParticleTypes.REVERSE_PORTAL, m.x, m.y, m.z, 25, 0.3, 0.4, 0.3, 0.3);
            }
            sound(ModSounds.REFLECT_HIT.get(), 1.6F, 0.85F + random.nextFloat() * 0.15F);
            return false;
        }
        if (shield == SHIELD_ICE && !bypass) {
            shieldHealth -= amount;
            ServerLevel sl = serverLevel();
            sl.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + getBbHeight() * 0.5, getZ(), 16, 1.2, 0.8, 1.2, 0.05);
            if (atk instanceof LivingEntity la && la != this && la.distanceToSqr(this) < 36) {
                la.setTicksFrozen(Math.min(la.getTicksRequiredToFreeze() + 80, la.getTicksFrozen() + 60));
                la.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
            }
            if (shieldHealth <= 0) {
                setShield(SHIELD_NONE);
                shieldTimer = 0;
                onIceShieldBroken(sl);
            } else {
                this.playSound(ModSounds.ICE_HIT.get(), 1.4F, 0.9F + random.nextFloat() * 0.1F);
            }
            return false;
        }
        if (shield == SHIELD_GUARD && !bypass) {
            amount *= 0.3F;
            serverLevel().sendParticles(dust(DARK, 2.0F), getX(), getY() + 1.5, getZ(), 10, 1.0, 0.8, 1.0, 0);
        }
        boolean hurt = super.hurt(src, amount);
        if (hurt && this.isAlive() && !isRecalling()) {
            recentHits += 1;
            if (src.getDirectEntity() instanceof Projectile) lastProjectileHit = this.tickCount;
            instinct(src, atk instanceof LivingEntity l && canFight(l) ? l : null);
        }
        return hurt;
    }

    protected void onIceShieldBroken(ServerLevel sl) {
        sl.sendParticles(ParticleTypes.ITEM_SNOWBALL, getX(), getY() + getBbHeight() * 0.5, getZ(), 60, 1.5, 1.0, 1.5, 0.2);
        this.playSound(ModSounds.ICE_BREAK.get(), 2.0F, 0.9F);
    }

    /** used by instinct code: counter attack with the basic attack */
    protected boolean counterMelee(@Nullable LivingEntity la, DamageSource src) {
        if (la != null && src.getDirectEntity() == la && getStance() != STANCE_PASSIVE && retreatTicks <= 0
                && distanceToSqr(la) < (meleeReach() + 2) * (meleeReach() + 2) && random.nextFloat() < 0.45F) {
            cooldowns[MELEE] = 0;
            if (currentAbility != MELEE) startAbility(MELEE, la);
            return true;
        }
        return false;
    }

    @Override
    public void die(DamageSource src) {
        if (isOwned() && !this.level().isClientSide()) {
            // a defeated Cille is never killed: it retreats into its marble
            this.setHealth(1F);
            startRecall(true);
            return;
        }
        super.die(src);
        endAbility();
        this.bossEvent.setProgress(0F);
    }

    public void startRecall(boolean defeated) {
        endAbility();
        this.recallTicks = 25;
        this.recallDefeated = defeated;
        this.setTarget(null);
        this.stopTravel();
        this.level().broadcastEntityEvent(this, (byte) (EVENT_ANIM_BASE + RECALL));
        this.playSound(defeated ? getHurtSound(damageSources().generic()) : getAmbientSound(), 2.0F, defeated ? 0.7F : 0.95F);
    }

    private void finishRecall() {
        ServerLevel sl = serverLevel();
        Vec3 c = position().add(0, 1.0, 0);
        sl.sendParticles(ParticleTypes.FLASH, c.x, c.y, c.z, 1, 0, 0, 0, 0);
        sl.sendParticles(ParticleTypes.REVERSE_PORTAL, c.x, c.y, c.z, 80, 1.0, 1.0, 1.0, 0.6);
        soundAt(c, ModSounds.MARBLE_CLACK.get(), 1.5F, 0.8F);
        ItemStack stack = new ItemStack(marbleItem());
        CompoundTag tag = marbleTag != null ? marbleTag.copy() : new CompoundTag();
        UUID o = getOwnerUUID();
        if (o != null) { tag.putUUID("Owner", o); tag.putString("OwnerName", ownerName); }
        tag.putFloat("Health", recallDefeated ? getMaxHealth() * 0.3F : getHealth());
        tag.putLong("Ready", this.level().getGameTime() + (recallDefeated ? 600 : 40));
        stack.setTag(tag);
        if (this.hasCustomName()) stack.setHoverName(this.getCustomName());
        Player owner = getOwner();
        if (owner != null && owner.isAlive() && owner.distanceToSqr(this) < 64 * 64) {
            if (!owner.getInventory().add(stack)) owner.drop(stack, false);
            if (recallDefeated) owner.displayClientMessage(Component.translatable("msg.cille.defeated", this.getDisplayName()).withStyle(ChatFormatting.RED), true);
        } else {
            this.spawnAtLocation(stack, 0.5F);
        }
        this.discard();
    }

    @Override
    protected void tickDeath() {
        ++this.deathTime;
        if (this.isNoGravity()) this.setNoGravity(false);
        if (this.level().isClientSide()) {
            for (int i = 0; i < 3; i++)
                this.level().addParticle(i == 0 ? ParticleTypes.LARGE_SMOKE : dust(i == 1 ? DARK : VIOLET, 1.8F),
                        getX() + (random.nextDouble() - 0.5) * 3, getY() + random.nextDouble() * 2.2, getZ() + (random.nextDouble() - 0.5) * 3, 0, 0.05, 0);
        } else {
            if (this.deathTime == 1) this.playSound(getDeathSoundEvent(), 4.0F, 1.0F);
            if (this.deathTime >= DEATH_TICKS && !this.isRemoved()) {
                ServerLevel sl = serverLevel();
                sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 1, getZ(), 1, 0, 0, 0, 0);
                sl.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1, getZ(), 120, 1.5, 1.0, 1.5, 0.4);
                this.level().broadcastEntityEvent(this, (byte) 60);
                this.remove(Entity.RemovalReason.KILLED);
            }
        }
    }

    /** the long death roar (vanilla death sound is muted, we play this ourselves) */
    protected abstract SoundEvent getDeathSoundEvent();

    @Override
    protected SoundEvent getDeathSound() { return null; }

    @Override
    protected void dropCustomDeathLoot(DamageSource src, int looting, boolean hitByPlayer) {
        super.dropCustomDeathLoot(src, looting, hitByPlayer);
        // a defeated wild Cille can be captured: its empty, unbound marble drops
        this.spawnAtLocation(new ItemStack(marbleItem()));
    }

    // ------------------------------------------------------------------ ability machine
    protected int duration(int id) { return id == RELEASE ? 36 : abilityDuration(id); }
    protected int cooldown(int id) { return id == RELEASE || id == RECALL ? 0 : abilityCooldown(id); }
    public boolean ready(int id) { return cooldowns[id] <= 0; }
    public float cooldownOf(int id) { return cooldowns[id] / (float) Math.max(1, cooldown(id)); }
    /** cooldown multiplier (sealed power etc.) */
    protected float cooldownScale() { return isPhase2() ? 0.65F : 1F; }

    protected void startAbility(int id, @Nullable LivingEntity target) {
        if (currentAbility >= 0) endAbility();
        this.currentAbility = id;
        this.abilityTick = 0;
        this.abilityTarget = target;
        this.cooldowns[id] = (int) (cooldown(id) * cooldownScale());
        if (id != MELEE) this.globalCooldown = isPhase2() ? 22 : 36;
        this.stopTravel();
        this.setRunning(false);
        onAbilityStart(id);
        this.level().broadcastEntityEvent(this, (byte) (EVENT_ANIM_BASE + id));
    }

    protected void onAbilityStart(int id) {}

    protected void endAbility() {
        if (getShield() == SHIELD_REFLECT) setShield(shieldTimer > 0 ? SHIELD_GUARD : SHIELD_NONE);
        if (getBeamTarget() >= 0) setBeam(-1);
        onAbilityEnd(currentAbility);
        this.currentAbility = -1;
        this.abilityTarget = null;
    }

    protected void onAbilityEnd(int id) {}

    /** cuts the current technique short (e.g. hit by Behmud's sound wave) */
    public void interrupt() {
        if (!isCasting() || currentAbility == RELEASE) return;
        endAbility();
        this.globalCooldown = Math.max(globalCooldown, 30);
        if (!this.level().isClientSide()) {
            serverLevel().sendParticles(ParticleTypes.ANGRY_VILLAGER, getX(), getY() + getBbHeight() + 0.3, getZ(), 3, 0.6, 0.2, 0.6, 0);
            this.level().broadcastEntityEvent(this, (byte) (EVENT_ANIM_BASE + INTERRUPTED));
        }
    }

    // ------------------------------------------------------------------ server tick
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        ServerLevel sl = serverLevel();

        if (recallTicks >= 0) {
            if (recallTicks % 3 == 0) sl.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1.2, getZ(), 12, 1.0, 1.0, 1.0, 0.2);
            if (--recallTicks < 0) finishRecall();
            return;
        }

        // entrance: grow out of the marble (once, also for spawn-egg Cilles)
        if (!released && this.tickCount >= 2) {
            released = true;
            startAbility(RELEASE, null);
        }

        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        if (this.tickCount % 10 == 0) this.bossEvent.setVisible(!isOwned() || getTarget() != null);
        if (this.tickCount % 40 == 0) recentHits = Math.max(0, recentHits - 1);

        if (!isPhase2() && this.getHealth() < this.getMaxHealth() * 0.5F) {
            this.entityData.set(DATA_PHASE2, true);
            onPhase2(sl);
        }

        for (int i = 0; i < cooldowns.length; i++) if (cooldowns[i] > 0) cooldowns[i]--;
        if (globalCooldown > 0) globalCooldown--;
        if (shieldTimer > 0 && --shieldTimer == 0 && (getShield() == SHIELD_GUARD || getShield() == SHIELD_ICE)) setShield(SHIELD_NONE);

        if (getShield() == SHIELD_REFLECT) reflectProjectiles(sl);
        else if (!isCasting() && this.tickCount % 2 == 0) senseProjectiles();

        if (retreatTicks > 0) retreatTicks--;
        if (moveTicks > 0) moveTicks--;
        if (commandLock > 0) commandLock--;
        if (currentAbility != RELEASE) updateTarget();
        LivingEntity target = this.getTarget();

        if (currentAbility >= 0) {
            LivingEntity t = abilityTarget != null && abilityTarget.isAlive() ? abilityTarget : target;
            if (t != null && facesTarget(currentAbility)) faceTowards(t);
            int id = currentAbility;
            if (id == RELEASE) releaseTick(sl, abilityTick);
            else tickAbility(id, abilityTick, t);
            abilityTick++;
            if (currentAbility == id && abilityTick >= duration(id)) endAbility();
        } else if (target != null && !isStaying() && retreatTicks <= 0 && getStance() != STANCE_PASSIVE) {
            int choice = chooseAbility(target, this.distanceToSqr(target));
            if (choice >= 0) startAbility(choice, target);
        }
    }

    /** reflection shield: incoming projectiles are turned around and now belong to this Cille */
    private void reflectProjectiles(ServerLevel sl) {
        double r = getBbWidth() * 0.5 + 2.2;
        for (Projectile p : this.level().getEntitiesOfClass(Projectile.class, this.getBoundingBox().inflate(2.2), pr -> pr.getOwner() != this)) {
            Vec3 v = p.getDeltaMovement();
            Vec3 to = position().add(0, getBbHeight() * 0.5, 0).subtract(p.position());
            if (v.lengthSqr() < 0.01 || v.dot(to) <= 0 || to.length() > r + 1) continue;
            p.setOwner(this);
            p.setDeltaMovement(v.scale(-1.15));
            if (p instanceof AbstractHurtingProjectile h) {
                h.xPower = -h.xPower;
                h.yPower = -h.yPower;
                h.zPower = -h.zPower;
            }
            p.hurtMarked = true;
            sl.sendParticles(ParticleTypes.FLASH, p.getX(), p.getY(), p.getZ(), 1, 0, 0, 0, 0);
            sl.sendParticles(dust(VIOLET, 1.8F), p.getX(), p.getY(), p.getZ(), 12, 0.2, 0.2, 0.2, 0);
            soundAt(p.position(), ModSounds.REFLECT_HIT.get(), 1.5F, 0.95F);
        }
    }

    /** instinct: notices projectiles flying straight at it */
    private void senseProjectiles() {
        for (Projectile p : this.level().getEntitiesOfClass(Projectile.class, this.getBoundingBox().inflate(5), pr -> pr.getOwner() != this && !(pr instanceof MarbleEntity))) {
            if (p.getOwner() != null && !canFight(p.getOwner())) continue;
            Vec3 v = p.getDeltaMovement();
            Vec3 to = position().add(0, getBbHeight() * 0.5, 0).subtract(p.position());
            if (v.lengthSqr() > 0.2 && v.normalize().dot(to.normalize()) > 0.92 && random.nextFloat() < 0.35F) {
                if (reactToProjectile(p)) return;
            }
        }
    }

    // ------------------------------------------------------------------ client tick
    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            prevRunBlend = runBlend;
            runBlend += ((isRunning() ? 1F : 0F) - runBlend) * 0.18F;
            prevSitBlend = sitBlend;
            boolean sit = isStaying() && getDeltaMovement().horizontalDistanceSqr() < 1.0E-4;
            sitBlend += ((sit ? 1F : 0F) - sitBlend) * 0.08F;
            if (this.isAlive()) {
                clientFx();
                if (clientAnimId == RECALL) {
                    for (int i = 0; i < 3; i++)
                        this.level().addParticle(ParticleTypes.REVERSE_PORTAL, getX() + (random.nextDouble() - 0.5) * 3, getY() + random.nextDouble() * 2.5,
                                getZ() + (random.nextDouble() - 0.5) * 3, 0, 0, 0);
                }
            }
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id >= EVENT_ANIM_BASE && id < EVENT_ANIM_BASE + MAX_ABILITIES) {
            this.clientAnimId = id - EVENT_ANIM_BASE;
            this.clientAnimStart = this.tickCount;
        } else {
            super.handleEntityEvent(id);
        }
    }

    // ------------------------------------------------------------------ battle commands from the Cahan
    protected static void tell(Player p, String key, Object... args) {
        p.displayClientMessage(Component.translatable(key, args).withStyle(ChatFormatting.AQUA), true);
    }

    /** a command sent by a player through the command menu / hot keys; only the Cahan is obeyed */
    public void command(ServerPlayer p, int cmd, int targetId, @Nullable Vec3 point) {
        if (!p.getUUID().equals(getOwnerUUID())) {
            p.displayClientMessage(Component.translatable("msg.cille.not_owner", this.getDisplayName(), ownerName).withStyle(ChatFormatting.DARK_PURPLE), true);
            return;
        }
        if (isRecalling() || currentAbility == RELEASE || !this.isAlive()) return;
        Entity te = targetId >= 0 ? this.level().getEntity(targetId) : null;
        LivingEntity tgt = te instanceof LivingEntity l && canFight(l) && l.distanceToSqr(this) < 64 * 64 ? l : null;
        Component me = this.getDisplayName();
        switch (cmd) {
            case CMD_ATTACK -> {
                LivingEntity t = tgt != null ? tgt : getTarget();
                if (t == null) { tell(p, "msg.cille.cmd.no_target"); return; }
                setStance(STANCE_AGGRESSIVE);
                retreatTicks = 0;
                moveTicks = 0;
                setStaying(false);
                setTarget(t);
                commandLock = 300;
                onAttackCommand(t);
                tell(p, "msg.cille.cmd.attack", me, t.getDisplayName());
            }
            case CMD_STOP -> {
                setStance(STANCE_PASSIVE);
                setTarget(null);
                commandLock = 0;
                if (isCasting() && !isDefensive(currentAbility)) endAbility();
                stopTravel();
                tell(p, "msg.cille.cmd.stop", me);
            }
            case CMD_AGGRESSIVE -> {
                setStance(STANCE_AGGRESSIVE);
                tell(p, "msg.cille.cmd.aggressive", me);
            }
            case CMD_DEFEND -> {
                setStance(STANCE_DEFENSIVE);
                retreatTicks = 0;
                int ab = defendAbility();
                if (ab >= 0 && (!isCasting() || currentAbility == MELEE)) startAbility(ab, getTarget());
                tell(p, "msg.cille.cmd.defend", me);
            }
            case CMD_RETREAT -> {
                setTarget(null);
                commandLock = 0;
                if (isCasting() && !isDefensive(currentAbility)) endAbility();
                retreatTicks = 140;
                moveTicks = 0;
                setStaying(false);
                tell(p, "msg.cille.cmd.retreat", me);
            }
            case CMD_MOVE -> {
                if (point == null || point.distanceToSqr(position()) > 80 * 80) return;
                movePoint = point;
                moveTicks = 240;
                retreatTicks = 0;
                setStaying(false);
                setTarget(null);
                commandLock = 0;
                if (isCasting() && !isDefensive(currentAbility)) endAbility();
                tell(p, "msg.cille.cmd.move", me);
            }
            case CMD_STAY -> {
                setStaying(!isStaying());
                if (isStaying()) { setTarget(null); stopTravel(); }
                tell(p, isStaying() ? "msg.cille.stay" : "msg.cille.follow");
            }
            case CMD_RECALL -> {
                startRecall(false);
                tell(p, "msg.cille.recall", me);
            }
            default -> {
                if (cmd >= CMD_ABILITY) useAbility(p, cmd - CMD_ABILITY, tgt);
            }
        }
    }

    /** first ready defensive ability for the "defend" order, or -1 */
    protected abstract int defendAbility();
    protected abstract boolean isDefensive(int id);
    protected void onAttackCommand(LivingEntity t) { this.playSound(getAmbientSound(), 2.0F, 0.9F); }

    private void useAbility(Player p, int id, @Nullable LivingEntity tgt) {
        boolean allowed = false;
        for (int a : commandableAbilities()) if (a == id) allowed = true;
        if (!allowed) return;
        LivingEntity t = tgt != null ? tgt : getTarget();
        if (needsTarget(id) && t == null) { tell(p, "msg.cille.cmd.no_target"); return; }
        if (!ready(id)) {
            tell(p, "msg.cille.cmd.cooldown", Component.translatable(abilityKey(id)), (cooldowns[id] + 19) / 20);
            return;
        }
        if (!canUseNow(id)) {
            tell(p, "msg.cille.cmd.cannot", Component.translatable(abilityKey(id)));
            return;
        }
        setStaying(false);
        retreatTicks = 0;
        moveTicks = 0;
        if (needsTarget(id)) setStance(STANCE_AGGRESSIVE);
        if (t != null) {
            setTarget(t);
            commandLock = 300;
        }
        startAbility(id, t);
        tell(p, "msg.cille.cmd.ability", this.getDisplayName(), Component.translatable(abilityKey(id)));
    }

    /** extra condition for commanded abilities (e.g. sealed power only once) */
    protected boolean canUseNow(int id) { return true; }

    // ------------------------------------------------------------------ helpers
    public ServerLevel serverLevel() { return (ServerLevel) this.level(); }

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

    public static ParticleOptions dust(int rgb, float scale) {
        return new DustParticleOptions(new Vector3f(((rgb >> 16) & 255) / 255F, ((rgb >> 8) & 255) / 255F, (rgb & 255) / 255F), scale);
    }

    public void hit(LivingEntity e, DamageSource src, float amount, double knock, double lift) {
        if (e.hurt(src, amount)) knock(e, knock, lift);
    }

    public void knock(LivingEntity e, double knock, double lift) {
        Vec3 d = e.position().subtract(this.position()).multiply(1, 0, 1);
        if (d.lengthSqr() < 1.0E-4) d = forward();
        d = d.normalize();
        double kr = 1.0 - e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) * 0.6;
        e.setDeltaMovement(e.getDeltaMovement().add(d.x * knock * kr, lift * kr, d.z * knock * kr));
        e.hurtMarked = true;
    }

    public void sound(SoundEvent s, float vol, float pitch) {
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), s, SoundSource.HOSTILE, vol, pitch);
    }

    public void soundAt(Vec3 p, SoundEvent s, float vol, float pitch) {
        this.level().playSound(null, p.x, p.y, p.z, s, SoundSource.HOSTILE, vol, pitch);
    }

    @Override protected float getSoundVolume() { return 2.0F; }
    @Override public int getAmbientSoundInterval() { return 160; }

    // ------------------------------------------------------------------ goals
    /** freezes movement while an ability / entrance / recall plays, or while told to stay */
    static class HoldGoal extends Goal {
        private final CilleEntity k;

        HoldGoal(CilleEntity k) {
            this.k = k;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() { return (k.isCasting() && k.holdsStillWhileCasting()) || k.isRecalling() || (k.isStaying() && k.getTarget() == null); }

        @Override
        public void start() { k.stopTravel(); }
    }

    /** abilities that move the Cille themselves (dashes) return false here */
    protected boolean holdsStillWhileCasting() { return true; }

    /** where to stand/fly while chasing a target */
    protected Vec3 chasePoint(LivingEntity t) { return t.position(); }

    static class ChaseGoal extends Goal {
        private final CilleEntity k;
        private int repath;

        ChaseGoal(CilleEntity k) {
            this.k = k;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = k.getTarget();
            return t != null && t.isAlive() && !k.isCasting() && !k.isStaying() && k.retreatTicks <= 0 && k.moveTicks <= 0;
        }

        @Override public boolean canContinueToUse() { return canUse(); }
        @Override public void start() { repath = 0; }
        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override
        public void stop() {
            k.stopTravel();
            k.setRunning(false);
        }

        @Override
        public void tick() {
            LivingEntity t = k.getTarget();
            if (t == null) return;
            k.getLookControl().setLookAt(t, 30.0F, 30.0F);
            double d = k.distanceToSqr(t);
            boolean far = d > 9 * 9;
            k.setRunning(far && k.isTravelling());
            if (--repath <= 0) {
                repath = 4 + k.getRandom().nextInt(5);
                double reach = k.meleeReach() * 0.85;
                Vec3 p = k.chasePoint(t);
                if (d > reach * reach) k.travelTo(p.x, p.y, p.z, far ? 1.5 : 1.0);
                else k.stopTravel();
            }
        }
    }

    /** where to go when following the owner */
    protected Vec3 followPoint(Player o) { return o.position(); }

    static class FollowOwnerGoal extends Goal {
        private final CilleEntity k;
        private int repath;

        FollowOwnerGoal(CilleEntity k) {
            this.k = k;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Player o = k.getOwner();
            return o != null && !k.isStaying() && k.getTarget() == null && !o.isSpectator() && k.moveTicks <= 0 && k.retreatTicks <= 0 && k.distanceToSqr(o) > 10 * 10;
        }

        @Override
        public boolean canContinueToUse() {
            Player o = k.getOwner();
            return o != null && !k.isStaying() && k.getTarget() == null && k.distanceToSqr(o) > 5 * 5;
        }

        @Override
        public void stop() {
            k.stopTravel();
            k.setRunning(false);
        }

        @Override
        public void tick() {
            Player o = k.getOwner();
            if (o == null) return;
            k.getLookControl().setLookAt(o, 10.0F, k.getMaxHeadXRot());
            double d = k.distanceToSqr(o);
            if (d > 30 * 30) {
                for (int i = 0; i < 10; i++) {
                    double a = k.getRandom().nextDouble() * Math.PI * 2;
                    if (k.randomTeleport(o.getX() + Math.cos(a) * 4, o.getY() + 1, o.getZ() + Math.sin(a) * 4, true)) break;
                }
                return;
            }
            k.setRunning(d > 16 * 16);
            if (--repath <= 0) {
                repath = 10;
                Vec3 p = k.followPoint(o);
                k.travelTo(p.x, p.y, p.z, d > 16 * 16 ? 1.5 : 1.1);
            }
        }
    }

    /** obeys "retreat" (run back to the Cahan) and "move there" commands */
    static class CommandMoveGoal extends Goal {
        private final CilleEntity k;
        private int repath;

        CommandMoveGoal(CilleEntity k) {
            this.k = k;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() { return !k.isCasting() && (k.retreatTicks > 0 || (k.moveTicks > 0 && k.movePoint != null)); }

        @Override public boolean canContinueToUse() { return canUse(); }
        @Override public void start() { repath = 0; }
        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override
        public void stop() {
            k.stopTravel();
            k.setRunning(false);
        }

        @Override
        public void tick() {
            Vec3 dest;
            if (k.retreatTicks > 0) {
                Player o = k.getOwner();
                if (o == null) { k.retreatTicks = 0; return; }
                dest = k.followPoint(o);
                if (k.distanceToSqr(o) < 5 * 5) { k.retreatTicks = 0; return; }
            } else {
                dest = k.movePoint;
                if (k.position().distanceToSqr(dest) < 3 * 3) {
                    k.moveTicks = 0;
                    k.setStaying(true);            // hold the position it was sent to
                    return;
                }
            }
            k.setRunning(true);
            k.getLookControl().setLookAt(dest.x, dest.y + 1, dest.z);
            if (--repath <= 0) {
                repath = 6;
                k.travelTo(dest.x, dest.y, dest.z, 1.6);
            }
        }
    }

    static class WildStrollGoal extends WaterAvoidingRandomStrollGoal {
        private final CilleEntity k;

        WildStrollGoal(CilleEntity k) {
            super(k, 0.7D);
            this.k = k;
        }

        @Override
        public boolean canUse() { return !k.isOwned() && super.canUse(); }
    }
}
