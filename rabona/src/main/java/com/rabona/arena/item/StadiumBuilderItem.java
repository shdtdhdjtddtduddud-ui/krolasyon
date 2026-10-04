package com.rabona.arena.item;

import com.rabona.arena.game.StadiumBuilder;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

public class StadiumBuilderItem extends Item {
    public StadiumBuilderItem(Properties p) { super(p); }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (ctx.getLevel() instanceof ServerLevel sl && ctx.getPlayer() instanceof ServerPlayer sp) {
            Direction d = sp.getDirection();
            StadiumBuilder.build(sl, ctx.getClickedPos(), d.getAxis() == Direction.Axis.X, sp);
        }
        return InteractionResult.sidedSuccess(ctx.getLevel().isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack s, Level l, List<Component> tip, TooltipFlag f) {
        tip.add(Component.translatable("tooltip.rabonaarena.stadium_builder"));
        tip.add(Component.translatable("tooltip.rabonaarena.stadium_builder2"));
    }
}
