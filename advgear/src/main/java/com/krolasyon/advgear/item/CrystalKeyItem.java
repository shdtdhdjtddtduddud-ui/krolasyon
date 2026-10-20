package com.krolasyon.advgear.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Kristal Anahtar: shift + sağ tık bulunduğun yeri "kilitler"; sağ tık basılı tutunca 2 saniye kanalize edip o kapıya geri döner.
 * Aynı boyutta çalışır, 45 saniye bekleme süresi vardır.
 */
public class CrystalKeyItem extends Item {
    public CrystalKeyItem(Properties props) {
        super(props.durability(40));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                CompoundTag t = stack.getOrCreateTag();
                t.putInt("X", player.getBlockX());
                t.putInt("Y", player.getBlockY());
                t.putInt("Z", player.getBlockZ());
                t.putString("Dim", level.dimension().location().toString());
                level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1F, 1.5F);
                ((ServerLevel) level).sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 0.2, player.getZ(), 30, 0.5, 0.1, 0.5, 0.05);
                player.displayClientMessage(Component.translatable("msg.advgear.crystal_bound", player.getBlockX(), player.getBlockY(), player.getBlockZ())
                        .withStyle(ChatFormatting.LIGHT_PURPLE), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        CompoundTag t = stack.getTag();
        if (t == null || !t.contains("Dim")) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable("msg.advgear.crystal_unbound").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(stack);
        }
        if (!t.getString("Dim").equals(level.dimension().location().toString())) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable("msg.advgear.crystal_dim").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 40;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void onUseTick(Level level, LivingEntity e, ItemStack stack, int remaining) {
        if (level.isClientSide) {
            double a = remaining * 0.4;
            for (int i = 0; i < 3; i++) {
                double r = 1.2 * remaining / 40.0 + 0.2;
                level.addParticle(ParticleTypes.END_ROD, e.getX() + Math.cos(a + i * 2.1) * r, e.getY() + (40 - remaining) * 0.05,
                        e.getZ() + Math.sin(a + i * 2.1) * r, 0, 0.01, 0);
            }
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity e) {
        CompoundTag t = stack.getTag();
        if (!level.isClientSide && t != null && e instanceof Player p) {
            BlockPos to = new BlockPos(t.getInt("X"), t.getInt("Y"), t.getInt("Z"));
            ((ServerLevel) level).sendParticles(ParticleTypes.REVERSE_PORTAL, p.getX(), p.getY() + 1, p.getZ(), 60, 0.4, 0.8, 0.4, 0.1);
            p.teleportTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5);
            p.resetFallDistance();
            level.playSound(null, to, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1F, 0.8F);
            ((ServerLevel) level).sendParticles(ParticleTypes.END_ROD, to.getX() + 0.5, to.getY() + 1, to.getZ() + 0.5, 50, 0.4, 0.8, 0.4, 0.08);
            p.getCooldowns().addCooldown(this, 900);
            stack.hurtAndBreak(1, p, pl -> pl.broadcastBreakEvent(p.getUsedItemHand()));
        }
        return stack;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.getTag() != null && stack.getTag().contains("Dim");
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return repair.is(net.minecraft.world.item.Items.AMETHYST_SHARD);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("tooltip.advgear.crystal_key").withStyle(ChatFormatting.AQUA));
        CompoundTag t = stack.getTag();
        if (t != null && t.contains("Dim"))
            tip.add(Component.literal(t.getInt("X") + ", " + t.getInt("Y") + ", " + t.getInt("Z")).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(com.krolasyon.advgear.client.ClientHooks.ITEM_EXTENSIONS);
    }
}
