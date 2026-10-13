package com.krolasyon.beyblade.entity;

import com.krolasyon.beyblade.registry.ModItems;
import com.krolasyon.beyblade.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

/**
 * Rock Leone 145WB. A heavy right-spinning defense type: takes 25% less spin damage and fights with wind and earth.
 * Its special moves (wind wall, fang volley, lion pounce, rock quake, storm cage) differ from Libra's and L-Drago's.
 */
public class LeoneEntity extends BeybladeEntity {
    public static final int LE_WALL = 1, LE_FANG = 2, LE_POUNCE = 3, LE_QUAKE = 4, LE_CAGE = 5;
    public static final int[] LE_DURATION = {0, 0, 54, 60, 92, 112};
    public static final int[] LE_COOLDOWN = {0, 550, 450, 600, 650, 1000};
    public static final int WALL_TICKS = 180;
    public static final double WALL_RADIUS = 1.7;

    public static final int FANG_FIRST = 6, FANG_STEP = 2, FANG_COUNT = 20, FANG_FLIGHT = 6;
    public static final int P_LEAP = 12, P_DASH = 20, P_DASH_TICKS = 10;
    public static final int Q_SLAM = 14, Q_RINGS = 3, Q_RING_GAP = 10, Q_RING_LIFE = 18;
    public static final float Q_SPEED = 0.48F;
    public static final double Q_RADIUS = 9;
    public static final int C_FORM = 10, C_BLAST = 92;
    public static final double C_PILLAR_DIST = 3.6, C_HOLD_HEIGHT = 2.4;

    public static final byte EV_QUAKE = 80, EV_CAGE = 81, EV_CAGE_BLAST = 82, EV_WALL_HIT = 83;

    private static final DustParticleOptions LEAF = new DustParticleOptions(new Vector3f(0.3F, 0.95F, 0.4F), 1.2F);
    private static final DustParticleOptions DUST = new DustParticleOptions(new Vector3f(0.55F, 0.48F, 0.38F), 1.8F);

    // server
    private Vec3 pounceDir = Vec3.ZERO;
    private int quakeStart = -1;
    @Nullable private Vec3 cageCenter;
    @SuppressWarnings("unchecked")
    private final Set<Integer>[] ringHits = new Set[]{new HashSet<>(), new HashSet<>(), new HashSet<>()};
    private final Set<Integer> pounceHits = new HashSet<>();

    // client
    public int quakeTime = -1000, cageTime = -1000, cageBlastTime = -1000, wallHitTime = -1000;
    public double cageX, cageY, cageZ;

    public LeoneEntity(EntityType<? extends LeoneEntity> type, Level level) {
        super(type, level);
    }

    // ---------------- hooks ----------------
    @Override public String kindId() { return "leone"; }
    @Override public String abilityKey(int id) { return "ability.librabeyblade.leone." + id; }
    @Override public int duration(int id) { return LE_DURATION[id]; }
    @Override public int cooldown(int id) { return LE_COOLDOWN[id]; }
    @Override public int buffAbility() { return LE_WALL; }
    @Override public int buffTicks() { return WALL_TICKS; }
    @Override public boolean requiresTarget(int id) { return id == LE_FANG || id == LE_POUNCE || id == LE_CAGE; }
    @Override public boolean buffReflects() { return true; }
    @Override public float incomingMul() { return isShielded() ? 0.5F : 0.75F; }
    @Override protected ItemStack defaultItem() { return new ItemStack(ModItems.LEONE.get()); }

    @Override
    protected float clientSpinSpeed(int ab, float at, float speed) {
        if (ab == LE_POUNCE && at >= P_LEAP) return 100F;
        if (ab == LE_CAGE) return 95F;
        return speed;
    }

    /** Lion Gale Force Wall: a spinning wall of wind that throws back everything that comes close */
    @Override
    protected void onBuffStart(@Nullable Player owner) {
        play(ModSounds.WIND_WALL.get(), 1.8F, 1.0F);
        play(ModSounds.LION_ROAR.get(), 1.0F, 1.3F);
    }

    @Override
    protected void tickBuff() {
        tickShield();
        ServerLevel sl = (ServerLevel) this.level();
        for (Entity e : enemiesWithin(WALL_RADIUS + 0.9)) {
            Vec3 out = flat(e.position().subtract(position()));
            double d = out.length();
            out = d < 1.0E-3 ? new Vec3(1, 0, 0) : out.scale(1 / d);
            Vec3 v = e.getDeltaMovement();
            if (v.dot(out) < 0.4) {
                e.setDeltaMovement(v.add(out.scale(0.45)).add(0, 0.12, 0));
                e.hurtMarked = true;
                if (e instanceof BeybladeEntity b) b.damageSpin(8F, this);
                else if (e instanceof LivingEntity l && l.hurtTime == 0) l.hurt(damageSources().indirectMagic(this, getOwner()), 2F);
                if (this.tickCount % 3 == 0) {
                    this.level().broadcastEntityEvent(this, EV_WALL_HIT);
                    sl.sendParticles(ParticleTypes.CLOUD, e.getX(), e.getY() + 0.4, e.getZ(), 4, 0.2, 0.2, 0.2, 0.05);
                }
            }
        }
    }

