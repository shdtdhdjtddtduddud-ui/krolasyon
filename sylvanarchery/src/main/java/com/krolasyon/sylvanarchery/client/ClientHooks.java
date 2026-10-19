package com.krolasyon.sylvanarchery.client;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public final class ClientHooks {
    private ClientHooks() {}

    private static SylvanItemRenderer renderer;

    public static IClientItemExtensions itemExtensions() {
        return new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new SylvanItemRenderer();
                return renderer;
            }
        };
    }
}
