package com.krolasyon.omnitrix.entity;

import com.krolasyon.omnitrix.Fx;
import com.krolasyon.omnitrix.ModRegistry;
import com.krolasyon.omnitrix.Util;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/** One projectile class for every alien shot; the kind decides flight, damage and looks. */
public class AlienProjectile extends Projectile {
    public enum Kind { FIREBALL, CRYSTAL, ACID, ACID_BOMB, SONIC_DISC, BOULDER }

    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(AlienProjectile.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<BlockState>> BLOCK = SynchedEntityData.defineId(AlienProjectile.class, EntityDataSerializers.OPTIONAL_BLOCK_STATE);
    private static final EntityDataAccessor<Integer> STUCK = SynchedEntityData.defineId(AlienProjectile.class, EntityDataSerializers.INT);

    private final Set<Integer> pierced = new HashSet<>();
    private int stuckTicks;

    public AlienProjectile(EntityType<? extends AlienProjectile> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public AlienProjectile(Level level, LivingEntity owner, Kind kind) {
        this(ModRegistry.PROJECTILE.get(), level);
        setOwner(owner);
        entityData.set(KIND, kind.ordinal());
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(KIND, 0);
        entityData.define(BLOCK, Optional.empty());
        entityData.define(STUCK, -1);
    }

    public Kind kind() { return Kind.values()[Math.max(0, Math.min(Kind.values().length - 1, entityData.get(KIND)))]; }

    public BlockState block() { return entityData.get(BLOCK).orElse(Blocks.DIRT.defaultBlockState()); }

    public void setBlock(BlockState s) { entityData.set(BLOCK, Optional.of(s)); }

    public int stuckTo() { return entityData.get(STUCK); }

    private double gravity() {
        return switch (kind()) {
            case ACID_BOMB -> 0.05;
            case BOULDER -> 0.035;
            case ACID -> 0.01;
            default -> 0;
        };
    }

    @Override
    public void tick() {
        super.tick();
        if (stuckTo() >= 0) { tickStuck(); return; }
        Vec3 pos = position();
        Vec3 vel = getDeltaMovement();
        if (kind() == Kind.SONIC_DISC && !level().isClientSide && getOwner() instanceof ServerPlayer sp) {
            // home in on the closest enemy
            LivingEntity t = Util.around(sp, pos, 10, true).stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(this))).orElse(null);
            if (t != null) vel = vel.add(t.position().add(0, t.getBbHeight() / 2, 0).subtract(pos).normalize().scale(0.25)).normalize().scale(vel.length());
        }
        Vec3 next = pos.add(vel);
        if (level() instanceof ServerLevel sl) {
            if (!(getOwner() instanceof ServerPlayer owner) || tickCount > 100) { discard(); return; }
            BlockHitResult bh = sl.clip(new ClipContext(pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            boolean wall = bh.getType() != HitResult.Type.MISS;
            if (wall) next = bh.getLocation();
            final Vec3 end = next;
            for (Entity e : sl.getEntities(this, getBoundingBox().expandTowards(vel).inflate(0.4), e -> Util.canHit(owner, e, false))) {
                if (pierced.contains(e.getId()) || e.getBoundingBox().inflate(0.35).clip(pos, end).isEmpty()) continue;
                pierced.add(e.getId());
                if (hitEntity(sl, owner, (LivingEntity) e)) return;
            }
            if (wall) { impact(sl, owner, next); return; }
        } else {
            clientTrail();
        }
        setDeltaMovement(vel.scale(0.995).add(0, -gravity(), 0));
        setPos(next.x, next.y, next.z);
    }

    /** @return true when the projectile is done */
    private boolean hitEntity(ServerLevel sl, ServerPlayer owner, LivingEntity e) {
        switch (kind()) {
            case FIREBALL -> {
                Util.hit(e, 7F, this, owner);
                e.setSecondsOnFire(5);
                impact(sl, owner, position());
                return true;
            }
            case CRYSTAL -> {
                Util.hit(e, 4.5F, this, owner);
                Util.particles(sl, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.EMERALD_BLOCK.defaultBlockState()), e.position().add(0, e.getBbHeight() / 2, 0), 8, 0.2, 0.1);
                Util.play(sl, position(), ModRegistry.SHATTER.get(), 0.6F, 1.6F);
                if (pierced.size() >= 2) { discard(); return true; }
                return false;
            }
            case ACID -> {
                Util.hit(e, 5F, this, owner);
                e.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 1));
                for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                    if (!e.getItemBySlot(s).isEmpty() && e.getItemBySlot(s).isDamageableItem()) e.getItemBySlot(s).hurtAndBreak(6, e, x -> x.broadcastBreakEvent(s));
                }
                impact(sl, owner, position());
                return true;
            }
            case SONIC_DISC -> {
                entityData.set(STUCK, e.getId());
                stuckTicks = 0;
                Util.play(sl, position(), ModRegistry.SONIC.get(), 0.6F, 1.6F);
                return true;
            }
            default -> {
                impact(sl, owner, position());
                return true;
            }
        }
    }

    private void impact(ServerLevel sl, ServerPlayer owner, Vec3 at) {
        switch (kind()) {
            case FIREBALL -> {
                for (LivingEntity e : Util.around(owner, at, 2.5, true)) {
                    Util.hit(e, 3F, this, owner);
                    e.setSecondsOnFire(3);
                }
                Fx.at(sl, Fx.Type.FIRE_RING, at, 10, 2.2F);
                Util.particles(sl, ParticleTypes.FLAME, at, 20, 0.3, 0.1);
                Util.play(sl, at, ModRegistry.SUPERNOVA.get(), 0.6F, 1.6F);
            }
            case ACID, ACID_BOMB -> {
                boolean bomb = kind() == Kind.ACID_BOMB;
                Fx.at(sl, Fx.Type.SLIME_SPLASH, at, bomb ? 40 : 12, bomb ? 3.5F : 1.2F);
                Util.play(sl, at, ModRegistry.SPLAT.get(), 1F, bomb ? 0.7F : 1.3F);
                if (bomb) com.krolasyon.omnitrix.Powers.acidPool(sl, owner, at);
            }
            case BOULDER -> {
                for (LivingEntity e : Util.around(owner, at, 2.8, false)) {
                    if (!Util.canHit(owner, e, true) && e.distanceToSqr(at) > 2.2 * 2.2) continue;
                    Util.hit(e, 11F, this, owner);
                    Util.push(e, at, 1.0, 0.3);
                }
                Util.particles(sl, new BlockParticleOption(ParticleTypes.BLOCK, block()), at, 60, 0.6, 0.2);
                Fx.at(sl, Fx.Type.SHOCKWAVE, at, 16, 3F);
                Util.play(sl, at, ModRegistry.SLAM.get(), 1.3F, 1.1F);
            }
            case CRYSTAL -> {
                Util.particles(sl, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.EMERALD_BLOCK.defaultBlockState()), at, 6, 0.1, 0.1);
                Util.play(sl, at, ModRegistry.SHATTER.get(), 0.4F, 1.8F);
            }
            default -> {}
        }
        discard();
    }

    private void tickStuck() {
        Entity t = level().getEntity(stuckTo());
        if (t == null || !t.isAlive()) { if (!level().isClientSide) discard(); return; }
        setPos(t.getX(), t.getY() + t.getBbHeight() * 0.6, t.getZ());
        if (level() instanceof ServerLevel sl && getOwner() instanceof ServerPlayer owner) {
            stuckTicks++;
            if (stuckTicks % 10 == 0 && t instanceof LivingEntity le) {
                Util.hit(le, 3F, this, owner);
                le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 15, 1));
                Fx.at(sl, Fx.Type.HIT, position(), 6, 7);
                Util.play(sl, position(), ModRegistry.SONIC.get(), 0.4F, 1.8F);
            }
            if (stuckTicks > 60) discard();
        }
    }

    private void clientTrail() {
        switch (kind()) {
            case FIREBALL -> level().addParticle(ModRegistry.FIRE.get(), getX(), getY(), getZ(), 0, 0.02, 0);
            case ACID, ACID_BOMB -> { if (random.nextBoolean()) level().addParticle(ModRegistry.SLIME.get(), getX(), getY(), getZ(), 0, -0.05, 0); }
            case BOULDER -> { if (random.nextInt(3) == 0) level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, block()), getX(), getY(), getZ(), 0, 0, 0); }
            default -> {}
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        t.putInt("Kind", entityData.get(KIND));
        t.put("Block", NbtUtils.writeBlockState(block()));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        entityData.set(KIND, t.getInt("Kind"));
    }

    @Override
    public boolean isPickable() { return false; }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) { return d < 128 * 128; }
}
