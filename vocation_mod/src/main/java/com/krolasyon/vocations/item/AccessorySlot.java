package com.krolasyon.vocations.item;

import java.util.Locale;

public enum AccessorySlot {
    AMULET,
    RING;

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}
