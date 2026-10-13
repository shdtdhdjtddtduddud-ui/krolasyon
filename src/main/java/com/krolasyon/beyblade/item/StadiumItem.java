package com.krolasyon.beyblade.item;

import com.krolasyon.beyblade.entity.StadiumEntity;
import com.krolasyon.beyblade.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Right click a block to set up a beyblade stadium centred on it. */
public class StadiumItem extends Item {
    public StadiumItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos().relative(ctx.getClickedFace());
        if (!level.isClientSide()) {
            Player player = ctx.getPlayer();
            double x = pos.getX() + 0.5, y = pos.getY(), z = pos.getZ() + 0.5;
            if (!level.getEntitiesOfClass(StadiumEntity.class, new net.minecraft.world.phys.AABB(x - 18, y - 4, z - 18, x + 18, y + 4, z + 18)).isEmpty()) {
                if (player != null) player.displayClientMessage(Component.translatable("msg.librabeyblade.stadium_near"), true);
                return InteractionResult.FAIL;
            }
            StadiumEntity st = ModEntities.STADIUM.get().create(level);
            if (st == null) return InteractionResult.FAIL;
            st.moveTo(x, y, z, 0F, 0F);
            level.addFreshEntity(st);
            level.playSound(null, x, y, z, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.6F, 1.4F);
            if (player == null || !player.getAbilities().instabuild) ctx.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("item.librabeyblade.stadium.desc").withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("item.librabeyblade.stadium.desc2").withStyle(ChatFormatting.DARK_GRAY));
    }
}
