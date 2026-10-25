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

    public record Bubble(net.minecraft.network.chat.Component text, long start) {}

    public record Fall(int kind, long start, int ticks) {}

    public static final java.util.Map<Integer, Bubble> bubbles = new java.util.HashMap<>();
    public static final java.util.Map<Integer, Fall> stumbles = new java.util.HashMap<>();

    /** 0..1 progress of a stumble animation for an entity, or -1 */
    public static float fall(int entityId, float partial) {
        Fall f = stumbles.get(entityId);
        var lvl = net.minecraft.client.Minecraft.getInstance().level;
        if (f == null || lvl == null) return -1F;
        float t = (lvl.getGameTime() - f.start() + partial) / f.ticks();
        if (t >= 1F) {
            stumbles.remove(entityId);
            return -1F;
        }
        return Math.max(0F, t);
    }

    public static int fallKind(int entityId) {
        Fall f = stumbles.get(entityId);
        return f == null ? 0 : f.kind();
    }

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
