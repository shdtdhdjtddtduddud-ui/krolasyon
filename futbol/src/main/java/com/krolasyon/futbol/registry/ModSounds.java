package com.krolasyon.futbol.registry;

import com.krolasyon.futbol.FutbolMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    private ModSounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, FutbolMod.MODID);

    private static RegistryObject<SoundEvent> reg(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(FutbolMod.MODID, name)));
    }

    public static final RegistryObject<SoundEvent> KICK = reg("kick");
    public static final RegistryObject<SoundEvent> KICK_POWER = reg("kick_power");
    public static final RegistryObject<SoundEvent> PASS = reg("pass");
    public static final RegistryObject<SoundEvent> HEADER = reg("header");
    public static final RegistryObject<SoundEvent> BOUNCE = reg("bounce");
    public static final RegistryObject<SoundEvent> POST_HIT = reg("post_hit");
    public static final RegistryObject<SoundEvent> NET_HIT = reg("net_hit");
    public static final RegistryObject<SoundEvent> WHISTLE = reg("whistle");
    public static final RegistryObject<SoundEvent> WHISTLE_END = reg("whistle_end");
    public static final RegistryObject<SoundEvent> CROWD_CHEER = reg("crowd_cheer");
    public static final RegistryObject<SoundEvent> CROWD_AMBIENT = reg("crowd_ambient");
    public static final RegistryObject<SoundEvent> CROWD_OOH = reg("crowd_ooh");
    public static final RegistryObject<SoundEvent> GOAL_HORN = reg("goal_horn");
    public static final RegistryObject<SoundEvent> SLIDE = reg("slide");
    public static final RegistryObject<SoundEvent> SUPER_CHARGE = reg("super_charge");
    public static final RegistryObject<SoundEvent> SKILL = reg("skill");
    public static final RegistryObject<SoundEvent> TACKLE = reg("tackle");
    public static final RegistryObject<SoundEvent> CATCH = reg("catch");
}
