package com.krolasyon.bosses.morph.abilities;

import com.krolasyon.bosses.morph.MorphServer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** shared transformation cocoon / burst effects for the fire and shadow forms */
final class FireFx {
    private FireFx() {}

    static void cocoon(ServerPlayer p, ServerLevel sl, int t, int color, ParticleOptions main, ParticleOptions extra) {
        Vec3 c = p.position();
        float k = t / 21F;
        if (t < 21) {
            for (int i = 0; i < 3; i++) {
                double a = t * 0.55 + i * Math.PI * 2 / 3;
                double r = 1.6 - k * 0.9;
                double y = (t % 10) * 0.28;
                sl.sendParticles(MorphServer.dust(color, 0.9F), c.x + Math.cos(a) * r, c.y + y, c.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
                sl.sendParticles(main, c.x + Math.cos(a + 1) * r, c.y + y * 0.5, c.z + Math.sin(a + 1) * r, 1, 0, 0.05, 0, 0.02);
            }
            if (t % 3 == 0) sl.sendParticles(extra, c.x, c.y + 1.2, c.z, 2, 0.5, 0.8, 0.5, 0.02);
        } else if (t < 41 && t % 2 == 0) {
            sl.sendParticles(main, c.x, c.y + 0.1, c.z, 3, 0.8, 0.0, 0.8, 0.02);
        }
    }

    static void burst(ServerPlayer p, ServerLevel sl, int color, ParticleOptions main, ParticleOptions extra) {
        Vec3 c = p.position();
        sl.sendParticles(main, c.x, c.y + 1.5, c.z, 160, 1.2, 1.4, 1.2, 0.15);
        sl.sendParticles(extra, c.x, c.y + 1.5, c.z, 40, 1.0, 1.2, 1.0, 0.05);
        sl.sendParticles(ParticleTypes.LAVA, c.x, c.y + 1, c.z, 20, 0.8, 0.5, 0.8, 0);
        MorphServer.ring(sl, c.add(0, 0.1, 0), 2.5, 40, MorphServer.dust(color, 1.0F), 0);
        MorphServer.ring(sl, c.add(0, 0.1, 0), 4.0, 50, main, 0.08);
    }
}
