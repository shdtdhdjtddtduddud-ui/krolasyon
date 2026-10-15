package com.krolasyon.ben10;

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

public class OmnitrixItem extends Item {
    public OmnitrixItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
            if (Transform.equip(sp)) {
                if (!sp.getAbilities().instabuild) stack.shrink(1);
            }
        } else if (OmniData.get(player).worn) {
            // already wearing one: raise the arm and open the dial
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.krolasyon.ben10.client.WatchMode.open());
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        for (int i = 1; i <= 5; i++) tip.add(Component.translatable("item.krolben10.omnitrix.tip" + i).withStyle(i == 1 ? ChatFormatting.GREEN : ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return false;
    }
}
