package com.krolasyon.bosses.client;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;

/** Sky and fog behaviour of Azrakor: no sun or clouds, the horizon simply burns in the colour of the biome fog. */
public class AzrakorEffects extends DimensionSpecialEffects {
    public AzrakorEffects() {
        super(Float.NaN, true, SkyType.NONE, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fog, float brightness) { return fog; }

    @Override
    public boolean isFoggyAt(int x, int z) { return false; }
}
