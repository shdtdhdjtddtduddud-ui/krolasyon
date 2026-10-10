package com.krolasyon.storm.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/** Small flickering electric spark (sprites generated with Higgsfield). */
public class SparkParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    protected SparkParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z, vx, vy, vz);
        this.sprites = sprites;
        this.xd = vx + (random.nextDouble() - 0.5) * 0.05;
        this.yd = vy + (random.nextDouble() - 0.5) * 0.05;
        this.zd = vz + (random.nextDouble() - 0.5) * 0.05;
        this.lifetime = 5 + random.nextInt(8);
        this.quadSize = 0.07F + random.nextFloat() * 0.09F;
        this.friction = 0.82F;
        this.gravity = 0.25F;
        this.hasPhysics = false;
        float c = random.nextFloat();
        if (c < 0.55F) setColor(0.85F, 0.95F, 1F);
        else if (c < 0.85F) setColor(0.45F, 0.75F, 1F);
        else setColor(0.7F, 0.5F, 1F);
        pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (age % 2 == 0) pickSprite(sprites);
        alpha = 1F - (float) age / lifetime * 0.8F;
        if (random.nextInt(4) == 0) alpha *= 0.4F;
    }

    @Override
    public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }

    @Override
    protected int getLightColor(float partial) { return 0xF000F0; }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) { this.sprites = sprites; }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
            return new SparkParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}
