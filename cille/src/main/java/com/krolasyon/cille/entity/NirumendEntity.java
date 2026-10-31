package com.krolasyon.cille.entity;

import com.krolasyon.cille.registry.ModItems;
import com.krolasyon.cille.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Nirumend — the giant earth mammoth Cille. Bumehen earth attack, Ajirak trunk throw, bone-head bash charge,
 * hard rock barrage, electric shield, dust cloud and a quake stomp; enormous strength and toughness.
 */
public class NirumendEntity extends CilleEntity {
    public static final int BUMEHEN = 1, AJIRAK = 2, SKULL_BASH = 3, HARD_ROCK = 4, ELECTRIC_SHIELD = 5, DUST_CLOUD = 6, STOMP = 7;
    private static final int[] COMMANDABLE = {BUMEHEN, AJIRAK, SKULL_BASH, HARD_ROCK, ELECTRIC_SHIELD, DUST_CLOUD, STOMP};
    public static final int EARTH = 0xA88A5C, SPARK = 0x9AD8FF;

    @Nullable private LivingEntity grabbed;
    private boolean bashDone;
    private int dustTicks;
    private final List<BlockState> rocks = new ArrayList<>();

    public NirumendEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setMaxUpStep(1.6F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 480.0D)
                .add(Attributes.ARMOR, 16.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 6.0D)
                .add(Attributes.ATTACK_DAMAGE, 17.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override public String cilleName() { return "nirumend"; }
    @Override public Item marbleItem() { return ModItems.NIRUMEND_MISKETI.get(); }
    @Override public int[] commandableAbilities() { return COMMANDABLE; }
    @Override public double meleeReach() { return 4.4; }
    @Override public boolean needsTarget(int id) { return id == BUMEHEN || id == AJIRAK || id == SKULL_BASH || id == HARD_ROCK; }
    @Override protected int defendAbility() { return ready(ELECTRIC_SHIELD) ? ELECTRIC_SHIELD : ready(DUST_CLOUD) ? DUST_CLOUD : -1; }
    @Override protected boolean isDefensive(int id) { return id == ELECTRIC_SHIELD; }
    @Override protected boolean facesTarget(int id) { return id != SKULL_BASH || abilityTick < 6; }
    @Override protected boolean holdsStillWhileCasting() { return currentAbility != SKULL_BASH; }

    /** where the trunk's tip is (grabs, throws) */
    public Vec3 trunkTip() { return position().add(forward().scale(3.6)).add(0, 1.0, 0); }

    // ------------------------------------------------------------------ abilities
    @Override
    protected int abilityDuration(int id) {
        return switch (id) {
            case MELEE -> 20;
            case BUMEHEN -> 40;
            case AJIRAK -> 48;
            case SKULL_BASH -> 40;
            case HARD_ROCK -> 40;
            case ELECTRIC_SHIELD -> 32;
            case DUST_CLOUD -> 40;
            case STOMP -> 40;
            default -> 20;
        };
    }

    @Override
    protected int abilityCooldown(int id) {
        return switch (id) {
            case MELEE -> 22;
            case BUMEHEN -> 240;
            case AJIRAK -> 340;
            case SKULL_BASH -> 200;
            case HARD_ROCK -> 220;
            case ELECTRIC_SHIELD -> 460;
            case DUST_CLOUD -> 640;
            case STOMP -> 260;
            default -> 0;
        };
    }

    @Override
    protected int chooseAbility(LivingEntity target, double distSqr) {
        double dist = Math.sqrt(distSqr);
        boolean melee = dist <= meleeReach() + target.getBbWidth() * 0.5 && ready(MELEE);
        if (globalCooldown > 0 || this.tickCount % 5 != 0) return melee ? MELEE : -1;
        if (melee && random.nextFloat() < 0.45F) return MELEE;
        boolean los = this.hasLineOfSight(target);
        int crowd = hostilesAround(position(), 7).size();
        int[] opts = new int[9];
        int[] w = new int[9];
        int n = 0, total = 0;
        if (ready(BUMEHEN) && dist > 4 && dist < 22 && los) { opts[n] = BUMEHEN; w[n] = 3; total += w[n++]; }
        if (ready(AJIRAK) && dist < 7 && target.getBbHeight() < 4) { opts[n] = AJIRAK; w[n] = 3; total += w[n++]; }
        if (ready(SKULL_BASH) && dist > 5 && dist < 18 && los) { opts[n] = SKULL_BASH; w[n] = 4; total += w[n++]; }
        if (ready(HARD_ROCK) && dist > 7 && dist < 28 && los) { opts[n] = HARD_ROCK; w[n] = 3; total += w[n++]; }
        if (ready(ELECTRIC_SHIELD) && getShield() == SHIELD_NONE && (getHealth() < getMaxHealth() * 0.6F || this.tickCount - lastProjectileHit < 80)) { opts[n] = ELECTRIC_SHIELD; w[n] = 4; total += w[n++]; }
        if (ready(DUST_CLOUD) && dustTicks <= 0 && dist < 12) { opts[n] = DUST_CLOUD; w[n] = 1 + crowd; total += w[n++]; }
        if (ready(STOMP) && dist < 8 && this.onGround()) { opts[n] = STOMP; w[n] = 2 + crowd; total += w[n++]; }
        if (n == 0 || random.nextFloat() > 0.55F) return melee ? MELEE : -1;
        int roll = random.nextInt(total);
        for (int i = 0; i < n; i++) {
            roll -= w[i];
            if (roll < 0) return opts[i];
        }
        return opts[0];
    }

    @Override
    protected void onAbilityStart(int id) {
        bashDone = false;
        if (id != AJIRAK) releaseGrab(false);
    }

    @Override
    protected void onAbilityEnd(int id) {
        if (id == AJIRAK) releaseGrab(true);
    }

    @Override
    protected void tickAbility(int id, int t, @Nullable LivingEntity target) {
        ServerLevel sl = serverLevel();
        switch (id) {
            case MELEE -> {
                if (t == 5) sound(ModSounds.RUSH_DASH.get(), 1.4F, 0.8F);
                if (t == 11) {
                    Vec3 c = position().add(forward().scale(3.4)).add(0, 1.2, 0);
                    sl.sendParticles(ParticleTypes.SWEEP_ATTACK, c.x, c.y, c.z, 3, 1.0, 0.3, 1.0, 0);
                    for (LivingEntity e : hostilesIn(new AABB(c, c).inflate(3.0, 2.0, 3.0))) {
                        hit(e, damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * power(), 1.8, 0.5);
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
                    }
                }
            }
            case BUMEHEN -> {
                if (t == 4) sound(ModSounds.NIRUMEND_TRUMPET.get(), 3.0F, 0.9F);
                if (t == 22) {
                    quake(sl, position().add(forward().scale(2.6)), 2.5F, 0.8);
                    Vec3 dir = target != null ? target.position().subtract(position()).multiply(1, 0, 1).normalize() : forward();
                    float dmg = 11F * power();
                    for (int i = 0; i < 16; i++) {
                        Vec3 p = position().add(dir.scale(3.0 + i * 1.3));
                        ShadowSpikeEntity.spawn(this, p.x, p.z, getY(), 1 + i, dmg, ShadowSpikeEntity.KIND_ROCK);
                    }
                    if (target != null) {
                        for (int i = 0; i < 6; i++) {
                            double a = i * Math.PI / 3;
                            ShadowSpikeEntity.spawn(this, target.getX() + Math.cos(a) * 2.2, target.getZ() + Math.sin(a) * 2.2, target.getY(), 12, dmg * 0.8F, ShadowSpikeEntity.KIND_ROCK);
                        }
                    }
                }
            }
            case AJIRAK -> tickAjirak(sl, t, target);
            case SKULL_BASH -> tickBash(sl, t, target);
            case HARD_ROCK -> tickHardRock(sl, t, target);
            case ELECTRIC_SHIELD -> {
                if (t == 6) sound(ModSounds.ELECTRIC_SHIELD.get(), 2.5F, 1.0F);
                if (t == 8) {
                    setShield(SHIELD_ELECTRIC);
                    shieldTimer = 220;
                    sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 2, getZ(), 80, 2.5, 1.8, 2.5, 0.4);
                }
            }
            case DUST_CLOUD -> {
                if (t == 6 || t == 16) {
                    sound(ModSounds.HEAVY_STEP.get(), 3.0F, 0.8F);
                    quake(sl, position().add(forward().scale(2.4)), 0F, 0);
                }
                if (t == 22) {
                    sound(ModSounds.DUST_BURST.get(), 4.0F, 1.0F);
                    dustTicks = isPhase2() ? 260 : 200;
                    setEmpowered(true);
                    sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1, getZ(), 120, 5, 1.5, 5, 0.1);
                }
            }
            case STOMP -> {
                if (t == 6) sound(ModSounds.NIRUMEND_TRUMPET.get(), 3.5F, 0.85F);
                if (t == 24) {
                    Vec3 c = position().add(forward().scale(2.2));
                    quake(sl, c, (isPhase2() ? 20F : 16F) * power(), 1.0);
                    Terrain.crater(sl, this, c, isPhase2() ? 4.2 : 3.6, 18, 3.0F);
                    for (int i = 0; i < 10; i++) {
                        double a = i * Math.PI * 2 / 10;
                        ShadowSpikeEntity.spawn(this, c.x + Math.cos(a) * 4.6, c.z + Math.sin(a) * 4.6, getY(), 4, 8F * power(), ShadowSpikeEntity.KIND_ROCK);
                    }
                }
            }
            default -> {}
        }
    }

    /** shock wave from a heavy foot: damage + throw everything around, earthy debris */
    private void quake(ServerLevel sl, Vec3 c, float dmg, double lift) {
        sound(ModSounds.ATLAS_SMASH.get(), 3.5F, 0.8F);
        BlockState below = sl.getBlockState(BlockPos.containing(c.x, getY() - 0.5, c.z));
        if (below.isAir()) below = Blocks.DIRT.defaultBlockState();
        BlockParticleOption bp = new BlockParticleOption(ParticleTypes.BLOCK, below);
        for (int ring = 0; ring < 3; ring++) {
            for (int i = 0; i < 40; i++) {
                double a = i * Math.PI * 2 / 40;
                sl.sendParticles(ring == 2 ? ParticleTypes.CLOUD : bp, c.x + Math.cos(a) * 1.5, getY() + 0.2, c.z + Math.sin(a) * 1.5,
                        0, Math.cos(a), 0.05, Math.sin(a), 0.5 + ring * 0.35);
            }
        }
        if (dmg > 0) {
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, getY() + 0.3, c.z, 1, 0, 0, 0, 0);
            for (LivingEntity e : hostilesAround(c, 7.0)) {
                hit(e, damageSources().mobAttack(this), dmg * (float) (1.0 - 0.4 * e.distanceTo(this) / 7.0), 1.8, lift);
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
            }
        }
    }

    private void tickAjirak(ServerLevel sl, int t, @Nullable LivingEntity target) {
        if (t == 4) sound(ModSounds.RUSH_DASH.get(), 1.6F, 0.75F);
        if (t >= 8 && t <= 13 && grabbed == null && target != null && target.isAlive() && isHostileTo(target)
                && target.distanceToSqr(trunkTip()) < 4.5 * 4.5) {
            grabbed = target;
            sound(ModSounds.CLAMP_BITE.get(), 2.0F, 0.7F);
        }
        if (grabbed == null) return;
        if (!grabbed.isAlive() || grabbed.isRemoved()) { grabbed = null; return; }
        if (t < 36) {
            // lifted high up in the trunk
            double up = t < 24 ? 1.0 + (t - 12) * 0.45 : 6.4;
            Vec3 p = position().add(forward().scale(t < 24 ? 3.2 : 1.6)).add(0, up, 0);
            grabbed.teleportTo(p.x, p.y, p.z);
            grabbed.setDeltaMovement(Vec3.ZERO);
            grabbed.fallDistance = 0;
            if (t % 6 == 0) grabbed.hurt(damageSources().mobAttack(this), 3F * power());
        }
        if (t == 36) {
            // and smashed into the ground in front
            LivingEntity g = grabbed;
            grabbed = null;
            Vec3 land = position().add(forward().scale(4.5));
            Double gy = ShadowSpikeEntity.findGround(sl, land.x, land.z, getY() + 4, getY() - 6);
            g.teleportTo(land.x, gy != null ? gy : getY(), land.z);
            g.hurt(damageSources().mobAttack(this), 16F * power());
            g.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 3));
            g.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0));
            sound(ModSounds.ROCK_IMPACT.get(), 3.0F, 0.8F);
            Terrain.crater(sl, this, g.position(), 2.2, 6, 2.0F);
            sl.sendParticles(ParticleTypes.EXPLOSION, g.getX(), g.getY() + 0.3, g.getZ(), 2, 0.3, 0.2, 0.3, 0);
        }
    }

    private void releaseGrab(boolean drop) {
        if (grabbed == null) return;
        if (drop && grabbed.isAlive()) grabbed.setDeltaMovement(forward().scale(0.6).add(0, 0.3, 0));
        grabbed.hurtMarked = true;
        grabbed = null;
    }

    private void tickBash(ServerLevel sl, int t, @Nullable LivingEntity target) {
        if (t == 2) sound(ModSounds.NIRUMEND_TRUMPET.get(), 3.5F, 1.0F);
        if (t < 6) {
            sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.3, getZ(), 4, 1.2, 0.1, 1.2, 0.02);
            return;
        }
        if (bashDone || t > 30) return;
        Vec3 dir = forward();
        if (target != null && t < 14) {
            Vec3 d = target.position().subtract(position()).multiply(1, 0, 1);
            if (d.lengthSqr() > 1.0E-3) {
                dir = d.normalize();
                faceTowards(target);
            }
        }
        this.setDeltaMovement(dir.x * 0.85, getDeltaMovement().y, dir.z * 0.85);
        this.hasImpulse = true;
        if (t % 3 == 0) sound(ModSounds.HEAVY_STEP.get(), 1.5F, 1.1F);
        sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.4, getZ(), 6, 1.4, 0.2, 1.4, 0.02);
        // ploughs through soft and weak blocks
        Vec3 front = position().add(dir.scale(2.4));
        if (Terrain.allowed(sl, this)) {
            BlockPos fp = BlockPos.containing(front);
            for (BlockPos p : BlockPos.betweenClosed(fp.offset(-1, 0, -1), fp.offset(1, 3, 1))) {
                BlockState st = sl.getBlockState(p);
                if (Terrain.breakable(sl, p, st, 1.5F)) sl.destroyBlock(p, random.nextFloat() < 0.2F, this);
            }
        }
        AABB box = new AABB(front, front).inflate(2.0, 2.0, 2.0).move(0, 1.2, 0);
        List<LivingEntity> hits = hostilesIn(box);
        if (!hits.isEmpty()) {
            bashDone = true;
            sound(ModSounds.HEADBUTT.get(), 3.5F, 1.0F);
            for (LivingEntity e : hits) {
                hit(e, damageSources().mobAttack(this), 20F * power(), 2.8, 0.9);
                e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                if (e instanceof CilleEntity c) c.interrupt();
            }
            sl.sendParticles(ParticleTypes.EXPLOSION, front.x, front.y + 1.2, front.z, 3, 0.5, 0.5, 0.5, 0);
            this.setDeltaMovement(Vec3.ZERO);
        }
    }

    private void tickHardRock(ServerLevel sl, int t, @Nullable LivingEntity target) {
        if (t == 10) {
            sound(ModSounds.ROCK_RIP.get(), 3.0F, 0.8F);
            rocks.clear();
            for (int i = 0; i < 3; i++) {
                Vec3 off = forward().scale(2.5).add(right().scale((i - 1) * 2.0));
                BlockPos g = BlockPos.containing(position().add(off)).below();
                BlockState st = sl.getBlockState(g);
                BlockState use = Blocks.STONE.defaultBlockState();
                if (Terrain.allowed(sl, this) && Terrain.breakable(sl, g, st, 3.0F) && st.isSolidRender(sl, g)) {
                    use = st;
                    sl.destroyBlock(g, false, this);
                }
                rocks.add(use);
                sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, use), g.getX() + 0.5, g.getY() + 1.2, g.getZ() + 0.5, 30, 0.5, 0.4, 0.5, 0.2);
            }
        }
        int shot = t == 20 ? 0 : t == 24 ? 1 : t == 28 ? 2 : -1;
        if (shot >= 0 && target != null) {
            BlockState st = shot < rocks.size() ? rocks.get(shot) : Blocks.STONE.defaultBlockState();
            Vec3 from = position().add(forward().scale(2.4)).add(right().scale((shot - 1) * 1.6)).add(0, 4.2, 0);
            Vec3 aim = target.getBoundingBox().getCenter().add(target.getDeltaMovement().scale(8));
            Vec3 d = aim.subtract(from);
            double flat = Math.sqrt(d.x * d.x + d.z * d.z);
            CilleProjectileEntity.launchRock(this, st, from, d.add(0, flat * 0.16, 0), 1.4 + flat * 0.01, target);
            sound(ModSounds.RUSH_DASH.get(), 1.8F, 0.75F);
        }
    }

    // ------------------------------------------------------------------ toughness, instincts
    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (!this.level().isClientSide() && !src.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && amount > 12F) {
            amount = 12F + (amount - 12F) * 0.5F;     // high endurance: huge blows are blunted
        }
        return super.hurt(src, amount);
    }

    @Override
    protected void instinct(DamageSource src, @Nullable LivingEntity la) {
        if (isCasting() && currentAbility != MELEE) return;
        Entity direct = src.getDirectEntity();
        if (getShield() == SHIELD_NONE && ready(ELECTRIC_SHIELD) && (direct instanceof Projectile || getHealth() < getMaxHealth() * 0.45F) && random.nextFloat() < 0.5F) {
            startAbility(ELECTRIC_SHIELD, la);
        } else if (la != null && direct == la && ready(STOMP) && distanceToSqr(la) < 49 && getStance() != STANCE_PASSIVE && random.nextFloat() < 0.3F) {
            startAbility(STOMP, la);
        } else {
            counterMelee(la, src);
        }
    }

    @Override
    protected boolean reactToProjectile(Projectile p) {
        if (getShield() == SHIELD_NONE && ready(ELECTRIC_SHIELD) && random.nextFloat() < 0.45F) {
            startAbility(ELECTRIC_SHIELD, p.getOwner() instanceof LivingEntity l ? l : null);
            return true;
        }
        return false;
    }

    @Override
    protected void releaseTick(ServerLevel sl, int t) {
        if (t == 12) {
            sound(ModSounds.NIRUMEND_TRUMPET.get(), 4.0F, 1.0F);
            quake(sl, position(), 0F, 0);
            for (LivingEntity e : hostilesAround(position(), 8)) {
                if (e instanceof CilleEntity) continue;
                knock(e, 1.4, 0.4);
            }
        }
    }

    @Override
    protected void onPhase2(ServerLevel sl) {
        sound(ModSounds.NIRUMEND_TRUMPET.get(), 4.0F, 0.8F);
        quake(sl, position(), 6F, 0.5);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (dustTicks > 0) {
            dustTicks--;
            if (dustTicks % 5 == 0) {
                double r = isPhase2() ? 16 : 13;
                for (LivingEntity e : hostilesAround(position(), r)) {
                    e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
                    if (e instanceof Player) e.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0));
                    if (e instanceof Mob m && m.getTarget() == this && random.nextFloat() < 0.2F) m.setTarget(null);   // they lose sight of it
                }
            }
            if (dustTicks == 0) setEmpowered(false);
        }
    }

    // ------------------------------------------------------------------ client fx
    @Override
    protected void clientFx() {
        Level l = this.level();
        if (getDeltaMovement().horizontalDistanceSqr() > 0.002 && random.nextInt(2) == 0)
            l.addParticle(ParticleTypes.CLOUD, getX() + (random.nextDouble() - 0.5) * 3, getY() + 0.1, getZ() + (random.nextDouble() - 0.5) * 3, 0, 0.01, 0);
        if (isEmpowered()) {
            double r = isPhase2() ? 16 : 13;
            for (int i = 0; i < 16; i++) {
                double a = random.nextDouble() * Math.PI * 2, d = random.nextDouble() * r;
                l.addParticle(i % 3 == 0 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.CLOUD, getX() + Math.cos(a) * d, getY() + random.nextDouble() * 5, getZ() + Math.sin(a) * d,
                        (random.nextDouble() - 0.5) * 0.05, 0.01, (random.nextDouble() - 0.5) * 0.05);
            }
        }
        if (getShield() == SHIELD_ELECTRIC && random.nextInt(2) == 0) {
            double a = random.nextDouble() * Math.PI * 2, b = random.nextDouble() * Math.PI - Math.PI / 2;
            double r = 3.6;
            l.addParticle(ParticleTypes.ELECTRIC_SPARK, getX() + Math.cos(a) * Math.cos(b) * r, getY() + 2 + Math.sin(b) * r * 0.8, getZ() + Math.sin(a) * Math.cos(b) * r, 0, 0, 0);
        }
    }

    @Override
    public Vec3 mouthPos() { return position().add(forward().scale(3.2)).add(0, 2.0, 0); }

    @Override protected SoundEvent getAmbientSound() { return ModSounds.NIRUMEND_AMBIENT.get(); }
    @Override protected SoundEvent getHurtSound(DamageSource src) { return ModSounds.NIRUMEND_HURT.get(); }
    @Override protected SoundEvent getDeathSoundEvent() { return ModSounds.NIRUMEND_DEATH.get(); }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(ModSounds.HEAVY_STEP.get(), 1.0F, 0.85F + random.nextFloat() * 0.2F);
    }
}
