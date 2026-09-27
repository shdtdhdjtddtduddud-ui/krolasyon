package com.krolasyon.bosses.morph;

import com.krolasyon.bosses.morph.abilities.*;
import com.krolasyon.bosses.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;

/** Registry of the transformations: ids, names, cooldowns, colours and sound sets. */
public final class Forms {
    private Forms() {}

    public static final int AIGOAR = 0, SOLAR = 1, DRAGON = 2, FLAME = 3, SHADE = 4, LANCER = 5, COUNT = 6;
    public static final String[] KEY = {"aigoar", "solar", "dragon", "flame", "shade", "lancer"};

    public static final int[][] COOLDOWN = {
            {120, 280, 200, 320, 440},
            {140, 220, 300, 120, 460},
            {300, 200, 360, 240, 480},
            {100, 320, 260, 160, 520},
            {120, 200, 220, 260, 600},
            {160, 180, 220, 300, 520}};

    /** main and secondary particle colours */
    public static final int[] COLOR = {0x3FE0E6, 0xFF9A2A, 0xFF6A20, 0xFFB030, 0xD01828, 0xFF5020};
    public static final int[] COLOR2 = {0x7FFFF6, 0xFFE070, 0xFFC050, 0xFFF0A0, 0x200008, 0xFFC040};

    public static final FormAbilities[] LOGIC = {new AigoarAbilities(), new SolarAbilities(), new DragonAbilities(),
            new FlameAbilities(), new ShadeAbilities(), new LancerAbilities()};

    public static boolean valid(int f) { return f >= 0 && f < COUNT; }

    public static boolean fiery(int f) { return f == SOLAR || f == DRAGON || f == FLAME || f == LANCER; }

    public static int duration(int f, int id) { return FormDurations.TICKS[f][id]; }

    public static SoundEvent ambient(int f) {
        return switch (f) {
            case SOLAR -> ModSounds.SOLAR_AMBIENT.get(); case DRAGON -> ModSounds.DRAGON_AMBIENT.get(); case FLAME -> ModSounds.FLAME_AMBIENT.get();
            case SHADE -> ModSounds.SHADE_AMBIENT.get(); case LANCER -> ModSounds.LANCER_AMBIENT.get(); default -> ModSounds.AIGOAR_AMBIENT.get();
        };
    }

    public static SoundEvent hurt(int f) {
        return switch (f) {
            case SOLAR -> ModSounds.SOLAR_HURT.get(); case DRAGON -> ModSounds.DRAGON_HURT.get(); case FLAME -> ModSounds.FLAME_HURT.get();
            case SHADE -> ModSounds.SHADE_HURT.get(); case LANCER -> ModSounds.LANCER_HURT.get(); default -> ModSounds.AIGOAR_HURT.get();
        };
    }

    public static SoundEvent death(int f) {
        return switch (f) {
            case SOLAR -> ModSounds.SOLAR_DEATH.get(); case DRAGON -> ModSounds.DRAGON_DEATH.get(); case FLAME -> ModSounds.FLAME_DEATH.get();
            case SHADE -> ModSounds.SHADE_DEATH.get(); case LANCER -> ModSounds.LANCER_DEATH.get(); default -> ModSounds.AIGOAR_DEATH.get();
        };
    }

    public static SoundEvent step(int f) {
        return switch (f) {
            case SOLAR -> ModSounds.SOLAR_STEP.get(); case DRAGON -> ModSounds.DRAGON_STEP.get(); case FLAME -> ModSounds.FLAME_STEP.get();
            case SHADE -> ModSounds.SHADE_STEP.get(); case LANCER -> ModSounds.LANCER_STEP.get(); default -> ModSounds.AIGOAR_STEP.get();
        };
    }

    public static SoundEvent swing(int f) {
        return switch (f) {
            case SOLAR, DRAGON -> ModSounds.HEAVY_SWING.get(); case FLAME, SHADE -> ModSounds.BLADE_SWING.get();
            case LANCER -> ModSounds.SPEAR_SWING.get(); default -> ModSounds.CLAW_SWIPE.get();
        };
    }

    public static SoundEvent transform(int f) {
        return switch (f) {
            case SOLAR -> ModSounds.SOLAR_TRANSFORM.get(); case DRAGON -> ModSounds.DRAGON_TRANSFORM.get(); case FLAME -> ModSounds.FLAME_TRANSFORM.get();
            case SHADE -> ModSounds.SHADE_TRANSFORM.get(); case LANCER -> ModSounds.LANCER_TRANSFORM.get(); default -> ModSounds.TRANSFORM.get();
        };
    }

    public static SoundEvent revert(int f) {
        return switch (f) {
            case SOLAR -> ModSounds.SOLAR_REVERT.get(); case DRAGON -> ModSounds.DRAGON_REVERT.get(); case FLAME -> ModSounds.FLAME_REVERT.get();
            case SHADE -> ModSounds.SHADE_REVERT.get(); case LANCER -> ModSounds.LANCER_REVERT.get(); default -> ModSounds.REVERT.get();
        };
    }
}
