package com.krolasyon.labirent.item;

import com.krolasyon.labirent.entity.GladerEntity;
import com.krolasyon.labirent.maze.MazeManager;
import com.krolasyon.labirent.net.Net;
import com.krolasyon.labirent.net.ServerHooks;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** The captain's staff: opens the command screen, gives orders to single Gladers, sneak-use whistles everyone over. */
public class CaptainBatonItem extends Item {
    public CaptainBatonItem(Properties p) { super(p); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer sp) {
            MazeManager m = MazeManager.get();
            if (m == null) return InteractionResultHolder.pass(player.getItemInHand(hand));
            if (player.isShiftKeyDown()) {
                if (m.isCaptain(sp) || sp.hasPermissions(2)) {
                    ServerHooks.issue(m, sp, ServerHooks.ORDER_GATHER, "");
                    player.getCooldowns().addCooldown(this, 60);
                } else sp.displayClientMessage(Component.translatable("msg.labirent.not_captain").withStyle(ChatFormatting.RED), true);
            } else {
                Net.sendState(sp, m);
                Net.open(sp, Net.SCREEN_CAPTAIN, 0);
            }
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof GladerEntity g) {
            if (player instanceof ServerPlayer sp) {
                MazeManager m = MazeManager.get();
                if (m != null) Net.sendState(sp, m);
                g.openFor(sp);
            }
            return InteractionResult.sidedSuccess(player.level().isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean isFoil(ItemStack s) { return true; }

    @Override
    public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> tip, TooltipFlag f) {
        tip.add(Component.translatable("item.labirent.captain_baton.tip1").withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("item.labirent.captain_baton.tip2").withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("item.labirent.captain_baton.tip3").withStyle(ChatFormatting.GRAY));
    }
}
