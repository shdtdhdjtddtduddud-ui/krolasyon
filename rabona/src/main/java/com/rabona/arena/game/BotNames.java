package com.rabona.arena.game;

import net.minecraft.util.RandomSource;

/** Kurgusal oyuncu isimleri. */
public final class BotNames {
    private static final String[] FIRST = {"Kaan", "Arda", "Deniz", "Emir", "Mert", "Yigit", "Ozan", "Baran", "Efe", "Tuna",
            "Kerem", "Alp", "Cenk", "Doruk", "Eren", "Koray", "Levent", "Onur", "Rüzgar", "Selim", "Toprak", "Umut",
            "Volkan", "Batu", "Caner", "Aras", "Mirac", "Poyraz", "Atlas", "Kuzey", "Leo", "Marco", "Luka", "Rafa",
            "Nico", "Thiago", "Kenji", "Dario", "Iker", "Sami"};
    private static final String[] LAST = {"Yildiz", "Kartal", "Simsek", "Aslan", "Demir", "Kaya", "Cakir", "Bozkurt",
            "Firtina", "Tekin", "Ates", "Gunes", "Erdem", "Kilic", "Akin", "Tan", "Duman", "Sahin", "Kurt", "Ozdemir",
            "Volta", "Ferro", "Costa", "Moreno", "Silva", "Rossi", "Novak", "Kovac", "Santos", "Lopez"};

    private BotNames() {}

    public static String random(RandomSource r) {
        return FIRST[r.nextInt(FIRST.length)] + " " + LAST[r.nextInt(LAST.length)];
    }
}
