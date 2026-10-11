package com.krolasyon.omnitrix;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** The 9 Omnitrix forms. Stats are applied as attribute modifiers while transformed. */
public enum Alien {
    HEATBLAST("heatblast", 0xFFB020, 2.05F, 10, 0.05, 2, 0),
    TERRASPIN("terraspin", 0x9FB84A, 2.0F, 14, 0.0, 2, 4),
    DIAMONDHEAD("diamondhead", 0x8FE8B8, 2.3F, 16, 0.0, 4, 8),
    GOOP("goop", 0x7CDC2A, 2.25F, 8, 0.02, 2, 0),
    XLR8("xlr8", 0x3FA9C0, 1.9F, 6, 0.12, 3, 0),
    FOURARMS("fourarms", 0xD0102A, 2.6F, 24, -0.01, 7, 6),
    BIGCHILL("bigchill", 0x2D7FB0, 2.3F, 10, 0.03, 3, 2),
    ULTIMATE_ECHO_ECHO("ultimate_echo_echo", 0x2A8CE8, 2.3F, 12, 0.02, 3, 6),
    GHOSTFREAK("ghostfreak", 0xB0B0B0, 2.2F, 8, 0.03, 3, 0);

    public static final Alien[] VALUES = values();

    public final String id;
    public final int color;
    /** visual height in blocks */
    public final float height;
    /** extra max health, movement speed (added to base 0.1), attack damage, armor */
    public final int health;
    public final double speed;
    public final int damage, armor;

    Alien(String id, int color, float height, int health, double speed, int damage, int armor) {
        this.id = id;
        this.color = color;
        this.height = height;
        this.health = health;
        this.speed = speed;
        this.damage = damage;
        this.armor = armor;
    }

    public Ability ability(int slot) { return Ability.of(this, slot); }

    public Component displayName() { return Component.translatable("alien.omnitrix." + id); }

    public ResourceLocation icon() { return new ResourceLocation(Omnitrix.MODID, "textures/gui/aliens/" + id + ".png"); }

    public ResourceLocation silhouette() { return new ResourceLocation(Omnitrix.MODID, "textures/gui/aliens/" + id + "_sil.png"); }

    public ResourceLocation model() { return new ResourceLocation(Omnitrix.MODID, "models/alien/" + id + ".omx"); }

    public static Alien byId(int i) { return i >= 0 && i < VALUES.length ? VALUES[i] : null; }
}
