package com.krolasyon.cille.entity;

import com.krolasyon.cille.registry.ModEntities;
import com.krolasyon.cille.registry.ModItems;
import com.krolasyon.cille.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Kabkaba — the dark Cille. Lives inside a marble; thrown marbles that collide release the Cilles to fight.
 * Bound to the player who first threw its marble (its "Cahan"): only that player can command it.
 * Without an owner (spawn egg) it is a wild, boss-class beast.
 */
public class KabkabaEntity extends PathfinderMob {
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER = SynchedEntityData.defineId(KabkabaEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> DATA_STAY = SynchedEntityData.defineId(KabkabaEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_RUNNING = SynchedEntityData.defineId(KabkabaEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_PHASE2 = SynchedEntityData.defineId(KabkabaEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_SHIELD = SynchedEntityData.defineId(KabkabaEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_BEAM = SynchedEntityData.defineId(KabkabaEntity.class, EntityDataSerializers.INT);

    public static final int MELEE = 0, BLACK_HOLE = 1, BLINK = 2, DRAIN = 3, REFLECT = 4, SHADOW = 5, GUARD = 6, SLAM = 7, DODGE = 8, RELEASE = 9, RECALL = 10;
    public static final int SHIELD_NONE = 0, SHIELD_REFLECT = 1, SHIELD_GUARD = 2;
    public static final byte EVENT_ANIM_BASE = 100;
    public static final int DEATH_TICKS = 60;
    public static final int DARK = 0x1A1430, VIOLET = 0x8A4CFF, PALE = 0xD8CCFF, INK = 0x05030C;
    private static final ThreadLocal<Boolean> REFLECTING = ThreadLocal.withInitial(() -> false);

    private final ServerBossEvent bossEvent = new ServerBossEvent(Component.translatable("entity.cille.kabkaba"),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);

    // server state
    private int currentAbility = -1;
    private int abilityTick;
    private final int[] cooldowns = new int[11];
    private int globalCooldown = 30;
    @Nullable private LivingEntity abilityTarget;
    private int shieldTimer;
    private int recallTicks = -1;
    private boolean recallDefeated;
    private boolean released;
    private boolean slamDone;
    private int lastProjectileHit = -1000;
    private int recentHits;
    private String ownerName = "";
    @Nullable private CompoundTag marbleTag;

    // client animation state
    public int clientAnimId = -1;
    public float clientAnimStart;
    public float runBlend, prevRunBlend, sitBlend, prevSitBlend;

    public KabkabaEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 200;
        this.setMaxUpStep(1.5F);
        this.setPersistenceRequired();
        this.bossEvent.setVisible(false);
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
    }

    @Nullable public UUID getOwnerUUID() { return this.entityData.get(DATA_OWNER).orElse(null); }
    public boolean isOwned() { return this.entityData.get(DATA_OWNER).isPresent(); }
    public boolean isStaying() { return this.entityData.get(DATA_STAY); }
    public void setStaying(boolean b) { this.entityData.set(DATA_STAY, b); }
    public boolean isRunning() { return this.entityData.get(DATA_RUNNING); }
    public void setRunning(boolean b) { if (b != isRunning()) this.entityData.set(DATA_RUNNING, b); }
    public boolean isPhase2() { return this.entityData.get(DATA_PHASE2); }
    public int getShield() { return this.entityData.get(DATA_SHIELD); }
    private void setShield(int s) { this.entityData.set(DATA_SHIELD, s); }
    public int getBeamTarget() { return this.entityData.get(DATA_BEAM); }
    private void setBeam(int id) { this.entityData.set(DATA_BEAM, id); }
    public boolean isCasting() { return currentAbility >= 0; }
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

    private void updateBossName() {
        if (isOwned()) this.bossEvent.setName(Component.translatable("entity.cille.kabkaba.owned", this.getDisplayName(), ownerName));
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
        if (marbleTag != null) tag.put("Marble", marbleTag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) setOwner(tag.getUUID("Owner"), tag.getString("OwnerName"));
        setStaying(tag.getBoolean("Staying"));
        released = tag.getBoolean("Released");
        this.entityData.set(DATA_PHASE2, tag.getBoolean("Phase2"));
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
    @Override public AABB getBoundingBoxForCulling() { return this.getBoundingBox().inflate(3.5D, 4.0D, 3.5D); }
    @Override public boolean canBeLeashed(Player p) { return false; }

    // ------------------------------------------------------------------ friend / foe
    /** Cille bond: who this Kabkaba is allowed to fight at all */
    public boolean canFight(@Nullable Entity e) {
        if (!(e instanceof LivingEntity) || e == this || !e.isAlive()) return false;
        if (e instanceof Player p && (p.isCreative() || p.isSpectator())) return false;
        if (e instanceof net.minecraft.world.entity.decoration.ArmorStand) return false;
        UUID o = getOwnerUUID();
        if (o != null) {
            if (e.getUUID().equals(o)) return false;
            if (e instanceof KabkabaEntity k && o.equals(k.getOwnerUUID())) return false;
            if (e instanceof OwnableEntity t && o.equals(t.getOwnerUUID())) return false;
            return true;
        }
        return !(e instanceof KabkabaEntity k && !k.isOwned());
    }

    /** who area attacks hurt: enemies only — never the owner, its pets or bystanding animals/villagers */
    public boolean isHostileTo(@Nullable Entity e) {
        if (!canFight(e)) return false;
        LivingEntity le = (LivingEntity) e;
        if (e == this.getTarget() || e == abilityTarget) return true;
        if (e instanceof Enemy || e instanceof KabkabaEntity) return true;
        if (le.getLastHurtMob() == this) return true;
        if (e instanceof Mob m && (m.getTarget() == this || (m.getTarget() != null && m.getTarget() == getOwner()))) return true;
        return !isOwned() && e instanceof Player;
    }

    @Override
    public boolean isAlliedTo(Entity e) {
        UUID o = getOwnerUUID();
        if (o != null && (e.getUUID().equals(o) || (e instanceof KabkabaEntity k && o.equals(k.getOwnerUUID())))) return true;
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
        if (e instanceof KabkabaEntity) return true;                // rival Cilles always fight
        if (isOwned()) return e instanceof Enemy && !(e instanceof net.minecraft.world.entity.monster.Creeper);
        return e instanceof Enemy || e instanceof Player;
    }

    private void updateTarget() {
        LivingEntity cur = getTarget();
        if (cur != null && (!cur.isAlive() || !canFight(cur) || distanceToSqr(cur) > 48 * 48)) {
            setTarget(null);
            cur = null;
        }
        Player owner = getOwner();
        if (owner != null) {
            LivingEntity atk = owner.getLastHurtByMob();
            if (atk != null && owner.tickCount - owner.getLastHurtByMobTimestamp() < 80 && canFight(atk)) {
                if (cur != atk) onOwnerAttacked(atk);
                return;
            }
            LivingEntity hit = owner.getLastHurtMob();
            if (hit != null && owner.tickCount - owner.getLastHurtMobTimestamp() < 80 && canFight(hit) && cur != hit && !isStaying()) {
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
        if (cur != null || isStaying() || this.tickCount % 10 != 0) return;
        double r = isOwned() ? 18 : 28;
        LivingEntity best = null;
        double bd = Double.MAX_VALUE;
        for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(r, 8, r), this::autoTarget)) {
            double d = e.distanceToSqr(this) * (e instanceof KabkabaEntity ? 0.25 : 1.0);
            if (d < bd && this.hasLineOfSight(e)) { bd = d; best = e; }
        }
        if (best != null) setTarget(best);
    }

    /** strong Cille bond: the owner was hit — rage and go for the attacker */
    private void onOwnerAttacked(LivingEntity atk) {
        setTarget(atk);
        if (isStaying()) setStaying(false);
        this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 0, false, true));
        if (!isCasting() && ready(BLINK) && distanceToSqr(atk) > 64) startAbility(BLINK, atk);
    }

    // ------------------------------------------------------------------ goals
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new HoldGoal(this));
        this.goalSelector.addGoal(2, new ChaseGoal(this));
        this.goalSelector.addGoal(3, new FollowOwnerGoal(this));
        this.goalSelector.addGoal(7, new WildStrollGoal(this));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    // ------------------------------------------------------------------ interaction (only the Cahan commands Kabkaba)
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!isOwned()) return InteractionResult.PASS;
        boolean isOwner = player.getUUID().equals(getOwnerUUID());
        if (this.level().isClientSide()) return isOwner ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
        if (!isOwner) {
            player.displayClientMessage(Component.translatable("msg.cille.not_owner", ownerName).withStyle(ChatFormatting.DARK_PURPLE), true);
            sound(ModSounds.AMBIENT.get(), 1.5F, 0.7F);
            return InteractionResult.CONSUME;
        }
        if (isRecalling() || recallTicks >= 0) return InteractionResult.CONSUME;
        if (player.isShiftKeyDown()) {
            startRecall(false);
            player.displayClientMessage(Component.translatable("msg.cille.recall").withStyle(ChatFormatting.AQUA), true);
        } else {
            setStaying(!isStaying());
            if (isStaying()) { setTarget(null); getNavigation().stop(); }
            player.displayClientMessage(Component.translatable(isStaying() ? "msg.cille.stay" : "msg.cille.follow").withStyle(ChatFormatting.AQUA), true);
        }
        return InteractionResult.CONSUME;
    }

