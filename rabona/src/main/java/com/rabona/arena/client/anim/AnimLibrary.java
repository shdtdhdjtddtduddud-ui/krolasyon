package com.rabona.arena.client.anim;

import com.rabona.arena.game.Move;

import java.util.EnumMap;
import java.util.Map;

/**
 * Her hareketin elle ayarlanmis animasyonu. Degerler derece; bacak/kol xRot negatif = one/yukari.
 * Kok: root(x = one devrilme, y = donus, z = saga yatma), pos(x = sag, y = yukari, z = ileri).
 */
public final class AnimLibrary {
    private static final Map<Move, Anim> ANIMS = new EnumMap<>(Move.class);

    private AnimLibrary() {}

    public static Anim get(Move m) {
        if (ANIMS.isEmpty()) build();
        return ANIMS.get(m);
    }

    private static void put(Move m, Anim.B b) { ANIMS.put(m, b.build()); }

    /** Sag ayakla klasik vurus iskeleti. */
    private static Anim.B kick(Move m, float back, float strike, float follow, float bodyLean) {
        int i = m.impact, d = m.duration;
        return Anim.of(d)
                .at(0).rest()
                .at(Math.max(1, i - 3)).rl(back, 0, 4).ll(-6, 0, 0).body(-6, -8, 0).arms(25, 18, -35, -38).head(8, 0, 0)
                .at(i).rl(strike, 0, 0).ll(4, 0, 0).body(bodyLean, 10, 0).arms(-30, 25, 18, -42).head(16, 0, 0)
                .at(Math.min(d - 2, i + 3)).rl(follow, 0, -4).ll(6, 0, 0).body(bodyLean * 0.6f, 14, 0).arms(-20, 30, 12, -30)
                .at(d).rest();
    }

