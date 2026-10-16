package com.krolasyon.beyblade.entity;

import com.krolasyon.beyblade.registry.ModEntities;
import com.krolasyon.beyblade.registry.ModItems;
import com.krolasyon.beyblade.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Flame Libra T125ES. A spinning top with "spin" as its health: hits, abilities and friction drain it and at zero it
 * topples. Owned beyblades are launched with the launcher and commanded with key bindings; rival beyblades (no owner)
 * fight every other beyblade on their own and use their abilities by themselves.
 */
public class BeybladeEntity extends Entity {
    public static final float WIDTH = 0.9F, HEIGHT = 0.66F, RADIUS = WIDTH / 2F;
    public static final float MAX_SPIN = 5000F;

    public static final int STATE_BATTLE = 0, STATE_DEFEATED = 1, STATE_RECALL = 2;

    public static final int AB_NONE = 0, AB_SONIC_WAVE = 1, AB_SHIELD = 2, AB_INFERNO = 3, AB_BUSTER = 4, AB_JUDGMENT = 5;
    public static final int ABILITY_COUNT = 6;
    /** how long the ability occupies the beyblade (shield is a timed buff instead) */
    public static final int[] DURATION = {0, 46, 0, 84, 62, 112};
    public static final int[] COOLDOWN = {0, 200, 500, 600, 700, 1200};
    public static final int SHIELD_TICKS = 170;

    public static final int[] WAVE_PULSES = {10, 20, 30};
    public static final int WAVE_LIFE = 16;
    public static final float WAVE_SPEED = 0.62F;
    public static final int INF_TORNADO = 24, INF_DIVE = 52;
    public static final double INF_RADIUS = 3.2, INF_HEIGHT = 9.0, INF_IMPACT_RADIUS = 5.5;
    public static final int BUS_CHARGE = 12, BUS_FIRE = 32, BUS_END = 52;
    public static final int JUD_LINK = 22, JUD_STRIKE = 86;
    public static final double JUD_RANGE = 14;

    /** battle modes (anime types): each changes damage dealt/taken, speed and spin decay */
    public static final int MODE_ATTACK = 0, MODE_DEFENSE = 1, MODE_STAMINA = 2, MODE_BALANCE = 3, MODE_COUNT = 4;
    public static final String[] MODE_IDS = {"attack", "defense", "stamina", "balance"};
    /** player orders (GUI): how the beyblade moves and fights */
    public static final int OR_AUTO = 0, OR_ATTACK = 1, OR_RETREAT = 2, OR_CIRCLE = 3, OR_CENTER = 4, OR_FOLLOW = 5, OR_GUARD = 6,
            OR_RUSH = 7, OR_DEFEND = 8, OR_ZIGZAG = 9, OR_BERSERK = 10, OR_CONSERVE = 11, OR_FLANK = 12, OR_UPPER = 13, ORDER_COUNT = 14;
    public static final String[] ORDER_IDS = {"auto", "attack", "retreat", "circle", "center", "follow", "guard", "rush", "defend",
            "zigzag", "berserk", "conserve", "flank", "upper"};
    public static final int MAX_LEVEL = 30;

