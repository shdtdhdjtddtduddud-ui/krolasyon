package com.krolasyon.bosses.rpg.entity;

import com.krolasyon.bosses.rpg.def.RpgDefs;
import com.krolasyon.bosses.rpg.def.RpgDefs.Archetype;
import com.krolasyon.bosses.rpg.mob.AbilityLogic;
import com.krolasyon.bosses.rpg.mob.RpgAnimatable;
import com.krolasyon.bosses.rpg.util.Combat;
import com.krolasyon.bosses.rpg.util.FX;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.UUID;

/** Temporary ally created by spells and sword skills. */
public class Summon extends PathfinderMob implements RpgAnimatable, Combat.Allegiance {
    private static final EntityDataAccessor<Byte> KIND = SynchedEntityData.defineId(Summon.class, EntityDataSerializers.BYTE);

    public enum Kind {
        WOLF("Büyü Kurdu", Archetype.QUADRUPED, 20, 5, 0.38, RpgDefs.P_EARS | RpgDefs.P_TAIL, 0xC0C0C0),
        SPIRIT_WOLF("Ruh Kurdu", Archetype.QUADRUPED, 24, 6, 0.40, RpgDefs.P_EARS | RpgDefs.P_TAIL | RpgDefs.P_MANE, 0x60FFE0),
        SKELETON("Diriltilmiş İskelet", Archetype.HUMANOID, 20, 5, 0.28, RpgDefs.P_CROWN, 0x40FFFF),
        ANGEL("Melek Muhafız", Archetype.HUMANOID, 60, 10, 0.30, RpgDefs.P_WINGS | RpgDefs.P_CROWN, 0xFFF0A0),
        TREANT("Ağaç Adam", Archetype.TREANT, 80, 9, 0.22, RpgDefs.P_CROWN, 0x60C040),
        BLOOD_GOLEM("Kan Golemi", Archetype.GOLEM, 70, 11, 0.24, RpgDefs.P_SPIKES, 0xC01020),
        SPIRIT_WARRIOR("Ruh Savaşçısı", Archetype.HUMANOID, 30, 8, 0.32, RpgDefs.P_HOOD, 0x60FFE0),
        MIRROR("Ayna İkizi", Archetype.HUMANOID, 16, 6, 0.34, 0, 0xFFFFFF);

        public final String title; public final Archetype arch; public final float hp, atk; public final double speed;
        public final int parts, color;

        Kind(String title, Archetype arch, float hp, float atk, double speed, int parts, int color) {
            this.title = title; this.arch = arch; this.hp = hp; this.atk = atk; this.speed = speed; this.parts = parts; this.color = color;
        }

        public String texture() { return "summon_" + name().toLowerCase(); }
    }

    @Nullable private UUID owner;
    private int life = 600;
    private float power = 1;

