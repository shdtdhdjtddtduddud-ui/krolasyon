package com.krolasyon.voidtree.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/** Shadow wisps (slow, drifting, growing) and sparkle stars (short twinkles); sprites generated with Higgsfield. */
public class VoidParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final boolean star;
    private final float baseSize;

    protected VoidParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites, boolean star) {
        super(level, x, y, z, vx, vy, vz);
        this.sprites = sprites;
        this.star = star;
        this.xd = vx + (random.nextDouble() - 0.5) * 0.02;
        this.yd = vy + (random.nextDouble() - 0.5) * 0.02 + (star ? 0 : 0.01);
        this.zd = vz + (random.nextDouble() - 0.5) * 0.02;
        this.hasPhysics = false;
        if (star) {
            lifetime = 6 + random.nextInt(10);
            baseSize = 0.06F + random.nextFloat() * 0.08F;
            friction = 0.88F;
            setColor(1F, 0.92F + random.nextFloat() * 0.08F, 1F);
        } else {
            lifetime = 14 + random.nextInt(16);
            baseSize = 0.12F + random.nextFloat() * 0.16F;
            friction = 0.9F;
            gravity = -0.02F;
            float c = random.nextFloat();
            if (c < 0.5F) setColor(0.72F, 0.42F, 1F);
            else if (c < 0.8F) setColor(0.9F, 0.65F, 1F);
            else setColor(0.45F, 0.2F, 0.75F);
        }
        quadSize = baseSize;
        pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        float f = (float) age / lifetime;
        if (star) {
            quadSize = baseSize * (float) Math.sin(f * Math.PI) * 1.4F;
            alpha = 1F;
        } else {
            if (age % 4 == 0) pickSprite(sprites);
            quadSize = baseSize * (1F + f * 1.2F);
            alpha = (1F - f) * 0.85F;
        }
    }

    @Override
    public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }

    @Override
    protected int getLightColor(float partial) { return 0xF000F0; }

    public static class WispProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public WispProvider(SpriteSet sprites) { this.sprites = sprites; }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
            return new VoidParticle(level, x, y, z, vx, vy, vz, sprites, false);
        }
    }

    public static class StarProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public StarProvider(SpriteSet sprites) { this.sprites = sprites; }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
            return new VoidParticle(level, x, y, z, vx, vy, vz, sprites, true);
        }
    }
}