    @Override
    protected void shieldHit() {
        play(ModSounds.FANG_SHOT.get(), 1.0F, 0.7F);
        this.level().broadcastEntityEvent(this, EV_WALL_HIT);
    }

    // ---------------- abilities ----------------
    @Override
    protected boolean tickAbility(int ab, int t, @Nullable Player owner) {
        ServerLevel sl = (ServerLevel) this.level();
        switch (ab) {
            case LE_FANG -> {
                if (target == null || !target.isAlive()) {
                    endAbilityEarly();
                    return true;
                }
                if (t == 1) play(ModSounds.LION_ROAR.get(), 1.2F, 1.4F);
                hover(0.5, 0.25);
                int rel = t - FANG_FIRST;
                if (rel >= 0 && rel % FANG_STEP == 0 && rel / FANG_STEP < FANG_COUNT) play(ModSounds.FANG_SHOT.get(), 0.8F, 0.9F + this.random.nextFloat() * 0.4F);
                // fang k lands FANG_FLIGHT ticks after it was fired
                int landed = t - FANG_FIRST - FANG_FLIGHT;
                if (landed >= 0 && landed % FANG_STEP == 0 && landed / FANG_STEP < FANG_COUNT) {
                    Vec3 push = flat(target.position().subtract(position()));
                    push = push.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : push.normalize();
                    if (target instanceof BeybladeEntity b) {
                        b.damageSpin(10F, this);
                        b.setDeltaMovement(b.getDeltaMovement().add(push.scale(0.08)));
                    } else if (target instanceof LivingEntity l) {
                        l.invulnerableTime = 0;
                        l.hurt(damageSources().indirectMagic(this, owner), 1.5F);
                        l.knockback(0.15, -push.x, -push.z);
                    }
                    sl.sendParticles(LEAF, target.getX(), target.getY() + 0.5, target.getZ(), 4, 0.2, 0.2, 0.2, 0);
                }
                return true;
            }
            case LE_POUNCE -> {
                if (t == 1) {
                    play(ModSounds.LION_ROAR.get(), 2.0F, 0.9F);
                    pounceHits.clear();
                }
                if (t < P_LEAP) {
                    setDeltaMovement(getDeltaMovement().multiply(0.5, 1, 0.5));
                    return true;
                }
                if (t < P_DASH) {
                    hover(3.0, 0.3);
                    if (target != null) {
                        Vec3 d = target.position().add(0, 0.3, 0).subtract(position());
                        pounceDir = d.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : d.normalize();
                    }
                    return true;
                }
                if (t == P_DASH) play(ModSounds.WIND_WALL.get(), 1.6F, 1.6F);
                if (t < P_DASH + P_DASH_TICKS) {
                    setNoGravity(true);
                    setDeltaMovement(pounceDir.scale(1.5));
                    for (Entity e : enemiesWithin(2.0)) {
                        if (!pounceHits.add(e.getId())) continue;
                        Vec3 side = flat(e.position().subtract(position()));
                        side = side.lengthSqr() < 1.0E-4 ? new Vec3(pounceDir.z, 0, -pounceDir.x) : side.normalize();
                        if (e instanceof BeybladeEntity b) {
                            b.damageSpin(130F, this);
                            b.setDeltaMovement(b.getDeltaMovement().add(side.scale(0.7)).add(0, 0.6, 0));
                        } else if (e instanceof LivingEntity l) {
                            l.hurt(damageSources().indirectMagic(this, owner), 9F);
                            l.knockback(1.2, -side.x, -side.z);
                            l.setDeltaMovement(l.getDeltaMovement().add(0, 0.5, 0));
                        }
                        sl.sendParticles(ParticleTypes.SWEEP_ATTACK, e.getX(), e.getY() + 0.5, e.getZ(), 1, 0, 0, 0, 0);
                        sl.sendParticles(ParticleTypes.CLOUD, e.getX(), e.getY() + 0.5, e.getZ(), 8, 0.3, 0.3, 0.3, 0.1);
                        play(ModSounds.CLASH_HEAVY.get(), 1.2F, 0.8F);
                    }
                    return true;
                }
                setNoGravity(false);
                setDeltaMovement(getDeltaMovement().multiply(0.6, 1, 0.6));
                return true;
            }
            case LE_QUAKE -> {
                if (t == 1) {
                    quakeStart = -1;
                    for (Set<Integer> s : ringHits) s.clear();
                }
                if (t < Q_SLAM) {
                    hover(1.6, 0.3);
                    return true;
                }
                if (t == Q_SLAM) {
                    setNoGravity(false);
                    setDeltaMovement(0, -1.6, 0);
                }
                if (quakeStart < 0 && (onGround() || t > Q_SLAM + 12)) {
                    // leave exactly enough time for the three shock rings
                    int newT = LE_DURATION[LE_QUAKE] - ((Q_RINGS - 1) * Q_RING_GAP + Q_RING_LIFE + 2);
                    if (t < newT) {
                        abilityTicks = newT;
                        t = newT;
                    }
                    quakeStart = t;
                    play(ModSounds.QUAKE.get(), 2.4F, 1.0F);
                    this.level().broadcastEntityEvent(this, EV_QUAKE);
                    BlockState ground = this.level().getBlockState(BlockPos.containing(getX(), getY() - 0.2, getZ()));
                    if (!ground.isAir()) sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), getX(), getY() + 0.1, getZ(), 60, 1.5, 0.1, 1.5, 0.3);
                }
                if (quakeStart >= 0) {
                    setDeltaMovement(getDeltaMovement().multiply(0.3, 1, 0.3));
                    for (int i = 0; i < Q_RINGS; i++) {
                        int age = t - quakeStart - i * Q_RING_GAP;
                        if (age < 0 || age >= Q_RING_LIFE) continue;
                        if (age == 0 && i > 0) play(ModSounds.QUAKE.get(), 1.4F, 1.2F + i * 0.15F);
                        double front = 0.8 + age * Q_SPEED * (1 + i * 0.25);
                        for (Entity e : enemiesWithin(front + 0.6)) {
                            double d = Math.sqrt(flat(e.position().subtract(position())).lengthSqr());
                            if (d < front - 1.2 || !ringHits[i].add(e.getId())) continue;
                            if (e instanceof BeybladeEntity b) {
                                b.damageSpin(55F, this);
                                b.stun(30);
                                b.setDeltaMovement(b.getDeltaMovement().multiply(0.2, 1, 0.2).add(0, 0.45, 0));
                            } else if (e instanceof LivingEntity l) {
                                l.hurt(damageSources().indirectMagic(this, owner), 5F);
                                l.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 5));
                                l.setDeltaMovement(l.getDeltaMovement().add(0, 0.55, 0));
                                l.hurtMarked = true;
                            }
                            sl.sendParticles(DUST, e.getX(), e.getY() + 0.2, e.getZ(), 10, 0.4, 0.2, 0.4, 0);
                        }
                    }
                }
                return true;
            }
            case LE_CAGE -> {
                if (t == 1) {
                    if (target == null) {
                        endAbilityEarly();
                        return true;
                    }
                    cageCenter = target.position();
                    this.level().broadcastEntityEvent(this, EV_CAGE);
                    play(ModSounds.CAGE_WIND.get(), 2.0F, 1.0F);
                    play(ModSounds.LION_ROAR.get(), 1.4F, 1.1F);
                }
                if (cageCenter == null) return true;
                setDeltaMovement(getDeltaMovement().multiply(0.5, 1, 0.5));
                if (t >= C_FORM && t < C_BLAST) {
                    if ((t - C_FORM) % 40 == 0) play(ModSounds.CAGE_WIND.get(), 1.6F, 1.1F);
                    if (target != null && target.isAlive()) {
                        Vec3 hold = cageCenter.add(Math.cos(t * 0.35) * 0.6, C_HOLD_HEIGHT + Math.sin(t * 0.2) * 0.3, Math.sin(t * 0.35) * 0.6);
                        Vec3 d = hold.subtract(target.position());
                        target.setDeltaMovement(d.scale(0.35));
                        target.hurtMarked = true;
                        target.fallDistance = 0;
                        if (target instanceof BeybladeEntity b) b.damageSpin(4F, this);
                        else if (target instanceof LivingEntity l && t % 5 == 0) {
                            l.invulnerableTime = 0;
                            l.hurt(damageSources().indirectMagic(this, owner), 1.5F);
                        }
                    }
                    // others are kept out of the cage
                    for (Entity e : enemiesWithin(16)) {
                        if (e == target) continue;
                        Vec3 rel = flat(e.position().subtract(cageCenter));
                        double d = rel.length();
                        if (d < C_PILLAR_DIST + 0.8 && d > 1.0E-3) {
                            e.setDeltaMovement(e.getDeltaMovement().add(rel.scale(0.25 / d)));
                            e.hurtMarked = true;
                        }
                    }
                }
                if (t == C_BLAST) {
                    play(ModSounds.CAGE_BLAST.get(), 2.6F, 1.0F);
                    this.level().broadcastEntityEvent(this, EV_CAGE_BLAST);
                    sl.sendParticles(ParticleTypes.CLOUD, cageCenter.x, cageCenter.y + 2, cageCenter.z, 50, 1.5, 1.5, 1.5, 0.3);
                    for (Entity e : this.level().getEntities(this, new net.minecraft.world.phys.AABB(cageCenter, cageCenter).inflate(5))) {
                        boolean enemy = e instanceof BeybladeEntity b ? isEnemy(b) : (e instanceof LivingEntity l && isHostileLiving(l));
                        if (!enemy && e != target) continue;
                        Vec3 out = flat(e.position().subtract(cageCenter));
                        out = out.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : out.normalize();
                        if (e instanceof BeybladeEntity b) {
                            b.damageSpin(e == target ? 110F : 50F, this);
                            b.setDeltaMovement(out.scale(1.0).add(0, 0.3, 0));
                        } else if (e instanceof LivingEntity l) {
                            l.hurt(damageSources().indirectMagic(this, owner), e == target ? 10F : 5F);
                            l.knockback(1.5, -out.x, -out.z);
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

    /** the 4 tornado pillars of the storm cage, around its centre */
    public static Vec3 pillar(double cx, double cy, double cz, int i) {
        double a = Math.PI / 4 + i * Math.PI / 2;
        return new Vec3(cx + Math.cos(a) * C_PILLAR_DIST, cy, cz + Math.sin(a) * C_PILLAR_DIST);
    }

    // ---------------- client ----------------
    @Override
    public void handleEntityEvent(byte id) {
        switch (id) {
            case EV_QUAKE -> quakeTime = this.tickCount;
            case EV_CAGE -> {
                cageTime = this.tickCount;
                Entity t = this.level().getEntity(getTargetId());
                Entity c = t != null ? t : this;
                cageX = c.getX();
                cageY = c.getY();
                cageZ = c.getZ();
            }
            case EV_CAGE_BLAST -> cageBlastTime = this.tickCount;
            case EV_WALL_HIT -> wallHitTime = this.tickCount;
            default -> super.handleEntityEvent(id);
        }
    }

    @Override
    protected void clientBuffParticles(Level lv) {
        for (int i = 0; i < 2; i++) {
            double a = this.random.nextDouble() * Math.PI * 2, h = this.random.nextDouble() * 2.0;
            double r = WALL_RADIUS;
            lv.addParticle(i == 0 ? ParticleTypes.CLOUD : LEAF, getX() + Math.cos(a) * r, getY() + h, getZ() + Math.sin(a) * r,
                    -Math.sin(a) * 0.35, 0.04, Math.cos(a) * 0.35);
        }
    }

    @Override
    protected void clientAbilityParticles(int ab, float at, Level lv) {
        switch (ab) {
            case LE_POUNCE -> {
                if (at < P_LEAP) {
                    double a = this.random.nextDouble() * Math.PI * 2;
                    lv.addParticle(LEAF, getX() + Math.cos(a) * 1.5, getY() + 0.2, getZ() + Math.sin(a) * 1.5, -Math.cos(a) * 0.15, 0.05, -Math.sin(a) * 0.15);
                } else if (at >= P_DASH && at < P_DASH + P_DASH_TICKS) {
                    for (int i = 0; i < 3; i++)
                        lv.addParticle(ParticleTypes.CLOUD, getX() + this.random.nextGaussian() * 0.3, getY() + 0.4, getZ() + this.random.nextGaussian() * 0.3, 0, 0.02, 0);
                }
            }
            case LE_QUAKE -> {
                if (at < Q_SLAM) lv.addParticle(DUST, getX() + this.random.nextGaussian() * 0.5, getY() - 0.3, getZ() + this.random.nextGaussian() * 0.5, 0, -0.05, 0);
            }
            case LE_CAGE -> {
                if (at >= C_FORM && at < C_BLAST) {
                    for (int i = 0; i < 4; i++) {
                        Vec3 p = pillar(cageX, cageY, cageZ, i);
                        double a = this.random.nextDouble() * Math.PI * 2, h = this.random.nextDouble() * 6;
                        lv.addParticle(i % 2 == 0 ? ParticleTypes.CLOUD : LEAF, p.x + Math.cos(a) * 0.8, p.y + h, p.z + Math.sin(a) * 0.8,
                                -Math.sin(a) * 0.3, 0.1, Math.cos(a) * 0.3);
                    }
                }
            }
            case LE_FANG -> {
                if (this.random.nextInt(2) == 0) lv.addParticle(LEAF, getX(), getY() + 0.5, getZ(), this.random.nextGaussian() * 0.1, 0.05, this.random.nextGaussian() * 0.1);
            }
            default -> {}
        }
    }

    public static float clampAge(float x) { return Mth.clamp(x, 0F, 1F); }
}
