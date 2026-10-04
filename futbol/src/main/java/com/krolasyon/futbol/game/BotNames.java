package com.krolasyon.futbol.game;

import net.minecraft.util.RandomSource;

public final class BotNames {
    private BotNames() {}

    private static final String[] NAMES = {"Kemal", "Arda", "Burak", "Hakan", "Cenk", "Emre", "Mert", "Oğuz", "Selim", "Tolga",
            "Yusuf", "Kaan", "Barış", "Ferdi", "Kerem", "Orkun", "Okay", "İrfan", "Salih", "Uğur", "Volkan", "Altay", "Zeki",
            "Mehmet", "Ozan", "Rıdvan", "Semih", "Taylan", "Doğan", "Efe", "Berk", "Alper", "Serdar", "Caner", "Eren", "Atakan",
            "Halil", "Onur", "Sinan", "Yunus", "Cengiz", "Tarık", "Metin", "Lefter", "Rüştü", "Fatih", "Bülent", "Hasan"};

    public static String random(RandomSource r) { return NAMES[r.nextInt(NAMES.length)]; }
}
