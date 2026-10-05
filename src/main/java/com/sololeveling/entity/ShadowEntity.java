package com.sololeveling.entity;

import com.sololeveling.gen.Content;
import com.sololeveling.player.ModCaps;
import com.sololeveling.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

/** A summoned Shadow Soldier that fights for its owner. */
public class ShadowEntity extends PathfinderMob implements SLCaster {
    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(ShadowEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> CAST = SynchedEntityData.defineId(ShadowEntity.class, EntityDataSerializers.INT);

    public final Content.ShadowDef def;
    private int rankIdx = 0;
    private int castCd = 0;
    private int rescaleCd = 0;
    public boolean empowered = false;

    public ShadowEntity(EntityType<? extends ShadowEntity> type, Level level, Content.ShadowDef def) {
        super(type, level);
        this.def = def;
        this.xpReward = 0;
        initGoals();
    }

    public static AttributeSupplier.Builder attributes(Content.ShadowDef def) {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, def.hp())
                .add(Attributes.ATTACK_DAMAGE, def.dmg())
                .add(Attributes.MOVEMENT_SPEED, def.speed())
                .add(Attributes.FOLLOW_RANGE, 32)
                .add(Attributes.ATTACK_KNOCKBACK, 0.3)
                .add(Attributes.ARMOR, 4);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER, Optional.empty());
        this.entityData.define(CAST, 0);
    }

    public void setOwner(Player p) { this.entityData.set(OWNER, Optional.of(p.getUUID())); }
    public UUID ownerId() { return this.entityData.get(OWNER).orElse(null); }
    public boolean isOwnedBy(Entity e) { return e != null && e.getUUID().equals(ownerId()); }
    public Player owner() { UUID id = ownerId(); return id == null ? null : level().getPlayerByUUID(id); }
    public void setRankIdx(int i) { this.rankIdx = i; rescale(true); }
    public int rankIdx() { return rankIdx; }
    @Override public boolean isCasting() { return this.entityData.get(CAST) > 0; }

    @Override
    protected void registerGoals() { }

    private void initGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        if (def.ranged()) this.goalSelector.addGoal(1, new CastGoal());
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25D, true));
        this.goalSelector.addGoal(4, new FollowOwnerGoal());
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new OwnerTargetGoal(true));
        this.targetSelector.addGoal(2, new OwnerTargetGoal(false));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Mob.class, 10, true, false,
                e -> e instanceof Enemy && !(e instanceof ShadowEntity) && nearOwner(e, 22)));
    }

    private boolean nearOwner(Entity e, double r) {
        Player o = owner();
        return o == null || o.distanceToSqr(e) < r * r;
    }

    @Override
    public boolean canAttack(LivingEntity t) {
        if (t instanceof Player || t instanceof ShadowEntity || t instanceof NpcEntity) return false;
        return super.canAttack(t);
    }

    @Override
    public boolean hurt(DamageSource src, float amt) {
        Entity e = src.getEntity();
        if (e != null && (isOwnedBy(e) || e instanceof ShadowEntity)) return false;
        return super.hurt(src, amt);
    }

    @Override public boolean requiresCustomPersistence() { return true; }
    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override public boolean isPersistenceRequired() { return true; }

    /** scale stats with the owner's level and the extracted rank */
    public void rescale(boolean heal) {
        Player o = owner();
        int lvl = o == null ? 1 : ModCaps.get(o).level;
        double rm = 1.0 + 0.45 * rankIdx;
        double hp = def.hp() * (1 + lvl / 15.0) * rm * (empowered ? 1.3 : 1.0);
        double dm = def.dmg() * (1 + lvl / 14.0) * rm * (empowered ? 1.3 : 1.0);
        float ratio = getHealth() / Math.max(1, getMaxHealth());
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(dm);
        getAttribute(Attributes.ARMOR).setBaseValue(4 + rankIdx * 2 + lvl / 10.0);
        setHealth(heal ? (float) hp : Math.min((float) hp, (float) (ratio * hp)));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            if (tickCount % 3 == 0)
                level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, getX() + (random.nextDouble() - 0.5) * getBbWidth(), getY() + random.nextDouble() * getBbHeight(), getZ() + (random.nextDouble() - 0.5) * getBbWidth(), 0, 0.03, 0);
            return;
        }
        int c = this.entityData.get(CAST);
        if (c > 0) this.entityData.set(CAST, c - 1);
        if (castCd > 0) castCd--;
        if (tickCount % 20 == 0 && getHealth() < getMaxHealth()) heal(1.0F + rankIdx * 0.5F);
        if (--rescaleCd <= 0) { rescaleCd = 200; rescale(false); }
    }

    @Override
    public void die(DamageSource src) {
        super.die(src);
        Player o = owner();
        if (o instanceof net.minecraft.server.level.ServerPlayer sp)
            sp.sendSystemMessage(Component.translatable("gui.sololeveling.shadow_fallen", getDisplayName()));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        UUID o = ownerId();
        if (o != null) t.putUUID("owner", o);
        t.putInt("rank", rankIdx);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        if (t.hasUUID("owner")) this.entityData.set(OWNER, Optional.of(t.getUUID("owner")));
        rankIdx = t.getInt("rank");
    }

    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource s) { return SoundEvents.WITHER_SKELETON_HURT; }
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound() { return SoundEvents.WITHER_SKELETON_DEATH; }
    @Override protected void playStepSound(BlockPos pos, net.minecraft.world.level.block.state.BlockState st) { playSound(SoundEvents.WITHER_SKELETON_STEP, 0.3F, 0.7F); }

    // ------------------------------------------------------------------ goals
    private class FollowOwnerGoal extends Goal {
        FollowOwnerGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }

        @Override public boolean canUse() {
            Player o = owner();
            return o != null && !o.isSpectator() && distanceToSqr(o) > 6 * 6;
        }

        @Override public boolean canContinueToUse() {
            Player o = owner();
            return o != null && !getNavigation().isDone() && distanceToSqr(o) > 3 * 3;
        }

        @Override public void tick() {
            Player o = owner();
            if (o == null) return;
            getLookControl().setLookAt(o, 10, getMaxHeadXRot());
            if (tickCount % 10 == 0) {
                if (distanceToSqr(o) > 26 * 26) {
                    Vec3 p = o.position().add((random.nextDouble() - 0.5) * 4, 0, (random.nextDouble() - 0.5) * 4);
                    teleportTo(p.x, p.y, p.z);
                    getNavigation().stop();
                } else {
                    getNavigation().moveTo(o, 1.3D);
                }
            }
        }
    }

    private class OwnerTargetGoal extends TargetGoal {
        private final boolean defend;
        private LivingEntity pick;

        OwnerTargetGoal(boolean defend) {
            super(ShadowEntity.this, false);
            this.defend = defend;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override public boolean canUse() {
            Player o = owner();
            if (o == null) return false;
            pick = defend ? o.getLastHurtByMob() : o.getLastHurtMob();
            if (pick == null || !pick.isAlive() || pick instanceof ShadowEntity || pick instanceof NpcEntity || pick == o) return false;
            return ShadowEntity.this.canAttack(pick);
        }

        @Override public void start() {
            mob.setTarget(pick);
            super.start();
        }
    }

    private class CastGoal extends Goal {
        CastGoal() { setFlags(EnumSet.of(Flag.LOOK)); }

        @Override public boolean canUse() {
            LivingEntity t = getTarget();
            return t != null && t.isAlive() && castCd <= 0 && distanceTo(t) < 18 && hasLineOfSight(t);
        }

        @Override public void start() {
            LivingEntity t = getTarget();
            if (t == null) return;
            castCd = 30;
            entityData.set(CAST, 12);
            swing(InteractionHand.MAIN_HAND);
            MagicBoltEntity b = new MagicBoltEntity(ModEntities.MAGIC_BOLT.get(), level());
            b.setOwner(ShadowEntity.this);
            b.setup(isOwnedByBlue() ? "ice" : "shadow", (float) getAttributeValue(Attributes.ATTACK_DAMAGE));
            b.setPos(getX(), getY() + getBbHeight() * 0.7, getZ());
            Vec3 d = new Vec3(t.getX() - getX(), t.getEyeY() - b.getY(), t.getZ() - getZ()).normalize();
            b.shoot(d.x, d.y, d.z, 1.6F, 0.4F);
            level().addFreshEntity(b);
        }

        private boolean isOwnedByBlue() { return def.id().equals("shadow_mage"); }
    }
}
