package com.krolasyon.cille.entity;

import com.krolasyon.cille.registry.ModEntities;
import com.krolasyon.cille.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;

/**
 * Behmud's living ammunition: exploding fish (arc and home in), torpedo fish (straight and fast, bigger blast)
 * and falling stars from the space power.
 */
public class CilleProjectileEntity extends Projectile {
    public static final int KIND_FISH = 0, KIND_TORPEDO = 1, KIND_STAR = 2, KIND_ROCK = 3;
    private static final EntityDataAccessor<net.minecraft.world.level.block.state.BlockState> DATA_BLOCK =
            SynchedEntityData.defineId(CilleProjectileEntity.class, EntityDataSerializers.BLOCK_STATE);
    private static final EntityDataAccessor<Integer> DATA_KIND = SynchedEntityData.defineId(CilleProjectileEntity.class, EntityDataSerializers.INT);

    @Nullable private LivingEntity homing;
    private boolean exploded;

    public CilleProjectileEntity(EntityType<? extends CilleProjectileEntity> type, Level level) {
        super(type, level);
    }

    public static void launch(CilleEntity owner, int kind, Vec3 from, Vec3 dir, double speed, @Nullable LivingEntity target) {
        CilleProjectileEntity p = new CilleProjectileEntity(ModEntities.CILLE_PROJECTILE.get(), owner.level());
        p.setOwner(owner);
        p.entityData.set(DATA_KIND, kind);
        p.homing = target;
        p.setPos(from.x, from.y, from.z);
        Vec3 d = dir.lengthSqr() < 1.0E-6 ? owner.forward() : dir.normalize();
        p.setDeltaMovement(d.scale(speed));
        p.updateRotation();
        owner.level().addFreshEntity(p);
    }

