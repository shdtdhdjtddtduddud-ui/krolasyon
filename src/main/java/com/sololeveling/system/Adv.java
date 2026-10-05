package com.sololeveling.system;

import com.sololeveling.SoloLeveling;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class Adv {
    private Adv() {}

    public static void grant(ServerPlayer sp, String id) {
        if (sp.getServer() == null) return;
        Advancement a = sp.getServer().getAdvancements().getAdvancement(new ResourceLocation(SoloLeveling.MODID, id));
        if (a != null) sp.getAdvancements().award(a, "done");
    }
}
