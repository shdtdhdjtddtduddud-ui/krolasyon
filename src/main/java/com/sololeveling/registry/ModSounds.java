package com.sololeveling.registry;

import com.sololeveling.SoloLeveling;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    private ModSounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, SoloLeveling.MODID);

    private static RegistryObject<SoundEvent> reg(String n) {
        return SOUNDS.register(n, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(SoloLeveling.MODID, n)));
    }

    public static final RegistryObject<SoundEvent> SYSTEM_CHIME = reg("system_chime");
    public static final RegistryObject<SoundEvent> QUEST = reg("quest");
    public static final RegistryObject<SoundEvent> WARNING = reg("warning");
    public static final RegistryObject<SoundEvent> LEVEL_UP = reg("level_up");
    public static final RegistryObject<SoundEvent> ARISE = reg("arise");
    public static final RegistryObject<SoundEvent> GATE_HUM = reg("gate_hum");
    public static final RegistryObject<SoundEvent> GATE_ENTER = reg("gate_enter");
    public static final RegistryObject<SoundEvent> SHADOW_STEP = reg("shadow_step");
    public static final RegistryObject<SoundEvent> SKILL_CAST = reg("skill_cast");
    public static final RegistryObject<SoundEvent> ALARM = reg("alarm");
}
