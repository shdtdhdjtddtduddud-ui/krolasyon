package com.krolasyon.mythblades.client;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public final class ClientHooks {
    private ClientHooks() {}

    public static final IClientItemExtensions ITEM_EXTENSIONS = new IClientItemExtensions() {
        private BlockEntityWithoutLevelRenderer r;

        @Override
        public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            if (r == null) r = new MythItemRenderer();
            return r;
        }
    };
}
