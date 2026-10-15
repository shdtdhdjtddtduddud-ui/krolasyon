package com.krolasyon.alien.item;

import com.krolasyon.alien.ai.NoiseSystem;
import com.krolasyon.alien.entity.XenomorphEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** The radar itself is drawn by the client HUD while held. The beeping can be heard by the alien when it is close. */
public class MotionTrackerItem extends Item {
    public MotionTrackerItem(Properties p) { super(p); }

    @Override
    public void inventoryTick(ItemStack s, Level level, Entity e, int slot, boolean selected) {
        if (level.isClientSide || !(e instanceof Player p) || e.tickCount % 20 != 0) return;
        if (!selected && p.getOffhandItem() != s) return;
        if (!level.getEntitiesOfClass(XenomorphEntity.class, p.getBoundingBox().inflate(9), x -> !x.isHidden()).isEmpty())
            NoiseSystem.emit((ServerLevel) level, p.position(), 5, NoiseSystem.Kind.TRACKER, p);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack a, ItemStack b, boolean changed) { return changed; }

    @Override
    public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> tip, TooltipFlag f) {
        tip.add(Component.translatable("tooltip.alienisolation.tracker").withStyle(ChatFormatting.GRAY));
    }
}
