package com.krolasyon.furniture.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;

/** first-person broom: idle sway + a wide side-to-side sweeping arc when the item is used */
public class BroomItemExt extends FurnitureItemExt {
    @Override
    public boolean applyForgeHandTransform(PoseStack ps, LocalPlayer player, HumanoidArm arm, ItemStack stack, float partialTick, float equipProcess, float swingProcess) {
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        float sw = swingProcess;
        float arc = Mth.sin(sw * Mth.PI);                 // 0 -> 1 -> 0
        float sweep = Mth.cos(sw * Mth.PI);               // +1 -> -1 : side to side
        float idle = Mth.sin((player.tickCount + partialTick) * 0.09F) * 0.6F;
        ps.translate(side * 0.50F, -0.46F - equipProcess * 0.6F + arc * 0.06F, -0.62F - arc * 0.10F);
        ps.mulPose(Axis.YP.rotationDegrees(side * (22.0F + 52.0F * sweep * (sw > 0.0F ? 1.0F : 0.0F))));
        ps.mulPose(Axis.XP.rotationDegrees(-14.0F - 22.0F * arc + idle));
        ps.mulPose(Axis.ZP.rotationDegrees(side * (-6.0F + 16.0F * arc * sweep)));
        return true;
    }
}
