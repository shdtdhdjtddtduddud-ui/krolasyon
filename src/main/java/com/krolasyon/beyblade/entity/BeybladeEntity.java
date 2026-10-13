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
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkHooks;
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
    public static final float MAX_SPIN = 3000F;

    public static final int STATE_BATTLE = 0, STATE_DEFEATED = 1, STATE_RECALL = 2;

    public static final int AB_NONE = 0, AB_SONIC_WAVE = 1, AB_SHIELD = 2, AB_INFERNO = 3, AB_BUSTER = 4, AB_JUDGMENT = 5;
    public static final int ABILITY_COUNT = 5;
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

    public static final byte EV_HIT = 60, EV_HEAVY_HIT = 61, EV_LAND = 62, EV_SHIELD_HIT = 63, EV_WALL = 64, EV_IMPACT = 65, EV_STRIKE = 66;

    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER = SynchedEntityData.defineId(BeybladeEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Float> DATA_SPIN = SynchedEntityData.defineId(BeybladeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_MAX_SPIN = SynchedEntityData.defineId(BeybladeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_STATE = SynchedEntityData.defineId(BeybladeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ABILITY = SynchedEntityData.defineId(BeybladeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TARGET = SynchedEntityData.defineId(BeybladeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SHIELD_END = SynchedEntityData.defineId(BeybladeEntity.class, EntityDataSerializers.INT);
    @SuppressWarnings("unchecked")
    private static final EntityDataAccessor<Integer>[] DATA_COOLDOWN = new EntityDataAccessor[ABILITY_COUNT];

    static {
        for (int i = 0; i < ABILITY_COUNT; i++) DATA_COOLDOWN[i] = SynchedEntityData.defineId(BeybladeEntity.class, EntityDataSerializers.INT);
    }

    // ---------------- server state ----------------
    private ItemStack stored = ItemStack.EMPTY;
    protected int abilityTicks;
    protected double abilityBaseY;
    protected boolean diving;
    private int dashCooldown = 20, dashTicks;
    private int ownerMissing;
    private int stateTicks;
    private int targetRefresh;
    private int wallSoundCooldown, clashCooldown;
    @Nullable private StadiumEntity stadium;
    private boolean inBowl;
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
        b.entityData.set(DATA_MAX_SPIN, MAX_SPIN);
        b.entityData.set(DATA_SPIN, MAX_SPIN * Mth.clamp(power, 0.35F, 1F));
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
        float decay = 0.2F * (isInWater() ? 4F : 1F) * (isShielded() ? 1.15F : 1F);
        setSpin(getSpin() - decay);
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
        if (ab != AB_NONE) {
            abilityTicks++;
            controlled = tickAbility(ab, abilityTicks, owner);
            if (abilityTicks >= duration(ab)) endAbility();
        } else if (isRival() && rivalAbilities) {
            rivalUseAbilities();
        }
        if (isShielded()) tickBuff();
        if (stunTicks > 0) {
            stunTicks--;
            controlled = true;
            setDeltaMovement(getDeltaMovement().multiply(0.6, 1, 0.6));
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
        double mul = (0.55 + 0.45 * ratio) * (onGround() ? 1.0 : 0.25) * buffSpeedMul();
        Vec3 center;
        double orbit;
        if (target != null) {
            center = target.position();
            orbit = 2.4;
            if (--dashCooldown <= 0 && onGround()) {
                Vec3 d = flat(center.subtract(position()));
                if (d.lengthSqr() > 1.0E-4) {
                    v = v.add(d.normalize().scale(0.66 * (0.6 + 0.4 * ratio) * buffSpeedMul()));
                    dashTicks = 9;
                }
                dashCooldown = (int) ((16 + this.random.nextInt(24)) / buffSpeedMul());
            }
        } else {
            center = owner != null ? owner.position() : (home != null ? home : position());
            orbit = 2.8;
            if (stadium != null && inBowl) {
                // inside a stadium: circle its centre instead of trying to climb out to the owner
                center = stadium.position();
                orbit = 3.2;
            }
        }
        if (dashTicks > 0) {
            dashTicks--;
        } else {
            Vec3 to = flat(center.subtract(position()));
            double dist = to.length();
            if (dist > 1.0E-3) {
                Vec3 dir = to.scale(1.0 / dist);
                Vec3 tangent = new Vec3(dir.z, 0, -dir.x);
                double radial = Mth.clamp((dist - orbit) * 0.09, -0.14, dist > 12 ? 0.55 : 0.28);
                Vec3 desired = tangent.scale(0.25).add(dir.scale(radial)).scale(mul);
                v = v.add((desired.x - v.x) * 0.12, 0, (desired.z - v.z) * 0.12);
            }
        }
        setDeltaMovement(v);
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
                ((ServerLevel) this.level()).sendParticles(ParticleTypes.ELECTRIC_SPARK, getX() + before.x * 3, getY() + 0.3, getZ() + before.z * 3, 8, 0.1, 0.1, 0.1, 0.3);
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
        if (fight && (clashCooldown > 0 || o.clashCooldown > 0) && rel >= 0) return;
        if (rel >= 0 && !fight) return;
        if (rel > -0.02 && fight) rel = -0.02;
        double e = fight ? 0.95 : 0.4;
        double j = -(1 + e) * rel * 0.5;
        boolean opposite = spinDir() != o.spinDir();
        double kick = fight ? (opposite ? 0.08 : 0.13) : 0.0;
        // same-direction tops deflect sideways at contact; opposite spins grind into each other (spin steal)
        double tx = -nz, tz = nx, side = fight && !opposite ? 0.07 * spinDir() : 0.0;
        va = va.add(-nx * (j + kick) + tx * side, fight ? 0.05 : 0, -nz * (j + kick) + tz * side);
        vb = vb.add(nx * (j + kick) - tx * side, fight ? 0.05 : 0, nz * (j + kick) - tz * side);
        setDeltaMovement(va);
        o.setDeltaMovement(vb);
        if (!fight || clashCooldown > 0 || o.clashCooldown > 0) return;
        clashCooldown = 4;
        o.clashCooldown = 4;
        double impact = -rel;
        boolean heavy = impact > 0.5;
        float base = (float) (10 + 150 * impact);
        float toO = base * (0.6F + 0.6F * spinRatio()) * buffAttackMul();
        float toMe = base * (0.6F + 0.6F * o.spinRatio()) * o.buffAttackMul();
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
        ServerLevel sl = (ServerLevel) this.level();
        if (opposite) {
            // the left-spinning top absorbs part of the right-spinner's rotation
            BeybladeEntity thief = spinDir() < 0 ? this : o;
            thief.healSpin((thief == this ? toO : toMe) * 0.4F);
            sl.sendParticles(STEAL, thief.getX(), thief.getY() + 0.4, thief.getZ(), 10, 0.35, 0.2, 0.35, 0);
        }
        double cx = getX() + nx * RADIUS, cy = getY() + 0.38, cz = getZ() + nz * RADIUS;
        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, cx, cy, cz, heavy ? 28 : 12, 0.08, 0.08, 0.08, heavy ? 0.7 : 0.45);
        sl.sendParticles(ParticleTypes.CRIT, cx, cy, cz, heavy ? 14 : 6, 0.05, 0.05, 0.05, 0.6);
        if (heavy) {
            sl.sendParticles(ParticleTypes.END_ROD, cx, cy, cz, 14, 0.1, 0.1, 0.1, 0.25);
            sl.sendParticles(ParticleTypes.FIREWORK, cx, cy, cz, 10, 0.05, 0.05, 0.05, 0.25);
        }
        this.level().playSound(null, cx, cy, cz, heavy ? ModSounds.CLASH_HEAVY.get() : ModSounds.CLASH.get(), SoundSource.NEUTRAL,
                heavy ? 1.2F : 0.9F, 0.85F + this.random.nextFloat() * 0.35F);
        this.level().broadcastEntityEvent(this, heavy ? EV_HEAVY_HIT : EV_HIT);
        this.level().broadcastEntityEvent(o, heavy ? EV_HEAVY_HIT : EV_HIT);
        if (o.target == null) o.target = this;
        if (target == null) target = o;
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
                ((ServerLevel) this.level()).sendParticles(ParticleTypes.ELECTRIC_SPARK, getX() + nx * RADIUS, getY() + 0.35, getZ() + nz * RADIUS, 10, 0.1, 0.1, 0.1, 0.4);
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
        setSpin(getSpin() - amount * incomingMul());
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
        if (id < 1 || id > ABILITY_COUNT || getState() != STATE_BATTLE) return false;
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
        if (getAbility() != AB_NONE) {
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
        abilityTicks = 0;
        abilityBaseY = getY();
        diving = false;
        for (Set<Integer> s : waveHits) s.clear();
        this.entityData.set(DATA_ABILITY, id);
        announce(owner, id);
        return true;
    }

    private void announce(@Nullable Player owner, int id) {
        if (owner != null) owner.displayClientMessage(Component.translatable("msg.librabeyblade.ability." + kindId(), Component.translatable(abilityKey(id))), true);
    }

    private void startCooldown(int id) {
        this.entityData.set(DATA_COOLDOWN[id - 1], (int) (this.level().getGameTime() + cooldown(id)));
    }

    protected void endAbility() {
        if (getAbility() != AB_NONE) this.entityData.set(DATA_ABILITY, AB_NONE);
        setNoGravity(false);
        diving = false;
        abilityTicks = 0;
    }

    private void rivalUseAbilities() {
        if (target == null || target.distanceToSqr(this) > 12 * 12 || this.random.nextInt(45) != 0) return;
        int id = 1 + this.random.nextInt(ABILITY_COUNT);
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
                        sl.sendParticles(ParticleTypes.SONIC_BOOM, getX(), getY() + 0.4, getZ(), 1, 0, 0, 0, 0);
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
                            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, b.getX(), b.getY() + 0.4, b.getZ(), 8, 0.2, 0.2, 0.2, 0.3);
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
                        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), getX(), getY() + 0.1, getZ(), 10, 0.5, 0.05, 0.5, 0.2);
                    return true;
                }
                if (t < INF_DIVE) {
                    if (t == INF_TORNADO) {
                        play(ModSounds.INFERNO_TORNADO.get(), 2.0F, 1.0F);
                        sl.sendParticles(ParticleTypes.LAVA, getX(), getY() + 0.3, getZ(), 10, 0.8, 0.2, 0.8, 0);
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
                    if (t % 5 == 0) sl.sendParticles(ParticleTypes.SONIC_BOOM, to.x, to.y, to.z, 1, 0, 0, 0, 0);
                    sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, to.x, to.y, to.z, 6, 0.3, 0.3, 0.3, 0.4);
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
                        sl.sendParticles(ParticleTypes.END_ROD, e.getX(), e.getY() + 1, e.getZ(), 30, 0.3, 1.5, 0.3, 0.08);
                        sl.sendParticles(ParticleTypes.END_ROD, e.getX(), e.getY() + 0.5, e.getZ(), 14, 0.1, 0.1, 0.1, 0.25);
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
        sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.5, getZ(), 14, 0.1, 0.1, 0.1, 0.25);
        sl.sendParticles(ParticleTypes.LAVA, getX(), getY() + 0.3, getZ(), 20, 1.5, 0.2, 1.5, 0);
        for (int i = 0; i < 48; i++) {
            double a = i / 48.0 * Math.PI * 2;
            sl.sendParticles(ParticleTypes.FLAME, getX() + Math.cos(a) * 0.8, getY() + 0.15, getZ() + Math.sin(a) * 0.8, 0,
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

    /** curved bowl floor, walls and pockets of a nearby {@link StadiumEntity} */
    private void stadiumPhysics() {
        List<StadiumEntity> list = this.level().getEntitiesOfClass(StadiumEntity.class, getBoundingBox().inflate(11, 4, 11));
        if (list.isEmpty()) {
            stadium = null;
            inBowl = false;
            return;
        }
        StadiumEntity st = list.get(0);
        double dx = getX() - st.getX(), dz = getZ() - st.getZ();
        double r = Math.sqrt(dx * dx + dz * dz);
        double rel = getY() - st.getY();
        if (r > StadiumEntity.LEDGE_R + 1.5 || rel < -1.5 || rel > 7) {
            stadium = null;
            inBowl = false;
            return;
        }
        stadium = st;
        double nx = r < 1.0E-4 ? 1 : dx / r, nz = r < 1.0E-4 ? 0 : dz / r;
        Vec3 v = getDeltaMovement();
        boolean gap = StadiumEntity.isGap(Math.atan2(dz, dx));
        double wallR = StadiumEntity.WALL_IN - RADIUS;
        // only a hard knock carries a top out through a pocket; slower ones roll back off the pocket's rim
        double vrOut = v.x * nx + v.z * nz;
        boolean blocked = !gap || (inBowl && vrOut < 0.5);
        // striped walls
        if (blocked && rel < StadiumEntity.WALL_TOP && r > wallR && r < StadiumEntity.WALL_OUT + RADIUS) {
            double vr = v.x * nx + v.z * nz;
            if (inBowl) {
                setPos(st.getX() + nx * wallR, getY(), st.getZ() + nz * wallR);
                r = wallR;
                if (vr > 0) {
                    v = v.subtract(nx * vr * 1.75, 0, nz * vr * 1.75);
                    if (vr > 0.12 && wallSoundCooldown == 0 && !gap) {
                        wallSoundCooldown = 6;
                        play(ModSounds.WALL_HIT.get(), 0.9F, 0.9F + this.random.nextFloat() * 0.3F);
                        ((ServerLevel) this.level()).sendParticles(ParticleTypes.ELECTRIC_SPARK, getX() + nx * RADIUS, getY() + 0.35, getZ() + nz * RADIUS, 10, 0.1, 0.1, 0.1, 0.35);
                        setSpin(getSpin() - 3F);
                        this.level().broadcastEntityEvent(this, EV_WALL);
                    }
                }
            } else {
                double outR = StadiumEntity.WALL_OUT + RADIUS;
                setPos(st.getX() + nx * outR, getY(), st.getZ() + nz * outR);
                r = outR;
                if (vr < 0) v = v.subtract(nx * vr * 1.6, 0, nz * vr * 1.6);
            }
        }
        // the bowl surface
        double surf = StadiumEntity.surface(r);
        if (surf >= 0 && getState() != STATE_RECALL) {
            double fy = st.getY() + surf;
            if (getY() <= fy + 0.06 && rel > -1.2) {
                setPos(getX(), fy, getZ());
                if (v.y < 0) v = new Vec3(v.x, v.y < -0.35 ? -v.y * 0.25 : 0, v.z);
                setOnGround(true);
                double sl = StadiumEntity.slope(r);
                // gravity along the curved floor pulls tops toward the centre; same rolling friction as the ground
                v = new Vec3((v.x - nx * sl * 0.045) * 0.965, v.y, (v.z - nz * sl * 0.045) * 0.965);
            }
        }
        boolean nowIn = r < StadiumEntity.WALL_IN;
        // flew out through a pocket: over finish
        if (inBowl && !nowIn && gap && getState() == STATE_BATTLE) {
            Player owner = getOwner();
            if (owner != null) owner.displayClientMessage(Component.translatable("msg.librabeyblade.over_finish"), true);
            play(ModSounds.CLASH_HEAVY.get(), 1.0F, 0.6F);
            setDeltaMovement(v);
            defeat();
            inBowl = false;
            return;
        }
        inBowl = nowIn && rel < StadiumEntity.WALL_TOP + 0.5;
        setDeltaMovement(v);
    }

    private void defeat() {
        endAbility();
        setSpin(0F);
        setState(STATE_DEFEATED);
        play(ModSounds.DEFEAT.get(), 1.2F, 1.0F);
        Player owner = getOwner();
        if (owner != null) owner.displayClientMessage(Component.translatable("msg.librabeyblade.defeated"), true);
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
            ((ServerLevel) this.level()).sendParticles(ParticleTypes.POOF, getX(), getY() + 0.3, getZ(), 12, 0.3, 0.2, 0.3, 0.02);
            discard();
        }
    }

    private void giveTo(Player p) {
        if (isRival()) return;
        ItemStack s = stored.isEmpty() ? defaultItem() : stored.copy();
        stored = ItemStack.EMPTY;
        if (!p.getInventory().add(s)) p.drop(s, false);
    }

    private void dropStored() {
        if (isRival()) return;
        ItemStack s = stored.isEmpty() ? defaultItem() : stored.copy();
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
        if (clientAbility != AB_NONE || getState() == STATE_RECALL) return getBoundingBox().inflate(26, 14, 26);
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
            case EV_HIT, EV_WALL -> hitTime = this.tickCount;
            case EV_HEAVY_HIT -> { hitTime = this.tickCount; heavyHitTime = this.tickCount; }
            case EV_LAND -> {
                landTime = this.tickCount;
                for (int i = 0; i < 20; i++) {
                    double a = i / 20.0 * Math.PI * 2;
                    this.level().addParticle(ParticleTypes.CLOUD, getX() + Math.cos(a) * 0.4, getY() + 0.05, getZ() + Math.sin(a) * 0.4,
                            Math.cos(a) * 0.12, 0.01, Math.sin(a) * 0.12);
                }
            }
            case EV_SHIELD_HIT -> shieldHitTime = this.tickCount;
            case EV_IMPACT -> impactTime = this.tickCount;
            case EV_STRIKE -> strikeTime = this.tickCount;
            default -> super.handleEntityEvent(id);
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

    private void clientTick() {
        if (!soundStarted) {
            soundStarted = true;
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.krolasyon.beyblade.client.ClientHooks.startSpinSound(this));
        }
        float ratio = spinRatio();
        int state = getState();
        spinAngleO = spinAngle;
        precessO = precess;
        float speed;
        if (state == STATE_DEFEATED) speed = Math.max(0F, 34F * (1F - (this.tickCount - clientStateStart) / 40F));
        else if (state == STATE_RECALL) speed = 70F;
        else speed = 30F + 62F * ratio;
        int ab = getAbility();
        float at = abilityAge(0);
        speed = clientSpinSpeed(ab, at, speed);
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
            lv.addParticle(ParticleTypes.ELECTRIC_SPARK, getX() - vx * 2, getY() + 0.03, getZ() - vz * 2, -vx * 0.5, 0.05, -vz * 0.5);
        }
        if (state == STATE_BATTLE && ratio < 0.2F && this.random.nextInt(6) == 0) {
            lv.addParticle(ParticleTypes.SMOKE, getX(), getY() + 0.5, getZ(), 0, 0.02, 0);
        }
        if (isRival() && state == STATE_BATTLE && this.random.nextInt(3) == 0) {
            double a = this.random.nextDouble() * Math.PI * 2;
            lv.addParticle(RED, getX() + Math.cos(a) * 0.5, getY() + 0.5, getZ() + Math.sin(a) * 0.5, 0, 0.02, 0);
        }
        if (state == STATE_RECALL) {
            lv.addParticle(LIME, getX(), getY() + 0.3, getZ(), 0, 0, 0);
            lv.addParticle(ParticleTypes.END_ROD, getX(), getY() + 0.3, getZ(), 0, 0, 0);
        }
        if (isShielded()) clientBuffParticles(lv);
        if (ab == AB_NONE) return;
        clientAbilityParticles(ab, at, lv);
    }

    protected void clientBuffParticles(Level lv) {
        if (this.random.nextInt(3) == 0) {
            double a = this.random.nextDouble() * Math.PI * 2, b = this.random.nextDouble() * Math.PI * 0.5;
            lv.addParticle(ParticleTypes.END_ROD, getX() + Math.cos(a) * Math.cos(b) * 1.3, getY() + 0.3 + Math.sin(b) * 1.3,
                    getZ() + Math.sin(a) * Math.cos(b) * 1.3, 0, 0, 0);
        }
    }

    protected void clientAbilityParticles(int ab, float at, Level lv) {
        switch (ab) {
            case AB_SONIC_WAVE -> {
                if (this.random.nextInt(2) == 0)
                    lv.addParticle(ParticleTypes.NOTE, getX() + (this.random.nextDouble() - 0.5) * 2, getY() + 0.6 + this.random.nextDouble(),
                            getZ() + (this.random.nextDouble() - 0.5) * 2, 0.25 + this.random.nextDouble() * 0.2, 0, 0);
            }
            case AB_INFERNO -> {
                if (at < INF_TORNADO) {
                    for (int i = 0; i < 4; i++) {
                        double a = this.random.nextDouble() * Math.PI * 2, r = 1.0 + this.random.nextDouble() * 3.0;
                        double x = getX() + Math.cos(a) * r, z = getZ() + Math.sin(a) * r;
                        lv.addParticle(SAND, x, getY() + 0.1, z, -Math.cos(a) * 0.25 - Math.sin(a) * 0.2, 0.05, -Math.sin(a) * 0.25 + Math.cos(a) * 0.2);
                    }
                    lv.addParticle(ParticleTypes.FLAME, getX(), getY() + 0.1, getZ(), (this.random.nextDouble() - 0.5) * 0.2, 0.05, (this.random.nextDouble() - 0.5) * 0.2);
                } else if (at < INF_DIVE + 2) {
                    for (int i = 0; i < 6; i++) {
                        double a = this.random.nextDouble() * Math.PI * 2, h = this.random.nextDouble() * INF_HEIGHT;
                        double r = 1.4 + h * 0.13;
                        double x = abilityX + Math.cos(a) * r, z = abilityZ + Math.sin(a) * r;
                        ParticleOptions p = i % 3 == 0 ? ParticleTypes.FLAME : (i % 3 == 1 ? SAND : ParticleTypes.SMALL_FLAME);
                        lv.addParticle(p, x, abilityY + h, z, Math.sin(a) * 0.35, 0.12, -Math.cos(a) * 0.35);
                    }
                }
            }
            case AB_BUSTER -> {
                if (at >= BUS_CHARGE && at < BUS_FIRE) {
                    for (int i = 0; i < 3; i++) {
                        double a = this.random.nextDouble() * Math.PI * 2, b = (this.random.nextDouble() - 0.5) * Math.PI;
                        double r = 2.2;
                        double ox = Math.cos(a) * Math.cos(b) * r, oy = Math.sin(b) * r, oz = Math.sin(a) * Math.cos(b) * r;
                        lv.addParticle(ParticleTypes.END_ROD, getX() + ox, getY() + 0.4 + oy, getZ() + oz, -ox * 0.1, -oy * 0.1, -oz * 0.1);
                    }
                }
            }
            case AB_JUDGMENT -> {
                for (int i = 0; i < 2; i++) {
                    double a = this.random.nextDouble() * Math.PI * 2, r = this.random.nextDouble() * 6;
                    lv.addParticle(GOLD, abilityX + Math.cos(a) * r, abilityY + 0.1, abilityZ + Math.sin(a) * r, 0, 0.06, 0);
                }
                if (at >= JUD_LINK && at < JUD_STRIKE && this.tickCount % 2 == 0) {
                    for (Entity e : enemiesWithin(JUD_RANGE)) {
                        double f = this.random.nextDouble();
                        double x = Mth.lerp(f, e.getX(), getX()), y = Mth.lerp(f, e.getY() + e.getBbHeight() * 0.5, getY() + 0.4), z = Mth.lerp(f, e.getZ(), getZ());
                        lv.addParticle(GOLD, x, y, z, (getX() - e.getX()) * 0.03, (getY() - e.getY()) * 0.03, (getZ() - e.getZ()) * 0.03);
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
        if (tag.contains("Item")) stored = ItemStack.of(tag.getCompound("Item"));
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
        if (!stored.isEmpty()) tag.put("Item", stored.save(new CompoundTag()));
        if (home != null) {
            tag.putDouble("HomeX", home.x);
            tag.putDouble("HomeY", home.y);
            tag.putDouble("HomeZ", home.z);
        }
        for (int i = 1; i <= ABILITY_COUNT; i++) tag.putInt("Cooldown" + i, cooldownRemaining(i));
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    /** test hook: force an ability regardless of cooldown */
    public void debugForceAbility(int id) {
        this.entityData.set(DATA_COOLDOWN[id - 1], 0);
        if (id != buffAbility() && getAbility() != AB_NONE) endAbility();
        tryAbility(id, false);
    }
}
