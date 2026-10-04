package com.krolasyon.futbol.entity;

import com.krolasyon.futbol.game.FootData;
import com.krolasyon.futbol.game.MatchManager;
import com.krolasyon.futbol.game.MoveExecutor;
import com.krolasyon.futbol.game.Team;
import com.krolasyon.futbol.registry.ModBlocks;
import com.krolasyon.futbol.registry.ModItems;
import com.krolasyon.futbol.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The football. Server-authoritative physics: gravity, air drag, rolling friction, bounces with restitution,
 * Magnus-effect curve from spin, wall/post/net reactions, body deflections and dribble control.
 */
public class FootballEntity extends Entity {
    public static final double R = 0.25;
    public static final double GRAVITY = 0.045;
    public static final double AIR = 0.988;
    public static final double ROLL = 0.955;
    public static final double BOUNCE = 0.62;
    public static final double WALL = 0.55;
    public static final double MAGNUS = 0.022;

    public static final int SP_NONE = 0, SP_SEAL = 1, SP_JUGGLE = 2, SP_EAGLE = 3, SP_LIGHTNING = 4, SP_TORNADO = 5,
            SP_KNUCKLE = 6, SP_ICE = 7, SP_FIRE = 8;
    public static final int TR_NONE = 0, TR_FAST = 1, TR_FIRE = 2, TR_LIGHTNING = 3, TR_TORNADO = 4, TR_EAGLE = 5,
            TR_ICE = 6, TR_CURL = 7, TR_KNUCKLE = 8, TR_GOLD = 9;

    private static final EntityDataAccessor<Integer> DATA_CONTROLLER = SynchedEntityData.defineId(FootballEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_HOLDER = SynchedEntityData.defineId(FootballEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TRAIL = SynchedEntityData.defineId(FootballEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Vector3f> DATA_SPIN = SynchedEntityData.defineId(FootballEntity.class, EntityDataSerializers.VECTOR3);

    // ---- server state
    private Vec3 spin = Vec3.ZERO;
    public UUID lastToucher;
    public String lastToucherName = "";
    public Team lastTouchTeam = Team.NONE;
    private final Map<Integer, Long> immune = new HashMap<>();
    private long noBodyUntil;
    public boolean frozen;
    public Team restrictTeam = Team.NONE;
    public long restrictUntil;
    public int special;
    public int specialTimer;
    public LivingEntity specialOwner;
    public Vec3 specialTarget;
    private Vec3 specialPrevOffset = Vec3.ZERO;
    private int trailAge;
    private Vec3 lastCtrlPos;
    private double dribblePhase;
    private long lastSoundTick;

    // ---- client state
    public final Quaternionf rot = new Quaternionf();
    public final Quaternionf prevRot = new Quaternionf();
    private int lerpSteps;
    private double lx, ly, lz;
    private Vec3 clientCtrlPos;
    private double clientPhase;

    public FootballEntity(EntityType<? extends FootballEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = false;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_CONTROLLER, -1);
        this.entityData.define(DATA_HOLDER, -1);
        this.entityData.define(DATA_TRAIL, 0);
        this.entityData.define(DATA_SPIN, new Vector3f());
    }

    // ------------------------------------------------------------------ accessors

    public LivingEntity getController() {
        int id = this.entityData.get(DATA_CONTROLLER);
        if (id < 0) return null;
        Entity e = level().getEntity(id);
        return e instanceof LivingEntity l ? l : null;
    }

    public int getControllerId() { return this.entityData.get(DATA_CONTROLLER); }

    public void setController(LivingEntity e) {
        int id = e == null ? -1 : e.getId();
        if (this.entityData.get(DATA_CONTROLLER) != id) {
            this.entityData.set(DATA_CONTROLLER, id);
            lastCtrlPos = null;
            if (e != null) {
                touched(e);
                spin = Vec3.ZERO;
                setTrail(TR_NONE);
                if (special != SP_SEAL && special != SP_JUGGLE) special = SP_NONE;
            }
        }
    }

    public LivingEntity getHolder() {
        int id = this.entityData.get(DATA_HOLDER);
        if (id < 0) return null;
        Entity e = level().getEntity(id);
        return e instanceof LivingEntity l ? l : null;
    }

    public void setHolder(LivingEntity e) {
        this.entityData.set(DATA_HOLDER, e == null ? -1 : e.getId());
        if (e != null) {
            setController(null);
            touched(e);
            setDeltaMovement(Vec3.ZERO);
            spin = Vec3.ZERO;
            special = SP_NONE;
            setTrail(TR_NONE);
        }
    }

    public int getTrail() { return this.entityData.get(DATA_TRAIL); }

    public void setTrail(int t) {
        this.entityData.set(DATA_TRAIL, t);
        trailAge = 0;
    }

    public Vector3f getSpinData() { return this.entityData.get(DATA_SPIN); }

    public Vec3 getSpin() { return spin; }

    public Vec3 center() { return position().add(0, R, 0); }

    public void touched(LivingEntity e) {
        lastToucher = e.getUUID();
        lastToucherName = e.getName().getString();
        lastTouchTeam = MatchManager.teamOf(e);
        if (restrictTeam != Team.NONE && lastTouchTeam == restrictTeam) restrictTeam = Team.NONE;
    }

    public void immune(LivingEntity e, int ticks) { immune.put(e.getId(), level().getGameTime() + ticks); }

    public boolean isImmune(LivingEntity e) {
        Long until = immune.get(e.getId());
        return until != null && until > level().getGameTime();
    }

    public void noBodyCollision(int ticks) { noBodyUntil = level().getGameTime() + ticks; }

    // ------------------------------------------------------------------ kicking API

    public void kick(LivingEntity by, Vec3 vel, Vec3 spinVec, int trail, int sp) {
        if (frozen) return;
        this.entityData.set(DATA_HOLDER, -1);
        setController(null);
        if (by != null) {
            touched(by);
            immune(by, 7);
        }
        if (getY() < (by != null ? by.getY() : getY()) + 0.05 && vel.y > 0) setPos(getX(), getY() + 0.05, getZ());
        this.spin = spinVec;
        this.entityData.set(DATA_SPIN, new Vector3f((float) spinVec.x, (float) spinVec.y, (float) spinVec.z));
        setDeltaMovement(vel);
        setTrail(trail);
        this.special = sp;
        this.specialTimer = 0;
        this.specialOwner = by;
        this.specialPrevOffset = Vec3.ZERO;
        this.hasImpulse = true;
        noBodyCollision(2);
    }

    public void playBallSound(SoundEvent s, float vol, float pitch) {
        level().playSound(null, getX(), getY(), getZ(), s, SoundSource.PLAYERS, vol, pitch);
    }

    public void placeAt(Vec3 p) {
        setController(null);
        this.entityData.set(DATA_HOLDER, -1);
        special = SP_NONE;
        spin = Vec3.ZERO;
        setTrail(TR_NONE);
        setDeltaMovement(Vec3.ZERO);
        teleportTo(p.x, p.y, p.z);
    }

    // ------------------------------------------------------------------ tick

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            clientTick();
            return;
        }
        serverTick();
    }

