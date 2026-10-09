package com.krolasyon.vocations.item;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Amulet or ring. Right-click to wear it; the bonus is applied by {@link com.krolasyon.vocations.event.AccessoryEvents}
 * while it stays in its slot. Sneak + right-click with an empty hand takes all accessories off.
 */
public class AccessoryItem extends Item {
    private final AccessorySlot slot;
    private final Attribute attribute;
    private final double amount;
    private final AttributeModifier.Operation operation;

    public AccessoryItem(Properties properties, AccessorySlot slot, Attribute attribute, double amount,
                         AttributeModifier.Operation operation) {
        super(properties.stacksTo(1));
        this.slot = slot;
        this.attribute = attribute;
        this.amount = amount;
        this.operation = operation;
    }

    public AccessorySlot getSlot() {
        return slot;
    }

    public Attribute getAttribute() {
        return attribute;
    }

    public UUID modifierId() {
        return UUID.nameUUIDFromBytes(getDescriptionId().getBytes(StandardCharsets.UTF_8));
    }

    public AttributeModifier createModifier() {
        return new AttributeModifier(modifierId(), getDescriptionId(), amount, operation);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide) {
            ItemStack held = player.getItemInHand(hand);
            ItemStack previous = AccessoryData.equip(player, slot, held.split(1));
            if (!previous.isEmpty()) {
                AccessoryData.giveBack(player, previous);
            }
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.krolasyonvocations.accessory").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.krolasyonvocations.accessory_unequip").withStyle(ChatFormatting.DARK_GRAY));
    }
}