    private static void build() {
        // ============================================================ PASLAR
        put(Move.PASS_SHORT, Anim.of(Move.PASS_SHORT.duration)
                .at(0).rest()
                .at(2).rl(28, -30, 8).ll(-4, 0, 0).body(4, -6, 0).arms(10, 20, -15, -25).head(20, 0, 0)
                .at(4).rl(-35, -40, 14).body(8, 6, 0).arms(-10, 22, 5, -28)
                .at(6).rl(-42, -36, 10).body(6, 8, 0)
                .at(9).rest());
        put(Move.PASS_THROUGH, Anim.of(Move.PASS_THROUGH.duration)
                .at(0).rest()
                .at(2).rl(36, -25, 6).body(2, -8, 0).arms(20, 15, -25, -30).head(18, 0, 0)
                .at(5).rl(-48, -30, 8).body(10, 10, 0).arms(-25, 25, 15, -35)
                .at(7).rl(-55, -25, 6).head(5, 0, 0)
                .at(10).rest());
        put(Move.PASS_LOB, kick(Move.PASS_LOB, 55, -70, -95, -8).at(Move.PASS_LOB.impact).body(-12, 8, 0));
        put(Move.PASS_CROSS, kick(Move.PASS_CROSS, 60, -65, -90, -10)
                .at(3).root(0, -20, 0)
                .at(Move.PASS_CROSS.impact).rl(-65, -35, 18).root(0, -25, -8)
                .at(Move.PASS_CROSS.duration).root(0, 0, 0));
        put(Move.PASS_BACKHEEL, Anim.of(Move.PASS_BACKHEEL.duration)
                .at(0).rest()
                .at(2).rl(-25, 0, 0).body(8, 0, 0).head(25, 0, 0).arms(-10, 12, -10, -12)
                .at(5).rl(55, 10, 0).body(18, 0, 0).head(30, 25, 0).arms(15, 20, 15, -20)
                .at(7).rl(48, 8, 0)
                .at(10).rest());
        put(Move.PASS_NOLOOK, Anim.of(Move.PASS_NOLOOK.duration)
                .at(0).rest()
                .at(2).rl(25, -20, 0).head(0, -45, 0).body(0, -10, 0).arms(15, 15, -20, -25)
                .at(5).rl(-30, -60, 25).head(-5, -55, 0).body(4, 15, 0).arms(-20, 25, 10, -20)
                .at(8).rl(-25, -45, 15).head(-5, -50, 0)
                .at(10).rest());
        put(Move.PASS_RABONA, rabona(Move.PASS_RABONA, -45));
        // ============================================================ SUTLAR
        put(Move.SHOT_POWER, Anim.of(Move.SHOT_POWER.duration)
                .at(0).rest()
                .at(3).ll(-15, 0, -4).rl(20, 0, 0).body(-4, 0, 0)
                .at(6).rl(78, 0, 8).ll(-8, 0, 0).body(-12, -18, 0).arms(40, 30, -55, -55).head(10, 0, 0).pos(0, 0.05f, 0)
                .at(8).rl(-62, 0, 0).ll(6, 0, 0).body(16, 18, 0).arms(-45, 35, 30, -60).head(20, 0, 0).pos(0, 0.12f, 0.1f)
                .at(10).rl(-115, 0, -6).body(14, 22, 0).arms(-35, 45, 20, -40).pos(0, 0.22f, 0.25f)
                .at(12).rl(-40, 0, 0).pos(0, 0, 0.3f)
                .at(14).rest().pos(0, 0, 0));
        put(Move.SHOT_FINESSE, kick(Move.SHOT_FINESSE, 55, -60, -88, 6)
                .at(Move.SHOT_FINESSE.impact).rl(-60, -40, 22).root(0, 0, -8)
                .at(Move.SHOT_FINESSE.impact + 3).rl(-85, -45, 28).root(0, -10, -12).la(-60, 0, -70)
                .at(Move.SHOT_FINESSE.duration).root(0, 0, 0));
        put(Move.SHOT_CHIP, Anim.of(Move.SHOT_CHIP.duration)
                .at(0).rest()
                .at(4).rl(32, -15, 0).body(-6, 0, 0).arms(15, 25, -20, -25).head(15, 0, 0)
                .at(7).rl(-38, -25, 6).body(-18, 0, 0).arms(-25, 45, -25, -45).head(-15, 0, 0)
                .at(9).rl(-48, -20, 4).body(-14, 0, 0).head(-25, 0, 0)
                .at(12).rest());
        put(Move.SHOT_TRIVELA, kick(Move.SHOT_TRIVELA, 50, -55, -70, 8)
                .at(Move.SHOT_TRIVELA.impact).rl(-55, 40, -22).root(0, 10, 8)
                .at(Move.SHOT_TRIVELA.impact + 3).rl(-70, 50, -35).root(0, 18, 10)
                .at(Move.SHOT_TRIVELA.duration).root(0, 0, 0));
        // knuckleball: genis durus, sert ve kisa vurus
        put(Move.SHOT_KNUCKLE, Anim.of(Move.SHOT_KNUCKLE.duration)
                .at(0).rest()
                .at(3).rl(0, 0, 14).ll(0, 0, -14).body(-8, 0, 0).arms(15, 25, 15, -25).head(10, 0, 0)
                .at(7).rl(62, 0, 6).ll(-4, 0, -10).body(-10, -12, 0).arms(35, 40, -40, -60)
                .at(9).rl(-52, 0, 0).body(12, 10, 0).arms(-10, 60, 10, -70).pos(0, 0.1f, 0.1f)
                .at(11).rl(-50, 0, 0).body(10, 8, 0).arms(-5, 75, 5, -75).pos(0, 0.2f, 0.15f)
                .at(13).rl(-10, 0, 0).pos(0, 0, 0.2f)
                .at(15).rest().pos(0, 0, 0));
        put(Move.SHOT_RABONA, rabona(Move.SHOT_RABONA, -60));
        put(Move.SHOT_PANENKA, Anim.of(Move.SHOT_PANENKA.duration)
                .at(0).rest()
                .at(4).rl(45, 0, 0).body(-6, -10, 0).arms(30, 20, -30, -30)
                .at(7).rl(15, 0, 0).body(-4, 0, 0)
                .at(9).rl(-28, -10, 0).body(-10, 0, 0).arms(-10, 15, -10, -15).head(-10, 0, 0)
                .at(11).rl(-30, -10, 0).head(-20, 0, 0)
                .at(14).rest());
        put(Move.SHOT_TOEPOKE, Anim.of(Move.SHOT_TOEPOKE.duration)
                .at(0).rest()
                .at(2).rl(-20, 0, 0).body(10, 0, 0)
                .at(3).rl(-50, 0, 0).body(14, 0, 0).arms(-20, 20, 10, -20)
                .at(5).rl(-45, 0, 0)
                .at(7).rest());
        // ============================================================ HAVA
        put(Move.HEADER, Anim.of(Move.HEADER.duration)
                .at(0).rest()
                .at(2).body(-22, 0, 0).head(-30, 0, 0).arms(-40, 25, -40, -25).legs(10, -15).pos(0, 0.15f, 0)
                .at(4).body(30, 0, 0).head(35, 0, 0).arms(10, 40, 10, -40).pos(0, 0.3f, 0)
                .at(6).body(18, 0, 0).head(20, 0, 0).pos(0, 0.15f, 0)
                .at(10).rest().pos(0, 0, 0));
        put(Move.DIVING_HEADER, Anim.of(Move.DIVING_HEADER.duration)
                .at(0).rest()
                .at(2).root(25, 0, 0).pos(0, 0.1f, 0.2f).arms(-90, 15, -90, -15).legs(20, 10)
                .at(5).root(78, 0, 0).pos(0, -0.15f, 0.6f).arms(-165, 10, -165, -10).legs(25, 15).head(-60, 0, 0)
                .at(9).root(88, 0, 0).pos(0, -0.6f, 0.9f).arms(-150, 25, -150, -25).head(-50, 0, 0)
                .at(14).root(88, 0, 0).pos(0, -0.62f, 0.9f)
                .at(18).root(0, 0, 0).pos(0, 0, 0).rest());
        put(Move.VOLLEY, Anim.of(Move.VOLLEY.duration)
                .at(0).rest()
                .at(3).root(0, -35, 18).rl(30, 0, 40).ll(0, 0, -5).arms(-40, 70, -20, -60).body(0, -20, 0)
                .at(5).root(0, -40, 28).rl(-85, 0, 55).arms(-60, 80, 10, -40).body(0, 25, 0).pos(0, 0.1f, 0)
                .at(8).root(0, -20, 15).rl(-60, 0, 30)
                .at(12).root(0, 0, 0).rest().pos(0, 0, 0));
        put(Move.BICYCLE, Anim.of(Move.BICYCLE.duration)
                .at(0).rest()
                .at(3).root(-50, 0, 0).pos(0, 0.4f, 0).ll(-110, 0, 0).rl(20, 0, 0).arms(-40, 50, -40, -50)
                .at(6).root(-110, 0, 0).pos(0, 0.65f, 0).ll(-40, 0, 0).rl(-80, 0, 0)
                .at(8).root(-145, 0, 0).pos(0, 0.7f, -0.1f).rl(-160, 0, 0).ll(10, 0, 0).arms(10, 80, 10, -80)
                .at(11).root(-130, 0, 0).pos(0, 0.3f, -0.2f).rl(-90, 0, 0).ll(-30, 0, 0)
                .at(14).root(-92, 0, 0).pos(0, -0.62f, -0.3f).arms(20, 70, 20, -70).legs(-40, -30)
                .at(18).root(-90, 0, 0).pos(0, -0.62f, -0.3f)
                .at(22).root(0, 0, 0).pos(0, 0, 0).rest());
        put(Move.SCORPION, Anim.of(Move.SCORPION.duration)
                .at(0).rest()
                .at(3).root(45, 0, 0).pos(0, 0.1f, 0.4f).arms(-150, 15, -150, -15).rl(60, 0, 0).ll(30, 0, 0)
                .at(5).root(75, 0, 0).pos(0, -0.1f, 0.7f).rl(130, 0, 0).ll(40, 0, 0).head(-50, 0, 0)
                .at(7).root(80, 0, 0).pos(0, -0.15f, 0.8f).rl(175, 0, 0).ll(60, 0, 0)
                .at(10).root(85, 0, 0).pos(0, -0.55f, 0.9f).rl(40, 0, 0).ll(20, 0, 0)
                .at(16).root(0, 0, 0).pos(0, 0, 0).rest());
        put(Move.CHEST_CONTROL, Anim.of(Move.CHEST_CONTROL.duration)
                .at(0).rest()
                .at(2).body(-28, 0, 0).head(-10, 0, 0).arms(-20, 70, -20, -70).legs(-10, 10)
                .at(4).body(-15, 0, 0).head(25, 0, 0).arms(-10, 55, -10, -55)
                .at(7).body(5, 0, 0).head(30, 0, 0)
                .at(10).rest());
        // ============================================================ CALIMLAR
        put(Move.RAINBOW, Anim.of(Move.RAINBOW.duration)
                .at(0).rest()
                .at(2).rl(-15, 0, 0).ll(25, 0, 0).body(6, 0, 0).arms(-20, 30, -20, -30)
                .at(4).rl(40, 0, -10).ll(35, 0, 10).body(14, 0, 0).pos(0, 0.12f, 0).head(-10, 0, 0)
                .at(6).rl(105, 0, -5).ll(70, 0, 5).body(22, 0, 0).arms(-50, 70, -50, -70).head(-35, 0, 0).pos(0, 0.25f, 0)
                .at(8).rl(60, 0, 0).ll(20, 0, 0).head(-45, 0, 0).pos(0, 0.1f, 0.2f)
                .at(11).rl(-40, 0, 0).ll(40, 0, 0).body(15, 0, 0).arms(40, 10, -40, -10).head(-25, 0, 0).pos(0, 0, 0.3f)
                .at(14).rest().pos(0, 0, 0));
        put(Move.ELASTICO, Anim.of(Move.ELASTICO.duration)
                .at(0).rest()
                .at(2).rl(-15, -30, 38).body(0, 10, -12).arms(-10, 45, 10, -30).pos(0.1f, 0, 0)
                .at(4).rl(-10, 30, -32).body(0, -15, 14).arms(10, 30, -10, -55).pos(-0.15f, 0, 0)
                .at(6).rl(15, 0, -10).ll(-25, 0, -20).body(10, -10, 18).pos(-0.45f, 0.05f, 0.1f)
                .at(8).rl(-30, 0, 0).ll(30, 0, 0).body(12, 0, 6).arms(30, 15, -30, -15).pos(-0.55f, 0, 0.25f)
                .at(11).rest().pos(0, 0, 0));
        put(Move.ROULETTE, Anim.of(Move.ROULETTE.duration)
                .at(0).rest()
                .at(2).rl(-25, 0, 10).body(8, 0, 0).arms(-10, 45, -10, -45).root(0, 0, 0)
                .at(5).root(0, -120, 0).ll(-20, 0, -10).rl(10, 0, 10).arms(-20, 60, -20, -60).pos(0.2f, 0, 0)
                .at(8).root(0, -230, 0).rl(-20, 0, 15).ll(15, 0, 0).pos(0.4f, 0, 0)
                .at(11).root(0, -340, 0).ll(-20, 0, 0).rl(20, 0, 0).pos(0.5f, 0, 0)
                .at(12).root(0, -360, 0)
                .at(14).root(0, -360, 0).rest().pos(0, 0, 0));
        put(Move.STEPOVER, Anim.of(Move.STEPOVER.duration)
                .at(0).rest()
                .at(2).rl(-35, 30, -38).body(0, 15, -14).arms(-10, 50, 20, -20).pos(0.15f, 0, 0)
                .at(4).rl(5, 0, 0).ll(-35, -30, 38).body(0, -15, 14).arms(20, 20, -10, -50).pos(-0.15f, 0, 0)
                .at(6).ll(5, 0, 0).rl(-30, 30, -35).body(0, 15, -16).pos(0.25f, 0, 0)
                .at(9).rl(10, 0, 0).body(12, -10, 10).pos(-0.35f, 0, 0.2f)
                .at(12).rest().pos(0, 0, 0));
        put(Move.CRUYFF, Anim.of(Move.CRUYFF.duration)
                .at(0).rest()
                .at(3).rl(55, 0, 6).body(-10, -15, 0).arms(35, 30, -45, -45).head(10, 0, 0)
                .at(5).rl(-10, 25, -30).body(10, 15, 0).arms(-10, 40, 10, -30).head(30, 25, 0)
                .at(7).rl(15, 30, -35).ll(-10, 0, 0).body(14, 30, 0).head(20, 40, 0)
                .at(11).rest());
        put(Move.DRAGBACK, Anim.of(Move.DRAGBACK.duration)
                .at(0).rest()
                .at(2).rl(-30, 0, 0).body(4, 0, 0).head(25, 0, 0).arms(-10, 25, 10, -25)
                .at(4).rl(20, 0, 0).body(-12, 0, 0).arms(20, 30, -20, -30).pos(0, 0, -0.15f)
                .at(6).rl(30, 0, 0).ll(-15, 0, 0).body(-8, 0, 0).pos(0, 0, -0.3f)
                .at(10).rest().pos(0, 0, 0));
        put(Move.CROQUETA, Anim.of(Move.CROQUETA.duration)
                .at(0).rest()
                .at(1).rl(-10, 0, 25).body(0, 0, -10).arms(-10, 55, 10, -20)
                .at(3).rl(0, 0, -5).ll(-10, 0, -28).body(0, 0, 14).pos(0.35f, 0, 0).arms(10, 20, -10, -55)
                .at(5).ll(0, 0, 0).body(8, 0, 6).pos(0.55f, 0, 0.1f)
                .at(8).rest().pos(0, 0, 0));
        put(Move.SOMBRERO, Anim.of(Move.SOMBRERO.duration)
                .at(0).rest()
                .at(2).rl(-25, 0, 0).body(4, 0, 0).head(25, 0, 0)
                .at(4).rl(-55, -15, 0).body(-14, 0, 0).arms(-40, 50, -40, -50).head(-15, 0, 0).pos(0, 0.12f, 0)
                .at(6).rl(-70, -10, 0).body(-20, 0, 0).head(-35, 0, 0).pos(0, 0.25f, 0)
                .at(9).rl(-20, 0, 0).body(10, 0, 0).head(-20, 0, 0).pos(0, 0, 0.25f)
                .at(12).rest().pos(0, 0, 0));
        put(Move.NUTMEG, Anim.of(Move.NUTMEG.duration)
                .at(0).rest()
                .at(2).rl(20, 20, 0).body(20, 0, 0).head(15, 0, 0).pos(0, -0.08f, 0)
                .at(4).rl(-40, 25, 8).body(25, 0, 0).arms(-20, 30, 20, -30).pos(0, -0.12f, 0)
                .at(6).rl(10, 0, 0).body(18, 0, 10).pos(0.35f, -0.05f, 0.1f)
                .at(8).body(15, 0, -8).pos(0.4f, 0, 0.35f)
                .at(10).rest().pos(0, 0, 0));
        put(Move.CHOP, Anim.of(Move.CHOP.duration)
                .at(0).rest()
                .at(2).rl(40, 0, 0).body(-6, -10, 0).arms(30, 30, -30, -40).head(10, 0, 0)
                .at(4).rl(-15, 35, -40).ll(-10, 0, 0).body(10, 30, 0).arms(-30, 60, 30, -30).root(0, 25, 0)
                .at(6).rl(10, 20, -25).root(0, 40, 0).pos(-0.2f, 0, 0)
                .at(9).rest().root(0, 0, 0).pos(0, 0, 0));
        put(Move.HEEL_FLICK, Anim.of(Move.HEEL_FLICK.duration)
                .at(0).rest()
                .at(2).rl(-20, 0, 0).body(8, 0, 0).head(20, 0, 0)
                .at(4).rl(60, 0, 0).ll(-10, 0, 0).body(25, 0, 0).arms(40, 15, 40, -15).head(-10, 0, 0)
                .at(5).rl(95, 0, 0).body(30, 0, 0).arms(50, 20, 50, -20).head(-30, 0, 0).pos(0, 0.1f, 0)
                .at(8).rl(20, 0, 0).body(12, 0, 0).head(-35, 0, 0).pos(0, 0, 0.2f)
                .at(12).rest().pos(0, 0, 0));
        put(Move.SEAL, Anim.of(Move.SEAL.duration)
                .at(0).rest()
                .at(2).rl(-50, 0, 0).body(-8, 0, 0).head(-20, 0, 0).arms(-30, 40, -30, -40)
                .at(4).rl(-80, 0, 0).body(-20, 0, 0).head(-45, 0, 0).arms(-40, 50, -40, -50).pos(0, 0.1f, 0)
                .at(7).rl(0, 0, 0).head(-40, 0, 0).body(-12, 0, 0)
                .at(10).head(-50, 0, 0)
                .at(13).head(-35, 0, 0)
                .at(16).rest().pos(0, 0, 0));
        Anim.B aw = Anim.of(Move.AROUND_WORLD.duration).at(0).rest();
        for (int i = 0; i < 6; i++) {
            float t0 = 2 + i * 5;
            aw.at(t0).rl(-55, -40 + i * 14, 10).ll(0, 0, -6).body(-6, 0, 0).arms(-25, 55, -25, -55).head(25, 0, 0)
                    .at(t0 + 2.5f).rl(-15, 40 - i * 14, 25).head(35, 0, 0);
        }
        put(Move.AROUND_WORLD, aw.at(Move.AROUND_WORLD.duration).rest());
        put(Move.SPEED_BURST, Anim.of(Move.SPEED_BURST.duration)
                .at(0).rest()
                .at(2).rl(-30, 0, 0).body(12, 0, 0)
                .at(3).rl(-60, 0, 0).ll(35, 0, 0).body(30, 0, 0).arms(60, 10, -70, -10).head(-15, 0, 0)
                .at(6).rl(45, 0, 0).ll(-70, 0, 0).body(32, 0, 0).arms(-70, 10, 60, -10)
                .at(9).rest());
        put(Move.FAKE_SHOT, Anim.of(Move.FAKE_SHOT.duration)
                .at(0).rest()
                .at(4).rl(75, 0, 8).body(-12, -18, 0).arms(40, 30, -55, -55).head(10, 0, 0)
                .at(7).rl(-15, 0, 0).body(10, 10, 0).arms(-30, 35, 25, -50)
                .at(8).rl(-12, 0, 0).body(14, 20, 6).head(20, 0, 0)
                .at(10).rl(20, 0, 0).body(10, -10, -8).pos(0.3f, 0, 0)
                .at(12).rest().pos(0, 0, 0));
        // vucut calimi: govde bir yana yatar, agirlik aktarilir, ters yone patlar
        put(Move.BODY_FEINT, Anim.of(Move.BODY_FEINT.duration)
                .at(0).rest()
                .at(2).body(6, 18, 0).root(0, 0, 14).ll(-10, 0, -14).rl(0, 0, 10).arms(-10, 45, 15, -20).head(10, 20, 0).pos(0.12f, -0.04f, 0)
                .at(4).body(8, 24, 0).root(0, 0, 20).arms(-15, 55, 20, -15).head(12, 26, 0).pos(0.18f, -0.06f, 0)
                .at(6).body(10, -20, 0).root(0, 0, -16).rl(-25, 0, -18).ll(15, 0, 6).arms(25, 20, -20, -60).head(8, -20, 0).pos(-0.2f, 0, 0.1f)
                .at(9).body(14, -8, 0).root(0, 0, -6).rl(20, 0, 0).ll(-30, 0, 0).pos(-0.35f, 0, 0.25f)
                .at(12).rest().root(0, 0, 0).pos(0, 0, 0));
        // ============================================================ SAVUNMA
        put(Move.TACKLE, Anim.of(Move.TACKLE.duration)
                .at(0).rest()
                .at(2).ll(-30, 0, 0).body(14, 0, 0).arms(-10, 40, -10, -40)
                .at(4).rl(-55, 25, 42).ll(-15, 0, -10).body(22, -20, -10).arms(-30, 70, 30, -40).pos(0.2f, -0.1f, 0.35f)
                .at(7).rl(-30, 15, 25).body(15, 0, 0).pos(0.1f, 0, 0.3f)
                .at(10).rest().pos(0, 0, 0));
        put(Move.SLIDE, Anim.of(Move.SLIDE.duration)
                .at(0).rest()
                .at(2).root(-55, 0, 18).pos(0, -0.4f, 0.1f).rl(-80, 0, 0).ll(-35, 0, 0).arms(-30, 75, 10, -60).head(30, 0, 0)
                .at(5).root(-72, 0, 22).pos(0, -0.62f, 0.2f).rl(-88, 0, 5).ll(-30, 0, 0).arms(-20, 85, 20, -70)
                .at(13).root(-72, 0, 22).pos(0, -0.62f, 0.2f)
                .at(16).root(-35, 0, 10).pos(0, -0.3f, 0.1f).rl(-20, 0, 0).ll(-60, 0, 0)
                .at(20).root(0, 0, 0).pos(0, 0, 0).rest());
        put(Move.SHOULDER, Anim.of(Move.SHOULDER.duration)
                .at(0).rest()
                .at(2).body(10, -20, -10).arms(15, 15, -10, -25).pos(-0.1f, 0, 0)
                .at(4).body(14, 15, 26).root(0, 0, 18).arms(30, 10, -30, -55).pos(0.35f, 0, 0.1f)
                .at(6).root(0, 0, 10).pos(0.25f, 0, 0.1f)
                .at(9).rest().root(0, 0, 0).pos(0, 0, 0));
        put(Move.BLOCK, Anim.of(Move.BLOCK.duration)
                .at(0).rest()
                .at(2).arms(35, -15, 35, 15).rl(0, 0, 20).ll(0, 0, -20).body(-5, 0, 0).head(-10, 50, 0).pos(0, 0.25f, 0)
                .at(6).arms(40, -20, 40, 20).rl(-10, 0, 28).ll(-10, 0, -28).head(-5, 60, 0).pos(0, 0.4f, 0)
                .at(11).pos(0, 0.1f, 0)
                .at(16).rest().pos(0, 0, 0));
        put(Move.PRESS, Anim.of(Move.PRESS.duration)
                .at(0).rest()
                .at(2).body(25, 0, 0).arms(60, 10, -60, -10).legs(-55, 40).head(-20, 0, 0)
                .at(5).arms(-60, 10, 60, -10).legs(40, -55)
                .at(8).arms(60, 10, -60, -10).legs(-55, 40)
                .at(12).rest());
        // ============================================================ KALECI
        put(Move.GK_DIVE_LEFT, dive(-1));
        put(Move.GK_DIVE_RIGHT, dive(1));
        put(Move.GK_PUNT, Anim.of(Move.GK_PUNT.duration)
                .at(0).arms(-70, 15, -70, -15).body(4, 0, 0)
                .at(3).arms(-50, 20, -50, -20).body(-6, 0, 0).rl(30, 0, 0)
                .at(5).arms(-10, 40, -30, -40).rl(70, 0, 0).body(-14, 0, 0)
                .at(7).rl(-110, 0, 0).body(-20, 0, 0).arms(-60, 50, -40, -60).head(-20, 0, 0).pos(0, 0.15f, 0)
                .at(10).rl(-95, 0, 0).pos(0, 0.05f, 0)
                .at(14).rest().pos(0, 0, 0));
        put(Move.GK_THROW, Anim.of(Move.GK_THROW.duration)
                .at(0).ra(-60, 0, 10).la(-60, 0, -10)
                .at(3).ra(55, 0, 15).la(-30, 0, -30).body(-8, -25, 0).ll(-25, 0, 0)
                .at(6).ra(-165, 0, 5).la(20, 0, -40).body(12, 20, 0).ll(-10, 0, 0).rl(20, 0, 0)
                .at(8).ra(-120, 0, 10).body(15, 25, 0)
                .at(12).rest());
        put(Move.CATCH, Anim.of(Move.CATCH.duration)
                .at(0).arms(-85, -15, -85, 15).body(5, 0, 0)
                .at(3).arms(-55, -30, -55, 30).body(18, 0, 0).head(15, 0, 0)
                .at(6).arms(-45, -35, -45, 35).body(12, 0, 0)
                .at(10).rest());
        // ============================================================ GOL SEVINCLERI
        put(Move.CELEB_SIU, Anim.of(Move.CELEB_SIU.duration)
                .at(0).rest()
                .at(3).body(18, 0, 0).legs(-25, 20).arms(40, 20, 40, -20).pos(0, -0.12f, 0)
                .at(6).body(-10, 0, 0).legs(10, 10).arms(-160, 20, -160, -20).pos(0, 0.5f, 0).root(0, 0, 0)
                .at(10).root(0, 120, 0).pos(0, 0.9f, 0).arms(-40, 60, -40, -60).legs(-20, -20)
                .at(14).root(0, 180, 0).pos(0, 0.4f, 0)
                .at(16).root(0, 180, 0).pos(0, 0, 0).rl(0, 0, 32).ll(0, 0, -32).body(-12, 0, 0)
                .arms(0, 70, 0, -70).head(-25, 0, 0)
                .at(31).root(0, 180, 0).rl(0, 0, 32).ll(0, 0, -32).body(-16, 0, 0).arms(10, 75, 10, -75).head(-30, 0, 0)
                .at(36).root(0, 180, 0).rest());
        put(Move.CELEB_KNEESLIDE, Anim.of(Move.CELEB_KNEESLIDE.duration)
                .at(0).rest()
                .at(3).legs(80, 80).pos(0, -0.45f, 0).body(-15, 0, 0).arms(-60, 40, -60, -40)
                .at(6).legs(88, 88).pos(0, -0.5f, 0).body(-32, 0, 0).arms(-150, 40, -150, -40).head(-40, 0, 0)
                .at(30).legs(88, 88).pos(0, -0.5f, 0).body(-35, 0, 0).arms(-160, 50, -160, -50).head(-45, 0, 0)
                .at(36).legs(40, 40).pos(0, -0.25f, 0).body(-10, 0, 0)
                .at(40).rest().pos(0, 0, 0));
        put(Move.CELEB_BACKFLIP, Anim.of(Move.CELEB_BACKFLIP.duration)
                .at(0).rest()
                .at(3).body(15, 0, 0).legs(-30, -30).arms(50, 10, 50, -10).pos(0, -0.15f, 0)
                .at(6).root(-90, 0, 0).pos(0, 0.7f, -0.1f).arms(-170, 10, -170, -10).legs(-80, -80)
                .at(10).root(-210, 0, 0).pos(0, 1.0f, -0.2f).legs(-110, -110)
                .at(14).root(-330, 0, 0).pos(0, 0.4f, -0.3f).legs(-30, -30).arms(-60, 40, -60, -40)
                .at(16).root(-360, 0, 0).pos(0, 0, -0.3f).arms(-30, 80, -30, -80)
                .at(22).root(-360, 0, 0).rest().pos(0, 0, 0));
        Anim.B plane = Anim.of(Move.CELEB_PLANE.duration).at(0).rest();
        for (int i = 1; i <= 9; i++) {
            float t = i * 5;
            float s = (i % 2 == 0) ? 1 : -1;
            plane.at(t).arms(0, 90, 0, -90).legs(40 * s, -40 * s).body(8, 0, 0).root(0, 0, 22 * (float) Math.sin(i * 0.9)).head(-10, 0, 0);
        }
        put(Move.CELEB_PLANE, plane.at(Move.CELEB_PLANE.duration).rest().root(0, 0, 0));
        Anim.B dance = Anim.of(Move.CELEB_DANCE.duration).at(0).rest();
        for (int i = 1; i <= 11; i++) {
            float t = i * 4;
            float s = (i % 2 == 0) ? 1 : -1;
            dance.at(t).ra(-150 + (s > 0 ? 0 : 110), 0, 20).la(-150 + (s > 0 ? 110 : 0), 0, -20)
                    .rl(s > 0 ? -30 : 0, 0, 8).ll(s > 0 ? 0 : -30, 0, -8).body(0, 15 * s, 10 * s)
                    .head(10, 20 * s, 0).pos(0.15f * s, i % 2 == 0 ? 0.1f : 0, 0);
        }
        put(Move.CELEB_DANCE, dance.at(Move.CELEB_DANCE.duration).rest().pos(0, 0, 0));
        put(Move.CELEB_SHUSH, Anim.of(Move.CELEB_SHUSH.duration)
                .at(0).rest()
                .at(4).ra(-120, -35, 8).la(0, 0, -15).head(5, 15, 0).body(0, 10, 0)
                .at(30).ra(-122, -38, 8).head(8, 18, 0).body(-4, 12, 0)
                .at(36).rest());
        // forma cikarma: kollar formayi bastan cikarir, sonra havada sallar
        Anim.B shirt = Anim.of(Move.CELEB_SHIRT_OFF.duration)
                .at(0).rest()
                .at(4).arms(-60, -30, -60, 30).body(12, 0, 0).head(20, 0, 0)
                .at(8).arms(-150, -15, -150, 15).body(4, 0, 0).head(-10, 0, 0)
                .at(11).arms(-175, 10, -175, -10).body(-6, 0, 0).head(-25, 0, 0);
        for (int i = 0; i < 9; i++) {
            float t = 14 + i * 5;
            float sw = (i % 2 == 0) ? 1 : -1;
            shirt.at(t).ra(-170, 20 * sw, 20 + 15 * sw).la(-20, 0, -25).body(-8, 10 * sw, 0).head(-30, 0, 0)
                    .legs(i % 2 == 0 ? -25 : 10, i % 2 == 0 ? 10 : -25).pos(0, i % 2 == 0 ? 0.08f : 0, 0);
        }
        put(Move.CELEB_SHIRT_OFF, shirt.at(Move.CELEB_SHIRT_OFF.duration).rest().pos(0, 0, 0));
        // pas isteme: kol havada, isaret
        put(Move.CALL_PASS, Anim.of(Move.CALL_PASS.duration)
                .at(0).rest()
                .at(3).ra(-165, 0, -10).la(-40, 0, -30).head(-15, 0, 0).body(-4, 0, 0)
                .at(6).ra(-155, 0, 15)
                .at(9).ra(-165, 0, -10)
                .at(14).rest());

        // ============================================================ YETENEKLER
        put(Move.AB_FIRE, Anim.of(Move.AB_FIRE.duration)
                .at(0).rest()
                .at(4).body(-15, 0, 0).arms(-150, 40, -150, -40).head(-30, 0, 0).legs(-10, 10)
                .at(9).body(-22, -25, 0).arms(-60, 100, -60, -100).rl(100, 0, 10).head(5, 0, 0).pos(0, 0.25f, 0)
                .at(11).rl(120, 0, 10).pos(0, 0.45f, 0)
                .at(13).rl(-120, 0, -5).body(30, 30, 0).arms(-40, 50, 40, -60).head(25, 0, 0).root(12, 0, 0).pos(0, 0.3f, 0.3f)
                .at(16).rl(-90, 0, 0).root(8, 0, 0).pos(0, 0, 0.5f)
                .at(22).rest().root(0, 0, 0).pos(0, 0, 0));
        put(Move.AB_THUNDER, Anim.of(Move.AB_THUNDER.duration)
                .at(0).rest()
                .at(3).body(-15, 0, 0).arms(-170, 15, -170, -15).head(-40, 0, 0).pos(0, 0.25f, 0)
                .at(6).arms(-175, 25, -175, -25).pos(0, 0.4f, 0)
                .at(9).body(35, 0, 0).arms(60, 10, 60, -10).legs(-60, 40).head(-20, 0, 0).pos(0, -0.1f, 0)
                .at(16).rest().pos(0, 0, 0));
        put(Move.AB_TORNADO, Anim.of(Move.AB_TORNADO.duration)
                .at(0).rest()
                .at(2).arms(-20, 80, -20, -80).body(0, 0, 0).root(0, 0, 0)
                .at(6).root(0, -260, 0).pos(0, 0.25f, 0).rl(-20, 0, 25)
                .at(10).root(0, -540, 0).pos(0, 0.3f, 0).rl(60, 0, 10)
                .at(12).root(0, -720, 0).rl(-110, 0, 0).body(20, 20, 0).pos(0, 0.15f, 0.2f)
                .at(15).root(0, -720, 0).rl(-80, 0, 0).pos(0, 0, 0.3f)
                .at(22).root(0, -720, 0).rest().pos(0, 0, 0));
        put(Move.AB_GHOST, Anim.of(Move.AB_GHOST.duration)
                .at(0).rest()
                .at(3).ra(-90, 0, -55).la(-90, 0, 55).head(25, 0, 0).body(8, 0, 0).pos(0, 0.15f, 0)
                .at(6).pos(0, 0.3f, 0)
                .at(9).ra(-40, 0, 70).la(-40, 0, -70).head(-20, 0, 0).pos(0, 0.25f, 0)
                .at(14).rest().pos(0, 0, 0));
        put(Move.AB_MAGNET, Anim.of(Move.AB_MAGNET.duration)
                .at(0).rest()
                .at(3).ra(-95, 0, 0).la(-60, 0, -25).body(12, 0, 0).ll(-20, 0, 0).head(10, 0, 0)
                .at(6).ra(-92, 4, 3).la(-62, 0, -22)
                .at(9).ra(-96, -4, -3)
                .at(12).ra(-93, 3, 2)
                .at(18).rest());
        put(Move.AB_ICE, Anim.of(Move.AB_ICE.duration)
                .at(0).rest()
                .at(4).rl(-95, 0, 0).body(-10, 0, 0).arms(-40, 60, -40, -60).pos(0, 0.15f, 0)
                .at(7).rl(-100, 0, 0).pos(0, 0.3f, 0)
                .at(8).rl(0, 0, 10).body(30, 0, 0).arms(20, 45, 20, -45).head(20, 0, 0).pos(0, -0.2f, 0)
                .at(13).body(25, 0, 0).pos(0, -0.2f, 0)
                .at(18).rest().pos(0, 0, 0));
        // ============================================================ TEPKILER
        put(Move.STUMBLE, Anim.of(Move.STUMBLE.duration)
                .at(0).rest()
                .at(2).root(10, 0, 22).arms(-130, 70, -60, -80).legs(-20, 15).head(-20, 0, 0)
                .at(5).root(6, 0, -18).arms(-60, 80, -140, -60).legs(15, -25)
                .at(8).root(8, 0, 12).arms(-110, 60, -80, -70)
                .at(12).root(4, 0, -6)
                .at(16).root(0, 0, 0).rest());
        put(Move.FALL, Anim.of(Move.FALL.duration)
                .at(0).rest()
                .at(3).root(40, 0, 15).arms(-120, 40, -120, -40).legs(30, -10).pos(0, -0.1f, 0.2f)
                .at(6).root(88, 0, 10).arms(-160, 30, -160, -30).pos(0, -0.62f, 0.5f)
                .at(22).root(88, 0, 5).pos(0, -0.62f, 0.5f).arms(-150, 35, -100, -40)
                .at(26).root(40, 0, 0).pos(0, -0.3f, 0.3f).legs(-60, 30).arms(-20, 20, -20, -20)
                .at(30).root(0, 0, 0).pos(0, 0, 0).rest());
    }

