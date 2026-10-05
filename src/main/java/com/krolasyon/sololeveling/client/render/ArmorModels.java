package com.krolasyon.sololeveling.client.render;

import com.krolasyon.sololeveling.SoloLeveling;
import com.krolasyon.sololeveling.item.SLArmorItem;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.*;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public final class ArmorModels {
    public static final ModelLayerLocation MONARCH = new ModelLayerLocation(SoloLeveling.id("armor_monarch"), "main");
    public static final ModelLayerLocation KNIGHT = new ModelLayerLocation(SoloLeveling.id("armor_knight"), "main");
    public static final ModelLayerLocation ORC = new ModelLayerLocation(SoloLeveling.id("armor_orc"), "main");
    public static final ModelLayerLocation HUNTER = new ModelLayerLocation(SoloLeveling.id("armor_hunter"), "main");
    public static LayerDefinition monarchLayer() { return LayerDefinition.create(new MeshDefinition(), 64, 64); }
    public static LayerDefinition knightLayer() { return monarchLayer(); }
    public static LayerDefinition orcLayer() { return monarchLayer(); }
    public static LayerDefinition hunterLayer() { return monarchLayer(); }
    public static IClientItemExtensions extensions(SLArmorItem.Mat m) { return new IClientItemExtensions() {}; }
}
