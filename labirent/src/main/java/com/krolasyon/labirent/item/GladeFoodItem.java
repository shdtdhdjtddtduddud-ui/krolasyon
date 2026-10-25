package com.krolasyon.labirent.item;

import com.krolasyon.labirent.maze.MazeManager;
import com.krolasyon.labirent.story.Objective;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Meals cooked in the Glade kitchen. */
public class GladeFoodItem extends Item {
    private final boolean meal;

    public GladeFoodItem(Properties p, boolean meal) {
        super(p);
        this.meal = meal;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack s, Level level, LivingEntity e) {
        if (!level.isClientSide && e instanceof Player) {
            MazeManager m = MazeManager.get();
            if (m != null) m.story.progress(Objective.EAT_MEAL, 1);
        }
        return super.finishUsingItem(s, level, e);
    }

    public boolean isMeal() { return meal; }
}
