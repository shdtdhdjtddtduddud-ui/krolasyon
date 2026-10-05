package com.sololeveling.item;

import com.sololeveling.gen.Content;
import com.sololeveling.player.ModCaps;
import com.sololeveling.player.SLPlayer;
import com.sololeveling.system.PlayerSync;
import com.sololeveling.system.Stats;
import com.sololeveling.system.Sys;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PotionItem extends Item {
    public final Content.PotionDef def;

    public PotionItem(Content.PotionDef def) {
        super(new Item.Properties().stacksTo(16).rarity(def.kind().equals("elixir") || def.kind().equals("rebirth") ? Rarity.EPIC : (def.amount() >= 100 ? Rarity.RARE : Rarity.COMMON)));
        this.def = def;
    }

    @Override public UseAnim getUseAnimation(ItemStack s) { return UseAnim.DRINK; }
    @Override public int getUseDuration(ItemStack s) { return 20; }
    @Override public boolean isFoil(ItemStack s) { return def.kind().equals("elixir") || def.kind().equals("rebirth"); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack st = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(st);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(st);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof ServerPlayer sp) {
            SLPlayer d = ModCaps.get(sp);
            switch (def.kind()) {
                case "hp" -> sp.heal(sp.getMaxHealth() * def.amount() / 100F);
                case "mp" -> d.addMana(d.maxMana() * def.amount() / 100F);
                case "antidote" -> {
                    List<net.minecraft.world.effect.MobEffect> bad = new ArrayList<>();
                    for (MobEffectInstance e : sp.getActiveEffects()) if (!e.getEffect().isBeneficial()) bad.add(e.getEffect());
                    bad.forEach(sp::removeEffect);
                }
                case "stamina" -> {
                    d.fatigue = 0;
                    sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 30, 1));
                    sp.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 20 * 30, 1));
                }
                case "elixir" -> {
                    sp.setHealth(sp.getMaxHealth());
                    d.mana = d.maxMana();
                    d.fatigue = 0;
                    sp.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 20, 2));
                    sp.removeAllEffects();
                }
                case "rebirth" -> {
                    int spent = 0;
                    for (int i = 0; i < 5; i++) { spent += d.stats[i] - 10; d.stats[i] = 10; }
                    d.points += spent;
                    Stats.apply(sp);
                    Sys.notify(sp, Sys.REWARD, Component.translatable("gui.sololeveling.system"), Component.translatable("gui.sololeveling.rebirth_done", spent));
                }
                default -> { }
            }
            level.playSound(null, sp.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 1F, 1F);
            PlayerSync.sync(sp);
            sp.getCooldowns().addCooldown(this, 15);
            if (!sp.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("item.sololeveling." + def.id() + ".desc").withStyle(ChatFormatting.AQUA));
    }
}
