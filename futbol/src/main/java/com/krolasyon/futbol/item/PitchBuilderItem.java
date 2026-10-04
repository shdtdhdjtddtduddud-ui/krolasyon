package com.krolasyon.futbol.item;

import com.krolasyon.futbol.game.MatchManager;
import com.krolasyon.futbol.game.PitchBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

public class PitchBuilderItem extends Item {
    public PitchBuilderItem(Properties props) { super(props); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player p, InteractionHand hand) {
        ItemStack stack = p.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (!p.isShiftKeyDown()) {
            p.displayClientMessage(Component.literal("Stadyumu tam bulunduğun yere (orta nokta) kurmak için Shift + sağ tık. 89x65 alan temizlenir!")
                    .withStyle(ChatFormatting.YELLOW), false);
            return InteractionResultHolder.success(stack);
        }
        MatchManager.stop();
        PitchBuilder.build((ServerLevel) level, p.blockPosition());
        MatchManager.centerBall();
        MatchManager.markDirty();
        level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
        p.displayClientMessage(Component.literal("⚽ Stadyum kuruldu! Takım seçmek ve maç başlatmak için [J] menüsünü aç.").withStyle(ChatFormatting.GREEN), false);
        if (p instanceof ServerPlayer sp) MatchManager.welcome(sp);
        p.getCooldowns().addCooldown(this, 60);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("Shift + sağ tık: tribünlü, ışıklı tam stadyum kurar").withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal("Saha 65x41, kaleler, korner bayrakları ve tribünler").withStyle(ChatFormatting.DARK_GRAY));
    }
}
