package com.krolasyon.advgear.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.Property;

import javax.annotation.Nullable;
import java.util.List;

/** Mekanik Anahtar: tıklanan bloğu döndürür (yön, eksen ya da dönüş açısı olan her blok). */
public class MechanicKeyItem extends Item {
    private static final Property<?>[] PROPS = {BlockStateProperties.FACING, BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.AXIS,
            BlockStateProperties.HORIZONTAL_AXIS, BlockStateProperties.ROTATION_16, BlockStateProperties.FACING_HOPPER,
            BlockStateProperties.ORIENTATION};

    public MechanicKeyItem(Properties props) {
        super(props.durability(512));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockState s = level.getBlockState(pos);
        if (s.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) || s.hasProperty(BlockStateProperties.BED_PART)
                || (s.hasProperty(BlockStateProperties.CHEST_TYPE) && s.getValue(BlockStateProperties.CHEST_TYPE) != ChestType.SINGLE)
                || (s.hasProperty(BlockStateProperties.EXTENDED) && s.getValue(BlockStateProperties.EXTENDED))) return InteractionResult.PASS;
        for (Property<?> p : PROPS) {
            if (!s.hasProperty(p)) continue;
            if (!level.isClientSide) {
                BlockState n = s.cycle(p);
                if (!n.canSurvive(level, pos)) n = n.cycle(p);
                level.setBlock(pos, n, 3);
                level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.5F, 1.8F);
                ((ServerLevel) level).sendParticles(ParticleTypes.WAX_OFF, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.4, 0.4, 0.4, 0.5);
                if (ctx.getPlayer() != null) ctx.getItemInHand().hurtAndBreak(1, ctx.getPlayer(), pl -> pl.broadcastBreakEvent(ctx.getHand()));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("tooltip.advgear.mechanic_key").withStyle(ChatFormatting.AQUA));
    }

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(com.krolasyon.advgear.client.ClientHooks.ITEM_EXTENSIONS);
    }
}
