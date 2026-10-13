package com.krolasyon.beyblade.registry;

import com.krolasyon.beyblade.LibraBeyblade;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    private ModSounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, LibraBeyblade.MODID);

    private static RegistryObject<SoundEvent> reg(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(LibraBeyblade.MODID, name)));
    }

    public static final RegistryObject<SoundEvent> LAUNCH = reg("launch");
    public static final RegistryObject<SoundEvent> LAND = reg("land");
    public static final RegistryObject<SoundEvent> CHARGE = reg("charge");
    public static final RegistryObject<SoundEvent> SPIN_LOOP = reg("spin_loop");
    public static final RegistryObject<SoundEvent> CLASH = reg("clash");
    public static final RegistryObject<SoundEvent> CLASH_HEAVY = reg("clash_heavy");
    public static final RegistryObject<SoundEvent> WALL_HIT = reg("wall_hit");
    public static final RegistryObject<SoundEvent> SONIC_WAVE = reg("sonic_wave");
    public static final RegistryObject<SoundEvent> SHIELD_UP = reg("shield_up");
    public static final RegistryObject<SoundEvent> SHIELD_HIT = reg("shield_hit");
    public static final RegistryObject<SoundEvent> INFERNO_DRILL = reg("inferno_drill");
    public static final RegistryObject<SoundEvent> INFERNO_TORNADO = reg("inferno_tornado");
    public static final RegistryObject<SoundEvent> INFERNO_IMPACT = reg("inferno_impact");
    public static final RegistryObject<SoundEvent> BUSTER_CHARGE = reg("buster_charge");
    public static final RegistryObject<SoundEvent> BUSTER_FIRE = reg("buster_fire");
    public static final RegistryObject<SoundEvent> JUDGMENT_START = reg("judgment_start");
    public static final RegistryObject<SoundEvent> JUDGMENT_STRIKE = reg("judgment_strike");
    public static final RegistryObject<SoundEvent> DRAIN = reg("drain");
    public static final RegistryObject<SoundEvent> RECALL = reg("recall");
    public static final RegistryObject<SoundEvent> CATCH = reg("catch");
    public static final RegistryObject<SoundEvent> DEFEAT = reg("defeat");

    // L-Drago Destruction
    public static final RegistryObject<SoundEvent> DRAGON_ROAR = reg("dragon_roar");
    public static final RegistryObject<SoundEvent> VORTEX = reg("vortex");
    public static final RegistryObject<SoundEvent> DRAGON_RISE = reg("dragon_rise");
    public static final RegistryObject<SoundEvent> METEOR_CHARGE = reg("meteor_charge");
    public static final RegistryObject<SoundEvent> METEOR_IMPACT = reg("meteor_impact");
    public static final RegistryObject<SoundEvent> CLAW_SLASH = reg("claw_slash");
    public static final RegistryObject<SoundEvent> FIRE_BREATH = reg("fire_breath");

    // Storm Pegasus
    public static final RegistryObject<SoundEvent> PEGASUS_NEIGH = reg("pegasus_neigh");
    public static final RegistryObject<SoundEvent> STORM_CLOUD = reg("storm_cloud");
    public static final RegistryObject<SoundEvent> THUNDER_STRIKE = reg("thunder_strike");
    public static final RegistryObject<SoundEvent> STAR_CHIME = reg("star_chime");
    public static final RegistryObject<SoundEvent> RICOCHET = reg("ricochet");
    public static final RegistryObject<SoundEvent> GALLOP = reg("gallop");

    // Earth Eagle
    public static final RegistryObject<SoundEvent> EAGLE_SCREECH = reg("eagle_screech");
    public static final RegistryObject<SoundEvent> FEATHER_WHIRL = reg("feather_whirl");
    public static final RegistryObject<SoundEvent> FEATHER_BURST = reg("feather_burst");
    public static final RegistryObject<SoundEvent> WING_BLADE = reg("wing_blade");
    public static final RegistryObject<SoundEvent> SKY_DIVE = reg("sky_dive");
    public static final RegistryObject<SoundEvent> LANCE_RAIN = reg("lance_rain");

    // Rock Leone
    public static final RegistryObject<SoundEvent> LION_ROAR = reg("lion_roar");
    public static final RegistryObject<SoundEvent> WIND_WALL = reg("wind_wall");
    public static final RegistryObject<SoundEvent> FANG_SHOT = reg("fang_shot");
    public static final RegistryObject<SoundEvent> QUAKE = reg("quake");
    public static final RegistryObject<SoundEvent> CAGE_WIND = reg("cage_wind");
    public static final RegistryObject<SoundEvent> CAGE_BLAST = reg("cage_blast");
}
