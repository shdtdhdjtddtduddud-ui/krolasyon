package com.krolasyon.advgear.entity;

import com.krolasyon.advgear.AdvGear;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/** Fırlatılmış Kara Ölüm şişesi. */
public class PoisonFlaskEntity extends ThrowableItemProjectile {
    public PoisonFlaskEntity(EntityType<? extends PoisonFlaskEntity> type, Level level) {
        super(type, level);
    }

    public PoisonFlaskEntity(Level level, LivingEntity owner) {
        super(AdvGear.POISON_FLASK_ENTITY.get(), owner, level);
    }

    @Override
    protected Item getDefaultItem() {
        return AdvGear.BLACK_DEATH.get();
    }

    @Override
    protected float getGravity() {
        return 0.05F;
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (level().isClientSide) return;
        ServerLevel sl = (ServerLevel) level();
        sl.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(getDefaultItem())), getX(), getY(), getZ(), 20, 0.2, 0.2, 0.2, 0.1);
        sl.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 0.3, getZ(), 30, 0.8, 0.4, 0.8, 0.02);
        level().playSound(null, blockPosition(), SoundEvents.SPLASH_POTION_BREAK, SoundSource.NEUTRAL, 1F, 0.7F);
        AreaEffectCloud c = new AreaEffectCloud(level(), getX(), getY(), getZ());
        if (getOwner() instanceof LivingEntity le) c.setOwner(le);
        c.setRadius(3.5F);
        c.setRadiusOnUse(-0.3F);
        c.setWaitTime(5);
        c.setDuration(160);
        c.setRadiusPerTick(-c.getRadius() / c.getDuration());
        c.setFixedColor(0xA0156A);
        c.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 1));
        c.addEffect(new MobEffectInstance(MobEffects.WITHER, 120, 0));
        c.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 1));
        level().addFreshEntity(c);
        discard();
    }
}
