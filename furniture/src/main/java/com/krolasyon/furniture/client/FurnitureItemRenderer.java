package com.krolasyon.furniture.client;

import com.krolasyon.furniture.client.model.FurnitureModelLayers;
import com.krolasyon.furniture.client.render.*;
import com.krolasyon.furniture.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** renders the animated furniture models as inventory / hand / ground items (resting pose) */
public class FurnitureItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static FurnitureItemRenderer instance;

    public static FurnitureItemRenderer get() {
        if (instance == null) instance = new FurnitureItemRenderer();
        return instance;
    }

    private ModelPart sofa, piano, chalkboard, nightstand, wardrobe, broom;

    private FurnitureItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        sofa = piano = chalkboard = nightstand = wardrobe = broom = null;
    }

    private void bake() {
        if (sofa != null) return;
        var models = Minecraft.getInstance().getEntityModels();
        sofa = models.bakeLayer(FurnitureModelLayers.SOFA);
        piano = models.bakeLayer(FurnitureModelLayers.PIANO);
        chalkboard = models.bakeLayer(FurnitureModelLayers.CHALKBOARD);
        nightstand = models.bakeLayer(FurnitureModelLayers.NIGHTSTAND);
        wardrobe = models.bakeLayer(FurnitureModelLayers.WARDROBE);
        broom = models.bakeLayer(FurnitureModelLayers.BROOM);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        bake();
        Item item = stack.getItem();
        if (item == ModItems.SOFA.get()) show(sofa, SofaRenderer.TEX, 0.5F, ps, buf, light, overlay);
        else if (item == ModItems.PIANO.get()) show(piano, PianoRenderer.TEX, 0.5F, ps, buf, light, overlay);
        else if (item == ModItems.CHALKBOARD.get()) show(chalkboard, ChalkboardRenderer.TEX, 0.62F, ps, buf, light, overlay);
        else if (item == ModItems.NIGHTSTAND.get()) show(nightstand, NightstandRenderer.TEX, 0.92F, ps, buf, light, overlay);
        else if (item == ModItems.WARDROBE.get()) show(wardrobe, WardrobeRenderer.TEX, 0.62F, ps, buf, light, overlay);
        else if (item == ModItems.BROOM.get()) {
            // upright broom centred on the grip (handle middle), head down
            ps.pushPose();
            ps.translate(0.5D, 0.5D, 0.5D);
            ps.scale(0.62F, 0.62F, 0.62F);
            ps.translate(0.0D, -0.80D, 0.0D);
            FurnitureDraw.draw(broom, FurnitureModelLayers.tex("broom"), ps, buf, light, overlay, 0.0D, 0.0D, Direction.NORTH);
            ps.popPose();
        }
    }

    private static void show(ModelPart root, ResourceLocation tex, float scale, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0.0D, 0.5D);
        ps.scale(scale, scale, scale);
        FurnitureDraw.draw(root, tex, ps, buf, light, overlay, 0.0D, 0.0D, Direction.NORTH);
        ps.popPose();
    }
}
