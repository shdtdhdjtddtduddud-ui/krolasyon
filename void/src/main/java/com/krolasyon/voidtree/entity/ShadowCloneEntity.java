package com.krolasyon.voidtree.entity;

import com.krolasyon.voidtree.Fx;
import com.krolasyon.voidtree.ModRegistry;
import com.krolasyon.voidtree.SkillLogic;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

/** "Gölge Klonları": shadow copies of the caster that hunt monsters for 20s and burst when they fade. */
public class ShadowCloneEntity extends PathfinderMob {
    public static final int LIFE = 400;
    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(ShadowCloneEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    public ShadowCloneEntity(EntityType<? extends ShadowCloneEntity> type, Level level) {
        super(type, level);
        xpReward = 0;
        setPersistenceRequired();
    }

    public ShadowCloneEntity(Level level, Player owner) {
        this(ModRegistry.SHADOW_CLONE.get(), level);
        entityData.set(OWNER, Optional.of(owner.getUUID()));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 24).add(Attributes.ATTACK_DAMAGE, 6)
                .add(Attributes.MOVEMENT_SPEED, 0.36).add(Attributes.FOLLOW_RANGE, 24).add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(OWNER, Optional.empty());
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.3, true));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Mob.class, 5, true, false, e -> e instanceof Enemy && !(e instanceof ShadowCloneEntity)));
    }

    public Optional<UUID> ownerId() { return entityData.get(OWNER); }

    public Player owner() { return ownerId().map(id -> level().getPlayerByUUID(id)).orElse(null); }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        Player owner = owner();
        if (owner == null || tickCount > LIFE) { fade(); return; }
        // assist: take the owner's fight
        LivingEntity want = owner.getLastHurtMob() != null && owner.getLastHurtMob().isAlive() && !(owner.getLastHurtMob() instanceof ShadowCloneEntity)
                ? owner.getLastHurtMob() : owner.getLastHurtByMob();
        if (want != null && want.isAlive() && want != owner && !(want instanceof ShadowCloneEntity) && want.distanceToSqr(this) < 24 * 24) setTarget(want);
        if (getTarget() == null || !getTarget().isAlive()) {
            double d = distanceToSqr(owner);
            if (d > 144) {
                Vec3 at = owner.position().add((random.nextDouble() - 0.5) * 3, 0, (random.nextDouble() - 0.5) * 3);
                teleportTo(at.x, at.y, at.z);
            } else if (d > 16) {
                getNavigation().moveTo(owner, 1.2);
            }
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        Player owner = owner();
        if (target instanceof LivingEntity le && owner instanceof ServerPlayer sp) {
            swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            SkillLogic.hit(le, (float) getAttributeValue(Attributes.ATTACK_DAMAGE), this, sp);
            SkillLogic.wisps((ServerLevel) level(), le.position().add(0, le.getBbHeight() / 2, 0), 5, 0.25);
            return true;
        }
        return super.doHurtTarget(target);
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        if (other instanceof ShadowCloneEntity) return true;
        Player owner = owner();
        return other == owner || (owner != null && owner.isAlliedTo(other)) || super.isAlliedTo(other);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        Player owner = owner();
        if (source.getEntity() != null && (source.getEntity() == owner || source.getEntity() instanceof ShadowCloneEntity)) return false;
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!level().isClientSide) burst();
    }

    private void fade() {
        burst();
        discard();
    }

    private void burst() {
        ServerLevel sl = (ServerLevel) level();
        Vec3 c = position().add(0, 1, 0);
        Fx.at(sl, Fx.Type.WISPBURST, c, 14, 3F);
        Fx.at(sl, Fx.Type.RIFT, c, 14, 0.8F);
        SkillLogic.wisps(sl, c, 25, 0.5);
        SkillLogic.play(sl, c, ModRegistry.EVADE.get(), 1F, 0.7F);
        if (owner() instanceof ServerPlayer sp) {
            for (LivingEntity e : SkillLogic.enemiesAround(sp, c, 3, true)) SkillLogic.hit(e, 6F, this, sp);
        }
    }

    @Override
    protected boolean shouldDropLoot() { return false; }

    @Override
    public boolean removeWhenFarAway(double d) { return false; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        ownerId().ifPresent(id -> tag.putUUID("Owner", id));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) entityData.set(OWNER, Optional.of(tag.getUUID("Owner")));
    }

    public static void discardOwnedBy(ServerLevel l, Player p) {
        for (ShadowCloneEntity c : l.getEntitiesOfClass(ShadowCloneEntity.class, new AABB(p.blockPosition()).inflate(64),
                c -> c.ownerId().map(id -> id.equals(p.getUUID())).orElse(false))) c.fade();
    }
}
