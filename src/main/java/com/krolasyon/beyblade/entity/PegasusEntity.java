package com.krolasyon.beyblade.entity;

import com.krolasyon.beyblade.registry.ModItems;
import com.krolasyon.beyblade.registry.ModSounds;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Storm Pegasus 105RF. The fastest attack type: storm, stars and a galloping spirit pegasus. Pegasus Wings makes it
 * dash far more often; its special moves are lightning, a collapsing constellation, ricochet dashes and a stampede.
 */
public class PegasusEntity extends BeybladeEntity {
    public static final int PG_WINGS = 1, PG_STORM = 2, PG_STAR = 3, PG_RICO = 4, PG_GALLOP = 5;
    public static final int[] PG_DURATION = {0, 0, 100, 72, 64, 110};
    public static final int[] PG_COOLDOWN = {0, 600, 600, 550, 500, 1100};
    public static final int WINGS_TICKS = 200;

    public static final int S_FORM = 14, S_STRIKES = 16, S_STEP = 5;
    public static final double S_RANGE = 13, S_CLOUD_Y = 9;
    public static final int C_STAR0 = 6, C_STAR_STEP = 4, C_LINES = 24, C_COLLAPSE = 40, C_BLAST = 46;
    public static final double C_RADIUS = 4.0;
    public static final int R_FIRST = 4, R_STEP = 14, R_DASH = 11, R_COUNT = 4;
    public static final int G_START = 15, G_END = 95;
    public static final int B_RIDE_END = 48, B_TOP = 60, B_DIVE = 68;
    public static final double B_HEIGHT = 9;
    public static final double G_RADIUS = 6.5;

    public static final byte EV_STAR = 100, EV_RICO = 101, EV_NOVA = 102;

    private static final DustParticleOptions SKY = new DustParticleOptions(new Vector3f(0.35F, 0.7F, 1.0F), 1.3F);

    // server
    private Vec3 ricoDir = new Vec3(1, 0, 0);
    @Nullable private Vec3 starCenter;
    private final Set<Integer> hits = new HashSet<>();
    private int lap = -1;
    private double bcx, bcz, br, bang;

    // client
    public int starTime = -1000, ricoTime = -1000, novaTime = -1000;
    public double starX, starY, starZ;

    public PegasusEntity(EntityType<? extends PegasusEntity> type, Level level) {
        super(type, level);
    }

    // ---------------- hooks ----------------
    @Override protected float dodgeChance() { return isBuffActive() ? 0.3F : 0.16F; }
    @Override public int nativeMode() { return MODE_ATTACK; }
    @Override public String kindId() { return "pegasus"; }
    @Override public String abilityKey(int id) { return "ability.librabeyblade.pegasus." + id; }
    @Override public int duration(int id) { return PG_DURATION[id]; }
    @Override public int cooldown(int id) { return PG_COOLDOWN[id]; }
    @Override public int buffAbility() { return PG_WINGS; }
    @Override public int buffTicks() { return WINGS_TICKS; }
    @Override public boolean requiresTarget(int id) { return id == PG_STAR; }
    @Override public boolean buffReflects() { return false; }
    @Override public float buffAttackMul() { return isShielded() ? 1.4F : 1.1F; }
    @Override public double buffSpeedMul() { return isShielded() ? 1.65 : 1.12; }
    @Override protected ItemStack defaultItem() { return new ItemStack(ModItems.PEGASUS.get()); }

    @Override
    protected float clientSpinSpeed(int ab, float at, float speed) {
        if (ab == PG_RICO || ab == PG_GALLOP) return 105F;
        return isShielded() ? speed * 1.2F : speed;
    }

    /** Pegasus Wings: 10 s of +65% speed, +40% impact and far more frequent dashes */
    @Override
    protected void onBuffStart(@Nullable Player owner) {
        play(ModSounds.PEGASUS_NEIGH.get(), 1.8F, 1.0F);
        Px.send((ServerLevel) this.level(), SKY, getX(), getY() + 0.6, getZ(), 30, 0.7, 0.4, 0.7, 0);
    }

    @Override
    protected void tickBuff() {
        if (this.tickCount % 3 == 0)
            Px.send((ServerLevel) this.level(), ParticleTypes.END_ROD, getX(), getY() + 0.4, getZ(), 1, 0.2, 0.1, 0.2, 0.01);
    }

