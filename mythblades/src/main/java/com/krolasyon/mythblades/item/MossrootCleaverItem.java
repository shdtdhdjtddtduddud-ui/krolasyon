package com.krolasyon.mythblades.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Yosunkök Satırı: ağır bir taş balta-kılıç. Ağaçları hızla keser; taşa sağ tıkla yosunlandırır, toprağı çimlendirir.
 * Basılı tutup bırakınca yeri döver: genişleyen yosunlu şok dalgası düşmanları havaya atar ve köklerle yavaşlatır.
 * Yağmurda ya da çimenin üstünde kendini onarır.
 */
public final class MossrootCleaverItem extends AxeItem {
    public static final int CHARGE_FULL = 26;
    private static final Map<Block, Block> MOSSY = Map.of(
            Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE, Blocks.STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS,
            Blocks.COBBLESTONE_STAIRS, Blocks.MOSSY_COBBLESTONE_STAIRS, Blocks.COBBLESTONE_SLAB, Blocks.MOSSY_COBBLESTONE_SLAB,
            Blocks.COBBLESTONE_WALL, Blocks.MOSSY_COBBLESTONE_WALL, Blocks.STONE_BRICK_STAIRS, Blocks.MOSSY_STONE_BRICK_STAIRS,
            Blocks.STONE_BRICK_SLAB, Blocks.MOSSY_STONE_BRICK_SLAB, Blocks.STONE_BRICK_WALL, Blocks.MOSSY_STONE_BRICK_WALL,
            Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.STONE, Blocks.MOSS_BLOCK);

    /** Süren şok dalgaları: [level, x, y, z, radius, tick, power, owner id]. */
    private static final List<Object[]> WAVES = new ArrayList<>();

