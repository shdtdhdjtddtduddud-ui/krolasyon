package com.krolasyon.beyblade.item;

import com.krolasyon.beyblade.entity.BeybladeEntity;
import com.krolasyon.beyblade.entity.Px;
import com.krolasyon.beyblade.entity.StadiumEntity;
import com.krolasyon.beyblade.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import net.minecraft.world.entity.EntityType;

import javax.annotation.Nullable;
import java.util.function.Supplier;
import java.util.List;

/** Hold right click to wind the ripcord (more power = more spin), release to launch Libra. Sneak + use recalls it. */
public class LauncherItem extends Item {
    public static final int FULL_CHARGE = 40;

    private final Supplier<? extends Item> beyItem;
    private final Supplier<? extends EntityType<? extends BeybladeEntity>> beyType;

    public LauncherItem(Properties props, Supplier<? extends Item> beyItem, Supplier<? extends EntityType<? extends BeybladeEntity>> beyType) {
        super(props);
        this.beyItem = beyItem;
        this.beyType = beyType;
    }

    /** the Standard Launcher (no fixed beyblade) launches any beyblade: the one in the off hand first */
    public boolean isStandard() { return beyItem == null; }

    public int findLibra(Player p) {
        int off = p.getInventory().getContainerSize() - 1;
        if (isStandard() && p.getInventory().getItem(off).getItem() instanceof LibraItem) return off;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (isStandard() ? s.getItem() instanceof LibraItem : s.is(beyItem.get())) return i;
        }
        return -1;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BeybladeEntity active = BeybladeEntity.findActive(player);
        if (active != null) {
            if (!level.isClientSide()) {
                if (player.isShiftKeyDown()) active.recall();
                else player.displayClientMessage(Component.translatable("msg.librabeyblade.already"), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        if (findLibra(player) < 0) {
            if (!level.isClientSide()) player.displayClientMessage(isStandard() ? Component.translatable("msg.librabeyblade.no_bey_any")
                    : Component.translatable("msg.librabeyblade.no_bey", beyItem.get().getDescription()), true);
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        if (!level.isClientSide()) level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.CHARGE.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        int used = getUseDuration(stack) - remaining;
        if (entity instanceof Player p && !level.isClientSide()) {
            float power = Math.min(1F, used / (float) FULL_CHARGE);
            if (used % 4 == 0) {
                int bars = Math.round(power * 20);
                Component bar = Component.literal("█".repeat(bars)).withStyle(power >= 1F ? ChatFormatting.GOLD : ChatFormatting.GREEN)
                        .append(Component.literal("█".repeat(20 - bars)).withStyle(ChatFormatting.DARK_GRAY));
                p.displayClientMessage(Component.translatable("msg.librabeyblade.power", bar, Math.round(power * 100)), true);
            }
            if (used == FULL_CHARGE) {
                level.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.LAND.get(), SoundSource.PLAYERS, 0.8F, 1.6F);
                Px.send((ServerLevel) level, ParticleTypes.ELECTRIC_SPARK, p.getX(), p.getEyeY() - 0.3, p.getZ(), 10, 0.3, 0.2, 0.3, 0.1);
            }
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remaining) {
        if (!(entity instanceof Player player) || level.isClientSide()) return;
        int used = getUseDuration(stack) - remaining;
        if (used < 4) return;
        int slot = findLibra(player);
        if (slot < 0 || BeybladeEntity.findActive(player) != null) return;
        float power = Mth.clamp(used / (float) FULL_CHARGE, 0.25F, 1F);
        ItemStack libra = player.getInventory().getItem(slot);
        ItemStack libraType = libra.copy();
        ItemStack launched = libra.copy();
        launched.setCount(1);
        libra.shrink(1);
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        if (flat.lengthSqr() < 1.0E-4) flat = new Vec3(0, 0, 1);
        flat = flat.normalize();
        Vec3 pos = player.getEyePosition().add(flat.scale(1.0)).add(0, -0.5, 0);
        Vec3 vel = flat.scale(0.35 + 0.55 * power).add(0, 0.12, 0);
        // standing outside a stadium: a higher arc so the beyblade clears the wall and lands in the bowl
        for (StadiumEntity st : level.getEntitiesOfClass(StadiumEntity.class, player.getBoundingBox().inflate(24))) {
            double r = Math.hypot(player.getX() - st.getX(), player.getZ() - st.getZ());
            if (r > StadiumEntity.WALL_IN - 1) {
                Vec3 toC = new Vec3(st.getX() - pos.x, 0, st.getZ() - pos.z);
                double d = toC.length();
                if (d < 1.0E-3 || toC.scale(1 / d).dot(flat) < 0.5) continue;
                double h = Math.max(0.5, StadiumEntity.WALL_TOP + 1.2 - (pos.y - st.getY()));
                double vy = Math.sqrt(2 * 0.06 * h) + 0.05;
                double ticks = vy / 0.06 * 2;
                double vh = Math.min(0.95, Math.max(0.3, (d - 3.0) / ticks)) * (0.85 + 0.15 * power);
                vel = flat.scale(vh).add(0, vy, 0);
                break;
            }
        }
        EntityType<? extends BeybladeEntity> type = isStandard() && libraType.getItem() instanceof LibraItem li ? li.spawnType() : beyType.get();
        // launch techniques: a pull released right at full charge is a Perfect Launch, a sneaking launch is a Power Launch
        boolean perfect = used >= FULL_CHARGE && used <= FULL_CHARGE + 8;
        boolean powerLaunch = player.isShiftKeyDown();
        if (powerLaunch) vel = vel.multiply(1.35, 1.0, 1.35);
        BeybladeEntity bey = BeybladeEntity.launch(type, level, player, launched, pos, vel, 0.55F + 0.45F * power);
        bey.launchBonus(perfect, powerLaunch);
        if (perfect) player.displayClientMessage(Component.translatable("msg.librabeyblade.perfect_launch").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        if (powerLaunch) player.displayClientMessage(Component.translatable("msg.librabeyblade.power_launch").withStyle(ChatFormatting.RED), false);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LAUNCH.get(), SoundSource.PLAYERS, 1.2F, 0.95F + power * 0.1F);
        if (!perfect && !powerLaunch) player.displayClientMessage(Component.translatable("msg.librabeyblade.launch", Math.round(power * 100)), true);
        player.getCooldowns().addCooldown(this, 10);
    }

    @Override
    public int getUseDuration(ItemStack stack) { return 72000; }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.BOW; }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("item.librabeyblade.libra_launcher.desc2").withStyle(ChatFormatting.DARK_GRAY));
    }
}
