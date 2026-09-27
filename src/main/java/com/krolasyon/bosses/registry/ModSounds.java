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

    // Revenge + Heartbreaker Demon
    public static final RegistryObject<SoundEvent> REVENGE_AMBIENT = reg("revenge_ambient");
    public static final RegistryObject<SoundEvent> REVENGE_HURT = reg("revenge_hurt");
    public static final RegistryObject<SoundEvent> REVENGE_DEATH = reg("revenge_death");
    public static final RegistryObject<SoundEvent> REVENGE_STEP = reg("revenge_step");
    public static final RegistryObject<SoundEvent> SPEAR_THRUST = reg("spear_thrust");
    public static final RegistryObject<SoundEvent> LANCE_THROW = reg("lance_throw");
    public static final RegistryObject<SoundEvent> LANCE_IMPACT = reg("lance_impact");
    public static final RegistryObject<SoundEvent> BLOOD_WAVE = reg("blood_wave");
    public static final RegistryObject<SoundEvent> BLADE_SLASH = reg("blade_slash");
    public static final RegistryObject<SoundEvent> DRAIN = reg("drain");
    public static final RegistryObject<SoundEvent> VENGEANCE_CHARGE = reg("vengeance_charge");
    public static final RegistryObject<SoundEvent> VENGEANCE_BURST = reg("vengeance_burst");
    public static final RegistryObject<SoundEvent> REVENGE_PHASE = reg("revenge_phase");
    public static final RegistryObject<SoundEvent> DEMON_AMBIENT = reg("demon_ambient");
    public static final RegistryObject<SoundEvent> DEMON_HURT = reg("demon_hurt");
    public static final RegistryObject<SoundEvent> DEMON_DEATH = reg("demon_death");
    public static final RegistryObject<SoundEvent> DEMON_STEP = reg("demon_step");
    public static final RegistryObject<SoundEvent> THORN_WHIP = reg("thorn_whip");
    public static final RegistryObject<SoundEvent> HEART_SHOOT = reg("heart_shoot");
    public static final RegistryObject<SoundEvent> HEART_HIT = reg("heart_hit");
    public static final RegistryObject<SoundEvent> SHADOW_DASH = reg("shadow_dash");
    public static final RegistryObject<SoundEvent> THORN_ERUPT = reg("thorn_erupt");
    public static final RegistryObject<SoundEvent> SCYTHE_SPIN = reg("scythe_spin");
    public static final RegistryObject<SoundEvent> DEMON_PHASE = reg("demon_phase");

    // Aigoar (player transformation)
    public static final RegistryObject<SoundEvent> AIGOAR_AMBIENT = reg("aigoar_ambient");
    public static final RegistryObject<SoundEvent> AIGOAR_HURT = reg("aigoar_hurt");
    public static final RegistryObject<SoundEvent> AIGOAR_DEATH = reg("aigoar_death");
    public static final RegistryObject<SoundEvent> AIGOAR_STEP = reg("aigoar_step");
    public static final RegistryObject<SoundEvent> AIGOAR_LAND = reg("aigoar_land");
    public static final RegistryObject<SoundEvent> CLAW_SWIPE = reg("claw_swipe");
    public static final RegistryObject<SoundEvent> TRANSFORM = reg("transform");
    public static final RegistryObject<SoundEvent> REVERT = reg("revert");
    public static final RegistryObject<SoundEvent> TIDAL_REND = reg("tidal_rend");
    public static final RegistryObject<SoundEvent> MAELSTROM = reg("maelstrom");
    public static final RegistryObject<SoundEvent> VORTEX_LOOP = reg("vortex_loop");
    public static final RegistryObject<SoundEvent> VORTEX_BURST = reg("vortex_burst");
    public static final RegistryObject<SoundEvent> GEYSER_SLAM = reg("geyser_slam");
    public static final RegistryObject<SoundEvent> GEYSER_ERUPT = reg("geyser_erupt");
    public static final RegistryObject<SoundEvent> BEAM_CHARGE = reg("beam_charge");
    public static final RegistryObject<SoundEvent> BEAM_FIRE = reg("beam_fire");
    public static final RegistryObject<SoundEvent> TSUNAMI_LEAP = reg("tsunami_leap");
    public static final RegistryObject<SoundEvent> TSUNAMI_CRASH = reg("tsunami_crash");
    public static final RegistryObject<SoundEvent> DOUBLE_JUMP = reg("double_jump");

    // fire / shadow forms
    public static final RegistryObject<SoundEvent> SOLAR_AMBIENT = reg("solar_ambient");
    public static final RegistryObject<SoundEvent> SOLAR_HURT = reg("solar_hurt");
    public static final RegistryObject<SoundEvent> SOLAR_DEATH = reg("solar_death");
    public static final RegistryObject<SoundEvent> SOLAR_STEP = reg("solar_step");
    public static final RegistryObject<SoundEvent> SOLAR_TRANSFORM = reg("solar_transform");
    public static final RegistryObject<SoundEvent> SOLAR_REVERT = reg("solar_revert");
    public static final RegistryObject<SoundEvent> DRAGON_AMBIENT = reg("dragon_ambient");
    public static final RegistryObject<SoundEvent> DRAGON_HURT = reg("dragon_hurt");
    public static final RegistryObject<SoundEvent> DRAGON_DEATH = reg("dragon_death");
    public static final RegistryObject<SoundEvent> DRAGON_STEP = reg("dragon_step");
    public static final RegistryObject<SoundEvent> DRAGON_TRANSFORM = reg("dragon_transform");
    public static final RegistryObject<SoundEvent> DRAGON_REVERT = reg("dragon_revert");
    public static final RegistryObject<SoundEvent> FLAME_AMBIENT = reg("flame_ambient");
    public static final RegistryObject<SoundEvent> FLAME_HURT = reg("flame_hurt");
    public static final RegistryObject<SoundEvent> FLAME_DEATH = reg("flame_death");
    public static final RegistryObject<SoundEvent> FLAME_STEP = reg("flame_step");
    public static final RegistryObject<SoundEvent> FLAME_TRANSFORM = reg("flame_transform");
    public static final RegistryObject<SoundEvent> FLAME_REVERT = reg("flame_revert");
    public static final RegistryObject<SoundEvent> SHADE_AMBIENT = reg("shade_ambient");
    public static final RegistryObject<SoundEvent> SHADE_HURT = reg("shade_hurt");
    public static final RegistryObject<SoundEvent> SHADE_DEATH = reg("shade_death");
    public static final RegistryObject<SoundEvent> SHADE_STEP = reg("shade_step");
    public static final RegistryObject<SoundEvent> SHADE_TRANSFORM = reg("shade_transform");
    public static final RegistryObject<SoundEvent> SHADE_REVERT = reg("shade_revert");
    public static final RegistryObject<SoundEvent> LANCER_AMBIENT = reg("lancer_ambient");
    public static final RegistryObject<SoundEvent> LANCER_HURT = reg("lancer_hurt");
    public static final RegistryObject<SoundEvent> LANCER_DEATH = reg("lancer_death");
    public static final RegistryObject<SoundEvent> LANCER_STEP = reg("lancer_step");
    public static final RegistryObject<SoundEvent> LANCER_TRANSFORM = reg("lancer_transform");
    public static final RegistryObject<SoundEvent> LANCER_REVERT = reg("lancer_revert");
    public static final RegistryObject<SoundEvent> CLEAVE = reg("cleave");
    public static final RegistryObject<SoundEvent> CYCLONE = reg("cyclone");
    public static final RegistryObject<SoundEvent> SEAL_CHARGE = reg("seal_charge");
    public static final RegistryObject<SoundEvent> PHOENIX_DASH = reg("phoenix_dash");
    public static final RegistryObject<SoundEvent> METEOR_CALL = reg("meteor_call");
    public static final RegistryObject<SoundEvent> BREATH = reg("breath");
    public static final RegistryObject<SoundEvent> FIREBALL_WHOOSH = reg("fireball_whoosh");
    public static final RegistryObject<SoundEvent> SHIELD_UP = reg("shield_up");
    public static final RegistryObject<SoundEvent> DIVE = reg("dive");
    public static final RegistryObject<SoundEvent> DRAGON_SUMMON = reg("dragon_summon");
    public static final RegistryObject<SoundEvent> FLAME_BLINK = reg("flame_blink");
    public static final RegistryObject<SoundEvent> EMBER_RAIN = reg("ember_rain");
    public static final RegistryObject<SoundEvent> NOVA = reg("nova");
    public static final RegistryObject<SoundEvent> CRESCENT = reg("crescent");
    public static final RegistryObject<SoundEvent> PHOENIX_RISE = reg("phoenix_rise");
    public static final RegistryObject<SoundEvent> SHADOW_STEP = reg("shadow_step");
    public static final RegistryObject<SoundEvent> BLOOD_FLURRY = reg("blood_flurry");
    public static final RegistryObject<SoundEvent> TAIL_LASH = reg("tail_lash");
    public static final RegistryObject<SoundEvent> AWAKEN = reg("awaken");
    public static final RegistryObject<SoundEvent> SPEAR_THROW = reg("spear_throw");
    public static final RegistryObject<SoundEvent> SPEAR_IMPACT = reg("spear_impact");
    public static final RegistryObject<SoundEvent> CHARGE = reg("charge");
    public static final RegistryObject<SoundEvent> LAVA_ERUPT = reg("lava_erupt");
    public static final RegistryObject<SoundEvent> HELL_GATE = reg("hell_gate");
    public static final RegistryObject<SoundEvent> SPEAR_FALL = reg("spear_fall");
    public static final RegistryObject<SoundEvent> FIRE_EXPLODE = reg("fire_explode");
    public static final RegistryObject<SoundEvent> HEAVY_SWING = reg("heavy_swing");
    public static final RegistryObject<SoundEvent> BLADE_SWING = reg("blade_swing");
    public static final RegistryObject<SoundEvent> SPEAR_SWING = reg("spear_swing");
}
