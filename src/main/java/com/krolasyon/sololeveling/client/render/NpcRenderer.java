package com.krolasyon.sololeveling.client.render;

import com.krolasyon.sololeveling.SoloLeveling;
import com.krolasyon.sololeveling.entity.HunterNpc;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/** People of Seoul, drawn with player proportions and their own skins. */
public class NpcRenderer extends HumanoidMobRenderer<HunterNpc, PlayerModel<HunterNpc>> {
    private final Map<String, ResourceLocation> cache = new HashMap<>();

    public NpcRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), ctx.getModelManager()));
    }

    @Override
    public ResourceLocation getTextureLocation(HunterNpc e) {
        return cache.computeIfAbsent(e.skin(), s -> SoloLeveling.id("textures/entity/npc/" + s + ".png"));
    }
}
