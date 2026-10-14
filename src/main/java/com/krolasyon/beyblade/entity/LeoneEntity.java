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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Rock Leone 145WB. A heavy right-spinning defense type: takes 25% less spin damage and fights with wind and earth.
 * Its special moves (wind wall, fang volley, lion pounce, rock quake, storm cage) differ from Libra's and L-Drago's.
 */
public class LeoneEntity extends BeybladeEntity {
    public static final int LE_WALL = 1, LE_FANG = 2, LE_POUNCE = 3, LE_QUAKE = 4, LE_CAGE = 5, LE_GIANT = 6;
    public static final int[] LE_DURATION = {0, 0, 54, 60, 92, 250, 282};
    public static final int[] LE_COOLDOWN = {0, 550, 450, 600, 650, 1000, 1300};
    public static final int WALL_TICKS = 180;
    public static final double WALL_RADIUS = 1.7;

    public static final int FANG_FIRST = 6, FANG_STEP = 2, FANG_COUNT = 20, FANG_FLIGHT = 6;
    public static final int P_LEAP = 12, P_DASH = 20, P_DASH_TICKS = 10;
    public static final int Q_SLAM = 14, Q_RINGS = 3, Q_RING_GAP = 10, Q_RING_LIFE = 18;
    public static final float Q_SPEED = 0.48F;
    public static final double Q_RADIUS = 9;
    public static final int C_FORM = 14, C_BLAST = 205, C_FADE = 45;
    /** the colossal single tornado (ability 6) */
    public static final int G_FORM = 16, G_END = 205, G_FADE = 45, G_TRAVEL = 32, G_LIFT = 36;
    public static final double G_PULL = 40, G_CAPTURE = 4.6, G_TOP = 24, G_TOP_R = 22, G_TORNADO_H = 70;
    public static final double C_PILLAR_DIST = 5.2, C_PULL_RANGE = 20, C_HOLD_HEIGHT = 2.4;

    public static final byte EV_QUAKE = 80, EV_CAGE = 81, EV_CAGE_BLAST = 82, EV_WALL_HIT = 83, EV_GIANT = 84, EV_GIANT_END = 85;

    private static final DustParticleOptions LEAF = new DustParticleOptions(new Vector3f(0.3F, 0.95F, 0.4F), 1.2F);
    private static final DustParticleOptions DUST = new DustParticleOptions(new Vector3f(0.55F, 0.48F, 0.38F), 1.8F);

    // server
    private Vec3 pounceDir = Vec3.ZERO;
    private int quakeStart = -1;
    @Nullable private Vec3 cageCenter;
    private boolean stormSet;
    private final Map<Integer, double[]> captured = new HashMap<>();
    private final Map<Integer, Integer> released = new HashMap<>();
    private final Map<Integer, Integer> slamming = new HashMap<>();

    private void clearStorm(ServerLevel sl) {
        if (!stormSet) return;
        stormSet = false;
        sl.setWeatherParameters(6000, 0, false, false);
    }

    @Override
    protected void endAbility() {
        if (stormSet && this.level() instanceof ServerLevel sl && ((getAbility() == LE_CAGE && abilityTicks < C_BLAST + C_FADE - 5)
                || (getAbility() == LE_GIANT && abilityTicks < G_TRAVEL + G_END + G_FADE - 5))) clearStorm(sl);
        for (Integer id : slamming.keySet()) {
            Entity e = this.level().getEntity(id);
            if (e != null) e.setNoGravity(false);
        }
        slamming.clear();
        for (Integer id : captured.keySet()) {
            Entity e = this.level().getEntity(id);
            if (e != null) e.setNoGravity(false);
        }
        captured.clear();
        super.endAbility();
    }
    @SuppressWarnings("unchecked")
    private final Set<Integer>[] ringHits = new Set[]{new HashSet<>(), new HashSet<>(), new HashSet<>()};
    private final Set<Integer> pounceHits = new HashSet<>();

    // client
    public int giantTime = -1000;
    public int quakeTime = -1000, cageTime = -1000, cageBlastTime = -1000, wallHitTime = -1000;
    public double cageX, cageY, cageZ;

    public LeoneEntity(EntityType<? extends LeoneEntity> type, Level level) {
        super(type, level);
    }

