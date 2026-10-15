package com.krolasyon.alien.entity;

import com.krolasyon.alien.AlienConfig;
import com.krolasyon.alien.ai.FlashlightTracker;
import com.krolasyon.alien.ai.HidingManager;
import com.krolasyon.alien.ai.NoiseSystem;
import com.krolasyon.alien.ai.VentNetwork;
import com.krolasyon.alien.block.CeilingLightBlock;
import com.krolasyon.alien.block.LockerBlock;
import com.krolasyon.alien.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.*;

/**
 * The perfect organism. It can not be killed, only scared off with fire. It hunts with sight (light, stance, movement
 * and distance matter), hearing (every noise in the station is published to {@link NoiseSystem}), travels unseen through
 * vents, searches rooms methodically, sniffs lockers and learns from the players' tricks.
 * <p>
 * A built in "director" keeps it near the players without letting it cheat: it only ever gets a vague hint of where
 * they are, and when tension stayed high for too long it backs off into the vents for a while.
 */
public class XenomorphEntity extends Monster {
    public enum State { ROAM, INVESTIGATE, SEARCH, LOCKER_CHECK, ALERT, CHASE, KILL, FLEE, VENT_ENTER, IN_VENT, VENT_EXIT, BACKSTAGE, PAUSE }

    private static final EntityDataAccessor<Byte> DATA_STATE = SynchedEntityData.defineId(XenomorphEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> DATA_RUNNING = SynchedEntityData.defineId(XenomorphEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_HIDDEN = SynchedEntityData.defineId(XenomorphEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_KILL = SynchedEntityData.defineId(XenomorphEntity.class, EntityDataSerializers.INT);

    public static final int ANIM_SEARCH = 0, ANIM_HISS = 1, ANIM_ATTACK = 2, ANIM_KILL = 3, ANIM_VENT_ENTER = 4,
            ANIM_VENT_EXIT = 5, ANIM_FLINCH = 6, ANIM_LOCKER = 7;
    private static final byte EVENT_ANIM = 100;

    private static final double STALK = 0.78D, SEARCH_SPEED = 0.92D, RUN = 1.32D;
    private static final int KILL_LEN = 48, KILL_STRIKE = 32;

    // ------------------------------------------------------------------ client animation state
    public int clientAnimId = -1;
    public float clientAnimStart;
    public float runBlend, prevRunBlend;

    // ------------------------------------------------------------------ server brain
    private State state = State.ROAM;
    private int stateTicks;
    private int stateLen;
    @Nullable private UUID targetId;
    @Nullable private Vec3 goal;
    @Nullable private Vec3 lastSeen;
    private Vec3 lastSeenVel = Vec3.ZERO;
    private int lostTicks;
    private int noPathTicks;
    private long lastNoise;
    private final Map<UUID, Float> detection = new HashMap<>();
    private final Map<UUID, Vec3> prevPlayerPos = new HashMap<>();
    private final ArrayDeque<Vec3> searchPoints = new ArrayDeque<>();
    @Nullable private Vec3 searchCenter;
    private int searchPause;
    @Nullable private BlockPos lockerPos;
    private boolean lockerKnown;
    private int lockerSuspicion;
    private final Set<Long> checkedLockers = new HashSet<>();
    @Nullable private BlockPos ventFrom, ventTo;
    @Nullable private State afterVent;
    @Nullable private Vec3 afterVentGoal;
    private int backstageTicks;
    private float menace;
    private int farTicks;
    private int fireExposure;
    @Nullable private UUID fleeFrom;
    private int nextGrowl = 100;
    private int repath;

    // learning (persisted)
    private int flameHits;
    private int lockerFinds;
    private int lockerHides;
    private int lureCount;
    private final ArrayDeque<BlockPos> hotspots = new ArrayDeque<>();

    public XenomorphEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        this.setPersistenceRequired();
        this.setMaxUpStep(1.1F);
        this.setPathfindingMalus(BlockPathTypes.DOOR_WOOD_CLOSED, 0.0F);
        this.setPathfindingMalus(BlockPathTypes.WATER, 4.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 400.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.ATTACK_DAMAGE, 14.0D)
                .add(Attributes.ARMOR, 20.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        GroundPathNavigation nav = new GroundPathNavigation(this, level);
        nav.setCanOpenDoors(true);
        nav.setCanFloat(true);
        return nav;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new OpenDoorGoal(this, true));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_STATE, (byte) 0);
        this.entityData.define(DATA_RUNNING, false);
        this.entityData.define(DATA_HIDDEN, false);
        this.entityData.define(DATA_KILL, -1);
    }

    // ------------------------------------------------------------------ synced accessors
    public State getClientState() { return State.values()[Mth.clamp(this.entityData.get(DATA_STATE), 0, State.values().length - 1)]; }
    public boolean isRunning() { return this.entityData.get(DATA_RUNNING); }
    public boolean isHidden() { return this.entityData.get(DATA_HIDDEN); }
    public int getKillTargetId() { return this.entityData.get(DATA_KILL); }

    /** hunting states: used by the client for heartbeat and screen effects. */
    public boolean isHunting() {
        State s = getClientState();
        return s == State.CHASE || s == State.ALERT || s == State.KILL || s == State.SEARCH || s == State.LOCKER_CHECK || s == State.INVESTIGATE;
    }

    private void setHidden(boolean h) {
        this.entityData.set(DATA_HIDDEN, h);
        this.noPhysics = h;
        this.setNoGravity(h);
        this.setInvisible(h);
        if (h) this.getNavigation().stop();
    }

    private void setRunning(boolean b) { if (isRunning() != b) this.entityData.set(DATA_RUNNING, b); }

    private void playAnim(int id) { this.level().broadcastEntityEvent(this, (byte) (EVENT_ANIM + id)); }

    @Override
    public void handleEntityEvent(byte id) {
        if (id >= EVENT_ANIM && id < EVENT_ANIM + 20) {
            this.clientAnimId = id - EVENT_ANIM;
            this.clientAnimStart = this.tickCount;
        } else super.handleEntityEvent(id);
    }

    // ------------------------------------------------------------------ invulnerability / physics
    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurt(src, amount);
        if (src.getEntity() instanceof Player p && p.isCreative()) return super.hurt(src, amount);
        if (!this.level().isClientSide && src.getEntity() instanceof ServerPlayer p && !isHidden() && isValidPrey(p)) {
            // it can't be hurt, but it will not ignore being shot at
            if (state != State.KILL && state != State.FLEE && state != State.CHASE) {
                targetId = p.getUUID();
                lastSeen = p.position();
                enter(p.distanceTo(this) > 7 ? State.ALERT : State.CHASE);
            }
            this.playSound(ModSounds.XENO_SCREECH.get(), 1.2F, 1.1F);
        }
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource src) {
        return !src.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && !(src.getEntity() instanceof Player p && p.isCreative());
    }

