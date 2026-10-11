package com.krolasyon.ben10.ability;

import com.krolasyon.ben10.OmniData;
import net.minecraft.server.level.ServerPlayer;

public class Xlr8Kit extends Kit {
    @Override
    public boolean start(ServerPlayer p, OmniData d, int slot) {
        return true;
    }

    @Override
    public void tick(ServerPlayer p, OmniData d, int slot, int t) {}
}
