package com.krolasyon.labirent.item;

import com.krolasyon.labirent.maze.MazeManager;
import com.krolasyon.labirent.story.Objective;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** The pulsing device inside every Griever. It is what lets them pass through the Hole. */
public class GrieverCoreItem extends Item {
    public GrieverCoreItem(Properties p) { super(p); }

    @Override
    public void inventoryTick(ItemStack s, Level level, Entity e, int slot, boolean selected) {
        if (!level.isClientSide && level.getGameTime() % 40 == 0) {
            MazeManager m = MazeManager.get();
            if (m != null) m.story.progress(Objective.GET_CORE, 1);
        }
    }

    @Override
    public boolean isFoil(ItemStack s) { return true; }

    @Override
    public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> tip, TooltipFlag f) {
        tip.add(Component.translatable("item.labirent.griever_core.tip").withStyle(ChatFormatting.DARK_AQUA));
    }
}
