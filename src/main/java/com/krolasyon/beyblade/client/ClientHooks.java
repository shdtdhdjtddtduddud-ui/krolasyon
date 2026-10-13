package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.entity.BeybladeEntity;
import net.minecraft.client.Minecraft;

public final class ClientHooks {
    private ClientHooks() {}

    public static void startSpinSound(BeybladeEntity e) {
        Minecraft.getInstance().getSoundManager().play(new SpinSound(e));
    }
}
