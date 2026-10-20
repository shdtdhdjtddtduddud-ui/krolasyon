package com.krolasyon.advgear.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

/** İçilebilir iksir: etkiler + isteğe bağlı anında iyileştirme. */
public class TonicItem extends Item {
    private final Supplier<List<MobEffectInstance>> effects;
    private final float heal;
    private final Supplier<ParticleOptions> particle;

    public TonicItem(Properties props, float heal, Supplier<ParticleOptions> particle, Supplier<List<MobEffectInstance>> effects) {
        super(props.stacksTo(8));
        this.effects = effects;
        this.heal = heal;
        this.particle = particle;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity e) {
        if (!level.isClientSide) {
            for (MobEffectInstance m : effects.get()) e.addEffect(new MobEffectInstance(m));
            if (heal > 0) e.heal(heal);
            ((ServerLevel) level).sendParticles(particle.get(), e.getX(), e.getY() + 1, e.getZ(), 24, 0.4, 0.6, 0.4, 0.05);
            level.playSound(null, e.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1F, 1.3F);
        }
        if (!(e instanceof Player p && p.getAbilities().instabuild)) stack.shrink(1);
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 28;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("tooltip.advgear." + net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(this).getPath())
                .withStyle(ChatFormatting.AQUA));
        if (heal > 0) tip.add(Component.translatable("tooltip.advgear.heals", (int) heal / 2).withStyle(ChatFormatting.RED));
        for (MobEffectInstance m : effects.get()) {
            Component name = m.getEffect().getDisplayName();
            String lvl = m.getAmplifier() > 0 ? " " + (m.getAmplifier() + 1) : "";
            tip.add(Component.literal("• ").append(name).append(lvl + " (" + m.getDuration() / 20 / 60 + ":" + String.format("%02d", m.getDuration() / 20 % 60) + ")")
                    .withStyle(m.getEffect().isBeneficial() ? ChatFormatting.BLUE : ChatFormatting.DARK_RED));
        }
    }

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(com.krolasyon.advgear.client.ClientHooks.ITEM_EXTENSIONS);
    }
}
