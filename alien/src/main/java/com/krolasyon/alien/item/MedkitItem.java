package com.krolasyon.alien.item;

import com.krolasyon.alien.ai.NoiseSystem;
import com.krolasyon.alien.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class MedkitItem extends Item {
    public MedkitItem(Properties p) { super(p); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player.getHealth() >= player.getMaxHealth()) return InteractionResultHolder.fail(player.getItemInHand(hand));
        player.startUsingItem(hand);
        level.playSound(null, player.blockPosition(), ModSounds.MEDKIT.get(), SoundSource.PLAYERS, 0.7F, 1.0F);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack s, Level level, LivingEntity user) {
        if (!level.isClientSide) {
            user.heal(8.0F);
            user.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
            NoiseSystem.emit((ServerLevel) level, user.position(), 4, NoiseSystem.Kind.DEVICE, user);
            if (!(user instanceof Player p && p.getAbilities().instabuild)) s.shrink(1);
        }
        return s;
    }

    @Override
    public int getUseDuration(ItemStack s) { return 40; }

    @Override
    public UseAnim getUseAnimation(ItemStack s) { return UseAnim.BOW; }
}
