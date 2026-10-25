package com.krolasyon.labirent.mixin;

import com.krolasyon.labirent.client.PoseFX;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hooks the human pose so players and Gladers get the mod's running / stumbling animation. */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin<T extends LivingEntity> {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void labirent$setupAnim(T entity, float limbSwing, float amount, float age, float headYaw, float headPitch, CallbackInfo ci) {
        PoseFX.apply((HumanoidModel<?>) (Object) this, entity, limbSwing, amount, age);
    }
}
