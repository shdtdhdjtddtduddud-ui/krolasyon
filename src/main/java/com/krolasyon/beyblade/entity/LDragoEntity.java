package com.krolasyon.beyblade.entity;

import com.krolasyon.beyblade.registry.ModItems;
import com.krolasyon.beyblade.registry.ModSounds;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;

/**
 * L-Drago Destruction F:S. A LEFT-spinning attack type: when it grinds against a right-spinning top it steals part of
 * its rotation (handled in {@link BeybladeEntity}). Special moves are all dragon/fire themed and play out very
 * differently from Libra's sound-based ones.
 */
public class LDragoEntity extends BeybladeEntity {
    public static final int DR_AWAKEN = 1, DR_VORTEX = 2, DR_METEOR = 3, DR_CLAW = 4, DR_ROAR = 5;
    public static final int[] DR_DURATION = {0, 0, 100, 130, 54, 84};
    public static final int[] DR_COOLDOWN = {0, 600, 500, 700, 400, 900};
    public static final int AWAKEN_TICKS = 200;
    @Nullable private BeybladeEntity grabbed;
    private int grabStart;
    @Override
    protected void endAbility() {
        if (grabbed != null) {
            grabbed.setNoGravity(false);
            grabbed = null;
        }
        super.endAbility();
    }

    /** client: when the dive caught its target */
    public int grabTime = -1000;

    public static final double VORTEX_RADIUS = 7.5;
    public static final int M_RISE = 8, M_HANG = 30, M_DIVE = 44;
    public static final double M_HEIGHT = 16, M_RADIUS = 6.5;
    public static final int CLAW_FIRST = 4, CLAW_STEP = 9, CLAW_PASSES = 5, CLAW_UPPER = 48;
    public static final int R_BREATH = 20, R_END = 62;
    public static final double R_LENGTH = 13, R_HALF_ANGLE = Math.toRadians(26);

    public static final byte EV_CLAW = 70, EV_AWAKEN = 71, EV_UPPER = 72, EV_GRAB_UP = 73;
    public static final int GRAB_UP = 16;

    private static final DustParticleOptions CRIMSON = new DustParticleOptions(new Vector3f(0.85F, 0.03F, 0.12F), 1.6F);
    private static final DustParticleOptions EMBER = new DustParticleOptions(new Vector3f(1.0F, 0.45F, 0.05F), 1.2F);
    private static final DustParticleOptions SHADOW = new DustParticleOptions(new Vector3f(0.12F, 0.0F, 0.04F), 2.0F);

    // client
    public int awakenTime = -1000;
    public final int[] clawTimes = {-1000, -1000, -1000, -1000, -1000, -1000};
    public final double[][] clawPos = new double[6][3];
    public final float[] clawRot = new float[6];
    private int clawIndex;

    public LDragoEntity(EntityType<? extends LDragoEntity> type, Level level) {
        super(type, level);
    }

    // ---------------- hooks ----------------
    @Override public int spinDir() { return -1; }
    /** Reverse Slip: the left-spinning top slides off the hit and steals spin as it goes */
    @Override protected void onDodge(BeybladeEntity attacker) {
        float st = Math.min(40F, attacker.getSpin());
        attacker.damageSpin(st, this);
        healSpin(st);
    }
    @Override public int nativeMode() { return MODE_ATTACK; }
    @Override public String kindId() { return "ldrago"; }
    @Override public String abilityKey(int id) { return "ability.librabeyblade.ldrago." + id; }
    @Override public int duration(int id) { return DR_DURATION[id]; }
    @Override public int cooldown(int id) { return DR_COOLDOWN[id]; }
    @Override public int buffAbility() { return DR_AWAKEN; }
    @Override public int buffTicks() { return AWAKEN_TICKS; }
    @Override public boolean requiresTarget(int id) { return id == DR_CLAW || id == DR_ROAR; }
    @Override public boolean buffReflects() { return false; }
    @Override public float buffAttackMul() { return isShielded() ? 1.8F : 1F; }
    @Override public double buffSpeedMul() { return isShielded() ? 1.45 : 1.0; }
    @Override protected ItemStack defaultItem() { return new ItemStack(ModItems.LDRAGO.get()); }

