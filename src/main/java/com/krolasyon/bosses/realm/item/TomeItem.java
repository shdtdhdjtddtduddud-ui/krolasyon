package com.krolasyon.bosses.realm.item;

import com.krolasyon.bosses.realm.story.PlayerPowers;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Büyü Kitabı: casts its spell for mana. */
public class TomeItem extends LoreItem implements ManaUser {
    private final Spells.Spell spell;

    public TomeItem(Properties props, Spells.Spell spell) {
        super(props, true);
        this.spell = spell;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer sp) {
            if (PlayerPowers.useMana(sp, spell.mana)) {
                if (Spells.cast(sp, spell)) {
                    player.getCooldowns().addCooldown(this, spell.cooldown);
                    player.swing(hand, true);
                } else {
                    PlayerPowers.addMana(sp, spell.mana);
                }
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, level, lines, flag);
        lines.add(Component.literal("✦ " + spell.mana + " mana  ⌛ " + String.format("%.1f", spell.cooldown / 20F) + "s").withStyle(ChatFormatting.BLUE));
    }
}
