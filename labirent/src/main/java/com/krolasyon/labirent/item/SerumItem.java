package com.krolasyon.labirent.item;

import com.krolasyon.labirent.maze.MazeManager;
import com.krolasyon.labirent.registry.ModEffects;
import com.krolasyon.labirent.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Grief Serum: the only cure for a Griever sting. Starts the Changing. */
public class SerumItem extends Item {
    public SerumItem(Properties p) { super(p); }

    public static void cure(LivingEntity target, @Nullable Entity healer) {
        target.removeEffect(ModEffects.VENOM.get());
        target.addEffect(new MobEffectInstance(ModEffects.CHANGING.get(), 20 * 30, 0));
        target.level().playSound(null, target.blockPosition(), ModSounds.SERUM_INJECT.get(), SoundSource.PLAYERS, 1F, 1F);
        if (target.level() instanceof ServerLevel sl)
            sl.sendParticles(ParticleTypes.GLOW, target.getX(), target.getY() + 1, target.getZ(), 30, 0.4, 0.6, 0.4, 0.05);
        MazeManager m = MazeManager.get();
        if (m != null) m.story.onCured(target, healer);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack s = player.getItemInHand(hand);
        if (!player.hasEffect(ModEffects.VENOM.get())) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable("msg.labirent.serum_waste").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(s);
        }
        if (!level.isClientSide) {
            cure(player, player);
            if (!player.isCreative()) s.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(s, level.isClientSide);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack s, Player player, LivingEntity target, InteractionHand hand) {
        if (!target.hasEffect(ModEffects.VENOM.get())) return InteractionResult.PASS;
        if (!player.level().isClientSide) {
            cure(target, player);
            if (!player.isCreative()) s.shrink(1);
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    @Override
    public boolean isFoil(ItemStack s) { return true; }

    @Override
    public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> tip, TooltipFlag f) {
        tip.add(Component.translatable("item.labirent.grief_serum.tip").withStyle(ChatFormatting.GREEN));
    }
}
