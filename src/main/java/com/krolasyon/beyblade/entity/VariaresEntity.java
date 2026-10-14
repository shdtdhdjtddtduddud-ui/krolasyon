package com.krolasyon.beyblade.entity;

import com.krolasyon.beyblade.registry.ModItems;
import com.krolasyon.beyblade.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Variares 145WB. An attack type named after the god of war: it burns with the war god's fury, cuts with lightning
 * sword flashes, charges behind a war shield, whirls twin blades around itself and finally brings down a giant sword of
 * lightning.
 */
public class VariaresEntity extends BeybladeEntity {
    public static final int VA_FURY = 1, VA_FLASH = 2, VA_SHIELD = 3, VA_BLADES = 4, VA_FINAL = 5;
    public static final int[] VA_DURATION = {0, 0, 40, 36, 64, 80};
    public static final int[] VA_COOLDOWN = {0, 600, 420, 480, 520, 1100};
    public static final int FURY_TICKS = 180;
    public static final int[] FLASH_AT = {6, 17, 28};
    public static final int BLADES_N = 4;
    public static final double BLADES_R = 2.3;
    public static final int FINAL_FORM = 26, FINAL_FALL = 36;
    public static final double SWORD_HEIGHT = 15;

    public static final byte EV_SLASH = 125, EV_SWORD = 126, EV_SWORD_HIT = 127;

    // server
    private final Map<Integer, Integer> bladeHits = new HashMap<>();
    @Nullable private Vec3 swordAt;

    // client
    public int slashTime = -1000, swordTime = -1000, swordHitTime = -1000;
    public double slashX0, slashZ0, slashX1, slashZ1, slashY;
    public double swordX, swordY, swordZ;

    public VariaresEntity(EntityType<? extends VariaresEntity> type, Level level) {
        super(type, level);
    }

    // ---------------- hooks ----------------
    @Override public int nativeMode() { return MODE_ATTACK; }
    @Override public String kindId() { return "variares"; }
    @Override public String abilityKey(int id) { return "ability.librabeyblade.variares." + id; }
    @Override public int duration(int id) { return VA_DURATION[id]; }
    @Override public int cooldown(int id) { return VA_COOLDOWN[id]; }
    @Override public int buffAbility() { return VA_FURY; }
    @Override public int buffTicks() { return FURY_TICKS; }
    @Override public boolean requiresTarget(int id) { return id == VA_FLASH || id == VA_SHIELD || id == VA_FINAL; }
    @Override public boolean buffReflects() { return false; }
    @Override public float buffAttackMul() { return isShielded() ? 1.7F : 1.1F; }
    @Override public double buffSpeedMul() { return isShielded() ? 1.35 : 1.08; }
    @Override protected float dodgeChance() { return 0.09F; }
    @Override protected ItemStack defaultItem() { return new ItemStack(ModItems.VARIARES.get()); }

    @Override
    protected float clientSpinSpeed(int ab, float at, float speed) {
        if (ab == VA_BLADES || ab == VA_SHIELD) return speed * 1.4F;
        return speed;
    }

    /** War God's Fury: a crimson battle aura - far stronger, faster hits */
    @Override
    protected void onBuffStart(@Nullable Player owner) {
        play(ModSounds.DRAGON_ROAR.get(), 1.8F, 0.6F);
        play(ModSounds.CLASH_HEAVY.get(), 1.6F, 0.5F);
    }

    @Override
    protected void tickBuff() {
        tickShield();
    }

