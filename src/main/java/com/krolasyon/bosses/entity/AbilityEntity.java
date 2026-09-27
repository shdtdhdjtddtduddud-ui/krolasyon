package com.krolasyon.bosses.entity;

import com.krolasyon.bosses.morph.MorphServer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/** Base for the Aigoar ability effect entities: owned by a player, never saved, no collision. */
public abstract class AbilityEntity extends Entity {
    @Nullable private UUID ownerId;
    @Nullable private Player owner;

    protected AbilityEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    protected void setOwner(Player p) {
        this.owner = p;
        this.ownerId = p.getUUID();
    }

    @Nullable
    public Player owner() {
        if (owner == null && ownerId != null && this.level() instanceof ServerLevel sl) owner = sl.getPlayerByUUID(ownerId);
        return owner != null && owner.isAlive() ? owner : null;
    }

    protected List<LivingEntity> targets(AABB box) {
        Player p = owner();
        if (p == null) return List.of();
        return MorphServer.targets(p, box);
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) { this.discard(); }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public boolean isPickable() { return false; }

    @Override
    public boolean isPushable() { return false; }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) { return d < 128 * 128; }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
