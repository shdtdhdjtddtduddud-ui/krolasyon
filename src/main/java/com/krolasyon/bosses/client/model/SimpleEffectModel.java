package com.krolasyon.bosses.client.model;

import com.krolasyon.bosses.KrolasyonBosses;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/** static helper model for effect entities (crystal spikes, fire pillars, watcher orbs) */
public class SimpleEffectModel<T extends Entity> extends HierarchicalModel<T> {
    public static final ModelLayerLocation CRYSTAL_SPIKE = new ModelLayerLocation(new ResourceLocation(KrolasyonBosses.MODID, "crystal_spike"), "main");
    public static final ModelLayerLocation FIRE_PILLAR = new ModelLayerLocation(new ResourceLocation(KrolasyonBosses.MODID, "fire_pillar"), "main");
    public static final ModelLayerLocation WATCHER = new ModelLayerLocation(new ResourceLocation(KrolasyonBosses.MODID, "watcher"), "main");

    private final ModelPart root;

    public SimpleEffectModel(ModelPart root) {
        super(RenderType::entityCutout);
        this.root = root;
    }

    @Override
    public ModelPart root() { return root; }

    @Override
    public void setupAnim(T e, float a, float b, float c, float d, float f) {}
}
