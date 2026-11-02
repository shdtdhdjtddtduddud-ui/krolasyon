package com.krolasyon.horror.story;

import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.IntConsumer;

/**
 * The story is a singleplayer experience, so the integrated server and the client share this JVM: the server
 * drives cutscenes, tapes and HUD on the client through these queues instead of custom packets (which differ
 * between Forge 1.20.1 and 1.21.1).
 */
public final class StoryBridge {
    private StoryBridge() {}

    public record Msg(String type, String s, int i, double x, double y, double z) {
        public static Msg of(String type) { return new Msg(type, "", 0, 0, 0, 0); }

        public static Msg of(String type, String s) { return new Msg(type, s, 0, 0, 0, 0); }

        public static Msg of(String type, String s, int i) { return new Msg(type, s, i, 0, 0, 0); }
    }

    public static final ConcurrentLinkedQueue<Msg> TO_CLIENT = new ConcurrentLinkedQueue<>();
    public static final ConcurrentLinkedQueue<Msg> TO_SERVER = new ConcurrentLinkedQueue<>();

    /** client hook to open a note screen (installed by the client setup) */
    public static IntConsumer OPEN_NOTE = i -> {};

    public static boolean available(ServerPlayer p) {
        return p.getServer() != null && !p.getServer().isDedicatedServer();
    }

    public static void send(ServerPlayer p, Msg m) {
        if (available(p)) TO_CLIENT.add(m);
    }
}
