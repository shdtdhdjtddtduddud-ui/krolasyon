package com.krolasyon.mythblades.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * Kızıl Damar: her vuruş damarlara öz doldurur (en çok 5) ve can çalar. Sağ tık: Kızıl Atılış — bakılan düşmana
 * fırlar, öz başına artan hasar verir, kanatır ve iyileştirir. Dolu özle öldürmek kısa bir yenilenme verir.
 */
public final class CrimsonVeinItem extends SwordItem {
    public static final int MAX = 5;
    private static final DustParticleOptions BLOOD = new DustParticleOptions(new Vector3f(0.78F, 0.12F, 0.08F), 1.2F);

    public CrimsonVeinItem() {
        super(MythTier.CRIMSON, 3, -2.0F, new Properties().rarity(Rarity.EPIC));
    }

    public static int essence(ItemStack s) {
        return s.hasTag() ? s.getTag().getInt("Essence") : 0;
    }

    private static void setEssence(ItemStack s, int e) {
        s.getOrCreateTag().putInt("Essence", Math.max(0, Math.min(MAX, e)));
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean r = super.hurtEnemy(stack, target, attacker);
        if (attacker.level() instanceof ServerLevel sl) {
            int e = essence(stack);
            if (target.isDeadOrDying() && e >= MAX) {
                attacker.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 1));
                sl.sendParticles(ParticleTypes.HEART, attacker.getX(), attacker.getY() + 2, attacker.getZ(), 3, 0.3, 0.2, 0.3, 0);
            }
            setEssence(stack, e + 1);
            attacker.heal(1F);
            sl.sendParticles(BLOOD, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 14, 0.3, 0.35, 0.3, 0.02);
            if (e + 1 == MAX) sl.playSound(null, attacker.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1F, 0.5F);
        }
        return r;
    }

    @Nullable
    private static LivingEntity aim(Player p, double range) {
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle(), end = eye.add(look.scale(range));
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(p, eye, end, new AABB(eye, end).inflate(1.5),
                e -> e instanceof LivingEntity le && le.isAlive() && e != p && !e.isSpectator() && !p.isAlliedTo(e), range * range);
        return hit != null && hit.getEntity() instanceof LivingEntity le ? le : null;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        int e = essence(stack);
        if (e <= 0 || player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.pass(stack);
        LivingEntity target = aim(player, 13);
        Vec3 dir = target != null ? target.position().subtract(player.position()) : player.getLookAngle().scale(6);
        double dist = dir.length();
        Vec3 n = dir.normalize();
        double sp = Math.min(2.6, 0.55 + dist * 0.2);
        player.setDeltaMovement(n.x * sp, Math.max(0.25, n.y * sp + 0.18), n.z * sp);
        player.fallDistance = 0;
        if (level instanceof ServerLevel sl) {
            player.hurtMarked = true;
            Vec3 a = player.position().add(0, 1, 0);
            for (int i = 0; i <= 12; i++) {
                Vec3 q = a.add(dir.scale(i / 12.0));
                sl.sendParticles(BLOOD, q.x, q.y, q.z, 3, 0.15, 0.2, 0.15, 0);
            }
            sl.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1F, 0.7F);
            sl.playSound(null, player.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.2F, 1.4F);
            if (target != null) {
                float dmg = 4F + 3F * e;
                target.hurt(player.damageSources().playerAttack(player), dmg);
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 40 + 20 * e, 0), player);
                player.heal(1.5F * e);
                sl.sendParticles(BLOOD, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 40, 0.4, 0.5, 0.4, 0.05);
                sl.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 2, 0.3, 0.3, 0.3, 0);
                sl.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1F, 0.8F);
                setEssence(stack, 0);
            } else setEssence(stack, e - 1);
            stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
        }
        player.getCooldowns().addCooldown(this, 30);
        player.swing(hand);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("tooltip.mythblades.crimson.1").withStyle(ChatFormatting.RED));
        tip.add(Component.translatable("tooltip.mythblades.crimson.2").withStyle(ChatFormatting.GOLD));
        tip.add(Component.translatable("tooltip.mythblades.crimson.essence", essence(stack), MAX).withStyle(ChatFormatting.DARK_RED));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return essence(stack) > 0 || stack.isDamaged();
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return essence(stack) > 0 ? Math.round(13F * essence(stack) / MAX) : super.getBarWidth(stack);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return essence(stack) > 0 ? 0xD8301E : super.getBarColor(stack);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(com.krolasyon.mythblades.client.ClientHooks.ITEM_EXTENSIONS);
    }
}
