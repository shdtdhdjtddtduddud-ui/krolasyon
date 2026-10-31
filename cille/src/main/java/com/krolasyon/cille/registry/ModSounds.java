package com.krolasyon.cille.registry;

import com.krolasyon.cille.CilleMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    private ModSounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, CilleMod.MODID);

    private static RegistryObject<SoundEvent> reg(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CilleMod.MODID, name)));
    }

    public static final RegistryObject<SoundEvent> AMBIENT = reg("kabkaba_ambient");
    public static final RegistryObject<SoundEvent> HURT = reg("kabkaba_hurt");
    public static final RegistryObject<SoundEvent> DEATH = reg("kabkaba_death");
    public static final RegistryObject<SoundEvent> STEP = reg("kabkaba_step");
    public static final RegistryObject<SoundEvent> ROAR = reg("kabkaba_roar");
    public static final RegistryObject<SoundEvent> CLAW = reg("claw_swipe");
    public static final RegistryObject<SoundEvent> HOLE_OPEN = reg("black_hole_open");
    public static final RegistryObject<SoundEvent> HOLE_LOOP = reg("black_hole_loop");
    public static final RegistryObject<SoundEvent> HOLE_COLLAPSE = reg("black_hole_collapse");
    public static final RegistryObject<SoundEvent> BLINK = reg("blink");
    public static final RegistryObject<SoundEvent> DRAIN = reg("drain");
    public static final RegistryObject<SoundEvent> REFLECT = reg("reflect");
    public static final RegistryObject<SoundEvent> REFLECT_HIT = reg("reflect_hit");
    public static final RegistryObject<SoundEvent> SHADOW_RISE = reg("shadow_rise");
    public static final RegistryObject<SoundEvent> SLAM = reg("slam");
    public static final RegistryObject<SoundEvent> GUARD = reg("guard");
    public static final RegistryObject<SoundEvent> MARBLE_THROW = reg("marble_throw");
    public static final RegistryObject<SoundEvent> MARBLE_CLACK = reg("marble_clack");
    public static final RegistryObject<SoundEvent> MARBLE_BURST = reg("marble_burst");

    public static final RegistryObject<SoundEvent> ICE_HIT = reg("ice_hit");
    public static final RegistryObject<SoundEvent> ICE_BREAK = reg("ice_break");
    public static final RegistryObject<SoundEvent> BEHMUD_AMBIENT = reg("behmud_ambient");
    public static final RegistryObject<SoundEvent> BEHMUD_HURT = reg("behmud_hurt");
    public static final RegistryObject<SoundEvent> BEHMUD_DEATH = reg("behmud_death");
    public static final RegistryObject<SoundEvent> ICE_BEAM = reg("ice_beam");
    public static final RegistryObject<SoundEvent> ICE_FORM = reg("ice_form");
    public static final RegistryObject<SoundEvent> WHIRLPOOL = reg("whirlpool");
    public static final RegistryObject<SoundEvent> SONIC_WAVE = reg("sonic_wave");
    public static final RegistryObject<SoundEvent> FISH_LAUNCH = reg("fish_launch");
    public static final RegistryObject<SoundEvent> WATER_BOOM = reg("water_boom");
    public static final RegistryObject<SoundEvent> TORPEDO = reg("torpedo");
    public static final RegistryObject<SoundEvent> RUSH_DASH = reg("rush_dash");
    public static final RegistryObject<SoundEvent> WATERSPOUT = reg("waterspout");
    public static final RegistryObject<SoundEvent> CLAMP_BITE = reg("clamp_bite");
    public static final RegistryObject<SoundEvent> SEAL_RELEASE = reg("seal_release");
    public static final RegistryObject<SoundEvent> SPACE_POWER = reg("space_power");
}
