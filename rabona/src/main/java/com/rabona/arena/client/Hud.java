package com.rabona.arena.client;

import com.rabona.arena.entity.BallEntity;
import com.rabona.arena.entity.FootballerEntity;
import com.rabona.arena.game.Move;
import com.rabona.arena.game.Pitch;
import com.rabona.arena.game.Team;
import com.rabona.arena.net.S2C;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.overlay.ForgeGui;

import java.util.List;

/** Oyun ici gostergeler. */
public final class Hud {
    private static final String[] PHASES = {"", "phase.rabonaarena.kickoff", "", "phase.rabonaarena.goal", "phase.rabonaarena.halftime", "phase.rabonaarena.ended"};

    private Hud() {}

    public static void render(ForgeGui gui, GuiGraphics g, float partial, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.options.hideGui) return;
        Font font = mc.font;
        S2C.MatchState m = ClientState.match;
        BallEntity myBall = null, nearBall = null;
        for (BallEntity b : mc.level.getEntitiesOfClass(BallEntity.class, p.getBoundingBox().inflate(16))) {
            if (b.getControllerId() == p.getId()) myBall = b;
            if (nearBall == null || b.distanceToSqr(p) < nearBall.distanceToSqr(p)) nearBall = b;
        }
        boolean active = ClientState.inMatchTeam() || nearBall != null;
        boolean matchOn = m != null && m.phase() != 0;

