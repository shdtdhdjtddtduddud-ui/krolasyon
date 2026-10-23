package com.krolasyon.mythblades.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public final class ClientHooks {
    private ClientHooks() {}

    private static BlockEntityWithoutLevelRenderer renderer;

    private static BlockEntityWithoutLevelRenderer renderer() {
        if (renderer == null) renderer = new MythItemRenderer();
        return renderer;
    }

    public static final IClientItemExtensions ITEM_EXTENSIONS = new IClientItemExtensions() {
        @Override
        public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            return renderer();
        }
    };

    /** Kor Kılıcı'nın dövüş duruşu (üçüncü şahıs kol, gövde ve bacaklar). */
    public static final HumanoidModel.ArmPose EMBER_POSE = HumanoidModel.ArmPose.create("MYTHBLADES_EMBER", true, EmberClient.POSE_FN);

    /** Kor Kılıcı: 3B model + anime dövüş animasyonları. */
    public static final IClientItemExtensions EMBER_EXTENSIONS = new IClientItemExtensions() {
        @Override
        public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            return renderer();
        }

        @Override
        public HumanoidModel.ArmPose getArmPose(LivingEntity e, InteractionHand hand, ItemStack stack) {
            return hand == InteractionHand.MAIN_HAND ? EMBER_POSE : null;
        }

        @Override
        public boolean applyForgeHandTransform(PoseStack ps, LocalPlayer p, HumanoidArm arm, ItemStack stack, float pt, float equip, float swing) {
            if (p.getMainHandItem() != stack) return false;
            return EmberClient.firstPerson(ps, p, arm, pt, equip, swing);
        }
    };
}
