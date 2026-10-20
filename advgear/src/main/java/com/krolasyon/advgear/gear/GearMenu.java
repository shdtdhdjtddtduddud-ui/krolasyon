package com.krolasyon.advgear.gear;

import com.krolasyon.advgear.AdvGear;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Kuşanım ekranı: iki yüzük, gözlük, kemer ve sırt yuvası + oyuncu envanteri. */
public class GearMenu extends AbstractContainerMenu {
    /** Yuva konumları (x, y). */
    public static final int[][] POS = {{26, 34}, {26, 56}, {26, 12}, {134, 34}, {134, 12}};
    private final Container gear;

    public GearMenu(int id, Inventory inv) {
        this(id, inv, new SimpleContainer(GearType.SLOTS));
    }

    public GearMenu(int id, Inventory inv, Container gear) {
        super(AdvGear.GEAR_MENU.get(), id);
        this.gear = gear;
        for (int i = 0; i < GearType.SLOTS; i++) {
            final GearType t = GearType.ofSlot(i);
            addSlot(new Slot(gear, i, POS[i][0], POS[i][1]) {
                @Override
                public boolean mayPlace(ItemStack s) {
                    return s.getItem() instanceof GearItem g && g.type == t;
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, 84 + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, 8 + c * 18, 142));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack s = slot.getItem(), copy = s.copy();
        if (index < GearType.SLOTS) {
            if (!moveItemStackTo(s, GearType.SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            boolean moved = false;
            for (int i = 0; i < GearType.SLOTS && !moved; i++) {
                Slot g = slots.get(i);
                if (!g.hasItem() && g.mayPlace(s)) {
                    g.set(s.split(1));
                    moved = true;
                }
            }
            if (!moved) return ItemStack.EMPTY;
        }
        if (s.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
