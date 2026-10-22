package com.krolasyon.mythblades.item;

import net.minecraft.ChatFormatting;
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
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * Kor Kılıcı: taştaki sunaktan Kor Mührü ile çekilen akkor kılıç. Vuruşlar tutuşturur; elde tutarken ateşe dayanıklılık verir.
 * Sağ tık: Alev Kesiği (önündeki yayda alev süpürmesi). Eğilip sağ tık: Kor Saplama (yere saplanıp çevreye kor patlaması).
 */
public final class EmberbrandItem extends SwordItem {
    public static final int SLASH_CD = 50, SLAM_CD = 160;

    public EmberbrandItem() {
        super(MythTier.EMBER, 3, -2.4F, new Properties().rarity(Rarity.EPIC).fireResistant());
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean r = super.hurtEnemy(stack, target, attacker);
        target.setSecondsOnFire(5);
        if (attacker.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 14, 0.3, 0.35, 0.3, 0.03);
            sl.sendParticles(ParticleTypes.LAVA, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 2, 0.2, 0.2, 0.2, 0);
        }
        return r;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity e, int slot, boolean selected) {
        if (!level.isClientSide && e instanceof LivingEntity le && (selected || le.getOffhandItem() == stack) && le.tickCount % 20 == 0) {
            MobEffectInstance cur = le.getEffect(MobEffects.FIRE_RESISTANCE);
            if (cur == null || cur.getDuration() < 60) le.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 100, 0, true, false, true));
        }
    }

    private static boolean foe(Player p, LivingEntity e) {
        return e != p && e.isAlive() && !e.isSpectator() && !p.isAlliedTo(e) && !(e instanceof TamableAnimal t && t.isOwnedBy(p))
                && !(e instanceof Player);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean slam = player.isShiftKeyDown() && player.onGround();
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.pass(stack);
        if (level instanceof ServerLevel sl) {
            if (slam) slam(sl, player);
            else slash(sl, player);
            stack.hurtAndBreak(slam ? 3 : 1, player, p -> p.broadcastBreakEvent(hand));
        }
        player.getCooldowns().addCooldown(this, slam ? SLAM_CD : SLASH_CD);
        player.swing(hand, true);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** Alev Kesiği: önündeki 120°'lik yayda 6 blok menzile alev süpürmesi. */
    private static void slash(ServerLevel sl, Player p) {
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z).normalize();
        Vec3 side = new Vec3(-flat.z, 0, flat.x);
        for (int ring = 0; ring < 5; ring++) {
            double r = 1.4 + ring * 1.15;
            for (int k = -8; k <= 8; k++) {
                double a = k / 8.0 * Math.toRadians(60);
                Vec3 d = flat.scale(Math.cos(a)).add(side.scale(Math.sin(a)));
                Vec3 q = eye.add(d.scale(r)).add(0, -0.45 - k * 0.03, 0);
                sl.sendParticles(ring % 2 == 0 ? ParticleTypes.FLAME : ParticleTypes.SMALL_FLAME, q.x, q.y, q.z, 1, 0.05, 0.05, 0.05, 0.01);
            }
        }
        sl.sendParticles(ParticleTypes.SWEEP_ATTACK, eye.x + flat.x * 2, eye.y - 0.4, eye.z + flat.z * 2, 3, 0.8, 0.1, 0.8, 0);
        int hit = 0;
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(6.5), x -> foe(p, x))) {
            Vec3 to = e.position().add(0, e.getBbHeight() / 2, 0).subtract(eye);
            if (to.length() > 6.5) continue;
            Vec3 tf = new Vec3(to.x, 0, to.z);
            if (tf.lengthSqr() > 0.01 && tf.normalize().dot(flat) < 0.45) continue;
            e.hurt(p.damageSources().playerAttack(p), 8F);
            e.setSecondsOnFire(6);
            e.knockback(0.6, -flat.x, -flat.z);
            sl.sendParticles(ParticleTypes.LAVA, e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), 4, 0.3, 0.3, 0.3, 0);
            hit++;
        }
        sl.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1F, 0.6F);
        sl.playSound(null, p.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.9F, 0.9F);
        if (hit > 0) sl.playSound(null, p.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.7F, 0.8F);
    }

    /** Kor Saplama: kılıcı yere saplar, 5 blokta kor halkası patlar; düşmanları havaya atar ve tutuşturur. */
    private static void slam(ServerLevel sl, Player p) {
        Vec3 c = p.position();
        for (int ring = 1; ring <= 5; ring++) {
            int n = ring * 10;
            for (int k = 0; k < n; k++) {
                double a = Math.PI * 2 * k / n;
                double x = c.x + Math.cos(a) * ring, z = c.z + Math.sin(a) * ring;
                sl.sendParticles(ParticleTypes.FLAME, x, c.y + 0.1, z, 1, 0.05, 0.25, 0.05, 0.04);
                if (k % 3 == 0) sl.sendParticles(ParticleTypes.LAVA, x, c.y + 0.2, z, 1, 0, 0, 0, 0);
            }
        }
        sl.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 0.5, c.z, 2, 0.4, 0.1, 0.4, 0);
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, c.x, c.y + 0.3, c.z, 30, 1.5, 0.2, 1.5, 0.03);
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(5.5, 2.5, 5.5), x -> foe(p, x))) {
            double d = e.distanceTo(p);
            if (d > 5.5) continue;
            float dmg = (float) (10 - d);
            e.hurt(p.damageSources().playerAttack(p), dmg);
            e.setSecondsOnFire(8);
            Vec3 away = e.position().subtract(c).multiply(1, 0, 1).normalize();
            e.setDeltaMovement(e.getDeltaMovement().add(away.x * 0.7, 0.65, away.z * 0.7));
            e.hurtMarked = true;
        }
        sl.playSound(null, p.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8F, 1.3F);
        sl.playSound(null, p.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1F, 0.6F);
        sl.playSound(null, p.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.5F, 0.6F);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("tooltip.mythblades.ember.1").withStyle(ChatFormatting.GOLD));
        tip.add(Component.translatable("tooltip.mythblades.ember.2").withStyle(ChatFormatting.RED));
        tip.add(Component.translatable("tooltip.mythblades.ember.3").withStyle(ChatFormatting.YELLOW));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(com.krolasyon.mythblades.client.ClientHooks.ITEM_EXTENSIONS);
    }
}
