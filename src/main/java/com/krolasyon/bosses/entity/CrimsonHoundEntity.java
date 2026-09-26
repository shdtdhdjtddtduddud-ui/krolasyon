package com.krolasyon.bosses.entity;

import com.krolasyon.bosses.registry.ModEntities;
import com.krolasyon.bosses.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/** Kızıl Cehennem Kurdu — three-tailed hellhound with glowing crimson cracks. */
public class CrimsonHoundEntity extends BossEntity {
    public static final int MELEE = 0, LEAP = 1, HOWL = 2, FISSURE = 3, TAIL_BLAST = 4, FRENZY = 5;
    public static final int RED = 0xFF3A48, EMBER = 0xFF9A6A, DARK_RED = 0x8C1028;

    private boolean leaping;

    public CrimsonHoundEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.RED, 6);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 380.0D)
                .add(Attributes.ARMOR, 10.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 4.0D)
                .add(Attributes.ATTACK_DAMAGE, 13.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.33D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.85D);
    }

    @Override public double meleeReach() { return 3.6; }
    @Override public double walkSpeed() { return 0.85; }
    @Override public double runSpeed() { return 1.45; }

    /** approximate world positions of the three tail tips: center, right, left */
    public Vec3[] tailTips() {
        Vec3 p = position();
        Vec3 f = forward();
        Vec3 r = right();
        return new Vec3[]{
                p.add(f.scale(-2.9)).add(0, 3.1, 0),
                p.add(f.scale(-2.4)).add(r.scale(1.9)).add(0, 2.5, 0),
                p.add(f.scale(-2.4)).add(r.scale(-1.9)).add(0, 2.5, 0)};
    }

    // ------------------------------------------------------------------ abilities
    @Override
    protected int abilityDuration(int id) {
        return switch (id) {
            case MELEE -> 16;
            case LEAP -> 28;
            case HOWL -> 52;
            case FISSURE -> 32;
            case TAIL_BLAST -> 36;
            case FRENZY -> 24;
            default -> 20;
        };
    }

    @Override
    protected int abilityCooldown(int id) {
        return switch (id) {
            case MELEE -> 20;
            case LEAP -> 120;
            case HOWL -> 520;
            case FISSURE -> 180;
            case TAIL_BLAST -> 150;
            case FRENZY -> 190;
            default -> 100;
        };
    }

    @Override
    protected int chooseAbility(LivingEntity target, double distSqr) {
        double dist = Math.sqrt(distSqr);
        boolean melee = inMeleeRange(target, distSqr) && ready(MELEE);
        if (globalCooldown > 0 || this.tickCount % 7 != 0) return melee ? MELEE : -1;
        if (melee && random.nextFloat() < 0.5F) return MELEE;
        int[] opts = new int[6];
        int[] w = new int[6];
        int n = 0, total = 0;
        if (ready(LEAP) && dist > 6 && dist < 22 && this.onGround()) { opts[n] = LEAP; w[n++] = dist > 9 ? 4 : 2; total += w[n - 1]; }
        if (ready(HOWL) && (isPhase2() || getHealth() < getMaxHealth() * 0.85F)) { opts[n] = HOWL; w[n++] = 2; total += 2; }
        if (ready(FISSURE) && dist < 17) { opts[n] = FISSURE; w[n++] = 3; total += 3; }
        if (ready(TAIL_BLAST) && dist > 5 && dist < 30 && this.hasLineOfSight(target)) { opts[n] = TAIL_BLAST; w[n++] = 3; total += 3; }
        if (ready(FRENZY) && dist < 7) { opts[n] = FRENZY; w[n++] = 3; total += 3; }
        if (n == 0 || random.nextFloat() > 0.6F) return melee ? MELEE : -1;
        int roll = random.nextInt(total);
        for (int i = 0; i < n; i++) {
            roll -= w[i];
            if (roll < 0) return opts[i];
        }
        return opts[0];
    }

    @Override
    protected boolean isAirborneAbility() { return currentAbility == LEAP && leaping; }

    @Override
    protected void tickAbility(int id, int t, @Nullable LivingEntity target) {
        ServerLevel sl = serverLevel();
        switch (id) {
            case MELEE -> {
                if (t == 2) sound(ModSounds.HOUND_BITE.get(), 1.6F, 0.9F + random.nextFloat() * 0.2F);
                if (t == 6) clawArc(sl, (float) getAttributeValue(Attributes.ATTACK_DAMAGE), 4, 1, 1.0);
            }
            case LEAP -> tickLeap(sl, t, target);
            case HOWL -> tickHowl(sl, t);
            case FISSURE -> {
                if (t == 4) sound(ModSounds.HOUND_AMBIENT.get(), 2.0F, 0.7F);
                if (t == 15) {
                    sound(ModSounds.FISSURE.get(), 3.0F, 0.9F);
                    Vec3 dir = target != null ? target.position().subtract(position()).multiply(1, 0, 1).normalize() : forward();
                    float dmg = isPhase2() ? 13F : 10F;
                    float[] angles = isPhase2() ? new float[]{0F, 0.45F, -0.45F} : new float[]{0F};
                    for (float a : angles) {
                        Vec3 d = dir.yRot(a);
                        for (int i = 0; i < 15; i++) {
                            Vec3 p = position().add(d.scale(2.0 + i * 1.3));
                            EruptionEntity.spawn(this, p.x, p.z, getY(), EruptionEntity.KIND_FIRE, 1 + i, dmg);
                        }
                    }
                    Vec3 c = position().add(forward().scale(2.2));
                    sl.sendParticles(ParticleTypes.EXPLOSION, c.x, getY() + 0.3, c.z, 2, 0.5, 0.1, 0.5, 0);
                    sl.sendParticles(ParticleTypes.LAVA, c.x, getY() + 0.3, c.z, 30, 1.0, 0.2, 1.0, 0);
                    for (int i = 0; i < 36; i++) {
                        double a = i * Math.PI * 2 / 36;
                        sl.sendParticles(ParticleTypes.FLAME, c.x, getY() + 0.2, c.z, 0, Math.cos(a), 0.05, Math.sin(a), 0.45);
                    }
                    for (LivingEntity e : hostilesAround(c, 3.2)) hit(e, damageSources().mobAttack(this), dmg, 1.0, 0.6);
                }
            }
            case TAIL_BLAST -> {
                if (t == 3) sound(ModSounds.HOUND_AMBIENT.get(), 1.5F, 1.3F);
                int shot = t == 9 ? 0 : t == 14 ? 1 : t == 19 ? 2 : -1;
                Vec3[] tips = tailTips();
                if (t < 20) {
                    for (Vec3 tip : tips) sl.sendParticles(ParticleTypes.FLAME, tip.x, tip.y, tip.z, 2, 0.2, 0.2, 0.2, 0.02);
                }
                if (shot >= 0 && target != null) {
                    Vec3 from = tips[shot];
                    Vec3 aim = target.getBoundingBox().getCenter().add(target.getDeltaMovement().scale(8));
                    Vec3 d = aim.subtract(from);
                    HellfireBallEntity ball = new HellfireBallEntity(ModEntities.HELLFIRE_BALL.get(), this.level());
                    ball.setup(this, from, d, isPhase2());
                    this.level().addFreshEntity(ball);
                    soundAt(from, ModSounds.FIREBALL_SHOOT.get(), 2.0F, 0.8F + shot * 0.15F);
                    sl.sendParticles(ParticleTypes.FLASH, from.x, from.y, from.z, 1, 0, 0, 0, 0);
                    if (isPhase2()) {
                        for (float a : new float[]{0.3F, -0.3F}) {
                            HellfireBallEntity b2 = new HellfireBallEntity(ModEntities.HELLFIRE_BALL.get(), this.level());
                            b2.setup(this, from, d.yRot(a), true);
                            this.level().addFreshEntity(b2);
                        }
                    }
                }
            }
            case FRENZY -> {
                if (t == 2 || t == 6 || t == 10 || t == 14) {
                    Vec3 dir = target != null ? target.position().subtract(position()).multiply(1, 0, 1).normalize() : forward();
                    double dist = target != null ? this.distanceTo(target) : 5;
                    double push = dist > 2.5 ? 0.75 : 0.25;
                    this.setDeltaMovement(dir.x * push, 0.12, dir.z * push);
                    this.hasImpulse = true;
                }
                if (t == 3 || t == 7 || t == 11 || t == 15) {
                    sound(ModSounds.FRENZY_SLASH.get(), 1.6F, 0.8F + t * 0.03F);
                    float dmg = isPhase2() ? 9F : 7F;
                    float dealt = clawArc(sl, dmg, 3, (t / 4) % 2 == 0 ? 1 : -1, 0.5);
                    if (dealt > 0) this.heal(dealt * 0.4F);
                }
            }
            default -> {}
        }
    }

    /** claw sweep in front; returns total damage dealt */
    private float clawArc(ServerLevel sl, float dmg, int fireSeconds, int side, double knock) {
        Vec3 f = forward();
        Vec3 c = position().add(f.scale(2.1)).add(0, 1.0, 0);
        for (int i = 0; i < 12; i++) {
            double a = (i / 11.0 - 0.5) * Math.PI * 0.9 * side;
            Vec3 d = f.yRot((float) a).scale(2.8);
            sl.sendParticles(i % 3 == 0 ? ParticleTypes.FLAME : dust(i % 2 == 0 ? RED : EMBER, 1.5F),
                    getX() + d.x, getY() + 1.3 + i * 0.04, getZ() + d.z, 1, 0, 0, 0, 0);
        }
        sl.sendParticles(ParticleTypes.SWEEP_ATTACK, c.x, c.y, c.z, 1, 0.3, 0.1, 0.3, 0);
        float total = 0;
        for (LivingEntity e : hostilesIn(new AABB(c, c).inflate(2.3, 1.6, 2.3))) {
            Vec3 to = e.position().subtract(position());
            if (to.horizontalDistanceSqr() > 0.01 && to.normalize().dot(f) < -0.2) continue;
            float before = e.getHealth();
            hit(e, damageSources().mobAttack(this), dmg, knock, 0.25);
            e.setSecondsOnFire(fireSeconds);
            total += Math.max(0, before - e.getHealth());
        }
        return total;
    }

    private void tickLeap(ServerLevel sl, int t, @Nullable LivingEntity target) {
        if (t == 0) sound(ModSounds.HOUND_AMBIENT.get(), 2.0F, 0.8F);
        if (t < 6) sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.2, getZ(), 3, 0.8, 0.1, 0.8, 0.02);
        if (t == 6) {
            Vec3 dest = target != null ? target.position() : position().add(forward().scale(8));
            Vec3 d = dest.subtract(position());
            double horiz = Math.min(Math.sqrt(d.x * d.x + d.z * d.z), 20.0);
            Vec3 dir = new Vec3(d.x, 0, d.z).normalize();
            int flight = 14;
            double hv = horiz / 8.1;
            double vy = 0.62 + Math.max(-0.3, Math.min(0.6, d.y / flight));
            this.setDeltaMovement(dir.x * hv, vy, dir.z * hv);
            this.hasImpulse = true;
            this.leaping = true;
            sound(ModSounds.HOUND_LEAP.get(), 2.0F, 1.0F);
            sl.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.2, getZ(), 1, 0, 0, 0, 0);
        }
        if (leaping) {
            sl.sendParticles(ParticleTypes.FLAME, getX(), getY() + 1.2, getZ(), 6, 0.8, 0.6, 0.8, 0.02);
            sl.sendParticles(dust(RED, 2.0F), getX(), getY() + 1.2, getZ(), 4, 0.8, 0.6, 0.8, 0);
            if ((t > 9 && this.onGround()) || t >= 26) land(sl);
        }
    }

    private void land(ServerLevel sl) {
        leaping = false;
        sound(ModSounds.HOUND_LAND.get(), 3.0F, 0.9F);
        float dmg = isPhase2() ? 17F : 14F;
        for (LivingEntity e : hostilesAround(position(), 4.5)) {
            hit(e, damageSources().mobAttack(this), dmg, 1.3, 0.55);
            e.setSecondsOnFire(5);
        }
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 0.3, getZ(), 1, 0, 0, 0, 0);
        for (int ring = 0; ring < 2; ring++) {
            for (int i = 0; i < 48; i++) {
                double a = i * Math.PI * 2 / 48;
                sl.sendParticles(ring == 0 ? ParticleTypes.FLAME : dust(RED, 2.2F), getX() + Math.cos(a) * 1.2, getY() + 0.2, getZ() + Math.sin(a) * 1.2,
                        0, Math.cos(a), 0.04, Math.sin(a), ring == 0 ? 0.55 : 0.9);
            }
        }
        sl.sendParticles(ParticleTypes.LAVA, getX(), getY() + 0.3, getZ(), 25, 1.5, 0.2, 1.5, 0);
        // scorch ring of fire eruptions
        if (isPhase2()) {
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4;
                EruptionEntity.spawn(this, getX() + Math.cos(a) * 3.5, getZ() + Math.sin(a) * 3.5, getY(), EruptionEntity.KIND_FIRE, 3, 8F);
            }
        }
    }

    private void tickHowl(ServerLevel sl, int t) {
        if (t == 8) sound(ModSounds.HOUND_HOWL.get(), 5.0F, 1.0F);
        if (t >= 10 && t <= 40 && t % 3 == 1) {
            double r = 1.5 + (t - 10) * 0.55;
            int n = (int) (r * 10);
            for (int i = 0; i < n; i++) {
                double a = i * Math.PI * 2 / n;
                sl.sendParticles(dust(i % 2 == 0 ? RED : DARK_RED, 2.2F), getX() + Math.cos(a) * r, getY() + 0.4 + ((t / 3) % 2) * 0.6, getZ() + Math.sin(a) * r, 1, 0, 0.05, 0, 0);
            }
            Vec3 mouth = position().add(forward().scale(2.4)).add(0, 3.0, 0);
            sl.sendParticles(dust(RED, 3.0F), mouth.x, mouth.y, mouth.z, 8, 0.3, 0.3, 0.3, 0);
            sl.sendParticles(ParticleTypes.FLAME, mouth.x, mouth.y, mouth.z, 6, 0.2, 0.4, 0.2, 0.06);
        }
        if (t == 12) {
            for (LivingEntity e : hostilesAround(position(), 18)) {
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 1));
                if (e instanceof Player) e.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0));
                if (e.distanceToSqr(this) < 36) hit(e, damageSources().mobAttack(this), 5F, 1.8, 0.5);
            }
            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 320, 1));
            this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 320, 1));
            this.heal(isPhase2() ? 35F : 25F);
            sl.sendParticles(ParticleTypes.FLASH, getX(), getY() + 2.5, getZ(), 1, 0, 0, 0, 0);
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (isPhase2() && this.tickCount % 20 == 0) {
            for (LivingEntity e : hostilesAround(position(), 3.5)) {
                e.setSecondsOnFire(3);
                e.hurt(damageSources().onFire(), 2F);
            }
        }
    }

    @Override
    protected void onPhase2() {
        sound(ModSounds.HOUND_PHASE.get(), 4.0F, 1.0F);
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.39D);
        ServerLevel sl = serverLevel();
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 1, getZ(), 1, 0, 0, 0, 0);
        for (int i = 0; i < 90; i++) {
            double a = i * Math.PI * 2 / 90;
            sl.sendParticles(i % 2 == 0 ? ParticleTypes.FLAME : dust(RED, 2.5F), getX(), getY() + 0.5, getZ(), 0, Math.cos(a), 0.15, Math.sin(a), 0.9);
        }
        for (LivingEntity e : hostilesAround(position(), 9)) {
            hit(e, damageSources().mobAttack(this), 6F, 2.2, 0.6);
            e.setSecondsOnFire(6);
        }
        this.heal(20F);
    }

    // ------------------------------------------------------------------ fx
    @Override
    protected void ambientFx() {
        Level l = this.level();
        boolean p2 = isPhase2();
        int n = p2 ? 3 : 1;
        for (int i = 0; i < n; i++) {
            Vec3 f = forward();
            double along = (random.nextDouble() - 0.4) * 3.0;
            l.addParticle(random.nextInt(3) == 0 ? ParticleTypes.SMALL_FLAME : dust(random.nextBoolean() ? RED : EMBER, 1.0F),
                    getX() + f.x * along + (random.nextDouble() - 0.5) * 1.6, getY() + 1.6 + random.nextDouble() * 1.0,
                    getZ() + f.z * along + (random.nextDouble() - 0.5) * 1.6, 0, 0.04, 0);
        }
        for (Vec3 tip : tailTips()) {
            if (random.nextInt(p2 ? 1 : 2) == 0)
                l.addParticle(ParticleTypes.FLAME, tip.x + (random.nextDouble() - 0.5) * 0.5, tip.y + (random.nextDouble() - 0.5) * 0.5, tip.z + (random.nextDouble() - 0.5) * 0.5, 0, 0.03, 0);
        }
        if (random.nextInt(p2 ? 6 : 20) == 0) l.addParticle(ParticleTypes.LAVA, getX(), getY() + 1.5, getZ(), 0, 0, 0);
        if (random.nextInt(4) == 0) l.addParticle(ParticleTypes.SMOKE, getX() + (random.nextDouble() - 0.5) * 2, getY() + 2.2, getZ() + (random.nextDouble() - 0.5) * 2, 0, 0.03, 0);
        if (clientAnimId == LEAP || clientAnimId == FRENZY) {
            for (int i = 0; i < 3; i++) l.addParticle(ParticleTypes.FLAME, getX() + (random.nextDouble() - 0.5) * 2, getY() + random.nextDouble() * 2, getZ() + (random.nextDouble() - 0.5) * 2, 0, 0.02, 0);
        }
        if (phaseFlash > 0) {
            for (int i = 0; i < 4; i++) l.addParticle(ParticleTypes.FLAME, getX() + (random.nextDouble() - 0.5) * 3, getY() + random.nextDouble() * 2.5, getZ() + (random.nextDouble() - 0.5) * 3, 0, 0.12, 0);
        }
    }

    @Override
    protected void deathFx(int t) {
        if (this.level().isClientSide()) {
            for (int i = 0; i < 3; i++) {
                this.level().addParticle(i == 0 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.FLAME,
                        getX() + (random.nextDouble() - 0.5) * 2.5, getY() + random.nextDouble() * 2, getZ() + (random.nextDouble() - 0.5) * 2.5, 0, 0.06, 0);
            }
        } else {
            if (t == 1) sound(ModSounds.HOUND_DEATH.get(), 4.0F, 1.0F);
            if (t == DEATH_TICKS - 1) {
                ServerLevel sl = serverLevel();
                sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 1, getZ(), 1, 0, 0, 0, 0);
                sl.sendParticles(ParticleTypes.LAVA, getX(), getY() + 1, getZ(), 40, 1.5, 0.8, 1.5, 0);
                sl.sendParticles(dust(RED, 3F), getX(), getY() + 1, getZ(), 100, 1.5, 1.0, 1.5, 0);
                sound(ModSounds.FIREBALL_HIT.get(), 3.0F, 0.6F);
            }
        }
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource src, int looting, boolean hitByPlayer) {
        super.dropCustomDeathLoot(src, looting, hitByPlayer);
        this.spawnAtLocation(new ItemStack(Items.NETHER_STAR));
        this.spawnAtLocation(new ItemStack(Items.BLAZE_ROD, 8 + random.nextInt(6)));
        this.spawnAtLocation(new ItemStack(Items.NETHERITE_SCRAP, 2 + random.nextInt(2)));
        this.spawnAtLocation(new ItemStack(Items.MAGMA_CREAM, 6 + random.nextInt(6)));
    }

    // ------------------------------------------------------------------ sounds
    @Override protected SoundEvent getAmbientSound() { return ModSounds.HOUND_AMBIENT.get(); }
    @Override protected SoundEvent getHurtSound(DamageSource src) { return ModSounds.HOUND_HURT.get(); }
    @Override protected SoundEvent getDeathSound() { return null; }
    @Override protected float getSoundVolume() { return 2.0F; }
    @Override public int getAmbientSoundInterval() { return 140; }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(ModSounds.HOUND_STEP.get(), 0.8F, 0.9F + random.nextFloat() * 0.25F);
    }
}
