package com.rabona.arena.item;

import com.rabona.arena.entity.BallEntity;
import com.rabona.arena.registry.ModEntities;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class BallItem extends Item {
    public final int skin;

    public BallItem(Properties p, int skin) {
        super(p);
        this.skin = skin;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (!level.isClientSide) {
            Vec3 at = ctx.getClickLocation();
            BallEntity ball = new BallEntity(ModEntities.BALL.get(), level);
            ball.setSkin(skin);
            ball.moveTo(at.x, at.y + 0.05, at.z, 0, 0);
            level.addFreshEntity(ball);
            if (ctx.getPlayer() == null || !ctx.getPlayer().getAbilities().instabuild) ctx.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            BallEntity ball = new BallEntity(ModEntities.BALL.get(), level);
            ball.setSkin(skin);
            Vec3 look = player.getLookAngle();
            ball.moveTo(player.getX() + look.x, player.getEyeY() - 0.4, player.getZ() + look.z, 0, 0);
            ball.setDeltaMovement(look.scale(0.6).add(0, 0.15, 0));
            level.addFreshEntity(ball);
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack s, Level l, List<Component> tip, TooltipFlag f) {
        tip.add(Component.translatable("tooltip.rabonaarena.ball"));
        tip.add(Component.translatable("tooltip.rabonaarena.ball2"));
    }
}
