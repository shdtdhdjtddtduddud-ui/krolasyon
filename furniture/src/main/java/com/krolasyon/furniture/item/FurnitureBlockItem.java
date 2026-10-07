package com.krolasyon.furniture.item;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

public class FurnitureBlockItem extends BlockItem {
    public FurnitureBlockItem(Block block, Properties props) {
        super(block, props);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new com.krolasyon.furniture.client.FurnitureItemExt());
    }
}
