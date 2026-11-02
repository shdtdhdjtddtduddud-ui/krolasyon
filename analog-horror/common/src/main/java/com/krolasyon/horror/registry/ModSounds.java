package com.krolasyon.horror.registry;

import com.krolasyon.horror.HorrorMod;
import com.krolasyon.horror.compat.Compat;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> REG = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, HorrorMod.MODID);

    public static final RegistryObject<SoundEvent> AMBIENT = reg("hatman.ambient");
    public static final RegistryObject<SoundEvent> STEP = reg("hatman.step");
    public static final RegistryObject<SoundEvent> HURT = reg("hatman.hurt");
    public static final RegistryObject<SoundEvent> DEATH = reg("hatman.death");
    public static final RegistryObject<SoundEvent> SCREAM = reg("hatman.scream");
    public static final RegistryObject<SoundEvent> SWIPE = reg("hatman.swipe");
    public static final RegistryObject<SoundEvent> HIT = reg("hatman.hit");
    public static final RegistryObject<SoundEvent> GRAB = reg("hatman.grab");
    public static final RegistryObject<SoundEvent> LEAP = reg("hatman.leap");
    public static final RegistryObject<SoundEvent> TWITCH = reg("hatman.twitch");
    public static final RegistryObject<SoundEvent> HAT_TIP = reg("hatman.hat_tip");
    public static final RegistryObject<SoundEvent> VANISH = reg("hatman.vanish");
    public static final RegistryObject<SoundEvent> EMERGE = reg("hatman.emerge");
    public static final RegistryObject<SoundEvent> EXTINGUISH = reg("hatman.extinguish");
    public static final RegistryObject<SoundEvent> STATIC = reg("fx.static");
    public static final RegistryObject<SoundEvent> HEARTBEAT = reg("fx.heartbeat");
    public static final RegistryObject<SoundEvent> JUMPSCARE = reg("fx.jumpscare");
    public static final RegistryObject<SoundEvent> KNOCK = reg("fx.knock");
    public static final RegistryObject<SoundEvent> WHISPER = reg("fx.whisper");
    public static final RegistryObject<SoundEvent> FAKE_STEP = reg("fx.fake_step");
    public static final RegistryObject<SoundEvent> DRONE = reg("fx.drone");
    public static final RegistryObject<SoundEvent> DISTANT_CALL = reg("fx.distant_call");

    public static final String[] STORY_NAMES = {"jingle", "music_box", "applause", "phone_beep", "rain", "house_amb", "door_slam", "power_on", "tv_on",
            "radio_sweep", "fire", "page", "vcr_insert", "brim_voice", "pickup", "candle_light", "balloon_pop", "dial_click", "fuse_spark"};

    static {
        for (String n : STORY_NAMES) reg("story." + n);
    }

    /** looks up any of this mod's sound events by its short name, e.g. "story.jingle" or "fx.static" */
    public static SoundEvent byName(String name) {
        return net.minecraftforge.registries.ForgeRegistries.SOUND_EVENTS.getValue(Compat.id(name));
    }

    private static RegistryObject<SoundEvent> reg(String name) {
        return REG.register(name, () -> SoundEvent.createVariableRangeEvent(Compat.id(name)));
    }

    private ModSounds() {}
}
