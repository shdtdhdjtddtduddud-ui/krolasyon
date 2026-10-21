package com.krolasyon.mythblades.entity;

import com.krolasyon.mythblades.item.RainbowScimitarItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/** Gökkuşağı hilali: düz uçar, değdiği her düşmanı bir kez keser ve bir renk etkisi bırakır, bloğa çarpınca dağılır. */
public final class RainbowWaveEntity extends Projectile {
    public static final int LIFE = 22;
    private final Set<Integer> hit = new HashSet<>();

    public RainbowWaveEntity(EntityType<? extends RainbowWaveEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    public void tick() {
        super.tick();
        Vec3 v = getDeltaMovement();
        Vec3 from = position(), to = from.add(v);
        if (!level().isClientSide) {
            HitResult bh = level().clip(new ClipContext(from.add(0, 0.3, 0), to.add(0, 0.3, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (bh.getType() != HitResult.Type.MISS) {
                burst();
                discard();
                return;
            }
            Entity owner = getOwner();
            AABB box = getBoundingBox().expandTowards(v).inflate(1.3, 0.5, 1.3);
            for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, box, x -> x != owner && x.isAlive() && !hit.contains(x.getId()))) {
                if (owner instanceof LivingEntity lo && e.isAlliedTo(lo)) continue;
                hit.add(e.getId());
                e.hurt(damageSources().indirectMagic(this, owner), 8F);
                RainbowScimitarItem.prism(e, random.nextInt(7), owner);
                ((ServerLevel) level()).sendParticles(ParticleTypes.END_ROD, e.getX(), e.getY() + e.getBbHeight() * 0.6, e.getZ(), 12, 0.3, 0.4, 0.3, 0.08);
                level().playSound(null, e.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1F, 1.4F);
            }
            if (tickCount > LIFE) {
                burst();
                discard();
                return;
            }
        } else if (random.nextInt(2) == 0) {
            level().addParticle(ParticleTypes.END_ROD, getX() + random.nextGaussian() * 0.6, getY() + 0.3, getZ() + random.nextGaussian() * 0.6, 0, 0.01, 0);
        }
        setPos(to.x, to.y, to.z);
    }

    private void burst() {
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.3, getZ(), 24, 0.6, 0.3, 0.6, 0.12);
            sl.sendParticles(ParticleTypes.GLOW, getX(), getY() + 0.3, getZ(), 10, 0.6, 0.3, 0.6, 0.05);
            sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 0.8F, 1.6F);
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) {
        return d < 128 * 128;
    }
}
