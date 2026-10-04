package com.krolasyon.bosses.rpg.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/** Shared look for the RPG screens: dark parchment panels with golden borders. */
public abstract class RpgScreen extends Screen {
    protected int left, top, pw, ph;

    protected RpgScreen(String title) { super(Component.literal(title)); }

    /** called when new data arrives from the server */
    public void refresh() { this.rebuildWidgets(); }

    protected void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xFF8A6A2A);
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF2A1E10);
        g.fillGradient(x, y, x + w, y + h, 0xF0201810, 0xF0100C08);
        g.fill(x, y, x + w, y + 1, 0x60FFE0A0);
    }

    protected void title(GuiGraphics g, String s, int x, int y) {
        g.drawString(this.font, s, x, y, 0xFFE8C060, true);
    }

    protected int wrap(GuiGraphics g, String s, int x, int y, int width, int color) {
        List<FormattedCharSequence> lines = this.font.split(Component.literal(s), width);
        for (FormattedCharSequence l : lines) {
            g.drawString(this.font, l, x, y, color, false);
            y += 10;
        }
        return y;
    }

    protected Button button(String label, int x, int y, int w, int h, Button.OnPress press) {
        return this.addRenderableWidget(Button.builder(Component.literal(label), press).bounds(x, y, w, h).build());
    }

    protected void bar(GuiGraphics g, int x, int y, int w, int h, float frac, int color) {
        g.fill(x, y, x + w, y + h, 0xFF101010);
        g.fill(x + 1, y + 1, x + 1 + (int) ((w - 2) * Math.max(0, Math.min(1, frac))), y + h - 1, color);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
