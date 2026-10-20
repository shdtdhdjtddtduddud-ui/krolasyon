package com.krolasyon.swordvale.world;

import com.krolasyon.swordvale.SwordVale;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Yere saplanmış dev taş kılıcın geometrisi: elmas kesitli bıçak (yıpranmış, kenarları kırık, yüzünde rünler),
 * uçları sarkık siper, sargılı kabza, topuz. Her blok, kılıcın yerel eksenlerine dönüştürülüp test edilir.
 */
public final class SwordShape {
    private static final String[] GLYPHS = {
            "X..XX.X.XX..", "X..X.XXX.X.X", ".X.XXX.X..X.", "X.XXX.X..X..", "XXXX..XX.X..",
            "X..XX.X..XX.", ".X.X.X.X.X.X", "X.X.X..X.X.X", "XX.X.XXX.X.X", "..X.XXX.X..X"};

    public final ValeLayout.Sword s;
    private final double px, py, pz;
    private final double ax, ay, az, bx, by, bz, cx, cy, cz;
    private final float L, W, T, buried, bladeEnd, guardH, guardHalf, gripEnd, gripR, pomR, pomC, runeStart;

    public SwordShape(ValeLayout.Sword s, int groundY) {
        this.s = s;
        L = s.length();
        W = s.width();
        T = Math.max(1.0F, W * 0.36F);
        buried = s.lean() > Math.toRadians(55) ? 0.06F * L : 0.17F * L;
        bladeEnd = 0.62F * L;
        guardH = Math.max(1.5F, 0.045F * L);
        guardHalf = Math.max(W * 1.3F, 0.15F * L);
        gripEnd = bladeEnd + guardH + 0.22F * L;
        gripR = Math.max(0.6F, W * 0.24F);
        pomR = Math.max(1.0F, W * 0.36F);
        pomC = gripEnd + pomR * 0.8F;
        runeStart = buried + 2;

        px = s.x() + 0.5;
        py = groundY + 0.5;
        pz = s.z() + 0.5;
        double sl = Math.sin(s.lean()), cl = Math.cos(s.lean());
        ax = sl * Math.cos(s.leanDir());
        ay = cl;
        az = sl * Math.sin(s.leanDir());
        double hx = -Math.sin(s.twist()), hz = Math.cos(s.twist());
        double dot = hx * ax + hz * az;
        double ux = hx - ax * dot, uy = -ay * dot, uz = hz - az * dot;
        double ul = Math.sqrt(ux * ux + uy * uy + uz * uz);
        bx = ux / ul;
        by = uy / ul;
        bz = uz / ul;
        cx = ay * bz - az * by;
        cy = az * bx - ax * bz;
        cz = ax * by - ay * bx;
    }

    public double baseX() { return px; }
    public double baseZ() { return pz; }
    public int groundY() { return (int) Math.floor(py); }
    public float width() { return W; }

    /** Kılıç ekseninde (yerden yukarı) a uzaklığındaki dünya noktası. */
    public double[] axisPoint(double a) {
        double t = a - buried;
        return new double[]{px + ax * t, py + ay * t, pz + az * t};
    }

    public int[] bounds() {
        double r = Math.max(guardHalf, W) + 2;
        double[] lo = axisPoint(0), hi = axisPoint(pomC + pomR + 1);
        return new int[]{(int) Math.floor(Math.min(lo[0], hi[0]) - r), (int) Math.floor(Math.min(lo[1], hi[1]) - r), (int) Math.floor(Math.min(lo[2], hi[2]) - r),
                (int) Math.ceil(Math.max(lo[0], hi[0]) + r), (int) Math.ceil(Math.max(lo[1], hi[1]) + r), (int) Math.ceil(Math.max(lo[2], hi[2]) + r)};
    }

