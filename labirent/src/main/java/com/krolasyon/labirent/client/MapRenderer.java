package com.krolasyon.labirent.client;

import com.krolasyon.labirent.maze.Landmarks;
import com.krolasyon.labirent.maze.MazeLayout;
import com.krolasyon.labirent.net.Net;
import com.krolasyon.labirent.team.Role;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

/** Draws the explored maze (shared by the minimap and the full map screen). */
public final class MapRenderer {
    private MapRenderer() {}

    public static final int UNKNOWN = 0xFF0E1114;
    public static final int PATH = 0xFFC9B48E, PATH_OLD = 0xFF857A63;
    public static final int WALL = 0xFF3F5A35, WALL_OLD = 0xFF2B3527;
    public static final int GLADE = 0xFF5FA040;
    public static final int DOOR_OPEN = 0xFF7BE07B, DOOR_CLOSED = 0xFFD03A30;

    /**
     * @param cx,cz world block the view is centred on
     * @param scale pixels per block
     */
    public static void draw(GuiGraphics g, int x0, int y0, int w, int h, double cx, double cz, float scale, boolean full) {
        byte[] known = ClientState.known;
        Net.State st = ClientState.state;
        boolean doorsClosed = st != null && st.doorStep() >= 11;
        g.fill(x0, y0, x0 + w, y0 + h, UNKNOWN);
        double halfW = w / 2.0 / scale, halfH = h / 2.0 / scale;
        int iMin = Math.max(0, MazeLayout.unitOf((int) Math.max(MazeLayout.ORIGIN, Math.floor(cx - halfW))) - 1);
        int iMax = Math.min(MazeLayout.G - 1, unitClamp(cx + halfW) + 1);
        int jMin = Math.max(0, MazeLayout.unitOf((int) Math.max(MazeLayout.ORIGIN, Math.floor(cz - halfH))) - 1);
        int jMax = Math.min(MazeLayout.G - 1, unitClamp(cz + halfH) + 1);
        if (iMin < 0) iMin = 0;
        if (jMin < 0) jMin = 0;
        for (int i = iMin; i <= iMax; i++) {
            float px0 = (float) (x0 + w / 2.0 + (MazeLayout.start(i) - cx) * scale);
            float px1 = px0 + MazeLayout.width(i) * scale;
            if (px1 < x0 || px0 > x0 + w) continue;
            for (int j = jMin; j <= jMax; j++) {
                float py0 = (float) (y0 + h / 2.0 + (MazeLayout.start(j) - cz) * scale);
                float py1 = py0 + MazeLayout.width(j) * scale;
                if (py1 < y0 || py0 > y0 + h) continue;
                int col;
                int door = MazeLayout.door(i, j);
                if (door >= 0) col = doorsClosed ? DOOR_CLOSED : DOOR_OPEN;
                else if (MazeLayout.inGladeInterior(i, j)) col = GLADE;
                else {
                    byte b = known[MazeLayout.idx(i, j)];
                    if ((b & 1) == 0) continue;
                    boolean wall = (b & 2) != 0, fresh = (b & 4) != 0;
                    col = wall ? (fresh ? WALL : WALL_OLD) : (fresh ? PATH : PATH_OLD);
                }
                fill(g, px0, py0, px1, py1, col, x0, y0, w, h);
            }
        }
        overlay(g, x0, y0, w, h, cx, cz, scale, full);
    }

