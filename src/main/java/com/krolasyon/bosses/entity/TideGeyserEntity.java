package com.krolasyon.bosses.entity;

import com.krolasyon.bosses.morph.MorphServer;
import com.krolasyon.bosses.registry.ModEntities;
import com.krolasyon.bosses.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/** Abyssal Geyser: telegraphs with bubbling water, then a pillar of water erupts and launches enemies. */
public class TideGeyserEntity extends AbilityEntity {
    private static final EntityDataAccessor<Integer> DATA_WARMUP = SynchedEntityData.defineId(TideGeyserEntity.class, EntityDataSerializers.INT);
    public static final int ACTIVE = 24;
    private float damage = 9F;
    private boolean loud;
    private boolean struck;

    public TideGeyserEntity(EntityType<? extends TideGeyserEntity> type, Level level) {
        super(type, level);
    }

    public static void spawn(Player owner, double x, double z, double baseY, int warmup, float damage, boolean loud) {
        Level level = owner.level();
        Double y = findGround(level, x, z, baseY + 3, baseY - 6);
        if (y == null) return;
        TideGeyserEntity e = new TideGeyserEntity(ModEntities.TIDE_GEYSER.get(), level);
        e.setOwner(owner);
        e.damage = damage;
        e.loud = loud;
        e.moveTo(x, y, z, owner.getRandom().nextFloat() * 360F, 0F);
        e.entityData.set(DATA_WARMUP, warmup);
        level.addFreshEntity(e);
    }

    @Nullable
    public static Double findGround(Level level, double x, double z, double yMax, double yMin) {
        return EruptionEntity.findGround(level, x, z, yMax, yMin);
    }

    public int getWarmup() { return this.entityData.get(DATA_WARMUP); }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_WARMUP, 0);
    }

    @Override
    public void tick() {
        super.tick();
        int t = this.tickCount - getWarmup();
        if (this.level().isClientSide()) {
            if (t < 0) {
                this.level().addParticle(ParticleTypes.BUBBLE_POP, getX() + (random.nextDouble() - 0.5), getY() + 0.1, getZ() + (random.nextDouble() - 0.5), 0, 0.05, 0);
                if (random.nextInt(2) == 0) this.level().addParticle(ParticleTypes.SPLASH, getX() + (random.nextDouble() - 0.5), getY() + 0.1, getZ() + (random.nextDouble() - 0.5), 0, 0.1, 0);
            } else if (t < 16) {
                for (int i = 0; i < 4; i++) {
                    this.level().addParticle(i % 2 == 0 ? ParticleTypes.SPLASH : ParticleTypes.FALLING_WATER,
                            getX() + (random.nextDouble() - 0.5) * 1.2, getY() + random.nextDouble() * 3.2, getZ() + (random.nextDouble() - 0.5) * 1.2,
                            (random.nextDouble() - 0.5) * 0.3, 0.3, (random.nextDouble() - 0.5) * 0.3);
                }
            }
            return;
        }
        if (t == 0) {
            ServerLevel sl = (ServerLevel) this.level();
            if (loud) sl.playSound(null, getX(), getY(), getZ(), ModSounds.GEYSER_ERUPT.get(), SoundSource.PLAYERS, 1.1F, 0.85F + random.nextFloat() * 0.35F);
            sl.sendParticles(ParticleTypes.SPLASH, getX(), getY() + 0.5, getZ(), 30, 0.4, 0.8, 0.4, 0.3);
            sl.sendParticles(MorphServer.dust(0x7FFFF6, 1.6F), getX(), getY() + 1.5, getZ(), 10, 0.3, 1.2, 0.3, 0);
        }
        if (t >= 0 && t <= 3 && !struck) {
            struck = true;
            Player o = owner();
            for (LivingEntity e : targets(this.getBoundingBox().inflate(0.4, 0.6, 0.4))) {
                if (o != null && e.hurt(damageSources().indirectMagic(this, o), damage)) {
                    e.setDeltaMovement(e.getDeltaMovement().x * 0.3, 0.95, e.getDeltaMovement().z * 0.3);
                    e.hurtMarked = true;
                    e.clearFire();
                }
            }
        }
        if (t > ACTIVE) this.discard();
    }
}
