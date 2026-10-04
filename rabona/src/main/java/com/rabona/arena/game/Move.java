package com.rabona.arena.game;

import net.minecraft.network.chat.Component;

/**
 * Tum hareketler. Her hareketin kendine ozgu animasyonu (istemci: AnimLibrary) ve
 * sunucu etkisi (MoveLogic) vardir.
 */
public enum Move {
    // ---- PAS
    PASS_SHORT(Cat.PASS, Req.BALL, 9, 4, 3, 6),
    PASS_THROUGH(Cat.PASS, Req.BALL, 10, 5, 4, 8),
    PASS_LOB(Cat.PASS, Req.BALL, 12, 6, 5, 10),
    PASS_CROSS(Cat.PASS, Req.BALL, 13, 7, 6, 12),
    PASS_BACKHEEL(Cat.PASS, Req.BALL, 10, 5, 4, 10),
    PASS_NOLOOK(Cat.PASS, Req.BALL, 10, 5, 5, 12),
    PASS_RABONA(Cat.PASS, Req.BALL, 15, 9, 8, 20),
    // ---- SUT
    SHOT_POWER(Cat.SHOT, Req.BALL, 14, 8, 10, 16),
    SHOT_FINESSE(Cat.SHOT, Req.BALL, 13, 7, 9, 16),
    SHOT_CHIP(Cat.SHOT, Req.BALL, 12, 7, 8, 16),
    SHOT_TRIVELA(Cat.SHOT, Req.BALL, 13, 7, 10, 18),
    SHOT_KNUCKLE(Cat.SHOT, Req.BALL, 15, 9, 12, 20),
    SHOT_RABONA(Cat.SHOT, Req.BALL, 16, 10, 12, 24),
    SHOT_PANENKA(Cat.SHOT, Req.BALL, 14, 9, 6, 20),
    SHOT_TOEPOKE(Cat.SHOT, Req.BALL, 7, 3, 6, 10),
    // ---- HAVA TOPU
    HEADER(Cat.AERIAL, Req.AIR, 10, 4, 6, 10),
    DIVING_HEADER(Cat.AERIAL, Req.AIR, 18, 5, 12, 24),
    VOLLEY(Cat.AERIAL, Req.AIR, 12, 5, 10, 14),
    BICYCLE(Cat.AERIAL, Req.AIR, 22, 8, 18, 40),
    SCORPION(Cat.AERIAL, Req.AIR, 16, 7, 15, 36),
    CHEST_CONTROL(Cat.AERIAL, Req.AIR, 10, 3, 3, 8),
    // ---- CALIM
    RAINBOW(Cat.SKILL, Req.BALL, 14, 6, 12, 30),
    ELASTICO(Cat.SKILL, Req.BALL, 11, 3, 10, 26),
    ROULETTE(Cat.SKILL, Req.BALL, 14, 2, 10, 26),
    STEPOVER(Cat.SKILL, Req.BALL, 12, 3, 6, 16),
    CRUYFF(Cat.SKILL, Req.BALL, 11, 5, 7, 20),
    DRAGBACK(Cat.SKILL, Req.BALL, 10, 3, 5, 16),
    CROQUETA(Cat.SKILL, Req.BALL, 8, 2, 7, 18),
    SOMBRERO(Cat.SKILL, Req.BALL, 12, 5, 10, 28),
    NUTMEG(Cat.SKILL, Req.BALL, 10, 4, 8, 24),
    CHOP(Cat.SKILL, Req.BALL, 9, 4, 6, 18),
    HEEL_FLICK(Cat.SKILL, Req.BALL, 12, 5, 9, 24),
    SEAL(Cat.SKILL, Req.BALL, 16, 6, 16, 80),
    AROUND_WORLD(Cat.SKILL, Req.BALL, 34, 4, 8, 60),
    SPEED_BURST(Cat.SKILL, Req.BALL, 9, 3, 14, 40),
    FAKE_SHOT(Cat.SKILL, Req.BALL, 12, 7, 6, 20),
    BODY_FEINT(Cat.SKILL, Req.BALL, 12, 5, 5, 22),
    // ---- SAVUNMA
    TACKLE(Cat.DEFENSE, Req.NONE, 10, 4, 6, 14),
    SLIDE(Cat.DEFENSE, Req.NONE, 20, 2, 20, 120),
    SHOULDER(Cat.DEFENSE, Req.NONE, 9, 4, 6, 20),
    BLOCK(Cat.DEFENSE, Req.NONE, 16, 1, 6, 24),
    PRESS(Cat.DEFENSE, Req.NONE, 12, 1, 10, 50),
    // ---- KALECI
    GK_DIVE_LEFT(Cat.KEEPER, Req.NONE, 24, 1, 8, 24),
    GK_DIVE_RIGHT(Cat.KEEPER, Req.NONE, 24, 1, 8, 24),
    GK_PUNT(Cat.KEEPER, Req.HELD, 14, 7, 4, 10),
    GK_THROW(Cat.KEEPER, Req.HELD, 12, 6, 3, 10),
    // ---- GOL SEVINCI
    CELEB_SIU(Cat.CELEBRATION, Req.NONE, 36, 0, 0, 40),
    CELEB_KNEESLIDE(Cat.CELEBRATION, Req.NONE, 40, 0, 0, 40),
    CELEB_BACKFLIP(Cat.CELEBRATION, Req.NONE, 22, 0, 0, 30),
    CELEB_PLANE(Cat.CELEBRATION, Req.NONE, 50, 0, 0, 50),
    CELEB_DANCE(Cat.CELEBRATION, Req.NONE, 48, 0, 0, 50),
    CELEB_SHUSH(Cat.CELEBRATION, Req.NONE, 36, 0, 0, 40),
    CELEB_SHIRT_OFF(Cat.CELEBRATION, Req.NONE, 60, 0, 0, 60),
    // ---- OZEL YETENEKLER (enerji harcar)
    AB_FIRE(Cat.ABILITY, Req.BALL, 22, 13, 0, 60),
    AB_THUNDER(Cat.ABILITY, Req.NONE, 16, 6, 0, 60),
    AB_TORNADO(Cat.ABILITY, Req.BALL, 22, 12, 0, 60),
    AB_GHOST(Cat.ABILITY, Req.NONE, 14, 5, 0, 60),
    AB_MAGNET(Cat.ABILITY, Req.NONE, 18, 4, 0, 60),
    AB_ICE(Cat.ABILITY, Req.NONE, 18, 8, 0, 60),
    // ---- tepkiler (oyuncu secemez)
    STUMBLE(Cat.REACTION, Req.NONE, 16, 0, 0, 0),
    CALL_PASS(Cat.REACTION, Req.NONE, 14, 0, 0, 0),
    FALL(Cat.REACTION, Req.NONE, 30, 0, 0, 0),
    CATCH(Cat.REACTION, Req.NONE, 10, 0, 0, 0);

