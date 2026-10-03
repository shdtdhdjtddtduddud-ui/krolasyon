package com.krolasyon.bosses.realm.item;

import com.krolasyon.bosses.realm.Faction;
import com.krolasyon.bosses.realm.Realm;
import com.krolasyon.bosses.realm.story.Summoning;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Savaş Borusu: blown in a kingdom's lands, it challenges that kingdom's lord. */
public class WarHornItem extends LoreItem {
    private final Faction faction;

    public WarHornItem(Properties props, Faction faction) {
        super(props, false);
        this.faction = faction;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer sp) {
            if (!Realm.inRealm(level) || !Summoning.inBiome(sp.serverLevel(), sp.blockPosition(), Summoning.biome(faction))) {
                sp.displayClientMessage(Component.translatable("message.krolasyonbosses.horn_wrong_land", faction.title()).withStyle(ChatFormatting.RED), true);
                return InteractionResultHolder.fail(stack);
            }
            level.playSound(null, sp.blockPosition(), SoundEvents.RAID_HORN.value(), SoundSource.PLAYERS, 4.0F, 0.8F);
            if (Summoning.challenge(sp.serverLevel(), sp.blockPosition(), faction, sp)) {
                player.getCooldowns().addCooldown(this, 200);
                if (!player.getAbilities().instabuild) stack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