    @Override
    public boolean causeFallDamage(float d, float m, DamageSource s) { return false; }

    @Override
    public boolean removeWhenFarAway(double d) { return false; }

    @Override
    public boolean isPushable() { return !isHidden() && state != State.KILL; }

    @Override
    public boolean isPickable() { return !isHidden() && super.isPickable(); }

    @Override
    public boolean canBeCollidedWith() { return false; }

    @Override
    public boolean isInvisible() { return isHidden() || super.isInvisible(); }

    @Override
    public boolean canChangeDimensions() { return false; }

    @Override
    public boolean doHurtTarget(Entity e) { return false; }

    @Override
    public AABB getBoundingBoxForCulling() { return this.getBoundingBox().inflate(2.5D, 1.0D, 2.5D); }

    @Override
    protected void doPush(Entity e) { if (!isHidden()) super.doPush(e); }

    // ------------------------------------------------------------------ sounds
    @Override
    protected SoundEvent getAmbientSound() { return null; }

    @Override
    protected SoundEvent getHurtSound(DamageSource s) { return ModSounds.XENO_FLINCH.get(); }

    @Override
    protected SoundEvent getDeathSound() { return ModSounds.XENO_SCREECH.get(); }

    @Override
    protected void playStepSound(BlockPos pos, BlockState block) {
        if (isHidden()) return;
        this.playSound(ModSounds.XENO_STEP.get(), isRunning() ? 0.85F : 0.4F, 0.9F + this.random.nextFloat() * 0.2F);
    }

    @Override
    protected float getSoundVolume() { return 1.3F; }

    // ------------------------------------------------------------------ tick
    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            prevRunBlend = runBlend;
            runBlend += ((isRunning() ? 1F : 0F) - runBlend) * 0.15F;
            if (isHidden()) return;
            if (getClientState() == State.KILL && this.random.nextInt(3) == 0) {
                Vec3 h = this.position().add(this.getLookAngle().scale(0.9)).add(0, 1.7, 0);
                this.level().addParticle(ParticleTypes.DRIPPING_WATER, h.x, h.y, h.z, 0, 0, 0);
            } else if (this.random.nextInt(40) == 0) {
                Vec3 h = this.position().add(this.getLookAngle().scale(0.7)).add(0, 1.55, 0);
                this.level().addParticle(ParticleTypes.DRIPPING_WATER, h.x, h.y, h.z, 0, 0, 0);
            }
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        ServerLevel level = (ServerLevel) this.level();
        stateTicks++;
        if (fireExposure > 0 && this.tickCount % 10 == 0) fireExposure = Math.max(0, fireExposure - 2);
        if (state != State.KILL && getKillTargetId() != -1) this.entityData.set(DATA_KILL, -1);

        if (level.getDifficulty() == Difficulty.PEACEFUL) { this.getNavigation().stop(); return; }

        director(level);
        if (unstick(level)) return;
        if (!isHidden() && state != State.KILL && state != State.VENT_EXIT && state != State.VENT_ENTER && state != State.FLEE) {
            perceive(level);
            hear(level);
        } else if (isHidden()) {
            hear(level);
        }

        switch (state) {
            case ROAM -> tickRoam(level);
            case INVESTIGATE -> tickInvestigate(level);
            case SEARCH -> tickSearch(level);
            case LOCKER_CHECK -> tickLocker(level);
            case ALERT -> tickAlert(level);
            case CHASE -> tickChase(level);
            case KILL -> tickKill(level);
            case FLEE -> tickFlee(level);
            case VENT_ENTER -> tickVentEnter(level);
            case IN_VENT -> tickInVent(level);
            case VENT_EXIT -> tickVentExit(level);
            case BACKSTAGE -> tickBackstage(level);
            case PAUSE -> { if (stateTicks >= stateLen) enter(State.ROAM); }
        }

