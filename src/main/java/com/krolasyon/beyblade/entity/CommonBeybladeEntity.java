package com.krolasyon.beyblade.entity;

import com.krolasyon.beyblade.item.LibraItem;
import com.krolasyon.beyblade.registry.ModItems;
import com.krolasyon.beyblade.registry.ModSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * The ten ordinary beyblades (starter tops). They share one model with their own colours and emblem, have a battle
 * type each, and three moves: Power Rush, Spin Guard and a type move that unlocks at level {@link #BURST_UNLOCK}.
 */
public class CommonBeybladeEntity extends BeybladeEntity {
    public record Variant(String id, int mode, int primary, int secondary) {
        public float[] rgb(boolean primaryColour) {
            int c = primaryColour ? primary : secondary;
            return new float[]{((c >> 16) & 255) / 255F, ((c >> 8) & 255) / 255F, (c & 255) / 255F};
        }
    }

    public static final Variant[] VARIANTS = {
            new Variant("iron_bull", MODE_ATTACK, 0xD83228, 0xFFC23A),
            new Variant("frost_wolf", MODE_BALANCE, 0x6EC8FF, 0xF0FAFF),
            new Variant("jade_turtle", MODE_DEFENSE, 0x2FA24A, 0xB8FF9A),
            new Variant("thunder_viper", MODE_ATTACK, 0xF5D21E, 0x9A4CFF),
            new Variant("shadow_bat", MODE_STAMINA, 0x6B2FB5, 0xD9B8FF),
            new Variant("sky_falcon", MODE_BALANCE, 0x3E8EF0, 0xFFFFFF),
            new Variant("crimson_scorpion", MODE_ATTACK, 0xA0102A, 0xFF8A3A),
            new Variant("golden_phoenix", MODE_STAMINA, 0xFFB020, 0xFF5A1A),
            new Variant("ocean_shark", MODE_DEFENSE, 0x1A4FA0, 0x40E0D0),
            new Variant("storm_fox", MODE_BALANCE, 0xFF7A1A, 0xFFF2E0),
    };

    public static final int CM_RUSH = 1, CM_GUARD = 2, CM_BURST = 3;
    public static final int[] CM_DURATION = {0, 30, 0, 40, 0, 0};
    public static final int[] CM_COOLDOWN = {0, 260, 520, 720, 1, 1};
    public static final int GUARD_TICKS = 160, BURST_UNLOCK = 5;

    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(CommonBeybladeEntity.class, EntityDataSerializers.INT);

    public CommonBeybladeEntity(EntityType<? extends CommonBeybladeEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_VARIANT, 0);
    }

    public int getVariant() { return Math.floorMod(this.entityData.get(DATA_VARIANT), VARIANTS.length); }
    public void setVariant(int v) { this.entityData.set(DATA_VARIANT, Math.floorMod(v, VARIANTS.length)); }
    public Variant variant() { return VARIANTS[getVariant()]; }

    @Override
    protected void initFromItem(ItemStack stack) {
        if (stack.getItem() instanceof LibraItem li && li.variant() >= 0) setVariant(li.variant());
    }

    // ---------------- hooks ----------------
    @Override public int nativeMode() { return variant().mode(); }
    @Override public String kindId() { return "common"; }
    @Override public String titleKey() { return "item.librabeyblade.common_" + variant().id(); }
    @Override public String abilityKey(int id) {
        if (id == CM_BURST) return "ability.librabeyblade.common.3." + MODE_IDS[variant().mode()];
        if (id > CM_BURST) return "ability.librabeyblade.common.none";
        return "ability.librabeyblade.common." + id;
    }
    @Override public int duration(int id) { return CM_DURATION[id]; }
    @Override public int cooldown(int id) { return CM_COOLDOWN[id]; }
    @Override public int buffAbility() { return CM_GUARD; }
    @Override public int buffTicks() { return GUARD_TICKS; }
    @Override public boolean requiresTarget(int id) { return id == CM_RUSH; }
    @Override public boolean buffReflects() { return false; }
    @Override public float incomingMul() { return isShielded() ? 0.5F : 1F; }
    @Override public boolean abilityAvailable(int id) { return id <= CM_GUARD || (id == CM_BURST && getLevel() >= BURST_UNLOCK); }
    @Override protected boolean ramsInto(int id) {
        return id == CM_RUSH || (id == CM_BURST && (variant().mode() == MODE_ATTACK || variant().mode() == MODE_BALANCE));
    }
    @Override protected ItemStack defaultItem() { return new ItemStack(ModItems.COMMONS.get(getVariant()).get()); }
    @Override protected float clientSpinSpeed(int ab, float at, float speed) { return speed; }

    @Override
    protected void onBuffStart(@Nullable Player owner) {
        play(ModSounds.SHIELD_UP.get(), 1.2F, 1.3F);
    }

    @Override
    protected void tickBuff() { tickShield(); }

    @Override
    protected boolean tickAbility(int ab, int t, @Nullable Player owner) {
        switch (ab) {
            case CM_RUSH -> {
                // Power Rush: the impact that started it, then a second charge through the target
                if (t == 1) strike(2.4, 70F, 0.7, 0.15);
                if (t == 12 && target != null) {
                    Vec3 d = flat(target.position().subtract(position()));
                    if (d.lengthSqr() > 1.0E-4) setDeltaMovement(getDeltaMovement().add(d.normalize().scale(0.8)));
                }
                if (t > 12 && t < 22 && target != null && target.distanceToSqr(this) < 1.6 * 1.6) {
                    strike(2.0, 45F, 0.6, 0.2);
                    abilityTicks = 22;
                }
                return t > 12;
            }
            case CM_BURST -> {
                switch (variant().mode()) {
                    case MODE_ATTACK -> { if (t == 1) strike(2.6, 90F, 0.35, 0.8); }          // Upper Smash: launches the target
                    case MODE_DEFENSE -> { if (t == 6) strike(5.5, 55F, 0.9, 0.2); }          // Fortress Wave
                    case MODE_STAMINA -> {                                                     // Spin Recovery
                        if (t == 1) play(ModSounds.SHIELD_UP.get(), 1.0F, 1.6F);
                        if (t % 4 == 0) healSpin(getMaxSpin() * 0.013F);
                        setDeltaMovement(getDeltaMovement().multiply(0.8, 1, 0.8));
                    }
                    default -> { if (t == 1) { strike(3.2, 65F, 0.6, 0.15); healSpin(getMaxSpin() * 0.06F); } }   // Counter Burst
                }
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    /** hits every enemy within r: spin damage, knock outward and up */
    private void strike(double r, float dmg, double knock, double up) {
        play(ModSounds.CLASH_HEAVY.get(), 1.5F, 0.85F + this.random.nextFloat() * 0.2F);
        this.level().broadcastEntityEvent(this, EV_STRIKE);
        for (Entity e : enemiesWithin(r)) {
            Vec3 out = flat(e.position().subtract(position()));
            out = out.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : out.normalize();
            if (e instanceof BeybladeEntity b) {
                if (b.tryDodge(this)) continue;
                b.damageSpin(dmg * powerMul(), this);
                b.setDeltaMovement(b.getDeltaMovement().add(out.scale(knock * b.knockMul())).add(0, up, 0));
                this.level().broadcastEntityEvent(b, EV_HEAVY_HIT);
            } else if (e instanceof LivingEntity l) {
                l.hurt(damageSources().indirectMagic(this, getOwner()), dmg / 10F);
                l.knockback(knock + 0.3, -out.x, -out.z);
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setVariant(tag.getInt("Variant"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", getVariant());
    }

    public static int clampVariant(int v) { return Mth.clamp(v, 0, VARIANTS.length - 1); }
}
