package com.krolasyon.cille.entity;

import com.krolasyon.cille.registry.ModEntities;
import com.krolasyon.cille.registry.ModItems;
import com.krolasyon.cille.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Behmud — the cosmic flying whale Cille of water, ice and space.
 * Ice beam, whirlpool, sound wave, exploding fish, torpedoes, quick attack, waterspout, ice shield, quick defence,
 * the secret Erkemas clamp, its sealed power and the hidden power of space.
 */
public class BehmudEntity extends CilleEntity {
    public static final int ICE_BEAM = 1, WHIRLPOOL = 2, SONIC = 3, FISH_BOMB = 4, TORPEDO = 5, RUSH = 6, WATERSPOUT = 7,
            ICE_SHIELD = 8, QUICK_GUARD = 9, ERKEMAS = 10, SEAL = 11, SPACE = 12;
    private static final int[] COMMANDABLE = {ICE_BEAM, WHIRLPOOL, SONIC, FISH_BOMB, TORPEDO, RUSH, WATERSPOUT, ICE_SHIELD, QUICK_GUARD, ERKEMAS, SEAL, SPACE};
    public static final int ICE = 0x9FE8FF, WATER = 0x3C8CD8, DEEP = 0x0A1430, STAR = 0xE8F0FF;

    private int empoweredTicks;
    @Nullable private LivingEntity clamped;
    private boolean dashHit;