    private void serverTick() {
        long now = level().getGameTime();
        if (getY() < level().getMinBuildHeight() - 20) {
            MatchManager.onBallLost(this);
            if (isAlive()) placeAt(position().add(0, 200, 0));
            return;
        }
        LivingEntity holder = getHolder();
        if (this.entityData.get(DATA_HOLDER) >= 0) {
            if (holder == null || !holder.isAlive()) {
                this.entityData.set(DATA_HOLDER, -1);
            } else {
                Vec3 f = facing(holder);
                setPos(holder.getX() + f.x * 0.45, holder.getY() + 0.95, holder.getZ() + f.z * 0.45);
                setDeltaMovement(Vec3.ZERO);
                return;
            }
        }
        if (frozen) {
            setController(null);
            setDeltaMovement(Vec3.ZERO);
            spin = Vec3.ZERO;
            return;
        }
        if (special != SP_NONE && specialTick(now)) return;

        LivingEntity ctrl = getController();
        if (this.entityData.get(DATA_CONTROLLER) >= 0 && (ctrl == null || !canControl(ctrl, now))) {
            setController(null);
            ctrl = null;
        }
        if (ctrl != null) {
            dribble(ctrl);
            return;
        }
        physics(now);
        tryAcquire(now);
        if (getTrail() != TR_NONE && ++trailAge > 10 && getDeltaMovement().length() < 0.3) setTrail(TR_NONE);
        if ((now & 15) == 0) immune.values().removeIf(t -> t < now);
    }

