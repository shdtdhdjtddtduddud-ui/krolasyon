package com.rabona.arena.item;

import com.rabona.arena.game.Match;
import com.rabona.arena.game.Team;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

public class BotCardItem extends Item {
    public final Team team;

    public BotCardItem(Properties p, Team team) {
        super(p);
        this.team = team;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (ctx.getLevel() instanceof ServerLevel sl) {
            Match.get(sl.getServer()).spawnBot(sl, team, ctx.getClickLocation());
            if (ctx.getPlayer() == null || !ctx.getPlayer().getAbilities().instabuild) ctx.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(ctx.getLevel().isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack s, Level l, List<Component> tip, TooltipFlag f) {
        tip.add(Component.translatable("tooltip.rabonaarena.bot_card", team.displayName()).withStyle(ChatFormatting.GRAY));
    }
}
