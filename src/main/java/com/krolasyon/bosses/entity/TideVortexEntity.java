package com.krolasyon.bosses.entity;

import com.krolasyon.bosses.morph.MorphServer;
import com.krolasyon.bosses.registry.ModEntities;
import com.krolasyon.bosses.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Maelstrom: a swirling water vortex that drags enemies in, churns them and bursts. */
public class TideVortexEntity extends AbilityEntity {
    public static final int LIFE = 110;
    public static final int BURST = 100;
    public static final double RADIUS = 7.5;

    public TideVortexEntity(EntityType<? extends TideVortexEntity> type, Level level) {
        super(type, level);
    }

    public static void spawn(Player owner, Vec3 at) {
        TideVortexEntity e = new TideVortexEntity(ModEntities.TIDE_VORTEX.get(), owner.level());
        e.setOwner(owner);
        e.moveTo(at.x, at.y, at.z, 0F, 0F);
        owner.level().addFreshEntity(e);
        owner.level().playSound(null, at.x, at.y, at.z, ModSounds.VORTEX_LOOP.get(), SoundSource.PLAYERS, 1.6F, 1.0F);
    }

    /** 0..1 grow-in, 1 while active, shrinking during the burst */
    public float strength(float partial) {
        float t = this.tickCount + partial;
        if (t < 12) return t / 12F;
        if (t < BURST) return 1F;
        return Mth.clamp(1F - (t - BURST) / (LIFE - BURST), 0F, 1F);
    }

    @Override
    public void tick() {
        super.tick();
        int t = this.tickCount;
        Vec3 c = this.position();
        if (this.level().isClientSide()) {
            float s = strength(0);
            if (t < BURST) {
                for (int i = 0; i < 6; i++) {
                    double a = t * 0.45 + i * Math.PI / 3 + random.nextDouble() * 0.3;
                    double r = (0.8 + random.nextDouble() * (RADIUS - 1)) * s;
                    double h = random.nextDouble() * 4.5 * s;
                    double tx = -Math.sin(a), tz = Math.cos(a);
                    this.level().addParticle(i % 3 == 0 ? ParticleTypes.SPLASH : ParticleTypes.FISHING, c.x + Math.cos(a) * r, c.y + h, c.z + Math.sin(a) * r,
                            tx * 0.35 - Math.cos(a) * 0.08, 0.05, tz * 0.35 - Math.sin(a) * 0.08);
                }
                if (t % 2 == 0) this.level().addParticle(ParticleTypes.BUBBLE_POP, c.x + random.nextGaussian() * 1.5, c.y + random.nextDouble() * 3, c.z + random.nextGaussian() * 1.5, 0, 0.05, 0);
            }
            return;
        }
        ServerLevel sl = (ServerLevel) this.level();
        if (t < BURST) {
            AABB box = new AABB(c, c).inflate(RADIUS, 4.0, RADIUS);
            for (LivingEntity e : targets(box)) {
                Vec3 d = c.add(0, 1.2, 0).subtract(e.position());
                double dist = Math.sqrt(d.x * d.x + d.z * d.z);
                if (dist > RADIUS) continue;
                Vec3 in = new Vec3(d.x, 0, d.z).normalize();
                Vec3 tan = new Vec3(-in.z, 0, in.x);
                double pull = dist > 1.2 ? 0.16 : 0.02;
                double kr = 1.0 - e.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE) * 0.5;
                Vec3 v = e.getDeltaMovement().scale(0.6).add(in.scale(pull * kr)).add(tan.scale(0.22 * kr));
                double lift = dist < 2.5 ? 0.12 : 0.02;
                e.setDeltaMovement(v.x, Math.max(v.y, -0.1) + lift * 0.5, v.z);
                e.hurtMarked = true;
                e.fallDistance = 0;
                if (t % 10 == 5) {
                    Player o = owner();
                    if (o != null && e.hurt(damageSources().indirectMagic(this, o), 3.0F))
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2));
                }
            }
            if (t % 20 == 0) MorphServer.ring(sl, c.add(0, 0.1, 0), RADIUS * 0.8, 30, ParticleTypes.SPLASH, 0.1);
        } else if (t == BURST) {
            sl.playSound(null, c.x, c.y, c.z, ModSounds.VORTEX_BURST.get(), SoundSource.PLAYERS, 1.8F, 1.0F);
            sl.sendParticles(ParticleTypes.SPLASH, c.x, c.y + 1.5, c.z, 260, 2.5, 1.5, 2.5, 0.5);
            sl.sendParticles(ParticleTypes.BUBBLE_POP, c.x, c.y + 1.5, c.z, 60, 2.0, 1.5, 2.0, 0.1);
            sl.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 1.2, c.z, 3, 1.0, 0.6, 1.0, 0);
            sl.sendParticles(MorphServer.dust(0x7FFFF6, 2.2F), c.x, c.y + 2, c.z, 60, 2.2, 2.0, 2.2, 0);
            Player o = owner();
            if (o != null) {
                for (LivingEntity e : targets(new AABB(c, c).inflate(RADIUS * 0.75, 4.0, RADIUS * 0.75))) {
                    if (e.hurt(damageSources().indirectMagic(this, o), 10.0F)) {
                        Vec3 d = e.position().subtract(c);
                        Vec3 h = new Vec3(d.x, 0, d.z);
                        if (h.lengthSqr() < 1.0E-4) h = new Vec3(1, 0, 0);
                        h = h.normalize();
                        e.setDeltaMovement(h.x * 0.9, 1.1, h.z * 0.9);
                        e.hurtMarked = true;
                    }
                }
            }
        }
        if (t >= LIFE) this.discard();
    }
}
