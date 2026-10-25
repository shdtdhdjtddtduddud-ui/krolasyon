package com.krolasyon.labirent.item;

import com.krolasyon.labirent.maze.MazeManager;
import com.krolasyon.labirent.team.Role;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Bandage: heals; med-jacks heal twice as much. */
public class BandageItem extends Item {
    public BandageItem(Properties p) { super(p); }

    private static float amount(Player p, boolean other) {
        MazeManager m = MazeManager.get();
        boolean medic = m != null && m.state.role(p.getUUID()) == Role.SIFACI;
        return (other ? 6F : 4F) * (medic ? 2F : 1F);
    }

    private static void apply(Player by, LivingEntity t, boolean other, ItemStack s) {
        t.heal(amount(by, other));
        t.level().playSound(null, t.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.PLAYERS, 1F, 1.2F);
        if (t.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.HEART, t.getX(), t.getY() + 1.8, t.getZ(), 4, 0.3, 0.2, 0.3, 0);
        if (!by.isCreative()) s.shrink(1);
        by.getCooldowns().addCooldown(s.getItem(), 40);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack s = player.getItemInHand(hand);
        if (player.getHealth() >= player.getMaxHealth()) return InteractionResultHolder.pass(s);
        if (!level.isClientSide) apply(player, player, false, s);
        return InteractionResultHolder.sidedSuccess(s, level.isClientSide);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack s, Player player, LivingEntity target, InteractionHand hand) {
        if (target.getHealth() >= target.getMaxHealth()) return InteractionResult.PASS;
        if (!player.level().isClientSide) apply(player, target, true, s);
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }
}
