package com.krolasyon.aethercore.client;

import com.krolasyon.aethercore.block.AetherCoreBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class AetherCoreRenderer implements BlockEntityRenderer<AetherCoreBlockEntity> {
    public AetherCoreRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(AetherCoreBlockEntity be, float pt, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (be.getLevel() == null) return;
        float time = be.getLevel().getGameTime() + pt + be.getPhase();
        float open = be.getOpen(pt), flash = be.getFlash(pt), dome = be.getDome(pt);
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        CoreModel.render(pose, buffers, time, open, flash, light, overlay);
        if (dome > 0.01F) {
            Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            Vector3f rel = new Vector3f((float) (cam.x - be.getBlockPos().getX() - 0.5), (float) (cam.y - be.getBlockPos().getY() - 0.5),
                    (float) (cam.z - be.getBlockPos().getZ() - 0.5));
            CoreModel.renderDome(pose, buffers, (float) AetherCoreBlockEntity.RADIUS, time, dome, flash, rel);
        }
        pose.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(AetherCoreBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
