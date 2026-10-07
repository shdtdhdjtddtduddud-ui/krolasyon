package com.krolasyon.furniture.blockentity;

import com.krolasyon.furniture.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** two drawers = 18 slots; the drawers slide out while somebody has it open */
public class NightstandBlockEntity extends RandomizableContainerBlockEntity {
    private NonNullList<ItemStack> items = NonNullList.withSize(18, ItemStack.EMPTY);
    public float open, prevOpen;
    private boolean wantOpen;

    private final ContainerOpenersCounter counter = new ContainerOpenersCounter() {
        @Override
        protected void onOpen(Level level, BlockPos pos, BlockState state) {
            level.playSound(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, SoundEvents.BARREL_OPEN, SoundSource.BLOCKS, 0.6F, 1.25F);
        }

        @Override
        protected void onClose(Level level, BlockPos pos, BlockState state) {
            level.playSound(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, SoundEvents.BARREL_CLOSE, SoundSource.BLOCKS, 0.6F, 1.25F);
        }

        @Override
        protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int oldCount, int newCount) {
            level.blockEvent(pos, state.getBlock(), 1, newCount);
        }

        @Override
        protected boolean isOwnContainer(Player player) {
            return player.containerMenu instanceof ChestMenu m && m.getContainer() == NightstandBlockEntity.this;
        }
    };

    public NightstandBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NIGHTSTAND.get(), pos, state);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, NightstandBlockEntity be) {
        be.prevOpen = be.open;
        if (be.wantOpen) be.open = Math.min(1.0F, be.open + 0.075F);
        else be.open = Math.max(0.0F, be.open - 0.09F);
    }

    @Override
    public boolean triggerEvent(int id, int data) {
        if (id == 1) {
            wantOpen = data > 0;
            return true;
        }
        return super.triggerEvent(id, data);
    }

    @Override
    public int getContainerSize() { return 18; }

    @Override
    protected Component getDefaultName() { return Component.translatable("container.krolasyonfurniture.nightstand"); }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inv) { return ChestMenu.twoRows(id, inv, this); }

    @Override
    protected NonNullList<ItemStack> getItems() { return items; }

    @Override
    protected void setItems(NonNullList<ItemStack> list) { items = list; }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!trySaveLootTable(tag)) ContainerHelper.saveAllItems(tag, items);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        if (!tryLoadLootTable(tag)) ContainerHelper.loadAllItems(tag, items);
    }

    @Override
    public void startOpen(Player player) {
        if (!remove && !player.isSpectator()) counter.incrementOpeners(player, getLevel(), getBlockPos(), getBlockState());
    }

    @Override
    public void stopOpen(Player player) {
        if (!remove && !player.isSpectator()) counter.decrementOpeners(player, getLevel(), getBlockPos(), getBlockState());
    }

    public void recheckOpen() {
        if (!remove) counter.recheckOpeners(getLevel(), getBlockPos(), getBlockState());
    }

    @Override
    public AABB getRenderBoundingBox() { return new AABB(worldPosition).inflate(1.0D, 1.0D, 1.0D); }
}