    // ---------------- abilities ----------------
    @Override
    protected boolean tickAbility(int ab, int t, @Nullable Player owner) {
        switch (ab) {
            case VA_FLASH -> {
                // Lightning Sword Flash: three blinding cuts straight through the target
                if (target == null || !target.isAlive()) {
                    endAbilityEarly();
                    return true;
                }
                setDeltaMovement(getDeltaMovement().multiply(0.3, 1, 0.3));
                for (int k = 0; k < FLASH_AT.length; k++) {
                    if (t != FLASH_AT[k]) continue;
                    Vec3 to = flat(target.position().subtract(position()));
                    Vec3 dir = to.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : to.normalize();
                    // each cut comes in from a different angle
                    double rot = (k - 1) * 0.9;
                    Vec3 d = new Vec3(dir.x * Math.cos(rot) - dir.z * Math.sin(rot), 0, dir.x * Math.sin(rot) + dir.z * Math.cos(rot));
                    Vec3 end = target.position().add(d.scale(2.6));
                    this.level().broadcastEntityEvent(this, EV_SLASH);
                    if (this.level().noCollision(this, getBoundingBox().move(end.subtract(position())))) setPos(end.x, getY(), end.z);
                    play(ModSounds.CLAW_SLASH.get(), 1.8F, 1.3F);
                    play(ModSounds.THUNDER_STRIKE.get(), 1.0F, 1.8F);
                    if (target instanceof BeybladeEntity b) {
                        if (!b.tryDodge(this)) {
                            b.damageSpin(65F * powerMul(), this);
                            b.setDeltaMovement(b.getDeltaMovement().add(d.scale(-0.35)).add(0, 0.1, 0));
                            this.level().broadcastEntityEvent(b, EV_HEAVY_HIT);
                        }
                    } else if (target instanceof LivingEntity l) {
                        l.invulnerableTime = 0;
                        l.hurt(damageSources().indirectMagic(this, owner), 6F);
                    }
                }
                return true;
            }
            case VA_SHIELD -> {
                // War Shield Charge: a crimson shield in front, ramming straight through everything
                if (t == 1) play(ModSounds.SHIELD_UP.get(), 1.6F, 0.7F);
                Vec3 aim = target != null && target.isAlive() ? target.position() : position().add(getDeltaMovement().scale(10));
                Vec3 dir = flat(aim.subtract(position()));
                dir = dir.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : dir.normalize();
                if (t > 4) setDeltaMovement(dir.x * 0.85, getDeltaMovement().y, dir.z * 0.85);
                for (Entity e : enemiesWithin(1.6)) {
                    Integer last = bladeHits.get(e.getId());
                    if (last != null && t - last < 12) continue;
                    bladeHits.put(e.getId(), t);
                    if (e instanceof BeybladeEntity b) {
                        b.damageSpin(80F * powerMul(), this);
                        b.setDeltaMovement(dir.scale(1.4 * b.knockMul()).add(0, 0.2, 0));
                        b.shockwave(this, 0.9);
                    } else if (e instanceof LivingEntity l) {
                        l.hurt(damageSources().indirectMagic(this, owner), 8F);
                        l.knockback(1.6, -dir.x, -dir.z);
                    }
                    play(ModSounds.CLASH_HEAVY.get(), 2.2F, 0.6F);
                }
                if (t == VA_DURATION[VA_SHIELD] - 1) bladeHits.clear();
                return t > 4;
            }
            case VA_BLADES -> {
                // Twin Blade Whirlwind: four crimson blades orbit Variares and cut everything close
                if (t == 1) {
                    bladeHits.clear();
                    play(ModSounds.CLAW_SLASH.get(), 1.6F, 0.8F);
                }
                if (t % 8 == 0) play(ModSounds.CLAW_SLASH.get(), 1.2F, 1.0F + (t % 16) * 0.02F);
                for (Entity e : enemiesWithin(BLADES_R + 0.8)) {
                    Integer last = bladeHits.get(e.getId());
                    if (last != null && t - last < 8) continue;
                    bladeHits.put(e.getId(), t);
                    Vec3 out = flat(e.position().subtract(position()));
                    out = out.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : out.normalize();
                    Vec3 tan = new Vec3(-out.z, 0, out.x).scale(spinDir());
                    if (e instanceof BeybladeEntity b) {
                        b.damageSpin(24F * powerMul(), this);
                        b.setDeltaMovement(b.getDeltaMovement().add(tan.scale(0.35)).add(out.scale(0.2)));
                        this.level().broadcastEntityEvent(b, EV_HIT);
                    } else if (e instanceof LivingEntity l) {
                        l.invulnerableTime = 0;
                        l.hurt(damageSources().indirectMagic(this, owner), 3F);
                    }
                }
                return false;     // keeps fighting while the blades turn
            }
            case VA_FINAL -> {
                // Final Variares Lightning Sword: a giant sword forms in the sky and falls on the target
                if (t == 1) {
                    play(ModSounds.DRAGON_ROAR.get(), 2.0F, 0.55F);
                    play(ModSounds.STORM_CLOUD.get(), 2.0F, 0.8F);
                }
                hover(1.5, 0.2);
                if (t < FINAL_FALL && target != null && target.isAlive()) {
                    swordAt = target.position();
                    if (t == 2 || t % 5 == 0) this.level().broadcastEntityEvent(this, EV_SWORD);
                }
                if (t == FINAL_FORM) play(ModSounds.THUNDER_STRIKE.get(), 2.5F, 0.7F);
                if (t == FINAL_FALL && swordAt != null) {
                    ServerLevel sl = (ServerLevel) this.level();
                    play(ModSounds.METEOR_IMPACT.get(), 3.0F, 0.8F);
                    play(ModSounds.THUNDER_STRIKE.get(), 3.0F, 0.6F);
                    this.level().broadcastEntityEvent(this, EV_SWORD_HIT);
                    for (int i = 0; i < 3; i++) {
                        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(sl);
                        if (bolt == null) continue;
                        bolt.moveTo(swordAt.x + this.random.nextGaussian() * 2, swordAt.y, swordAt.z + this.random.nextGaussian() * 2);
                        bolt.setVisualOnly(true);
                        sl.addFreshEntity(bolt);
                    }
                    for (Entity e : this.level().getEntities(this, new net.minecraft.world.phys.AABB(swordAt, swordAt).inflate(6.5, 4, 6.5))) {
                        boolean enemy = e instanceof BeybladeEntity b ? isEnemy(b) : (e instanceof LivingEntity l && isHostileLiving(l));
                        if (!enemy && e != target) continue;
                        Vec3 out = flat(e.position().subtract(swordAt));
                        double d = out.length();
                        out = d < 1.0E-3 ? new Vec3(1, 0, 0) : out.scale(1 / d);
                        boolean main = e == target || d < 1.5;
                        if (e instanceof BeybladeEntity b) {
                            b.damageSpin((main ? 300F : 110F) * powerMul(), this);
                            b.setDeltaMovement(b.getDeltaMovement().add(out.scale(main ? 0.4 : 0.9)).add(0, main ? 0.5 : 0.3, 0));
                            if (main) b.shockwave(this, 1.0);
                        } else if (e instanceof LivingEntity l) {
                            l.hurt(damageSources().indirectMagic(this, owner), main ? 18F : 8F);
                            l.knockback(1.4, -out.x, -out.z);
                        }
                    }
                }
                if (t > FINAL_FALL + 6) setNoGravity(false);
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
            case EV_SLASH -> {
                slashTime = this.tickCount;
                Entity t = this.level().getEntity(getTargetId());
                slashX0 = getX();
                slashZ0 = getZ();
                slashY = getY() + 0.4;
                if (t != null) {
                    Vec3 d = new Vec3(t.getX() - getX(), 0, t.getZ() - getZ());
                    Vec3 e = d.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : d.normalize();
                    slashX1 = t.getX() + e.x * 2.6;
                    slashZ1 = t.getZ() + e.z * 2.6;
                } else {
                    slashX1 = getX() + 3;
                    slashZ1 = getZ();
                }
            }
            case EV_SWORD -> {
                if (swordTime < clientAbilityStart) swordTime = this.tickCount;
                Entity t = this.level().getEntity(getTargetId());
                if (t != null) {
                    swordX = t.getX();
                    swordY = t.getY();
                    swordZ = t.getZ();
                }
            }
            case EV_SWORD_HIT -> {
                swordHitTime = this.tickCount;
                for (StadiumEntity st : this.level().getEntitiesOfClass(StadiumEntity.class, getBoundingBox().inflate(18, 6, 18))) {
                    double dx = swordX - st.getX(), dz = swordZ - st.getZ();
                    if (dx * dx + dz * dz < StadiumEntity.BOWL_R * StadiumEntity.BOWL_R) st.addScar(swordX, swordZ, 4.5F);
                    break;
                }
            }
            default -> super.handleEntityEvent(id);
        }
    }
}
