package com.sololeveling.client.gui;

import com.sololeveling.client.ClientEvents;
import com.sololeveling.client.ClientHooks;
import com.sololeveling.gen.Content;
import com.sololeveling.player.SLPlayer;
import com.sololeveling.util.Ranks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.ForgeGui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** In-game overlay: level/mana panel, skill bar, daily quest tracker, system notifications and screen effects. */
public final class Hud {
    private Hud() {}

    public static void render(ForgeGui gui, GuiGraphics g, float pt, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        Font font = mc.font;
        SLPlayer d = ClientHooks.data;
        drawFx(g, font, w, h, pt);
        // ---- status panel (top-left)
        int x = 6, y = 6, pw = 132;
        UiKit.box(g, x, y, pw, 40, 0xB0081018, 0xFF3FB6F2);
        int rc = 0xFF000000 | Ranks.color(d.rank());
        g.fill(x + 1, y + 1, x + 22, y + 39, 0xFF0A1830);
        g.drawCenteredString(font, d.rank(), x + 12, y + 8, rc);
        g.drawCenteredString(font, "Lv", x + 12, y + 22, 0xFF8FB4CC);
        g.drawString(font, "Lv. " + d.level + "  " + Component.translatable("gui.sololeveling.job." + d.job).getString(), x + 27, y + 4, UiKit.TEXT, true);
        UiKit.bar(g, x + 27, y + 15, pw - 32, 8, d.mana / Math.max(1, d.maxMana()), 0xFF3F8CFF);
        g.drawString(font, (int) d.mana + "/" + (int) d.maxMana(), x + 30, y + 15, 0xFFFFFFFF, true);
        UiKit.bar(g, x + 27, y + 26, pw - 32, 5, (float) d.xp / Math.max(1, d.xpNeeded()), 0xFF3FD0A0);
        UiKit.bar(g, x + 27, y + 33, pw - 32, 3, d.fatigue / 100F, 0xFFD0A030);
        if (d.points > 0) {
            String pts = "+" + d.points + " " + Component.translatable("gui.sololeveling.points_short").getString() + "  [" + ClientEvents.KEY_SYSTEM.getTranslatedKeyMessage().getString() + "]";
            g.drawString(font, pts, x, y + 44, 0xFFFFD25A, true);
        }
        // ---- skill bar (bottom-right)
        List<Content.SkillDef> unlocked = new ArrayList<>();
        for (Content.SkillDef s : Content.SKILLS) if (d.hasSkill(s.id())) unlocked.add(s);
        if (!unlocked.isEmpty()) {
            int sel = Mth.clamp(ClientHooks.selectedSkill, 0, unlocked.size() - 1);
            ClientHooks.selectedSkill = sel;
            int n = Math.min(5, unlocked.size());
            int start = Mth.clamp(sel - 2, 0, Math.max(0, unlocked.size() - n));
            int bw = 26, total = n * (bw + 2);
            int bx = w - total - 8, by = h - 40;
            for (int i = 0; i < n; i++) {
                Content.SkillDef s = unlocked.get(start + i);
                boolean on = start + i == sel;
                int sx = bx + i * (bw + 2);
                UiKit.box(g, sx, by, bw, 26, on ? 0xE01E3A5A : 0xB0081018, on ? 0xFFFFFFFF : 0xFF3FB6F2);
                String ab = Component.translatable("skill.sololeveling." + s.id()).getString();
                String ini = ab.length() > 3 ? ab.substring(0, 3) : ab;
                g.drawCenteredString(font, ini, sx + bw / 2, by + 9, on ? 0xFFFFFFFF : 0xFF8FB4CC);
                float cd = ClientHooks.cooldownFrac(s.id());
                if (cd > 0) g.fill(sx + 1, by + 1 + (int) (24 * (1 - cd)), sx + bw - 1, by + 25, 0xAA000000);
                if (d.mana < s.mana()) g.fill(sx + 1, by + 1, sx + bw - 1, by + 25, 0x55203080);
            }
            Content.SkillDef cur = unlocked.get(sel);
            Component nm = Component.translatable("skill.sololeveling." + cur.id());
            g.drawString(font, nm, w - 8 - font.width(nm), by - 11, 0xFFE6F6FF, true);
            String keys = "[" + ClientEvents.KEY_CAST.getTranslatedKeyMessage().getString() + "] " + Component.translatable("gui.sololeveling.cast").getString();
            g.drawString(font, keys, w - 8 - font.width(keys), by + 29, 0xFF8FB4CC, true);
        }
        // ---- daily quest tracker (top-right)
        if (!d.dqClaimed && d.dailyDay >= 0) {
            int qx = w - 150, qy = 6;
            boolean done = d.dqKills >= d.dqKillTarget && d.dqDist >= d.dqDistTarget && (d.level < 10 || d.dqGates >= 1);
            UiKit.box(g, qx, qy, 144, d.level >= 10 ? 46 : 36, 0xA0081018, done ? 0xFF6BE070 : 0xFF3FB6F2);
            g.drawString(font, Component.translatable("gui.sololeveling.daily_name"), qx + 4, qy + 3, 0xFFFFD25A, true);
            g.drawString(font, Component.translatable("gui.sololeveling.dq_kills").getString() + " " + Math.min(d.dqKills, d.dqKillTarget) + "/" + d.dqKillTarget, qx + 4, qy + 14, d.dqKills >= d.dqKillTarget ? 0xFF6BE070 : 0xFFE6F6FF, false);
            g.drawString(font, Component.translatable("gui.sololeveling.dq_dist").getString() + " " + Math.min((int) d.dqDist, d.dqDistTarget) + "/" + d.dqDistTarget, qx + 4, qy + 24, d.dqDist >= d.dqDistTarget ? 0xFF6BE070 : 0xFFE6F6FF, false);
            if (d.level >= 10) g.drawString(font, Component.translatable("gui.sololeveling.dq_gate").getString() + " " + Math.min(d.dqGates, 1) + "/1", qx + 4, qy + 34, d.dqGates >= 1 ? 0xFF6BE070 : 0xFFE6F6FF, false);
        }
        drawToasts(g, font, w, h);
    }