    /** landmarks, sector numbers, people and the player arrow on top of the terrain */
    public static void overlay(GuiGraphics g, int x0, int y0, int w, int h, double cx, double cz, float scale, boolean full) {
        Net.State st = ClientState.state;
        // landmarks
        marker(g, 0, 0, cx, cz, x0, y0, w, h, scale, 0xFF8B5A2B, 3);   // the Box
        marker(g, -19, -19, cx, cz, x0, y0, w, h, scale, 0xFFE0E0E0, 2); // map room
        if (st != null) {
            for (int s = 0; s < MazeLayout.SECTORS; s++) {
                if ((st.stones() & (1 << s)) == 0) continue;
                BlockPos p = Landmarks.codeStonePos(s);
                marker(g, p.getX(), p.getZ(), cx, cz, x0, y0, w, h, scale, (st.words() & (1 << s)) != 0 ? 0xFF40E0FF : 0xFFFFD040, 3);
            }
            if (st.chapter() >= 7) marker(g, Landmarks.HOLE_X0, 0, cx, cz, x0, y0, w, h, scale, 0xFFA040FF, 4);
        }
        // sector numbers on the full map
        Font font = Minecraft.getInstance().font;
        if (full) {
            for (int s = 0; s < MazeLayout.SECTORS; s++) {
                double a = s * Math.PI * 2 / MazeLayout.SECTORS - Math.PI / 2;
                double bx = Math.cos(a) * 95, bz = Math.sin(a) * 95;
                int px = (int) (x0 + w / 2.0 + (bx - cx) * scale), py = (int) (y0 + h / 2.0 + (bz - cz) * scale);
                if (px < x0 || py < y0 || px > x0 + w || py > y0 + h) continue;
                boolean moved = st != null && (st.shiftedMask() & (1 << s)) != 0;
                String label = String.valueOf(s + 1);
                g.drawString(font, label, px - font.width(label) / 2, py - 4, moved ? 0xFFFF8060 : 0xFFFFFFFF, true);
            }
        }
        // people
        Minecraft mc = Minecraft.getInstance();
        if (st != null) {
            for (Net.RosterEntry e : st.roster()) {
                if (Float.isNaN(e.x()) || mc.player != null && e.id().equals(mc.player.getUUID())) continue;
                int col = e.npc() ? (0xFF000000 | Role.byId(e.role()).rgb) : 0xFF40C0FF;
                if (e.stung() && (mc.level != null && mc.level.getGameTime() / 5 % 2 == 0)) col = 0xFF30A030;
                int sz = e.npc() ? 1 : 2;
                float px = (float) (x0 + w / 2.0 + (e.x() - cx) * scale), py = (float) (y0 + h / 2.0 + (e.z() - cz) * scale);
                if (px < x0 || py < y0 || px > x0 + w || py > y0 + h) continue;
                g.fill((int) px - sz, (int) py - sz, (int) px + sz + 1, (int) py + sz + 1, 0xFF000000);
                g.fill((int) px - sz + 1, (int) py - sz + 1, (int) px + sz, (int) py + sz, col);
                if (full && !e.npc()) g.drawString(font, e.name(), (int) px + 4, (int) py - 4, 0xFFFFFFFF, true);
            }
        }
        if (mc.player != null) {
            float px = (float) (x0 + w / 2.0 + (mc.player.getX() - cx) * scale), py = (float) (y0 + h / 2.0 + (mc.player.getZ() - cz) * scale);
            if (px >= x0 && py >= y0 && px <= x0 + w && py <= y0 + h) arrow(g, px, py, mc.player.getYRot());
        }
    }

    private static int unitClamp(double b) {
        int u = MazeLayout.unitOf((int) Math.min(MazeLayout.ORIGIN + MazeLayout.SIZE - 1, Math.floor(b)));
        return u < 0 ? MazeLayout.G - 1 : u;
    }

    private static void fill(GuiGraphics g, float a, float b, float c, float d, int col, int x0, int y0, int w, int h) {
        int ia = Math.max(x0, Mth.floor(a)), ib = Math.max(y0, Mth.floor(b));
        int ic = Math.min(x0 + w, Mth.ceil(c)), id = Math.min(y0 + h, Mth.ceil(d));
        if (ic > ia && id > ib) g.fill(ia, ib, ic, id, col);
    }

    private static void marker(GuiGraphics g, double bx, double bz, double cx, double cz, int x0, int y0, int w, int h, float scale, int col, int r) {
        int px = (int) (x0 + w / 2.0 + (bx - cx) * scale), py = (int) (y0 + h / 2.0 + (bz - cz) * scale);
        if (px < x0 + r || py < y0 + r || px > x0 + w - r || py > y0 + h - r) return;
        g.fill(px - r - 1, py - r - 1, px + r + 1, py + r + 1, 0xFF000000);
        g.fill(px - r, py - r, px + r, py + r, col);
    }

    /** a little white arrow showing where the player looks */
    private static void arrow(GuiGraphics g, float px, float py, float yaw) {
        double a = Math.toRadians(yaw + 90);
        for (int k = 0; k < 7; k++) {
            double d = 4 - k;
            int x = (int) (px + Math.cos(a) * d), y = (int) (py + Math.sin(a) * d);
            int s = k < 2 ? 0 : 1;
            g.fill(x - s - 1, y - s - 1, x + s + 2, y + s + 2, 0xFF000000);
        }
        for (int k = 0; k < 7; k++) {
            double d = 4 - k;
            int x = (int) (px + Math.cos(a) * d), y = (int) (py + Math.sin(a) * d);
            int s = k < 2 ? 0 : 1;
            g.fill(x - s, y - s, x + s + 1, y + s + 1, 0xFFFFFFFF);
        }
    }
}
