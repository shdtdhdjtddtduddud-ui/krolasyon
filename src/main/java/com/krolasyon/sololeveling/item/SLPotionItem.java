package com.krolasyon.sololeveling.item;

import com.krolasyon.sololeveling.system.HunterCapability;
import com.krolasyon.sololeveling.system.HunterData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** System potions — every one has its own bottle model. */
public class SLPotionItem extends Item {
    public enum Kind {
        HEALING, GREATER_HEALING, MANA, GREATER_MANA, ELIXIR_OF_LIFE, HOLY_WATER, FATIGUE_RECOVERY,
        STRENGTH, AGILITY, PERCEPTION, STEALTH, ANTIDOTE
    }

    public final Kind kind;

    public SLPotionItem(Kind kind, Rarity rarity) {
        super(new Properties().stacksTo(16).rarity(rarity));
        this.kind = kind;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack s) { return UseAnim.DRINK; }

    @Override
    public int getUseDuration(ItemStack s) { return kind == Kind.HEALING || kind == Kind.MANA ? 20 : 28; }

    @Override
    public SoundEvent getDrinkingSound() { return SoundEvents.GENERIC_DRINK; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public boolean isFoil(ItemStack s) { return kind == Kind.ELIXIR_OF_LIFE || kind == Kind.HOLY_WATER; }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity e) {
        if (!level.isClientSide) {
            apply(e);
            if (level instanceof ServerLevel sl)
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, e.getX(), e.getY() + 1, e.getZ(), 12, 0.4, 0.6, 0.4, 0);
        }
        if (e instanceof Player p && !p.getAbilities().instabuild) stack.shrink(1);
        return stack;
    }

    private void apply(LivingEntity e) {
        HunterData d = e instanceof ServerPlayer sp ? HunterCapability.get(sp) : null;
        switch (kind) {
            case HEALING -> e.heal(8 + e.getMaxHealth() * 0.15F);
            case GREATER_HEALING -> {
                e.heal(20 + e.getMaxHealth() * 0.4F);
                e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
            }
            case MANA -> mana(d, 50, 0.15F);
            case GREATER_MANA -> mana(d, 150, 0.5F);
            case ELIXIR_OF_LIFE -> {
                e.setHealth(e.getMaxHealth());
                e.removeAllEffects();
                e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 2));
                e.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 1200, 4));
                mana(d, 9999, 1F);
                if (d != null) d.fatigue = 0;
            }
            case HOLY_WATER -> {
                e.setHealth(e.getMaxHealth());
                e.removeAllEffects();
                e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1200, 1));
                e.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 6000, 4));
                e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 1200, 1));
                mana(d, 9999, 1F);
            }
            case FATIGUE_RECOVERY -> {
                if (e instanceof Player p) p.getFoodData().eat(20, 1F);
                e.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 2400, 1));
                if (d != null) d.fatigue = 0;
            }
            case STRENGTH -> e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 3600, 1));
            case AGILITY -> {
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 3600, 1));
                e.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 3600, 0));
                e.addEffect(new MobEffectInstance(MobEffects.JUMP, 3600, 1));
            }
            case PERCEPTION -> {
                e.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 6000, 0));
                for (LivingEntity m : e.level().getEntitiesOfClass(LivingEntity.class, e.getBoundingBox().inflate(40), x -> x instanceof net.minecraft.world.entity.monster.Enemy))
                    m.addEffect(new MobEffectInstance(MobEffects.GLOWING, 1200, 0));
            }
            case STEALTH -> e.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 1200, 0));
            case ANTIDOTE -> {
                e.removeEffect(MobEffects.POISON);
                e.removeEffect(MobEffects.WITHER);
                e.removeEffect(MobEffects.WEAKNESS);
                e.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                e.removeEffect(MobEffects.CONFUSION);
            }
        }
        if (d != null) d.markDirty();
    }

    private static void mana(HunterData d, float flat, float frac) {
        if (d == null) return;
        d.mana = Math.min(d.maxMana(), d.mana + flat + d.maxMana() * frac);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
    }
}
