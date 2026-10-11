package com.krolasyon.omnitrix.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/** Fire, slime, frost, ecto and omnitrix-green particles (sprites generated with Higgsfield). */
public class OmniParticle extends TextureSheetParticle {
    public enum Style { FIRE, SLIME, FROST, ECTO, OMNI }

    private final SpriteSet sprites;
    private final Style style;
    private final float base;

    protected OmniParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites, Style style) {
        super(level, x, y, z, vx, vy, vz);
        this.sprites = sprites;
        this.style = style;
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.hasPhysics = style == Style.SLIME;
        switch (style) {
            case FIRE -> { lifetime = 10 + random.nextInt(10); base = 0.18F + random.nextFloat() * 0.18F; friction = 0.9F; gravity = -0.03F; }
            case SLIME -> { lifetime = 18 + random.nextInt(12); base = 0.1F + random.nextFloat() * 0.1F; friction = 0.95F; gravity = 0.5F; }
            case FROST -> { lifetime = 14 + random.nextInt(14); base = 0.1F + random.nextFloat() * 0.12F; friction = 0.9F; gravity = 0.05F; }
            case ECTO -> { lifetime = 16 + random.nextInt(14); base = 0.2F + random.nextFloat() * 0.2F; friction = 0.92F; gravity = -0.02F; }
            default -> { lifetime = 12 + random.nextInt(10); base = 0.06F + random.nextFloat() * 0.06F; friction = 0.9F; gravity = -0.01F; setColor(0.4F, 1F, 0.4F); }
        }
        quadSize = base;
        pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        float f = (float) age / lifetime;
        if (style == Style.FIRE || style == Style.ECTO) {
            if (age % 3 == 0) pickSprite(sprites);
            quadSize = base * (1 + f * (style == Style.FIRE ? 0.6F : 1.2F));
        }
        alpha = 1 - f * f;
    }

    @Override
    public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }

    @Override
    protected int getLightColor(float partial) { return style == Style.SLIME ? super.getLightColor(partial) : 0xF000F0; }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final Style style;

        public Provider(SpriteSet sprites, Style style) {
            this.sprites = sprites;
            this.style = style;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
            return new OmniParticle(level, x, y, z, vx, vy, vz, sprites, style);
        }
    }
}
