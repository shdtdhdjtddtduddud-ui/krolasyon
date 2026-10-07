package com.krolasyon.furniture.registry;

import com.krolasyon.furniture.FurnitureMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    private ModSounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, FurnitureMod.MODID);

    public static final RegistryObject<SoundEvent> PIANO_C4 = reg("piano_c4");
    public static final RegistryObject<SoundEvent> PIANO_C5 = reg("piano_c5");

    private static RegistryObject<SoundEvent> reg(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(FurnitureMod.MODID, name)));
    }
}
