package com.krolasyon.sololeveling.client.screen;

import com.krolasyon.sololeveling.client.ClientHooks;
import com.krolasyon.sololeveling.client.Holo;
import com.krolasyon.sololeveling.net.Net;
import com.krolasyon.sololeveling.system.Sys;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/** "You have acquired the qualifications to be a Player. Will you accept?" */
public class AwakenScreen extends Screen {
    private int ticks;
    private boolean declined;

    public AwakenScreen() { super(Sys.t("awaken.title")); }

    @Override
    public void tick() { ticks++; }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        g.fill(0, 0, width, height, Holo.a(0x000814, 0.7F));
        float alpha = Math.min(1F, (ticks + partial) / 20F);
        int w = 280, h = 120, x = (width - w) / 2, y = (height - h) / 2;
        int jitter = ticks < 12 && ticks % 3 == 0 ? 2 : 0;
        Holo.panel(g, x + jitter, y, w, h, alpha, declined ? Holo.RED : Holo.CYAN);
        Holo.title(g, font, Sys.t("system.notification"), x, y, w, alpha, declined ? Holo.RED : Holo.CYAN);
        List<FormattedCharSequence> lines = font.split(Sys.t(declined ? "awaken.declined_text" : "awaken.text"), w - 30);
        int ly = y + 32;
        for (FormattedCharSequence l : lines) {
            g.drawCenteredString(font, l, width / 2, ly, Holo.a(Holo.TEXT, alpha));
            ly += 11;
        }
        if (ticks > 20) {
            Holo.button(g, font, Sys.t("awaken.accept"), x + 30, y + h - 26, 100, 16, mx, my, true, Holo.CYAN);
            Holo.button(g, font, Sys.t("awaken.decline"), x + w - 130, y + h - 26, 100, 16, mx, my, !declined, Holo.RED);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        int w = 280, h = 120, x = (width - w) / 2, y = (height - h) / 2;
        if (ticks <= 20) return true;
        if (Holo.in(mx, my, x + 30, y + h - 26, 100, 16)) {
            ClientHooks.playClick();
            Net.toServer(new Net.Action("awaken", 1, 0, ""));
            onClose();
            return true;
        }
        if (!declined && Holo.in(mx, my, x + w - 130, y + h - 26, 100, 16)) {
            ClientHooks.playClick();
            declined = true;
            ticks = 0;
            return true;
        }
        return true;
    }
}
