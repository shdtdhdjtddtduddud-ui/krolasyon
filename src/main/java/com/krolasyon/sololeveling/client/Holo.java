package com.krolasyon.sololeveling.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** Drawing helpers for the blue holographic "System" windows. */
public final class Holo {
    public static final int CYAN = 0x6FD8FF, DEEP = 0x07142B, TEXT = 0xE6F6FF, DIM = 0x8FB4D9, GOLD = 0xFFD86B, RED = 0xFF5566, PURPLE = 0xB58CFF;

    private Holo() {}

    public static int a(int rgb, float alpha) { return (Mth.clamp((int) (alpha * 255), 0, 255) << 24) | (rgb & 0xFFFFFF); }

    /** A System window: dark translucent body, glowing border, bright corners. */
    public static void panel(GuiGraphics g, int x, int y, int w, int h, float alpha, int accent) {
        g.fillGradient(x, y, x + w, y + h, a(0x0B2347, 0.86F * alpha), a(DEEP, 0.92F * alpha));
        // inner glow
        g.fillGradient(x + 1, y + 1, x + w - 1, y + 6, a(accent, 0.22F * alpha), a(accent, 0F));
        // border
        int b = a(accent, 0.75F * alpha);
        g.fill(x, y, x + w, y + 1, b);
        g.fill(x, y + h - 1, x + w, y + h, b);
        g.fill(x, y, x + 1, y + h, b);
        g.fill(x + w - 1, y, x + w, y + h, b);
        // outer soft glow
        int gl = a(accent, 0.18F * alpha);
        g.fill(x - 1, y - 1, x + w + 1, y, gl);
        g.fill(x - 1, y + h, x + w + 1, y + h + 1, gl);
        g.fill(x - 1, y, x, y + h, gl);
        g.fill(x + w, y, x + w + 1, y + h, gl);
        // corners
        int c = a(0xFFFFFF, 0.95F * alpha);
        int L = Math.min(8, Math.min(w, h) / 4);
        g.fill(x - 1, y - 1, x + L, y + 1, c);
        g.fill(x - 1, y - 1, x + 1, y + L, c);
        g.fill(x + w - L, y - 1, x + w + 1, y + 1, c);
        g.fill(x + w - 1, y - 1, x + w + 1, y + L, c);
        g.fill(x - 1, y + h - 1, x + L, y + h + 1, c);
        g.fill(x - 1, y + h - L, x + 1, y + h + 1, c);
        g.fill(x + w - L, y + h - 1, x + w + 1, y + h + 1, c);
        g.fill(x + w - 1, y + h - L, x + w + 1, y + h + 1, c);
        // scan line shimmer
        long t = Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime();
        int sy = y + (int) ((t * 2) % Math.max(1, h));
        g.fill(x + 1, sy, x + w - 1, sy + 1, a(accent, 0.07F * alpha));
    }

    /** Title bar with the "!" System badge used by the anime. */
    public static void title(GuiGraphics g, Font f, Component text, int x, int y, int w, float alpha, int accent) {
        g.fill(x + 4, y + 4, x + w - 4, y + 17, a(accent, 0.16F * alpha));
        int bx = x + 6, by = y + 5;
        g.fill(bx, by, bx + 11, by + 11, a(accent, 0.9F * alpha));
        g.drawString(f, "!", bx + 4, by + 2, a(0x001022, alpha), false);
        g.drawString(f, text, bx + 16, by + 2, a(TEXT, alpha), true);
        g.fill(x + 4, y + 18, x + w - 4, y + 19, a(accent, 0.5F * alpha));
    }

    public static void bar(GuiGraphics g, int x, int y, int w, int h, float frac, int c1, int c2) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, a(0x000000, 0.6F));
        g.fill(x, y, x + w, y + h, a(0x1A2A44, 0.9F));
        int fw = (int) (w * Mth.clamp(frac, 0, 1));
        if (fw > 0) {
            g.fillGradient(x, y, x + fw, y + h, a(c1, 1F), a(c2, 1F));
            g.fill(x, y, x + fw, y + 1, a(0xFFFFFF, 0.35F));
        }
    }

    public static boolean in(double mx, double my, int x, int y, int w, int h) { return mx >= x && my >= y && mx < x + w && my < y + h; }

    /** A flat System style button. Returns true if hovered. */
    public static boolean button(GuiGraphics g, Font f, Component text, int x, int y, int w, int h, double mx, double my, boolean enabled, int accent) {
        boolean hover = enabled && in(mx, my, x, y, w, h);
        g.fill(x, y, x + w, y + h, a(hover ? accent : 0x14315C, hover ? 0.55F : 0.75F));
        int b = a(enabled ? accent : 0x445566, 0.9F);
        g.fill(x, y, x + w, y + 1, b);
        g.fill(x, y + h - 1, x + w, y + h, b);
        g.fill(x, y, x + 1, y + h, b);
        g.fill(x + w - 1, y, x + w, y + h, b);
        g.drawCenteredString(f, text, x + w / 2, y + (h - 8) / 2, enabled ? (hover ? 0xFFFFFF : TEXT) : 0x6B7C8F);
        return hover;
    }
}
