package com.krolasyon.advgear.block;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

/** Kilitli sandık türleri: demir kilit, kemik (zindan) kilidi, altın kalp kilidi. */
public enum LockType implements StringRepresentable {
    IRON("iron", 0.45F), BONE("bone", 0.3F), HEART("heart", 0.15F);

    public final String id;
    public final float pickChance;

    LockType(String id, float pickChance) {
        this.id = id;
        this.pickChance = pickChance;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public ResourceLocation loot() {
        return new ResourceLocation("advgear", "chests/locked_" + id);
    }
}
