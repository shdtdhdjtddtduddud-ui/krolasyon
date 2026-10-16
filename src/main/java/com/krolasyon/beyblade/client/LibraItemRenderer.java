package com.krolasyon.beyblade.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Renders the Flame Libra item as its full 3D model (slowly turning in the inventory). */
public class LibraItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final java.util.Map<String, LibraItemRenderer> CACHE = new java.util.HashMap<>();

    public static LibraItemRenderer get(String kind) {
        return CACHE.computeIfAbsent(kind, k -> new LibraItemRenderer(BeyMesh.of(k)));
    }

    private final BeyMesh mesh;

    private LibraItemRenderer(BeyMesh mesh) {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        this.mesh = mesh;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        float time = (mc.level != null ? mc.level.getGameTime() : 0) + com.krolasyon.beyblade.compat.VClient.partialTick();
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        float s;
        switch (ctx) {
            case GUI -> {
                pose.mulPose(Axis.XP.rotationDegrees(38F));
                pose.mulPose(Axis.YP.rotationDegrees(time * 1.5F));
                s = 0.43F;
            }
            case GROUND -> s = 0.35F;
            case FIXED -> {
                pose.mulPose(Axis.XP.rotationDegrees(-90F));
                s = 0.5F;
            }
            case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND -> {
                pose.translate(0.05, 0.2, -0.1);
                pose.mulPose(Axis.XP.rotationDegrees(20F));
                pose.mulPose(Axis.YP.rotationDegrees(time * 0.8F));
                s = 0.19F;
            }
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND -> {
                pose.translate(0, 0.12, 0.05);
                pose.mulPose(Axis.XP.rotationDegrees(-60F));
                s = 0.2F;
            }
            case HEAD -> {
                pose.translate(0, 0.3, 0);
                s = 0.5F;
            }
            default -> s = 0.4F;
        }
        pose.scale(s, s, s);
        com.krolasyon.beyblade.parts.Loadout parts = com.krolasyon.beyblade.parts.Loadout.of(stack);
        BeyMesh m = BeyMesh.forLoadout(parts, mesh);
        pose.translate(0, -(m.HEIGHT + m.lift(parts)) * 0.5, 0);
        m.renderAssembled(pose, buf, light, overlay, parts);
        pose.popPose();
    }
}
