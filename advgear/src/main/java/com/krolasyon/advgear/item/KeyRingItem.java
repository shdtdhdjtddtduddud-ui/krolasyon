package com.krolasyon.advgear.item;

import com.krolasyon.advgear.block.LockType;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/** Anahtar Halkası: 12 anahtara kadar taşır (envanterde anahtarı halkanın üstüne bırak); içindeki her anahtar gibi çalışır. */
public class KeyRingItem extends Item {
    public static final int CAPACITY = 12;

    public KeyRingItem(Properties props) {
        super(props.stacksTo(1));
    }

    public static List<ItemStack> keys(ItemStack ring) {
        List<ItemStack> out = new ArrayList<>();
        CompoundTag t = ring.getTag();
        if (t == null) return out;
        ListTag l = t.getList("Keys", Tag.TAG_COMPOUND);
        for (int i = 0; i < l.size(); i++) out.add(ItemStack.of(l.getCompound(i)));
        return out;
    }

    private static void write(ItemStack ring, List<ItemStack> keys) {
        ListTag l = new ListTag();
        for (ItemStack k : keys) if (!k.isEmpty()) l.add(k.save(new CompoundTag()));
        ring.getOrCreateTag().put("Keys", l);
    }

    private static boolean accepts(ItemStack s) {
        return s.getItem() instanceof KeyItem || s.getItem() instanceof MechanicKeyItem || s.getItem() instanceof LockpickItem
                || s.getItem() instanceof CrystalKeyItem;
    }

    private static boolean add(ItemStack ring, ItemStack key) {
        List<ItemStack> keys = keys(ring);
        if (keys.size() >= CAPACITY || !accepts(key)) return false;
        keys.add(key.split(1));
        write(ring, keys);
        return true;
    }

    /** Uygun anahtarı halkadan harcar. */
    public static boolean consume(ItemStack ring, LockType lock) {
        List<ItemStack> keys = keys(ring);
        for (int i = 0; i < keys.size(); i++) {
            if (keys.get(i).getItem() instanceof KeyItem k && k.lock == lock) {
                keys.remove(i);
                write(ring, keys);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack ring, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY) return false;
        ItemStack other = slot.getItem();
        if (other.isEmpty() || !accepts(other)) return false;
        if (add(ring, other)) player.playSound(SoundEvents.CHAIN_PLACE, 0.8F, 1.4F);
        return true;
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack ring, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess access) {
        if (action != ClickAction.SECONDARY || !slot.allowModification(player)) return false;
        if (other.isEmpty() || !accepts(other)) return false;
        if (add(ring, other)) player.playSound(SoundEvents.CHAIN_PLACE, 0.8F, 1.4F);
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack ring = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) return InteractionResultHolder.pass(ring);
        if (!level.isClientSide) {
            List<ItemStack> keys = keys(ring);
            for (ItemStack k : keys) if (!player.getInventory().add(k)) player.drop(k, false);
            write(ring, List.of());
            level.playSound(null, player.blockPosition(), SoundEvents.CHAIN_BREAK, SoundSource.PLAYERS, 0.8F, 1.3F);
        }
        return InteractionResultHolder.sidedSuccess(ring, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("tooltip.advgear.key_ring").withStyle(ChatFormatting.AQUA));
        List<ItemStack> keys = keys(stack);
        tip.add(Component.translatable("tooltip.advgear.key_ring_count", keys.size(), CAPACITY).withStyle(ChatFormatting.GRAY));
        for (ItemStack k : keys) tip.add(Component.literal("  • ").append(k.getHoverName()).withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(com.krolasyon.advgear.client.ClientHooks.ITEM_EXTENSIONS);
    }
}
