package com.krolasyon.bosses.rpg.def;

import com.krolasyon.bosses.rpg.def.RpgDefs.School;
import com.krolasyon.bosses.rpg.def.RpgDefs.SpellShape;

public record SpellDef(String id, String name, School school, SpellShape shape, float power, int mana, int cooldown, int tier) {
    public int color() { return RpgDefs.schoolColor(school); }
    public int color2() { return RpgDefs.schoolColor2(school); }
    /** character level needed to learn the spell */
    public int requiredLevel() { return switch (tier) { case 1 -> 1; case 2 -> 5; case 3 -> 12; case 4 -> 20; default -> 30; }; }
    public boolean channeled() { return shape == SpellShape.BEAM || shape == SpellShape.CONE && cooldown <= 4; }
}
