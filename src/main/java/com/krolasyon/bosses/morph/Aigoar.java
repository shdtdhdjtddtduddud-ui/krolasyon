package com.krolasyon.bosses.morph;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Shared constants of the Aigoar transformation (both sides). */
public final class Aigoar {
    private Aigoar() {}

    public static final int REND = 0, MAELSTROM = 1, GEYSER = 2, BEAM = 3, TSUNAMI = 4;
    public static final int ABILITIES = 5;
    /** ability animation durations in ticks (match the generated animation lengths) */
    public static final int[] DURATION = {24, 36, 28, 64, 48};
    /** cooldowns in ticks */
    public static final int[] COOLDOWN = {120, 280, 200, 320, 440};

    public static final byte ANIM_TRANSFORM = 10, ANIM_ATTACK_R = 11, ANIM_ATTACK_L = 12, ANIM_DOUBLE_JUMP = 13;
    public static final int TRANSFORM_TICKS = 48;
    /** ticks into the transform when the water cocoon bursts */
    public static final int TRANSFORM_BURST = 21;

    /** beam channel window in ticks from ability start */
    public static final int BEAM_START = 16, BEAM_END = 56;
    public static final double BEAM_RANGE = 26.0;

    /** entity ids of players that are transformed, as known by the client (never touched on a dedicated server) */
    public static final Set<Integer> CLIENT_MORPHED = ConcurrentHashMap.newKeySet();
}
