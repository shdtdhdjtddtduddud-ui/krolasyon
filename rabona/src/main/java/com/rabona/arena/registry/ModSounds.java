package com.rabona.arena.registry;

import com.rabona.arena.RabonaArena;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, RabonaArena.MODID);

    public static final RegistryObject<SoundEvent> KICK_SOFT = reg("kick_soft");
    public static final RegistryObject<SoundEvent> KICK_HARD = reg("kick_hard");
    public static final RegistryObject<SoundEvent> KICK_POWER = reg("kick_power");
    public static final RegistryObject<SoundEvent> TOUCH = reg("touch");
    public static final RegistryObject<SoundEvent> POST = reg("post");
    public static final RegistryObject<SoundEvent> NET = reg("net");
    public static final RegistryObject<SoundEvent> BOUNCE = reg("bounce");
    public static final RegistryObject<SoundEvent> WHISTLE = reg("whistle");
    public static final RegistryObject<SoundEvent> WHISTLE_END = reg("whistle_end");
    public static final RegistryObject<SoundEvent> CROWD_GOAL = reg("crowd_goal");
    public static final RegistryObject<SoundEvent> CROWD_OOH = reg("crowd_ooh");
    public static final RegistryObject<SoundEvent> CROWD_AMBIENT = reg("crowd_ambient");
    public static final RegistryObject<SoundEvent> GOAL_HORN = reg("goal_horn");
    public static final RegistryObject<SoundEvent> SLIDE = reg("slide");
    public static final RegistryObject<SoundEvent> TACKLE = reg("tackle");
    public static final RegistryObject<SoundEvent> SKILL = reg("skill");
    public static final RegistryObject<SoundEvent> CATCH = reg("catch");
    public static final RegistryObject<SoundEvent> FIRE = reg("ability_fire");
    public static final RegistryObject<SoundEvent> THUNDER = reg("ability_thunder");
    public static final RegistryObject<SoundEvent> WIND = reg("ability_wind");
    public static final RegistryObject<SoundEvent> GHOST = reg("ability_ghost");
    public static final RegistryObject<SoundEvent> MAGNET = reg("ability_magnet");
    public static final RegistryObject<SoundEvent> ICE = reg("ability_ice");

    private static RegistryObject<SoundEvent> reg(String n) {
        return SOUNDS.register(n, () -> SoundEvent.createVariableRangeEvent(RabonaArena.id(n)));
    }
}