    // ---------------- abilities ----------------
    @Override
    protected boolean tickAbility(int ab, int t, @Nullable Player owner) {
        ServerLevel sl = (ServerLevel) this.level();
        switch (ab) {
            case PG_STORM -> {
                if (t == 1) play(ModSounds.STORM_CLOUD.get(), 2.0F, 1.0F);
                hover(0.6, 0.2);
                int rel = t - S_FORM;
                if (rel >= 0 && rel % S_STEP == 0 && rel / S_STEP < S_STRIKES) {
                    int k = rel / S_STEP;
                    Entity tgt = strikeTarget(k);
                    Vec3 p = tgt != null ? tgt.position() : strikePoint(k, getX(), getY(), getZ());
                    this.level().playSound(null, p.x, p.y, p.z, ModSounds.THUNDER_STRIKE.get(), net.minecraft.sounds.SoundSource.NEUTRAL, 1.6F, 0.9F + this.random.nextFloat() * 0.3F);
                    Px.send(sl, ParticleTypes.ELECTRIC_SPARK, p.x, p.y + 0.3, p.z, 20, 0.3, 0.3, 0.3, 0.5);
                    if (tgt instanceof BeybladeEntity b) {
                        b.damageSpin(28F, this);
                        b.stun(6);
                    } else if (tgt instanceof LivingEntity l) {
                        l.invulnerableTime = 0;
                        l.hurt(damageSources().indirectMagic(this, owner), 4F);
                    }
                }
                return true;
            }
            case PG_STAR -> {
                if (t == 1) {
                    if (target == null) {
                        endAbilityEarly();
                        return true;
                    }
                    starCenter = target.position();
                    this.level().broadcastEntityEvent(this, EV_STAR);
                    play(ModSounds.STAR_CHIME.get(), 1.6F, 1.0F);
                }
                if (starCenter == null) return true;
                hover(1.0, 0.2);
                int rel = t - C_STAR0;
                if (rel >= 0 && rel % C_STAR_STEP == 0 && rel / C_STAR_STEP < 5) play(ModSounds.STAR_CHIME.get(), 1.0F, 1.2F + rel / C_STAR_STEP * 0.12F);
                if (t >= C_LINES && t < C_BLAST) {
                    // trapped inside the constellation
                    for (Entity e : enemiesWithin(24)) {
                        Vec3 d = flat(e.position().subtract(starCenter));
                        if (d.length() < C_RADIUS + 0.5) {
                            e.setDeltaMovement(e.getDeltaMovement().multiply(0.4, 1, 0.4).subtract(d.scale(0.04)));
                            e.hurtMarked = true;
                        }
                    }
                }
                if (t == C_BLAST) {
                    play(ModSounds.THUNDER_STRIKE.get(), 2.0F, 1.4F);
                    play(ModSounds.STAR_CHIME.get(), 2.0F, 0.7F);
                    this.level().broadcastEntityEvent(this, EV_NOVA);
                    Px.send(sl, ParticleTypes.END_ROD, starCenter.x, starCenter.y + 0.5, starCenter.z, 60, 1.5, 1.0, 1.5, 0.25);
                    for (Entity e : enemiesWithin(30)) {
                        Vec3 d = flat(e.position().subtract(starCenter));
                        double dist = d.length();
                        if (dist > C_RADIUS + 1) continue;
                        Vec3 out = dist < 1.0E-3 ? new Vec3(1, 0, 0) : d.scale(1 / dist);
                        if (e instanceof BeybladeEntity b) {
                            b.damageSpin(150F, this);
                            b.setDeltaMovement(out.scale(0.6).add(0, 0.7, 0));
                        } else if (e instanceof LivingEntity l) {
                            l.hurt(damageSources().indirectMagic(this, owner), 12F);
                            l.knockback(1.0, -out.x, -out.z);
                        }
                    }
                }
                return true;
            }
            case PG_RICO -> {
                int rel = t - R_FIRST;
                if (rel < 0) {
                    setDeltaMovement(getDeltaMovement().multiply(0.5, 1, 0.5));
                    return true;
                }
                int k = rel / R_STEP, s = rel % R_STEP;
                if (k >= R_COUNT) {
                    setNoGravity(false);
                    setDeltaMovement(getDeltaMovement().multiply(0.6, 1, 0.6));
                    return true;
                }
                if (s == 0) {
                    hits.clear();
                    Vec3 base = new Vec3(abilityX(), abilityBaseY, abilityZ0());
                    Vec3 dir;
                    if (k == 0 && target != null) dir = flat(target.position().subtract(position()));
                    else dir = rotate(ricoDir, Math.toRadians(140 + this.random.nextInt(60) - 30));
                    if (flat(position().subtract(base)).length() > 6) dir = flat(base.subtract(position()));
                    ricoDir = dir.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : dir.normalize();
                    play(ModSounds.RICOCHET.get(), 1.4F, 0.9F + k * 0.1F);
                    this.level().broadcastEntityEvent(this, EV_RICO);
                }
                if (s < R_DASH) {
                    setNoGravity(true);
                    hover(0.35, 0.3);
                    setDeltaMovement(ricoDir.x * 1.15, getDeltaMovement().y, ricoDir.z * 1.15);
                    for (Entity e : enemiesWithin(1.7)) {
                        if (!hits.add(e.getId())) continue;
                        Vec3 side = new Vec3(-ricoDir.z, 0, ricoDir.x);
                        if (side.dot(e.position().subtract(position())) < 0) side = side.scale(-1);
                        if (e instanceof BeybladeEntity b) {
                            b.damageSpin(70F, this);
                            b.setDeltaMovement(b.getDeltaMovement().add(side.scale(0.6)).add(ricoDir.scale(0.3)).add(0, 0.2, 0));
                        } else if (e instanceof LivingEntity l) {
                            l.hurt(damageSources().indirectMagic(this, owner), 7F);
                            l.knockback(0.9, -side.x, -side.z);
                        }
                        Px.send(sl, ParticleTypes.SWEEP_ATTACK, e.getX(), e.getY() + 0.5, e.getZ(), 1, 0, 0, 0, 0);
                        play(ModSounds.CLASH_HEAVY.get(), 1.1F, 1.3F);
                    }
                } else {
                    setDeltaMovement(getDeltaMovement().multiply(0.3, 1, 0.3));
                }
                return true;
            }
            case PG_GALLOP -> {
                // Star Booster Attack: race round the stadium rim dragging everyone into the middle, launch off the wall,
                // hang in the sky, then come down on the target like a falling star
                if (t == 1) {
                    double[] c = boosterRing(this.level(), getX(), getZ());
                    bcx = c[0];
                    bcz = c[1];
                    br = c[2];
                    bang = Math.atan2(getZ() - bcz, getX() - bcx);
                    hits.clear();
                    play(ModSounds.PEGASUS_NEIGH.get(), 2.2F, 0.9F);
                }
                if (t < B_RIDE_END) {
                    double speed = 0.55 + 0.85 * t / (double) B_RIDE_END;
                    bang += speed / br * spinDir();
                    Vec3 goal = new Vec3(bcx + Math.cos(bang) * br, getY(), bcz + Math.sin(bang) * br);
                    Vec3 d = flat(goal.subtract(position()));
                    setDeltaMovement(d.x, getDeltaMovement().y, d.z);
                    if (t % 12 == 1) play(ModSounds.GALLOP.get(), 1.6F, 1.0F + t * 0.01F);
                    if (t % 16 == 0) hits.clear();
                    for (Entity e : enemiesWithin(br + 8)) {
                        Vec3 rel = flat(e.position().subtract(new Vec3(bcx, 0, bcz)));
                        double dist = rel.length();
                        if (dist < 1.0E-3) continue;
                        Vec3 in = rel.scale(-1 / dist);
                        Vec3 tan = new Vec3(-in.z, 0, in.x).scale(spinDir());
                        e.setDeltaMovement(e.getDeltaMovement().add(in.scale(dist > 1.5 ? 0.08 : 0.0)).add(tan.scale(0.05)));
                        e.hurtMarked = true;
                        if (e.distanceToSqr(this) < 1.6 * 1.6 && hits.add(e.getId())) {
                            if (e instanceof BeybladeEntity b) {
                                b.damageSpin(45F * powerMul(), this);
                                b.setDeltaMovement(b.getDeltaMovement().add(in.scale(0.5)));
                                this.level().broadcastEntityEvent(b, EV_HEAVY_HIT);
                            } else if (e instanceof LivingEntity l) l.hurt(damageSources().indirectMagic(this, owner), 5F);
                            play(ModSounds.CLASH_HEAVY.get(), 1.2F, 1.1F);
                        }
                    }
                    return true;
                }
                Vec3 aim = target != null && target.isAlive() ? target.position() : new Vec3(bcx, abilityBaseY, bcz);
                if (t < B_DIVE) {
                    setNoGravity(true);
                    if (t == B_RIDE_END) {
                        play(ModSounds.PEGASUS_NEIGH.get(), 2.4F, 1.2F);
                        play(ModSounds.STAR_CHIME.get(), 2.0F, 1.0F);
                    }
                    Vec3 d = aim.add(0, B_HEIGHT, 0).subtract(position());
                    setDeltaMovement(d.scale(t < B_TOP ? 0.18 : 0.3));
                    return true;
                }
                if (t == B_DIVE) {
                    diving = true;
                    play(ModSounds.THUNDER_STRIKE.get(), 2.2F, 1.3F);
                }
                if (diving) {
                    Vec3 d = aim.add(0, 0.2, 0).subtract(position());
                    if (d.lengthSqr() > 1.0E-4) setDeltaMovement(d.normalize().scale(2.6));
                    boolean hit = target != null && target.distanceToSqr(this) < 1.6 * 1.6;
                    if (hit || onGround() || verticalCollision || t > B_DIVE + 20) {
                        diving = false;
                        setNoGravity(false);
                        setDeltaMovement(0, 0, 0);
                        boosterImpact(owner);
                        abilityTicks = Math.max(abilityTicks, PG_DURATION[PG_GALLOP] - 14);
                    }
                }
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    /** centre (x, z) and radius of the Star Booster ring: the stadium rim when Pegasus is in a stadium */
    public static double[] boosterRing(net.minecraft.world.level.Level level, double x, double z) {
        for (StadiumEntity st : level.getEntitiesOfClass(StadiumEntity.class, new net.minecraft.world.phys.AABB(x - 20, -1000, z - 20, x + 20, 1000, z + 20))) {
            double dx = x - st.getX(), dz = z - st.getZ();
            if (dx * dx + dz * dz < StadiumEntity.WALL_IN * StadiumEntity.WALL_IN) return new double[]{st.getX(), st.getZ(), StadiumEntity.BOWL_R * 0.8};
        }
        return new double[]{x, z, 5.5};
    }

    private void boosterImpact(@Nullable Player owner) {
        play(ModSounds.CLASH_HEAVY.get(), 3.0F, 0.6F);
        play(ModSounds.THUNDER_STRIKE.get(), 2.5F, 0.9F);
        this.level().broadcastEntityEvent(this, EV_NOVA);
        this.level().broadcastEntityEvent(this, EV_IMPACT);
        for (Entity e : enemiesWithin(5.5)) {
            Vec3 out = flat(e.position().subtract(position()));
            out = out.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : out.normalize();
            boolean main = e == target;
            if (e instanceof BeybladeEntity b) {
                b.damageSpin((main ? 260F : 90F) * powerMul(), this);
                b.setDeltaMovement(b.getDeltaMovement().add(out.scale(main ? 0.9 : 0.6)).add(0, 0.35, 0));
                if (main) b.shockwave(this, 1.0);
            } else if (e instanceof LivingEntity l) {
                l.hurt(damageSources().indirectMagic(this, owner), main ? 14F : 6F);
                l.knockback(1.2, -out.x, -out.z);
            }
        }
    }

    private double abilityX() { return abilityBaseX; }
    private double abilityZ0() { return abilityBaseZ; }

    private static Vec3 rotate(Vec3 v, double a) {
        double c = Math.cos(a), s = Math.sin(a);
        return new Vec3(v.x * c - v.z * s, 0, v.x * s + v.z * c);
    }

    /** angle of the spirit pegasus around the ring (two laps, counter-clockwise from above) */
    public static double gallopAngle(float t) {
        return Math.max(0, t - G_START) / (G_END - G_START) * Math.PI * 4;
    }

    /** k-th lightning strike target: enemies in id order, same rule on client and server */
    @Nullable
    public Entity strikeTarget(int k) {
        List<Entity> list = new ArrayList<>(enemiesWithin(S_RANGE));
        if (list.isEmpty()) return null;
        list.sort(Comparator.comparingInt(Entity::getId));
        return list.get(k % list.size());
    }

    public static Vec3 strikePoint(int k, double x, double y, double z) {
        double a = k * 2.4;
        double r = 2.5 + (k % 3) * 2.0;
        return new Vec3(x + Math.cos(a) * r, y, z + Math.sin(a) * r);
    }

    // ---------------- client ----------------
    @Override
    public void handleEntityEvent(byte id) {
        switch (id) {
            case EV_STAR -> {
                starTime = this.tickCount;
                Entity t = this.level().getEntity(getTargetId());
                Entity c = t != null ? t : this;
                starX = c.getX();
                starY = c.getY();
                starZ = c.getZ();
            }
            case EV_RICO -> ricoTime = this.tickCount;
            case EV_NOVA -> { novaTime = this.tickCount; clientScar(2.5F); }
            default -> super.handleEntityEvent(id);
        }
    }

    @Override
    protected void clientBuffParticles(Level lv) {
        Px.add(lv, SKY, getX() + this.random.nextGaussian() * 0.3, getY() + 0.5, getZ() + this.random.nextGaussian() * 0.3, 0, 0.03, 0);
    }

    @Override
    protected void clientAbilityParticles(int ab, float at, Level lv) {
        switch (ab) {
            case PG_STORM -> {
                for (int i = 0; i < 3; i++) {
                    double a = this.random.nextDouble() * Math.PI * 2, r = this.random.nextDouble() * S_RANGE * 0.8;
                    Px.add(lv, ParticleTypes.FALLING_WATER, abilityX + Math.cos(a) * r, abilityY + S_CLOUD_Y - 0.5, abilityZ + Math.sin(a) * r, 0, -0.5, 0);
                }
            }
            case PG_RICO, PG_GALLOP -> Px.add(lv, SKY, getX(), getY() + 0.4, getZ(), 0, 0.02, 0);
            default -> {}
        }
    }
}
