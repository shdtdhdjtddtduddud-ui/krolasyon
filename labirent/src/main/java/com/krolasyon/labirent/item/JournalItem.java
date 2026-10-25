package com.krolasyon.labirent.item;

import com.krolasyon.labirent.maze.MazeManager;
import com.krolasyon.labirent.net.Net;
import com.krolasyon.labirent.story.Objective;
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

/** The Glade journal: the story so far, growing with every chapter. */
public class JournalItem extends Item {
    public JournalItem(Properties p) { super(p); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer sp) {
            MazeManager m = MazeManager.get();
            if (m != null) {
                Net.sendState(sp, m);
                m.story.progress(Objective.READ_JOURNAL, 1);
            }
            Net.open(sp, Net.SCREEN_JOURNAL, 0);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> tip, TooltipFlag f) {
        tip.add(Component.translatable("item.labirent.glade_journal.tip"));
    }
}
