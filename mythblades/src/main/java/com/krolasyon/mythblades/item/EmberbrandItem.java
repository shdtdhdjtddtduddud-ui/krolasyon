package com.krolasyon.mythblades.item;

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
import net.minecraft.world.entity.TamableAnimal;
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
 * Kor Kılıcı: taştaki sunaktan Kor Mührü ile çekilen akkor kılıç. Vuruşlar tutuşturur; elde tutarken ateşe dayanıklılık verir.
 * Dövüş stili (EmberCombat / EmberClient): üç vuruşluk kombo (üçüncüsü alev hilali), sağ tık savunma ve karşı koyma,
 * R atılış, eğilip R kor saplama.
 */
public final class EmberbrandItem extends SwordItem {
    
    public EmberbrandItem() {
        super(MythTier.EMBER, 3, -2.0F, new Properties().rarity(Rarity.EPIC).fireResistant());
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean r = super.hurtEnemy(stack, target, attacker);
        target.setSecondsOnFire(5);
        if (attacker.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 14, 0.3, 0.35, 0.3, 0.03);
            sl.sendParticles(ParticleTypes.LAVA, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 2, 0.2, 0.2, 0.2, 0);
        }
        return r;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity e, int slot, boolean selected) {
        if (!level.isClientSide && e instanceof LivingEntity le && (selected || le.getOffhandItem() == stack) && le.tickCount % 20 == 0) {
            MobEffectInstance cur = le.getEffect(MobEffects.FIRE_RESISTANCE);
            if (cur == null || cur.getDuration() < 60) le.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 100, 0, true, false, true));
        }
    }

    /** Sağ tık basılı: savunma duruşu (ilk anlarda gelen darbe karşılanır: parry). */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(player.getItemInHand(hand));
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public net.minecraft.world.item.UseAnim getUseAnimation(ItemStack stack) {
        // vanilya kalkanı gibi tam engellemesin: hasar azaltma ve karşı koyma EmberCombat'ta
        return net.minecraft.world.item.UseAnim.NONE;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("tooltip.mythblades.ember.1").withStyle(ChatFormatting.GOLD));
        tip.add(Component.translatable("tooltip.mythblades.ember.2").withStyle(ChatFormatting.RED));
        tip.add(Component.translatable("tooltip.mythblades.ember.3").withStyle(ChatFormatting.YELLOW));
        tip.add(Component.translatable("tooltip.mythblades.ember.4").withStyle(ChatFormatting.GOLD));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(com.krolasyon.mythblades.client.ClientHooks.EMBER_EXTENSIONS);
    }
}