    public MossrootCleaverItem() {
        super(MythTier.MOSSROOT, 6F, -3.1F, new Properties().rarity(Rarity.EPIC));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockState st = level.getBlockState(pos);
        Block to = MOSSY.get(st.getBlock());
        if (to != null && !(ctx.getPlayer() != null && ctx.getPlayer().isShiftKeyDown())) {
            if (level instanceof ServerLevel sl) {
                BlockState ns = to.withPropertiesOf(st);
                sl.setBlock(pos, ns, 11);
                sl.playSound(null, pos, SoundEvents.MOSS_PLACE, SoundSource.BLOCKS, 1F, 0.9F);
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 8, 0.4, 0.2, 0.4, 0);
                if (ctx.getPlayer() != null) ctx.getItemInHand().hurtAndBreak(1, ctx.getPlayer(), p -> p.broadcastBreakEvent(ctx.getHand()));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.useOn(ctx);
    }

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
        if (!level.isClientSide) level.playSound(null, player.blockPosition(), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.PLAYERS, 1F, 0.5F);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        int used = getUseDuration(stack) - remaining;
        if (level instanceof ServerLevel sl && used % 3 == 0) {
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MOSS_BLOCK.defaultBlockState()), user.getX(), user.getY() + 0.1,
                    user.getZ(), 4, 0.6, 0.05, 0.6, 0.05);
            if (used == CHARGE_FULL) level.playSound(null, user.blockPosition(), SoundEvents.ROOTED_DIRT_PLACE, SoundSource.PLAYERS, 1.5F, 0.5F);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int remaining) {
        int used = getUseDuration(stack) - remaining;
        if (used < 8 || !(user instanceof Player player)) return;
        float power = Math.min(1F, used / (float) CHARGE_FULL);
        player.swing(player.getUsedItemHand());
        if (level instanceof ServerLevel sl) {
            Vec3 c = player.position().add(player.getLookAngle().multiply(1.6, 0, 1.6));
            synchronized (WAVES) {
                WAVES.add(new Object[]{sl, c.x, player.getY(), c.z, 2.5F + 4.5F * power, 0, power, player.getId()});
            }
            sl.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.7F + 0.4F * power, 0.55F);
            sl.playSound(null, player.blockPosition(), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.PLAYERS, 1.5F, 0.6F);
            player.getCooldowns().addCooldown(this, 70);
            stack.hurtAndBreak(2, player, p -> p.broadcastBreakEvent(player.getUsedItemHand()));
        }
    }

    /** Sunucu tikinde şok dalgalarını genişletir: halka halka parçacık, içindeki düşmanlara bir kez vurur. */
    public static void tickWaves() {
        synchronized (WAVES) {
            Iterator<Object[]> it = WAVES.iterator();
            while (it.hasNext()) {
                Object[] w = it.next();
                ServerLevel sl = (ServerLevel) w[0];
                double x = (Double) w[1], y = (Double) w[2], z = (Double) w[3];
                float maxR = (Float) w[4], power = (Float) w[6];
                int t = (Integer) w[5];
                float r0 = maxR * t / 7F, r1 = maxR * (t + 1) / 7F;
                int n = (int) (r1 * 7) + 6;
                for (int i = 0; i < n; i++) {
                    double a = Math.PI * 2 * i / n;
                    double px = x + Math.cos(a) * r1, pz = z + Math.sin(a) * r1;
                    BlockPos bp = BlockPos.containing(px, y - 0.5, pz);
                    BlockState ground = sl.getBlockState(bp);
                    if (ground.isAir()) ground = Blocks.MOSS_BLOCK.defaultBlockState();
                    sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), px, y + 0.15, pz, 3, 0.15, 0.1, 0.15, 0.15);
                    if (i % 3 == 0) sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MOSS_BLOCK.defaultBlockState()),
                            px, y + 0.3, pz, 2, 0.1, 0.2, 0.1, 0.25);
                }
                Entity owner = sl.getEntity((Integer) w[7]);
                for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(x - r1, y - 1.5, z - r1, x + r1, y + 2.5, z + r1))) {
                    double d = Math.hypot(e.getX() - x, e.getZ() - z);
                    if (d < r0 - 0.6 || d > r1 + 0.6 || e == owner) continue;
                    if (e instanceof TamableAnimal ta && owner instanceof LivingEntity lo && ta.isOwnedBy(lo)) continue;
                    if (owner instanceof Player op && e.isAlliedTo(op)) continue;
                    float dmg = 5F + 7F * power;
                    e.hurt(owner instanceof Player op ? sl.damageSources().playerAttack(op) : sl.damageSources().generic(), dmg);
                    Vec3 push = new Vec3(e.getX() - x, 0, e.getZ() - z).normalize().scale(0.3 + 0.4 * power);
                    e.setDeltaMovement(e.getDeltaMovement().add(push.x, 0.35 + 0.45 * power, push.z));
                    e.hurtMarked = true;
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 70, 3));
                }
                if (t % 2 == 0) sl.playSound(null, BlockPos.containing(x, y, z), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.PLAYERS, 0.8F, 0.6F + t * 0.08F);
                w[5] = t + 1;
                if (t + 1 >= 7) it.remove();
            }
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity e, int slot, boolean selected) {
        if (level.isClientSide || !stack.isDamaged() || e.tickCount % 100 != 0) return;
        BlockState below = level.getBlockState(e.blockPosition().below());
        if (level.isRainingAt(e.blockPosition()) || below.is(Blocks.GRASS_BLOCK) || below.is(Blocks.MOSS_BLOCK)) {
            stack.setDamageValue(stack.getDamageValue() - 1);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("tooltip.mythblades.mossroot.1").withStyle(ChatFormatting.GREEN));
        tip.add(Component.translatable("tooltip.mythblades.mossroot.2").withStyle(ChatFormatting.DARK_GREEN));
        tip.add(Component.translatable("tooltip.mythblades.mossroot.3").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(com.krolasyon.mythblades.client.ClientHooks.ITEM_EXTENSIONS);
    }

    static float lerp(float a, float b, float t) {
        return Mth.lerp(t, a, b);
    }
}
