package com.krolasyon.stormray.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Fırtına Vatozu: gökyüzünde süzülen, balıkla evcilleşen, eyerlenip baktığın yöne uçan binek.
 * Evcilken (binilirken ya da sahibinin yakınındayken) düşmanlara zincirleme şimşek çarptırır.
 */
public class StormRayEntity extends TamableAnimal {
    private static final EntityDataAccessor<Boolean> SADDLED = SynchedEntityData.defineId(StormRayEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> ZAP = SynchedEntityData.defineId(StormRayEntity.class, EntityDataSerializers.INT);

    @Nullable private Vec3 wanted;
    private int zapCooldown;

    // istemci animasyonu
    public float flapPhase, flapPhaseO, roll, rollO, pitch, pitchO;

    public StormRayEntity(EntityType<? extends StormRayEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noCulling = true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FLYING_SPEED, 0.6)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    public static boolean checkSpawn(EntityType<StormRayEntity> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource r) {
        if (reason == MobSpawnType.SPAWNER) return true;
        return level.getBlockState(pos).isAir() && pos.getY() >= level.getSeaLevel() - 1;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(SADDLED, false);
        entityData.define(ZAP, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new SoarGoal());
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, MobSpawnType reason,
                                        @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        if (reason == MobSpawnType.NATURAL || reason == MobSpawnType.CHUNK_GENERATION) {
            setPos(getX(), getY() + 10 + random.nextInt(12), getZ());
        }
        return super.finalizeSpawn(level, diff, reason, data, tag);
    }

    public boolean isSaddled() { return entityData.get(SADDLED); }
    public void setSaddled(boolean s) { entityData.set(SADDLED, s); }
    public int getZapTicks() { return entityData.get(ZAP); }

    // ------------------------------------------------------------------ etkileşim
    private static boolean isFish(ItemStack s) {
        return s.is(Items.COD) || s.is(Items.SALMON) || s.is(Items.TROPICAL_FISH) || s.is(Items.COOKED_COD) || s.is(Items.COOKED_SALMON);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!isTame()) {
            if (isFish(stack)) {
                if (!level().isClientSide) {
                    if (!player.getAbilities().instabuild) stack.shrink(1);
                    if (random.nextInt(3) == 0 && !ForgeEventFactory.onAnimalTame(this, player)) {
                        tame(player);
                        wanted = null;
                        level().broadcastEntityEvent(this, (byte) 7);
                        player.displayClientMessage(Component.translatable("message.stormray.tamed"), true);
                        playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.5F, 1.4F);
                    } else {
                        level().broadcastEntityEvent(this, (byte) 6);
                    }
                }
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
            return InteractionResult.PASS;
        }
        if (!isOwnedBy(player)) {
            if (!level().isClientSide) player.displayClientMessage(Component.translatable("message.stormray.not_owner"), true);
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (isFish(stack) && getHealth() < getMaxHealth()) {
            if (!level().isClientSide) {
                heal(8);
                if (!player.getAbilities().instabuild) stack.shrink(1);
                level().broadcastEntityEvent(this, (byte) 7);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (stack.is(Items.SADDLE) && !isSaddled()) {
            if (!level().isClientSide) {
                setSaddled(true);
                if (!player.getAbilities().instabuild) stack.shrink(1);
                level().playSound(null, this, SoundEvents.HORSE_SADDLE, SoundSource.NEUTRAL, 0.8F, 1.0F);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (!isVehicle() && !player.isSecondaryUseActive()) {
            if (!isSaddled()) {
                if (!level().isClientSide) player.displayClientMessage(Component.translatable("message.stormray.need_saddle"), true);
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
            if (!level().isClientSide) player.startRiding(this);
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return null;
    }

    // ------------------------------------------------------------------ binme
    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return isSaddled() && getFirstPassenger() instanceof Player p ? p : null;
    }

    @Override
    protected void tickRidden(Player player, Vec3 input) {
        super.tickRidden(player, input);
        float yaw = Mth.approachDegrees(getYRot(), player.getYRot(), 12F);
        setYRot(yaw);
        yRotO = yaw;
        yBodyRot = yaw;
        yHeadRot = yaw;
        setXRot(player.getXRot() * 0.5F);
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction move) {
        double bob = Math.sin((tickCount) * 0.15) * 0.04;
        move.accept(passenger, getX(), getY() + 0.42 + bob + passenger.getMyRidingOffset(), getZ());
    }

    @Override
    public double getPassengersRidingOffset() {
        return 0.42;
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (passenger instanceof Player p && !level().isClientSide) {
            p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 160, 0, false, false, true));
        }
    }

    // ------------------------------------------------------------------ uçuş
    @Override
    public void travel(Vec3 input) {
        Vec3 v = getDeltaMovement();
        LivingEntity rider = getControllingPassenger();
        if (rider instanceof Player p) {
            if (!isControlledByLocalInstance()) return;
            Vec3 look = p.getLookAngle();
            float fwd = p.zza, str = p.xxa;
            double sp = (p.isSprinting() ? 1.15 : 0.7) * getAttributeValue(Attributes.FLYING_SPEED) / 0.6;
            Vec3 side = new Vec3(look.z, 0, -look.x).normalize();
            Vec3 target = look.scale(fwd * sp).add(side.scale(str * 0.4 * sp));
            if (fwd == 0 && str == 0) target = new Vec3(0, Math.sin(tickCount * 0.08) * 0.02, 0);
            v = v.lerp(target, 0.12);
        } else if (isEffectiveAi()) {
            Vec3 target;
            if (wanted != null && wanted.distanceToSqr(position()) > 4) {
                Vec3 dir = wanted.subtract(position());
                double speed = 0.32 * getAttributeValue(Attributes.FLYING_SPEED) / 0.6;
                target = dir.normalize().scale(speed);
                float yaw = (float) (Mth.atan2(dir.z, dir.x) * (180F / Math.PI)) - 90F;
                setYRot(Mth.approachDegrees(getYRot(), yaw, 3.5F));
                yBodyRot = getYRot();
                yHeadRot = getYRot();
                // ileri doğru süzül, hedef yöne kademeli dön
                Vec3 fwd = Vec3.directionFromRotation(0, getYRot()).scale(speed);
                target = new Vec3(fwd.x, target.y, fwd.z);
            } else {
                target = new Vec3(0, Math.sin(tickCount * 0.05) * 0.03, 0);
            }
            v = v.lerp(target, 0.05);
        }
        setDeltaMovement(v);
        move(MoverType.SELF, v);
        if (horizontalCollision) setDeltaMovement(getDeltaMovement().add(0, 0.08, 0));
        calculateEntityAnimation(false);
    }

    @Override
    public boolean causeFallDamage(float dist, float mult, DamageSource src) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, net.minecraft.world.level.block.state.BlockState state, BlockPos pos) {}

    @Override
    public boolean removeWhenFarAway(double dist) {
        return !isTame() && !hasCustomName();
    }

    // ------------------------------------------------------------------ tick
    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            clientAnim();
            return;
        }
        if (getZapTicks() > 0) entityData.set(ZAP, getZapTicks() - 1);
        if (zapCooldown > 0) zapCooldown--;
        if (zapCooldown == 0) tryZap();
        if (isVehicle() && getFirstPassenger() instanceof Player p) p.fallDistance = 0;
    }

    private void tryZap() {
        LivingEntity first = null;
        if (isTame()) {
            LivingEntity owner = getOwner();
            if (!isVehicle() && (owner == null || owner.distanceToSqr(this) > 24 * 24)) return;
            first = nearestEnemy(position(), 15, null);
        } else if (getLastHurtByMob() != null && getLastHurtByMob().isAlive() && getLastHurtByMob().distanceToSqr(this) < 24 * 24) {
            first = getLastHurtByMob();
        }
        if (first == null) return;
        List<LivingEntity> chain = new ArrayList<>();
        chain.add(first);
        if (isTame()) {
            for (int i = 0; i < 2; i++) {
                LivingEntity next = nearestEnemy(chain.get(chain.size() - 1).position(), 7, chain);
                if (next == null) break;
                chain.add(next);
            }
        }
        ServerLevel sl = (ServerLevel) level();
        Vec3 from = position().add(0, 0.2, 0);
        float dmg = isTame() ? 8F : 5F;
        for (LivingEntity t : chain) {
            Vec3 to = t.position().add(0, t.getBbHeight() * 0.5, 0);
            bolt(sl, from, to);
            t.hurt(damageSources().mobAttack(this), dmg);
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, to.x, to.y, to.z, 20, 0.3, 0.4, 0.3, 0.3);
            from = to;
            dmg *= 0.75F;
        }
        sl.playSound(null, blockPosition(), SoundEvents.TRIDENT_THUNDER, SoundSource.NEUTRAL, 0.7F, 1.6F);
        entityData.set(ZAP, 12);
        zapCooldown = isTame() ? 40 : 60;
    }

