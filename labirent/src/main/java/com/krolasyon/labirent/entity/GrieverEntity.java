package com.krolasyon.labirent.entity;

import com.krolasyon.labirent.entity.ai.MazeNav;
import com.krolasyon.labirent.maze.Glade;
import com.krolasyon.labirent.maze.MazeLayout;
import com.krolasyon.labirent.maze.MazeManager;
import com.krolasyon.labirent.registry.ModEffects;
import com.krolasyon.labirent.registry.ModItems;
import com.krolasyon.labirent.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** Griever: a bloated slug body on clicking metal legs with a scorpion tail. Its sting starts the Changing. */
public class GrieverEntity extends Monster {
    public static final byte EV_CLAW = 60, EV_STING = 61, EV_ROLL = 62;
    public static final int ANIM_CLAW = 0, ANIM_STING = 1, ANIM_ROLL = 2;

    private boolean raider;
    private int stingCooldown = 60;
    private int rollCooldown = 80;
    private int clawCooldown;
    private int action = -1;
    private int actionTick;

    // client animation state
    public int clientAnim = -1;
    public float clientAnimStart;
    public float glowPulse;

    public GrieverEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 30;
        this.setMaxUpStep(1.6F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 70.0).add(Attributes.MOVEMENT_SPEED, 0.30)
                .add(Attributes.ATTACK_DAMAGE, 8.0).add(Attributes.ARMOR, 8.0).add(Attributes.FOLLOW_RANGE, 36.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8).add(Attributes.ATTACK_KNOCKBACK, 0.6);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new GrieverAttackGoal());
        goalSelector.addGoal(3, new PatrolGoal());
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 16F));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 5, true, false,
                p -> !(p instanceof Player pl) || !pl.isCreative()));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, GladerEntity.class, 5, true, false, g -> true));
    }

    public boolean isRaider() { return raider; }

    public void setRaider(boolean b) { raider = b; }

    // ------------------------------------------------------------------ combat
    private void startAction(int a, byte ev) {
        action = a;
        actionTick = 0;
        level().broadcastEntityEvent(this, ev);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id >= EV_CLAW && id <= EV_ROLL) {
            clientAnim = id - EV_CLAW;
            clientAnimStart = tickCount;
        } else super.handleEntityEvent(id);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            if (random.nextInt(12) == 0)
                level().addParticle(ParticleTypes.ELECTRIC_SPARK, getX() + (random.nextDouble() - 0.5) * 2, getY() + 0.6 + random.nextDouble(),
                        getZ() + (random.nextDouble() - 0.5) * 2, 0, 0.05, 0);
            if (random.nextInt(20) == 0)
                level().addParticle(ParticleTypes.DRIPPING_HONEY, getX() + (random.nextDouble() - 0.5) * 1.4, getY() + 0.5,
                        getZ() + (random.nextDouble() - 0.5) * 1.4, 0, 0, 0);
            return;
        }
        if (stingCooldown > 0) stingCooldown--;
        if (rollCooldown > 0) rollCooldown--;
        if (clawCooldown > 0) clawCooldown--;
        if (action >= 0) tickAction();
    }

    private void tickAction() {
        actionTick++;
        LivingEntity t = getTarget();
        switch (action) {
            case ANIM_CLAW -> {
                if (actionTick == 8 && t != null && distanceToSqr(t) < 3.6 * 3.6) doHurtTarget(t);
                if (actionTick >= 16) action = -1;
            }
            case ANIM_STING -> {
                if (actionTick == 6) playSound(ModSounds.GRIEVER_STING.get(), 1.5F, 1F);
                if (actionTick == 13 && t != null && distanceToSqr(t) < 5.0 * 5.0) {
                    if (t.hurt(damageSources().mobAttack(this), 5F)) {
                        t.addEffect(new MobEffectInstance(ModEffects.VENOM.get(), 20 * 150, 0));
                        t.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 0));
                        if (level() instanceof ServerLevel sl)
                            sl.sendParticles(ParticleTypes.ITEM_SLIME, t.getX(), t.getY() + 1, t.getZ(), 20, 0.3, 0.5, 0.3, 0.1);
                    }
                }
                if (actionTick >= 24) action = -1;
            }
            case ANIM_ROLL -> {
                if (actionTick == 4 && t != null) {
                    Vec3 d = t.position().subtract(position()).multiply(1, 0, 1).normalize().scale(1.25);
                    setDeltaMovement(d.x, 0.25, d.z);
                    playSound(ModSounds.GRIEVER_ROLL.get(), 1.6F, 1F);
                }
                if (actionTick > 4 && actionTick < 16 && t != null && distanceToSqr(t) < 2.6 * 2.6) {
                    doHurtTarget(t);
                    actionTick = 16;
                }
                if (actionTick >= 20) action = -1;
            }
            default -> action = -1;
        }
    }

    class GrieverAttackGoal extends Goal {
        private int repath;

        GrieverAttackGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }

        @Override
        public boolean canUse() { return getTarget() != null && getTarget().isAlive(); }

        @Override
        public boolean requiresUpdateEveryTick() { return true; }

        @Override
        public void stop() { getNavigation().stop(); }

        @Override
        public void tick() {
            LivingEntity t = getTarget();
            if (t == null) return;
            getLookControl().setLookAt(t, 40F, 30F);
            double d = distanceToSqr(t);
            if (action >= 0) {
                if (action != ANIM_ROLL) getNavigation().stop();
                return;
            }
            if (--repath <= 0) {
                repath = 8;
                getNavigation().moveTo(t, d > 64 ? 1.35 : 1.15);
            }
            if (d < 3.2 * 3.2 && clawCooldown <= 0) {
                clawCooldown = 22;
                startAction(ANIM_CLAW, EV_CLAW);
            } else if (d < 4.6 * 4.6 && stingCooldown <= 0 && hasLineOfSight(t)) {
                stingCooldown = 140 + random.nextInt(80);
                startAction(ANIM_STING, EV_STING);
            } else if (d > 6 * 6 && d < 14 * 14 && rollCooldown <= 0 && hasLineOfSight(t) && onGround()) {
                rollCooldown = 120 + random.nextInt(60);
                startAction(ANIM_ROLL, EV_ROLL);
            }
        }
    }

    /** prowls the corridors cell to cell; raiders head for the Glade */
    class PatrolGoal extends Goal {
        private final MazeNav nav = new MazeNav(GrieverEntity.this);
        private int wait;

        PatrolGoal() { setFlags(EnumSet.of(Flag.MOVE)); }

        @Override
        public boolean canUse() { return getTarget() == null; }

        @Override
        public boolean requiresUpdateEveryTick() { return true; }

        @Override
        public void stop() { nav.stop(); }

        @Override
        public void tick() {
            if (nav.tick()) return;
            if (--wait > 0) return;
            wait = 20 + random.nextInt(60);
            int ui = MazeLayout.unitOf(getBlockX()), uj = MazeLayout.unitOf(getBlockZ());
            if (ui < 0 || uj < 0) {
                // outside the maze (the chamber): wander locally
                Vec3 v = net.minecraft.world.entity.ai.util.DefaultRandomPos.getPos(GrieverEntity.this, 8, 3);
                if (v != null) getNavigation().moveTo(v.x, v.y, v.z, 1.0);
                return;
            }
            if (raider) {
                nav.goTo(MazeLayout.CENTER + 3, MazeLayout.CENTER + 3, 1.2);
                return;
            }
            int i = ui + (random.nextInt(9) - 4) * 2, j = uj + (random.nextInt(9) - 4) * 2;
            i = Mth.clamp(i | 1, 1, MazeLayout.G - 2);
            j = Mth.clamp(j | 1, 1, MazeLayout.G - 2);
            if (MazeLayout.inGladeArea(i, j)) return;
            nav.goTo(i, j, 0.9);
        }
    }

    // ------------------------------------------------------------------ misc
    @Override
    public boolean doHurtTarget(Entity e) {
        boolean ok = super.doHurtTarget(e);
        if (ok && level() instanceof ServerLevel sl)
            sl.sendParticles(ParticleTypes.CRIT, e.getX(), e.getY() + 1, e.getZ(), 10, 0.3, 0.4, 0.3, 0.2);
        return ok;
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (src.getEntity() instanceof Player p && MazeManager.get() != null
                && MazeManager.get().state.role(p.getUUID()) == com.krolasyon.labirent.team.Role.BEKCI) amount *= 1.3F;
        return super.hurt(src, amount);
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource src, int looting, boolean hit) {
        super.dropCustomDeathLoot(src, looting, hit);
        if (random.nextFloat() < 0.55F) spawnAtLocation(new ItemStack(ModItems.GRIEVER_STINGER.get()));
        spawnAtLocation(new ItemStack(ModItems.GRIEVER_PLATE.get(), 1 + random.nextInt(3)));
        spawnAtLocation(new ItemStack(Items.IRON_NUGGET, 2 + random.nextInt(5)));
        MazeManager m = MazeManager.get();
        boolean need = m != null && m.state.chapter >= 6 && m.state.progress(com.krolasyon.labirent.story.Objective.GET_CORE) == 0;
        if (need || random.nextFloat() < 0.12F) spawnAtLocation(new ItemStack(ModItems.GRIEVER_CORE.get()));
    }

    @Override
    public void die(DamageSource src) {
        super.die(src);
        if (!level().isClientSide && MazeManager.get() != null) MazeManager.get().story.onGrieverKilled(this, src);
    }

    @Override
    protected SoundEvent getAmbientSound() { return ModSounds.GRIEVER_AMBIENT.get(); }

    @Override
    public int getAmbientSoundInterval() { return 140; }

    @Override
    protected SoundEvent getHurtSound(DamageSource src) { return ModSounds.GRIEVER_HURT.get(); }

    @Override
    protected SoundEvent getDeathSound() { return ModSounds.GRIEVER_DEATH.get(); }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(ModSounds.GRIEVER_STEP.get(), 0.6F, 0.9F + random.nextFloat() * 0.2F);
    }

    @Override
    protected float getSoundVolume() { return 1.6F; }

    @Override
    public boolean removeWhenFarAway(double d) { return false; }

    @Override
    public int getMaxHeadYRot() { return 30; }

    @Override
    public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        t.putBoolean("Raider", raider);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        raider = t.getBoolean("Raider");
    }

    public boolean inGlade() { return Glade.inside(getX(), getZ()); }
}
