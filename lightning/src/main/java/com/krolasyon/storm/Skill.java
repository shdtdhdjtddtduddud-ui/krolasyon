package com.krolasyon.storm;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;

/**
 * The 14 skills of the lightning tree. gx/gy are the icon centres in the reference artwork (483x680),
 * the skill tree screen renders in that coordinate space.
 */
public enum Skill {
    // tier 1 (roots)
    THUNDER_STRIKE("thunder_strike", 1, 2, 20, 60, false, Anim.STRIKE, 127, 95),
    STATIC_CORE("static_core", 1, 2, 0, 0, true, Anim.NONE, 354, 95),
    // tier 2
    LIGHTNING_BOLT("lightning_bolt", 2, 5, 15, 16, false, Anim.THRUST, 70, 262),
    SPARK_BURST("spark_burst", 2, 5, 30, 100, false, Anim.BURST, 184, 262),
    STORM_DOME("storm_dome", 2, 5, 40, 600, false, Anim.DOME, 297, 262),
    STATIC_RING("static_ring", 2, 5, 35, 400, false, Anim.CIRCLE, 411, 262),
    // tier 3
    THUNDERSTORM("thunderstorm", 3, 8, 55, 400, false, Anim.STRIKE, 70, 428),
    STORM_AVATAR("storm_avatar", 3, 8, 50, 900, false, Anim.AVATAR, 184, 428),
    SHOCK_GRASP("shock_grasp", 3, 8, 35, 240, false, Anim.GRASP, 297, 428),
    BALL_LIGHTNING("ball_lightning", 3, 8, 45, 300, false, Anim.PUSH, 411, 428),
    // tier 4 (needs any two tier 3 skills)
    CHAIN_LIGHTNING("chain_lightning", 4, 12, 40, 120, false, Anim.THRUST, 70, 596),
    THUNDER_NOVA("thunder_nova", 4, 12, 80, 1200, false, Anim.NOVA, 184, 596),
    LIGHTNING_SPEAR("lightning_spear", 4, 12, 60, 400, false, Anim.SPEAR, 297, 596),
    STORM_REAPER("storm_reaper", 4, 12, 0, 0, true, Anim.NONE, 411, 596);

    public enum Anim {
        NONE(0), STRIKE(18), THRUST(12), BURST(16), DOME(22), CIRCLE(24), AVATAR(30), GRASP(26), PUSH(16), NOVA(34), SPEAR(30);
        public final int duration;

        Anim(int duration) { this.duration = duration; }
    }

    public static final Skill[] VALUES = values();

    public final String id;
    public final int tier, levelCost, energy, cooldown;
    public final boolean passive;
    public final Anim anim;
    public final int gx, gy;

    Skill(String id, int tier, int levelCost, int energy, int cooldown, boolean passive, Anim anim, int gx, int gy) {
        this.id = id;
        this.tier = tier;
        this.levelCost = levelCost;
        this.energy = energy;
        this.cooldown = cooldown;
        this.passive = passive;
        this.anim = anim;
        this.gx = gx;
        this.gy = gy;
    }

    /** the skill this one hangs from in the tree (tier 2 and 3), null for roots and tier 4 */
    public Skill parent() {
        return switch (this) {
            case LIGHTNING_BOLT, SPARK_BURST -> THUNDER_STRIKE;
            case STORM_DOME, STATIC_RING -> STATIC_CORE;
            case THUNDERSTORM -> LIGHTNING_BOLT;
            case STORM_AVATAR -> SPARK_BURST;
            case SHOCK_GRASP -> STORM_DOME;
            case BALL_LIGHTNING -> STATIC_RING;
            default -> null;
        };
    }

    public boolean requirementsMet(Set<Skill> unlocked) {
        if (tier == 4) {
            int t3 = 0;
            for (Skill s : unlocked) if (s.tier == 3) t3++;
            return t3 >= 2;
        }
        Skill p = parent();
        return p == null || unlocked.contains(p);
    }

    public ResourceLocation icon() { return new ResourceLocation(StormTree.MODID, "textures/gui/skills/" + id + ".png"); }

    public Component displayName() { return Component.translatable("skill.stormtree." + id); }

    public Component description() { return Component.translatable("skill.stormtree." + id + ".desc"); }

    public static Skill byId(int ordinal) { return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : null; }
}
