package com.krolasyon.bosses.rpg.util;

import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Server side particle / sound helpers shared by abilities, spells and sword skills. */
public final class FX {
    private FX() {}

    public static Vector3f rgb(int c) {
        return new Vector3f(((c >> 16) & 255) / 255F, ((c >> 8) & 255) / 255F, (c & 255) / 255F);
    }

    public static ParticleOptions dust(int c, float size) {
        return new DustParticleOptions(rgb(c), Mth.clamp(size, 0.05F, 4.0F));
    }

    public static ParticleOptions dust(int from, int to, float size) {
        return new DustColorTransitionOptions(rgb(from), rgb(to), Mth.clamp(size, 0.05F, 4.0F));
    }

    public static void send(Level level, ParticleOptions p, double x, double y, double z, int count, double dx, double dy, double dz, double speed) {
        if (level instanceof ServerLevel sl) sl.sendParticles(p, x, y, z, count, dx, dy, dz, speed);
    }

    public static void send(Level level, ParticleOptions p, Vec3 at, int count, double spread, double speed) {
        send(level, p, at.x, at.y, at.z, count, spread, spread, spread, speed);
    }

    public static void sound(Level level, Vec3 at, SoundEvent s, float vol, float pitch) {
        level.playSound(null, at.x, at.y, at.z, s, SoundSource.HOSTILE, vol, pitch);
    }

    public static void sound(Entity e, SoundEvent s, float vol, float pitch) {
        e.level().playSound(null, e.getX(), e.getY(), e.getZ(), s, e.getSoundSource(), vol, pitch);
    }

    /** horizontal ring of particles */
    public static void ring(Level level, ParticleOptions p, Vec3 c, double r, int points, double dy) {
        for (int i = 0; i < points; i++) {
            double a = i * Math.PI * 2 / points;
            send(level, p, c.x + Math.cos(a) * r, c.y, c.z + Math.sin(a) * r, 1, 0, dy, 0, 0.0);
        }
    }

    /** ring whose particles fly outwards */
    public static void burstRing(Level level, ParticleOptions p, Vec3 c, int points, double speed) {
        if (!(level instanceof ServerLevel sl)) return;
        for (int i = 0; i < points; i++) {
            double a = i * Math.PI * 2 / points;
            // count 0 makes dx/dy/dz the velocity
            sl.sendParticles(p, c.x, c.y, c.z, 0, Math.cos(a), 0.05, Math.sin(a), speed);
        }
    }

    public static void sphere(Level level, ParticleOptions p, Vec3 c, double r, int points) {
        for (int i = 0; i < points; i++) {
            double u = level.random.nextDouble() * 2 - 1;
            double th = level.random.nextDouble() * Math.PI * 2;
            double s = Math.sqrt(1 - u * u);
            send(level, p, c.x + s * Math.cos(th) * r, c.y + u * r, c.z + s * Math.sin(th) * r, 1, 0, 0, 0, 0);
        }
    }

    public static void line(Level level, ParticleOptions p, Vec3 a, Vec3 b, double step, double jitter) {
        Vec3 d = b.subtract(a);
        double len = d.length();
        if (len < 1.0E-3) return;
        int n = Math.max(1, (int) (len / step));
        for (int i = 0; i <= n; i++) {
            Vec3 q = a.add(d.scale(i / (double) n));
            send(level, p, q.x, q.y, q.z, 1, jitter, jitter, jitter, 0);
        }
    }

    /** jagged lightning-like line */
    public static void zigzag(Level level, ParticleOptions p, Vec3 a, Vec3 b, int segments, double amp) {
        Vec3 prev = a;
        for (int i = 1; i <= segments; i++) {
            Vec3 q = a.add(b.subtract(a).scale(i / (double) segments));
            if (i < segments) q = q.add((level.random.nextDouble() - 0.5) * amp, (level.random.nextDouble() - 0.5) * amp, (level.random.nextDouble() - 0.5) * amp);
            line(level, p, prev, q, 0.25, 0.02);
            prev = q;
        }
    }

    public static void column(Level level, ParticleOptions p, Vec3 base, double height, double radius, int count) {
        for (int i = 0; i < count; i++) {
            double a = level.random.nextDouble() * Math.PI * 2;
            double r = level.random.nextDouble() * radius;
            send(level, p, base.x + Math.cos(a) * r, base.y + level.random.nextDouble() * height, base.z + Math.sin(a) * r, 1, 0, 0.05, 0, 0.02);
        }
    }

    public static void spiral(Level level, ParticleOptions p, Vec3 base, double height, double radius, int turns, int points) {
        for (int i = 0; i < points; i++) {
            double t = i / (double) points;
            double a = t * Math.PI * 2 * turns;
            send(level, p, base.x + Math.cos(a) * radius, base.y + t * height, base.z + Math.sin(a) * radius, 1, 0, 0, 0, 0);
        }
    }

    /** crescent arc in front of an entity, used for sword slashes */
    public static void arc(Level level, ParticleOptions p, Vec3 center, float yawDeg, double radius, double spanDeg, double y, int points) {
        double base = Math.toRadians(yawDeg + 90);
        for (int i = 0; i < points; i++) {
            double t = i / (double) (points - 1) - 0.5;
            double a = base + Math.toRadians(spanDeg) * t;
            send(level, p, center.x + Math.cos(a) * radius, center.y + y + t * 0.6, center.z + Math.sin(a) * radius, 1, 0.02, 0.02, 0.02, 0);
        }
    }

    public static void cone(Level level, ParticleOptions p, Vec3 origin, Vec3 dir, double range, double spread, int count) {
        Vec3 d = dir.normalize();
        for (int i = 0; i < count; i++) {
            double t = level.random.nextDouble() * range;
            Vec3 q = origin.add(d.scale(t)).add((level.random.nextDouble() - 0.5) * spread * t, (level.random.nextDouble() - 0.5) * spread * t * 0.6, (level.random.nextDouble() - 0.5) * spread * t);
            send(level, p, q.x, q.y, q.z, 1, 0, 0, 0, 0);
        }
    }
}
