package com.krolasyon.vocations.item;

import com.krolasyon.vocations.VocationsMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Stores equipped accessories in the player's persistent data, so they survive relogs and death. */
public final class AccessoryData {
    private static final String KEY = VocationsMod.MODID + "_accessories";

    private AccessoryData() {}

    public static ItemStack get(Player player, AccessorySlot slot) {
        CompoundTag root = player.getPersistentData().getCompound(KEY);
        return root.contains(slot.key(), Tag.TAG_COMPOUND) ? ItemStack.of(root.getCompound(slot.key())) : ItemStack.EMPTY;
    }

    /** Puts the stack into the slot and returns whatever was there before. */
    public static ItemStack equip(Player player, AccessorySlot slot, ItemStack stack) {
        ItemStack previous = get(player, slot);
        root(player).put(slot.key(), stack.save(new CompoundTag()));
        return previous;
    }

    /** Moves every equipped accessory back into the player's inventory. */
    public static void returnAll(Player player) {
        for (AccessorySlot slot : AccessorySlot.values()) {
            ItemStack stack = get(player, slot);
            if (stack.isEmpty()) {
                continue;
            }
            root(player).remove(slot.key());
            giveBack(player, stack);
        }
    }

    public static void giveBack(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    public static void copyOnDeath(Player original, Player replacement) {
        if (original.getPersistentData().contains(KEY, Tag.TAG_COMPOUND)) {
            replacement.getPersistentData().put(KEY, original.getPersistentData().getCompound(KEY).copy());
        }
    }

    private static CompoundTag root(Player player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(KEY, Tag.TAG_COMPOUND)) {
            data.put(KEY, new CompoundTag());
        }
        return data.getCompound(KEY);
    }
}
