package com.krolasyon.advgear.item;

import com.krolasyon.advgear.AdvGear;
import com.krolasyon.advgear.gear.BackpackContainer;
import com.krolasyon.advgear.gear.BackpackMenu;
import com.krolasyon.advgear.gear.GearData;
import com.krolasyon.advgear.gear.GearItem;
import com.krolasyon.advgear.gear.GearType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/** Sırt çantası: sağ tık açar, shift + sağ tık sırta takar (sırttayken B tuşu açar). */
public class BackpackItem extends GearItem {
    public final int rows, variant;

    public BackpackItem(int rows, int variant, Properties props) {
        super(GearType.BACK, props);
        this.rows = rows;
        this.variant = variant;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player.isShiftKeyDown()) return super.use(level, player, hand);
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer sp) open(sp, stack, hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : 40);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    public void open(ServerPlayer p, ItemStack stack, int lockedSlot) {
        BackpackContainer c = new BackpackContainer(stack, rows * 9);
        p.level().playSound(null, p.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.PLAYERS, 0.9F, 0.8F);
        p.level().playSound(null, p.blockPosition(), SoundEvents.BUNDLE_DROP_CONTENTS, SoundSource.PLAYERS, 0.5F, 1.2F);
        Runnable close = lockedSlot < 0 ? () -> GearData.get(p).save() : null;
        p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new BackpackMenu(AdvGear.backpackMenu(rows), id, inv, c, rows, lockedSlot, close),
                stack.getHoverName()));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tip, flag);
        tip.add(Component.translatable("tooltip.advgear.backpack_fill", BackpackContainer.count(stack), rows * 9).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {

    }
}
