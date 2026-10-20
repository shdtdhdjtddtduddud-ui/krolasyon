package com.krolasyon.advgear.gear;

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

/** Kuşanılabilir eşya: shift + sağ tık ile boş yuvaya takılır; G tuşu kuşanım ekranını açar. */
public class GearItem extends Item {
    public final GearType type;

    public GearItem(GearType type, Properties props) {
        super(props.stacksTo(1));
        this.type = type;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown() && type == GearType.BACK) return InteractionResultHolder.pass(stack);
        if (!level.isClientSide) {
            if (GearData.equip(player, stack)) {
                player.setItemInHand(hand, ItemStack.EMPTY);
                level.playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.PLAYERS, 1F, 1.1F);
                player.displayClientMessage(Component.translatable("msg.advgear.equipped", stack.getHoverName()).withStyle(ChatFormatting.GOLD), true);
            } else {
                player.displayClientMessage(Component.translatable("msg.advgear.slot_full").withStyle(ChatFormatting.GRAY), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("tooltip.advgear." + net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(this).getPath())
                .withStyle(ChatFormatting.AQUA));
        tip.add(Component.translatable("tooltip.advgear.slot." + type.name().toLowerCase()).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
