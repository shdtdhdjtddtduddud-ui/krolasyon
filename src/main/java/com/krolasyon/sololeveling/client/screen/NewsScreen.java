package com.krolasyon.sololeveling.client.screen;

import com.krolasyon.sololeveling.client.ClientHooks;
import com.krolasyon.sololeveling.client.Holo;
import com.krolasyon.sololeveling.net.Net;
import com.krolasyon.sololeveling.system.Sys;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/** "Hunter News" — headlines of the living world and the list of open gates. */
public class NewsScreen extends Screen {
    private static final int[] CAT_COLOR = {0x5FA8FF, 0x7CFF9A, 0xFF4455, 0xFFD86B, 0xC86BFF, 0xB0C4DE};
    private final List<Component> headlines = new ArrayList<>();
    private final List<Long> days = new ArrayList<>();
    private final List<Integer> cats = new ArrayList<>();
    private final List<CompoundTag> gates = new ArrayList<>();
    private int scroll;

    public NewsScreen(CompoundTag t) {
        super(Component.empty());
        for (Tag x : t.getList("news", Tag.TAG_COMPOUND)) {
            CompoundTag e = (CompoundTag) x;
            headlines.add(DialogScreen.parse(e.getString("text")));
            days.add(e.getLong("day"));
            cats.add(e.getInt("cat"));
        }
        for (Tag x : t.getList("gates", Tag.TAG_COMPOUND)) gates.add((CompoundTag) x);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        int w = Math.min(420, width - 16), h = Math.min(240, height - 16), x = (width - w) / 2, y = (height - h) / 2;
        Holo.panel(g, x, y, w, h, 1F, 0xFFB347);
        g.fill(x + 4, y + 4, x + w - 4, y + 22, Holo.a(0xB0141E, 0.85F));
        g.pose().pushPose();
        g.pose().translate(x + 10, y + 8, 0);
        g.pose().scale(1.4F, 1.4F, 1);
        g.drawString(font, Sys.t("news.header"), 0, 0, 0xFFFFFF, true);
        g.pose().popPose();
        String live = "● LIVE";
        if ((minecraft.level.getGameTime() / 10) % 2 == 0) g.drawString(font, live, x + w - 12 - font.width(live), y + 9, 0xFF6666, true);
        int listW = w * 3 / 5;
        int ly = y + 28;
        for (int i = scroll; i < headlines.size(); i++) {
            List<FormattedCharSequence> lines = font.split(headlines.get(i), listW - 40);
            int hh = lines.size() * 10 + 6;
            if (ly + hh > y + h - 6) break;
            g.fill(x + 8, ly, x + 10, ly + hh - 3, Holo.a(CAT_COLOR[Math.min(5, cats.get(i))], 1F));
            g.drawString(font, Sys.t("news.day", days.get(i)), x + 14, ly, Holo.DIM, false);
            int ty = ly;
            for (FormattedCharSequence l : lines) {
                g.drawString(font, l, x + 50, ty, Holo.TEXT, false);
                ty += 10;
            }
            ly += hh;
        }
        if (headlines.isEmpty()) g.drawString(font, Sys.t("news.empty"), x + 14, ly, Holo.DIM, false);
        // gates column
        int gx = x + listW + 6, gw = w - listW - 14;
        g.fill(gx - 4, y + 26, gx - 3, y + h - 6, Holo.a(0xFFB347, 0.5F));
        g.drawString(font, Sys.t("news.gates", gates.size()), gx, y + 28, 0xFFB347, true);
        int gy = y + 42;
        for (CompoundTag gt : gates) {
            if (gy > y + h - 20) break;
            int c = gt.getInt("color");
            g.fill(gx, gy, gx + gw, gy + 22, Holo.a(0x0F2747, 0.7F));
            g.fill(gx, gy, gx + 3, gy + 22, Holo.a(c, 1F));
            g.drawString(font, Component.literal("[" + gt.getString("rank") + "] ").append(Component.translatable("sololeveling.place." + gt.getString("place"))), gx + 6, gy + 2, c, false);
            g.pose().pushPose();
            g.pose().scale(0.75F, 0.75F, 1);
            g.drawString(font, Component.translatable("sololeveling.theme." + gt.getString("theme")).copy().append("  " + gt.getInt("x") + ", " + gt.getInt("z")),
                    (int) ((gx + 6) / 0.75F), (int) ((gy + 13) / 0.75F), Holo.DIM, false);
            g.pose().popPose();
            Holo.button(g, font, Component.literal(">"), gx + gw - 16, gy + 4, 14, 14, mx, my, true, c);
            gy += 25;
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        int w = Math.min(420, width - 16), h = Math.min(240, height - 16), x = (width - w) / 2, y = (height - h) / 2;
        int listW = w * 3 / 5;
        int gx = x + listW + 6, gw = w - listW - 14;
        int gy = y + 42;
        for (CompoundTag gt : gates) {
            if (gy > y + h - 20) break;
            if (Holo.in(mx, my, gx + gw - 16, gy + 4, 14, 14)) {
                ClientHooks.playClick();
                Net.toServer(new Net.Action("travel", 0, 0, "gate:" + gt.getString("id")));
                onClose();
                return true;
            }
            gy += 25;
        }
        return super.mouseClicked(mx, my, b);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double d) {
        scroll = Math.max(0, Math.min(Math.max(0, headlines.size() - 1), scroll - (int) Math.signum(d)));
        return true;
    }
}
