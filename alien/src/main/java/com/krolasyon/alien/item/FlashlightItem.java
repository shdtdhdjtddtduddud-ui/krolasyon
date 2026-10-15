package com.krolasyon.alien.item;

import com.krolasyon.alien.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class FlashlightItem extends Item {
    public FlashlightItem(Properties p) { super(p); }

    public static boolean isOn(ItemStack s) { return s.hasTag() && s.getTag().getBoolean("On"); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack s = player.getItemInHand(hand);
        boolean on = !isOn(s);
        s.getOrCreateTag().putBoolean("On", on);
        level.playSound(null, player.blockPosition(), ModSounds.FLASHLIGHT.get(), SoundSource.PLAYERS, 0.6F, on ? 1.1F : 0.9F);
        return InteractionResultHolder.sidedSuccess(s, level.isClientSide);
    }

    @Override
    public boolean isFoil(ItemStack s) { return isOn(s); }

    @Override
    public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> tip, TooltipFlag f) {
        tip.add(Component.translatable(isOn(s) ? "tooltip.alienisolation.on" : "tooltip.alienisolation.off").withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("tooltip.alienisolation.flashlight").withStyle(ChatFormatting.DARK_GRAY));
    }
}
