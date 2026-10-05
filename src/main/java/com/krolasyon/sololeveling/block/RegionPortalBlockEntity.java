package com.krolasyon.sololeveling.block;

import com.krolasyon.sololeveling.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Stores where a region portal leads (a {@link com.krolasyon.sololeveling.world.Regions} waypoint id). */
public class RegionPortalBlockEntity extends BlockEntity {
    public String target = "seoul_plaza";

    public RegionPortalBlockEntity(BlockPos pos, BlockState state) { super(ModBlocks.REGION_PORTAL_BE.get(), pos, state); }

    @Override
    protected void saveAdditional(CompoundTag t) {
        super.saveAdditional(t);
        t.putString("Target", target);
    }

    @Override
    public void load(CompoundTag t) {
        super.load(t);
        if (t.contains("Target")) target = t.getString("Target");
    }

    @Override
    public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
