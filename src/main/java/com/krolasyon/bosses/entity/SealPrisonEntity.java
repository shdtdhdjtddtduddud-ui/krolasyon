package com.krolasyon.bosses.entity;

import com.krolasyon.bosses.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;

/** Triangle seal rune on the ground: drags enemies to its centre, roots them, then detonates. */
public class SealPrisonEntity extends Entity {
    private static final EntityDataAccessor<Boolean> DATA_EMPOWERED = SynchedEntityData.defineId(SealPrisonEntity.class, EntityDataSerializers.BOOLEAN);
    public static final int BURST_TICK = 44, LIFE = 56;
    public static final double RADIUS = 3.6;

    @Nullable private BossEntity owner;

    public SealPrisonEntity(EntityType<? extends SealPrisonEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public void setup(BossEntity owner, double x, double y, double z, boolean empowered) {
        this.owner = owner;
        this.moveTo(x, y + 0.02, z, 0, 0);
        this.entityData.set(DATA_EMPOWERED, empowered);
    }

    public boolean isEmpowered() { return this.entityData.get(DATA_EMPOWERED); }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_EMPOWERED, false);
    }

    @Override
    public void tick() {
        super.tick();
        int t = this.tickCount;
        double r = RADIUS * (isEmpowered() ? 1.25 : 1.0);
        if (this.level().isClientSide()) {
            if (t < BURST_TICK) {
                for (int i = 0; i < 3; i++) {
                    double a = random.nextDouble() * Math.PI * 2;
                    this.level().addParticle(BossEntity.dust(i == 0 ? SealWardenEntity.PINK_LIGHT : SealWardenEntity.PINK, 1.2F),
                            getX() + Math.cos(a) * r, getY() + 0.1, getZ() + Math.sin(a) * r, -Math.cos(a) * 0.08, 0.06, -Math.sin(a) * 0.08);
                }
                if (t > BURST_TICK - 12) {
                    this.level().addParticle(ParticleTypes.END_ROD, getX() + (random.nextDouble() - 0.5) * r, getY() + random.nextDouble() * 3, getZ() + (random.nextDouble() - 0.5) * r, 0, 0.15, 0);
                }
            }
            return;
        }
        ServerLevel sl = (ServerLevel) this.level();
        if (t == 1) this.level().playSound(null, getX(), getY(), getZ(), ModSounds.PRISON_FORM.get(), SoundSource.HOSTILE, 2.0F, 1.2F);
        if (t < BURST_TICK) {
            AABB box = new AABB(getX() - r, getY() - 1, getZ() - r, getX() + r, getY() + 4, getZ() + r);
            for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class, box)) {
                if (owner != null ? !owner.isHostileTo(e) : e instanceof BossEntity) continue;
                Vec3 to = new Vec3(getX() - e.getX(), 0, getZ() - e.getZ());
                double d = to.length();
                if (d > r) continue;
                if (d > 0.4) {
                    Vec3 pull = to.normalize().scale(Math.min(0.16, d * 0.05));
                    e.setDeltaMovement(e.getDeltaMovement().multiply(0.6, 1, 0.6).add(pull));
                    e.hurtMarked = true;
                }
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 3, false, false));
                e.addEffect(new MobEffectInstance(MobEffects.JUMP, 10, 128, false, false));
                if (t % 10 == 0 && owner != null) e.hurt(damageSources().indirectMagic(this, owner), 2F);
            }
        }
        if (t == BURST_TICK) {
            this.level().playSound(null, getX(), getY(), getZ(), ModSounds.PRISON_BURST.get(), SoundSource.HOSTILE, 3.0F, 1.0F);
            sl.sendParticles(ParticleTypes.FLASH, getX(), getY() + 1, getZ(), 2, 0.3, 0.3, 0.3, 0);
            sl.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 1, getZ(), 3, r * 0.4, 0.5, r * 0.4, 0);
            sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.5, getZ(), 80, 0.3, 0.3, 0.3, 0.45);
            for (int i = 0; i < 60; i++) {
                double a = i * Math.PI * 2 / 60;
                sl.sendParticles(BossEntity.dust(i % 2 == 0 ? SealWardenEntity.PINK : SealWardenEntity.PINK_LIGHT, 2.2F),
                        getX(), getY() + 0.3, getZ(), 0, Math.cos(a), 0.3, Math.sin(a), 0.8);
            }
            // pillar of light
            for (int i = 0; i < 40; i++) {
                sl.sendParticles(BossEntity.dust(SealWardenEntity.PINK_LIGHT, 2.5F), getX(), getY() + i * 0.2, getZ(), 2, 0.25, 0.05, 0.25, 0);
            }
            float dmg = isEmpowered() ? 20F : 16F;
            AABB box = new AABB(getX() - r, getY() - 1, getZ() - r, getX() + r, getY() + 5, getZ() + r);
            for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class, box)) {
                if (owner != null ? !owner.isHostileTo(e) : e instanceof BossEntity) continue;
                if (e.distanceToSqr(getX(), e.getY(), getZ()) > r * r) continue;
                boolean hurt = owner != null ? e.hurt(damageSources().indirectMagic(this, owner), dmg) : e.hurt(damageSources().magic(), dmg);
                if (hurt) {
                    e.setDeltaMovement(e.getDeltaMovement().add(0, 1.1, 0));
                    e.hurtMarked = true;
                    e.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 25, 1));
                }
            }
        }
        if (t >= LIFE) this.discard();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) { this.discard(); }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public boolean isPickable() { return false; }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @SuppressWarnings("unused")
    private static Entity none() { return null; }
}
