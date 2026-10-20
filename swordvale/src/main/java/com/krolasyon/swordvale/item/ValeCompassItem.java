package com.krolasyon.swordvale.item;

import com.krolasyon.swordvale.world.ValeLayout;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Vadi Pusulası: en yakın Kılıçlar Vadisi'nin merkezini gösterir. */
public final class ValeCompassItem extends Item {
    public ValeCompassItem(Properties props) {
        super(props);
    }

    public static void search(ServerLevel level, ItemStack stack, Entity holder) {
        ValeLayout.Vale v = ValeLayout.nearest(level.getSeed(), level.getChunkSource().getGenerator(), level.getChunkSource().randomState(),
                holder.getBlockX(), holder.getBlockZ(), 4);
        CompoundTag tag = stack.getOrCreateTag();
        if (v == null) {
            tag.remove("ValeX");
            tag.putBoolean("ValeNone", true);
        } else {
            tag.putInt("ValeX", v.cx());
            tag.putInt("ValeZ", v.cz());
            tag.remove("ValeNone");
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel sl && level.dimension() == Level.OVERWORLD) {
            search(sl, stack, player);
            level.playSound(null, player.blockPosition(), SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.PLAYERS, 1.0F, 1.0F);
            if (stack.getTag() != null && stack.getTag().contains("ValeX")) {
                int dx = stack.getTag().getInt("ValeX") - player.getBlockX(), dz = stack.getTag().getInt("ValeZ") - player.getBlockZ();
                player.displayClientMessage(Component.translatable("tooltip.swordvale.compass_target", stack.getTag().getInt("ValeX"),
                        stack.getTag().getInt("ValeZ"), (int) Math.sqrt((double) dx * dx + (double) dz * dz)).withStyle(ChatFormatting.GREEN), true);
            } else {
                player.displayClientMessage(Component.translatable("tooltip.swordvale.compass_none").withStyle(ChatFormatting.GRAY), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level instanceof ServerLevel sl && level.dimension() == Level.OVERWORLD && level.getGameTime() % 200 == 0
                && (stack.getTag() == null || (!stack.getTag().contains("ValeX") && !stack.getTag().contains("ValeNone")))) {
            search(sl, stack, entity);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.swordvale.compass").withStyle(ChatFormatting.GRAY));
        CompoundTag t = stack.getTag();
        if (t != null && t.contains("ValeX")) {
            tooltip.add(Component.literal(t.getInt("ValeX") + ", " + t.getInt("ValeZ")).withStyle(ChatFormatting.GREEN));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
