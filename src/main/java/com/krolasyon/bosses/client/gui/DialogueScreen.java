package com.krolasyon.bosses.client.gui;

import com.krolasyon.bosses.net.Net;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/** Parchment-style conversation window; every option is a button that answers the server. */
public class DialogueScreen extends Screen {
    private final Net.DialogueMsg msg;
    private boolean answered = false;
    private int boxX, boxY, boxW, boxH;
    private List<FormattedCharSequence> lines = List.of();

    public DialogueScreen(Net.DialogueMsg msg) {
        super(Component.translatable(msg.speakerKey()));
        this.msg = msg;
    }

    public int npcId() { return msg.npcId(); }

    @Override
    protected void init() {
        boxW = Math.min(width - 20, 340);
        boxX = (width - boxW) / 2;
        Object[] a = msg.args().toArray();
        lines = font.split(Component.translatable(msg.textKey(), a), boxW - 24);
        int optH = 22;
        int textH = lines.size() * (font.lineHeight + 2);
        boxH = 34 + textH + 10 + msg.options().size() * (optH + 3);
        boxY = Math.max(6, height - boxH - 24);
        int y = boxY + 34 + textH + 10;
        for (int i = 0; i < msg.options().size(); i++) {
            final int idx = i;
            Component label = Component.translatable(msg.options().get(i));
            addRenderableWidget(Button.builder(label, b -> choose(idx)).bounds(boxX + 10, y, boxW - 20, optH).build());
            y += optH + 3;
        }
    }

    private void choose(int idx) {
        answered = true;
        Net.CH.sendToServer(new Net.ChoiceMsg(msg.npcId(), idx));
        // the server answers with the next page (a new screen) or nothing at all -> close
        onClose();
    }

    @Override
    public void onClose() {
        if (!answered) {
            answered = true;
            Net.CH.sendToServer(new Net.ChoiceMsg(msg.npcId(), -1));
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        int col = 0xFF000000 | msg.color();
        g.fill(boxX - 2, boxY - 2, boxX + boxW + 2, boxY + boxH + 2, 0xFF000000 | ((col >> 1) & 0x7F7F7F));
        g.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xE6140E10);
        g.fill(boxX, boxY, boxX + boxW, boxY + 22, 0xFF000000 | ((col >> 2) & 0x3F3F3F));
        g.fill(boxX, boxY + 22, boxX + boxW, boxY + 23, col);
        g.drawString(font, getTitle(), boxX + 10, boxY + 7, col, true);
        int y = boxY + 30;
        for (FormattedCharSequence l : lines) {
            g.drawString(font, l, boxX + 12, y, 0xE8DCC8, false);
            y += font.lineHeight + 2;
        }
        super.render(g, mx, my, pt);
    }
}
