package com.krolasyon.furniture.client;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public class FurnitureItemExt implements IClientItemExtensions {
    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        return FurnitureItemRenderer.get();
    }
}
