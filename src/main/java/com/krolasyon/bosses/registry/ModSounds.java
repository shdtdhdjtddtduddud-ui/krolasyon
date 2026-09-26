package com.krolasyon.bosses.registry;

import com.krolasyon.bosses.KrolasyonBosses;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    private ModSounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, KrolasyonBosses.MODID);

    private static RegistryObject<SoundEvent> reg(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(KrolasyonBosses.MODID, name)));
    }

    // Seal Warden
    public static final RegistryObject<SoundEvent> WARDEN_AMBIENT = reg("warden_ambient");
    public static final RegistryObject<SoundEvent> WARDEN_HURT = reg("warden_hurt");
    public static final RegistryObject<SoundEvent> WARDEN_DEATH = reg("warden_death");
    public static final RegistryObject<SoundEvent> WARDEN_STEP = reg("warden_step");
    public static final RegistryObject<SoundEvent> WARDEN_SWIPE = reg("warden_swipe");
    public static final RegistryObject<SoundEvent> LASER_CHARGE = reg("laser_charge");
    public static final RegistryObject<SoundEvent> LASER_FIRE = reg("laser_fire");
    public static final RegistryObject<SoundEvent> CRYSTAL_ERUPT = reg("crystal_erupt");
    public static final RegistryObject<SoundEvent> PRISON_FORM = reg("prison_form");
    public static final RegistryObject<SoundEvent> PRISON_BURST = reg("prison_burst");
    public static final RegistryObject<SoundEvent> BLINK = reg("blink");
    public static final RegistryObject<SoundEvent> SUMMON = reg("summon");
    public static final RegistryObject<SoundEvent> BOLT_SHOOT = reg("bolt_shoot");
    public static final RegistryObject<SoundEvent> BOLT_HIT = reg("bolt_hit");
    public static final RegistryObject<SoundEvent> WARDEN_PHASE = reg("warden_phase");

    // Crimson Hound
    public static final RegistryObject<SoundEvent> HOUND_AMBIENT = reg("hound_ambient");
    public static final RegistryObject<SoundEvent> HOUND_HURT = reg("hound_hurt");
    public static final RegistryObject<SoundEvent> HOUND_DEATH = reg("hound_death");
    public static final RegistryObject<SoundEvent> HOUND_STEP = reg("hound_step");
    public static final RegistryObject<SoundEvent> HOUND_BITE = reg("hound_bite");
    public static final RegistryObject<SoundEvent> HOUND_LEAP = reg("hound_leap");
    public static final RegistryObject<SoundEvent> HOUND_LAND = reg("hound_land");
    public static final RegistryObject<SoundEvent> HOUND_HOWL = reg("hound_howl");
    public static final RegistryObject<SoundEvent> FISSURE = reg("fissure");
    public static final RegistryObject<SoundEvent> FIREBALL_SHOOT = reg("fireball_shoot");
    public static final RegistryObject<SoundEvent> FIREBALL_HIT = reg("fireball_hit");
    public static final RegistryObject<SoundEvent> FRENZY_SLASH = reg("frenzy_slash");
    public static final RegistryObject<SoundEvent> HOUND_PHASE = reg("hound_phase");
}
