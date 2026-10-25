package com.krolasyon.labirent.item;

import com.krolasyon.labirent.maze.MazeManager;
import com.krolasyon.labirent.net.Net;
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

/** A runner's copy of the Map Room map, updated live as anyone explores. */
public class RunnerMapItem extends Item {
    public RunnerMapItem(Properties p) { super(p); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer sp) {
            MazeManager m = MazeManager.get();
            if (m != null) Net.sendState(sp, m);
            Net.open(sp, Net.SCREEN_MAP, 0);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> tip, TooltipFlag f) {
        tip.add(Component.translatable("item.labirent.runner_map.tip"));
    }
}
