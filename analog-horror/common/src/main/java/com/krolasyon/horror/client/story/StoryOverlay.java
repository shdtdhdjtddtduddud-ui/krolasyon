package com.krolasyon.horror.client.story;

import com.krolasyon.horror.story.StoryData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.List;

/** Letterbox bars, subtitles, chapter cards, the current objective and fades. */
public final class StoryOverlay {
    private StoryOverlay() {}

    static int argb(float a, int rgb) {
        return (Mth.clamp((int) (a * 255), 6, 255) << 24) | (rgb & 0xFFFFFF);
    }

    public static void render(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || (!ClientStory.active && ClientStory.cutscene == null && ClientStory.fade < 0.01F)) return;
        Font font = mc.font;
        int w = g.guiWidth(), h = g.guiHeight();
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        int bar = (int) (h * 0.12F * ClientStory.letterbox);
        if (bar > 0) {
            g.fill(0, 0, w, bar, 0xFF000000);
            g.fill(0, h - bar, w, h, 0xFF000000);
        }
        StoryData.Line line = ClientStory.currentLine();
        if (line != null) drawLine(g, font, w, h - Math.max(bar, (int) (h * 0.08F)) - 8, line, ClientStory.csTick - line.start());

        int ct = ClientStory.tick - ClientStory.chapterTick;
        if (ct >= 0 && ct < 150 && ClientStory.cutscene == null) {
            float a = Math.min(1F, ct / 20F) * Math.min(1F, (150 - ct) / 25F);
            g.fill(0, 0, w, h, argb(a * 0.85F, 0));
            String top = I18n.get("msg.krolasyonhorror.chapter", ClientStory.chapter);
            String title = I18n.get("chapter.krolasyonhorror." + ClientStory.chapter);
            String sub = I18n.get("chapter.krolasyonhorror." + ClientStory.chapter + ".sub");
            g.drawCenteredString(font, top, w / 2, h / 2 - 34, argb(a * 0.8F, 0xB0A890));
            g.pose().pushPose();
            g.pose().scale(2.6F, 2.6F, 1F);
            g.drawCenteredString(font, title, (int) (w / 2 / 2.6F), (int) ((h / 2 - 18) / 2.6F), argb(a, 0xEDE3C8));
            g.pose().popPose();
            g.drawCenteredString(font, sub, w / 2, h / 2 + 18, argb(a * 0.85F, 0xA09880));
        }

        if (ClientStory.active && ClientStory.cutscene == null && !ClientStory.objective.isEmpty() && ct >= 150) {
            int ot = ClientStory.tick - ClientStory.objectiveTick;
            String text = ClientStory.objectiveArg >= 0 ? I18n.get("objective.krolasyonhorror." + ClientStory.objective, ClientStory.objectiveArg)
                    : I18n.get("objective.krolasyonhorror." + ClientStory.objective);
            float a = Math.min(1F, ot / 15F);
            if (ot < 90) g.drawString(font, I18n.get("msg.krolasyonhorror.new_objective"), 10, 8, argb(a * (ot / 6 % 2 == 0 ? 1F : 0.6F), 0xE8C66A), true);
            g.fill(8, 20, 10, 30, argb(a * 0.8F, 0xE8C66A));
            g.drawString(font, text, 14, 21, argb(a * 0.9F, 0xEDE3C8), true);
        }

        int cg = ClientStory.tick - ClientStory.caughtTick;
        if (cg > 16 && cg < 70) {
            float a = Math.min(1F, (cg - 16) / 8F) * Math.min(1F, (70 - cg) / 10F);
            g.pose().pushPose();
            g.pose().scale(2F, 2F, 1F);
            g.drawCenteredString(font, I18n.get("msg.krolasyonhorror.caught"), w / 4, h / 4 - 4, argb(a, 0xC02020));
            g.pose().popPose();
        }
        if (ClientStory.fade > 0.01F) g.fill(0, 0, w, h, argb(ClientStory.fade, 0));
        g.pose().popPose();
    }

    static void drawLine(GuiGraphics g, Font font, int w, int baseline, StoryData.Line line, int t) {
        String full = I18n.get(line.key());
        int chars = Math.min(full.length(), (int) (t * 1.6F) + 1);
        String shown = full.substring(0, chars);
        float a = Math.min(1F, (line.dur() - t) / 8F);
        List<FormattedCharSequence> rows = font.split(Component.literal(shown), (int) (w * 0.72F));
        int y = baseline - rows.size() * 11;
        String sp = I18n.get("speaker.krolasyonhorror." + line.speaker());
        if (!sp.isEmpty()) g.drawCenteredString(font, sp, w / 2, y - 12, argb(a, 0xE8C66A));
        int maxW = 0;
        for (FormattedCharSequence r : rows) maxW = Math.max(maxW, font.width(r));
        g.fill(w / 2 - maxW / 2 - 4, y - 2, w / 2 + maxW / 2 + 4, y + rows.size() * 11, argb(a * 0.45F, 0));
        for (FormattedCharSequence r : rows) {
            g.drawString(font, r, w / 2 - font.width(r) / 2, y, argb(a, line.speaker().equals("you") || line.speaker().equals("narr") ? 0xC8C0B0 : 0xFFFFFF), true);
            y += 11;
        }
    }
}
