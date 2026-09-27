package com.krolasyon.bosses.item;

import com.krolasyon.bosses.morph.MorphServer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

/** Aigoar's Tide Blade: a strong sword that transforms its wielder into the abyssal tide lord. */
public class TideBladeItem extends SwordItem {
    public static final Tier TIER = new Tier() {
        @Override public int getUses() { return 2400; }
        @Override public float getSpeed() { return 9.0F; }
        @Override public float getAttackDamageBonus() { return 5.0F; }
        @Override public int getLevel() { return 4; }
        @Override public int getEnchantmentValue() { return 18; }
        @Override public Ingredient getRepairIngredient() { return Ingredient.of(Items.PRISMARINE_CRYSTALS); }
    };

    /** key names for the tooltip, filled in on the client */
    public static Supplier<String[]> keyNames = () -> new String[]{"R", "G", "V", "Z", "B", "H"};

    public TideBladeItem(Properties props) {
        super(TIER, 3, -2.3F, props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean morphed = MorphServer.isMorphed(player);
        if (morphed && !player.isShiftKeyDown()) return InteractionResultHolder.pass(stack);
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            if (morphed) MorphServer.revert(sp);
            else MorphServer.transform(sp);
        }
        player.getCooldowns().addCooldown(this, 30);
        if (level.isClientSide()) player.playSound(SoundEvents.TRIDENT_RIPTIDE_1, 0.6F, 1.2F);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public boolean isFoil(ItemStack stack) { return true; }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        String[] k = keyNames.get();
        tip.add(Component.translatable("tooltip.krolasyonbosses.tide_blade.1").withStyle(ChatFormatting.AQUA));
        tip.add(Component.translatable("tooltip.krolasyonbosses.tide_blade.2", "[" + k[0] + "]", "[" + k[1] + "]", "[" + k[2] + "]", "[" + k[3] + "]", "[" + k[4] + "]")
                .withStyle(ChatFormatting.DARK_AQUA));
        tip.add(Component.translatable("tooltip.krolasyonbosses.tide_blade.3").withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("tooltip.krolasyonbosses.tide_blade.4").withStyle(ChatFormatting.GRAY));
    }
}
