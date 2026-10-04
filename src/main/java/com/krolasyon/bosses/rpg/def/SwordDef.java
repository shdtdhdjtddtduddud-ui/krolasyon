package com.krolasyon.bosses.rpg.def;

import com.krolasyon.bosses.rpg.def.RpgDefs.SwordPassive;
import com.krolasyon.bosses.rpg.def.RpgDefs.SwordSkill;

public record SwordDef(String id, String name, float damage, float speed, int tier, int blade, int guard, int grip, int gem,
                       SwordPassive passive, SwordSkill skill) {}
