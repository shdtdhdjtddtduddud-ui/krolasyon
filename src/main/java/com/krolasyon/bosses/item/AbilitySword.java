package com.krolasyon.bosses.item;

import com.krolasyon.bosses.faction.PlayerData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.ChatFormatting;

import javax.annotation.Nullable;
import java.util.List;

/** Hellsteel blade with a passive on-hit curse and an active power on right click (costs mana). */
public class AbilitySword extends SwordItem {
    private final String id;
    private final int mana, cooldown;

    public AbilitySword(String id, int dmg, float speed, Properties props, int mana, int cooldown) {
        super(HellTier.HELLSTEEL, dmg, speed, props);
        this.id = id;
        this.mana = mana;
        this.cooldown = cooldown;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        switch (id) {
            case "ember_brand", "hellfire_scythe" -> target.setSecondsOnFire(id.equals("ember_brand") ? 5 : 8);
            case "bone_reaper" -> target.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 0), attacker);
            case "blood_thirst" -> attacker.heal(2.0F);
            case "shadow_fang" -> target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0), attacker);
            case "rot_cleaver" -> target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1), attacker);
            case "sealbreaker" -> target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1), attacker);
            case "chain_whip" -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1), attacker);
            case "obsidian_greataxe" -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 3), attacker);
            case "sovereign_blade" -> {
                target.setSecondsOnFire(4);
                attacker.heal(1.0F);
            }
            default -> {}
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide() || !(player instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
        if (PlayerData.mana(sp) < mana && !sp.isCreative()) {
            sp.displayClientMessage(Component.translatable("msg.no_mana"), true);
            return InteractionResultHolder.fail(stack);
        }
        if (!Magic.cast(id, sp)) return InteractionResultHolder.fail(stack);
        PlayerData.useMana(sp, mana);
        player.getCooldowns().addCooldown(this, cooldown);
        stack.hurtAndBreak(2, sp, p -> p.broadcastBreakEvent(hand));
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("item.krolasyonbosses." + id + ".desc").withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("item.krolasyonbosses.mana", mana).withStyle(ChatFormatting.AQUA));
    }
}
