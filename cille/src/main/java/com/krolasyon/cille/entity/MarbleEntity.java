package com.krolasyon.cille.entity;

import com.krolasyon.cille.registry.ModEntities;
import com.krolasyon.cille.registry.ModItems;
import com.krolasyon.cille.registry.ModSounds;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;

/**
 * A thrown Cille marble. It flies, bounces and rolls like a real marble. When two marbles hit each other
 * both burst and the Cilles inside appear facing each other, ready to fight. A marble that rolls to a stop
 * on its own also opens after a moment.
 */
public class MarbleEntity extends Projectile {
    private ItemStack stack = ItemStack.EMPTY;
    private int restTicks;
    private boolean burst;
    public float spin, prevSpin;

    public MarbleEntity(EntityType<? extends MarbleEntity> type, Level level) {
        super(type, level);
    }

    public MarbleEntity(Level level, LivingEntity thrower, ItemStack stack) {
        this(ModEntities.MARBLE.get(), level);
        this.setOwner(thrower);
        this.stack = stack;
        this.setPos(thrower.getX(), thrower.getEyeY() - 0.15, thrower.getZ());
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    public void tick() {
        super.tick();
        prevSpin = spin;
        Vec3 v = this.getDeltaMovement();
        if (!this.isNoGravity()) v = v.add(0, this.isInWater() ? -0.015 : -0.045, 0);
        if (this.isInWater()) v = v.scale(0.85);
        this.setDeltaMovement(v);
        this.move(MoverType.SELF, v);
        Vec3 r = this.getDeltaMovement();
        double nx = v.x, ny = v.y, nz = v.z;
        boolean clack = false;
        if (this.horizontalCollision) {
            if (Math.abs(r.x) < 1.0E-6 && Math.abs(v.x) > 1.0E-4) { nx = -v.x * 0.55; clack = Math.abs(v.x) > 0.08; }
            if (Math.abs(r.z) < 1.0E-6 && Math.abs(v.z) > 1.0E-4) { nz = -v.z * 0.55; clack |= Math.abs(v.z) > 0.08; }
        }
        if (this.verticalCollision) {
            if (v.y < -0.14) { ny = -v.y * 0.45; clack = true; }
            else ny = 0;
        }
        if (this.onGround()) { nx *= 0.9; nz *= 0.9; }
        else { nx *= 0.99; nz *= 0.99; }
        this.setDeltaMovement(nx, ny, nz);
        double speed = Math.sqrt(nx * nx + nz * nz);
        spin += (float) (speed * 90F);

        if (this.level().isClientSide()) {
            if (this.tickCount % 2 == 0)
                this.level().addParticle(KabkabaEntity.dust(0x3A5ADB, 0.7F), getX(), getY() + 0.15, getZ(), 0, 0, 0);
            return;
        }
        if (clack) this.level().playSound(null, getX(), getY(), getZ(), ModSounds.MARBLE_CLACK.get(), SoundSource.PLAYERS, 0.8F, 0.9F + random.nextFloat() * 0.3F);

        if (burst || this.isRemoved()) return;
        // marble duel: two marbles touching
        List<MarbleEntity> others = this.level().getEntitiesOfClass(MarbleEntity.class, this.getBoundingBox().inflate(0.35),
                m -> m != this && !m.burst && !m.isRemoved());
        if (!others.isEmpty()) {
            duel(this, others.get(0));
            return;
        }
        restTicks = this.onGround() && speed < 0.02 ? restTicks + 1 : 0;
        if (restTicks > 24 || this.tickCount > 300 || this.getY() < this.level().getMinBuildHeight()) open(null);
    }

    private static void duel(MarbleEntity a, MarbleEntity b) {
        ServerLevel sl = (ServerLevel) a.level();
        Vec3 mid = a.position().add(b.position()).scale(0.5);
        sl.sendParticles(ParticleTypes.FLASH, mid.x, mid.y + 0.3, mid.z, 2, 0, 0, 0, 0);
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, mid.x, mid.y + 0.5, mid.z, 1, 0, 0, 0, 0);
        sl.sendParticles(ParticleTypes.END_ROD, mid.x, mid.y + 0.3, mid.z, 60, 0.1, 0.1, 0.1, 0.4);
        for (int i = 0; i < 64; i++) {
            double ang = i * Math.PI * 2 / 64;
            sl.sendParticles(KabkabaEntity.dust(i % 2 == 0 ? 0x5C7CFF : KabkabaEntity.VIOLET, 2.5F), mid.x, mid.y + 0.2, mid.z, 0, Math.cos(ang), 0.05, Math.sin(ang), 0.9);
        }
        a.level().playSound(null, mid.x, mid.y, mid.z, ModSounds.MARBLE_CLACK.get(), SoundSource.PLAYERS, 2.0F, 0.7F);
        KabkabaEntity ka = a.open(mid);
        KabkabaEntity kb = b.open(mid);
        if (ka != null && kb != null) {
            if (ka.canFight(kb)) ka.setTarget(kb);
            if (kb.canFight(ka)) kb.setTarget(ka);
        }
    }