    private void physics(long now) {
        Vec3 v = getDeltaMovement();
        if (spin.lengthSqr() > 1.0E-4) {
            v = v.add(spin.cross(v).scale(MAGNUS));
            spin = spin.scale(onGround() ? 0.85 : 0.985);
            if ((now & 3) == 0)
                this.entityData.set(DATA_SPIN, new Vector3f((float) spin.x, (float) spin.y, (float) spin.z));
        }
        v = v.add(0, -GRAVITY, 0).scale(AIR);
        Vec3 before = position();
        move(MoverType.SELF, v);
        Vec3 moved = position().subtract(before);
        boolean hitX = Math.abs(moved.x - v.x) > 1.0E-5;
        boolean hitY = Math.abs(moved.y - v.y) > 1.0E-5;
        boolean hitZ = Math.abs(moved.z - v.z) > 1.0E-5;
        boolean impact = false;
        double speed = v.length();
        if (hitY) {
            if (v.y < -0.12) {
                if (v.y < -0.25) sound(ModSounds.BOUNCE.get(), (float) Math.min(1.0, -v.y * 1.6), 0.9F + random.nextFloat() * 0.2F, now);
                v = new Vec3(v.x * 0.92, -v.y * BOUNCE, v.z * 0.92);
                spin = spin.scale(0.7);
            } else if (v.y < 0) {
                v = new Vec3(v.x, 0, v.z);
            } else {
                v = new Vec3(v.x, -v.y * 0.4, v.z);
                impact = true;
            }
        }
        if (hitX) {
            v = new Vec3(-v.x * WALL, v.y, v.z);
            impact = true;
        }
        if (hitZ) {
            v = new Vec3(v.x, v.y, -v.z * WALL);
            impact = true;
        }
        if (impact || (hitY && v.y > 0.05)) {
            int kind = touchingGoal();
            if (kind == 1) {
                if (speed > 0.3) {
                    sound(ModSounds.POST_HIT.get(), (float) Math.min(1.5, speed), 0.9F + random.nextFloat() * 0.25F, now);
                    if (speed > 0.8) MatchManager.onNearMiss(this);
                }
                v = v.scale(0.9);
            } else if (kind == 2) {
                if (speed > 0.25) sound(ModSounds.NET_HIT.get(), (float) Math.min(1.2, speed), 0.9F + random.nextFloat() * 0.2F, now);
                v = v.scale(0.12);
                spin = Vec3.ZERO;
            } else if (impact && speed > 0.5) {
                sound(ModSounds.BOUNCE.get(), 0.6F, 0.7F, now);
            }
        }
        if (onGround() && Math.abs(v.y) < 0.1) {
            v = new Vec3(v.x * ROLL, v.y, v.z * ROLL);
            if (v.horizontalDistanceSqr() < 1.0E-4) v = new Vec3(0, v.y, 0);
        }
        setDeltaMovement(v);
        if (v.horizontalDistanceSqr() > 0.06 && now >= noBodyUntil) bodyCollision(v);
    }

    /** 1 = goal frame, 2 = net */
    private int touchingGoal() {
        AABB box = getBoundingBox().inflate(0.12);
        for (BlockPos bp : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
            BlockState s = level().getBlockState(bp);
            if (s.is(ModBlocks.GOAL_POST.get())) return 1;
            if (s.is(ModBlocks.GOAL_NET.get()) || s.is(ModBlocks.GOAL_NET_ROOF.get())) return 2;
        }
        return 0;
    }

