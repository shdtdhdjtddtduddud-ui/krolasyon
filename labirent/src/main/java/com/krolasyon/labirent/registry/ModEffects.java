package com.krolasyon.labirent.registry;

import com.krolasyon.labirent.Labirent;
import com.krolasyon.labirent.effect.ChangingEffect;
import com.krolasyon.labirent.effect.VenomEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEffects {
    private ModEffects() {}

    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, Labirent.MODID);

    public static final RegistryObject<MobEffect> VENOM = EFFECTS.register("griever_venom", VenomEffect::new);
    public static final RegistryObject<MobEffect> CHANGING = EFFECTS.register("changing", ChangingEffect::new);
}
