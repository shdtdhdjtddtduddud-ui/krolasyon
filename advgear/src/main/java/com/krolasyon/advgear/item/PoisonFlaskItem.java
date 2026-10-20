package com.krolasyon.advgear.item;

import com.krolasyon.advgear.entity.PoisonFlaskEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
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

/** Kara Ölüm şişesi: fırlatılır, çarptığı yerde zehirli bir bulut bırakır. */
public class PoisonFlaskItem extends Item {
    public PoisonFlaskItem(Properties props) {
        super(props.stacksTo(16));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SPLASH_POTION_THROW, SoundSource.PLAYERS, 0.6F, 0.8F);
        if (!level.isClientSide) {
            PoisonFlaskEntity e = new PoisonFlaskEntity(level, player);
            e.setItem(stack);
            e.shootFromRotation(player, player.getXRot(), player.getYRot(), -12F, 0.8F, 1F);
            level.addFreshEntity(e);
        }
        player.getCooldowns().addCooldown(this, 20);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("tooltip.advgear.black_death").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
