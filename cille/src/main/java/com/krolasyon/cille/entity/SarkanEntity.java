package com.krolasyon.cille.entity;

import com.krolasyon.cille.registry.ModItems;
import com.krolasyon.cille.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import java.util.*;

/**
 * Şarkan — the sand and rock warrior Cille. Sand vortex, Atlas attack, Deveran attack, desert storm, sand sea,
 * sand shield (drains whoever hits it), rock throw and sand walls that hem the enemy in; extremely tough, and
 * devastating with its fists up close.
 */
public class SarkanEntity extends CilleEntity {
    public static final int SAND_VORTEX = 1, ATLAS = 2, DEVERAN = 3, STORM = 4, SAND_SEA = 5, SAND_SHIELD = 6, ROCK = 7, WALLS = 8;
    private static final int[] COMMANDABLE = {SAND_VORTEX, ATLAS, DEVERAN, STORM, SAND_SEA, SAND_SHIELD, ROCK, WALLS};
    public static final int SAND = 0xD8B878, DUSK = 0x8A6A40;
    private static final BlockParticleOption SAND_DUST = new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState());
    private static final BlockParticleOption SAND_BLOCK = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState());

    private boolean atlasDone;
    private boolean lastStand;
    private int stormTicks;
    private BlockState heldRock = Blocks.SANDSTONE.defaultBlockState();
    private final Set<Integer> waveHit = new HashSet<>();
    /** temporary sand walls: position -> game time when they crumble */
    private final Map<BlockPos, Long> walls = new HashMap<>();

    public SarkanEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setMaxUpStep(1.5F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 420.0D)
                .add(Attributes.ARMOR, 16.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 6.0D)
                .add(Attributes.ATTACK_DAMAGE, 15.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override public String cilleName() { return "sarkan"; }
    @Override public Item marbleItem() { return ModItems.SARKAN_MISKETI.get(); }
    @Override public int[] commandableAbilities() { return COMMANDABLE; }
    @Override public double meleeReach() { return 3.0; }
    @Override public boolean needsTarget(int id) { return id == SAND_VORTEX || id == ATLAS || id == SAND_SEA || id == ROCK; }
    @Override protected int defendAbility() { return ready(SAND_SHIELD) ? SAND_SHIELD : ready(DEVERAN) ? DEVERAN : -1; }
    @Override protected boolean isDefensive(int id) { return id == SAND_SHIELD || id == DEVERAN; }
    @Override protected boolean facesTarget(int id) { return id != STORM; }

    // ------------------------------------------------------------------ abilities
    @Override
    protected int abilityDuration(int id) {
        return switch (id) {
            case MELEE -> 18;
            case SAND_VORTEX -> 40;
            case ATLAS -> 40;
            case DEVERAN -> 48;
            case STORM -> 44;
            case SAND_SEA -> 40;
            case SAND_SHIELD -> 28;
            case ROCK -> 32;
            case WALLS -> 36;
            default -> 20;
        };
    }

    @Override
    protected int abilityCooldown(int id) {
        return switch (id) {
            case MELEE -> 18;
            case SAND_VORTEX -> 380;
            case ATLAS -> 200;
            case DEVERAN -> 260;
            case STORM -> 700;
            case SAND_SEA -> 320;
            case SAND_SHIELD -> 460;
            case ROCK -> 160;
            case WALLS -> 520;
            default -> 0;
        };
    }

    @Override
    protected int chooseAbility(LivingEntity target, double distSqr) {
        double dist = Math.sqrt(distSqr);
        boolean melee = dist <= meleeReach() + target.getBbWidth() * 0.5 && ready(MELEE);
        if (globalCooldown > 0 || this.tickCount % 5 != 0) return melee ? MELEE : -1;
        if (melee && random.nextFloat() < 0.5F) return MELEE;
        boolean los = this.hasLineOfSight(target);
        int crowd = hostilesAround(target.position(), 6).size();
        int[] opts = new int[10];
        int[] w = new int[10];
        int n = 0, total = 0;
        if (ready(SAND_VORTEX) && dist > 4 && dist < 22) { opts[n] = SAND_VORTEX; w[n] = 2 + crowd; total += w[n++]; }
        if (ready(ATLAS) && dist > 4 && dist < 15 && this.onGround()) { opts[n] = ATLAS; w[n] = 3 + crowd; total += w[n++]; }
        if (ready(DEVERAN) && dist < 5) { opts[n] = DEVERAN; w[n] = 4; total += w[n++]; }
        if (ready(STORM) && stormTicks <= 0 && dist < 14) { opts[n] = STORM; w[n] = 1 + crowd; total += w[n++]; }
        if (ready(SAND_SEA) && dist > 4 && dist < 18 && los) { opts[n] = SAND_SEA; w[n] = 3; total += w[n++]; }
        if (ready(SAND_SHIELD) && getShield() == SHIELD_NONE && (getHealth() < getMaxHealth() * 0.6F || this.tickCount - lastProjectileHit < 80)) { opts[n] = SAND_SHIELD; w[n] = 4; total += w[n++]; }
        if (ready(ROCK) && dist > 6 && dist < 30 && los) { opts[n] = ROCK; w[n] = dist > 12 ? 5 : 2; total += w[n++]; }
        if (ready(WALLS) && dist < 20 && (target.getDeltaMovement().horizontalDistanceSqr() > 0.01 || dist > 10)) { opts[n] = WALLS; w[n] = 2; total += w[n++]; }
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
        atlasDone = false;
        waveHit.clear();
    }

    @Override
    protected void tickAbility(int id, int t, @Nullable LivingEntity target) {
        ServerLevel sl = serverLevel();
        switch (id) {
            case MELEE -> {
                if (t == 6 || t == 11) {
                    sound(ModSounds.PUNCH.get(), 1.6F, 0.9F + random.nextFloat() * 0.1F);
                    punch(sl, (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * (t == 6 ? 1F : 0.8F), 1.1, 0.3);
                }
            }
            case SAND_VORTEX -> {
                if (t == 6) sound(ModSounds.SAND_VORTEX.get(), 3.0F, 1.0F);
                if (t < 22 && t % 2 == 0) {
                    Vec3 h = position().add(forward().scale(1.5)).add(0, 2.2, 0);
                    sl.sendParticles(SAND_DUST, h.x, h.y, h.z, 8, 0.6, 0.6, 0.6, 0.02);
                }
                if (t == 22) VortexEntity.spawn(this, target != null ? target.position() : position().add(forward().scale(8)), VortexEntity.KIND_SAND, isPhase2());
            }
            case ATLAS -> tickAtlas(sl, t, target);
            case DEVERAN -> {
                if (t >= 6 && t <= 34 && t % 3 == 0) {
                    if (target != null && distanceToSqr(target) > 9) {
                        Vec3 d = target.position().subtract(position()).multiply(1, 0, 1).normalize().scale(0.35);
                        this.setDeltaMovement(d.x, getDeltaMovement().y, d.z);
                    }
                    sound(ModSounds.PUNCH.get(), 1.4F, 0.85F + random.nextFloat() * 0.2F);
                    punch(sl, 5.5F, 0.35, 0.1);
                }
                if (t == 40) {
                    sound(ModSounds.ATLAS_SMASH.get(), 1.8F, 1.2F);
                    punch(sl, 12F, 1.4, 1.0);
                }
            }
            case STORM -> {
                if (t == 6) sound(ModSounds.SANDSTORM.get(), 4.0F, 1.0F);
                if (t == 10) {
                    stormTicks = isPhase2() ? 260 : 200;
                    setEmpowered(true);
                }
            }
            case SAND_SEA -> tickSandSea(sl, t, target);
            case SAND_SHIELD -> {
                if (t == 4) sound(ModSounds.SAND_SHIELD.get(), 2.0F, 0.9F);
                if (t == 8) {
                    setShield(SHIELD_SAND);
                    shieldTimer = 240;
                    for (int i = 0; i < 60; i++) {
                        double a = i * Math.PI * 2 / 60;
                        sl.sendParticles(SAND_BLOCK, getX() + Math.cos(a) * 2.2, getY() + 0.3 + (i % 6) * 0.5, getZ() + Math.sin(a) * 2.2, 1, 0, 0.05, 0, 0);
                    }
                }
            }
            case ROCK -> tickRock(sl, t, target);
            case WALLS -> {
                if (t == 6) sound(ModSounds.ARMOUR_STEP.get(), 3.0F, 0.7F);
                if (t == 18) raiseWalls(sl, target != null ? target.position() : position());
            }
            default -> {}
        }
    }

    /** fists in front: everything hostile in the arc gets hit */
    private void punch(ServerLevel sl, float dmg, double knock, double lift) {
        Vec3 c = position().add(forward().scale(2.2)).add(0, 1.6, 0);
        sl.sendParticles(ParticleTypes.CRIT, c.x, c.y, c.z, 8, 0.5, 0.4, 0.5, 0.2);
        sl.sendParticles(SAND_DUST, c.x, c.y, c.z, 6, 0.5, 0.4, 0.5, 0.02);
        for (LivingEntity e : hostilesIn(new AABB(c, c).inflate(2.0, 1.8, 2.0))) hit(e, damageSources().mobAttack(this), dmg, knock, lift);
    }

    private void tickAtlas(ServerLevel sl, int t, @Nullable LivingEntity target) {
        if (t == 2) sound(getAmbientSound(), 2.5F, 0.8F);
        if (t == 8) {
            Vec3 dest = target != null ? target.position() : position().add(forward().scale(6));
            Vec3 d = dest.subtract(position());
            double horiz = Math.min(Math.sqrt(d.x * d.x + d.z * d.z), 13.0);
            Vec3 dir = new Vec3(d.x, 0, d.z);
            dir = dir.lengthSqr() < 1.0E-4 ? forward() : dir.normalize();
            this.setDeltaMovement(dir.x * horiz / 13.0, 1.0, dir.z * horiz / 13.0);
            this.hasImpulse = true;
            sl.sendParticles(SAND_BLOCK, getX(), getY() + 0.2, getZ(), 30, 1.0, 0.1, 1.0, 0.2);
        }
        if (!atlasDone && ((t > 14 && this.onGround()) || t == 26)) {
            atlasDone = true;
            sound(ModSounds.ATLAS_SMASH.get(), 4.0F, 1.0F);
            Vec3 c = position();
            float dmg = (isPhase2() ? 21F : 18F) * power();
            for (LivingEntity e : hostilesAround(c, 5.0)) {
                hit(e, damageSources().mobAttack(this), dmg * (float) (1.0 - 0.35 * e.distanceTo(this) / 5.0), 1.5, 0.8);
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
            }
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y + 0.3, c.z, 1, 0, 0, 0, 0);
            for (int ring = 0; ring < 2; ring++) {
                for (int i = 0; i < 48; i++) {
                    double a = i * Math.PI * 2 / 48;
                    sl.sendParticles(ring == 0 ? SAND_BLOCK : ParticleTypes.CLOUD, c.x + Math.cos(a) * 1.5, c.y + 0.2, c.z + Math.sin(a) * 1.5, 0, Math.cos(a), 0.05, Math.sin(a), 0.6 + ring * 0.3);
                }
            }
            Terrain.crater(sl, this, c, isPhase2() ? 3.6 : 3.0, 14, 3.0F);
        }
    }

    private void tickSandSea(ServerLevel sl, int t, @Nullable LivingEntity target) {
        if (t == 8) sound(ModSounds.SAND_WAVE.get(), 3.0F, 1.0F);
        if (target == null) return;
        Vec3 dir = target.position().subtract(position()).multiply(1, 0, 1);
        double total = dir.length();
        dir = dir.lengthSqr() < 1.0E-4 ? forward() : dir.normalize();
        if (t >= 16 && t <= 32) {
            double along = Math.min(total + 1, 2 + (t - 16) * 1.3);
            Vec3 p = position().add(dir.scale(along));
            Vec3 side = new Vec3(-dir.z, 0, dir.x);
            for (int k = -2; k <= 2; k++) {
                Vec3 q = p.add(side.scale(k * 0.9));
                sl.sendParticles(SAND_BLOCK, q.x, getY() + 0.5, q.z, 6, 0.3, 0.6, 0.3, 0.15);
                sl.sendParticles(SAND_DUST, q.x, getY() + 1.4, q.z, 3, 0.3, 0.6, 0.3, 0.02);
            }
            for (LivingEntity e : hostilesAround(p.add(0, 0.8, 0), 2.6)) {
                if (!waveHit.add(e.getId())) continue;
                hit(e, damageSources().mobAttack(this), 8F * power(), 0.8, 0.6);
                e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
            }
        }
        if (t == 32 && target.isAlive() && target.distanceToSqr(this) < 22 * 22) bury(sl, target);
    }

    /** buries the target in sand up to the waist */
    private void bury(ServerLevel sl, LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 6));
        target.addEffect(new MobEffectInstance(MobEffects.JUMP, 100, 128));       // cannot jump out
        target.hurt(damageSources().mobAttack(this), 6F * power());
        sl.sendParticles(SAND_BLOCK, target.getX(), target.getY() + 0.5, target.getZ(), 60, 0.8, 0.5, 0.8, 0.2);
        if (!Terrain.allowed(sl, this)) return;
        BlockPos b = target.blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(b.offset(-1, 0, -1), b.offset(1, 0, 1))) {
            BlockState st = sl.getBlockState(p);
            if (st.isAir() || st.canBeReplaced()) {
                if (!p.equals(b) && sl.getBlockState(p.below()).isFaceSturdy(sl, p.below(), Direction.UP)) sl.setBlockAndUpdate(p, Blocks.SAND.defaultBlockState());
            }
        }
        BlockPos below = b.below();
        if (sl.getBlockState(below).isFaceSturdy(sl, below, Direction.UP) && (sl.getBlockState(b).isAir() || sl.getBlockState(b).canBeReplaced())) {
            // fill the target's own block too: they are stuck waist deep (the game pushes them up slowly)
            sl.setBlockAndUpdate(b, Blocks.SAND.defaultBlockState());
        }
    }

    private void tickRock(ServerLevel sl, int t, @Nullable LivingEntity target) {
        if (t == 8) {
            sound(ModSounds.ROCK_RIP.get(), 2.5F, 1.0F);
            heldRock = Blocks.SANDSTONE.defaultBlockState();
            BlockPos g = BlockPos.containing(position().add(forward().scale(1.8))).below();
            BlockState st = sl.getBlockState(g);
            if (Terrain.allowed(sl, this) && Terrain.breakable(sl, g, st, 3.0F) && st.isSolidRender(sl, g)) {
                heldRock = st;
                sl.destroyBlock(g, false, this);
            }
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, heldRock), getX(), getY() + 0.4, getZ(), 40, 1.0, 0.3, 1.0, 0.2);
        }
        if (t > 8 && t < 20) {
            Vec3 over = position().add(0, 4.0, 0);
            sl.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, heldRock), over.x, over.y, over.z, 2, 0.4, 0.2, 0.4, 0);
        }
        if (t == 20) {
            Vec3 from = position().add(forward().scale(1.2)).add(0, 3.8, 0);
            Vec3 aim = target != null ? target.getBoundingBox().getCenter().add(target.getDeltaMovement().scale(8)) : from.add(forward().scale(12));
            Vec3 d = aim.subtract(from);
            double flat = Math.sqrt(d.x * d.x + d.z * d.z);
            Vec3 dir = d.add(0, flat * 0.18, 0);
            CilleProjectileEntity.launchRock(this, heldRock, from, dir, 1.35 + flat * 0.01, target);
            sound(ModSounds.RUSH_DASH.get(), 1.8F, 0.8F);
        }
    }

    /** a ring of sandstone walls around the enemy: it cannot run away */
    private void raiseWalls(ServerLevel sl, Vec3 c) {
        sound(ModSounds.EARTH_RISE.get(), 3.5F, 1.0F);
        if (!Terrain.allowed(sl, this)) {
            for (LivingEntity e : hostilesAround(c, 5)) e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 3));
            return;
        }
        double r = 4.5;
        long until = sl.getGameTime() + 300;
        int steps = 36;
        for (int i = 0; i < steps; i++) {
            double a = i * Math.PI * 2 / steps;
            Double gy = ShadowSpikeEntity.findGround(sl, c.x + Math.cos(a) * r, c.z + Math.sin(a) * r, c.y + 4, c.y - 6);
            if (gy == null) continue;
            BlockPos base = BlockPos.containing(c.x + Math.cos(a) * r, gy, c.z + Math.sin(a) * r);
            for (int h = 0; h < 3; h++) {
                BlockPos p = base.above(h);
                BlockState st = sl.getBlockState(p);
                if (st.isAir() || st.canBeReplaced()) {
                    sl.setBlockAndUpdate(p, Blocks.SANDSTONE.defaultBlockState());
                    walls.put(p.immutable(), until);
                }
            }
            sl.sendParticles(SAND_BLOCK, base.getX() + 0.5, base.getY() + 1.5, base.getZ() + 0.5, 6, 0.3, 0.8, 0.3, 0.1);
        }
    }

    private void crumbleWalls(ServerLevel sl, boolean all) {
        if (walls.isEmpty()) return;
        long now = sl.getGameTime();
        Iterator<Map.Entry<BlockPos, Long>> it = walls.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, Long> e = it.next();
            if (!all && e.getValue() > now) continue;
            if (sl.getBlockState(e.getKey()).is(Blocks.SANDSTONE)) {
                sl.removeBlock(e.getKey(), false);
                sl.sendParticles(SAND_BLOCK, e.getKey().getX() + 0.5, e.getKey().getY() + 0.5, e.getKey().getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0.05);
            }
            it.remove();
        }
    }

    // ------------------------------------------------------------------ toughness, instincts
    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (!this.level().isClientSide() && !src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            // high endurance: very heavy blows are blunted, and the Deveran stance guards as it strikes
            if (amount > 10F) amount = 10F + (amount - 10F) * 0.5F;
            if (currentAbility == DEVERAN) amount *= 0.5F;
        }
        boolean r = super.hurt(src, amount);
        if (r && !lastStand && this.isAlive() && getHealth() < getMaxHealth() * 0.25F) {
            lastStand = true;
            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 240, 1));
            this.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 400, 3));
            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 400, 1));
            sound(getAmbientSound(), 3.0F, 0.7F);
            serverLevel().sendParticles(SAND_BLOCK, getX(), getY() + 1.5, getZ(), 80, 1.5, 1.5, 1.5, 0.3);
        }
        return r;
    }

    @Override
    protected void instinct(DamageSource src, @Nullable LivingEntity la) {
        if (isCasting() && currentAbility != MELEE) return;
        Entity direct = src.getDirectEntity();
        if (getShield() == SHIELD_NONE && ready(SAND_SHIELD) && (getHealth() < getMaxHealth() * 0.5F || direct instanceof Projectile) && random.nextFloat() < 0.5F) {
            startAbility(SAND_SHIELD, la);
        } else if (la != null && direct == la && ready(DEVERAN) && distanceToSqr(la) < 16 && getStance() != STANCE_PASSIVE && random.nextFloat() < 0.25F) {
            startAbility(DEVERAN, la);
        } else {
            counterMelee(la, src);
        }
    }

    @Override
    protected boolean reactToProjectile(Projectile p) {
        if (getShield() == SHIELD_NONE && ready(SAND_SHIELD) && random.nextFloat() < 0.4F) {
            startAbility(SAND_SHIELD, p.getOwner() instanceof LivingEntity l ? l : null);
            return true;
        }
        return false;
    }

    @Override
    protected void releaseTick(ServerLevel sl, int t) {
        if (t == 2) sound(ModSounds.ATLAS_SMASH.get(), 1.5F, 1.1F);
        if (t == 16) {
            sound(getAmbientSound(), 4.0F, 0.85F);
            sl.sendParticles(SAND_BLOCK, getX(), getY() + 0.3, getZ(), 120, 3.0, 0.3, 3.0, 0.3);
            sl.sendParticles(SAND_DUST, getX(), getY() + 1.5, getZ(), 60, 2.5, 1.5, 2.5, 0.05);
            for (LivingEntity e : hostilesAround(position(), 7)) {
                if (e instanceof CilleEntity) continue;
                knock(e, 1.2, 0.4);
            }
        }
    }

    @Override
    protected void onPhase2(ServerLevel sl) {
        sound(getAmbientSound(), 4.0F, 0.75F);
        sl.sendParticles(SAND_BLOCK, getX(), getY() + 0.3, getZ(), 100, 3.0, 0.3, 3.0, 0.3);
        for (LivingEntity e : hostilesAround(position(), 7)) knock(e, 1.5, 0.5);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        ServerLevel sl = serverLevel();
        if (this.tickCount % 20 == 0) crumbleWalls(sl, false);
        if (stormTicks > 0) {
            stormTicks--;
            if (stormTicks % 60 == 0) sound(ModSounds.SANDSTORM.get(), 3.0F, 0.95F);
            if (stormTicks % 5 == 0) {
                double r = isPhase2() ? 15 : 12;
                for (LivingEntity e : hostilesAround(position(), r)) {
                    e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
                    if (stormTicks % 20 == 0) e.hurt(damageSources().indirectMagic(this, this), 1.5F);
                    // the wind drags enemies around Şarkan
                    Vec3 d = e.position().subtract(position()).multiply(1, 0, 1);
                    if (d.lengthSqr() > 1.0E-3) {
                        Vec3 tan = new Vec3(-d.z, 0, d.x).normalize().scale(0.18);
                        e.setDeltaMovement(e.getDeltaMovement().add(tan));
                        e.hurtMarked = true;
                    }
                }
                // and pushes away projectiles flying through it
                for (Projectile p : sl.getEntitiesOfClass(Projectile.class, this.getBoundingBox().inflate(r), pr -> pr.getOwner() != this && !(pr instanceof MarbleEntity))) {
                    p.setDeltaMovement(p.getDeltaMovement().scale(0.6).add((random.nextDouble() - 0.5) * 0.3, 0, (random.nextDouble() - 0.5) * 0.3));
                    p.hurtMarked = true;
                }
            }
            if (stormTicks == 0) setEmpowered(false);
        }
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (!this.level().isClientSide()) crumbleWalls(serverLevel(), true);
        super.remove(reason);
    }

    // ------------------------------------------------------------------ client fx
    @Override
    protected void clientFx() {
        Level l = this.level();
        if (getDeltaMovement().horizontalDistanceSqr() > 0.002 && random.nextInt(2) == 0)
            l.addParticle(SAND_DUST, getX() + (random.nextDouble() - 0.5) * 1.2, getY() + 0.1, getZ() + (random.nextDouble() - 0.5) * 1.2, 0, 0, 0);
        if (isEmpowered()) {
            double r = isPhase2() ? 15 : 12;
            for (int i = 0; i < 14; i++) {
                double a = random.nextDouble() * Math.PI * 2, d = random.nextDouble() * r;
                l.addParticle(i % 4 == 0 ? ParticleTypes.CLOUD : SAND_DUST, getX() + Math.cos(a) * d, getY() + random.nextDouble() * 5, getZ() + Math.sin(a) * d,
                        -Math.sin(a) * 0.4, 0.02, Math.cos(a) * 0.4);
            }
        }
        if (getShield() == SHIELD_SAND && random.nextInt(2) == 0) {
            double a = random.nextDouble() * Math.PI * 2;
            l.addParticle(SAND_DUST, getX() + Math.cos(a) * 2.4, getY() + random.nextDouble() * 3.4, getZ() + Math.sin(a) * 2.4, 0, 0, 0);
        }
    }

    @Override
    public Vec3 mouthPos() { return position().add(forward().scale(0.4)).add(0, 3.0, 0); }

    @Override protected SoundEvent getAmbientSound() { return ModSounds.SARKAN_AMBIENT.get(); }
    @Override protected SoundEvent getHurtSound(DamageSource src) { return ModSounds.SARKAN_HURT.get(); }
    @Override protected SoundEvent getDeathSoundEvent() { return ModSounds.SARKAN_DEATH.get(); }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(ModSounds.ARMOUR_STEP.get(), 0.9F, 0.85F + random.nextFloat() * 0.2F);
    }
}
