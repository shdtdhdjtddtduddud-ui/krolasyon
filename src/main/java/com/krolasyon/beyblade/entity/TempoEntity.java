package com.krolasyon.beyblade.entity;

import com.krolasyon.beyblade.registry.ModItems;
import com.krolasyon.beyblade.registry.ModSounds;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Twisted Tempo 145WD. A balance type ruled by the clock: it stops time around itself, rewinds its own spin, sweeps the
 * stadium with giant clock hands, speeds up its own time for a barrage, and finally passes judgment with the great clock.
 */
public class TempoEntity extends BeybladeEntity {
    public static final int TE_STOP = 1, TE_REWIND = 2, TE_HANDS = 3, TE_ACCEL = 4, TE_CLOCK = 5;
    public static final int[] TE_DURATION = {0, 0, 24, 52, 56, 116};
    public static final int[] TE_COOLDOWN = {0, 650, 500, 550, 450, 1100};
    public static final int STOP_TICKS = 140;
    public static final double STOP_RADIUS = 9, HANDS_LONG = 7.5, HANDS_SHORT = 5.5, CLOCK_RANGE = 12, CLOCK_HEIGHT = 7;
    public static final int HANDS_START = 6, HANDS_END = 48, ACCEL_STEP = 4, ACCEL_HITS = 12, CLOCK_FORM = 14, CLOCK_TOLL = 92;

    public static final byte EV_TOLL = 120, EV_REWIND = 121;

    // server
    private final ArrayDeque<double[]> history = new ArrayDeque<>();
    private final List<double[]> rewindPath = new ArrayList<>();
    private final Map<Integer, Integer> handHits = new HashMap<>();
    private int accelHits;

    // client
    public int tollTime = -1000, rewindTime = -1000;

    public TempoEntity(EntityType<? extends TempoEntity> type, Level level) {
        super(type, level);
    }

    // ---------------- hooks ----------------
    @Override public int nativeMode() { return MODE_BALANCE; }
    @Override public String kindId() { return "tempo"; }
    @Override public String abilityKey(int id) { return "ability.librabeyblade.tempo." + id; }
    @Override public int duration(int id) { return TE_DURATION[id]; }
    @Override public int cooldown(int id) { return TE_COOLDOWN[id]; }
    @Override public int buffAbility() { return TE_STOP; }
    @Override public int buffTicks() { return STOP_TICKS; }
    @Override public boolean requiresTarget(int id) { return id == TE_ACCEL; }
    @Override public boolean buffReflects() { return false; }
    @Override protected boolean ramsInto(int id) { return id == TE_HANDS || id == TE_ACCEL || id == TE_CLOCK; }
    @Override protected float dodgeChance() { return isBuffActive() ? 0.3F : 0.12F; }
    @Override protected ItemStack defaultItem() { return new ItemStack(ModItems.TEMPO.get()); }

    @Override
    protected float clientSpinSpeed(int ab, float at, float speed) {
        if (ab == TE_REWIND) return -speed * 1.3F;        // the top visibly spins backwards while time rewinds
        if (ab == TE_ACCEL) return speed * 1.5F;
        return speed;
    }

    /** Time Stop: everything around Tempo is frozen in time */
    @Override
    protected void onBuffStart(@Nullable Player owner) {
        play(ModSounds.TIME_STOP.get(), 2.0F, 1.0F);
        play(ModSounds.CLOCK_BELL.get(), 1.4F, 1.4F);
    }