        if (matchOn) scoreboard(g, font, m, w);
        if (active) radar(g, mc, p, w, h, matchOn);
        if (active && ClientState.stats != null) bars(g, font, w, h);
        if (active) slots(g, font, w, h);
        if (ClientInput.charging) power(g, font, w, h);
        if (myBall != null) hint(g, font, w, h, myBall.isHeld());
        feed(g, font, w, matchOn);
        if (ClientState.bannerTicks > 0 && ClientState.banner != null) banner(g, font, w, h, partial);
    }

    // ================================================================ skor tabelasi
    private static void scoreboard(GuiGraphics g, Font f, S2C.MatchState m, int w) {
        int cw = 236, x = w / 2 - cw / 2, y = 4;
        g.fill(x - 1, y - 1, x + cw + 1, y + 27, 0x90000000);
        g.fill(x, y, x + cw, y + 26, 0xE0141A22);
        // takim bloklari
        g.fill(x, y, x + 78, y + 26, 0xFFB71C1C);
        g.fill(x, y, x + 78, y + 3, 0xFFFF5252);
        g.fill(x + cw - 78, y, x + cw, y + 26, 0xFF0D47A1);
        g.fill(x + cw - 78, y, x + cw, y + 3, 0xFF448AFF);
        g.drawString(f, Component.translatable("team.rabonaarena.kirmizi"), x + 6, y + 9, 0xFFFFFFFF, true);
        Component blue = Component.translatable("team.rabonaarena.mavi");
        g.drawString(f, blue, x + cw - 6 - f.width(blue), y + 9, 0xFFFFFFFF, true);
        // skorlar (buyuk)
        g.pose().pushPose();
        g.pose().translate(x + 96, y + 5, 0);
        g.pose().scale(2f, 2f, 1f);
        String sr = String.valueOf(m.scoreRed());
        g.drawString(f, sr, -f.width(sr) / 2, 0, 0xFFFFFFFF, true);
        g.pose().popPose();
        g.pose().pushPose();
        g.pose().translate(x + cw - 96, y + 5, 0);
        g.pose().scale(2f, 2f, 1f);
        String sb = String.valueOf(m.scoreBlue());
        g.drawString(f, sb, -f.width(sb) / 2, 0, 0xFFFFFFFF, true);
        g.pose().popPose();
        // sure
        int secs = Math.max(0, m.timeLeft()) / 20;
        String time = String.format("%02d:%02d", secs / 60, secs % 60);
        boolean low = secs < 30 && m.phase() == 2;
        int tc = low && (ClientState.clientTicks / 10) % 2 == 0 ? 0xFFFF5252 : 0xFFFFEB3B;
        g.drawString(f, time, w / 2 - f.width(time) / 2, y + 4, tc, true);
        String half = m.half() + ". " + Component.translatable("hud.rabonaarena.half").getString();
        g.drawString(f, half, w / 2 - f.width(half) / 2, y + 15, 0xFFB0BEC5, false);
        // ilerleme cizgisi
        if (m.totalTime() > 0) {
            float prog = 1 - (float) m.timeLeft() / m.totalTime();
            g.fill(x, y + 26, x + (int) (cw * prog), y + 27, 0xFFFFC107);
        }
        String key = PHASES[Mth.clamp(m.phase(), 0, PHASES.length - 1)];
        if (!key.isEmpty()) {
            Component c = Component.translatable(key);
            int pw = f.width(c) + 12;
            g.fill(w / 2 - pw / 2, y + 29, w / 2 + pw / 2, y + 41, 0xC0000000);
            g.drawString(f, c, w / 2 - f.width(c) / 2, y + 31, 0xFFFFD54F, true);
        } else if (m.restrictTeam() > 0) {
            Component c = Component.translatable("hud.rabonaarena.restart", Team.byId(m.restrictTeam()).displayName());
            int pw = f.width(c) + 12;
            g.fill(w / 2 - pw / 2, y + 29, w / 2 + pw / 2, y + 41, 0xA0000000);
            g.drawString(f, c, w / 2 - f.width(c) / 2, y + 31, 0xFFFFFFFF, true);
        }
    }

    // ================================================================ radar
    private static void radar(GuiGraphics g, Minecraft mc, LocalPlayer p, int w, int h, boolean matchOn) {
        Pitch pitch = ClientState.pitch;
        if (pitch == null || !pitch.inside(p.position(), 25)) return;
        int rw = 112, rh = 72, x = 6;
        int y = (matchOn && w / 2 - 120 < x + rw + 6) ? 48 : 6;
        float sx = rw / (2f * (Pitch.HALF_LEN + 1)), sy = rh / (2f * (Pitch.HALF_WID + 1));
        g.fill(x - 2, y - 2, x + rw + 2, y + rh + 2, 0xB0000000);
        g.fill(x, y, x + rw, y + rh, 0xD02E7D32);
        for (int i = 0; i < 7; i++) if (i % 2 == 0) g.fill(x + i * rw / 7, y, x + (i + 1) * rw / 7, y + rh, 0x2066BB6A);
        int line = 0x90FFFFFF;
        g.renderOutline(x + 1, y + 1, rw - 2, rh - 2, line);
        g.fill(x + rw / 2, y + 1, x + rw / 2 + 1, y + rh - 1, line);
        int bw = (int) (Pitch.BOX_DEPTH * sx), bh = (int) (Pitch.BOX_HALF * 2 * sy);
        g.renderOutline(x + 1, y + rh / 2 - bh / 2, bw, bh, line);
        g.renderOutline(x + rw - 1 - bw, y + rh / 2 - bh / 2, bw, bh, line);
        int gh = (int) (Pitch.GOAL_HALF_W * 2 * sy);
        g.fill(x - 2, y + rh / 2 - gh / 2, x, y + rh / 2 + gh / 2, 0xFFFFFFFF);
        g.fill(x + rw, y + rh / 2 - gh / 2, x + rw + 2, y + rh / 2 + gh / 2, 0xFFFFFFFF);
        AABB area = new AABB(pitch.world(-Pitch.HALF_LEN - 4, -Pitch.HALF_WID - 4, pitch.surfaceY() - 4),
                pitch.world(Pitch.HALF_LEN + 4, Pitch.HALF_WID + 4, pitch.surfaceY() + 20)).inflate(1);
        for (Entity e : mc.level.getEntities((Entity) null, area, en -> en instanceof Player || en instanceof FootballerEntity || en instanceof BallEntity)) {
            Vec3 pos = e.position();
            int px = x + rw / 2 + (int) (pitch.a(pos) * sx);
            int py = y + rh / 2 + (int) (pitch.b(pos) * sy);
            if (px < x - 2 || px > x + rw + 2 || py < y - 2 || py > y + rh + 2) continue;
            if (e instanceof BallEntity) {
                g.fill(px - 1, py - 1, px + 2, py + 2, 0xFF000000);
                g.fill(px, py, px + 1, py + 1, 0xFFFFFFFF);
                g.fill(px - 1, py, px + 2, py + 1, 0xFFFFFFFF);
                g.fill(px, py - 1, px + 1, py + 2, 0xFFFFFFFF);
                continue;
            }
            Team t = ClientState.teamOf(e);
            int c = t == Team.RED ? 0xFFFF3B30 : t == Team.BLUE ? 0xFF2F80FF : 0xFFBDBDBD;
            if (e == p) {
                g.fill(px - 2, py - 2, px + 3, py + 3, 0xFFFFFFFF);
            }
            g.fill(px - 1, py - 1, px + 2, py + 2, c);
            if (e instanceof FootballerEntity fb && fb.isKeeper()) g.fill(px, py, px + 1, py + 1, 0xFFFFEB3B);
        }
    }

    // ================================================================ dayaniklilik / enerji
    private static void bars(GuiGraphics g, Font f, int w, int h) {
        S2C.Stats s = ClientState.stats;
        int bw = 100, x = w - 130, y = h - 168;
        g.fill(x - 3, y - 4, x + bw + 3, y + 33, 0x90000000);
        g.drawString(f, Component.translatable("hud.rabonaarena.stamina"), x, y - 1, 0xFFB2FF59, true);
        bar(g, x, y + 8, bw, 5, s.stamina() / 100f, s.stamina() > 50 ? 0xFF76FF03 : s.stamina() > 20 ? 0xFFFFC400 : 0xFFFF3D00);
        boolean full = s.energy() >= 99.5f;
        int ec = full ? Mth.hsvToRgb((ClientState.clientTicks % 40) / 40f, 0.5f, 1f) | 0xFF000000 : 0xFFFFC107;
        Component en = full ? Component.translatable("hud.rabonaarena.energy_ready", Keys.ABILITY.getTranslatedKeyMessage())
                : Component.translatable("hud.rabonaarena.energy");
        g.drawString(f, en, x, y + 16, full ? ec : 0xFFFFE082, true);
        bar(g, x, y + 25, bw, 5, s.energy() / 100f, ec);
        // yetenek esikleri
        for (int mark : new int[]{50, 60, 70}) g.fill(x + bw * mark / 100, y + 24, x + bw * mark / 100 + 1, y + 31, 0x80FFFFFF);
    }

    private static void bar(GuiGraphics g, int x, int y, int w, int h, float v, int color) {
        v = Mth.clamp(v, 0, 1);
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF000000);
        g.fill(x, y, x + w, y + h, 0xFF263238);
        g.fill(x, y, x + (int) (w * v), y + h, color);
        g.fill(x, y, x + (int) (w * v), y + 1, 0x60FFFFFF);
    }

    // ================================================================ calim yuvalari
    private static void slots(GuiGraphics g, Font f, int w, int h) {
        int x = w - 132, y = h - 124;
        g.fill(x - 4, y - 4, w - 4, y + 86, 0x90000000);
        for (int i = 0; i < 4; i++) slotRow(g, f, x, y + i * 13, Keys.SKILLS[i].getTranslatedKeyMessage(), ClientState.slots[i]);
        slotRow(g, f, x, y + 54, Keys.ABILITY.getTranslatedKeyMessage(), ClientState.ability);
        Component st = Component.literal("").append(Keys.STYLE.getTranslatedKeyMessage()).append(": ").append(ClientState.shotStyle.title());
        g.drawString(f, st, x, y + 70, 0xFFEF9A9A, true);
    }

    private static void slotRow(GuiGraphics g, Font f, int x, int y, Component key, Move m) {
        int cd = ClientState.cooldown(m);
        g.fill(x, y, x + 12, y + 11, 0xFF37474F);
        g.drawString(f, key, x + 6 - f.width(key) / 2, y + 2, 0xFFFFFFFF, false);
        int color = cd > 0 ? 0xFF757575 : m.cat.color | 0xFF000000;
        g.drawString(f, m.title(), x + 16, y + 2, color, true);
        if (cd > 0 && m.cooldown > 0) {
            int bw = 108;
            g.fill(x + 16, y + 10, x + 16 + bw * cd / m.cooldown, y + 11, 0xFFFF7043);
        }
    }

    // ================================================================ sut gucu
    private static void power(GuiGraphics g, Font f, int w, int h) {
        int x = w / 2 + 16, y = h / 2 - 30, bh = 60, bw = 7;
        float c = ClientInput.charge;
        g.fill(x - 1, y - 1, x + bw + 1, y + bh + 1, 0xFF000000);
        g.fill(x, y, x + bw, y + bh, 0xFF263238);
        int okH = (int) (bh / 1.35f);
        g.fill(x, y, x + bw, y + bh - okH, 0x80B71C1C);
        int fillH = (int) (bh * Math.min(1.35f, c) / 1.35f);
        for (int i = 0; i < fillH; i++) {
            float t = (float) i / okH;
            int col = t < 0.6f ? 0xFF76FF03 : t < 0.9f ? 0xFFFFEA00 : t <= 1.0f ? 0xFFFF9100 : 0xFFFF1744;
            g.fill(x, y + bh - i - 1, x + bw, y + bh - i, col);
        }
        g.fill(x - 3, y + bh - okH, x + bw + 3, y + bh - okH + 1, 0xFFFFFFFF);
        Component st = ClientState.shotStyle.title();
        g.drawString(f, st, x + bw + 5, y + bh / 2 - 4, 0xFFFFFFFF, true);
        String pct = (int) (Math.min(c, 1.35f) * 100) + "%";
        g.drawString(f, pct, x + bw + 5, y + bh / 2 + 6, c > 1 ? 0xFFFF5252 : 0xFFB0BEC5, true);
    }

    // ================================================================ ipucu
    private static void hint(GuiGraphics g, Font f, int w, int h, boolean held) {
        Component c = held
                ? Component.translatable("hud.rabonaarena.hint_gk", Keys.SHOOT.getTranslatedKeyMessage(), Keys.PASS.getTranslatedKeyMessage())
                : Component.translatable("hud.rabonaarena.hint", Keys.SHOOT.getTranslatedKeyMessage(), Keys.PASS.getTranslatedKeyMessage(),
                Keys.LOB.getTranslatedKeyMessage(), Keys.SKILL1.getTranslatedKeyMessage(), Keys.ABILITY.getTranslatedKeyMessage());
        int tw = f.width(c);
        int y = h - 72;
        g.fill(w / 2 - tw / 2 - 5, y - 3, w / 2 + tw / 2 + 5, y + 11, 0x90000000);
        g.drawString(f, c, w / 2 - tw / 2, y, 0xFFE0F7FA, true);
    }

    // ================================================================ akis
    private static void feed(GuiGraphics g, Font f, int w, boolean matchOn) {
        List<ClientState.FeedLine> lines = ClientState.feed;
        int y = matchOn ? 50 : 6;
        for (int i = lines.size() - 1; i >= 0; i--) {
            ClientState.FeedLine l = lines.get(i);
            int age = ClientState.clientTicks - l.born();
            if (age > 140) continue;
            int alpha = age > 110 ? (int) (255 * (140 - age) / 30f) : 255;
            if (alpha < 8) continue;
            int tw = f.width(l.text());
            g.fill(w - tw - 12, y - 2, w - 4, y + 10, (alpha / 2) << 24);
            g.drawString(f, l.text(), w - tw - 8, y, 0xFFFFFF | (alpha << 24), true);
            y += 13;
        }
    }

    // ================================================================ buyuk yazi
    private static void banner(GuiGraphics g, Font f, int w, int h, float partial) {
        S2C.Banner b = ClientState.banner;
        int total = b.type() == 1 ? 100 : 200;
        float t = total - ClientState.bannerTicks + partial;
        float in = Mth.clamp(t / 8f, 0, 1);
        float out = Mth.clamp(ClientState.bannerTicks / 10f, 0, 1);
        float alpha = Math.min(in, out);
        int a = (int) (alpha * 255) << 24;
        if (alpha < 0.03f) return;
        int cy = h / 2 - 50;
        g.fill(0, cy - 8, w, cy + 62, (int) (alpha * 150) << 24);
        if (b.type() == 1) {
            String[] ex = b.extra().split("\\|", -1);
            int team = ex.length > 2 ? parse(ex[2]) : 1;
            int col = team == 1 ? 0xFF5252 : 0x448AFF;
            g.fill(0, cy - 8, w, cy - 6, a | col);
            g.fill(0, cy + 60, w, cy + 62, a | col);
            float bounce = 1 + 0.15f * Mth.sin(t * 0.5f) * Math.max(0, 1 - t / 40f);
            float sc = 4.2f * bounce * (0.6f + 0.4f * in);
            Component goal = Component.translatable("banner.rabonaarena.goal");
            g.pose().pushPose();
            g.pose().translate(w / 2f, cy + 16, 0);
            g.pose().scale(sc, sc, 1);
            int rainbow = Mth.hsvToRgb((t * 0.02f) % 1f, 0.35f, 1f);
            g.drawString(f, goal, -f.width(goal) / 2, -4, a | (t < 30 ? rainbow : 0xFFFFFF), true);
            g.pose().popPose();
            Component who = Component.literal(b.name()).withStyle(team == 1 ? net.minecraft.ChatFormatting.RED : net.minecraft.ChatFormatting.BLUE);
            g.pose().pushPose();
            g.pose().translate(w / 2f, cy + 36, 0);
            g.pose().scale(1.5f, 1.5f, 1);
            g.drawString(f, who, -f.width(who) / 2, 0, a | 0xFFFFFF, true);
            g.pose().popPose();
            StringBuilder sub = new StringBuilder();
            if (ex.length > 1 && !ex[1].isEmpty()) sub.append(Component.translatable("move.rabonaarena." + ex[1]).getString());
            if (!ex[0].isEmpty()) {
                if (sub.length() > 0) sub.append("  •  ");
                sub.append(Component.translatable("banner.rabonaarena.assist", ex[0]).getString());
            }
            String score = b.scoreRed() + " - " + b.scoreBlue();
            g.drawString(f, sub.toString(), w / 2 - f.width(sub.toString()) / 2, cy + 52, a | 0xFFE082, true);
            g.drawString(f, score, w / 2 - f.width(score) / 2, cy - 2 - 12, a | 0xFFFFFF, true);
        } else {
            Component title = Component.translatable("banner.rabonaarena.full_time");
            g.pose().pushPose();
            g.pose().translate(w / 2f, cy + 8, 0);
            g.pose().scale(3f, 3f, 1);
            g.drawString(f, title, -f.width(title) / 2, -4, a | 0xFFD54F, true);
            g.pose().popPose();
            String score = b.scoreRed() + " - " + b.scoreBlue();
            g.pose().pushPose();
            g.pose().translate(w / 2f, cy + 26, 0);
            g.pose().scale(2.5f, 2.5f, 1);
            g.drawString(f, score, -f.width(score) / 2, 0, a | 0xFFFFFF, true);
            g.pose().popPose();
            if (!b.name().isEmpty()) {
                Component mvp = Component.translatable("banner.rabonaarena.mvp", b.name());
                g.drawString(f, mvp, w / 2 - f.width(mvp) / 2, cy + 50, a | 0xE1BEE7, true);
            }
        }
    }

    private static int parse(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return 1;
        }
    }
}
