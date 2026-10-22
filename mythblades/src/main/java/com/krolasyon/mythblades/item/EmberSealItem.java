package com.krolasyon.mythblades.item;

import com.krolasyon.mythblades.world.AltarLayout;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/** Kor Mührü: Kor Kılıcı'nı taştan çekmeye yarar. Sağ tık: en yakın Kor Sunağı'nın yönünü alevle gösterir. */
public final class EmberSealItem extends Item {
    private static final String[] DIRS = {"güneyde", "güneybatıda", "batıda", "kuzeybatıda", "kuzeyde", "kuzeydoğuda", "doğuda", "güneydoğuda"};

    public EmberSealItem() {
        super(new Properties().stacksTo(16).rarity(Rarity.RARE).fireResistant());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel sl && level.dimension() == Level.OVERWORLD) {
            BlockPos a = AltarLayout.nearest(sl.getSeed(), sl.getChunkSource().getGenerator(), sl.getChunkSource().randomState(), sl,
                    player.getBlockX(), player.getBlockZ(), 2);
            if (a == null) {
                player.displayClientMessage(Component.translatable("msg.mythblades.seal_none").withStyle(ChatFormatting.GRAY), true);
            } else {
                double dx = a.getX() + 0.5 - player.getX(), dz = a.getZ() + 0.5 - player.getZ();
                int dist = (int) Math.sqrt(dx * dx + dz * dz);
                double yaw = Math.toDegrees(Math.atan2(-dx, dz));
                String dir = DIRS[(int) Math.floorMod(Math.round(yaw / 45.0), 8)];
                player.displayClientMessage(Component.translatable("msg.mythblades.seal_dir", dist, dir).withStyle(ChatFormatting.GOLD), true);
                Vec3 n = new Vec3(dx, 0, dz).normalize();
                for (int i = 1; i <= 14; i++) {
                    Vec3 q = player.position().add(n.scale(i * 0.6)).add(0, 1.1 + Math.sin(i * 0.7) * 0.15, 0);
                    sl.sendParticles(i % 3 == 0 ? ParticleTypes.LAVA : ParticleTypes.FLAME, q.x, q.y, q.z, 1, 0.02, 0.02, 0.02, 0.005);
                }
                sl.playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.4F, 1.6F);
            }
        }
        player.getCooldowns().addCooldown(this, 30);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("tooltip.mythblades.seal.1").withStyle(ChatFormatting.GOLD));
        tip.add(Component.translatable("tooltip.mythblades.seal.2").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
