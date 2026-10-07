package com.krolasyon.furniture.client.model;

import com.krolasyon.furniture.FurnitureMod;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

public final class FurnitureModelLayers {
    private FurnitureModelLayers() {}

    public static final ModelLayerLocation SOFA = loc("sofa");
    public static final ModelLayerLocation PIANO = loc("piano");
    public static final ModelLayerLocation CHALKBOARD = loc("chalkboard");
    public static final ModelLayerLocation NIGHTSTAND = loc("nightstand");
    public static final ModelLayerLocation WARDROBE = loc("wardrobe");
    public static final ModelLayerLocation BROOM = loc("broom");

    private static ModelLayerLocation loc(String name) {
        return new ModelLayerLocation(new ResourceLocation(FurnitureMod.MODID, name), "main");
    }

    public static ResourceLocation tex(String name) {
        return new ResourceLocation(FurnitureMod.MODID, "textures/entity/" + name + ".png");
    }
}