        if (!isHidden() && (state == State.ROAM || state == State.SEARCH || state == State.INVESTIGATE) && --nextGrowl <= 0) {
            nextGrowl = 140 + this.random.nextInt(260);
            this.playSound(this.random.nextInt(3) == 0 ? ModSounds.XENO_SNIFF.get() : ModSounds.XENO_GROWL.get(), 0.9F, 0.9F + this.random.nextFloat() * 0.2F);
        }
    }

    private void enter(State s) {
        if (state == State.KILL && s != State.KILL) this.entityData.set(DATA_KILL, -1);
        state = s;
        stateTicks = 0;
        stateLen = 0;
        repath = 0;
        noPathTicks = 0;
        this.entityData.set(DATA_STATE, (byte) s.ordinal());
        setRunning(s == State.CHASE || s == State.FLEE);
        switch (s) {
            case ALERT -> {
                this.getNavigation().stop();
                stateLen = 18;
                playAnim(ANIM_HISS);
                this.playSound(ModSounds.XENO_SCREECH.get(), 1.6F, 1.0F);
                ServerPlayer t = target();
                if (t != null) t.playNotifySound(ModSounds.STINGER.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
            }
            case CHASE -> {
                ServerPlayer t = target();
                if (t != null && stateTicks == 0 && lostTicks > 200) t.playNotifySound(ModSounds.STINGER.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
                lostTicks = 0;
            }
            default -> {}
        }
    }

    // ------------------------------------------------------------------ helpers
    @Nullable
    private ServerPlayer target() {
        if (targetId == null) return null;
        Player p = this.level().getPlayerByUUID(targetId);
        return p instanceof ServerPlayer sp && isValidPrey(sp) ? sp : null;
    }

    public static boolean isValidPrey(Player p) {
        return p.isAlive() && !p.isCreative() && !p.isSpectator();
    }

    private List<ServerPlayer> prey(ServerLevel level, double range) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : level.players())
            if (isValidPrey(p) && p.distanceToSqr(this) < range * range) out.add(p);
        return out;
    }

    private boolean moveTo(Vec3 pos, double speed) {
        if (repath-- > 0 && !this.getNavigation().isDone()) return true;
        repath = 8;
        Path path = this.getNavigation().createPath(BlockPos.containing(pos), 1);
        if (path == null) {
            noPathTicks += 8;
            return false;
        }
        this.getNavigation().moveTo(path, speed);
        return true;
    }

    private boolean near(Vec3 p, double d) {
        return this.position().distanceToSqr(p.x, this.getY(), p.z) < d * d && Math.abs(p.y - this.getY()) < 2.5;
    }

    private boolean canSee(Entity e) {
        Vec3 from = this.getEyePosition();
        for (double h : new double[]{e.getEyeHeight(), e.getBbHeight() * 0.5, 0.2}) {
            Vec3 to = e.position().add(0, h, 0);
            BlockHitResult r = this.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (r.getType() == HitResult.Type.MISS) return true;
        }
        return false;
    }

    @Nullable
    private Vec3 standableNear(ServerLevel level, Vec3 around, int radius, int tries) {
        for (int i = 0; i < tries; i++) {
            double a = this.random.nextDouble() * Math.PI * 2;
            double r = radius * (0.35 + this.random.nextDouble() * 0.65);
            BlockPos base = BlockPos.containing(around.x + Math.cos(a) * r, around.y, around.z + Math.sin(a) * r);
            for (int dy = 0; dy <= 4; dy++) {
                for (int s : new int[]{1, -1}) {
                    BlockPos p = base.above(dy * s);
                    if (VentNetwork.standable(level, p)) {
                        Path path = this.getNavigation().createPath(p, 1);
                        if (path != null && path.canReach()) return Vec3.atBottomCenterOf(p);
                    }
                }
            }
        }
        Vec3 v = LandRandomPos.getPosTowards(this, radius, 6, around);
        return v;
    }

    // ------------------------------------------------------------------ director: pacing & vague knowledge
    private void director(ServerLevel level) {
        if (this.tickCount % 20 != 0) return;
        ServerPlayer focus = null;
        double best = Double.MAX_VALUE;
        for (ServerPlayer p : level.players()) {
            if (!isValidPrey(p)) continue;
            double d = p.distanceToSqr(this);
            if (d < best) { best = d; focus = p; }
        }
        if (focus == null) return;
        double dist = Math.sqrt(best);
        boolean pressure = state == State.CHASE || state == State.KILL || state == State.ALERT || (dist < 12 && !isHidden() && canSee(focus));
        menace = Mth.clamp(menace + (pressure ? 2.5F : dist < 20 ? 0.4F : -1.2F), 0F, 100F);
        farTicks = dist > 45 ? farTicks + 20 : 0;

        // the player has been hounded for a long time: give them room to breathe
        if (menace >= 70 && (state == State.ROAM || state == State.SEARCH || state == State.INVESTIGATE)) {
            menace = 25;
            retreatToVents(level, 500 + this.random.nextInt(500));
            return;
        }
        // far away and bored: come back towards the players (through the vents when possible)
        if (farTicks > 400 && state == State.ROAM) {
            farTicks = 0;
            Vec3 hint = focus.position().add((this.random.nextDouble() - 0.5) * 24, 0, (this.random.nextDouble() - 0.5) * 24);
            if (!travelVents(level, hint, State.ROAM, null)) goal = hint;
        }
    }

    private Vec3 stuckPos = Vec3.ZERO;
    private int stuckTicks;

    /** wandering states that make no progress for 10 s: slip into the ducts (or hop somewhere free) instead. */
    private boolean unstick(ServerLevel level) {
        boolean wandering = state == State.ROAM || state == State.INVESTIGATE || state == State.SEARCH || state == State.FLEE && stateLen <= 0;
        if (!wandering || isHidden()) { stuckTicks = 0; stuckPos = this.position(); return false; }
        if (this.position().distanceToSqr(stuckPos) > 2.0) { stuckPos = this.position(); stuckTicks = 0; return false; }
        if (++stuckTicks < 200) return false;
        stuckTicks = 0;
        Player p = level.getNearestPlayer(this.getX(), this.getY(), this.getZ(), 200, e -> e instanceof Player pl && isValidPrey(pl));
        Vec3 c = p != null ? p.position() : this.position();
        BlockPos v = pickExitVent(level, c, 8, 26);
        if (v == null) v = nearestVent(level, this.position(), 60, null);
        if (v != null) {
            this.playSound(ModSounds.XENO_VENT.get(), 1.2F, 1.0F);
            setHidden(true);
            startVentTravel(level, BlockPos.containing(this.position()), v, State.ROAM, null);
            return true;
        }
        Vec3 free = standableNear(level, this.position(), 6, 6);
        if (free != null) this.teleportTo(free.x, free.y, free.z);
        goal = null;
        return true;
    }

    private void retreatToVents(ServerLevel level, int ticks) {
        backstageTicks = ticks;
        BlockPos v = nearestVent(level, this.position(), 28, null);
        if (v != null) {
            ventFrom = v;
            goal = Vec3.atBottomCenterOf(VentNetwork.exitFor(level, v));
            afterVent = State.BACKSTAGE;
            enter(State.FLEE);
            setRunning(false);
            stateLen = -1; // walking, not fleeing in panic
        } else {
            Vec3 away = LandRandomPos.getPosAway(this, 24, 6, closestPlayerPos(level));
            goal = away;
            enter(State.ROAM);
        }
    }

    private Vec3 closestPlayerPos(ServerLevel level) {
        Player p = level.getNearestPlayer(this, 64);
        return p == null ? this.position() : p.position();
    }

    // ------------------------------------------------------------------ sight
    private void perceive(ServerLevel level) {
        double range = AlienConfig.SIGHT_RANGE.get();
        boolean alert = state == State.SEARCH || state == State.INVESTIGATE || state == State.LOCKER_CHECK;
        Vec3 look = Vec3.directionFromRotation(this.getXRot(), this.getYHeadRot());
        for (ServerPlayer p : prey(level, range)) {
            UUID id = p.getUUID();
            Vec3 prev = prevPlayerPos.put(id, p.position());
            if (HidingManager.isHidden(p)) { detection.remove(id); continue; }
            double d = this.distanceTo(p);
            Vec3 to = p.getEyePosition().subtract(this.getEyePosition()).normalize();
            double dot = look.dot(to);
            boolean peripheral = dot < 0.45;
            if (dot < -0.2 && d > 2.5) { decay(id); continue; }
            if (peripheral && d > 9) { decay(id); continue; }
            if (!canSee(p)) { decay(id); continue; }

            if (state == State.CHASE && id.equals(targetId)) continue;
            float light = level.getMaxLocalRawBrightness(p.blockPosition()) / 15F;
            float vis = 0.2F + 0.8F * light;
            if (FlashlightTracker.isLit(p)) vis = Math.max(vis, 1.1F);
            float stance = p.isCrouching() ? 0.42F : 1F;
            boolean moving = prev != null && prev.distanceToSqr(p.position()) > 0.0025;
            float motion = moving ? (p.isSprinting() ? 1.6F : 1.15F) : 0.7F;
            float distF = (float) Mth.clamp(1.0 - d / range, 0.04, 1.0);
            float rate = 0.30F * distF * distF * vis * stance * motion * (peripheral ? 0.35F : 1F) * (alert ? 1.6F : 1F);
            if (d < 2.6) rate = 1F;
            float m = detection.getOrDefault(id, 0F) + rate;
            detection.put(id, m);
            if (m >= 1F) spotted(p);
            else if (m > 0.35F && (state == State.ROAM || state == State.PAUSE)) {
                // something caught its eye: look and drift towards it
                this.getLookControl().setLookAt(p, 30, 30);
                goal = p.position();
                if (state == State.PAUSE) enter(State.ROAM);
            }
        }
    }

    private void decay(UUID id) {
        Float m = detection.get(id);
        if (m != null) {
            if (m <= 0.02F) detection.remove(id);
            else detection.put(id, m - 0.02F);
        }
    }

    private void spotted(ServerPlayer p) {
        detection.put(p.getUUID(), 1F);
        boolean newTarget = !p.getUUID().equals(targetId) || state != State.CHASE;
        targetId = p.getUUID();
        lastSeen = p.position();
        lostTicks = 0;
        remember(p.blockPosition());
        if (state == State.CHASE || state == State.ALERT || state == State.KILL) return;
        if (newTarget) enter(this.distanceTo(p) > 7 ? State.ALERT : State.CHASE);
        if (state == State.CHASE) p.playNotifySound(ModSounds.STINGER.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
    }

    private void remember(BlockPos p) {
        for (BlockPos h : hotspots) if (h.distSqr(p) < 64) return;
        hotspots.addLast(p.immutable());
        while (hotspots.size() > 10) hotspots.removeFirst();
    }

    // ------------------------------------------------------------------ hearing
    private void hear(ServerLevel level) {
        long now = level.getGameTime();
        List<NoiseSystem.Noise> noises = NoiseSystem.since(level, lastNoise);
        lastNoise = now;
        if (noises.isEmpty()) return;
        double mult = AlienConfig.HEARING.get() * (isHidden() ? 0.85 : 1.0);
        NoiseSystem.Noise best = null;
        double bestScore = 0;
        for (NoiseSystem.Noise n : noises) {
            double d = n.pos().distanceTo(this.position());
            double r = n.radius() * mult;
            if (d > r) continue;
            double score = (r - d) + (n.kind() == NoiseSystem.Kind.NOISEMAKER ? 6 : 0);
            if (score > bestScore) { bestScore = score; best = n; }
        }
        if (best == null) return;
        NoiseSystem.Noise n = best;

        switch (state) {
            case CHASE, KILL, ALERT, VENT_ENTER, VENT_EXIT, FLEE -> {
                if (state == State.CHASE && n.source() != null && n.source().equals(targetId)) lastSeen = n.pos();
                return;
            }
            case LOCKER_CHECK -> {
                // a gasp from the very locker it is sniffing is handled in tickLocker
                if (n.pos().distanceTo(this.position()) > 6 && n.radius() < 15) return;
            }
            default -> {}
        }
        if (n.kind() == NoiseSystem.Kind.NOISEMAKER || n.kind() == NoiseSystem.Kind.FLARE) {
            // it learns that some beeping gadgets are decoys
            if (this.random.nextFloat() < Math.min(0.55F, lureCount * 0.12F)) return;
        }
        // a very close player noise while it is alert is enough to give them away
        if (n.source() != null && (state == State.SEARCH || state == State.INVESTIGATE) && n.pos().distanceTo(this.position()) < 5) {
            Player p = level.getPlayerByUUID(n.source());
            if (p instanceof ServerPlayer sp && isValidPrey(sp) && !HidingManager.isHidden(sp)) {
                detection.merge(sp.getUUID(), 0.45F, Float::sum);
                if (detection.get(sp.getUUID()) >= 1F) { spotted(sp); return; }
            }
        }
        investigate(level, n.pos(), n.kind());
    }

    private void investigate(ServerLevel level, Vec3 pos, NoiseSystem.Kind kind) {
        if (kind == NoiseSystem.Kind.NOISEMAKER || kind == NoiseSystem.Kind.FLARE) lureCount++;
        if (isHidden()) {
            if (state == State.IN_VENT && ventTo != null && Vec3.atCenterOf(ventTo).distanceTo(pos) < 10) return;
            BlockPos v = nearestVent(level, pos, 18, null);
            if (v != null) {
                startVentTravel(level, ventFrom != null ? ventFrom : v, v, State.INVESTIGATE, pos);
                return;
            }
            return;
        }
        double d = pos.distanceTo(this.position());
        if (d > 30 && travelVents(level, pos, State.INVESTIGATE, pos)) return;
        goal = pos;
        enter(State.INVESTIGATE);
        this.getLookControl().setLookAt(pos.x, pos.y, pos.z);
    }

    // ------------------------------------------------------------------ states
    private void tickRoam(ServerLevel level) {
        setRunning(false);
        if (searchPause > 0) { searchPause--; return; }
        if (goal == null || near(goal, 1.6) || noPathTicks > 60 || stateTicks > 600) {
            if (goal != null && near(goal, 1.6) && this.random.nextFloat() < 0.45F) {
                playAnim(ANIM_SEARCH);
                this.getNavigation().stop();
                searchPause = 60;
                goal = null;
                return;
            }
            goal = pickRoamGoal(level);
            noPathTicks = 0;
            stateTicks = 0;
            if (goal == null) { searchPause = 20; return; }
        }
        if (!moveTo(goal, STALK)) { goal = null; searchPause = 10; }
    }

    @Nullable
    private Vec3 pickRoamGoal(ServerLevel level) {
        if (!hotspots.isEmpty() && this.random.nextFloat() < 0.25F) {
            BlockPos h = new ArrayList<>(hotspots).get(this.random.nextInt(hotspots.size()));
            return Vec3.atBottomCenterOf(h);
        }
        Player focus = level.getNearestPlayer(this.getX(), this.getY(), this.getZ(), 120, e -> e instanceof Player p && isValidPrey(p));
        if (focus != null) {
            // stay around the players: closer when tension is low, further away when it is high
            int radius = (int) Mth.lerp(menace / 100F, 9F, 26F);
            Vec3 v = standableNear(level, focus.position(), radius, 6);
            if (v != null) return v;
        }
        return LandRandomPos.getPos(this, 14, 6);
    }

    private void tickInvestigate(ServerLevel level) {
        if (goal == null) { enter(State.ROAM); return; }
        setRunning(false);
        if (this.tickCount % 5 == 0) {
            for (ThrownDeviceEntity d : level.getEntitiesOfClass(ThrownDeviceEntity.class, this.getBoundingBox().inflate(2.0),
                    e -> e.landed() && e.kind() == ThrownDeviceEntity.NOISEMAKER)) {
                this.getLookControl().setLookAt(d);
                playAnim(ANIM_ATTACK);
                this.playSound(ModSounds.XENO_HISS.get(), 1.3F, 1.1F);
                d.smash();
                startSearch(this.position(), 0.1F);
                return;
            }
        }
        if (near(goal, 2.0) || noPathTicks > 40 || stateTicks > 500) {
            startSearch(goal, 0.15F);
            return;
        }
        if (!moveTo(goal, SEARCH_SPEED)) {
            Vec3 alt = standableNear(level, goal, 4, 3);
            if (alt != null) goal = alt;
        }
    }

    private void startSearch(Vec3 center, float lockerBias) {
        searchCenter = center;
        searchPoints.clear();
        checkedLockers.clear();
        int n = 4 + this.random.nextInt(3);
        for (int i = 0; i < n; i++) {
            double a = this.random.nextDouble() * Math.PI * 2;
            double r = 3 + this.random.nextDouble() * 7;
            searchPoints.add(center.add(Math.cos(a) * r, 0, Math.sin(a) * r));
        }
        goal = null;
        lockerSuspicion = (int) (lockerBias * 100);
        enter(State.SEARCH);
        stateLen = 420 + this.random.nextInt(200);
        playAnim(ANIM_SEARCH);
        this.getNavigation().stop();
        searchPause = 50;
        this.playSound(ModSounds.XENO_SNIFF.get(), 1.0F, 1.0F);
    }

    private void tickSearch(ServerLevel level) {
        setRunning(false);
        if (searchPause > 0) { searchPause--; return; }
        if (stateTicks > stateLen) { enter(State.ROAM); return; }
        if (goal == null || near(goal, 1.5) || noPathTicks > 30) {
            if (goal != null && near(goal, 1.5)) {
                if (maybeCheckLocker(level)) return;
                if (this.random.nextFloat() < 0.6F) {
                    playAnim(ANIM_SEARCH);
                    this.getNavigation().stop();
                    searchPause = 60;
                }
            }
            goal = null;
            while (goal == null && !searchPoints.isEmpty()) {
                Vec3 p = searchPoints.poll();
                Vec3 s = standableNear(level, p, 2, 2);
                goal = s;
            }
            noPathTicks = 0;
            if (goal == null) { enter(State.ROAM); return; }
        }
        moveTo(goal, SEARCH_SPEED);
    }

    /** chance to inspect a nearby locker grows every time it found or suspected someone in one. */
    private boolean maybeCheckLocker(ServerLevel level) {
        float chance = Math.min(0.85F, 0.18F + lockerHides * 0.1F + lockerFinds * 0.15F + lockerSuspicion / 100F);
        BlockPos me = this.blockPosition();
        List<BlockPos> lockers = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(me.offset(-6, -2, -6), me.offset(6, 2, 6))) {
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof LockerBlock && s.getValue(LockerBlock.HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER
                    && !checkedLockers.contains(p.asLong())) lockers.add(p.immutable());
        }
        if (lockers.isEmpty()) return false;
        lockers.sort(Comparator.comparingDouble(p -> p.distSqr(me)));
        for (BlockPos l : lockers) {
            checkedLockers.add(l.asLong());
            // occupied lockers are not known to it; however a locker the player is in "smells" a bit
            float c = chance + (HidingManager.occupant(level, l) != null ? 0.12F : 0F);
            if (this.random.nextFloat() < c) {
                lockerPos = l;
                lockerKnown = false;
                enter(State.LOCKER_CHECK);
                return true;
            }
        }
        return false;
    }

    private void tickLocker(ServerLevel level) {
        if (lockerPos == null) { resumeSearch(); return; }
        BlockState s = level.getBlockState(lockerPos);
        if (!(s.getBlock() instanceof LockerBlock)) { lockerPos = null; resumeSearch(); return; }
        Direction f = s.getValue(LockerBlock.FACING);
        Vec3 front = Vec3.atBottomCenterOf(lockerPos.relative(f));
        Vec3 lc = Vec3.atCenterOf(lockerPos).add(0, 0.6, 0);
        ServerPlayer occ = HidingManager.occupant(level, lockerPos);

        if (stateLen == 0) {
            // walking up to the locker
            setRunning(lockerKnown);
            if (!near(front, 1.3)) {
                if (!moveTo(front, lockerKnown ? RUN : SEARCH_SPEED) || stateTicks > 300) resumeSearch();
                return;
            }
            this.getNavigation().stop();
            stateLen = 72;
            stateTicks = 0;
            lockerSuspicion = 0;
            if (lockerKnown && occ != null) {
                pullOut(occ);
                return;
            }
            playAnim(ANIM_LOCKER);
            this.playSound(ModSounds.XENO_SNIFF.get(), 1.0F, 0.9F);
        }
        this.getLookControl().setLookAt(lc.x, lc.y, lc.z, 30, 30);
        this.setYBodyRot(f.getOpposite().toYRot());
        if (occ != null && stateTicks > 8 && stateTicks < 66) {
            if (HidingManager.audible(occ)) lockerSuspicion += 2;
            else lockerSuspicion = Math.max(0, lockerSuspicion - 1);
            if (lockerSuspicion >= 14) {
                lockerFinds++;
                pullOut(occ);
                return;
            }
        }
        if (stateTicks == 36) this.playSound(ModSounds.XENO_SNIFF.get(), 1.0F, 1.1F);
        if (stateTicks >= stateLen) {
            lockerPos = null;
            resumeSearch();
        }
    }

    private void resumeSearch() {
        lockerPos = null;
        if (searchPoints.isEmpty()) {
            if (searchCenter != null && this.random.nextBoolean()) startSearch(searchCenter, 0F);
            else enter(State.ROAM);
            return;
        }
        enter(State.SEARCH);
        stateLen = 240;
        goal = null;
    }

    private void pullOut(ServerPlayer p) {
        HidingManager.forceOut(p);
        targetId = p.getUUID();
        lastSeen = p.position();
        lockerPos = null;
        startKill(p);
    }

    private void tickAlert(ServerLevel level) {
        ServerPlayer t = target();
        if (t == null) { enter(State.ROAM); return; }
        this.getLookControl().setLookAt(t, 40, 40);
        this.getNavigation().stop();
        if (stateTicks >= stateLen) enter(State.CHASE);
    }

    private void tickChase(ServerLevel level) {
        ServerPlayer t = target();
        if (t == null) {
            if (lastSeen != null) startSearch(lastSeen, 0.2F);
            else enter(State.ROAM);
            return;
        }
        setRunning(true);
        if (HidingManager.isHidden(t)) {
            HidingManager.Hide h = HidingManager.get(t);
            if (h != null) {
                // it lost sight right as they vanished: it will sniff that area, lockers first
                lockerHides++;
                if (lockerKnown && h.locker.equals(lockerPos)) {
                    enter(State.LOCKER_CHECK);
                    return;
                }
                startSearch(Vec3.atBottomCenterOf(h.locker), 0.35F);
                return;
            }
        }
        boolean see = canSee(t) && this.distanceTo(t) < AlienConfig.SIGHT_RANGE.get() + 6;
        if (see) {
            lastSeenVel = t.position().subtract(lastSeen == null ? t.position() : lastSeen);
            lastSeen = t.position();
            lostTicks = 0;
            this.getLookControl().setLookAt(t, 40, 40);
        } else {
            lostTicks++;
        }
        if (see && this.distanceTo(t) < 1.9 && Math.abs(t.getY() - this.getY()) < 1.5) {
            startKill(t);
            return;
        }
        // for a short while it keeps tracking the scent, then it has to guess
        Vec3 dest = lostTicks < 50 ? t.position() : lastSeen;
        if (dest == null || lostTicks > 140 || noPathTicks > 80) {
            Vec3 guess = lastSeen == null ? t.position() : lastSeen.add(lastSeenVel.normalize().scale(4));
            startSearch(guess, 0.25F);
            return;
        }
        if (this.tickCount % 3 == 0) repath = 0;
        moveTo(dest, RUN);
        if (this.random.nextInt(70) == 0) this.playSound(ModSounds.XENO_HISS.get(), 1.2F, 1.0F);
    }

    private void startKill(ServerPlayer t) {
        targetId = t.getUUID();
        enter(State.KILL);
        this.entityData.set(DATA_KILL, t.getId());
        this.getNavigation().stop();
        playAnim(ANIM_KILL);
        this.playSound(ModSounds.XENO_HISS.get(), 1.6F, 0.9F);
        t.playNotifySound(ModSounds.STINGER.get(), SoundSource.HOSTILE, 1.0F, 0.8F);
    }

    private void tickKill(ServerLevel level) {
        ServerPlayer t = target();
        if (t == null || t.level() != level) { enter(State.ROAM); return; }
        // hold the victim right in front of its face; the camera is forced to look at it
        Vec3 fwd = Vec3.directionFromRotation(0, this.getYRot());
        Vec3 spot = this.position().add(fwd.scale(1.15));
        Vec3 eye = this.position().add(0, 1.55, 0).add(fwd.scale(0.45));
        Vec3 d = eye.subtract(spot.add(0, t.getEyeHeight(), 0));
        float yaw = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90F;
        float pitch = (float) -(Mth.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * Mth.RAD_TO_DEG);
        if (stateTicks == 1) {
            float face = (float) (Mth.atan2(t.getZ() - this.getZ(), t.getX() - this.getX()) * Mth.RAD_TO_DEG) - 90F;
            this.setYRot(face);
            this.setYBodyRot(face);
            this.setYHeadRot(face);
        }
        if (stateTicks % 2 == 1 || t.position().distanceToSqr(spot) > 0.09) {
            t.connection.teleport(spot.x, spot.y, spot.z, yaw, pitch);
        }
        t.setDeltaMovement(Vec3.ZERO);
        if (stateTicks == KILL_STRIKE - 3) this.playSound(ModSounds.XENO_KILL.get(), 1.6F, 1.0F);
        if (stateTicks == KILL_STRIKE) {
            float dmg = AlienConfig.INSTANT_KILL.get() && level.getDifficulty() != Difficulty.EASY ? Float.MAX_VALUE : 16F;
            t.invulnerableTime = 0;
            t.hurt(this.damageSources().mobAttack(this), dmg);
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, t.getX(), t.getEyeY(), t.getZ(), 8, 0.2, 0.2, 0.2, 0.1);
        }
        if (stateTicks >= KILL_LEN) {
            this.entityData.set(DATA_KILL, -1);
            if (t.isAlive()) {
                // easy difficulty / no instant kill: it wounds and slinks away, giving a chance to hide
                menace = 30;
                retreatToVents(level, 240 + this.random.nextInt(200));
            } else {
                detection.remove(t.getUUID());
                targetId = null;
                menace = 40;
                // after a kill it lingers a moment, then slips away so the survivor gets a chance
                retreatToVents(level, 400 + this.random.nextInt(300));
            }
        }
    }

    // ------------------------------------------------------------------ fire
    /** called every tick a flamethrower jet touches it. */
    public void onFlamed(@Nullable Player by) {
        if (this.level().isClientSide || isHidden()) return;
        if (state == State.FLEE && stateLen > 0) return;
        fireExposure += 3;
        int needed = 4 + Math.min(18, flameHits * 3);
        if (by != null && state != State.CHASE && state != State.KILL && isValidPrey(by)) {
            targetId = by.getUUID();
            lastSeen = by.position();
        }
        if (fireExposure >= needed) {
            fireExposure = 0;
            flameHits++;
            fleeFrom = by == null ? null : by.getUUID();
            ServerLevel level = (ServerLevel) this.level();
            if (state == State.KILL) this.entityData.set(DATA_KILL, -1);
            playAnim(ANIM_FLINCH);
            this.playSound(ModSounds.XENO_FLINCH.get(), 2.0F, 1.0F);
            backstageTicks = Math.max(240, 900 - flameHits * 120);
            Vec3 from = by == null ? this.position() : by.position();
            BlockPos v = nearestVent(level, this.position(), 30, p -> Vec3.atCenterOf(p).distanceTo(from) > 7);
            ventFrom = v;
            afterVent = State.BACKSTAGE;
            goal = v != null ? Vec3.atBottomCenterOf(VentNetwork.exitFor(level, v)) : LandRandomPos.getPosAway(this, 24, 7, from);
            enter(State.FLEE);
            stateLen = 26;
        }
    }

    private void tickFlee(ServerLevel level) {
        if (stateLen > 0 && stateTicks < stateLen) { this.getNavigation().stop(); return; }
        setRunning(stateLen > 0);
        if (goal == null || noPathTicks > 60 || stateTicks > 400) {
            if (ventFrom != null) startVentEnter();
            else { menace = 10; enter(State.ROAM); searchPause = 0; goal = null; }
            return;
        }
        if (near(goal, 1.4)) {
            if (ventFrom != null) startVentEnter();
            else { enter(State.ROAM); goal = null; }
            return;
        }
        moveTo(goal, stateLen > 0 ? RUN * 1.05 : STALK * 1.1);
    }

    // ------------------------------------------------------------------ vents
    @Nullable
    private BlockPos nearestVent(ServerLevel level, Vec3 c, double max, @Nullable java.util.function.Predicate<BlockPos> extra) {
        VentNetwork net = VentNetwork.get(level);
        if (net.isEmpty()) return null;
        return net.nearest(level, c, max, p -> VentNetwork.exitFor(level, p) != null && (extra == null || extra.test(p)));
    }

    /** walk to the nearest vent, crawl to one close to {@code dest} and come out there. */
    private boolean travelVents(ServerLevel level, Vec3 dest, State after, @Nullable Vec3 afterGoal) {
        BlockPos in = nearestVent(level, this.position(), 22, null);
        if (in == null) return false;
        BlockPos out = pickExitVent(level, dest, 5, 18);
        if (out == null || out.equals(in)) return false;
        ventFrom = in;
        ventTo = out;
        afterVent = after;
        afterVentGoal = afterGoal;
        goal = Vec3.atBottomCenterOf(VentNetwork.exitFor(level, in));
        enter(State.FLEE);
        setRunning(false);
        stateLen = -1;
        return true;
    }

    /** a vent near dest that no player is currently looking at. */
    @Nullable
    private BlockPos pickExitVent(ServerLevel level, Vec3 dest, double min, double max) {
        List<BlockPos> list = VentNetwork.get(level).within(level, dest, min, max);
        Collections.shuffle(list, new Random(this.random.nextLong()));
        BlockPos fallback = null;
        for (BlockPos v : list) {
            if (VentNetwork.exitFor(level, v) == null) continue;
            if (fallback == null) fallback = v;
            boolean watched = false;
            for (ServerPlayer p : level.players()) {
                if (!isValidPrey(p)) continue;
                Vec3 to = Vec3.atCenterOf(v).subtract(p.getEyePosition());
                if (to.length() < 16 && p.getLookAngle().dot(to.normalize()) > 0.55) { watched = true; break; }
            }
            if (!watched) return v;
        }
        return fallback;
    }

    private void startVentEnter() {
        enter(State.VENT_ENTER);
        this.getNavigation().stop();
        playAnim(ANIM_VENT_ENTER);
        this.playSound(ModSounds.XENO_HISS.get(), 0.8F, 1.1F);
    }

    private void tickVentEnter(ServerLevel level) {
        if (ventFrom != null) {
            Vec3 c = Vec3.atCenterOf(ventFrom);
            this.getLookControl().setLookAt(c.x, c.y, c.z);
        }
        if (stateTicks == 18) {
            this.playSound(ModSounds.XENO_VENT.get(), 1.4F, 1.0F);
            setHidden(true);
            if (ventFrom != null) {
                Vec3 c = Vec3.atBottomCenterOf(ventFrom);
                this.teleportTo(c.x, c.y, c.z);
            }
        }
        if (stateTicks >= 24) {
            if (afterVent == State.BACKSTAGE || ventTo == null) {
                enter(State.BACKSTAGE);
            } else {
                startVentTravel(level, ventFrom, ventTo, afterVent, afterVentGoal);
            }
        }
    }

    private void startVentTravel(ServerLevel level, @Nullable BlockPos from, BlockPos to, @Nullable State after, @Nullable Vec3 afterGoal) {
        ventFrom = from;
        ventTo = to;
        afterVent = after;
        afterVentGoal = afterGoal;
        enter(State.IN_VENT);
    }

    private void tickInVent(ServerLevel level) {
        if (!isHidden()) setHidden(true);
        if (ventTo == null) { enter(State.BACKSTAGE); return; }
        Vec3 dest = Vec3.atBottomCenterOf(ventTo);
        Vec3 d = dest.subtract(this.position());
        double len = d.length();
        double speed = afterVent == State.INVESTIGATE ? 0.42 : 0.3;
        if (len <= speed) {
            this.setPos(dest);
            if (backstageDrift) {
                backstageDrift = false;
                int left = backstageTicks;
                enter(State.BACKSTAGE);
                backstageTicks = Math.max(80, left - 160);
                return;
            }
            exitVent(level, ventTo);
            return;
        }
        Vec3 step = d.scale(speed / len);
        this.setPos(this.position().add(step));
        this.setDeltaMovement(Vec3.ZERO);
        this.setYRot((float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90F);
        if (stateTicks % 7 == 0)
            level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.XENO_VENT.get(), SoundSource.HOSTILE,
                    1.3F, 0.8F + this.random.nextFloat() * 0.4F);
    }

    private void exitVent(ServerLevel level, BlockPos vent) {
        BlockPos out = VentNetwork.exitFor(level, vent);
        if (out == null) { enter(State.BACKSTAGE); return; }
        Vec3 o = Vec3.atBottomCenterOf(out);
        this.teleportTo(o.x, o.y, o.z);
        setHidden(false);
        ventFrom = vent;
        // face the nearest player-ish direction
        Player p = level.getNearestPlayer(this, 32);
        if (p != null) {
            float face = (float) (Mth.atan2(p.getZ() - o.z, p.getX() - o.x) * Mth.RAD_TO_DEG) - 90F;
            this.setYRot(face);
            this.setYBodyRot(face);
            this.setYHeadRot(face);
        }
        enter(State.VENT_EXIT);
        playAnim(ANIM_VENT_EXIT);
        this.playSound(ModSounds.XENO_DROP.get(), 1.6F, 1.0F);
        // lights around the drop point stutter
        for (BlockPos lp : BlockPos.betweenClosed(out.offset(-4, 0, -4), out.offset(4, 6, 4))) {
            BlockState s = level.getBlockState(lp);
            if (s.getBlock() instanceof CeilingLightBlock && this.random.nextInt(2) == 0) CeilingLightBlock.blackout(level, lp.immutable(), s, 8 + this.random.nextInt(25));
        }
    }

    private void tickVentExit(ServerLevel level) {
        this.getNavigation().stop();
        if (stateTicks == 16) this.playSound(ModSounds.XENO_HISS.get(), 1.4F, 1.0F);
        if (stateTicks >= 32) {
            State next = afterVent == null ? State.ROAM : afterVent;
            Vec3 g = afterVentGoal;
            afterVent = null;
            afterVentGoal = null;
            ventTo = null;
            if (next == State.INVESTIGATE && g != null) { goal = g; enter(State.INVESTIGATE); }
            else { goal = null; enter(State.ROAM); }
        }
    }

    private void tickBackstage(ServerLevel level) {
        if (!isHidden()) setHidden(true);
        if (stateTicks == 1) setRunning(false);
        // occasionally crawl around above the players so they hear (and track) it
        if (stateTicks % 160 == 80 && this.random.nextInt(3) == 0) {
            Player p = level.getNearestPlayer(this, 80);
            if (p != null) {
                BlockPos v = pickExitVent(level, p.position(), 10, 26);
                if (v != null) {
                    BlockPos from = BlockPos.containing(this.position());
                    startVentTravel(level, from, v, State.BACKSTAGE, null);
                    // travelling inside the duct; exitVent is skipped for backstage drifts
                    afterVent = State.BACKSTAGE;
                    backstageDrift = true;
                    return;
                }
            }
        }
        if (stateTicks >= backstageTicks) {
            Player p = level.getNearestPlayer(this.getX(), this.getY(), this.getZ(), 200, e -> e instanceof Player pl && isValidPrey(pl));
            Vec3 c = p != null ? p.position() : this.position();
            BlockPos v = pickExitVent(level, c, 7, 20);
            if (v == null) v = nearestVent(level, c, 60, null);
            if (v == null) {
                // the vent network vanished: just drop down here
                setHidden(false);
                enter(State.ROAM);
                return;
            }
            startVentTravel(level, BlockPos.containing(this.position()), v, State.ROAM, null);
        }
    }

    private boolean backstageDrift;

    // ------------------------------------------------------------------ hooks from the world
    /** a player just climbed into a locker: aliens that were watching them remember exactly which one. */
    public static void onPlayerHid(ServerPlayer p, BlockPos locker) {
        ServerLevel level = p.serverLevel();
        for (XenomorphEntity x : level.getEntitiesOfClass(XenomorphEntity.class, p.getBoundingBox().inflate(40))) {
            if (x.isHidden()) continue;
            boolean watching = (x.state == State.CHASE && p.getUUID().equals(x.targetId) && x.lostTicks < 10)
                    || x.detection.getOrDefault(p.getUUID(), 0F) > 0.6F;
            if (watching && x.canSee(p)) {
                x.lockerPos = locker;
                x.lockerKnown = true;
                x.lockerHides++;
                x.targetId = p.getUUID();
                x.enter(State.LOCKER_CHECK);
            }
        }
    }

    // ------------------------------------------------------------------ persistence
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("FlameHits", flameHits);
        tag.putInt("LockerFinds", lockerFinds);
        tag.putInt("LockerHides", lockerHides);
        tag.putInt("Lures", lureCount);
        tag.putFloat("Menace", menace);
        tag.putBoolean("Hidden", isHidden());
        ListTag hs = new ListTag();
        for (BlockPos p : hotspots) hs.add(LongTag.valueOf(p.asLong()));
        tag.put("Hotspots", hs);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        flameHits = tag.getInt("FlameHits");
        lockerFinds = tag.getInt("LockerFinds");
        lockerHides = tag.getInt("LockerHides");
        lureCount = tag.getInt("Lures");
        menace = tag.getFloat("Menace");
        hotspots.clear();
        for (Tag t : tag.getList("Hotspots", Tag.TAG_LONG)) hotspots.add(BlockPos.of(((LongTag) t).getAsLong()));
        if (tag.getBoolean("Hidden")) {
            setHidden(true);
            backstageTicks = 200;
            state = State.BACKSTAGE;
            this.entityData.set(DATA_STATE, (byte) State.BACKSTAGE.ordinal());
        }
    }

    public String debugState() {
        return state + " t=" + stateTicks + " target=" + (target() == null ? null : target().getName().getString())
                + " hidden=" + isHidden() + " menace=" + (int) menace + " pos=" + this.blockPosition().toShortString();
    }

    public void debugAnim(int id) { playAnim(id); }

    /** spawned hidden inside a vent (used by the station generator). */
    public void startInVents(int ticks) {
        setHidden(true);
        backstageTicks = ticks;
        enter(State.BACKSTAGE);
    }
}
