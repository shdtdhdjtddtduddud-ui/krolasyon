package com.krolasyon.cille.client.render;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/** full-bright emissive layer (glowing eyes) */
public class GlowLayer<T extends Entity, M extends EntityModel<T>> extends EyesLayer<T, M> {
    private final RenderType type;

    public GlowLayer(RenderLayerParent<T, M> parent, ResourceLocation texture) {
        super(parent);
        this.type = RenderType.eyes(texture);
    }

    @Override
    public RenderType renderType() { return type; }
}
