package com.krolasyon.bosses.rpg.item;

import com.krolasyon.bosses.rpg.magic.PlayerMagic;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Casting focus: right click casts the selected spell with a power and mana-regen bonus. */
public class StaffItem extends Item {
    public final int tier;
    private final String title;

    public StaffItem(int tier, String title, Properties p) {
        super(p);
        this.tier = tier;
        this.title = title;
    }

    public float power() { return 1.0F + tier * 0.25F; }

    @Override
    public Component getName(ItemStack stack) { return Component.literal(title); }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("§7Büyü gücü: §d+" + (int) ((power() - 1) * 100) + "%"));
        tip.add(Component.literal("§7Mana yenilenmesi: §b+" + tier * 20 + "%"));
        tip.add(Component.literal("§8Sağ tık: seçili büyüyü yap"));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack st = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer sp) PlayerMagic.castSelected(sp);
        player.swing(hand);
        return InteractionResultHolder.sidedSuccess(st, level.isClientSide());
    }
}
