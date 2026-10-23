package com.krolasyon.realweather.client.particle;

import com.krolasyon.realweather.client.ClientWeather;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Hava parçacıkları: kar tanesi, yaprak, kum, ateş böceği, su halkası, sis, nefes buharı, yağmur sıçraması. */
public final class WeatherParticles {
    private WeatherParticles() {}

    private abstract static class Base extends TextureSheetParticle {
        protected final SpriteSet sprites;

        Base(ClientLevel l, double x, double y, double z, SpriteSet s) {
            super(l, x, y, z);
            this.sprites = s;
            pickSprite(s);
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    // ------------------------------------------------------------------ kar tanesi: süzülür, sallanır, rüzgârla savrulur
    public static final class Snowflake extends Base {
        private final float phase, sway;

        Snowflake(ClientLevel l, double x, double y, double z, double vx, double vz, SpriteSet s) {
            super(l, x, y, z, s);
            lifetime = 60 + random.nextInt(80);
            quadSize = 0.035F + random.nextFloat() * 0.06F;
            gravity = 0;
            phase = random.nextFloat() * 6.28F;
            sway = 0.01F + random.nextFloat() * 0.02F;
            xd = vx;
            zd = vz;
            yd = -0.04 - random.nextFloat() * 0.04;
            alpha = 0;
            roll = random.nextFloat() * 6.28F;
        }

        @Override
        public void tick() {
            super.tick();
            float t = age + phase * 10;
            double tx = ClientWeather.windX * (0.6 + 0.6 * ClientWeather.stormness), tz = ClientWeather.windZ * (0.6 + 0.6 * ClientWeather.stormness);
            xd += (tx - xd) * 0.08 + Math.sin(t * 0.13) * sway;
            zd += (tz - zd) * 0.08 + Math.cos(t * 0.11) * sway;
            yd += (-0.05 - 0.03 * ClientWeather.stormness - yd) * 0.1;
            oRoll = roll;
            roll += 0.05F;
            alpha = Math.min(0.95F, alpha + 0.12F) * (age > lifetime - 10 ? (lifetime - age) / 10F : 1);
            if (onGround) remove();
        }
    }

    // ------------------------------------------------------------------ yaprak: biyom renginde, takla atarak düşer
    public static final class Leaf extends Base {
        private final float spin, flutter;
        private int grounded;

        Leaf(ClientLevel l, double x, double y, double z, double vx, double vz, SpriteSet s) {
            super(l, x, y, z, s);
            lifetime = 220 + random.nextInt(120);
            quadSize = 0.07F + random.nextFloat() * 0.05F;
            int c = BiomeColors.getAverageFoliageColor(l, BlockPos.containing(x, y, z));
            float shade = 0.85F + random.nextFloat() * 0.25F;
            rCol = ((c >> 16) & 255) / 255F * shade;
            gCol = ((c >> 8) & 255) / 255F * shade;
            bCol = (c & 255) / 255F * shade;
            if (random.nextInt(6) == 0) {
                // ara sıra kurumuş, sararmış yaprak
                rCol = 0.75F;
                gCol = 0.55F;
                bCol = 0.2F;
            }
            spin = (random.nextFloat() - 0.5F) * 0.3F;
            flutter = random.nextFloat() * 6.28F;
            gravity = 0;
            xd = vx * 0.5;
            zd = vz * 0.5;
            yd = -0.02;
            roll = random.nextFloat() * 6.28F;
        }

        @Override
        public void tick() {
            super.tick();
            if (onGround) {
                xd = zd = 0;
                if (++grounded > 60) alpha = Math.max(0, alpha - 0.05F);
                if (alpha <= 0) remove();
                return;
            }
            float t = age * 0.12F + flutter;
            xd += (ClientWeather.windX * 0.7 - xd) * 0.05 + Math.sin(t) * 0.006;
            zd += (ClientWeather.windZ * 0.7 - zd) * 0.05 + Math.cos(t * 0.8) * 0.006;
            yd += (-0.035 - 0.02 * Math.abs(Math.sin(t)) - yd) * 0.1;
            oRoll = roll;
            roll += spin * (1 + (float) Math.abs(Math.sin(t)));
        }
    }

    // ------------------------------------------------------------------ kum: küçük, hızlı, yere yakın
    public static final class Sand extends Base {
        Sand(ClientLevel l, double x, double y, double z, double vx, double vz, SpriteSet s) {
            super(l, x, y, z, s);
            lifetime = 25 + random.nextInt(35);
            quadSize = 0.015F + random.nextFloat() * 0.035F;
            float k = 0.8F + random.nextFloat() * 0.3F;
            rCol = 0.86F * k;
            gCol = 0.70F * k;
            bCol = 0.47F * k;
            gravity = 0.01F;
            xd = vx * (0.7 + random.nextFloat() * 0.6);
            zd = vz * (0.7 + random.nextFloat() * 0.6);
            yd = (random.nextFloat() - 0.4F) * 0.04;
            alpha = 0.85F;
            hasPhysics = true;
        }

        @Override
        public void tick() {
            super.tick();
            xd += (ClientWeather.windX * 1.4 - xd) * 0.1 + (random.nextFloat() - 0.5F) * 0.04;
            zd += (ClientWeather.windZ * 1.4 - zd) * 0.1 + (random.nextFloat() - 0.5F) * 0.04;
            if (onGround) yd = 0.03 + random.nextFloat() * 0.05;
            if (age > lifetime - 8) alpha = (lifetime - age) / 8F * 0.85F;
        }
    }

    // ------------------------------------------------------------------ ateş böceği: yanıp söner, gezinir, kendi ışığıyla parlar
    public static final class Firefly extends Base {
        private final float phase;
        private double tx, ty, tz;

        Firefly(ClientLevel l, double x, double y, double z, SpriteSet s) {
            super(l, x, y, z, s);
            lifetime = 160 + random.nextInt(200);
            quadSize = 0.045F + random.nextFloat() * 0.025F;
            rCol = 0.85F;
            gCol = 1.0F;
            bCol = 0.45F;
            gravity = 0;
            hasPhysics = false;
            phase = random.nextFloat() * 100;
            alpha = 0;
            tx = x;
            ty = y;
            tz = z;
        }

        @Override
        public void tick() {
            super.tick();
            if (age % 20 == 0) {
                tx = x + (random.nextFloat() - 0.5F) * 2;
                ty = y + (random.nextFloat() - 0.5F) * 1;
                tz = z + (random.nextFloat() - 0.5F) * 2;
            }
            xd += (tx - x) * 0.004 - xd * 0.05;
            yd += (ty - y) * 0.004 - yd * 0.05;
            zd += (tz - z) * 0.004 - zd * 0.05;
            float blink = (float) Math.max(0, Math.sin((age + phase) * 0.09)) * (float) Math.max(0, Math.sin((age + phase) * 0.023 + 1));
            float fade = Math.min(1, Math.min(age / 20F, (lifetime - age) / 20F));
            alpha = Mth.clamp(0.15F + blink * 1.4F, 0, 1) * fade;
        }

        @Override
        protected int getLightColor(float pt) {
            return 0xF000F0;
        }
    }

    // ------------------------------------------------------------------ su halkası: yere yatık, genişleyerek söner
    public static final class Ripple extends Base {
        Ripple(ClientLevel l, double x, double y, double z, SpriteSet s) {
            super(l, x, y, z, s);
            lifetime = 10 + random.nextInt(6);
            quadSize = 0.05F;
            gravity = 0;
            hasPhysics = false;
            alpha = 0.7F;
            setSpriteFromAge(s);
        }

        @Override
        public void tick() {
            super.tick();
            quadSize = 0.05F + 0.35F * age / lifetime;
            alpha = 0.7F * (1 - age / (float) lifetime);
        }

        @Override
        public void render(VertexConsumer vc, Camera cam, float pt) {
            Vec3 c = cam.getPosition();
            float x = (float) (Mth.lerp(pt, xo, this.x) - c.x), y = (float) (Mth.lerp(pt, yo, this.y) - c.y), z = (float) (Mth.lerp(pt, zo, this.z) - c.z);
            float s = getQuadSize(pt);
            float u0 = getU0(), u1 = getU1(), v0 = getV0(), v1 = getV1();
            int light = getLightColor(pt);
            float[][] q = {{-s, -s}, {-s, s}, {s, s}, {s, -s}};
            float[][] uv = {{u1, v1}, {u1, v0}, {u0, v0}, {u0, v1}};
            for (int side = 0; side < 2; side++) {
                for (int k = 0; k < 4; k++) {
                    int i = side == 0 ? k : 3 - k;
                    vc.vertex(x + q[i][0], y, z + q[i][1]).uv(uv[i][0], uv[i][1]).color(rCol, gCol, bCol, alpha).uv2(light).endVertex();
                }
            }
        }
    }

    // ------------------------------------------------------------------ sis/pus: büyük, çok saydam, ağır ağır süzülen bulutçuk
    public static final class Mist extends Base {
        private final float maxAlpha;

        Mist(ClientLevel l, double x, double y, double z, double vx, double vz, SpriteSet s) {
            super(l, x, y, z, s);
            lifetime = 180 + random.nextInt(160);
            quadSize = 1.6F + random.nextFloat() * 2.4F;
            maxAlpha = 0.05F + random.nextFloat() * 0.08F;
            alpha = 0;
            gravity = 0;
            hasPhysics = false;
            xd = vx;
            zd = vz;
            yd = 0.002;
            roll = random.nextFloat() * 6.28F;
            float k = 0.9F + random.nextFloat() * 0.1F;
            rCol = gCol = bCol = k;
        }

        @Override
        public void tick() {
            super.tick();
            float f = Math.min(1, Math.min(age / 40F, (lifetime - age) / 50F));
            alpha = maxAlpha * f;
            xd *= 0.99;
            zd *= 0.99;
            oRoll = roll;
            roll += 0.002F;
        }
    }

    // ------------------------------------------------------------------ nefes buharı
    public static final class Breath extends Base {
        Breath(ClientLevel l, double x, double y, double z, double vx, double vy, double vz, SpriteSet s) {
            super(l, x, y, z, s);
            lifetime = 22 + random.nextInt(10);
            quadSize = 0.06F;
            alpha = 0.35F;
            gravity = -0.002F;
            hasPhysics = false;
            xd = vx;
            yd = vy;
            zd = vz;
            rCol = gCol = bCol = 0.95F;
        }

        @Override
        public void tick() {
            super.tick();
            quadSize = 0.06F + 0.16F * age / lifetime;
            alpha = 0.35F * (1 - age / (float) lifetime);
            xd *= 0.92;
            zd *= 0.92;
        }
    }

    // ------------------------------------------------------------------ yağmur sıçraması: sekip düşen damlacık
    public static final class Splash extends Base {
        Splash(ClientLevel l, double x, double y, double z, SpriteSet s) {
            super(l, x, y, z, s);
            lifetime = 8 + random.nextInt(5);
            quadSize = 0.025F + random.nextFloat() * 0.02F;
            gravity = 0.9F;
            xd = (random.nextFloat() - 0.5F) * 0.12;
            zd = (random.nextFloat() - 0.5F) * 0.12;
            yd = 0.1 + random.nextFloat() * 0.08;
            rCol = 0.75F;
            gCol = 0.82F;
            bCol = 0.95F;
            alpha = 0.75F;
        }
    }

    // ------------------------------------------------------------------ sağlayıcılar
    public record Provider(SpriteSet sprites, String kind) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType t, ClientLevel l, double x, double y, double z, double vx, double vy, double vz) {
            return switch (kind) {
                case "snowflake" -> new Snowflake(l, x, y, z, vx, vz, sprites);
                case "leaf" -> new Leaf(l, x, y, z, vx, vz, sprites);
                case "sand" -> new Sand(l, x, y, z, vx, vz, sprites);
                case "firefly" -> new Firefly(l, x, y, z, sprites);
                case "ripple" -> new Ripple(l, x, y, z, sprites);
                case "mist" -> new Mist(l, x, y, z, vx, vz, sprites);
                case "breath" -> new Breath(l, x, y, z, vx, vy, vz, sprites);
                default -> new Splash(l, x, y, z, sprites);
            };
        }
    }

}
