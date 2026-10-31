package com.krolasyon.cille.entity;

import com.krolasyon.cille.registry.ModEntities;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Behmud's water vortices.
 * WHIRLPOOL (Su Sarmalı): a wide spinning whirlpool that drags enemies in towards its eye and grinds them.
 * SPOUT (Hortum): a towering waterspout that sucks enemies and loose ground up, spins them high and flings them out.
 */
public class VortexEntity extends Entity {
    public static final int KIND_WHIRLPOOL = 0, KIND_SPOUT = 1;
    private static final EntityDataAccessor<Integer> DATA_KIND = SynchedEntityData.defineId(VortexEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_BIG = SynchedEntityData.defineId(VortexEntity.class, EntityDataSerializers.BOOLEAN);

    @Nullable private CilleEntity owner;
    private final List<FallingBlockEntity> debris = new ArrayList<>();
    private int ripped;

    public VortexEntity(EntityType<? extends VortexEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public static void spawn(CilleEntity owner, Vec3 at, int kind, boolean big) {
        Level level = owner.level();
        Double y = ShadowSpikeEntity.findGround(level, at.x, at.z, at.y + 6, at.y - 12);
        VortexEntity v = new VortexEntity(ModEntities.VORTEX.get(), level);
        v.owner = owner;
        v.moveTo(at.x, y != null ? y : at.y, at.z, 0F, 0F);
        v.entityData.set(DATA_KIND, kind);
        v.entityData.set(DATA_BIG, big);
        level.addFreshEntity(v);
    }

    public int getKind() { return this.entityData.get(DATA_KIND); }
    public boolean isBig() { return this.entityData.get(DATA_BIG); }
    public int life() { return getKind() == KIND_SPOUT ? 100 : 110; }
    public float radius() { return (getKind() == KIND_SPOUT ? 3.0F : 5.5F) * (isBig() ? 1.3F : 1F); }
    public float height() { return (getKind() == KIND_SPOUT ? 11F : 2.6F) * (isBig() ? 1.2F : 1F); }

    /** 0..1 grow-in / fade-out */
    public float size(float partial) {
        float t = this.tickCount + partial;
        if (t < 12) return t / 12F;
        if (t > life() - 12) return Math.max(0F, (life() - t) / 12F);
        return 1F;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_KIND, KIND_WHIRLPOOL);
        this.entityData.define(DATA_BIG, false);
    }

    @Override
    public void tick() {
        super.tick();
        int t = this.tickCount;
        boolean spout = getKind() == KIND_SPOUT;
        float R = radius(), Hh = height();
        if (this.level().isClientSide()) {
            float s = size(0F);
            for (int i = 0; i < (spout ? 8 : 6); i++) {
                double h = random.nextDouble() * Hh * s;
                double r = (spout ? 0.6 + R * (h / Hh) : R * (0.3 + 0.7 * random.nextDouble())) * s;
                double a = random.nextDouble() * Math.PI * 2;
                double x = getX() + Math.cos(a) * r, z = getZ() + Math.sin(a) * r;
                this.level().addParticle(i % 3 == 0 ? ParticleTypes.SPLASH : (i % 3 == 1 ? ParticleTypes.BUBBLE_POP : ParticleTypes.CLOUD),
                        x, getY() + h, z, -Math.sin(a) * 0.3, spout ? 0.15 : 0.02, Math.cos(a) * 0.3);
            }
            if (random.nextInt(2) == 0) this.level().addParticle(ParticleTypes.FALLING_WATER,
                    getX() + (random.nextDouble() - 0.5) * R * 2, getY() + Hh * s, getZ() + (random.nextDouble() - 0.5) * R * 2, 0, 0, 0);
            return;
        }
        ServerLevel sl = (ServerLevel) this.level();
        if (owner == null || owner.isRemoved()) {
            if (t > 2) { finish(sl); return; }
        }
        if (t % 30 == 2) sl.playSound(null, getX(), getY(), getZ(), spout ? ModSounds.WATERSPOUT.get() : ModSounds.WHIRLPOOL.get(), SoundSource.HOSTILE, 2.0F, 0.95F);

        float s = size(0F);
        Vec3 c = position();
        AABB box = new AABB(c.x - R * 1.8, c.y - 2, c.z - R * 1.8, c.x + R * 1.8, c.y + Hh + 2, c.z + R * 1.8);
        float power = owner instanceof BehmudEntity b ? b.power() : 1F;
        for (Entity e : this.level().getEntities(this, box, e -> e instanceof ItemEntity || (e instanceof LivingEntity && owner != null && owner.isHostileTo(e)))) {
            Vec3 d = new Vec3(c.x - e.getX(), 0, c.z - e.getZ());
            double dist = d.length();
            if (dist > R * 1.8) continue;
            Vec3 in = dist < 1.0E-3 ? Vec3.ZERO : d.scale(1 / dist);
            Vec3 tangent = new Vec3(-in.z, 0, in.x);
            Vec3 v;
            if (spout) {
                double lift = e.getY() < c.y + Hh - 1 ? 0.22 : -0.02;
                v = tangent.scale(0.45).add(in.scale(dist > 1.5 ? 0.14 : 0.02)).add(0, lift, 0);
            } else {
                v = tangent.scale(0.32).add(in.scale(0.10 + 0.08 * (1 - dist / (R * 1.8)))).add(0, dist < 1.6 ? -0.05 : 0.02, 0);
            }
            e.setDeltaMovement(e.getDeltaMovement().scale(0.6).add(v.scale(s)));
            e.hurtMarked = true;
            e.fallDistance = 0;
            e.clearFire();
            if (e instanceof LivingEntity le) {
                le.setAirSupply(Math.max(-10, le.getAirSupply() - 4));
                if (t % 10 == 0) {
                    float dmg = (spout ? 3.0F : 3.5F) * power;
                    le.hurt(damageSources().indirectMagic(this, owner), dist < 1.8 ? dmg * 1.5F : dmg);
                    le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2));
                }
            }
        }

