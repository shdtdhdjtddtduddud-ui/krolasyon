package com.krolasyon.beyblade.entity;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/** All vanilla particle spawns of the mod go through here; they are switched off (the rendered effects carry the visuals). */
public final class Px {
    private Px() {}

    public static boolean ENABLED = false;

    public static void send(ServerLevel l, ParticleOptions p, double x, double y, double z, int n, double dx, double dy, double dz, double speed) {
        if (ENABLED) l.sendParticles(p, x, y, z, n, dx, dy, dz, speed);
    }

    public static void add(Level l, ParticleOptions p, double x, double y, double z, double vx, double vy, double vz) {
        if (ENABLED) l.addParticle(p, x, y, z, vx, vy, vz);
    }
}
