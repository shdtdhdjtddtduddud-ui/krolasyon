package com.krolasyon.storm;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Opens the skill tree; carried anywhere in the hotbar/offhand it lowers energy costs by 20%. */
public class StormTomeItem extends Item {
    public StormTomeItem(Properties p) { super(p); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> com.krolasyon.storm.client.ClientHooks::openTree);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public boolean isFoil(ItemStack stack) { return true; }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("item.stormtree.storm_tome.tip1").withStyle(ChatFormatting.AQUA));
        tip.add(Component.translatable("item.stormtree.storm_tome.tip2").withStyle(ChatFormatting.GRAY));
    }

    public static boolean carries(Player p) {
        if (p.getOffhandItem().getItem() instanceof StormTomeItem) return true;
        for (int i = 0; i < 9; i++) if (p.getInventory().getItem(i).getItem() instanceof StormTomeItem) return true;
        return false;
    }
}
