package com.krolasyon.bosses.entity;

import com.krolasyon.bosses.morph.MorphServer;
import com.krolasyon.bosses.registry.ModEntities;
import com.krolasyon.bosses.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** A glowing ground rune: the Solar Seal (detonates), the Ember Rain circle and the Hell Gate (rains spears). */
public class ZoneEntity extends AbilityEntity {
    public static final int SUN_SEAL = 0, EMBER_RAIN = 1, HELL_GATE = 2;
    private static final EntityDataAccessor<Integer> DATA_KIND = SynchedEntityData.defineId(ZoneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_RADIUS = SynchedEntityData.defineId(ZoneEntity.class, EntityDataSerializers.FLOAT);
    public static final int SEAL_BURST = 30;

    public ZoneEntity(EntityType<? extends ZoneEntity> type, Level level) {
        super(type, level);
    }

    public static void spawn(Player owner, Vec3 at, int kind, float radius) {
        ZoneEntity e = new ZoneEntity(ModEntities.ZONE.get(), owner.level());
        e.setOwner(owner);
        e.entityData.set(DATA_KIND, kind);
        e.entityData.set(DATA_RADIUS, radius);
        e.moveTo(at.x, at.y, at.z, 0F, 0F);
        owner.level().addFreshEntity(e);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_KIND, SUN_SEAL);
        this.entityData.define(DATA_RADIUS, 6F);
    }

    public int getKind() { return this.entityData.get(DATA_KIND); }
    public float getRadius() { return this.entityData.get(DATA_RADIUS); }

    public int life() {
        return switch (getKind()) { case SUN_SEAL -> SEAL_BURST + 12; case EMBER_RAIN -> 70; default -> 60; };
    }

    @Override
    public void tick() {
        super.tick();
        int t = this.tickCount;
        int kind = getKind();
        float r = getRadius();
        Vec3 c = position();
        if (level().isClientSide()) {
            for (int i = 0; i < 3; i++) {
                double a = random.nextDouble() * Math.PI * 2;
                double rr = r * (0.3 + random.nextDouble() * 0.7);
                var part = kind == HELL_GATE ? (i == 0 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME) : ParticleTypes.FLAME;
                if (kind == SUN_SEAL && t < SEAL_BURST) part = ParticleTypes.END_ROD;
                level().addParticle(part, c.x + Math.cos(a) * rr, c.y + 0.1, c.z + Math.sin(a) * rr, 0, kind == SUN_SEAL ? 0.12 : 0.05, 0);
            }
            if (kind == SUN_SEAL && t == SEAL_BURST) {
                for (int i = 0; i < 60; i++)
                    level().addParticle(ParticleTypes.FLAME, c.x + random.nextGaussian() * 0.6, c.y + random.nextDouble() * 6, c.z + random.nextGaussian() * 0.6, 0, 0.4, 0);
            }
            return;
        }
        ServerLevel sl = (ServerLevel) level();
        Player o = owner();
        switch (kind) {
            case SUN_SEAL -> {
                if (t == SEAL_BURST && o != null) {
                    MorphServer.sound(sl, c, ModSounds.FIRE_EXPLODE.get(), 2.0F, 0.9F);
                    sl.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 1, c.z, 4, r * 0.4, 0.5, r * 0.4, 0);
                    sl.sendParticles(ParticleTypes.LAVA, c.x, c.y + 0.5, c.z, 30, r * 0.4, 0.3, r * 0.4, 0);
                    MorphServer.ring(sl, c.add(0, 0.2, 0), r, 60, ParticleTypes.FLAME, 0.1);
                    TsunamiWaveEntity.spawn(o, c, TsunamiWaveEntity.FIRE, r + 5, 8F);
                    for (LivingEntity e : targets(new AABB(c, c).inflate(r, 4, r))) {
                        if (e.position().distanceToSqr(c) > r * r) continue;
                        if (e.hurt(damageSources().indirectMagic(this, o), 15F)) {
                            e.setSecondsOnFire(8);
                            e.setDeltaMovement(e.getDeltaMovement().add(0, 1.0, 0));
                            e.hurtMarked = true;
                        }
                    }
                }
            }
            case EMBER_RAIN -> {
                if (o != null && t >= 4 && t % 2 == 0 && t < life() - 10) {
                    double a = random.nextDouble() * Math.PI * 2, rr = Math.sqrt(random.nextDouble()) * r;
                    Vec3 at = c.add(Math.cos(a) * rr, 14, Math.sin(a) * rr);
                    FormProjectileEntity.shoot(o, FormProjectileEntity.EMBER, at, new Vec3(random.nextGaussian() * 0.05, -1.2, random.nextGaussian() * 0.05), 5F);
                }
            }
            default -> {
                if (o != null && t >= 8 && t % 4 == 0 && t < life() - 12) {
                    double a = random.nextDouble() * Math.PI * 2, rr = Math.sqrt(random.nextDouble()) * r;
                    Vec3 at = c.add(Math.cos(a) * rr, 16, Math.sin(a) * rr);
                    FormProjectileEntity.shoot(o, FormProjectileEntity.FALLING_SPEAR, at, new Vec3(0, -1.6, 0), 8F);
                    if (t % 12 == 0) MorphServer.sound(sl, at, ModSounds.SPEAR_FALL.get(), 1.0F, 0.9F + random.nextFloat() * 0.2F);
                }
            }
        }
        if (t >= life()) discard();
    }
}
