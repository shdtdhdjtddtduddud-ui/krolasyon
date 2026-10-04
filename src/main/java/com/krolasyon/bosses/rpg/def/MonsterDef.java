package com.krolasyon.bosses.rpg.def;

import com.krolasyon.bosses.rpg.def.RpgDefs.*;

import javax.annotation.Nullable;

/** Static description of one monster or boss type. Generated rows live in {@link RpgDefs}. */
public record MonsterDef(String id, String name, Archetype arch, float scale, float hp, float atk, double speed,
                         int baseColor, int secondColor, int accentColor, int eyeColor, int parts, Ability[] abilities,
                         int traits, RegionId[] regions, int armor, @Nullable String summon, boolean glow, boolean boss) {

    public boolean has(int trait) { return (traits & trait) != 0; }
    public boolean part(int p) { return (parts & p) != 0; }
    public boolean flying() { return has(RpgDefs.T_FLYING) || has(RpgDefs.T_FLOATING); }

    /** base hitbox width/height of the archetype at scale 1 */
    public float baseWidth() {
        return switch (arch) {
            case HUMANOID -> 0.6F; case BRUTE -> 1.1F; case QUADRUPED -> 1.0F; case ARACHNID -> 1.3F; case INSECT -> 1.0F;
            case FLYER -> 0.9F; case SLIME -> 1.0F; case SERPENT -> 0.9F; case GOLEM -> 1.3F; case FLOATER -> 0.9F;
            case TREANT -> 1.1F; case CRUSTACEAN -> 1.3F;
        };
    }

    public float baseHeight() {
        return switch (arch) {
            case HUMANOID -> 1.9F; case BRUTE -> 2.2F; case QUADRUPED -> 1.2F; case ARACHNID -> 0.9F; case INSECT -> 0.9F;
            case FLYER -> 0.9F; case SLIME -> 1.0F; case SERPENT -> 0.8F; case GOLEM -> 2.5F; case FLOATER -> 1.1F;
            case TREANT -> 2.5F; case CRUSTACEAN -> 0.8F;
        };
    }

    /** hitbox scale is capped so huge bosses still fit through the world */
    public float hitWidth() { return Math.min(baseWidth() * scale, 5.0F); }
    public float hitHeight() { return Math.min(baseHeight() * scale, 9.0F); }

    public int danger() {
        if (boss) return 6;
        float s = hp + atk * 4;
        return s < 35 ? 1 : s < 55 ? 2 : s < 80 ? 3 : s < 110 ? 4 : 5;
    }
}
