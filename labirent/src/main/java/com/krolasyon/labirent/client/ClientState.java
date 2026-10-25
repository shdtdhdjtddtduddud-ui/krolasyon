package com.krolasyon.labirent.client;

import com.krolasyon.labirent.maze.MazeLayout;
import com.krolasyon.labirent.net.Net;

import javax.annotation.Nullable;

/** Everything the client knows about the run: the shared map, story progress and the roster. */
public final class ClientState {
    private ClientState() {}

    public static byte[] known = new byte[MazeLayout.G * MazeLayout.G];
    @Nullable public static Net.State state;
    public static int mapVersion;

    // camera shake
    public static float shake;
    public static int shakeTicks;
    public static int shakeTotal;

    // cinematics
    public static int cinematic = -1;
    public static int cinematicTicks;

    public static boolean minimap = true;

    public static boolean sunGone() {
        return state != null && (state.events() & com.krolasyon.labirent.maze.MazeState.EV_SUN_GONE) != 0
                && (state.events() & com.krolasyon.labirent.maze.MazeState.EV_ESCAPED) == 0;
    }

    public static int chapter() { return state == null ? 0 : state.chapter(); }

    public static void tick() {
        if (shakeTicks > 0) shakeTicks--;
        if (cinematic >= 0 && ++cinematicTicks > 600) cinematic = -1;
    }
}