    private void bodyCollision(Vec3 v) {
        List<LivingEntity> list = level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.15),
                e -> e.isAlive() && !e.isSpectator() && !isImmune(e));
        for (LivingEntity e : list) {
            if (e instanceof FootballerEntity bot && bot.isKeeper() && com.krolasyon.futbol.game.BotBrain.tryCatch(bot, this)) return;
            Vec3 n = new Vec3(getX() - e.getX(), 0, getZ() - e.getZ());
            n = n.lengthSqr() < 1.0E-6 ? v.scale(-1).normalize() : n.normalize();
            double vn = v.dot(n);
            Vec3 out = vn < 0 ? v.subtract(n.scale(2 * vn)).scale(0.35) : v.scale(0.6);
            setDeltaMovement(out.add(0, 0.08, 0));
            spin = Vec3.ZERO;
            double push = special == SP_FIRE ? 1.6 : special == SP_LIGHTNING ? 1.2 : 0.18;
            e.push(v.x * push, special == SP_FIRE ? 0.45 : 0.05, v.z * push);
            e.hurtMarked = true;
            if (special == SP_FIRE && level() instanceof ServerLevel sl)
                sl.sendParticles(ParticleTypes.FLAME, e.getX(), e.getY() + 1, e.getZ(), 30, 0.4, 0.6, 0.4, 0.08);
            if (special == SP_LIGHTNING && level() instanceof ServerLevel sl)
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, e.getX(), e.getY() + 1, e.getZ(), 30, 0.4, 0.6, 0.4, 0.3);
            special = SP_NONE;
            touched(e);
            immune(e, 3);
            playBallSound(ModSounds.TACKLE.get(), 0.7F, 1.3F);
            return;
        }
    }

    private boolean canControl(LivingEntity e, long now) {
        if (!e.isAlive() || e.isSpectator() || e.level() != level()) return false;
        if (FootData.of(e).stunnedUntil > now) return false;
        return e.distanceToSqr(this) < 2.4 * 2.4;
    }

    public boolean eligible(LivingEntity e, long now) {
        if (!(e instanceof Player || e instanceof FootballerEntity)) return false;
        if (!e.isAlive() || e.isSpectator() || isImmune(e)) return false;
        if (e instanceof Player p && p.isCreative() && p.getAbilities().flying) return false;
        if (FootData.of(e).stunnedUntil > now) return false;
        if (restrictTeam != Team.NONE && now < restrictUntil && MatchManager.teamOf(e) != restrictTeam) return false;
        return true;
    }

    private void tryAcquire(long now) {
        if (getDeltaMovement().length() > 0.95) return;
        LivingEntity best = null;
        double bestD = 1.05 * 1.05;
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(1.3, 1.2, 1.3), e -> eligible(e, now))) {
            double dy = getY() - e.getY();
            if (dy < -0.5 || dy > 1.0) continue;
            double dx = e.getX() - getX(), dz = e.getZ() - getZ();
            double d = dx * dx + dz * dz;
            if (d < bestD) {
                bestD = d;
                best = e;
            }
        }
        if (best != null) setController(best);
    }

    private void dribble(LivingEntity c) {
        Vec3 f = facing(c);
        Vec3 cp = c.position();
        Vec3 vel = lastCtrlPos == null ? Vec3.ZERO : cp.subtract(lastCtrlPos);
        lastCtrlPos = cp;
        double sp = Math.min(vel.horizontalDistance(), 0.45);
        double oldPhase = dribblePhase;
        dribblePhase += sp * 1.25;
        double reach = 0.62 + sp * 1.6 + Math.max(0, Math.sin(dribblePhase)) * Math.min(sp * 2.5, 0.45);
        if (c.isShiftKeyDown()) reach = 0.38;
        Vec3 target = cp.add(f.x * reach + vel.x, 0, f.z * reach + vel.z);
        Vec3 d = target.subtract(position());
        Vec3 step = new Vec3(d.x * 0.55, Math.min(0, d.y) - 0.15, d.z * 0.55);
        Vec3 before = position();
        move(MoverType.SELF, step);
        Vec3 moved = position().subtract(before);
        setDeltaMovement(new Vec3(moved.x, 0, moved.z));
        if (Math.floor(oldPhase / Math.PI) != Math.floor(dribblePhase / Math.PI) && sp > 0.12)
            playBallSound(ModSounds.PASS.get(), 0.18F, 1.3F + random.nextFloat() * 0.2F);
        if (horizontalDist(c) > 2.4) setController(null);
    }

    private double horizontalDist(Entity e) {
        double dx = e.getX() - getX(), dz = e.getZ() - getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** @return true if the special fully drives the ball position this tick */
    private boolean specialTick(long now) {
        specialTimer++;
        LivingEntity o = specialOwner;
        ServerLevel sl = (ServerLevel) level();
        switch (special) {
            case SP_SEAL -> {
                if (o == null || !o.isAlive() || specialTimer > 80 || FootData.of(o).stunnedUntil > now) {
                    special = SP_NONE;
                    if (o != null && o.isAlive()) {
                        Vec3 f = facing(o);
                        setPos(o.getX() + f.x * 0.7, o.getY() + 0.6, o.getZ() + f.z * 0.7);
                    }
                    setDeltaMovement(new Vec3(0, -0.1, 0));
                    return false;
                }
                Vec3 f = facing(o);
                double bounce = Math.abs(Math.sin(specialTimer * 0.35)) * 0.38;
                setPos(o.getX() + f.x * 0.06, o.getY() + o.getBbHeight() + 0.02 + bounce, o.getZ() + f.z * 0.06);
                if (Math.abs(Math.sin(specialTimer * 0.35)) < 0.17 && specialTimer % 9 < 2)
                    playBallSound(ModSounds.HEADER.get(), 0.25F, 1.6F);
                return true;
            }
            case SP_JUGGLE -> {
                if (o == null || !o.isAlive() || specialTimer > 36 || FootData.of(o).stunnedUntil > now) {
                    special = SP_NONE;
                    setDeltaMovement(new Vec3(0, -0.1, 0));
                    return false;
                }
                Vec3 f = facing(o);
                double ph = specialTimer * Math.PI / 8.0;
                double h = 0.15 + Math.abs(Math.sin(ph)) * 1.15;
                double side = (Math.floor(specialTimer / 8.0) % 2 == 0) ? 0.18 : -0.18;
                Vec3 r = new Vec3(-f.z, 0, f.x).scale(side);
                setPos(o.getX() + f.x * 0.45 + r.x, o.getY() + h, o.getZ() + f.z * 0.45 + r.z);
                if (specialTimer % 8 == 0) playBallSound(ModSounds.PASS.get(), 0.3F, 1.5F);
                return true;
            }
            case SP_EAGLE -> {
                Vec3 v = getDeltaMovement();
                if (specialTimer < 11) {
                    setDeltaMovement(new Vec3(v.x * 0.96, Math.max(v.y, 0.75) * 0.97 + GRAVITY, v.z * 0.96));
                    sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + R, getZ(), 2, 0.1, 0.1, 0.1, 0.02);
                } else if (specialTimer == 11) {
                    Vec3 tgt = specialTarget != null ? specialTarget : position().add(facingVec(v).scale(20)).add(0, -getY() + 1, 0);
                    Vec3 dir = tgt.subtract(center()).normalize();
                    setDeltaMovement(dir.scale(2.6));
                    playBallSound(ModSounds.KICK_POWER.get(), 1.3F, 1.6F);
                    sl.sendParticles(ParticleTypes.FLASH, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
                    special = SP_NONE;
                }
                return false;
            }
            case SP_LIGHTNING -> {
                if (specialTimer > 32) {
                    special = SP_NONE;
                    return false;
                }
                if (specialTimer % 4 == 0) {
                    Vec3 v = getDeltaMovement();
                    Vec3 p = new Vec3(-v.z, 0, v.x);
                    if (p.lengthSqr() > 1.0E-4) {
                        p = p.normalize();
                        double amt = specialTimer == 4 ? 0.28 : 0.56;
                        double sign = (specialTimer / 4) % 2 == 0 ? 1 : -1;
                        setDeltaMovement(v.add(p.scale(amt * sign)));
                        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + R, getZ(), 12, 0.2, 0.2, 0.2, 0.25);
                    }
                }
                return false;
            }
            case SP_TORNADO -> {
                if (specialTimer > 40) {
                    special = SP_NONE;
                    return false;
                }
                Vec3 v = getDeltaMovement().subtract(specialPrevOffset);
                Vec3 h = new Vec3(v.x, 0, v.z);
                if (h.lengthSqr() < 1.0E-4) {
                    special = SP_NONE;
                    return false;
                }
                h = h.normalize();
                Vec3 p1 = new Vec3(-h.z, 0, h.x);
                double th = specialTimer * 0.9;
                Vec3 off = p1.scale(Math.cos(th) * 0.32).add(0, Math.sin(th) * 0.22, 0);
                setDeltaMovement(v.add(off));
                specialPrevOffset = off;
                sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + R, getZ(), 2, 0.15, 0.15, 0.15, 0.01);
                return false;
            }
            case SP_KNUCKLE -> {
                if (specialTimer > 26 || onGround()) {
                    special = SP_NONE;
                    return false;
                }
                if (specialTimer % 2 == 0) {
                    Vec3 v = getDeltaMovement();
                    Vec3 p = new Vec3(-v.z, 0, v.x);
                    if (p.lengthSqr() > 1.0E-4) {
                        p = p.normalize().scale((random.nextDouble() - 0.5) * 0.11);
                        setDeltaMovement(v.add(p).add(0, (random.nextDouble() - 0.5) * 0.06, 0));
                    }
                }
                return false;
            }
            case SP_ICE -> {
                if (specialTimer > 45 || getDeltaMovement().length() < 0.2) {
                    special = SP_NONE;
                    return false;
                }
                Team mine = o == null ? Team.NONE : MatchManager.teamOf(o);
                for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(2.2), x -> x != o && x.isAlive())) {
                    Team t = MatchManager.teamOf(e);
                    if (mine != Team.NONE && t == mine) continue;
                    if (!(e instanceof Player || e instanceof FootballerEntity)) continue;
                    if (FootData.of(e).stunnedUntil > now) continue;
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 4, false, true));
                    FootData.of(e).stunnedUntil = now + 35;
                    e.setTicksFrozen(Math.max(e.getTicksFrozen(), 120));
                    sl.sendParticles(ParticleTypes.SNOWFLAKE, e.getX(), e.getY() + 1, e.getZ(), 40, 0.4, 0.8, 0.4, 0.05);
                    playBallSound(net.minecraft.sounds.SoundEvents.GLASS_BREAK, 0.8F, 1.4F);
                }
                return false;
            }
            case SP_FIRE -> {
                if (specialTimer > 40 || getDeltaMovement().length() < 0.4) special = SP_NONE;
                return false;
            }
            default -> {
                special = SP_NONE;
                return false;
            }
        }
    }

    private static Vec3 facingVec(Vec3 v) {
        Vec3 h = new Vec3(v.x, 0, v.z);
        return h.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : h.normalize();
    }

    public static Vec3 facing(LivingEntity e) { return Vec3.directionFromRotation(0, e.getYRot()); }

    private void sound(SoundEvent s, float vol, float pitch, long now) {
        if (now - lastSoundTick < 3) return;
        lastSoundTick = now;
        playBallSound(s, vol, pitch);
    }

    // ------------------------------------------------------------------ client

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport) {
        this.lx = x;
        this.ly = y;
        this.lz = z;
        this.lerpSteps = teleport ? 1 : 2;
    }

    private void clientTick() {
        prevRot.set(rot);
        LivingEntity ctrl = getController();
        if (ctrl != null && this.entityData.get(DATA_HOLDER) < 0) {
            // client-side dribble prediction so the ball sticks to the feet without network lag
            Vec3 f = facing(ctrl);
            Vec3 cp = ctrl.position();
            Vec3 vel = clientCtrlPos == null ? Vec3.ZERO : cp.subtract(clientCtrlPos);
            clientCtrlPos = cp;
            double sp = Math.min(vel.horizontalDistance(), 0.45);
            clientPhase += sp * 1.25;
            double reach = 0.62 + sp * 1.6 + Math.max(0, Math.sin(clientPhase)) * Math.min(sp * 2.5, 0.45);
            if (ctrl.isShiftKeyDown()) reach = 0.38;
            double tx = cp.x + f.x * reach + vel.x, tz = cp.z + f.z * reach + vel.z;
            double ty = lerpSteps > 0 ? ly : getY();
            setPos(getX() + (tx - getX()) * 0.55, getY() + (ty - getY()) * 0.5, getZ() + (tz - getZ()) * 0.55);
            lerpSteps = 0;
        } else {
            clientCtrlPos = null;
            if (lerpSteps > 0) {
                double nx = getX() + (lx - getX()) / lerpSteps;
                double ny = getY() + (ly - getY()) / lerpSteps;
                double nz = getZ() + (lz - getZ()) / lerpSteps;
                lerpSteps--;
                setPos(nx, ny, nz);
            }
        }
        Vec3 d = new Vec3(getX() - xo, getY() - yo, getZ() - zo);
        double h = d.horizontalDistance();
        if (h > 1.0E-4) {
            Quaternionf inc = new Quaternionf().rotateAxis((float) (h / R), (float) (d.z / h), 0F, (float) (-d.x / h));
            rot.premul(inc);
        }
        Vector3f s = getSpinData();
        if (Math.abs(s.y) > 0.05 && !onGround()) rot.premul(new Quaternionf().rotateY(s.y * 0.35F));
        rot.normalize();
        spawnTrail(d);
    }

    private void spawnTrail(Vec3 d) {
        int t = getTrail();
        double sp = d.length();
        if (t == TR_NONE || sp < 0.15) return;
        double x = getX(), y = getY() + R, z = getZ();
        Level l = level();
        switch (t) {
            case TR_FAST -> {
                if (random.nextInt(2) == 0) l.addParticle(ParticleTypes.CLOUD, x, y, z, 0, 0, 0);
                l.addParticle(ParticleTypes.CRIT, x, y, z, 0, 0, 0);
            }
            case TR_FIRE -> {
                for (int i = 0; i < 4; i++)
                    l.addParticle(ParticleTypes.FLAME, x + rnd(0.25), y + rnd(0.25), z + rnd(0.25), -d.x * 0.1, 0.02, -d.z * 0.1);
                l.addParticle(ParticleTypes.LAVA, x, y, z, 0, 0, 0);
                l.addParticle(ParticleTypes.LARGE_SMOKE, x, y, z, 0, 0.02, 0);
            }
            case TR_LIGHTNING -> {
                for (int i = 0; i < 3; i++)
                    l.addParticle(ParticleTypes.ELECTRIC_SPARK, x + rnd(0.3), y + rnd(0.3), z + rnd(0.3), rnd(0.3), rnd(0.3), rnd(0.3));
                l.addParticle(new DustParticleOptions(new Vector3f(0.55F, 0.9F, 1.0F), 1.6F), x, y, z, 0, 0, 0);
            }
            case TR_TORNADO -> {
                double a = tickCount * 0.9;
                for (int i = 0; i < 3; i++) {
                    double aa = a + i * 2.094;
                    l.addParticle(ParticleTypes.SWEEP_ATTACK, x + Math.cos(aa) * 0.5, y + Math.sin(aa) * 0.5, z + Math.sin(aa) * 0.5, 0, 0, 0);
                }
                l.addParticle(ParticleTypes.CLOUD, x, y, z, rnd(0.05), rnd(0.05), rnd(0.05));
            }
            case TR_EAGLE -> {
                l.addParticle(ParticleTypes.END_ROD, x, y, z, rnd(0.05), rnd(0.05), rnd(0.05));
                l.addParticle(new DustParticleOptions(new Vector3f(1.0F, 0.82F, 0.25F), 2.0F), x, y, z, 0, 0, 0);
            }
            case TR_ICE -> {
                l.addParticle(ParticleTypes.SNOWFLAKE, x + rnd(0.2), y + rnd(0.2), z + rnd(0.2), 0, 0, 0);
                l.addParticle(new DustParticleOptions(new Vector3f(0.7F, 0.95F, 1.0F), 1.4F), x, y, z, 0, 0, 0);
            }
            case TR_CURL -> l.addParticle(new DustParticleOptions(new Vector3f(1.0F, 1.0F, 1.0F), 0.9F), x, y, z, 0, 0, 0);
            case TR_KNUCKLE -> l.addParticle(new DustParticleOptions(new Vector3f(0.6F, 0.6F, 0.65F), 1.2F), x, y, z, 0, 0, 0);
            case TR_GOLD -> {
                l.addParticle(new DustParticleOptions(new Vector3f(1.0F, 0.85F, 0.2F), 1.0F), x, y, z, 0, 0, 0);
                if (random.nextInt(3) == 0) l.addParticle(ParticleTypes.WAX_ON, x, y, z, 0, 0, 0);
            }
            default -> {}
        }
    }

    private double rnd(double s) { return (random.nextDouble() - 0.5) * 2 * s; }

    // ------------------------------------------------------------------ interaction

    @Override
    public boolean skipAttackInteraction(Entity attacker) {
        if (!level().isClientSide && attacker instanceof Player p) MoveExecutor.basicKick(p, this);
        return true;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            if (!level().isClientSide) {
                if (MatchManager.isMatchBall(this)) {
                    player.displayClientMessage(Component.literal("Maç topu alınamaz!").withStyle(net.minecraft.ChatFormatting.RED), true);
                    return InteractionResult.FAIL;
                }
                if (!player.getAbilities().instabuild) player.getInventory().add(new ItemStack(ModItems.FOOTBALL.get()));
                discard();
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) { return false; }

    @Override
    public boolean isPickable() { return isAlive(); }

    @Override
    public boolean isPushable() { return false; }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) { return d < 128 * 128; }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() { return new ClientboundAddEntityPacket(this); }

    public static float clampf(float v, float lo, float hi) { return Mth.clamp(v, lo, hi); }
}
