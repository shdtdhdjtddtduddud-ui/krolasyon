package com.krolasyon.beyblade.compat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * The few Minecraft calls that differ between game versions. This is the 1.20.1 implementation; the ports under
 * {@code versions/} replace this file with their own.
 */
public final class V {
    private V() {}

    public static ResourceLocation rl(String namespace, String path) { return new ResourceLocation(namespace, path); }

    public static ResourceLocation rl(String id) { return new ResourceLocation(id); }

    /** the item's custom data (level, experience, mode), or null; read only */
    @Nullable
    public static CompoundTag tag(ItemStack stack) { return stack.getTag(); }

    /** edits the item's custom data in place */
    public static void editTag(ItemStack stack, Consumer<CompoundTag> edit) { edit.accept(stack.getOrCreateTag()); }

    public static CompoundTag saveStack(Level level, ItemStack stack) { return stack.save(new CompoundTag()); }

    public static ItemStack loadStack(Level level, CompoundTag tag) { return ItemStack.of(tag); }
}
