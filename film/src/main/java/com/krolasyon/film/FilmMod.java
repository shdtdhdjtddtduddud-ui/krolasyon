package com.krolasyon.film;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

/** Yalnızca -Dfilm.run=true ile (CI) etkin: dünyayı kurar, senaryoyu oynar, kareleri ve sesleri kaydeder. */
@Mod(FilmMod.MODID)
public final class FilmMod {
    public static final String MODID = "krolasyonfilm";

    public FilmMod() {
        if (FMLEnvironment.dist == Dist.CLIENT && Boolean.getBoolean("film.run")) {
            MinecraftForge.EVENT_BUS.register(new Director(Scenes.build()));
        }
    }
}
