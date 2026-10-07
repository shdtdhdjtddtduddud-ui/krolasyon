package com.krolasyon.furniture.menu;

import com.krolasyon.furniture.blockentity.WardrobeBlockEntity;
import com.krolasyon.furniture.registry.ModMenus;
import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public class WardrobeMenu extends AbstractContainerMenu {
    public static final int BTN_SWAP = 0;
    private static final ResourceLocation[] ICONS = {InventoryMenu.EMPTY_ARMOR_SLOT_HELMET, InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE,
            InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS, InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS};

    private final Container container;

    public WardrobeMenu(int id, Inventory inv) {
        this(id, inv, new SimpleContainer(WardrobeBlockEntity.SIZE));
    }

    public WardrobeMenu(int id, Inventory inv, Container container) {
        super(ModMenus.WARDROBE.get(), id);
        checkContainerSize(container, WardrobeBlockEntity.SIZE);
        this.container = container;
        container.startOpen(inv.player);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(container, col + row * 9, 8 + col * 18, 44 + row * 18));
            }
        }
        for (int i = 0; i < 4; i++) {
            addSlot(new OutfitSlot(container, WardrobeBlockEntity.STORAGE + i, 8 + i * 18, 18, WardrobeBlockEntity.OUTFIT[i], ICONS[i]));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 112 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 170));
        }
    }

    public Container getContainer() { return container; }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != BTN_SWAP) return false;
        boolean any = false;
        for (int i = 0; i < 4; i++) {
            EquipmentSlot slot = WardrobeBlockEntity.OUTFIT[i];
            ItemStack stored = container.getItem(WardrobeBlockEntity.STORAGE + i);
            ItemStack worn = player.getItemBySlot(slot);
            if (stored.isEmpty() && worn.isEmpty()) continue;
            if (!player.isCreative() && !worn.isEmpty() && EnchantmentHelper.hasBindingCurse(worn)) continue;
            player.setItemSlot(slot, stored.copy());
            container.setItem(WardrobeBlockEntity.STORAGE + i, worn.copy());
            any = true;
        }
        if (any) {
            container.setChanged();
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARMOR_EQUIP_GENERIC, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int own = WardrobeBlockEntity.SIZE;
            if (index < own) {
                if (!moveItemStackTo(stack, own, slots.size(), true)) return ItemStack.EMPTY;
            } else {
                boolean moved = false;
                for (int i = WardrobeBlockEntity.STORAGE; i < own && !moved; i++) {
                    Slot o = slots.get(i);
                    if (!o.hasItem() && o.mayPlace(stack)) {
                        o.setByPlayer(stack.split(1));
                        moved = true;
                    }
                }
                if (!moved && !moveItemStackTo(stack, 0, WardrobeBlockEntity.STORAGE, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) { return container.stillValid(player); }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }

    private static class OutfitSlot extends Slot {
        private final EquipmentSlot type;
        private final ResourceLocation icon;

        OutfitSlot(Container c, int index, int x, int y, EquipmentSlot type, ResourceLocation icon) {
            super(c, index, x, y);
            this.type = type;
            this.icon = icon;
        }

        @Override
        public boolean mayPlace(ItemStack stack) { return stack.canEquip(type, null); }

        @Override
        public int getMaxStackSize() { return 1; }

        @Override
        public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() { return Pair.of(InventoryMenu.BLOCK_ATLAS, icon); }
    }
}
