package com.krolasyon.ben10.entity;

import com.krolasyon.ben10.ModRegistry;
import com.krolasyon.ben10.ability.Fx;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/** Every thrown/shot alien attack: fireballs, crystal shards, acid, sonic discs, ice spears, ripped-up boulders. */
public class AlienProjectile extends ThrowableProjectile {
    public static final int FIREBALL = 0, CRYSTAL = 1, ACID = 2, DISC = 3, ICE_SPEAR = 4, BOULDER = 5, SHARD_FALL = 6, HOMING_DISC = 7, REFLECT = 8;
    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(AlienProjectile.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> BLOCK = SynchedEntityData.defineId(AlienProjectile.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> STUCK = SynchedEntityData.defineId(AlienProjectile.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(AlienProjectile.class, EntityDataSerializers.FLOAT);

    public float damage = 4;
    public float gravity = 0;
    public int life = 80;
    public int pierce = 0;
    public int homing = -1;
    private final Set<Integer> hits = new HashSet<>();
    private Vec3 stuckOffset;
    private int stuckTicks;

    public AlienProjectile(EntityType<? extends AlienProjectile> type, Level level) {
        super(type, level);
    }

    public static AlienProjectile shoot(ServerPlayer owner, int kind, Vec3 from, Vec3 vel, float damage) {
        AlienProjectile e = new AlienProjectile(ModRegistry.PROJECTILE.get(), owner.level());
        e.setOwner(owner);
        e.setKind(kind);
        e.setPos(from.x, from.y, from.z);
        e.setDeltaMovement(vel);
        e.damage = damage;
        e.alignToVelocity();
        owner.level().addFreshEntity(e);
        return e;
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(KIND, 0);
        entityData.define(BLOCK, 0);
        entityData.define(STUCK, -2);
        entityData.define(SCALE, 1F);
    }

    public int kind() { return entityData.get(KIND); }

    public void setKind(int k) { entityData.set(KIND, k); }

    public BlockState block() { return Block.stateById(entityData.get(BLOCK)); }

    public void setBlock(BlockState s) { entityData.set(BLOCK, Block.getId(s)); }

    public float scale() { return entityData.get(SCALE); }

    /** freeze in mid-air as a planted disc */
    public void plant() {
        setDeltaMovement(Vec3.ZERO);
        entityData.set(STUCK, -1);
    }

    public void setScale(float s) { entityData.set(SCALE, s); }

    /** -2 flying, -1 stuck in a block, >= 0 stuck to an entity id */
    public int stuck() { return entityData.get(STUCK); }

    @Override
    protected float getGravity() {
        return gravity;
    }

    public void alignToVelocity() {
        Vec3 v = getDeltaMovement();
        double h = v.horizontalDistance();
        setYRot((float) (Mth.atan2(v.x, v.z) * Mth.RAD_TO_DEG));
        setXRot((float) (Mth.atan2(v.y, h) * Mth.RAD_TO_DEG));
        yRotO = getYRot();
        xRotO = getXRot();
    }

    @Override
    public void tick() {
        if (stuck() != -2) {
            tickStuck();
            return;
        }
        if (!level().isClientSide && homing >= 0 && tickCount > 6) {
            Entity t = level().getEntity(homing);
            if (t != null && t.isAlive()) {
                Vec3 to = t.position().add(0, t.getBbHeight() / 2, 0).subtract(position()).normalize();
                Vec3 v = getDeltaMovement();
                double sp = Math.max(0.9, v.length());
                setDeltaMovement(v.normalize().scale(0.75).add(to.scale(0.45)).normalize().scale(sp));
            }
        }
        super.tick();
        if (kind() != DISC && kind() != HOMING_DISC) alignToVelocity();
        if (level().isClientSide) {
            trail();
        } else if (tickCount > life) {
            if (kind() == FIREBALL) burst(position());
            discard();
        }
    }

    private void trail() {
        Vec3 p = position();
        var r = random;
        switch (kind()) {
            case FIREBALL -> {
                for (int i = 0; i < 3; i++) level().addParticle(ParticleTypes.FLAME, p.x + r.nextGaussian() * 0.15, p.y + 0.25 + r.nextGaussian() * 0.15, p.z + r.nextGaussian() * 0.15, 0, 0.01, 0);
                if (r.nextInt(3) == 0) level().addParticle(ParticleTypes.LARGE_SMOKE, p.x, p.y + 0.25, p.z, 0, 0.02, 0);
                if (r.nextInt(4) == 0) level().addParticle(ParticleTypes.LAVA, p.x, p.y + 0.25, p.z, 0, 0, 0);
            }
            case CRYSTAL, SHARD_FALL, REFLECT -> level().addParticle(Fx.dust(0x9CFFC8, 0.8F), p.x, p.y + 0.2, p.z, 0, 0, 0);
            case ACID -> {
                level().addParticle(Fx.dust(0x80F030, 1.1F), p.x + r.nextGaussian() * 0.1, p.y + 0.2, p.z + r.nextGaussian() * 0.1, 0, 0, 0);
                if (r.nextInt(3) == 0) level().addParticle(ParticleTypes.ITEM_SLIME, p.x, p.y + 0.2, p.z, 0, 0, 0);
            }
            case DISC, HOMING_DISC -> level().addParticle(Fx.dust(0x30E8FF, 0.7F), p.x, p.y + 0.2, p.z, 0, 0, 0);
            case ICE_SPEAR -> {
                level().addParticle(ParticleTypes.SNOWFLAKE, p.x, p.y + 0.2, p.z, r.nextGaussian() * 0.02, 0, r.nextGaussian() * 0.02);
                level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.ICE)), p.x, p.y + 0.2, p.z, 0, 0, 0);
            }
            case BOULDER -> {
                if (r.nextInt(2) == 0) level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, block()), p.x, p.y + 0.3, p.z, 0, 0, 0);
            }
            default -> {}
        }
    }

    private void tickStuck() {
        baseTick();
        stuckTicks++;
        int s = stuck();
        if (s >= 0) {
            Entity t = level().getEntity(s);
            if (t == null || !t.isAlive()) {
                if (!level().isClientSide) pop();
                return;
            }
            if (stuckOffset == null) stuckOffset = position().subtract(t.position());
            setPos(t.position().add(stuckOffset));
        }
        setDeltaMovement(Vec3.ZERO);
        if (!level().isClientSide && (kind() == DISC || kind() == HOMING_DISC)) {
            // Ultimate Echo Echo disc: sonic pulse every 8 ticks
            if (stuckTicks % 8 == 0 && getOwner() instanceof ServerPlayer p) {
                ServerLevel l = (ServerLevel) level();
                Fx.sound(l, position(), ModRegistry.S_SONIC.get(), 0.5F, 1.4F + random.nextFloat() * 0.3F);
                FxEntity.spawn(l, position(), FxEntity.SONIC_RING, 10, 2.4F, 0x40E8FF, null);
                for (LivingEntity e : Fx.around(p, position(), 2.6)) {
                    Fx.hit(p, e, damage * 0.45F);
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
                    e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0));
                }
                Fx.shatter(l, position(), 2.0, 3);
            }
            if (stuckTicks > 70) pop();
        } else if (!level().isClientSide && stuckTicks > 40) {
            discard();
        }
    }

    private void pop() {
        ServerLevel l = (ServerLevel) level();
        Fx.particles(l, Fx.dust(0x30E8FF, 1.2F), position(), 12, 0.3, 0.1);
        Fx.sound(l, position(), SoundEvents.AMETHYST_BLOCK_BREAK, 0.5F, 1.8F);
        discard();
    }

    @Override
    protected boolean canHitEntity(Entity e) {
        if (e == getOwner() || hits.contains(e.getId())) return false;
        if (getOwner() instanceof ServerPlayer p) return Fx.valid(p, e);
        return e instanceof LivingEntity && super.canHitEntity(e);
    }

    @Override
    protected void onHitEntity(EntityHitResult r) {
        if (level().isClientSide) return;
        Entity t = r.getEntity();
        hits.add(t.getId());
        if (!(t instanceof LivingEntity le)) return;
        ServerPlayer p = getOwner() instanceof ServerPlayer sp ? sp : null;
        ServerLevel l = (ServerLevel) level();
        switch (kind()) {
            case FIREBALL -> {
                burst(position());
                discard();
            }
            case CRYSTAL, REFLECT -> {
                damage(p, le, damage);
                Fx.particles(l, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.EMERALD_BLOCK.defaultBlockState()), position(), 8, 0.2, 0.1);
                Fx.sound(l, position(), SoundEvents.AMETHYST_BLOCK_HIT, 0.8F, 1.3F);
                if (pierce-- <= 0) discard();
            }
            case SHARD_FALL -> {
                shardImpact(p);
                discard();
            }
            case ACID -> {
                damage(p, le, damage);
                le.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
                le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                splash();
                discard();
            }
            case DISC, HOMING_DISC -> {
                damage(p, le, damage);
                entityData.set(STUCK, t.getId());
                stuckOffset = position().subtract(t.position());
                Fx.sound(l, position(), SoundEvents.AMETHYST_BLOCK_PLACE, 0.8F, 1.6F);
            }
            case ICE_SPEAR -> {
                damage(p, le, damage);
                le.setTicksFrozen(Math.max(le.getTicksFrozen(), 240));
                le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3));
                Fx.particles(l, ParticleTypes.SNOWFLAKE, position(), 20, 0.4, 0.05);
                Fx.sound(l, position(), SoundEvents.GLASS_BREAK, 0.8F, 1.4F);
                if (pierce-- <= 0) {
                    iceBurst();
                    discard();
                }
            }
            case BOULDER -> {
                damage(p, le, damage);
                Fx.knock(le, position().subtract(getDeltaMovement()), 1.6, 0.5);
                boulderBreak();
            }
            default -> discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult r) {
        super.onHitBlock(r);
        if (level().isClientSide) return;
        ServerLevel l = (ServerLevel) level();
        ServerPlayer p = getOwner() instanceof ServerPlayer sp ? sp : null;
        switch (kind()) {
            case FIREBALL -> {
                burst(r.getLocation());
                discard();
            }
            case CRYSTAL, REFLECT -> {
                Fx.particles(l, Fx.dust(0x9CFFC8, 1.0F), r.getLocation(), 10, 0.2, 0.1);
                Fx.sound(l, r.getLocation(), SoundEvents.AMETHYST_CLUSTER_BREAK, 0.7F, 1.5F);
                BlockState s = l.getBlockState(r.getBlockPos());
                if (Fx.soft(s) && Fx.grief()) l.destroyBlock(r.getBlockPos(), true);
                discard();
            }
            case SHARD_FALL -> {
                shardImpact(p);
                discard();
            }
            case ACID -> {
                splash();
                if (Fx.grief()) {
                    for (BlockPos bp : BlockPos.betweenClosed(r.getBlockPos().offset(-1, -1, -1), r.getBlockPos().offset(1, 1, 1))) {
                        BlockState s = l.getBlockState(bp);
                        if (Fx.soft(s)) l.destroyBlock(bp, false);
                    }
                }
                discard();
            }
            case DISC, HOMING_DISC -> {
                setPos(r.getLocation());
                setDeltaMovement(Vec3.ZERO);
                entityData.set(STUCK, -1);
                Fx.sound(l, position(), SoundEvents.AMETHYST_BLOCK_PLACE, 0.8F, 1.6F);
            }
            case ICE_SPEAR -> {
                iceBurst();
                discard();
            }
            case BOULDER -> boulderBreak();
            default -> discard();
        }
    }

    private void damage(ServerPlayer p, LivingEntity le, float amount) {
        if (p != null) Fx.hit(p, le, amount);
        else le.hurt(damageSources().thrown(this, null), amount);
    }

    /** fire explosion without block damage */
    private void burst(Vec3 at) {
        ServerLevel l = (ServerLevel) level();
        Fx.particles(l, ParticleTypes.FLAME, at, 40, 0.5, 0.15);
        Fx.particles(l, ParticleTypes.LAVA, at, 8, 0.4, 0.1);
        Fx.particles(l, ParticleTypes.EXPLOSION, at, 1, 0, 0);
        FxEntity.spawn(l, at, FxEntity.FLASH, 8, 2.2F * scale(), 0xFF9020, null);
        Fx.sound(l, at, SoundEvents.FIRECHARGE_USE, 1F, 0.8F);
        Fx.sound(l, at, SoundEvents.GENERIC_EXPLODE, 0.5F, 1.5F);
        if (getOwner() instanceof ServerPlayer p) {
            for (LivingEntity e : Fx.around(p, at, 2.6 * scale())) {
                Fx.hitFire(p, e, hits.contains(e.getId()) ? damage * 0.5F : damage, 5);
                Fx.knock(e, at, 0.6, 0.3);
            }
        }
        BlockPos c = BlockPos.containing(at);
        for (BlockPos bp : BlockPos.betweenClosed(c.offset(-1, -1, -1), c.offset(1, 1, 1))) Fx.heat(l, bp.immutable(), scale() > 1.2F);
    }

    private void shardImpact(ServerPlayer p) {
        ServerLevel l = (ServerLevel) level();
        Fx.particles(l, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.EMERALD_BLOCK.defaultBlockState()), position(), 14, 0.4, 0.15);
        Fx.sound(l, position(), SoundEvents.AMETHYST_CLUSTER_BREAK, 0.9F, 0.8F + random.nextFloat() * 0.4F);
        if (p != null) for (LivingEntity e : Fx.around(p, position(), 1.8)) {
            Fx.hit(p, e, damage);
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
        }
    }

    private void splash() {
        ServerLevel l = (ServerLevel) level();
        Fx.particles(l, Fx.dust(0x80F030, 1.4F), position(), 20, 0.5, 0.1);
        Fx.particles(l, ParticleTypes.ITEM_SLIME, position(), 14, 0.4, 0.1);
        Fx.sound(l, position(), SoundEvents.SLIME_SQUISH, 1F, 0.7F);
        Fx.sound(l, position(), SoundEvents.LAVA_EXTINGUISH, 0.4F, 1.6F);
        double gy = Fx.groundY(l, getX(), getY(), getZ());
        FxEntity.spawn(l, new Vec3(getX(), gy + 0.02, getZ()), FxEntity.PUDDLE, 100, 1.8F, 0x7EE030, getOwner());
    }

    private void iceBurst() {
        ServerLevel l = (ServerLevel) level();
        Fx.particles(l, ParticleTypes.SNOWFLAKE, position(), 40, 0.8, 0.1);
        Fx.particles(l, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ICE.defaultBlockState()), position(), 20, 0.5, 0.2);
        Fx.sound(l, position(), SoundEvents.GLASS_BREAK, 1F, 0.8F);
        BlockPos c = BlockPos.containing(position());
        for (BlockPos bp : BlockPos.betweenClosed(c.offset(-2, -2, -2), c.offset(2, 2, 2))) {
            if (bp.distSqr(c) <= 5) Fx.frost(l, bp.immutable(), true);
        }
        if (getOwner() instanceof ServerPlayer p) for (LivingEntity e : Fx.around(p, position(), 2.5)) {
            e.setTicksFrozen(Math.max(e.getTicksFrozen(), 200));
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
        }
    }

    private void boulderBreak() {
        ServerLevel l = (ServerLevel) level();
        BlockState s = block();
        Fx.particles(l, new BlockParticleOption(ParticleTypes.BLOCK, s), position(), 30, 0.6, 0.2);
        Fx.sound(l, position(), s.getSoundType().getBreakSound(), 1.2F, 0.7F);
        Fx.sound(l, position(), SoundEvents.GENERIC_EXPLODE, 0.6F, 1.2F);
        for (int i = 0; i < 6; i++) {
            DebrisEntity.spawn(l, position().add(0, 0.3, 0), s, new Vec3(random.nextGaussian() * 0.25, 0.3 + random.nextDouble() * 0.3, random.nextGaussian() * 0.25));
        }
        if (getOwner() instanceof ServerPlayer p) for (LivingEntity e : Fx.around(p, position(), 2.5)) {
            if (!hits.contains(e.getId())) Fx.hit(p, e, damage * 0.5F);
            Fx.knock(e, position(), 0.8, 0.4);
        }
        discard();
    }

    @Override
    public boolean isOnFire() {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        t.putInt("Kind", kind());
        t.putFloat("Damage", damage);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        setKind(t.getInt("Kind"));
        damage = t.getFloat("Damage");
        life = 1;
    }
}