    public Summon(EntityType<? extends Summon> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 40).add(Attributes.ATTACK_DAMAGE, 6)
                .add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FOLLOW_RANGE, 24).add(Attributes.ARMOR, 4);
    }

    public static Summon spawn(LivingEntity caster, Kind kind, double x, double y, double z, int lifeTicks, float power) {
        Summon s = new Summon(com.krolasyon.bosses.rpg.registry.RpgEntities.SUMMON.get(), caster.level());
        s.entityData.set(KIND, (byte) kind.ordinal());
        s.owner = caster instanceof Player ? caster.getUUID() : Combat.leaderOf(caster);
        s.life = lifeTicks;
        s.power = power;
        s.moveTo(x, y, z, caster.getYRot(), 0);
        s.getAttribute(Attributes.MAX_HEALTH).setBaseValue(kind.hp * power);
        s.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(kind.atk * power);
        s.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(kind.speed);
        s.setHealth(s.getMaxHealth());
        caster.level().addFreshEntity(s);
        FX.column(caster.level(), FX.dust(kind.color, 1.5F), s.position(), 2.2, 0.6, 30);
        FX.send(caster.level(), ParticleTypes.SOUL, s.position().add(0, 1, 0), 10, 0.3, 0.05);
        return s;
    }

    public Kind kind() { return Kind.values()[Math.floorMod(this.entityData.get(KIND), Kind.values().length)]; }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(KIND, (byte) 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25, true));
        this.goalSelector.addGoal(4, new FollowLeaderGoal(this));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new DefendLeaderGoal(this));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Mob.class, 5, true, false, e -> e instanceof Enemy && !(e instanceof Combat.Allegiance)));
    }

    @Nullable
    public LivingEntity leaderEntity() {
        if (owner == null || !(this.level() instanceof ServerLevel sl)) return null;
        Entity e = sl.getEntity(owner);
        return e instanceof LivingEntity le ? le : null;
    }

    @Nullable @Override
    public UUID leader() { return owner; }

    @Override
    public boolean hostileTo(LivingEntity other) { return other == this.getTarget(); }

    @Override
    public boolean isAlliedTo(Entity e) {
        if (owner != null && (owner.equals(e.getUUID()) || owner.equals(Combat.leaderOf(e)))) return true;
        return super.isAlliedTo(e);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            if (this.tickCount % 3 == 0) {
                Kind k = kind();
                this.level().addParticle(FX.dust(k.color, 0.8F), getRandomX(0.6), getRandomY(), getRandomZ(0.6), 0, 0.02, 0);
            }
            return;
        }
        if (--life <= 0 || owner != null && this.tickCount > 40 && leaderEntity() == null && this.tickCount % 20 == 0 && !(this.level().getPlayerByUUID(owner) != null)) {
            FX.send(level(), ParticleTypes.POOF, position().add(0, 0.8, 0), 20, 0.4, 0.05);
            FX.send(level(), FX.dust(kind().color, 1.5F), position().add(0, 0.8, 0), 20, 0.4, 0.05);
            this.discard();
        }
    }

    @Override
    public boolean doHurtTarget(Entity e) {
        boolean r = super.doHurtTarget(e);
        if (r && e instanceof LivingEntity le) {
            switch (kind()) {
                case SPIRIT_WOLF, SPIRIT_WARRIOR -> Combat.effect(le, net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 40, 1);
                case ANGEL -> Combat.applyOnHit(this, le, MagicBolt.OnHit.HOLY, 4);
                case BLOOD_GOLEM -> this.heal(2);
                case TREANT -> Combat.applyOnHit(this, le, MagicBolt.OnHit.ROOT, 0);
                default -> {}
            }
        }
        return r;
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (src.getEntity() != null && owner != null && owner.equals(src.getEntity().getUUID())) return false;
        return super.hurt(src, amount);
    }

    @Override
    protected boolean shouldDespawnInPeaceful() { return false; }

    @Override
    public boolean removeWhenFarAway(double d) { return false; }

    @Override
    public Component getTypeName() { return Component.literal(kind().title); }

    @Override
    protected SoundEvent getHurtSound(DamageSource src) {
        return switch (kind().arch) { case QUADRUPED -> SoundEvents.WOLF_HURT; case GOLEM -> SoundEvents.IRON_GOLEM_HURT; case TREANT -> SoundEvents.WOOD_HIT; default -> SoundEvents.ALLAY_HURT; };
    }

    @Override
    protected SoundEvent getDeathSound() { return SoundEvents.ALLAY_DEATH; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("Kind", this.entityData.get(KIND));
        tag.putInt("Life", life);
        if (owner != null) tag.putUUID("Owner", owner);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(KIND, tag.getByte("Kind"));
        life = tag.getInt("Life");
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }

    // ------------------------------------------------------------------ animation
    @Override public Archetype archetype() { return kind().arch; }
    @Override public int bodyParts() { return kind().parts; }
    @Nullable @Override public AbilityLogic.Style castStyle() { return null; }
    @Override public float castAge(float pt) { return -1; }
    @Override public int castWindup() { return 10; }
    @Override public boolean airborne() { return false; }

    // ------------------------------------------------------------------ goals
    static class FollowLeaderGoal extends Goal {
        private final Summon mob;

        FollowLeaderGoal(Summon mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity l = mob.leaderEntity();
            return l != null && mob.getTarget() == null && mob.distanceToSqr(l) > 36;
        }

        @Override
        public void tick() {
            LivingEntity l = mob.leaderEntity();
            if (l == null) return;
            if (mob.distanceToSqr(l) > 24 * 24) mob.teleportTo(l.getX(), l.getY(), l.getZ());
            else mob.getNavigation().moveTo(l, 1.2);
        }
    }

    static class DefendLeaderGoal extends TargetGoal {
        private final Summon mob;
        @Nullable private LivingEntity found;

        DefendLeaderGoal(Summon mob) {
            super(mob, false);
            this.mob = mob;
        }

        @Override
        public boolean canUse() {
            LivingEntity l = mob.leaderEntity();
            if (l == null) return false;
            LivingEntity t = l.getLastHurtByMob();
            if (t == null || !t.isAlive() || t == mob) t = l.getLastHurtMob();
            if (t == null || !t.isAlive() || Combat.sameSide(mob, t)) return false;
            found = t;
            return true;
        }

        @Override
        public void start() {
            mob.setTarget(found);
            super.start();
        }
    }
}
