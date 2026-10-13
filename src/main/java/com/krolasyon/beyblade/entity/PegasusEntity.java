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
    public static final int[] PG_DURATION = {0, 0, 100, 72, 64, 112};
    public static final int[] PG_COOLDOWN = {0, 600, 600, 550, 500, 1100};
    public static final int WINGS_TICKS = 200;

    public static final int S_FORM = 14, S_STRIKES = 16, S_STEP = 5;
    public static final double S_RANGE = 13, S_CLOUD_Y = 9;
    public static final int C_STAR0 = 6, C_STAR_STEP = 4, C_LINES = 24, C_COLLAPSE = 40, C_BLAST = 46;
    public static final double C_RADIUS = 4.0;
    public static final int R_FIRST = 4, R_STEP = 14, R_DASH = 11, R_COUNT = 4;
    public static final int G_START = 15, G_END = 95;
    public static final double G_RADIUS = 6.5;

    public static final byte EV_STAR = 100, EV_RICO = 101, EV_NOVA = 102;

    private static final DustParticleOptions SKY = new DustParticleOptions(new Vector3f(0.35F, 0.7F, 1.0F), 1.3F);

    // server
    private Vec3 ricoDir = new Vec3(1, 0, 0);
    @Nullable private Vec3 starCenter;
    private final Set<Integer> hits = new HashSet<>();
    private int lap = -1;

    // client
    public int starTime = -1000, ricoTime = -1000, novaTime = -1000;
    public double starX, starY, starZ;

    public PegasusEntity(EntityType<? extends PegasusEntity> type, Level level) {
        super(type, level);
    }

    // ---------------- hooks ----------------
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
        ((ServerLevel) this.level()).sendParticles(SKY, getX(), getY() + 0.6, getZ(), 30, 0.7, 0.4, 0.7, 0);
    }

    @Override
    protected void tickBuff() {
        if (this.tickCount % 3 == 0)
            ((ServerLevel) this.level()).sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.4, getZ(), 1, 0.2, 0.1, 0.2, 0.01);
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
                    sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y + 0.3, p.z, 20, 0.3, 0.3, 0.3, 0.5);
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
                    sl.sendParticles(ParticleTypes.END_ROD, starCenter.x, starCenter.y + 0.5, starCenter.z, 60, 1.5, 1.0, 1.5, 0.25);
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
                        sl.sendParticles(ParticleTypes.SWEEP_ATTACK, e.getX(), e.getY() + 0.5, e.getZ(), 1, 0, 0, 0, 0);
                        play(ModSounds.CLASH_HEAVY.get(), 1.1F, 1.3F);
                    }
                } else {
                    setDeltaMovement(getDeltaMovement().multiply(0.3, 1, 0.3));
                }
                return true;
            }
            case PG_GALLOP -> {
                if (t == 1) {
                    lap = -1;
                    play(ModSounds.PEGASUS_NEIGH.get(), 2.2F, 0.9F);
                }
                if (t == G_START) play(ModSounds.GALLOP.get(), 2.0F, 1.0F);
                if (t == G_START + 40) play(ModSounds.GALLOP.get(), 2.0F, 1.1F);
                hover(1.0, 0.2);
                if (t >= G_START && t < G_END) {
                    double ang = gallopAngle(t);
                    int curLap = (int) (ang / (Math.PI * 2));
                    if (curLap != lap) {
                        lap = curLap;
                        hits.clear();
                    }
                    Vec3 sp = new Vec3(abilityX() + Math.cos(ang) * G_RADIUS, abilityBaseY, abilityZ0() + Math.sin(ang) * G_RADIUS);
                    for (Entity e : enemiesWithin(G_RADIUS + 6)) {
                        Vec3 rel = flat(e.position().subtract(new Vec3(abilityX(), 0, abilityZ0())));
                        // keep everyone inside the gallop ring
                        if (rel.length() > G_RADIUS - 0.5 && rel.length() < G_RADIUS + 4) {
                            e.setDeltaMovement(e.getDeltaMovement().subtract(rel.normalize().scale(0.06)));
                            e.hurtMarked = true;
                        }
                        if (flat(e.position().subtract(sp)).length() < 2.3 && hits.add(e.getId())) {
                            Vec3 out = rel.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : rel.normalize();
                            if (e instanceof BeybladeEntity b) {
                                b.damageSpin(90F, this);
                                b.setDeltaMovement(out.scale(-0.4).add(0, 0.75, 0));
                            } else if (e instanceof LivingEntity l) {
                                l.hurt(damageSources().indirectMagic(this, owner), 10F);
                                l.setDeltaMovement(l.getDeltaMovement().add(0, 0.7, 0));
                            }
                            sl.sendParticles(ParticleTypes.CLOUD, e.getX(), e.getY() + 0.3, e.getZ(), 10, 0.3, 0.2, 0.3, 0.05);
                            play(ModSounds.CLASH_HEAVY.get(), 1.2F, 0.8F);
                        }
                    }
                }
                if (t == G_END) {
                    play(ModSounds.THUNDER_STRIKE.get(), 2.0F, 1.2F);
                    this.level().broadcastEntityEvent(this, EV_NOVA);
                    for (Entity e : enemiesWithin(G_RADIUS + 1.5)) {
                        Vec3 out = flat(e.position().subtract(position()));
                        out = out.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : out.normalize();
                        if (e instanceof BeybladeEntity b) {
                            b.damageSpin(50F, this);
                            b.setDeltaMovement(b.getDeltaMovement().add(out.scale(0.6)));
                        } else if (e instanceof LivingEntity l) {
                            l.hurt(damageSources().indirectMagic(this, owner), 5F);
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
            case EV_NOVA -> novaTime = this.tickCount;
            default -> super.handleEntityEvent(id);
        }
    }

    @Override
    protected void clientBuffParticles(Level lv) {
        lv.addParticle(SKY, getX() + this.random.nextGaussian() * 0.3, getY() + 0.5, getZ() + this.random.nextGaussian() * 0.3, 0, 0.03, 0);
    }

    @Override
    protected void clientAbilityParticles(int ab, float at, Level lv) {
        switch (ab) {
            case PG_STORM -> {
                for (int i = 0; i < 3; i++) {
                    double a = this.random.nextDouble() * Math.PI * 2, r = this.random.nextDouble() * S_RANGE * 0.8;
                    lv.addParticle(ParticleTypes.FALLING_WATER, abilityX + Math.cos(a) * r, abilityY + S_CLOUD_Y - 0.5, abilityZ + Math.sin(a) * r, 0, -0.5, 0);
                }
            }
            case PG_RICO, PG_GALLOP -> lv.addParticle(SKY, getX(), getY() + 0.4, getZ(), 0, 0.02, 0);
            default -> {}
        }
    }
}