    /** Bloğun (merkezinin) ait olduğu kılıç bloğu; dışarıdaysa null. */
    public BlockState blockAt(int x, int y, int z) {
        double dx = x + 0.5 - px, dy = y + 0.5 - py, dz = z + 0.5 - pz;
        double a = dx * ax + dy * ay + dz * az + buried;
        if (a < 0 || a > pomC + pomR * 1.2) return null;
        double b = dx * bx + dy * by + dz * bz;
        double c = dx * cx + dy * cy + dz * cz;
        double ab = Math.abs(b), ac = Math.abs(c);
        long seed = s.seed();

        if (a < bladeEnd) {
            double halfW = W / 2;
            if (a < 0.1 * L) halfW *= Math.max(0.15, a / (0.1 * L));
            if (ab > halfW) return null;
            double halfT = Math.max(0.5, T / 2 * (1 - 0.45 * ab / Math.max(halfW, 0.5)));
            if (ac > halfT) return null;
            if (a < buried - 0.5) return Blocks.STONE.defaultBlockState();
            // yıpranmış, kırık kenarlar
            if (ab > halfW - 1 && Noise.hash(seed + 3, x, y, z) < 0.12 && W > 2) return null;
            if (W >= 3.4F && ac > halfT - 0.95 && a > runeStart && a < bladeEnd - 2 && rune(a, b)) {
                return SwordVale.RUNE_STONE.get().defaultBlockState();
            }
            return stone(x, y, z, a, true);
        }
        float gStart = bladeEnd, gEnd = bladeEnd + guardH;
        if (a < gEnd + guardH) {
            double droop = guardH * 0.9 * Math.pow(ab / guardHalf, 2);
            double ga = a - gStart + droop;
            double thick = Math.max(0.6, T * 0.75 + 0.3) * (1 - 0.25 * ab / guardHalf);
            if (ab <= guardHalf && ga >= 0 && ga <= guardH && ac <= thick) {
                if (Noise.hash(seed + 4, x, y, z) < 0.3 || ga > guardH - 1) return Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
                return Noise.hash(seed + 5, x, y, z) < 0.2 ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState();
            }
        }
        if (a >= gEnd && a <= gripEnd) {
            if (b * b + c * c <= gripR * gripR) {
                double ang = Math.atan2(c, b);
                boolean band = ((int) Math.floor((a - gEnd) * 0.9 + ang * 0.6)) % 2 == 0;
                if (Noise.fbm2(seed + 6, a * 0.2, ang, 2) > 0.62) return Blocks.MOSSY_COBBLESTONE.defaultBlockState();
                return band ? Blocks.POLISHED_ANDESITE.defaultBlockState() : Blocks.STONE.defaultBlockState();
            }
        }
        double pa = a - pomC;
        if ((pa * pa) / (pomR * pomR * 1.21) + (b * b + c * c) / (pomR * pomR) <= 1) {
            return Noise.hash(seed + 7, x, y, z) < 0.5 ? Blocks.CHISELED_STONE_BRICKS.defaultBlockState() : Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        }
        return null;
    }

    private BlockState stone(int x, int y, int z, double a, boolean blade) {
        long seed = s.seed();
        double moss = Noise.fbm3(seed + 1, x * 0.13, y * 0.09, z * 0.13, 3);
        double thr = 0.6 - 0.14 * (1 - Math.min(1, a / L));
        if (moss > thr) return Noise.hash(seed + 2, x, y, z) < 0.5 ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : Blocks.MOSS_BLOCK.defaultBlockState();
        if (moss > thr - 0.05) return Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        double h = Noise.hash(seed + 8, x, y, z);
        double streak = Noise.value2(seed + 9, a * 0.35, (x + z) * 0.4);
        if (streak > 0.7) return Blocks.ANDESITE.defaultBlockState();
        if (h < 0.62) return Blocks.STONE.defaultBlockState();
        if (h < 0.84) return Blocks.ANDESITE.defaultBlockState();
        if (h < 0.94) return Blocks.COBBLESTONE.defaultBlockState();
        return Blocks.TUFF.defaultBlockState();
    }

    /** Bıçağın orta sütununa kazınmış rün yazısı (3x4 glifler, aralarında 1 satır boşluk). */
    private boolean rune(double a, double b) {
        int cb = (int) Math.round(b) + 1;
        if (cb < 0 || cb > 2) return false;
        double ra = a - runeStart;
        int row = (int) Math.floor(ra / 5);
        int ca = (int) Math.floor(ra - row * 5);
        if (ca > 3) return false;
        int g = (int) (Noise.hash(s.seed() + 11, row, 0, 0) * GLYPHS.length);
        String glyph = GLYPHS[Math.min(GLYPHS.length - 1, g)];
        int rowIdx = 3 - ca;
        return glyph.charAt(rowIdx * 3 + cb) == 'X';
    }

    /** Mezar sandığının yeri: kılıcın önünde, toprağın 1 blok altında. */
    public BlockPos gravePos() {
        double fx = -bz, fz = bx;
        double fl = Math.sqrt(fx * fx + fz * fz);
        if (fl < 1e-4) { fx = 1; fz = 0; fl = 1; }
        double d = W / 2 + T + 2.5;
        return BlockPos.containing(px + fx / fl * d, py - 1.5, pz + fz / fl * d);
    }
}
