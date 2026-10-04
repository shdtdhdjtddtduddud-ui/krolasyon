package com.rabona.arena.entity;

import com.rabona.arena.game.Match;
import com.rabona.arena.registry.ModBlocks;
import com.rabona.arena.registry.ModItems;
import com.rabona.arena.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
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
import java.util.Iterator;
import java.util.Map;

/**
 * Futbol topu. Kendi fizik motoru: yercekimi, hava direnci, Magnus (falso), zemin yuvarlanma
 * surtunmesi, sekme katsayilari (direk/file/zemin), oyuncu govdesinden sekme ve top surme (dribbling).
 */
public class BallEntity extends Entity {
    public static final float RADIUS = 0.22f;

    public static final double GRAVITY = 0.034;
    public static final double AIR_DRAG = 0.010;
    public static final double MAGNUS = 0.032;
    public static final double ROLL_FRICTION = 0.952;
    public static final double RESTITUTION = 0.62;

    // efektler (iz/partikul)
    public static final int FX_NONE = 0, FX_FIRE = 1, FX_THUNDER = 2, FX_TORNADO = 3, FX_GHOST = 4, FX_ICE = 5,
            FX_KNUCKLE = 6, FX_POWER = 7, FX_RAINBOW = 8, FX_CURL = 9;

