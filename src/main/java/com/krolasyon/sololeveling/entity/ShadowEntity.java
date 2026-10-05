package com.krolasyon.sololeveling.entity;

import com.krolasyon.sololeveling.registry.ModEntities;
import com.krolasyon.sololeveling.shadow.ShadowManager;
import com.krolasyon.sololeveling.system.HunterCapability;
import com.krolasyon.sololeveling.system.HunterData;
import com.krolasyon.sololeveling.system.Stat;
import com.krolasyon.sololeveling.system.Title;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.UUID;

/** A shadow soldier extracted with "Arise". Renders as a dark, purple-eyed copy of the monster it came from. */
public class ShadowEntity extends PathfinderMob implements OwnableEntity {
    private static final EntityDataAccessor<String> SOURCE = SynchedEntityData.defineId(ShadowEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> LEVEL = SynchedEntityData.defineId(ShadowEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> NAMED = SynchedEntityData.defineId(ShadowEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> RISE = SynchedEntityData.defineId(ShadowEntity.class, EntityDataSerializers.INT);

    @Nullable
    private UUID owner;
    public UUID recordId = UUID.randomUUID();
    private EntityDimensions sourceDims;

    public ShadowEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 40).add(Attributes.ATTACK_DAMAGE, 6)
                .add(Attributes.MOVEMENT_SPEED, 0.33).add(Attributes.FOLLOW_RANGE, 40).add(Attributes.ARMOR, 4)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(SOURCE, "minecraft:zombie");
        entityData.define(LEVEL, 1);
        entityData.define(NAMED, false);
        entityData.define(RISE, 30);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (SOURCE.equals(key)) {
            sourceDims = null;
            refreshDimensions();
        }
    }

    public String sourceId() { return entityData.get(SOURCE); }

    @Nullable
    public EntityType<?> sourceType() {
        ResourceLocation rl = ResourceLocation.tryParse(sourceId());
        return rl == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(rl).orElse(null);
    }

    public int shadowLevel() { return entityData.get(LEVEL); }

    public boolean isNamed() { return entityData.get(NAMED); }

    /** Remaining "rising from the ground" animation ticks. */
    public int riseTicks() { return entityData.get(RISE); }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        if (sourceDims == null) {
            EntityType<?> t = sourceType();
            sourceDims = t == null ? super.getDimensions(pose) : t.getDimensions();
        }
        return sourceDims;
    }

    /** Sets everything from the stored record and the owner's stats. */
    public void setup(ServerPlayer owner, HunterData.ShadowRecord rec) {
        this.owner = owner.getUUID();
        this.recordId = rec.id;
        entityData.set(SOURCE, rec.type);
        entityData.set(LEVEL, rec.level);
        entityData.set(NAMED, rec.named);
        sourceDims = null;
        refreshDimensions();
        setCustomName(Component.literal(rec.name));
        setCustomNameVisible(rec.named);
        applyStats(owner, rec);
        setHealth(getMaxHealth());
    }

