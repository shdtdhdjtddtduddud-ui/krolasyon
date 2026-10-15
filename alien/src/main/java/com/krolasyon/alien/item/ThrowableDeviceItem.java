package com.krolasyon.alien.item;

import com.krolasyon.alien.entity.ThrownDeviceEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Noisemaker (beeps and lures the alien) or flare (bright light, mild lure). */
public class ThrowableDeviceItem extends Item {
    private final int kind;

    public ThrowableDeviceItem(Properties p, int kind) {
        super(p);
        this.kind = kind;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack s = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.5F, 0.6F);
        if (!level.isClientSide) {
            ThrownDeviceEntity e = new ThrownDeviceEntity(level, player, kind);
            e.setItem(s);
            e.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 0.9F, 1.0F);
            level.addFreshEntity(e);
        }
        player.getCooldowns().addCooldown(this, 20);
        if (!player.getAbilities().instabuild) s.shrink(1);
        return InteractionResultHolder.sidedSuccess(s, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> tip, TooltipFlag f) {
        tip.add(Component.translatable(kind == ThrownDeviceEntity.NOISEMAKER ? "tooltip.alienisolation.noisemaker" : "tooltip.alienisolation.flare")
                .withStyle(ChatFormatting.GRAY));
    }
}
