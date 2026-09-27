package com.krolasyon.bosses.item;

import com.krolasyon.bosses.morph.Forms;
import com.krolasyon.bosses.morph.MorphServer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
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

/** A transformation weapon: a strong sword that turns its wielder into its form. */
public class FormBladeItem extends SwordItem {
    public static final Tier TIER = new Tier() {
        @Override public int getUses() { return 2400; }
        @Override public float getSpeed() { return 9.0F; }
        @Override public float getAttackDamageBonus() { return 5.0F; }
        @Override public int getLevel() { return 4; }
        @Override public int getEnchantmentValue() { return 18; }
        @Override public Ingredient getRepairIngredient() { return Ingredient.of(Items.NETHERITE_SCRAP, Items.PRISMARINE_CRYSTALS); }
    };

    /** key names for the tooltip, filled in on the client */
    public static Supplier<String[]> keyNames = () -> new String[]{"R", "G", "V", "Z", "B", "H"};

    public final int form;

    public FormBladeItem(int form, int damage, float speed, Properties props) {
        super(TIER, damage, speed, props);
        this.form = form;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean morphed = MorphServer.isMorphed(player);
        if (morphed && !player.isShiftKeyDown()) return InteractionResultHolder.pass(stack);
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            if (morphed) MorphServer.revert(sp);
            else MorphServer.transform(sp, form);
        }
        player.getCooldowns().addCooldown(this, 30);
        if (level.isClientSide()) player.playSound(form == Forms.AIGOAR ? SoundEvents.TRIDENT_RIPTIDE_1 : SoundEvents.BLAZE_SHOOT, 0.6F, 1.2F);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (Forms.fiery(form)) target.setSecondsOnFire(4);
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public boolean isFoil(ItemStack stack) { return true; }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        String[] k = keyNames.get();
        String f = Forms.KEY[form];
        tip.add(Component.translatable("tooltip.krolasyonbosses.blade.transform", Component.translatable("form.krolasyonbosses." + f)).withStyle(ChatFormatting.GOLD));
        tip.add(Component.translatable("tooltip.krolasyonbosses.blade.revert", "[" + k[5] + "]").withStyle(ChatFormatting.GRAY));
        for (int i = 0; i < 5; i++)
            tip.add(Component.literal("[" + k[i] + "] ").append(Component.translatable("ability.krolasyonbosses." + f + "." + i)).withStyle(ChatFormatting.DARK_AQUA));
        tip.add(Component.translatable("tooltip.krolasyonbosses.blade.passive." + f).withStyle(ChatFormatting.GRAY));
    }
}
