package com.krolasyon.beyblade.entity;

import com.krolasyon.beyblade.registry.ModItems;
import com.krolasyon.beyblade.registry.ModSounds;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Evil Befall UW145EWD. A balance type with the dark peacock spirit: it raises its tail as a shield of eye feathers,
 * hypnotises its rival with the evil eye, fires storms of feathers, hides among rainbow mirages and finally calls the
 * giant dark peacock whose hundred eyes all strike at once.
 */
public class BeffalEntity extends BeybladeEntity {
    public static final int BF_PLUMAGE = 1, BF_GAZE = 2, BF_FEATHERS = 3, BF_MIRAGE = 4, BF_REQUIEM = 5;
    public static final int[] BF_DURATION = {0, 0, 50, 40, 110, 84};
    public static final int[] BF_COOLDOWN = {0, 620, 520, 460, 560, 1150};
    public static final int PLUMAGE_TICKS = 170;
    public static final int HYPNO_TICKS = 70;
    public static final int[] VOLLEY_AT = {8, 16, 24, 32};
    public static final int VOLLEY_N = 5;
    public static final double VOLLEY_RANGE = 11, MIRAGE_R = 1.7;
    public static final int MIRAGE_N = 3;
    public static final int REQ_FORM = 28, REQ_FIRE = 34, REQ_BLAST = 60, REQ_EYES = 12;
    public static final double REQ_RING = 4.2;

    public static final byte EV_GAZE = 111, EV_VOLLEY = 112, EV_REQUIEM = 113, EV_REQUIEM_HIT = 114, EV_MIRAGE_STRIKE = 115;

    // server
    private final Map<Integer, Integer> hypnotised = new HashMap<>();
    @Nullable private Vec3 requiemAt;

    // client
    public int gazeTime = -1000, requiemTime = -1000, requiemHitTime = -1000, mirageStrikeTime = -1000;
    public int gazeTargetId = -1;
    public final int[] volleyTime = {-1000, -1000, -1000, -1000};
    public final double[][] volleyDir = new double[4][3];
    public double reqX, reqY, reqZ;

    public BeffalEntity(EntityType<? extends BeffalEntity> type, Level level) {
        super(type, level);
    }

    // ---------------- hooks ----------------
    @Override public int nativeMode() { return MODE_BALANCE; }
    @Override public String kindId() { return "beffal"; }
    @Override public String abilityKey(int id) { return "ability.librabeyblade.beffal." + id; }
    @Override public int duration(int id) { return BF_DURATION[id]; }
    @Override public int cooldown(int id) { return BF_COOLDOWN[id]; }
    @Override public int buffAbility() { return BF_PLUMAGE; }
    @Override public int buffTicks() { return PLUMAGE_TICKS; }
    @Override public boolean requiresTarget(int id) { return id == BF_GAZE || id == BF_FEATHERS || id == BF_REQUIEM; }
    @Override public boolean buffReflects() { return true; }
    @Override public float incomingMul() { return isShielded() ? 0.55F : 0.9F; }
    @Override protected boolean ramsInto(int id) { return false; }
    @Override protected ItemStack defaultItem() { return new ItemStack(ModItems.BEFFAL.get()); }

    @Override
    protected float dodgeChance() {
        if (getAbility() == BF_MIRAGE) return 0.55F;
        return isBuffActive() ? 0.25F : 0.11F;
    }

    /** Rainbow Mirage: the rival hits an illusion and the real Befall strikes back from the side */
    @Override
    protected void onDodge(BeybladeEntity attacker) {
        if (getAbility() != BF_MIRAGE) return;
        attacker.damageSpin(45F * powerMul(), this);
        Vec3 away = flat(attacker.position().subtract(position()));
        away = away.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : away.normalize();
        attacker.setDeltaMovement(attacker.getDeltaMovement().add(away.scale(0.6 * attacker.knockMul())).add(0, 0.15, 0));
        this.level().broadcastEntityEvent(this, EV_MIRAGE_STRIKE);
        this.level().broadcastEntityEvent(attacker, EV_HEAVY_HIT);
        play(ModSounds.WING_BLADE.get(), 1.6F, 1.2F);
    }

    @Override
    protected float clientSpinSpeed(int ab, float at, float speed) {
        if (ab == BF_MIRAGE || ab == BF_REQUIEM) return speed * 1.35F;
        return speed;
    }

    /** Peacock Plumage Guard: the tail rises behind Befall as a wall of eye feathers */
    @Override
    protected void onBuffStart(@Nullable Player owner) {
        play(ModSounds.FEATHER_WHIRL.get(), 1.8F, 0.8F);
        play(ModSounds.EAGLE_SCREECH.get(), 1.4F, 1.5F);
        play(ModSounds.SHIELD_UP.get(), 1.4F, 0.8F);
    }

