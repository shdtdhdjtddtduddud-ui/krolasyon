package com.krolasyon.futbol.item;

import com.krolasyon.futbol.entity.FootballEntity;
import com.krolasyon.futbol.registry.ModEntities;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
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

import javax.annotation.Nullable;
import java.util.List;

public class FootballItem extends Item {
    public FootballItem(Properties props) { super(props); }

    private static void spawn(Level level, Vec3 at, Player p, ItemStack stack) {
        FootballEntity b = ModEntities.BALL.get().create(level);
        if (b == null) return;
        b.moveTo(at.x, at.y, at.z);
        level.addFreshEntity(b);
        if (p == null || !p.getAbilities().instabuild) stack.shrink(1);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (!level.isClientSide) {
            Vec3 at = ctx.getClickLocation();
            spawn(level, new Vec3(at.x, at.y + 0.05, at.z), ctx.getPlayer(), ctx.getItemInHand());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player p, InteractionHand hand) {
        ItemStack stack = p.getItemInHand(hand);
        if (!level.isClientSide) {
            Vec3 f = p.getLookAngle().multiply(1, 0, 1).normalize();
            spawn(level, p.position().add(f.scale(1.2)).add(0, 0.6, 0), p, stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("Yere koymak için sağ tıkla").withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal("Topa vurmak: sol tık • Geri almak: Shift + sağ tık").withStyle(ChatFormatting.DARK_GRAY));
    }
}
