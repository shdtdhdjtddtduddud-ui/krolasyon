package com.krolasyon.bosses.entity;

import com.krolasyon.bosses.morph.MorphServer;
import com.krolasyon.bosses.registry.ModEntities;
import com.krolasyon.bosses.registry.ModSounds;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/** Player-owned ground eruption: water geyser, fire pillar, black thorns or blood spikes. Telegraphs, erupts and launches. */
public class TideGeyserEntity extends AbilityEntity {
    public static final int WATER = 0, FIRE = 1, THORN = 2, BLOOD = 3;
    private static final EntityDataAccessor<Integer> DATA_WARMUP = SynchedEntityData.defineId(TideGeyserEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_KIND = SynchedEntityData.defineId(TideGeyserEntity.class, EntityDataSerializers.INT);
    public static final int ACTIVE = 24;
    private float damage = 9F;
    private boolean loud;
    private boolean struck;

    public TideGeyserEntity(EntityType<? extends TideGeyserEntity> type, Level level) {
        super(type, level);
    }

    public static void spawn(Player owner, double x, double z, double baseY, int warmup, float damage, boolean loud) {
        spawn(owner, WATER, x, z, baseY, warmup, damage, loud);
    }

    public static void spawn(Player owner, int kind, double x, double z, double baseY, int warmup, float damage, boolean loud) {
        Level level = owner.level();
        Double y = findGround(level, x, z, baseY + 3, baseY - 6);
        if (y == null) return;
        TideGeyserEntity e = new TideGeyserEntity(ModEntities.TIDE_GEYSER.get(), level);
        e.setOwner(owner);
        e.damage = damage;
        e.loud = loud;
        e.moveTo(x, y, z, owner.getRandom().nextFloat() * 360F, 0F);
        e.entityData.set(DATA_WARMUP, warmup);
        e.entityData.set(DATA_KIND, kind);
        level.addFreshEntity(e);
    }

    @Nullable
    public static Double findGround(Level level, double x, double z, double yMax, double yMin) {
        return EruptionEntity.findGround(level, x, z, yMax, yMin);
    }

    public int getWarmup() { return this.entityData.get(DATA_WARMUP); }
    public int getKind() { return this.entityData.get(DATA_KIND); }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_WARMUP, 0);
        this.entityData.define(DATA_KIND, WATER);
    }

    @Override
    public void tick() {
        super.tick();
        int t = this.tickCount - getWarmup();
        int kind = getKind();
        if (this.level().isClientSide()) {
            if (t < 0) {
                ParticleOptions tele = switch (kind) {
                    case FIRE -> ParticleTypes.SMALL_FLAME;
                    case THORN -> ParticleTypes.SMOKE;
                    case BLOOD -> MorphServer.dust(0x7A0612, 1.0F);
                    default -> ParticleTypes.BUBBLE_POP;
                };
                this.level().addParticle(tele, getX() + (random.nextDouble() - 0.5), getY() + 0.1, getZ() + (random.nextDouble() - 0.5), 0, 0.05, 0);
                if (kind == WATER && random.nextInt(2) == 0)
                    this.level().addParticle(ParticleTypes.SPLASH, getX() + (random.nextDouble() - 0.5), getY() + 0.1, getZ() + (random.nextDouble() - 0.5), 0, 0.1, 0);
            } else if (t < 16) {
                for (int i = 0; i < 4; i++) {
                    ParticleOptions p = switch (kind) {
                        case FIRE -> i % 2 == 0 ? ParticleTypes.FLAME : ParticleTypes.LARGE_SMOKE;
                        case THORN -> i % 2 == 0 ? ParticleTypes.SMOKE : MorphServer.dust(0x8C1028, 1.0F);
                        case BLOOD -> MorphServer.dust(i == 0 ? 0xC21A28 : 0x6A0410, 1.2F);
                        default -> i % 2 == 0 ? ParticleTypes.SPLASH : ParticleTypes.FALLING_WATER;
                    };
                    double h = kind == FIRE ? 3.2 : kind == WATER ? 3.2 : 1.6;
                    this.level().addParticle(p, getX() + (random.nextDouble() - 0.5) * 1.2, getY() + random.nextDouble() * h, getZ() + (random.nextDouble() - 0.5) * 1.2,
                            (random.nextDouble() - 0.5) * 0.2, kind == FIRE ? 0.18 : 0.3, (random.nextDouble() - 0.5) * 0.2);
                }
            }
            return;
        }
        if (t == 0) {
            ServerLevel sl = (ServerLevel) this.level();
            SoundEvent snd = switch (kind) {
                case FIRE -> ModSounds.FISSURE.get();
                case THORN -> ModSounds.THORN_ERUPT.get();
                case BLOOD -> ModSounds.BLOOD_WAVE.get();
                default -> ModSounds.GEYSER_ERUPT.get();
            };
            if (loud) sl.playSound(null, getX(), getY(), getZ(), snd, SoundSource.PLAYERS, 1.1F, 0.85F + random.nextFloat() * 0.35F);
            switch (kind) {
                case FIRE -> {
                    sl.sendParticles(ParticleTypes.LAVA, getX(), getY() + 0.3, getZ(), 4, 0.3, 0.2, 0.3, 0);
                    sl.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.5, getZ(), 16, 0.3, 0.8, 0.3, 0.08);
                }
                case THORN -> sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.5, getZ(), 6, 0.3, 0.6, 0.3, 0.02);
                case BLOOD -> sl.sendParticles(MorphServer.dust(0xB0121E, 1.4F), getX(), getY() + 0.4, getZ(), 14, 0.5, 0.4, 0.5, 0);
                default -> {
                    sl.sendParticles(ParticleTypes.SPLASH, getX(), getY() + 0.5, getZ(), 30, 0.4, 0.8, 0.4, 0.3);
                    sl.sendParticles(MorphServer.dust(0x7FFFF6, 1.0F), getX(), getY() + 1.5, getZ(), 10, 0.3, 1.2, 0.3, 0);
                }
            }
        }
        if (t >= 0 && t <= 3 && !struck) {
            struck = true;
            Player o = owner();
            for (LivingEntity e : targets(this.getBoundingBox().inflate(0.4, 0.6, 0.4))) {
                if (o == null || !e.hurt(damageSources().indirectMagic(this, o), damage)) continue;
                double lift = switch (kind) { case THORN -> 0.3; case BLOOD -> 0.6; case FIRE -> 0.8; default -> 0.95; };
                e.setDeltaMovement(e.getDeltaMovement().x * 0.3, lift, e.getDeltaMovement().z * 0.3);
                e.hurtMarked = true;
                switch (kind) {
                    case FIRE -> e.setSecondsOnFire(6);
                    case THORN -> {
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 3));
                        e.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
                    }
                    case BLOOD -> e.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
                    default -> e.clearFire();
                }
            }
        }
        if (t > ACTIVE) this.discard();
    }
}