    private static Anim.B rabona(Move m, float strike) {
        int i = m.impact, d = m.duration;
        return Anim.of(d)
                .at(0).rest()
                .at(i - 6).ll(-15, 0, 0).body(0, -12, 0).arms(10, 35, -10, -35)
                .at(i - 3).rl(55, 30, -38).ll(-8, 0, 0).body(-6, -25, -10).arms(40, 50, -50, -60).head(15, -15, 0)
                .at(i).rl(strike, 35, -42).body(10, 20, -14).arms(-30, 70, 30, -50).head(20, 10, 0).pos(0, 0.05f, 0)
                .at(i + 3).rl(strike - 15, 25, -30).body(8, 25, -8).pos(0, 0.08f, 0.1f)
                .at(d).rest().pos(0, 0, 0);
    }

    /** Kaleci plonjonu: s = -1 sol, +1 sag. */
    private static Anim.B dive(int s) {
        return Anim.of(24)
                .at(0).rest().arms(-30, 20, -30, -20).body(10, 0, 0).pos(0, -0.08f, 0)
                .at(2).root(0, 0, 30 * s).pos(0.15f * s, 0.15f, 0).arms(-150, 25 * s, -150, 25 * s).legs(-10, 10)
                .at(5).root(0, 0, 70 * s).pos(0.3f * s, 0.1f, 0).arms(-178, 10 * s, -178, 10 * s).head(0, 0, 10 * s)
                .at(9).root(0, 0, 88 * s).pos(0.35f * s, -0.45f, 0).arms(-175, 5 * s, -170, 15 * s)
                .at(12).root(0, 0, 90 * s).pos(0.35f * s, -0.62f, 0)
                .at(18).root(0, 0, 90 * s).pos(0.35f * s, -0.62f, 0).arms(-120, 30 * s, -100, 40 * s)
                .at(22).root(0, 0, 30 * s).pos(0.1f * s, -0.2f, 0)
                .at(24).root(0, 0, 0).pos(0, 0, 0).rest();
    }
}