    public BehmudEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 12, true);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 340.0D)
                .add(Attributes.ARMOR, 10.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 3.0D)
                .add(Attributes.ATTACK_DAMAGE, 13.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.FLYING_SPEED, 0.55D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9D);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        nav.setCanPassDoors(false);
        return nav;
    }

    // ------------------------------------------------------------------ definition
    @Override public String cilleName() { return "behmud"; }
    @Override public Item marbleItem() { return ModItems.BEHMUD_MISKETI.get(); }
    @Override public int[] commandableAbilities() { return COMMANDABLE; }
    @Override public double meleeReach() { return 4.6; }
    @Override public boolean needsTarget(int id) {
        return id == ICE_BEAM || id == WHIRLPOOL || id == FISH_BOMB || id == TORPEDO || id == RUSH || id == WATERSPOUT || id == ERKEMAS || id == SPACE;
    }
    @Override protected int defendAbility() { return ready(ICE_SHIELD) ? ICE_SHIELD : ready(QUICK_GUARD) ? QUICK_GUARD : -1; }
    @Override protected boolean isDefensive(int id) { return id == ICE_SHIELD || id == QUICK_GUARD; }
    @Override protected boolean facesTarget(int id) { return id != WHIRLPOOL || abilityTick > 30; }
    @Override protected boolean holdsStillWhileCasting() { return currentAbility != RUSH && currentAbility != ERKEMAS && currentAbility != QUICK_GUARD; }
    @Override protected boolean canUseNow(int id) { return id != SEAL || !isEmpowered(); }
    @Override protected float cooldownScale() { return (isEmpowered() ? 0.5F : 1F) * (isPhase2() ? 0.75F : 1F); }

    /** damage multiplier: sealed power unleashed */
    @Override
    public float power() { return isEmpowered() ? 1.6F : 1F; }

    // ------------------------------------------------------------------ flight
    @Override
    public void travelTo(double x, double y, double z, double speed) {
        this.getNavigation().stop();
        this.getMoveControl().setWantedPosition(x, y, z, speed);
    }

    @Override
    public void travelTo(Entity e, double speed) { travelTo(e.getX(), e.getY() + 3, e.getZ(), speed); }

    @Override
    public void stopTravel() {
        this.getNavigation().stop();
        this.getMoveControl().setWantedPosition(getX(), getY(), getZ(), 0.0);
    }

    @Override
    public boolean isTravelling() { return this.getMoveControl().hasWanted() && this.getDeltaMovement().lengthSqr() > 0.003; }

    @Override
    protected Vec3 chasePoint(LivingEntity t) {
        Vec3 away = position().subtract(t.position()).multiply(1, 0, 1);
        away = away.lengthSqr() < 1.0E-4 ? forward().scale(-1) : away.normalize();
        return t.position().add(away.scale(3.0)).add(0, 1.2 + t.getBbHeight() * 0.3, 0);
    }

    @Override
    protected Vec3 followPoint(Player o) { return o.position().add(o.getLookAngle().multiply(-1, 0, -1).normalize().scale(4)).add(0, 4.0, 0); }

    @Override
    protected Goal wanderGoal() {
        return new WaterAvoidingRandomFlyingGoal(this, 0.8D) {
            @Override
            public boolean canUse() { return !isOwned() && super.canUse(); }
        };
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {}

    // ------------------------------------------------------------------ abilities
    @Override
    protected int abilityDuration(int id) {
        return switch (id) {
            case MELEE -> 18;
            case ICE_BEAM -> 52;
            case WHIRLPOOL -> 44;
            case SONIC -> 36;
            case FISH_BOMB -> 36;
            case TORPEDO -> 30;
            case RUSH -> 40;
            case WATERSPOUT -> 40;
            case ICE_SHIELD -> 32;
            case QUICK_GUARD -> 16;
            case ERKEMAS -> 48;
            case SEAL -> 60;
            case SPACE -> 52;
            default -> 20;
        };
    }

    @Override
    protected int abilityCooldown(int id) {
        return switch (id) {
            case MELEE -> 20;
            case ICE_BEAM -> 260;
            case WHIRLPOOL -> 380;
            case SONIC -> 240;
            case FISH_BOMB -> 180;
            case TORPEDO -> 200;
            case RUSH -> 160;
            case WATERSPOUT -> 400;
            case ICE_SHIELD -> 480;
            case QUICK_GUARD -> 80;
            case ERKEMAS -> 420;
            case SEAL -> 2400;
            case SPACE -> 600;
            default -> 0;
        };
    }

    @Override
    protected int chooseAbility(LivingEntity target, double distSqr) {
        double dist = Math.sqrt(distSqr);
        boolean melee = dist <= meleeReach() + target.getBbWidth() * 0.5 && ready(MELEE);
        if (globalCooldown > 0 || this.tickCount % 5 != 0) return melee ? MELEE : -1;
        if (!isEmpowered() && ready(SEAL) && getHealth() < getMaxHealth() * 0.35F) return SEAL;
        if (melee && random.nextFloat() < 0.4F) return MELEE;
        boolean los = this.hasLineOfSight(target);
        int crowd = hostilesAround(target.position(), 6).size();
        int[] opts = new int[14];
        int[] w = new int[14];
        int n = 0, total = 0;
        if (ready(ICE_BEAM) && dist > 4 && dist < 22 && los) { opts[n] = ICE_BEAM; w[n] = 3; total += w[n++]; }
        if (ready(WHIRLPOOL) && dist > 5 && dist < 24) { opts[n] = WHIRLPOOL; w[n] = 2 + crowd; total += w[n++]; }
        if (ready(SONIC) && dist < 11) { opts[n] = SONIC; w[n] = 2 + crowd + (target instanceof CilleEntity c && c.isCasting() ? 4 : 0); total += w[n++]; }
        if (ready(FISH_BOMB) && dist > 6 && dist < 26) { opts[n] = FISH_BOMB; w[n] = 3; total += w[n++]; }
        if (ready(TORPEDO) && dist > 8 && dist < 32 && los) { opts[n] = TORPEDO; w[n] = 3; total += w[n++]; }
        if (ready(RUSH) && dist > 5 && dist < 20) { opts[n] = RUSH; w[n] = 3; total += w[n++]; }
        if (ready(WATERSPOUT) && dist > 5 && dist < 24) { opts[n] = WATERSPOUT; w[n] = 2 + crowd; total += w[n++]; }
        if (ready(ICE_SHIELD) && getShield() == SHIELD_NONE && (getHealth() < getMaxHealth() * 0.6F || this.tickCount - lastProjectileHit < 80)) { opts[n] = ICE_SHIELD; w[n] = 4; total += w[n++]; }
        if (ready(ERKEMAS) && dist < 12 && target.getBbHeight() < 4) { opts[n] = ERKEMAS; w[n] = 2; total += w[n++]; }
        if (ready(SPACE) && dist < 28) { opts[n] = SPACE; w[n] = (isEmpowered() ? 4 : 1) + crowd; total += w[n++]; }
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
        dashHit = false;
        if (id != ERKEMAS) releaseClamp(false);
    }

    @Override
    protected void onAbilityEnd(int id) {
        if (id == ERKEMAS) releaseClamp(true);
    }

    @Override
    protected void tickAbility(int id, int t, @Nullable LivingEntity target) {
        ServerLevel sl = serverLevel();
        switch (id) {
            case MELEE -> {
                if (t == 4) sound(ModSounds.RUSH_DASH.get(), 1.4F, 0.9F);
                if (t == 9) {
                    Vec3 c = position().add(forward().scale(3.2)).add(0, 0.8, 0);
                    sl.sendParticles(ParticleTypes.SPLASH, c.x, c.y, c.z, 40, 1.5, 0.6, 1.5, 0.3);
                    sl.sendParticles(ParticleTypes.SWEEP_ATTACK, c.x, c.y, c.z, 2, 0.8, 0.2, 0.8, 0);
                    for (LivingEntity e : hostilesIn(new AABB(c, c).inflate(3.2, 2.0, 3.2)))
                        hit(e, damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * power(), 1.3, 0.4);
                }
            }
            case ICE_BEAM -> tickIceBeam(sl, t, target);
            case WHIRLPOOL -> {
                if (t == 6) sound(ModSounds.WHIRLPOOL.get(), 3.0F, 1.0F);
                if (t == 16) VortexEntity.spawn(this, target != null ? target.position() : position().add(forward().scale(8)), VortexEntity.KIND_WHIRLPOOL, isEmpowered());
            }
            case SONIC -> {
                if (t == 6) sound(ModSounds.SONIC_WAVE.get(), 4.0F, 1.0F);
                if (t == 12 || t == 19 || t == 26) sonicPulse(sl, t);
            }
            case FISH_BOMB -> {
                if ((t == 9 || t == 16 || t == 23) && target != null) {
                    int count = isEmpowered() ? 3 : 2;
                    for (int i = 0; i < count; i++) {
                        Vec3 from = mouthPos();
                        Vec3 aim = target.getBoundingBox().getCenter().subtract(from);
                        Vec3 dir = aim.normalize().yRot((i - (count - 1) / 2F) * 0.35F).add(0, 0.35, 0);
                        CilleProjectileEntity.launch(this, CilleProjectileEntity.KIND_FISH, from, dir, 0.75, target);
                    }
                    sound(ModSounds.FISH_LAUNCH.get(), 1.6F, 0.85F + random.nextFloat() * 0.15F);
                    sl.sendParticles(ParticleTypes.SPLASH, mouthPos().x, mouthPos().y, mouthPos().z, 20, 0.4, 0.3, 0.4, 0.2);
                }
            }
            case TORPEDO -> {
                if ((t == 10 || t == 17 || t == 23) && target != null) {
                    Vec3 from = mouthPos();
                    Vec3 aim = target.getBoundingBox().getCenter().add(target.getDeltaMovement().scale(6)).subtract(from);
                    CilleProjectileEntity.launch(this, CilleProjectileEntity.KIND_TORPEDO, from, aim, 1.6, target);
                    sound(ModSounds.TORPEDO.get(), 2.0F, 0.9F);
                    sl.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, from.x, from.y, from.z, 20, 0.3, 0.3, 0.3, 0.1);
                }
            }
            case RUSH -> tickRush(sl, t, target);
            case WATERSPOUT -> {
                if (t == 8) sound(ModSounds.WATERSPOUT.get(), 3.5F, 1.0F);
                if (t == 16) VortexEntity.spawn(this, target != null ? target.position() : position().add(forward().scale(8)), VortexEntity.KIND_SPOUT, isEmpowered());
            }
            case ICE_SHIELD -> {
                if (t == 4) sound(ModSounds.ICE_FORM.get(), 2.5F, 1.0F);
                if (t == 8) {
                    setShield(SHIELD_ICE);
                    shieldHealth = isEmpowered() ? 110F : 70F;
                    shieldTimer = 260;
                    for (int i = 0; i < 70; i++) {
                        double a = i * Math.PI * 2 / 70;
                        sl.sendParticles(ParticleTypes.SNOWFLAKE, getX() + Math.cos(a) * 3.2, getY() + 1.0 + (i % 5) * 0.4, getZ() + Math.sin(a) * 3.2, 1, 0, 0.05, 0, 0);
                    }
                }
            }
            case QUICK_GUARD -> {
                if (t == 1) {
                    Vec3 side = right().scale(random.nextBoolean() ? 1.1 : -1.1);
                    this.setDeltaMovement(side.x, 0.25, side.z);
                    this.hasImpulse = true;
                    sound(ModSounds.RUSH_DASH.get(), 1.4F, 1.0F);
                }
                if (t == 8) {
                    // counter: tail swat on everything that was pressing in
                    sl.sendParticles(ParticleTypes.SPLASH, getX(), getY() + 1, getZ(), 60, 2.5, 0.8, 2.5, 0.3);
                    for (LivingEntity e : hostilesAround(position().add(0, 1, 0), 5.5))
                        hit(e, damageSources().mobAttack(this), 8F * power(), 1.6, 0.5);
                }
            }
            case ERKEMAS -> tickErkemas(sl, t, target);
            case SEAL -> tickSeal(sl, t);
            case SPACE -> tickSpace(sl, t, target);
            default -> {}
        }
    }

    private void tickIceBeam(ServerLevel sl, int t, @Nullable LivingEntity target) {
        if (t == 4) sound(ModSounds.ICE_BEAM.get(), 3.0F, 1.0F);
        if (target == null || !target.isAlive() || distanceToSqr(target) > 26 * 26 || !this.hasLineOfSight(target)) {
            if (t >= 12) endAbility();
            return;
        }
        if (t == 12) setBeam(target.getId());
        if (t >= 12 && t <= 44 && t % 4 == 0) {
            Vec3 hitPos = target.position();
            for (LivingEntity e : hostilesIn(target.getBoundingBox().inflate(1.5))) {
                e.hurt(damageSources().indirectMagic(this, this), 3.2F * power());
                e.setTicksFrozen(Math.min(e.getTicksRequiredToFreeze() + 140, e.getTicksFrozen() + 30));
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3));
                e.clearFire();
            }
            Vec3 m = target.getBoundingBox().getCenter();
            sl.sendParticles(ParticleTypes.SNOWFLAKE, m.x, m.y, m.z, 14, 0.5, 0.6, 0.5, 0.05);
            sl.sendParticles(ParticleTypes.ITEM_SNOWBALL, m.x, m.y, m.z, 6, 0.4, 0.4, 0.4, 0.1);
            freezeGround(sl, hitPos, 2);
        }
        if (t == 45) setBeam(-1);
    }

    /** freezes water into ice and dusts the ground with snow around a point */
    private void freezeGround(ServerLevel sl, Vec3 at, int r) {
        if (!Terrain.allowed(sl, this)) return;
        BlockPos c = BlockPos.containing(at);
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-r, -2, -r), c.offset(r, 1, r))) {
            if (p.distSqr(c) > r * r + 1) continue;
            BlockState st = sl.getBlockState(p);
            if (st.is(Blocks.WATER) && st.getFluidState().isSource()) {
                sl.setBlockAndUpdate(p, Blocks.ICE.defaultBlockState());
            } else if (st.is(Blocks.LAVA) && st.getFluidState().isSource()) {
                sl.setBlockAndUpdate(p, Blocks.OBSIDIAN.defaultBlockState());
            } else if (st.isAir() && random.nextFloat() < 0.5F) {
                BlockState below = sl.getBlockState(p.below());
                if (below.isFaceSturdy(sl, p.below(), net.minecraft.core.Direction.UP) && !below.is(Blocks.ICE))
                    sl.setBlockAndUpdate(p, Blocks.SNOW.defaultBlockState());
            } else if (st.is(Blocks.FIRE)) {
                sl.removeBlock(p, false);
            }
        }
    }

    private void sonicPulse(ServerLevel sl, int t) {
        Vec3 c = mouthPos();
        double r = isEmpowered() ? 13 : 10;
        sl.sendParticles(ParticleTypes.SONIC_BOOM, c.x, c.y, c.z, 1, 0, 0, 0, 0);
        for (int i = 0; i < 48; i++) {
            double a = i * Math.PI * 2 / 48;
            sl.sendParticles(ParticleTypes.CLOUD, c.x, c.y, c.z, 0, Math.cos(a), 0.02, Math.sin(a), 0.9);
        }
        for (LivingEntity e : hostilesAround(c, r)) {
            float dmg = (5.5F - (float) e.distanceTo(this) * 0.2F) * power();
            hit(e, damageSources().sonicBoom(this), Math.max(2F, dmg), 1.3, 0.35);
            // interrupts their attacks
            if (e instanceof CilleEntity cille) cille.interrupt();
            if (e instanceof Mob m) {
                m.getNavigation().stop();
                m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3));
            }
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
            if (e instanceof Player) e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
            e.stopUsingItem();
        }
        // the sound shatters glass and ice nearby
        if (t == 12 && Terrain.allowed(sl, this)) {
            BlockPos b = BlockPos.containing(c);
            int R = 6;
            for (BlockPos p : BlockPos.betweenClosed(b.offset(-R, -R, -R), b.offset(R, R, R))) {
                if (p.distSqr(b) > R * R) continue;
                BlockState st = sl.getBlockState(p);
                if ((st.is(BlockTags.IMPERMEABLE) || st.is(BlockTags.ICE) || st.is(Blocks.GLASS_PANE)) && !st.hasBlockEntity())
                    sl.destroyBlock(p, false, this);
            }
        }
    }

    private void tickRush(ServerLevel sl, int t, @Nullable LivingEntity target) {
        int phase = t % 12;
        if (phase == 2 && target != null) {
            Vec3 d = target.getBoundingBox().getCenter().subtract(position().add(0, 1, 0));
            d = d.lengthSqr() < 1.0E-4 ? forward() : d.normalize();
            double sp = isEmpowered() ? 2.0 : 1.6;
            this.setDeltaMovement(d.scale(sp));
            this.hasImpulse = true;
            dashHit = false;
            faceTowards(target);
            sound(ModSounds.RUSH_DASH.get(), 2.0F, 0.9F + random.nextFloat() * 0.15F);
        }
        if (phase >= 2 && phase <= 8) {
            sl.sendParticles(ParticleTypes.SPLASH, getX(), getY() + 1, getZ(), 12, 1.2, 0.6, 1.2, 0.2);
            sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1, getZ(), 2, 0.6, 0.4, 0.6, 0.02);
            if (!dashHit) {
                for (LivingEntity e : hostilesIn(this.getBoundingBox().inflate(1.2))) {
                    hit(e, damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.9F * power(), 1.6, 0.6);
                    dashHit = true;
                }
            }
        }
        if (phase == 9) this.setDeltaMovement(this.getDeltaMovement().scale(0.2));
    }

    private void tickErkemas(ServerLevel sl, int t, @Nullable LivingEntity target) {
        if (t == 2) sound(getAmbientSound(), 3.0F, 0.75F);
        if (t == 7 && target != null) {
            Vec3 d = target.getBoundingBox().getCenter().subtract(position().add(0, 1, 0));
            d = d.lengthSqr() < 1.0E-4 ? forward() : d.normalize();
            this.setDeltaMovement(d.scale(1.5));
            this.hasImpulse = true;
        }
        if (t == 11 && target != null && target.isAlive() && target.distanceToSqr(mouthPos()) < 6 * 6 && isHostileTo(target)) {
            clamped = target;
            sound(ModSounds.CLAMP_BITE.get(), 2.5F, 0.9F);
            this.setDeltaMovement(Vec3.ZERO);
        }
        if (clamped != null) {
            if (!clamped.isAlive() || clamped.isRemoved()) {
                clamped = null;
                return;
            }
            Vec3 m = mouthPos();
            clamped.teleportTo(m.x, m.y - clamped.getBbHeight() * 0.4, m.z);
            clamped.setDeltaMovement(Vec3.ZERO);
            clamped.fallDistance = 0;
            if (t % 5 == 0) {
                clamped.hurt(damageSources().mobAttack(this), 4.5F * power());
                clamped.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 4));
                sl.sendParticles(ParticleTypes.CRIT, m.x, m.y, m.z, 10, 0.4, 0.4, 0.4, 0.2);
                sl.sendParticles(ParticleTypes.SPLASH, m.x, m.y, m.z, 10, 0.4, 0.3, 0.4, 0.1);
            }
            if (t == 38) {
                LivingEntity c = clamped;
                clamped = null;
                c.hurt(damageSources().mobAttack(this), 7F * power());
                Vec3 fl = forward().scale(1.6).add(0, 1.1, 0);
                c.setDeltaMovement(fl);
                c.hurtMarked = true;
                sound(ModSounds.WATER_BOOM.get(), 1.8F, 1.0F);
            }
        }
    }

    private void releaseClamp(boolean fling) {
        if (clamped == null) return;
        if (fling && clamped.isAlive()) {
            clamped.setDeltaMovement(forward().scale(0.8).add(0, 0.6, 0));
            clamped.hurtMarked = true;
        }
        clamped = null;
    }

    private void tickSeal(ServerLevel sl, int t) {
        if (t == 0) sound(ModSounds.SEAL_RELEASE.get(), 5.0F, 1.0F);
        if (t < 38 && t % 2 == 0) {
            sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.4, getZ(), 6, 2.5, 1.5, 2.5, 0.0);
            sl.sendParticles(dust(ICE, 2.0F), getX(), getY() + 1.4, getZ(), 6, 2.0, 1.2, 2.0, 0.0);
        }
        if (t == 38) {
            setEmpowered(true);
            empoweredTicks = 600;
            this.heal(getMaxHealth() * 0.3F);
            this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 1));
            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 0));
            this.getAttribute(Attributes.FLYING_SPEED).setBaseValue(0.8D);
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 1.4, getZ(), 2, 0.5, 0.5, 0.5, 0);
            sl.sendParticles(ParticleTypes.FLASH, getX(), getY() + 1.4, getZ(), 2, 0, 0, 0, 0);
            for (int i = 0; i < 90; i++) {
                double a = i * Math.PI * 2 / 90;
                sl.sendParticles(i % 2 == 0 ? ParticleTypes.END_ROD : dust(ICE, 2.6F), getX(), getY() + 1.2, getZ(), 0, Math.cos(a), 0.1, Math.sin(a), 1.1);
            }
            for (LivingEntity e : hostilesAround(position(), 9)) hit(e, damageSources().indirectMagic(this, this), 8F, 2.0, 0.7);
        }
    }

    private void tickSpace(ServerLevel sl, int t, @Nullable LivingEntity target) {
        if (t == 4) sound(ModSounds.SPACE_POWER.get(), 4.0F, 1.0F);
        Vec3 c = target != null ? target.position() : position().add(forward().scale(8));
        if (t == 12) {
            // space bends: enemies around lose their weight and float helplessly
            for (LivingEntity e : hostilesAround(c, 9)) {
                e.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 40, 1));
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
            }
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, c.x, c.y + 1, c.z, 120, 4.0, 1.5, 4.0, 0.1);
        }
        if (t >= 16 && t <= 46 && t % 3 == 1) {
            double ox = (random.nextDouble() - 0.5) * 10, oz = (random.nextDouble() - 0.5) * 10;
            if (random.nextFloat() < 0.4F && target != null) { ox *= 0.25; oz *= 0.25; }
            Vec3 from = new Vec3(c.x + ox, c.y + 16 + random.nextDouble() * 4, c.z + oz);
            Vec3 dir = new Vec3((random.nextDouble() - 0.5) * 0.2, -1, (random.nextDouble() - 0.5) * 0.2);
            CilleProjectileEntity.launch(this, CilleProjectileEntity.KIND_STAR, from, dir, 1.4, target);
        }
    }

    @Override
    protected void instinct(DamageSource src, @Nullable LivingEntity la) {
        if (isCasting() && currentAbility != MELEE) return;
        Entity direct = src.getDirectEntity();
        if (!isEmpowered() && ready(SEAL) && getHealth() < getMaxHealth() * 0.35F) {
            startAbility(SEAL, la);
        } else if (getHealth() < getMaxHealth() * 0.45F && ready(ICE_SHIELD) && getShield() == SHIELD_NONE && random.nextFloat() < 0.6F) {
            startAbility(ICE_SHIELD, la);
        } else if (ready(QUICK_GUARD) && random.nextFloat() < (direct instanceof Projectile ? 0.6F : 0.4F)) {
            startAbility(QUICK_GUARD, la);
        } else {
            counterMelee(la, src);
        }
    }

    @Override
    protected boolean reactToProjectile(Projectile p) {
        if (ready(QUICK_GUARD)) {
            startAbility(QUICK_GUARD, p.getOwner() instanceof LivingEntity l ? l : null);
            return true;
        }
        if (ready(ICE_SHIELD) && getShield() == SHIELD_NONE && random.nextFloat() < 0.3F) {
            startAbility(ICE_SHIELD, null);
            return true;
        }
        return false;
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        // quick defence: the first moments of the roll are a perfect dodge
        if (!this.level().isClientSide() && currentAbility == QUICK_GUARD && abilityTick < 9 && !src.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            serverLevel().sendParticles(ParticleTypes.SPLASH, getX(), getY() + 1, getZ(), 20, 1.0, 0.5, 1.0, 0.2);
            return false;
        }
        return super.hurt(src, amount);
    }

    @Override
    protected void releaseTick(ServerLevel sl, int t) {
        if (t == 2) sound(ModSounds.MARBLE_BURST.get(), 2.0F, 0.9F);
        if (t == 12) {
            sound(getAmbientSound(), 4.0F, 1.0F);
            sl.sendParticles(ParticleTypes.SPLASH, getX(), getY() + 1, getZ(), 120, 3.0, 1.0, 3.0, 0.4);
            sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1, getZ(), 40, 2.5, 1.5, 2.5, 0.05);
            for (LivingEntity e : hostilesAround(position(), 7)) {
                if (e instanceof CilleEntity) continue;
                knock(e, 1.3, 0.4);
            }
        }
    }

    @Override
    protected void onPhase2(ServerLevel sl) {
        sound(getAmbientSound(), 4.0F, 0.8F);
        sl.sendParticles(ParticleTypes.SPLASH, getX(), getY() + 1, getZ(), 100, 3.0, 1.0, 3.0, 0.4);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (empoweredTicks > 0 && --empoweredTicks == 0) {
            setEmpowered(false);
            this.getAttribute(Attributes.FLYING_SPEED).setBaseValue(0.55D);
        }
        // keep hovering above the ground a little
        if (!isCasting() && this.tickCount % 10 == 0 && !this.level().getBlockState(this.blockPosition().below()).isAir()
                && !this.getMoveControl().hasWanted()) {
            travelTo(getX(), getY() + 1.5, getZ(), 0.5);
        }
    }

    // ------------------------------------------------------------------ client fx
    @Override
    protected void clientFx() {
        Level l = this.level();
        if (random.nextInt(3) == 0) {
            Vec3 f = forward();
            double along = (random.nextDouble() - 0.5) * 4.5;
            l.addParticle(random.nextInt(4) == 0 ? ParticleTypes.FALLING_WATER : ParticleTypes.DRIPPING_WATER,
                    getX() + f.x * along + (random.nextDouble() - 0.5) * 1.6, getY() + 0.3 + random.nextDouble() * 1.2,
                    getZ() + f.z * along + (random.nextDouble() - 0.5) * 1.6, 0, 0, 0);
        }
        if (random.nextInt(isEmpowered() ? 1 : 8) == 0) {
            l.addParticle(isEmpowered() ? ParticleTypes.END_ROD : dust(STAR, 0.8F),
                    getX() + (random.nextDouble() - 0.5) * 5, getY() + random.nextDouble() * 2.6, getZ() + (random.nextDouble() - 0.5) * 5, 0, 0.01, 0);
        }
        int beam = getBeamTarget();
        if (beam >= 0) {
            Entity t = l.getEntity(beam);
            if (t != null) {
                Vec3 a = mouthPos(), b = t.getBoundingBox().getCenter();
                for (int i = 0; i < 4; i++) {
                    Vec3 p = a.add(b.subtract(a).scale(random.nextDouble()));
                    l.addParticle(i % 2 == 0 ? ParticleTypes.SNOWFLAKE : dust(ICE, 1.0F), p.x, p.y, p.z, 0, 0, 0);
                }
            }
        }
        if (getShield() == SHIELD_ICE && random.nextInt(2) == 0) {
            double a = random.nextDouble() * Math.PI * 2, bb = random.nextDouble() * Math.PI - Math.PI / 2;
            double r = 3.4;
            l.addParticle(ParticleTypes.SNOWFLAKE, getX() + Math.cos(a) * Math.cos(bb) * r, getY() + 1.2 + Math.sin(bb) * r * 0.7, getZ() + Math.sin(a) * Math.cos(bb) * r, 0, 0, 0);
        }
        if (clientAnimId == RUSH || clientAnimId == QUICK_GUARD) {
            for (int i = 0; i < 3; i++)
                l.addParticle(ParticleTypes.SPLASH, getX() + (random.nextDouble() - 0.5) * 3, getY() + random.nextDouble() * 2, getZ() + (random.nextDouble() - 0.5) * 3, 0, 0, 0);
        }
    }

    // ------------------------------------------------------------------ helpers
    @Override
    public Vec3 mouthPos() {
        return position().add(forward().scale(2.9)).add(0, 0.8, 0);
    }

    @Override protected SoundEvent getAmbientSound() { return ModSounds.BEHMUD_AMBIENT.get(); }
    @Override protected SoundEvent getHurtSound(DamageSource src) { return ModSounds.BEHMUD_HURT.get(); }
    @Override protected SoundEvent getDeathSoundEvent() { return ModSounds.BEHMUD_DEATH.get(); }
    @Override protected void playStepSound(BlockPos pos, BlockState state) {}
    @Override public int getAmbientSoundInterval() { return 220; }
}
