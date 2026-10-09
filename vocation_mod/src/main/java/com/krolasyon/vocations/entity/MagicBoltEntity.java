package com.krolasyon.vocations.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.level.Level;

/** Straight-flying bolt cast from a staff. Damages the first living thing it hits as magic. */
public class MagicBoltEntity extends ThrowableItemProjectile {
    private float damage = 5.0F;

    public MagicBoltEntity(EntityType<? extends MagicBoltEntity> type, Level level) {
        super(type, level);
    }

    public MagicBoltEntity(EntityType<? extends MagicBoltEntity> type, LivingEntity shooter, Level level, float damage) {
        super(type, shooter, level);
        this.damage = damage;
    }

    @Override
    protected Item getDefaultItem() {
        return Items.STICK;
    }

    @Override
    protected float getGravity() {
        return 0.0F;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity target = result.getEntity();
        if (!this.level.isClientSide) {
            target.hurt(this.damageSources().indirectMagic(this, this.getOwner()), this.damage);
        }
    }
}