    public int getKind() { return this.entityData.get(DATA_KIND); }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_KIND, KIND_FISH);
        this.entityData.define(DATA_BLOCK, net.minecraft.world.level.block.Blocks.SANDSTONE.defaultBlockState());
    }

    /** the boulder's block (Şarkan's rock throw) */
    public net.minecraft.world.level.block.state.BlockState getBlock() { return this.entityData.get(DATA_BLOCK); }

    public static void launchRock(CilleEntity owner, net.minecraft.world.level.block.state.BlockState state, Vec3 from, Vec3 dir, double speed, @Nullable LivingEntity target) {
        CilleProjectileEntity p = new CilleProjectileEntity(ModEntities.CILLE_PROJECTILE.get(), owner.level());
        p.setOwner(owner);
        p.entityData.set(DATA_KIND, KIND_ROCK);
        p.entityData.set(DATA_BLOCK, state);
        p.homing = target;
        p.setPos(from.x, from.y, from.z);
        Vec3 d = dir.lengthSqr() < 1.0E-6 ? owner.forward() : dir.normalize();
        p.setDeltaMovement(d.scale(speed));
        p.updateRotation();
        owner.level().addFreshEntity(p);
    }

    @Override
    public void tick() {
        super.tick();
        int kind = getKind();
        Vec3 v = this.getDeltaMovement();
        if (!this.level().isClientSide()) {
            HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (hit.getType() != HitResult.Type.MISS) {
                this.setPos(hit.getLocation());
                explode();
                return;
            }
            if (kind == KIND_FISH && homing != null && homing.isAlive() && this.tickCount > 6) {
                Vec3 want = homing.getBoundingBox().getCenter().subtract(position()).normalize().scale(v.length());
                v = v.scale(0.9).add(want.scale(0.1));
            }
            if (this.tickCount > 160) { explode(); return; }
        }
        if (kind == KIND_FISH) v = v.add(0, -0.035, 0);
        if (kind == KIND_ROCK) v = v.add(0, -0.045, 0);
        if (kind == KIND_STAR) v = v.scale(1.04);
        this.setDeltaMovement(v);
        this.setPos(getX() + v.x, getY() + v.y, getZ() + v.z);
        this.updateRotation();
        if (this.level().isClientSide()) {
            switch (kind) {
                case KIND_STAR -> {
                    this.level().addParticle(ParticleTypes.END_ROD, getX(), getY(), getZ(), 0, 0, 0);
                    this.level().addParticle(CilleEntity.dust(BehmudEntity.STAR, 1.6F), getX(), getY(), getZ(), 0, 0, 0);
                }
                case KIND_TORPEDO -> {
                    this.level().addParticle(ParticleTypes.BUBBLE, getX(), getY(), getZ(), -v.x * 0.2, -v.y * 0.2, -v.z * 0.2);
                    this.level().addParticle(ParticleTypes.CLOUD, getX(), getY(), getZ(), 0, 0, 0);
                }
                case KIND_ROCK -> this.level().addParticle(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, getBlock()), getX(), getY() + 0.4, getZ(), 0, 0, 0);
                default -> this.level().addParticle(ParticleTypes.SPLASH, getX(), getY(), getZ(), 0, 0, 0);
            }
        }
    }

    @Override
    protected boolean canHitEntity(Entity e) {
        if (!super.canHitEntity(e) || e instanceof CilleProjectileEntity) return false;
        return !(this.getOwner() instanceof CilleEntity c) || c.isHostileTo(e);
    }

    private void explode() {
        if (exploded || this.level().isClientSide()) return;
        exploded = true;
        ServerLevel sl = (ServerLevel) this.level();
        int kind = getKind();
        Entity ownerE = this.getOwner();
        CilleEntity owner = ownerE instanceof CilleEntity c ? c : null;
        float power = owner != null ? owner.power() : 1F;
        double r = switch (kind) { case KIND_TORPEDO -> 3.4; case KIND_STAR -> 2.8; case KIND_ROCK -> 3.0; default -> 2.6; };
        float dmg = switch (kind) { case KIND_TORPEDO -> 12F; case KIND_STAR -> 9F; case KIND_ROCK -> 15F; default -> 8F; } * power;
        Vec3 c = position();
        if (kind == KIND_ROCK) {
            sl.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, getBlock()), c.x, c.y + 0.4, c.z, 80, 1.0, 0.6, 1.0, 0.25);
            sl.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 0.4, c.z, 2, 0.4, 0.3, 0.4, 0);
        } else if (kind == KIND_STAR) {
            sl.sendParticles(ParticleTypes.FLASH, c.x, c.y, c.z, 1, 0, 0, 0, 0);
            sl.sendParticles(ParticleTypes.END_ROD, c.x, c.y + 0.3, c.z, 40, 0.3, 0.3, 0.3, 0.35);
            sl.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 0.3, c.z, 2, 0.3, 0.3, 0.3, 0);
        } else {
            sl.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 0.3, c.z, kind == KIND_TORPEDO ? 4 : 2, 0.5, 0.5, 0.5, 0);
            sl.sendParticles(ParticleTypes.SPLASH, c.x, c.y + 0.3, c.z, 80, r * 0.4, 0.5, r * 0.4, 0.5);
            sl.sendParticles(ParticleTypes.BUBBLE_POP, c.x, c.y + 0.3, c.z, 30, r * 0.4, 0.5, r * 0.4, 0.2);
        }
        sl.playSound(null, c.x, c.y, c.z, kind == KIND_ROCK ? ModSounds.ROCK_IMPACT.get() : ModSounds.WATER_BOOM.get(), SoundSource.HOSTILE, kind == KIND_TORPEDO ? 3.0F : 2.0F, kind == KIND_STAR ? 0.8F : 1.0F);
        for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(r))) {
            if (owner != null ? !owner.isHostileTo(e) : e == ownerE) continue;
            double d = Math.sqrt(e.distanceToSqr(this));
            if (d > r) continue;
            if (e.hurt(this.damageSources().explosion(this, ownerE), dmg * (float) (1.0 - 0.4 * d / r))) {
                Vec3 k = e.position().subtract(c).multiply(1, 0, 1);
                k = k.lengthSqr() < 1.0E-4 ? Vec3.ZERO : k.normalize().scale(0.9);
                e.setDeltaMovement(e.getDeltaMovement().add(k.x, 0.45, k.z));
                e.hurtMarked = true;
                e.clearFire();
            }
        }
        if (owner != null) Terrain.crater(sl, owner, c, kind == KIND_TORPEDO || kind == KIND_ROCK ? 2.4 : 1.5, kind == KIND_TORPEDO || kind == KIND_ROCK ? 6 : 3, 2.0F);
        this.discard();
    }

    @Override protected void readAdditionalSaveData(CompoundTag tag) { this.discard(); }
    @Override public boolean isPickable() { return false; }
    @Override public boolean shouldRenderAtSqrDistance(double d) { return d < 96 * 96; }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
