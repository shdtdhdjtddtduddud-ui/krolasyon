package com.krolasyon.sylvanarchery.item;

import com.krolasyon.sylvanarchery.client.ClientHooks;
import com.krolasyon.sylvanarchery.entity.SylvanArrowEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * Orman Bekçisi Yayı: hızlı gerilir (14 tick), %50 güçlü atar, tam gerilince delici rüzgar oku,
 * eğilerek tam gerilince üçlü yaylım; okları önce sadaktan alır (%25 tasarruf).
 */
public final class SylvanBowItem extends BowItem {
    public static final int FULL_DRAW = 14;

    public SylvanBowItem(Properties props) {
        super(props);
    }

    public static float powerFor(float ticks) {
        float f = ticks / FULL_DRAW;
        f = (f * f + f * 2.0F) / 3.0F;
        return Math.min(1.0F, f);
    }

    private record Ammo(ItemStack type, ItemStack quiver, ItemStack loose) {
        boolean isEmpty() { return type.isEmpty(); }
    }

    private static Ammo findAmmo(Player player, ItemStack bow) {
        ItemStack quiver = QuiverItem.find(player);
        if (!quiver.isEmpty() && QuiverItem.count(quiver) > 0) return new Ammo(QuiverItem.peek(quiver), quiver, ItemStack.EMPTY);
        ItemStack loose = player.getProjectile(bow);
        if (!loose.isEmpty()) return new Ammo(loose, ItemStack.EMPTY, loose);
        return new Ammo(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack bow = player.getItemInHand(hand);
        if (!player.getAbilities().instabuild && findAmmo(player, bow).isEmpty()) return InteractionResultHolder.fail(bow);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(bow);
    }

    @Override
    public void releaseUsing(ItemStack bow, Level level, LivingEntity user, int timeLeft) {
        if (!(user instanceof Player player)) return;
        int charge = getUseDuration(bow) - timeLeft;
        float power = powerFor(charge);
        if (power < 0.1F) return;
        Ammo ammo = findAmmo(player, bow);
        boolean creative = player.getAbilities().instabuild;
        ItemStack type = ammo.isEmpty() ? new ItemStack(Items.ARROW) : ammo.type();
        if (ammo.isEmpty() && !creative) return;
        boolean infinite = creative || (type.is(Items.ARROW) && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.INFINITY_ARROWS, bow) > 0);
        boolean full = power >= 1.0F;
        boolean volley = full && player.isShiftKeyDown();

        if (!level.isClientSide) {
            ArrowItem arrowItem = type.getItem() instanceof ArrowItem a ? a : (ArrowItem) Items.ARROW;
            float[] offsets = volley ? new float[]{-9F, 0F, 9F} : new float[]{0F};
            for (int i = 0; i < offsets.length; i++) {
                AbstractArrow arrow = arrowItem.createArrow(level, type, player);
                arrow = customArrow(arrow);
                arrow.shootFromRotation(player, player.getXRot(), player.getYRot() + offsets[i], 0.0F, power * 3.4F, 0.8F);
                arrow.setBaseDamage(arrow.getBaseDamage() * 1.5 + 0.5);
                if (full) {
                    arrow.setCritArrow(true);
                    arrow.setPierceLevel((byte) 2);
                    if (arrow instanceof SylvanArrowEntity s) s.setWind(true);
                }
                int pw = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS, bow);
                if (pw > 0) arrow.setBaseDamage(arrow.getBaseDamage() + pw * 0.5 + 0.5);
                int punch = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PUNCH_ARROWS, bow);
                if (punch > 0) arrow.setKnockback(punch);
                if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FLAMING_ARROWS, bow) > 0) arrow.setSecondsOnFire(100);
                if (infinite || i > 0) arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                level.addFreshEntity(arrow);
            }
            bow.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(player.getUsedItemHand()));
            if (!infinite) {
                if (!ammo.quiver().isEmpty()) {
                    if (level.random.nextFloat() >= 0.25F) QuiverItem.consumeOne(ammo.quiver());
                } else if (!ammo.loose().isEmpty()) {
                    ammo.loose().shrink(1);
                    if (ammo.loose().isEmpty()) player.getInventory().removeItem(ammo.loose());
                }
            }
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS,
                1.0F, 1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + power * 0.5F);
        if (full) level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2F, 1.5F);
        player.awardStat(Stats.ITEM_USED.get(this));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.sylvanarchery.bow1").withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.translatable("tooltip.sylvanarchery.bow2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.sylvanarchery.bow3").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.sylvanarchery.bow4").withStyle(ChatFormatting.DARK_AQUA));
    }

    @Override
    public int getEnchantmentValue() {
        return 18;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(ClientHooks.itemExtensions());
    }
}
