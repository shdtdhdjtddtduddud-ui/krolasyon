package com.krolasyon.labirent.entity;

import com.krolasyon.labirent.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkHooks;

import java.util.Comparator;
import java.util.List;

/** What is left of a Griever: stays where it fell, twitches and sparks now and then, and can be stripped for parts once. */
public class GrieverCorpseEntity extends Entity {
    private static final EntityDataAccessor<Float> DATA_YAW = SynchedEntityData.defineId(GrieverCorpseEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_STRIPPED = SynchedEntityData.defineId(GrieverCorpseEntity.class, EntityDataSerializers.BOOLEAN);
    public static final int MAX_CORPSES = 40;
    public long born;

    public GrieverCorpseEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_YAW, 0F);
        entityData.define(DATA_STRIPPED, false);
    }

    public void setYBodyRot(float yaw) { entityData.set(DATA_YAW, yaw); }

    public float bodyYaw() { return entityData.get(DATA_YAW); }

    public boolean stripped() { return entityData.get(DATA_STRIPPED); }

    @Override
    public void tick() {
        super.tick();
        if (!onGround()) {
            setDeltaMovement(getDeltaMovement().add(0, -0.04, 0).scale(0.98));
            move(net.minecraft.world.entity.MoverType.SELF, getDeltaMovement());
        }
        if (level().isClientSide) {
            if (!stripped() && random.nextInt(90) == 0)
                for (int k = 0; k < 6; k++)
                    level().addParticle(ParticleTypes.ELECTRIC_SPARK, getX() + (random.nextDouble() - 0.5) * 1.6, getY() + 0.3 + random.nextDouble() * 0.5,
                            getZ() + (random.nextDouble() - 0.5) * 1.6, 0, 0.08, 0);
            if (random.nextInt(140) == 0)
                level().addParticle(ParticleTypes.SMOKE, getX(), getY() + 0.5, getZ(), 0, 0.03, 0);
        } else if (!stripped() && random.nextInt(600) == 0) {
            level().playSound(null, blockPosition(), SoundEvents.ANVIL_STEP, SoundSource.HOSTILE, 0.4F, 0.6F);
        }
    }

    @Override
    public InteractionResult interact(Player p, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (stripped()) {
            p.displayClientMessage(Component.translatable("msg.labirent.corpse_empty"), true);
            return InteractionResult.CONSUME;
        }
        entityData.set(DATA_STRIPPED, true);
        p.getInventory().add(new ItemStack(ModItems.GRIEVER_PLATE.get(), 2 + random.nextInt(2)));
        if (random.nextFloat() < 0.4F) p.getInventory().add(new ItemStack(ModItems.GRIEVER_STINGER.get()));
        if (random.nextFloat() < 0.08F) p.getInventory().add(new ItemStack(ModItems.GRIEVER_CORE.get()));
        ((ServerLevel) level()).sendParticles(ParticleTypes.ITEM_SLIME, getX(), getY() + 0.5, getZ(), 20, 0.6, 0.3, 0.6, 0.1);
        level().playSound(null, blockPosition(), SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.5F, 1.4F);
        p.displayClientMessage(Component.translatable("msg.labirent.corpse_stripped"), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean isPickable() { return true; }

    @Override
    public boolean isAttackable() { return false; }

    /** keeps the number of bodies lying around in check: the oldest one crumbles */
    public static void limit(Level level) {
        AABB all = new AABB(-400, -64, -400, 800, 320, 400);
        List<GrieverCorpseEntity> list = level.getEntitiesOfClass(GrieverCorpseEntity.class, all);
        if (list.size() <= MAX_CORPSES) return;
        list.sort(Comparator.comparingInt(e -> -e.tickCount));
        for (int k = 0; k < list.size() - MAX_CORPSES; k++) list.get(k).discard();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag t) {
        entityData.set(DATA_YAW, t.getFloat("BodyYaw"));
        entityData.set(DATA_STRIPPED, t.getBoolean("Stripped"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag t) {
        t.putFloat("BodyYaw", bodyYaw());
        t.putBoolean("Stripped", stripped());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