    public enum Cat {
        PASS(0x4FC3F7), SHOT(0xEF5350), AERIAL(0xFFB74D), SKILL(0xBA68C8), DEFENSE(0x66BB6A),
        KEEPER(0xFFEE58), CELEBRATION(0xF06292), ABILITY(0xFFD54F), REACTION(0x9E9E9E);
        public final int color;

        Cat(int c) { color = c; }

        public Component title() { return Component.translatable("cat.rabonaarena." + name().toLowerCase()); }
    }

    /** Gereksinim: BALL = top ayakta/yakinda, AIR = top havada ulasilabilir, HELD = kaleci elinde. */
    public enum Req { NONE, BALL, AIR, HELD }

    public final Cat cat;
    public final Req req;
    public final int duration, impact, stamina, cooldown;

    Move(Cat cat, Req req, int duration, int impact, int stamina, int cooldown) {
        this.cat = cat;
        this.req = req;
        this.duration = duration;
        this.impact = impact;
        this.stamina = stamina;
        this.cooldown = cooldown;
    }

    public String key() { return name().toLowerCase(); }

    public Component title() { return Component.translatable("move.rabonaarena." + key()); }

    public Component desc() { return Component.translatable("move.rabonaarena." + key() + ".desc"); }

    public boolean selectable() { return cat != Cat.REACTION; }

    public int energyCost() {
        return switch (this) {
            case AB_FIRE, AB_TORNADO -> 100;
            case AB_THUNDER, AB_GHOST -> 60;
            case AB_MAGNET -> 50;
            case AB_ICE -> 70;
            default -> 0;
        };
    }

    public static Move byId(int i) {
        Move[] v = values();
        return i >= 0 && i < v.length ? v[i] : PASS_SHORT;
    }
}
