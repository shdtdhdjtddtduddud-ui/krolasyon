package com.sololeveling.entity;

import com.sololeveling.gen.Content;
import com.sololeveling.registry.ModEntities;
import com.sololeveling.system.Scheduler;
import com.sololeveling.world.DungeonManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Data driven Solo Leveling monster / boss. */
public class SLMonster extends Monster implements SLCaster {
    private static final EntityDataAccessor<Integer> CAST = SynchedEntityData.defineId(SLMonster.class, EntityDataSerializers.INT);

    public final Content.MonsterDef def;
    private ServerBossEvent bossBar;
    private int abilityCd = 60;
    private int abilityIdx = 0;
    private boolean enraged = false;
    private boolean minion = false;
    public long dungeonId = -1;
    private int chargeTicks = 0;
    private Vec3 chargeDir = Vec3.ZERO;

    public SLMonster(EntityType<? extends SLMonster> type, Level level, Content.MonsterDef def) {
        super(type, level);
        this.def = def;
        this.xpReward = 0;
        if (def.boss()) {
            bossBar = new ServerBossEvent(Component.translatable("entity.sololeveling." + def.id()),
                    BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
            bossBar.setDarkenScreen(false);
        }
    }

    public static AttributeSupplier.Builder attributes(Content.MonsterDef def) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, def.hp())
                .add(Attributes.ATTACK_DAMAGE, def.dmg())
                .add(Attributes.MOVEMENT_SPEED, def.speed())
                .add(Attributes.ARMOR, def.armor())
                .add(Attributes.FOLLOW_RANGE, def.boss() ? 48 : 28)
                .add(Attributes.ATTACK_KNOCKBACK, def.boss() ? 1.2 : 0.4)
                .add(Attributes.KNOCKBACK_RESISTANCE, def.boss() ? 0.85 : Math.min(0.5, def.scale() * 0.15));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(CAST, 0);
    }

    @Override public boolean isCasting() { return this.entityData.get(CAST) > 0; }

    public void markMinion() { this.minion = true; }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15D, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, SLMonster.class));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, ShadowEntity.class, false));
    }

    @Override public boolean requiresCustomPersistence() { return def.boss() || dungeonId >= 0; }
    @Override public boolean removeWhenFarAway(double d) { return !def.boss() && dungeonId < 0 && !isPersistenceRequired(); }
    @Override public boolean canBeLeashed(Player p) { return false; }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean ok = super.doHurtTarget(target);
        if (ok && target instanceof LivingEntity le) {
            switch (def.id()) {
                case "venom_ant", "kasaka" -> le.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1));
                case "ice_elf", "baruka" -> le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                case "hell_hound", "cerberus", "kamish" -> le.setSecondsOnFire(4);
                case "demon_knight", "igris" -> le.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
                default -> { }
            }
        }
        return ok;
    }

    @Override
    public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p); if (bossBar != null) bossBar.addPlayer(p); }

    @Override
    public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); if (bossBar != null) bossBar.removePlayer(p); }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            if (isCasting() && random.nextInt(2) == 0)
                level().addParticle(def.id().contains("ice") || def.id().equals("baruka") ? ParticleTypes.SNOWFLAKE : ParticleTypes.ENCHANT,
                        getX() + (random.nextDouble() - 0.5) * getBbWidth(), getY() + random.nextDouble() * getBbHeight(), getZ() + (random.nextDouble() - 0.5) * getBbWidth(), 0, 0.05, 0);
            return;
        }
        if (bossBar != null) bossBar.setProgress(getHealth() / getMaxHealth());
        int c = this.entityData.get(CAST);
        if (c > 0) this.entityData.set(CAST, c - 1);
        if (chargeTicks > 0) {
            chargeTicks--;
            setDeltaMovement(chargeDir.x, getDeltaMovement().y, chargeDir.z);
            hurtMarked = true;
            for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.6), t -> t != this && canAttackTarget(t))) {
                e.hurt(damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE));
                e.knockback(1.2, -chargeDir.x, -chargeDir.z);
            }
        }
        if (def.boss() && !enraged && getHealth() < getMaxHealth() * 0.5F) {
            enraged = true;
            addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 600, 1));
            addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 600, 0));
            level().playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 2.0F, 0.8F);
            if (bossBar != null) bossBar.setColor(BossEvent.BossBarColor.RED);
            for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(40)))
                p.sendSystemMessage(Component.translatable("gui.sololeveling.boss_enraged", getDisplayName()));
        }
        LivingEntity t = getTarget();
        if (t != null && def.abilities().length > 0 && t.isAlive()) {
            if (--abilityCd <= 0) {
                String ab = def.abilities()[abilityIdx++ % def.abilities().length];
                if (useAbility(ab, t)) abilityCd = (def.boss() ? 70 : 100) - (enraged ? 25 : 0) + random.nextInt(30);
                else abilityCd = 10;
            }
        }
    }

    private boolean canAttackTarget(LivingEntity e) {
        return e instanceof Player || e instanceof ShadowEntity;
    }

    private void cast(int ticks) {
        this.entityData.set(CAST, ticks);
        swing(InteractionHand.MAIN_HAND);
    }

    private List<LivingEntity> enemiesAround(double r) {
        return level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(r, 2, r), e -> e != this && canAttackTarget(e) && e.isAlive());
    }

    private float dmg(float mul) { return (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * mul; }

    private boolean useAbility(String id, LivingEntity t) {
        double dist = distanceTo(t);
        ServerLevel sl = (ServerLevel) level();
        switch (id) {
            case "slam" -> {
                if (dist > 9) return false;
                cast(24);
                getNavigation().stop();
                sl.playSound(null, blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.4F, 0.9F);
                float r = 5.0F * Math.max(1, def.scale() * 0.8F);
                Scheduler.later(18, () -> {
                    if (!isAlive()) return;
                    sl.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.2, getZ(), 6, r * 0.3, 0.1, r * 0.3, 0);
                    sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX(), getY() + 0.2, getZ(), 30, r * 0.4, 0.1, r * 0.4, 0.02);
                    sl.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.5F, 0.7F);
                    for (LivingEntity e : enemiesAround(r)) {
                        e.hurt(damageSources().mobAttack(this), dmg(1.3F));
                        Vec3 away = e.position().subtract(position()).normalize();
                        e.setDeltaMovement(away.x * 1.0, 0.7, away.z * 1.0);
                        e.hurtMarked = true;
                    }
                });
            }
            case "charge" -> {
                if (dist < 5 || dist > 22) return false;
                cast(20);
                sl.playSound(null, blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.2F, 1.2F);
                Vec3 d = t.position().subtract(position()).multiply(1, 0, 1).normalize();
                Scheduler.later(14, () -> {
                    if (!isAlive()) return;
                    chargeDir = d.scale(1.25);
                    chargeTicks = 14;
                });
            }
            case "leap", "lunge" -> {
                if (dist < 3 || dist > 16) return false;
                cast(14);
                Vec3 d = t.position().subtract(position());
                Vec3 h = new Vec3(d.x, 0, d.z).normalize().scale(Math.min(1.5, 0.35 + dist * 0.11));
                setDeltaMovement(h.x, 0.6, h.z);
                hurtMarked = true;
                Scheduler.later(14, () -> {
                    if (!isAlive()) return;
                    for (LivingEntity e : enemiesAround(2.6)) e.hurt(damageSources().mobAttack(this), dmg(1.2F));
                    sl.sendParticles(ParticleTypes.POOF, getX(), getY(), getZ(), 16, 0.8, 0.1, 0.8, 0.05);
                });
            }
            case "sword_beam" -> {
                if (dist > 26) return false;
                cast(26);
                sl.playSound(null, blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.6F, 0.6F);
                int n = enraged ? 5 : 3;
                Scheduler.later(14, () -> {
                    if (!isAlive() || !t.isAlive()) return;
                    for (int i = 0; i < n; i++) fireBolt(t, "beam", dmg(0.9F), (i - (n - 1) / 2.0) * 0.12);
                });
            }
            case "frost_bolt" -> {
                if (dist > 24) return false;
                cast(14);
                Scheduler.later(8, () -> { if (isAlive() && t.isAlive()) fireBolt(t, "ice", dmg(0.8F), 0); });
            }
            case "spit" -> {
                if (dist > 20 || dist < 3) return false;
                cast(12);
                Scheduler.later(6, () -> { if (isAlive() && t.isAlive()) { fireBolt(t, "poison", dmg(0.6F), 0); fireBolt(t, "poison", dmg(0.6F), 0.1); } });
            }
            case "ice_nova" -> {
                cast(30);
                sl.playSound(null, blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2.0F, 0.6F);
                Scheduler.later(24, () -> {
                    if (!isAlive()) return;
                    sl.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 1, getZ(), 120, 3.5, 0.8, 3.5, 0.15);
                    for (LivingEntity e : enemiesAround(7)) {
                        e.hurt(damageSources().freeze(), dmg(1.1F));
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3));
                        e.setTicksFrozen(200);
                    }
                });
            }
            case "fire_breath" -> {
                if (dist > 14) return false;
                cast(46);
                sl.playSound(null, blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 2F, 0.5F);
                for (int i = 0; i < 20; i++) {
                    final int k = i;
                    Scheduler.later(8 + i * 2, () -> {
                        if (!isAlive()) return;
                        Vec3 look = getLookAngle();
                        Vec3 eye = position().add(0, getBbHeight() * 0.75, 0);
                        for (int s = 1; s <= 8; s++) {
                            Vec3 p = eye.add(look.scale(s * 1.3));
                            sl.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 2, 0.3 + s * 0.08, 0.3 + s * 0.08, 0.3 + s * 0.08, 0.01);
                        }
                        if (k % 3 == 0) {
                            AABB cone = new AABB(eye, eye.add(look.scale(11))).inflate(2.2);
                            for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, cone, x -> x != this && canAttackTarget(x))) {
                                e.hurt(damageSources().onFire(), dmg(0.5F));
                                e.setSecondsOnFire(4);
                            }
                        }
                    });
                }
            }
            case "poison_cloud" -> {
                cast(20);
                AreaEffectCloud cloud = new AreaEffectCloud(level(), t.getX(), t.getY(), t.getZ());
                cloud.setOwner(this);
                cloud.setRadius(4.0F);
                cloud.setDuration(160);
                cloud.setParticle(ParticleTypes.ITEM_SLIME);
                cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
                level().addFreshEntity(cloud);
            }
            case "howl" -> {
                cast(30);
                sl.playSound(null, blockPosition(), SoundEvents.WOLF_HOWL, SoundSource.HOSTILE, 2.5F, 0.5F);
                Scheduler.later(20, () -> {
                    if (!isAlive()) return;
                    sl.sendParticles(ParticleTypes.SONIC_BOOM, getX(), getY() + 1.5, getZ(), 1, 0, 0, 0, 0);
                    for (LivingEntity e : enemiesAround(14)) {
                        e.hurt(damageSources().mobAttack(this), dmg(0.6F));
                        e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1));
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
                        Vec3 away = e.position().subtract(position()).normalize();
                        e.setDeltaMovement(away.x * 1.4, 0.4, away.z * 1.4);
                        e.hurtMarked = true;
                    }
                });
            }
            case "blink" -> {
                if (dist < 6) return false;
                Vec3 back = t.position().add(t.getLookAngle().multiply(1, 0, 1).normalize().scale(-2.2));
                sl.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 1, getZ(), 30, 0.4, 0.8, 0.4, 0.3);
                teleportTo(back.x, t.getY(), back.z);
                sl.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 1, getZ(), 30, 0.4, 0.8, 0.4, 0.3);
                sl.playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1F, 1F);
            }
            case "summon_goblin" -> summon("goblin", 3, sl);
            case "summon_elf" -> summon("ice_elf", 2, sl);
            case "summon_ant" -> summon("giant_ant", 3, sl);
            case "summon_stone" -> summon("stone_soldier", 2, sl);
            default -> { return false; }
        }
        return true;
    }

    private void fireBolt(LivingEntity t, String kind, float damage, double spread) {
        MagicBoltEntity b = new MagicBoltEntity(ModEntities.MAGIC_BOLT.get(), level());
        b.setOwner(this);
        b.setup(kind, damage);
        b.setPos(getX(), getY() + getBbHeight() * 0.7, getZ());
        Vec3 d = new Vec3(t.getX() - getX(), t.getEyeY() - b.getY(), t.getZ() - getZ()).normalize();
        Vec3 side = new Vec3(-d.z, 0, d.x).scale(spread);
        b.shoot(d.x + side.x, d.y, d.z + side.z, 1.5F, 0.5F);
        level().addFreshEntity(b);
    }

    private void summon(String mobId, int n, ServerLevel sl) {
        cast(30);
        int existing = level().getEntitiesOfClass(SLMonster.class, getBoundingBox().inflate(24), m -> m.minion).size();
        if (existing >= 6) return;
        sl.playSound(null, blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 1.5F, 0.8F);
        for (int i = 0; i < n; i++) {
            SLMonster m = ModEntities.MONSTERS.get(mobId).get().create(level());
            if (m == null) continue;
            double a = random.nextDouble() * Math.PI * 2;
            m.moveTo(getX() + Math.cos(a) * 3, getY(), getZ() + Math.sin(a) * 3, random.nextFloat() * 360, 0);
            m.markMinion();
            m.dungeonId = dungeonId;
            m.setPersistenceRequired();
            level().addFreshEntity(m);
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, m.getX(), m.getY() + 1, m.getZ(), 12, 0.3, 0.5, 0.3, 0.05);
        }
    }

    @Override
    public void die(DamageSource src) {
        super.die(src);
        if (!level().isClientSide && dungeonId >= 0) DungeonManager.onMobDeath(this);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        t.putLong("dungeon", dungeonId);
        t.putBoolean("minion", minion);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        dungeonId = t.contains("dungeon") ? t.getLong("dungeon") : -1;
        minion = t.getBoolean("minion");
    }

    @Override
    protected float getStandingEyeHeight(Pose pose, EntityDimensions dim) { return dim.height * 0.85F; }

    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource s) { return SoundEvents.IRON_GOLEM_HURT; }
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound() { return def.boss() ? SoundEvents.WITHER_DEATH : SoundEvents.RAVAGER_DEATH; }
    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound() { return def.boss() ? SoundEvents.RAVAGER_AMBIENT : SoundEvents.ZOMBIE_AMBIENT; }
    @Override protected void playStepSound(BlockPos pos, net.minecraft.world.level.block.state.BlockState st) { playSound(SoundEvents.IRON_GOLEM_STEP, 0.5F, 0.8F); }
}
