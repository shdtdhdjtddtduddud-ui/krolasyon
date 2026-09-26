package com.krolasyon.bosses.item;

import com.krolasyon.bosses.entity.BossEntity;
import com.krolasyon.bosses.entity.HeartDemonEntity;
import com.krolasyon.bosses.form.DemonForm;
import com.krolasyon.bosses.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Kalp Kırıcı Kılıcı — hold right click to plunge the blade into your heart and become the Heartbreaker Demon.
 * Shift + right click while transformed returns to human form.
 */
public class HeartbreakerBladeItem extends SwordItem {
    public static final int CHARGE_TICKS = 24;

    public HeartbreakerBladeItem(Properties props) {
        super(Tiers.NETHERITE, 5, -2.4F, props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (DemonForm.isDemon(player)) {
            // only a deliberate Shift + right click returns to human form; a held right click must never undo the transformation
            if (!player.isShiftKeyDown()) return InteractionResultHolder.pass(stack);
            if (!level.isClientSide() && player instanceof ServerPlayer sp) DemonForm.revert(sp, true);
            player.getCooldowns().addCooldown(this, 60);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        player.startUsingItem(hand);
        if (!level.isClientSide()) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TRANSFORM_CHARGE.get(), SoundSource.PLAYERS, 1.6F, 1.0F);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack) { return CHARGE_TICKS; }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.SPEAR; }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        if (!(level instanceof ServerLevel sl)) return;
        int t = CHARGE_TICKS - remaining;
        float k = t / (float) CHARGE_TICKS;
        // dark energy spiralling into the blade
        for (int i = 0; i < 3; i++) {
            double a = t * 0.55 + i * Math.PI * 2 / 3;
            double r = 2.6 * (1 - k) + 0.3;
            sl.sendParticles(BossEntity.dust(i == 0 ? HeartDemonEntity.CRIMSON : HeartDemonEntity.SHADOW, 1.4F),
                    user.getX() + Math.cos(a) * r, user.getY() + 0.2 + k * 1.8, user.getZ() + Math.sin(a) * r, 1, 0, 0, 0, 0);
        }
        if (t % 4 == 0) {
            sl.sendParticles(BossEntity.dust(HeartDemonEntity.HEART_PINK, 1.0F), user.getX(), user.getY() + 1.3, user.getZ(), 3, 0.2, 0.2, 0.2, 0);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (user instanceof Player p) p.getCooldowns().addCooldown(this, 40);
        if (!level.isClientSide() && user instanceof ServerPlayer sp) DemonForm.transform(sp);
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.krolasyonbosses.heartbreaker_blade.desc1").withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("item.krolasyonbosses.heartbreaker_blade.desc2").withStyle(ChatFormatting.GRAY));
        for (int i = 0; i < DemonForm.COUNT; i++) {
            tooltip.add(Component.translatable("ability.krolasyonbosses." + DemonForm.NAMES[i]).withStyle(ChatFormatting.DARK_RED));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) { return false; }
}
