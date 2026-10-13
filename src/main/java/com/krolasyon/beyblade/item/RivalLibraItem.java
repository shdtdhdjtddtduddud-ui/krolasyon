package com.krolasyon.beyblade.item;

import com.krolasyon.beyblade.entity.BeybladeEntity;
import com.krolasyon.beyblade.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/** Launches an ownerless rival Libra a few blocks ahead that battles every other beyblade on its own. */
public class RivalLibraItem extends Item {
    public RivalLibraItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            Vec3 look = player.getLookAngle();
            Vec3 flat = new Vec3(look.x, 0, look.z);
            flat = flat.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : flat.normalize();
            Vec3 pos = player.position().add(flat.scale(5)).add(0, 1.5, 0);
            BeybladeEntity.launch(level, null, ItemStack.EMPTY, pos, flat.scale(-0.2).add(0, 0.1, 0), 1F);
            level.playSound(null, pos.x, pos.y, pos.z, ModSounds.LAUNCH.get(), SoundSource.NEUTRAL, 1.2F, 0.8F);
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("item.librabeyblade.rival_libra.desc").withStyle(ChatFormatting.RED));
    }
}