    private static void drawToasts(GuiGraphics g, Font font, int w, int h) {
        int y = 30;
        Iterator<ClientHooks.Toast> it = ClientHooks.toasts.iterator();
        int[] cols = {0xFF3FB6F2, 0xFFFFD25A, 0xFFB070FF, 0xFFFF5A5A, 0xFF6BE070, 0xFFB070FF};
        while (it.hasNext()) {
            ClientHooks.Toast t = it.next();
            t.age++;
            if (t.age > 150) { it.remove(); continue; }
            float a = Math.min(1F, Math.min(t.age / 8F, (150 - t.age) / 14F));
            int bw = 230;
            int bx = (w - bw) / 2;
            int slide = (int) ((1 - Math.min(1F, t.age / 8F)) * -20);
            int alpha = (int) (a * 255) & 0xFF;
            int col = cols[Mth.clamp(t.kind, 0, 5)];
            g.fill(bx - 2, y + slide - 2, bx + bw + 2, y + slide + 34, (int) (a * 0x44) << 24 | (col & 0xFFFFFF));
            g.fill(bx, y + slide, bx + bw, y + slide + 32, (int) (a * 0xD0) << 24 | 0x081018);
            g.fill(bx, y + slide, bx + bw, y + slide + 1, alpha << 24 | (col & 0xFFFFFF));
            g.fill(bx, y + slide + 31, bx + bw, y + slide + 32, alpha << 24 | (col & 0xFFFFFF));
            g.fill(bx, y + slide, bx + 3, y + slide + 32, alpha << 24 | (col & 0xFFFFFF));
            if (alpha > 12) {
                g.drawCenteredString(font, Component.literal("[ ").append(t.title).append(" ]"), bx + bw / 2, y + slide + 5, (alpha << 24) | (col & 0xFFFFFF));
                g.drawCenteredString(font, t.body, bx + bw / 2, y + slide + 18, (alpha << 24) | 0xE6F6FF);
            }
            y += 38;
        }
    }

    private static void drawFx(GuiGraphics g, Font font, int w, int h, float pt) {
        if (ClientHooks.fxKind.isEmpty()) return;
        ClientHooks.fxAge++;
        int age = ClientHooks.fxAge;
        int life = switch (ClientHooks.fxKind) {
            case "arise" -> 60;
            case "levelup" -> 50;
            default -> 16;
        };
        if (age > life) { ClientHooks.fxKind = ""; return; }
        float t = age / (float) life;
        switch (ClientHooks.fxKind) {
            case "arise" -> {
                float a = (float) Math.sin(t * Math.PI);
                int al = (int) (a * 150);
                g.fillGradient(0, 0, w, h, (al << 24) | 0x2A0A50, ((al / 2) << 24) | 0x000000);
                g.pose().pushPose();
                g.pose().translate(w / 2F, h / 3F, 0);
                float sc = 3F + t * 1.5F;
                g.pose().scale(sc, sc, 1);
                String s = "ARISE";
                int ta = (int) (Math.min(1F, a * 1.6F) * 255);
                g.drawString(font, s, -font.width(s) / 2, -4, (ta << 24) | 0xC890FF, true);
                g.pose().popPose();
            }
            case "levelup" -> {
                float a = 1F - t;
                g.fillGradient(0, h - (int) (h * 0.5F * (1 - t * 0.2F)), w, h, 0x00_3FB6F2, ((int) (a * 90) << 24) | 0x3FB6F2);
                g.pose().pushPose();
                g.pose().translate(w / 2F, h / 4F, 0);
                g.pose().scale(2.6F, 2.6F, 1);
                String s = ClientHooks.fxArg <= 1 ? "AWAKENED" : "LEVEL UP!";
                int ta = (int) (Math.min(1F, a * 2F) * 255);
                g.drawString(font, s, -font.width(s) / 2, -4, (ta << 24) | 0xFFE060, true);
                g.pose().popPose();
            }
            default -> {
                int al = (int) ((1F - t) * 220);
                g.fill(0, 0, w, h, (al << 24) | 0xE0F4FF);
            }
        }
    }
}
