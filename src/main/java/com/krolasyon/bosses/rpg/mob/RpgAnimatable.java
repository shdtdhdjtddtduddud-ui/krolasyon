package com.krolasyon.bosses.rpg.mob;

import com.krolasyon.bosses.rpg.def.RpgDefs.Archetype;

import javax.annotation.Nullable;

/** What the shared archetype models need to know about an entity to pose it. */
public interface RpgAnimatable {
    Archetype archetype();

    /** bitmask of RpgDefs.P_* optional body parts */
    int bodyParts();

    /** current cast animation style or null */
    @Nullable AbilityLogic.Style castStyle();

    /** ticks since the current cast started (client side), -1 if none */
    float castAge(float partialTick);

    /** windup length of the current cast */
    int castWindup();

    boolean airborne();
}