        // the spout tears loose ground up into the funnel; both rip up plants and put out fires
        if (t % 6 == 0 && Terrain.allowed(sl, owner)) {
            BlockPos base = BlockPos.containing(c);
            double a = random.nextDouble() * Math.PI * 2, r = random.nextDouble() * R;
            BlockPos col = base.offset((int) Math.round(Math.cos(a) * r), 0, (int) Math.round(Math.sin(a) * r));
            Terrain.shatterSoft(sl, owner, col, 1, 2);
            for (BlockPos p : BlockPos.betweenClosed(col.offset(-1, -1, -1), col.offset(1, 2, 1))) {
                if (sl.getBlockState(p).is(net.minecraft.world.level.block.Blocks.FIRE)) sl.removeBlock(p, false);
            }
            if (spout && ripped < (isBig() ? 30 : 20)) {
                BlockPos g = col;
                for (int k = 0; k < 4 && sl.getBlockState(g).isAir(); k++) g = g.below();
                BlockState st = sl.getBlockState(g);
                if (Terrain.breakable(sl, g, st, 0.8F) && Terrain.exposed(sl, g)) {
                    FallingBlockEntity fb = Terrain.rip(sl, g, new Vec3(0, 0.5, 0));
                    if (fb != null) {
                        fb.setNoGravity(true);
                        debris.add(fb);
                        ripped++;
                    }
                }
            }
        }
        // debris spins up the funnel
        debris.removeIf(fb -> {
            if (fb.isRemoved()) return true;
            Vec3 d = new Vec3(c.x - fb.getX(), 0, c.z - fb.getZ());
            double dist = Math.max(0.3, d.length());
            Vec3 in = d.scale(1 / dist);
            Vec3 tangent = new Vec3(-in.z, 0, in.x);
            double target = 0.8 + R * ((fb.getY() - c.y) / Hh);
            double radial = (dist - target) * 0.25;
            double up = fb.getY() < c.y + Hh ? 0.18 : 0.0;
            fb.setDeltaMovement(tangent.scale(0.55).add(in.scale(radial)).add(0, up, 0));
            fb.time = 1;
            fb.hurtMarked = true;
            return false;
        });

        if (t >= life()) finish(sl);
    }

    private void finish(ServerLevel sl) {
        Vec3 c = position();
        sl.sendParticles(ParticleTypes.SPLASH, c.x, c.y + 1, c.z, 200, radius(), height() * 0.4, radius(), 0.4);
        sl.playSound(null, c.x, c.y, c.z, ModSounds.WATER_BOOM.get(), SoundSource.HOSTILE, 2.0F, 1.0F);
        // everything held up is thrown outward
        for (FallingBlockEntity fb : debris) {
            if (fb.isRemoved()) continue;
            Vec3 out = fb.position().subtract(c).multiply(1, 0, 1);
            out = out.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : out.normalize();
            fb.setNoGravity(false);
            fb.setDeltaMovement(out.x * 0.9, 0.3, out.z * 0.9);
            fb.hurtMarked = true;
        }
        debris.clear();
        if (owner != null && getKind() == KIND_SPOUT) {
            for (LivingEntity e : owner.hostilesAround(c.add(0, height() * 0.5, 0), radius() * 2 + 2)) {
                Vec3 out = e.position().subtract(c).multiply(1, 0, 1);
                out = out.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : out.normalize();
                e.setDeltaMovement(out.x * 1.4, 0.5, out.z * 1.4);
                e.hurtMarked = true;
            }
        }
        this.discard();
    }

    @Override protected void readAdditionalSaveData(CompoundTag tag) { this.discard(); }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
    @Override public boolean isPickable() { return false; }
    @Override public boolean shouldRenderAtSqrDistance(double d) { return d < 128 * 128; }
    @Override public AABB getBoundingBoxForCulling() { return this.getBoundingBox().inflate(radius() + 1, height(), radius() + 1); }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
