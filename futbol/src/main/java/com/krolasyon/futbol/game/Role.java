package com.krolasyon.futbol.game;

/** Playing position. */
public enum Role {
    GK("Kaleci", "KL"), DEF("Defans", "DEF"), MID("Orta Saha", "OS"), WING("Kanat", "KNT"), FWD("Forvet", "FV");

    public final String title;
    public final String abbr;

    Role(String title, String abbr) {
        this.title = title;
        this.abbr = abbr;
    }

    public static Role byId(int id) {
        Role[] v = values();
        return id >= 0 && id < v.length ? v[id] : MID;
    }
}
