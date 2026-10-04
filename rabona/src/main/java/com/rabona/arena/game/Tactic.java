package com.rabona.arena.game;

import net.minecraft.network.chat.Component;

import static com.rabona.arena.game.Pos.*;

/**
 * Menajerin oyun plani. Botlarin savunma cizgisi, pres sayisi, pas uzunlugu, calim/orta egilimi,
 * sut mesafesi, hucum genisligi ve 11 kisilik dizilisi buna gore degisir.
 */
public enum Tactic {
    //            cizgi  pres kisa  direkt calim orta  sut  genislik kontra  dizilis
    BALANCED(0.0, 1, 0.0, 0.0, 0.0, 0.0, 0, 1.0, 0.0, "4-3-3",
            new Pos[]{GK, LB, LCB, RCB, RB, DM, LCM, RCM, LW, RW, ST}),
    TIKI_TAKA(0.07, 2, 0.55, -0.4, -0.1, -0.25, -3, 1.05, 0.0, "4-3-3",
            new Pos[]{GK, LB, LCB, RCB, RB, DM, LCM, RCM, LW, RW, ST}),
    GEGENPRESS(0.13, 3, 0.1, 0.25, 0.05, 0.0, 0, 1.0, 0.0, "4-2-3-1",
            new Pos[]{GK, LB, LCB, RCB, RB, LCM, RCM, LW, AM, RW, ST}),
    COUNTER(-0.1, 1, -0.3, 0.65, 0.2, 0.05, 2, 0.92, 0.22, "4-4-1-1",
            new Pos[]{GK, LB, LCB, RCB, RB, LM, LCM, RCM, RM, AM, ST}),
    PARK_BUS(-0.2, 1, -0.1, 0.3, -0.2, 0.0, 1, 0.72, 0.12, "5-4-1",
            new Pos[]{GK, LB, LCB, CB, RCB, RB, LM, LCM, RCM, RM, ST}),
    WING_PLAY(0.0, 1, 0.0, 0.1, 0.12, 0.65, 0, 1.25, 0.0, "4-4-2",
            new Pos[]{GK, LB, LCB, RCB, RB, LM, LCM, RCM, RM, AM, ST}),
    LONG_BALL(-0.04, 1, -0.55, 0.5, -0.1, 0.3, 2, 1.05, 0.1, "4-4-2",
            new Pos[]{GK, LB, LCB, RCB, RB, LM, DM, CM, RM, AM, ST});

    /** Savunma cizgisi kaydirmasi (a ekseni, -1..1 olcekte). */
    public final double line;
    /** Topa ayni anda pres yapan oyuncu sayisi. */
    public final int pressers;
    /** Kisa pas tercihi (+) / uzun top tercihi (-). */
    public final double shortPass;
    /** Ileri pas (dikine oyun) agirligi. */
    public final double direct;
    public final double dribble, cross;
    /** Sut mesafesi farki (blok). */
    public final int shotRange;
    /** Hucumda genislik carpani. */
    public final double width;
    /** Savunmada forvetlerin onde bekleme miktari. */
    public final double counter;
    public final String shape;
    public final Pos[] eleven;

    Tactic(double line, int pressers, double shortPass, double direct, double dribble, double cross, int shotRange,
           double width, double counter, String shape, Pos[] eleven) {
        this.line = line;
        this.pressers = pressers;
        this.shortPass = shortPass;
        this.direct = direct;
        this.dribble = dribble;
        this.cross = cross;
        this.shotRange = shotRange;
        this.width = width;
        this.counter = counter;
        this.shape = shape;
        this.eleven = eleven;
    }

    public Component title() { return Component.translatable("tactic.rabonaarena." + name().toLowerCase()); }

    public Component desc() { return Component.translatable("tactic.rabonaarena." + name().toLowerCase() + ".desc"); }

    public static Tactic byId(int i) {
        Tactic[] v = values();
        return i >= 0 && i < v.length ? v[i] : BALANCED;
    }
}
