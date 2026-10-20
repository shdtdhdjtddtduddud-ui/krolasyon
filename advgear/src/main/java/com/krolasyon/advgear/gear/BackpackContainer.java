package com.krolasyon.advgear.gear;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

/** Çantanın içeriği, çanta eşyasının NBT'sinde saklanır. */
public class BackpackContainer extends SimpleContainer {
    private final ItemStack bag;
    private boolean loading;

    public BackpackContainer(ItemStack bag, int size) {
        super(size);
        this.bag = bag;
        loading = true;
        CompoundTag tag = bag.getTag();
        if (tag != null) {
            ListTag list = tag.getList("Items", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag t = list.getCompound(i);
                int slot = t.getByte("Slot") & 255;
                if (slot < size) setItem(slot, ItemStack.of(t));
            }
        }
        loading = false;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (loading) return;
        ListTag list = new ListTag();
        for (int i = 0; i < getContainerSize(); i++) {
            ItemStack s = getItem(i);
            if (s.isEmpty()) continue;
            CompoundTag t = s.save(new CompoundTag());
            t.putByte("Slot", (byte) i);
            list.add(t);
        }
        bag.getOrCreateTag().put("Items", list);
    }

    public static int count(ItemStack bag) {
        CompoundTag tag = bag.getTag();
        return tag == null ? 0 : tag.getList("Items", Tag.TAG_COMPOUND).size();
    }
}
