package com.krolasyon.voidtree;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;

/**
 * The 14 skills of the void tree. gx/gy are icon centres in the reference artwork (736x920);
 * the skill tree screen renders in that coordinate space.
 */
public enum Skill {
    // tier 1
    SHADOW_DASH("shadow_dash", 1, 2, 20, 80, false, Anim.DASH, 147, 132),
    PHANTOM_FORM("phantom_form", 1, 2, 35, 500, false, Anim.PHANTOM, 368, 132),
    VOID_MANTLE("void_mantle", 1, 2, 0, 0, true, Anim.NONE, 590, 132),
    // tier 2
    VOID_VORTEX("void_vortex", 2, 5, 35, 300, false, Anim.VORTEX, 147, 350),
    SPIRIT_TWISTER("spirit_twister", 2, 5, 30, 240, false, Anim.PUSH, 295, 350),
    SHADOW_CLAWS("shadow_claws", 2, 5, 15, 50, false, Anim.CLAWS, 443, 350),
    SOUL_PRISON("soul_prison", 2, 5, 35, 360, false, Anim.GRASP, 590, 350),
    // tier 3
    SHADOW_RAIN("shadow_rain", 3, 8, 50, 360, false, Anim.RAIN, 221, 568),
    BLACK_HOLE("black_hole", 3, 8, 60, 600, false, Anim.HOLE, 368, 568),
    RIFT_WALK("rift_walk", 3, 8, 30, 160, false, Anim.THROW, 516, 568),
    // tier 4
    ASCENSION("ascension", 4, 12, 70, 1200, false, Anim.ASCEND, 147, 785),
    VOID_SEAL("void_seal", 4, 12, 60, 700, false, Anim.SEAL, 295, 785),
    SHADOW_CLONES("shadow_clones", 4, 12, 70, 900, false, Anim.SUMMON, 443, 785),
    COSMIC_COLLAPSE("cosmic_collapse", 4, 12, 90, 1200, false, Anim.HOLE, 590, 785);

    public enum Anim {
        NONE(0), DASH(12), PHANTOM(22), VORTEX(24), PUSH(16), CLAWS(14), GRASP(26), RAIN(18), HOLE(34), THROW(12),
        ASCEND(30), SEAL(18), SUMMON(24);
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

    /** any one of these unlocks this skill (mirrors the connector lines of the artwork); empty = root */
    public Skill[] parents() {
        return switch (this) {
            case VOID_VORTEX, SPIRIT_TWISTER, SHADOW_CLAWS, SOUL_PRISON -> new Skill[]{SHADOW_DASH, PHANTOM_FORM, VOID_MANTLE};
            case SHADOW_RAIN, BLACK_HOLE, RIFT_WALK -> new Skill[]{SPIRIT_TWISTER, SHADOW_CLAWS, SOUL_PRISON};
            case ASCENSION, VOID_SEAL, SHADOW_CLONES -> new Skill[]{SHADOW_RAIN, BLACK_HOLE};
            case COSMIC_COLLAPSE -> new Skill[]{RIFT_WALK};
            default -> new Skill[0];
        };
    }

    public boolean requirementsMet(Set<Skill> unlocked) {
        Skill[] p = parents();
        if (p.length == 0) return true;
        for (Skill s : p) if (unlocked.contains(s)) return true;
        return false;
    }

    public ResourceLocation icon() { return new ResourceLocation(VoidTree.MODID, "textures/gui/skills/" + id + ".png"); }

    public Component displayName() { return Component.translatable("skill.voidtree." + id); }

    public Component description() { return Component.translatable("skill.voidtree." + id + ".desc"); }

    public static Skill byId(int ordinal) { return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : null; }
}