    public static final byte EV_HIT = 60, EV_HEAVY_HIT = 61, EV_LAND = 62, EV_SHIELD_HIT = 108, EV_WALL = 64, EV_IMPACT = 65, EV_STRIKE = 66, EV_GRIND = 67, EV_SHOCK = 68, EV_SPIRIT = 69, EV_EVADE = 110;

    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER = SynchedEntityData.defineId(BeybladeEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Float> DATA_SPIN = SynchedEntityData.defineId(BeybladeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_MAX_SPIN = SynchedEntityData.defineId(BeybladeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_STATE = SynchedEntityData.defineId(BeybladeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ABILITY = SynchedEntityData.defineId(BeybladeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TARGET = SynchedEntityData.defineId(BeybladeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SHIELD_END = SynchedEntityData.defineId(BeybladeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_MODE = SynchedEntityData.defineId(BeybladeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ORDER = SynchedEntityData.defineId(BeybladeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LEVEL = SynchedEntityData.defineId(BeybladeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SPIRIT = SynchedEntityData.defineId(BeybladeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SPIRIT_END = SynchedEntityData.defineId(BeybladeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_XP = SynchedEntityData.defineId(BeybladeEntity.class, EntityDataSerializers.INT);
    /** the assembled parts ("wheel;track;tip;ring;bolt"), for the renderer */
    private static final EntityDataAccessor<String> DATA_PARTS = SynchedEntityData.defineId(BeybladeEntity.class, EntityDataSerializers.STRING);
    /** special move clash: the other beyblade's id (-1 = none) and this one's share of the total power (0..1) */
    private static final EntityDataAccessor<Integer> DATA_SCLASH = SynchedEntityData.defineId(BeybladeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SCLASH_RATIO = SynchedEntityData.defineId(BeybladeEntity.class, EntityDataSerializers.FLOAT);
    /** how unsteady the beyblade is after a sloppy launch (0 = steady .. 1 = barely up), settles over time */
    private static final EntityDataAccessor<Float> DATA_WOBBLE = SynchedEntityData.defineId(BeybladeEntity.class, EntityDataSerializers.FLOAT);
    @SuppressWarnings("unchecked")
    private static final EntityDataAccessor<Integer>[] DATA_COOLDOWN = new EntityDataAccessor[ABILITY_COUNT];

    static {
        for (int i = 0; i < ABILITY_COUNT; i++) DATA_COOLDOWN[i] = SynchedEntityData.defineId(BeybladeEntity.class, EntityDataSerializers.INT);
    }

    // ---------------- server state ----------------
    private ItemStack stored = ItemStack.EMPTY;
    protected int abilityTicks;
    protected double abilityBaseY, abilityBaseX, abilityBaseZ;
    protected boolean diving;
    private int dashCooldown = 20, dashTicks;
    /** ability waiting for contact: the beyblade charges into its target and the move starts on impact */
    private int pendingAbility, ramTicks;
    private int rushPhase, trainTicks;
    /** hit-and-run: back off after a clash before charging again; lock: two tops pressed together, grinding */
    private int retreatTicks, lockTicks;
    /** thrown out of a lock: flies freely (no steering) for a moment */
    private int flungTicks;
    @Nullable private BeybladeEntity lockPartner;
    /** who last hurt us and when an enemy special move last hit us (match scoring: ability finish) */
    @Nullable private BeybladeEntity lastAttacker;
    private long lastAttackTick = -1000, lastAbilityHitTick = -1000;
    private int lerpSteps;
    private double lerpX, lerpY, lerpZ;
    private int ownerMissing;
    private int stateTicks;
    private int targetRefresh;
    private int wallSoundCooldown, clashCooldown;
    @Nullable private StadiumEntity stadium;
    private boolean inBowl, contained;
    @Nullable protected Entity target;
    @Nullable private Vec3 home;
    /** test/display flag: keeps spinning in place (still collides and can be knocked around) */
    private boolean frozen;
    /** rivals pick abilities on their own unless disabled (visual self-test) */
    public static boolean rivalAbilities = true;
    @SuppressWarnings("unchecked")
    private final Set<Integer>[] waveHits = new Set[]{new HashSet<>(), new HashSet<>(), new HashSet<>()};

    // ---------------- client-side animation state (read by the renderer) ----------------
    public float spinAngle, spinAngleO, precess, precessO;
    public int clientAbility, clientAbilityStart = -1000, clientStateStart;
    public double abilityX, abilityY, abilityZ;
    public int grindTime = -1000, shockTime = -1000, spiritTime = -1000, evadeTime = -1000;
    public double shockDirX, shockDirZ, shockX, shockY, shockZ;
    public int hitTime = -1000, heavyHitTime = -1000, landTime = -1000, shieldHitTime = -1000, impactTime = -1000, strikeTime = -1000;
    public final double[] trailX = new double[8], trailY = new double[8], trailZ = new double[8];
    private boolean soundStarted, trailInit;

    public BeybladeEntity(EntityType<? extends BeybladeEntity> type, Level level) {
        super(type, level);
        this.setMaxUpStep(0.6F);
        this.blocksBuilding = true;
    }

    /** spawned by the launcher (owner != null) or the rival item (owner == null) */
    public static BeybladeEntity launch(Level level, @Nullable Player owner, ItemStack stack, Vec3 pos, Vec3 velocity, float power) {
        return launch(ModEntities.LIBRA.get(), level, owner, stack, pos, velocity, power);
    }

    public static BeybladeEntity launch(EntityType<? extends BeybladeEntity> type, Level level, @Nullable Player owner, ItemStack stack, Vec3 pos, Vec3 velocity, float power) {
        BeybladeEntity b = type.create(level);
        if (b == null) throw new IllegalStateException("cannot create beyblade");
        b.moveTo(pos.x, pos.y, pos.z, 0F, 0F);
        b.setDeltaMovement(velocity);
        if (owner != null) b.entityData.set(DATA_OWNER, Optional.of(owner.getUUID()));
        b.stored = stack.copy();
        b.initFromItem(stack);
        b.refreshLoadout();
        CompoundTag t = com.krolasyon.beyblade.compat.V.tag(stack);
        int lvl = t != null && t.contains("BeyLevel") ? Mth.clamp(t.getInt("BeyLevel"), 1, MAX_LEVEL) : 1;
        b.entityData.set(DATA_LEVEL, lvl);
        b.entityData.set(DATA_XP, t != null ? t.getInt("BeyXp") : 0);
        if (t != null && t.contains("BeyMode")) b.entityData.set(DATA_MODE, Mth.clamp(t.getInt("BeyMode"), 0, MODE_COUNT - 1));
        float max = maxSpinFor(lvl) * b.partsSpinMul();
        b.entityData.set(DATA_MAX_SPIN, max);
        b.entityData.set(DATA_SPIN, max * Mth.clamp(power, 0.35F, 1F));
        b.home = pos;
        level.addFreshEntity(b);
        return b;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_OWNER, Optional.empty());
        this.entityData.define(DATA_SPIN, MAX_SPIN);
        this.entityData.define(DATA_MAX_SPIN, MAX_SPIN);
        this.entityData.define(DATA_STATE, STATE_BATTLE);
        this.entityData.define(DATA_ABILITY, AB_NONE);
        this.entityData.define(DATA_TARGET, -1);
        this.entityData.define(DATA_SHIELD_END, 0);
        this.entityData.define(DATA_MODE, -1);
        this.entityData.define(DATA_ORDER, OR_AUTO);
        this.entityData.define(DATA_LEVEL, 1);
        this.entityData.define(DATA_XP, 0);
        this.entityData.define(DATA_PARTS, "");
        this.entityData.define(DATA_SCLASH, -1);
        this.entityData.define(DATA_SCLASH_RATIO, 0.5F);
        this.entityData.define(DATA_WOBBLE, 0F);
        this.entityData.define(DATA_SPIRIT, 0);
        this.entityData.define(DATA_SPIRIT_END, 0);
        for (EntityDataAccessor<Integer> a : DATA_COOLDOWN) this.entityData.define(a, 0);
    }

    // ---------------- accessors ----------------
    @Nullable public UUID getOwnerId() { return this.entityData.get(DATA_OWNER).orElse(null); }
    public boolean isRival() { return getOwnerId() == null; }
    @Nullable public Player getOwner() {
        UUID id = getOwnerId();
        return id == null ? null : this.level().getPlayerByUUID(id);
    }
    public float getSpin() { return this.entityData.get(DATA_SPIN); }
    public float getMaxSpin() { return this.entityData.get(DATA_MAX_SPIN); }
    public float spinRatio() { return Mth.clamp(getSpin() / Math.max(1F, getMaxSpin()), 0F, 1F); }
    private void setSpin(float v) { this.entityData.set(DATA_SPIN, Mth.clamp(v, 0F, getMaxSpin())); }
    public int getState() { return this.entityData.get(DATA_STATE); }
    private void setState(int s) { this.entityData.set(DATA_STATE, s); this.stateTicks = 0; }
    public int getAbility() { return this.entityData.get(DATA_ABILITY); }
    public int getTargetId() { return this.entityData.get(DATA_TARGET); }
    public boolean isShielded() { return this.level().getGameTime() < this.entityData.get(DATA_SHIELD_END); }
    public int shieldRemaining() { return (int) Math.max(0, this.entityData.get(DATA_SHIELD_END) - this.level().getGameTime()); }
    /** remaining cooldown ticks of ability 1..5 */
    public int cooldownRemaining(int ability) {
        return (int) Math.max(0, this.entityData.get(DATA_COOLDOWN[ability - 1]) - this.level().getGameTime());
    }

    // ---------------- modes / orders / level ----------------
    /** the anime type of this beyblade; it fights best in this mode */
    public int nativeMode() { return MODE_STAMINA; }
    public int getMode() { int m = this.entityData.get(DATA_MODE); return m < 0 ? nativeMode() : m; }
    public void setMode(int m) {
        this.entityData.set(DATA_MODE, Math.floorMod(m, MODE_COUNT));
        Player owner = getOwner();
        if (owner != null) owner.displayClientMessage(Component.translatable("msg.librabeyblade.mode", Component.translatable("mode.librabeyblade." + MODE_IDS[getMode()])), true);
        play(ModSounds.CHARGE.get(), 0.7F, 1.4F + getMode() * 0.1F);
    }
    public int getOrder() { return Mth.clamp(this.entityData.get(DATA_ORDER), 0, ORDER_COUNT - 1); }
    public void setOrder(int o) {
        this.entityData.set(DATA_ORDER, Mth.clamp(o, 0, ORDER_COUNT - 1));
        rushPhase = 0;
        Player owner = getOwner();
        if (owner != null) owner.displayClientMessage(Component.translatable("msg.librabeyblade.order", Component.translatable("order.librabeyblade." + ORDER_IDS[getOrder()])), true);
    }
    public int getLevel() { return this.entityData.get(DATA_LEVEL); }
    public int getXp() { return this.entityData.get(DATA_XP); }
    public static int xpNeeded(int level) { return 60 + 40 * level; }
    public static float maxSpinFor(int level) { return MAX_SPIN * (1F + 0.04F * (level - 1)); }
    public void setLevel(int lvl) {
        lvl = Mth.clamp(lvl, 1, MAX_LEVEL);
        this.entityData.set(DATA_LEVEL, lvl);
        float old = getMaxSpin();
        this.entityData.set(DATA_MAX_SPIN, maxSpinFor(lvl));
        setSpin(getSpin() + (getMaxSpin() - old));
    }

    /** experience from battles, matches and training; levels raise max spin, power and speed */
    public void addXp(int amount) {
        if (amount <= 0 || isRival() || getLevel() >= MAX_LEVEL) return;
        int xp = getXp() + amount, lvl = getLevel();
        boolean up = false;
        while (lvl < MAX_LEVEL && xp >= xpNeeded(lvl)) {
            xp -= xpNeeded(lvl);
            lvl++;
            up = true;
        }
        this.entityData.set(DATA_XP, lvl >= MAX_LEVEL ? 0 : xp);
        if (up) {
            setLevel(lvl);
            healSpin(getMaxSpin() * 0.15F);
            Player owner = getOwner();
            if (owner != null) {
                owner.displayClientMessage(Component.translatable("msg.librabeyblade.level_up", Component.translatable(titleKey()), lvl)
                        .withStyle(net.minecraft.ChatFormatting.GOLD), false);
                this.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(), net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.9F, 1.2F);
            }
        }
    }

    // ---------------- Metal Fight mechanics ----------------
    public static final int SPIRIT_MAX = 100, SPIRIT_TICKS = 180;
    public int getSpirit() { return this.entityData.get(DATA_SPIRIT); }
    public void addSpirit(int n) {
        if (getState() != STATE_BATTLE || isSpiritActive()) return;
        // the face bolt carries the bit-beast: rarer / upgraded bolts fire the Blader Spirit up faster
        com.krolasyon.beyblade.parts.Loadout l = parts();
        com.krolasyon.beyblade.parts.BeyPart bolt = l.get(com.krolasyon.beyblade.parts.PartType.BOLT);
        if (bolt != null && n > 0) n = Math.round(n * (1F + 0.08F * bolt.rarity() + 0.06F * l.level(com.krolasyon.beyblade.parts.PartType.BOLT)));
        this.entityData.set(DATA_SPIRIT, Mth.clamp(getSpirit() + n, 0, SPIRIT_MAX));
    }
    /** Blader Spirit burst (anime "Get up!"): more power and speed, spin recovers, moves recharge faster */
    public boolean isSpiritActive() { return this.level().getGameTime() < this.entityData.get(DATA_SPIRIT_END); }
    public int spiritRemaining() { return (int) Math.max(0, this.entityData.get(DATA_SPIRIT_END) - this.level().getGameTime()); }
    public void releaseSpirit() {
        Player owner = getOwner();
        if (getState() != STATE_BATTLE) return;
        if (getSpirit() < SPIRIT_MAX) {
            if (owner != null) owner.displayClientMessage(Component.translatable("msg.librabeyblade.spirit_low", getSpirit()), true);
            return;
        }
        this.entityData.set(DATA_SPIRIT, 0);
        this.entityData.set(DATA_SPIRIT_END, (int) (this.level().getGameTime() + SPIRIT_TICKS));
        healSpin(getMaxSpin() * 0.15F);
        long now = this.level().getGameTime();
        for (int i = 0; i < ABILITY_COUNT; i++) {
            int end = this.entityData.get(DATA_COOLDOWN[i]);
            if (end > now) this.entityData.set(DATA_COOLDOWN[i], (int) (now + (end - now) / 2));
        }
        play(ModSounds.SHIELD_UP.get(), 2.0F, 0.7F);
        play(ModSounds.CLASH_HEAVY.get(), 1.5F, 0.6F);
        this.level().broadcastEntityEvent(this, EV_SPIRIT);
        if (owner != null) owner.displayClientMessage(Component.translatable("msg.librabeyblade.spirit", Component.translatable(titleKey()))
                .withStyle(net.minecraft.ChatFormatting.GOLD, net.minecraft.ChatFormatting.BOLD), true);
    }
    /** Final Drive (Metal Fury): attack types surge forward when their spin runs low */
    public boolean isFinalDrive() { return getMode() == MODE_ATTACK && spinRatio() < 0.35F && getState() == STATE_BATTLE; }
    /** Life After Death: stamina types keep wobbling on far longer at the very end of their spin */
    public boolean isLifeAfterDeath() { return getMode() == MODE_STAMINA && spinRatio() < 0.12F && getState() == STATE_BATTLE; }

    private boolean nativeBonus() { return getMode() == nativeMode(); }
    /** multiplier on spin damage this beyblade deals */
    public float powerMul() {
        float m = switch (getMode()) { case MODE_ATTACK -> 1.3F; case MODE_DEFENSE -> 0.85F; case MODE_STAMINA -> 0.8F; default -> 1.05F; };
        if (nativeBonus()) m *= 1.1F;
        if (getOrder() == OR_BERSERK) m *= 1.15F;
        if (getOrder() == OR_UPPER) m *= 0.9F;
        if (isSpiritActive()) m *= 1.3F;
        if (isFinalDrive()) m *= 1.15F;
        return m * (1F + 0.03F * (getLevel() - 1)) * com.krolasyon.beyblade.parts.Loadout.mul(parts().attack());
    }
    /** multiplier on spin damage taken */
    public float guardMul() {
        float m = switch (getMode()) { case MODE_ATTACK -> 1.15F; case MODE_DEFENSE -> 0.65F; case MODE_STAMINA -> 0.95F; default -> 0.9F; };
        if (nativeBonus()) m *= 0.92F;
        if (getOrder() == OR_DEFEND) m *= 0.7F;
        if (getOrder() == OR_BERSERK) m *= 1.15F;
        com.krolasyon.beyblade.parts.Loadout l = parts();
        if (l.trackTrait() == com.krolasyon.beyblade.parts.BeyPart.Trait.DEFENSE_RING) m *= 0.92F;
        return m / com.krolasyon.beyblade.parts.Loadout.mul(l.defense());
    }
    public double modeSpeed() {
        double m = switch (getMode()) { case MODE_ATTACK -> 1.25; case MODE_DEFENSE -> 0.85; case MODE_STAMINA -> 0.9; default -> 1.05; };
        if (isSpiritActive()) m *= 1.2;
        if (isFinalDrive()) m *= 1.25;
        com.krolasyon.beyblade.parts.Loadout l = parts();
        // Final Drive tips switch to a flat, faster mode when the spin runs low
        if (l.tip() == com.krolasyon.beyblade.parts.BeyPart.Tip.FINAL_DRIVE && spinRatio() < 0.35F) m *= 1.2;
        return m * (1.0 + 0.008 * (getLevel() - 1)) * com.krolasyon.beyblade.parts.Loadout.mul(l.speed());
    }
    public float modeDecay() {
        float m = switch (getMode()) { case MODE_ATTACK -> 1.25F; case MODE_DEFENSE -> 0.95F; case MODE_STAMINA -> 0.55F; default -> 0.85F; };
        if (getMode() == MODE_STAMINA && nativeBonus()) m *= 0.85F;
        if (getOrder() == OR_CONSERVE) m *= 0.6F;
        if (getOrder() == OR_BERSERK) m *= 1.3F;
        if (isLifeAfterDeath()) m *= 0.4F;
        com.krolasyon.beyblade.parts.Loadout l = parts();
        com.krolasyon.beyblade.parts.BeyPart.Trait tr = l.trackTrait();
        if (tr == com.krolasyon.beyblade.parts.BeyPart.Trait.WING) m *= 0.92F;
        if (tr == com.krolasyon.beyblade.parts.BeyPart.Trait.TORNADO) m *= 0.9F;
        if (l.tip() == com.krolasyon.beyblade.parts.BeyPart.Tip.FINAL_SURVIVE && spinRatio() < 0.3F) m *= 0.6F;
        return m / com.krolasyon.beyblade.parts.Loadout.mul(l.stamina());
    }
    /** how much of a collision knock this beyblade takes */
    public double knockMul() {
        double m = switch (getMode()) { case MODE_ATTACK -> 1.1; case MODE_DEFENSE -> 0.45; case MODE_STAMINA -> 1.0; default -> 0.8; };
        m *= 1 + 0.6 * getWobble();
        com.krolasyon.beyblade.parts.Loadout l = parts();
        // heavier combos are pushed around less; some tracks hold the ground
        m *= Mth.clamp(46.0 / Math.max(20F, l.weight()), 0.72, 1.35);
        switch (l.trackTrait()) {
            case BOOST_DISK -> m *= 0.8;
            case RUBBER_RING -> m *= 0.85;
            case GRAVITY -> m *= 1.15;
            default -> {}
        }
        return getOrder() == OR_DEFEND ? m * 0.5 : m;
    }
    public boolean isRamming() { return pendingAbility != 0; }
    /** true when the last enemy special move hit us within the last 3 seconds (ability finish) */
    public boolean finishedByAbility() { return this.level().getGameTime() - lastAbilityHitTick < 60; }
    @Nullable public BeybladeEntity lastAttacker() { return this.level().getGameTime() - lastAttackTick < 200 ? lastAttacker : null; }

    public boolean isEnemy(BeybladeEntity o) {
        if (o == this || !o.isAlive() || o.getState() != STATE_BATTLE) return false;
        UUID a = getOwnerId();
        return a == null || !a.equals(o.getOwnerId());
    }

    /** living entities our abilities may hurt: the current target and hostile monsters. Never players or our owner. */
    public boolean isHostileLiving(LivingEntity e) {
        if (!e.isAlive() || e instanceof Player || isRival()) return false;
        return e == target || e instanceof Enemy;
    }

    @Nullable
    public static BeybladeEntity findActive(Player player) {
        List<BeybladeEntity> list = player.level().getEntitiesOfClass(BeybladeEntity.class, player.getBoundingBox().inflate(96),
                b -> b.isAlive() && player.getUUID().equals(b.getOwnerId()));
        return list.isEmpty() ? null : list.get(0);
    }

    // ---------------- per-beyblade hooks (Flame Libra defaults; see LDragoEntity) ----------------
    /** +1 = right spin (clockwise from above), -1 = left spin */
    public int spinDir() { return 1; }
    public String kindId() { return "libra"; }
    /** translation key of this beyblade's display name */
    public String titleKey() { return "hud.librabeyblade.title." + kindId(); }
    public String abilityKey(int id) { return "ability.librabeyblade." + id; }
    public int duration(int id) { return DURATION[id]; }
    public int cooldown(int id) { return COOLDOWN[id]; }
    /** the ability that is a timed buff instead of an action (Libra: Sonic Shield) */
    public int buffAbility() { return AB_SHIELD; }
    public int buffTicks() { return SHIELD_TICKS; }
    public boolean requiresTarget(int id) { return id == AB_BUSTER; }
    /** buff reflects collision damage (Sonic Shield) */
    public boolean buffReflects() { return true; }
    public float buffAttackMul() { return 1F; }
    public double buffSpeedMul() { return 1.0; }
    protected void onBuffStart(@Nullable Player owner) { play(ModSounds.SHIELD_UP.get(), 1.4F, 1.0F); }
    protected void tickBuff() { tickShield(); }
    protected ItemStack defaultItem() { return new ItemStack(ModItems.LIBRA.get()); }
    protected float clientSpinSpeed(int ab, float at, float speed) { return ab == AB_INFERNO && at < INF_TORNADO ? 96F : speed; }
    /** chance to evade a collision completely (Earth Eagle's Eagle Eye); return true if this hit was dodged */
    public boolean tryDodge(BeybladeEntity attacker) {
        float c = dodgeChance();
        if (c <= 0F || getState() != STATE_BATTLE || this.random.nextFloat() >= c) return false;
        Vec3 away = flat(position().subtract(attacker.position()));
        away = away.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : away.normalize();
        Vec3 side = new Vec3(-away.z, 0, away.x).scale(this.random.nextBoolean() ? 1 : -1);
        Vec3 jump = side.scale(0.9).add(away.scale(0.35));
        if (this.level().noCollision(this, getBoundingBox().move(jump))) setPos(getX() + jump.x, getY(), getZ() + jump.z);
        setDeltaMovement(getDeltaMovement().add(side.scale(0.35)));
        this.level().broadcastEntityEvent(this, EV_EVADE);
        play(ModSounds.CHARGE.get(), 0.9F, 1.9F);
        onDodge(attacker);
        Player owner = getOwner();
        if (owner != null) owner.displayClientMessage(Component.translatable("dodge.librabeyblade." + kindId()).withStyle(net.minecraft.ChatFormatting.AQUA), true);
        return true;
    }

    /** chance to slip out of a collision completely with this beyblade's own evasion move */
    protected float dodgeChance() { return isBuffActive() ? 0.22F : 0.1F; }

    protected void onDodge(BeybladeEntity attacker) {}

    /** multiplier on all spin damage taken (defense types take less) */
    public float incomingMul() { return 1F; }

    /** server: seconds of helpless wobbling (no steering), e.g. from Rock Leone's quake */
    protected int stunTicks;

    public void stun(int ticks) { stunTicks = Math.max(stunTicks, ticks); }

    /** true while the buff is running (alias of isShielded for non-Libra beyblades) */
    public boolean isBuffActive() { return isShielded(); }

    // ---------------- ticking ----------------
    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            clientTick();
            return;
        }
        serverTick();
    }

    private void serverTick() {
        stateTicks++;
        if (wallSoundCooldown > 0) wallSoundCooldown--;
        if (clashCooldown > 0) clashCooldown--;
        if (home == null) home = position();
        Player owner = getOwner();
        if (getOwnerId() != null) {
            if (owner == null || !owner.isAlive()) {
                if (++ownerMissing > 100) {
                    dropStored();
                    discard();
                    return;
                }
            } else {
                ownerMissing = 0;
                if (getState() != STATE_RECALL && owner.distanceToSqr(this) > 48 * 48) recall();
            }
        }
        if (getY() < this.level().getMinBuildHeight() - 8) {
            if (owner != null) giveTo(owner); else dropStored();
            discard();
            return;
        }
        switch (getState()) {
            case STATE_RECALL -> tickRecall(owner);
            case STATE_DEFEATED -> tickDefeated(owner);
            default -> tickBattle(owner);
        }
    }

    private void tickBattle(@Nullable Player owner) {
        float wobble = getWobble();
        // an unsteady top scrubs off spin much faster until it settles
        float decay = 0.2F * modeDecay() * (isInWater() ? 4F : 1F) * (isShielded() ? 1.15F : 1F) * (1F + 1.8F * wobble);
        if (wobble > 0F) tickWobble(owner, wobble);
        // training: every 5 seconds spent spinning gives experience
        if (++trainTicks >= 100) {
            trainTicks = 0;
            if (!isRival()) addXp(target != null ? 2 : 1);
            // the spirit rises when things look bad, and slowly in any battle
            addSpirit(spinRatio() < 0.3F ? 8 : (target != null ? 3 : 1));
        }
        // rivals and berserkers let their spirit loose as soon as it is full
        if ((isRival() || getOrder() == OR_BERSERK) && getSpirit() >= SPIRIT_MAX && target != null) releaseSpirit();
        setSpin(getSpin() - decay);
        // a last stand (L-Drago's stolen power) can pull it back from the brink, once
        if (getSpin() <= getMaxSpin() * 0.02F) lastStand();
        if (getSpin() <= 0F) {
            defeat();
            return;
        }
        int ab = getAbility();
        if (ab == AB_NONE && --targetRefresh <= 0) {
            targetRefresh = 10;
            target = findTarget(owner);
        }
        if (target != null && (!target.isAlive() || target.distanceToSqr(this) > 32 * 32
                || (target instanceof BeybladeEntity b && !isEnemy(b)))) {
            target = null;
        }
        int tid = target == null ? -1 : target.getId();
        if (tid != getTargetId()) this.entityData.set(DATA_TARGET, tid);

        boolean controlled = false;
        if (sclashCooldown > 0) sclashCooldown--;
        if (sclashTicks > 0 || startSpecialClash()) {
            // two special moves met head on: everything waits until the clash is decided
            controlled = true;
            tickSpecialClash();
        } else if (pendingAbility != 0) {
            controlled = tickRam(owner);
        } else if (ab != AB_NONE) {
            abilityTicks++;
            controlled = tickAbility(ab, abilityTicks, owner);
            if (abilityTicks >= duration(ab)) endAbility();
        } else if (isRival() && rivalAbilities) {
            rivalUseAbilities();
        } else if (getOrder() == OR_BERSERK && target != null && this.tickCount % 25 == 0) {
            for (int k = 0; k < 3; k++) {
                int id = 1 + this.random.nextInt(abilityCount());
                if (cooldownRemaining(id) == 0 && abilityAvailable(id) && tryAbility(id, false)) break;
            }
        }
        if (isShielded()) tickBuff();
        if (stunTicks > 0) {
            stunTicks--;
            controlled = true;
            setDeltaMovement(getDeltaMovement().multiply(0.6, 1, 0.6));
        }
        if (flungTicks > 0) {
            flungTicks--;
            controlled = true;
        }
        if (!controlled && !frozen) steer(owner);
        if (frozen && onGround()) setDeltaMovement(getDeltaMovement().multiply(0.7, 1, 0.7));
        physics();
        stadiumPhysics();
        collideBeyblades();
        collideLiving(owner);
    }

    @Nullable
    protected Entity findTarget(@Nullable Player owner) {
        BeybladeEntity best = null;
        double bestD = 24 * 24;
        for (BeybladeEntity b : this.level().getEntitiesOfClass(BeybladeEntity.class, getBoundingBox().inflate(24), this::isEnemy)) {
            double d = b.distanceToSqr(this);
            if (d < bestD) { bestD = d; best = b; }
        }
        if (best != null) return best;
        if (owner != null) {
            LivingEntity m = owner.getLastHurtMob();
            if (m != null && m.isAlive() && owner.tickCount - owner.getLastHurtMobTimestamp() < 300 && m.distanceToSqr(this) < 24 * 24 && !(m instanceof Player)) return m;
            LivingEntity by = owner.getLastHurtByMob();
            if (by != null && by.isAlive() && !(by instanceof Player) && by.distanceToSqr(this) < 24 * 24) return by;
        }
        return null;
    }

    private void steer(@Nullable Player owner) {
        Vec3 v = getDeltaMovement();
        double ratio = spinRatio();
        double mul = (0.55 + 0.45 * ratio) * (onGround() ? 1.0 : 0.25) * buffSpeedMul() * modeSpeed();
        boolean arena = stadium != null && inBowl;
        Vec3 base = arena ? stadium.position() : (owner != null ? owner.position() : (home != null ? home : position()));
        int order = getOrder();
        Entity tg = target;
        if (order == OR_FOLLOW) tg = null;
        if (order == OR_GUARD && tg != null && owner != null && tg.distanceToSqr(owner) > 7 * 7) tg = null;
        int mode = getMode();
        if (order == OR_DEFEND) {
            setDeltaMovement(v.multiply(0.86, 1, 0.86));
            return;
        }
        if (lockTicks > 0 && lockPartner != null && lockPartner.isAlive() && lockPartner.getState() == STATE_BATTLE && lockPartner.distanceToSqr(this) < 4) {
            // locked together: keep pressing in, sliding around the contact point, sparks flying
            lockTicks--;
            Vec3 to = flat(lockPartner.position().subtract(position()));
            Vec3 dir = to.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : to.normalize();
            Vec3 desired = dir.scale(0.22).add(new Vec3(-dir.z, 0, dir.x).scale(0.1 * spinDir()));
            setDeltaMovement(v.add((desired.x - v.x) * 0.4, 0, (desired.z - v.z) * 0.4));
            clashCooldown = Math.max(clashCooldown, 2);
            if (this.tickCount % 2 == 0) this.level().broadcastEntityEvent(this, EV_GRIND);
            // the lock breaks: whoever pushes harder throws the other one across the stadium
            if (lockTicks == 0) resolveLock(lockPartner);
            return;
        }
        lockTicks = 0;
        if (retreatTicks > 0 && tg != null) {
            retreatTicks--;
            Vec3 to = flat(tg.position().subtract(position()));
            Vec3 dir = to.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : to.normalize();
            Vec3 desired = dir.scale(-0.42 * mul).add(new Vec3(dir.z, 0, -dir.x).scale(0.12 * spinDir()));
            setDeltaMovement(v.add((desired.x - v.x) * 0.3, 0, (desired.z - v.z) * 0.3));
            if (retreatTicks == 0) dashCooldown = 0;
            return;
        }
        if (tg == null || order == OR_CIRCLE || order == OR_CENTER || order == OR_CONSERVE) {
            Vec3 c = order == OR_FOLLOW || order == OR_GUARD ? (owner != null ? owner.position() : base) : base;
            double r, tan;
            switch (order) {
                case OR_CIRCLE -> { r = arena ? StadiumEntity.BOWL_R * 0.8 : 5.0; tan = 0.95; }
                case OR_CENTER -> { r = 0.25; tan = 0.04; }
                case OR_CONSERVE -> { r = arena ? 1.2 : 1.5; tan = 0.1; }
                case OR_FOLLOW -> { r = 1.8; tan = 0.18; }
                case OR_GUARD -> { r = 2.2; tan = 0.22; }
                default -> { r = arena ? 3.4 : 2.8; tan = 0.25; }
            }
            setDeltaMovement(orbit(v, c, r, tan, order == OR_CIRCLE ? Math.max(mul, 0.9 * modeSpeed()) : mul, order == OR_CIRCLE ? 0.3 : 0.12));
            return;
        }
        Vec3 to = flat(tg.position().subtract(position()));
        double dist = to.length();
        Vec3 dir = dist > 1.0E-3 ? to.scale(1 / dist) : new Vec3(1, 0, 0);
        switch (order) {
            case OR_RETREAT -> {
                Vec3 away = dir.scale(-0.3).add(new Vec3(dir.z, 0, -dir.x).scale(0.14));
                if (arena) {
                    Vec3 fromC = flat(position().subtract(base));
                    if (fromC.length() > StadiumEntity.BOWL_R * 0.8) away = away.add(fromC.normalize().scale(-0.25));
                }
                Vec3 d = away.scale(mul);
                setDeltaMovement(v.add((d.x - v.x) * 0.14, 0, (d.z - v.z) * 0.14));
                return;
            }
            case OR_RUSH -> {
                // back off, then charge straight through the target
                if (rushPhase > 0) {
                    rushPhase--;
                    Vec3 d = dir.scale(-0.3 * mul);
                    setDeltaMovement(v.add((d.x - v.x) * 0.2, 0, (d.z - v.z) * 0.2));
                    if (rushPhase == 0 && onGround()) {
                        setDeltaMovement(getDeltaMovement().add(dir.scale(0.85 * (0.6 + 0.4 * ratio) * buffSpeedMul())));
                        dashTicks = 12;
                    }
                    return;
                }
                if (dashTicks > 0) { dashTicks--; return; }
                if (dist < 1.4) rushPhase = 12;
                else setDeltaMovement(orbit(v, tg.position(), 0.2, 0.05, mul * 1.2, 0.15));
                return;
            }
            case OR_ZIGZAG -> {
                Vec3 side = new Vec3(dir.z, 0, -dir.x).scale(Math.sin(this.tickCount * 0.38) * 0.42);
                Vec3 d = dir.scale(0.3).add(side).scale(mul);
                setDeltaMovement(v.add((d.x - v.x) * 0.2, 0, (d.z - v.z) * 0.2));
                return;
            }
            case OR_FLANK -> {
                Vec3 tv = flat(tg.getDeltaMovement());
                boolean behind = tv.lengthSqr() > 1.0E-3 && tv.normalize().dot(dir) > 0.3;
                if (behind && --dashCooldown <= 0 && onGround()) {
                    setDeltaMovement(v.add(dir.scale(0.75 * (0.6 + 0.4 * ratio))));
                    dashTicks = 8;
                    dashCooldown = 14;
                    return;
                }
                if (dashTicks > 0) { dashTicks--; return; }
                setDeltaMovement(orbit(v, tg.position(), 2.0, 0.4, mul, 0.15));
                return;
            }
            default -> {}
        }
        // AUTO / ATTACK / BERSERK / UPPER / GUARD: close in, grind against the target and dash into it
        // they keep coming back at each other: short pauses between dashes
        double r = 0.3;
        int dashMin = 6, dashRand = 8;
        double dashPow = 0.58;
        if (order == OR_ATTACK || order == OR_BERSERK || order == OR_UPPER) { r = 0.1; dashMin = 3; dashRand = 5; dashPow = 0.68; }
        else if (mode == MODE_ATTACK) { r = 0.15; dashMin = 4; dashRand = 6; dashPow = 0.66; }
        else if (mode == MODE_DEFENSE) { r = 0.6; dashMin = 10; dashRand = 10; dashPow = 0.5; }
        else if (mode == MODE_STAMINA) { r = 0.8; dashMin = 12; dashRand = 12; dashPow = 0.46; }
        if (--dashCooldown <= 0 && onGround()) {
            setDeltaMovement(v.add(dir.scale(dashPow * (0.6 + 0.4 * ratio) * buffSpeedMul())));
            dashTicks = 5;
            dashCooldown = (int) ((dashMin + this.random.nextInt(dashRand)) / buffSpeedMul());
            return;
        }
        if (dashTicks > 0) { dashTicks--; return; }
        setDeltaMovement(orbit(v, tg.position(), r, 0.22, mul, 0.14));
    }

    /** steer toward a circle of radius r around c, moving along it with the given tangential speed */
    private Vec3 orbit(Vec3 v, Vec3 c, double r, double tan, double mul, double gain) {
        Vec3 to = flat(c.subtract(position()));
        double dist = to.length();
        if (dist < 1.0E-3) return v;
        Vec3 dir = to.scale(1.0 / dist);
        Vec3 tangent = new Vec3(dir.z, 0, -dir.x).scale(spinDir());
        double radial = Mth.clamp((dist - r) * 0.12, -0.18, dist > 12 ? 0.55 : 0.32);
        Vec3 desired = tangent.scale(tan).add(dir.scale(radial)).scale(mul);
        return v.add((desired.x - v.x) * gain, 0, (desired.z - v.z) * gain);
    }

    /** charge into the target; the pending special move starts the moment they touch */
    private boolean tickRam(@Nullable Player owner) {
        ramTicks++;
        Entity tg = target;
        int id = pendingAbility;
        if (tg == null || !tg.isAlive() || ramTicks > 50 || (tg instanceof BeybladeEntity b && b.getState() != STATE_BATTLE)) {
            pendingAbility = 0;
            beginAbility(id, owner);
            return true;
        }
        Vec3 to = flat(tg.position().subtract(position()));
        double dist = to.length();
        if (dist <= RADIUS * 2 + 0.3 && Math.abs(tg.getY() - getY()) < 1.2) {
            pendingAbility = 0;
            ramImpact(tg);
            beginAbility(id, owner);
            return true;
        }
        Vec3 dir = to.scale(1 / Math.max(1.0E-3, dist));
        double sp = 0.62 * (0.7 + 0.3 * spinRatio()) * modeSpeed();
        Vec3 v = getDeltaMovement();
        setDeltaMovement(v.x + (dir.x * sp - v.x) * 0.45, v.y, v.z + (dir.z * sp - v.z) * 0.45);
        return true;
    }

    private void ramImpact(Entity tg) {
        Vec3 dir = flat(tg.position().subtract(position()));
        dir = dir.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : dir.normalize();
        if (tg instanceof BeybladeEntity b) {
            if (!b.tryDodge(this)) b.damageSpin(45F * powerMul(), this);
            b.setDeltaMovement(b.getDeltaMovement().add(dir.scale(0.45 * b.knockMul())).add(0, 0.08, 0));
            this.level().broadcastEntityEvent(b, EV_HEAVY_HIT);
        } else if (tg instanceof LivingEntity l && isHostileLiving(l)) {
            l.hurt(damageSources().thrown(this, getOwner()), 6F);
            l.knockback(0.8, -dir.x, -dir.z);
        }
        setDeltaMovement(getDeltaMovement().scale(0.2));
        this.level().broadcastEntityEvent(this, EV_HEAVY_HIT);
        this.level().playSound(null, getX(), getY(), getZ(), ModSounds.CLASH_HEAVY.get(), SoundSource.NEUTRAL, 1.4F, 0.8F + this.random.nextFloat() * 0.2F);
    }

    protected static Vec3 flat(Vec3 v) { return new Vec3(v.x, 0, v.z); }

    private void physics() {
        Vec3 v = getDeltaMovement();
        if (!isNoGravity()) v = v.add(0, -0.06, 0);
        Vec3 before = v;
        Vec3 p0 = position();
        boolean wasGround = onGround();
        setDeltaMovement(v);
        move(MoverType.SELF, v);
        Vec3 moved = position().subtract(p0);
        v = getDeltaMovement();
        if (horizontalCollision) {
            boolean hx = Math.abs(moved.x - before.x) > 1.0E-4, hz = Math.abs(moved.z - before.z) > 1.0E-4;
            v = new Vec3(hx ? -before.x * 0.75 : v.x, v.y, hz ? -before.z * 0.75 : v.z);
            double speed = flat(before).length();
            if (speed > 0.14 && wallSoundCooldown == 0) {
                wallSoundCooldown = 6;
                play(ModSounds.WALL_HIT.get(), 0.7F, 0.9F + this.random.nextFloat() * 0.3F);
                Px.send((ServerLevel) this.level(), ParticleTypes.ELECTRIC_SPARK, getX() + before.x * 3, getY() + 0.3, getZ() + before.z * 3, 8, 0.1, 0.1, 0.1, 0.3);
                setSpin(getSpin() - 4F);
                this.level().broadcastEntityEvent(this, EV_WALL);
            }
        }
        if (onGround() && !wasGround && before.y < -0.32) {
            v = new Vec3(v.x, -before.y * 0.3, v.z);
            play(ModSounds.LAND.get(), 0.8F, 1.0F);
            this.level().broadcastEntityEvent(this, EV_LAND);
        }
        if (onGround()) v = new Vec3(v.x * 0.955, v.y, v.z * 0.955);
        else v = new Vec3(v.x * 0.99, v.y * 0.98, v.z * 0.99);
        setDeltaMovement(v);
    }

    // ---------------- collisions ----------------
    private void collideBeyblades() {
        for (BeybladeEntity o : this.level().getEntitiesOfClass(BeybladeEntity.class, getBoundingBox().inflate(0.5),
                b -> b != this && b.getState() != STATE_RECALL)) {
            // each pair is resolved exactly once per tick, by the lower id (or by us when the other one doesn't run collisions)
            if (o.getId() < this.getId() && o.getState() == STATE_BATTLE) continue;
            resolve(o);
        }
    }

    private void resolve(BeybladeEntity o) {
        double dx = o.getX() - getX(), dz = o.getZ() - getZ();
        if (Math.abs(o.getY() - getY()) > HEIGHT) return;
        double dist = Math.sqrt(dx * dx + dz * dz);
        double min = RADIUS * 2;
        if (dist >= min) return;
        if (dist < 1.0E-4) {
            dx = this.random.nextDouble() - 0.5;
            dz = this.random.nextDouble() - 0.5;
            dist = Math.sqrt(dx * dx + dz * dz);
        }
        double nx = dx / dist, nz = dz / dist;
        double overlap = min - dist + 0.01;
        // push them fully apart so the models never intersect
        shift(this, -nx * overlap * 0.5, -nz * overlap * 0.5);
        shift(o, nx * overlap * 0.5, nz * overlap * 0.5);
        double rx = o.getX() - getX(), rz = o.getZ() - getZ();
        if (rx * rx + rz * rz < (min - 0.02) * (min - 0.02)) {
            // one of them is against a wall: move the other the whole way
            double d2 = Math.sqrt(rx * rx + rz * rz);
            double rest = min - d2 + 0.01;
            shift(o, nx * rest, nz * rest);
            shift(this, -nx * rest, -nz * rest);
        }
        Vec3 va = getDeltaMovement(), vb = o.getDeltaMovement();
        double rel = (vb.x - va.x) * nx + (vb.z - va.z) * nz;
        boolean fight = isEnemy(o) && getState() == STATE_BATTLE && o.getState() == STATE_BATTLE;
        boolean opposite = spinDir() != o.spinDir();
        if (fight && (clashCooldown > 0 || o.clashCooldown > 0) && rel >= -0.05) {
            // still pressed together: the rims grind against each other
            o.damageSpin(0.7F * powerMul(), this);
            damageSpin(0.7F * o.powerMul(), o);
            double tx = -nz, tz = nx, s = opposite ? 0.0 : 0.012 * spinDir();
            setDeltaMovement(getDeltaMovement().add(-nx * 0.012 + tx * s, 0, -nz * 0.012 + tz * s));
            o.setDeltaMovement(o.getDeltaMovement().add(nx * 0.012 - tx * s, 0, nz * 0.012 - tz * s));
            if (this.tickCount % 5 == 0) {
                this.level().playSound(null, getX() + nx * RADIUS, getY() + 0.3, getZ() + nz * RADIUS, ModSounds.GRIND.get(), SoundSource.NEUTRAL,
                        0.55F, 0.9F + this.random.nextFloat() * 0.25F);
                this.level().broadcastEntityEvent(this, EV_GRIND);
            }
            return;
        }
        if (rel >= 0 && !fight) return;
        if (rel > -0.02 && fight) rel = -0.02;
        // fairly soft rubber-on-metal contact: they hit, stay close and fight it out instead of flying apart
        double e = fight ? 0.55 : 0.3;
        double j = -(1 + e) * rel * 0.5;
        double kick = fight ? (opposite ? 0.035 : 0.06) : 0.0;
        // same-direction tops deflect sideways at contact; opposite spins grind into each other (spin steal)
        double tx = -nz, tz = nx, side = fight && !opposite ? 0.05 * spinDir() : 0.0;
        double ka = knockMul(), kb = o.knockMul();
        va = va.add((-nx * (j + kick) + tx * side) * ka, fight ? 0.02 : 0, (-nz * (j + kick) + tz * side) * ka);
        vb = vb.add((nx * (j + kick) - tx * side) * kb, fight ? 0.02 : 0, (nz * (j + kick) - tz * side) * kb);
        if (fight && getOrder() == OR_UPPER) vb = vb.add(0, 0.34, 0);
        if (fight && o.getOrder() == OR_UPPER) va = va.add(0, 0.34, 0);
        setDeltaMovement(va);
        o.setDeltaMovement(vb);
        if (!fight || clashCooldown > 0 || o.clashCooldown > 0) return;
        clashCooldown = 4;
        o.clashCooldown = 4;
        double impact = -rel;
        boolean heavy = impact > 0.42;
        float base = (float) (10 + 150 * impact);
        float toO = base * (0.6F + 0.6F * spinRatio()) * buffAttackMul() * powerMul() * partsClashMul(o);
        float toMe = base * (0.6F + 0.6F * o.spinRatio()) * o.buffAttackMul() * o.powerMul() * o.partsClashMul(this);
        // a taller spin track strikes the other one from above (upper attack)
        if (heightEdge(o) && partsUpperKick()) o.setDeltaMovement(o.getDeltaMovement().add(0, 0.22, 0));
        if (o.heightEdge(this) && o.partsUpperKick()) setDeltaMovement(getDeltaMovement().add(0, 0.22, 0));
        if (toO >= 15F) addXp(1);
        if (toMe >= 15F) o.addXp(1);
        if (o.tryDodge(this)) toO = 0F;
        if (tryDodge(o)) toMe = 0F;
        if (o.isShielded() && o.buffReflects()) {
            toMe += toO * 0.6F;
            toO *= 0.2F;
            setDeltaMovement(getDeltaMovement().add(-nx * 0.35, 0.08, -nz * 0.35));
            o.shieldHit();
        }
        if (isShielded() && buffReflects()) {
            toO += toMe * 0.6F;
            toMe *= 0.2F;
            o.setDeltaMovement(o.getDeltaMovement().add(nx * 0.35, 0.08, nz * 0.35));
            shieldHit();
        }
        o.damageSpin(toO, this);
        damageSpin(toMe, o);
        onClash(o, toO, toMe);
        o.onClash(this, toMe, toO);
        ServerLevel sl = (ServerLevel) this.level();
        if (opposite) {
            // the left-spinning top absorbs part of the right-spinner's rotation
            BeybladeEntity thief = spinDir() < 0 ? this : o;
            thief.healSpin((thief == this ? toO : toMe) * 0.4F);
            Px.send(sl, STEAL, thief.getX(), thief.getY() + 0.4, thief.getZ(), 10, 0.35, 0.2, 0.35, 0);
        }
        double cx = getX() + nx * RADIUS, cy = getY() + 0.38, cz = getZ() + nz * RADIUS;
        Px.send(sl, ParticleTypes.ELECTRIC_SPARK, cx, cy, cz, heavy ? 28 : 12, 0.08, 0.08, 0.08, heavy ? 0.7 : 0.45);
        Px.send(sl, ParticleTypes.CRIT, cx, cy, cz, heavy ? 14 : 6, 0.05, 0.05, 0.05, 0.6);
        if (heavy) {
            Px.send(sl, ParticleTypes.END_ROD, cx, cy, cz, 14, 0.1, 0.1, 0.1, 0.25);
            Px.send(sl, ParticleTypes.FIREWORK, cx, cy, cz, 10, 0.05, 0.05, 0.05, 0.25);
        }
        this.level().playSound(null, cx, cy, cz, heavy ? ModSounds.CLASH_HEAVY.get() : ModSounds.CLASH.get(), SoundSource.NEUTRAL,
                heavy ? 2.2F : 1.4F, 0.8F + this.random.nextFloat() * 0.3F);
        this.level().broadcastEntityEvent(this, heavy ? EV_HEAVY_HIT : EV_HIT);
        this.level().broadcastEntityEvent(o, heavy ? EV_HEAVY_HIT : EV_HIT);
        if (heavy) {
            // the shock wave runs out on the side of the one that took the blow
            BeybladeEntity loser = toO >= toMe ? o : this, winner = loser == o ? this : o;
            loser.shockwave(winner, Math.min(1.0, impact));
        }
        addSpirit(heavy ? 6 : 3);
        o.addSpirit(heavy ? 6 : 3);
        // what happens next: sometimes they lock and grind, often they bounce back and charge again
        float r = this.random.nextFloat();
        if (r < 0.3F && lockTicks == 0 && o.lockTicks == 0) {
            int n = 24 + this.random.nextInt(18);
            lockTicks = n;
            o.lockTicks = n;
            lockPartner = o;
            o.lockPartner = this;
            this.level().playSound(null, cx, cy, cz, ModSounds.GRIND.get(), SoundSource.NEUTRAL, 1.4F, 0.8F);
        } else if (r < 0.7F) {
            retreatTicks = 6 + this.random.nextInt(8);
            o.retreatTicks = 6 + this.random.nextInt(8);
        }
        if (o.target == null) o.target = this;
        if (target == null) target = o;
    }

    // ---------------- launch wobble ----------------
    private boolean wobbleLanded;
    /** server copy of the wobble (synced to the client every few ticks) */
    private float wobbleNow;

    public float getWobble() { return this.level().isClientSide() ? this.entityData.get(DATA_WOBBLE) : wobbleNow; }

    /** set by the launcher from how clean the launch was */
    public void setLaunchWobble(float w) {
        wobbleNow = Mth.clamp(w, 0F, 1F);
        this.entityData.set(DATA_WOBBLE, wobbleNow);
        wobbleLanded = false;
    }

    private void tickWobble(@Nullable Player owner, float w) {
        if (onGround()) {
            // first touch of the floor: a badly tilted top scrapes and loses a chunk of spin
            if (!wobbleLanded) {
                wobbleLanded = true;
                if (w > 0.55F) {
                    setSpin(getSpin() - getMaxSpin() * 0.14F * w);
                    play(ModSounds.WALL_HIT.get(), 1.0F, 0.6F);
                    play(ModSounds.GRIND.get(), 1.2F, 0.7F);
                    this.level().broadcastEntityEvent(this, EV_HIT);
                    if (owner != null) owner.displayClientMessage(Component.translatable("msg.librabeyblade.wobble_land")
                            .withStyle(net.minecraft.ChatFormatting.RED), true);
                }
            }
            // it staggers around instead of going where it wants
            Vec3 v = getDeltaMovement();
            setDeltaMovement(v.add((this.random.nextFloat() - 0.5F) * 0.1F * w, 0, (this.random.nextFloat() - 0.5F) * 0.1F * w));
        }
        // and settles down over about fifteen seconds
        wobbleNow = Math.max(0F, w - 0.0028F);
        if (wobbleNow == 0F || this.tickCount % 5 == 0) this.entityData.set(DATA_WOBBLE, wobbleNow);
    }

    // ---------------- special move clash ----------------
    public static final int SCLASH_TICKS = 30;
    public static final byte EV_SCLASH_BOOM = 104;
    /** server: when this beyblade's current special move was started */
    private long specialStart = -1000;
    private int sclashTicks, sclashCooldown;
    @Nullable private BeybladeEntity sclashPartner;
    private boolean sclashLeader, sclashPerfect;
    private float sclashPower;
    /** client */
    public int sclashClientStart = -1000, sclashBoomTime = -1000;
    public double boomX, boomY, boomZ;
    public boolean boomDraw;

    public int getSpecialClashPartner() { return this.entityData.get(DATA_SCLASH); }

    /** where the two moves meet, as a fraction of the way from this beyblade to the other (k = 0..1 of the clash) */
    public static double sclashMeet(float ratio, float k) {
        double end = Mth.clamp(ratio, 0.15, 0.85);
        double e = k * k * (3 - 2 * k);
        return 0.5 + (end - 0.5) * e;
    }
    public float getSpecialClashRatio() { return this.entityData.get(DATA_SCLASH_RATIO); }

    /** the special move in progress (charging in or already running), or 0 */
    private int specialMove() {
        if (pendingAbility != 0) return pendingAbility;
        int ab = getAbility();
        return ab != AB_NONE && ab != buffAbility() ? ab : 0;
    }

    /**
     * Power of the special move in a clash: the move itself (the final, strongest move counts most), how hard it is
     * hitting (attack, level, parts, spin left), how fast the beyblade is moving, and the timing.
     */
    private float specialPower(boolean initiative, boolean perfect) {
        int id = specialMove();
        float tier = id >= abilityCount() ? 1.35F : 1F + 0.06F * id;
        double v = Math.sqrt(getDeltaMovement().x * getDeltaMovement().x + getDeltaMovement().z * getDeltaMovement().z);
        float speed = 0.85F + 0.3F * (float) Mth.clamp(v / 0.6, 0, 1) + 0.1F * (float) (modeSpeed() - 1);
        float p = tier * powerMul() * (0.55F + 0.45F * spinRatio()) * speed;
        if (perfect) p *= 1.1F;
        else if (initiative) p *= 1.12F;
        if (isSpiritActive()) p *= 1.2F;
        return p * (0.9F + 0.2F * this.random.nextFloat());
    }

    /** looks for an enemy using its own special move at the same moment */
    private boolean startSpecialClash() {
        if (sclashCooldown > 0 || specialMove() == 0 || this.tickCount % 2 != 0) return false;
        long now = this.level().getGameTime();
        if (now - specialStart > 60) return false;
        BeybladeEntity best = null;
        double bd = 14 * 14;
        for (BeybladeEntity b : this.level().getEntitiesOfClass(BeybladeEntity.class, getBoundingBox().inflate(14, 6, 14))) {
            if (b == this || !isEnemy(b) || b.sclashTicks > 0 || b.sclashCooldown > 0 || b.specialMove() == 0) continue;
            if (Math.abs(b.specialStart - specialStart) > 40) continue;
            double d = b.distanceToSqr(this);
            if (d < bd) { bd = d; best = b; }
        }
        if (best == null) return false;
        BeybladeEntity o = best;
        long dt = specialStart - o.specialStart;
        boolean perfect = Math.abs(dt) <= 4;
        sclashPower = specialPower(!perfect && dt < 0, perfect);
        o.sclashPower = o.specialPower(!perfect && dt > 0, perfect);
        float ratio = sclashPower / Math.max(0.01F, sclashPower + o.sclashPower);
        sclashPartner = o;
        o.sclashPartner = this;
        sclashLeader = true;
        o.sclashLeader = false;
        sclashPerfect = o.sclashPerfect = perfect;
        sclashTicks = SCLASH_TICKS;
        o.sclashTicks = SCLASH_TICKS + 3;
        this.entityData.set(DATA_SCLASH, o.getId());
        o.entityData.set(DATA_SCLASH, getId());
        this.entityData.set(DATA_SCLASH_RATIO, ratio);
        o.entityData.set(DATA_SCLASH_RATIO, 1F - ratio);
        play(ModSounds.CLASH_HEAVY.get(), 2.6F, 0.5F);
        play(ModSounds.THUNDER_STRIKE.get(), 1.8F, 1.3F);
        announceNear(Component.translatable("msg.librabeyblade.sclash", Component.translatable(titleKey()), Component.translatable(o.titleKey()))
                .append(perfect ? Component.literal(" ").append(Component.translatable("msg.librabeyblade.sclash_perfect")) : Component.empty())
                .withStyle(net.minecraft.ChatFormatting.GOLD, net.minecraft.ChatFormatting.BOLD));
        return true;
    }

    private void tickSpecialClash() {
        BeybladeEntity o = sclashPartner;
        if (o == null || !o.isAlive() || o.getState() != STATE_BATTLE || getState() != STATE_BATTLE) {
            endSpecialClash();
            return;
        }
        sclashTicks--;
        // first they spring apart to a few blocks, then both hold their ground, pouring power into the clash
        Vec3 to = flat(o.position().subtract(position()));
        double dist = to.length();
        Vec3 dir = dist < 1.0E-3 ? new Vec3(1, 0, 0) : to.scale(1 / dist);
        if (dist < 5.0 && SCLASH_TICKS - sclashTicks < 10) setDeltaMovement(dir.scale(-0.32).add(0, getDeltaMovement().y, 0));
        else setDeltaMovement(getDeltaMovement().multiply(0.4, 1, 0.4).add(dir.scale(0.015)));
        if (sclashLeader) {
            if (sclashTicks % 6 == 0) {
                play(ModSounds.GRIND.get(), 1.4F, 1.4F + this.random.nextFloat() * 0.3F);
                play(ModSounds.THUNDER_STRIKE.get(), 0.7F, 1.8F);
            }
            if (sclashTicks <= 0) resolveSpecialClash(o);
        } else if (sclashTicks <= 0) {
            endSpecialClash();
        }
    }

    private void endSpecialClash() {
        sclashTicks = 0;
        sclashPartner = null;
        sclashCooldown = 100;
        this.entityData.set(DATA_SCLASH, -1);
    }

    private void resolveSpecialClash(BeybladeEntity o) {
        float a = sclashPower, b = o.sclashPower;
        float margin = Math.max(a, b) / Math.max(0.01F, Math.min(a, b));
        boolean draw = margin < 1.07F;
        BeybladeEntity win = a >= b ? this : o, lose = win == this ? o : this;
        this.level().broadcastEntityEvent(this, draw ? (byte) (EV_SCLASH_BOOM + 1) : EV_SCLASH_BOOM);
        double cx = (getX() + o.getX()) / 2, cy = getY() + 0.5, cz = (getZ() + o.getZ()) / 2;
        ServerLevel sl = (ServerLevel) this.level();
        Px.send(sl, ParticleTypes.EXPLOSION, cx, cy, cz, 3, 0.4, 0.2, 0.4, 0);
        Px.send(sl, ParticleTypes.ELECTRIC_SPARK, cx, cy, cz, 40, 0.3, 0.3, 0.3, 1.0);
        this.level().playSound(null, cx, cy, cz, ModSounds.METEOR_IMPACT.get(), SoundSource.NEUTRAL, 3.0F, draw ? 1.1F : 0.8F);
        this.level().playSound(null, cx, cy, cz, ModSounds.CLASH_HEAVY.get(), SoundSource.NEUTRAL, 2.6F, 0.6F);
        if (draw) {
            // equal power: both moves are cancelled and both are blown back
            for (BeybladeEntity e : List.of(this, o)) {
                BeybladeEntity other = e == this ? o : this;
                Vec3 away = flat(e.position().subtract(other.position()));
                away = away.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : away.normalize();
                e.cancelSpecial();
                e.damageSpin(70F * other.powerMul(), other);
                e.setDeltaMovement(away.scale(1.1 * Mth.clamp(e.knockMul(), 0.5, 1.3)).add(0, 0.3, 0));
                e.flungTicks = 10;
            }
            announceNear(Component.translatable("msg.librabeyblade.sclash_draw").withStyle(net.minecraft.ChatFormatting.YELLOW));
        } else {
            // the stronger move breaks through: it carries on, the other one is cancelled and its beyblade thrown back
            Vec3 away = flat(lose.position().subtract(win.position()));
            away = away.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : away.normalize();
            lose.cancelSpecial();
            lose.damageSpin(Mth.clamp(80F + 140F * (margin - 1F), 80F, 260F) * win.powerMul(), win);
            lose.setDeltaMovement(away.scale(Mth.clamp(1.2 + 0.8 * (margin - 1F), 1.2, 2.2) * Mth.clamp(lose.knockMul(), 0.55, 1.4)).add(0, 0.35, 0));
            lose.flungTicks = 14;
            lose.shockwave(win, 1.0);
            win.addSpirit(15);
            lose.lastAbilityHitTick = this.level().getGameTime();
            announceNear(Component.translatable("msg.librabeyblade.sclash_win", Component.translatable(win.titleKey()),
                    Component.translatable(win.abilityKey(win.specialMove())), Component.translatable(lose.titleKey()))
                    .withStyle(net.minecraft.ChatFormatting.GOLD));
        }
        endSpecialClash();
        o.endSpecialClash();
    }

    /** a special move lost in a clash: it stops (the cooldown is still spent) */
    private void cancelSpecial() {
        pendingAbility = 0;
        if (getAbility() != AB_NONE) endAbility();
    }

    /** action bar message for the owners and everyone watching close by */
    private void announceNear(Component c) {
        for (Player p : this.level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(32))) p.displayClientMessage(c, true);
    }

    /** pushing power in a lock: spin, attack, weight of the parts, Blader Spirit, plus some luck */
    private float lockPower() {
        float p = getSpin() * powerMul() * (float) Math.sqrt(Math.max(20F, parts().weight()) / 40F);
        if (isSpiritActive()) p *= 1.25F;
        if (isShielded()) p *= 1.1F;
        return p * (0.85F + 0.3F * this.random.nextFloat());
    }

    /** end of a lock: the stronger beyblade launches the other one away */
    private void resolveLock(@Nullable BeybladeEntity o) {
        lockPartner = null;
        if (o == null || !o.isAlive() || o.getState() != STATE_BATTLE || getState() != STATE_BATTLE) return;
        o.lockTicks = 0;
        o.lockPartner = null;
        float a = lockPower(), b = o.lockPower();
        BeybladeEntity win = a >= b ? this : o, lose = win == this ? o : this;
        float margin = Math.max(a, b) / Math.max(1F, Math.min(a, b));
        Vec3 dir = flat(lose.position().subtract(win.position()));
        dir = dir.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : dir.normalize();
        if (lose.tryDodge(win)) return;
        double power = Mth.clamp(1.5 + 0.9 * (margin - 1F), 1.5, 2.6) * Mth.clamp(lose.knockMul(), 0.55, 1.4);
        lose.setDeltaMovement(dir.scale(power).add(0, 0.32, 0));
        lose.hurtMarked = true;
        lose.flungTicks = 14;
        lose.damageSpin(Mth.clamp(45F * margin, 45F, 120F) * win.powerMul(), win);
        lose.shockwave(win, 1.0);
        // the winner holds its ground at the centre of the clash
        win.setDeltaMovement(win.getDeltaMovement().multiply(0.2, 1, 0.2));
        win.retreatTicks = 0;
        win.addSpirit(8);
        double cx = (getX() + o.getX()) / 2, cy = getY() + 0.4, cz = (getZ() + o.getZ()) / 2;
        ServerLevel sl = (ServerLevel) this.level();
        Px.send(sl, ParticleTypes.ELECTRIC_SPARK, cx, cy, cz, 30, 0.1, 0.1, 0.1, 0.8);
        Px.send(sl, ParticleTypes.CRIT, cx, cy, cz, 16, 0.08, 0.08, 0.08, 0.7);
        this.level().playSound(null, cx, cy, cz, ModSounds.CLASH_HEAVY.get(), SoundSource.NEUTRAL, 2.6F, 0.55F);
        this.level().playSound(null, cx, cy, cz, ModSounds.WALL_HIT.get(), SoundSource.NEUTRAL, 1.6F, 0.7F);
        this.level().broadcastEntityEvent(lose, EV_HEAVY_HIT);
        this.level().broadcastEntityEvent(win, EV_HEAVY_HIT);
        Player wo = win.getOwner(), lo = lose.getOwner();
        if (wo != null) wo.displayClientMessage(Component.translatable("msg.librabeyblade.lock_win", Component.translatable(win.titleKey()))
                .withStyle(net.minecraft.ChatFormatting.GOLD), true);
        if (lo != null && lo != wo) lo.displayClientMessage(Component.translatable("msg.librabeyblade.lock_lose", Component.translatable(lose.titleKey()))
                .withStyle(net.minecraft.ChatFormatting.RED), true);
    }

    /** a strong hit: a shock wave travels away from the attacker, shoving whatever stands in its way */
    public void shockwave(Entity from, double strength) {
        Vec3 dir = flat(position().subtract(from.position()));
        dir = dir.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : dir.normalize();
        this.level().broadcastEntityEvent(this, EV_SHOCK);
        Vec3 c = position().add(from.position()).scale(0.5);
        double range = 3.5 + 2.5 * strength;
        for (Entity e : this.level().getEntities(this, getBoundingBox().inflate(range, 2, range))) {
            if (e == from || e instanceof StadiumEntity || e instanceof Player p && p.isSpectator()) continue;
            Vec3 rel = flat(e.position().subtract(c));
            double d = rel.length();
            if (d < 0.3 || d > range || rel.scale(1 / d).dot(dir) < 0.55) continue;
            double k = (1 - d / range) * strength;
            if (e instanceof BeybladeEntity b) {
                if (b.getState() != STATE_BATTLE) continue;
                b.setDeltaMovement(b.getDeltaMovement().add(dir.scale(0.35 * k)));
                if (isEnemy(b) || b.isEnemy(this)) b.damageSpin((float) (25 * k), from);
            } else if (e instanceof LivingEntity l) {
                l.push(dir.x * 0.6 * k, 0.15 * k, dir.z * 0.6 * k);
                l.hurtMarked = true;
            }
        }
    }

    private static void shift(Entity e, double dx, double dz) {
        AABB box = e.getBoundingBox().move(dx, 0, dz);
        if (e.level().noCollision(e, box)) {
            e.setPos(e.getX() + dx, e.getY(), e.getZ() + dz);
        } else {
            AABB half = e.getBoundingBox().move(dx * 0.5, 0, dz * 0.5);
            if (e.level().noCollision(e, half)) e.setPos(e.getX() + dx * 0.5, e.getY(), e.getZ() + dz * 0.5);
        }
    }

    private void collideLiving(@Nullable Player owner) {
        for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.2),
                l -> l.isAlive() && !l.isSpectator())) {
            double dx = e.getX() - getX(), dz = e.getZ() - getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            double min = RADIUS + e.getBbWidth() / 2.0;
            if (dist >= min || dist < 1.0E-4) continue;
            if (e.getY() > getY() + HEIGHT || e.getY() + e.getBbHeight() < getY()) continue;
            double nx = dx / dist, nz = dz / dist;
            shift(this, -nx * (min - dist) * 0.8, -nz * (min - dist) * 0.8);
            Vec3 v = getDeltaMovement();
            double sp = flat(v).length();
            if (isHostileLiving(e) && sp > 0.1 && e.hurtTime == 0) {
                e.hurt(damageSources().thrown(this, owner), 3F + (float) (sp * 16) * (0.6F + 0.4F * spinRatio()));
                e.knockback(0.5 + sp, -nx, -nz);
                setDeltaMovement(v.add(-nx * (sp * 1.3 + 0.1), 0.05, -nz * (sp * 1.3 + 0.1)));
                Px.send((ServerLevel) this.level(), ParticleTypes.ELECTRIC_SPARK, getX() + nx * RADIUS, getY() + 0.35, getZ() + nz * RADIUS, 10, 0.1, 0.1, 0.1, 0.4);
                play(ModSounds.CLASH.get(), 0.7F, 1.2F);
                setSpin(getSpin() - 6F);
                this.level().broadcastEntityEvent(this, EV_HIT);
            } else {
                double along = v.x * nx + v.z * nz;
                if (along > 0) setDeltaMovement(v.add(-nx * along * 1.6, 0, -nz * along * 1.6));
                e.push(nx * 0.04, 0, nz * 0.04);
            }
        }
    }

    public void damageSpin(float amount, @Nullable Entity source) {
        if (getState() != STATE_BATTLE || amount <= 0) return;
        float taken = amount * incomingMul() * guardMul();
        // eternal / 4D tips turn freely and lose less spin
        if (parts().tip().freeSpin()) taken *= 0.86F;
        setSpin(getSpin() - taken);
        if (taken > 30F) addSpirit(4);
        // Defense types bounce part of a collision back at the attacker (counter)
        if (getMode() == MODE_DEFENSE && source instanceof BeybladeEntity att && att != this && att.getAbility() == AB_NONE && taken > 12F)
            att.setSpin(att.getSpin() - taken * 0.15F);
        if (source instanceof BeybladeEntity b && b != this) {
            lastAttacker = b;
            lastAttackTick = this.level().getGameTime();
            if (b.getAbility() != AB_NONE || b.isBuffActive()) lastAbilityHitTick = lastAttackTick;
        }
        if (source != null && target == null && (!(source instanceof BeybladeEntity b) || isEnemy(b))) target = source;
    }

    public void healSpin(float amount) {
        if (getState() == STATE_BATTLE) setSpin(getSpin() + amount);
    }

    protected void shieldHit() {
        play(ModSounds.SHIELD_HIT.get(), 1.0F, 0.9F + this.random.nextFloat() * 0.3F);
        this.level().broadcastEntityEvent(this, EV_SHIELD_HIT);
    }

    // ---------------- abilities ----------------
    public boolean tryAbility(int id, boolean fromPlayer) {
        if (id < 1 || id > abilityCount() || getState() != STATE_BATTLE) return false;
        Player owner = getOwner();
        int cd = cooldownRemaining(id);
        if (cd > 0) {
            if (fromPlayer && owner != null)
                owner.displayClientMessage(Component.translatable("msg.librabeyblade.cooldown", Component.translatable(abilityKey(id)), (cd + 19) / 20), true);
            return false;
        }
        if (id == buffAbility()) {
            this.entityData.set(DATA_SHIELD_END, (int) (this.level().getGameTime() + buffTicks()));
            startCooldown(id);
            onBuffStart(owner);
            announce(owner, id);
            return true;
        }
        if (!abilityAvailable(id)) {
            if (fromPlayer && owner != null) owner.displayClientMessage(Component.translatable("msg.librabeyblade.locked", Component.translatable(abilityKey(id))), true);
            return false;
        }
        if (getAbility() != AB_NONE || pendingAbility != 0) {
            if (fromPlayer && owner != null) owner.displayClientMessage(Component.translatable("msg.librabeyblade.busy"), true);
            return false;
        }
        if (requiresTarget(id)) {
            if (target == null) target = findTarget(owner);
            if (target == null) {
                if (fromPlayer && owner != null) owner.displayClientMessage(Component.translatable("msg.librabeyblade.no_target"), true);
                return false;
            }
            this.entityData.set(DATA_TARGET, target.getId());
        }
        startCooldown(id);
        addXp(4);
        specialStart = this.level().getGameTime();
        // special moves are delivered by crashing into the opponent: charge first, the move starts on contact
        Entity tg = target != null ? target : findTarget(owner);
        if (ramsInto(id) && tg instanceof BeybladeEntity b && b.getState() == STATE_BATTLE && !frozen && stunTicks == 0
                && flat(tg.position().subtract(position())).length() > RADIUS * 2 + 0.3) {
            target = tg;
            this.entityData.set(DATA_TARGET, tg.getId());
            pendingAbility = id;
            ramTicks = 0;
            if (owner != null) owner.displayClientMessage(Component.translatable("msg.librabeyblade.ram", Component.translatable(abilityKey(id))), true);
            play(ModSounds.CHARGE.get(), 1.0F, 1.5F);
            return true;
        }
        beginAbility(id, owner);
        return true;
    }

    private void beginAbility(int id, @Nullable Player owner) {
        abilityTicks = 0;
        abilityBaseY = getY();
        abilityBaseX = getX();
        abilityBaseZ = getZ();
        diving = false;
        for (Set<Integer> s : waveHits) s.clear();
        this.entityData.set(DATA_ABILITY, id);
        announce(owner, id);
    }

    /** launcher bonus: a perfectly timed pull starts with a fuller spirit gauge */
    public void launchBonus(boolean perfect, boolean power) {
        if (perfect) this.entityData.set(DATA_SPIRIT, 40);
        if (power) { dashTicks = 10; }
    }

    /** how many special moves this beyblade has (Rock Leone has a sixth) */
    public int abilityCount() { return 5; }

    /** called after every clash with another beyblade (damage dealt and taken) */
    protected void onClash(BeybladeEntity other, float dealt, float taken) {}

    /** chance to come back when the spin is about to run out; return true if it did */
    protected boolean lastStand() { return false; }

    /** an extra HUD line for beyblade-specific passives, or null */
    @Nullable
    public Component passiveLine() { return null; }

    /** whether this move is delivered by charging into the target first (all action moves by default) */
    protected boolean ramsInto(int id) { return id != buffAbility(); }

    /** read extra data (common beyblade variant) from the launched item */
    protected void initFromItem(ItemStack stack) {}

    /** some beyblades (common ones) unlock their stronger moves only at higher levels */
    public boolean abilityAvailable(int id) { return true; }

    private void announce(@Nullable Player owner, int id) {
        if (owner != null) owner.displayClientMessage(Component.translatable("msg.librabeyblade.ability." + kindId(), Component.translatable(abilityKey(id))), true);
    }

    private void startCooldown(int id) {
        this.entityData.set(DATA_COOLDOWN[id - 1], (int) (this.level().getGameTime() + cooldown(id)));
    }

    protected void endAbility() {
        pendingAbility = 0;
        if (getAbility() != AB_NONE) this.entityData.set(DATA_ABILITY, AB_NONE);
        setNoGravity(false);
        diving = false;
        abilityTicks = 0;
    }

    private void rivalUseAbilities() {
        if (target == null || target.distanceToSqr(this) > 12 * 12 || this.random.nextInt(45) != 0) return;
        int id = 1 + this.random.nextInt(abilityCount());
        if (cooldownRemaining(id) == 0) tryAbility(id, false);
    }

    /** enemies an area ability affects: enemy beyblades + hostile living things */
    public List<Entity> enemiesWithin(double r) {
        List<Entity> out = new ArrayList<>();
        AABB box = getBoundingBox().inflate(r, r, r);
        for (Entity e : this.level().getEntities(this, box)) {
            if (e.distanceToSqr(this) > r * r) continue;
            if (e instanceof BeybladeEntity b ? isEnemy(b) : (e instanceof LivingEntity l && isHostileLiving(l))) out.add(e);
        }
        return out;
    }

    protected void hover(double height, double strength) {
        setNoGravity(true);
        Vec3 v = getDeltaMovement();
        double dy = (abilityBaseY + height) - getY();
        setDeltaMovement(v.x * 0.82, Mth.clamp(dy * strength, -0.6, 0.6), v.z * 0.82);
    }

    protected boolean tickAbility(int ab, int t, @Nullable Player owner) {
        ServerLevel sl = (ServerLevel) this.level();
        switch (ab) {
            case AB_SONIC_WAVE -> {
                if (t < 40) hover(0.7, 0.3); else setNoGravity(false);
                for (int i = 0; i < WAVE_PULSES.length; i++) {
                    int age = t - WAVE_PULSES[i];
                    if (age == 0) {
                        waveHits[i].clear();
                        play(ModSounds.SONIC_WAVE.get(), 1.6F, 0.9F + i * 0.1F);
                        Px.send(sl, ParticleTypes.SONIC_BOOM, getX(), getY() + 0.4, getZ(), 1, 0, 0, 0, 0);
                    }
                    if (age < 0 || age >= WAVE_LIFE) continue;
                    double front = 0.6 + age * WAVE_SPEED;
                    for (Entity e : enemiesWithin(front + 0.5)) {
                        double d = Math.sqrt(flat(e.position().subtract(position())).lengthSqr());
                        if (d < front - 1.0 || !waveHits[i].add(e.getId())) continue;
                        Vec3 out = flat(e.position().subtract(position()));
                        out = out.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : out.normalize();
                        if (e instanceof BeybladeEntity b) {
                            b.damageSpin(60F, this);
                            b.setDeltaMovement(b.getDeltaMovement().add(out.scale(0.6)).add(0, 0.18, 0));
                            Px.send(sl, ParticleTypes.ELECTRIC_SPARK, b.getX(), b.getY() + 0.4, b.getZ(), 8, 0.2, 0.2, 0.2, 0.3);
                        } else if (e instanceof LivingEntity l) {
                            l.hurt(damageSources().indirectMagic(this, owner), 5F);
                            l.knockback(0.9, -out.x, -out.z);
                        }
                    }
                }
                return true;
            }
            case AB_INFERNO -> {
                BlockPos below = BlockPos.containing(abilityX(), abilityBaseY - 0.2, abilityZ());
                if (t == 1) play(ModSounds.INFERNO_DRILL.get(), 1.4F, 1.0F);
                if (t < INF_TORNADO) {
                    setDeltaMovement(getDeltaMovement().multiply(0.55, 1, 0.55));
                    for (Entity e : enemiesWithin(9)) {
                        Vec3 pull = flat(position().subtract(e.position()));
                        if (pull.lengthSqr() < 0.25) continue;
                        e.setDeltaMovement(e.getDeltaMovement().add(pull.normalize().scale(e instanceof BeybladeEntity ? 0.07 : 0.05)));
                        e.hurtMarked = true;
                    }
                    BlockState ground = this.level().getBlockState(below);
                    if (!ground.isAir() && t % 2 == 0)
                        Px.send(sl, new BlockParticleOption(ParticleTypes.BLOCK, ground), getX(), getY() + 0.1, getZ(), 10, 0.5, 0.05, 0.5, 0.2);
                    return true;
                }
                if (t < INF_DIVE) {
                    if (t == INF_TORNADO) {
                        play(ModSounds.INFERNO_TORNADO.get(), 2.0F, 1.0F);
                        Px.send(sl, ParticleTypes.LAVA, getX(), getY() + 0.3, getZ(), 10, 0.8, 0.2, 0.8, 0);
                    }
                    hover(6.0, 0.12);
                    for (Entity e : enemiesWithin(INF_HEIGHT)) {
                        Vec3 rel = flat(e.position().subtract(new Vec3(getX(), 0, getZ())));
                        double d = rel.length();
                        if (d > INF_RADIUS || e.getY() < abilityBaseY - 1 || e.getY() > abilityBaseY + INF_HEIGHT) continue;
                        Vec3 dir = d < 1.0E-3 ? new Vec3(1, 0, 0) : rel.scale(1 / d);
                        Vec3 swirl = new Vec3(dir.z, 0, -dir.x).scale(0.34).add(dir.scale(-0.08));
                        Vec3 nv = new Vec3(swirl.x, Math.min(0.32, e.getDeltaMovement().y + 0.12), swirl.z);
                        e.setDeltaMovement(nv);
                        e.hurtMarked = true;
                        e.fallDistance = 0;
                        if (e instanceof BeybladeEntity b) b.damageSpin(6F, this);
                        else if (e instanceof LivingEntity l && t % 8 == 0) {
                            l.hurt(damageSources().indirectMagic(this, owner), 3F);
                            l.setSecondsOnFire(4);
                        }
                    }
                    return true;
                }
                if (t == INF_DIVE) {
                    setNoGravity(false);
                    Vec3 aim = target != null ? target.position() : new Vec3(getX(), abilityBaseY, getZ());
                    Vec3 d = flat(aim.subtract(position()));
                    setDeltaMovement(d.x * 0.14, -1.5, d.z * 0.14);
                    diving = true;
                }
                if (diving && (onGround() || t > INF_DIVE + 24)) {
                    diving = false;
                    infernoImpact(owner);
                    abilityTicks = Math.max(abilityTicks, DURATION[AB_INFERNO] - 16);
                }
                return true;
            }
            case AB_BUSTER -> {
                if (t == 1) play(ModSounds.BUSTER_CHARGE.get(), 1.5F, 1.0F);
                if (t < BUS_END) hover(2.4, 0.2); else setNoGravity(false);
                if (target == null) {
                    if (t < BUS_FIRE) endAbilityEarly();
                    return true;
                }
                if (t == BUS_FIRE) play(ModSounds.BUSTER_FIRE.get(), 2.0F, 1.0F);
                if (t >= BUS_FIRE && t < BUS_END) {
                    Vec3 from = position().add(0, 0.35, 0);
                    Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0);
                    Vec3 dir = to.subtract(from);
                    double len = dir.length();
                    if (len > 1.0E-3) dir = dir.scale(1 / len);
                    for (Entity e : enemiesWithin(len + 2)) {
                        Vec3 c = e.position().add(0, e.getBbHeight() * 0.5, 0);
                        double along = c.subtract(from).dot(dir);
                        if (along < 0 || along > len + 1) continue;
                        double off = c.subtract(from.add(dir.scale(along))).length();
                        if (off > 1.1 && e != target) continue;
                        if (e instanceof BeybladeEntity b) {
                            b.damageSpin(e == target ? 14F : 7F, this);
                            b.setDeltaMovement(b.getDeltaMovement().add(flat(dir).scale(0.09)));
                        } else if (e instanceof LivingEntity l && t % 4 == 0) {
                            l.hurt(damageSources().indirectMagic(this, owner), 3F);
                            l.knockback(0.3, -dir.x, -dir.z);
                        }
                    }
                    if (t % 5 == 0) Px.send(sl, ParticleTypes.SONIC_BOOM, to.x, to.y, to.z, 1, 0, 0, 0, 0);
                    Px.send(sl, ParticleTypes.ELECTRIC_SPARK, to.x, to.y, to.z, 6, 0.3, 0.3, 0.3, 0.4);
                }
                return true;
            }
            case AB_JUDGMENT -> {
                if (t == 1) play(ModSounds.JUDGMENT_START.get(), 2.0F, 1.0F);
                if (t < DURATION[AB_JUDGMENT] - 10) hover(1.2, 0.15); else setNoGravity(false);
                if (t >= JUD_LINK && t < JUD_STRIKE) {
                    if ((t - JUD_LINK) % 20 == 0) play(ModSounds.DRAIN.get(), 1.2F, 1.0F);
                    for (Entity e : enemiesWithin(JUD_RANGE)) {
                        if (e instanceof BeybladeEntity b) {
                            float d = Math.min(5F, b.getSpin());
                            b.damageSpin(d, this);
                            healSpin(d * 0.6F);
                            b.setDeltaMovement(b.getDeltaMovement().multiply(0.8, 1, 0.8));
                        } else if (e instanceof LivingEntity l && t % 10 == 0) {
                            l.hurt(damageSources().indirectMagic(this, owner), 2F);
                            l.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3));
                            l.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1));
                        }
                    }
                }
                if (t == JUD_STRIKE) {
                    play(ModSounds.JUDGMENT_STRIKE.get(), 2.5F, 1.0F);
                    this.level().broadcastEntityEvent(this, EV_STRIKE);
                    for (Entity e : enemiesWithin(JUD_RANGE)) {
                        if (e instanceof BeybladeEntity b) {
                            b.damageSpin(130F, this);
                            b.setDeltaMovement(b.getDeltaMovement().add(0, 0.5, 0));
                        } else if (e instanceof LivingEntity l) {
                            l.hurt(damageSources().indirectMagic(this, owner), 12F);
                        }
                        Px.send(sl, ParticleTypes.END_ROD, e.getX(), e.getY() + 1, e.getZ(), 30, 0.3, 1.5, 0.3, 0.08);
                        Px.send(sl, ParticleTypes.END_ROD, e.getX(), e.getY() + 0.5, e.getZ(), 14, 0.1, 0.1, 0.1, 0.25);
                    }
                }
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private double abilityX() { return getX(); }
    private double abilityZ() { return getZ(); }

    protected void endAbilityEarly() {
        abilityTicks = duration(getAbility());
    }

    private void infernoImpact(@Nullable Player owner) {
        ServerLevel sl = (ServerLevel) this.level();
        play(ModSounds.INFERNO_IMPACT.get(), 2.5F, 1.0F);
        this.level().broadcastEntityEvent(this, EV_IMPACT);
        Px.send(sl, ParticleTypes.END_ROD, getX(), getY() + 0.5, getZ(), 14, 0.1, 0.1, 0.1, 0.25);
        Px.send(sl, ParticleTypes.LAVA, getX(), getY() + 0.3, getZ(), 20, 1.5, 0.2, 1.5, 0);
        for (int i = 0; i < 48; i++) {
            double a = i / 48.0 * Math.PI * 2;
            Px.send(sl, ParticleTypes.FLAME, getX() + Math.cos(a) * 0.8, getY() + 0.15, getZ() + Math.sin(a) * 0.8, 0,
                    Math.cos(a), 0.05, Math.sin(a), 0.45);
        }
        for (Entity e : enemiesWithin(INF_IMPACT_RADIUS)) {
            Vec3 out = flat(e.position().subtract(position()));
            out = out.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : out.normalize();
            double falloff = 1.0 - Math.min(1.0, e.distanceTo(this) / INF_IMPACT_RADIUS) * 0.5;
            if (e instanceof BeybladeEntity b) {
                b.damageSpin((float) (160 * falloff), this);
                b.setDeltaMovement(b.getDeltaMovement().add(out.scale(0.9 * falloff)).add(0, 0.35, 0));
            } else if (e instanceof LivingEntity l) {
                l.hurt(damageSources().indirectMagic(this, owner), (float) (10 * falloff));
                l.setSecondsOnFire(5);
                l.knockback(1.2 * falloff, -out.x, -out.z);
            }
        }
    }

    protected void tickShield() {
        for (Projectile p : this.level().getEntitiesOfClass(Projectile.class, getBoundingBox().inflate(1.6))) {
            Entity po = p.getOwner();
            if (po != null && po.getUUID().equals(getOwnerId())) continue;
            Vec3 v = p.getDeltaMovement();
            Vec3 away = p.position().subtract(position());
            if (v.dot(away) < 0) {
                p.setDeltaMovement(v.scale(-0.7));
                p.hurtMarked = true;
                shieldHit();
            }
        }
    }

    // ---------------- recall / defeat ----------------
    public void recall() {
        if (getState() == STATE_RECALL || isRival()) return;
        endAbility();
        setState(STATE_RECALL);
        setNoGravity(true);
        this.noPhysics = true;
        play(ModSounds.RECALL.get(), 1.0F, 1.0F);
    }

    private void tickRecall(@Nullable Player owner) {
        this.noPhysics = true;
        if (owner == null) {
            dropStored();
            discard();
            return;
        }
        Vec3 goal = owner.position().add(0, owner.getBbHeight() * 0.55, 0);
        Vec3 d = goal.subtract(position());
        double dist = d.length();
        if (dist < 1.1 || stateTicks > 200) {
            giveTo(owner);
            this.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(), ModSounds.CATCH.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            discard();
            return;
        }
        double speed = Math.min(1.5, 0.25 + stateTicks * 0.07);
        Vec3 arc = stateTicks < 8 ? new Vec3(0, 0.25, 0) : Vec3.ZERO;
        Vec3 v = d.scale(Math.min(speed, dist) / dist).add(arc);
        setDeltaMovement(v);
        setPos(getX() + v.x, getY() + v.y, getZ() + v.z);
    }

    /** curved bowl floor and the all-round wall of a nearby {@link StadiumEntity}; once inside, a beyblade can never leave */
    private void stadiumPhysics() {
        List<StadiumEntity> list = this.level().getEntitiesOfClass(StadiumEntity.class, getBoundingBox().inflate(18, 6, 18));
        if (list.isEmpty()) {
            stadium = null;
            inBowl = false;
            contained = false;
            return;
        }
        StadiumEntity st = list.get(0);
        double dx = getX() - st.getX(), dz = getZ() - st.getZ();
        double r = Math.sqrt(dx * dx + dz * dz);
        double rel = getY() - st.getY();
        if (r > StadiumEntity.APOTHEM1 + 1.5 || rel < -1.5 || rel > 14) {
            stadium = null;
            inBowl = false;
            contained = false;
            return;
        }
        stadium = st;
        double nx = r < 1.0E-4 ? 1 : dx / r, nz = r < 1.0E-4 ? 0 : dz / r;
        Vec3 v = getDeltaMovement();
        double wallR = StadiumEntity.WALL_IN - RADIUS;
        if (r < wallR && rel > -1.0) contained = true;
        if (contained && getState() != STATE_RECALL && r > wallR) {
            // the wall runs all the way round, and an invisible cylinder above it keeps flying tops in too
            setPos(st.getX() + nx * wallR, getY(), st.getZ() + nz * wallR);
            r = wallR;
            double vr = v.x * nx + v.z * nz;
            if (vr > 0) {
                v = v.subtract(nx * vr * 1.7, 0, nz * vr * 1.7);
                if (vr > 0.7 && getState() == STATE_BATTLE) {
                    // slammed into the wall at speed: real damage and a heavy crash
                    damageSpin((float) ((vr - 0.4) * 150), lastAttacker());
                    this.level().playSound(null, getX(), getY(), getZ(), ModSounds.CLASH_HEAVY.get(), SoundSource.NEUTRAL, 2.2F, 0.55F);
                    this.level().broadcastEntityEvent(this, EV_HEAVY_HIT);
                }
                if (vr > 0.12 && wallSoundCooldown == 0 && rel < StadiumEntity.WALL_TOP + 0.5) {
                    wallSoundCooldown = 6;
                    play(ModSounds.WALL_HIT.get(), 0.9F, 0.9F + this.random.nextFloat() * 0.3F);
                    setSpin(getSpin() - 3F);
                    this.level().broadcastEntityEvent(this, EV_WALL);
                }
            }
        }
        // the bowl surface and the base tiers
        double surf = StadiumEntity.surface(r);
        if (surf >= 0 && getState() != STATE_RECALL) {
            double fy = st.getY() + surf;
            if (getY() <= fy + 0.06 && rel > -1.2) {
                setPos(getX(), fy, getZ());
                if (v.y < 0) v = new Vec3(v.x, v.y < -0.35 ? -v.y * 0.25 : 0, v.z);
                setOnGround(true);
                double sl = StadiumEntity.slope(r);
                // Tornado Ridge: riding the outer slope of the bowl tangentially builds up speed
                if (r > StadiumEntity.BOWL_R * 0.62) {
                    double tx = -nz, tz = nx;
                    double vt = v.x * tx + v.z * tz;
                    if (Math.abs(vt) > 0.25 && Math.abs(vt) < 1.05) v = v.add(tx * vt * 0.045, 0, tz * vt * 0.045);
                }
                // gravity along the curved floor pulls tops toward the centre; same rolling friction as the ground
                v = new Vec3((v.x - nx * sl * 0.045) * 0.965, v.y, (v.z - nz * sl * 0.045) * 0.965);
            }
        }
        inBowl = r < StadiumEntity.WALL_IN && (contained || rel < StadiumEntity.WALL_TOP + 0.5);
        setDeltaMovement(v);
    }

    private void defeat() {
        endAbility();
        setSpin(0F);
        setState(STATE_DEFEATED);
        play(ModSounds.DEFEAT.get(), 1.2F, 1.0F);
        Player owner = getOwner();
        if (owner != null) owner.displayClientMessage(Component.translatable("msg.librabeyblade.defeated"), true);
        BeybladeEntity winner = lastAttacker();
        if (winner != null && winner.isAlive() && winner.getState() == STATE_BATTLE) {
            winner.addXp(isRival() ? 30 : 50);
            Player wo = winner.getOwner();
            if (wo != null) wo.displayClientMessage(Component.translatable(finishedByAbility() ? "msg.librabeyblade.ko_ability" : "msg.librabeyblade.ko"), true);
        }
    }

    private void tickDefeated(@Nullable Player owner) {
        Vec3 v = getDeltaMovement();
        setDeltaMovement(v.x * 0.9, v.y, v.z * 0.9);
        physics();
        stadiumPhysics();
        collideBeyblades();
        if (stateTicks == 70 && owner != null) {
            recall();
            return;
        }
        if (stateTicks > 90 && owner == null) {
            Px.send((ServerLevel) this.level(), ParticleTypes.POOF, getX(), getY() + 0.3, getZ(), 12, 0.3, 0.2, 0.3, 0.02);
            discard();
        }
    }

    /** level of the best beyblade item a player carries (for matching AI opponents) */
    public static int heldLevel(Player p) {
        int best = 1;
        for (ItemStack st : p.getInventory().items) {
            if (st.getItem() instanceof com.krolasyon.beyblade.item.LibraItem) best = Math.max(best, com.krolasyon.beyblade.item.LibraItem.level(st));
        }
        return best;
    }

    /** gives experience to the player's active beyblade, or else to the beyblade item in hand / inventory */
    public static void grantXp(Player p, int amount) {
        BeybladeEntity active = findActive(p);
        if (active != null) {
            active.addXp(amount);
            return;
        }
        ItemStack target = p.getMainHandItem().getItem() instanceof com.krolasyon.beyblade.item.LibraItem ? p.getMainHandItem() : ItemStack.EMPTY;
        if (target.isEmpty()) {
            for (ItemStack st : p.getInventory().items) {
                if (st.getItem() instanceof com.krolasyon.beyblade.item.LibraItem) { target = st; break; }
            }
        }
        if (target.isEmpty()) return;
        CompoundTag t0 = com.krolasyon.beyblade.compat.V.tag(target);
        int lvl = t0 == null ? 1 : Math.max(1, t0.getInt("BeyLevel")), xp = (t0 == null ? 0 : t0.getInt("BeyXp")) + amount;
        int before = lvl;
        while (lvl < MAX_LEVEL && xp >= xpNeeded(lvl)) { xp -= xpNeeded(lvl); lvl++; }
        final int nl = lvl, nx = lvl >= MAX_LEVEL ? 0 : xp;
        com.krolasyon.beyblade.compat.V.editTag(target, t -> {
            t.putInt("BeyLevel", nl);
            t.putInt("BeyXp", nx);
        });
        if (lvl > before) p.displayClientMessage(Component.translatable("msg.librabeyblade.level_up", target.getHoverName(), lvl).withStyle(net.minecraft.ChatFormatting.GOLD), false);
    }

    /** the launched item with this beyblade's level, experience and mode written back */
    public ItemStack itemWithProgress() {
        ItemStack s = stored.isEmpty() ? defaultItem() : stored.copy();
        com.krolasyon.beyblade.compat.V.editTag(s, t -> {
            t.putInt("BeyLevel", getLevel());
            t.putInt("BeyXp", getXp());
            t.putInt("BeyMode", getMode());
        });
        return s;
    }

    private void giveTo(Player p) {
        if (isRival()) return;
        ItemStack s = itemWithProgress();
        stored = ItemStack.EMPTY;
        if (!p.getInventory().add(s)) p.drop(s, false);
    }

    private void dropStored() {
        if (isRival()) return;
        ItemStack s = itemWithProgress();
        stored = ItemStack.EMPTY;
        spawnAtLocation(s, 0.3F);
    }

    protected void play(SoundEvent s, float vol, float pitch) {
        this.level().playSound(null, getX(), getY(), getZ(), s, SoundSource.NEUTRAL, vol, pitch);
    }

    // ---------------- interaction ----------------
    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (player.getUUID().equals(getOwnerId())) {
            if (!this.level().isClientSide()) recall();
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide() || isInvulnerableTo(source) || getState() != STATE_BATTLE) return false;
        Entity att = source.getEntity();
        if (att instanceof Player p) {
            if (p.getUUID().equals(getOwnerId())) return false;
            damageSpin(amount * 8F, null);
            Vec3 push = flat(position().subtract(p.position()));
            if (push.lengthSqr() > 1.0E-4) setDeltaMovement(getDeltaMovement().add(push.normalize().scale(0.4)).add(0, 0.15, 0));
            this.level().broadcastEntityEvent(this, EV_HIT);
            play(ModSounds.CLASH.get(), 0.7F, 1.3F);
            return true;
        }
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            damageSpin(amount * 4F, null);
            return true;
        }
        return false;
    }

    @Override public boolean isPickable() { return !isRemoved(); }
    @Override public boolean isPushable() { return false; }
    @Override public void push(Entity e) {}
    @Override public boolean canBeCollidedWith() { return false; }
    @Override public boolean displayFireAnimation() { return false; }

    @Override
    public AABB getBoundingBoxForCulling() {
        if (clientAbility != AB_NONE || getState() == STATE_RECALL) return getBoundingBox().inflate(30, 80, 30);
        if (isShielded()) return getBoundingBox().inflate(2);
        return getBoundingBox().inflate(1.5);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) { return d < 128 * 128; }

    // ---------------- client ----------------
    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (this.level() != null && this.level().isClientSide()) {
            if (DATA_SCLASH.equals(key) && getSpecialClashPartner() >= 0) sclashClientStart = this.tickCount;
            if (DATA_ABILITY.equals(key)) {
                clientAbility = getAbility();
                clientAbilityStart = this.tickCount;
                abilityX = getX();
                abilityY = getY();
                abilityZ = getZ();
            } else if (DATA_STATE.equals(key)) {
                clientStateStart = this.tickCount;
            }
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        switch (id) {
            case EV_SCLASH_BOOM, EV_SCLASH_BOOM + 1 -> {
                // where the two moves met when the clash was decided
                sclashBoomTime = this.tickCount;
                boomDraw = id != EV_SCLASH_BOOM;
                Entity o = this.level().getEntity(getSpecialClashPartner());
                double f = boomDraw ? 0.5 : sclashMeet(getSpecialClashRatio(), 1F);
                if (o != null) {
                    boomX = getX() + (o.getX() - getX()) * f;
                    boomY = getY() + (o.getY() - getY()) * f + 0.55;
                    boomZ = getZ() + (o.getZ() - getZ()) * f;
                    for (StadiumEntity st : this.level().getEntitiesOfClass(StadiumEntity.class, getBoundingBox().inflate(18, 6, 18))) {
                        st.addScar(boomX, boomZ, 3.2F);
                        break;
                    }
                }
            }
            case EV_HIT, EV_WALL -> hitTime = this.tickCount;
            case EV_GRIND -> grindTime = this.tickCount;
            case EV_SPIRIT -> spiritTime = this.tickCount;
            case EV_EVADE -> evadeTime = this.tickCount;
            case EV_SHOCK -> {
                // direction: away from the nearest other beyblade (the one that hit us)
                BeybladeEntity other = null;
                double best = 9;
                for (BeybladeEntity b : this.level().getEntitiesOfClass(BeybladeEntity.class, getBoundingBox().inflate(2.5), b -> b != this)) {
                    double d = b.distanceToSqr(this);
                    if (d < best) { best = d; other = b; }
                }
                Vec3 dir = other == null ? new Vec3(1, 0, 0) : flat(position().subtract(other.position()));
                dir = dir.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : dir.normalize();
                shockTime = this.tickCount;
                clientScar(1.2F);
                shockDirX = dir.x;
                shockDirZ = dir.z;
                Vec3 c = other == null ? position() : position().add(other.position()).scale(0.5);
                shockX = c.x;
                shockY = c.y;
                shockZ = c.z;
            }
            case EV_HEAVY_HIT -> { hitTime = this.tickCount; heavyHitTime = this.tickCount; }
            case EV_LAND -> {
                landTime = this.tickCount;
                for (int i = 0; i < 20; i++) {
                    double a = i / 20.0 * Math.PI * 2;
                    Px.add(this.level(), ParticleTypes.CLOUD, getX() + Math.cos(a) * 0.4, getY() + 0.05, getZ() + Math.sin(a) * 0.4,
                            Math.cos(a) * 0.12, 0.01, Math.sin(a) * 0.12);
                }
            }
            case EV_SHIELD_HIT -> shieldHitTime = this.tickCount;
            case EV_IMPACT -> { impactTime = this.tickCount; clientScar(3.4F); }
            case EV_STRIKE -> { strikeTime = this.tickCount; clientScar(1.8F); }
            default -> super.handleEntityEvent(id);
        }
    }

    /** degrees per tick of the spin animation at a given spin ratio */
    public static float spinSpeedFor(float ratio) { return 70F + 175F * ratio; }

    /** client: leave impact cracks in the stadium bowl under this beyblade (the stadium is marked, never broken) */
    public void clientScar(float r) {
        for (StadiumEntity st : this.level().getEntitiesOfClass(StadiumEntity.class, getBoundingBox().inflate(18, 6, 18))) {
            double dx = getX() - st.getX(), dz = getZ() - st.getZ();
            if (dx * dx + dz * dz < (StadiumEntity.BOWL_R - 0.5) * (StadiumEntity.BOWL_R - 0.5)) st.addScar(getX(), getZ(), r);
            return;
        }
    }

    public float abilityAge(float partial) {
        return clientAbility == AB_NONE ? -1F : this.tickCount - clientAbilityStart + partial;
    }

    private static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1.0F, 0.82F, 0.3F), 1.4F);
    private static final DustParticleOptions LIME = new DustParticleOptions(new Vector3f(0.6F, 1.0F, 0.25F), 1.1F);
    private static final DustParticleOptions SAND = new DustParticleOptions(new Vector3f(0.92F, 0.78F, 0.5F), 1.6F);
    private static final DustParticleOptions STEAL = new DustParticleOptions(new Vector3f(0.9F, 0.05F, 0.2F), 1.3F);
    private static final DustParticleOptions RED = new DustParticleOptions(new Vector3f(1.0F, 0.15F, 0.1F), 0.9F);

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport) {
        lerpX = x;
        lerpY = y;
        lerpZ = z;
        lerpSteps = 2;
        if (position().distanceToSqr(x, y, z) > 64) {
            setPos(x, y, z);
            lerpSteps = 0;
        }
    }

    private void clientTick() {
        if (lerpSteps > 0) {
            setPos(getX() + (lerpX - getX()) / lerpSteps, getY() + (lerpY - getY()) / lerpSteps, getZ() + (lerpZ - getZ()) / lerpSteps);
            lerpSteps--;
        }
        if (!soundStarted) {
            soundStarted = true;
            com.krolasyon.beyblade.client.ClientHooks.startSpinSound(this);
        }
        float ratio = spinRatio();
        int state = getState();
        spinAngleO = spinAngle;
        precessO = precess;
        float speed;
        if (state == STATE_DEFEATED) speed = Math.max(0F, 34F * (1F - (this.tickCount - clientStateStart) / 40F));
        else if (state == STATE_RECALL) speed = 70F;
        else speed = spinSpeedFor(ratio);
        int ab = getAbility();
        float at = abilityAge(0);
        float s2 = clientSpinSpeed(ab, at, speed);
        if (s2 < 0) speed = s2 * 1.45F;
        else if (s2 != speed) speed = Math.max(speed, s2 * 1.45F);
        spinAngle += speed;
        precess += state == STATE_DEFEATED ? Math.max(0F, 22F * (1F - (this.tickCount - clientStateStart) / 50F)) : 10F + (1F - ratio) * 22F;
        if (spinAngle > 36000F) { spinAngle -= 36000F; spinAngleO -= 36000F; }
        if (precess > 36000F) { precess -= 36000F; precessO -= 36000F; }

        if (!trailInit) {
            trailInit = true;
            for (int i = 0; i < trailX.length; i++) { trailX[i] = getX(); trailY[i] = getY(); trailZ[i] = getZ(); }
        }
        for (int i = trailX.length - 1; i > 0; i--) { trailX[i] = trailX[i - 1]; trailY[i] = trailY[i - 1]; trailZ[i] = trailZ[i - 1]; }
        trailX[0] = getX(); trailY[0] = getY(); trailZ[0] = getZ();

        Level lv = this.level();
        double vx = getX() - this.xo, vz = getZ() - this.zo;
        double sp = Math.sqrt(vx * vx + vz * vz);
        if (state == STATE_BATTLE && onGround() && sp > 0.2 && this.random.nextInt(2) == 0) {
            Px.add(lv, ParticleTypes.ELECTRIC_SPARK, getX() - vx * 2, getY() + 0.03, getZ() - vz * 2, -vx * 0.5, 0.05, -vz * 0.5);
        }
        if (state == STATE_BATTLE && ratio < 0.2F && this.random.nextInt(6) == 0) {
            Px.add(lv, ParticleTypes.SMOKE, getX(), getY() + 0.5, getZ(), 0, 0.02, 0);
        }
        if (isRival() && state == STATE_BATTLE && this.random.nextInt(3) == 0) {
            double a = this.random.nextDouble() * Math.PI * 2;
            Px.add(lv, RED, getX() + Math.cos(a) * 0.5, getY() + 0.5, getZ() + Math.sin(a) * 0.5, 0, 0.02, 0);
        }
        if (state == STATE_RECALL) {
            Px.add(lv, LIME, getX(), getY() + 0.3, getZ(), 0, 0, 0);
            Px.add(lv, ParticleTypes.END_ROD, getX(), getY() + 0.3, getZ(), 0, 0, 0);
        }
        if (isShielded()) clientBuffParticles(lv);
        if (ab == AB_NONE) return;
        clientAbilityParticles(ab, at, lv);
    }

    protected void clientBuffParticles(Level lv) {
        if (this.random.nextInt(3) == 0) {
            double a = this.random.nextDouble() * Math.PI * 2, b = this.random.nextDouble() * Math.PI * 0.5;
            Px.add(lv, ParticleTypes.END_ROD, getX() + Math.cos(a) * Math.cos(b) * 1.3, getY() + 0.3 + Math.sin(b) * 1.3,
                    getZ() + Math.sin(a) * Math.cos(b) * 1.3, 0, 0, 0);
        }
    }

    protected void clientAbilityParticles(int ab, float at, Level lv) {
        switch (ab) {
            case AB_SONIC_WAVE -> {
                if (this.random.nextInt(2) == 0)
                    Px.add(lv, ParticleTypes.NOTE, getX() + (this.random.nextDouble() - 0.5) * 2, getY() + 0.6 + this.random.nextDouble(),
                            getZ() + (this.random.nextDouble() - 0.5) * 2, 0.25 + this.random.nextDouble() * 0.2, 0, 0);
            }
            case AB_INFERNO -> {
                if (at < INF_TORNADO) {
                    for (int i = 0; i < 4; i++) {
                        double a = this.random.nextDouble() * Math.PI * 2, r = 1.0 + this.random.nextDouble() * 3.0;
                        double x = getX() + Math.cos(a) * r, z = getZ() + Math.sin(a) * r;
                        Px.add(lv, SAND, x, getY() + 0.1, z, -Math.cos(a) * 0.25 - Math.sin(a) * 0.2, 0.05, -Math.sin(a) * 0.25 + Math.cos(a) * 0.2);
                    }
                    Px.add(lv, ParticleTypes.FLAME, getX(), getY() + 0.1, getZ(), (this.random.nextDouble() - 0.5) * 0.2, 0.05, (this.random.nextDouble() - 0.5) * 0.2);
                } else if (at < INF_DIVE + 2) {
                    for (int i = 0; i < 6; i++) {
                        double a = this.random.nextDouble() * Math.PI * 2, h = this.random.nextDouble() * INF_HEIGHT;
                        double r = 1.4 + h * 0.13;
                        double x = abilityX + Math.cos(a) * r, z = abilityZ + Math.sin(a) * r;
                        ParticleOptions p = i % 3 == 0 ? ParticleTypes.FLAME : (i % 3 == 1 ? SAND : ParticleTypes.SMALL_FLAME);
                        Px.add(lv, p, x, abilityY + h, z, Math.sin(a) * 0.35, 0.12, -Math.cos(a) * 0.35);
                    }
                }
            }
            case AB_BUSTER -> {
                if (at >= BUS_CHARGE && at < BUS_FIRE) {
                    for (int i = 0; i < 3; i++) {
                        double a = this.random.nextDouble() * Math.PI * 2, b = (this.random.nextDouble() - 0.5) * Math.PI;
                        double r = 2.2;
                        double ox = Math.cos(a) * Math.cos(b) * r, oy = Math.sin(b) * r, oz = Math.sin(a) * Math.cos(b) * r;
                        Px.add(lv, ParticleTypes.END_ROD, getX() + ox, getY() + 0.4 + oy, getZ() + oz, -ox * 0.1, -oy * 0.1, -oz * 0.1);
                    }
                }
            }
            case AB_JUDGMENT -> {
                for (int i = 0; i < 2; i++) {
                    double a = this.random.nextDouble() * Math.PI * 2, r = this.random.nextDouble() * 6;
                    Px.add(lv, GOLD, abilityX + Math.cos(a) * r, abilityY + 0.1, abilityZ + Math.sin(a) * r, 0, 0.06, 0);
                }
                if (at >= JUD_LINK && at < JUD_STRIKE && this.tickCount % 2 == 0) {
                    for (Entity e : enemiesWithin(JUD_RANGE)) {
                        double f = this.random.nextDouble();
                        double x = Mth.lerp(f, e.getX(), getX()), y = Mth.lerp(f, e.getY() + e.getBbHeight() * 0.5, getY() + 0.4), z = Mth.lerp(f, e.getZ(), getZ());
                        Px.add(lv, GOLD, x, y, z, (getX() - e.getX()) * 0.03, (getY() - e.getY()) * 0.03, (getZ() - e.getZ()) * 0.03);
                    }
                }
            }
            default -> {}
        }
    }

    // ---------------- persistence ----------------
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) this.entityData.set(DATA_OWNER, Optional.of(tag.getUUID("Owner")));
        if (tag.contains("MaxSpin")) this.entityData.set(DATA_MAX_SPIN, tag.getFloat("MaxSpin"));
        if (tag.contains("Spin")) this.entityData.set(DATA_SPIN, tag.getFloat("Spin"));
        int st = tag.getInt("State");
        this.entityData.set(DATA_STATE, st == STATE_RECALL ? STATE_RECALL : (st == STATE_DEFEATED ? STATE_DEFEATED : STATE_BATTLE));
        if (st == STATE_RECALL) { this.noPhysics = true; setNoGravity(true); }
        frozen = tag.getBoolean("Frozen");
        if (tag.contains("Level")) this.entityData.set(DATA_LEVEL, Mth.clamp(tag.getInt("Level"), 1, MAX_LEVEL));
        this.entityData.set(DATA_XP, tag.getInt("Xp"));
        if (tag.contains("Mode")) this.entityData.set(DATA_MODE, Mth.clamp(tag.getInt("Mode"), 0, MODE_COUNT - 1));
        this.entityData.set(DATA_ORDER, Mth.clamp(tag.getInt("Order"), 0, ORDER_COUNT - 1));
        this.entityData.set(DATA_SPIRIT, Mth.clamp(tag.getInt("Spirit"), 0, SPIRIT_MAX));
        if (tag.contains("Level") && !tag.contains("MaxSpin")) this.entityData.set(DATA_MAX_SPIN, maxSpinFor(getLevel()));
        if (tag.contains("Item")) stored = com.krolasyon.beyblade.compat.V.loadStack(level(), tag.getCompound("Item"));
        refreshLoadout();
        if (tag.contains("HomeX")) home = new Vec3(tag.getDouble("HomeX"), tag.getDouble("HomeY"), tag.getDouble("HomeZ"));
        long now = this.level() == null ? 0 : this.level().getGameTime();
        for (int i = 0; i < ABILITY_COUNT; i++) {
            int left = tag.getInt("Cooldown" + (i + 1));
            if (left > 0) this.entityData.set(DATA_COOLDOWN[i], (int) (now + left));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        UUID o = getOwnerId();
        if (o != null) tag.putUUID("Owner", o);
        tag.putFloat("Spin", getSpin());
        tag.putFloat("MaxSpin", getMaxSpin());
        tag.putInt("State", getState());
        if (frozen) tag.putBoolean("Frozen", true);
        tag.putInt("Level", getLevel());
        tag.putInt("Xp", getXp());
        tag.putInt("Mode", getMode());
        tag.putInt("Order", getOrder());
        tag.putInt("Spirit", getSpirit());
        if (!stored.isEmpty()) tag.put("Item", com.krolasyon.beyblade.compat.V.saveStack(level(), stored));
        if (home != null) {
            tag.putDouble("HomeX", home.x);
            tag.putDouble("HomeY", home.y);
            tag.putDouble("HomeZ", home.z);
        }
        for (int i = 1; i <= ABILITY_COUNT; i++) tag.putInt("Cooldown" + i, cooldownRemaining(i));
    }


    // ---------------- parts (customisation) ----------------
    @Nullable private com.krolasyon.beyblade.parts.Loadout loadout;
    private String clientPartsKey;
    private com.krolasyon.beyblade.parts.Loadout clientLoadout;

    /** server: re-read the parts from the launched item */
    public void refreshLoadout() {
        loadout = com.krolasyon.beyblade.parts.Loadout.of(stored.isEmpty() ? defaultItem() : stored);
        this.entityData.set(DATA_PARTS, loadout.encode());
    }

    /** the assembled parts (server: from the item, client: from the synced data) */
    public com.krolasyon.beyblade.parts.Loadout parts() {
        if (this.level().isClientSide()) {
            String k = this.entityData.get(DATA_PARTS);
            if (clientLoadout == null || !k.equals(clientPartsKey)) {
                clientPartsKey = k;
                clientLoadout = com.krolasyon.beyblade.parts.Loadout.decode(defaultItem().getItem() instanceof com.krolasyon.beyblade.item.LibraItem li ? li.kind() : kindId(), k);
            }
            return clientLoadout;
        }
        if (loadout == null) refreshLoadout();
        return loadout;
    }

    /** stamina parts give a little more spin to start with */
    public float partsSpinMul() { return 0.92F + 0.08F * com.krolasyon.beyblade.parts.Loadout.mul(parts().stamina()); }

    /** taller track by at least 2 mm (Twin Height counts as tall against anything up to 14.5) */
    public boolean heightEdge(BeybladeEntity o) {
        int a = parts().height(), b = o.parts().height();
        if (parts().trackTrait() == com.krolasyon.beyblade.parts.BeyPart.Trait.TWIN_HEIGHT && b <= 145) return true;
        return a - b >= 20;
    }

    /** tracks built to lift the other beyblade when striking from above */
    public boolean partsUpperKick() {
        var t = parts().trackTrait();
        return t == com.krolasyon.beyblade.parts.BeyPart.Trait.UPPER_WING || t == com.krolasyon.beyblade.parts.BeyPart.Trait.CLAW
                || t == com.krolasyon.beyblade.parts.BeyPart.Trait.GRAVITY;
    }

    /** clash damage from weight and height against this opponent */
    public float partsClashMul(BeybladeEntity o) {
        float m = (float) Math.pow(Math.max(20F, parts().weight()) / Math.max(20F, o.parts().weight()), 0.35);
        if (heightEdge(o)) {
            var t = parts().trackTrait();
            m *= t == com.krolasyon.beyblade.parts.BeyPart.Trait.GRAVITY ? 1.2F : t == com.krolasyon.beyblade.parts.BeyPart.Trait.CLAW ? 1.18F : 1.1F;
        }
        return Mth.clamp(m, 0.7F, 1.4F);
    }

    /** test hook: lock two beyblades together right now */
    public void debugLock(BeybladeEntity o, int ticks) {
        lockTicks = ticks;
        o.lockTicks = ticks + 5;
        lockPartner = o;
        o.lockPartner = this;
    }

    /** test hook: hold still (as the Frozen NBT flag) */
    public void setFrozen(boolean f) { frozen = f; }

    /** test hook: force an ability regardless of cooldown */
    public void debugForceAbility(int id) {
        this.entityData.set(DATA_COOLDOWN[id - 1], 0);
        pendingAbility = 0;
        if (id != buffAbility() && getAbility() != AB_NONE) endAbility();
        tryAbility(id, false);
    }
}