    // ---------------- hooks ----------------
    @Override protected float dodgeChance() { return 0.08F; }
    @Override public int nativeMode() { return MODE_DEFENSE; }
    @Override public String kindId() { return "leone"; }
    @Override public String abilityKey(int id) { return "ability.librabeyblade.leone." + id; }
    @Override public int duration(int id) { return LE_DURATION[id]; }
    @Override public int cooldown(int id) { return LE_COOLDOWN[id]; }
    @Override public int buffAbility() { return LE_WALL; }
    @Override public int buffTicks() { return WALL_TICKS; }
    @Override public boolean requiresTarget(int id) { return id == LE_FANG || id == LE_POUNCE; }
    @Override protected boolean ramsInto(int id) { return id == LE_FANG || id == LE_POUNCE || id == LE_QUAKE || id == LE_CAGE; }
    @Override public int abilityCount() { return 6; }
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
                    Px.send(sl, ParticleTypes.CLOUD, e.getX(), e.getY() + 0.4, e.getZ(), 4, 0.2, 0.2, 0.2, 0.05);
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
                    Px.send(sl, LEAF, target.getX(), target.getY() + 0.5, target.getZ(), 4, 0.2, 0.2, 0.2, 0);
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
                        Px.send(sl, ParticleTypes.SWEEP_ATTACK, e.getX(), e.getY() + 0.5, e.getZ(), 1, 0, 0, 0, 0);
                        Px.send(sl, ParticleTypes.CLOUD, e.getX(), e.getY() + 0.5, e.getZ(), 8, 0.3, 0.3, 0.3, 0.1);
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
                    if (!ground.isAir()) Px.send(sl, new BlockParticleOption(ParticleTypes.BLOCK, ground), getX(), getY() + 0.1, getZ(), 60, 1.5, 0.1, 1.5, 0.3);
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
                            Px.send(sl, DUST, e.getX(), e.getY() + 0.2, e.getZ(), 10, 0.4, 0.2, 0.4, 0);
                        }
                    }
                }
                return true;
            }
            case LE_CAGE -> {
                // King Lion Tearing Blast: four huge grey tornadoes circle Leone (two each way) and drag enemies in
                if (t == 1) {
                    // the tornadoes rise where Leone stands and stay there: Leone fights on inside the storm
                    cageCenter = position();
                    this.level().broadcastEntityEvent(this, EV_CAGE);
                    play(ModSounds.CAGE_WIND.get(), 2.2F, 0.9F);
                    play(ModSounds.LION_ROAR.get(), 1.6F, 1.0F);
                    // a real storm breaks out over the area while the tornadoes stand
                    if (!sl.isThundering()) {
                        stormSet = true;
                        sl.setWeatherParameters(0, LE_DURATION[LE_CAGE] + 60, true, true);
                    }
                }
                if (cageCenter == null) cageCenter = position();
                if (t >= C_FORM && t < C_BLAST && t % 35 == 0) {
                    net.minecraft.world.entity.LightningBolt bolt = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(sl);
                    if (bolt != null) {
                        Vec3 p = pillar(cageCenter.x, cageCenter.y, cageCenter.z, this.random.nextInt(4), t);
                        bolt.moveTo(p.x + this.random.nextGaussian() * 2, cageCenter.y, p.z + this.random.nextGaussian() * 2);
                        bolt.setVisualOnly(true);
                        sl.addFreshEntity(bolt);
                    }
                }
                if (t == C_BLAST + C_FADE - 5) clearStorm(sl);
                if (t >= C_FORM && t < C_BLAST) {
                    if ((t - C_FORM) % 30 == 0) play(ModSounds.CAGE_WIND.get(), 2.0F, 0.85F + this.random.nextFloat() * 0.2F);
                    for (Entity e : enemiesWithin(C_PULL_RANGE)) {
                        Vec3 best = null;
                        double bd = 1.0E9;
                        for (int i = 0; i < 4; i++) {
                            Vec3 p = pillar(cageCenter.x, cageCenter.y, cageCenter.z, i, t);
                            double d = flat(p.subtract(e.position())).lengthSqr();
                            if (d < bd) { bd = d; best = p; }
                        }
                        Vec3 to = flat(best.subtract(e.position()));
                        double d = Math.sqrt(bd);
                        if (d < 1.0E-3) continue;
                        Vec3 dir = to.scale(1 / d);
                        Vec3 swirl = new Vec3(-dir.z, 0, dir.x);
                        double k = e == target ? 1.4 : 1.0;
                        double pull = d < 1.8 ? 0.03 : Math.min(0.22, 0.9 / (d + 1.5)) * k;
                        Vec3 v = e.getDeltaMovement().add(dir.scale(pull)).add(swirl.scale(d < 3.2 ? 0.2 : 0.06));
                        if (d < 3.0) {
                            v = new Vec3(v.x, Math.min(0.32, v.y + 0.1), v.z);
                            e.fallDistance = 0;
                            if (e instanceof BeybladeEntity b) b.damageSpin(e == target ? 5F : 3F, this);
                            else if (e instanceof LivingEntity l && t % 6 == 0) {
                                l.invulnerableTime = 0;
                                l.hurt(damageSources().indirectMagic(this, owner), 1.5F);
                            }
                        }
                        e.setDeltaMovement(v);
                        e.hurtMarked = true;
                    }
                }
                if (t == C_BLAST) {
                    play(ModSounds.CAGE_BLAST.get(), 2.8F, 0.9F);
                    this.level().broadcastEntityEvent(this, EV_CAGE_BLAST);
                    for (Entity e : enemiesWithin(9)) {
                        Vec3 out = flat(e.position().subtract(position()));
                        out = out.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : out.normalize();
                        if (e instanceof BeybladeEntity b) {
                            b.damageSpin(e == target ? 120F : 60F, this);
                            b.setDeltaMovement(out.scale(1.0).add(0, 0.3, 0));
                        } else if (e instanceof LivingEntity l) {
                            l.hurt(damageSources().indirectMagic(this, owner), e == target ? 10F : 5F);
                            l.knockback(1.5, -out.x, -out.z);
                        }
                    }
                }
                // Leone keeps fighting while the storm rages around it
                return t < C_FORM;
            }
            case LE_GIANT -> {
                // King Lion Colossal Tornado: Leone runs to the middle of the stadium and becomes the eye of one colossal
                // tornado. Enemies are sucked in; whoever touches the funnel is whirled twice around Leone and hurled into the wall.
                if (t <= G_TRAVEL) {
                    if (t == 1) {
                        play(ModSounds.LION_ROAR.get(), 2.0F, 0.9F);
                        Vec3 c = position();
                        for (StadiumEntity st : this.level().getEntitiesOfClass(StadiumEntity.class, getBoundingBox().inflate(20, 6, 20))) {
                            double dx = getX() - st.getX(), dz = getZ() - st.getZ();
                            if (dx * dx + dz * dz < StadiumEntity.WALL_IN * StadiumEntity.WALL_IN) c = new Vec3(st.getX(), getY(), st.getZ());
                            break;
                        }
                        cageCenter = c;
                    }
                    Vec3 d = flat(cageCenter.subtract(position()));
                    if (d.length() > 0.5 && t < G_TRAVEL) {
                        Vec3 v = d.normalize().scale(Math.min(0.75, d.length() * 0.5));
                        setDeltaMovement(v.x, getDeltaMovement().y, v.z);
                        return true;
                    }
                    // in position: the storm begins (the timeline continues from G_TRAVEL)
                    abilityTicks = G_TRAVEL + 1;
                    cageCenter = new Vec3(getX(), getY(), getZ());
                    captured.clear();
                    released.clear();
                    this.level().broadcastEntityEvent(this, EV_GIANT);
                    play(ModSounds.CAGE_WIND.get(), 3.0F, 0.6F);
                    play(ModSounds.LION_ROAR.get(), 2.4F, 0.8F);
                    if (!sl.isThundering()) {
                        stormSet = true;
                        sl.setWeatherParameters(0, LE_DURATION[LE_GIANT] + 60, true, true);
                    }
                    return true;
                }
                if (cageCenter == null) cageCenter = position();
                int a = t - G_TRAVEL;
                // Leone stays in the eye of the storm
                Vec3 hold = flat(cageCenter.subtract(position()));
                setDeltaMovement(hold.x * 0.4, getDeltaMovement().y, hold.z * 0.4);
                if (a >= G_FORM && a < G_END && a % 30 == 0) {
                    net.minecraft.world.entity.LightningBolt bolt = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(sl);
                    if (bolt != null) {
                        double ang = this.random.nextDouble() * Math.PI * 2, r = 8 + this.random.nextDouble() * 10;
                        bolt.moveTo(cageCenter.x + Math.cos(ang) * r, cageCenter.y, cageCenter.z + Math.sin(ang) * r);
                        bolt.setVisualOnly(true);
                        sl.addFreshEntity(bolt);
                    }
                }
                if (a == G_END + G_FADE - 5) clearStorm(sl);
                if (a >= G_FORM && a < G_END) {
                    if ((a - G_FORM) % 25 == 0) play(ModSounds.CAGE_WIND.get(), 3.0F, 0.6F + this.random.nextFloat() * 0.15F);
                    for (Entity e : enemiesWithin(G_PULL)) {
                        if (captured.containsKey(e.getId())) continue;
                        Vec3 to = flat(cageCenter.subtract(e.position()));
                        double d = to.length();
                        if (d < 1.0E-3) continue;
                        Integer rel = released.get(e.getId());
                        boolean cooling = rel != null && t - rel < 45;
                        if (!cooling && d < G_CAPTURE) {
                            // touched the funnel: caught by the wind
                            captured.put(e.getId(), new double[]{t, Math.atan2(e.getZ() - cageCenter.z, e.getX() - cageCenter.x)});
                            e.setNoGravity(true);
                            play(ModSounds.CAGE_WIND.get(), 1.6F, 1.4F);
                            continue;
                        }
                        Vec3 dir = to.scale(1 / d);
                        Vec3 swirl = new Vec3(-dir.z, 0, dir.x);
                        double pull = cooling ? 0.02 : Math.min(0.26, 2.4 / (d + 3)) * (e == target ? 1.3 : 1.0);
                        e.setDeltaMovement(e.getDeltaMovement().add(dir.scale(pull)).add(swirl.scale(0.07)));
                        e.hurtMarked = true;
                    }
                }
                // the caught ones: spun up the funnel to the very top, then smashed straight down at full speed
                for (java.util.Iterator<Map.Entry<Integer, double[]>> it = captured.entrySet().iterator(); it.hasNext(); ) {
                    Map.Entry<Integer, double[]> en = it.next();
                    Entity e = this.level().getEntity(en.getKey());
                    if (e == null || !e.isAlive()) { it.remove(); continue; }
                    double prog = (t - en.getValue()[0]) / G_LIFT;
                    if (prog < 1 && a < G_END) {
                        double ang = en.getValue()[1] + prog * Math.PI * 3 * spinDir();
                        double r = (G_CAPTURE - 0.6) * (1 - 0.45 * prog);
                        double y = cageCenter.y + 0.3 + G_TOP * prog * (2 - prog);
                        e.setPos(cageCenter.x + Math.cos(ang) * r, y, cageCenter.z + Math.sin(ang) * r);
                        e.setDeltaMovement(new Vec3(-Math.sin(ang), 0.2, Math.cos(ang)).scale(0.3 * spinDir()));
                        e.hurtMarked = true;
                        e.fallDistance = 0;
                        if (e instanceof BeybladeEntity b) b.stun(5);
                    } else {
                        e.setNoGravity(false);
                        e.setDeltaMovement(0, -3.2, 0);
                        e.hurtMarked = true;
                        play(ModSounds.CAGE_BLAST.get(), 1.6F, 1.5F);
                        slamming.put(en.getKey(), t);
                        it.remove();
                    }
                }
                for (java.util.Iterator<Map.Entry<Integer, Integer>> it = slamming.entrySet().iterator(); it.hasNext(); ) {
                    Map.Entry<Integer, Integer> en = it.next();
                    Entity e = this.level().getEntity(en.getKey());
                    if (e == null || !e.isAlive()) { it.remove(); continue; }
                    if (e.getDeltaMovement().y > -1.0 && t - en.getValue() < 30 && !e.onGround()) e.setDeltaMovement(e.getDeltaMovement().x, -3.2, e.getDeltaMovement().z);
                    boolean down = e.onGround() || e.getY() <= cageCenter.y + 0.8 || t - en.getValue() > 30;
                    if (!down) continue;
                    if (e instanceof BeybladeEntity b) {
                        b.damageSpin(220F * powerMul(), this);
                        b.setDeltaMovement(0, 0.35, 0);
                        b.shockwave(this, 1.0);
                        this.level().broadcastEntityEvent(b, EV_STRIKE);
                    } else if (e instanceof LivingEntity l) {
                        l.hurt(damageSources().indirectMagic(this, owner), 14F);
                    }
                    e.fallDistance = 0;
                    play(ModSounds.QUAKE.get(), 2.6F, 0.8F);
                    play(ModSounds.CLASH_HEAVY.get(), 2.6F, 0.5F);
                    released.put(en.getKey(), t);
                    it.remove();
                }
                if (a == G_END) {
                    play(ModSounds.CAGE_BLAST.get(), 2.8F, 0.8F);
                    this.level().broadcastEntityEvent(this, EV_GIANT_END);
                }
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    /** the 4 tornado pillars of the storm cage, around its centre */
    public static Vec3 pillar(double cx, double cy, double cz, int i, float t) {
        // even tornadoes circle one way, odd ones the other; each sways in and out on its own rhythm
        double dir = i % 2 == 0 ? 1 : -1;
        double a = Math.PI / 4 + i * Math.PI / 2 + dir * t * 0.065;
        double r = C_PILLAR_DIST + Math.sin(t * 0.083 + i * 2.1) * 1.3 + Math.sin(t * 0.21 + i) * 0.4;
        return new Vec3(cx + Math.cos(a) * r, cy, cz + Math.sin(a) * r);
    }

    // ---------------- client ----------------
    @Override
    public void handleEntityEvent(byte id) {
        switch (id) {
            case EV_QUAKE -> { quakeTime = this.tickCount; clientScar(4.5F); }
            case EV_CAGE -> {
                cageTime = this.tickCount;
                cageX = getX();
                cageY = getY();
                cageZ = getZ();
            }
            case EV_CAGE_BLAST -> {
                cageBlastTime = this.tickCount;
                clientScar(4F);
                cageX = getX();
                cageY = getY();
                cageZ = getZ();
            }
            case EV_GIANT -> {
                giantTime = this.tickCount;
                cageX = getX();
                cageY = getY();
                cageZ = getZ();
            }
            case EV_GIANT_END -> {
                cageBlastTime = this.tickCount;
                cageX = getX();
                cageY = getY();
                cageZ = getZ();
                clientScar(4F);
            }
            case EV_WALL_HIT -> wallHitTime = this.tickCount;
            default -> super.handleEntityEvent(id);
        }
    }

    @Override
    protected void clientBuffParticles(Level lv) {
        for (int i = 0; i < 2; i++) {
            double a = this.random.nextDouble() * Math.PI * 2, h = this.random.nextDouble() * 2.0;
            double r = WALL_RADIUS;
            Px.add(lv, i == 0 ? ParticleTypes.CLOUD : LEAF, getX() + Math.cos(a) * r, getY() + h, getZ() + Math.sin(a) * r,
                    -Math.sin(a) * 0.35, 0.04, Math.cos(a) * 0.35);
        }
    }

    @Override
    protected void clientAbilityParticles(int ab, float at, Level lv) {
        switch (ab) {
            case LE_POUNCE -> {
                if (at < P_LEAP) {
                    double a = this.random.nextDouble() * Math.PI * 2;
                    Px.add(lv, LEAF, getX() + Math.cos(a) * 1.5, getY() + 0.2, getZ() + Math.sin(a) * 1.5, -Math.cos(a) * 0.15, 0.05, -Math.sin(a) * 0.15);
                } else if (at >= P_DASH && at < P_DASH + P_DASH_TICKS) {
                    for (int i = 0; i < 3; i++)
                        Px.add(lv, ParticleTypes.CLOUD, getX() + this.random.nextGaussian() * 0.3, getY() + 0.4, getZ() + this.random.nextGaussian() * 0.3, 0, 0.02, 0);
                }
            }
            case LE_QUAKE -> {
                if (at < Q_SLAM) Px.add(lv, DUST, getX() + this.random.nextGaussian() * 0.5, getY() - 0.3, getZ() + this.random.nextGaussian() * 0.5, 0, -0.05, 0);
            }
            case LE_CAGE -> {
                if (at >= C_FORM && at < C_BLAST) {
                    for (int i = 0; i < 4; i++) {
                        Vec3 p = pillar(cageX, cageY, cageZ, i, at);
                        double a = this.random.nextDouble() * Math.PI * 2, h = this.random.nextDouble() * 6;
                        Px.add(lv, i % 2 == 0 ? ParticleTypes.CLOUD : LEAF, p.x + Math.cos(a) * 0.8, p.y + h, p.z + Math.sin(a) * 0.8,
                                -Math.sin(a) * 0.3, 0.1, Math.cos(a) * 0.3);
                    }
                }
            }
            case LE_FANG -> {
                if (this.random.nextInt(2) == 0) Px.add(lv, LEAF, getX(), getY() + 0.5, getZ(), this.random.nextGaussian() * 0.1, 0.05, this.random.nextGaussian() * 0.1);
            }
            default -> {}
        }
    }

    public static float clampAge(float x) { return Mth.clamp(x, 0F, 1F); }
}
