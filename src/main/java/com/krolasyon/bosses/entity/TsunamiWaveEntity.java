package com.krolasyon.bosses.entity;

import com.krolasyon.bosses.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/** Tsunami Crash: a ring shaped wall of water expanding outward from the impact point. */
public class TsunamiWaveEntity extends AbilityEntity {
    public static final int LIFE = 30;
    public static final float MAX_RADIUS = 14F;
    private final Set<Integer> hit = new HashSet<>();

    public TsunamiWaveEntity(EntityType<? extends TsunamiWaveEntity> type, Level level) {
        super(type, level);
    }

    public static void spawn(Player owner, Vec3 at) {
        TsunamiWaveEntity e = new TsunamiWaveEntity(ModEntities.TSUNAMI_WAVE.get(), owner.level());
        e.setOwner(owner);
        e.moveTo(at.x, at.y, at.z, 0F, 0F);
        owner.level().addFreshEntity(e);
    }

    public static float radius(float t) {
        float k = Mth.clamp(t / LIFE, 0F, 1F);
        return 1.0F + (MAX_RADIUS - 1.0F) * (1F - (1F - k) * (1F - k));
    }

    /** wall height: rises quickly, then sinks as it spreads */
    public static float height(float t) {
        float k = Mth.clamp(t / LIFE, 0F, 1F);
        return 3.4F * Mth.sin(Mth.clamp(k * 3.2F, 0F, Mth.HALF_PI)) * (1F - k * k);
    }

    @Override
    public void tick() {
        super.tick();
        int t = this.tickCount;
        float r = radius(t);
        Vec3 c = this.position();
        if (this.level().isClientSide()) {
            int n = (int) (r * 5);
            for (int i = 0; i < n; i++) {
                double a = random.nextDouble() * Math.PI * 2;
                double x = c.x + Math.cos(a) * r, z = c.z + Math.sin(a) * r;
                double h = height(t);
                this.level().addParticle(ParticleTypes.SPLASH, x, c.y + random.nextDouble() * h, z, Math.cos(a) * 0.3, 0.2, Math.sin(a) * 0.3);
                if (i % 4 == 0) this.level().addParticle(ParticleTypes.CLOUD, x, c.y + h, z, Math.cos(a) * 0.08, 0.02, Math.sin(a) * 0.08);
            }
            return;
        }
        Player o = owner();
        if (o != null && t < LIFE) {
            AABB box = new AABB(c, c).inflate(r + 1, 3.5, r + 1);
            for (LivingEntity e : targets(box)) {
                Vec3 d = e.position().subtract(c);
                double dist = Math.sqrt(d.x * d.x + d.z * d.z);
                if (dist > r + 0.8 || dist < r - 2.0 || !hit.add(e.getId())) continue;
                if (e.hurt(damageSources().indirectMagic(this, o), 14.0F)) {
                    Vec3 h = dist < 1.0E-3 ? new Vec3(1, 0, 0) : new Vec3(d.x / dist, 0, d.z / dist);
                    double kr = 1.0 - e.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE) * 0.5;
                    e.setDeltaMovement(h.x * 1.8 * kr, 0.75 * kr, h.z * 1.8 * kr);
                    e.hurtMarked = true;
                    e.clearFire();
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                }
            }
        }
        if (t >= LIFE + 8) this.discard();
    }
}
