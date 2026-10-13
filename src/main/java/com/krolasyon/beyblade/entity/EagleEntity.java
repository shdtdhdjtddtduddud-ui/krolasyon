package com.krolasyon.beyblade.entity;

import com.krolasyon.beyblade.registry.ModItems;
import com.krolasyon.beyblade.registry.ModSounds;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Earth Eagle 145WD. A light, fast balance type of the skies: Eagle Eye lets it dodge hits outright, and its special
 * moves are built around feathers, talons, metal wings and a rain of light lances from a giant spectral eagle.
 */
public class EagleEntity extends BeybladeEntity {
    public static final int EA_EYE = 1, EA_FEATHER = 2, EA_TALON = 3, EA_WING = 4, EA_SKY = 5;
    public static final int[] EA_DURATION = {0, 0, 60, 84, 70, 124};
    public static final int[] EA_COOLDOWN = {0, 600, 450, 650, 550, 1100};
    public static final int EYE_TICKS = 200;

    public static final int F_GATHER = 20, F_GAP = 12, F_BURSTS = 3, F_COUNT = 16, F_LIFE = 11;
    public static final float F_SPEED = 0.9F;
    public static final int T_SWOOP = 10, T_SWOOP_END = 26, T_CARRY = 32;
    public static final double T_LIFT = 7.0;
    public static final int W_OPEN = 8, W_CLOSE = 62;
    public static final double W_RADIUS = 3.3;
    public static final int S_DESCEND = 20, S_LANCES = 27, S_STEP = 3, S_FALL = 8, S_SCREECH = 104;
    public static final double S_RANGE = 14;

    public static final byte EV_DODGE = 90, EV_GRAB = 91, EV_SLAM = 92, EV_SCREECH = 93;

    private static final DustParticleOptions VIOLET = new DustParticleOptions(new Vector3f(0.6F, 0.3F, 1.0F), 1.3F);
    private static final DustParticleOptions SILVER = new DustParticleOptions(new Vector3f(0.9F, 0.9F, 1.0F), 1.0F);

    // server
    @SuppressWarnings("unchecked")
    private final Set<Integer>[] burstHits = new Set[]{new HashSet<>(), new HashSet<>(), new HashSet<>()};
    private final Map<Integer, Integer> wingHit = new HashMap<>();
    private int grabTick = -1;

    // client
    public int dodgeTime = -1000, grabTime = -1000, slamTime = -1000, screechTime = -1000;
    public double dodgeX, dodgeY, dodgeZ;

    public EagleEntity(EntityType<? extends EagleEntity> type, Level level) {
        super(type, level);
    }

    // ---------------- hooks ----------------
    @Override public String kindId() { return "eagle"; }
    @Override public String abilityKey(int id) { return "ability.librabeyblade.eagle." + id; }
    @Override public int duration(int id) { return EA_DURATION[id]; }
    @Override public int cooldown(int id) { return EA_COOLDOWN[id]; }
    @Override public int buffAbility() { return EA_EYE; }
    @Override public int buffTicks() { return EYE_TICKS; }
    @Override public boolean requiresTarget(int id) { return id == EA_TALON; }
    @Override public boolean buffReflects() { return false; }
    @Override public double buffSpeedMul() { return isShielded() ? 1.5 : 1.12; }
    @Override public float incomingMul() { return 0.9F; }
    @Override protected ItemStack defaultItem() { return new ItemStack(ModItems.EAGLE.get()); }

    @Override
    protected float clientSpinSpeed(int ab, float at, float speed) {
        if (ab == EA_WING) return 105F;
        if (ab == EA_FEATHER) return 90F;
        return speed;
    }

    /** Eagle Eye: 10 s of +50% speed and a 50% chance to blink out of every collision */
    @Override
    protected void onBuffStart(@Nullable Player owner) {
        play(ModSounds.EAGLE_SCREECH.get(), 1.6F, 1.1F);
        ((ServerLevel) this.level()).sendParticles(VIOLET, getX(), getY() + 0.6, getZ(), 30, 0.6, 0.4, 0.6, 0);
    }

