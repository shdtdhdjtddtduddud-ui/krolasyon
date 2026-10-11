package com.krolasyon.omnitrix.mixin;

import com.krolasyon.omnitrix.client.ClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Ben-style watch activation: raise the left wrist, slap the dial with the right hand. */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin<T extends LivingEntity> {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void omnitrix$watchPose(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (entity != mc.player || mc.level == null) return;
        float t = (mc.level.getGameTime() - ClientEvents.watchAnim + mc.getFrameTime()) / 14F;
        if (t < 0 || t > 1) return;
        HumanoidModel<?> m = (HumanoidModel<?>) (Object) this;
        float w = Mth.sin(t * Mth.PI);
        float slap = t > 0.45F ? Mth.clamp((t - 0.45F) / 0.15F, 0, 1) : 0;
        m.leftArm.xRot = Mth.lerp(w, m.leftArm.xRot, -1.35F);
        m.leftArm.yRot = Mth.lerp(w, m.leftArm.yRot, 0.55F);
        m.rightArm.xRot = Mth.lerp(w, m.rightArm.xRot, -1.1F - 0.35F * (1 - slap));
        m.rightArm.yRot = Mth.lerp(w, m.rightArm.yRot, -0.45F);
        m.head.xRot = Mth.lerp(w, m.head.xRot, 0.5F);
    }
}
