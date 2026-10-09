package com.krolasyon.vocations.item;

import com.krolasyon.vocations.registry.VocationEntities;
import com.krolasyon.vocations.entity.MagicBoltEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Casts a magic bolt. Each cast costs one durability. */
public class StaffItem extends Item {
    private final float damage;
    private final int cooldownTicks;

    public StaffItem(Properties properties, float damage, int cooldownTicks) {
        super(properties.durability(250));
        this.damage = damage;
        this.cooldownTicks = cooldownTicks;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide) {
            MagicBoltEntity bolt = new MagicBoltEntity(VocationEntities.MAGIC_BOLT.get(), player, level, damage);
            bolt.setItem(new ItemStack(this));
            bolt.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.6F, 1.0F);
            level.addFreshEntity(bolt);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLAZE_SHOOT,
                    SoundSource.PLAYERS, 0.8F, 1.2F);
            stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
        }
        player.getCooldowns().addCooldown(this, cooldownTicks);
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