    @Override
    protected float clientSpinSpeed(int ab, float at, float speed) {
        if (ab == DR_VORTEX) return 100F;
        if (ab == DR_METEOR && at >= M_RISE) return 110F;
        return isShielded() ? speed * 1.2F : speed;
    }

    /** Dragon Emperor Awakening: roar shockwave, then 10 s of +80% impact damage and +45% speed */
    @Override
    protected void onBuffStart(@Nullable Player owner) {
        play(ModSounds.DRAGON_ROAR.get(), 2.0F, 1.0F);
        this.level().broadcastEntityEvent(this, EV_AWAKEN);
        ServerLevel sl = (ServerLevel) this.level();
        Px.send(sl, ParticleTypes.FLAME, getX(), getY() + 0.4, getZ(), 40, 0.4, 0.3, 0.4, 0.25);
        Px.send(sl, CRIMSON, getX(), getY() + 0.6, getZ(), 30, 0.8, 0.6, 0.8, 0);
        for (Entity e : enemiesWithin(4.5)) {
            Vec3 out = flat(e.position().subtract(position()));
            out = out.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : out.normalize();
            if (e instanceof BeybladeEntity b) {
                b.damageSpin(40F, this);
                b.setDeltaMovement(b.getDeltaMovement().add(out.scale(0.65)).add(0, 0.15, 0));
            } else if (e instanceof LivingEntity l) {
                l.hurt(damageSources().indirectMagic(this, owner), 4F);
                l.knockback(1.0, -out.x, -out.z);
            }
        }
    }

    @Override
    protected void tickBuff() {
        if (this.tickCount % 4 == 0)
            Px.send((ServerLevel) this.level(), ParticleTypes.FLAME, getX(), getY() + 0.3, getZ(), 2, 0.3, 0.1, 0.3, 0.02);
    }

