package com.krolasyon.futbol.game;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Pitch geometry. Long axis is X. Line blocks span x in [cx-HL, cx+HL], z in [cz-HW, cz+HW]; ground blocks are at
 * {@code y}, players stand at y+1. RED defends the -X goal, BLUE the +X goal.
 */
public final class Pitch {
    public static final int HL = 32;
    public static final int HW = 20;
    public static final int GOAL_HALF = 4;      // posts at cz +- 4
    public static final double MOUTH_HALF = 3.8; // free space between posts measured from cz+0.5
    public static final double BAR_Y = 3.8;      // ball bottom must stay under y+1+BAR_Y-1 ... (see inGoal)
    public static final int BOX_DEPTH = 11;
    public static final int BOX_HALF = 15;

    public final String dim;
    public final int cx, cz, y;

    public Pitch(String dim, int cx, int y, int cz) {
        this.dim = dim;
        this.cx = cx;
        this.cz = cz;
        this.y = y;
    }

    public double xMin() { return cx - HL; }
    public double xMax() { return cx + HL + 1; }
    public double zMin() { return cz - HW; }
    public double zMax() { return cz + HW + 1; }
    public double midZ() { return cz + 0.5; }
    public double midX() { return cx + 0.5; }
    public double floor() { return y + 1; }

    public Vec3 center() { return new Vec3(midX(), floor(), midZ()); }

    /** goal-line x at the given end (-1 or +1) */
    public double lineX(int side) { return side > 0 ? xMax() : xMin(); }

    public Vec3 goalCenter(int side) { return new Vec3(lineX(side), floor(), midZ()); }

    /** goal the team attacks */
    public Vec3 attackGoal(Team t) { return goalCenter(t.attackDir()); }

    public Vec3 ownGoal(Team t) { return goalCenter(-t.attackDir()); }

    /** -1/+1 if the ball (center pos, radius r) is completely inside the goal at that end, else 0 */
    public int goalSide(Vec3 p, double r) {
        if (Math.abs(p.z - midZ()) > MOUTH_HALF - r * 0.2) return 0;
        if (p.y - r > floor() + 3.3) return 0;
        if (p.x + r < xMin() && p.x > xMin() - 3.0) return -1;
        if (p.x - r > xMax() && p.x < xMax() + 3.0) return 1;
        return 0;
    }

    public boolean outOverGoalLine(Vec3 p, double r) { return p.x + r < xMin() || p.x - r > xMax(); }

    public boolean outOverTouchLine(Vec3 p, double r) { return p.z + r < zMin() || p.z - r > zMax(); }

    public boolean near(Vec3 p, double margin) {
        return p.x > xMin() - margin && p.x < xMax() + margin && p.z > zMin() - margin && p.z < zMax() + margin;
    }

    /** team relative coordinates: u = 0 own goal line .. 1 opponent goal line, v = -1..1 across */
    public Vec3 point(Team t, double u, double v) {
        double len = xMax() - xMin();
        double x = t.attackDir() > 0 ? xMin() + u * len : xMax() - u * len;
        double z = midZ() + v * HW * t.attackDir();
        return new Vec3(x, floor(), z);
    }

    public double teamU(Team t, double x) {
        double u = (x - xMin()) / (xMax() - xMin());
        return t.attackDir() > 0 ? u : 1.0 - u;
    }

    public double teamV(Team t, double z) { return (z - midZ()) / HW * t.attackDir(); }

    public boolean inPenaltyArea(Team defending, Vec3 p) {
        double u = teamU(defending, p.x) * (HL * 2 + 1);
        return u >= -1 && u <= BOX_DEPTH + 1 && Math.abs(p.z - midZ()) <= BOX_HALF + 0.5;
    }

    public Vec3 clampInside(Vec3 p, double m) {
        return new Vec3(Mth.clamp(p.x, xMin() + m, xMax() - m), p.y, Mth.clamp(p.z, zMin() + m, zMax() - m));
    }

    public ServerLevel level(MinecraftServer server) {
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(dim));
        ServerLevel l = server.getLevel(key);
        return l != null ? l : server.overworld();
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putString("dim", dim);
        t.putInt("cx", cx);
        t.putInt("cz", cz);
        t.putInt("y", y);
        return t;
    }

    public static Pitch load(CompoundTag t) { return new Pitch(t.getString("dim"), t.getInt("cx"), t.getInt("y"), t.getInt("cz")); }
}
