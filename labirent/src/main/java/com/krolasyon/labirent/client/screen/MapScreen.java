package com.krolasyon.labirent.client.screen;

import com.krolasyon.labirent.client.ClientState;
import com.krolasyon.labirent.client.MapRenderer;
import com.krolasyon.labirent.maze.MazeLayout;
import com.krolasyon.labirent.net.Net;
import com.krolasyon.labirent.story.StoryDirector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** Full map of the maze: drag to pan, scroll to zoom. At the Map Room table it also lists the code words. */
public class MapScreen extends Screen {
    private final boolean mapRoom;
    private double cx, cz;
    private float zoom = -1;

    public MapScreen(boolean mapRoom) {
        super(Component.translatable("screen.labirent.map"));
        this.mapRoom = mapRoom;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && !mapRoom) {
            cx = mc.player.getX();
            cz = mc.player.getZ();
        }
    }

    @Override
    protected void init() {
        if (zoom < 0) zoom = Math.max(0.5F, (Math.min(width, height) - 60) / (float) MazeLayout.SIZE);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        int pad = 24;
        int x0 = pad, y0 = pad, w = width - pad * 2 - (mapRoom ? 140 : 0), h = height - pad * 2;
        g.fill(x0 - 3, y0 - 3, x0 + w + 3, y0 + h + 3, 0xFF2A2418);
        g.fill(x0 - 2, y0 - 2, x0 + w + 2, y0 + h + 2, 0xFF8C7A55);
        g.enableScissor(x0, y0, x0 + w, y0 + h);
        MapRenderer.draw(g, x0, y0, w, h, cx, cz, zoom, true);
        g.disableScissor();
        g.drawCenteredString(font, title, width / 2, 8, 0xFFFFE0A0);
        Net.State st = ClientState.state;
        String pct = st == null ? "?" : st.mapPercent() + "%";
        g.drawString(font, Component.translatable("screen.labirent.map.info", pct), x0, height - pad + 6, 0xFFCCCCCC, true);
        legend(g, x0 + w - 150, height - pad + 6);
        if (mapRoom) codeWords(g, x0 + w + 12, y0);
        super.render(g, mx, my, partial);
    }

    private void legend(GuiGraphics g, int x, int y) {
        int[] cols = {MapRenderer.PATH, MapRenderer.WALL, MapRenderer.PATH_OLD, MapRenderer.DOOR_CLOSED};
        String[] keys = {"legend.labirent.path", "legend.labirent.wall", "legend.labirent.old", "legend.labirent.door"};
        for (int k = 0; k < cols.length; k++) {
            int lx = x + (k % 2) * 80, ly = y + (k / 2) * 10 - 4;
            g.fill(lx, ly, lx + 7, ly + 7, cols[k]);
            g.drawString(font, Component.translatable(keys[k]), lx + 10, ly, 0xFFBBBBBB, false);
        }
    }

    private void codeWords(GuiGraphics g, int x, int y) {
        Net.State st = ClientState.state;
        g.drawString(font, Component.translatable("screen.labirent.map.sections"), x, y, 0xFFFFD060, true);
        y += 14;
        for (int s = 0; s < MazeLayout.SECTORS; s++) {
            boolean stone = st != null && (st.stones() & (1 << s)) != 0;
            boolean word = st != null && (st.words() & (1 << s)) != 0;
            boolean moved = st != null && (st.shiftedMask() & (1 << s)) != 0;
            String w = word ? StoryDirector.WORDS[s] : stone ? "????" : "-";
            g.drawString(font, (s + 1) + ". " + w, x, y, word ? 0xFF60E8FF : stone ? 0xFFFFD040 : 0xFF888888, true);
            if (moved) g.drawString(font, "↻", x + 120, y, 0xFFFF8060, true);
            y += 11;
        }
        y += 6;
        for (var line : font.split(Component.translatable("screen.labirent.map.hint"), 130)) {
            g.drawString(font, line, x, y, 0xFFAAAAAA, false);
            y += 9;
        }
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        cx -= dx / zoom;
        cz -= dy / zoom;
        cx = Mth.clamp(cx, MazeLayout.ORIGIN, MazeLayout.ORIGIN + MazeLayout.SIZE);
        cz = Mth.clamp(cz, MazeLayout.ORIGIN, MazeLayout.ORIGIN + MazeLayout.SIZE);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        zoom = Mth.clamp(zoom * (delta > 0 ? 1.25F : 0.8F), 0.4F, 12F);
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (com.krolasyon.labirent.client.Keys.MAP.matches(key, scan)) {
            onClose();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
