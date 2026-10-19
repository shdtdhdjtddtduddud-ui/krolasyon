package com.krolasyon.sylvanarchery.client;

import com.krolasyon.sylvanarchery.item.QuiverItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** Sadak envanterdeyken oyuncunun sırtında, çapraz olarak çizilir. */
public final class QuiverLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public QuiverLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack ps, MultiBufferSource buf, int light, AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float pt, float age, float headYaw, float headPitch) {
        if (player.isInvisible()) return;
        ItemStack quiver = QuiverItem.find(player);
        if (quiver.isEmpty()) return;
        ps.pushPose();
        getParentModel().body.translateAndRotate(ps);
        boolean armor = !player.getItemBySlot(EquipmentSlot.CHEST).isEmpty();
        ps.translate(0.0F, 0.38F, armor ? 0.26F : 0.21F);
        ps.mulPose(Axis.ZP.rotationDegrees(180));
        ps.mulPose(Axis.ZP.rotationDegrees(-28));
        ps.mulPose(Axis.XP.rotationDegrees(8));
        ps.scale(0.95F, 0.95F, 0.95F);
        int count = QuiverItem.count(quiver);
        QuiverModel.render(ps, buf, light, age, count == 0 ? 0 : 3 + Math.round(8F * count / QuiverItem.CAPACITY));
        ps.popPose();
    }
}