    private static void bolt(ServerLevel sl, Vec3 a, Vec3 b) {
        Vec3 d = b.subtract(a);
        int n = Math.max(4, (int) (d.length() * 3));
        RandomSource r = sl.random;
        for (int i = 0; i <= n; i++) {
            Vec3 p = a.add(d.scale(i / (double) n)).add((r.nextDouble() - 0.5) * 0.3, (r.nextDouble() - 0.5) * 0.3, (r.nextDouble() - 0.5) * 0.3);
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0, 0, 0, 0);
            if (i % 2 == 0) sl.sendParticles(ParticleTypes.GLOW, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }

    @Nullable
    private LivingEntity nearestEnemy(Vec3 at, double r, @Nullable List<LivingEntity> exclude) {
        LivingEntity best = null;
        double bd = r * r;
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(r))) {
            if (!(e instanceof Enemy) || !e.isAlive() || (exclude != null && exclude.contains(e))) continue;
            double d = e.position().distanceToSqr(at);
            if (d < bd) {
                bd = d;
                best = e;
            }
        }
        return best;
    }

    private void clientAnim() {
        flapPhaseO = flapPhase;
        rollO = roll;
        pitchO = pitch;
        Vec3 v = getDeltaMovement();
        double speed = v.length();
        flapPhase += 0.10F + (float) Math.min(0.25, speed * 0.55);
        float dyaw = Mth.wrapDegrees(getYRot() - yRotO);
        roll += (Mth.clamp(-dyaw * 4F, -35F, 35F) - roll) * 0.15F;
        pitch += ((float) Mth.clamp(-v.y * 70, -30, 30) - pitch) * 0.15F;
        if (random.nextInt(6) == 0) {
            level().addParticle(ParticleTypes.GLOW, getX() + (random.nextDouble() - 0.5) * 2, getY() + 0.2, getZ() + (random.nextDouble() - 0.5) * 2, 0, -0.02, 0);
        }
        if (getZapTicks() > 0 || random.nextInt(25) == 0) {
            level().addParticle(ParticleTypes.ELECTRIC_SPARK, getX() + (random.nextDouble() - 0.5), getY() - 0.3 - random.nextDouble(), getZ() + (random.nextDouble() - 0.5), 0, 0, 0);
        }
    }

    // ------------------------------------------------------------------ ses / kayıt
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PHANTOM_AMBIENT;
    }

    @Override
    public float getVoicePitch() {
        return 0.5F;
    }

    @Override
    protected float getSoundVolume() {
        return 0.4F;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource src) {
        return SoundEvents.PHANTOM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PHANTOM_DEATH;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource src, int looting, boolean hit) {
        super.dropCustomDeathLoot(src, looting, hit);
        if (isSaddled()) spawnAtLocation(Items.SADDLE);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Saddled", isSaddled());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setSaddled(tag.getBoolean("Saddled"));
    }

    /** Gökyüzünde süzülme: yabaniyken rastgele noktalara, evcilken sahibinin üstüne doğru uçar. */
    private final class SoarGoal extends Goal {
        private int timer;

        SoarGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return !isVehicle();
        }

        @Override
        public void tick() {
            if (--timer > 0 && wanted != null && wanted.distanceToSqr(position()) > 9) return;
            timer = 80 + random.nextInt(80);
            LivingEntity owner = isTame() ? getOwner() : null;
            if (owner != null && owner.distanceToSqr(StormRayEntity.this) > 10 * 10) {
                wanted = owner.position().add(random.nextGaussian() * 3, 4 + random.nextInt(3), random.nextGaussian() * 3);
                timer = 20;
                return;
            }
            Vec3 c = owner != null ? owner.position() : position();
            double ang = random.nextDouble() * Math.PI * 2, d = owner != null ? 5 + random.nextDouble() * 5 : 10 + random.nextDouble() * 18;
            int x = (int) (c.x + Math.cos(ang) * d), z = (int) (c.z + Math.sin(ang) * d);
            int ground = level().getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            double y = owner != null ? owner.getY() + 3 + random.nextInt(4) : Mth.clamp(getY() + random.nextInt(13) - 6, ground + 8, ground + 28);
            wanted = new Vec3(x + 0.5, Math.max(y, ground + 3), z + 0.5);
        }
    }
}
