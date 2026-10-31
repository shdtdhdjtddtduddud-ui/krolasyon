package com.krolasyon.cille.client.model;

import com.krolasyon.cille.CilleMod;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/** static helper model for effects (marble / shield / black hole sphere, shadow spike) */
public class SimpleModel<T extends Entity> extends HierarchicalModel<T> {
    public static final ModelLayerLocation SPHERE = new ModelLayerLocation(new ResourceLocation(CilleMod.MODID, "sphere"), "main");
    public static final ModelLayerLocation FISH = new ModelLayerLocation(new ResourceLocation(CilleMod.MODID, "fish"), "main");
    public static final ModelLayerLocation SHADOW_SPIKE = new ModelLayerLocation(new ResourceLocation(CilleMod.MODID, "shadow_spike"), "main");

    private final ModelPart root;

    public SimpleModel(ModelPart root) {
        super(RenderType::entityTranslucent);
        this.root = root;
    }

    @Override
    public ModelPart root() { return root; }

    @Override
    public void setupAnim(T e, float a, float b, float c, float d, float f) {}
}
