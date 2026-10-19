package com.krolasyon.sylvanarchery.item;

import com.krolasyon.sylvanarchery.client.ClientHooks;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Orman Sadağı: 256 oka kadar her tür oku saklar (demet gibi tıklayarak doldurulur/boşaltılır). */
public final class QuiverItem extends Item {
    public static final int CAPACITY = 256;
    private static final String KEY = "Arrows";

    public QuiverItem(Properties props) {
        super(props);
    }

    // ------------------------------------------------------------ içerik
    public static List<ItemStack> contents(ItemStack quiver) {
        List<ItemStack> out = new ArrayList<>();
        CompoundTag tag = quiver.getTag();
        if (tag == null || !tag.contains(KEY, Tag.TAG_LIST)) return out;
        ListTag list = tag.getList(KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            ItemStack s = ItemStack.of(list.getCompound(i));
            if (!s.isEmpty()) out.add(s);
        }
        return out;
    }

    private static void write(ItemStack quiver, List<ItemStack> stacks) {
        ListTag list = new ListTag();
        for (ItemStack s : stacks) if (!s.isEmpty()) list.add(s.save(new CompoundTag()));
        if (list.isEmpty()) quiver.removeTagKey(KEY);
        else quiver.getOrCreateTag().put(KEY, list);
    }

    public static int count(ItemStack quiver) {
        int n = 0;
        for (ItemStack s : contents(quiver)) n += s.getCount();
        return n;
    }

    /** Okları ekler; sığmayan kısmı geri döndürür. */
    public static ItemStack insert(ItemStack quiver, ItemStack arrows) {
        if (arrows.isEmpty() || !(arrows.getItem() instanceof ArrowItem)) return arrows;
        List<ItemStack> list = contents(quiver);
        int room = CAPACITY - count(quiver);
        int move = Math.min(room, arrows.getCount());
        if (move <= 0) return arrows;
        ItemStack rest = arrows.copy();
        rest.shrink(move);
        int left = move;
        for (ItemStack s : list) {
            if (left <= 0) break;
            if (ItemStack.isSameItemSameTags(s, arrows) && s.getCount() < s.getMaxStackSize()) {
                int add = Math.min(left, s.getMaxStackSize() - s.getCount());
                s.grow(add);
                left -= add;
            }
        }
        while (left > 0) {
            ItemStack n = arrows.copyWithCount(Math.min(left, arrows.getMaxStackSize()));
            list.add(n);
            left -= n.getCount();
        }
        write(quiver, list);
        return rest;
    }

    public static ItemStack takeStack(ItemStack quiver) {
        List<ItemStack> list = contents(quiver);
        if (list.isEmpty()) return ItemStack.EMPTY;
        ItemStack s = list.remove(list.size() - 1);
        write(quiver, list);
        return s;
    }

    /** Atılacak ilk ok türünden 1 adet kopya (yoksa boş). */
    public static ItemStack peek(ItemStack quiver) {
        List<ItemStack> list = contents(quiver);
        return list.isEmpty() ? ItemStack.EMPTY : list.get(0).copyWithCount(1);
    }

    public static void consumeOne(ItemStack quiver) {
        List<ItemStack> list = contents(quiver);
        if (list.isEmpty()) return;
        list.get(0).shrink(1);
        write(quiver, list);
    }

    // ------------------------------------------------------------ envanter etkileşimi
    @Override
    public boolean overrideStackedOnOther(ItemStack quiver, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY) return false;
        ItemStack other = slot.getItem();
        if (other.isEmpty()) {
            ItemStack taken = takeStack(quiver);
            if (!taken.isEmpty()) {
                ItemStack rem = slot.safeInsert(taken);
                if (!rem.isEmpty()) insert(quiver, rem);
                player.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8F, 1.0F);
            }
            return true;
        }
        if (other.getItem() instanceof ArrowItem) {
            ItemStack rest = insert(quiver, other);
            if (rest.getCount() != other.getCount()) {
                slot.set(rest);
                player.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 1.0F);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack quiver, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess carried) {
        if (!slot.allowModification(player)) return false;
        if (action == ClickAction.PRIMARY && other.getItem() instanceof ArrowItem) {
            ItemStack rest = insert(quiver, other);
            if (rest.getCount() != other.getCount()) {
                carried.set(rest);
                player.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 1.0F);
            }
            return true;
        }
        if (action == ClickAction.SECONDARY && other.isEmpty()) {
            ItemStack taken = takeStack(quiver);
            if (!taken.isEmpty()) {
                carried.set(taken);
                player.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8F, 1.0F);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return count(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.min(13, Math.round(13F * count(stack) / CAPACITY));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x3FBFA8;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.sylvanarchery.quiver1", count(stack), CAPACITY).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.sylvanarchery.quiver2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.sylvanarchery.quiver3").withStyle(ChatFormatting.DARK_AQUA));
    }

    /** Oyuncunun envanterindeki ilk dolu sadak (yoksa ilk sadak, o da yoksa boş). */
    public static ItemStack find(Player player) {
        ItemStack first = ItemStack.EMPTY;
        for (ItemStack s : player.getInventory().offhand) if (s.getItem() instanceof QuiverItem) { if (count(s) > 0) return s; if (first.isEmpty()) first = s; }
        for (ItemStack s : player.getInventory().items) if (s.getItem() instanceof QuiverItem) { if (count(s) > 0) return s; if (first.isEmpty()) first = s; }
        return first;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(ClientHooks.itemExtensions());
    }
}
