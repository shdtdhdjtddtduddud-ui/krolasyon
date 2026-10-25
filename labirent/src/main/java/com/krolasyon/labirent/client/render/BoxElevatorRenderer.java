package com.krolasyon.labirent.client.render;

import com.krolasyon.labirent.entity.BoxElevatorEntity;
import com.krolasyon.labirent.registry.ModBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.BlockState;

/** The rising Box: a 4x4 iron cage with crates, drawn from real block models. */
public class BoxElevatorRenderer extends EntityRenderer<BoxElevatorEntity> {
    public BoxElevatorRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0F;
    }

    @Override
    public void render(BoxElevatorEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buf, int light) {
        BlockRenderDispatcher d = Minecraft.getInstance().getBlockRenderer();
        BlockState floor = ModBlocks.BOX_FLOOR.get().defaultBlockState();
        BlockState post = Blocks.IRON_BLOCK.defaultBlockState();
        BlockState barsX = Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.EAST, true).setValue(IronBarsBlock.WEST, true);
        BlockState barsZ = Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.NORTH, true).setValue(IronBarsBlock.SOUTH, true);
        BlockState lamp = Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT, true);
        int lit = Math.max(light, LightTexture.pack(8, 0));
        for (int x = -2; x <= 1; x++)
            for (int z = -2; z <= 1; z++) {
                block(d, pose, buf, floor, x, -1, z, lit);
                boolean edgeX = x == -2 || x == 1, edgeZ = z == -2 || z == 1;
                for (int y = 0; y < 3; y++) {
                    if (edgeX && edgeZ) block(d, pose, buf, post, x, y, z, lit);
                    else if (edgeZ) block(d, pose, buf, barsX, x, y, z, lit);
                    else if (edgeX) block(d, pose, buf, barsZ, x, y, z, lit);
                }
            }
        block(d, pose, buf, lamp, -2, 3, -2, LightTexture.FULL_BRIGHT);
        block(d, pose, buf, lamp, 1, 3, 1, LightTexture.FULL_BRIGHT);
        block(d, pose, buf, Blocks.BARREL.defaultBlockState(), 0, 0, 0, lit);
        super.render(e, yaw, partial, pose, buf, light);
    }

    private static void block(BlockRenderDispatcher d, PoseStack pose, MultiBufferSource buf, BlockState s, int x, int y, int z, int light) {
        pose.pushPose();
        pose.translate(x, y, z);
        d.renderSingleBlock(s, pose, buf, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getTextureLocation(BoxElevatorEntity e) { return TextureAtlas.LOCATION_BLOCKS; }
}
