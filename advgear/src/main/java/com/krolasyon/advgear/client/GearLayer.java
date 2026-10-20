package com.krolasyon.advgear.client;

import com.krolasyon.advgear.item.BackpackItem;
import com.krolasyon.advgear.item.GogglesItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** Oyuncunun üstünde gözlük (başta), kemer (belde) ve sırt çantası (sırtta). */
public final class GearLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public GearLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack ps, MultiBufferSource buf, int light, AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float pt, float age, float headYaw, float headPitch) {
        if (player.isInvisible()) return;
        ItemStack[] gear = ClientGear.of(player);
        ItemStack goggles = gear[2], belt = gear[3], back = gear[4];
        if (goggles.getItem() instanceof GogglesItem g) {
            ps.pushPose();
            getParentModel().head.translateAndRotate(ps);
            if (!player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) ps.scale(1.18F, 1.18F, 1.18F);
            GearRender.goggles(ps, buf, light, g.variant);
            ps.popPose();
        }
        if (!belt.isEmpty()) {
            ps.pushPose();
            getParentModel().body.translateAndRotate(ps);
            if (!player.getItemBySlot(EquipmentSlot.LEGS).isEmpty() || !player.getItemBySlot(EquipmentSlot.CHEST).isEmpty()) ps.scale(1.12F, 1F, 1.2F);
            GearRender.belt(ps, buf, light, beltVariant(belt));
            ps.popPose();
        }
        if (back.getItem() instanceof BackpackItem b) {
            ps.pushPose();
            getParentModel().body.translateAndRotate(ps);
            if (!player.getItemBySlot(EquipmentSlot.CHEST).isEmpty()) ps.translate(0, 0, 0.07F);
            float sway = Mth.sin(limbSwing * 0.6662F) * limbSwingAmount * 0.12F;
            GearRender.backpack(ps, buf, light, b.variant, sway);
            ps.popPose();
        }
    }

    public static int beltVariant(ItemStack s) {
        String p = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(s.getItem()).getPath();
        return p.startsWith("tool") ? 1 : p.startsWith("alchemist") ? 2 : 0;
    }
}