    @Override
    public boolean tryDodge(BeybladeEntity attacker) {
        if (!isShielded() || this.random.nextFloat() >= 0.5F) return false;
        Vec3 away = flat(position().subtract(attacker.position()));
        away = away.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : away.normalize();
        Vec3 side = new Vec3(-away.z, 0, away.x).scale(this.random.nextBoolean() ? 1.3 : -1.3).add(away.scale(0.5));
        this.level().broadcastEntityEvent(this, EV_DODGE);
        if (this.level().noCollision(this, getBoundingBox().move(side))) setPos(getX() + side.x, getY(), getZ() + side.z);
        setDeltaMovement(getDeltaMovement().add(side.scale(0.15)));
        play(ModSounds.FEATHER_BURST.get(), 0.7F, 1.5F);
        return true;
    }

    // ---------------- abilities ----------------
    @Override
    protected boolean tickAbility(int ab, int t, @Nullable Player owner) {
        ServerLevel sl = (ServerLevel) this.level();
        switch (ab) {
            case EA_FEATHER -> {
                if (t == 1) {
                    play(ModSounds.FEATHER_WHIRL.get(), 1.6F, 1.0F);
                    for (Set<Integer> s : burstHits) s.clear();
                }
                hover(0.6, 0.25);
                for (int i = 0; i < F_BURSTS; i++) {
                    int age = t - (F_GATHER + i * F_GAP);
                    if (age == 0) play(ModSounds.FEATHER_BURST.get(), 1.6F, 0.9F + i * 0.15F);
                    if (age < 0 || age >= F_LIFE) continue;
                    double front = 0.8 + age * F_SPEED;
                    for (Entity e : enemiesWithin(front + 0.6)) {
                        double d = Math.sqrt(flat(e.position().subtract(position())).lengthSqr());
                        if (d < front - 1.2 || !burstHits[i].add(e.getId())) continue;
                        Vec3 out = flat(e.position().subtract(position()));
                        out = out.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : out.normalize();
                        if (e instanceof BeybladeEntity b) {
                            b.damageSpin(32F, this);
                            b.setDeltaMovement(b.getDeltaMovement().add(out.scale(0.35)));
                        } else if (e instanceof LivingEntity l) {
                            l.hurt(damageSources().indirectMagic(this, owner), 3F);
                            l.knockback(0.5, -out.x, -out.z);
                        }
                        sl.sendParticles(SILVER, e.getX(), e.getY() + 0.5, e.getZ(), 6, 0.2, 0.2, 0.2, 0);
                    }
                }
                return true;
            }
            case EA_TALON -> {
                if (target == null || !target.isAlive()) {
                    grabTick = -1;
                    endAbilityEarly();
                    return true;
                }
                if (t == 1) {
                    grabTick = -1;
                    play(ModSounds.EAGLE_SCREECH.get(), 1.8F, 1.0F);
                }
                if (t < T_SWOOP) {
                    hover(4.0, 0.3);
                    return true;
                }
                if (grabTick < 0) {
                    if (t == T_SWOOP) play(ModSounds.SKY_DIVE.get(), 1.6F, 1.0F);
                    setNoGravity(true);
                    Vec3 d = target.position().add(0, 0.5, 0).subtract(position());
                    double len = d.length();
                    setDeltaMovement(d.scale(Math.min(1.3, len) / Math.max(1.0E-3, len)));
                    if (len < 1.4) {
                        grabTick = t;
                        play(ModSounds.CLASH_HEAVY.get(), 1.4F, 1.2F);
                        this.level().broadcastEntityEvent(this, EV_GRAB);
                        if (target instanceof BeybladeEntity b) b.damageSpin(40F, this);
                        else if (target instanceof LivingEntity l) l.hurt(damageSources().indirectMagic(this, owner), 5F);
                    } else if (t > T_SWOOP_END) {
                        endAbilityEarly();
                    }
                    return true;
                }
                int carry = t - grabTick;
                if (carry < T_CARRY) {
                    // fly upward holding the target in the talons
                    hover(T_LIFT * Math.min(1.0, carry / 18.0) + 0.5, 0.25);
                    Vec3 hold = position().add(0, -0.75, 0);
                    target.setDeltaMovement(hold.subtract(target.position()).scale(0.6));
                    target.hurtMarked = true;
                    target.fallDistance = 0;
                    if (target instanceof BeybladeEntity b) b.damageSpin(2.5F, this);
                    if (carry % 4 == 0) sl.sendParticles(VIOLET, getX(), getY() - 0.3, getZ(), 4, 0.3, 0.2, 0.3, 0);
                    return true;
                }
                if (carry == T_CARRY) {
                    // hurl it into the ground
                    play(ModSounds.SKY_DIVE.get(), 1.4F, 1.4F);
                    target.setDeltaMovement(new Vec3(0, -2.2, 0));
                    target.hurtMarked = true;
                    if (target instanceof BeybladeEntity b) {
                        b.damageSpin(110F, this);
                        b.stun(25);
                    } else if (target instanceof LivingEntity l) {
                        l.hurt(damageSources().indirectMagic(this, owner), 9F);
                    }
                    this.level().broadcastEntityEvent(this, EV_SLAM);
                    abilityTicks = Math.max(abilityTicks, EA_DURATION[EA_TALON] - 14);
                }
                setNoGravity(false);
                return true;
            }
            case EA_WING -> {
                if (t == 1) {
                    wingHit.clear();
                    play(ModSounds.WING_BLADE.get(), 1.8F, 1.0F);
                }
                if (t == W_OPEN + 30) play(ModSounds.WING_BLADE.get(), 1.4F, 1.15F);
                if (target != null && t > W_OPEN && t < W_CLOSE) {
                    Vec3 d = flat(target.position().subtract(position()));
                    if (d.lengthSqr() > 1.0) setDeltaMovement(getDeltaMovement().scale(0.6).add(d.normalize().scale(0.18)));
                } else {
                    setDeltaMovement(getDeltaMovement().multiply(0.7, 1, 0.7));
                }
                if (t >= W_OPEN && t < W_CLOSE) {
                    for (Entity e : enemiesWithin(W_RADIUS + 0.5)) {
                        Integer last = wingHit.get(e.getId());
                        if (last != null && t - last < 8) continue;
                        wingHit.put(e.getId(), t);
                        Vec3 rel = flat(e.position().subtract(position()));
                        Vec3 out = rel.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : rel.normalize();
                        Vec3 tang = new Vec3(out.z, 0, -out.x);   // blades sweep with the (right) spin
                        if (e instanceof BeybladeEntity b) {
                            b.damageSpin(42F, this);
                            b.setDeltaMovement(b.getDeltaMovement().add(tang.scale(0.55)).add(out.scale(0.35)).add(0, 0.1, 0));
                        } else if (e instanceof LivingEntity l) {
                            l.hurt(damageSources().indirectMagic(this, owner), 6F);
                            l.knockback(0.9, -out.x, -out.z);
                        }
                        sl.sendParticles(ParticleTypes.SWEEP_ATTACK, e.getX(), e.getY() + 0.5, e.getZ(), 1, 0, 0, 0, 0);
                        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, e.getX(), e.getY() + 0.5, e.getZ(), 10, 0.2, 0.2, 0.2, 0.3);
                        play(ModSounds.CLASH.get(), 1.0F, 1.3F);
                    }
                }
                return true;
            }
            case EA_SKY -> {
                if (t == 1) play(ModSounds.EAGLE_SCREECH.get(), 2.4F, 0.8F);
                if (t == S_DESCEND) play(ModSounds.LANCE_RAIN.get(), 2.2F, 1.0F);
                if (t < S_SCREECH + 8) hover(1.5, 0.2); else setNoGravity(false);
                int rel = t - S_DESCEND - S_FALL;
                if (rel >= 0 && rel % S_STEP == 0 && rel / S_STEP < S_LANCES) {
                    Entity tgt = lanceTarget(rel / S_STEP);
                    if (tgt != null) {
                        if (tgt instanceof BeybladeEntity b) {
                            b.damageSpin(26F, this);
                            b.setDeltaMovement(b.getDeltaMovement().add(0, 0.2, 0));
                        } else if (tgt instanceof LivingEntity l) {
                            l.invulnerableTime = 0;
                            l.hurt(damageSources().indirectMagic(this, owner), 4F);
                        }
                        sl.sendParticles(ParticleTypes.END_ROD, tgt.getX(), tgt.getY() + 0.3, tgt.getZ(), 8, 0.2, 0.1, 0.2, 0.15);
                    }
                }
                if (t == S_SCREECH) {
                    play(ModSounds.EAGLE_SCREECH.get(), 2.6F, 0.7F);
                    this.level().broadcastEntityEvent(this, EV_SCREECH);
                    for (Entity e : enemiesWithin(S_RANGE)) {
                        Vec3 out = flat(e.position().subtract(position()));
                        out = out.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : out.normalize();
                        if (e instanceof BeybladeEntity b) {
                            b.damageSpin(60F, this);
                            b.setDeltaMovement(b.getDeltaMovement().add(out.scale(0.6)));
                        } else if (e instanceof LivingEntity l) {
                            l.hurt(damageSources().indirectMagic(this, owner), 6F);
                            l.knockback(1.0, -out.x, -out.z);
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

    /** which enemy the k-th light lance strikes (same rule on client and server so the visuals line up) */
    @Nullable
    public Entity lanceTarget(int k) {
        List<Entity> list = new ArrayList<>(enemiesWithin(S_RANGE));
        if (list.isEmpty()) return null;
        list.sort(Comparator.comparingInt(Entity::getId));
        return list.get(k % list.size());
    }

    // ---------------- client ----------------
    @Override
    public void handleEntityEvent(byte id) {
        switch (id) {
            case EV_DODGE -> {
                dodgeTime = this.tickCount;
                dodgeX = getX();
                dodgeY = getY();
                dodgeZ = getZ();
                for (int i = 0; i < 14; i++)
                    this.level().addParticle(i % 2 == 0 ? VIOLET : SILVER, getX(), getY() + 0.4, getZ(), this.random.nextGaussian() * 0.15, 0.08, this.random.nextGaussian() * 0.15);
            }
            case EV_GRAB -> grabTime = this.tickCount;
            case EV_SLAM -> slamTime = this.tickCount;
            case EV_SCREECH -> screechTime = this.tickCount;
            default -> super.handleEntityEvent(id);
        }
    }

    @Override
    protected void clientBuffParticles(Level lv) {
        if (this.random.nextInt(2) == 0) {
            double a = this.random.nextDouble() * Math.PI * 2;
            lv.addParticle(VIOLET, getX() + Math.cos(a) * 0.5, getY() + 0.5, getZ() + Math.sin(a) * 0.5, -getDeltaMovement().x * 0.3, 0.02, -getDeltaMovement().z * 0.3);
        }
    }

    @Override
    protected void clientAbilityParticles(int ab, float at, Level lv) {
        switch (ab) {
            case EA_FEATHER -> {
                if (at < F_GATHER) {
                    double a = this.random.nextDouble() * Math.PI * 2;
                    lv.addParticle(SILVER, getX() + Math.cos(a) * 2.5, getY() + 0.6, getZ() + Math.sin(a) * 2.5, -Math.cos(a) * 0.2, 0, -Math.sin(a) * 0.2);
                }
            }
            case EA_TALON -> lv.addParticle(VIOLET, getX(), getY() + 0.3, getZ(), 0, -0.05, 0);
            case EA_WING -> {
                if (at > W_OPEN && at < W_CLOSE) {
                    double a = this.random.nextDouble() * Math.PI * 2;
                    lv.addParticle(ParticleTypes.ELECTRIC_SPARK, getX() + Math.cos(a) * W_RADIUS, getY() + 0.4, getZ() + Math.sin(a) * W_RADIUS, 0, 0.05, 0);
                }
            }
            case EA_SKY -> {
                double a = this.random.nextDouble() * Math.PI * 2, r = this.random.nextDouble() * S_RANGE;
                lv.addParticle(VIOLET, abilityX + Math.cos(a) * r, abilityY + 6 + this.random.nextDouble() * 6, abilityZ + Math.sin(a) * r, 0, -0.1, 0);
            }
            default -> {}
        }
    }

    public static AABB unused() { return new AABB(0, 0, 0, 0, 0, 0); }

    public static float clamp01(float x) { return Mth.clamp(x, 0F, 1F); }
}
