package com.krolasyon.futbol.client;

import com.krolasyon.futbol.game.Team;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.ForgeGui;

/** Scoreboard, clock, goal banner, kick-off countdown, shot power bar and control hints. */
public final class Hud {
    private Hud() {}

    private static final String[] STATES = {"", "BAŞLAMA VURUŞU", "", "GOL!", "MAÇ BİTTİ"};

    public static void render(ForgeGui gui, GuiGraphics g, float partial, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) return;
        Font font = mc.font;
        boolean active = ClientState.state != 0;
        if (active) scoreboard(g, font, w);
        if (ClientState.state == 1) {
            int sec = ClientState.stateTimer / 20 + 1;
            String s = sec > 3 ? "Hazırlan..." : Integer.toString(sec);
            big(g, font, s, w / 2, h / 2 - 50, 4F, 0xFFFFFF);
            centered(g, font, "Başlama vuruşu: " + Team.byId(ClientState.kickoffTeam).title, w / 2, h / 2 - 12, Team.byId(ClientState.kickoffTeam).color);
        }
        if (ClientState.goalTicks > 0) goalBanner(g, font, w, h, partial);
        if (ClientState.state == 4) {
            Team win = ClientState.red > ClientState.blue ? Team.RED : ClientState.blue > ClientState.red ? Team.BLUE : Team.NONE;
            g.fill(0, h / 2 - 46, w, h / 2 + 10, 0xB0000000);
            big(g, font, "MAÇ BİTTİ", w / 2, h / 2 - 40, 3F, 0xFFD54F);
            centered(g, font, win == Team.NONE ? "Berabere!" : "Kazanan: " + win.title, w / 2, h / 2 - 6, win == Team.NONE ? 0xFFFFFF : win.color);
        }
        if (ClientState.charging) chargeBar(g, w, h);
        hints(g, font, w, h, active);
    }

    private static void scoreboard(GuiGraphics g, Font font, int w) {
        int bw = 220, x = w / 2 - bw / 2, y = 4;
        g.fill(x - 1, y - 1, x + bw + 1, y + 23, 0xFF101418);
        g.fillGradient(x, y, x + bw, y + 22, 0xF0283038, 0xF0161B20);
        // team blocks
        g.fill(x, y, x + 74, y + 22, 0xFFB71C1C);
        g.fillGradient(x, y, x + 74, y + 11, 0x40FFFFFF, 0x00FFFFFF);
        g.fill(x + bw - 74, y, x + bw, y + 22, 0xFF0D47A1);
        g.fillGradient(x + bw - 74, y, x + bw, y + 11, 0x40FFFFFF, 0x00FFFFFF);
        g.drawString(font, Team.RED.abbr, x + 6, y + 7, 0xFFFFFF, true);
        g.drawString(font, Team.BLUE.abbr, x + bw - 6 - font.width(Team.BLUE.abbr), y + 7, 0xFFFFFF, true);
        String rs = Integer.toString(ClientState.red), bs = Integer.toString(ClientState.blue);
        g.pose().pushPose();
        g.pose().translate(x + 60 - font.width(rs) * 1.6F / 2F, y + 3.5F, 0);
        g.pose().scale(1.6F, 1.6F, 1F);
        g.drawString(font, rs, 0, 0, 0xFFFFFF, true);
        g.pose().popPose();
        g.pose().pushPose();
        g.pose().translate(x + bw - 60 - font.width(bs) * 1.6F / 2F, y + 3.5F, 0);
        g.pose().scale(1.6F, 1.6F, 1F);
        g.drawString(font, bs, 0, 0, 0xFFFFFF, true);
        g.pose().popPose();
        int t = Math.max(0, ClientState.timeLeft) / 20;
        String clock = String.format("%02d:%02d", t / 60, t % 60);
        int clockColor = t < 30 && ClientState.state == 2 && (ClientState.clientTicks / 10) % 2 == 0 ? 0xFF5252 : 0xFFFFFF;
        g.drawString(font, clock, w / 2 - font.width(clock) / 2, y + 3, clockColor, true);
        String st = ClientState.state < STATES.length ? STATES[ClientState.state] : "";
        if (!st.isEmpty()) g.drawString(font, st, w / 2 - font.width(st) / 2, y + 13, 0xFFD54F, false);
        else {
            String live = "● CANLI";
            g.drawString(font, live, w / 2 - font.width(live) / 2, y + 13, (ClientState.clientTicks / 15) % 2 == 0 ? 0xFF5252 : 0xB71C1C, false);
        }
    }

    private static void goalBanner(GuiGraphics g, Font font, int w, int h, float partial) {
        Team t = Team.byId(ClientState.goalTeam);
        float age = 90 - ClientState.goalTicks + partial;
        float in = Mth.clamp(age / 8F, 0F, 1F);
        float out = Mth.clamp(ClientState.goalTicks / 10F, 0F, 1F);
        float a = Math.min(in, out);
        int band = (int) (40 * a);
        int cy = h / 2 - 40;
        int col = t.color;
        g.fill(0, cy - band, w, cy + band, ((int) (0xC0 * a) << 24) | (col & 0xFFFFFF));
        g.fillGradient(0, cy - band, w, cy, ((int) (0x50 * a) << 24) | 0xFFFFFF, 0x00FFFFFF);
        if (a > 0.3F) {
            float pulse = 1F + Mth.sin(age * 0.5F) * 0.06F;
            int o = Math.min(14, (int) (age / 3));
            String gol = "G" + "O".repeat(Math.max(2, o)) + "L!";
            big(g, font, gol, w / 2, cy - 22, 4.2F * pulse * in, 0xFFFFFF);
            String sub = ClientState.goalScorer + (ClientState.goalOwn ? " (kendi kalesine)" : "") + "  —  " + Team.RED.abbr + " " + ClientState.red + " : " + ClientState.blue + " " + Team.BLUE.abbr;
            centered(g, font, sub, w / 2, cy + 22, 0xFFFFFF);
        }
    }

    private static void chargeBar(GuiGraphics g, int w, int h) {
        float c = ClientState.chargeTicks / (float) ClientEvents.MAX_CHARGE;
        int bw = 90, x = w / 2 - bw / 2, y = h / 2 + 18;
        g.fill(x - 1, y - 1, x + bw + 1, y + 6, 0xC0000000);
        int fillW = (int) (bw * c);
        int r = (int) (80 + 175 * c), gr = (int) (220 - 160 * c);
        g.fillGradient(x, y, x + fillW, y + 5, 0xFF000000 | (r << 16) | (gr << 8) | 40, 0xFF000000 | ((r * 3 / 4) << 16) | ((gr * 3 / 4) << 8) | 20);
        if (c >= 1F && (ClientState.clientTicks / 3) % 2 == 0) g.fill(x, y, x + bw, y + 5, 0x60FFFFFF);
    }

    private static void hints(GuiGraphics g, Font font, int w, int h, boolean active) {
        boolean near = ClientState.nearestBallDist < 14;
        if (!near && !active) return;
        int y = h - 62;
        int x = 4;
        long now = ClientState.clientTicks;
        String sup = ClientState.favSuper.title;
        int left = (int) Math.max(0, ClientState.superCooldownEnd - now);
        String supState = left > 0 ? (left / 20 + 1) + " sn" : "HAZIR";
        line(g, font, x, y, Keys.name(Keys.SHOOT) + " Şut (basılı tut)  " + Keys.name(Keys.PASS) + " Pas  " + Keys.name(Keys.TACKLE) + " Top Çal", 0xE0E0E0);
        line(g, font, x, y + 10, Keys.name(Keys.SKILL) + " " + ClientState.favSkill.title + "  " + Keys.name(Keys.MOVES) + " Tüm Hareketler  " + Keys.name(Keys.MATCH) + " Maç", 0xE0E0E0);
        line(g, font, x, y + 20, Keys.name(Keys.SUPER) + " ✦ " + sup + " [" + supState + "]", left > 0 ? 0xB0B0B0 : 0xFF80C0);
        if (left > 0) {
            float frac = 1F - left / (float) Math.max(1, ClientState.superCooldownTotal);
            g.fill(x, y + 30, x + 100, y + 32, 0x80000000);
            g.fill(x, y + 30, x + (int) (100 * frac), y + 32, 0xFFFF4081);
        }
        if (ClientState.nearestBall != null && ClientState.nearestBall.getControllerId() == Minecraft.getInstance().player.getId()) {
            String s = "⚽ Top sende!  (Shift: topu ayağının altına al)";
            g.drawString(font, s, w / 2 - font.width(s) / 2, h - 72, 0x80FF80, true);
        }
    }

    private static void line(GuiGraphics g, Font font, int x, int y, String s, int c) {
        g.fill(x - 2, y - 1, x + font.width(s) + 2, y + 9, 0x70000000);
        g.drawString(font, s, x, y, c, false);
    }

    private static void centered(GuiGraphics g, Font font, String s, int cx, int y, int c) { g.drawString(font, s, cx - font.width(s) / 2, y, c, true); }

    private static void big(GuiGraphics g, Font font, String s, int cx, int y, float scale, int c) {
        if (scale <= 0.01F) return;
        g.pose().pushPose();
        g.pose().translate(cx, y, 0);
        g.pose().scale(scale, scale, 1F);
        g.drawString(font, s, -font.width(s) / 2, 0, c, true);
        g.pose().popPose();
    }

}
