package com.krolasyon.emeraldblade.item;

import com.krolasyon.emeraldblade.client.ClientHooks;
import com.krolasyon.emeraldblade.entity.EmeraldSpikeEntity;
import com.krolasyon.emeraldblade.entity.RootSnareEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * Zümrüt Kök Kılıcı: kök tuzağı + can çalma, şarjlı zümrüt diken dalgası, büyütme ve güneşte kendini onarma.
 */
public final class EmeraldRootBladeItem extends SwordItem {
    public static final int CHARGE_FULL = 30;
    private static final int MIN_CHARGE = 10;

    public EmeraldRootBladeItem() {
        super(BladeTier.INSTANCE, 3, -2.9F, new Properties().fireResistant().rarity(Rarity.EPIC));
    }

    // ------------------------------------------------------------ vuruş
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean r = super.hurtEnemy(stack, target, attacker);
        Level level = attacker.level();
        if (level instanceof ServerLevel sl) {
            attacker.heal(1.5F);
            sl.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.EMERALD)),
                    target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 10, 0.25, 0.3, 0.25, 0.12);
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 6, 0.3, 0.4, 0.3, 0);
            if (attacker.getRandom().nextFloat() < 0.35F) RootSnareEntity.snare(sl, target, attacker);
        }
        return r;
    }

    // ------------------------------------------------------------ şarj + dalga
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPEAR;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
        player.startUsingItem(hand);
        if (!level.isClientSide) level.playSound(null, player.blockPosition(), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.PLAYERS, 1.0F, 0.6F);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        int used = getUseDuration(stack) - remaining;
        if (level instanceof ServerLevel sl) {
            if (used % 4 == 0) {
                double a = used * 0.6;
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, user.getX() + Math.cos(a) * 1.1, user.getY() + 0.2 + (used % 20) * 0.08,
                        user.getZ() + Math.sin(a) * 1.1, 2, 0.05, 0.05, 0.05, 0);
                sl.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.EMERALD)),
                        user.getX(), user.getY() + 1.6, user.getZ(), 2, 0.4, 0.4, 0.4, 0.05);
            }
            if (used == CHARGE_FULL) {
                level.playSound(null, user.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.4F, 0.7F);
            }
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int remaining) {
        int used = getUseDuration(stack) - remaining;
        if (used < MIN_CHARGE || !(user instanceof Player player)) return;
        if (!level.isClientSide) {
            float power = Math.min(1.0F, used / (float) CHARGE_FULL);
            castWave((ServerLevel) level, player, power);
            player.getCooldowns().addCooldown(this, 140);
            stack.hurtAndBreak(3, player, p -> p.broadcastBreakEvent(player.getUsedItemHand()));
        }
        player.swing(player.getUsedItemHand());
    }

    /** Oyuncunun baktığı yönde, yerden sırayla fışkıran zümrüt diken dalgası. */
    public static void castWave(ServerLevel level, LivingEntity caster, float power) {
        Vec3 look = caster.getLookAngle();
        Vec3 dir = new Vec3(look.x, 0, look.z);
        if (dir.lengthSqr() < 1e-4) dir = new Vec3(0, 0, 1);
        dir = dir.normalize();
        Vec3 side = new Vec3(-dir.z, 0, dir.x);
        int steps = 9 + Math.round(power * 5);
        float scale = 0.75F + 0.45F * power;
        level.playSound(null, caster.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.2F, 1.3F);
        level.playSound(null, caster.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.5F, 1.6F);
        for (int i = 0; i < steps; i++) {
            double dist = 1.8 + i * 1.25;
            double jitter = Math.sin(i * 2.3) * 0.35;
            spawnSpike(level, caster, caster.position().add(dir.scale(dist)).add(side.scale(jitter)), 2 + i, scale * (1 + i * 0.03F));
            if (i == steps - 1 || (power >= 1 && i % 3 == 2)) {
                spawnSpike(level, caster, caster.position().add(dir.scale(dist + 0.6)).add(side.scale(1.3)), 3 + i, scale * 0.8F);
                spawnSpike(level, caster, caster.position().add(dir.scale(dist + 0.6)).add(side.scale(-1.3)), 3 + i, scale * 0.8F);
            }
        }
    }

    private static void spawnSpike(ServerLevel level, LivingEntity caster, Vec3 at, int delay, float scale) {
        BlockPos base = BlockPos.containing(at.x, caster.getY() + 2, at.z);
        for (int dy = 0; dy < 7; dy++) {
            BlockPos p = base.below(dy);
            if (level.getBlockState(p.below()).isFaceSturdy(level, p.below(), net.minecraft.core.Direction.UP)
                    && level.getBlockState(p).getCollisionShape(level, p).isEmpty()) {
                EmeraldSpikeEntity spike = new EmeraldSpikeEntity(level, at.x, p.getY(), at.z, delay, scale, caster);
                level.addFreshEntity(spike);
                return;
            }
        }
    }

    // ------------------------------------------------------------ büyütme
    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null || !player.isShiftKeyDown()) return InteractionResult.PASS;
        Level level = ctx.getLevel();
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResult.FAIL;
        if (!level.isClientSide) {
            BlockPos c = ctx.getClickedPos();
            int grown = 0;
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        BlockPos p = c.offset(dx, dy, dz);
                        if (BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), level, p)) {
                            grown++;
                            level.levelEvent(1505, p, 0);
                        }
                    }
                }
            }
            ((ServerLevel) level).sendParticles(ParticleTypes.HAPPY_VILLAGER, c.getX() + 0.5, c.getY() + 1.2, c.getZ() + 0.5, 30, 2, 0.4, 2, 0);
            level.playSound(null, c, SoundEvents.ROOTED_DIRT_PLACE, SoundSource.PLAYERS, 1.0F, 0.8F);
            if (grown > 0) ctx.getItemInHand().hurtAndBreak(2, player, p -> p.broadcastBreakEvent(ctx.getHand()));
            player.getCooldowns().addCooldown(this, 40);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // ------------------------------------------------------------ güneşte onarım
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !selected || !stack.isDamaged() || level.getGameTime() % 100 != 0) return;
        if (level.isDay() && !level.isRaining() && level.canSeeSky(entity.blockPosition().above())) {
            stack.setDamageValue(stack.getDamageValue() - 2);
            ((ServerLevel) level).sendParticles(ParticleTypes.HAPPY_VILLAGER, entity.getX(), entity.getY() + 1.2, entity.getZ(), 4, 0.3, 0.3, 0.3, 0);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.emeraldblade.l1").withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.translatable("tooltip.emeraldblade.l2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.emeraldblade.l3").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.emeraldblade.l4").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.emeraldblade.l5").withStyle(ChatFormatting.DARK_GREEN));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(ClientHooks.itemExtensions());
    }
}
