package com.krolasyon.cille.item;

import com.krolasyon.cille.entity.MarbleEntity;
import com.krolasyon.cille.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * Kabkaba's marble. The first player to throw it becomes its Cahan: the bond is permanent and nobody else can
 * open the marble. Throw it; when it hits another marble (or rolls to a stop) Kabkaba bursts out.
 */
public class CilleMarbleItem extends Item {
    public CilleMarbleItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        CompoundTag tag = stack.getOrCreateTag();
        long ready = tag.getLong("Ready");
        if (ready > level.getGameTime()) {
            if (!level.isClientSide()) {
                long secs = (ready - level.getGameTime() + 19) / 20;
                player.displayClientMessage(Component.translatable("msg.cille.resting", secs).withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        if (tag.hasUUID("Owner") && !tag.getUUID("Owner").equals(player.getUUID())) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("msg.cille.refuse", tag.getString("OwnerName")).withStyle(ChatFormatting.DARK_PURPLE), true);
                player.hurt(level.damageSources().magic(), 2F);
                player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0));
                level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.AMBIENT.get(), SoundSource.PLAYERS, 1.2F, 0.6F);
            }
            player.getCooldowns().addCooldown(this, 40);
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide()) {
            if (!tag.hasUUID("Owner")) {
                tag.putUUID("Owner", player.getUUID());
                tag.putString("OwnerName", player.getName().getString());
                player.displayClientMessage(Component.translatable("msg.cille.bond").withStyle(ChatFormatting.LIGHT_PURPLE), false);
                ((ServerLevel) level).sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1, player.getZ(), 60, 0.6, 1.0, 0.6, 0.3);
            }
            ItemStack thrown = stack.copy();
            thrown.setCount(1);
            MarbleEntity marble = new MarbleEntity(level, player, thrown);
            marble.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.1F, 0.5F);
            level.addFreshEntity(marble);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.MARBLE_THROW.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        player.swing(hand);
        player.getCooldowns().addCooldown(this, 20);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /** smooth 3D marble model in hand, inventory and on the ground */
    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.krolasyon.cille.client.render.MarbleItemRenderer.get();
            }
        });
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.hasTag() && stack.getTag().hasUUID("Owner");
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        CompoundTag tag = stack.getTag();
        lines.add(Component.translatable("item.cille.kabkaba_misketi.desc").withStyle(ChatFormatting.DARK_PURPLE));
        if (tag != null && tag.hasUUID("Owner")) {
            lines.add(Component.translatable("item.cille.kabkaba_misketi.owner", tag.getString("OwnerName")).withStyle(ChatFormatting.LIGHT_PURPLE));
        } else {
            lines.add(Component.translatable("item.cille.kabkaba_misketi.unbound").withStyle(ChatFormatting.GRAY));
        }
        if (tag != null && tag.contains("Health")) {
            lines.add(Component.translatable("item.cille.kabkaba_misketi.health", Math.round(tag.getFloat("Health"))).withStyle(ChatFormatting.RED));
        }
        if (level != null && tag != null && tag.getLong("Ready") > level.getGameTime()) {
            lines.add(Component.translatable("item.cille.kabkaba_misketi.resting").withStyle(ChatFormatting.GRAY));
        }
        lines.add(Component.translatable("item.cille.kabkaba_misketi.help").withStyle(ChatFormatting.DARK_GRAY));
    }
}
