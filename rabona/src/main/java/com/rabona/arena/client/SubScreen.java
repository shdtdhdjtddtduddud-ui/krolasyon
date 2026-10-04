package com.rabona.arena.client;

import com.rabona.arena.game.Cards;
import com.rabona.arena.game.Pos;
import com.rabona.arena.game.Tactic;
import com.rabona.arena.game.Team;
import com.rabona.arena.net.C2S;
import com.rabona.arena.net.Net;
import com.rabona.arena.net.S2C;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Oyuncu degisikligi (FIFA gibi): solda sahadakiler (kondisyon cubugu), sagda yedekler.
 * Fare: once sahadaki oyuncuya, sonra yedege tikla. Kumanda: yukari/asagi sec, sag/sol sutun, A onay, B kapat.
 * Klavye: P / kumanda: R3 (sag cubuga bas) ile acilir.
 */
public class SubScreen extends Screen {
    private final boolean p2;
    private int col, rowField, rowBench = -1;
    private int selField = -1;
    private int[] fieldBox = new int[4], benchBox = new int[4];
    private static final int ROW = 18;

    public SubScreen(boolean p2) {
        super(Component.translatable("screen.rabonaarena.subs"));
        this.p2 = p2;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        g.fillGradient(0, 0, width, height, 0xE0061A12, 0xE0021008);
        S2C.Bench b = ClientState.bench;
        g.drawString(font, title.copy().append(p2 ? "  (P2)" : ""), 12, 10, 0xFF69F0AE, true);
        if (b == null) return;
        Tactic tc = Tactic.byId(b.tactic());
        Component mg = Component.translatable("screen.rabonaarena.manager_line", b.manager(), tc.title(), tc.shape);
        g.drawString(font, mg, 12, 22, 0xFF80D8FF, false);
        Component left = Component.translatable("screen.rabonaarena.subs_left", b.subsLeft(), com.rabona.arena.game.Match.MAX_SUBS);
        g.drawString(font, left, width - font.width(left) - 12, 10, b.subsLeft() > 0 ? 0xFFFFD54F : 0xFFFF5252, true);
        int colW = Math.min(220, width / 2 - 24);
        int lx = width / 2 - colW - 8, rx = width / 2 + 8, y0 = 44;
        g.drawString(font, Component.translatable("screen.rabonaarena.on_pitch"), lx, y0 - 10, 0xFFB0BEC5, false);
        g.drawString(font, Component.translatable("screen.rabonaarena.bench"), rx, y0 - 10, 0xFFB0BEC5, false);
        fieldBox = new int[]{lx, y0, colW, b.field().size() * ROW};
        benchBox = new int[]{rx, y0, colW, b.bench().size() * ROW};
        for (int i = 0; i < b.field().size(); i++) {
            S2C.Bench.FieldEntry e = b.field().get(i);
            int y = y0 + i * ROW;
            boolean hover = in(fieldBox, mx, my) && (my - y0) / ROW == i;
            boolean focus = col == 0 && rowField == i;
            int bg = selField == i ? 0xC000C853 : hover || focus ? 0x80FFFFFF : 0x60000000;
            g.fill(lx, y, lx + colW, y + ROW - 2, bg);
            g.drawString(font, Pos.byId(e.pos()).shortName(), lx + 4, y + 4, 0xFFFFD54F, false);
            g.drawString(font, e.name() + (e.subbed() ? " ⇄" : ""), lx + 30, y + 4, 0xFFFFFFFF, false);
            g.drawString(font, Integer.toString(e.ovr()), lx + colW - 70, y + 4, 0xFFFFE082, false);
            // kondisyon
            int sw = 40, sx = lx + colW - 46;
            int st = Mth.clamp(e.stamina(), 0, 100);
            int c = st > 60 ? 0xFF00E676 : st > 30 ? 0xFFFFC400 : 0xFFFF1744;
            g.fill(sx, y + 6, sx + sw, y + 10, 0xFF263238);
            g.fill(sx, y + 6, sx + sw * st / 100, y + 10, c);
        }
        for (int i = 0; i < b.bench().size(); i++) {
            Cards.Card c = b.bench().get(i);
            int y = y0 + i * ROW;
            boolean hover = in(benchBox, mx, my) && (my - y0) / ROW == i;
            boolean focus = col == 1 && rowBench == i;
            g.fill(rx, y, rx + colW, y + ROW - 2, hover || focus ? 0x80FFFFFF : 0x60000000);
            g.drawString(font, Pos.byId(c.pos()).shortName(), rx + 4, y + 4, 0xFFFFD54F, false);
            g.drawString(font, c.name(), rx + 30, y + 4, 0xFFFFFFFF, false);
            String st = c.ovr() + "  " + c.pac() + "/" + c.sho() + "/" + c.pas();
            g.drawString(font, st, rx + colW - font.width(st) - 6, y + 4, 0xFFFFE082, false);
        }
        Component tip = Component.translatable(selField < 0 ? "screen.rabonaarena.sub_tip1" : "screen.rabonaarena.sub_tip2");
        g.drawCenteredString(font, tip, width / 2, height - 30, 0xFFB0BEC5);
        g.drawCenteredString(font, Component.translatable("screen.rabonaarena.sub_pad"), width / 2, height - 18, 0xFF78909C);
        super.render(g, mx, my, partial);
    }

    private static boolean in(int[] box, double mx, double my) {
        return mx >= box[0] && mx < box[0] + box[2] && my >= box[1] && my < box[1] + box[3];
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        S2C.Bench b = ClientState.bench;
        if (b != null) {
            if (in(fieldBox, mx, my)) {
                int i = (int) ((my - fieldBox[1]) / ROW);
                selField = selField == i ? -1 : i;
                rowField = i;
                col = 1;
                return true;
            }
            if (in(benchBox, mx, my) && selField >= 0) {
                confirm((int) ((my - benchBox[1]) / ROW));
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    private void confirm(int benchIdx) {
        S2C.Bench b = ClientState.bench;
        if (b == null || selField < 0 || selField >= b.field().size() || benchIdx < 0 || benchIdx >= b.bench().size()) return;
        Net.toServer(new C2S.Sub(b.field().get(selField).id(), benchIdx, p2));
        selField = -1;
        col = 0;
    }

    /** Kumanda ile gezinme (PadControl cagirir). */
    public void pad(Gamepads.Pad pad) {
        S2C.Bench b = ClientState.bench;
        if (b == null) return;
        if (pad.pressed(Gamepads.B)) {
            if (selField >= 0) selField = -1; else onClose();
            return;
        }
        if (pad.pressed(Gamepads.UP)) move(-1);
        if (pad.pressed(Gamepads.DOWN)) move(1);
        if (pad.pressed(Gamepads.LEFT)) col = 0;
        if (pad.pressed(Gamepads.RIGHT) && selField >= 0) col = 1;
        if (pad.pressed(Gamepads.A)) {
            if (col == 0) {
                selField = rowField;
                col = 1;
                if (rowBench < 0) rowBench = 0;
            } else {
                confirm(rowBench);
            }
        }
    }

    private void move(int d) {
        S2C.Bench b = ClientState.bench;
        if (col == 0) rowField = Mth.clamp(rowField + d, 0, Math.max(0, b.field().size() - 1));
        else rowBench = Mth.clamp(rowBench + d, 0, Math.max(0, b.bench().size() - 1));
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (Keys.SUBS.matches(key, scan)) {
            onClose();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    static Team team() {
        S2C.Bench b = ClientState.bench;
        return b == null ? Team.NONE : Team.byId(b.team());
    }
}
