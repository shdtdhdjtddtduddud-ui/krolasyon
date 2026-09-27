package com.krolasyon.bosses.morph;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Shared constants of the transformations (both sides). */
public final class Aigoar {
    private Aigoar() {}

    public static final int REND = 0, MAELSTROM = 1, GEYSER = 2, BEAM = 3, TSUNAMI = 4;
    public static final int ABILITIES = 5;

    public static final byte ANIM_TRANSFORM = 10, ANIM_ATTACK_R = 11, ANIM_ATTACK_L = 12, ANIM_DOUBLE_JUMP = 13;
    public static final int TRANSFORM_TICKS = 48;
    /** ticks into the transform when the cocoon bursts */
    public static final int TRANSFORM_BURST = 21;

    /** Aigoar pressure beam channel window in ticks from ability start */
    public static final int BEAM_START = 16, BEAM_END = 56;
    public static final double BEAM_RANGE = 26.0;

    /** entity id -> form of transformed players, as known by the client (never touched on a dedicated server) */
    public static final Map<Integer, Integer> CLIENT_FORM = new ConcurrentHashMap<>();
}
