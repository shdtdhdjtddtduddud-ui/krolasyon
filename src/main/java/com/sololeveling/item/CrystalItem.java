package com.sololeveling.item;

import com.sololeveling.gen.Content;
import com.sololeveling.player.ModCaps;
import com.sololeveling.player.SLPlayer;
import com.sololeveling.system.PlayerSync;
import com.sololeveling.system.Sys;
import com.sololeveling.util.Ranks;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CrystalItem extends Item {
    public final Content.CrystalDef def;

    public CrystalItem(Content.CrystalDef def) {
        super(new Item.Properties().rarity(Ranks.rarity(def.rank())));
        this.def = def;
    }

    @Override public boolean isFoil(ItemStack s) { return Ranks.index(def.rank()) >= 4; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack st = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            int n = sp.isShiftKeyDown() ? st.getCount() : 1;
            SLPlayer d = ModCaps.get(sp);
            d.gold += (long) def.value() * n;
            st.shrink(n);
            level.playSound(null, sp.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1F, 1.3F);
            Sys.notify(sp, Sys.REWARD, Component.translatable("gui.sololeveling.system"), Component.translatable("gui.sololeveling.gold_gain", (long) def.value() * n));
            PlayerSync.sync(sp);
        }
        return InteractionResultHolder.sidedSuccess(st, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.empty().append(Ranks.tag(def.rank())).append(Component.literal("  " + def.value() + " G").withStyle(ChatFormatting.GOLD)));
        tip.add(Component.translatable("item.sololeveling." + def.id() + ".desc").withStyle(ChatFormatting.DARK_AQUA));
    }
}
