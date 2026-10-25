package com.krolasyon.labirent.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/** Griever venom: slow, painful, and deadly unless a Grief Serum is used before it runs its course. */
public class VenomEffect extends MobEffect {
    public VenomEffect() { super(MobEffectCategory.HARMFUL, 0x5E7A1E); }

    @Override
    public void applyEffectTick(LivingEntity e, int amp) {
        if (e.level().isClientSide) return;
        if (e.getHealth() > 3F) e.hurt(e.damageSources().magic(), 1F);
        if (e.getRandom().nextInt(3) == 0) e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0, false, false));
        if (e.getRandom().nextInt(4) == 0) e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0, false, false));
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amp) { return duration % 80 == 0; }
}
