package com.krolasyon.skyislands.item;

import com.krolasyon.skyislands.world.BlockSink;
import com.krolasyon.skyislands.world.Island;
import com.krolasyon.skyislands.world.IslandBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/** Grows a small floating island in front of the player. */
public final class IslandSeedItem extends Item {
    public IslandSeedItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel sl) {
            Vec3 look = player.getLookAngle();
            Vec3 flat = new Vec3(look.x, 0, look.z);
            if (flat.lengthSqr() < 1e-4) flat = new Vec3(1, 0, 0);
            flat = flat.normalize();
            int radius = 6 + sl.random.nextInt(4);
            int cx = (int) Math.floor(player.getX() + flat.x * (radius + 8));
            int cz = (int) Math.floor(player.getZ() + flat.z * (radius + 8));
            int baseY = (int) Math.floor(player.getEyeY() + 3 + Math.max(0, look.y) * 14);
            baseY = Math.min(baseY, sl.getMaxBuildHeight() - 40);
            spawn(sl, cx, baseY, cz, radius, sl.random.nextLong());

            sl.playSound(null, cx, baseY, cz, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 2.0F, 0.8F);
            sl.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.4F);
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, cx + 0.5, baseY + 2, cz + 0.5, 120, radius * 0.6, 3, radius * 0.6, 0.1);
            sl.sendParticles(ParticleTypes.END_ROD, cx + 0.5, baseY - 2, cz + 0.5, 60, radius * 0.5, 4, radius * 0.5, 0.02);
            player.displayClientMessage(Component.translatable("message.skyislands.grown").withStyle(ChatFormatting.AQUA), true);
            player.getCooldowns().addCooldown(this, 40);
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    public static void spawn(ServerLevel sl, int cx, int baseY, int cz, int radius, long seed) {
        Island is = new Island(seed, cx, cz, baseY, radius, (int) (radius * 1.7), radius >= 8, false, false);
        int span = radius * 2 + 12;
        BlockSink sink = new BlockSink(sl, sl.getChunkSource().getGenerator(), cx - span, cz - span, cx + span, cz + span, 3);
        IslandBuilder.build(is, sink, sl.random);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.skyislands.island_seed.desc").withStyle(ChatFormatting.GRAY));
    }
}
