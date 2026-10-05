package com.krolasyon.sololeveling.registry;

import com.krolasyon.sololeveling.SoloLeveling;
import com.krolasyon.sololeveling.entity.MobKind;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> REG = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, SoloLeveling.MODID);
    public static final List<String> IDS = new ArrayList<>();

    public static final RegistryObject<SoundEvent> SYSTEM = s("system");
    public static final RegistryObject<SoundEvent> SYSTEM_WARN = s("system_warn");
    public static final RegistryObject<SoundEvent> LEVEL_UP = s("level_up");
    public static final RegistryObject<SoundEvent> NEWS = s("news");
    public static final RegistryObject<SoundEvent> ARISE = s("arise");
    public static final RegistryObject<SoundEvent> DASH = s("dash");
    public static final RegistryObject<SoundEvent> SLASH = s("slash");
    public static final RegistryObject<SoundEvent> BLOODLUST = s("bloodlust");
    public static final RegistryObject<SoundEvent> AUTHORITY = s("authority");
    public static final RegistryObject<SoundEvent> ROAR = s("roar");
    public static final RegistryObject<SoundEvent> DOMAIN = s("domain");
    public static final RegistryObject<SoundEvent> HOWL = s("howl");
    public static final RegistryObject<SoundEvent> LASER = s("laser");
    public static final RegistryObject<SoundEvent> FIRE = s("fire");
    public static final RegistryObject<SoundEvent> THUNDER = s("thunder");
    public static final RegistryObject<SoundEvent> SLAM = s("slam");
    public static final RegistryObject<SoundEvent> BOSS_DEATH = s("boss_death");
    public static final RegistryObject<SoundEvent> GATE_HUM = s("gate_hum");
    public static final RegistryObject<SoundEvent> GATE_ENTER = s("gate_enter");
    public static final RegistryObject<SoundEvent> SMALL_VOICE = s("small_voice");
    public static final RegistryObject<SoundEvent> BEAST_GROWL = s("beast_growl");
    public static final RegistryObject<SoundEvent> INSECT_CLICK = s("insect_click");
    public static final RegistryObject<SoundEvent> STONE_GRIND = s("stone_grind");
    public static final RegistryObject<SoundEvent> ARMOR_CLANK = s("armor_clank");
    public static final RegistryObject<SoundEvent> DEMON_GROWL = s("demon_growl");
    public static final RegistryObject<SoundEvent> SERPENT_HISS = s("serpent_hiss");
    public static final RegistryObject<SoundEvent> HURT_FLESH = s("hurt_flesh");
    public static final RegistryObject<SoundEvent> HURT_HARD = s("hurt_hard");

    private ModSounds() {}

    private static RegistryObject<SoundEvent> s(String id) {
        IDS.add(id);
        return REG.register(id, () -> SoundEvent.createVariableRangeEvent(SoloLeveling.id(id)));
    }

    public static SoundEvent mobAmbient(MobKind k) {
        return switch (k) {
            case GOBLIN, HOBGOBLIN, ICE_ELF -> SMALL_VOICE.get();
            case STEEL_FANGED_LYCAN, ICE_BEAR, CERBERUS -> BEAST_GROWL.get();
            case GIANT_CENTIPEDE, ANT_SOLDIER, BERU -> INSECT_CLICK.get();
            case STONE_STATUE, STATUE_OF_GOD -> STONE_GRIND.get();
            case CASTLE_KNIGHT, IGRIS -> ARMOR_CLANK.get();
            case KASAKA -> SERPENT_HISS.get();
            default -> DEMON_GROWL.get();
        };
    }

    public static SoundEvent mobHurt(MobKind k) {
        return switch (k.category) {
            case CONSTRUCT, KNIGHT, INSECT -> HURT_HARD.get();
            default -> HURT_FLESH.get();
        };
    }
}
