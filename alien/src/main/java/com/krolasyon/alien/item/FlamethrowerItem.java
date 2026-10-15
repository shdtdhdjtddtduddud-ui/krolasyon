package com.krolasyon.alien.item;

import com.krolasyon.alien.ai.NoiseSystem;
import com.krolasyon.alien.entity.XenomorphEntity;
import com.krolasyon.alien.registry.ModItems;
import com.krolasyon.alien.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/** Hold use to spray fire. The only thing the creature fears. Fuel is the damage bar; canisters reload it. */
public class FlamethrowerItem extends Item {
    public static final int FUEL = 240;
    private static final double RANGE = 7.5;

    public FlamethrowerItem(Properties p) { super(p.durability(FUEL)); }

    private static int fuel(ItemStack s) { return s.getMaxDamage() - s.getDamageValue(); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack s = player.getItemInHand(hand);
        if (fuel(s) <= 1 && !reload(player, s)) {
            level.playSound(null, player.blockPosition(), ModSounds.FLAME_EMPTY.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
            return InteractionResultHolder.fail(s);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(s);
    }

    private static boolean reload(Player player, ItemStack s) {
        if (player.getAbilities().instabuild) { s.setDamageValue(0); return true; }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack c = player.getInventory().getItem(i);
            if (c.is(ModItems.FUEL.get())) {
                c.shrink(1);
                s.setDamageValue(0);
                player.level().playSound(null, player.blockPosition(), ModSounds.LOCKER.get(), SoundSource.PLAYERS, 0.6F, 1.6F);
                return true;
            }
        }
        return false;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack s, int remaining) {
        Vec3 look = user.getLookAngle();
        Vec3 eye = user.getEyePosition().add(0, -0.15, 0);
        if (level.isClientSide) {
            for (int i = 0; i < 6; i++) {
                double sp = 0.45 + level.random.nextDouble() * 0.35;
                Vec3 v = look.add((level.random.nextDouble() - 0.5) * 0.18, (level.random.nextDouble() - 0.5) * 0.18,
                        (level.random.nextDouble() - 0.5) * 0.18).scale(sp);
                level.addParticle(i % 3 == 0 ? ParticleTypes.SMOKE : ParticleTypes.FLAME, eye.x + look.x * 0.6, eye.y + look.y * 0.6,
                        eye.z + look.z * 0.6, v.x, v.y, v.z);
            }
            return;
        }
        if (!(user instanceof Player player)) return;
        if (fuel(s) <= 1) {
            if (!reload(player, s)) {
                level.playSound(null, user.blockPosition(), ModSounds.FLAME_EMPTY.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
                user.stopUsingItem();
                return;
            }
        }
        if (!player.getAbilities().instabuild) s.setDamageValue(s.getDamageValue() + 1);
        int t = getUseDuration(s) - remaining;
        if (t % 8 == 0) level.playSound(null, user.blockPosition(), ModSounds.FLAMETHROWER.get(), SoundSource.PLAYERS, 1.0F, 0.95F + level.random.nextFloat() * 0.1F);
        if (t % 10 == 0) NoiseSystem.emit((ServerLevel) level, user.position(), 22, NoiseSystem.Kind.FIRE, user);

        // the jet stops at walls
        Vec3 end = eye.add(look.scale(RANGE));
        HitResult hr = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, user));
        double reach = hr.getType() == HitResult.Type.MISS ? RANGE : hr.getLocation().distanceTo(eye);
        AABB box = user.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.5);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != user && e.isAlive())) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            double d = to.length();
            if (d > reach + 0.8 || look.dot(to.normalize()) < 0.88) continue;
            if (e instanceof XenomorphEntity x) x.onFlamed(player);
            else {
                e.setSecondsOnFire(5);
                if (t % 5 == 0) e.hurt(level.damageSources().onFire(), 2.0F);
            }
        }
    }

    @Override
    public int getUseDuration(ItemStack s) { return 72000; }

    @Override
    public UseAnim getUseAnimation(ItemStack s) { return UseAnim.NONE; }

    @Override
    public boolean isValidRepairItem(ItemStack s, ItemStack r) { return false; }

    @Override
    public boolean isEnchantable(ItemStack s) { return false; }

    @Override
    public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> tip, TooltipFlag f) {
        tip.add(Component.translatable("tooltip.alienisolation.fuel", fuel(s), FUEL).withStyle(ChatFormatting.GOLD));
        tip.add(Component.translatable("tooltip.alienisolation.flamethrower").withStyle(ChatFormatting.GRAY));
    }
}