    // ------------------------------------------------------------------ damage, instincts, defeat
    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (this.level().isClientSide()) return super.hurt(src, amount);
        if (isRecalling()) return false;
        if (currentAbility == RELEASE && !src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        Entity atk = src.getEntity();
        if (getShield() == SHIELD_REFLECT && !src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
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
            sound(ModSounds.REFLECT_HIT.get(), 1.6F, 0.9F + random.nextFloat() * 0.3F);
            return false;
        }
        if (getShield() == SHIELD_GUARD && !src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            amount *= 0.3F;
            serverLevel().sendParticles(dust(DARK, 2.0F), getX(), getY() + 1.5, getZ(), 10, 1.0, 0.8, 1.0, 0);
        }
        boolean hurt = super.hurt(src, amount);
        if (hurt && this.isAlive() && !isRecalling()) instinct(src);
        return hurt;
    }

    /** high battle instinct: dodge projectiles, counter melee hits, raise defences when hurt */
    private void instinct(DamageSource src) {
        recentHits += 1;
        Entity direct = src.getDirectEntity();
        Entity atk = src.getEntity();
        if (direct instanceof Projectile) lastProjectileHit = this.tickCount;
        if (isCasting() && currentAbility != MELEE) return;
        LivingEntity la = atk instanceof LivingEntity l && canFight(l) ? l : null;
        if (getHealth() < getMaxHealth() * 0.4F && ready(GUARD) && random.nextFloat() < 0.5F) {
            startAbility(GUARD, la);
        } else if (direct instanceof Projectile && ready(REFLECT) && random.nextFloat() < 0.35F) {
            startAbility(REFLECT, la);
        } else if (direct instanceof Projectile && ready(DODGE) && random.nextFloat() < 0.5F) {
            startAbility(DODGE, la);
        } else if (la != null && direct == la && distanceToSqr(la) < 30 && random.nextFloat() < 0.45F) {
            cooldowns[MELEE] = 0;
            if (currentAbility != MELEE) startAbility(MELEE, la);
        }
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

    private void startRecall(boolean defeated) {
        endAbility();
        this.recallTicks = 25;
        this.recallDefeated = defeated;
        this.setTarget(null);
        this.getNavigation().stop();
        this.level().broadcastEntityEvent(this, (byte) (EVENT_ANIM_BASE + RECALL));
        sound(defeated ? ModSounds.HURT.get() : ModSounds.AMBIENT.get(), 2.0F, defeated ? 0.7F : 1.2F);
    }

    private void finishRecall() {
        ServerLevel sl = serverLevel();
        Vec3 c = position().add(0, 1.0, 0);
        sl.sendParticles(ParticleTypes.FLASH, c.x, c.y, c.z, 1, 0, 0, 0, 0);
        sl.sendParticles(ParticleTypes.REVERSE_PORTAL, c.x, c.y, c.z, 80, 1.0, 1.0, 1.0, 0.6);
        soundAt(c, ModSounds.MARBLE_CLACK.get(), 1.5F, 0.8F);
        ItemStack stack = new ItemStack(ModItems.KABKABA_MISKETI.get());
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
            if (recallDefeated) owner.displayClientMessage(Component.translatable("msg.cille.defeated").withStyle(ChatFormatting.RED), true);
        } else {
            this.spawnAtLocation(stack, 0.5F);
        }
        this.discard();
    }

