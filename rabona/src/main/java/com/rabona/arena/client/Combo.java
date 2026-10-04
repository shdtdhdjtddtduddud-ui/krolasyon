package com.rabona.arena.client;

import com.rabona.arena.game.Move;

import java.util.ArrayList;
import java.util.List;

/**
 * FIFA tarzi kombo tanima. Girdi: sag cubuk (kumanda) ya da yon oklari (klavye), ekrana gore.
 * Yonler: 0 yukari (ileri), 1 sag, 2 asagi (geri), 3 sol.
 * <pre>
 *  ← / →          Vucut Calimi        ↓          Topu Geri Cekme      ↑        Hiz Patlamasi
 *  ↓ ↑ ↑          Gokkusagi           ↑ ↑        Sombrero             ↓ ↓      Cruyff Donusu
 *  ← ← / → →      La Croqueta         ← → / → ←  Makas                ↑ ↓      Bacak Arasi
 *  ↓ ↑            Topuk Asirtma       → ↓ / ← ↓  Ronaldo Kesisi       ↑ ← / ↑ →  Sahte Sut
 *  → ↓ ← / ← ↓ →  Elastico (yarim tur)          4 yon tam tur        Marsilya Donusu
 * </pre>
 */
public final class Combo {
    public record Result(Move move, int side) {}

    private final List<Integer> seq = new ArrayList<>();
    private int lastDir = -1, neutral;
    private boolean waitNeutral;

    /** dir: -1 notr, 0..3 yon. Kombo tamamlaninca sonuc doner. */
    public Result feed(int dir) {
        if (dir < 0) {
            lastDir = -1;
            waitNeutral = false;
            neutral++;
            if (!seq.isEmpty() && neutral >= 5) {
                Result r = match(seq);
                seq.clear();
                return r;
            }
            return null;
        }
        neutral = 0;
        if (waitNeutral) return null;
        if (dir != lastDir) {
            seq.add(dir);
            lastDir = dir;
            if (seq.size() >= 4) {
                Result r = match(seq);
                seq.clear();
                waitNeutral = true;
                return r;
            }
        }
        return null;
    }

    /** Analog cubuktan yon (olu bolge 0.55). */
    public static int dirOf(float x, float y) {
        if (x * x + y * y < 0.55f * 0.55f) return -1;
        if (Math.abs(x) > Math.abs(y)) return x > 0 ? 1 : 3;
        return y < 0 ? 0 : 2; // GLFW: yukari = negatif y
    }

    static Result match(List<Integer> s) {
        int n = s.size();
        int a = s.get(0), b = n > 1 ? s.get(1) : -1, c = n > 2 ? s.get(2) : -1;
        final int U = 0, R = 1, D = 2, L = 3;
        if (n >= 4) {
            boolean cw = true, ccw = true;
            for (int i = 1; i < 4; i++) {
                if (s.get(i) != (s.get(i - 1) + 1) % 4) cw = false;
                if (s.get(i) != (s.get(i - 1) + 3) % 4) ccw = false;
            }
            if (cw || ccw) return new Result(Move.ROULETTE, cw ? 1 : -1);
            return null;
        }
        if (n == 3) {
            if (a == D && b == U && c == U) return new Result(Move.RAINBOW, 0);
            if (a == R && b == D && c == L) return new Result(Move.ELASTICO, 1);
            if (a == L && b == D && c == R) return new Result(Move.ELASTICO, -1);
            if ((a == U && b == R && c == D) || (a == R && b == D && c == L)) return new Result(Move.ROULETTE, 1);
            if (a == U && b == L && c == D) return new Result(Move.ROULETTE, -1);
            return null;
        }
        if (n == 2) {
            if (a == U && b == U) return new Result(Move.SOMBRERO, 0);
            if (a == D && b == D) return new Result(Move.CRUYFF, 0);
            if (a == L && b == L) return new Result(Move.CROQUETA, -1);
            if (a == R && b == R) return new Result(Move.CROQUETA, 1);
            if ((a == L && b == R) || (a == R && b == L)) return new Result(Move.STEPOVER, b == R ? 1 : -1);
            if (a == U && b == D) return new Result(Move.NUTMEG, 0);
            if (a == D && b == U) return new Result(Move.HEEL_FLICK, 0);
            if ((a == R || a == L) && b == D) return new Result(Move.CHOP, a == R ? 1 : -1);
            if (a == U && (b == L || b == R)) return new Result(Move.FAKE_SHOT, b == R ? 1 : -1);
            return null;
        }
        if (a == L) return new Result(Move.BODY_FEINT, -1);
        if (a == R) return new Result(Move.BODY_FEINT, 1);
        if (a == D) return new Result(Move.DRAGBACK, 0);
        return new Result(Move.SPEED_BURST, 0);
    }
}
