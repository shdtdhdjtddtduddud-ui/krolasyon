package com.krolasyon.labirent.client;

import com.krolasyon.labirent.maze.Dims;
import com.krolasyon.labirent.net.Net;
import com.krolasyon.labirent.registry.ModEffects;
import com.krolasyon.labirent.story.Objective;
import com.krolasyon.labirent.team.Role;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.ForgeGui;

/** Minimap, day / door / Box clock, the current chapter's objectives and screen effects. */
public final class Hud {
    private Hud() {}

    public static void render(ForgeGui gui, GuiGraphics g, float partial, int sw, int sh) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui) {
            cinematic(g, sw, sh, partial);
            return;
        }
        effects(g, sw, sh, mc, partial);
        if (mc.level.dimension() == Dims.MAZE && ClientState.minimap && mc.screen == null) {
            int size = 112, x0 = sw - size - 8, y0 = 8;
            g.fill(x0 - 2, y0 - 2, x0 + size + 2, y0 + size + 2, 0xFF2A2418);
            g.fill(x0 - 1, y0 - 1, x0 + size + 1, y0 + size + 1, 0xFF8C7A55);
            g.enableScissor(x0, y0, x0 + size, y0 + size);
            MapRenderer.draw(g, x0, y0, size, size, mc.player.getX(), mc.player.getZ(), 1.0F, false);
            g.disableScissor();
            Font f = mc.font;
            g.drawString(f, "K", x0 + size / 2 - 2, y0 + 2, 0xFFFFFFFF, true);
            int y = y0 + size + 5;
            for (Component line : infoLines(mc)) {
                int w = f.width(line);
                g.drawString(f, line, x0 + size - w, y, 0xFFFFFFFF, true);
                y += 10;
            }
            objectives(g, f, sw, y + 4);
        }
        cinematic(g, sw, sh, partial);
    }

    private static java.util.List<Component> infoLines(Minecraft mc) {
        java.util.List<Component> out = new java.util.ArrayList<>();
        long time = mc.level.getDayTime();
        int day = (int) (time / 24000L) + 1;
        int phase = (int) (time % 24000L);
        Net.State st = ClientState.state;
        out.add(Component.translatable("hud.labirent.day", day));
        if (ClientState.sunGone()) out.add(Component.translatable("hud.labirent.no_doors"));
        else if (st != null && st.doorStep() >= 11) {
            int toOpen = phase >= 23300 ? 24000 - phase + 23300 : 23300 - phase;
            out.add(Component.translatable("hud.labirent.doors_closed", clock(toOpen)));
        } else if (st != null && st.doorStep() > 0) out.add(Component.translatable("hud.labirent.doors_moving"));
        else {
            int toClose = phase < 12500 ? 12500 - phase : 24000 - phase + 12500;
            out.add(Component.translatable(toClose < 1500 ? "hud.labirent.doors_soon" : "hud.labirent.doors_open", clock(toClose)));
        }
        if (st != null) {
            if ((st.events() & com.krolasyon.labirent.maze.MazeState.EV_TERESA) != 0) out.add(Component.translatable("hud.labirent.box_never"));
            else if (st.lastArrivalDay() >= 0) {
                int next = st.lastArrivalDay() + st.monthDays() - (day - 1);
                out.add(next <= 0 ? Component.translatable("hud.labirent.box_today") : Component.translatable("hud.labirent.box_in", next));
            }
            out.add(Component.translatable("hud.labirent.mapped", st.mapPercent()));
            Role r = Role.SERBEST;
            boolean captain = st.captain() != null && st.captain().equals(mc.player.getUUID());
            for (Net.RosterEntry e : st.roster()) if (e.id().equals(mc.player.getUUID())) r = Role.byId(e.role());
            Component rc = r.title();
            out.add(captain ? Component.translatable("hud.labirent.captain").append(" • ").append(rc) : rc);
        }
        return out;
    }

    private static String clock(int ticks) {
        int sec = ticks / 20;
        return String.format("%d:%02d", sec / 60, sec % 60);
    }

    private static void objectives(GuiGraphics g, Font f, int sw, int y) {
        Net.State st = ClientState.state;
        if (st == null || st.chapter() >= 8) return;
        Component title = Component.translatable("chapter.labirent.num", st.chapter() + 1).append(": ").append(Component.translatable("chapter.labirent." + st.chapter()));
        int x = sw - 8;
        g.drawString(f, title, x - f.width(title), y, 0xFFFFC040, true);
        y += 11;
        for (Objective o : Objective.values()) {
            if (o.chapter != st.chapter()) continue;
            int p = o.ordinal() < st.progress().length ? st.progress()[o.ordinal()] : 0;
            boolean done = p >= o.target;
            Component line = Component.literal(done ? "✔ " : "• ").append(o.text())
                    .append(o.target > 1 ? Component.literal(" " + Math.min(p, o.target) + "/" + o.target) : Component.empty());
            g.drawString(f, line, x - f.width(line), y, done ? 0xFF70E070 : 0xFFDDDDDD, true);
            y += 10;
        }
    }

    private static void effects(GuiGraphics g, int sw, int sh, Minecraft mc, float partial) {
        boolean venom = mc.player.hasEffect(ModEffects.VENOM.get());
        boolean changing = mc.player.hasEffect(ModEffects.CHANGING.get());
        if (venom) {
            float pulse = 0.35F + 0.25F * Mth.sin((mc.player.tickCount + partial) * 0.12F);
            vignette(g, sw, sh, 0x2E6B10, pulse);
        }
        if (changing) {
            float pulse = 0.3F + 0.3F * Mth.sin((mc.player.tickCount + partial) * 0.3F);
            vignette(g, sw, sh, 0x9FC8FF, pulse);
            if (mc.player.tickCount % 50 < 3) g.fill(0, 0, sw, sh, 0x60FFFFFF);
        }
    }

    private static void vignette(GuiGraphics g, int sw, int sh, int rgb, float alpha) {
        int bands = 12;
        for (int k = 0; k < bands; k++) {
            float a = alpha * (1F - k / (float) bands) * 0.55F;
            int col = ((int) (a * 255) << 24) | rgb;
            int t = (int) (k * Math.min(sw, sh) * 0.018F);
            int s = Math.max(2, (int) (Math.min(sw, sh) * 0.018F));
            g.fill(0, t, sw, t + s, col);
            g.fill(0, sh - t - s, sw, sh - t, col);
            g.fill(t, 0, t + s, sh, col);
            g.fill(sw - t - s, 0, sw - t, sh, col);
        }
    }

    private static void cinematic(GuiGraphics g, int sw, int sh, float partial) {
        if (ClientState.cinematic < 0) return;
        float t = ClientState.cinematicTicks + partial;
        Font f = Minecraft.getInstance().font;
        if (ClientState.cinematic == 1) {
            // escape: white flash fading into the open sky
            float a = t < 20 ? t / 20F : Math.max(0F, 1F - (t - 20) / 100F);
            g.fill(0, 0, sw, sh, ((int) (a * 255) << 24) | 0xFFFFFF);
            if (t > 30 && t < 400) {
                float ta = Math.min(1F, (t - 30) / 40F) * Math.min(1F, (400 - t) / 40F);
                int col = ((int) (Math.max(0.05F, ta) * 255) << 24) | 0xFFFFFF;
                Component c = Component.translatable("cinematic.labirent.wicked_good");
                g.drawCenteredString(f, c, sw / 2, sh - 40, col);
            }
        } else if (ClientState.cinematic == 2) {
            float a = t < 10 ? t / 10F : Math.max(0F, 1F - (t - 10) / 60F);
            g.fill(0, 0, sw, sh, ((int) (a * 200) << 24));
        }
    }
}