    private static final EntityDataAccessor<Integer> SKIN = SynchedEntityData.defineId(BallEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> EFFECT = SynchedEntityData.defineId(BallEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CONTROLLER = SynchedEntityData.defineId(BallEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> HELD = SynchedEntityData.defineId(BallEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Vector3f> SPIN = SynchedEntityData.defineId(BallEntity.class, EntityDataSerializers.VECTOR3);

    // sunucu durumu
    private final Map<Integer, Integer> immune = new HashMap<>();
    public int lastToucherId = -1, prevToucherId = -1;
    public int effectTicks;
    public int touchTimer;
    public int restrictTeam = -1, restrictUntil;
    public int controlTicks;
    private Vec3 ctrlPrev;
    private Vec3 ctrlVel = Vec3.ZERO;
    public int airDribbleTicks; // fok calimi: top kafada
    public int lastMove = -1;
    /** Pasin alicisi: top ona dogru yon duzeltir (miknatis). */
    public int passTargetId = -1, passTick;
    public boolean juggle;

    // istemci gorsel durumu
    public final Quaternionf rot = new Quaternionf();
    public final Quaternionf rotO = new Quaternionf();
    public final Vec3[] trail = new Vec3[14];
    public int trailHead;
    private double lx, ly, lz;
    private int lSteps;

    public BallEntity(EntityType<? extends BallEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(SKIN, 0);
        entityData.define(EFFECT, 0);
        entityData.define(CONTROLLER, -1);
        entityData.define(HELD, false);
        entityData.define(SPIN, new Vector3f());
    }

    // ---------------------------------------------------------------- erisim
    public int getSkin() { return entityData.get(SKIN); }

    public void setSkin(int s) { entityData.set(SKIN, s); }

    public int getEffect() { return entityData.get(EFFECT); }

    public void setEffect(int fx, int ticks) {
        entityData.set(EFFECT, fx);
        effectTicks = ticks;
    }

    public Vector3f getSpin() { return entityData.get(SPIN); }

    public void setSpin(Vector3f v) { entityData.set(SPIN, new Vector3f(v)); }

    public boolean isHeld() { return entityData.get(HELD); }

    public int getControllerId() { return entityData.get(CONTROLLER); }

    public LivingEntity getController() {
        int id = getControllerId();
        if (id < 0) return null;
        Entity e = level().getEntity(id);
        return e instanceof LivingEntity le && le.isAlive() ? le : null;
    }

    public boolean isFree() { return getControllerId() < 0; }

    public Vec3 center() { return position().add(0, RADIUS, 0); }

    public void setController(LivingEntity e, boolean held) {
        entityData.set(CONTROLLER, e == null ? -1 : e.getId());
        entityData.set(HELD, e != null && held);
        controlTicks = 0;
        ctrlPrev = e == null ? null : e.position();
        ctrlVel = Vec3.ZERO;
        airDribbleTicks = 0;
        juggle = false;
        if (e != null) {
            if (e.getId() == passTargetId) com.rabona.arena.game.MoveLogic.COUNTS.merge("PASS_OK", 1, Integer::sum);
            touched(e);
            passTargetId = -1;
        }
    }

    public void touched(LivingEntity e) {
        if (e.getId() != passTargetId && tickCount - passTick > 2) passTargetId = -1;
        if (e.getId() != lastToucherId) {
            prevToucherId = lastToucherId;
            lastToucherId = e.getId();
        }
        if (!level().isClientSide) Match.onTouch(this, e);
    }

    public void makeImmune(LivingEntity e, int ticks) { immune.put(e.getId(), tickCount + ticks); }

    public boolean isImmune(LivingEntity e) {
        Integer t = immune.get(e.getId());
        return t != null && t > tickCount;
    }

    public void restrict(int teamId, int ticks) {
        restrictTeam = teamId;
        restrictUntil = tickCount + ticks;
    }

    /** Topa vur: kontrol birakilir, hiz/falso atanir. */
    public void kick(LivingEntity by, Vec3 vel, Vector3f spin, int fx, int fxTicks) {
        setController(null, false);
        setDeltaMovement(vel);
        setSpin(spin);
        hasImpulse = true;
        if (by != null) {
            makeImmune(by, 9);
            touched(by);
        }
        setEffect(fx, fxTicks);
        double sp = vel.length();
        SoundEvent s = sp > 1.45 ? ModSounds.KICK_POWER.get() : sp > 0.75 ? ModSounds.KICK_HARD.get() : ModSounds.KICK_SOFT.get();
        level().playSound(null, getX(), getY(), getZ(), s, SoundSource.PLAYERS, (float) Math.min(1.6, 0.6 + sp * 0.5), 0.9f + random.nextFloat() * 0.2f);
        if (level() instanceof ServerLevel sl && sp > 1.2) {
            sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + RADIUS, getZ(), 6, 0.1, 0.1, 0.1, 0.06);
        }
    }

    // ---------------------------------------------------------------- tick
    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            clientTick();
            return;
        }
        immune.values().removeIf(t -> t < tickCount - 40);
        if (effectTicks > 0 && --effectTicks == 0) entityData.set(EFFECT, FX_NONE);

        LivingEntity ctrl = getController();
        if (getControllerId() >= 0 && ctrl == null) setController(null, false);
        if (ctrl != null && (ctrl.isSpectator() || ctrl.isDeadOrDying())) {
            setController(null, false);
            ctrl = null;
        }

        if (ctrl != null && isHeld()) {
            holdTick(ctrl);
            return;
        }
        if (ctrl != null) {
            dribble(ctrl);
        } else {
            homing();
            tryControl();
        }
        physics(true);
        if (getY() < level().getMinBuildHeight() - 16) discard();
    }

    private void holdTick(LivingEntity ctrl) {
        float yaw = ctrl.getYRot() * Mth.DEG_TO_RAD;
        Vec3 f = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
        Vec3 p = ctrl.position().add(f.scale(0.5)).add(0, 0.85, 0);
        setPos(p.x, p.y, p.z);
        setDeltaMovement(ctrl.getDeltaMovement());
        setSpin(new Vector3f());
        if (++controlTicks > 20 * 30) setController(null, false);
    }

    /** Top surme: top oyuncunun onunde tutulur, kosu hizina gore uzaklasir. */
    private void dribble(LivingEntity ctrl) {
        controlTicks++;
        Vec3 cp = ctrl.position();
        if (ctrlPrev != null) {
            Vec3 d = cp.subtract(ctrlPrev);
            if (d.lengthSqr() > 4) d = Vec3.ZERO;
            ctrlVel = ctrlVel.scale(0.5).add(d.scale(0.5));
        }
        ctrlPrev = cp;
        double spd = Math.sqrt(ctrlVel.x * ctrlVel.x + ctrlVel.z * ctrlVel.z);
        float yaw = (ctrl instanceof Player ? ctrl.getYRot() : ctrl.yBodyRot) * Mth.DEG_TO_RAD;
        Vec3 face = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
        if (spd > 0.06) face = face.scale(0.35).add(new Vec3(ctrlVel.x, 0, ctrlVel.z).normalize().scale(0.65)).normalize();

        if (airDribbleTicks > 0) { // fok calimi: top kafanin ustunde zipliyor
            airDribbleTicks--;
            Vec3 head;
            if (juggle) { // dunya turu: top ayak etrafinda doner
                double ph = airDribbleTicks * 0.42;
                Vec3 side = new Vec3(-face.z, 0, face.x);
                head = cp.add(face.scale(0.5 + Math.cos(ph) * 0.18)).add(side.scale(Math.sin(ph) * 0.25))
                        .add(0, 0.15 + Math.abs(Math.sin(airDribbleTicks * 0.3)) * 0.9, 0);
            } else {
                head = cp.add(face.scale(0.15)).add(0, ctrl.getBbHeight() + 0.05 + Math.abs(Math.sin(tickCount * 0.45)) * 0.35, 0);
            }
            setPos(head.x, head.y, head.z);
            setDeltaMovement(ctrlVel);
            if (airDribbleTicks == 0) {
                setDeltaMovement(ctrlVel.add(face.scale(0.15)).add(0, juggle ? 0.1 : 0.2, 0));
                juggle = false;
            }
            return;
        }

        boolean sprint = ctrl.isSprinting() || spd > 0.2;
        double dist = 0.42 + RADIUS + Mth.clamp(spd * 2.4, 0, 0.9) + (sprint ? 0.15 : 0);
        Vec3 target = cp.add(face.scale(dist));
        Vec3 bp = position();
        Vec3 to = new Vec3(target.x - bp.x, 0, target.z - bp.z);
        double far = Math.sqrt((bp.x - cp.x) * (bp.x - cp.x) + (bp.z - cp.z) * (bp.z - cp.z));
        if (far > 2.8 || Math.abs(bp.y - cp.y) > 1.6) {
            setController(null, false);
            return;
        }
        Vec3 v = getDeltaMovement();
        Vec3 desired = to.scale(0.42).add(ctrlVel.x, 0, ctrlVel.z);
        double k = 0.6;
        Vec3 nv = new Vec3(v.x * (1 - k) + desired.x * k, v.y, v.z * (1 - k) + desired.z * k);
        // kosarken kucuk dokunuslar
        if (spd > 0.08 && --touchTimer <= 0) {
            touchTimer = sprint ? 7 : 11;
            nv = nv.add(face.scale(sprint ? 0.07 : 0.04));
            level().playSound(null, getX(), getY(), getZ(), ModSounds.TOUCH.get(), SoundSource.PLAYERS, 0.35f, 0.9f + random.nextFloat() * 0.3f);
        }
        setDeltaMovement(nv);
        Vector3f s = getSpin();
        if (s.lengthSquared() > 1e-4) setSpin(s.mul(0.7f));
    }

    private void tryControl() {
        Vec3 v = getDeltaMovement();
        double sp = v.length();
        AABB box = getBoundingBox().inflate(1.1, 0.6, 1.1);
        LivingEntity best = null;
        double bestD = 99;
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, box, this::canPlay)) {
            if (isImmune(e)) continue;
            Vec3 c = center();
            double dx = c.x - e.getX(), dz = c.z - e.getZ();
            double h = Math.sqrt(dx * dx + dz * dz);
            double rel = c.y - e.getY();
            if (restrictTeam >= 0 && tickCount < restrictUntil && Match.teamOf(e).ordinal() != restrictTeam) {
                if (h < 0.75 && rel < 1.9) bounceOff(e, v);
                continue;
            }
            // hizli top: govdeden seker (ilk kontrol sansli)
            double limit = e.getId() == passTargetId ? 1.6 : 0.62 + skillBonus(e);
            boolean controllable = sp < limit && rel < 0.85 && rel > -0.3;
            if (h < 0.9 && controllable) {
                if (h < bestD) { bestD = h; best = e; }
            } else if (h < 0.55 + RADIUS && rel < e.getBbHeight() + 0.1 && rel > -0.3) {
                bounceOff(e, v);
            }
        }
        if (best != null) {
            setController(best, false);
            restrictTeam = -1;
            setDeltaMovement(getDeltaMovement().scale(0.3));
            level().playSound(null, getX(), getY(), getZ(), ModSounds.TOUCH.get(), SoundSource.PLAYERS, 0.6f, 1.0f);
        }
    }

    /** Pas miknatisi: top, alicinin gidecegi noktaya dogru yumusakca doner ve ona yetecek hizi korur. */
    private void homing() {
        if (passTargetId < 0) return;
        Entity t = level().getEntity(passTargetId);
        if (!(t instanceof LivingEntity target) || !target.isAlive() || tickCount - passTick > 70) {
            passTargetId = -1;
            return;
        }
        Vec3 v = getDeltaMovement();
        double h = Math.sqrt(v.x * v.x + v.z * v.z);
        if (h < 0.05) return;
        Vec3 c = center();
        Vec3 tv = target.getDeltaMovement();
        double dist = Math.sqrt(Math.pow(target.getX() - c.x, 2) + Math.pow(target.getZ() - c.z, 2));
        double eta = Math.min(25, dist / Math.max(0.2, h));
        Vec3 aim = target.position().add(tv.x * eta * 0.6, 0, tv.z * eta * 0.6);
        Vec3 want = new Vec3(aim.x - c.x, 0, aim.z - c.z);
        if (want.lengthSqr() < 1e-4) return;
        want = want.normalize();
        Vec3 cur = new Vec3(v.x / h, 0, v.z / h);
        double maxTurn = Math.toRadians(onGround() ? 5 : 2.5);
        double ang = Math.acos(Mth.clamp(cur.dot(want), -1, 1));
        if (ang > Math.toRadians(70)) return; // ters yondeyse dokunma
        double k = ang < 1e-4 ? 1 : Math.min(1, maxTurn / ang);
        Vec3 dir = cur.scale(1 - k).add(want.scale(k)).normalize();
        double sp = h;
        // yerdeyse aliciya yetecek minimum hiz
        if (onGround()) sp = Math.max(h, Math.min(1.4, 0.18 + dist * 0.048));
        setDeltaMovement(dir.x * sp, v.y, dir.z * sp);
    }

    private double skillBonus(LivingEntity e) {
        return e instanceof FootballerEntity f ? f.getSkill() * 0.004 : 0.15;
    }

    private void bounceOff(LivingEntity e, Vec3 v) {
        Vec3 c = center();
        Vec3 n = new Vec3(c.x - e.getX(), 0, c.z - e.getZ());
        if (n.lengthSqr() < 1e-6) n = new Vec3(random.nextDouble() - 0.5, 0, random.nextDouble() - 0.5);
        n = n.normalize();
        Vec3 ev = e.getDeltaMovement();
        Vec3 rv = v.subtract(ev.x, 0, ev.z);
        double vn = rv.dot(n);
        if (vn >= 0) return;
        Vec3 nv = v.subtract(n.scale((1 + 0.35) * vn)).scale(0.7);
        setDeltaMovement(nv.add(ev.x * 0.5, 0, ev.z * 0.5));
        makeImmune(e, 5);
        touched(e);
        if (v.length() > 0.5) {
            level().playSound(null, getX(), getY(), getZ(), ModSounds.BOUNCE.get(), SoundSource.PLAYERS, 0.8f, 0.8f);
        }
        if (e instanceof FootballerEntity f) f.onBallBlocked(v);
    }

    /** Fizik adimi. server=true ise ses/olaylar uretilir. */
    private void physics(boolean server) {
        Vec3 v = getDeltaMovement();
        Vector3f w = getSpin();
        boolean ground = onGround();

        v = v.add(0, -GRAVITY, 0);

        // Magnus etkisi: a = k (w x v)
        if (w.lengthSquared() > 1e-6) {
            Vec3 wv = new Vec3(w.x, w.y, w.z);
            Vec3 m = wv.cross(v).scale(ground ? 0.25 * MAGNUS : MAGNUS);
            v = v.add(m);
        }
        int fx = getEffect();
        if (server && fx == FX_KNUCKLE && !ground) { // titreyen top
            v = v.add((random.nextDouble() - 0.5) * 0.07, (random.nextDouble() - 0.5) * 0.04, (random.nextDouble() - 0.5) * 0.07);
        }
        if (server && fx == FX_TORNADO && !ground) {
            double t = tickCount * 0.9;
            Vec3 h = new Vec3(v.x, 0, v.z);
            if (h.lengthSqr() > 1e-4) {
                Vec3 side = new Vec3(-h.z, 0, h.x).normalize();
                v = v.add(side.scale(Math.sin(t) * 0.09)).add(0, Math.cos(t) * 0.045, 0);
            }
        }

        double sp = v.length();
        if (ground && Math.abs(v.y) < 0.05) {
            v = new Vec3(v.x * ROLL_FRICTION, v.y, v.z * ROLL_FRICTION);
            if (Math.abs(v.x) < 0.004) v = new Vec3(0, v.y, v.z);
            if (Math.abs(v.z) < 0.004) v = new Vec3(v.x, v.y, 0);
            w.mul(0.85f);
        } else {
            double drag = 1 - AIR_DRAG * sp - 0.003;
            v = v.scale(Math.max(0.9, drag));
            w.mul(0.988f);
        }
        if (fx == FX_FIRE || fx == FX_THUNDER) v = v.scale(1.008); // ozel sutlar yavaslamaz

        Vec3 start = position();
        move(MoverType.SELF, v);
        Vec3 moved = position().subtract(start);

        boolean hx = Math.abs(moved.x - v.x) > 1e-5;
        boolean hy = Math.abs(moved.y - v.y) > 1e-5;
        boolean hz = Math.abs(moved.z - v.z) > 1e-5;
        double nx = v.x, ny = v.y, nz = v.z;
        if (hx) {
            double e = restitution(new Vec3(Math.signum(v.x), 0, 0), server, Math.abs(v.x));
            nx = -v.x * e;
            if (isNet(new Vec3(Math.signum(v.x), 0, 0))) { ny *= 0.5; nz *= 0.5; }
        }
        if (hz) {
            double e = restitution(new Vec3(0, 0, Math.signum(v.z)), server, Math.abs(v.z));
            nz = -v.z * e;
            if (isNet(new Vec3(0, 0, Math.signum(v.z)))) { ny *= 0.5; nx *= 0.5; }
        }
        if (hy) {
            if (v.y < 0) {
                if (-v.y > 0.09) {
                    ny = -v.y * RESTITUTION;
                    // topspin/backspin zeminde ileri/geri firlatir
                    nx += -w.z * 0.22;
                    nz += w.x * 0.22;
                    w.mul(0.55f);
                    if (server && -v.y > 0.2)
                        level().playSound(null, getX(), getY(), getZ(), ModSounds.BOUNCE.get(), SoundSource.PLAYERS, (float) Math.min(1, -v.y * 2), 1.1f);
                } else {
                    ny = 0;
                }
            } else {
                double e = restitution(new Vec3(0, 1, 0), server, v.y);
                ny = -v.y * e;
            }
        }
        setDeltaMovement(nx, ny, nz);
        setSpin(w);
    }

    private boolean isNet(Vec3 dir) {
        BlockPos p = BlockPos.containing(center().add(dir.scale(RADIUS + 0.12)));
        return level().getBlockState(p).is(ModBlocks.GOAL_NET.get());
    }

    private double restitution(Vec3 dir, boolean server, double speed) {
        BlockPos p = BlockPos.containing(center().add(dir.scale(RADIUS + 0.12)));
        BlockState s = level().getBlockState(p);
        if (s.is(ModBlocks.GOAL_NET.get())) {
            if (server && speed > 0.25) {
                level().playSound(null, getX(), getY(), getZ(), ModSounds.NET.get(), SoundSource.PLAYERS, (float) Math.min(1.2, speed), 1f);
            }
            return 0.1;
        }
        if (s.is(ModBlocks.GOAL_POST.get())) {
            if (server && speed > 0.2) {
                level().playSound(null, getX(), getY(), getZ(), ModSounds.POST.get(), SoundSource.PLAYERS, 1.4f, 1f);
                Match.onPostHit(this);
            }
            return 0.78;
        }
        return 0.5;
    }

    // ---------------------------------------------------------------- istemci
    private void clientTick() {
        rotO.set(rot);
        Vec3 before = position();
        if (lSteps > 0) {
            double nx = getX() + (lx - getX()) / lSteps;
            double ny = getY() + (ly - getY()) / lSteps;
            double nz = getZ() + (lz - getZ()) / lSteps;
            lSteps--;
            setPos(nx, ny, nz);
        } else if (getControllerId() < 0) {
            physics(false);
        }
        Vec3 d = position().subtract(before);
        double h = Math.sqrt(d.x * d.x + d.z * d.z);
        if (h > 1e-4) {
            Vector3f axis = new Vector3f((float) d.z, 0, (float) -d.x).normalize();
            rot.premul(new Quaternionf().rotateAxis((float) (h / RADIUS), axis));
        }
        Vector3f w = getSpin();
        float wl = w.length();
        if (wl > 0.01f) rot.premul(new Quaternionf().rotateAxis(wl * 1.6f, new Vector3f(w).div(wl)));
        rot.normalize();

        trailHead = (trailHead + 1) % trail.length;
        trail[trailHead] = center();
        spawnFx(d);
    }

    private void spawnFx(Vec3 d) {
        int fx = getEffect();
        double sp = d.length();
        Vec3 c = center();
        switch (fx) {
            case FX_FIRE -> {
                for (int i = 0; i < 3; i++)
                    level().addParticle(ParticleTypes.FLAME, c.x + rnd(0.2), c.y + rnd(0.2), c.z + rnd(0.2), -d.x * 0.1, 0.02, -d.z * 0.1);
                level().addParticle(ParticleTypes.LARGE_SMOKE, c.x, c.y, c.z, 0, 0.03, 0);
                if (random.nextInt(3) == 0) level().addParticle(ParticleTypes.LAVA, c.x, c.y, c.z, 0, 0, 0);
            }
            case FX_THUNDER -> {
                for (int i = 0; i < 3; i++)
                    level().addParticle(ParticleTypes.ELECTRIC_SPARK, c.x + rnd(0.35), c.y + rnd(0.35), c.z + rnd(0.35), rnd(0.3), rnd(0.3), rnd(0.3));
                level().addParticle(ParticleTypes.END_ROD, c.x, c.y, c.z, 0, 0, 0);
            }
            case FX_TORNADO -> {
                for (int i = 0; i < 4; i++) {
                    double a = tickCount * 0.8 + i * Math.PI / 2;
                    level().addParticle(ParticleTypes.CLOUD, c.x + Math.cos(a) * 0.45, c.y + Math.sin(a) * 0.45, c.z + Math.sin(a) * 0.45, 0, 0, 0);
                }
            }
            case FX_GHOST -> level().addParticle(ParticleTypes.SOUL, c.x + rnd(0.2), c.y + rnd(0.2), c.z + rnd(0.2), 0, 0.02, 0);
            case FX_ICE -> {
                level().addParticle(ParticleTypes.SNOWFLAKE, c.x + rnd(0.25), c.y + rnd(0.25), c.z + rnd(0.25), 0, -0.02, 0);
                level().addParticle(ParticleTypes.ITEM_SNOWBALL, c.x, c.y, c.z, 0, 0, 0);
            }
            case FX_RAINBOW -> level().addParticle(ParticleTypes.END_ROD, c.x, c.y, c.z, 0, 0, 0);
            default -> {
                if (sp > 1.2 && random.nextInt(2) == 0) level().addParticle(ParticleTypes.WHITE_ASH, c.x, c.y, c.z, 0, 0, 0);
            }
        }
        if (onGround() && sp > 0.5 && random.nextInt(3) == 0) {
            level().addParticle(ParticleTypes.COMPOSTER, c.x, getY() + 0.05, c.z, 0, 0.05, 0);
        }
    }

    private double rnd(double s) { return (random.nextDouble() - 0.5) * 2 * s; }

    @Override
    public void lerpTo(double x, double y, double z, float yr, float xr, int steps, boolean tp) {
        lx = x;
        ly = y;
        lz = z;
        lSteps = tp ? 1 : 2;
        if (position().distanceToSqr(x, y, z) > 64) {
            setPos(x, y, z);
            lSteps = 0;
        }
    }

    // ---------------------------------------------------------------- etkilesim
    @Override
    public boolean isPickable() { return !isRemoved(); }

    @Override
    public boolean isAttackable() { return true; }

    @Override
    public boolean canBeCollidedWith() { return false; }

    @Override
    public boolean isPushable() { return false; }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (isInvulnerableTo(src)) return false;
        if (level().isClientSide) return true;
        if (src.getEntity() instanceof LivingEntity le && !isHeld()) {
            // sol tik: hizli vurus
            Vec3 look = le.getLookAngle();
            Vec3 dir = new Vec3(look.x, 0, look.z).normalize();
            double lift = Mth.clamp(-look.y, -0.1, 0.6) * 0.8 + 0.12;
            boolean strong = le.isSprinting();
            kick(le, dir.scale(strong ? 1.15 : 0.8).add(0, lift, 0), new Vector3f(), strong ? FX_POWER : FX_NONE, 20);
            return true;
        }
        if (src.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) {
            setDeltaMovement(getDeltaMovement().add(0, 0.8, 0));
            return true;
        }
        return false;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!level().isClientSide) {
            if (player.isShiftKeyDown()) {
                ItemStack st = new ItemStack(ModItems.BALLS.get(Mth.clamp(getSkin(), 0, ModItems.BALLS.size() - 1)).get());
                if (!player.getInventory().add(st)) spawnAtLocation(st);
                discard();
            } else {
                // sag tik: top sektirme (juggling)
                setController(null, false);
                setDeltaMovement(player.getLookAngle().scale(0.05).add(0, 0.42, 0));
                makeImmune(player, 6);
                touched(player);
                level().playSound(null, getX(), getY(), getZ(), ModSounds.TOUCH.get(), SoundSource.PLAYERS, 0.7f, 1.2f);
            }
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.BALLS.get(Mth.clamp(getSkin(), 0, ModItems.BALLS.size() - 1)).get());
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) { return d < 160 * 160; }

    public boolean canPlay(LivingEntity e) {
        if (e.isSpectator() || !e.isAlive()) return false;
        if (e instanceof Player p) return !p.isCreative() || Match.teamOf(p).playing() || !Match.isRunning();
        return e instanceof FootballerEntity;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag t) {
        setSkin(t.getInt("Skin"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag t) {
        t.putInt("Skin", getSkin());
    }

    /** Iz cizimi icin gecmis konumlar (en yeniden eskiye). */
    public Iterator<Vec3> trailIterator() {
        return new Iterator<>() {
            int i = 0;

            public boolean hasNext() {
                return i < trail.length && trail[(trailHead - i + trail.length) % trail.length] != null;
            }

            public Vec3 next() {
                return trail[(trailHead - i++ + trail.length) % trail.length];
            }
        };
    }
}
