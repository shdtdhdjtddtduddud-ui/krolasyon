package com.krolasyon.omnitrix.client;

import java.util.Arrays;

/** Per-bone local rotation (radians, applied Z then Y then X about the bone pivot), offset and scale. */
public final class Pose {
    public final float[] rx, ry, rz, tx, ty, tz, s;
    /** whole-model transform */
    public float rootX, rootY, rootZ, rootYaw, rootPitch, rootRoll, scaleX = 1, scaleY = 1, scaleZ = 1;

    public Pose(int bones) {
        rx = new float[bones];
        ry = new float[bones];
        rz = new float[bones];
        tx = new float[bones];
        ty = new float[bones];
        tz = new float[bones];
        s = new float[bones];
        Arrays.fill(s, 1F);
    }

    public void reset() {
        Arrays.fill(rx, 0);
        Arrays.fill(ry, 0);
        Arrays.fill(rz, 0);
        Arrays.fill(tx, 0);
        Arrays.fill(ty, 0);
        Arrays.fill(tz, 0);
        Arrays.fill(s, 1F);
        rootX = rootY = rootZ = rootYaw = rootPitch = rootRoll = 0;
        scaleX = scaleY = scaleZ = 1;
    }
}
