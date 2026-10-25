package com.krolasyon.labirent.client;

import com.krolasyon.labirent.Labirent;
import com.krolasyon.labirent.maze.MazeLayout;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * The minimap's terrain is baked into a small texture and only redrawn when the player moves a block or the
 * map changes, instead of drawing hundreds of rectangles every frame.
 */
public final class MinimapTexture {
    public static final int SIZE = 112;
    private static DynamicTexture tex;
    private static ResourceLocation loc;
    private static int lastX = Integer.MIN_VALUE, lastZ, lastVersion = -1, lastDoor = -1;

    private MinimapTexture() {}

    public static ResourceLocation get(int px, int pz) {
        Minecraft mc = Minecraft.getInstance();
        if (tex == null) {
            tex = new DynamicTexture(SIZE, SIZE, false);
            loc = mc.getTextureManager().register(Labirent.MODID + "_minimap", tex);
        }
        int door = ClientState.state == null ? 0 : ClientState.state.doorStep();
        if (px != lastX || pz != lastZ || ClientState.mapVersion != lastVersion || door != lastDoor) {
            lastX = px;
            lastZ = pz;
            lastVersion = ClientState.mapVersion;
            lastDoor = door;
            redraw(px, pz, door >= 11);
        }
        return loc;
    }

    private static void redraw(int px, int pz, boolean doorsClosed) {
        NativeImage img = tex.getPixels();
        if (img == null) return;
        int half = SIZE / 2;
        for (int y = 0; y < SIZE; y++) {
            int j = MazeLayout.unitOf(pz - half + y);
            for (int x = 0; x < SIZE; x++) {
                int i = MazeLayout.unitOf(px - half + x);
                img.setPixelRGBA(x, y, abgr(color(i, j, doorsClosed)));
            }
        }
        tex.upload();
    }

    static int color(int i, int j, boolean doorsClosed) {
        if (i < 0 || j < 0) return MapRenderer.UNKNOWN;
        int door = MazeLayout.door(i, j);
        if (door >= 0) return doorsClosed ? MapRenderer.DOOR_CLOSED : MapRenderer.DOOR_OPEN;
        if (MazeLayout.inGladeInterior(i, j)) return MapRenderer.GLADE;
        byte b = ClientState.known[MazeLayout.idx(i, j)];
        if ((b & 1) == 0) return MapRenderer.UNKNOWN;
        boolean wall = (b & 2) != 0, fresh = (b & 4) != 0;
        return wall ? (fresh ? MapRenderer.WALL : MapRenderer.WALL_OLD) : (fresh ? MapRenderer.PATH : MapRenderer.PATH_OLD);
    }

    private static int abgr(int argb) {
        int a = argb >>> 24, r = (argb >> 16) & 255, g = (argb >> 8) & 255, b = argb & 255;
        return (a << 24) | (b << 16) | (g << 8) | r;
    }
}
