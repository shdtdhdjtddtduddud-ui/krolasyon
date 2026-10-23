package com.krolasyon.realweather.item;

import com.krolasyon.realweather.WeatherServer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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

/** Barometre: şu anki havayı, rüzgârı ve yaklaşan cepheyi söyler. */
public final class BarometerItem extends Item {
    public BarometerItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player p, InteractionHand hand) {
        if (level instanceof ServerLevel sl) {
            p.displayClientMessage(WeatherServer.report(WeatherServer.data(sl)), false);
            sl.playSound(null, p.blockPosition(), SoundEvents.SPYGLASS_USE, SoundSource.PLAYERS, 0.6F, 1.4F);
        }
        p.getCooldowns().addCooldown(this, 20);
        return InteractionResultHolder.sidedSuccess(p.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("tooltip.realweather.barometer").withStyle(ChatFormatting.GRAY));
    }
}