    @Override
    protected void tickDeath() {
        ++this.deathTime;
        if (this.level().isClientSide()) {
            for (int i = 0; i < 3; i++)
                this.level().addParticle(i == 0 ? ParticleTypes.LARGE_SMOKE : dust(i == 1 ? DARK : VIOLET, 1.8F),
                        getX() + (random.nextDouble() - 0.5) * 3, getY() + random.nextDouble() * 2.2, getZ() + (random.nextDouble() - 0.5) * 3, 0, 0.05, 0);
        } else {
            if (this.deathTime == 1) sound(ModSounds.DEATH.get(), 4.0F, 1.0F);
            if (this.deathTime >= DEATH_TICKS && !this.isRemoved()) {
                ServerLevel sl = serverLevel();
                sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 1, getZ(), 1, 0, 0, 0, 0);
                sl.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1, getZ(), 120, 1.5, 1.0, 1.5, 0.4);
                this.level().broadcastEntityEvent(this, (byte) 60);
                this.remove(Entity.RemovalReason.KILLED);
            }
        }
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource src, int looting, boolean hitByPlayer) {
        super.dropCustomDeathLoot(src, looting, hitByPlayer);
        // a defeated wild Kabkaba can be captured: its empty, unbound marble drops
        this.spawnAtLocation(new ItemStack(ModItems.KABKABA_MISKETI.get()));
    }

    // ------------------------------------------------------------------ abilities
    private int duration(int id) {
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
            case RELEASE -> 36;
            default -> 20;
        };
    }

    private int cooldown(int id) {
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

    public boolean ready(int id) { return cooldowns[id] <= 0; }

    private void startAbility(int id, @Nullable LivingEntity target) {
        if (currentAbility >= 0) endAbility();
        this.currentAbility = id;
        this.abilityTick = 0;
        this.abilityTarget = target;
        this.slamDone = false;
        int cd = cooldown(id);
        this.cooldowns[id] = isPhase2() ? (int) (cd * 0.65F) : cd;
        if (id != MELEE && id != DODGE) this.globalCooldown = isPhase2() ? 22 : 38;
        this.getNavigation().stop();
        this.setRunning(false);
        this.level().broadcastEntityEvent(this, (byte) (EVENT_ANIM_BASE + id));
    }

    private void endAbility() {
        if (currentAbility == REFLECT && getShield() == SHIELD_REFLECT) setShield(shieldTimer > 0 ? SHIELD_GUARD : SHIELD_NONE);
        if (getBeamTarget() >= 0) setBeam(-1);
        this.currentAbility = -1;
        this.abilityTarget = null;
    }

    private int chooseAbility(LivingEntity target, double distSqr) {
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

    private void tickAbility(int id, int t, @Nullable LivingEntity target) {
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
                    sound(ModSounds.BLINK.get(), 1.5F, 1.4F);
                }
            }
            case RELEASE -> {
                if (t == 14) {
                    sound(ModSounds.ROAR.get(), 4.0F, 1.0F);
                    Vec3 mouth = mouthPos();
                    sl.sendParticles(ParticleTypes.SONIC_BOOM, mouth.x, mouth.y, mouth.z, 1, 0, 0, 0, 0);
                    for (int i = 0; i < 72; i++) {
                        double a = i * Math.PI * 2 / 72;
                        sl.sendParticles(dust(i % 3 == 0 ? VIOLET : DARK, 2.4F), getX(), getY() + 0.4, getZ(), 0, Math.cos(a), 0.05, Math.sin(a), 0.8);
                    }
                    for (LivingEntity e : hostilesAround(position(), 7)) {
                        if (e instanceof KabkabaEntity) continue;
                        knock(e, 1.4, 0.4);
                        if (e instanceof Player) e.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0));
                    }
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
            sound(ModSounds.BLINK.get(), 2.0F, 1.2F);
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
        if (t == 2) sound(ModSounds.ROAR.get(), 2.5F, 1.25F);
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
        sound(ModSounds.SLAM.get(), 1.4F, 1.3F);
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

        // entrance: grow out of the marble and roar (once, also for spawn-egg Kabkabas)
        if (!released && this.tickCount >= 2) {
            released = true;
            startAbility(RELEASE, null);
            this.cooldowns[RELEASE] = 0;
        }

        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        if (this.tickCount % 10 == 0) this.bossEvent.setVisible(!isOwned() || getTarget() != null);
        if (this.tickCount % 40 == 0) recentHits = Math.max(0, recentHits - 1);

        if (!isPhase2() && this.getHealth() < this.getMaxHealth() * 0.5F) {
            this.entityData.set(DATA_PHASE2, true);
            sound(ModSounds.ROAR.get(), 4.0F, 0.8F);
            sl.sendParticles(ParticleTypes.SONIC_BOOM, getX(), getY() + 1.6, getZ(), 1, 0, 0, 0, 0);
            for (LivingEntity e : hostilesAround(position(), 8)) knock(e, 1.8, 0.5);
        }

        for (int i = 0; i < cooldowns.length; i++) if (cooldowns[i] > 0) cooldowns[i]--;
        if (globalCooldown > 0) globalCooldown--;
        if (shieldTimer > 0 && --shieldTimer == 0 && getShield() == SHIELD_GUARD) setShield(SHIELD_NONE);

        if (getShield() == SHIELD_REFLECT) reflectProjectiles(sl);
        else if (!isCasting() && ready(DODGE) && this.tickCount % 2 == 0) senseProjectiles();

        if (currentAbility != RELEASE) updateTarget();
        LivingEntity target = this.getTarget();

        if (currentAbility >= 0) {
            LivingEntity t = abilityTarget != null && abilityTarget.isAlive() ? abilityTarget : target;
            if (t != null && currentAbility != SLAM && currentAbility != DODGE) faceTowards(t);
            int id = currentAbility;
            tickAbility(id, abilityTick, t);
            abilityTick++;
            if (currentAbility == id && abilityTick >= duration(id)) endAbility();
        } else if (target != null && !isStaying()) {
            int choice = chooseAbility(target, this.distanceToSqr(target));
            if (choice >= 0) startAbility(choice, target);
        }
    }

    /** reflection shield: incoming projectiles are turned around and now belong to Kabkaba */
    private void reflectProjectiles(ServerLevel sl) {
        for (Projectile p : this.level().getEntitiesOfClass(Projectile.class, this.getBoundingBox().inflate(2.2), pr -> pr.getOwner() != this)) {
            Vec3 v = p.getDeltaMovement();
            Vec3 to = position().add(0, 1.4, 0).subtract(p.position());
            if (v.lengthSqr() < 0.01 || v.dot(to) <= 0) continue;
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
            soundAt(p.position(), ModSounds.REFLECT_HIT.get(), 1.5F, 1.2F);
        }
    }

    /** instinct: an arrow/fireball flying at us gets dodged with a shadow step */
    private void senseProjectiles() {
        for (Projectile p : this.level().getEntitiesOfClass(Projectile.class, this.getBoundingBox().inflate(5), pr -> pr.getOwner() != this && !(pr instanceof MarbleEntity))) {
            if (p.getOwner() != null && !canFight(p.getOwner())) continue;
            Vec3 v = p.getDeltaMovement();
            Vec3 to = position().add(0, 1.4, 0).subtract(p.position());
            if (v.lengthSqr() > 0.2 && v.normalize().dot(to.normalize()) > 0.92 && random.nextFloat() < 0.35F) {
                startAbility(DODGE, p.getOwner() instanceof LivingEntity l ? l : null);
                return;
            }
        }
    }

    // ------------------------------------------------------------------ client tick + fx
    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            prevRunBlend = runBlend;
            runBlend += ((isRunning() ? 1F : 0F) - runBlend) * 0.18F;
            prevSitBlend = sitBlend;
            boolean sit = isStaying() && getDeltaMovement().horizontalDistanceSqr() < 1.0E-4;
            sitBlend += ((sit ? 1F : 0F) - sitBlend) * 0.08F;
            if (this.isAlive()) clientFx();
        }
    }

    private void clientFx() {
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
        if (clientAnimId == RECALL) {
            for (int i = 0; i < 3; i++)
                l.addParticle(ParticleTypes.REVERSE_PORTAL, getX() + (random.nextDouble() - 0.5) * 3, getY() + random.nextDouble() * 2.5, getZ() + (random.nextDouble() - 0.5) * 3, 0, 0, 0);
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id >= EVENT_ANIM_BASE && id < EVENT_ANIM_BASE + 20) {
            this.clientAnimId = id - EVENT_ANIM_BASE;
            this.clientAnimStart = this.tickCount;
        } else {
            super.handleEntityEvent(id);
        }
    }

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

    /** approximate world position of the mouth */
    public Vec3 mouthPos() {
        return position().add(forward().scale(3.1)).add(0, 1.55, 0);
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

    private void shadowPuff(ServerLevel sl, Vec3 p) {
        sl.sendParticles(ParticleTypes.SQUID_INK, p.x, p.y + 1.2, p.z, 40, 1.0, 1.0, 1.0, 0.05);
        sl.sendParticles(dust(INK, 3.0F), p.x, p.y + 0.6, p.z, 30, 1.2, 0.6, 1.2, 0);
        sl.sendParticles(ParticleTypes.REVERSE_PORTAL, p.x, p.y + 1.2, p.z, 30, 0.8, 1.0, 0.8, 0.2);
    }

    public void hit(LivingEntity e, DamageSource src, float amount, double knock, double lift) {
        if (e.hurt(src, amount)) knock(e, knock, lift);
    }

    private void knock(LivingEntity e, double knock, double lift) {
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

    public double meleeReach() { return 3.2; }

    @Override protected SoundEvent getAmbientSound() { return ModSounds.AMBIENT.get(); }
    @Override protected SoundEvent getHurtSound(DamageSource src) { return ModSounds.HURT.get(); }
    @Override protected SoundEvent getDeathSound() { return null; }
    @Override protected float getSoundVolume() { return 2.0F; }
    @Override public int getAmbientSoundInterval() { return 160; }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(ModSounds.STEP.get(), 0.9F, 0.85F + random.nextFloat() * 0.25F);
    }

    // ------------------------------------------------------------------ goals
    /** freezes movement while an ability / entrance / recall plays, or while told to stay */
    static class HoldGoal extends Goal {
        private final KabkabaEntity k;

        HoldGoal(KabkabaEntity k) {
            this.k = k;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() { return k.isCasting() || k.isRecalling() || (k.isStaying() && k.getTarget() == null); }

        @Override
        public void start() { k.getNavigation().stop(); }
    }

    static class ChaseGoal extends Goal {
        private final KabkabaEntity k;
        private int repath;

        ChaseGoal(KabkabaEntity k) {
            this.k = k;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = k.getTarget();
            return t != null && t.isAlive() && !k.isCasting() && !k.isStaying();
        }

        @Override public boolean canContinueToUse() { return canUse(); }
        @Override public void start() { repath = 0; }
        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override
        public void stop() {
            k.getNavigation().stop();
            k.setRunning(false);
        }

        @Override
        public void tick() {
            LivingEntity t = k.getTarget();
            if (t == null) return;
            k.getLookControl().setLookAt(t, 30.0F, 30.0F);
            double d = k.distanceToSqr(t);
            boolean far = d > 9 * 9;
            k.setRunning(far && k.getNavigation().isInProgress());
            if (--repath <= 0) {
                repath = 4 + k.getRandom().nextInt(5);
                double reach = k.meleeReach() * 0.85;
                if (d > reach * reach) k.getNavigation().moveTo(t, far ? 1.5 : 1.0);
                else k.getNavigation().stop();
            }
        }
    }

    static class FollowOwnerGoal extends Goal {
        private final KabkabaEntity k;
        private int repath;

        FollowOwnerGoal(KabkabaEntity k) {
            this.k = k;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Player o = k.getOwner();
            return o != null && !k.isStaying() && k.getTarget() == null && !o.isSpectator() && k.distanceToSqr(o) > 10 * 10;
        }

        @Override
        public boolean canContinueToUse() {
            Player o = k.getOwner();
            return o != null && !k.isStaying() && k.getTarget() == null && k.distanceToSqr(o) > 5 * 5;
        }

        @Override
        public void stop() {
            k.getNavigation().stop();
            k.setRunning(false);
        }

        @Override
        public void tick() {
            Player o = k.getOwner();
            if (o == null) return;
            k.getLookControl().setLookAt(o, 10.0F, k.getMaxHeadXRot());
            double d = k.distanceToSqr(o);
            if (d > 30 * 30) {
                // too far: step through the shadows to the owner
                for (int i = 0; i < 10; i++) {
                    double a = k.getRandom().nextDouble() * Math.PI * 2;
                    if (k.randomTeleport(o.getX() + Math.cos(a) * 4, o.getY() + 1, o.getZ() + Math.sin(a) * 4, true)) break;
                }
                return;
            }
            k.setRunning(d > 16 * 16);
            if (--repath <= 0) {
                repath = 10;
                k.getNavigation().moveTo(o, d > 16 * 16 ? 1.5 : 1.1);
            }
        }
    }

    static class WildStrollGoal extends WaterAvoidingRandomStrollGoal {
        private final KabkabaEntity k;

        WildStrollGoal(KabkabaEntity k) {
            super(k, 0.7D);
            this.k = k;
        }

        @Override
        public boolean canUse() { return !k.isOwned() && super.canUse(); }
    }
}
