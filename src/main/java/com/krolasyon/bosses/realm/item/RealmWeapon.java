package com.krolasyon.bosses.realm.item;

import com.krolasyon.bosses.realm.Allegiance;
import com.krolasyon.bosses.realm.story.PlayerPowers;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Weapons of the realm: each lord's blade carries a passive and a right-click power. */
public class RealmWeapon extends SwordItem implements ManaUser {
    public enum Kind { STEEL, ASHBRINGER, BLOODTHIRSTER, SHADOWFANG, HAMMER, REAPER, SOVEREIGN }

    public final Kind kind;

    public RealmWeapon(Tier tier, int damage, float speed, Properties props, Kind kind) {
        super(tier, damage, speed, props);
        this.kind = kind;
    }

    @Nullable
    public Spells.Spell power() {
        return switch (kind) {
            case ASHBRINGER -> Spells.Spell.ASH_NOVA;
            case BLOODTHIRSTER -> Spells.Spell.BLOOD_LANCE;
            case SHADOWFANG -> Spells.Spell.SHADOW_STEP;
            case HAMMER -> Spells.Spell.EARTHSHATTER;
            case REAPER -> Spells.Spell.SOUL_WAVE;
            case SOVEREIGN -> Spells.Spell.METEOR_STRIKE;
            default -> null;
        };
    }

    @Override
    public boolean isFoil(ItemStack stack) { return kind != Kind.STEEL || super.isFoil(stack); }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        switch (kind) {
            case STEEL -> target.setSecondsOnFire(3);
            case SOVEREIGN -> target.setSecondsOnFire(5);
            case ASHBRINGER -> {
                target.setSecondsOnFire(4);
                int hits = stack.getOrCreateTag().getInt("AshHits") + 1;
                if (hits >= 3 && attacker.level() instanceof ServerLevel sl && attacker instanceof Player p) {
                    hits = 0;
                    for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(2.5), e -> e != target && Allegiance.playerMayHit(p, e))) {
                        e.hurt(p.damageSources().playerAttack(p), 6F);
                        e.setSecondsOnFire(4);
                    }
                    sl.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY() + 1, target.getZ(), 1, 0, 0, 0, 0);
                    sl.sendParticles(ParticleTypes.WHITE_ASH, target.getX(), target.getY() + 1, target.getZ(), 40, 1, 0.6, 1, 0.05);
                    sl.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + 1, target.getZ(), 20, 0.6, 0.5, 0.6, 0.08);
                    sl.playSound(null, target.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.7F, 1.3F);
                }
                stack.getOrCreateTag().putInt("AshHits", hits);
            }
            case HAMMER -> {
                target.setDeltaMovement(target.getDeltaMovement().add(attacker.getLookAngle().scale(0.8)).add(0, 0.35, 0));
                target.hurtMarked = true;
            }
            case REAPER -> target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 40, 1), attacker);
            default -> {}
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Spells.Spell s = power();
        if (s == null) return super.use(level, player, hand);
        if (player instanceof ServerPlayer sp && PlayerPowers.useMana(sp, s.mana)) {
            if (Spells.cast(sp, s)) {
                player.getCooldowns().addCooldown(this, s.cooldown);
                stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
            } else {
                PlayerPowers.addMana(sp, s.mana);
            }
        }
        player.swing(hand);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        LoreItem.addLore(getDescriptionId(), lines);
        Spells.Spell s = power();
        if (s != null) lines.add(Component.literal("✦ " + s.mana + " mana  ⌛ " + String.format("%.1f", s.cooldown / 20F) + "s").withStyle(ChatFormatting.BLUE));
        super.appendHoverText(stack, level, lines, flag);
    }
}
