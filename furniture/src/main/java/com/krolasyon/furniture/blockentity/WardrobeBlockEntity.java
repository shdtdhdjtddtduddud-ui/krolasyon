package com.krolasyon.furniture.blockentity;

import com.krolasyon.furniture.menu.WardrobeMenu;
import com.krolasyon.furniture.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** 27 storage slots + a 4-piece outfit (head/chest/legs/feet) that can be swapped with what the player wears */
public class WardrobeBlockEntity extends RandomizableContainerBlockEntity {
    public static final int STORAGE = 27;
    public static final int SIZE = 31;
    public static final EquipmentSlot[] OUTFIT = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    public float open, prevOpen;
    private boolean wantOpen;

    private final ContainerOpenersCounter counter = new ContainerOpenersCounter() {
        @Override
        protected void onOpen(Level level, BlockPos pos, BlockState state) {
            level.playSound(null, pos.getX() + 0.5D, pos.getY() + 0.8D, pos.getZ() + 0.5D, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.5F, 0.8F);
        }

        @Override
        protected void onClose(Level level, BlockPos pos, BlockState state) {
            level.playSound(null, pos.getX() + 0.5D, pos.getY() + 0.8D, pos.getZ() + 0.5D, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.5F, 0.8F);
        }

        @Override
        protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int oldCount, int newCount) {
            level.blockEvent(pos, state.getBlock(), 1, newCount);
        }

        @Override
        protected boolean isOwnContainer(Player player) {
            return player.containerMenu instanceof WardrobeMenu m && m.getContainer() == WardrobeBlockEntity.this;
        }
    };

    public WardrobeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WARDROBE.get(), pos, state);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, WardrobeBlockEntity be) {
        be.prevOpen = be.open;
        if (be.wantOpen) be.open = Math.min(1.0F, be.open + 0.06F);
        else be.open = Math.max(0.0F, be.open - 0.075F);
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
    public int getContainerSize() { return SIZE; }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot < STORAGE) return true;
        return stack.canEquip(OUTFIT[slot - STORAGE], null);
    }

    @Override
    protected Component getDefaultName() { return Component.translatable("container.krolasyonfurniture.wardrobe"); }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inv) { return new WardrobeMenu(id, inv, this); }

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
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
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
    public AABB getRenderBoundingBox() { return new AABB(worldPosition).inflate(1.5D, 1.5D, 1.5D); }
}
