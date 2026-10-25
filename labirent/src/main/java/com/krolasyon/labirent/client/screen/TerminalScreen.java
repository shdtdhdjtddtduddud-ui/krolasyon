package com.krolasyon.labirent.client.screen;

import com.krolasyon.labirent.client.ClientState;
import com.krolasyon.labirent.net.Net;
import com.krolasyon.labirent.story.StoryDirector;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

/** The burrow terminal: press the code words in the order of the maze sections. */
public class TerminalScreen extends Screen {
    private final List<Integer> entered = new ArrayList<>();
    private final int[] shuffled = {0, 1, 2, 3, 4, 5, 6, 7};

    public TerminalScreen() {
        super(Component.translatable("screen.labirent.terminal"));
        RandomSource r = RandomSource.create(System.nanoTime());
        for (int k = 7; k > 0; k--) {
            int s = r.nextInt(k + 1);
            int t = shuffled[k];
            shuffled[k] = shuffled[s];
            shuffled[s] = t;
        }
    }

    @Override
    protected void init() {
        Net.State st = ClientState.state;
        int words = st == null ? 0 : st.words();
        int x0 = width / 2 - 130, y0 = height / 2 - 30;
        for (int k = 0; k < 8; k++) {
            int sec = shuffled[k];
            boolean known = (words & (1 << sec)) != 0;
            Button b = Button.builder(Component.literal(known ? StoryDirector.WORDS[sec] : "?????"), btn -> {
                entered.add(sec);
                btn.active = false;
                if (entered.size() == 8) submit();
            }).bounds(x0 + (k % 4) * 66, y0 + (k / 4) * 24, 62, 20).build();
            b.active = known && !entered.contains(sec);
            addRenderableWidget(b);
        }
        addRenderableWidget(Button.builder(Component.translatable("screen.labirent.terminal.clear"), b -> {
            entered.clear();
            rebuildWidgets();
        }).bounds(width / 2 - 100, y0 + 60, 95, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.labirent.terminal.send"), b -> submit())
                .bounds(width / 2 + 5, y0 + 60, 95, 20).build());
    }

    private void submit() {
        int[] o = entered.stream().mapToInt(Integer::intValue).toArray();
        Net.CH.sendToServer(new Net.TerminalCode(o));
        onClose();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        g.fill(0, 0, width, height, 0xF0050A08);
        int x0 = width / 2 - 140, y0 = height / 2 - 90;
        g.fill(x0, y0, x0 + 280, y0 + 190, 0xFF0B1A12);
        g.fill(x0 + 1, y0 + 1, x0 + 279, y0 + 189, 0xFF06100A);
        g.drawCenteredString(font, Component.literal("W.I.C.K.E.D.  // KOD GİRİŞİ"), width / 2, y0 + 8, 0xFF40FF80);
        g.drawCenteredString(font, Component.translatable("screen.labirent.terminal.hint"), width / 2, y0 + 22, 0xFF30B060);
        StringBuilder sb = new StringBuilder("> ");
        for (int s : entered) sb.append(StoryDirector.WORDS[s]).append(' ');
        if ((System.currentTimeMillis() / 400) % 2 == 0) sb.append('_');
        g.drawString(font, sb.toString(), x0 + 10, y0 + 40, 0xFF70FFA0, false);
        super.render(g, mx, my, partial);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
