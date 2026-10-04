package com.rabona.arena.game;

import net.minecraft.network.chat.Component;

/**
 * Saha mevkileri. a: -1 kendi kalesi .. +1 rakip kale, b: -1 sol .. +1 sag (hucum yonune gore).
 * Rol, botun oyun tarzini belirler.
 */
public enum Pos {
    GK(-0.96, 0, Role.GK),
    LB(-0.55, -0.72, Role.DEF), LCB(-0.66, -0.26, Role.DEF), CB(-0.68, 0, Role.DEF), RCB(-0.66, 0.26, Role.DEF), RB(-0.55, 0.72, Role.DEF),
    DM(-0.38, 0, Role.MID), LCM(-0.18, -0.3, Role.MID), CM(-0.18, 0, Role.MID), RCM(-0.18, 0.3, Role.MID),
    LM(-0.08, -0.7, Role.MID), RM(-0.08, 0.7, Role.MID), AM(0.12, 0, Role.MID),
    LW(0.3, -0.66, Role.FWD), RW(0.3, 0.66, Role.FWD), ST(0.42, 0, Role.FWD);

    public enum Role { GK, DEF, MID, FWD }

    public final double a, b;
    public final Role role;

    Pos(double a, double b, Role role) {
        this.a = a;
        this.b = b;
        this.role = role;
    }

    public Component title() { return Component.translatable("pos.rabonaarena." + name().toLowerCase()); }

    public Component shortName() { return Component.translatable("pos.rabonaarena." + name().toLowerCase() + ".short"); }

    public boolean wide() { return Math.abs(b) > 0.5; }

    public static Pos byId(int i) {
        Pos[] v = values();
        return i >= 0 && i < v.length ? v[i] : CM;
    }

    /** Takim buyuklugune gore dizilis (1..11). */
    public static Pos[] formation(int n) {
        return switch (Math.max(1, Math.min(11, n))) {
            case 1 -> new Pos[]{GK};
            case 2 -> new Pos[]{GK, ST};
            case 3 -> new Pos[]{GK, CB, ST};
            case 4 -> new Pos[]{GK, CB, CM, ST};
            case 5 -> new Pos[]{GK, CB, LM, RM, ST};
            case 6 -> new Pos[]{GK, LCB, RCB, LM, RM, ST};
            case 7 -> new Pos[]{GK, LCB, RCB, LM, CM, RM, ST};
            case 8 -> new Pos[]{GK, LB, CB, RB, LM, CM, RM, ST};
            case 9 -> new Pos[]{GK, LB, CB, RB, DM, LM, RM, AM, ST};
            case 10 -> new Pos[]{GK, LB, LCB, RCB, RB, LM, CM, RM, AM, ST};
            default -> new Pos[]{GK, LB, LCB, RCB, RB, DM, LCM, RCM, LW, RW, ST};
        };
    }

    public double dist(Pos o) { return Math.hypot(a - o.a, (b - o.b) * 0.8); }
}
