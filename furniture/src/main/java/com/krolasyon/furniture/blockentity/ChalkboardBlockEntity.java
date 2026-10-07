package com.krolasyon.furniture.blockentity;

import com.krolasyon.furniture.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

public class ChalkboardBlockEntity extends BlockEntity {
    public static final int LINES = 6;
    public static final int MAX_LEN = 22;
    public static final int CHALK_COLOR = 0xE8E4D6;

    private final String[] lines = {"", "", "", "", "", ""};
    private int dye = -1;
    private boolean glow;
    private boolean waxed;

    public ChalkboardBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHALKBOARD.get(), pos, state);
    }

    public String getLine(int i) { return lines[i]; }

    public boolean hasText() {
        for (String l : lines) if (!l.isEmpty()) return true;
        return false;
    }

    public void setLines(String[] in) {
        for (int i = 0; i < LINES; i++) {
            String s = i < in.length && in[i] != null ? in[i] : "";
            lines[i] = s.length() > MAX_LEN ? s.substring(0, MAX_LEN) : s;
        }
        sync();
    }

    public int getTextColor() { return dye < 0 ? CHALK_COLOR : DyeColor.byId(dye).getTextColor(); }

    public void setColor(DyeColor c) { dye = c.getId(); sync(); }

    public boolean isGlow() { return glow; }

    public void setGlow(boolean g) { glow = g; sync(); }

    public boolean isWaxed() { return waxed; }

    public void setWaxed(boolean w) { waxed = w; sync(); }

    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        for (int i = 0; i < LINES; i++) tag.putString("Line" + i, lines[i]);
        tag.putInt("Dye", dye);
        tag.putBoolean("Glow", glow);
        tag.putBoolean("Waxed", waxed);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        for (int i = 0; i < LINES; i++) lines[i] = tag.getString("Line" + i);
        dye = tag.contains("Dye") ? tag.getInt("Dye") : -1;
        glow = tag.getBoolean("Glow");
        waxed = tag.getBoolean("Waxed");
    }

    @Override
    public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    @Override
    public AABB getRenderBoundingBox() { return new AABB(worldPosition).inflate(1.0D, 1.5D, 1.0D); }
}
