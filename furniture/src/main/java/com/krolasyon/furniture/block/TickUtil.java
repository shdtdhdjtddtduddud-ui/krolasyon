package com.krolasyon.furniture.block;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class TickUtil {
    private TickUtil() {}

    @SuppressWarnings("unchecked")
    public static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> ticker(BlockEntityType<A> given, BlockEntityType<E> expected, BlockEntityTicker<? super E> t) {
        return expected == given ? (BlockEntityTicker<A>) t : null;
    }
}
