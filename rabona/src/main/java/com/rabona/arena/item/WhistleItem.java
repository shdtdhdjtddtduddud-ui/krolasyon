package com.rabona.arena.item;

import com.rabona.arena.net.Net;
import com.rabona.arena.net.S2C;
import com.rabona.arena.registry.ModSounds;
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

import java.util.List;

public class WhistleItem extends Item {
    public WhistleItem(Properties p) { super(p); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer sp) {
            level.playSound(null, player.blockPosition(), ModSounds.WHISTLE.get(), SoundSource.PLAYERS, 1f, 1f);
            Net.toPlayer(sp, new S2C.OpenMenu(1));
        }
        player.getCooldowns().addCooldown(this, 10);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack s, Level l, List<Component> tip, TooltipFlag f) {
        tip.add(Component.translatable("tooltip.rabonaarena.whistle"));
    }
}
