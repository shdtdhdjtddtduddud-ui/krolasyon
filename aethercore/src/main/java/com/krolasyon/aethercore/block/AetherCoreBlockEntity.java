package com.krolasyon.aethercore.block;

import com.krolasyon.aethercore.AetherCore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Çekirdeğin durumu: enerji, aktiflik, kalkan mantığı ve istemci tarafı animasyon değerleri.
 */
public final class AetherCoreBlockEntity extends BlockEntity {
    public static final double RADIUS = 16.0;
    public static final int MAX_ENERGY = 20 * 60 * 60 * 8;
    private static final int EVENT_IMPACT = 1;

    private int energy;
    private int age;

    // istemci animasyonu
    private float open, prevOpen;
    private float flash, prevFlash;
    private float dome, prevDome;
    private final float phase;

    public AetherCoreBlockEntity(BlockPos pos, BlockState state) {
        super(AetherCore.AETHER_CORE_BE.get(), pos, state);
        phase = (pos.hashCode() & 1023) / 1023F * 100F;
    }

    public int getEnergy() { return energy; }

    public void addEnergy(int amount) {
        energy = Math.min(MAX_ENERGY, energy + amount);
        setChanged();
    }

    public void setEnergyForTest(int e) { energy = e; setChanged(); }

    public boolean isActive() {
        return getBlockState().getValue(AetherCoreBlock.ACTIVE);
    }

    public void setActive(boolean active) {
        if (level == null || level.isClientSide) return;
        BlockState st = getBlockState();
        if (st.getValue(AetherCoreBlock.ACTIVE) == active) return;
        level.setBlock(worldPosition, st.setValue(AetherCoreBlock.ACTIVE, active), 3);
        ActiveCores.set(level, worldPosition, active);
        level.playSound(null, worldPosition, active ? SoundEvents.BEACON_ACTIVATE : SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.2F, active ? 1.3F : 0.9F);
        if (active) level.playSound(null, worldPosition, SoundEvents.CONDUIT_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.1F);
        setChanged();
    }

    public static String formatTime(int ticks) {
        int s = ticks / 20;
        if (s >= 3600) return String.format("%d sa %02d dk", s / 3600, (s / 60) % 60);
        return String.format("%d dk %02d sn", s / 60, s % 60);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) ActiveCores.set(level, worldPosition, isActive());
    }

    @Override
    public void setRemoved() {
        if (level != null) ActiveCores.set(level, worldPosition, false);
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        if (level != null) ActiveCores.set(level, worldPosition, false);
        super.onChunkUnloaded();
    }

    // ------------------------------------------------------------------ sunucu
    public static void serverTick(Level level, BlockPos pos, BlockState state, AetherCoreBlockEntity be) {
        be.age++;
        if (!state.getValue(AetherCoreBlock.ACTIVE)) return;
        if (be.energy <= 0) {
            be.setActive(false);
            return;
        }
        be.energy--;
        if (be.age % 100 == 0) be.setChanged();
        Vec3 c = Vec3.atCenterOf(pos);
        double r = RADIUS;
        AABB box = new AABB(c.x - r, c.y - r, c.z - r, c.x + r, c.y + r, c.z + r);

        // mermi kalkanı: oyuncu dışındakilerin attığı her şey kubbede yok olur
        for (Projectile p : level.getEntitiesOfClass(Projectile.class, box)) {
            Entity owner = p.getOwner();
            if (owner instanceof Player) continue;
            if (p.position().distanceToSqr(c) > r * r) continue;
            ((ServerLevel) level).sendParticles(ParticleTypes.ELECTRIC_SPARK, p.getX(), p.getY(), p.getZ(), 14, 0.2, 0.2, 0.2, 0.25);
            ((ServerLevel) level).sendParticles(ParticleTypes.GLOW, p.getX(), p.getY(), p.getZ(), 6, 0.3, 0.3, 0.3, 0.05);
            level.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.BLOCKS, 1.0F, 1.6F);
            p.discard();
            level.blockEvent(pos, state.getBlock(), EVENT_IMPACT, 0);
        }

        if (be.age % 10 == 0) {
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box)) {
                double d2 = e.position().distanceToSqr(c);
                if (d2 > r * r) continue;
                if (e instanceof Player player) {
                    if (be.age % 40 == 0) {
                        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0, true, false, true));
                        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 0, true, false, true));
                    }
                } else if (e instanceof Enemy) {
                    Vec3 away = e.position().subtract(c).multiply(1, 0, 1);
                    if (away.lengthSqr() < 1e-3) away = new Vec3(1, 0, 0);
                    away = away.normalize();
                    e.push(away.x * 0.55, 0.25, away.z * 0.55);
                    e.hurtMarked = true;
                    if (be.age % 20 == 0) {
                        e.hurt(level.damageSources().magic(), 2.0F);
                        ((ServerLevel) level).sendParticles(ParticleTypes.ELECTRIC_SPARK, e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), 8, 0.3, 0.4, 0.3, 0.2);
                    }
                }
            }
        }
        if (be.age % 80 == 0) level.playSound(null, pos, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.8F, 1.25F);
    }

    // ------------------------------------------------------------------ istemci
    public static void clientTick(Level level, BlockPos pos, BlockState state, AetherCoreBlockEntity be) {
        be.prevOpen = be.open;
        be.prevFlash = be.flash;
        be.prevDome = be.dome;
        boolean active = state.getValue(AetherCoreBlock.ACTIVE);
        be.open += ((active ? 1F : 0F) - be.open) * 0.06F;
        be.dome += ((active ? 1F : 0F) - be.dome) * (active ? 0.025F : 0.08F);
        be.flash *= 0.88F;
    }

    @Override
    public boolean triggerEvent(int id, int param) {
        if (id == EVENT_IMPACT) {
            flash = 1.0F;
            return true;
        }
        return super.triggerEvent(id, param);
    }

    public float getOpen(float pt) { return prevOpen + (open - prevOpen) * pt; }
    public float getFlash(float pt) { return prevFlash + (flash - prevFlash) * pt; }
    public float getDome(float pt) { return prevDome + (dome - prevDome) * pt; }
    public float getPhase() { return phase; }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(RADIUS + 1);
    }

    // ------------------------------------------------------------------ kayıt
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Energy", energy);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energy = tag.getInt("Energy");
    }
}
