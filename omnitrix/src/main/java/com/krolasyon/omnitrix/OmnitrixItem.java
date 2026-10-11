package com.krolasyon.omnitrix;

import com.krolasyon.omnitrix.net.Net;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Right click straps the Omnitrix onto your left wrist (it then lives on the player, not in the inventory). */
public class OmnitrixItem extends Item {
    public OmnitrixItem(Properties p) { super(p); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer sp) {
            OmniData d = OmniCapability.get(sp);
            if (d.wearing) {
                sp.displayClientMessage(Component.translatable("msg.omnitrix.already"), true);
                return InteractionResultHolder.fail(stack);
            }
            d.wearing = true;
            if (!sp.getAbilities().instabuild) stack.shrink(1);
            sp.level().playSound(null, sp.blockPosition(), ModRegistry.ACTIVATE.get(), SoundSource.PLAYERS, 1F, 0.8F);
            sp.displayClientMessage(Component.translatable("msg.omnitrix.equipped"), false);
            Net.sync(sp);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("item.omnitrix.omnitrix.tip1").withStyle(ChatFormatting.GREEN));
        tip.add(Component.translatable("item.omnitrix.omnitrix.tip2").withStyle(ChatFormatting.GRAY));
    }
}
