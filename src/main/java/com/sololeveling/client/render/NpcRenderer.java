package com.sololeveling.client.render;

import com.sololeveling.SoloLeveling;
import com.sololeveling.entity.NpcEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

public class NpcRenderer extends HumanoidMobRenderer<NpcEntity, HumanoidModel<NpcEntity>> {
    public NpcRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5F);
        this.addLayer(new ItemInHandLayer<>(this, ctx.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(NpcEntity e) {
        return new ResourceLocation(SoloLeveling.MODID, "textures/entity/npc_" + e.skin() + ".png");
    }
}
