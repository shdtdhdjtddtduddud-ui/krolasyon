package com.krolasyon.cille.entity;

import com.krolasyon.cille.registry.ModSounds;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Kabkaba's black hole: pulls its enemies in (they can barely walk away), tears blocks out of the ground and
 * swallows them, crushes whatever reaches the core, then collapses in a shock wave that flings the debris.
 */
public class BlackHoleEntity extends Entity {
    private static final EntityDataAccessor<Boolean> DATA_BIG = SynchedEntityData.defineId(BlackHoleEntity.class, EntityDataSerializers.BOOLEAN);
    public static final int LIFE = 130;
    public static final double PULL_RADIUS = 14.0;

    @Nullable private KabkabaEntity owner;
    private final List<FallingBlockEntity> debris = new ArrayList<>();
    private int ripped;

    public BlackHoleEntity(EntityType<? extends BlackHoleEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public void setup(KabkabaEntity owner, Vec3 at, boolean big) {
        this.owner = owner;
        this.moveTo(at.x, at.y - 0.75, at.z, 0F, 0F);
        this.entityData.set(DATA_BIG, big);
    }

    public boolean isBig() { return this.entityData.get(DATA_BIG); }

    /** 0..1 visual size: grows in, pulses, collapses */
    public float size(float partial) {
        float t = this.tickCount + partial;
        if (t < 16) return (float) Math.sin(t / 16F * Math.PI / 2);
        if (t > LIFE - 8) return Math.max(0F, (LIFE - t) / 8F);
        return 1F;
    }

    public Vec3 center() { return position().add(0, 0.75, 0); }

    @Override
    protected void defineSynchedData() { this.entityData.define(DATA_BIG, false); }

    @Override
    public void tick() {
        super.tick();
        Vec3 c = center();
        int t = this.tickCount;
        if (this.level().isClientSide()) {
            float s = size(0F);
            for (int i = 0; i < 6; i++) {
                double a = random.nextDouble() * Math.PI * 2, r = 2.5 + random.nextDouble() * 6 * s;
                double y = (random.nextDouble() - 0.5) * 2.5;
                Vec3 p = c.add(Math.cos(a) * r, y, Math.sin(a) * r);
                Vec3 v = c.subtract(p).scale(0.08).add(-Math.sin(a) * 0.25, 0, Math.cos(a) * 0.25);
                this.level().addParticle(i % 3 == 0 ? ParticleTypes.REVERSE_PORTAL : KabkabaEntity.dust(i % 2 == 0 ? KabkabaEntity.VIOLET : KabkabaEntity.INK, 1.5F), p.x, p.y, p.z, v.x, v.y, v.z);
            }
            if (random.nextInt(3) == 0) this.level().addParticle(ParticleTypes.SQUID_INK, c.x, c.y, c.z, 0, 0, 0);
            return;
        }
        if (owner == null || owner.isRemoved()) {
            if (t > 2) { collapse((ServerLevel) this.level()); return; }
        }
        ServerLevel sl = (ServerLevel) this.level();
        float strength = isBig() ? 1.3F : 1.0F;
        if (t % 40 == 5) sl.playSound(null, c.x, c.y, c.z, ModSounds.HOLE_LOOP.get(), SoundSource.HOSTILE, 2.5F, 0.9F + random.nextFloat() * 0.2F);

        // pull enemies + loose items
        AABB box = new AABB(c, c).inflate(PULL_RADIUS);
        for (Entity e : this.level().getEntities(this, box, e -> e instanceof ItemEntity || (e instanceof LivingEntity && owner != null && owner.isHostileTo(e)))) {
            Vec3 d = c.subtract(e.position().add(0, e.getBbHeight() * 0.5, 0));
            double dist = d.length();
            if (dist > PULL_RADIUS || dist < 1.0E-3) continue;
            double f = (0.05 + 0.16 * (1 - dist / PULL_RADIUS)) * strength * size(0F);
            Vec3 pull = d.normalize().scale(f);
            e.setDeltaMovement(e.getDeltaMovement().scale(0.85).add(pull));
            e.hurtMarked = true;
            if (e instanceof LivingEntity le) {
                le.fallDistance = 0;
                if (t % 20 == 0) le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2));
                if (dist < 2.2 && t % 8 == 0) {
                    float dmg = (isBig() ? 6F : 4.5F);
                    le.hurt(damageSources().indirectMagic(this, owner), dmg);
                    if (le instanceof Player) le.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0));
                    sl.sendParticles(KabkabaEntity.dust(KabkabaEntity.PALE, 1.4F), le.getX(), le.getY() + 1, le.getZ(), 6, 0.3, 0.4, 0.3, 0);
                }
            } else if (e instanceof ItemEntity && dist < 1.2) {
                e.discard();
            }
        }

        // tear blocks out of the ground and swallow them
        int maxRip = isBig() ? 60 : 40;
        if (t > 12 && t < LIFE - 10 && t % 2 == 0 && ripped < maxRip && Terrain.allowed(this.level(), owner)) {
            for (int tries = 0; tries < 4; tries++) {
                double a = random.nextDouble() * Math.PI * 2, r = 1.5 + random.nextDouble() * 6.5;
                BlockPos p = BlockPos.containing(c.x + Math.cos(a) * r, c.y - 1 - random.nextInt(7), c.z + Math.sin(a) * r);
                // walk up to the surface block of that column
                for (int up = 0; up < 6 && !this.level().getBlockState(p.above()).isAir(); up++) p = p.above();
                BlockState st = this.level().getBlockState(p);
                if (!Terrain.breakable(this.level(), p, st, 3.0F) || !Terrain.exposed(this.level(), p)) continue;
                FallingBlockEntity fb = Terrain.rip(sl, p, new Vec3(0, 0.3, 0));
                if (fb != null) {
                    fb.setNoGravity(true);
                    debris.add(fb);
                    ripped++;
                }
                break;
            }
        }
        debris.removeIf(fb -> {
            if (fb.isRemoved()) return true;
            Vec3 d = c.subtract(fb.position());
            double dist = d.length();
            if (dist < 1.3) {
                sl.sendParticles(ParticleTypes.REVERSE_PORTAL, fb.getX(), fb.getY() + 0.5, fb.getZ(), 10, 0.2, 0.2, 0.2, 0.2);
                fb.discard();
                return true;
            }
            Vec3 tangent = new Vec3(-d.z, 0, d.x).normalize().scale(0.18);
            fb.setDeltaMovement(d.normalize().scale(Math.min(0.55, 0.18 + 1.6 / dist)).add(tangent));
            fb.time = 1;
            fb.hurtMarked = true;
            return false;
        });

        if (t >= LIFE) collapse(sl);
    }

    private void collapse(ServerLevel sl) {
        Vec3 c = center();
        sl.playSound(null, c.x, c.y, c.z, ModSounds.HOLE_COLLAPSE.get(), SoundSource.HOSTILE, 4.0F, 1.0F);
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y, c.z, 2, 0.5, 0.5, 0.5, 0);
        sl.sendParticles(ParticleTypes.FLASH, c.x, c.y, c.z, 2, 0, 0, 0, 0);
        sl.sendParticles(ParticleTypes.REVERSE_PORTAL, c.x, c.y, c.z, 200, 0.5, 0.5, 0.5, 1.2);
        for (int i = 0; i < 80; i++) {
            double a = i * Math.PI * 2 / 80;
            sl.sendParticles(KabkabaEntity.dust(i % 2 == 0 ? KabkabaEntity.VIOLET : KabkabaEntity.INK, 3.0F), c.x, c.y, c.z, 0, Math.cos(a), 0.1, Math.sin(a), 1.2);
        }
        for (FallingBlockEntity fb : debris) {
            if (fb.isRemoved()) continue;
            Vec3 out = fb.position().subtract(c);
            out = out.lengthSqr() < 1.0E-4 ? new Vec3(0, 1, 0) : out.normalize();
            fb.setNoGravity(false);
            fb.setDeltaMovement(out.x * 0.9, 0.5 + random.nextDouble() * 0.4, out.z * 0.9);
            fb.hurtMarked = true;
        }
        debris.clear();
        if (owner != null) {
            float dmg = isBig() ? 16F : 12F;
            for (LivingEntity e : owner.hostilesAround(c, 7)) {
                if (e.hurt(damageSources().indirectMagic(this, owner), dmg)) {
                    Vec3 k = e.position().subtract(c).multiply(1, 0, 1);
                    k = k.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : k.normalize();
                    e.setDeltaMovement(k.x * 1.6, 0.7, k.z * 1.6);
                    e.hurtMarked = true;
                }
            }
        }
        this.discard();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) { this.discard(); }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public boolean isPickable() { return false; }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) { return d < 128 * 128; }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
