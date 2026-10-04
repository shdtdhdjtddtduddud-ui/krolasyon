package com.krolasyon.futbol.client;

import com.krolasyon.futbol.game.Move;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/** All 49 moves grouped in tabs. Left click: perform, right click: bind to the quick key. */
public class MoveScreen extends Screen {
    private static Move.Cat tab = Move.Cat.SKILL;

    public MoveScreen() { super(Component.literal("Hareketler")); }

    @Override
    protected void init() {
        Move.Cat[] cats = Move.Cat.values();
        int tw = Math.min(78, (width - 20) / cats.length);
        int tx = width / 2 - tw * cats.length / 2;
        for (int i = 0; i < cats.length; i++) {
            Move.Cat c = cats[i];
            Button b = Button.builder(Component.literal(c.title), btn -> {
                tab = c;
                rebuildWidgets();
            }).bounds(tx + i * tw, 26, tw - 2, 18).build();
            b.active = c != tab;
            addRenderableWidget(b);
        }
        List<Move> list = new ArrayList<>();
        for (Move m : Move.values()) if (m.cat == tab) list.add(m);
        int cols = width > 520 ? 3 : 2;
        int cw = Math.min(168, (width - 30) / cols);
        int ch = 42;
        int gx = width / 2 - cols * cw / 2;
        int gy = 52;
        for (int i = 0; i < list.size(); i++) {
            int col = i % cols, row = i / cols;
            addRenderableWidget(new Card(gx + col * cw, gy + row * (ch + 4), cw - 4, ch, list.get(i)));
        }
        addRenderableWidget(Button.builder(Component.literal("Kapat"), b -> onClose()).bounds(width / 2 - 40, height - 24, 80, 18).build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        g.drawCenteredString(font, "⚽ HAREKETLER — sol tık: yap • sağ tık: hızlı tuşa ata", width / 2, 10, 0xFFD54F);
        super.render(g, mx, my, pt);
        String fav = "V: " + ClientState.favSkill.title + "   B: " + ClientState.favSuper.title + "   H: " + ClientState.favCeleb.title;
        g.drawCenteredString(font, fav, width / 2, height - 36, 0xA0A0A0);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private class Card extends AbstractWidget {
        private final Move move;

        Card(int x, int y, int w, int h, Move move) {
            super(x, y, w, h, Component.literal(move.title));
            this.move = move;
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            Font f = Minecraft.getInstance().font;
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            boolean hov = isHovered();
            int c = move.cat.color;
            g.fill(x, y, x + w, y + h, hov ? 0xE0303840 : 0xD0181C22);
            g.fill(x, y, x + 3, y + h, 0xFF000000 | c);
            g.fillGradient(x + 3, y, x + w, y + h / 2, (hov ? 0x30000000 : 0x18000000) | c, 0x00000000);
            if (hov) {
                g.fill(x, y, x + w, y + 1, 0xFFFFFFFF);
                g.fill(x, y + h - 1, x + w, y + h, 0xFFFFFFFF);
            }
            String title = move.title;
            if (move == ClientState.favSkill || move == ClientState.favSuper || move == ClientState.favCeleb) title = "★ " + title;
            g.drawString(f, title, x + 7, y + 4, 0xFFFFFF, true);
            String tag = move.isSuper() ? (move.cooldown / 20) + " sn" : move.needsBall ? "⚽" : "";
            if (!tag.isEmpty()) g.drawString(f, tag, x + w - 5 - f.width(tag), y + 4, 0xB0B0B0, false);
            List<FormattedCharSequence> lines = f.split(Component.literal(move.desc), w - 12);
            for (int i = 0; i < Math.min(2, lines.size()); i++) g.drawString(f, lines.get(i), x + 7, y + 16 + i * 10, 0xC8C8C8, false);
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (!active || !visible || !isMouseOver(mx, my)) return false;
            playDownSound(Minecraft.getInstance().getSoundManager());
            if (button == 1) {
                if (move.isSuper()) ClientState.favSuper = move;
                else if (move.cat == Move.Cat.CELEBRATION) ClientState.favCeleb = move;
                else ClientState.favSkill = move;
                ClientState.savePrefs();
            } else {
                Minecraft.getInstance().setScreen(null);
                ClientEvents.send(move, 0.8F);
            }
            return true;
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput out) { defaultButtonNarrationText(out); }
    }
}
