package com.krolasyon.furniture.block;

import net.minecraft.util.StringRepresentable;

/** which half of a two-block-wide piece (seen from the front of the furniture). RIGHT owns the block entity. */
public enum WidePart implements StringRepresentable {
    RIGHT("right"), LEFT("left");

    private final String name;

    WidePart(String name) { this.name = name; }

    @Override
    public String getSerializedName() { return name; }
}
