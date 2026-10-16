package com.krolasyon.beyblade.parts;

/** the five parts of a Metal Fight beyblade, top to bottom */
public enum PartType {
    BOLT("Bolt"), RING("Ring"), WHEEL("Wheel"), TRACK("Track"), TIP("Tip");

    /** NBT key inside the beyblade's "Parts" compound */
    public final String key;

    PartType(String key) { this.key = key; }

    public String nameKey() { return "parttype.librabeyblade." + name().toLowerCase(java.util.Locale.ROOT); }
}