    // ---------------- abilities ----------------
    @Override
    protected boolean tickAbility(int ab, int t, @Nullable Player owner) {
        ServerLevel sl = (ServerLevel) this.level();
        switch (ab) {
            case DR_VORTEX -> {
                if (t == 1) play(ModSounds.VORTEX.get(), 1.8F, 1.0F);
                if (t % 20 == 10) play(ModSounds.DRAIN.get(), 1.2F, 0.7F);
                setDeltaMovement(getDeltaMovement().multiply(0.6, 1, 0.6));
                for (Entity e : enemiesWithin(VORTEX_RADIUS)) {
                    Vec3 rel = flat(e.position().subtract(position()));
                    double d = rel.length();
                    Vec3 dir = d < 1.0E-3 ? new Vec3(1, 0, 0) : rel.scale(1 / d);
                    // counter-clockwise (left) whirl, dragged inward
                    Vec3 swirl = new Vec3(dir.z, 0, -dir.x).scale(-0.26).add(dir.scale(d > 1.6 ? -0.07 : 0.05));
                    Vec3 v = e.getDeltaMovement();
                    e.setDeltaMovement(new Vec3(v.x * 0.6 + swirl.x, v.y, v.z * 0.6 + swirl.z));
                    e.hurtMarked = true;
                    if (e instanceof BeybladeEntity b) {
                        float steal = Math.min(3.5F, b.getSpin());
                        b.damageSpin(steal, this);
                        healSpin(steal * 0.7F);
                    } else if (e instanceof LivingEntity l && t % 10 == 0) {
                        l.hurt(damageSources().indirectMagic(this, owner), 2F);
                        l.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2));
                    }
                }
                return true;
            }
            case DR_METEOR -> {
                if (t < M_RISE) {
                    setDeltaMovement(getDeltaMovement().multiply(0.5, 1, 0.5));
                    return true;
                }
                if (t == M_RISE) {
                    play(ModSounds.DRAGON_RISE.get(), 2.0F, 1.0F);
                    Px.send(sl, ParticleTypes.LAVA, getX(), getY() + 0.2, getZ(), 12, 0.5, 0.1, 0.5, 0);
                }
                if (t < M_DIVE) {
                    if (t == M_HANG) play(ModSounds.METEOR_CHARGE.get(), 2.0F, 1.0F);
                    hover(M_HEIGHT, t < M_HANG ? 0.22 : 0.3);
                    return true;
                }
                if (t == M_DIVE) {
                    Vec3 aim = target != null ? target.position() : new Vec3(getX(), abilityBaseY, getZ());
                    Vec3 d = aim.subtract(position());
                    if (d.lengthSqr() < 1.0E-4) d = new Vec3(0, -1, 0);
                    setNoGravity(true);
                    setDeltaMovement(d.normalize().scale(2.2));
                    diving = true;
                    play(ModSounds.DRAGON_ROAR.get(), 1.6F, 1.3F);
                }
                if (diving && target != null && target.isAlive()) {
                    // homing dive
                    Vec3 d = target.position().add(0, 0.3, 0).subtract(position());
                    if (d.lengthSqr() > 1.0E-4) setDeltaMovement(d.normalize().scale(2.2));
                }
                if (diving && target instanceof BeybladeEntity tb && target.distanceToSqr(this) < 1.8 * 1.8) {
                    // caught it: both shoot up into the sky together...
                    diving = false;
                    grabbed = tb;
                    grabStart = t;
                    tb.stun(40);
                    tb.damageSpin(60F * powerMul(), this);
                    play(ModSounds.CLASH_HEAVY.get(), 2.5F, 0.6F);
                    play(ModSounds.DRAGON_ROAR.get(), 2.0F, 0.9F);
                    this.level().broadcastEntityEvent(this, EV_GRAB_UP);
                    this.level().broadcastEntityEvent(tb, EV_HEAVY_HIT);
                }
                if (grabbed != null) {
                    int g = t - grabStart;
                    if (!grabbed.isAlive()) { grabbed = null; setNoGravity(false); return true; }
                    if (g < GRAB_UP) {
                        setNoGravity(true);
                        setDeltaMovement(0, 0.55 * (1 - g / (double) GRAB_UP) + 0.1, 0);
                    } else if (g < GRAB_UP + 4) {
                        setDeltaMovement(0, 0, 0);
                    } else {
                        // ...then L-Drago drives it down and smashes it into the ground
                        setDeltaMovement(0, -1.9, 0);
                        if (g == GRAB_UP + 4) play(ModSounds.METEOR_CHARGE.get(), 2.0F, 1.4F);
                    }
                    grabbed.setNoGravity(true);
                    grabbed.setPos(getX(), getY() - 0.75, getZ());
                    grabbed.setDeltaMovement(getDeltaMovement());
                    grabbed.hurtMarked = true;
                    boolean landed = g > GRAB_UP + 4 && (grabbed.onGround() || onGround() || verticalCollision || g > GRAB_UP + 22);
                    if (landed) {
                        grabbed.setNoGravity(false);
                        grabbed.damageSpin(280F * powerMul(), this);
                        grabbed.shockwave(this, 1.0);
                        grabbed = null;
                        setNoGravity(false);
                        meteorImpact(owner);
                        abilityTicks = Math.max(abilityTicks, DR_DURATION[DR_METEOR] - 18);
                    }
                    return true;
                }
                if (diving && (onGround() || verticalCollision || horizontalCollision || t > M_DIVE + 20)) {
                    diving = false;
                    setNoGravity(false);
                    meteorImpact(owner);
                    abilityTicks = Math.max(abilityTicks, DR_DURATION[DR_METEOR] - 18);
                }
                return true;
            }
            case DR_CLAW -> {
                if (target == null || !target.isAlive()) {
                    endAbilityEarly();
                    return true;
                }
                setNoGravity(t < CLAW_UPPER + 2);
                int rel = t - CLAW_FIRST;
                if (rel >= 0 && rel < CLAW_STEP * CLAW_PASSES) {
                    int k = rel / CLAW_STEP, s = rel % CLAW_STEP;
                    if (s == 0) {
                        // streak past the target, slightly offset, alternating sides
                        Vec3 tp = target.position();
                        Vec3 through = flat(tp.subtract(position()));
                        through = through.lengthSqr() < 1.0E-3 ? new Vec3(1, 0, 0) : through.normalize();
                        Vec3 side = new Vec3(-through.z, 0, through.x).scale(k % 2 == 0 ? 0.75 : -0.75);
                        Vec3 to = tp.add(through.scale(2.2)).add(side).add(0, 0.15, 0);
                        setDeltaMovement(to.subtract(position()).scale(1 / 3.0));
                        play(ModSounds.CLAW_SLASH.get(), 1.2F, 0.9F + k * 0.08F);
                    } else if (s == 2) {
                        clawHit(owner, 45F, false);
                    } else if (s >= 3) {
                        setDeltaMovement(getDeltaMovement().scale(0.35));
                    }
                } else if (t == CLAW_UPPER) {
                    Vec3 d = target.position().subtract(position());
                    setDeltaMovement(d.scale(0.5).add(0, 0.4, 0));
                    play(ModSounds.CLAW_SLASH.get(), 1.5F, 0.7F);
                } else if (t == CLAW_UPPER + 1) {
                    clawHit(owner, 70F, true);
                }
                return true;
            }
            case DR_ROAR -> {
                if (target == null || !target.isAlive()) {
                    if (t < R_BREATH) endAbilityEarly();
                    return true;
                }
                if (t == 1) play(ModSounds.DRAGON_ROAR.get(), 2.2F, 0.85F);
                if (t < R_END) hover(2.2, 0.2); else setNoGravity(false);
                if (t == R_BREATH) play(ModSounds.FIRE_BREATH.get(), 2.0F, 1.0F);
                if (t >= R_BREATH && t < R_END) {
                    Vec3 from = position().add(0, 0.4, 0);
                    Vec3 dir = breathDir(t, from, target.position().add(0, target.getBbHeight() * 0.5, 0));
                    for (Entity e : enemiesWithin(R_LENGTH + 1)) {
                        Vec3 c = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(from);
                        double along = c.dot(dir);
                        if (along < 0 || along > R_LENGTH) continue;
                        double ang = Math.acos(Mth.clamp(along / Math.max(1.0E-4, c.length()), -1, 1));
                        if (ang > R_HALF_ANGLE) continue;
                        if (e instanceof BeybladeEntity b) {
                            b.damageSpin(7F, this);
                            b.setDeltaMovement(b.getDeltaMovement().add(flat(dir).scale(0.05)));
                        } else if (e instanceof LivingEntity l && t % 5 == 0) {
                            l.hurt(damageSources().indirectMagic(this, owner), 2F);
                            l.setSecondsOnFire(5);
                        }
                    }
                    if (t % 3 == 0) {
                        Vec3 p = from.add(dir.scale(R_LENGTH * 0.7));
                        Px.send(sl, ParticleTypes.LAVA, p.x, p.y, p.z, 2, 1.0, 0.6, 1.0, 0);
                    }
                }
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    /** breath direction toward the target with a slow side-to-side sweep (same formula on client and server) */
    public static Vec3 breathDir(float t, Vec3 from, Vec3 to) {
        Vec3 d = to.subtract(from);
        if (d.lengthSqr() < 1.0E-4) d = new Vec3(1, 0, 0);
        d = d.normalize();
        double sweep = Math.toRadians(15) * Math.sin((t - R_BREATH) * 0.16);
        double c = Math.cos(sweep), s = Math.sin(sweep);
        return new Vec3(d.x * c - d.z * s, d.y, d.x * s + d.z * c).normalize();
    }

    private void clawHit(@Nullable Player owner, float spin, boolean upper) {
        if (target == null || target.distanceToSqr(this) > 3.2 * 3.2) return;
        Vec3 side = flat(target.position().subtract(position()));
        side = side.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : side.normalize();
        if (target instanceof BeybladeEntity b) {
            b.damageSpin(spin * (isShielded() ? 1.4F : 1F), this);
            b.setDeltaMovement(b.getDeltaMovement().add(side.scale(0.35)).add(0, upper ? 0.75 : 0.08, 0));
        } else if (target instanceof LivingEntity l) {
            l.hurt(damageSources().indirectMagic(this, owner), upper ? 8F : 5F);
            l.knockback(0.6, -side.x, -side.z);
            if (upper) l.setDeltaMovement(l.getDeltaMovement().add(0, 0.6, 0));
        }
        ServerLevel sl = (ServerLevel) this.level();
        Px.send(sl, ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + 0.5, target.getZ(), 1, 0, 0, 0, 0);
        Px.send(sl, ParticleTypes.CRIT, target.getX(), target.getY() + 0.5, target.getZ(), 12, 0.2, 0.2, 0.2, 0.5);
        Px.send(sl, CRIMSON, target.getX(), target.getY() + 0.5, target.getZ(), 10, 0.3, 0.3, 0.3, 0);
        this.level().broadcastEntityEvent(this, upper ? EV_UPPER : EV_CLAW);
    }

    private void meteorImpact(@Nullable Player owner) {
        ServerLevel sl = (ServerLevel) this.level();
        play(ModSounds.METEOR_IMPACT.get(), 3.0F, 1.0F);
        this.level().broadcastEntityEvent(this, EV_IMPACT);
        Px.send(sl, ParticleTypes.LAVA, getX(), getY() + 0.3, getZ(), 30, 2.0, 0.3, 2.0, 0);
        Px.send(sl, CRIMSON, getX(), getY() + 0.5, getZ(), 40, 2.5, 0.5, 2.5, 0);
        for (int i = 0; i < 60; i++) {
            double a = i / 60.0 * Math.PI * 2;
            Px.send(sl, ParticleTypes.FLAME, getX() + Math.cos(a), getY() + 0.15, getZ() + Math.sin(a), 0, Math.cos(a), 0.08, Math.sin(a), 0.6);
        }
        for (Entity e : enemiesWithin(M_RADIUS)) {
            Vec3 out = flat(e.position().subtract(position()));
            out = out.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : out.normalize();
            double falloff = 1.0 - Math.min(1.0, e.distanceTo(this) / M_RADIUS) * 0.5;
            if (e instanceof BeybladeEntity b) {
                b.damageSpin((float) (220 * falloff), this);
                b.setDeltaMovement(b.getDeltaMovement().add(out.scale(1.0 * falloff)).add(0, 0.45, 0));
            } else if (e instanceof LivingEntity l) {
                l.hurt(damageSources().indirectMagic(this, owner), (float) (14 * falloff));
                l.setSecondsOnFire(6);
                l.knockback(1.4 * falloff, -out.x, -out.z);
            }
        }
    }

    // ---------------- client ----------------
    @Override
    public void handleEntityEvent(byte id) {
        switch (id) {
            case EV_AWAKEN -> awakenTime = this.tickCount;
            case EV_GRAB_UP -> grabTime = this.tickCount;
            case EV_CLAW, EV_UPPER -> {
                Entity t = this.level().getEntity(getTargetId());
                int i = clawIndex++ % clawTimes.length;
                clawTimes[i] = this.tickCount;
                Entity at = t != null ? t : this;
                clawPos[i][0] = at.getX();
                clawPos[i][1] = at.getY() + 0.45 + (id == EV_UPPER ? 0.4 : 0);
                clawPos[i][2] = at.getZ();
                clawRot[i] = id == EV_UPPER ? 90F : this.random.nextFloat() * 360F;
                for (int k = 0; k < 12; k++)
                    Px.add(this.level(), EMBER, at.getX(), at.getY() + 0.5, at.getZ(), (this.random.nextDouble() - 0.5) * 0.6, this.random.nextDouble() * 0.4, (this.random.nextDouble() - 0.5) * 0.6);
            }
            default -> super.handleEntityEvent(id);
        }
    }

    @Override
    protected void clientBuffParticles(Level lv) {
        double a = this.random.nextDouble() * Math.PI * 2;
        Px.add(lv, ParticleTypes.FLAME, getX() + Math.cos(a) * 0.45, getY() + 0.25, getZ() + Math.sin(a) * 0.45, 0, 0.05, 0);
        if (this.random.nextInt(2) == 0)
            Px.add(lv, CRIMSON, getX() + Math.cos(a + 1) * 0.6, getY() + 0.5, getZ() + Math.sin(a + 1) * 0.6, 0, 0.04, 0);
    }

    @Override
    protected void clientAbilityParticles(int ab, float at, Level lv) {
        switch (ab) {
            case DR_VORTEX -> {
                for (int i = 0; i < 4; i++) {
                    double a = this.random.nextDouble() * Math.PI * 2, r = 1 + this.random.nextDouble() * (VORTEX_RADIUS - 1);
                    double x = getX() + Math.cos(a) * r, z = getZ() + Math.sin(a) * r;
                    // particles swirl counter-clockwise and inward
                    Px.add(lv, i % 2 == 0 ? SHADOW : CRIMSON, x, getY() + 0.1, z, -Math.sin(a) * -0.3 - Math.cos(a) * 0.1, 0.02, Math.cos(a) * -0.3 - Math.sin(a) * 0.1);
                }
                if (this.tickCount % 2 == 0) {
                    for (Entity e : enemiesWithin(VORTEX_RADIUS)) {
                        double f = this.random.nextDouble();
                        Px.add(lv, CRIMSON, Mth.lerp(f, e.getX(), getX()), Mth.lerp(f, e.getY() + 0.5, getY() + 0.4), Mth.lerp(f, e.getZ(), getZ()),
                                (getX() - e.getX()) * 0.05, 0.02, (getZ() - e.getZ()) * 0.05);
                    }
                }
            }
            case DR_METEOR -> {
                if (at >= M_RISE) {
                    for (int i = 0; i < 4; i++)
                        Px.add(lv, i % 2 == 0 ? ParticleTypes.FLAME : EMBER, getX() + (this.random.nextDouble() - 0.5) * 0.6, getY() + this.random.nextDouble() * 0.6,
                                getZ() + (this.random.nextDouble() - 0.5) * 0.6, 0, at < M_DIVE ? -0.15 : 0.1, 0);
                    if (at >= M_DIVE) Px.add(lv, ParticleTypes.LAVA, getX(), getY() + 0.4, getZ(), 0, 0, 0);
                }
            }
            case DR_ROAR -> {
                Entity t = this.level().getEntity(getTargetId());
                if (t == null) return;
                if (at < R_BREATH) {
                    Px.add(lv, EMBER, getX() + (this.random.nextDouble() - 0.5) * 2, getY() + 0.4 + this.random.nextDouble(), getZ() + (this.random.nextDouble() - 0.5) * 2, 0, 0.02, 0);
                } else if (at < R_END) {
                    Vec3 from = position().add(0, 0.4, 0);
                    Vec3 dir = breathDir(at, from, t.position().add(0, t.getBbHeight() * 0.5, 0));
                    for (int i = 0; i < 6; i++) {
                        double sp = 0.55 + this.random.nextDouble() * 0.5;
                        Vec3 j = new Vec3(this.random.nextGaussian(), this.random.nextGaussian(), this.random.nextGaussian()).scale(0.12);
                        Vec3 v = dir.add(j).scale(sp);
                        Px.add(lv, i % 3 == 0 ? ParticleTypes.SMALL_FLAME : ParticleTypes.FLAME, from.x + dir.x * 0.8, from.y + dir.y * 0.8, from.z + dir.z * 0.8, v.x, v.y, v.z);
                    }
                }
            }
            default -> {}
        }
    }
}