    @Override
    protected void tickBuff() {
        tickShield();
        if (this.tickCount % 20 == 0) play(ModSounds.CLOCK_TICK.get(), 1.2F, 0.8F);
        for (Entity e : enemiesWithin(STOP_RADIUS)) {
            e.setDeltaMovement(e.getDeltaMovement().multiply(0.08, 0.5, 0.08));
            e.hurtMarked = true;
            if (e instanceof BeybladeEntity b) b.stun(4);
            else if (e instanceof LivingEntity l) l.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 6, false, false));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide() && getState() == STATE_BATTLE && getAbility() != TE_REWIND) {
            history.addLast(new double[]{getX(), getY(), getZ(), getSpin()});
            while (history.size() > 70) history.removeFirst();
        }
    }

    // ---------------- abilities ----------------
    @Override
    protected boolean tickAbility(int ab, int t, @Nullable Player owner) {
        switch (ab) {
            case TE_REWIND -> {
                // Time Rewind: run back along the path of the last 3 seconds and get that spin back
                if (t == 1) {
                    rewindPath.clear();
                    List<double[]> h = new ArrayList<>(history);
                    for (int i = h.size() - 1; i >= 0; i--) rewindPath.add(h.get(i));
                    play(ModSounds.REWIND.get(), 2.0F, 1.0F);
                    this.level().broadcastEntityEvent(this, EV_REWIND);
                }
                if (rewindPath.isEmpty()) return true;
                int idx = Math.min(rewindPath.size() - 1, t * 3);
                double[] p = rewindPath.get(idx);
                Vec3 d = new Vec3(p[0], p[1], p[2]).subtract(position());
                if (this.level().noCollision(this, getBoundingBox().move(d))) setPos(p[0], p[1], p[2]);
                setDeltaMovement(0, 0, 0);
                float best = 0;
                for (int i = 0; i <= idx; i++) best = Math.max(best, (float) rewindPath.get(i)[3]);
                if (best > getSpin()) healSpin((best - getSpin()) * 0.35F);
                return true;
            }
            case TE_HANDS -> {
                // Clock Hands Sweep: a giant hour and minute hand sweep around Tempo twice
                if (t == 1) {
                    handHits.clear();
                    play(ModSounds.CLOCK_BELL.get(), 1.8F, 1.0F);
                }
                setDeltaMovement(getDeltaMovement().multiply(0.4, 1, 0.4));
                if (t % 6 == 0) play(ModSounds.CLOCK_TICK.get(), 1.4F, 1.0F + t * 0.01F);
                if (t < HANDS_START || t >= HANDS_END) return true;
                double[] angles = {minuteAngle(t), hourAngle(t)};
                double[] lengths = {HANDS_LONG, HANDS_SHORT};
                for (Entity e : enemiesWithin(HANDS_LONG + 0.5)) {
                    Vec3 rel = flat(e.position().subtract(position()));
                    double dist = rel.length();
                    if (dist < 0.4) continue;
                    double a = Math.atan2(rel.z, rel.x);
                    for (int k = 0; k < 2; k++) {
                        double diff = Math.abs(Math.atan2(Math.sin(a - angles[k]), Math.cos(a - angles[k])));
                        if (dist > lengths[k] || diff > 0.28) continue;
                        Integer last = handHits.get(e.getId());
                        if (last != null && t - last < 10) continue;
                        handHits.put(e.getId(), t);
                        Vec3 tan = new Vec3(-Math.sin(angles[k]), 0, Math.cos(angles[k]));
                        if (e instanceof BeybladeEntity b) {
                            b.damageSpin(70F * powerMul(), this);
                            b.setDeltaMovement(b.getDeltaMovement().add(tan.scale(0.8)).add(0, 0.12, 0));
                            this.level().broadcastEntityEvent(b, EV_HEAVY_HIT);
                        } else if (e instanceof LivingEntity l) {
                            l.hurt(damageSources().indirectMagic(this, owner), 7F);
                            l.knockback(0.8, -tan.x, -tan.z);
                        }
                        play(ModSounds.CLASH_HEAVY.get(), 1.4F, 1.2F);
                    }
                }
                return true;
            }
            case TE_ACCEL -> {
                // Tempo Acceleration: its own time runs fast - a barrage of dashes faster than the eye can follow
                if (t == 1) {
                    accelHits = 0;
                    play(ModSounds.TIME_STOP.get(), 1.5F, 1.8F);
                }
                if (target == null || !target.isAlive()) {
                    endAbilityEarly();
                    return true;
                }
                if (t % ACCEL_STEP == 0 && accelHits < ACCEL_HITS) {
                    Vec3 to = flat(target.position().subtract(position()));
                    double side = (accelHits % 2 == 0 ? 1 : -1) * 1.4;
                    Vec3 dir = to.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : to.normalize();
                    Vec3 from = target.position().subtract(dir.scale(1.6)).add(new Vec3(-dir.z, 0, dir.x).scale(side));
                    if (this.level().noCollision(this, getBoundingBox().move(from.subtract(position())))) setPos(from.x, getY(), from.z);
                    accelHits++;
                    if (target instanceof BeybladeEntity b) {
                        if (!b.tryDodge(this)) {
                            b.damageSpin(28F * powerMul(), this);
                            b.setDeltaMovement(b.getDeltaMovement().add(dir.scale(0.18)));
                            this.level().broadcastEntityEvent(b, EV_HIT);
                        }
                    } else if (target instanceof LivingEntity l) {
                        l.invulnerableTime = 0;
                        l.hurt(damageSources().indirectMagic(this, owner), 2.5F);
                    }
                    play(ModSounds.CLASH.get(), 1.2F, 1.3F + accelHits * 0.03F);
                    if (accelHits % 3 == 0) play(ModSounds.CLOCK_TICK.get(), 1.4F, 1.6F);
                }
                setDeltaMovement(getDeltaMovement().multiply(0.3, 1, 0.3));
                return true;
            }
            case TE_CLOCK -> {
                // Twisted Time Judgment: the great clock appears above the battle, ages every enemy, then strikes twelve
                if (t == 1) play(ModSounds.CLOCK_BELL.get(), 2.5F, 0.7F);
                setDeltaMovement(getDeltaMovement().multiply(0.6, 1, 0.6));
                if (t >= CLOCK_FORM && t < CLOCK_TOLL) {
                    if (t % 10 == 0) play(ModSounds.CLOCK_TICK.get(), 1.8F, 0.6F + (t - CLOCK_FORM) / 80F);
                    for (Entity e : enemiesWithin(CLOCK_RANGE)) {
                        Vec3 in = flat(position().subtract(e.position()));
                        double d = in.length();
                        if (d > 1.5) e.setDeltaMovement(e.getDeltaMovement().add(in.scale(0.05 / d)));
                        e.hurtMarked = true;
                        if (e instanceof BeybladeEntity b) {
                            float drain = 4F;
                            b.damageSpin(drain, this);
                            healSpin(drain * 0.4F);
                        } else if (e instanceof LivingEntity l && t % 10 == 0) {
                            l.hurt(damageSources().indirectMagic(this, owner), 1.5F);
                            l.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2));
                        }
                    }
                }
                if (t == CLOCK_TOLL) {
                    play(ModSounds.CLOCK_BELL.get(), 3.0F, 0.5F);
                    play(ModSounds.CLASH_HEAVY.get(), 2.5F, 0.5F);
                    this.level().broadcastEntityEvent(this, EV_TOLL);
                    for (Entity e : enemiesWithin(CLOCK_RANGE)) {
                        if (e instanceof BeybladeEntity b) {
                            b.damageSpin(170F * powerMul(), this);
                            b.setDeltaMovement(b.getDeltaMovement().add(0, 0.4, 0));
                            b.shockwave(this, 0.8);
                        } else if (e instanceof LivingEntity l) {
                            l.hurt(damageSources().indirectMagic(this, owner), 12F);
                        }
                    }
                }
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    /** minute hand: two full turns, clockwise seen from above */
    public static double minuteAngle(float t) {
        return -Math.PI / 2 + Math.max(0, t - HANDS_START) / (double) (HANDS_END - HANDS_START) * Math.PI * 4;
    }

    /** hour hand: half as fast, starting on the other side */
    public static double hourAngle(float t) {
        return Math.PI / 2 + Math.max(0, t - HANDS_START) / (double) (HANDS_END - HANDS_START) * Math.PI * 2;
    }

    // ---------------- client ----------------
    @Override
    public void handleEntityEvent(byte id) {
        switch (id) {
            case EV_TOLL -> { tollTime = this.tickCount; clientScar(3.5F); }
            case EV_REWIND -> rewindTime = this.tickCount;
            default -> super.handleEntityEvent(id);
        }
    }
}
