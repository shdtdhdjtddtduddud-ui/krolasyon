package com.krolasyon.advgear.gear;

import com.krolasyon.advgear.item.BackpackItem;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Sırt çantası içeriği: sandık arayüzü, ama çantanın içine çanta konamaz ve açık çanta yerinden oynatılamaz. */
public class BackpackMenu extends ChestMenu {
    private final int lockedSlot;
    private final Runnable onClose;

    public BackpackMenu(MenuType<?> type, int id, Inventory inv, int rows) {
        this(type, id, inv, new SimpleContainer(rows * 9), rows, -1, null);
    }

    public BackpackMenu(MenuType<?> type, int id, Inventory inv, Container c, int rows, int lockedSlot, Runnable onClose) {
        super(type, id, inv, c, rows);
        this.lockedSlot = lockedSlot;
        this.onClose = onClose;
        for (int i = 0; i < rows * 9; i++) {
            Slot old = slots.get(i);
            Slot n = new Slot(c, i, old.x, old.y) {
                @Override
                public boolean mayPlace(ItemStack s) {
                    return !(s.getItem() instanceof BackpackItem) && !(s.getItem() instanceof net.minecraft.world.item.BlockItem bi
                            && bi.getBlock() instanceof net.minecraft.world.level.block.ShulkerBoxBlock);
                }
            };
            n.index = i;
            slots.set(i, n);
        }
    }

    @Override
    public void clicked(int slotId, int button, ClickType type, Player player) {
        if (lockedSlot >= 0) {
            if (type == ClickType.SWAP && button == lockedSlot) return;
            if (slotId >= 0 && slotId < slots.size()) {
                Slot s = slots.get(slotId);
                if (s.container == player.getInventory() && s.getContainerSlot() == lockedSlot) return;
            }
        }
        super.clicked(slotId, button, type, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot s = slots.get(index);
        if (index >= getRowCount() * 9 && s.getItem().getItem() instanceof BackpackItem) return ItemStack.EMPTY;
        return super.quickMoveStack(player, index);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (onClose != null) onClose.run();
    }
}
