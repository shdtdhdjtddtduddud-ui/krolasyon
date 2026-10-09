package com.krolasyon.vocations.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Vocation ammunition: a normal arrow with extra base damage. */
public class VocationArrowItem extends ArrowItem {
    private final float bonusDamage;

    public VocationArrowItem(Properties properties, float bonusDamage) {
        super(properties);
        this.bonusDamage = bonusDamage;
    }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack stack, LivingEntity shooter) {
        AbstractArrow arrow = super.createArrow(level, stack, shooter);
        arrow.setBaseDamage(arrow.getBaseDamage() + bonusDamage);
        return arrow;
    }
}
