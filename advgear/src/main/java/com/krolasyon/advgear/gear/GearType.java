package com.krolasyon.advgear.gear;

/** Kuşanım yuvası türü. Yuva sırası: 0-1 yüzük, 2 gözlük, 3 kemer, 4 sırt. */
public enum GearType {
    RING, GOGGLES, BELT, BACK;

    public static final int SLOTS = 5;

    public static GearType ofSlot(int slot) {
        return switch (slot) {
            case 0, 1 -> RING;
            case 2 -> GOGGLES;
            case 3 -> BELT;
            default -> BACK;
        };
    }
}