    @Override
    protected void tickBuff() {
        tickShield();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() || hypnotised.isEmpty()) return;
        // hypnotised rivals wander helplessly and their spin seeps away
        hypnotised.entrySet().removeIf(en -> {
            Entity e = this.level().getEntity(en.getKey());
            if (e == null || !e.isAlive() || this.tickCount > en.getValue()) return true;
            if (e instanceof BeybladeEntity b) {
                b.stun(3);
                if (this.tickCount % 6 == 0) {
                    double a = this.random.nextDouble() * Math.PI * 2;
                    b.setDeltaMovement(Math.cos(a) * 0.32, b.getDeltaMovement().y, Math.sin(a) * 0.32);
                }
                b.damageSpin(2.2F * powerMul(), this);
            } else if (e instanceof LivingEntity l && this.tickCount % 10 == 0) {
                l.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 3, false, false));
                l.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0, false, false));
            }
            return false;
        });
    }

    // ---------------- abilities ----------------
    @Override
    protected boolean tickAbility(int ab, int t, @Nullable Player owner) {
        switch (ab) {
            case BF_GAZE -> {
                // Evil Eye Hypnosis: a hundred eyes open and stare the rival into a trance
                if (target == null || !target.isAlive()) {
                    endAbilityEarly();
                    return true;
                }
                setDeltaMovement(getDeltaMovement().multiply(0.4, 1, 0.4));
                if (t == 1) {
                    play(ModSounds.JUDGMENT_START.get(), 1.6F, 1.4F);
                    play(ModSounds.EAGLE_SCREECH.get(), 1.4F, 0.7F);
                    this.level().broadcastEntityEvent(this, EV_GAZE);
                }
                if (t == 18) {
                    play(ModSounds.DRAIN.get(), 1.8F, 0.7F);
                    hypnotised.put(target.getId(), this.tickCount + HYPNO_TICKS);
                    if (target instanceof BeybladeEntity b) b.damageSpin(60F * powerMul(), this);
                    else if (target instanceof LivingEntity l) l.hurt(damageSources().indirectMagic(this, owner), 5F);
                    for (Entity e : enemiesWithin(4.5)) {
                        if (e != target && e instanceof BeybladeEntity b2) hypnotised.put(b2.getId(), this.tickCount + HYPNO_TICKS / 2);
                    }
                }
                return true;
            }
            case BF_FEATHERS -> {
                // Hundred Feather Storm: four fans of eye feathers shot at the rival
                if (target == null || !target.isAlive()) {
                    if (t > VOLLEY_AT[0]) return true;
                    endAbilityEarly();
                    return true;
                }
                setDeltaMovement(getDeltaMovement().multiply(0.5, 1, 0.5));
                if (t == 1) play(ModSounds.FEATHER_WHIRL.get(), 1.6F, 1.1F);
                for (int k = 0; k < VOLLEY_AT.length; k++) {
                    if (t != VOLLEY_AT[k]) continue;
                    Vec3 to = flat(target.position().subtract(position()));
                    Vec3 dir = to.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : to.normalize();
                    this.level().broadcastEntityEvent(this, (byte) (EV_VOLLEY));
                    play(ModSounds.FEATHER_BURST.get(), 1.6F, 1.0F + k * 0.08F);
                    for (Entity e : enemiesWithin(VOLLEY_RANGE)) {
                        Vec3 d = flat(e.position().subtract(position()));
                        double len = d.length();
                        if (len < 1.0E-3 || d.scale(1 / len).dot(dir) < 0.82) continue;
                        if (e instanceof BeybladeEntity b) {
                            if (b.tryDodge(this)) continue;
                            b.damageSpin((e == target ? 42F : 26F) * powerMul(), this);
                            b.setDeltaMovement(b.getDeltaMovement().add(dir.scale(0.25)));
                            this.level().broadcastEntityEvent(b, EV_HIT);
                        } else if (e instanceof LivingEntity l) {
                            l.invulnerableTime = 0;
                            l.hurt(damageSources().indirectMagic(this, owner), 4F);
                        }
                    }
                }
                return true;
            }
            case BF_MIRAGE -> {
                // Rainbow Mirage: three rainbow illusions circle Befall; most attacks hit an illusion
                if (t == 1) {
                    play(ModSounds.STAR_CHIME.get(), 1.8F, 0.8F);
                    play(ModSounds.FEATHER_WHIRL.get(), 1.4F, 1.4F);
                }
                if (t % 20 == 0) play(ModSounds.STAR_CHIME.get(), 0.8F, 1.2F + (t % 40) * 0.01F);
                return false;     // keeps fighting inside the mirage
            }
            case BF_REQUIEM -> {
                // Dark Peacock Requiem: the giant dark peacock spreads its tail, a ring of eyes surrounds the rival,
                // every eye fires and the ring collapses on it
                if (t == 1) {
                    play(ModSounds.EAGLE_SCREECH.get(), 2.4F, 0.55F);
                    play(ModSounds.JUDGMENT_START.get(), 2.0F, 0.8F);
                }
                hover(1.3, 0.2);
                if (target != null && target.isAlive() && t < REQ_BLAST) {
                    requiemAt = target.position();
                    if (t == 2 || t % 6 == 0) this.level().broadcastEntityEvent(this, EV_REQUIEM);
                    if (t >= REQ_FORM) {
                        // held in place by the stare of the eyes
                        target.setDeltaMovement(target.getDeltaMovement().multiply(0.2, 0.5, 0.2));
                        if (target instanceof BeybladeEntity b) b.stun(4);
                    }
                }
                if (t >= REQ_FIRE && t < REQ_BLAST && (t - REQ_FIRE) % 2 == 0) {
                    play(ModSounds.WING_BLADE.get(), 1.2F, 1.4F + (t - REQ_FIRE) * 0.02F);
                    if (target instanceof BeybladeEntity b) b.damageSpin(9F * powerMul(), this);
                }
                if (t == REQ_BLAST && requiemAt != null) {
                    play(ModSounds.JUDGMENT_STRIKE.get(), 3.0F, 0.7F);
                    play(ModSounds.METEOR_IMPACT.get(), 2.6F, 0.9F);
                    this.level().broadcastEntityEvent(this, EV_REQUIEM_HIT);
                    for (Entity e : this.level().getEntities(this, new net.minecraft.world.phys.AABB(requiemAt, requiemAt).inflate(6, 4, 6))) {
                        boolean enemy = e instanceof BeybladeEntity b ? isEnemy(b) : (e instanceof LivingEntity l && isHostileLiving(l));
                        if (!enemy && e != target) continue;
                        Vec3 out = flat(e.position().subtract(requiemAt));
                        double d = out.length();
                        out = d < 1.0E-3 ? new Vec3(1, 0, 0) : out.scale(1 / d);
                        boolean main = e == target || d < 1.5;
                        if (e instanceof BeybladeEntity b) {
                            b.damageSpin((main ? 290F : 100F) * powerMul(), this);
                            b.setDeltaMovement(b.getDeltaMovement().add(out.scale(main ? 0.5 : 0.9)).add(0, main ? 0.45 : 0.3, 0));
                            if (main) b.shockwave(this, 1.0);
                            hypnotised.put(b.getId(), this.tickCount + 30);
                        } else if (e instanceof LivingEntity l) {
                            l.hurt(damageSources().indirectMagic(this, owner), main ? 17F : 8F);
                            l.knockback(1.3, -out.x, -out.z);
                        }
                    }
                }
                if (t > REQ_BLAST + 6) setNoGravity(false);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    // ---------------- client ----------------
    @Override
    public void handleEntityEvent(byte id) {
        switch (id) {
            case EV_GAZE -> {
                gazeTime = this.tickCount;
                gazeTargetId = getTargetId();
            }
            case EV_VOLLEY -> {
                // a ring buffer of the last volleys (direction to the target when fired)
                int slot = 0;
                for (int i = 1; i < volleyTime.length; i++) if (volleyTime[i] < volleyTime[slot]) slot = i;
                volleyTime[slot] = this.tickCount;
                Entity t = this.level().getEntity(getTargetId());
                double dx = 1, dz = 0;
                if (t != null) {
                    dx = t.getX() - getX();
                    dz = t.getZ() - getZ();
                    double l = Math.sqrt(dx * dx + dz * dz);
                    if (l > 1.0E-3) { dx /= l; dz /= l; } else { dx = 1; dz = 0; }
                }
                volleyDir[slot][0] = dx;
                volleyDir[slot][2] = dz;
            }
            case EV_REQUIEM -> {
                if (requiemTime < clientAbilityStart) requiemTime = this.tickCount;
                Entity t = this.level().getEntity(getTargetId());
                if (t != null) {
                    reqX = t.getX();
                    reqY = t.getY();
                    reqZ = t.getZ();
                }
            }
            case EV_REQUIEM_HIT -> {
                requiemHitTime = this.tickCount;
                for (StadiumEntity st : this.level().getEntitiesOfClass(StadiumEntity.class, getBoundingBox().inflate(18, 6, 18))) {
                    double dx = reqX - st.getX(), dz = reqZ - st.getZ();
                    if (dx * dx + dz * dz < StadiumEntity.BOWL_R * StadiumEntity.BOWL_R) st.addScar(reqX, reqZ, 4.0F);
                    break;
                }
            }
            case EV_MIRAGE_STRIKE -> mirageStrikeTime = this.tickCount;
            default -> super.handleEntityEvent(id);
        }
    }
}
