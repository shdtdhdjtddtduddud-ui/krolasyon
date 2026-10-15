package com.krolasyon.alien.registry;

import com.krolasyon.alien.AlienMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    private ModSounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, AlienMod.MODID);

    private static RegistryObject<SoundEvent> reg(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(AlienMod.MODID, name)));
    }

    // xenomorph
    public static final RegistryObject<SoundEvent> XENO_HISS = reg("xeno_hiss");
    public static final RegistryObject<SoundEvent> XENO_SCREECH = reg("xeno_screech");
    public static final RegistryObject<SoundEvent> XENO_STEP = reg("xeno_step");
    public static final RegistryObject<SoundEvent> XENO_VENT = reg("xeno_vent");
    public static final RegistryObject<SoundEvent> XENO_SNIFF = reg("xeno_sniff");
    public static final RegistryObject<SoundEvent> XENO_KILL = reg("xeno_kill");
    public static final RegistryObject<SoundEvent> XENO_GROWL = reg("xeno_growl");
    public static final RegistryObject<SoundEvent> XENO_FLINCH = reg("xeno_flinch");
    public static final RegistryObject<SoundEvent> XENO_DROP = reg("xeno_drop");
    // player gear
    public static final RegistryObject<SoundEvent> TRACKER_PING = reg("tracker_ping");
    public static final RegistryObject<SoundEvent> TRACKER_BEEP = reg("tracker_beep");
    public static final RegistryObject<SoundEvent> FLAMETHROWER = reg("flamethrower");
    public static final RegistryObject<SoundEvent> FLAME_EMPTY = reg("flame_empty");
    public static final RegistryObject<SoundEvent> NOISEMAKER = reg("noisemaker");
    public static final RegistryObject<SoundEvent> FLARE = reg("flare");
    public static final RegistryObject<SoundEvent> MEDKIT = reg("medkit");
    public static final RegistryObject<SoundEvent> FLASHLIGHT = reg("flashlight");
    // station
    public static final RegistryObject<SoundEvent> DOOR = reg("door");
    public static final RegistryObject<SoundEvent> LOCKER = reg("locker");
    public static final RegistryObject<SoundEvent> SAVE_BEEP = reg("save_beep");
    public static final RegistryObject<SoundEvent> SAVE_DONE = reg("save_done");
    public static final RegistryObject<SoundEvent> TERMINAL = reg("terminal");
    public static final RegistryObject<SoundEvent> STATION_HUM = reg("station_hum");
    public static final RegistryObject<SoundEvent> STATION_CREAK = reg("station_creak");
    public static final RegistryObject<SoundEvent> STATION_ALARM = reg("station_alarm");
    public static final RegistryObject<SoundEvent> STATION_VENT = reg("station_vent");
    public static final RegistryObject<SoundEvent> LIGHT_BUZZ = reg("light_buzz");
    // fear
    public static final RegistryObject<SoundEvent> STINGER = reg("stinger");
    public static final RegistryObject<SoundEvent> HEARTBEAT = reg("heartbeat");
    public static final RegistryObject<SoundEvent> BREATH_HOLD = reg("breath_hold");
    public static final RegistryObject<SoundEvent> GASP = reg("gasp");
}
