package com.krolasyon.labirent.registry;

import com.krolasyon.labirent.Labirent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    private ModSounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Labirent.MODID);

    public static final RegistryObject<SoundEvent> WALL_GRIND = reg("wall_grind");
    public static final RegistryObject<SoundEvent> DOOR_CLOSE = reg("door_close");
    public static final RegistryObject<SoundEvent> DOOR_SLAM = reg("door_slam");
    public static final RegistryObject<SoundEvent> DOOR_OPEN = reg("door_open");
    public static final RegistryObject<SoundEvent> BOX_ALARM = reg("box_alarm");
    public static final RegistryObject<SoundEvent> BOX_RISE = reg("box_rise");
    public static final RegistryObject<SoundEvent> BOX_ARRIVE = reg("box_arrive");
    public static final RegistryObject<SoundEvent> GRIEVER_AMBIENT = reg("griever_ambient");
    public static final RegistryObject<SoundEvent> GRIEVER_HURT = reg("griever_hurt");
    public static final RegistryObject<SoundEvent> GRIEVER_DEATH = reg("griever_death");
    public static final RegistryObject<SoundEvent> GRIEVER_STEP = reg("griever_step");
    public static final RegistryObject<SoundEvent> GRIEVER_STING = reg("griever_sting");
    public static final RegistryObject<SoundEvent> GRIEVER_ROLL = reg("griever_roll");
    public static final RegistryObject<SoundEvent> BEETLE_SCUTTLE = reg("beetle_scuttle");
    public static final RegistryObject<SoundEvent> STORY_STING = reg("story_sting");
    public static final RegistryObject<SoundEvent> MAZE_AMBIENT = reg("maze_ambient");
    public static final RegistryObject<SoundEvent> SERUM_INJECT = reg("serum_inject");
    public static final RegistryObject<SoundEvent> TERMINAL_BEEP = reg("terminal_beep");
    public static final RegistryObject<SoundEvent> ESCAPE = reg("escape");

    private static RegistryObject<SoundEvent> reg(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(Labirent.MODID, name)));
    }
}
