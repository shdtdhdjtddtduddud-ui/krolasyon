package com.krolasyon.bosses.item;

import com.krolasyon.bosses.faction.PlayerData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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

/** A spell book: right click to cast, mana is the price, the book never wears out. */
public class SpellTome extends Item {
    private final String spell;
    private final int mana, cooldown;

    public SpellTome(String id, Properties props, int mana, int cooldown) {
        super(props);
        this.spell = id.substring("tome_".length());
        this.mana = mana;
        this.cooldown = cooldown;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide() || !(player instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
        if (PlayerData.mana(sp) < mana && !sp.isCreative()) {
            sp.displayClientMessage(Component.translatable("msg.no_mana"), true);
            return InteractionResultHolder.fail(stack);
        }
        if (!Magic.cast(spell, sp)) return InteractionResultHolder.fail(stack);
        PlayerData.useMana(sp, mana);
        player.getCooldowns().addCooldown(this, cooldown);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("item.krolasyonbosses.tome_" + spell + ".desc").withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("item.krolasyonbosses.mana", mana).withStyle(ChatFormatting.AQUA));
    }
}