    /** bursts the marble and releases the Cille; `away` = point the Cille is pushed away from (duel) */
    public KabkabaEntity open(Vec3 away) {
        if (burst || this.level().isClientSide()) return null;
        burst = true;
        ServerLevel sl = (ServerLevel) this.level();
        Vec3 p = position();
        sl.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(ModItems.KABKABA_MISKETI.get())), p.x, p.y + 0.2, p.z, 30, 0.2, 0.2, 0.2, 0.2);
        sl.sendParticles(ParticleTypes.REVERSE_PORTAL, p.x, p.y + 1.0, p.z, 80, 1.2, 1.4, 1.2, 0.3);
        sl.sendParticles(KabkabaEntity.dust(KabkabaEntity.INK, 3.5F), p.x, p.y + 1.0, p.z, 50, 1.2, 1.2, 1.2, 0);
        sl.sendParticles(ParticleTypes.SQUID_INK, p.x, p.y + 0.5, p.z, 40, 0.8, 0.8, 0.8, 0.1);
        this.level().playSound(null, p.x, p.y, p.z, ModSounds.MARBLE_BURST.get(), SoundSource.PLAYERS, 3.0F, 1.0F);

        KabkabaEntity k = ModEntities.KABKABA.get().create(this.level());
        if (k == null) { this.discard(); return null; }
        Vec3 spot = p;
        float yaw = this.getOwner() != null ? this.getOwner().getYRot() : random.nextFloat() * 360F;
        if (away != null) {
            Vec3 d = p.subtract(away).multiply(1, 0, 1);
            d = d.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : d.normalize();
            spot = p.add(d.scale(2.2));
            yaw = (float) (Mth.atan2(-d.z, -d.x) * (180F / Math.PI)) - 90.0F;   // face the rival
        }
        k.moveTo(spot.x, spot.y, spot.z, yaw, 0F);
        for (int i = 0; i < 4 && !this.level().noCollision(k); i++) k.setPos(k.getX(), k.getY() + 1, k.getZ());
        if (!this.level().noCollision(k)) k.setPos(p.x, p.y, p.z);
        k.yBodyRot = yaw;
        k.yHeadRot = yaw;
        k.fromMarble(stack.isEmpty() ? new ItemStack(ModItems.KABKABA_MISKETI.get()) : stack);
        this.level().addFreshEntity(k);
        this.discard();
        return k;
    }

    @Override
    protected boolean canHitEntity(Entity e) { return false; }

    @Override
    public boolean isPickable() { return false; }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (!stack.isEmpty()) tag.put("Marble", stack.save(new CompoundTag()));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Marble")) stack = ItemStack.of(tag.getCompound("Marble"));
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) { return d < 64 * 64; }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
