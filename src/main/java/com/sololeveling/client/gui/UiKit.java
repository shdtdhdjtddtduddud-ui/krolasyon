package com.sololeveling.client.gui;

import net.minecraft.client.gui.GuiGraphics;

/** Holographic blue "System" look used by every Solo Leveling screen. */
public final class UiKit {
    private UiKit() {}

    public static final int BG = 0xE60A1220, BG2 = 0xCC101C30, BORDER = 0xFF3FB6F2, GLOW = 0x553FB6F2;
    public static final int TEXT = 0xFFE6F6FF, DIM = 0xFF8FB4CC, GOLD = 0xFFFFD25A, RED = 0xFFFF5A5A, GREEN = 0xFF6BE070, BLUE = 0xFF58A8FF, PURPLE = 0xFFB070FF;

    public static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, GLOW);
        g.fill(x, y, x + w, y + h, BG);
        g.fill(x, y, x + w, y + 1, BORDER);
        g.fill(x, y + h - 1, x + w, y + h, BORDER);
        g.fill(x, y, x + 1, y + h, BORDER);
        g.fill(x + w - 1, y, x + w, y + h, BORDER);
        // corner accents
        g.fill(x - 3, y - 3, x + 9, y - 1, 0xFFFFFFFF);
        g.fill(x - 3, y - 3, x - 1, y + 9, 0xFFFFFFFF);
        g.fill(x + w - 9, y + h + 1, x + w + 3, y + h + 3, 0xFFFFFFFF);
        g.fill(x + w + 1, y + h - 9, x + w + 3, y + h + 3, 0xFFFFFFFF);
    }

    public static void box(GuiGraphics g, int x, int y, int w, int h, int fill, int border) {
        g.fill(x, y, x + w, y + h, fill);
        g.fill(x, y, x + w, y + 1, border);
        g.fill(x, y + h - 1, x + w, y + h, border);
        g.fill(x, y, x + 1, y + h, border);
        g.fill(x + w - 1, y, x + w, y + h, border);
    }

    public static void bar(GuiGraphics g, int x, int y, int w, int h, float frac, int fill) {
        frac = Math.max(0, Math.min(1, frac));
        g.fill(x, y, x + w, y + h, 0xFF05080F);
        g.fill(x + 1, y + 1, x + 1 + (int) ((w - 2) * frac), y + h - 1, fill);
        g.fill(x + 1, y + 1, x + 1 + (int) ((w - 2) * frac), y + 2 + (h > 6 ? 1 : 0), 0x55FFFFFF);
        g.fill(x, y, x + w, y + 1, 0xFF22405A);
        g.fill(x, y + h - 1, x + w, y + h, 0xFF22405A);
    }

    public static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    public static String time(long gameTime) {
        if (gameTime <= 0) return "--";
        long day = gameTime / 24000;
        long t = (gameTime % 24000);
        int hh = (int) ((t / 1000 + 6) % 24), mm = (int) (t % 1000 * 60 / 1000);
        return String.format("D%d %02d:%02d", day, hh, mm);
    }
}
