package com.krolasyon.mythblades.item;

import com.krolasyon.mythblades.MythBlades;
import com.krolasyon.mythblades.entity.RainbowWaveEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * Gökkuşağı Palası: vuruşları rastgele bir renk etkisi bırakır; yerde sağ tık gökkuşağı hilali fırlatır,
 * havada sağ tık bulut basamağıyla ikinci kez zıplatır. Elde tutarken düşme hasarı alınmaz.
 */
public final class RainbowScimitarItem extends SwordItem {
    public static final String CLOUD = "mythblades_cloud";

    public RainbowScimitarItem() {
        super(MythTier.RAINBOW, 3, -2.2F, new Properties().rarity(Rarity.EPIC));
    }

    /** Yedi renk: kırmızı ateş, turuncu zayıflık, sarı parıltı, yeşil zehir, mavi yavaşlık, çivit körlük, mor havalanma. */
    public static void prism(LivingEntity t, int color, @Nullable Entity src) {
        switch (color % 7) {
            case 0 -> t.setSecondsOnFire(3);
            case 1 -> t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1), src);
            case 2 -> t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 160, 0), src);
            case 3 -> t.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1), src);
            case 4 -> t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2), src);
            case 5 -> t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0), src);
            default -> t.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 25, 1), src);
        }
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean r = super.hurtEnemy(stack, target, attacker);
        if (attacker.level() instanceof ServerLevel sl) {
            prism(target, attacker.getRandom().nextInt(7), attacker);
            sl.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 10, 0.3, 0.3, 0.3, 0.06);
            sl.sendParticles(ParticleTypes.GLOW, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 6, 0.3, 0.3, 0.3, 0.1);
            sl.playSound(null, target.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1F, 1.2F + attacker.getRandom().nextFloat() * 0.5F);
        }
        return r;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean air = !player.onGround() && !player.isInWater() && !player.getAbilities().flying;
        if (air) {
            if (player.getPersistentData().getBoolean(CLOUD)) return InteractionResultHolder.pass(stack);
            player.getPersistentData().putBoolean(CLOUD, true);
            Vec3 look = player.getLookAngle();
            player.setDeltaMovement(player.getDeltaMovement().x * 0.6 + look.x * 0.45, 0.78, player.getDeltaMovement().z * 0.6 + look.z * 0.45);
            player.fallDistance = 0;
            if (level instanceof ServerLevel sl) {
                player.hurtMarked = true;
                sl.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY(), player.getZ(), 24, 0.5, 0.1, 0.5, 0.02);
                sl.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY(), player.getZ(), 8, 0.4, 0.1, 0.4, 0.02);
                level.playSound(null, player.blockPosition(), SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 1.2F, 0.6F);
                level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.6F, 1.8F);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
        if (!level.isClientSide) {
            RainbowWaveEntity w = new RainbowWaveEntity(MythBlades.WAVE.get(), level);
            w.setOwner(player);
            Vec3 look = player.getLookAngle();
            w.moveTo(player.getX() + look.x * 0.8, player.getEyeY() - 0.45 + look.y * 0.8, player.getZ() + look.z * 0.8, player.getYRot(), player.getXRot());
            w.setDeltaMovement(look.scale(1.25));
            level.addFreshEntity(w);
            level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_1, SoundSource.PLAYERS, 0.9F, 1.5F);
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.3F);
            stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
        }
        player.getCooldowns().addCooldown(this, 24);
        player.swing(hand);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity e, int slot, boolean selected) {
        if (e instanceof Player p && (p.onGround() || p.isInWater()) && p.getPersistentData().getBoolean(CLOUD)) p.getPersistentData().remove(CLOUD);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("tooltip.mythblades.rainbow.1").withStyle(ChatFormatting.LIGHT_PURPLE));
        tip.add(Component.translatable("tooltip.mythblades.rainbow.2").withStyle(ChatFormatting.AQUA));
        tip.add(Component.translatable("tooltip.mythblades.rainbow.3").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(com.krolasyon.mythblades.client.ClientHooks.ITEM_EXTENSIONS);
    }
}
