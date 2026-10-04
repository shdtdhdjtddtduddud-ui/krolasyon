package com.rabona.arena.game;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

/**
 * Saha geometrisi. Yerel koordinatlar: a = uzunluk ekseni (-HALF_LEN..HALF_LEN, +a "dogu kale"),
 * b = genislik ekseni. Zemin y = origin.y, oyun yuzeyi = origin.y + 1.
 */
public class Pitch {
    public static final int HALF_LEN = 35, HALF_WID = 22;
    public static final double GOAL_HALF_W = 3.85, GOAL_H = 3.0;
    public static final int BOX_DEPTH = 12, BOX_HALF = 12, SMALL_DEPTH = 5, SMALL_HALF = 6, PEN_SPOT = 9, CIRCLE = 7;

    public final BlockPos origin;
    public final boolean alongX;

    public Pitch(BlockPos origin, boolean alongX) {
        this.origin = origin;
        this.alongX = alongX;
    }

    public double surfaceY() { return origin.getY() + 1; }

    public Vec3 center() { return new Vec3(origin.getX() + 0.5, surfaceY(), origin.getZ() + 0.5); }

    /** dunya -> yerel (a, b). */
    public double a(Vec3 p) { return alongX ? p.x - (origin.getX() + 0.5) : p.z - (origin.getZ() + 0.5); }

    public double b(Vec3 p) { return alongX ? p.z - (origin.getZ() + 0.5) : -(p.x - (origin.getX() + 0.5)); }

    public Vec3 world(double a, double b, double y) {
        double cx = origin.getX() + 0.5, cz = origin.getZ() + 0.5;
        return alongX ? new Vec3(cx + a, y, cz + b) : new Vec3(cx - b, y, cz + a);
    }

    public BlockPos block(int a, int b, int dy) {
        return alongX ? origin.offset(a, dy, b) : origin.offset(-b, dy, a);
    }

    /** yerel a ekseni yonu dunyada. */
    public Vec3 axisA() { return alongX ? new Vec3(1, 0, 0) : new Vec3(0, 0, 1); }

    public Vec3 axisB() { return alongX ? new Vec3(0, 0, 1) : new Vec3(-1, 0, 0); }

    /** kale cizgisinin ic kenari (top tamamen gecmeli). side = +1 / -1 */
    public double goalLineA() { return HALF_LEN + 0.5; }

    public Vec3 goalCenter(int side) { return world(side * goalLineA(), 0, surfaceY()); }

    public boolean inside(Vec3 p, double margin) {
        return Math.abs(a(p)) <= HALF_LEN + 0.5 + margin && Math.abs(b(p)) <= HALF_WID + 0.5 + margin
                && p.y > surfaceY() - 3 && p.y < surfaceY() + 25;
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putLong("o", origin.asLong());
        t.putBoolean("x", alongX);
        return t;
    }

    public static Pitch load(CompoundTag t) {
        return new Pitch(BlockPos.of(t.getLong("o")), t.getBoolean("x"));
    }
}
