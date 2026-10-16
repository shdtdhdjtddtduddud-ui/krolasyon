package com.krolasyon.beyblade.compat;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.function.Consumer;

/** Version dependent calls, Minecraft 1.21.1 implementation (item data lives in the custom_data component). */
public final class V {
    private V() {}

    public static ResourceLocation rl(String namespace, String path) { return ResourceLocation.fromNamespaceAndPath(namespace, path); }

    public static ResourceLocation rl(String id) { return ResourceLocation.parse(id); }

    @Nullable
    public static CompoundTag tag(ItemStack stack) {
        CustomData d = stack.get(DataComponents.CUSTOM_DATA);
        return d == null ? null : d.copyTag();
    }

    public static void editTag(ItemStack stack, Consumer<CompoundTag> edit) { CustomData.update(DataComponents.CUSTOM_DATA, stack, edit); }

    public static CompoundTag saveStack(Level level, ItemStack stack) { return (CompoundTag) stack.save(level.registryAccess()); }

    public static ItemStack loadStack(Level level, CompoundTag tag) { return ItemStack.parseOptional(level.registryAccess(), tag); }
}
