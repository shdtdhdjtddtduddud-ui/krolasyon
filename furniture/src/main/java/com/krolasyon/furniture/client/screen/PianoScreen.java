package com.krolasyon.furniture.client.screen;

import com.krolasyon.furniture.network.ModNetwork;
import com.krolasyon.furniture.network.PianoNotePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.HashSet;
import java.util.Set;

/** two-octave playable keyboard: mouse (click / drag) or the computer keyboard (Z-M, S D G H J, Q-U, 2 3 5 6 7) */
public class PianoScreen extends Screen {
    private static final int[] WHITE = {0, 2, 4, 5, 7, 9, 11, 12, 14, 16, 17, 19, 21, 23};
    private static final int[] BLACK = {1, 3, 6, 8, 10, 13, 15, 18, 20, 22};
    private static final int[] BLACK_AFTER = {0, 1, 3, 4, 5, 7, 8, 10, 11, 12};
    private static final String WHITE_KEYS = "ZXCVBNMQWERTYU";
    private static final String BLACK_KEYS = "SDGHJ23567";

    private final BlockPos pos;
    private final int[] flash = new int[24];
    private final Set<Integer> held = new HashSet<>();
    private int lastDrag = -1;

    public PianoScreen(BlockPos pos) {
        super(Component.translatable("gui.krolasyonfurniture.piano"));
        this.pos = pos;
    }

    private int kw() { return Math.max(14, Math.min(34, (width - 30) / 14)); }

    private int left() { return (width - kw() * 14) / 2; }

    private int whiteH() { return Math.min(110, height / 3); }

    private int top() { return height - whiteH() - 28; }

    private int blackW() { return Math.max(8, kw() * 6 / 10); }

    private int blackX(int j) { return left() + (BLACK_AFTER[j] + 1) * kw() - blackW() / 2; }

    private int blackH() { return whiteH() * 62 / 100; }

    private int noteAt(double mx, double my) {
        if (my < top() || my >= top() + whiteH()) return -1;
        if (my < top() + blackH()) {
            for (int j = 0; j < BLACK.length; j++) {
                int bx = blackX(j);
                if (mx >= bx && mx < bx + blackW()) return BLACK[j];
            }
        }
        int i = (int) ((mx - left()) / kw());
        if (mx < left() || i < 0 || i >= 14) return -1;
        return WHITE[i];
    }

    private void press(int note) {
        if (note < 0) return;
        flash[note] = 7;
        ModNetwork.CHANNEL.sendToServer(new PianoNotePacket(pos, (byte) note));
    }

    private static int keyToNote(int key) {
        char c = (char) key;
        int wi = WHITE_KEYS.indexOf(c);
        if (wi >= 0) return WHITE[wi];
        int bi = BLACK_KEYS.indexOf(c);
        if (bi >= 0) return BLACK[bi];
        return -1;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int n = noteAt(mx, my);
        if (n >= 0) {
            press(n);
            lastDrag = n;
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        int n = noteAt(mx, my);
        if (n >= 0 && n != lastDrag) {
            press(n);
            lastDrag = n;
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        lastDrag = -1;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        int n = keyToNote(key);
        if (n >= 0) {
            if (held.add(key)) press(n);
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean keyReleased(int key, int scan, int mods) {
        held.remove(key);
        return super.keyReleased(key, scan, mods);
    }

    @Override
    public void tick() {
        for (int i = 0; i < flash.length; i++) if (flash[i] > 0) flash[i]--;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        int l = left(), t = top(), kw = kw(), wh = whiteH();
        g.fill(l - 8, t - 14, l + kw * 14 + 8, t + wh + 8, 0xFF3A1E22);
        g.fill(l - 6, t - 12, l + kw * 14 + 6, t + wh + 6, 0xFF6E3C36);
        g.fill(l - 4, t - 4, l + kw * 14 + 4, t + wh + 4, 0xFF1C0F13);
        for (int i = 0; i < 14; i++) {
            int n = WHITE[i];
            boolean down = flash[n] > 0;
            int x = l + i * kw;
            g.fill(x + 1, t, x + kw - 1, t + wh, down ? 0xFFB9CCE4 : 0xFFF4EEDA);
            g.fill(x + 1, t + wh - 3, x + kw - 1, t + wh, down ? 0xFF7E94B4 : 0xFFC9BFA2);
            g.drawCenteredString(font, String.valueOf(WHITE_KEYS.charAt(i)), x + kw / 2, t + wh - 14, 0xFF55453C);
        }
        for (int j = 0; j < BLACK.length; j++) {
            int n = BLACK[j];
            boolean down = flash[n] > 0;
            int x = blackX(j);
            g.fill(x, t, x + blackW(), t + blackH(), down ? 0xFF4C6C9C : 0xFF17121C);
            g.fill(x + 1, t, x + blackW() - 1, t + blackH() - 3, down ? 0xFF5E80B2 : 0xFF2B2434);
            g.drawCenteredString(font, String.valueOf(BLACK_KEYS.charAt(j)), x + blackW() / 2, t + blackH() - 13, 0xFFC8BFD8);
        }
        g.drawCenteredString(font, title, width / 2, t - 40, 0xFFF4EBDD);
        g.drawCenteredString(font, Component.translatable("gui.krolasyonfurniture.piano.hint"), width / 2, t - 26, 0xFFD8C4A8);
        super.render(g, mx, my, partial);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
