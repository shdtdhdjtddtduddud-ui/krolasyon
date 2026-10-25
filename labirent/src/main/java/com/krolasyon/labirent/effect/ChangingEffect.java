package com.krolasyon.labirent.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/** The Changing: the serum burns the venom out and drags old memories up with it. */
public class ChangingEffect extends MobEffect {
    public ChangingEffect() { super(MobEffectCategory.NEUTRAL, 0x9FB8FF); }

    @Override
    public void applyEffectTick(LivingEntity e, int amp) {
        if (e.level().isClientSide) return;
        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, false, false));
        if (e.getRandom().nextInt(2) == 0) e.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false));
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amp) { return duration % 40 == 0; }
}
