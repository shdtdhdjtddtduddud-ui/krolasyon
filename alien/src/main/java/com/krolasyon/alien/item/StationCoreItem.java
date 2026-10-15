package com.krolasyon.alien.item;

import com.krolasyon.alien.world.StationGenerator;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Builds a complete Sevastopol section around the player and releases the alien in it. */
public class StationCoreItem extends Item {
    public StationCoreItem(Properties p) { super(p); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack s = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            StationGenerator.generate((ServerLevel) level, sp, level.random.nextLong());
            if (!player.getAbilities().instabuild) s.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(s, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> tip, TooltipFlag f) {
        tip.add(Component.translatable("tooltip.alienisolation.station_core").withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("tooltip.alienisolation.station_core2").withStyle(ChatFormatting.RED));
    }
}