    @SuppressWarnings("unchecked")
    private void applyStats(ServerPlayer owner, HunterData.ShadowRecord rec) {
        double hp = 30, dmg = 5, armor = 4, speed = 0.32;
        EntityType<?> t = sourceType();
        MobKind k = t == null ? null : ModEntities.kindOrNull(t);
        if (k != null) {
            hp = k.cappedHp();
            dmg = k.damage;
            armor = k.armor;
            speed = Math.max(0.28, k.speed);
        } else if (t != null && DefaultAttributes.hasSupplier(t)) {
            AttributeSupplier s = DefaultAttributes.getSupplier((EntityType<? extends LivingEntity>) t);
            if (s.hasAttribute(Attributes.MAX_HEALTH)) hp = s.getBaseValue(Attributes.MAX_HEALTH);
            if (s.hasAttribute(Attributes.ATTACK_DAMAGE)) dmg = Math.max(3, s.getBaseValue(Attributes.ATTACK_DAMAGE));
            if (s.hasAttribute(Attributes.ARMOR)) armor = s.getBaseValue(Attributes.ARMOR);
            if (s.hasAttribute(Attributes.MOVEMENT_SPEED)) speed = Math.max(0.28, Math.min(0.42, s.getBaseValue(Attributes.MOVEMENT_SPEED) * 1.3));
        }
        HunterData d = HunterCapability.get(owner);
        double mult = (1 + 0.06 * rec.level) * (rec.named ? 1.6 : 1.0) * (1 + d.stat(Stat.INT) * 0.004);
        if (d.title == Title.SHADOW_MONARCH) mult *= 1.2;
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(Math.min(1000, hp * mult));
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(dmg * mult);
        getAttribute(Attributes.ARMOR).setBaseValue(Math.min(30, armor + rec.level * 0.3));
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(speed);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25, true));
        goalSelector.addGoal(3, new FollowOwnerGoal());
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new DefendOwnerGoal());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Mob.class, 5, true, false,
                e -> e instanceof Enemy && !(e instanceof ShadowEntity) && getOwner() != null && e.distanceToSqr(getOwner()) < 20 * 20));
    }

    @Override
    public void tick() {
        super.tick();
        int rise = riseTicks();
        if (rise > 0 && !level().isClientSide) entityData.set(RISE, rise - 1);
        if (level().isClientSide) {
            if (random.nextInt(3) == 0) {
                double w = getBbWidth();
                level().addParticle(ParticleTypes.SMOKE, getX() + (random.nextDouble() - 0.5) * w, getY() + random.nextDouble() * getBbHeight() * 0.4,
                        getZ() + (random.nextDouble() - 0.5) * w, 0, 0.02, 0);
            }
            if (rise > 0) {
                for (int i = 0; i < 4; i++)
                    level().addParticle(ParticleTypes.SQUID_INK, getX() + (random.nextDouble() - 0.5) * getBbWidth() * 2, getY() + 0.1,
                            getZ() + (random.nextDouble() - 0.5) * getBbWidth() * 2, 0, 0.05, 0);
            }
            return;
        }
        LivingEntity o = getOwner();
        if (o == null || !o.isAlive() || o.level() != level()) {
            if (tickCount > 40) {
                ShadowManager.onShadowLost(this);
                discard();
            }
            return;
        }
        if (distanceToSqr(o) > 40 * 40) teleportNear(o);
        if (tickCount % 40 == 0 && getHealth() < getMaxHealth()) heal(1 + shadowLevel() * 0.1F);
        HunterData d = HunterCapability.get((Player) o);
        if (d.domainTicks > 0 && tickCount % 20 == 0) heal(getMaxHealth() * 0.05F);
    }

    public void teleportNear(LivingEntity o) {
        Vec3 p = o.position().add((random.nextDouble() - 0.5) * 4, 0, (random.nextDouble() - 0.5) * 4);
        teleportTo(p.x, o.getY(), p.z);
        getNavigation().stop();
        if (level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 1, getZ(), 20, 0.4, 0.8, 0.4, 0.05);
    }

    @Override
    public boolean doHurtTarget(Entity e) {
        float bonus = 1F;
        if (getOwner() instanceof Player p) {
            HunterData d = HunterCapability.get(p);
            if (d.domainTicks > 0) bonus = 1.5F;
        }
        if (bonus > 1F) getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue() * bonus);
        boolean ok = super.doHurtTarget(e);
        if (bonus > 1F) getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue() / bonus);
        if (ok && e instanceof LivingEntity le && !le.isAlive() && getOwner() instanceof ServerPlayer sp) ShadowManager.onShadowKill(this, sp, le);
        return ok;
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        Entity a = src.getEntity();
        if (a != null && (a == getOwner() || a instanceof ShadowEntity)) return false;
        return super.hurt(src, amount);
    }

    @Override
    public void die(DamageSource src) {
        super.die(src);
        if (!level().isClientSide) ShadowManager.onShadowLost(this);
    }

    @Override
    public boolean isAlliedTo(Entity e) {
        if (e == getOwner() || e instanceof ShadowEntity) return true;
        return super.isAlliedTo(e);
    }

    @Override
    public boolean removeWhenFarAway(double d) { return false; }

    @Override
    protected boolean shouldDropLoot() { return false; }

    @Override
    public boolean shouldDropExperience() { return false; }

    @Nullable
    @Override
    public UUID getOwnerUUID() { return owner; }

    @Nullable
    @Override
    public LivingEntity getOwner() {
        if (owner == null) return null;
        Player p = level().getPlayerByUUID(owner);
        return p;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        if (owner != null) t.putUUID("Owner", owner);
        t.putUUID("Record", recordId);
        t.putString("Source", sourceId());
        t.putInt("ShadowLevel", shadowLevel());
        t.putBoolean("Named", isNamed());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        if (t.hasUUID("Owner")) owner = t.getUUID("Owner");
        if (t.hasUUID("Record")) recordId = t.getUUID("Record");
        entityData.set(SOURCE, t.getString("Source"));
        entityData.set(LEVEL, Math.max(1, t.getInt("ShadowLevel")));
        entityData.set(NAMED, t.getBoolean("Named"));
        entityData.set(RISE, 0);
    }

    class FollowOwnerGoal extends Goal {
        FollowOwnerGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }

        @Override
        public boolean canUse() {
            LivingEntity o = getOwner();
            return o != null && getTarget() == null && distanceToSqr(o) > 36;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity o = getOwner();
            return o != null && getTarget() == null && distanceToSqr(o) > 9;
        }

        @Override
        public void tick() {
            LivingEntity o = getOwner();
            if (o == null) return;
            getLookControl().setLookAt(o, 10, getMaxHeadXRot());
            if (tickCount % 10 == 0) getNavigation().moveTo(o, distanceToSqr(o) > 144 ? 1.5 : 1.1);
        }

        @Override
        public void stop() { getNavigation().stop(); }
    }

    /** Attacks whatever hurts the owner or whatever the owner attacks. */
    class DefendOwnerGoal extends TargetGoal {
        private LivingEntity pick;
        private int lastHurtBy, lastHurt;

        DefendOwnerGoal() {
            super(ShadowEntity.this, false);
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            LivingEntity o = getOwner();
            if (o == null) return false;
            LivingEntity a = o.getLastHurtByMob();
            if (a != null && o.getLastHurtByMobTimestamp() != lastHurtBy && valid(a)) {
                pick = a;
                lastHurtBy = o.getLastHurtByMobTimestamp();
                return true;
            }
            LivingEntity b = o.getLastHurtMob();
            if (b != null && o.getLastHurtMobTimestamp() != lastHurt && valid(b)) {
                pick = b;
                lastHurt = o.getLastHurtMobTimestamp();
                return true;
            }
            return false;
        }

        private boolean valid(LivingEntity e) {
            return e.isAlive() && !(e instanceof ShadowEntity) && e != getOwner() && !(e instanceof Player);
        }

        @Override
        public void start() {
            mob.setTarget(pick);
            super.start();
        }
    }
}
